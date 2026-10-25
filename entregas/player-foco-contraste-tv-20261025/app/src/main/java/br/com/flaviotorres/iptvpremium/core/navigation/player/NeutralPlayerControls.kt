package br.com.flaviotorres.iptvpremium.core.player

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Default false: shared controls used by the approved Channels UI keep their existing appearance. */
internal val LocalNeutralPlayerControls = staticCompositionLocalOf { false }
internal object NeutralPlayerPalette {
    val text = Color(0xFFD0D0D0)
    val focused = Color(0xFFFFFFFF)
    val secondary = Color(0xFFA8A8A8)
    val icon = Color(0xFFBDBDBD)
    val panel = Color(0xB8222222)
    val menu = Color(0xC4222222)
}

@Composable
internal fun NeutralPlayerIconButton(icon:PlayerGlyph,label:String,focusRequester:FocusRequester?,
    prominent:Boolean,compact:Boolean,onClick:()->Unit) {
    var focused by remember { mutableStateOf(false) }
    val interaction=remember { MutableInteractionSource() }
    val width=if(prominent) { if(compact)70.dp else 76.dp } else { if(compact)49.dp else 54.dp }
    val base=if(prominent) { if(compact)30.dp else 34.dp } else { if(compact)23.dp else 26.dp }
    val shortLabel=when(icon) {
        PlayerGlyph.PREVIOUS->"Anterior";PlayerGlyph.NEXT->"Próximo"
        PlayerGlyph.BACK->"Voltar 10s";PlayerGlyph.FORWARD->"Avançar 10s"
        PlayerGlyph.PLAY->"Reproduzir";PlayerGlyph.PAUSE->"Pausar"
        PlayerGlyph.AUDIO->"Áudio";PlayerGlyph.SUBTITLES->"Legendas"
        PlayerGlyph.ASPECT->"Formato";PlayerGlyph.SETTINGS->"Opções"
    }
    Box(Modifier.width(width).height(if(compact)74.dp else 80.dp)
        .then(if(focusRequester!=null)Modifier.focusRequester(focusRequester)else Modifier)
        .onFocusChanged { focused=it.isFocused }
        .semantics { contentDescription=label }
        .clickable(interactionSource=interaction,indication=null,onClick=onClick)) {
        Box(Modifier.align(Alignment.TopCenter).height(if(compact)50.dp else 54.dp).fillMaxWidth(),
            contentAlignment=Alignment.Center) {
            NeutralPlayerGlyph(icon,if(focused)NeutralPlayerPalette.focused else NeutralPlayerPalette.icon,
                Modifier.size(base*if(focused)1.30f else 1f))
        }
        if(focused)Text(shortLabel,color=NeutralPlayerPalette.focused,fontSize=18.sp,
            fontWeight=FontWeight.Medium,maxLines=1,modifier=Modifier.align(Alignment.BottomCenter)
                .padding(bottom=3.dp).wrapContentWidth(unbounded=true))
    }
}

@Composable
internal fun NeutralPlayerMenuItem(label:String,description:String,focusRequester:FocusRequester?,
    glyph:PlayerMenuGlyph?,enabled:Boolean,onClick:()->Unit) {
    var focused by remember { mutableStateOf(false) }
    val interaction=remember { MutableInteractionSource() }
    Row(Modifier.fillMaxWidth()
        .then(if(focusRequester!=null)Modifier.focusRequester(focusRequester)else Modifier)
        .onFocusChanged { focused=it.isFocused }
        .clickable(enabled=enabled,interactionSource=interaction,indication=null,onClick=onClick)
        .padding(horizontal=6.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(if(focused)"•" else " ",color=NeutralPlayerPalette.focused,fontSize=16.sp,modifier=Modifier.width(8.dp))
        if(glyph!=null)Box(Modifier.size(26.dp),contentAlignment=Alignment.Center) {
            PlayerMenuGlyphDrawing(glyph,if(focused)NeutralPlayerPalette.focused else NeutralPlayerPalette.icon,
                Modifier.size(if(focused)21.dp else 17.dp),monochrome=true)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(label,color=if(!enabled)NeutralPlayerPalette.secondary.copy(alpha=.45f)
                else if(focused)NeutralPlayerPalette.focused else NeutralPlayerPalette.text,
                fontSize=14.sp,fontWeight=if(focused)FontWeight.SemiBold else FontWeight.Normal,
                maxLines=2,overflow=TextOverflow.Ellipsis)
            if(description.isNotBlank())Text(description,color=NeutralPlayerPalette.secondary,
                fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
        Text("›",color=NeutralPlayerPalette.secondary,fontSize=21.sp)
    }
}

@Composable
internal fun NeutralPlayerTextButton(label:String,focusRequester:FocusRequester?,onClick:()->Unit) {
    var focused by remember { mutableStateOf(false) }
    val interaction=remember { MutableInteractionSource() }
    Row(Modifier.then(if(focusRequester!=null)Modifier.focusRequester(focusRequester)else Modifier)
        .onFocusChanged { focused=it.isFocused }
        .clickable(interactionSource=interaction,indication=null,onClick=onClick)
        .padding(horizontal=15.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(if(focused)"•" else " ",color=NeutralPlayerPalette.focused,modifier=Modifier.width(12.dp))
        Text(label,color=if(focused)NeutralPlayerPalette.focused else NeutralPlayerPalette.text,
            fontSize=14.sp,fontWeight=if(focused)FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun NeutralPlayerGlyph(icon:PlayerGlyph,color:Color,modifier:Modifier) {
    if(icon!=PlayerGlyph.BACK && icon!=PlayerGlyph.FORWARD) {
        PlayerGlyphDrawing(icon,color,modifier,monochrome=true)
        return
    }
    Box(modifier,contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            scale(scaleX=if(icon==PlayerGlyph.FORWARD)-1f else 1f,scaleY=1f) {
                drawArc(color,startAngle=-90f,sweepAngle=-300f,useCenter=false,
                    topLeft=Offset(size.width*.16f,size.height*.16f),
                    size=Size(size.width*.68f,size.height*.68f),style=Stroke(size.width*.07f))
                val arrow=Path().apply {
                    moveTo(size.width*.35f,size.height*.16f)
                    lineTo(size.width*.59f,size.height*.025f)
                    lineTo(size.width*.59f,size.height*.295f)
                    close()
                }
                drawPath(arrow,color)
            }
        }
        Text("10",color=color,fontSize=10.sp,fontWeight=FontWeight.Medium)
    }
}
