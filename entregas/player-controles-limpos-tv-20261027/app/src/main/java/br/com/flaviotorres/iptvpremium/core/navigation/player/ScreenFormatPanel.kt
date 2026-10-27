package br.com.flaviotorres.iptvpremium.core.player

import android.view.KeyEvent as AndroidKey
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

import kotlinx.coroutines.delay

private enum class FormatPage { MENU,ZOOM,MANUAL }
private enum class Triangle { UP,DOWN,LEFT,RIGHT }
private enum class FormatGlyph { NORMAL,ZOOM,FILL,MANUAL }

/** No selection dismisses this panel: only Back or five seconds with no input. */
@Composable
internal fun ScreenFormatPanel(layout:PlaybackVideoLayout,onChange:(PlaybackVideoLayout)->Unit,
    onClose:()->Unit,modifier:Modifier=Modifier,initialFocus:FocusRequester?=null) {
    var page by remember { mutableStateOf(FormatPage.MENU) }
    var editor by remember { mutableStateOf(FormatPage.MANUAL) }
    var activity by remember { mutableIntStateOf(0) }
    var direction by remember { mutableStateOf(Triangle.UP) }
    val focus=initialFocus ?: remember { FocusRequester() }
    val menuFocus=remember(focus){listOf(focus)+List(3){FocusRequester()}}
    val close by rememberUpdatedState(onClose)
    val current by rememberUpdatedState(layout)
    val change by rememberUpdatedState(onChange)
    val accent=NeutralPlayerPalette.focused
    fun touch(){activity++}
    fun beginEditing(next:FormatPage) {
        touch()
        change(ScreenAdjustmentPolicy.freshLayout())
        editor=next
        page=next
    }
    fun adjust(arrow:Triangle) {
        touch();direction=arrow
        val m=current.manual
        val next=if(page==FormatPage.ZOOM) m.copy(zoom=ScreenAdjustmentPolicy.step(m.zoom,if(arrow==Triangle.UP)1 else -1))
        else when(arrow) {
            Triangle.UP->m.copy(heightScale=ScreenAdjustmentPolicy.step(m.heightScale,1))
            Triangle.DOWN->m.copy(heightScale=ScreenAdjustmentPolicy.step(m.heightScale,-1))
            Triangle.LEFT->m.copy(widthScale=ScreenAdjustmentPolicy.step(m.widthScale,-1))
            Triangle.RIGHT->m.copy(widthScale=ScreenAdjustmentPolicy.step(m.widthScale,1))
        }
        change(PlaybackVideoLayout(PlaybackAspectMode.MANUAL,next))
    }
    LaunchedEffect(activity) {delay(ScreenAdjustmentPolicy.IDLE_MS);close()}
    LaunchedEffect(page) { direction=Triangle.UP; withFrameNanos {}; focus.requestFocus() }
    BackHandler { close() }
    val shape=RoundedCornerShape(10.dp)
    BoxWithConstraints(modifier.fillMaxSize()) {
    val manual=page!=FormatPage.MENU
    val placement=if(manual) Modifier.align(Alignment.BottomCenter).padding(bottom=16.dp)
        else Modifier.align(Alignment.CenterEnd).padding(end=16.dp).width((maxWidth*.32f).coerceIn(220.dp,320.dp))
    Column(placement
        .onPreviewKeyEvent { event ->
            val code=event.nativeKeyEvent.keyCode
            if(event.type==KeyEventType.KeyDown)touch()
            if(page==FormatPage.MENU)return@onPreviewKeyEvent false
            val arrow=when(code) {
                AndroidKey.KEYCODE_DPAD_UP->Triangle.UP
                AndroidKey.KEYCODE_DPAD_DOWN->Triangle.DOWN
                AndroidKey.KEYCODE_DPAD_LEFT->Triangle.LEFT
                AndroidKey.KEYCODE_DPAD_RIGHT->Triangle.RIGHT
                else->null
            }
            val ok=code in listOf(AndroidKey.KEYCODE_DPAD_CENTER,AndroidKey.KEYCODE_ENTER,AndroidKey.KEYCODE_NUMPAD_ENTER)
            if(arrow!=null) {
                if(event.type==KeyEventType.KeyDown && (page==FormatPage.MANUAL || arrow==Triangle.UP || arrow==Triangle.DOWN))adjust(arrow)
                true
            } else if(ok) {
                if(event.type==KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount==0){page=FormatPage.MENU;touch()}
                true
            } else false
        }
        .then(if(page!=FormatPage.MENU)Modifier.focusRequester(focus).focusable()else Modifier)
        .background(NeutralPlayerPalette.menu,shape)
        .border(if(manual).6.dp else 0.dp,if(manual)Color(0xFF707070)else Color.Transparent,shape)
        .padding(if(manual)10.dp else 16.dp),
        horizontalAlignment=if(manual)Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement=Arrangement.spacedBy(if(manual)4.dp else 6.dp)) {
        if(!manual) {
            Text("FORMATO DE TELA",color=NeutralPlayerPalette.text,fontSize=15.sp,fontWeight=FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(.6.dp).background(Color(0xFF535353)))
        }
        if(page==FormatPage.MENU) {
            FormatOption("Normal",FormatGlyph.NORMAL,layout.mode==PlaybackAspectMode.NORMAL,menuFocus[0],menuFocus[0],menuFocus[1]) {
                touch();change(layout.copy(mode=PlaybackAspectMode.NORMAL))
            }
            FormatOption("Zoom",FormatGlyph.ZOOM,layout.mode==PlaybackAspectMode.MANUAL && editor==FormatPage.ZOOM,menuFocus[1],menuFocus[0],menuFocus[2]) {
                beginEditing(FormatPage.ZOOM)
            }
            FormatOption("Tela cheia",FormatGlyph.FILL,layout.mode==PlaybackAspectMode.FULL_SCREEN,menuFocus[2],menuFocus[1],menuFocus[3]) {
                touch();change(layout.copy(mode=PlaybackAspectMode.FULL_SCREEN))
            }
            FormatOption("Ajuste manual",FormatGlyph.MANUAL,layout.mode==PlaybackAspectMode.MANUAL && editor==FormatPage.MANUAL,menuFocus[3],menuFocus[2],menuFocus[3]) {
                beginEditing(FormatPage.MANUAL)
            }
        } else Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(1.dp)) {
                TriangleButton(Triangle.UP,direction==Triangle.UP,accent){adjust(Triangle.UP)}
                if(page==FormatPage.ZOOM)Text("${(layout.manual.zoom*100).toInt()}%",color=NeutralPlayerPalette.text,fontSize=12.sp,modifier=Modifier.padding(vertical=2.dp))
                if(page==FormatPage.MANUAL) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(1.dp)) {
                    TriangleButton(Triangle.LEFT,direction==Triangle.LEFT,accent){adjust(Triangle.LEFT)}
                    Box(Modifier.size(28.dp).focusProperties{canFocus=false}
                        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null){touch();page=FormatPage.MENU},contentAlignment=Alignment.Center) {
                        Text("OK",color=NeutralPlayerPalette.text,fontSize=11.sp)
                    }
                    TriangleButton(Triangle.RIGHT,direction==Triangle.RIGHT,accent){adjust(Triangle.RIGHT)}
                }
                TriangleButton(Triangle.DOWN,direction==Triangle.DOWN,accent){adjust(Triangle.DOWN)}
            }
            if(page==FormatPage.MANUAL)Text("L ${(layout.manual.widthScale*100).toInt()}% · A ${(layout.manual.heightScale*100).toInt()}%",color=NeutralPlayerPalette.text,fontSize=11.sp)
        }
    }
    }
}

