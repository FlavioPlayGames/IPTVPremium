package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors

/** Fine neon double rim and short colored halo, without blur layers on API 28. */
fun Modifier.reinforcedCatalogFocus(enabled: Boolean, roundedDp: Float = 12f): Modifier =
    if (!enabled) this else if (CatalogStyle.regular) this.drawWithCache {
        val color=CatalogStyle.selectionBorder
        val stroke=Stroke(1.2.dp.toPx())
        onDrawWithContent {
            drawContent()
            val inset=.8.dp.toPx()
            drawRoundRect(color, Offset(inset,inset), Size((size.width-inset*2).coerceAtLeast(0f),(size.height-inset*2).coerceAtLeast(0f)),
                CornerRadius(roundedDp.dp.toPx()), style=stroke)
        }
    } else drawWithCache {
        // Increase color saturation and halo intensity by 25%; keep geometry unchanged.
        // The luminous core already has maximum HSV value, so brightness gain is in the halo.
        val intensity = 1.25f
        val accent = ThemeColors.tone(1f, (.90f * intensity).coerceAtMost(1f))
        val reflection = ThemeColors.tone(1f, (.42f * intensity).coerceAtMost(1f))
        val metal = Brush.linearGradient(
            0f to reflection, .18f to accent, .48f to reflection,
            .72f to accent, 1f to reflection,
            start = Offset.Zero, end = Offset(size.width, size.height)
        )
        val radius = roundedDp.dp.toPx()
        val outerInset = .7.dp.toPx()
        val gapInset = 1.6.dp.toPx()
        val innerInset = 2.3.dp.toPx()
        // Precompute strokes; only the focused item draws this small static halo.
        val halo = listOf(6f to .025f, 4.5f to .045f, 3f to .09f, 2f to .14f)
            .map { (width, alpha) -> Stroke(width.dp.toPx()) to accent.copy(alpha = (alpha * intensity).coerceAtMost(1f)) }
        val outerStroke = Stroke(1.2.dp.toPx())
        val separatorStroke = Stroke(.7.dp.toPx())
        val innerStroke = Stroke(.6.dp.toPx())
        onDrawWithContent {
            drawContent()
            if (size.width > innerInset * 2 && size.height > innerInset * 2) {
                for ((stroke, color) in halo) {
                    drawRoundRect(color, topLeft = Offset(outerInset, outerInset),
                        size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                        cornerRadius = CornerRadius((radius - outerInset).coerceAtLeast(0f)), style = stroke)
                }
                drawRoundRect(metal, topLeft = Offset(outerInset, outerInset),
                    size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                    cornerRadius = CornerRadius((radius - outerInset).coerceAtLeast(0f)), style = outerStroke)
                drawRoundRect(Color(0xB3000000), topLeft = Offset(gapInset, gapInset),
                    size = Size(size.width - gapInset * 2, size.height - gapInset * 2),
                    cornerRadius = CornerRadius((radius - gapInset).coerceAtLeast(0f)), style = separatorStroke)
                drawRoundRect(metal, topLeft = Offset(innerInset, innerInset),
                    size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                    cornerRadius = CornerRadius((radius - innerInset).coerceAtLeast(0f)), style = innerStroke)
            }
        }
    }
