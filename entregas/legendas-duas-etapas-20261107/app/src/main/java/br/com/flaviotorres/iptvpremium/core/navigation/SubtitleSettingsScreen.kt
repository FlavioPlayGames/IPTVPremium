package br.com.flaviotorres.iptvpremium.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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

/** Local keyboard fallback; the wizard keeps official signup separate from TV pairing. */
@Composable
internal fun SubtitleCredentialsEditor(provider:String,onBack:()->Unit) {
    BackHandler(onBack=onBack)
    val context=LocalContext.current
    val credentials=remember(context){SubdlCredentialStore(context.applicationContext)}
    val scope=rememberCoroutineScope()
    var connected by remember{mutableStateOf(credentials.isConfigured())}
    var input by remember{mutableStateOf("")}
    var visible by remember{mutableStateOf(false)}
    var processing by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf<String?>(null)}
    var focused by remember{mutableStateOf(false)}
    SettingsLayout(title=if(provider=="subdl")"CONTA SUBDL" else "CONTA OPENSUBTITLES",onBack=onBack) {
        if (provider == "subdl") {
        Text("CADASTRO SUBDL", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
                                br.com.flaviotorres.iptvpremium.core.player.SubtitleProviderUpdates.changed()
                                input = ""
                                visible = false
                                "Conectado! As legendas serão procuradas ao abrir o menu Legendas no player."
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
                description = "Remover a chave deste dispositivo, mantendo sua conta OpenSubtitles e os arquivos já baixados",
                enabled = !processing,
                onClick = {
                    scope.launch {
                        processing = true
                        withContext(Dispatchers.IO) {
                            credentials.disconnect()
                            br.com.flaviotorres.iptvpremium.core.player.SubtitleProviderUpdates.changed()
                        }
                        connected = false
                        input = ""
                        message = "Conta desconectada deste dispositivo."
                        processing = false
                    }
                }
            )
        }
        message?.let { Text(it, color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontSize = 14.sp) }


        }
        if(provider=="opensubtitles") {
            OpenSubtitlesAccountPanel(onChanged={})
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
                    Text(title, color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text, fontWeight = FontWeight.Bold, fontSize = 22.sp,
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
internal fun SettingsAction(
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

