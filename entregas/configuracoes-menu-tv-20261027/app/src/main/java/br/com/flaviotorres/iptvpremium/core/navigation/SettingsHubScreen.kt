package br.com.flaviotorres.iptvpremium.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.BuildConfig
import br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle
import br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass
import br.com.flaviotorres.iptvpremium.ui.components.CatalogThemeBackdrop
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val hubGold get() = CatalogStyle.text
private val hubFrame get() = CatalogStyle.selectionBorder
private data class HubEntry(val title: String, val description: String, val path: String)
private val hubEntries = listOf(
    HubEntry("Meu aparelho", "Identificação e acesso", "M6 8H42V32H6Z M24 32V40 M15 40H33"),
    HubEntry("Minha assinatura", "Validade e renovação", "M8 10H40V41H8Z M8 20H40 M16 5V15 M32 5V15 M16 27H20 M28 27H32 M16 34H20 M28 34H32"),
    HubEntry("Reprodução", "Player e comportamento", "M43 24A19 19 0 1 1 5 24A19 19 0 1 1 43 24 M20 15L33 24L20 33Z"),
    HubEntry("Legendas", "Provedores e personalização", "M5 9H43V38H5Z M12 23H22 M27 23H36 M12 30H32"),
    HubEntry("Categorias", "Organize o que aparece", "M6 6H20V20H6Z M28 6H42V20H28Z M6 28H20V42H6Z M28 28H42V42H28Z"),
    HubEntry("Aparência", "Personalize a interface", "M24 5C12 5 5 13 5 24C5 35 14 43 24 43C31 43 25 34 32 32C41 31 43 28 43 21C43 12 34 5 24 5Z M14 17H16 M24 12H26 M33 18H35 M12 27H14"),
    HubEntry("Histórico", "Gerencie seus registros", "M7 17A19 19 0 1 1 7 32 M3 9V20H14 M24 13V25L32 30"),
    HubEntry("Atualizações", "Verifique novas versões", "M24 5V33 M13 22L24 33L35 22 M7 42H41"),
    HubEntry("Controle parental", "PIN, categorias e canais adultos", "M10 21H38V43H10Z M15 21V14A9 9 0 0 1 33 14V21 M24 29V35")
)

