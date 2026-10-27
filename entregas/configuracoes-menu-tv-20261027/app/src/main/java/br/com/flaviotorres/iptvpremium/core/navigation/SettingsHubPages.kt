package br.com.flaviotorres.iptvpremium.ui.settings

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.BuildConfig
import br.com.flaviotorres.iptvpremium.core.device.*
import br.com.flaviotorres.iptvpremium.core.update.AppUpdateManager
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val pageGold get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text
@Composable
private fun Detail(value: String) { Text(value, color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFCDCED1), fontSize = 16.sp) }

@Composable
internal fun SubscriptionSettings() {
    val context = LocalContext.current.applicationContext
    val state by produceState<org.json.JSONObject?>(null, context) {
        value = withContext(Dispatchers.IO) { runCatching { PortalIdentityStore(context).load() }.getOrNull() }
    }
    Detail("A assinatura pertence a este aparelho. A renovação é confirmada pelo administrador após o pagamento.")
    val data = state
    if (data == null || !data.has("licenseState")) Detail("Os dados de validade ainda não foram recebidos. Abra o aplicativo com conexão à central.")
    else {
        val label = when (data.optString("licenseState")) { "trial" -> "Período de teste"; "licensed" -> "Assinatura ativa"; "blocked" -> "Bloqueado pelo administrador"; else -> "Acesso vencido" }
        Text(label, color = pageGold, fontSize = 22.sp)
        if (data.optLong("validUntil") > 0L) {
            val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, java.util.Locale.forLanguageTag("pt-BR"))
            Detail("Válido até ${date.format(java.util.Date(data.optLong("validUntil")))}")
        }
    }
    if (BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) DevicePortalQr(destination = "payment", compact = true)
}

@Composable
internal fun PlaybackPreferencesSettings() {
    val settings = rememberSettingsPreferences()
    val selected = settings.prefs.getString("engine", "media3")
    Detail("Escolha o player preferido para as próximas reproduções. Você continua podendo trocar durante o vídeo.")
    br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton((if (selected == "media3") "✓  " else "") + "Media3 · Padrão recomendado") { settings.setEngine("media3") }
    br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton((if (selected == "vlc") "✓  " else "") + "VLC · Player alternativo") { settings.setEngine("vlc") }
    val next = settings.prefs.getBoolean("auto_next", true)
    br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Próximo episódio automático: " + if (next) "Ativado" else "Desativado") { settings.setAutoNext(!next) }
    Detail("Os controles desaparecem após 5 segundos sem interação. HDR e Dolby Vision são selecionados automaticamente conforme a fonte e o aparelho.")
}

@Composable
internal fun CategoryPreferencesSettings() {
    val settings = rememberSettingsPreferences()
    Detail("Escolha a ordem das categorias. A alteração será aplicada quando você abrir novamente a seção correspondente.")
    listOf("LIVE_TV" to "Canais", "MOVIES" to "Filmes", "SERIES" to "Séries").forEach { (type, label) ->
        Text(label, color = pageGold, fontSize = 19.sp)
        val current = settings.categoryOrder(type)
        listOf("default" to "Padrão do aplicativo", "az" to "Nome: A–Z", "za" to "Nome: Z–A").forEach { (value, title) ->
            br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton((if (current == value) "✓  " else "") + title) { settings.setCategoryOrder(type, value) }
        }
    }
    Detail("Nenhuma categoria ou conteúdo será excluído. Ocultar categorias individualmente pode ser acrescentado em uma próxima etapa.")
}

@Composable
internal fun AppearanceSettingsScreen(onBack:()->Unit) {
    val initial=remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { androidx.compose.runtime.withFrameNanos {}; initial.requestFocus() }
    Box(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.background)) {
        br.com.flaviotorres.iptvpremium.ui.components.SettingsBackdrop(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text("APARÊNCIA",color=pageGold,fontSize=26.sp,modifier=Modifier.weight(1f))
                br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("‹ Voltar",modifier=Modifier.width(108.dp),onClick=onBack)
            }
            AppearancePreferencesSettings(initial,Modifier.weight(1f))
        }
    }
}

