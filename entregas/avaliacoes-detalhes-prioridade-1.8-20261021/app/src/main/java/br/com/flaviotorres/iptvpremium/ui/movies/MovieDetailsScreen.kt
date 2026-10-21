package br.com.flaviotorres.iptvpremium.ui.movies

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.roundToInt

private val CinzelSemiBold = FontFamily(
    Font(
        resId = R.font.cinzel_semibold,
        weight = FontWeight.SemiBold
    )
)

private val MetallicGoldBrush get() = Brush.linearGradient(
    colors = listOf(
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF8A6414),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFD4AF37),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFE991),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFF4BD),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFE3B83F),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF9E761B)
    )
)

enum class MovieRatingSource {
    IMDB,
    TMDB,
    PROVIDER
}

data class MovieCastMember(
    val id: String,
    val name: String,
    val profileUrl: String? = null,
    val characterName: String? = null
)

/**
 * Resultado do preparo visual de um filme.
 * Nenhuma URL, credencial ou dado privado e registrado nos logs.
 */
data class MovieDetailsPreloadResult(
    val backdropReady: Boolean,
    val titleLogoReady: Boolean,
    val castPhotosReady: Int,
    val castPhotosRequested: Int
)

/**
 * Prepara as imagens da primeira tela ANTES de navegar ate MovieDetailsScreen.
 *
 * Usa exatamente o mesmo carregador e caches da tela, sem downloads duplicados.
 * O chamador decide quando navegar e deve aplicar um prazo maximo para nao
 * bloquear a grade indefinidamente caso a rede ou uma imagem falhe.
 *
 * Este metodo nao consulta o TMDB: recebe as URLs ja resolvidas para o filme.
 */
suspend fun preloadMovieDetailsImages(
    context: Context,
    backdropUrl: String?,
    titleLogoUrl: String?,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    castProfileUrls: List<String?> = emptyList()
): MovieDetailsPreloadResult = coroutineScope {
    val appContext = context.applicationContext
    val startedAt = SystemClock.elapsedRealtime()
    val widthPx = viewportWidthPx.coerceAtLeast(1)
    val heightPx = viewportHeightPx.coerceAtLeast(1)
    val density = appContext.resources.displayMetrics.density.coerceAtLeast(0.1f)
    val compactLayout =
        heightPx / density < 500f || widthPx / density < 800f

    // Mesmas dimensoes da MovieDetailsScreen: assim o primeiro desenho
    // encontra os bitmaps ja decodificados no cache RAM.
    val logoWidthPx = ((if (compactLayout) 250 else 300) * density)
        .roundToInt().coerceAtLeast(1)
    val logoHeightPx = ((if (compactLayout) 64 else 80) * density)
        .roundToInt().coerceAtLeast(1)
    val castWidthPx = ((if (compactLayout) 96 else 108) * density)
        .roundToInt().coerceAtLeast(1)
    val castHeightPx = ((if (compactLayout) 132 else 148) * density)
        .roundToInt().coerceAtLeast(1)

    // So fotos da primeira faixa do elenco, nunca a filmografia completa.
    val firstCastUrls = castProfileUrls
        .asSequence()
        .filterNotNull()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(12)
        .toList()

    val backdropTask = async {
        MovieDetailsImageLoader.load(
            context = appContext,
            imageUrl = backdropUrl,
            targetWidthPx = widthPx,
            targetHeightPx = heightPx,
            imageType = "preload_backdrop"
        ) != null
    }
    val logoTask = async {
        MovieDetailsImageLoader.load(
            context = appContext,
            imageUrl = titleLogoUrl,
            targetWidthPx = logoWidthPx,
            targetHeightPx = logoHeightPx,
            imageType = "preload_logo"
        ) != null
    }
    // A arte principal tem prioridade sobre as fotos do elenco.
    val backdropReady = backdropTask.await()
    val titleLogoReady = logoTask.await()

    // Do not spend the selected film's opening window downloading twelve actor
    // portraits. The visible cast cards load normally after the details are shown.
    val castReady = firstCastUrls.count { url ->
        MovieDetailsImageLoader.peekFromMemory(appContext, url, castWidthPx, castHeightPx) != null
    }

    Log.i(
        "MovieDetailsPreload",
        "backdropReady=$backdropReady logoReady=$titleLogoReady " +
                "castCache=$castReady/${firstCastUrls.size} castRedeNaAbertura=0 " +
                "tempoMs=${SystemClock.elapsedRealtime() - startedAt}"
    )

    MovieDetailsPreloadResult(
        backdropReady = backdropReady,
        titleLogoReady = titleLogoReady,
        castPhotosReady = castReady,
        castPhotosRequested = firstCastUrls.size
    )
}

