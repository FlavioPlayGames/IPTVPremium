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

/** Reuses the sharp shared theme surface in both modes. */
@Composable
internal fun PlaylistThemeBackdrop(modifier: Modifier = Modifier) {
    br.com.flaviotorres.iptvpremium.ui.components.CatalogThemeBackdrop(modifier)
}

internal object PlaylistGlass {
    val panel get()=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.panel
    val control get()=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.control
    val selection get()=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.selection
    val dialog get()=panel
    fun surface(source:Long)=panel
}