@Composable
internal fun SettingsHubScreen(onBack: () -> Unit, onSubtitles: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(-1) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val back = { if (page >= 0) page = -1 else onBack() }
    BackHandler(onBack = back)
    if (page == 8) {
        ParentalPreferencesSettings(onBack = back)
        return
    }
    if(page==5) {
        AppearanceSettingsScreen(back)
        return
    }
    if (page >= 0) {
        val appearanceFocus = remember(page) { FocusRequester() }
        HubPane(hubEntries[page].title.uppercase(), back, if (page == 5) appearanceFocus else null) { HubPageContent(page, appearanceFocus) }
        return
    }
    val requesters = remember { List(9) { FocusRequester() } }
    val backFocus = remember { FocusRequester() }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    LaunchedEffect(page) {
        gridState.scrollToItem(selected)
        withFrameNanos { }
        requesters[selected].requestFocus()
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.background)) {
        br.com.flaviotorres.iptvpremium.ui.components.SettingsBackdrop(Modifier.matchParentSize())
        val availableWidth = maxWidth - 56.dp
        val availableHeight=maxHeight - 36.dp
        val fixedGridFocus = availableWidth >= 700.dp && availableHeight >= 430.dp
        val columns = if (availableWidth >= 700.dp) 3 else if (availableWidth >= 390.dp) 2 else 1
        val rows = (9 + columns - 1) / columns
        val cardHeight = ((availableHeight - 116.dp - 12.dp * (rows - 1)) / rows).coerceIn(100.dp, 174.dp)
        Column(Modifier.fillMaxSize().padding(horizontal=28.dp,vertical=18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CONFIGURAÇÕES", color = hubGold, fontSize = if (availableWidth >= 700.dp) 28.sp else 22.sp,
                        fontFamily = if(br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active)FontFamily(Font(R.font.cinzel_semibold))else FontFamily.SansSerif)
                    Text("Seu aplicativo, suas preferências.", color = CatalogStyle.ink(0xFFC7C7CC), fontSize = 15.sp)
                }
                if (availableWidth >= 850.dp) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogWordmark(false)
                    Spacer(Modifier.width(24.dp))
                }
                MarbleSettingsButton("‹  Voltar", modifier = Modifier.width(104.dp).focusRequester(backFocus)
                    .focusProperties { down = requesters[selected] }, onClick = back)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.edge))
            LazyVerticalGrid(columns = GridCells.Fixed(columns), state = gridState, modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(hubEntries, key = { _, item -> item.title }) { index, item ->
                    var focused by remember { mutableStateOf(false) }
                    val shape = RoundedCornerShape(14.dp)
                    Row(Modifier.fillMaxWidth().heightIn(min = cardHeight).focusRequester(requesters[index])
                        .focusProperties {
                            // A grade rolavel utiliza a busca nativa de foco dos lazy layouts.
                            if (fixedGridFocus) {
                            left = if (index % columns == 0) FocusRequester.Cancel else requesters[index - 1]
                            right = if (index % columns == columns - 1 || index == 8) FocusRequester.Cancel else requesters[index + 1]
                            up = if (index < columns) backFocus else requesters[index - columns]
                            down = if (index + columns > 8) FocusRequester.Cancel else requesters[index + columns]
                            }
                        }
                        .onFocusChanged { focused = it.isFocused; if (it.isFocused) selected = index }
                        
                        .clip(shape)
                        .background(androidx.compose.ui.graphics.SolidColor(if(focused)br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.selection else br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.panel))
                        .border(if(focused)1.5.dp else .7.dp,br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.border(focused),shape)
                        .clickable(role=Role.Button,interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null) {
                            selected = index
                            when (index) { 3 -> onSubtitles(); else -> page = index }
                        }.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val path = remember(item.path) { PathParser().parsePathString(item.path).toPath() }
                        Canvas(Modifier.size(40.dp)) { scale(size.width / 48f, size.height / 48f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                            drawPath(path, androidx.compose.ui.graphics.SolidColor(CatalogStyle.accent), style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        } }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(item.title, color = CatalogStyle.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.description, color = CatalogStyle.ink(0xFFBCBDC2), fontSize = 13.sp)
                        }
                    }
                }
            }
            Text("Use as setas para navegar e OK para selecionar.", color = CatalogStyle.ink(0xFFA7A9AE), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun HubPane(title: String, onBack: () -> Unit, initialFocus: FocusRequester? = null, body: @Composable ColumnScope.() -> Unit) {
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(title) { withFrameNanos { }; (initialFocus ?: backFocus).requestFocus() }
    Box(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.background)) {
        br.com.flaviotorres.iptvpremium.ui.components.SettingsBackdrop(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (title == "CONTROLE PARENTAL") 8.dp else 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = hubGold, fontSize = 24.sp, fontFamily = if(br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active)FontFamily(Font(R.font.cinzel_semibold))else FontFamily.SansSerif, modifier = Modifier.weight(1f))
                MarbleSettingsButton("‹  Voltar", modifier = Modifier.width(104.dp).focusRequester(backFocus), onClick = onBack)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.edge))
            body()
        }
    }
}

@Composable
private fun HubPageContent(page: Int, appearanceFocus: FocusRequester) {
    when (page) {
        0 -> {
            br.com.flaviotorres.iptvpremium.core.device.DeviceIdentityCard()
            Text("IPTV Premium · Versão ${BuildConfig.VERSION_NAME}", color = CatalogStyle.text, fontSize = 16.sp)
            Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}", color = CatalogStyle.ink(0xFFBCBDC2), fontSize = 14.sp)
        }
        1 -> SubscriptionSettings()
        2 -> PlaybackPreferencesSettings()
        4 -> CategoryPreferencesSettings()
        5 -> AppearancePreferencesSettings(appearanceFocus)
        6 -> HistorySettings()
        7 -> UpdateSettings()

    }
}
