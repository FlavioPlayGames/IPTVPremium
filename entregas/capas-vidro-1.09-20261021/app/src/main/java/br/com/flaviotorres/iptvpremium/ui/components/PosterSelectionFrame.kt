package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Option 2: defined metal rim and a fine dark separator, confined to the cover. */
internal fun Modifier.posterSelectionFrame(enabled: Boolean, roundedDp: Float): Modifier =
    if (!enabled) this else if (!CatalogStyle.regular) reinforcedCatalogFocus(true, roundedDp)
    else drawWithCache {
        val metal = Brush.linearGradient(listOf(
            CatalogStyle.borderColor(0xFF694019), CatalogStyle.borderColor(0xFFFFF3CE),
            CatalogStyle.borderColor(0xFFD6A32E), CatalogStyle.borderColor(0xFFFFDE8A),
            CatalogStyle.borderColor(0xFFB77D25)), end=Offset(size.width,size.height))
        val radius = roundedDp.dp.toPx()
        val outer = 1.3.dp.toPx()
        val inner = 2.9.dp.toPx()
        val rim = Stroke(2.4.dp.toPx())
        val separator = Stroke(.7.dp.toPx())
        onDrawWithContent {
            drawContent()
            if (size.minDimension > inner * 2) {
                drawRoundRect(metal, Offset(outer,outer), Size(size.width-outer*2,size.height-outer*2),
                    CornerRadius((radius-outer).coerceAtLeast(0f)), style=rim)
                drawRoundRect(Color(0xD9212429), Offset(inner,inner), Size(size.width-inner*2,size.height-inner*2),
                    CornerRadius((radius-inner).coerceAtLeast(0f)), style=separator)
            }
        }
    }

internal fun Modifier.posterSelectionShadow(enabled: Boolean, roundedDp: Float): Modifier =
    if (!enabled || !CatalogStyle.regular) this else shadow(
        elevation=4.dp, shape=RoundedCornerShape(roundedDp.dp), clip=false,
        ambientColor=Color.Black.copy(alpha=.18f), spotColor=Color.Black.copy(alpha=.24f))
