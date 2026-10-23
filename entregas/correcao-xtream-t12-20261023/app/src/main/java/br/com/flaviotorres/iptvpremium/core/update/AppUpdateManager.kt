package br.com.flaviotorres.iptvpremium.core.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ClipData
import br.com.flaviotorres.iptvpremium.core.device.PackageSigningCompat
import android.os.StatFs
import android.util.AtomicFile
import androidx.core.content.FileProvider
import br.com.flaviotorres.iptvpremium.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class AppUpdateState(val policy: AppUpdatePolicy? = null, val stage: String = "idle", val progress: Float = 0f, val message: String? = null, val showPrompt: Boolean = false)

/** No APK or installation credential is sent through the portal database. */
internal object AppUpdateManager {
    private const val REMINDER_DELAY_MS = 15L * 24 * 60 * 60 * 1000
    val state = MutableStateFlow(AppUpdateState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private lateinit var app: Context
    private val preferences get() = app.getSharedPreferences("iptv_optional_updates", Context.MODE_PRIVATE)
    private val policyFile get() = AtomicFile(File(app.noBackupFilesDir, "mandatory_update.json"))
    private val directory get() = File(app.cacheDir, "updates").apply { mkdirs() }
    private fun target(policy: AppUpdatePolicy) = File(directory, "update-${policy.versionCode}-${policy.sha256.take(16)}.apk")

    fun initialize(context: Context) {
        app = context.applicationContext
        // Adopt the reminder deadline for deferrals saved by the earlier 1.03 test build.
        if (preferences.getLong("deferred_version", 0L) > 0L && !preferences.contains("deferred_until")) {
            preferences.edit().putLong("deferred_until", System.currentTimeMillis() + REMINDER_DELAY_MS).apply()
        }
        runCatching {
            val file = policyFile
            if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                require(file.baseFile.length() <= 16_384)
                receive(JSONObject(file.readFully().toString(Charsets.UTF_8)))
            } else directory.listFiles()?.forEach { it.delete() }
        }.onFailure { policyFile.delete() }
    }

    @Synchronized fun receive(data: JSONObject?, now: Long = System.currentTimeMillis()) {
        val policy = data?.let { AppUpdatePolicy.parse(it) }?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        val reminderDue = now >= preferences.getLong("deferred_until", 0L)
        if (policy != null && policy == state.value.policy) {
            state.update { if (!it.showPrompt && reminderDue && it.stage != "installed") it.copy(showPrompt = true) else it }
            return
        }
        downloadJob?.cancel()
        if (policy == null) {
            policyFile.delete()
            directory.listFiles()?.forEach { it.delete() }
        } else {
            val file = policyFile; val output = file.startWrite()
            try { output.write(policy.json().toString().toByteArray()); file.finishWrite(output) }
            catch (error: Exception) { file.failWrite(output); throw error }
        }
        state.value = AppUpdateState(policy, showPrompt = policy != null && reminderDue)
    }

