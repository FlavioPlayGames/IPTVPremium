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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle
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
    val accent=CatalogStyle.accent
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
    val shape=RoundedCornerShape(14.dp)
    Column(modifier.widthIn(max=440.dp)
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
        .background(Color(0xFF292D32),shape).border(1.dp,Color(0xFF555C65),shape).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(when(page){FormatPage.MENU->"Formato de tela";FormatPage.ZOOM->"Zoom";FormatPage.MANUAL->"Ajuste manual"},
            color=Color.White,fontSize=18.sp,fontWeight=FontWeight.SemiBold)
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
        } else Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                TriangleButton(Triangle.UP,direction==Triangle.UP,accent){adjust(Triangle.UP)}
                if(page==FormatPage.MANUAL) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    TriangleButton(Triangle.LEFT,direction==Triangle.LEFT,accent){adjust(Triangle.LEFT)}
                    Box(Modifier.size(44.dp).focusProperties{canFocus=false}
                        .background(Color(0xFF393E45),RoundedCornerShape(8.dp))
                        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null){touch();page=FormatPage.MENU},contentAlignment=Alignment.Center) {
                        Text("OK",color=Color.White,fontSize=15.sp)
                    }
                    TriangleButton(Triangle.RIGHT,direction==Triangle.RIGHT,accent){adjust(Triangle.RIGHT)}
                }
                TriangleButton(Triangle.DOWN,direction==Triangle.DOWN,accent){adjust(Triangle.DOWN)}
            }
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(page==FormatPage.ZOOM)Text("Zoom ${(layout.manual.zoom*100).toInt()}%",color=Color.White,fontSize=18.sp)
                else {
                    Text("Largura ${(layout.manual.widthScale*100).toInt()}%",color=Color.White,fontSize=17.sp)
                    Text("Altura ${(layout.manual.heightScale*100).toInt()}%",color=Color.White,fontSize=17.sp)
                }
                Text("Um toque: 2%\nSegure para continuar",color=Color(0xFFC6CAD0),fontSize=12.sp)
            }
        }
        Text("Voltar para fechar · 5 s sem interação",color=Color(0xFFC6CAD0),fontSize=11.sp)
    }
}

@Composable
private fun FormatOption(label:String,description:String,selected:Boolean,focus:FocusRequester?=null,above:FocusRequester?=null,below:FocusRequester?=null,onClick:()->Unit) {
    var focused by remember {mutableStateOf(false)}
    val shape=RoundedCornerShape(8.dp)
    Column(Modifier.fillMaxWidth().then(if(focus!=null)Modifier.focusRequester(focus)else Modifier)
        .onFocusChanged{focused=it.isFocused}
        .focusProperties{left=FocusRequester.Cancel;right=FocusRequester.Cancel;if(above!=null)up=if(above===focus)FocusRequester.Cancel else above;if(below!=null)down=if(below===focus)FocusRequester.Cancel else below}
        .background(if(focused)Color(0xFF3B4148)else Color(0xFF30353B),shape)
        .border(if(focused)2.dp else 1.dp,if(focused||selected)CatalogStyle.accent else Color(0xFF545B64),shape)
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(horizontal=12.dp,vertical=9.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Text((if(selected)"✓ " else "")+label,color=Color.White,fontSize=15.sp)
        Text(description,color=Color(0xFFC6CAD0),fontSize=11.sp)
    }
}
@Composable
private fun TriangleButton(arrow:Triangle,active:Boolean,accent:Color,onClick:()->Unit) {
    val label=when(arrow){Triangle.UP->"Aumentar altura ou zoom";Triangle.DOWN->"Diminuir altura ou zoom";Triangle.LEFT->"Diminuir largura";Triangle.RIGHT->"Aumentar largura"}
    Box(Modifier.size(44.dp).focusProperties{canFocus=false}.semantics{contentDescription=label;role=Role.Button}
        .background(Color(0xFF343A42),RoundedCornerShape(8.dp))
        .border(if(active)2.dp else 1.dp,if(active)accent else Color(0xFF454C55),RoundedCornerShape(8.dp))
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick).padding(10.dp)) {
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
