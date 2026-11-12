package br.com.flaviotorres.iptvpremium.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle
import kotlin.random.Random

/** Shared static brushed metal: no wallpaper decoding, blur, animation or per-frame noise. */
internal object MetallicPalette {
    val accent get()=when(ThemeState.selected) {
        PremiumTheme.BLUE->Color(0xFF009EF5)
        PremiumTheme.GOLD->Color(0xFFF2BC36)
        PremiumTheme.PINK->Color(0xFFF64C9B)
        PremiumTheme.PURPLE->Color(0xFFAC68ED)
        PremiumTheme.RED->Color(0xFFF25550)
    }
    val card get()=if(CatalogStyle.light)Color(0xFF506476)else Color(0xFF344655)
    val selected get()=androidx.compose.ui.graphics.lerp(Color(0xFF142B40),accent,.28f)
    val ink get()=Color(0xFFF5F6F8)
    val secondary get()=Color(0xFFDFE6ED)
}

@Composable
internal fun MetallicThemeBackdrop(modifier:Modifier=Modifier) {
    val light=CatalogStyle.light
    val accent=MetallicPalette.accent
    Box(modifier.graphicsLayer().drawWithCache {
        val width=size.width;val height=size.height
        val metal=Brush.horizontalGradient(if(light)listOf(Color(0xFF697F94),Color(0xFFABB9C6),Color(0xFFCBD1D6),Color(0xFF9DAEBD),Color(0xFF5C738B))
            else listOf(Color(0xFF111E2A),Color(0xFF26343F),Color(0xFF35424C),Color(0xFF202D38),Color(0xFF0E1B28)))
        val shine=Brush.verticalGradient(listOf(Color.White.copy(alpha=if(light).07f else .025f),Color.Transparent,Color.Black.copy(alpha=.12f)))
        // Resolution-bounded deterministic grain is recorded once per size/theme.
        val random=Random(8127)
        val count=(height/1.5f).toInt().coerceIn(1,720)
        val grain=List(count){i->
            val y=(i+.25f)*height/count
            val start=random.nextFloat()*.18f*width
            val end=width*(.80f+random.nextFloat()*.20f)
            Triple(Offset(start,y),Offset(end,y),random.nextFloat())
        }
        fun wedge(right:Boolean):Path {
            fun x(value:Float)=if(right)width-value else value
            return Path().apply {moveTo(x(0f),0f);lineTo(x(width*.014f),0f);lineTo(x(width*.058f),height*.69f);lineTo(x(0f),height*.48f);close()}
        }
        val left=wedge(false);val right=wedge(true)
        onDrawBehind {
            drawRect(metal);drawRect(shine)
            for((a,b,t)in grain)drawLine(if(t>.48f)Color.White.copy(alpha=if(light).065f else .035f)else Color.Black.copy(alpha=.065f),a,b,.55f)
            for(isRight in listOf(false,true)) {
                fun x(value:Float)=if(isRight)width-value else value
                val from=Offset(x(width*.009f),0f);val to=Offset(x(width*.055f),height*.67f)
                drawLine(accent.copy(alpha=.045f),from,to,width*.044f)
                drawLine(accent.copy(alpha=.09f),from,to,width*.022f)
                drawPath(if(isRight)right else left,Brush.horizontalGradient(if(isRight)listOf(accent.copy(alpha=.2f),Color(0xFF092238),accent)else listOf(accent,Color(0xFF092238),accent.copy(alpha=.2f))))
                drawLine(accent,from,to,(width*.0022f).coerceAtLeast(1f))
                drawLine(Color(0xFFD5EDFF).copy(alpha=.8f),Offset(x(width*.007f),0f),Offset(x(width*.052f),height*.64f),(width*.00065f).coerceAtLeast(.6f))
            }
        }
    })
}
