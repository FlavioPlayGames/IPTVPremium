package br.com.flaviotorres.iptvpremium.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.core.player.ExternalSubtitleRepository
import br.com.flaviotorres.iptvpremium.core.player.SubdlConnectionResult
import br.com.flaviotorres.iptvpremium.core.player.SubdlCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val gold get() = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.color(0xFFFFE6A0)
private val settingsGoldBorder get() = Brush.linearGradient(listOf(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFF926315)))
private val panelShape = RoundedCornerShape(18.dp)

/** Entrada Configurações da Home. Não abre nenhuma pesquisa de legenda. */
@Composable
fun IPTVSettingsScreen(onBack: () -> Unit, onSubdlClick: () -> Unit) {
    SettingsHubScreen(onBack = onBack, onSubtitles = onSubdlClick)
}

/** Conta individual: não solicita e não guarda senha do site SubDL. */
@Composable
fun SubdlAccountScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val credentials = remember(context) { SubdlCredentialStore(context.applicationContext) }
    val repo = remember(context) { ExternalSubtitleRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(credentials.isConfigured()) }
    var input by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }
    // Provedor independente: nunca substitui nem reutiliza a chave do SubDL.
    val subsourceCredentials = remember(context) {
        SubsourceCredentialStore(context.applicationContext)
    }
    var subsourceConfigured by remember { mutableStateOf(subsourceCredentials.isConfigured()) }
    var subsourceInput by remember { mutableStateOf("") }
    var subsourceVisible by remember { mutableStateOf(false) }
    var subsourceSaving by remember { mutableStateOf(false) }
    var subsourceMessage by remember { mutableStateOf<String?>(null) }
    var subsourceFocused by remember { mutableStateOf(false) }

    var editingProvider by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            connected = credentials.isConfigured()
            subsourceConfigured = subsourceCredentials.isConfigured()
            kotlinx.coroutines.delay(3_000)
        }
    }
    SettingsLayout(title = "LEGENDAS EXTERNAS", onBack = onBack) {
        Text("Se o seu provedor não oferecer legendas em português, use SubDL e SubSource. Recomendamos cadastrar os dois; a disponibilidade depende do catálogo de cada serviço.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFF2F2F2), fontSize = 14.sp)
        val portalPanel: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0x60111419), panelShape)
                .border(1.dp, settingsGoldBorder, panelShape).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                br.com.flaviotorres.iptvpremium.core.device.DevicePortalQr(destination = "subtitles", compact = true)
                br.com.flaviotorres.iptvpremium.core.device.PortalSyncControls(label = "Sincronizar legendas") {
                    connected = credentials.isConfigured()
                    subsourceConfigured = subsourceCredentials.isConfigured()
                }
            }
        }
        val providerPanel: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0x60111419), panelShape)
                .border(1.dp, settingsGoldBorder, panelShape).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SEUS PROVEDORES", color = gold, fontSize = 17.sp,
                    fontFamily = if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular)androidx.compose.ui.text.font.FontFamily.SansSerif else androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(br.com.flaviotorres.iptvpremium.R.font.cinzel_semibold)))
                SettingsAction("SubDL · " + (if (connected) "Configurado" else "Não configurado"),
                    if (connected) "Editar cadastro no aparelho" else "Cadastrar chave no aparelho",
                    onClick = { editingProvider = if (editingProvider == "subdl") null else "subdl" })
                SettingsAction("SubSource · " + (if (subsourceConfigured) "Configurado" else "Não configurado"),
                    if (subsourceConfigured) "Editar cadastro no aparelho" else "Cadastrar chave no aparelho",
                    onClick = { editingProvider = if (editingProvider == "subsource") null else "subsource" })
                Text("Cadastre pelo celular usando o QR ou selecione um provedor para editar aqui.", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFF2F2F2), fontSize = 14.sp)
            }
        }
        androidx.compose.foundation.layout.BoxWithConstraints {
            if (maxWidth >= 760.dp && br.com.flaviotorres.iptvpremium.BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { portalPanel() }
                    Box(Modifier.weight(1f)) { providerPanel() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (br.com.flaviotorres.iptvpremium.BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) portalPanel()
                    providerPanel()
                }
            }
        }
        if (editingProvider != null) SettingsAction("Fechar edição", "", compact = true, onClick = { editingProvider = null })
        if (editingProvider == "subdl") {
        Text("CADASTRO SUBDL", color = gold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        SettingsAction(
            title = "Criar conta ou gerar chave de API",
            description = "subdl.com → Conta → API → copie a chave",
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://subdl.com"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: ActivityNotFoundException) {
                    message = "Sem navegador disponível. Abra subdl.com no celular ou computador."
                } catch (_: Exception) {
                    message = "Não foi possível abrir o navegador. Acesse subdl.com manualmente."
                }
            }
        )
        Text("Chave de API pessoal", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 15.sp)
        BasicTextField(
            value = input,
            onValueChange = { if (it.length <= 512) input = it },
            singleLine = true,
            enabled = !processing,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            cursorBrush = SolidColor(gold),
            textStyle = TextStyle(color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 17.sp),
            modifier = Modifier.fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .clip(RoundedCornerShape(10.dp))
                .background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0x600B0C10))
                .border(1.5.dp, if (focused) gold else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF).copy(alpha = .35f), RoundedCornerShape(10.dp))
                .padding(horizontal = 15.dp, vertical = 12.dp),
            decorationBox = { textField ->
                Box {
                    if (input.isBlank()) Text(
                        "Cole ou digite sua chave do SubDL",
                        color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFF2F2F2), fontSize = 14.sp
                    )
                    textField()
                }
            }
        )
        SettingsAction(
            title = if (visible) "Ocultar chave" else "Mostrar chave",
            description = "",
            onClick = { visible = !visible }
        )
        SettingsAction(
            title = if (processing) "Verificando…" else "Conectar / testar e salvar",
            description = "Valida a chave no SubDL sem pesquisar filmes",
            enabled = !processing && input.isNotBlank(),
            onClick = {
                if (!processing) {
                    processing = true
                    message = null
                    scope.launch {
                        val result = credentials.connect(input)
                        connected = credentials.isConfigured()
                        message = when (result) {
                            SubdlConnectionResult.CONNECTED -> {
                                withContext(Dispatchers.IO) { repo.clearCaches() }
                                input = ""
                                visible = false
                                "Conectado! As legendas serão procuradas quando você abrir o player."
                            }
                            SubdlConnectionResult.INVALID_FORMAT -> "A chave informada parece incompleta."
                            SubdlConnectionResult.INVALID_KEY -> "Chave recusada pelo SubDL. Confira sua conta."
                            SubdlConnectionResult.LIMITED -> "SubDL temporariamente indisponível por limite de requisições."
                            SubdlConnectionResult.NETWORK_ERROR -> "Falha de conexão. Verifique a internet e tente novamente."
                            SubdlConnectionResult.STORAGE_ERROR -> "Não foi possível proteger a chave neste dispositivo."
                            SubdlConnectionResult.SERVER_ERROR -> "O SubDL não confirmou a chave. Tente novamente mais tarde."
                        }
                        processing = false
                    }
                }
            }
        )
        if (connected) {
            SettingsAction(
                title = "Desconectar SubDL",
                description = "Apagar a chave deste dispositivo e limpar o cache de legendas externas",
                enabled = !processing,
                onClick = {
                    scope.launch {
                        processing = true
                        withContext(Dispatchers.IO) {
                            credentials.disconnect()
                            repo.clearCaches()
                        }
                        connected = false
                        input = ""
                        message = "Conta desconectada deste dispositivo."
                        processing = false
                    }
                }
            )
        }
        message?.let { Text(it, color = gold, fontSize = 14.sp) }


        }
        if (editingProvider == "subsource") {
        // SubSource: cadastrado na mesma tela, sem alterar a chave SubDL.
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(gold.copy(alpha = .38f)))
        Text("CADASTRO SUBSOURCE", color = gold, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        SettingsAction(
            title = "Gerar chave do SubSource",
            description = "subsource.net → Configurações → desça a página e copie a chave",
            onClick = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://subsource.net"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                    subsourceMessage = "Abra subsource.net no celular ou computador."
                }
            }
        )
        Text("Chave de API do SubSource", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 15.sp)
        BasicTextField(
            value = subsourceInput,
            onValueChange = { if (it.length <= 512) subsourceInput = it },
            singleLine = true,
            enabled = !subsourceSaving,
            visualTransformation = if (subsourceVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            cursorBrush = SolidColor(gold),
            textStyle = TextStyle(color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 17.sp),
            modifier = Modifier.fillMaxWidth()
                .onFocusChanged { subsourceFocused = it.isFocused }
                .clip(RoundedCornerShape(10.dp))
                .background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0x600B0C10))
                .border(
                    1.5.dp,
                    if (subsourceFocused) gold else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF).copy(alpha = .35f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
            decorationBox = { field ->
                Box {
                    if (subsourceInput.isBlank()) {
                        Text("Cole sua chave do SubSource", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFF2F2F2), fontSize = 14.sp)
                    }
                    field()
                }
            }
        )
        SettingsAction(
            title = if (subsourceVisible) "Ocultar chave SubSource" else "Mostrar chave SubSource",
            description = "",
            onClick = { subsourceVisible = !subsourceVisible }
        )
        SettingsAction(
            title = if (subsourceSaving) "Salvando…" else "Salvar chave SubSource",
            description = "Salva com criptografia; a conexão será testada ao abrir o player",
            enabled = !subsourceSaving && subsourceInput.isNotBlank(),
            onClick = {
                if (!subsourceSaving) {
                    subsourceSaving = true
                    subsourceMessage = null
                    val typedKey = subsourceInput
                    scope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            subsourceCredentials.saveKey(typedKey)
                        }
                        subsourceConfigured = subsourceCredentials.isConfigured()
                        subsourceMessage = if (saved) {
                            subsourceInput = ""
                            subsourceVisible = false
                            "Chave salva! A validação ocorrerá na pesquisa pelo player."
                        } else {
                            "Não foi possível salvar. Verifique a chave e tente novamente."
                        }
                        subsourceSaving = false
                    }
                }
            }
        )
        if (subsourceConfigured) {
            SettingsAction(
                title = "Remover chave SubSource",
                description = "Não altera o SubDL nem apaga legendas baixadas",
                enabled = !subsourceSaving,
                onClick = {
                    scope.launch {
                        subsourceSaving = true
                        withContext(Dispatchers.IO) { subsourceCredentials.disconnect() }
                        subsourceConfigured = false
                        subsourceInput = ""
                        subsourceVisible = false
                        subsourceMessage = "Chave SubSource removida deste dispositivo."
                        subsourceSaving = false
                    }
                }
            )
        }
        subsourceMessage?.let { Text(it, color = gold, fontSize = 14.sp) }

        }
    }
}