@Composable
internal fun AppearancePreferencesSettings(initialFocus: androidx.compose.ui.focus.FocusRequester,modifier:Modifier=Modifier) {
    val settings=rememberSettingsPreferences()
    val context=LocalContext.current.applicationContext
    val selectedTheme=br.com.flaviotorres.iptvpremium.ui.theme.ThemeState.selected
    val controls:@Composable ()->Unit={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Tema",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme.entries.forEach { theme ->
                AppearanceChoice(theme.label,selectedTheme==theme,
                    Modifier.fillMaxWidth().then(if(selectedTheme==theme)Modifier.focusRequester(initialFocus)else Modifier),
                    br.com.flaviotorres.iptvpremium.ui.theme.homeThemeAccent(theme)) {
                    br.com.flaviotorres.iptvpremium.ui.theme.ThemeState.choose(context,theme)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Modo de exibição",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.components.CatalogBackground.entries.forEach { background ->
                AppearanceChoice(background.label,br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selected==background,Modifier.fillMaxWidth()) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.choose(context,background)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Visual do Kids",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.entries.forEach { style ->
                AppearanceChoice(style.label,settings.kidsStyle()==style.key,Modifier.fillMaxWidth(),style.accent) {settings.setKidsStyle(style.key)}
            }
            Text("Tamanho dos textos",color=pageGold,fontSize=18.sp)
            listOf(1f to "Padrão",1.1f to "Maior",1.2f to "Extra grande").forEach { (scale,title) ->
                AppearanceChoice(title,settings.textScale()==scale,Modifier.fillMaxWidth()){settings.setTextScale(scale)}
            }
            Text("Capas por fileira",color=pageGold,fontSize=18.sp)
            listOf(4,5).forEach { columns ->
                AppearanceChoice("$columns por fileira · ${columns*2} capas",settings.catalogColumns()==columns,Modifier.fillMaxWidth()){settings.setCatalogColumns(columns)}
            }
        }
    }
    val previews:@Composable ()->Unit={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Prévia da Home",color=pageGold,fontSize=20.sp)
            br.com.flaviotorres.iptvpremium.ui.home.MarbleHomePreview(Modifier.fillMaxWidth().aspectRatio(16f/9f)
                .clip(RoundedCornerShape(12.dp)).border(1.dp,br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.edge,RoundedCornerShape(12.dp)))
            Detail("As alterações são salvas automaticamente.")
            Text("Kids",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.kids.KidsHomeArtwork(
                br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.fromKey(settings.kidsStyle()),
                Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)),labels=true)
            Text("Grade · ${settings.catalogColumns()} por fileira",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.components.CatalogLayoutPreview(settings.catalogColumns())
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if(maxWidth>=660.dp) Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
            Box(Modifier.weight(.30f).fillMaxHeight().verticalScroll(androidx.compose.foundation.rememberScrollState())){controls()}
            Box(Modifier.weight(.70f).fillMaxHeight().verticalScroll(androidx.compose.foundation.rememberScrollState())){previews()}
        } else Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            controls();previews()
        }
    }
}

@Composable
private fun AppearanceChoice(label:String,selected:Boolean,modifier:Modifier=Modifier,swatch:Color?=null,onClick:()->Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape=RoundedCornerShape(10.dp)
    Row(modifier.heightIn(min=46.dp).semantics { this.selected=selected;role=androidx.compose.ui.semantics.Role.Button }
        .onFocusChanged { focused=it.isFocused }.clip(shape)
        .background(if(focused||selected)br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.selection else br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.control)
        .border(if(focused)2.dp else 1.dp,br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.border(focused||selected),shape)
        .clickable(interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        if(swatch!=null)Box(Modifier.size(22.dp).background(swatch,androidx.compose.foundation.shape.CircleShape))
        Text(label,color=pageGold,fontSize=15.sp,modifier=Modifier.weight(1f))
        if(selected)Text("✓",color=pageGold,fontSize=15.sp)
    }
}

@Composable
internal fun UpdateSettings() {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Text("Versão instalada: ${BuildConfig.VERSION_NAME}", color = pageGold, fontSize = 21.sp)
    Detail("Número interno: ${BuildConfig.VERSION_CODE}")
    Detail("O aplicativo também verifica atualizações ao abrir ou voltar para ele.")
    br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton(if (busy) "Verificando…" else "Verificar atualização", enabled = !busy && BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
        scope.launch {
            busy = true; message = null
            try {
                AppUpdateManager.refresh()
                message = if (AppUpdateManager.state.value.policy == null) "Seu aplicativo está atualizado conforme a versão publicada na central." else "Atualização disponível. Siga as instruções de instalação."
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Não foi possível consultar a central. Confira sua conexão e tente novamente." }
            finally { busy = false }
        }
    }
    if (BuildConfig.DEVICE_PORTAL_URL.isBlank()) Detail("A central de atualização ainda não foi configurada nesta instalação.")
    message?.let { Detail(it) }
}

@Composable
internal fun HistorySettings() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<PlaybackContentType?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Detail("Apague o histórico de filmes ou séries deste aparelho, em todas as listas. Favoritos, listas e dados de acesso serão preservados.")
    Detail("Essa ação também remove as posições salvas em Continuar assistindo.")
    listOf(PlaybackContentType.MOVIE to "filmes", PlaybackContentType.EPISODE to "séries").forEach { (type, title) ->
        br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Limpar histórico de $title", enabled = !busy) { confirm = type; message = null }
    }
    if (confirm != null) {
        Text("Confirma a exclusão do histórico de ${if (confirm == PlaybackContentType.MOVIE) "filmes" else "séries"}?", color = pageGold, fontSize = 18.sp)
        br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton(if (busy) "Apagando…" else "Confirmar exclusão", enabled = !busy) {
            val type = confirm ?: return@MarbleSettingsButton
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { br.com.flaviotorres.iptvpremium.data.history.WatchHistoryStore(context).clearType(type) }
                    confirm = null; message = "Histórico apagado. Seus favoritos e listas foram preservados."
                } catch (_: Exception) { message = "Não foi possível concluir a limpeza. Tente novamente." }
                finally { busy = false }
            }
        }
        br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Cancelar", enabled = !busy) { confirm = null }
    }
    message?.let { Detail(it) }
}
