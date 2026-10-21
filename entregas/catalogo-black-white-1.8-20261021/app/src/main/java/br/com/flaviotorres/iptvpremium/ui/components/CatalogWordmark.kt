package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Flat crowned wordmark: no raster recolouring, shine animation or extra network work. */
@Composable
internal fun CatalogWordmark(compact: Boolean, modifier: Modifier = Modifier) {
    val tint=CatalogStyle.accent
    Column(modifier.width(if(compact)82.dp else 98.dp).height(if(compact)52.dp else 62.dp)
        .semantics(mergeDescendants=true){contentDescription="IPTV Premium"},
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Canvas(Modifier.width(if(compact)24.dp else 28.dp).height(if(compact)13.dp else 16.dp)) {
            val w=size.width;val h=size.height
            val crown=Path().apply {
                moveTo(w*.12f,h*.8f);lineTo(w*.04f,h*.22f);lineTo(w*.31f,h*.44f)
                lineTo(w*.5f,h*.03f);lineTo(w*.69f,h*.44f);lineTo(w*.96f,h*.22f)
                lineTo(w*.88f,h*.8f);close()
            }
            drawPath(crown,tint,style=Stroke(1.dp.toPx()))
            drawLine(tint,androidx.compose.ui.geometry.Offset(w*.14f,h*.97f),androidx.compose.ui.geometry.Offset(w*.86f,h*.97f),1.dp.toPx())
            for(x in listOf(.04f,.5f,.96f))drawCircle(tint,1.dp.toPx(),androidx.compose.ui.geometry.Offset(w*x,if(x==.5f)h*.03f else h*.22f))
        }
        Text("IPTV",color=tint,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=if(compact)28.sp else 34.sp,lineHeight=if(compact)28.sp else 34.sp)
        Text("PREMIUM",color=tint,fontFamily=FontFamily.Serif,fontWeight=FontWeight.Medium,fontSize=if(compact)7.sp else 8.sp,letterSpacing=if(compact)2.sp else 2.5.sp,lineHeight=9.sp)
    }
}
