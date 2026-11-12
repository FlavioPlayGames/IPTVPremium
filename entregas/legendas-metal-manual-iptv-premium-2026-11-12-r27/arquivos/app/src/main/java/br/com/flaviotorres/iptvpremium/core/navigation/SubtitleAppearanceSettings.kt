package br.com.flaviotorres.iptvpremium.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import br.com.flaviotorres.iptvpremium.ui.theme.MetallicPalette
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
                    SubtitleChipRow("Cor da letra",subtitleTextColors.map{it.label},subtitleTextColors.indexOfFirst{it.argb==value.textArgb},first,
                        colors=subtitleTextColors.map{Color(it.argb)}){update(value.copy(textArgb=subtitleTextColors[it].argb))}
                    SubtitleChipRow("Tamanho",subtitleSizeChoices.map{"$it"},subtitleSizeChoices.indexOf(value.sizeSp)){update(value.copy(sizeSp=subtitleSizeChoices[it]))}
                    SubtitleChipRow("Fonte",SubtitleTypeface.entries.map{it.label},value.typeface.ordinal){update(value.copy(typeface=SubtitleTypeface.entries[it]))}
                    SubtitleChipRow("Fundo",SubtitleBackground.entries.map{it.label},value.background.ordinal){update(value.copy(background=SubtitleBackground.entries[it]))}
                    SubtitleChipRow("Acabamento",SubtitleEdge.entries.map{it.label},value.edge.ordinal){update(value.copy(edge=SubtitleEdge.entries[it]))}
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
                controls(Modifier.weight(.58f).fillMaxHeight());preview(Modifier.weight(.42f))
            }else Column(Modifier.fillMaxSize()) {preview(Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp));controls(Modifier.weight(1f))}
        }
    }
}

@Composable
private fun SubtitleChipRow(title:String,options:List<String>,selected:Int,first:FocusRequester?=null,
    colors:List<Color>?=null,onSelect:(Int)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(title,color=MetallicPalette.ink,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
        val columns=if(options.size==4)4 else 3
        options.indices.toList().chunked(columns).forEach{row->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                row.forEach{index->
                    var focused by remember{mutableStateOf(false)}
                    val active=index==selected
                    val shape=RoundedCornerShape(8.dp)
                    Row(Modifier.weight(1f).heightIn(min=42.dp).then(if(index==0&&first!=null)Modifier.focusRequester(first)else Modifier)
                        .onFocusChanged{focused=it.isFocused}.semantics{this.selected=active;role=Role.RadioButton}
                        .background(if(active||focused)MetallicPalette.selected else MetallicPalette.card,shape)
                        .border(if(focused)3.dp else if(active)2.dp else 1.dp,if(active||focused)MetallicPalette.accent else MetallicPalette.secondary.copy(alpha=.4f),shape)
                        .clickable(role=Role.RadioButton,interactionSource=remember{MutableInteractionSource()},indication=null){onSelect(index)}
                        .padding(horizontal=8.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        colors?.getOrNull(index)?.let{Box(Modifier.size(14.dp).background(it,RoundedCornerShape(50)))}
                        Text(options[index],color=MetallicPalette.ink,fontSize=12.sp,fontWeight=if(active)FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
