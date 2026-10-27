package br.com.flaviotorres.iptvpremium.ui.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors
import br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme
import br.com.flaviotorres.iptvpremium.ui.theme.themeBackdropPainter
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource

/** Blur is confined to a decorative, static image layer. Content stays sharp. */
@Composable
internal fun PlaylistThemeBackdrop(modifier: Modifier = Modifier) {
    if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) {
        br.com.flaviotorres.iptvpremium.ui.theme.MarbleCatalogWallpaper(modifier)
        return
    }
    Box(modifier.clipToBounds()) {
        Image(themeBackdropPainter(R.drawable.home_background, 18.dp), contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(18.dp), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(ThemeColors.background.copy(alpha = .45f)))
    }
}

internal object PlaylistGlass {
    val panel get()=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.panel
    val control get()=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.control
    val selection get()=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.selection
    val dialog get()=panel
    fun surface(source:Long)=panel
}