    @Synchronized fun defer(now: Long = System.currentTimeMillis()) {
        val current = state.value.policy ?: return
        preferences.edit().putLong("deferred_version", current.versionCode).putLong("deferred_until", now + REMINDER_DELAY_MS).apply()
        state.update { it.copy(showPrompt = false) }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val endpoint = URL(BuildConfig.DEVICE_PORTAL_URL.trimEnd('/') + "/api/app/update")
        require(endpoint.protocol == "https" && endpoint.userInfo == null)
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.setRequestProperty("X-IPTV-Update-Protocol", "2")
            connection.connectTimeout = 7_000; connection.readTimeout = 10_000; connection.instanceFollowRedirects = false
            check(connection.responseCode == 200)
            val bytes = connection.inputStream.use(::readUpdatePolicyBytes); require(bytes.size <= 16_384)
            val body = JSONObject(bytes.toString(Charsets.UTF_8))
            require(body.has("update")) { "Resposta de atualização inválida." }
            receive(if (body.isNull("update")) null else body.getJSONObject("update"))
            state.update { it.copy(showPrompt = it.policy != null) }
        } finally { connection.disconnect() }
    }

    fun download() {
        val policy = state.value.policy ?: return
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            var reservation: br.com.flaviotorres.iptvpremium.core.storage.CacheStorageBudget.Reservation? = null
            var pending = File(directory, "download-${policy.versionCode}-${java.util.UUID.randomUUID()}.part")
            try {
                state.update { if (it.policy == policy) it.copy(stage = "downloading", progress = 0f, message = null) else it }
                val apk = target(policy)
                if (!apk.exists()) {
                    directory.listFiles()?.filter { it != pending }?.forEach { it.delete() }
                    require(StatFs(directory.path).availableBytes > policy.sizeBytes + 32L * 1024 * 1024) { "Espaço insuficiente. Libere espaço no aparelho e tente novamente." }
                    reservation = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(app)
                        .reserve(apk, policy.sizeBytes)
                        ?: error("A atualização excede o espaço disponível no limite de cache.")
                    pending = reservation.temporary
                    downloadFile(policy, pending)
                    state.update { if (it.policy == policy) it.copy(stage = "verifying", progress = 1f) else it }
                    verify(policy, pending)
                    currentCoroutineContext().ensureActive()
                    check(state.value.policy == policy)
                    check(pending.renameTo(apk)) { "Não foi possível salvar a atualização." }
                } else verify(policy, apk)
                state.update { if (it.policy == policy) it.copy(stage = "ready", progress = 1f, message = "Download verificado. Confirme a instalação no Android.") else it }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                target(policy).delete()
                state.update { if (it.policy == policy) it.copy(stage = "error", message = safeMessage(error)) else it }
            } finally { pending.delete(); reservation?.close() }
        }
    }

    private suspend fun downloadFile(policy: AppUpdatePolicy, pending: File) {
        var address = URL(policy.url)
        repeat(6) { hop ->
            require(address.protocol == "https" && address.userInfo == null) { "O link da atualização precisa manter uma conexão HTTPS segura." }
            val connection = address.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000; connection.readTimeout = 30_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept-Encoding", "identity")
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    require(hop < 5) { "O link do APK redirecionou muitas vezes." }
                    address = URL(address, connection.getHeaderField("Location") ?: error("Link de download inválido."))
                } else {
                    check(status == 200) { "O download não está disponível. Tente novamente mais tarde." }
                    val length = connection.contentLengthLong
                    require(length == -1L || length == policy.sizeBytes) { "O tamanho do APK não corresponde à versão publicada." }
                    var total = 0L; var lastProgress = 0L
                    connection.inputStream.use { input -> pending.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer); if (n < 0) break
                            total += n; require(total <= policy.sizeBytes) { "O tamanho do APK não corresponde à versão publicada." }
                            output.write(buffer, 0, n)
                            if (total - lastProgress >= 256 * 1024 || total == policy.sizeBytes) {
                                lastProgress = total
                                state.update { if (it.policy == policy) it.copy(progress = total.toFloat() / policy.sizeBytes) else it }
                            }
                        }
                    } }
                    require(total == policy.sizeBytes) { "O download ficou incompleto. Tente novamente." }
                    return
                }
            } finally { connection.disconnect() }
        }
        error("Não foi possível abrir o link do APK.")
    }

    internal fun verify(policy: AppUpdatePolicy, file: File) {
        require(file.length() == policy.sizeBytes) { "O download ficou incompleto. Tente novamente." }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(64 * 1024); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        require(hex(digest.digest()) == policy.sha256) { "Falha na verificação do APK. Baixe novamente." }
        verifyUpdateApkAbi(file, policy.abi, android.os.Build.SUPPORTED_ABIS.toList())
        val pm = app.packageManager
        val candidate = PackageSigningCompat.archive(pm, file.path) ?: error("Este arquivo não é um APK válido.")
        val installed = PackageSigningCompat.installed(pm, app.packageName)
        require(candidate.packageName == app.packageName) { "Este APK pertence a outro aplicativo." }
        require(PackageSigningCompat.version(candidate) == policy.versionCode && PackageSigningCompat.version(candidate) > PackageSigningCompat.version(installed)) { "O número interno do APK não corresponde à atualização publicada." }
        val current = PackageSigningCompat.signers(installed).map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }?.toSet().orEmpty()
        val incoming = PackageSigningCompat.signers(candidate).map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }?.toSet().orEmpty()
        require(current.isNotEmpty() && current == incoming) { "A assinatura deste APK é diferente. O administrador precisa publicar um APK assinado com a mesma chave." }
    }

    @Suppress("DEPRECATION") // This Android installer action supports returning the user's result.
    suspend fun install(context: Context, launchInstaller: (Intent) -> Unit) {
        val before = state.value
        val policy = before.policy ?: return
        if (!before.showPrompt || before.stage !in listOf("ready", "install_cancelled", "install_failed")) return
        if (!state.compareAndSet(before, before.copy(stage = "preparing_install", message = null))) return
        try {
            val apk = target(policy)
            withContext(Dispatchers.IO) { verify(policy, apk) }
            currentCoroutineContext().ensureActive()
            if (state.value.policy != policy) return
            if (!state.value.showPrompt) {
                state.update { if (it.policy == policy) it.copy(stage = "ready") else it }
                return
            }
            val uri = FileProvider.getUriForFile(context, app.packageName + ".updates", apk)
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            intent.clipData = ClipData.newRawUri("Atualização IPTV Premium", uri)
            state.update { if (it.policy == policy) it.copy(stage = "installing") else it }
            launchInstaller(intent)
        } catch (cancelled: CancellationException) {
            state.update { if (it.policy == policy && it.stage == "preparing_install") it.copy(stage = "ready") else it }
            throw cancelled
        } catch (error: Exception) {
            state.update { if (it.policy == policy) it.copy(stage = "install_failed", message = safeMessage(error)) else it }
        }
    }

    fun installationResult(policy: AppUpdatePolicy, resultCode: Int) {
        state.update {
            if (it.policy != policy || it.stage != "installing") it
            else when (resultCode) {
                Activity.RESULT_OK -> it.copy(stage = "installed", showPrompt = false, message = null)
                Activity.RESULT_CANCELED -> it.copy(stage = "install_cancelled",
                    message = "Instalação cancelada. Você pode tentar novamente ou escolher “Mais tarde” para continuar usando o aplicativo.")
                else -> it.copy(stage = "install_failed",
                    message = "O Android não concluiu a instalação. Você pode tentar novamente ou escolher “Mais tarde”.")
            }
        }
    }

    fun reportRefreshFailure() { state.update { it.copy(message = "Não foi possível consultar a central. Confira a internet e tente novamente.") } }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun safeMessage(error: Exception): String = when(error) {
        is java.net.SocketTimeoutException, is java.net.UnknownHostException -> "Falha de conexão. Confira a internet e tente novamente."
        is IllegalArgumentException, is IllegalStateException -> error.message ?: "Não foi possível validar a atualização."
        else -> "Não foi possível concluir a atualização. Tente novamente."
    }
}

/** Bounded policy read without InputStream.readNBytes, which needs Android 13. */
internal fun readUpdatePolicyBytes(input: InputStream): ByteArray {
    val buffer = ByteArray(16_385)
    var count = 0
    while (count < buffer.size) {
        val read = input.read(buffer, count, buffer.size - count)
        if (read < 0) break
        if (read == 0) {
            val next = input.read()
            if (next < 0) break
            buffer[count++] = next.toByte()
        } else {
            count += read
        }
    }
    return buffer.copyOf(count)
}
