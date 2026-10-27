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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

import kotlinx.coroutines.delay

private enum class FormatPage { MENU,ZOOM,MANUAL }
private enum class Triangle { UP,DOWN,LEFT,RIGHT }

/** No selection dismisses this panel: only Back or five seconds with no input. */
@Composable
internal fun ScreenFormatPanel(layout:PlaybackVideoLayout,onChange:(PlaybackVideoLayout)->Unit,
    onClose:()->Unit,modifier:Modifier=Modifier,initialFocus:FocusRequester?=null) {
    var page by remember { mutableStateOf(FormatPage.MENU) }
    var activity by remember { mutableIntStateOf(0) }
    var direction by remember { mutableStateOf(Triangle.UP) }
    val focus=initialFocus ?: remember { FocusRequester() }
    val menuFocus=remember(focus){listOf(focus)+List(3){FocusRequester()}}
    val close by rememberUpdatedState(onClose)
    val current by rememberUpdatedState(layout)
    val change by rememberUpdatedState(onChange)
    val accent=NeutralPlayerPalette.focused
    fun touch(){activity++}
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
            FormatOption("Normal","Proporção original",layout.mode==PlaybackAspectMode.NORMAL,menuFocus[0],menuFocus[0],menuFocus[1]) {
                touch();change(layout.copy(mode=PlaybackAspectMode.NORMAL))
            }
            FormatOption("Zoom","Aumente ou diminua com ▲ e ▼",false,menuFocus[1],menuFocus[0],menuFocus[2]) {
                touch();change(layout.copy(mode=PlaybackAspectMode.MANUAL,manual=layout.manual.copy(offsetX=0f,offsetY=0f)));page=FormatPage.ZOOM
            }
            FormatOption("Tela cheia","Preenche a tela; pode cortar as bordas",layout.mode==PlaybackAspectMode.FULL_SCREEN,menuFocus[2],menuFocus[1],menuFocus[3]) {
                touch();change(layout.copy(mode=PlaybackAspectMode.FULL_SCREEN))
            }
            FormatOption("Ajuste manual","Ajuste largura e altura com as setas",layout.mode==PlaybackAspectMode.MANUAL,menuFocus[3],menuFocus[2],menuFocus[3]) {
                touch();change(layout.copy(mode=PlaybackAspectMode.MANUAL,manual=layout.manual.copy(offsetX=0f,offsetY=0f)));page=FormatPage.MANUAL
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
private fun FormatOption(label:String,description:String,selected:Boolean,focus:FocusRequester?=null,above:FocusRequester?=null,below:FocusRequester?=null,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().then(if(focus!=null)Modifier.focusRequester(focus)else Modifier)
        .onFocusChanged{focused=it.isFocused}
        .focusProperties{left=FocusRequester.Cancel;right=FocusRequester.Cancel;if(above!=null)up=if(above===focus)FocusRequester.Cancel else above;if(below!=null)down=if(below===focus)FocusRequester.Cancel else below}
        .semantics{this.selected=selected}
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        Text(if(focused)"•" else " ",color=NeutralPlayerPalette.focused,fontSize=14.sp,modifier=Modifier.width(8.dp))
        PlayerMenuGlyphDrawing(PlayerMenuGlyph.SCREEN,if(focused)NeutralPlayerPalette.focused else NeutralPlayerPalette.icon,Modifier.size(19.dp),monochrome=true)
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
        .border(if(active).8.dp else 0.dp,if(active)accent else Color.Transparent,RoundedCornerShape(4.dp))
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick).padding(7.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val p=Path();val w=size.width;val h=size.height
            when(arrow) {
                Triangle.UP->{p.moveTo(w/2,0f);p.lineTo(w,h);p.lineTo(0f,h)}
                Triangle.DOWN->{p.moveTo(0f,0f);p.lineTo(w,0f);p.lineTo(w/2,h)}
                Triangle.LEFT->{p.moveTo(0f,h/2);p.lineTo(w,0f);p.lineTo(w,h)}
                Triangle.RIGHT->{p.moveTo(w,h/2);p.lineTo(0f,h);p.lineTo(0f,0f)}
            };p.close();drawPath(p,Color.White)
        }
    }
}
