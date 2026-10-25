package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.themeBackdropPainter
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource

/** Shared catalogue wallpaper; only this static decorative layer is blurred. */
@Composable
internal fun CatalogThemeBackdrop(modifier: Modifier = Modifier, cinemaKids: Boolean = false, cinemaTitle: String = "") {
    if (br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active) {
        if (cinemaKids) Box(modifier) {
            br.com.flaviotorres.iptvpremium.ui.kids.KidsCinemaBackdrop(modifier=Modifier.fillMaxSize(),catalog=true)
        }
        else br.com.flaviotorres.iptvpremium.ui.kids.KidsScenery(modifier)
        return
    }
    br.com.flaviotorres.iptvpremium.ui.theme.MarbleCatalogWallpaper(modifier)

}

/** Neutral smoked glass avoids tinting all panels brown/olive in the Gold theme. */
internal object CatalogGlass {
    val panelBrush get() = if (br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active)
        Brush.verticalGradient(if(br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.style == br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.PINK)
            listOf(Color(0xEB230A25),Color(0xDD120718)) else listOf(Color(0xEB05152C),Color(0xDD060B1B)))
        else Brush.verticalGradient(listOf(CatalogStyle.panel.copy(alpha=if(CatalogStyle.light).64f else .90f), CatalogStyle.panel.copy(alpha=if(CatalogStyle.light).58f else .88f)))
    val categoryPanelBrush get() = if (!CatalogStyle.regular) panelBrush else
        Brush.verticalGradient(listOf(CatalogStyle.panel.copy(alpha=if(CatalogStyle.light).54f else .72f), CatalogStyle.panel.copy(alpha=if(CatalogStyle.light).46f else .64f)))
    fun surface(source: Long): Color = CatalogStyle.surface(source)
}