@Composable
fun MovieDetailsScreen(
    movieName: String = "Filme",
    tmdbId: String? = null,
    imdbId: String? = null,
    backdropUrl: String? = null,
    titleLogoUrl: String? = null,
    rating: Double? = null,
    ratingSource: MovieRatingSource? = null,
    year: Int? = null,
    durationSeconds: Int? = null,
    genres: List<String> = emptyList(),
    plot: String? = null,
    cast: List<MovieCastMember> = emptyList(),
    isFavorite: Boolean = false,
    onWatchClick: () -> Unit = {},
    onTrailerClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onCastMemberClick: (MovieCastMember) -> Unit = {},
    ratingPlaylistId: String? = null,
    ratingContentId: String? = null,
    ratingIdentityTitle: String? = null,
    ratingIdentityYear: Int? = null
) {
    var favoriteState by remember(isFavorite) {
        mutableStateOf(isFavorite)
    }
    var showFullSynopsis by remember(movieName, plot) { mutableStateOf(false) }
    var synopsisPreviewTruncated by remember(movieName, plot) { mutableStateOf(false) }
    BackHandler(enabled = showFullSynopsis) { showFullSynopsis = false }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070708))
    ) {
        val compactLayout =
            maxHeight < 500.dp ||
                    maxWidth < 800.dp

        val denseHeader = compactLayout && maxHeight < 430.dp && cast.isNotEmpty()
        var measuredCastHeight by remember(cast.isNotEmpty(), compactLayout) { mutableStateOf(0) }
        val headerDensity = LocalDensity.current
        val reservedCastHeight = if(cast.isEmpty()) 0.dp else if(measuredCastHeight > 0) {
            with(headerDensity) { measuredCastHeight.toDp() }
        } else if(compactLayout) 200.dp else 220.dp
        val maxHeaderHeight = (maxHeight - (if(compactLayout)32.dp else 48.dp) -
            reservedCastHeight - (if(cast.isNotEmpty())8.dp else 0.dp)).coerceAtLeast(0.dp)
        val context = LocalContext.current.applicationContext
        var primaryLogoFailed by remember(titleLogoUrl) { mutableStateOf(false) }
        var primaryBackdropFailed by remember(backdropUrl) { mutableStateOf(false) }
        var fanart by remember(movieName, tmdbId, imdbId) {
            mutableStateOf(br.com.flaviotorres.iptvpremium.data.repository.MovieFanartArtwork())
        }
        val needsFanart = titleLogoUrl.isNullOrBlank() || backdropUrl.isNullOrBlank() || primaryLogoFailed || primaryBackdropFailed
        androidx.compose.runtime.LaunchedEffect(needsFanart, tmdbId, imdbId) {
            if (needsFanart && (!tmdbId.isNullOrBlank() || !imdbId.isNullOrBlank())) {
                fanart = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                    br.com.flaviotorres.iptvpremium.data.repository.loadMovieFanartArtwork(tmdbId, imdbId,
                        br.com.flaviotorres.iptvpremium.data.remote.ArtworkHttpCache.getInstance(context)::get)
                } ?: br.com.flaviotorres.iptvpremium.data.repository.MovieFanartArtwork()
            }
        }
        val backdropCandidates = (listOfNotNull(backdropUrl?.takeUnless { primaryBackdropFailed }) + fanart.backdrops).distinct()
        val logoCandidates = (listOfNotNull(titleLogoUrl?.takeUnless { primaryLogoFailed }) + fanart.logos).distinct()

        val density = LocalDensity.current

        // Decodifica o backdrop no tamanho da área efetiva da tela.
        // O arquivo original permanece em disco; a memória guarda a versão
        // apropriada para esta TV, sem alterar o FillBounds aprovado.
        val backdropWidthPx = with(density) {
            maxWidth.roundToPx().coerceAtLeast(1)
        }
        val backdropHeightPx = with(density) {
            maxHeight.roundToPx().coerceAtLeast(1)
        }

        val titleLogoWidth =
            if (compactLayout) 250.dp else 300.dp
        val titleLogoHeight =
            if (compactLayout) 64.dp else 80.dp // Match the preparation cache; display can be smaller.

        val titleLogoWidthPx = with(density) {
            titleLogoWidth.roundToPx().coerceAtLeast(1)
        }
        val titleLogoHeightPx = with(density) {
            titleLogoHeight.roundToPx().coerceAtLeast(1)
        }

        val backdropBitmap by androidx.compose.runtime.key(backdropCandidates, backdropWidthPx, backdropHeightPx) {
            produceState<ImageBitmap?>(
            initialValue = backdropCandidates.firstNotNullOfOrNull {
                MovieDetailsImageLoader.peekFromMemory(context, it, backdropWidthPx, backdropHeightPx) },
            key1 = backdropCandidates, key2 = backdropWidthPx to backdropHeightPx
        ) {
            for (url in backdropCandidates) {
                val bitmap = MovieDetailsImageLoader.load(context, url, backdropWidthPx, backdropHeightPx, "backdrop")
                if (bitmap != null) { value = bitmap; break }
            }
            if (value == null && !backdropUrl.isNullOrBlank()) primaryBackdropFailed = true
        }
        }
        val titleLogoBitmap by androidx.compose.runtime.key(logoCandidates, titleLogoWidthPx, titleLogoHeightPx) {
            produceState<ImageBitmap?>(
            initialValue = logoCandidates.firstNotNullOfOrNull {
                MovieDetailsImageLoader.peekFromMemory(context, it, titleLogoWidthPx, titleLogoHeightPx) },
            key1 = logoCandidates, key2 = titleLogoWidthPx to titleLogoHeightPx
        ) {
            for (url in logoCandidates) {
                val bitmap = MovieDetailsImageLoader.load(context, url, titleLogoWidthPx, titleLogoHeightPx, "logo")
                if (bitmap != null) { value = bitmap; break }
            }
            if (value == null && !titleLogoUrl.isNullOrBlank()) primaryLogoFailed = true
        }
        }

        Box(
            modifier = Modifier.fillMaxSize().then(
                if (showFullSynopsis) Modifier.blur(16.dp) else Modifier
            )
        ) {
            if (backdropBitmap != null) {
                Image(
                    bitmap = backdropBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF080809),
                                    Color(0xFF0B0B0D),
                                    Color(0xFF111113)
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xE6000000),
                                0.38f to Color(0xB3000000),
                                0.68f to Color(0x3D000000),
                                1.00f to Color(0x10000000)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x10000000),
                                Color.Transparent,
                                Color(0x66000000)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal =
                            if (compactLayout) {
                                12.dp
                            } else {
                                16.dp
                            },
                        vertical =
                            if (compactLayout) {
                                16.dp
                            } else {
                                24.dp
                            }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .heightIn(max = maxHeaderHeight)
                        .verticalScroll(rememberScrollState())
                    // A linha de metadados usa a largura toda para não cortar
                    // os gêneros. Sinopse e título alternativo continuam compactos.
                ) {
                    if (titleLogoBitmap != null) {
                        Image(
                            bitmap = titleLogoBitmap!!,
                            contentDescription = movieName,
                            modifier = Modifier
                                .width(
                                    if (compactLayout) {
                                        250.dp
                                    } else {
                                        300.dp
                                    }
                                )
                                .height(
                                    if (denseHeader) {
                                        32.dp
                                    } else if (compactLayout) {
                                        50.dp
                                    } else {
                                        72.dp
                                    }
                                ),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart
                        )
                    } else {
                        Text(
                            text = movieName.uppercase(),
                            modifier = Modifier.fillMaxWidth(if (compactLayout) 0.78f else 0.60f),
                            style = TextStyle(
                                fontFamily = CinzelSemiBold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize =
                                    if (denseHeader) {
                                        20.sp
                                    } else if (compactLayout) {
                                        28.sp
                                    } else {
                                        32.sp
                                    },
                                brush = MetallicGoldBrushText
                            ),
                            maxLines = if(denseHeader)1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(
                            if (denseHeader) {
                                3.dp
                            } else if (compactLayout) {
                                7.dp
                            } else {
                                9.dp
                            }
                        )
                    )

                    br.com.flaviotorres.iptvpremium.ui.components.ReferenceMetadataRow(
                        rating = rating, year = year, durationSeconds = durationSeconds,
                        genres = genres, compact = compactLayout, dense = denseHeader
                    )

                    Spacer(
                        modifier = Modifier.height(
                            if (denseHeader) {
                                3.dp
                            } else if (compactLayout) {
                                8.dp
                            } else {
                                10.dp
                            }
                        )
                    )

                    if (!plot.isNullOrBlank()) {
                        Text(
                            text = plot,
                            modifier = Modifier.fillMaxWidth(if (compactLayout) 0.78f else 0.60f)
                                .then(if(denseHeader) Modifier.clickable(onClickLabel="Ler sinopse completa") {showFullSynopsis=true} else Modifier),
                            color = br.com.flaviotorres.iptvpremium.ui.components.ReferenceDetailsPalette.text,
                            fontSize =
                                if (compactLayout) {
                                    13.sp
                                } else {
                                    14.sp
                                },
                            lineHeight =
                                if (compactLayout) {
                                    15.sp
                                } else {
                                    18.sp
                                },
                            maxLines = if(denseHeader)1 else if (compactLayout) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                val truncated = result.hasVisualOverflow
                                if (synopsisPreviewTruncated != truncated) {
                                    synopsisPreviewTruncated = truncated
                                }
                            }
                        )

                        if (synopsisPreviewTruncated && !denseHeader) {
                            Spacer(modifier = Modifier.height(3.dp))
                            MovieDetailsTextAction(
                                label = "Ler sinopse completa",
                                onClick = { showFullSynopsis = true }
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(
                                if (compactLayout) {
                                    8.dp
                                } else {
                                    10.dp
                                }
                            )
                        )
                    }

                    br.com.flaviotorres.iptvpremium.ui.components.ReferenceDetailActions(
                        watchText = "ASSISTIR", compact = compactLayout, isFavorite = favoriteState,
                        onWatch = onWatchClick,
                        onFavorite = { favoriteState = !favoriteState; onFavoriteClick() },
                        kind = "movies", playlist = ratingPlaylistId, content = ratingContentId,
                        tmdb = tmdbId, imdb = imdbId, dense = denseHeader, identityTitle = ratingIdentityTitle, identityYear = ratingIdentityYear
                    )
                }

                if (cast.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .onSizeChanged { if(measuredCastHeight != it.height) measuredCastHeight = it.height }
                            .padding(
                                bottom =
                                    if (compactLayout) {
                                        4.dp
                                    } else {
                                        6.dp
                                    }
                            )
                    ) {
                        Text(
                            text = "ELENCO",
                            style = TextStyle(
                                fontFamily = CinzelSemiBold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize =
                                    if (compactLayout) {
                                        16.sp
                                    } else {
                                        18.sp
                                    },
                                brush = MetallicGoldBrushText
                            )
                        )

                        Spacer(
                            modifier = Modifier.height(
                                if (compactLayout) {
                                    7.dp
                                } else {
                                    8.dp
                                }
                            )
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            // Reserva espaco real para o crescimento de 4% do card
                            // selecionado. Sem essa margem, o LazyRow pode cortar
                            // as extremidades da borda ao receber foco.
                            contentPadding = PaddingValues(
                                horizontal = 4.dp,
                                vertical = 4.dp
                            ),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    if (compactLayout) {
                                        9.dp
                                    } else {
                                        11.dp
                                    }
                                )
                        ) {
                            items(
                                items = cast,
                                key = { member ->
                                    member.id
                                }
                            ) { member ->
                                CastCard(
                                    member = member,
                                    compactLayout = compactLayout,
                                    onClick = {
                                        onCastMemberClick(
                                            member
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } // Final da camada da tela que recebe desfoque.

        if (fanart.logos.isNotEmpty() || fanart.backdrops.isNotEmpty()) {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Text("fanart.tv", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    .clickable { uriHandler.openUri("https://fanart.tv") })
        }
        if (showFullSynopsis && !plot.isNullOrBlank()) {
            Dialog(
                onDismissRequest = { showFullSynopsis = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                MovieSynopsisOverlay(
                    title = movieName,
                    synopsis = plot,
                    onClose = { showFullSynopsis = false }
                )
            }
        }
    }
}

/** Acao discreta dourada: usada no resumo e no fechamento da sinopse. */
@Composable
private fun MovieDetailsTextAction(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = label,
        color = if (focused) Color.White else Color.Unspecified,
        style = if (focused) TextStyle.Default else TextStyle(brush = (if (focused) Brush.linearGradient(listOf(Color.White, Color.White)) else MetallicGoldBrushText)),
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) 1.dp else 0.dp,
                MetallicGoldBrush,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

/**
 * Sinopse em destaque sobre a tela desfocada, com FECHAR focado desde a abertura.
 * As setas verticais rolam o texto sem retirar o foco do unico botao.
 */
@Composable
private fun MovieSynopsisOverlay(
    title: String,
    synopsis: String,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scrollScope = rememberCoroutineScope()
    val closeFocus = remember { FocusRequester() }

    LaunchedEffect(closeFocus) {
        withFrameNanos { }
        closeFocus.requestFocus()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x5203070D)),
        contentAlignment = Alignment.Center
    ) {
        val tight = maxWidth < 860.dp || maxHeight < 550.dp
        val synopsisScrollMaxHeight = maxHeight * 0.53f
        Column(
            modifier = Modifier
                .fillMaxWidth(if (tight) 0.90f else 0.82f)
                .widthIn(max = 1050.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) false
                    else when (event.key) {
                        Key.DirectionDown -> {
                            if (scrollState.value < scrollState.maxValue) {
                                scrollScope.launch { scrollState.scrollBy(110f) }
                            }
                            true // nao move o foco para outra opcao
                        }
                        Key.DirectionUp -> {
                            if (scrollState.value > 0) {
                                scrollScope.launch { scrollState.scrollBy(-110f) }
                            }
                            true
                        }
                        else -> false
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = if (tight) 22.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(if (tight) 14.dp else 22.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = synopsisScrollMaxHeight)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = synopsis,
                    color = Color.White,
                    fontSize = if (tight) 14.sp else 17.sp,
                    lineHeight = if (tight) 19.sp else 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(if (tight) 12.dp else 20.dp))
            MovieDetailsTextAction(
                label = "FECHAR",
                onClick = onClose,
                focusRequester = closeFocus
            )
        }
    }
}

@Composable
private fun MovieMetadataRow(
    rating: Double?,
    ratingSource: MovieRatingSource?,
    year: Int?,
    durationSeconds: Int?,
    genres: List<String>,
    compactLayout: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 5.dp else 6.dp)
    ) {
        if (rating != null) {
            // Alinha estrelas e nota pela mesma linha de base.
            Text(
                text = ratingStars(rating),
                modifier = Modifier.alignByBaseline(),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC928),
                fontSize =
                    if (compactLayout) {
                        14.sp
                    } else {
                        16.sp
                    }
            )

            Text(
                text = formatRatingValue(rating),
                modifier = Modifier.alignByBaseline(),
                color = Color.White,
                fontSize =
                    if (compactLayout) {
                        12.sp
                    } else {
                        13.sp
                    },
                fontWeight = FontWeight.Bold
            )

            when (ratingSource) {
                MovieRatingSource.IMDB -> RatingSourceChip(
                    text = "IMDb",
                    compactLayout = compactLayout
                )

                MovieRatingSource.TMDB -> Unit // Sem marca de fornecedor na interface.

                MovieRatingSource.PROVIDER,
                null -> Unit
            }

            if (
                year != null ||
                durationSeconds != null ||
                genres.isNotEmpty()
            ) {
                MetadataSeparator()
            }
        }

        if (year != null) {
            Text(
                text = year.toString(),
                color = Color.White,
                fontSize =
                    if (compactLayout) {
                        13.sp
                    } else {
                        15.sp
                    },
                fontWeight = FontWeight.Bold
            )

            if (
                durationSeconds != null ||
                genres.isNotEmpty()
            ) {
                MetadataSeparator()
            }
        }

        if (durationSeconds != null) {
            Text(
                text = formatDuration(durationSeconds),
                color = Color.White,
                fontSize =
                    if (compactLayout) {
                        13.sp
                    } else {
                        15.sp
                    },
                fontWeight = FontWeight.Bold
            )

            if (genres.isNotEmpty()) {
                MetadataSeparator()
            }
        }

        if (genres.isNotEmpty()) {
            Text(
                text = genres.joinToString(" • "),
                color = Color.White,
                fontSize =
                    if (compactLayout) {
                        13.sp
                    } else {
                        15.sp
                    },
                fontWeight = FontWeight.Bold
                // Sem corte: se faltar largura, os gêneros quebram de linha.
            )
        }
    }
}

