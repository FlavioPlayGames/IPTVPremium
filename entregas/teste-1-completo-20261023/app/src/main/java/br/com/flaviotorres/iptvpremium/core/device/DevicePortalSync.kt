package br.com.flaviotorres.iptvpremium.core.device

import android.content.Context
import android.os.Build
import android.util.AtomicFile
import br.com.flaviotorres.iptvpremium.BuildConfig
import br.com.flaviotorres.iptvpremium.core.player.*
import br.com.flaviotorres.iptvpremium.data.storage.PlaylistStorage
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/** The private credential is never shown and never included in Android cloud backup. */
internal class PortalIdentityStore(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "device_portal_v1.json"))
    fun load(): JSONObject = synchronized(lock) {
        val saved = if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists())
            JSONObject(file.readFully().toString(Charsets.UTF_8)) else null
        if (saved?.optString("server") == BuildConfig.DEVICE_PORTAL_URL) {
            require(saved.optString("secret").matches(Regex("[0-9a-f]{64}")))
            saved
        } else {
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val created = JSONObject().put("server", BuildConfig.DEVICE_PORTAL_URL)
                .put("secret", bytes.joinToString("") { "%02x".format(it.toInt() and 255) })
            save(created)
            created
        }
    }
    fun registeredIdentity(): DeviceIdentity? = load().let { data ->
        val mac = data.optString("mac").takeIf { it.isNotBlank() } ?: return@let null
        DeviceIdentity(mac, data.getString("code"))
    }
    fun save(data: JSONObject) = synchronized(lock) {
        val stream = file.startWrite()
        try { stream.write(data.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw error }
    }
    companion object { private val lock = Any() }
}

internal object PortalListPreparation {
    val activeRequests = MutableStateFlow(0)
    fun begin() { activeRequests.update { it + 1 } }
    fun finish() { activeRequests.update { (it - 1).coerceAtLeast(0) } }
}

internal object PortalAccess {
    val checking = MutableStateFlow(BuildConfig.DEVICE_PORTAL_URL.isNotBlank() || !BuildConfig.DEBUG)
    fun connectionFailed() {
        if (message.value == "Validando acesso à central…")
            message.value = "Não foi possível conectar à central. Confira a internet; tentaremos novamente automaticamente."
    }
    val message = MutableStateFlow<String?>(if (BuildConfig.DEVICE_PORTAL_URL.isNotBlank() || !BuildConfig.DEBUG) "Validando acesso à central…" else null)
}