@Composable
private fun SettingsLayout(
    title: String,
    onBack: () -> Unit,
    body: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.background)
            .background(Brush.verticalGradient(listOf(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0x600B0C10))))
            .padding(22.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(.94f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Text(title, color = gold, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        fontFamily = if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular)androidx.compose.ui.text.font.FontFamily.SansSerif else androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(br.com.flaviotorres.iptvpremium.R.font.cinzel_semibold)),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(12.dp))
                SettingsAction("Voltar", "", onClick = onBack, compact = true)
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(gold.copy(alpha = .45f)))
            body()
        }
    }
}

@Composable
private fun SettingsAction(
    title: String,
    description: String,
    enabled: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = (if (compact) Modifier else Modifier.fillMaxWidth())
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor() else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0x60111419))
            .border(if (focused) 1.7.dp else 1.dp, settingsGoldBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 15.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = if (enabled) br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF) else Color.Gray,
            fontSize = if (compact) 13.sp else 16.sp, fontWeight = FontWeight.SemiBold)
        if (description.isNotEmpty() && !compact) Text(description,
            color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFF2F2F2), fontSize = 14.sp)
    }
}

/**
 * Armazena a chave pessoal do SubSource separadamente do SubDL.
 * O repositório de legendas pode acessar esta classe para ler a chave,
 * usando o cabeçalho X-API-Key (nunca parâmetros de URL).
 *
 * Esta classe não realiza buscas nem downloads e não afirma validar a chave.
 */
