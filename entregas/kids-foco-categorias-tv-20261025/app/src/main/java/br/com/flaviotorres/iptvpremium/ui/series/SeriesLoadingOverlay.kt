package br.com.flaviotorres.iptvpremium.ui.series

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource as painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A TV exibe carregamento animado enquanto os dados sao preparados.
 * Quando prontos, revela a pagina e preserva o encaixe/zoom da sala.
 * Apenas desenha dados ja preparados, sem iniciar carregamentos de rede.
 */
@Composable
fun SeriesLoadingOverlay(
    seriesName: String,
    isSeriesReady: Boolean,
    onComplete: (isReady: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    previewBackdrop: ImageBitmap? = null,
    detailsContent: (@Composable () -> Unit)? = null,
    openingKey: String = seriesName,
    isSeriesFailed: Boolean = false,
    isArtworkReady: Boolean = true
) {
    val readyLatest by rememberUpdatedState(isSeriesReady)
    val failedLatest by rememberUpdatedState(isSeriesFailed)
    val artworkLatest by rememberUpdatedState(isArtworkReady)
    val onCompleteLatest by rememberUpdatedState(onComplete)
    if(br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active) {
        LaunchedEffect(openingKey) {
            val ready=br.com.flaviotorres.iptvpremium.ui.theme.awaitSeriesOpeningReady(
                60_000L,{readyLatest},{failedLatest})
            onCompleteLatest(ready)
        }
        br.com.flaviotorres.iptvpremium.ui.kids.KidsNeutralLoadingScreen(modifier=modifier)
        return
    }
    val loadingAlpha = remember(openingKey) { Animatable(1f) }
    val cameraApproach = remember(openingKey) { Animatable(0f) }
    val sceneAlpha = remember(openingKey) { Animatable(1f) }

    LaunchedEffect(openingKey) {
        val openingStarted = SystemClock.uptimeMillis()
        // SERIES_REAL_TV_PAGE_V1
        // A TV ja mostra um carregamento vivo. So revela a pagina quando
        // os episodios reais e a sala estiverem preparados.
        val ready = br.com.flaviotorres.iptvpremium.ui.theme.awaitSeriesOpeningReady(
            60_000L, { readyLatest }, { failedLatest }
        )

        if (!ready) {
            onCompleteLatest(false)
            return@LaunchedEffect
        }

        // The existing transition takes two seconds. Artwork may use only the
        // remaining preparation budget, revealing available images before the transition.
        awaitSeriesArtworkWindow(openingStarted, { artworkLatest }, { failedLatest }, SystemClock::uptimeMillis)
        if (failedLatest) { onCompleteLatest(false); return@LaunchedEffect }

        // Permite ao Compose desenhar a pagina atualizada antes
        // de iniciar a animacao original.
        withFrameNanos { }

        val started = SystemClock.uptimeMillis()
        suspend fun waitFor(ms: Long) {
            val remaining = ms - (SystemClock.uptimeMillis() - started)
            if (remaining > 0L) delay(remaining)
        }

        // Transicao da tela de carregamento para a pagina real da serie.
        loadingAlpha.animateTo(0f, animationSpec = tween(180, easing = LinearEasing))
        // A imagem precisa estar visivel antes de a camera comecar a avancar.
        waitFor(SERIES_OPENING_TRANSITION_MS - 420L)
        coroutineScope {
            launch {
                cameraApproach.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(420, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(350L)
                sceneAlpha.animateTo(0f, animationSpec = tween(70))
            }
        }
        // A animacao termina com os detalhes ja preparados.
        onCompleteLatest(true)
    }

    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF070809))
            .semantics { contentDescription = "Abrindo detalhes da série $seriesName" }
    ) {
        val viewportWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val viewportHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val geometry = remember(viewportWidth, viewportHeight) {
            seriesTvGeometry(viewportWidth, viewportHeight)
        }
        val screen = geometry.screen
        
        val panelWidth = with(density) { screen.width.toDp() }
        val panelHeight = with(density) { screen.height.toDp() }
        val textScale = screen.width / viewportWidth
        val screenMask = remember(geometry) {
            Path().apply {
                addRoundRect(
                    RoundRect(
                        left = screen.left,
                        top = screen.top,
                        right = screen.right,
                        bottom = screen.bottom,
                        cornerRadius = CornerRadius(geometry.cornerRadius)
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = sceneAlpha.value
                    val approach = cameraApproach.value
                    val zoom = 1f + (geometry.finalZoom - 1f) * approach
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = zoom
                    scaleY = zoom
                    translationX = geometry.finalTranslationX * approach
                    translationY = geometry.finalTranslationY * approach
                }
        ) {
            Image(
                painter = painterResource(R.drawable.series_living_room),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop
            )

            // Uma unica mascara limita a imagem e a luz ao interior do painel.
            // Nao usamos dimensoes proporcionais ao viewport para adivinhar a TV.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val canvas = drawContext.canvas
                        canvas.save()
                        try {
                            canvas.clipPath(screenMask)
                            drawContent()
                        } finally { canvas.restore() }
                    }
            ) {
                // O painel e medido na proporcao real da TV. Crop preenche-o
                // sem esticar a foto; a moldura do asset fica sempre descoberta.
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .size(panelWidth, panelHeight)
                        .graphicsLayer {
                            translationX = screen.left
                            translationY = screen.top
                        }
                        .clipToBounds()
                        .background(Color(0xFF05070A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 1f - loadingAlpha.value }
                    ) {
                        if (detailsContent != null) {
                            // Usa a mesma pagina da rota de detalhes.
                            // Mantem o tamanho logico da tela inteira e
                            // reduz apenas a renderizacao para caber na TV.
                            // O painel continua com o recorte original.
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(
                                        Alignment.TopStart,
                                        unbounded = true
                                    )
                                    .requiredSize(
                                        with(density) { viewportWidth.toDp() },
                                        with(density) { viewportHeight.toDp() }
                                    )
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        scaleX = screen.width / viewportWidth
                                        scaleY = screen.height / viewportHeight
                                    }
                            ) {
                                detailsContent()
                            }
                        } else {
                            SeriesPreviewPicture(
                                seriesName = seriesName,
                                previewBackdrop = previewBackdrop,
                                textScale = textScale,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    if (loadingAlpha.value > 0f) {
                        SeriesTvLoadingPanel(Modifier.fillMaxSize().graphicsLayer { alpha = loadingAlpha.value })
                    }
                }
            }


        }
    }
}

