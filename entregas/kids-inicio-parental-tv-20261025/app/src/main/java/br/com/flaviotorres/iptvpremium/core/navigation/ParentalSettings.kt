package br.com.flaviotorres.iptvpremium.ui.settings

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.core.device.PortalOutlineButton

/** PIN local: hash com sal, sem PIN em texto e fora do backup do Android. */
internal class ParentalPinStore(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "parental_pin_v1.json"))
    private fun read(): JSONObject = if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists())
        JSONObject(file.readFully().toString(Charsets.UTF_8)) else JSONObject()
    private fun write(value: JSONObject) {
        val stream = file.startWrite()
        try { stream.write(value.toString().toByteArray()); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
    fun enabled(): Boolean = synchronized(lock) { file.baseFile.exists() || File(file.baseFile.path + ".bak").exists() }
    fun set(pin: String) = synchronized(lock) {
        require(pin.length == 6 && pin.all { it in '0'..'9' })
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        write(JSONObject().put("salt", hex(salt)).put("hash", hex(derive(pin, salt))).put("failures", 0).put("until", 0))
    }
    fun verify(pin: String): Boolean = synchronized(lock) {
        if (!enabled()) return@synchronized true
        val data = read()
        val now = System.currentTimeMillis()
        check(now >= data.optLong("until")) { "Aguarde 30 segundos antes de tentar novamente." }
        val valid = pin.length in 4..6 && pin.all { it in '0'..'9' } &&
            MessageDigest.isEqual(derive(pin, bytes(data.getString("salt"))), bytes(data.getString("hash")))
        val failures = if (valid) 0 else data.optInt("failures") + 1
        data.put("failures", if (failures >= 5) 0 else failures)
            .put("until", if (failures >= 5) now + 30_000L else 0L)
        write(data)
        valid
    }
    fun disable() = synchronized(lock) { file.delete() }
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    private fun hex(value: ByteArray) = value.joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun bytes(value: String): ByteArray {
        require(value.length in listOf(32, 64) && value.matches(Regex("[0-9a-f]+")))
        return value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
    companion object { private val lock = Any() }
}

internal object ParentalSession {
    val enabled = MutableStateFlow(false)
    val unlocked = MutableStateFlow(false)
    val epoch = MutableStateFlow(0L)
    fun initialize(context: Context) {
        enabled.value = ParentalPinStore(context).enabled()
        unlocked.value = false
        ParentalPolicy.initialize(context)
    }
    fun lock() { unlocked.value = false; ParentalPolicy.grants.value = emptySet(); epoch.value += 1L }
}

@Composable
internal fun PinField(label: String, value: String, onValue: (String) -> Unit, enabled: Boolean = true, legacyAuth: Boolean = false) {
    NumericPinField(label, value, onValue, enabled, if (legacyAuth) 4 else 6)
}

@Composable
internal fun ParentalAccess(content: @Composable () -> Unit) {
    val enabled by ParentalSession.enabled.collectAsState()
    val unlocked by ParentalSession.unlocked.collectAsState()
    val rules by ParentalPolicy.rules.collectAsState()
    val epoch by ParentalSession.epoch.collectAsState()
    if (!enabled || !rules.general || rules.kidsOnly || rules.startInKids || unlocked) { content(); return }
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var pin by remember(epoch) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember(epoch) { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF080A0C)), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("CONTROLE PARENTAL", color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0), fontSize = 25.sp)
            Text("Digite o PIN para acessar o aplicativo.", color = Color.White, fontSize = 17.sp)
            PinField("PIN", pin, { pin = it }, !busy, legacyAuth = true)
            PortalOutlineButton(if (busy) "Validando…" else "Entrar", enabled = !busy && pin.length >= 4) {
                scope.launch {
                    busy = true
                    try {
                        val valid = withContext(Dispatchers.IO) { ParentalPinStore(context).verify(pin) }
                        if (valid && epoch == ParentalSession.epoch.value) ParentalSession.unlocked.value = true else message = "PIN incorreto. Após cinco erros, aguarde 30 segundos."
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { message = "Não foi possível validar. Se houve cinco erros, aguarde 30 segundos." }
                    finally { pin = ""; busy = false }
                }
            }
            message?.let { Text(it, color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0), fontSize = 15.sp) }
        }
    }
}