@Composable
private fun MetadataSeparator() {
    Text(
        text = "•",
        color = Color(0xFF74747A),
        fontSize = 12.sp
    )
}

@Composable
private fun RatingSourceChip(
    text: String,
    compactLayout: Boolean
) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFFD7AF2D),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(
                horizontal =
                    if (compactLayout) 7.dp else 8.dp,
                vertical = 3.dp
            )
    ) {
        Text(
            text = text,
            color = Color(0xFF111113),
            fontSize =
                if (compactLayout) {
                    8.sp
                } else {
                    9.sp
                },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    br.com.flaviotorres.iptvpremium.ui.components.MediaActionButton(text, compactLayout, onClick)
}

@Composable
private fun SecondaryActionButton(
    text: String,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    // TRAILER e ASSISTIR compartilham dimensões, dourado e estados de foco.
    PrimaryActionButton(
        text = text,
        compactLayout = compactLayout,
        onClick = onClick
    )
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val buttonSize = if (compactLayout) 36.dp else 40.dp

    Box(
        modifier = Modifier
            .size(buttonSize)
            // O clique não produz ripple retangular sobre a imagem de fundo.
            // O próprio coração é o único indicador visual de foco.
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        FavoriteHeart(
            isFavorite = isFavorite,
            isFocused = isFocused,
            size = if (compactLayout) 26 else 29
        )
    }
}

@Composable
private fun CastCard(
    member: MovieCastMember,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardWidth = if (compactLayout) 96.dp else 108.dp
    val cardHeight = if (compactLayout) 132.dp else 148.dp
    val shape = RoundedCornerShape(if (compactLayout) 8.dp else 10.dp)

    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val imageWidthPx = with(density) { cardWidth.roundToPx().coerceAtLeast(1) }
    val imageHeightPx = with(density) { cardHeight.roundToPx().coerceAtLeast(1) }

    val initialProfile = remember(member.profileUrl, imageWidthPx, imageHeightPx) {
        MovieDetailsImageLoader.peekFromMemory(
            context, member.profileUrl, imageWidthPx, imageHeightPx
        )
    }
    val profileBitmap by produceState<ImageBitmap?>(
        initialValue = initialProfile,
        key1 = member.profileUrl,
        key2 = imageWidthPx to imageHeightPx
    ) {
        value = MovieDetailsImageLoader.load(
            context = context,
            imageUrl = member.profileUrl,
            targetWidthPx = imageWidthPx,
            targetHeightPx = imageHeightPx,
            imageType = "ator"
        )
    }

    // Mesma regra da filmografia: centraliza nomes curtos; faz marquee
    // somente para nomes que ultrapassam a largura e estao focados.
    val titleFontSize = if (compactLayout) 9.sp else 11.sp
    val textMeasurer = rememberTextMeasurer()
    val titleWidthPx = with(density) { cardWidth.roundToPx() }
    val titleIsLong = remember(member.name, titleFontSize, titleWidthPx, textMeasurer) {
        textMeasurer.measure(
            text = member.name,
            style = TextStyle(fontSize = titleFontSize, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = titleWidthPx)
        ).hasVisualOverflow
    }

    Column(modifier = Modifier.width(cardWidth)) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .scale(if (isFocused) 1.04f else 1f)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF26262A), Color(0xFF17171A))
                    )
                )
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onClick)
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            if (profileBitmap != null) {
                Image(
                    bitmap = profileBitmap!!,
                    contentDescription = member.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "•••",
                    color = Color(0xFFD2D2D6),
                    fontSize = if (compactLayout) 20.sp else 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // A borda e desenhada DEPOIS da foto: nenhuma parte do bitmap
            // pode esconder o contorno dourado quando o card ganha foco.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        BorderStroke(
                            if (isFocused) 2.dp else 1.dp,
                            if (isFocused) MetallicGoldBrush
                            else Brush.linearGradient(
                                listOf(Color(0xFF4A4A50), Color(0xFF4A4A50))
                            )
                        ),
                        shape
                    )
            )
        }
        // Separa a borda ampliada do nome, sem reduzir a foto.
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            color = if (isFocused) Color.Unspecified else Color.White,
            style = if (isFocused) TextStyle(brush = (if (isFocused) Brush.linearGradient(listOf(Color.White, Color.White)) else MetallicGoldBrushText)) else TextStyle.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleFontSize,
            maxLines = 1,
            softWrap = false,
            textAlign = if (titleIsLong) TextAlign.Start else TextAlign.Center,
            overflow = if (isFocused && titleIsLong) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFocused && titleIsLong) {
                        Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 500,
                            repeatDelayMillis = 700,
                            velocity = 26.dp
                        )
                    } else Modifier
                )
        )
    }
}