/** Server-side queue: lists and keys stay on this device after an acknowledgement. */
class DevicePortalSync(context: Context) {
    private val app = context.applicationContext
    /** Reuse the existing access policy before waiting for portal network/commands. */
    suspend fun restoreAccessForOpening() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val start = android.os.SystemClock.elapsedRealtime()
        if (BuildConfig.DEVICE_PORTAL_URL.isBlank()) {
            if (!BuildConfig.DEBUG) PortalAccess.message.value = "A central de acesso ainda não foi configurada."
            PortalAccess.checking.value = false
            return@withContext
        }
        val state = PortalIdentityStore(app).load()
        if (state.has("mac") && state.optLong("serverNow") > 0L) {
            applyCachedAccess(state) // Same expiry/blocked checks; no new offline entitlement.
            PortalAccess.checking.value = false
        }
        android.util.Log.i("StartupPerf", "etapa=acesso_local cache=${state.has("mac")} duracaoMs=${android.os.SystemClock.elapsedRealtime()-start}")
    }
    suspend fun sync(force: Boolean = false, automatic: Boolean = false, refreshCatalog: Boolean = false): String = mutex.withLock {
        if (BuildConfig.DEVICE_PORTAL_URL.isBlank()) {
            if (!BuildConfig.DEBUG) PortalAccess.message.value = "A central de acesso ainda não foi configurada."
            return@withLock "A central de acesso ainda não foi configurada."
        }
        val endpoint = URL(BuildConfig.DEVICE_PORTAL_URL)
        require(endpoint.protocol == "https" || (BuildConfig.DEBUG && endpoint.protocol == "http" &&
            endpoint.host in listOf("10.0.2.2", "127.0.0.1", "localhost")))
        require(endpoint.userInfo == null && endpoint.query == null && endpoint.ref == null && endpoint.path in listOf("", "/"))
        val identityStore = PortalIdentityStore(app)
        val state = identityStore.load()
        applyCachedAccess(state)
        flushTrialChoice(state)
        val lastAttempt = state.optLong(if (automatic) "lastAutomaticSuccess" else "lastAttempt", 0)
        val interval = if (automatic) 24 * 60 * 60_000L else 30 * 60_000L
        if (!force && System.currentTimeMillis() - lastAttempt in 0 until interval) return@withLock "Próxima verificação automática em até 24 horas."
        state.put("lastAttempt", System.currentTimeMillis())
        identityStore.save(state)
        val secret = state.getString("secret")
        val recoverySecret = try { reinstallRecoverySecret(app) } catch (error: Exception) {
            PortalAccess.message.value = "Não foi possível obter a identificação estável desta TV. Entre em contato com o administrador."
            throw error
        }
        val recoveryBinding = fingerprint(recoverySecret)
        if (!state.has("mac") || state.optString("recoveryBinding") != recoveryBinding) {
            val registration = call("/api/device/register", "POST", JSONObject().put("secret", secret)
                .put("model", "${Build.MANUFACTURER} ${Build.MODEL}".take(80))
                .put("recoverySecret", recoverySecret), null)
            state.put("recoveryBinding", recoveryBinding)
            saveIdentityAndLicense(state, registration)
            identityStore.save(state)
            applyCachedAccess(state)
        }
        val config = call(if (automatic) "/api/device/pull?mode=auto" else "/api/device/pull", "GET", null, secret)
        if (automatic) check(config.optInt("syncPolicyVersion", 0) >= 2 && config.optString("syncMode") == "lists") {
            "Publique a central atualizada antes de usar esta versão do aplicativo."
        }
        if (config.has("appUpdate")) br.com.flaviotorres.iptvpremium.core.update.AppUpdateManager.receive(
            if (config.isNull("appUpdate")) null else config.getJSONObject("appUpdate"))
        saveIdentityAndLicense(state, config)
        identityStore.save(state)
        applyCachedAccess(state)
        flushTrialChoice(state)
        val commands = config.getJSONArray("commands")
        if (commands.length() == 0) {
            if (refreshCatalog) PlaylistStorage(app).requestActiveCatalogLoad()
            if (automatic) { state.put("lastAutomaticSuccess", System.currentTimeMillis()); identityStore.save(state) }
            return@withLock "Conectado à central. Nenhum envio pendente."
        }
        val storage = PlaylistStorage(app)
        val appearanceStore = SubtitleAppearanceStore(app)
        val subdl = SubdlCredentialStore(app)
        val subsource = br.com.flaviotorres.iptvpremium.ui.settings.SubsourceCredentialStore(app)
        val processed = state.optJSONObject("processed") ?: JSONObject()
        val results = JSONArray()
        var receivedCatalogRequest = false
        for (i in 0 until commands.length()) {
            val command = commands.getJSONObject(i)
            if (automatic && command.getString("kind") !in listOf("list_upsert", "list_delete", "snapshot")) continue
            val id = command.getString("id")
            val cached = processed.optJSONObject(id)
            val result = if (cached != null) cached else {
                val data = command.getJSONObject("data")
                try {
                    when (command.getString("kind")) {
                        "snapshot" -> Unit
                        "list_upsert" -> {
                            val source = decodePortalLists(JSONArray().put(data)).single()
                            storage.applyPortalOperation(source.id, source)
                            if (source.isEnabled) receivedCatalogRequest = true
                        }
                        "list_delete" -> storage.applyPortalOperation(data.getString("id"), null)
                        "subtitle_style" -> appearanceStore.save(decodePortalStyle(data))
                        "subtitle_key" -> {
                            val provider = data.getString("provider")
                            require(provider in listOf("subdl", "subsource"))
                            if (data.isNull("key")) {
                                if (provider == "subdl") subdl.disconnect() else subsource.disconnect()
                                check(if (provider == "subdl") !subdl.isConfigured() else !subsource.isConfigured())
                            } else {
                                val key = data.getString("key")
                                require(key.length in 8..512 && key.none { it.isWhitespace() || it.isISOControl() })
                                check(if (provider == "subdl") subdl.saveKeyFromPortal(key) else subsource.saveKey(key))
                            }
                        }
                        else -> throw IllegalArgumentException("Comando inválido")
                    }
                    JSONObject().put("id", id).put("success", true)
                } catch (_: IllegalArgumentException) {
                    JSONObject().put("id", id).put("success", false).put("error", "invalid_data")
                } catch (_: Exception) {
                    JSONObject().put("id", id).put("success", false).put("error", "storage_error")
                }.also {
                    // Persist receipt before requesting deletion from the server.
                    processed.put(id, it)
                    while (processed.length() > 128) processed.remove(processed.keys().next().toString())
                    state.put("processed", processed)
                    identityStore.save(state)
                }
            }
            results.put(result)
        }
        if (refreshCatalog && !receivedCatalogRequest) storage.requestActiveCatalogLoad()
        val snapshot = JSONObject().put("lists", encodePortalLists(storage.playlists.first()))
            .put("subtitles", encodePortalStyle(appearanceStore.load()))
            .put("providers", JSONObject().put("subdl", subdl.isConfigured()).put("subsource", subsource.isConfigured()))
        call("/api/device/ack", "POST", JSONObject().put("results", results).put("snapshot", snapshot), secret)
        if (automatic) { state.put("lastAutomaticSuccess", System.currentTimeMillis()); identityStore.save(state) }
        val failures = (0 until results.length()).count { !results.getJSONObject(it).getBoolean("success") }
        if (failures > 0) "Sincronização concluída, mas $failures envio(s) não puderam ser aplicados. Confira o status no site."
        else "Sincronização concluída. Envios recebidos pelo aplicativo: ${results.length()}."
    }

    internal suspend fun registerPushToken(token: String, projectId: String) = mutex.withLock {
        val state = PortalIdentityStore(app).load()
        if (!state.has("mac")) return@withLock
        val binding = fingerprint(projectId + ":" + token)
        val now = System.currentTimeMillis()
        if (state.optString("pushBinding") == binding && now - state.optLong("pushRegisteredAt") in 0 until 30 * 24 * 60 * 60_000L) return@withLock
        call("/api/device/push-token", "PUT", JSONObject().put("token", token).put("projectId", projectId), state.getString("secret"))
        state.put("pushBinding", binding).put("pushRegisteredAt", now)
        PortalIdentityStore(app).save(state)
    }

    suspend fun createQrLogin(destination: String = "lists"): Pair<String, Long> = mutex.withLock {
        val endpoint = URL(BuildConfig.DEVICE_PORTAL_URL)
        require(endpoint.protocol == "https" && endpoint.userInfo == null)
        val state = PortalIdentityStore(app).load()
        require(state.has("mac")) { "Sincronize o aparelho primeiro." }
        val response = call("/api/device/qr-login", "POST", JSONObject(), state.getString("secret"))
        val link = response.getString("url")
        require(link.startsWith(BuildConfig.DEVICE_PORTAL_URL.trimEnd('/') + "/#access="))
        require(destination in listOf("lists", "subtitles", "payment"))
        (link + when (destination) { "subtitles" -> "&view=subtitles"; "payment" -> "&view=payment"; else -> "" }) to response.getLong("expires")
    }

    private fun saveIdentityAndLicense(state: JSONObject, data: JSONObject) {
        require(normalizedHardwareMac(data.getString("mac")) == data.getString("mac"))
        require(data.getString("code").matches(Regex("[0-9]{6}")))
        if (data.has("renewal")) state.put("renewal", data.getJSONObject("renewal"))
        if (data.has("trialNotice")) {
            if (data.isNull("trialNotice")) state.remove("trialNotice")
            else state.put("trialNotice", data.getJSONObject("trialNotice"))
        }
        val license = data.getJSONObject("license")
        state.put("trialUntil", license.optLong("trialUntil", 0)).put("licenseUntil", license.optLong("licenseUntil", 0))
        require(license.getString("state") in listOf("trial", "licensed", "expired", "blocked"))
        state.put("mac", data.getString("mac")).put("code", data.getString("code"))
            .put("label", data.getString("label")).put("licenseState", license.getString("state"))
            .put("validUntil", license.getLong("validUntil")).put("serverNow", license.getLong("serverNow"))
            .put("elapsedAtValidation", android.os.SystemClock.elapsedRealtime())
            .put("clockOffset", license.getLong("serverNow") - System.currentTimeMillis())
    }

    private fun applyCachedAccess(state: JSONObject) {
        val serverNow = state.optLong("serverNow", 0)
        val elapsedNow = android.os.SystemClock.elapsedRealtime()
        val elapsedAtValidation = state.optLong("elapsedAtValidation", elapsedNow)
        val elapsedDelta = (elapsedNow - elapsedAtValidation).coerceAtLeast(0)
        val now = maxOf(System.currentTimeMillis() + state.optLong("clockOffset", 0), serverNow + elapsedDelta)
        PortalTrialState.receive(app, state, now, elapsedNow)
        PortalAccess.message.value = when {
            !state.has("mac") || serverNow == 0L -> "Conecte à internet para iniciar os seus 7 dias de teste."
            state.optString("licenseState") == "blocked" -> "Este aparelho foi bloqueado pelo administrador. Entre em contato com o responsável."
            state.optLong("validUntil") <= now && (state.optString("licenseState") == "trial" ||
                (state.has("licenseUntil") && state.optLong("licenseUntil") == 0L)) -> "Seu período de experiência terminou. Para continuar usando o aplicativo, faça a ativação pela central."
            state.optLong("validUntil") <= now -> "Seu período de acesso terminou. Para continuar usando o aplicativo, renove a assinatura anual na central. Se já pagou, aguarde a confirmação do administrador."
            else -> null
        }
        PortalRenewalState.receive(if (PortalAccess.message.value == null) state.optJSONObject("renewal") else null)
        // Release the splash only after the existing access policy has run with
        // a stored or server-confirmed license. Pull/ack may continue afterwards.
        if (state.has("mac") && serverNow > 0L) PortalAccess.checking.value = false
    }

    internal suspend fun sendPendingTrialChoice(): Boolean = mutex.withLock {
        flushTrialChoice(PortalIdentityStore(app).load())
    }

    private fun flushTrialChoice(state: JSONObject): Boolean {
        val pending = TrialAcknowledgement.pending(app) ?: return true
        if (pending.mac != state.optString("mac") || pending.expiresAt != state.optLong("trialUntil")) return true
        if (state.optString("licenseState") != "trial") {
            TrialAcknowledgement.clearPending(app, pending)
            return true
        }
        if (!TrialAcknowledgement.readyToRetry(app)) return true
        return try {
            val result = call("/api/device/trial-choice", "POST", JSONObject()
                .put("choice", pending.choice).put("expiresAt", pending.expiresAt), state.getString("secret"))
            val notice = result.getJSONObject("trialNotice")
            check(notice.getLong("expiresAt") == pending.expiresAt && notice.getBoolean("acknowledged"))
            state.put("trialNotice", notice)
            PortalIdentityStore(app).save(state)
            TrialAcknowledgement.clearPending(app, pending)
            applyCachedAccess(state)
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) {
            runCatching { TrialAcknowledgement.failed(app, pending) }
            false
        } // Small persisted backoff; does not trigger a catalogue import or full pull.
    }

    internal suspend fun chooseRenewal(choice: String, reminder: RenewalReminder) = mutex.withLock {
        require(choice in listOf("now", "due"))
        val store = PortalIdentityStore(app)
        val state = store.load()
        val response = call("/api/device/renewal-choice", "POST", JSONObject().put("choice", choice)
            .put("stage", reminder.stage).put("expiresAt", reminder.expiresAt), state.getString("secret"))
        val renewal = response.getJSONObject("renewal")
        state.optJSONObject("renewal")?.let { old -> if (old.has("amountCents")) renewal.put("amountCents", old.opt("amountCents")) }
        state.put("renewal", renewal)
        store.save(state)
        PortalRenewalState.receive(renewal)
    }

    private fun call(path: String, method: String, body: JSONObject?, secret: String?): JSONObject {
        br.com.flaviotorres.iptvpremium.core.network.AppNetwork.initialize(app)
        val request=okhttp3.Request.Builder().url(BuildConfig.DEVICE_PORTAL_URL+path)
            .header("Accept","application/json").header("X-IPTV-Update-Protocol","2")
        secret?.let{request.header("Authorization","Bearer $it")}
        val payload=body?.toString()?.toByteArray(Charsets.UTF_8)?.let{it.toRequestBody("application/json".toMediaType())}
        request.method(method,payload)
        val client=br.com.flaviotorres.iptvpremium.core.network.AppNetwork.client().newBuilder()
            .connectTimeout(7,java.util.concurrent.TimeUnit.SECONDS).readTimeout(10,java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(false).build()
        return client.newCall(request.build()).execute().use{response->
            check(response.isSuccessful){"Portal indisponível (${response.code})"}
            val stream=response.body.byteStream();val bytes=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true){val size=stream.read(buffer);if(size<0)break;check(bytes.size()+size<=512000);bytes.write(buffer,0,size)}
            JSONObject(bytes.toString("UTF-8"))
        }
    }

    companion object { private val mutex = Mutex() }
}

