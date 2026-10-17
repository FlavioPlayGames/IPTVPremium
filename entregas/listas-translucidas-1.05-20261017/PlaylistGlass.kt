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
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource

/** Blur is confined to a decorative, static image layer. Content stays sharp. */
@Composable
internal fun PlaylistThemeBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.clipToBounds()) {
        Image(themePainterResource(R.drawable.home_background), contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(18.dp), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(ThemeColors.background.copy(alpha = .45f)))
    }
}

internal object PlaylistGlass {
    val panel get() = ThemeColors.panel.copy(alpha = .60f)
    val control get() = ThemeColors.control.copy(alpha = .52f)
    val selection get() = SelectionTheme.color().copy(alpha = .82f)
    val dialog get() = ThemeColors.panel.copy(alpha = .82f)
    fun surface(source: Long) = ThemeColors.surface(source).let { it.copy(alpha = minOf(it.alpha, .60f)) }
}