@Composable
private fun FavoriteHeart(
    isFavorite: Boolean,
    isFocused: Boolean,
    size: Int
) {
    Canvas(
        modifier = Modifier.size(
            size.dp
        )
    ) {
        val path = Path()

        val width = this.size.width
        val height = this.size.height

        path.moveTo(
            width * 0.50f,
            height * 0.88f
        )

        path.cubicTo(
            width * 0.42f,
            height * 0.80f,
            width * 0.08f,
            height * 0.58f,
            width * 0.08f,
            height * 0.32f
        )

        path.cubicTo(
            width * 0.08f,
            height * 0.12f,
            width * 0.23f,
            height * 0.05f,
            width * 0.36f,
            height * 0.05f
        )

        path.cubicTo(
            width * 0.44f,
            height * 0.05f,
            width * 0.49f,
            height * 0.10f,
            width * 0.50f,
            height * 0.17f
        )

        path.cubicTo(
            width * 0.51f,
            height * 0.10f,
            width * 0.56f,
            height * 0.05f,
            width * 0.64f,
            height * 0.05f
        )

        path.cubicTo(
            width * 0.77f,
            height * 0.05f,
            width * 0.92f,
            height * 0.12f,
            width * 0.92f,
            height * 0.32f
        )

        path.cubicTo(
            width * 0.92f,
            height * 0.58f,
            width * 0.58f,
            height * 0.80f,
            width * 0.50f,
            height * 0.88f
        )

        path.close()

        if (isFavorite) {
            // Favoritado: preenchimento dourado metálico completo.
            drawPath(path = path, brush = MetallicGoldBrush)
            if (isFocused) {
                // Realce do foco no próprio coração, sem caixa ao redor.
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        } else if (isFocused) {
            // Vazio no foco: contorno dourado metálico.
            drawPath(
                path = path,
                brush = MetallicGoldBrush,
                style = Stroke(width = 2.6.dp.toPx())
            )
        } else {
            // Vazio sem foco: somente contorno branco.
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 1.9.dp.toPx())
            )
        }

    }
}