@Composable
private fun SeriesTvLoadingPanel(modifier: Modifier = Modifier) {
    val accent = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD6A32E)
    val motion = rememberInfiniteTransition(label = "series-loading")
    val angle by motion.animateFloat(0f, 360f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "series-spinner")
    val pulse by motion.animateFloat(0f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "series-pulse")
    BoxWithConstraints(modifier.background(Color(0xFF05070A)).semantics {
        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        contentDescription = "Carregando temporadas e episódios"
    }) {
        val spinnerSize = (maxHeight * 0.23f).coerceIn(20.dp, 72.dp)
        val spinnerGap = (maxHeight * 0.06f).coerceAtLeast(5.dp)
        val titleSize = (maxWidth.value * 0.04f).coerceIn(10f, 20f).sp
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.radialGradient(listOf(accent.copy(alpha = 0.12f), Color.Transparent),
                center = center, radius = size.maxDimension * 0.65f))
        }
        Column(Modifier.align(Alignment.Center).fillMaxWidth(0.84f), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(spinnerSize)) {
                val stroke = Stroke(width = (size.minDimension * 0.065f).coerceAtLeast(1f))
                val inset = stroke.width
                drawCircle(accent.copy(alpha = 0.14f), radius = size.minDimension / 2f - inset, style = stroke)
                drawArc(accent.copy(alpha = 0.85f), angle, 100f + pulse * 90f, false,
                    topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2), style = stroke)
            }
            Spacer(Modifier.height(spinnerGap))
            Text("Carregando série", color = Color.White, fontSize = titleSize, maxLines = 1)
        }
    }
}

@Composable
private fun SeriesPreviewPicture(
    seriesName: String,
    previewBackdrop: ImageBitmap?,
    textScale: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color(0xFF08090B))) {
        if (previewBackdrop != null) {
            Image(
                bitmap = previewBackdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(Color(0xE9000000), Color(0x80000000), Color(0x20000000))
                    )
                )
            )
        }
        Text(
            text = seriesName,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.65f)
                .padding(start = 26.dp * textScale),
            color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFE9CC7A),
            fontSize = 30.sp * textScale,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
