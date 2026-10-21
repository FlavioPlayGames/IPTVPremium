package br.com.flaviotorres.iptvpremium.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource as painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.components.HomeMenuCard

// Mesmo dourado metálico usado no contorno dos ícones principais.
private val MetallicGoldBrush get() = Brush.linearGradient(
    colors = listOf(
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF8A6414),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD4AF37),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE991),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFF4BD),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFE3B83F),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF9E761B)
    )
)

@Composable
fun HomeScreen(
    onLiveTvClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onSeriesClick: () -> Unit,
    onListaClick: () -> Unit = {},
    onAtualizarClick: () -> Unit = {},
    onConfiguracoesClick: () -> Unit = {},
    onKidsClick: () -> Unit = {},
    normalEnabled: Boolean = true
) {
    MarbledHomeScreen(onLiveTvClick,onMoviesClick,onSeriesClick,onKidsClick,onListaClick,
        onAtualizarClick,onConfiguracoesClick,normalEnabled)
}

@Composable
private fun HomeTopIconButton(
    imageResId: Int,
    contentDescription: String,
    isWideScreen: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val touchAreaSize =
        if (isWideScreen) {
            46.dp
        } else {
            40.dp
        }

    val iconSize =
        if (isWideScreen) {
            33.dp
        } else {
            28.6.dp
        }

    // Fundo transparente; foco indicado pelo aumento suave do ícone.
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(touchAreaSize)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        HomeScaledResourceImage(
            imageResId = imageResId,
            contentDescription = contentDescription,
            targetWidth = iconSize * 1.12f,
            targetHeight = iconSize * 1.12f,
            modifier = Modifier
                .size(iconSize)
                .scale(if (isFocused) 1.12f else 1f),
            contentScale = ContentScale.Fit
        )
    }
}

/** Home artwork is sampled once at physical pixel size, with headroom for focus. */
@Composable
private fun HomeScaledResourceImage(
    @DrawableRes imageResId: Int,
    contentDescription: String?,
    targetWidth: Dp,
    targetHeight: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val density = LocalDensity.current
    val size = with(density) { androidx.compose.ui.unit.IntSize(
        (targetWidth * 1.12f).roundToPx().coerceAtLeast(1), (targetHeight * 1.12f).roundToPx().coerceAtLeast(1)) }
    Image(painter = painterResource(imageResId, size), contentDescription = contentDescription,
        modifier = modifier, contentScale = contentScale)
}


@Composable
private fun PreparingExperiencePanel() {
    val accent = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD4AF37)
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "experience")
    val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "experience-ring")
    Box(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF060608)),
        contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(accent.copy(alpha = 0.13f), Color.Transparent))))
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            androidx.compose.foundation.Canvas(Modifier.size(38.dp)) {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx())
                val inset = stroke.width
                drawCircle(accent.copy(alpha = 0.16f), radius = size.minDimension / 2f - inset, style = stroke)
                drawArc(accent, angle, 105f, false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset), style = stroke)
            }
            androidx.tv.material3.Text("Preparando sua experiência…", color = Color.White,
                style = MaterialTheme.typography.titleMedium)
            Box(Modifier.width(160.dp).height(1.dp).background(MetallicGoldBrush))
        }
    }
}
