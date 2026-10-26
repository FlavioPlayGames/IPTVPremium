package br.com.flaviotorres.iptvpremium.ui.kids

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.components.HomeMenuCard
import br.com.flaviotorres.iptvpremium.ui.theme.*

@Composable
internal fun KidsLogo(modifier: Modifier = Modifier) {
    Image(kidsHomePainter(), "Kids", modifier)
}

@Composable
internal fun KidsScreen(onChannels:()->Unit,onMovies:()->Unit,onSeries:()->Unit,onBack:()->Unit,onSettings:()->Unit) {
    val access by br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.accessFlow.collectAsState(initial=br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access())
    val blocked=if(access.enabled)access.rules.kidsBlockedSections else emptySet()
    val first=remember { FocusRequester() }
    val settingsFocus=remember { FocusRequester() }
    var restoreSettings by rememberSaveable {mutableStateOf(false)}
    val settings=br.com.flaviotorres.iptvpremium.ui.settings.rememberSettingsPreferences()
    val style=KidsStyle.fromKey(settings.kidsStyle())
    BackHandler(onBack=onBack)
    LaunchedEffect(Unit) { withFrameNanos { };if(restoreSettings)settingsFocus.requestFocus()else first.requestFocus();restoreSettings=false }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF020513))
        .onPreviewKeyEvent {if(it.key==Key.Escape){if(it.type==KeyEventType.KeyUp)onBack();true}else false},contentAlignment=Alignment.Center) {
        val width=minOf(maxWidth,maxHeight*16f/9f)
        val height=width*9f/16f
        Box(Modifier.width(width).height(height)) {
            KidsHomeArtwork(style,Modifier.fillMaxSize())
            KidsSettingsButton({restoreSettings=true;onSettings()},Modifier.align(Alignment.TopEnd)
                .padding(top=height*.065f,end=width*.07f).focusRequester(settingsFocus)
                .focusProperties {down=first})
            val actions=listOf(onChannels,onMovies,onSeries)
            KidsHomeMenu(style,width,height) {index,cell ->
                val allowed=listOf("LIVE_TV","MOVIES","SERIES")[index] !in blocked
                if(!allowed) return@KidsHomeMenu
                var focused by remember {mutableStateOf(false)}
                KidsHomeCell(style,index,focused,height,cell
                    .then(if(index==0)Modifier.focusRequester(first)else Modifier)
                    .focusProperties {canFocus=true;up=settingsFocus}
                    .onFocusChanged {focused=it.isFocused}
                    .clickable(interactionSource=remember {androidx.compose.foundation.interaction.MutableInteractionSource()},
                        indication=null,role=androidx.compose.ui.semantics.Role.Button,onClick=actions[index]).focusable())
            }
        }
    }
}

@Composable
internal fun KidsRouteAllowed(onBack:()->Unit):Boolean {
    val access by br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.accessFlow.collectAsState(
        initial=br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access())
    if(!access.enabled || !access.rules.kidsOnly || access.kidsCatalog) return true
    BackHandler(onBack=onBack)
    Column(Modifier.fillMaxSize().background(ThemeColors.surface(0xFF080A0C)).padding(32.dp),
        verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        Text("Acesso disponível apenas no Kids",color=Color.White,fontSize=22.sp)
        Spacer(Modifier.height(16.dp))
        br.com.flaviotorres.iptvpremium.core.device.PortalOutlineButton("Voltar",onClick=onBack)
    }
    return false
}
