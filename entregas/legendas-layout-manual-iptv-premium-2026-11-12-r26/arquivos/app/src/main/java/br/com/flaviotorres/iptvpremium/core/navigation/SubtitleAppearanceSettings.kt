package br.com.flaviotorres.iptvpremium.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.core.player.*
import br.com.flaviotorres.iptvpremium.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Shares the existing player's preference store; no new subtitle style or reset on entry. */
@Composable
internal fun SubtitleAppearanceSettingsScreen(onBack:()->Unit) {
    BackHandler(onBack=onBack)
    val context=LocalContext.current.applicationContext
    val store=remember(context){SubtitleAppearanceStore(context)}
    var appearance by remember{mutableStateOf<SubtitleAppearance?>(null)}
    val scope=rememberCoroutineScope()
    val first=remember{FocusRequester()}
    val back=remember{FocusRequester()}
    var loaded by remember{mutableStateOf(false)}
    DisposableEffect(store) {
        val observer=store.observe {scope.launch {appearance=withContext(Dispatchers.IO){store.load()}}}
        onDispose{observer.close()}
    }
    LaunchedEffect(store){appearance=withContext(Dispatchers.IO){store.load()};loaded=true}
    LaunchedEffect(loaded){if(loaded){withFrameNanos{};first.requestFocus()}}
    fun update(value:SubtitleAppearance){appearance=value;store.save(value)}
    SubtitleSettingsFrame("APARÊNCIA DAS LEGENDAS","As alterações são salvas automaticamente.",onBack,back) {
        val value=appearance
        if(value==null){Text("Carregando preferências…",color=SettingsLook.secondary);return@SubtitleSettingsFrame}
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val controls:@Composable (Modifier)->Unit={modifier ->
                Column(modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    SettingsHeading("Cor da letra")
                    subtitleTextColors.forEachIndexed {index,color ->
                        SettingsRadio(color.label,value.textArgb==color.argb,Modifier.fillMaxWidth()
                            .then(if(index==0)Modifier.focusRequester(first)else Modifier)){update(value.copy(textArgb=color.argb))}
                    }
                    SettingsHeading("Tamanho")
                    subtitleSizeChoices.forEach {size->SettingsRadio("$size",value.sizeSp==size){update(value.copy(sizeSp=size))}}
                    SettingsHeading("Fonte")
                    SubtitleTypeface.entries.forEach {font->SettingsRadio(font.label,value.typeface==font){update(value.copy(typeface=font))}}
                    SettingsHeading("Fundo")
                    SubtitleBackground.entries.forEach {background->SettingsRadio(background.label,value.background==background){update(value.copy(background=background))}}
                    SettingsHeading("Acabamento")
                    SubtitleEdge.entries.forEach {edge->SettingsRadio(edge.label,value.edge==edge){update(value.copy(edge=edge))}}
                }
            }
            val preview:@Composable (Modifier)->Unit={modifier->
                Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    SettingsHeading("Prévia da legenda")
                    Box(Modifier.fillMaxWidth().height(200.dp).background(Color(0xFF17191B)),contentAlignment=Alignment.BottomCenter) {
                        StyledSubtitleText("Esta é a sua legenda.",value,Modifier.fillMaxWidth().height(100.dp))
                    }
                }
            }
            if(maxWidth>=660.dp)Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                controls(Modifier.weight(.45f).fillMaxHeight());preview(Modifier.weight(.55f))
            }else Column(Modifier.fillMaxSize()) {preview(Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));controls(Modifier.weight(1f))}
        }
    }
}
