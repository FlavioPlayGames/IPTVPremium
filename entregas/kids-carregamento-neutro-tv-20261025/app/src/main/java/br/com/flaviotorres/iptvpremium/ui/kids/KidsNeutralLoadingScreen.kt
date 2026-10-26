package br.com.flaviotorres.iptvpremium.ui.kids

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Same local static artwork in both themes. Only three vector-star alphas animate. */
@Composable
internal fun KidsNeutralLoadingScreen(modifier:Modifier=Modifier,errorMessage:String?=null,
    onRetry:(()->Unit)?=null,onBack:(()->Unit)?=null) {
    val loadingFocus=remember { FocusRequester() }
    val actionFocus=remember { FocusRequester() }
    if(onBack!=null) BackHandler(onBack=onBack)
    LaunchedEffect(errorMessage,onBack!=null) {
        if(onBack!=null) { withFrameNanos{};if(errorMessage==null)loadingFocus.requestFocus() else actionFocus.requestFocus() }
    }
    BoxWithConstraints(modifier.fillMaxSize().then(if(onBack!=null)Modifier
        .onPreviewKeyEvent { event ->
            if(event.key==Key.Escape || event.key==Key.Back) {
                if(event.type==KeyEventType.KeyUp)onBack()
                true
            } else false
        }.focusRequester(loadingFocus).focusable(enabled=errorMessage==null) else Modifier),contentAlignment=Alignment.Center) {
        val viewportWidth=maxWidth
        val viewportHeight=maxHeight
        KidsNeutralLoadingBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxWidth(.78f).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            KidsCinemaBrand(Modifier.width((viewportWidth*.36f).coerceIn(180.dp,330.dp))
                .height((viewportHeight*.32f).coerceIn(90.dp,210.dp)))
            Spacer(Modifier.height(14.dp))
            if(errorMessage==null) LoadingStars()
            Spacer(Modifier.height(12.dp))
            Text(errorMessage?:"Preparando sua aventura…",color=kidsCinemaText,
                fontSize=if(viewportHeight<500.dp)17.sp else 21.sp,fontWeight=FontWeight.Bold,
                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            if(errorMessage!=null) {
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    if(onRetry!=null) LoadingAction("Tentar novamente",actionFocus,onRetry)
                    if(onBack!=null) LoadingAction("Voltar",if(onRetry==null)actionFocus else null,onBack)
                }
            }
        }
    }
}

@Composable
private fun LoadingStars() {
    val transition=rememberInfiniteTransition(label="kids-loading-stars")
    val phase by transition.animateFloat(0f,3f,infiniteRepeatable(tween(1800,easing=LinearEasing)),label="star-opacity")
    Box(Modifier.width(116.dp).height(30.dp).semantics { progressBarRangeInfo=ProgressBarRangeInfo.Indeterminate }.drawWithCache {
        val radius=size.height*.44f
        val star=Path().apply {
            for(i in 0 until 10) {
                val angle=-Math.PI/2+i*Math.PI/5
                val r=if(i%2==0)radius else radius*.49f
                val x=(cos(angle)*r).toFloat();val y=(sin(angle)*r).toFloat()
                if(i==0)moveTo(x,y)else lineTo(x,y)
            }
            close()
        }
        val gold=Brush.verticalGradient(listOf(Color(0xFFFFE4A1),Color(0xFFE6B445),Color(0xFFC99025)),startY=-radius,endY=radius)
        onDrawBehind {
        for(i in 0..2) {
            val distance=abs(phase-i)
            val opacity=.28f+.72f*(1f-minOf(distance,3f-distance).coerceIn(0f,1f))
            withTransform({translate(size.width*(i+.5f)/3f,size.height*.5f)}) {drawPath(star,gold,alpha=opacity)}
        }
        }
    })
}

@Composable
private fun LoadingAction(label:String,focus:FocusRequester?,onClick:()->Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.then(if(focus!=null)Modifier.focusRequester(focus)else Modifier)
        .height(38.dp).background(if(focused)Color(0xFF8B631E)else Color(0xFFFFF5E5),RoundedCornerShape(12.dp))
        .border(1.dp,Color(0xFFD2AC66),RoundedCornerShape(12.dp)).onFocusChanged{focused=it.isFocused}
        .kidsCinemaClickable(onClick).focusable().padding(horizontal=18.dp),contentAlignment=Alignment.Center) {
        Text(label,color=kidsCinemaRowInk(focused,0xFFFFFFFF),fontWeight=FontWeight.Bold,fontSize=14.sp)
    }
}