class SubsourceCredentialStore(context: android.content.Context) {
    private val appContext = context.applicationContext
    private val credentialFile = File(
        appContext.noBackupFilesDir, "iptvpremium_subsource_account_v1.bin"
    )
    private val alias = "iptvpremium_subsource_secret_v1"

    fun isConfigured(): Boolean = readKey() != null

    fun readKey(): String? = runCatching {
        if (!credentialFile.isFile) return@runCatching null
        val pair = DataInputStream(credentialFile.inputStream()).use { input ->
            if (input.readInt() != 1) return@use null
            val iv = ByteArray(12)
            input.readFully(iv)
            val length = input.readInt()
            if (length !in 17..2048) return@use null
            val encrypted = ByteArray(length)
            input.readFully(encrypted)
            iv to encrypted
        } ?: return@runCatching null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, pair.first))
        String(cipher.doFinal(pair.second), Charsets.UTF_8)
            .trim().takeIf { it.isNotEmpty() }
    }.getOrNull()

    /** Salva sem consultar a API e sem consumir quota. Validação virá na busca. */
    fun saveKey(rawKey: String): Boolean {
        val key = rawKey.trim()
        if (key.length !in 8..512 || key.any { it.isWhitespace() || it.isISOControl() }) {
            return false
        }
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
            val temp = File(credentialFile.parentFile, credentialFile.name + ".tmp")
            try {
                DataOutputStream(temp.outputStream()).use { output ->
                    output.writeInt(1)
                    output.write(cipher.iv)
                    output.writeInt(encrypted.size)
                    output.write(encrypted)
                }
                if (!temp.renameTo(credentialFile)) {
                    temp.copyTo(credentialFile, overwrite = true)
                    temp.delete()
                }
            } finally {
                temp.delete()
            }
            true
        }.getOrDefault(false)
    }

    fun disconnect() {
        credentialFile.delete()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }
}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_POLISH_LOADING_HOLD_V1

// IPTV_DEVICE_IDENTITY_V1

// IPTV_SUBTITLE_LAYOUT_PORTAL_LOAD_V1

// IPTV_SETTINGS_APPROVED_GRID_V1
