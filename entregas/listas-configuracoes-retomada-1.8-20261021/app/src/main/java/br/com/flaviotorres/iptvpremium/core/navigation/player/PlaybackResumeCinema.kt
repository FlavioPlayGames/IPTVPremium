package br.com.flaviotorres.iptvpremium.core.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.ui.movies.MovieDetailsImageLoader
import java.util.Locale

/** Resume confirmation only: callbacks retain their original navigation and persistence. */
@Composable
fun PlaybackResumeCinema(
    media: PlaybackMedia,
    positionMs: Long,
    backgroundUrl: String?,
    seriesTitle: String?,
    durationMs: Long? = media.durationMs,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current.applicationContext
    val configuration = LocalConfiguration.current
    val compact = configuration.screenWidthDp < 700
    // A moderate decoded size is sufficient behind a light GPU blur.
    val imageUrl = backgroundUrl?.takeIf { it.isNotBlank() }
        ?: media.backdropUrl?.takeIf { it.isNotBlank() }
    // Never enlarge a portrait cover or episode thumbnail as a background.
    val image by produceState<ImageBitmap?>(null, imageUrl, context) {
        value = null
        value = MovieDetailsImageLoader.load(context, imageUrl, 1280, 720, "retomada_backdrop")
    }
    val continueFocus = remember { FocusRequester() }
    LaunchedEffect(media.id) {
        withFrameNanos { }
        runCatching { continueFocus.requestFocus() }
    }
    val gold = PlayerChrome.color(0xFFD4AF37)
    val frame = resumeCatalogBorder()
    val name = if (media.type == PlaybackContentType.EPISODE) {
        seriesTitle?.trim()?.takeIf { it.isNotBlank() }
            ?: media.subtitleSearchTitle?.trim()?.takeIf { it.isNotBlank() } ?: media.title
    } else media.title
    val episodeLabel = if (media.type == PlaybackContentType.EPISODE) buildList {
        media.seasonNumber?.let { add(if (it == 0) "Especiais" else "Temporada $it") }
        media.episodeNumber?.takeIf { it > 0 }?.let { add("Episódio $it") }
    }.joinToString(" · ") else ""
    val duration = durationMs?.takeIf { it > 0L }
    val shape = RoundedCornerShape(10.dp)

    Box(Modifier.fillMaxSize().background(PlayerChrome.surface(0xFF090A0B)), contentAlignment = Alignment.Center) {
        image?.let { bitmap ->
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(6.dp))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.78f))
        )))
        Column(
            modifier = Modifier.padding(16.dp).widthIn(max = 480.dp).fillMaxWidth()
                .heightIn(max = (configuration.screenHeightDp - 32).coerceAtLeast(120).dp)
                .shadow(8.dp, shape, ambientColor = gold.copy(alpha = 0.12f), spotColor = gold.copy(alpha = 0.12f))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(PlayerChrome.glassColor(0xE617140F), PlayerChrome.surface(0xE60B0C10), PlayerChrome.surface(0xEB111014))))
                .border(1.2.dp, frame, shape)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 20.dp else 26.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (media.type == PlaybackContentType.EPISODE) "RETOMAR EPISÓDIO" else "CONTINUAR ASSISTINDO",
                color = gold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            ResumeCatalogDivider()
            Text(name, color = Color.White, fontSize = if (compact) 18.sp else 20.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            if (episodeLabel.isNotBlank()) Text(episodeLabel, color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(2.dp))
            // Read-only resume timeline; uses real catalog duration, never an invented total.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(resumeCinemaTime(positionMs), color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp)
                val progress = duration?.let { (positionMs.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }
                Canvas(Modifier.weight(1f).height(22.dp)) {
                    val inset = 5.dp.toPx(); val y = size.height / 2f
                    val right = (size.width - inset).coerceAtLeast(inset)
                    drawLine(Color.White.copy(alpha = 0.32f), Offset(inset, y), Offset(right, y), 2.dp.toPx(), StrokeCap.Round)
                    if (progress != null) {
                        val end = inset + (right - inset) * progress
                        drawLine(Brush.horizontalGradient(listOf(PlayerChrome.borderColor(0xFF9E761B), PlayerChrome.borderColor(0xFFD4AF37), PlayerChrome.borderColor(0xFFFFE991))), Offset(inset, y), Offset(end, y), 2.dp.toPx(), StrokeCap.Round)
                        drawCircle(Brush.radialGradient(listOf(PlayerChrome.color(0x42EFC052), Color.Transparent), Offset(end, y), 8.dp.toPx()), 8.dp.toPx(), Offset(end, y))
                        drawCircle(Brush.radialGradient(listOf(PlayerChrome.color(0xDDFFF4BD), PlayerChrome.color(0xCCE3B83F), PlayerChrome.color(0xBB9E761B)), Offset(end, y), 4.dp.toPx()), 4.dp.toPx(), Offset(end, y))
                    }
                }
                Text(duration?.let(::resumeCinemaTime) ?: "--:--", color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CinemaResumeButton("Continuar", false, Modifier.weight(1f), continueFocus, onContinue)
                CinemaResumeButton("Do início", true, Modifier.weight(1f), null, onRestart)
            }
        }
    }
}