@Composable
private fun FormatOption(label:String,glyph:FormatGlyph,selected:Boolean,focus:FocusRequester?=null,above:FocusRequester?=null,below:FocusRequester?=null,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().then(if(focus!=null)Modifier.focusRequester(focus)else Modifier)
        .onFocusChanged{focused=it.isFocused}
        .focusProperties{left=FocusRequester.Cancel;right=FocusRequester.Cancel;if(above!=null)up=if(above===focus)FocusRequester.Cancel else above;if(below!=null)down=if(below===focus)FocusRequester.Cancel else below}
        .semantics{this.selected=selected}
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        Text(if(focused)"•" else " ",color=NeutralPlayerPalette.focused,fontSize=14.sp,modifier=Modifier.width(8.dp))
        FormatOptionGlyph(glyph,if(focused)NeutralPlayerPalette.focused else NeutralPlayerPalette.icon,Modifier.size(20.dp))
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(label,color=if(focused)NeutralPlayerPalette.focused else NeutralPlayerPalette.text,fontSize=14.sp,fontWeight=if(focused)FontWeight.SemiBold else FontWeight.Normal)
            if(selected)Text("Selecionado",color=NeutralPlayerPalette.secondary,fontSize=11.sp)
        }
        Text("›",color=NeutralPlayerPalette.secondary,fontSize=20.sp)
    }
}