internal object MovieDetailsImageLoader {

    private const val TAG = "MovieDetailsImages"
    private const val MEGABYTE = 1024 * 1024
    private const val MAX_MEMORY_CACHE_BYTES = 24 * MEGABYTE
    private const val MAX_TV_MEMORY_CACHE_BYTES = 24 * MEGABYTE
    private const val MIN_MEMORY_CACHE_BYTES = 8 * MEGABYTE
    private const val MAX_DOWNLOAD_BYTES = 20L * MEGABYTE

    // Evita dezenas de downloads paralelos sobrecarregando a TV.
    private val downloadSlots = Semaphore(permits = 3)
    // Conjunto fixo de locks: sem criar um Mutex permanente para cada URL.
    private val urlLocks = Array(64) { Mutex() }

    private val initLock = Any()
    @Volatile
    private var memoryCache: LruCache<String, Bitmap>? = null

    fun trimMemory(fraction: Float = .5f) {
        memoryCache?.let { it.trimToSize((it.maxSize() * fraction.coerceIn(0f, 1f)).toInt()) }
    }

    private fun getMemoryCache(context: Context): LruCache<String, Bitmap> {
        memoryCache?.let { return it }

        return synchronized(initLock) {
            memoryCache ?: run {
                val cacheBytes = br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy
                    .forContext(context).detailsMb * MEGABYTE

                val cache = object : LruCache<String, Bitmap>(cacheBytes) {
                    override fun sizeOf(key: String, value: Bitmap): Int {
                        return value.allocationByteCount
                    }
                }

                memoryCache = cache
                Log.i(TAG, "Cache RAM adaptativo: ${cacheBytes / MEGABYTE} MB")
                cache
            }
        }
    }