@Composable
private fun CinemaResumeButton(label: String, restart: Boolean, modifier: Modifier, focusRequester: FocusRequester?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val gold = PlayerChrome.color(0xFFD4AF37)
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier.then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .shadow(if (focused) 4.dp else 0.dp, shape, ambientColor = gold.copy(alpha = 0.18f), spotColor = gold.copy(alpha = 0.18f))
            .clip(shape)
            .background(Brush.verticalGradient(if (focused) if(PlayerChrome.kids)br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.colors()else listOf(Color.White.copy(alpha=.12f),Color.White.copy(alpha=.08f)) else listOf(PlayerChrome.surface(0x70111419), PlayerChrome.surface(0x700B0C10))))
            .border(if (focused) 1.8.dp else 1.2.dp, resumeCatalogBorder(), shape)

            .clickable(onClick = onClick).height(44.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
            if (restart) {
                drawArc(gold, -80f, 285f, false, Offset(size.width * 0.13f, size.height * 0.13f), Size(size.width * 0.74f, size.height * 0.74f), style = stroke)
                val arrow = Path().apply {
                    moveTo(size.width * 0.53f, size.height * 0.02f)
                    lineTo(size.width * 0.40f, size.height * 0.16f)
                    lineTo(size.width * 0.59f, size.height * 0.28f)
                }
                drawPath(arrow, gold, style = stroke)
            } else {
                val path = Path().apply {
                    moveTo(size.width * 0.23f, size.height * 0.13f)
                    lineTo(size.width * 0.84f, size.height * 0.5f)
                    lineTo(size.width * 0.23f, size.height * 0.87f)
                    close()
                }
                drawPath(path, gold, style = stroke)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (focused) Color.White else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun resumeCinemaTime(positionMs: Long): String {
    val seconds = positionMs.coerceAtLeast(0L) / 1000L
    return if (seconds >= 3600L) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600L, seconds / 60L % 60L, seconds % 60L)
    else String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L)
}

// Exact border palette and ornament used by the Movies catalog.
private fun resumeCatalogBorder() = Brush.linearGradient(listOf(
    PlayerChrome.borderColor(0xFF7A5215), PlayerChrome.borderColor(0xFFFFE6A0), PlayerChrome.borderColor(0xFFD6A32E), PlayerChrome.borderColor(0xFFFFF5CD), PlayerChrome.borderColor(0xFF926315)
))

@Composable
private fun ResumeCatalogDivider() {
    val gold = PlayerChrome.color(0xFFD4AF37)
    Row(Modifier.fillMaxWidth().height(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, gold))))
        Text("◇✦◇", color = gold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(gold, Color.Transparent))))
    }
}

// IPTV_RESUME_PANORAMA_V2

// IPTV_RESUME_CATALOG_V3

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_POLISH_LOADING_HOLD_V1