@Composable
private fun TriangleButton(arrow:Triangle,active:Boolean,accent:Color,onClick:()->Unit) {
    val label=when(arrow){Triangle.UP->"Aumentar altura ou zoom";Triangle.DOWN->"Diminuir altura ou zoom";Triangle.LEFT->"Diminuir largura";Triangle.RIGHT->"Aumentar largura"}
    Box(Modifier.size(28.dp).focusProperties{canFocus=false}.semantics{contentDescription=label;role=Role.Button}
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick).padding(6.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            // Slightly rounded, filled triangles; active direction changes only brightness/size.
            val w=size.width;val h=size.height
            val p=Path().apply {
                moveTo(w*.43f,h*.20f)
                quadraticBezierTo(w*.50f,h*.08f,w*.57f,h*.20f)
                lineTo(w*.91f,h*.78f)
                quadraticBezierTo(w*.99f,h*.92f,w*.83f,h*.92f)
                lineTo(w*.17f,h*.92f)
                quadraticBezierTo(w*.01f,h*.92f,w*.09f,h*.78f)
                close()
            }
            val angle=when(arrow){Triangle.UP->0f;Triangle.DOWN->180f;Triangle.LEFT->270f;Triangle.RIGHT->90f}
            rotate(angle){scale(if(active)1.10f else 1f){drawPath(p,if(active)accent else NeutralPlayerPalette.icon)}}
        }
    }
}

/** Separate native vector symbol per format, matching the approved sidebar. */
@Composable
private fun FormatOptionGlyph(glyph:FormatGlyph,color:Color,modifier:Modifier) {
    Canvas(modifier) {
        val w=size.width;val h=size.height;val stroke=w*.075f
        fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(color,Offset(w*x1,h*y1),Offset(w*x2,h*y2),stroke,StrokeCap.Round)
        when(glyph) {
            FormatGlyph.NORMAL->{
                drawRoundRect(color,Offset(w*.12f,h*.23f),Size(w*.76f,h*.54f),CornerRadius(w*.06f),style=Stroke(stroke))
                line(.39f,.50f,.61f,.50f)
            }
            FormatGlyph.ZOOM->{
                drawCircle(color,w*.28f,Offset(w*.41f,h*.41f),style=Stroke(stroke))
                line(.63f,.63f,.89f,.89f)
            }
            FormatGlyph.FILL->{
                line(.12f,.37f,.12f,.12f);line(.12f,.12f,.37f,.12f)
                line(.63f,.12f,.88f,.12f);line(.88f,.12f,.88f,.37f)
                line(.88f,.63f,.88f,.88f);line(.88f,.88f,.63f,.88f)
                line(.37f,.88f,.12f,.88f);line(.12f,.88f,.12f,.63f)
            }
            FormatGlyph.MANUAL->{
                drawRoundRect(color,Offset(w*.12f,h*.20f),Size(w*.76f,h*.60f),CornerRadius(w*.04f),style=Stroke(stroke))
                line(.32f,.64f,.68f,.36f);line(.46f,.36f,.68f,.36f);line(.68f,.36f,.68f,.55f)
            }
        }
    }
}