internal fun fingerprint(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

internal fun encodePortalStyle(value: SubtitleAppearance): JSONObject = JSONObject()
    .put("textColor", "%06X".format(value.textArgb and 0xFFFFFF))
    .put("background", value.background.name).put("sizeSp", value.sizeSp)
    .put("typeface", value.typeface.name).put("edge", value.edge.name)
internal fun decodePortalStyle(value: JSONObject): SubtitleAppearance {
    val color = value.getString("textColor")
    require(color in listOf("FFFFFF", "F2F2F2", "E5E5E5", "FFFF00", "FFD54F", "FFE991"))
    val size = value.getInt("sizeSp"); require(size in listOf(18, 22, 26, 30))
    return SubtitleAppearance((0xFF000000L or color.toLong(16)).toInt(),
        SubtitleBackground.valueOf(value.getString("background")), size,
        SubtitleTypeface.valueOf(value.getString("typeface")), SubtitleEdge.valueOf(value.getString("edge")))
}

/** Stable recovery capability scoped to this application and signing certificate.
 * ANDROID_ID stays local; it is not a hardware MAC or a proof against cloned ROMs.
 */
internal fun reinstallRecoverySecret(context: Context): String {
    val androidId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        ?.lowercase(java.util.Locale.ROOT)?.takeIf { it.matches(Regex("[0-9a-f]{1,16}")) }?.padStart(16, '0')
    require(androidId != null && androidId != "0000000000000000" && androidId != "9774d56d682e549c") {
        "O Android não forneceu uma identificação estável. Contate o administrador."
    }
    val info = PackageSigningCompat.installed(context.packageManager, context.packageName)
    val certs = PackageSigningCompat.signers(info, lineage = true)
    require(certs.isNotEmpty()) { "Assinatura indisponível." }
    val fingerprints = certs.map { cert -> MessageDigest.getInstance("SHA-256").digest(cert.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) } }
    val certificate = if (certs.size > 1 && PackageSigningCompat.signers(info).size > 1) fingerprints.sorted().joinToString(",") else fingerprints.first()
    return fingerprint("IPTVPremium/reinstall/v1|${context.packageName}|$certificate|$androidId")
}

// IPTV_PORTAL_SYNC_BUTTON_V1

// IPTV_SUBTITLE_LAYOUT_PORTAL_LOAD_V1

// IPTV_MANDATORY_UPDATE_V1

// IPTV_REINSTALL_RECOVERY_V1

// IPTV_RENEWAL_AND_DAILY_SYNC_V1

// IPTV_LIST_LOADING_AND_PLAYER_INPUT_V1

// IPTV_FIREBASE_LIST_HINTS_V1