    // Leitura imediata e leve, sem rede nem disco, para a primeira composicao.
    // O cache so entrega a imagem se a URL e o tamanho forem identicos.
    fun peekFromMemory(
        context: Context,
        imageUrl: String?,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): ImageBitmap? {
        val normalizedUrl = imageUrl
            ?.trim()
            ?.takeIf {
                it.startsWith("https://", ignoreCase = true) ||
                        it.startsWith("http://", ignoreCase = true)
            } ?: return null

        val width = targetWidthPx.coerceAtLeast(1)
        val height = targetHeightPx.coerceAtLeast(1)
        return getMemoryCache(context)
            .get("$normalizedUrl|${width}x$height")
            ?.asImageBitmap()
    }

    suspend fun load(
        context: Context,
        imageUrl: String?,
        targetWidthPx: Int,
        targetHeightPx: Int,
        imageType: String
    ): ImageBitmap? {
        val normalizedUrl = imageUrl
            ?.trim()
            ?.takeIf {
                it.startsWith("https://", ignoreCase = true) ||
                        it.startsWith("http://", ignoreCase = true)
            }
            ?: return null

        val targetWidth = targetWidthPx.coerceAtLeast(1)
        val targetHeight = targetHeightPx.coerceAtLeast(1)
        val memoryKey = "$normalizedUrl|${targetWidth}x$targetHeight"
        val cache = getMemoryCache(context)

        cache.get(memoryKey)?.let { bitmap ->
            Log.d(TAG, "tipo=$imageType origem=RAM sucesso=true tempoMs=0")
            return bitmap.asImageBitmap()
        }

        return withContext(Dispatchers.IO) {
            val startedAt = SystemClock.elapsedRealtime()
            val urlLock = urlLocks[
                (normalizedUrl.hashCode() and Int.MAX_VALUE) % urlLocks.size
            ]

            urlLock.lock()
            try {
                currentCoroutineContext().ensureActive()

                // Outra requisição pode ter preenchido o cache antes do lock.
                cache.get(memoryKey)?.let { bitmap ->
                    Log.d(
                        TAG,
                        "tipo=$imageType origem=RAM sucesso=true " +
                                "tempoMs=${SystemClock.elapsedRealtime() - startedAt}"
                    )
                    return@withContext bitmap.asImageBitmap()
                }

                val directory = File(context.applicationContext.cacheDir, "movie_details_images")
                // Várias imagens podem tentar criar a pasta ao mesmo tempo.
                // mkdirs() pode retornar false quando outra já a criou.
                // Revalidamos isDirectory para não descartar imagens nesse caso.
                if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
                    Log.w(TAG, "Diretório de cache realmente indisponível")
                    return@withContext null
                }

                // O disco guarda a imagem comprimida original e permite
                // diferentes tamanhos de decodificação sem novo download.
                val diskFile = File(directory, "${sha256(normalizedUrl)}.img")
                br.com.flaviotorres.iptvpremium.core.storage.ImageDiskCache.get(context).retain(diskFile).use {
                    var bitmap = decodeSampled(diskFile, targetWidth, targetHeight)
                    var origin = "DISK"

                    if (bitmap == null) {
                        if (diskFile.exists()) diskFile.delete()

                        downloadSlots.acquire()
                        val downloaded = try {
                            downloadToDisk(context, normalizedUrl, diskFile)
                        } finally {
                            downloadSlots.release()
                        }

                        if (downloaded) {
                            bitmap = decodeSampled(diskFile, targetWidth, targetHeight)
                            origin = "NETWORK"
                        }
                    } else {
                        diskFile.setLastModified(System.currentTimeMillis())
                    }

                    if (bitmap != null) {
                        cache.put(memoryKey, bitmap)
                    }

                    Log.d(
                        TAG,
                        "tipo=$imageType origem=$origin sucesso=${bitmap != null} " +
                                "tempoMs=${SystemClock.elapsedRealtime() - startedAt}"
                    )

                    bitmap?.asImageBitmap()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (oom: OutOfMemoryError) {
                cache.evictAll()
                Log.e(TAG, "Memória insuficiente na decodificação ($imageType)", oom)
                null
            } catch (error: Exception) {
                Log.w(TAG, "Não foi possível carregar a imagem ($imageType): ${error.javaClass.simpleName}")
                null
            } finally {
                urlLock.unlock()
            }
        }
    }

    private fun decodeSampled(
        file: File,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        if (!file.isFile || file.length() == 0L) return null

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        val sourceWidth = bounds.outWidth
        val sourceHeight = bounds.outHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return null

        // Usa potências de dois para evitar decodificar bitmaps muito maiores
        // que o espaço visível. Preserva ARGB_8888 e transparência dos logos.
        var sampleSize = 1
        while (
            sourceWidth / (sampleSize * 2) >= targetWidth &&
            sourceHeight / (sampleSize * 2) >= targetHeight &&
            sampleSize <= 512
        ) {
            sampleSize *= 2
        }

        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }

    private suspend fun downloadToDisk(context: Context, url: String, destination: File): Boolean {
        var reservation: br.com.flaviotorres.iptvpremium.core.storage.CacheStorageBudget.Reservation? = null
        var connection: HttpURLConnection? = null
        var partialFile: File? = null

        return try {
            val directory = destination.parentFile ?: return false

            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            connection.connect()
            if (connection.responseCode !in 200..299) return false
            if (connection.contentLengthLong > MAX_DOWNLOAD_BYTES) return false
            reservation = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
                .reserve(destination, connection.contentLengthLong.takeIf { it > 0 } ?: MAX_DOWNLOAD_BYTES) ?: return false
            val temporary = reservation.temporary
            partialFile = temporary

            var totalBytes = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count == -1) break
                        totalBytes += count
                        if (totalBytes > reservation.limit) return false
                        output.write(buffer, 0, count)
                    }
                }
            }

            if (totalBytes == 0L) return false
            if (!temporary.renameTo(destination)) return false
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Download de imagem falhou: ${error.javaClass.simpleName}")
            false
        } finally {
            runCatching { connection?.disconnect() }
            partialFile?.delete()
            reservation?.close()
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

private fun ratingStars(
    rating: Double
): String {
    val normalized =
        if (rating > 5.0) {
            (rating / 2.0).coerceIn(
                0.0,
                5.0
            )
        } else {
            rating.coerceIn(
                0.0,
                5.0
            )
        }

    val fullStars =
        normalized.toInt()

    return buildString {
        repeat(fullStars) {
            append("★")
        }

        repeat(
            5 - fullStars
        ) {
            append("☆")
        }
    }
}

private fun formatRatingValue(
    rating: Double
): String {
    return if (
        rating % 1.0 == 0.0
    ) {
        rating
            .toInt()
            .toString()
    } else {
        "%.1f".format(
            rating
        )
    }
}

private fun formatDuration(
    durationSeconds: Int
): String {
    val totalMinutes =
        durationSeconds / 60

    val hours =
        totalMinutes / 60

    val minutes =
        totalMinutes % 60

    return if (hours > 0) {
        "${hours}h ${minutes}min"
    } else {
        "${minutes}min"
    }

}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_MEDIA_ACTION_BUTTONS_V1

// Text retains the approved palette; blue darkening applies to rims.
private val MetallicGoldBrushText get() = Brush.linearGradient(
    colors = listOf(
        Color(0xFF8A6414),
        Color(0xFFD4AF37),
        Color(0xFFFFE991),
        Color(0xFFFFF4BD),
        Color(0xFFE3B83F),
        Color(0xFF9E761B)
    )
)
