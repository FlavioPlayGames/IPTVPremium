package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors

internal val LocalCatalogCardHeight = staticCompositionLocalOf { Dp.Unspecified }
internal val LocalCatalogRowSpacing = staticCompositionLocalOf { 10.dp }

/** Header-free catalog frame shared by movies and series, including local lists. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CatalogTwoRowFrame(compactLayout: Boolean, columns: Int,
    content: @Composable (Float) -> Unit) {
    val shape = RoundedCornerShape(if (compactLayout) 14.dp else 18.dp)
    BoxWithConstraints(Modifier.fillMaxSize()
        .background(CatalogGlass.surface(0x400B0D10), shape)
        .border(1.dp, CatalogStyle.outline(false, ThemeColors.metallicBorder), shape)
        .padding(if (compactLayout) 4.dp else 6.dp)) {
        val scale = LocalDensity.current.fontScale
        val metadata = if (compactLayout) 26f * scale + 6f else 34f * scale + 10f
        val geometry = catalogTwoRowGeometry(maxWidth.value, maxHeight.value, columns,
            if (compactLayout) 10f else 16f, metadata)
        // Quantize the pair of rows together. Rounding each Dp independently can
        // otherwise leave a pixel of the second row outside the viewport.
        val density = LocalDensity.current
        val cardPx = kotlin.math.floor(with(density) { geometry.cardHeight.dp.toPx() }).toInt()
        val metadataPx = kotlin.math.ceil(with(density) { metadata.dp.toPx() }).toInt()
        val paddingPx = with(density) { 8.dp.roundToPx() }
        val gapPx = (constraints.maxHeight - paddingPx * 2 - cardPx * 2).coerceAtLeast(0)
        val posterPx = minOf(with(density) { geometry.posterWidth.dp.toPx() },
            (cardPx - metadataPx).coerceAtLeast(0) * CATALOG_POSTER_ASPECT)
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides CatalogRowBringIntoViewSpec,
            LocalCatalogPosterWidth provides with(density) { posterPx.toDp() },
            LocalCatalogMetadataHeight provides metadata.dp,
            LocalCatalogCardHeight provides with(density) { cardPx.toDp() },
            LocalCatalogRowSpacing provides with(density) { gapPx.toDp() }
        ) { content(CATALOG_POSTER_ASPECT) }
    }
}

/** DPAD navigation owns row alignment; platform focus must not recenter a card. */
@OptIn(ExperimentalFoundationApi::class)
internal object CatalogRowBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}
