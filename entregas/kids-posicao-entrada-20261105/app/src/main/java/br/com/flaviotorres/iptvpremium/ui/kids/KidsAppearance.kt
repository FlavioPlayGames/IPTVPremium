package br.com.flaviotorres.iptvpremium.ui.kids

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeState

internal enum class KidsStyle(val key:String,val label:String,val accent:Color,val theme:PremiumTheme) {
    BLUE("blue","Azul",Color(0xFF51C8FF),PremiumTheme.BLUE),
    PINK("pink","Rosa",Color(0xFFFF86D5),PremiumTheme.PINK);
    val backdrop:Int get()=if(this==BLUE)R.drawable.kids_sky_blue else R.drawable.kids_sky_pink
    companion object { fun fromKey(key:String?)=entries.firstOrNull {it.key==key}?:BLUE }
}

/** Aparência infantil é independente da preferência do menu principal. */
internal object KidsAppearance {
    var active by mutableStateOf(false); private set
    var style by mutableStateOf(KidsStyle.BLUE); private set
    fun apply(active:Boolean,key:String?) {
        this.active=active;style=KidsStyle.fromKey(key)
        ThemeState.useKidsPalette(if(active)style.theme else null)
    }
    val accent get()=style.accent
}

@Composable
internal fun KidsHomeArtwork(style:KidsStyle,modifier:Modifier=Modifier,labels:Boolean=false) {
    BoxWithConstraints(modifier) {
        KidsCinemaHomeArtwork(style,Modifier.fillMaxSize())
        if(labels) KidsHomeMenu(style,maxWidth,maxHeight) { index,cell ->
            KidsHomeCell(style,index,false,maxHeight,cell)
        }
    }
}

/** Mesma geometria nas duas cores e na prévia: a arte não contém nomes ou seleção. */
@Composable
internal fun KidsHomeMenu(style:KidsStyle,width:Dp,height:Dp,content:@Composable (Int,Modifier)->Unit) {
    Row(Modifier.fillMaxWidth().offset(y=height*.31f).height(height*.51f)
        .padding(horizontal=width*.14f),horizontalArrangement=Arrangement.spacedBy(width*.025f)) {
        repeat(3) {index ->content(index,Modifier.weight(1f).fillMaxHeight())}
    }
}

@Composable
internal fun KidsHomeCell(style:KidsStyle,index:Int,focused:Boolean,height:Dp,modifier:Modifier=Modifier) {
    KidsCinemaHomeCell(style,index,focused,height,modifier)
}

/** Decoração estática: sem shimmer, partículas animadas ou trabalho por foco. */
@Composable
internal fun KidsScenery(modifier:Modifier=Modifier,panel:Boolean=false) {
    KidsCinemaBackdrop(modifier=modifier,catalog=true)
}

