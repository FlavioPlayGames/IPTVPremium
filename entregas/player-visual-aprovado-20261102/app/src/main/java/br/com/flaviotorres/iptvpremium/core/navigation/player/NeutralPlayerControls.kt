package br.com.flaviotorres.iptvpremium.core.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
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
    val menu = Color(0xC9111822)
}

@Composable
internal fun NeutralPlayerIconButton(icon:PlayerGlyph,label:String,focusRequester:FocusRequester?,
    prominent:Boolean,compact:Boolean,onClick:()->Unit) {
    var focused by remember { mutableStateOf(false) }
    val interaction=remember { MutableInteractionSource() }
    val width=if(compact)68.dp else 82.dp
    val base=if(prominent) { if(compact)24.dp else 27.dp } else { if(compact)20.dp else 23.dp }
    val shortLabel=when(icon) {
        PlayerGlyph.PREVIOUS->"Ep. anterior";PlayerGlyph.NEXT->"Próximo ep."
        PlayerGlyph.BACK->"Voltar 10s";PlayerGlyph.FORWARD->"Avançar 10s"
        PlayerGlyph.PLAY->"Reproduzir";PlayerGlyph.PAUSE->"Pausar"
        PlayerGlyph.AUDIO->"Áudio";PlayerGlyph.SUBTITLES->"Legendas"
        PlayerGlyph.CATALOG->"Catálogo";PlayerGlyph.ASPECT->"Tela";PlayerGlyph.SETTINGS->"Opções"
    }
    val accent=playerThemeAccent()
    Box(Modifier.width(width).height(if(compact)52.dp else 58.dp)
        .background(if(focused)accent.copy(alpha=.18f)else Color.Transparent,androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
        .then(if(focused)Modifier.border(1.dp,accent,androidx.compose.foundation.shape.RoundedCornerShape(7.dp))else Modifier)
        .then(if(focusRequester!=null)Modifier.focusRequester(focusRequester)else Modifier)
        .onFocusChanged { focused=it.isFocused }
        .semantics { contentDescription=label }
        .clickable(interactionSource=interaction,indication=null,onClick=onClick)) {
        Box(Modifier.align(Alignment.TopCenter).height(if(compact)32.dp else 36.dp).fillMaxWidth(),
            contentAlignment=Alignment.Center) {
            NeutralPlayerGlyph(icon,if(focused)accent else NeutralPlayerPalette.icon,
                Modifier.size(base*if(focused)1.10f else 1f))
        }
        Text(shortLabel,color=if(focused)accent else NeutralPlayerPalette.text,fontSize=11.sp,
            fontWeight=FontWeight.Medium,maxLines=1,modifier=Modifier.align(Alignment.BottomCenter)
                .padding(bottom=3.dp).wrapContentWidth(unbounded=true))
    }
}

@Composable
internal fun NeutralPlayerMenuItem(label:String,description:String,focusRequester:FocusRequester?,
    glyph:PlayerMenuGlyph?,enabled:Boolean,onClick:()->Unit) {
    var focused by remember { mutableStateOf(false) }
    val interaction=remember { MutableInteractionSource() }
    val accent=playerThemeAccent()
    Row(Modifier.fillMaxWidth().background(if(focused)accent.copy(alpha=.18f)else Color.White.copy(alpha=.035f),androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
        .border(1.dp,if(focused)accent else Color.White.copy(alpha=.10f),androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
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
                fontSize=11.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
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
        Canvas(modifier) {
            val w=size.width;val h=size.height;val stroke=w*.065f
            fun line(x:Float,y:Float,xx:Float,yy:Float)=drawLine(color,Offset(w*x,h*y),Offset(w*xx,h*yy),stroke,StrokeCap.Round)
            fun triangle(left:Boolean) {
                val p=Path().apply {
                    moveTo(w*(if(left).78f else .22f),h*.17f)
                    lineTo(w*(if(left).22f else .78f),h*.5f)
                    lineTo(w*(if(left).78f else .22f),h*.83f);close()
                };drawPath(p,color,style=Stroke(stroke))
            }
            when(icon) {
                PlayerGlyph.PLAY->triangle(false)
                PlayerGlyph.PAUSE->{line(.34f,.17f,.34f,.83f);line(.66f,.17f,.66f,.83f)}
                PlayerGlyph.PREVIOUS->{triangle(true);line(.12f,.17f,.12f,.83f)}
                PlayerGlyph.NEXT->{triangle(false);line(.88f,.17f,.88f,.83f)}
                PlayerGlyph.CATALOG->for(x in listOf(.12f,.57f))for(y in listOf(.12f,.57f))drawRoundRect(color,Offset(w*x,h*y),Size(w*.30f,h*.30f),CornerRadius(w*.025f),style=Stroke(stroke))
                PlayerGlyph.ASPECT->{drawRoundRect(color,Offset(w*.08f,h*.13f),Size(w*.84f,h*.60f),CornerRadius(w*.03f),style=Stroke(stroke));line(.5f,.73f,.5f,.89f);line(.32f,.89f,.68f,.89f)}
                PlayerGlyph.SUBTITLES->{
                    val p=Path().apply{moveTo(w*.12f,h*.14f);lineTo(w*.88f,h*.14f);lineTo(w*.88f,h*.72f);lineTo(w*.64f,h*.72f);lineTo(w*.51f,h*.88f);lineTo(w*.51f,h*.72f);lineTo(w*.12f,h*.72f);close()}
                    drawPath(p,color,style=Stroke(stroke));line(.26f,.34f,.74f,.34f);line(.26f,.51f,.64f,.51f)
                }
                PlayerGlyph.AUDIO->{line(.2f,.4f,.35f,.4f);line(.35f,.4f,.55f,.2f);line(.55f,.2f,.55f,.8f);line(.55f,.8f,.35f,.6f);line(.35f,.6f,.2f,.6f);line(.2f,.6f,.2f,.4f);drawArc(color,-60f,120f,false,Offset(w*.43f,h*.18f),Size(w*.48f,h*.64f),style=Stroke(stroke))}
                PlayerGlyph.SETTINGS->{
                    val p=Path();for(i in 0 until 32){val angle=i*Math.PI/16-Math.PI/2;val radius=if(i%4 in 1..2).44f else .34f
                        val x=w*(.5f+radius*kotlin.math.cos(angle).toFloat());val y=h*(.5f+radius*kotlin.math.sin(angle).toFloat());if(i==0)p.moveTo(x,y)else p.lineTo(x,y)};p.close()
                    drawPath(p,color,style=Stroke(stroke));drawCircle(color,w*.13f,style=Stroke(stroke))
                }
                else->Unit
            }
        }
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
