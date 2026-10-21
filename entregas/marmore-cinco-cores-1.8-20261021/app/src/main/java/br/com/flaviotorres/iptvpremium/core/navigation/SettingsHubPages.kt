package br.com.flaviotorres.iptvpremium.ui.settings

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

private val pageGold get() = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0)
@Composable
private fun Detail(value: String) { Text(value, color = Color(0xFFCDCED1), fontSize = 16.sp) }

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
    PortalOutlineButton((if (selected == "media3") "✓  " else "") + "Media3 · Padrão recomendado") { settings.setEngine("media3") }
    PortalOutlineButton((if (selected == "vlc") "✓  " else "") + "VLC · Player alternativo") { settings.setEngine("vlc") }
    val next = settings.prefs.getBoolean("auto_next", true)
    PortalOutlineButton("Próximo episódio automático: " + if (next) "Ativado" else "Desativado") { settings.setAutoNext(!next) }
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
            PortalOutlineButton((if (current == value) "✓  " else "") + title) { settings.setCategoryOrder(type, value) }
        }
    }
    Detail("Nenhuma categoria ou conteúdo será excluído. Ocultar categorias individualmente pode ser acrescentado em uma próxima etapa.")
}

@Composable
internal fun AppearancePreferencesSettings(initialFocus: androidx.compose.ui.focus.FocusRequester) {
    val settings = rememberSettingsPreferences()
    val context = LocalContext.current.applicationContext
    val selectedTheme = br.com.flaviotorres.iptvpremium.ui.theme.ThemeState.selected
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 660.dp
        val controls: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cor do tema", color = pageGold, fontSize = 18.sp)
                br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme.entries.chunked(4).forEach { themes ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        themes.forEach { theme ->
                            val label = when (theme) {
                                br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme.GOLD -> "Dourado"
                                else -> theme.label
                            }
                            AppearanceChoice(label, selectedTheme == theme, Modifier.weight(1f)
                                .then(if (selectedTheme == theme) Modifier.focusRequester(initialFocus) else Modifier),
                                Color.hsv(theme.hue, .65f, 1f)) {
                                br.com.flaviotorres.iptvpremium.ui.theme.ThemeState.choose(context, theme)
                            }
                        }
                        repeat(4 - themes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Fundo da Home e dos catálogos", color = pageGold, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogBackground.entries.forEach { background ->
                        AppearanceChoice(background.label,
                            br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selected == background, Modifier.weight(1f)) {
                            br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.choose(context, background)
                        }
                    }
                }
                Detail("Escolha mármore preto ou branco e combine com uma das cinco cores. O Kids mantém seu próprio visual.")
                Text("Visual do Kids", color = pageGold, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.entries.forEach { style ->
                        AppearanceChoice("Kids · " + style.label, settings.kidsStyle() == style.key, Modifier.weight(1f), style.accent) {
                            settings.setKidsStyle(style.key)
                        }
                    }
                }
                Text("Tamanho dos textos", color = pageGold, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1f to "Padrão", 1.1f to "Maior", 1.2f to "Extra grande").forEach { (scale, title) ->
                        AppearanceChoice(title, settings.textScale() == scale, Modifier.weight(1f)) {
                            settings.setTextScale(scale)
                        }
                    }
                }
                Text("Capas por fileira", color = pageGold, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4 to "4 por fileira · 8 capas", 5 to "5 por fileira · 10 capas").forEach { (columns, title) ->
                        AppearanceChoice(title, settings.catalogColumns() == columns, Modifier.weight(1f)) {
                            settings.setCatalogColumns(columns)
                        }
                    }
                }
            }
        }
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Box(Modifier.weight(.58f)) { controls() }
                Column(Modifier.weight(.42f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Prévia · " + selectedTheme.label, color = pageGold, fontSize = 18.sp)
                    AppearanceThemePreview(compact = true)
                    Text("Kids · " + br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.fromKey(settings.kidsStyle()).label, color = pageGold, fontSize = 16.sp)
                    br.com.flaviotorres.iptvpremium.ui.kids.KidsHomeArtwork(
                        br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.fromKey(settings.kidsStyle()),
                        Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)), labels=true)

                    Text("Grade · ${settings.catalogColumns()} por fileira · ${settings.catalogColumns() * 2} capas",
                        color = pageGold, fontSize = 16.sp)
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogLayoutPreview(settings.catalogColumns())
                    Text("Aplicado automaticamente", color = Color(0xFFB7BBC1), fontSize = 13.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                controls()
                AppearanceThemePreview()
                br.com.flaviotorres.iptvpremium.ui.kids.KidsHomeArtwork(
                    br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.fromKey(settings.kidsStyle()),
                    Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)), labels=true)

                Text("Grade · ${settings.catalogColumns()} por fileira · ${settings.catalogColumns() * 2} capas",
                    color = pageGold, fontSize = 16.sp)
                br.com.flaviotorres.iptvpremium.ui.components.CatalogLayoutPreview(settings.catalogColumns())
            }
        }
    }
}

@Composable
private fun AppearanceChoice(label: String, selected: Boolean, modifier: Modifier = Modifier,
    swatch: Color? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(modifier.heightIn(min = 48.dp)
        .semantics { this.selected = selected; role = androidx.compose.ui.semantics.Role.Button }
        .onFocusChanged { focused = it.isFocused }
        .clip(shape)
        .background(if (focused || selected) br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color() else br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF111519))
        .border(if (focused || selected) 2.dp else 1.dp, br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.metallicBorder, shape)
        .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, androidx.compose.ui.Alignment.CenterHorizontally)) {
        if (swatch != null) Box(Modifier.size(11.dp).background(swatch, androidx.compose.foundation.shape.CircleShape))
        Text((if (selected) "✓ " else "") + label, color = if (selected || focused) Color.White else Color.White,
            fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AppearanceThemePreview(compact: Boolean = false) {
    br.com.flaviotorres.iptvpremium.ui.home.MarbleHomePreview(
        Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)))
}

@Composable
internal fun UpdateSettings() {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Text("Versão instalada: ${BuildConfig.VERSION_NAME}", color = pageGold, fontSize = 21.sp)
    Detail("Número interno: ${BuildConfig.VERSION_CODE}")
    Detail("O aplicativo também verifica atualizações ao abrir ou voltar para ele.")
    PortalOutlineButton(if (busy) "Verificando…" else "Verificar atualização", enabled = !busy && BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
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
        PortalOutlineButton("Limpar histórico de $title", enabled = !busy) { confirm = type; message = null }
    }
    if (confirm != null) {
        Text("Confirma a exclusão do histórico de ${if (confirm == PlaybackContentType.MOVIE) "filmes" else "séries"}?", color = pageGold, fontSize = 18.sp)
        PortalOutlineButton(if (busy) "Apagando…" else "Confirmar exclusão", enabled = !busy) {
            val type = confirm ?: return@PortalOutlineButton
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { br.com.flaviotorres.iptvpremium.data.history.WatchHistoryStore(context).clearType(type) }
                    confirm = null; message = "Histórico apagado. Seus favoritos e listas foram preservados."
                } catch (_: Exception) { message = "Não foi possível concluir a limpeza. Tente novamente." }
                finally { busy = false }
            }
        }
        PortalOutlineButton("Cancelar", enabled = !busy) { confirm = null }
    }
    message?.let { Detail(it) }
}
