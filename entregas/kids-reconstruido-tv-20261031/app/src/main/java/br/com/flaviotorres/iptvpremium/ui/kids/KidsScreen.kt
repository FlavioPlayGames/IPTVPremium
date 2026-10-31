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
internal fun KidsScreen(onChannels:()->Unit,onMovies:()->Unit,onSeries:()->Unit,onBack:()->Unit,onSettings:()->Unit) {
    val access by br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.accessFlow.collectAsState(initial=br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access())
    val blocked=if(access.enabled)access.rules.kidsBlockedSections else emptySet()
    val sections=listOf("LIVE_TV","MOVIES","SERIES")
    val allowed=sections.indices.filter {sections[it] !in blocked}
    val requesters=remember {List(3){FocusRequester()}}
    val settingsFocus=remember{FocusRequester()}
    var selected by rememberSaveable{mutableIntStateOf(0)}
    var restoreSettings by rememberSaveable{mutableStateOf(false)}
    val preferences=br.com.flaviotorres.iptvpremium.ui.settings.rememberSettingsPreferences()
    val style=KidsStyle.fromKey(preferences.kidsStyle())
    fun current()=selected.takeIf {it in allowed} ?: allowed.firstOrNull()
    BackHandler(onBack=onBack)
    LaunchedEffect(allowed) {
        withFrameNanos{}
        if(restoreSettings || allowed.isEmpty())settingsFocus.requestFocus()
        else requesters[current()!!].requestFocus()
        restoreSettings=false
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFF7FBFF))
        .onPreviewKeyEvent {if(it.key==Key.Escape){if(it.type==KeyEventType.KeyUp)onBack();true}else false}) {
        val width=maxWidth;val height=maxHeight
        KidsCinemaHomeArtwork(style,Modifier.fillMaxSize())
        KidsSettingsButton({restoreSettings=true;onSettings()},Modifier.align(Alignment.TopEnd)
            .padding(top=height*.055f,end=width*.045f).focusRequester(settingsFocus)
            .onPreviewKeyEvent {event ->
                if(event.key==Key.DirectionDown) {
                    if(event.type==KeyEventType.KeyDown)current()?.let{requesters[it].requestFocus()}
                    true
                }else false
            })
        val actions=listOf(onChannels,onMovies,onSeries)
        KidsHomeMenu(style,width,height) {index,cell ->
            if(index in allowed) {
                var focused by remember{mutableStateOf(false)}
                KidsHomeCell(style,index,focused,height,cell.focusRequester(requesters[index])
                    .onFocusChanged {focused=it.isFocused;if(it.isFocused)selected=index}
                    .onPreviewKeyEvent {event ->
                        when(event.key) {
                            Key.DirectionLeft,Key.DirectionRight -> {
                                if(event.type==KeyEventType.KeyDown) {
                                    val step=if(event.key==Key.DirectionLeft)-1 else 1
                                    kidsHomeStep(allowed,index,step)?.let{requesters[it].requestFocus()}
                                };true
                            }
                            Key.DirectionUp -> {if(event.type==KeyEventType.KeyDown)settingsFocus.requestFocus();true}
                            Key.DirectionDown -> true
                            else -> false
                        }
                    }
                    .clickable(interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},
                        indication=null,role=androidx.compose.ui.semantics.Role.Button,onClick={selected=index;actions[index]()}).focusable())
            }else Spacer(cell)
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
