package br.com.flaviotorres.iptvpremium.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val CinemaTitleFont = FontFamily(Font(R.font.cinema_title))
internal val CinemaTitleRed = Color(0xFFFF242E)
private val qualityOrPart = Regex(
    "(?<![\\p{L}\\p{N}])(?:\\[(?:DV|HDR\\+?)\\]|(?:HDR\\+?|DV)(?![\\p{L}\\p{N}])|PARTE\\s+\\d+(?![\\p{L}\\p{N}]))"
)
private val titleWord = Regex("[\\p{L}\\p{N}]+(?:[-’'][\\p{L}\\p{N}]+)*")

/** Markers are highlighted as complete tokens, never fragments of words. */
internal fun cinemaTitleText(title: String): AnnotatedString {
    val text = title.trim().replace(Regex("\\s+"), " ").uppercase(Locale.forLanguageTag("pt-BR"))
    val markers = qualityOrPart.findAll(text).map { it.range }.toList()
    val words = titleWord.findAll(text).filter { word ->
        markers.none { word.range.first <= it.last && word.range.last >= it.first }
    }.toList()
    return buildAnnotatedString {
        append(text)
        markers.forEach { addStyle(SpanStyle(color = CinemaTitleRed), it.first, it.last + 1) }
        if (words.size == 2) {
            val second = words[1].range
            addStyle(SpanStyle(color = CinemaTitleRed), second.first, second.last + 1)
        }
    }
}

@Composable
internal fun CinemaDetailsTitle(
    title: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = 2
) {
    val label = remember(title) { cinemaTitleText(title) }
    Text(
        text = label,
        modifier = modifier,
        style = TextStyle(
            fontFamily = CinemaTitleFont,
            fontSize = fontSize,
            lineHeight = fontSize * 1.18f,
            letterSpacing = 0.2.sp,
            color = Color.White,
            shadow = Shadow(Color(0xCC000000), Offset(1.5f, 2f), 2f)
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/** One local decode preserving definition up to a Full HD reference budget. */
internal object CinemaBackdropCache {
    @Volatile private var bitmap: ImageBitmap? = null
    private val mutex = Mutex()
    fun peek(): ImageBitmap? = bitmap
    suspend fun load(context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            bitmap ?: run {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.cinema_details_fallback, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 1920 && bounds.outHeight / (sample * 2) >= 1080) sample *= 2
                BitmapFactory.decodeResource(context.resources, R.drawable.cinema_details_fallback,
                    BitmapFactory.Options().apply {
                        inScaled = false
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    })?.asImageBitmap()?.also { bitmap = it }
            }
        }
    }
}

@Composable
internal fun CinemaDetailsBackdrop(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val image by produceState(CinemaBackdropCache.peek(), context) {
        value = CinemaBackdropCache.load(context)
    }
    image?.let { Image(it, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop,
        filterQuality = androidx.compose.ui.graphics.FilterQuality.High) }
}
