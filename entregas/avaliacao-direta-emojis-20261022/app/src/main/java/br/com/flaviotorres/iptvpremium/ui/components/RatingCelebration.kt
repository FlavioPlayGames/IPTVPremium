package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Text
import kotlin.math.*

/** A temporary, non-focusable drawing layer; the details page and its focus remain mounted. */
@Composable
internal fun RatingCelebration(score:Int,progress:()->Float,origin:Offset,originWidth:Float) {
    Popup(alignment=Alignment.TopStart,properties=PopupProperties(focusable=false,clippingEnabled=false)) {
        BoxWithConstraints(Modifier.fillMaxSize().clearAndSetSemantics {
            liveRegion=LiveRegionMode.Polite
            contentDescription=when(score){1,2->"Avaliação salva. Sorriso e derretimento.";3,4->"Avaliação salva. Beijinho com coração.";else->"Avaliação salva. Obrigado!"}
        }) {
            Canvas(Modifier.fillMaxSize()) {
                val p=progress().coerceIn(0f,1f)
                val center=Offset(size.width*.53f,size.height*.46f)
                val r=min(size.minDimension*.17f,108.dp.toPx())
                val fade=((1f-p)/.1f).coerceIn(0f,1f)
                val merge=(p/.20f).coerceIn(0f,1f)
                val eased=merge*merge*(3f-2f*merge)
                val morph=((p-.15f)/.08f).coerceIn(0f,1f)
                drawCircle(Brush.radialGradient(listOf(Color(0x665E4010),Color.Transparent),center,r*2f),r*2f,center,alpha=fade*morph)
                if(morph<1f)repeat(score){i->
                    val start=origin+Offset((i-2f)*originWidth/6.7f,0f)
                    val location=start+(center-start)*eased
                    val radius=9.dp.toPx()+(r-9.dp.toPx())*eased
                    drawPath(reactionStar(location,radius),Brush.verticalGradient(listOf(Color(0xFFFFE68B),ReferenceDetailsPalette.gold)),alpha=(1f-morph)*fade)
                }
                if(score<5&&morph>0f) {
                    val melt=if(score<=2)((p-.51f)/.36f).coerceIn(0f,1f)else 0f
                    val kiss=score>=3&&p>.56f
                    drawReactionFace(center,r,melt,kiss,morph*fade)
                    if(kiss){
                        val q=((p-.57f)/.31f).coerceIn(0f,1f)
                        val heartCenter=center+Offset(r*(.48f+1.9f*q),r*(-.02f-1.3f*q))
                        drawPath(reactionHeart(heartCenter,r*(.17f+.16f*q)),Color(0xFFF32B52),alpha=min(q/.13f,(1f-q)/.22f).coerceIn(0f,1f)*fade)
                    }
                }
                if(score==5&&morph>0f) {
                    val launch=((p-.31f)/.57f).coerceIn(0f,1f)
                    val speed=launch*launch
                    val start=center+Offset(-r*.6f,r*.6f)
                    val end=center+Offset(size.width*.31f,-size.height*.36f)
                    val rocket=start+(end-start)*speed
                    val visible=morph*fade
                    if(launch>0f)repeat(36){i->
                        val t=(i/35f)*speed
                        val point=start+(end-start)*t
                        val normal=Offset(sin(i*2.4f)*r*.16f,cos(i*1.7f)*r*.16f)
                        drawCircle(if(i%3==0)Color(0xFFFF8947)else Color(0xFFFFD566),r*(.018f+.018f*(i%3)),point+normal,alpha=visible*(.3f+.7f*i/35f))
                    }
                    withTransform({translate(rocket.x,rocket.y);rotate(42f,Offset.Zero)}) {
                        val flame=.83f+.17f*sin(p*170f)
                        drawPath(Path().apply{moveTo(-r*.14f,r*.35f);quadraticTo(0f,r*(1f+flame*.2f),r*.14f,r*.35f);close()},Color(0xFFFFA02F),alpha=visible)
                        drawPath(Path().apply{moveTo(-r*.07f,r*.36f);lineTo(0f,r*.87f);lineTo(r*.07f,r*.36f);close()},Color(0xFFFFE487),alpha=visible)
                        drawPath(Path().apply{moveTo(-r*.2f,r*.02f);lineTo(-r*.44f,r*.43f);lineTo(-r*.12f,r*.31f);lineTo(r*.12f,r*.31f);lineTo(r*.44f,r*.43f);lineTo(r*.2f,r*.02f);close()},Color(0xFFE34C53),alpha=visible)
                        drawPath(Path().apply{moveTo(0f,-r*.65f);cubicTo(-r*.26f,-r*.39f,-r*.26f,r*.12f,-r*.14f,r*.38f);lineTo(r*.14f,r*.38f);cubicTo(r*.26f,r*.12f,r*.26f,-r*.39f,0f,-r*.65f);close()},Brush.horizontalGradient(listOf(Color(0xFFD5DFE7),Color.White,Color(0xFFAEBFCB)),-r*.25f,r*.25f),alpha=visible)
                        drawPath(Path().apply{moveTo(0f,-r*.65f);quadraticTo(-r*.13f,-r*.51f,-r*.18f,-r*.28f);lineTo(r*.18f,-r*.28f);quadraticTo(r*.13f,-r*.51f,0f,-r*.65f);close()},Color(0xFFEE575E),alpha=visible)
                        drawCircle(Color(0xFF4D789A),r*.105f,Offset(0f,-r*.1f),alpha=visible)
                        drawCircle(Color(0xFFA6DAF3),r*.06f,Offset(-r*.018f,-r*.12f),alpha=visible)
                    }
                }
            }
            if(score==5) {
                val p=progress()
                val written=((p-.40f)/.31f).coerceIn(0f,1f)
                val text="OBRIGADO!".take((written*9).toInt())
                Text(text,color=Color(0xFFFFDE83),fontSize=36.sp,
                    fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier=Modifier.offset(x=maxWidth*.36f,y=maxHeight*.46f)
                        .graphicsLayer{rotationZ=-28f;alpha=((1f-progress())/.1f).coerceIn(0f,1f)})
            }
        }
    }
}

