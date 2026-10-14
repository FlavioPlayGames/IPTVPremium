package br.com.flaviotorres.iptvpremium.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@Composable
fun HomeMenuCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes imageResId: Int? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWideScreen = screenWidthDp >= 700

    val slotWidth = if (isWideScreen) {
        ((screenWidthDp - 108) / 4).coerceIn(120, 158).dp
    } else {
        ((screenWidthDp - 64) / 2).coerceIn(96, 170).dp
    }
    val frameWidth = minOf(slotWidth, 158.dp)
    val frameHeight = frameWidth * 0.74f
    val titleGap = 12.dp
    val verticalAdjustment = if (isWideScreen) (-22).dp else 0.dp
    val titleFontSize = if (isWideScreen) 15.sp else 14.sp

    val cardScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "HomeMenuCardScale"
    )

    Column(
        modifier = modifier
            .width(slotWidth)
            .offset(y = verticalAdjustment)
            .scale(cardScale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(),
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

        Spacer(modifier = Modifier.height(titleGap))

        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = titleFontSize,
                letterSpacing = 2.8.sp,
                color = if (isFocused) Color(0xFFFFD369) else Color(0xFFFFF8E8),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.90f),
                    offset = Offset(x = 0f, y = 2f),
                    blurRadius = 6f
                )
            )
        )
    }
}


/**
 * Decodificacao economica das 3 artes grandes da Home.
 *
 * Conserva ARGB_8888 (inclusive alfa), sem recomprimir os PNGs ou mudar
 * os dp da interface. 2x da imagem efetivamente desenhada + folga para
 * o aumento de 6% no foco. Cache fica restrito a 10 MiB na RAM.
 */
private object HomeMenuArtworkCache {
    private const val MAX_CACHE_KB = 10 * 1024
    private val decodeSlot = Semaphore(1)

    private val images = object : LruCache<String, ImageBitmap>(MAX_CACHE_KB) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            ((value.width.toLong() * value.height.toLong() * 4L + 1023L) / 1024L)
                .coerceAtLeast(1L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
    }

    private fun cacheKey(drawableId: Int, widthPx: Int, heightPx: Int): String =
        "$drawableId:$widthPx:$heightPx"

    fun cached(drawableId: Int, widthPx: Int, heightPx: Int): ImageBitmap? =
        images.get(cacheKey(drawableId, widthPx, heightPx))

    suspend fun load(
        context: Context,
        @DrawableRes drawableId: Int,
        widthPx: Int,
        heightPx: Int
    ): ImageBitmap? {
        val cacheKey = cacheKey(drawableId, widthPx, heightPx)
        images.get(cacheKey)?.let { return it }

        return withContext(Dispatchers.Default) {
            decodeSlot.withPermit {
                images.get(cacheKey)?.let { return@withPermit it }

                try {
                    val resources = context.applicationContext.resources
                    val bounds = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                        inScaled = false
                    }
                    BitmapFactory.decodeResource(resources, drawableId, bounds)

                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        return@withPermit null
                    }

                    var sample = 1
                    while (
                        sample < 128 &&
                        bounds.outWidth / (sample * 2) >= widthPx &&
                        bounds.outHeight / (sample * 2) >= heightPx
                    ) {
                        sample *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inScaled = false
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bitmap = BitmapFactory.decodeResource(
                        resources,
                        drawableId,
                        decodeOptions
                    )?.asImageBitmap()

                    if (bitmap != null) images.put(cacheKey, bitmap)
                    bitmap
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: OutOfMemoryError) {
                    images.evictAll()
                    null
                } catch (_: Exception) {
                    // Uma falha isolada de arte nao impede navegar pela Home.
                    null
                }
            }
        }
    }
}

@Composable
private fun HomeMenuArtwork(
    @DrawableRes imageResId: Int,
    contentDescription: String,
    artworkSize: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Margem de resolucao >2x para o zoom 1.06f, sem carregar 1254x1254
    // em uma TV onde a arte ocupa apenas 155 ou 200 dp.
    val requiredPixels = with(density) {
        (artworkSize * 2.12f).roundToPx()
    }.coerceAtLeast(1)

    // Ao reutilizar um composable com outra imagem, nunca mostramos a
    // imagem antiga enquanto a nova e carregada em background.
    key(imageResId, requiredPixels) {
        val bitmap by produceState<ImageBitmap?>(
            initialValue = HomeMenuArtworkCache.cached(
                imageResId, requiredPixels, requiredPixels
            ),
            key1 = imageResId,
            key2 = requiredPixels
        ) {
            value = HomeMenuArtworkCache.load(
                context.applicationContext,
                imageResId,
                requiredPixels,
                requiredPixels
            )
        }

        bitmap?.let { loaded ->
            Image(
                bitmap = loaded,
                contentDescription = contentDescription,
                modifier = Modifier.requiredSize(artworkSize),
                contentScale = ContentScale.Fit
            )
        }
    }
}
