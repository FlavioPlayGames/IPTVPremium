package br.com.flaviotorres.iptvpremium.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun HomeMenuCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes imageResId: Int? = null,
    enabled: Boolean = true,
    slots: Int = 4
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWideScreen = screenWidthDp >= 700

    val slotWidth = if (isWideScreen) {
        ((screenWidthDp - 108) / slots).coerceIn(120, 158).dp
    } else {
        ((screenWidthDp - 64) / 2).coerceIn(96, 170).dp
    }
    val frameWidth = minOf(slotWidth, 158.dp)
    val frameHeight = frameWidth * 0.74f
    val verticalAdjustment = if (isWideScreen) (-22).dp else 0.dp
    val titleFontSize = if (isWideScreen) 15.sp else 14.sp

    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else .4f)
            .width(slotWidth)
            .offset(y = verticalAdjustment)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .focusable(enabled),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(width = frameWidth, height = frameHeight),
            contentAlignment = Alignment.Center
        ) {
            if (imageResId != null) {
                HomeMenuArtwork(
                    imageResId = imageResId,
                    contentDescription = title,
                    artworkSize = frameWidth
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))
        Box(Modifier.width(frameWidth * .46f).height(2.dp).background(
            if(isFocused) br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color() else Color.Transparent,
            androidx.compose.foundation.shape.RoundedCornerShape(50)))
        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = title.uppercase(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = titleFontSize,
                letterSpacing = 2.8.sp,
                color = if (isFocused) Color.White else Color.White,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.90f),
                    offset = Offset(x = 0f, y = 2f),
                    blurRadius = 6f
                )
            )
        )
    }
}


@Composable
private fun HomeMenuArtwork(@DrawableRes imageResId: Int, contentDescription: String,
    artworkSize: androidx.compose.ui.unit.Dp) {
    val pixels = with(LocalDensity.current) { (artworkSize * 1.12f).roundToPx() }.coerceAtLeast(1)
    Image(painter = br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource(imageResId,
        androidx.compose.ui.unit.IntSize(pixels, pixels)), contentDescription = contentDescription,
        modifier = Modifier.width(artworkSize).height(artworkSize * .74f), contentScale = ContentScale.Fit, alignment = Alignment.BottomCenter)
}