private fun reactionStar(c:Offset,r:Float)=Path().apply {
    repeat(10){i->val a=(-90+i*36)*PI/180;val d=if(i%2==0)r else r*.46f
        val x=c.x+cos(a).toFloat()*d;val y=c.y+sin(a).toFloat()*d
        if(i==0)moveTo(x,y)else lineTo(x,y)};close()
}
private fun reactionHeart(c:Offset,r:Float)=Path().apply {
    moveTo(c.x,c.y+r);cubicTo(c.x-r*1.8f,c.y-r*.1f,c.x-r*.7f,c.y-r*1.5f,c.x,c.y-r*.5f)
    cubicTo(c.x+r*.7f,c.y-r*1.5f,c.x+r*1.8f,c.y-r*.1f,c.x,c.y+r);close()
}
private fun DrawScope.drawReactionFace(c:Offset,r:Float,melt:Float,kiss:Boolean,alpha:Float) {
    val ink=Color(0xFF68410F).copy(alpha=alpha)
    val drop=melt*melt
    val fill=Brush.verticalGradient(listOf(Color(0xFFFFE88B),Color(0xFFFFCD3F),Color(0xFFEAB02D)),-r,r*1.4f)
    if(melt>0f)drawOval(Color(0xFFF4BF35),c+Offset(-r*(.5f+.8f*drop),r*.77f),Size(r*(1f+1.6f*drop),r*(.15f+.30f*drop)),alpha=alpha)
    withTransform({translate(c.x,c.y+r*.45f*drop);scale(1f+drop*.17f,1f-drop*.57f,Offset.Zero)}) {
        drawCircle(fill,r,Offset.Zero,alpha=alpha)
        if(drop>0f)repeat(3){i->
            val x=(i-1)*r*.57f
            drawRoundRect(Color(0xFFF7C33B),Offset(x-r*.10f,r*.58f),Size(r*.20f,r*(.2f+drop*(.38f+if(i==1).20f else 0f))),androidx.compose.ui.geometry.CornerRadius(r*.1f),alpha=alpha)
        }
        val sw=r*.045f
        if(melt>.25f) {
            for(sign in listOf(-1f,1f))drawLine(ink,Offset(sign*r*.34f-r*.10f,-r*.14f),Offset(sign*r*.34f+r*.10f,-r*.18f),sw,StrokeCap.Round)
        }else{
            for(sign in listOf(-1f,1f))drawArc(ink,190f,160f,false,Offset(sign*r*.34f-r*.13f,-r*.28f),Size(r*.26f,r*.24f),style=Stroke(sw,cap=StrokeCap.Round))
        }
        for(sign in listOf(-1f,1f))drawOval(Color(0xFFEE8F4B),Offset(sign*r*.53f-r*.12f,r*.01f),Size(r*.24f,r*.10f),alpha=alpha*.55f)
        if(kiss) {
            drawPath(Path().apply{moveTo(-r*.04f,r*.16f);quadraticTo(r*.25f,r*.07f,r*.11f,r*.25f);quadraticTo(r*.27f,r*.41f,-r*.03f,r*.33f)},ink,style=Stroke(sw,cap=StrokeCap.Round))
        }else drawArc(ink,0f,180f,false,Offset(-r*.32f,r*.04f),Size(r*.64f,r*.43f),style=Stroke(sw,cap=StrokeCap.Round))
    }
}
