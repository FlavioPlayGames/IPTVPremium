package br.com.flaviotorres.iptvpremium.core.player

import android.os.SystemClock
import android.view.SurfaceView
import android.view.View
import androidx.media3.ui.SubtitleView
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.ui.movies.MovieDetailsImageLoader
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.sin

/**
 * Primeira interface visual dos dois players internos do IPTVPremium.
 *
 * - Recebe uma PlaybackMedia com URL resolvida pelo catálogo.
 * - NÃO altera AppNavigation, Room ou o cache de imagens.
 * - onExit recebe a posição atual; a navegação gravará o histórico futuramente.
 * - onChangeLiveChannel recebe +1 ou -1; a navegação decidirá qual canal abrir.
 * - SurfaceView pertence a esta tela; os decodificadores pertencem ao controller.
 *
 * Esta etapa implementa reprodução visual, controle remoto, barra, velocidade,
 * troca manual Media3/VLC e painel técnico. Os menus de áudio, legendas,
 * EPG e opções avançadas de legendas serão adicionados em etapas posteriores.
 * Inclui Normal/Tela cheia/Manual, barra unica translucida e seek por DPAD com aceleracao.
 * Barra de progresso normal com avanço, retrocesso e busca por controle ou toque.
 */
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun PlaybackScreen(
    media: PlaybackMedia,
    onExit: (positionMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    startPositionMs: Long = 0L,
    settings: PlaybackSettings? = null,
    onChangeLiveChannel: ((direction: Int) -> Unit)? = null,
    titleLogoUrl: String? = null,
    onPlaybackEnded: (() -> Unit)? = null,
    onChangeEpisode: ((direction: Int, positionMs: Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val effectiveSettings = settings ?: remember(context) { br.com.flaviotorres.iptvpremium.ui.settings.SettingsPreferences(context).playback() }
    val isCompactScreen = LocalConfiguration.current.screenWidthDp < 700
    val controller = remember(context) { PlaybackController(context.applicationContext) }
    val playback by controller.state.collectAsState()
    val capabilities by controller.capabilities.collectAsState()

    val onEnded by rememberUpdatedState(onPlaybackEnded)
    var endHandled by remember(media.id, media.type, media.streamUrl) { mutableStateOf(false) }
    LaunchedEffect(media.id, media.streamUrl, playback.status) {
        if (!endHandled && playback.media?.id == media.id &&
            playback.status == PlaybackStatus.ENDED &&
            media.type == PlaybackContentType.EPISODE && effectiveSettings.autoPlayNextEpisode && onEnded != null) {
            endHandled = true
            onEnded?.invoke()
        }
    }
    val isLive = media.type == PlaybackContentType.LIVE_CHANNEL
    val audioTracks = playback.availableTracks.filter { it.type == PlaybackTrackType.AUDIO }
    val subtitleTracks = playback.availableTracks.filter { it.type == PlaybackTrackType.SUBTITLE }
    // A presença de uma única faixa já permite abrir o menu e ver o idioma atual.
    // Não depender do StateFlow de capabilities aqui: a lista de tracks é a
    // informação efetiva da sessão, mesmo quando os flows chegam em momentos diferentes.
    val showAudioButton = audioTracks.isNotEmpty()
    val externalSubtitleRepo = remember(context) { ExternalSubtitleRepository(context.applicationContext) }
    var externalOptions by remember(media.id, media.type, media.streamUrl) {
        mutableStateOf<List<ExternalSubtitleOption>>(emptyList())
    }
    var searchingExternal by remember(media.id, media.type, media.streamUrl) { mutableStateOf(false) }
    var externalSearchStatus by remember(media.id, media.type, media.streamUrl) {
        mutableStateOf("Ainda não pesquisado")
    }
    var downloadingExternal by remember(media.id, media.type, media.streamUrl) { mutableStateOf(false) }
    var externalCues by remember(media.id, media.type, media.streamUrl) {
        mutableStateOf<List<ExternalSubtitleCue>>(emptyList())
    }
    var activeExternal by remember(media.id, media.type, media.streamUrl) {
        mutableStateOf<ExternalSubtitleOption?>(null)
    }
    val subtitleScope = rememberCoroutineScope()
    // Styling and its preview remain available even with captions disabled.
    val showSubtitleButton = true
    val appearanceStore = remember(context) { SubtitleAppearanceStore(context) }
    var subtitleAppearance by remember(appearanceStore) { mutableStateOf(appearanceStore.load()) }
    androidx.compose.runtime.DisposableEffect(appearanceStore) {
        val observer = appearanceStore.observe { subtitleAppearance = appearanceStore.load() }
        onDispose { observer.close() }
    }
    fun updateSubtitleAppearance(value: SubtitleAppearance) {
        subtitleAppearance = value
        appearanceStore.save(value)
    }
    val gold = PlayerChrome.color(0xFFD4AF37) // Branco neutro no player regular, independente do tema da Home
    val screenFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val panelFocus = remember { FocusRequester() }
    val seekFocus = remember { FocusRequester() }
    // Referencia apenas da SurfaceView ativa para escondê-la de imediato
    // ao sair, antes de o NavHost finalizar a transicao entre as telas.
    val activeVideoSurface = remember { java.util.concurrent.atomic.AtomicReference<SurfaceView?>() }
    val density = LocalDensity.current

    // A arte oficial do titulo e opcional e somente visual.
    // O catalogo/rota precisa fornecer a URL real; capa nao vira logo.
    // Canal ao vivo continua com o cartao (sem confundir logo do canal
    // com a arte do titulo de um filme/serie).
    val artworkUrl = titleLogoUrl?.trim()?.takeIf { url ->
        !isLive && (url.startsWith("https://", ignoreCase = true) ||
                url.startsWith("http://", ignoreCase = true))
    }
    val artworkWidth = if (isCompactScreen) 280.dp else 380.dp
    val artworkHeight = if (isCompactScreen) 76.dp else 104.dp
    val artworkWidthPx = with(density) { artworkWidth.roundToPx().coerceAtLeast(1) }
    val artworkHeightPx = with(density) { artworkHeight.roundToPx().coerceAtLeast(1) }

    // Reutiliza o carregador e o cache RAM/disco dos detalhes de Filmes.
    // A busca e assincrona, nunca bloqueia a preparacao Media3/VLC.
    // Enquanto a imagem nao chega ou falha, o cartao permanece visivel.
    val titleArtwork by produceState<ImageBitmap?>(
        initialValue = MovieDetailsImageLoader.peekFromMemory(
            context.applicationContext, artworkUrl, artworkWidthPx, artworkHeightPx
        ),
        key1 = media.type to media.id,
        key2 = artworkUrl,
        key3 = artworkWidthPx to artworkHeightPx
    ) {
        value = MovieDetailsImageLoader.peekFromMemory(
            context.applicationContext, artworkUrl, artworkWidthPx, artworkHeightPx
        )
        if (value == null && artworkUrl != null) {
            value = MovieDetailsImageLoader.load(
                context = context.applicationContext,
                imageUrl = artworkUrl,
                targetWidthPx = artworkWidthPx,
                targetHeightPx = artworkHeightPx,
                imageType = "playback_title_artwork"
            )
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var panel by remember { mutableStateOf(PlayerPanel.NONE) }
    // Menus comuns reiniciam a rolagem ao trocar de painel.
    // No INFO, apenas as linhas tecnicas rolam: o titulo e Voltar ficam fixos.
    val menuScrollState = remember(panel) { ScrollState(0) }
    val infoScrollState = rememberScrollState()
    LaunchedEffect(panel) {
        if (panel == PlayerPanel.INFO) infoScrollState.scrollTo(0)
    }
    // Evita que pressionamentos repetidos de Voltar retirem duas telas
    // da pilha enquanto a navegacao ainda esta encerrando o player.
    var exitRequested by remember(media.id, media.type, media.streamUrl) {
        mutableStateOf(false)
    }
    var interactionTick by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var speed by remember { mutableFloatStateOf(effectiveSettings.speed) }
    var currentLayout by remember(media.id) { mutableStateOf(effectiveSettings.aspect) }
    var seekFocused by remember { mutableStateOf(false) }
    var seekTargetMs by remember { mutableStateOf<Long?>(null) }
    var dpadScrubbing by remember(media.id) { mutableStateOf(false) }

    // Disparar UMA pesquisa ao entrar no player, nunca ao navegar no catálogo.
    LaunchedEffect(media.id, media.streamUrl, media.type) {
        externalOptions = emptyList()
        activeExternal = null
        externalCues = emptyList()
        if (!isLive && externalSubtitleRepo.configured) {
            searchingExternal = true
            externalSearchStatus = "Consultando SubDL..."
            try {
                externalOptions = externalSubtitleRepo.search(media)
                externalSearchStatus = externalSubtitleRepo.lastSearchStatus
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                externalOptions = emptyList()
                externalSearchStatus = "Falha inesperada na pesquisa"
            } finally { searchingExternal = false }
        } else if (!isLive) {
            externalSearchStatus = "SubDL não conectado"
        }
    }

    LaunchedEffect(panel, showAudioButton, showSubtitleButton) {
        if ((panel == PlayerPanel.AUDIO && !showAudioButton) ||
            (panel == PlayerPanel.SUBTITLES && !showSubtitleButton)
        ) panel = PlayerPanel.NONE
    }

    fun reveal() {
        controlsVisible = true
        interactionTick++
    }

    fun exit() {
        if (exitRequested) return
        val resumePosition = if (isLive) 0L else playback.positionMs.coerceAtLeast(0L)
        exitRequested = true
        // Invalidate recovery and start native shutdown before the outgoing SurfaceView disappears.
        controller.stop()
        // Esconde o video nativo no mesmo evento do controle remoto;
        // SurfaceView pode sobreviver por alguns frames a transicao Compose.
        activeVideoSurface.get()?.let { videoView ->
            videoView.alpha = 0f
            videoView.visibility = View.INVISIBLE
            videoView.keepScreenOn = false
        }
        // A transicao de navegacao pode manter esta rota composta por mais
        // alguns frames. O conteudo visual abaixo e removido imediatamente,
        // evitando que a ultima imagem da SurfaceView apareca sobre a tela anterior.
        controlsVisible = false
        panel = PlayerPanel.NONE
        onExit(resumePosition)
    }

    // Um unico destino visual para Voltar e Fechar: video limpo, sem barra.
    // Sem revelar novamente os controles ao encerrar o menu Opcoes.
    fun dismissOverlaysToVideo() {
        panel = PlayerPanel.NONE
        controlsVisible = false
        seekTargetMs = null
        dpadScrubbing = false
        seekFocused = false
    }

    // Evita que KeyDown do controle e o dispatcher de Back processem
    // duas vezes o mesmo pressionamento fisico na Android TV.
    var lastBackHandledAtMs by remember { mutableStateOf(0L) }
    fun handlePlayerBack() {
        val now = SystemClock.uptimeMillis()
        if (now - lastBackHandledAtMs < 230L) return
        lastBackHandledAtMs = now
        if (panel == PlayerPanel.SUBTITLE_STYLE) {
            panel = PlayerPanel.SUBTITLES
        } else if (controlsVisible || panel != PlayerPanel.NONE) {
            dismissOverlaysToVideo()
        } else {
            exit()
        }
    }

    fun commitSeekPending() {
        val target = seekTargetMs ?: return
        seekTargetMs = null
        dpadScrubbing = false
        if (controller.seekTo(target) != PlaybackCommandResult.ACCEPTED) {
            feedback = "Este conteúdo não permite buscar essa posição"
        }
    }

    fun togglePlayback() {
        // OK on the timeline confirms any pending seek and toggles playback once.
        if (seekTargetMs != null) {
            commitSeekPending()
        }
        val result = if (playback.isPlaying) controller.pause() else controller.play()
        if (result != PlaybackCommandResult.ACCEPTED) {
            feedback = "Comando indisponível neste momento"
        }
        reveal()
    }

    // Botões +/- 10 segundos continuam independentes do modo de prévias removido.
    fun seek(deltaMs: Long) {
        if (isLive && !capabilities.supportsLiveTimeshift) return
        if (!playback.canSeek) return
        if (controller.seekBy(deltaMs) != PlaybackCommandResult.ACCEPTED) {
            feedback = "Busca indisponível neste conteúdo"
        }
        reveal()
    }

    fun changeAspect(mode: PlaybackAspectMode) {
        currentLayout = currentLayout.copy(mode = mode)
        // A geometria e gerenciada pela SurfaceView da interface, nao pelos
        // diferentes renderizadores. Evita crop duplo ao trocar Media3/VLC.
        controller.setVideoLayout(PlaybackVideoLayout())
        reveal()
    }

    fun adjustManual(transform: PlaybackManualTransform) {
        currentLayout = PlaybackVideoLayout(
            mode = PlaybackAspectMode.MANUAL,
            manual = transform
        )
        reveal()
    }

    fun stepSeek(direction: Int, repeatCount: Int) {
        val duration = playback.durationMs?.takeIf { it > 0L } ?: return
        if (isLive || !playback.canSeek) return
        val base = seekTargetMs ?: playback.positionMs
        val interval = when {
            repeatCount >= 20 -> 60_000L
            repeatCount >= 8 -> 30_000L
            else -> 10_000L
        }
        dpadScrubbing = true
        seekTargetMs = (base + interval * direction).coerceIn(0L, duration)
        reveal()
    }

    fun commitSeek() {
        commitSeekPending()
        reveal()
    }

    // Primeira tecla Voltar esconde menu/barra; a seguinte sai do video.
    // BackHandler cobre a navegacao do sistema (gesto/dispatcher).
    BackHandler { handlePlayerBack() }

    LaunchedEffect(media.id, media.type, media.streamUrl, startPositionMs, effectiveSettings) {
        if (exitRequested) return@LaunchedEffect
        speed = effectiveSettings.speed
        panel = PlayerPanel.NONE
        controlsVisible = true
        seekTargetMs = null
        dpadScrubbing = false
        currentLayout = effectiveSettings.aspect
        controller.open(
            PlaybackOpenRequest(
                media = media,
                startPositionMs = if (isLive) 0L else startPositionMs.coerceAtLeast(0L),
                // A UI e responsavel pelo enquadramento da SurfaceView.
                settings = effectiveSettings.copy(aspect = PlaybackVideoLayout())
            )
        )
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    // O foco na barra nao prende os controles na tela. Cada interacao reinicia
    // os cinco segundos; uma busca em andamento termina antes de esconder.
    LaunchedEffect(controlsVisible, panel, interactionTick, seekTargetMs) {
        if (controlsVisible && panel == PlayerPanel.NONE && seekTargetMs == null) {
            delay(5_000L)
            if (panel == PlayerPanel.NONE && seekTargetMs == null) {
                dismissOverlaysToVideo()
            }
        }
    }

    LaunchedEffect(controlsVisible, panel) {
        withFrameNanos { }
        runCatching {
            when {
                panel != PlayerPanel.NONE -> panelFocus.requestFocus()
                controlsVisible -> playFocus.requestFocus()
                else -> screenFocus.requestFocus()
            }
        }
    }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(2_500L)
            feedback = null
        }
    }

    // O NavHost pode animar a saida da rota. Remover a SurfaceView logo ao
    // solicitar a saida evita a sobreposicao residual sobre Canais/Filmes/Series.
    if (exitRequested) {
        Box(modifier = modifier.fillMaxSize())
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(screenFocus)
                .onPreviewKeyEvent { key ->
                    val native = key.nativeKeyEvent
                    // Prioridade ao Voltar diretamente na arvore de foco da TV:
                    // nao deixar o primeiro toque ser absorvido pelo menu Opcoes.
                    // Consome KeyUp tambem, impedindo processamento duplicado.
                    if (native.keyCode == AndroidKeyEvent.KEYCODE_BACK ||
                        native.keyCode == AndroidKeyEvent.KEYCODE_ESCAPE) {
                        if (key.type == KeyEventType.KeyDown && native.repeatCount == 0) {
                            handlePlayerBack()
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (key.type == KeyEventType.KeyUp) {
                        if (seekFocused && panel == PlayerPanel.NONE && native.keyCode in listOf(
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER,
                            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)) return@onPreviewKeyEvent true
                        if (dpadScrubbing && (
                                    native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT ||
                                            native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT ||
                                            native.keyCode == AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD ||
                                            native.keyCode == AndroidKeyEvent.KEYCODE_MEDIA_REWIND
                                    )) {
                            commitSeek()
                            return@onPreviewKeyEvent true
                        }
                        return@onPreviewKeyEvent false
                    }
                    if (key.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (controlsVisible && panel == PlayerPanel.NONE) interactionTick++
                    if (panel == PlayerPanel.NONE && !controlsVisible && native.keyCode in listOf(AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_DPAD_DOWN)) {
                        if (native.repeatCount == 0) reveal()
                        return@onPreviewKeyEvent true
                    }
                    if (seekFocused && panel == PlayerPanel.NONE && native.keyCode in listOf(
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)) {
                        if (native.repeatCount == 0) togglePlayback()
                        return@onPreviewKeyEvent true
                    }
                    // Na barra de progresso o repeatCount acelera a busca
                    // progressivamente; nos outros controles ignoramos repeticoes.
                    if (seekFocused && panel == PlayerPanel.NONE && !isLive && playback.canSeek) {
                        when (native.keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                stepSeek(-1, native.repeatCount)
                                return@onPreviewKeyEvent true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                stepSeek(1, native.repeatCount)
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    // D-Pad percorre a lista tecnica sem deslocar o cabecalho
                    // nem o botao Voltar. Funciona mesmo com foco no Voltar.
                    if (panel == PlayerPanel.INFO &&
                        (native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP ||
                                native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN)) {
                        val delta = if (native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN) 88 else -88
                        subtitleScope.launch {
                            infoScrollState.scrollTo(
                                (infoScrollState.value + delta).coerceIn(0, infoScrollState.maxValue)
                            )
                        }
                        return@onPreviewKeyEvent true
                    }
                    // Primeira seta CIMA com o menu fechado: abre os controles.
                    // Segunda seta CIMA: vai diretamente a bolinha do progresso,
                    // sem realizar seek. Apos focada, ESQUERDA/DIREITA continuam
                    // movimentando apenas o cursor ate o KeyUp ou OK.
                    if (native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP &&
                        panel == PlayerPanel.NONE) {
                        if (!controlsVisible) {
                            if (native.repeatCount == 0) reveal()
                            return@onPreviewKeyEvent true
                        }
                        if (!seekFocused && !isLive && playback.canSeek &&
                            (playback.durationMs ?: 0L) > 0L) {
                            if (native.repeatCount == 0) {
                                runCatching { seekFocus.requestFocus() }
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                    // Da barra para os botoes sem tocar na posicao do filme.
                    if (native.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN &&
                        seekFocused && panel == PlayerPanel.NONE && native.repeatCount == 0) {
                        runCatching { playFocus.requestFocus() }
                        return@onPreviewKeyEvent true
                    }
                    if (native.repeatCount > 0 &&
                        native.keyCode != AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD &&
                        native.keyCode != AndroidKeyEvent.KEYCODE_MEDIA_REWIND
                    ) return@onPreviewKeyEvent false

                    when (native.keyCode) {
                        // Botoes fisicos de canal do controle remoto (Android TV).
                        // Somente canais ao vivo: Filmes e Series mantem os atalhos atuais.
                        // O callback e resolvido pela navegacao sem alterar o Media3.
                        AndroidKeyEvent.KEYCODE_CHANNEL_UP,
                        AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            if (isLive && onChangeLiveChannel != null) {
                                onChangeLiveChannel(
                                    if (native.keyCode == AndroidKeyEvent.KEYCODE_CHANNEL_UP) 1 else -1
                                )
                                true
                            } else false
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY,
                        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            if (panel == PlayerPanel.NONE) {
                                togglePlayback()
                                true
                            } else false
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                        AndroidKeyEvent.KEYCODE_ENTER -> {
                            if (!controlsVisible && panel == PlayerPanel.NONE) {
                                reveal()
                                true
                            } else false
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Setas navegam pelo foco; busca temporal somente na barra.
                            if (!controlsVisible && panel == PlayerPanel.NONE) { reveal(); true } else false
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_UP,
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> false
                        AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            stepSeek(1, native.repeatCount)
                            true
                        }
                        AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                            stepSeek(-1, native.repeatCount)
                            true
                        }
                        else -> false
                    }
                }
                .focusable()
        ) {
            // SurfaceView evita forcar a renderizacao HDR por TextureView.
            // O container utiliza a proporcao real recebida do Media3; quando
            // nao vier metadado (por exemplo, no VLC), parte de 16:9 e permite
            // corrigir o enquadramento manualmente sem trocar o decoder.
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                val viewportW = maxWidth.value.coerceAtLeast(1f)
                val viewportH = maxHeight.value.coerceAtLeast(1f)
                val videoW = playback.technicalInfo.width?.takeIf { it > 0 } ?: 16
                val videoH = playback.technicalInfo.height?.takeIf { it > 0 } ?: 9
                val videoRatio = (videoW.toFloat() / videoH.toFloat()).coerceIn(0.4f, 4f)
                val viewportRatio = viewportW / viewportH
                val fittedW: Float
                val fittedH: Float
                if (videoRatio >= viewportRatio) {
                    fittedW = viewportW
                    fittedH = viewportW / videoRatio
                } else {
                    fittedH = viewportH
                    fittedW = viewportH * videoRatio
                }
                val fillFactor = max(viewportW / fittedW, viewportH / fittedH)
                val transform = currentLayout.manual
                val scaleX = when (currentLayout.mode) {
                    PlaybackAspectMode.NORMAL -> 1f
                    PlaybackAspectMode.FULL_SCREEN -> fillFactor
                    PlaybackAspectMode.MANUAL -> transform.zoom * transform.widthScale
                }
                val scaleY = when (currentLayout.mode) {
                    PlaybackAspectMode.NORMAL -> 1f
                    PlaybackAspectMode.FULL_SCREEN -> fillFactor
                    PlaybackAspectMode.MANUAL -> transform.zoom * transform.heightScale
                }
                val offsetXpx = with(density) {
                    (viewportW * if (currentLayout.mode == PlaybackAspectMode.MANUAL)
                        transform.offsetX else 0f).dp.toPx()
                }
                val offsetYpx = with(density) {
                    (viewportH * if (currentLayout.mode == PlaybackAspectMode.MANUAL)
                        transform.offsetY else 0f).dp.toPx()
                }
                AndroidView(
                    factory = { appContext ->
                        SurfaceView(appContext).apply {
                            isFocusable = false
                            isFocusableInTouchMode = false
                            keepScreenOn = true
                            activeVideoSurface.set(this)
                        }
                    },
                    update = { view ->
                        // A View mantem sua Surface nativa; zoom/posicao sao
                        // transformacoes de apresentacao e nao de pixels HDR.
                        view.scaleX = scaleX
                        view.scaleY = scaleY
                        view.translationX = offsetXpx
                        view.translationY = offsetYpx
                        controller.attachVideoSurface(view.holder)
                    },
                    onRelease = { view ->
                        controller.detachVideoSurface(view.holder)
                        activeVideoSurface.compareAndSet(view, null)
                        view.keepScreenOn = false
                    },
                    modifier = Modifier.size(fittedW.dp, fittedH.dp)
                )
            }

            // Legenda externa: desenhada por cima do único vídeo (Media3 ou VLC).
            // Nenhum restart, seek ou novo player para adicionar a legenda.
            if (activeExternal != null && externalCues.isNotEmpty()) {
                val position = playback.positionMs
                val textNow = remember(externalCues, position) {
                    externalCues.firstOrNull { position >= it.startMs && position < it.endMs }?.text
                }
                if (!textNow.isNullOrBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(
                            bottom = if (controlsVisible) {
                                if (isCompactScreen) 158.dp else 188.dp
                            } else 48.dp
                        ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        StyledSubtitleText(
                            text = textNow, appearance = subtitleAppearance,
                            modifier = Modifier.fillMaxWidth(0.88f).height(120.dp)
                        )
                    }
                }
            }

            // O Media3 entrega os cues mas não os desenha em uma SurfaceView isolada.
            // A SubtitleView é uma camada transparente, NÃO outro player.
            // O VLC já desenha as legendas na própria saída de vídeo.
            if (playback.engine == PlaybackEngine.MEDIA3) {
                AndroidView(
                    factory = { appContext ->
                        SubtitleView(appContext).apply {
                            isClickable = false
                            isFocusable = false
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                            setApplyEmbeddedStyles(true)
                            setApplyEmbeddedFontSizes(true)
                            setBottomPaddingFraction(0.08f)
                        }
                    },
                    update = { view ->
                        applySubtitleAppearance(view, subtitleAppearance)
                        view.setCues(playback.subtitleCues)
                    },
                    modifier = Modifier.fillMaxSize().padding(
                        bottom = if (controlsVisible) {
                            if (isCompactScreen) 102.dp else 130.dp
                        } else 0.dp
                    )
                )
            }

            // Touch layer above native video/subtitles, below buttons and error actions.
            if (panel == PlayerPanel.NONE) Box(Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(onTap = { reveal() })
            })

            if (playback.isBuffering) {
                Text(
                    text = "Carregando vídeo…",
                    color = Color.White,
                    fontSize = 17.sp,
                    modifier = Modifier.align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }

            val failure = playback.failure
            if (playback.status == PlaybackStatus.ERROR && failure != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                        .padding(24.dp)
                ) {
                    Text("Não foi possível reproduzir", color = Color.White, fontSize = 21.sp)
                    Text(failure.message, color = Color.LightGray, fontSize = 14.sp)
                    PlayerControlButton("Trocar reprodutor", gold) {
                        val target = playback.engine.alternate()
                        controller.switchEngine(target)
                        reveal()
                    }
                }
            }

            if (controlsVisible && panel == PlayerPanel.NONE) {
                // Mesmo cabecalho preto e dourado para Canais, Filmes e Episodios.
                // O titulo e os metadados vem exclusivamente de PlaybackMedia.
                if (media.type == PlaybackContentType.MOVIE && titleArtwork != null) {
                    // Quando houver logo/titulo artistico oficial, ele assume
                    // o lugar do cartao de texto, sem placa ao redor da arte.
                    Image(
                        bitmap = titleArtwork!!,
                        contentDescription = media.title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = if (isCompactScreen) 16.dp else 30.dp,
                                top = if (isCompactScreen) 16.dp else 28.dp
                            )
                            .width(artworkWidth)
                            .height(artworkHeight)
                    )
                } else {
                    PlayerTitleCard(
                        media = media,
                        gold = gold,
                        compact = isCompactScreen,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = if (isCompactScreen) 16.dp else 30.dp,
                                top = if (isCompactScreen) 16.dp else 28.dp
                            )
                    )
                }

                PlayerCinemaPanel(
                    compact = isCompactScreen,
                    gold = gold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isCompactScreen) 8.dp else 16.dp,
                            vertical = if (isCompactScreen) 9.dp else 18.dp
                        )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isLive) "AO VIVO" else formatPlaybackTime(seekTargetMs ?: playback.positionMs),
                            color = if (seekTargetMs != null) gold else Color.White,
                            fontSize = if (isCompactScreen) 16.sp else 18.sp
                        )
                        Spacer(Modifier.width(18.dp))
                        Box(Modifier.weight(1f)) {
                            if (!isLive && playback.durationMs != null && playback.durationMs!! > 0L) {
                                val target = seekTargetMs
                                // O tempo no topo e o marcador dourado fornecem feedback
                                // sem alterar a altura do painel ao segurar as setas.
                                PlayerProgressBar(
                                    progress = if (target != null) {
                                        (target.toFloat() / playback.durationMs!!.toFloat()).coerceIn(0f, 1f)
                                    } else playback.progressFraction,
                                    durationMs = playback.durationMs!!,
                                    gold = gold,
                                    focusRequester = seekFocus,
                                    onFocused = { seekFocused = it },
                                    onScrubStart = { reveal() },
                                    onScrubEnd = { commitSeek() },
                                    onScrubCancel = { seekTargetMs = null },
                                    onSeek = { position ->
                                        if (playback.canSeek) {
                                            dpadScrubbing = false
                                            seekTargetMs = position
                                            reveal()
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    Modifier.fillMaxWidth().height(4.dp)
                                        .background(gold.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                )
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Text(
                            if (isLive) "Ao vivo" else formatPlaybackTime(playback.durationMs ?: 0L),
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = if (isCompactScreen) 16.sp else 18.sp
                        )
                    }

                    // Os controles de transporte permanecem CENTRALIZADOS na area util da TV,
                    // independente de quantos botoes auxiliares estiverem disponiveis.
                    // Os demais controles se ancoram a direita: nao deslocam o PAUSE.
                    // Em telas muito estreitas, uma segunda linha evita sobreposicao.
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val showEpisodeButtons = !isLive && media.type == PlaybackContentType.EPISODE && onChangeEpisode != null
                        val showSeekButtons = !isLive && playback.canSeek
                        val buttonWidth = if (isCompactScreen) 49.dp else 54.dp
                        val transportGap = if (isCompactScreen) 8.dp else 13.dp
                        val transportWidth = (if (isCompactScreen) 70.dp else 76.dp) +
                            (buttonWidth + transportGap) * ((if (showEpisodeButtons) 2 else 0) + (if (showSeekButtons) 2 else 0))
                        val auxiliaryWidth = buttonWidth * (2 + (if (showAudioButton) 1 else 0) + (if (showSubtitleButton) 1 else 0)) +
                            (if (isCompactScreen) 8.dp else 13.dp) +
                            (if (isCompactScreen) 5.dp else 13.dp) * ((if (showAudioButton) 1 else 0) + (if (showSubtitleButton) 1 else 0))
                        val narrowControls = maxWidth < transportWidth + (auxiliaryWidth + 12.dp) * 2
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .height(if (isCompactScreen) 74.dp else 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (showEpisodeButtons) {
                                        PlayerIconButton(
                                            icon = PlayerGlyph.PREVIOUS,
                                            label = "Episódio anterior",
                                            gold = gold,
                                            compact = isCompactScreen
                                        ) { onChangeEpisode?.invoke(-1, playback.positionMs) }
                                        Spacer(Modifier.width(transportGap))
                                    }
                                    if (showSeekButtons) {
                                        PlayerIconButton(
                                            icon = PlayerGlyph.BACK,
                                            label = "Voltar 10 segundos",
                                            gold = gold,
                                            compact = isCompactScreen
                                        ) { seek(-10_000L) }
                                        Spacer(Modifier.width(transportGap))
                                    }
                                    PlayerIconButton(
                                        icon = if (playback.isPlaying) PlayerGlyph.PAUSE else PlayerGlyph.PLAY,
                                        label = if (playback.isPlaying) "Pausar" else "Reproduzir",
                                        gold = gold,
                                        focusRequester = playFocus,
                                        prominent = true,
                                        compact = isCompactScreen
                                    ) { togglePlayback() }
                                    if (showSeekButtons) {
                                        Spacer(Modifier.width(transportGap))
                                        PlayerIconButton(
                                            icon = PlayerGlyph.FORWARD,
                                            label = "Avançar 10 segundos",
                                            gold = gold,
                                            compact = isCompactScreen
                                        ) { seek(10_000L) }
                                    }
                                    if (showEpisodeButtons) {
                                        Spacer(Modifier.width(transportGap))
                                        PlayerIconButton(
                                            icon = PlayerGlyph.NEXT,
                                            label = "Próximo episódio",
                                            gold = gold,
                                            compact = isCompactScreen
                                        ) { onChangeEpisode?.invoke(1, playback.positionMs) }
                                    }
                                }
                                if (!narrowControls) {
                                    PlayerAuxiliaryButtons(
                                        modifier = Modifier.align(Alignment.CenterEnd),
                                        gold = gold,
                                        compact = isCompactScreen,
                                        showAudio = showAudioButton,
                                        showSubtitles = showSubtitleButton,
                                        onAudio = { panel = PlayerPanel.AUDIO; reveal() },
                                        onSubtitles = { panel = PlayerPanel.SUBTITLES; reveal() },
                                        onAspect = { panel = PlayerPanel.ASPECT; reveal() },
                                        onSettings = { panel = PlayerPanel.MAIN; reveal() }
                                    )
                                }
                            }
                            if (narrowControls) {
                                PlayerAuxiliaryButtons(
                                    gold = gold,
                                    compact = isCompactScreen,
                                    showAudio = showAudioButton,
                                    showSubtitles = showSubtitleButton,
                                    onAudio = { panel = PlayerPanel.AUDIO; reveal() },
                                    onSubtitles = { panel = PlayerPanel.SUBTITLES; reveal() },
                                    onAspect = { panel = PlayerPanel.ASPECT; reveal() },
                                    onSettings = { panel = PlayerPanel.MAIN; reveal() }
                                )
                            }
                        }
                    }
                }
            }

            if (panel != PlayerPanel.NONE) {
                // Fundo levemente escurecido: permite ver o video enquanto o menu
                // permanece legivel, sem modificar a SurfaceView HDR do player.
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f))
                        .clickable { dismissOverlaysToVideo() }
                )
                val menuGlassShape = RoundedCornerShape(14.dp)
                val panelWidth = (LocalConfiguration.current.screenWidthDp *
                    if (isCompactScreen) 0.84f else if (panel == PlayerPanel.INFO) 0.40f else 0.32f
                ).dp.coerceAtMost(if (panel == PlayerPanel.INFO) 560.dp else 420.dp)
                val menuTitle = when (panel) {
                    PlayerPanel.MAIN -> "OPÇÕES"
                    PlayerPanel.ENGINE -> "REPRODUTOR"
                    PlayerPanel.AUDIO -> "FAIXAS DE ÁUDIO"
                    PlayerPanel.SUBTITLES -> "LEGENDAS"
                    PlayerPanel.SUBTITLE_STYLE -> "ESTILO DAS LEGENDAS"
                    PlayerPanel.SPEED -> "VELOCIDADE"
                    PlayerPanel.ASPECT -> "FORMATO DE TELA"
                    PlayerPanel.MANUAL -> "AJUSTE MANUAL"
                    PlayerPanel.INFO -> "INFORMAÇÕES TÉCNICAS"
                    PlayerPanel.NONE -> "OPÇÕES"
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(panelWidth)
                        .then(
                            if (panel == PlayerPanel.INFO) Modifier.fillMaxHeight()
                            else Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp - 24).coerceAtLeast(120).dp)
                        )
                        .padding(
                            start = if (isCompactScreen) 7.dp else 10.dp,
                            end = if (isCompactScreen) 8.dp else 16.dp,
                            top = 12.dp,
                            bottom = 12.dp
                        )
                        .shadow(
                            elevation = 10.dp,
                            shape = menuGlassShape,
                            ambientColor = Color.Black.copy(alpha = 0.35f),
                            spotColor = Color.Black.copy(alpha = 0.45f)
                        )
                        .clip(menuGlassShape)
                        .background(playerGlassBrush())
                        .border(1.1.dp, playerGoldFrameBrush(), menuGlassShape)
                        .then(
                            if (panel == PlayerPanel.INFO || panel == PlayerPanel.SUBTITLE_STYLE) Modifier
                            else Modifier.verticalScroll(menuScrollState)
                        )
                        .padding(
                            horizontal = 14.dp,
                            vertical = 16.dp
                        )
                ) {
                    Text(menuTitle, color = gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier.fillMaxWidth().height(1.dp).background(
                            Brush.horizontalGradient(
                                listOf(
                                    gold.copy(alpha = 0.57f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                    )
                    Spacer(Modifier.height(3.dp))
                    when (panel) {
                        PlayerPanel.MAIN -> {
                            PlayerMenuItem("Reprodutor", "Exo Player ou VLC", gold, panelFocus, PlayerMenuGlyph.PLAYER) {
                                panel = PlayerPanel.ENGINE
                            }
                            if (showAudioButton) {
                                PlayerMenuItem("Faixas de áudio", "Selecionar idioma e faixa", gold, glyph=if(PlayerChrome.kids)null else PlayerMenuGlyph.AUDIO) {
                                    panel = PlayerPanel.AUDIO
                                }
                            }
                            if (showSubtitleButton) {
                                PlayerMenuItem(
                                    "Legendas",
                                    when {
                                        activeExternal != null -> "Externa: ${activeExternal!!.language}"
                                        playback.selectedSubtitleTrackId != null -> "Ativadas"
                                        searchingExternal -> "Procurando externas"
                                        else -> "Desativadas"
                                    },
                                    gold, glyph=if(PlayerChrome.kids)null else PlayerMenuGlyph.SUBTITLES
                                ) { panel = PlayerPanel.SUBTITLES }
                            }
                            PlayerMenuItem("Velocidade", "${speed}×", gold, glyph = PlayerMenuGlyph.SPEED) {
                                panel = PlayerPanel.SPEED
                            }
                            PlayerMenuItem("Formato de tela", when (currentLayout.mode) {
                                PlaybackAspectMode.NORMAL -> "Normal"
                                PlaybackAspectMode.FULL_SCREEN -> "Tela cheia"
                                PlaybackAspectMode.MANUAL -> "Ajuste manual"
                            }, gold, glyph = PlayerMenuGlyph.SCREEN) {
                                panel = PlayerPanel.ASPECT
                            }
                            PlayerMenuItem("Informações técnicas", "Resolução, codec e buffer", gold, glyph = PlayerMenuGlyph.INFO) {
                                panel = PlayerPanel.INFO
                            }
                            PlayerMenuItem("Fechar", "Voltar ao vídeo", gold, glyph = PlayerMenuGlyph.CLOSE) {
                                dismissOverlaysToVideo()
                            }
                        }
                        PlayerPanel.ENGINE -> {
                            listOf(PlaybackEngine.MEDIA3, PlaybackEngine.VLC).forEachIndexed { index, engine ->
                                PlayerMenuItem(
                                    label = if (engine == PlaybackEngine.MEDIA3) "Exo Player" else "VCL",
                                    description = if (engine == playback.engine) "Em utilização" else "Alternar mecanismo",
                                    accent = gold,
                                    focusRequester = if (index == 0) panelFocus else null
                                ) {
                                    val result = controller.switchEngine(engine)
                                    if (result != PlaybackCommandResult.ACCEPTED) feedback = "Troca indisponível"
                                    panel = PlayerPanel.NONE
                                    reveal()
                                }
                            }
                        }
                        PlayerPanel.AUDIO -> {
                            // A seleção padrão restaura o controle automático do motor.
                            PlayerMenuItem(
                                "Automático (padrão)",
                                "Deixar o reprodutor escolher a faixa",
                                gold,
                                panelFocus
                            ) {
                                val result = controller.selectTrack(PlaybackTrackType.AUDIO, null)
                                if (result == PlaybackCommandResult.ACCEPTED) {
                                    panel = PlayerPanel.NONE
                                } else feedback = "Não foi possível selecionar o áudio padrão"
                                reveal()
                            }
                            audioTracks.forEachIndexed { index, track ->
                                PlayerMenuItem(
                                    trackPresentationName(track, index, audioTrack = true),
                                    if (track.id == playback.selectedAudioTrackId) "Em uso" else "Selecionar áudio",
                                    gold
                                ) {
                                    val result = controller.selectTrack(PlaybackTrackType.AUDIO, track.id)
                                    if (result == PlaybackCommandResult.ACCEPTED) {
                                        panel = PlayerPanel.NONE
                                    } else feedback = "Esta faixa de áudio não pôde ser selecionada"
                                    reveal()
                                }
                            }
                        }
                        PlayerPanel.SUBTITLES -> {
                            PlayerMenuItem("Estilo", "Fonte, cor, fundo e tamanho", gold, panelFocus) {
                                panel = PlayerPanel.SUBTITLE_STYLE
                            }
                            PlayerMenuItem(
                                "Desativadas",
                                if (playback.selectedSubtitleTrackId == null && activeExternal == null) "Selecionada" else "Desligar legendas",
                                gold
                            ) {
                                activeExternal = null
                                externalCues = emptyList()
                                val result = controller.selectTrack(PlaybackTrackType.SUBTITLE, null)
                                if (result == PlaybackCommandResult.ACCEPTED) {
                                    panel = PlayerPanel.NONE
                                } else feedback = "Não foi possível desativar as legendas"
                                reveal()
                            }
                            subtitleTracks.forEachIndexed { index, track ->
                                PlayerMenuItem(
                                    trackPresentationName(track, index),
                                    if (track.id == playback.selectedSubtitleTrackId) "Selecionada" else "Ativar legenda",
                                    gold
                                ) {
                                    val result = controller.selectTrack(PlaybackTrackType.SUBTITLE, track.id)
                                    if (result == PlaybackCommandResult.ACCEPTED) {
                                        activeExternal = null
                                        externalCues = emptyList()
                                        panel = PlayerPanel.NONE
                                    } else feedback = "Esta legenda não pôde ser selecionada"
                                    reveal()
                                }
                            }
                            if (searchingExternal) {
                                Text("Buscando legendas externas…", color = Color.LightGray, fontSize = 13.sp)
                            }
                            if (downloadingExternal) {
                                Text("Baixando legenda…", color = Color.LightGray, fontSize = 13.sp)
                            }
                            externalOptions.forEach { option ->
                                PlayerMenuItem(
                                    option.displayName,
                                    if (activeExternal == option) "Em uso" else "Legenda externa",
                                    gold
                                ) {
                                    if (!downloadingExternal) {
                                        downloadingExternal = true
                                        subtitleScope.launch {
                                            try {
                                                val cues = externalSubtitleRepo.download(option)
                                                if (cues.isNotEmpty()) {
                                                    controller.selectTrack(PlaybackTrackType.SUBTITLE, null)
                                                    externalCues = cues
                                                    activeExternal = option
                                                    panel = PlayerPanel.NONE
                                                } else {
                                                    feedback = "Este arquivo de legenda não está disponível"
                                                }
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (_: Exception) {
                                                feedback = "Não foi possível baixar esta legenda"
                                            } finally { downloadingExternal = false }
                                        }
                                    }
                                    reveal()
                                }
                            }
                        }
                        PlayerPanel.SUBTITLE_STYLE -> {
                            Text("PRÉVIA", color = gold, fontSize = 11.sp)
                            StyledSubtitleText("Esta é a sua legenda.", subtitleAppearance,
                                Modifier.fillMaxWidth().height(100.dp)
                                    .background(Brush.verticalGradient(listOf(PlayerChrome.surface(0xFF32363B), PlayerChrome.surface(0xFF171A20))), RoundedCornerShape(8.dp)))
                            Column(
                                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                            PlayerMenuItem("Fundo", subtitleAppearance.background.label + " • alterar", gold, panelFocus) {
                                val modes = SubtitleBackground.entries
                                updateSubtitleAppearance(subtitleAppearance.copy(background = modes[(subtitleAppearance.background.ordinal + 1) % modes.size]))
                            }
                            PlayerMenuItem("Cor da letra", (subtitleTextColors.firstOrNull { it.argb == subtitleAppearance.textArgb }?.label ?: "Branco") + " • alterar", gold) {
                                val index = subtitleTextColors.indexOfFirst { it.argb == subtitleAppearance.textArgb }
                                updateSubtitleAppearance(subtitleAppearance.copy(textArgb = subtitleTextColors[(index + 1) % subtitleTextColors.size].argb))
                            }
                            PlayerMenuItem("Tamanho", "${subtitleAppearance.sizeSp} • alterar", gold) {
                                val index = subtitleSizeChoices.indexOf(subtitleAppearance.sizeSp)
                                updateSubtitleAppearance(subtitleAppearance.copy(sizeSp = subtitleSizeChoices[(index + 1) % subtitleSizeChoices.size]))
                            }
                            PlayerMenuItem("Fonte", subtitleAppearance.typeface.label + " (${subtitleAppearance.typeface.ordinal + 1}/3)", gold) {
                                val fonts = SubtitleTypeface.entries
                                updateSubtitleAppearance(subtitleAppearance.copy(typeface = fonts[(subtitleAppearance.typeface.ordinal + 1) % fonts.size]))
                            }
                            PlayerMenuItem("Acabamento", subtitleAppearance.edge.label + " • alterar", gold) {
                                val edges = SubtitleEdge.entries
                                updateSubtitleAppearance(subtitleAppearance.copy(edge = edges[(subtitleAppearance.edge.ordinal + 1) % edges.size]))
                            }
                            if (playback.engine == PlaybackEngine.VLC && activeExternal == null) {
                                Text("No VCL, este estilo vale para legendas externas. Para personalizar legendas internas, use Exo Player.", color = Color.LightGray, fontSize = 11.sp)
                            }
                            if (playback.subtitleCues.any { it.bitmap != null }) {
                                Text("Legendas em imagem mantêm a aparência original.", color = Color.LightGray, fontSize = 11.sp)
                            }
                            PlayerMenuItem("Restaurar padrão", "Branco, fundo transparente e contorno", gold) {
                                updateSubtitleAppearance(SubtitleAppearance())
                            }
                            PlayerMenuItem("Voltar", "Legendas", gold) { panel = PlayerPanel.SUBTITLES }
                            }
                        }
                        PlayerPanel.SPEED -> {
                            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEachIndexed { index, option ->
                                PlayerMenuItem(
                                    label = "${option}×",
                                    description = if (option == speed) "Selecionada" else "",
                                    accent = gold,
                                    focusRequester = if (index == 0) panelFocus else null
                                ) {
                                    val result = controller.setPlaybackSpeed(option)
                                    if (result == PlaybackCommandResult.ACCEPTED) {
                                        speed = option
                                    } else {
                                        feedback = "Velocidade indisponível nesta transmissão"
                                    }
                                    panel = PlayerPanel.NONE
                                    reveal()
                                }
                            }
                        }
                        PlayerPanel.ASPECT -> {
                            PlayerMenuItem("Normal", "Proporção original, sem cortes", gold, panelFocus) {
                                changeAspect(PlaybackAspectMode.NORMAL)
                                panel = PlayerPanel.NONE
                            }
                            PlayerMenuItem("Tela cheia", "Preenche a tela; pode cortar as bordas", gold) {
                                changeAspect(PlaybackAspectMode.FULL_SCREEN)
                                panel = PlayerPanel.NONE
                            }
                            PlayerMenuItem("Ajuste manual", "Zoom, largura, altura e posição", gold) {
                                changeAspect(PlaybackAspectMode.MANUAL)
                                panel = PlayerPanel.MANUAL
                            }
                            PlayerMenuItem("Voltar", "Menu principal", gold) {
                                panel = PlayerPanel.MAIN
                            }
                        }
                        PlayerPanel.MANUAL -> {
                            val manual = currentLayout.manual
                            PlayerAdjustRow(
                                label = "Zoom", value = manual.zoom,
                                gold = gold, focusRequester = panelFocus,
                                onDecrease = { adjustManual(manual.copy(zoom = (manual.zoom - 0.1f).coerceIn(0.25f, 4f))) },
                                onIncrease = { adjustManual(manual.copy(zoom = (manual.zoom + 0.1f).coerceIn(0.25f, 4f))) }
                            )
                            PlayerAdjustRow(
                                label = "Largura", value = manual.widthScale,
                                gold = gold,
                                onDecrease = { adjustManual(manual.copy(widthScale = (manual.widthScale - 0.1f).coerceIn(0.25f, 4f))) },
                                onIncrease = { adjustManual(manual.copy(widthScale = (manual.widthScale + 0.1f).coerceIn(0.25f, 4f))) }
                            )
                            PlayerAdjustRow(
                                label = "Altura", value = manual.heightScale,
                                gold = gold,
                                onDecrease = { adjustManual(manual.copy(heightScale = (manual.heightScale - 0.1f).coerceIn(0.25f, 4f))) },
                                onIncrease = { adjustManual(manual.copy(heightScale = (manual.heightScale + 0.1f).coerceIn(0.25f, 4f))) }
                            )
                            PlayerAdjustRow(
                                label = "Horizontal", value = manual.offsetX,
                                gold = gold,
                                onDecrease = { adjustManual(manual.copy(offsetX = (manual.offsetX - 0.05f).coerceIn(-1f, 1f))) },
                                onIncrease = { adjustManual(manual.copy(offsetX = (manual.offsetX + 0.05f).coerceIn(-1f, 1f))) }
                            )
                            PlayerAdjustRow(
                                label = "Vertical", value = manual.offsetY,
                                gold = gold,
                                onDecrease = { adjustManual(manual.copy(offsetY = (manual.offsetY - 0.05f).coerceIn(-1f, 1f))) },
                                onIncrease = { adjustManual(manual.copy(offsetY = (manual.offsetY + 0.05f).coerceIn(-1f, 1f))) }
                            )
                            PlayerMenuItem("Restaurar", "Voltar ao enquadramento original", gold) {
                                currentLayout = PlaybackVideoLayout()
                                panel = PlayerPanel.ASPECT
                            }
                            PlayerMenuItem("Concluído", "Voltar ao vídeo", gold) {
                                dismissOverlaysToVideo()
                            }
                        }
                        PlayerPanel.INFO -> {
                            val info = playback.technicalInfo
                            val rows = listOf(
                                "Reprodutor" to (if (playback.engine == PlaybackEngine.MEDIA3) "Exo Player" else "VCL"),
                                "Resolução" to if (info.width != null && info.height != null) "${info.width} × ${info.height}" else "Não informada",
                                "HDR" to info.hdrFormat.displayName,
                                "Vídeo" to (info.videoCodec ?: "Não informado"),
                                "Áudio" to (info.audioCodec ?: "Não informado"),
                                "Faixas de áudio" to if (audioTracks.isNotEmpty()) "${audioTracks.size} detectada(s)"
                                else "Nenhuma detectada",
                                "Faixas de legendas" to if (subtitleTracks.isNotEmpty()) "${subtitleTracks.size} detectada(s)"
                                else "Nenhuma detectada",
                                "Legendas externas" to when {
                                    searchingExternal -> "Buscando"
                                    !externalSubtitleRepo.configured -> "API não configurada"
                                    externalOptions.isNotEmpty() -> "${externalOptions.size} disponível(is)"
                                    else -> externalSearchStatus
                                },
                                "FPS" to (info.framesPerSecond?.toString() ?: "Não informado"),
                                "Bitrate" to (info.bitrateBitsPerSecond?.let { "${it / 1_000} kb/s" } ?: "Não informado"),
                                "Buffer" to (info.bufferedDurationMs?.let { "${it / 1_000} s" } ?: "Não informado")
                            )
                            // Somente os dados tecnicos sao rolaveis. Assim o
                            // foco inicial em Voltar NAO empurra o titulo para fora
                            // do painel, independentemente do tamanho da TV.
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(infoScrollState),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rows.forEach { (key, value) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.045f))
                                            .border(
                                                0.6.dp,
                                                Color.White.copy(alpha = 0.11f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 11.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = key,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(0.44f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = value,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            textAlign = TextAlign.End,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(0.56f)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            // Rodape sempre acessivel no controle remoto.
                            PlayerMenuItem("Voltar", "", gold, panelFocus) {
                                panel = PlayerPanel.MAIN
                            }
                        }
                        PlayerPanel.NONE -> Unit
                    }
                }
            }

            feedback?.let { message ->
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .padding(top = 88.dp)
                        .background(PlayerChrome.surface(0xDD202020), RoundedCornerShape(8.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    } // fim da camada visual; o controller sera liberado no DisposableEffect.
}


/** Usa o nome do provedor e apresenta o idioma em português quando identificável. */
private fun trackPresentationName(
    track: PlaybackTrack,
    index: Int,
    audioTrack: Boolean = false
): String {
    val original = track.label.trim()
    val code = track.languageTag?.trim()?.takeUnless {
        it.isBlank() || it.equals("und", true) || it.equals("mul", true)
    }
    // A faixa de audio identificada apenas como 'pt', 'por' ou 'Portugues'
    // recebe o nome desejado no player. Se a origem explicita for pt-PT,
    // respeita a identificacao Portugal; nunca traduz 'pt' como desconhecido.
    if (audioTrack) {
        val normalized = code?.lowercase(Locale.ROOT)?.replace('_', '-')
        val label = original.lowercase(Locale.ROOT)
        val isPortuguese = normalized == "pt" || normalized == "por" ||
                normalized == "pob" || normalized == "pt-br" ||
                normalized?.startsWith("pt-") == true ||
                label.contains("portugu") || label.contains("brazilian portuguese")
        if (isPortuguese) {
            return if (normalized == "pt-pt" ||
                label.contains("portugal", ignoreCase = true)) {
                "Português (Portugal)"
            } else "Português brasileiro"
        }
    }
    val language = code?.let { tag ->
        val locale = Locale.forLanguageTag(tag)
        locale.getDisplayLanguage(Locale.forLanguageTag("pt-BR"))
            .takeIf { it.isNotBlank() && it != tag }
            ?.replaceFirstChar { c -> c.titlecase(Locale.forLanguageTag("pt-BR")) }
    }
    val title = if (original.isBlank() || original.equals(code, true)) {
        language ?: "Faixa ${index + 1}"
    } else if (language != null && !original.contains(language, ignoreCase = true)) {
        "$language • $original"
    } else original
    return title
}


/** Botoes auxiliares ancorados a direita, separados do trio central de reproducao. */
@Composable
private fun PlayerTitleCard(
    media: PlaybackMedia,
    gold: Color,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val isEpisode = media.type == PlaybackContentType.EPISODE
    val mainTitle = if (isEpisode) {
        media.subtitleSearchTitle?.trim()?.takeIf { it.isNotBlank() } ?: media.title
    } else media.title
    val details = when (media.type) {
        PlaybackContentType.LIVE_CHANNEL -> "AO VIVO"
        PlaybackContentType.MOVIE -> "FILME"
        PlaybackContentType.EPISODE -> buildList {
            media.seasonNumber?.takeIf { it >= 0 }?.let {
                add(if (it == 0) "Especiais" else "Temporada $it")
            }
            media.episodeNumber?.takeIf { it > 0 }?.let { add("Episódio $it") }
        }.joinToString(" · ").ifBlank { "SÉRIE" }
    }
    val maxWidth = (LocalConfiguration.current.screenWidthDp * if (compact) 0.88f else 0.64f).dp
    val shape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
    Column(
        modifier = modifier
            .widthIn(max = maxWidth)
            .wrapContentWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(PlayerChrome.surface(0xAA141819), PlayerChrome.surface(0xB8090C0E)))
            )
            .border(
                1.1.dp,
                Brush.linearGradient(
                    listOf(PlayerChrome.borderColor(0xFFFFF4BD), gold, PlayerChrome.borderColor(0xFFFFE991), PlayerChrome.borderColor(0xFFB98132))
                ),
                shape
            )
            .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = if (compact) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = mainTitle,
            color = if(PlayerChrome.kids)Color(0xFFFFF9EC)else Color.White,
            fontSize = if (compact) 18.sp else 21.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = details,
            color = gold,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Fixed neutral player chrome for both catalog backgrounds; Kids retains its own palette.
 * Changes only painted controls, never the video surface or subtitle appearance. */
internal object PlayerChrome {
    val kids get()=br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active
    fun color(original:Long):Color=if(kids)br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(original)else Color.White.copy(alpha=Color(original).alpha)
    fun borderColor(original:Long):Color=if(kids)br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(original)else Color.White.copy(alpha=Color(original).alpha)
    fun surface(original:Long):Color=if(kids)br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(original)else Color(0xCC111417)
    fun glassColor(original:Long):Color=if(kids)br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.glassColor(original)else Color(0xCC111417)
}

// BEGIN_PLAYER_SHARED_PALETTE
private fun playerGlassBrush(): Brush = Brush.verticalGradient(
    listOf(PlayerChrome.surface(0x701E211F), PlayerChrome.glassColor(0x80110F0D), PlayerChrome.glassColor(0x95090807))
)

private fun playerGoldFrameBrush(alpha: Float = 1f): Brush = Brush.linearGradient(
    listOf(PlayerChrome.borderColor(0xFF7A5215), PlayerChrome.borderColor(0xFFFFE6A0), PlayerChrome.borderColor(0xFFD6A32E), PlayerChrome.borderColor(0xFFFFF5CD), PlayerChrome.borderColor(0xFF926315))
        .map { it.copy(alpha = alpha) }
)

private fun playerGoldCircleBrush(): Brush = Brush.sweepGradient(
    listOf(PlayerChrome.color(0xFF7A5215), PlayerChrome.color(0xFFFFE6A0), PlayerChrome.color(0xFFD6A32E), PlayerChrome.color(0xFFFFF5CD), PlayerChrome.color(0xFF926315))
)

private fun playerGoldSelectionBrush(alpha: Float = 1f): Brush =
    if(PlayerChrome.kids)br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.brush(alpha)
    else Brush.verticalGradient(listOf(Color.White.copy(alpha=.12f*alpha),Color.White.copy(alpha=.08f*alpha)))

private fun playerGoldIconBrush(color: Color): Brush = Brush.verticalGradient(
    listOf(PlayerChrome.borderColor(0xFF7A5215), PlayerChrome.borderColor(0xFFFFE6A0), PlayerChrome.borderColor(0xFFD6A32E), PlayerChrome.borderColor(0xFFFFF5CD), PlayerChrome.borderColor(0xFF926315))
        .map { it.copy(alpha = color.alpha) }
)
// END_PLAYER_SHARED_PALETTE

@Composable
private fun PlayerCinemaPanel(
    compact: Boolean,
    gold: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 12.dp else 14.dp)
    val goldRim = playerGoldFrameBrush()
    val visualScale = 0.5f
    val density = LocalDensity.current
    val originalGapPx = with(density) { 5.dp.roundToPx() }

    Layout(
        content = content,
        modifier = modifier
            .clip(shape)
            .background(playerGlassBrush())
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, gold.copy(alpha = 0.76f), Color.Transparent)
                    ),
                    start = Offset(size.width * 0.08f, size.height - 1.dp.toPx()),
                    end = Offset(size.width * 0.92f, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .border(1.1.dp, goldRim, shape)
            .padding(horizontal = if (compact) 14.dp else 22.dp, vertical = 6.5.dp)
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(
            minWidth = (constraints.minWidth / visualScale).toInt(),
            maxWidth = (constraints.maxWidth / visualScale).toInt(),
            minHeight = 0
        )
        val children = measurables.map { it.measure(childConstraints) }
        val originalHeight = children.sumOf { it.height } +
            originalGapPx * (children.size - 1).coerceAtLeast(0)
        // Mantém as proporções aprovadas, agora com metade da altura visual.
        val extraHeight = 0
        val width = kotlin.math.round((children.maxOfOrNull { it.width } ?: 0) * visualScale).toInt()
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = kotlin.math.round((originalHeight + extraHeight) * visualScale).toInt()
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var y = 0
            children.forEachIndexed { index, child ->
                child.placeRelativeWithLayer(0, kotlin.math.round(y * visualScale).toInt()) {
                    scaleX = visualScale
                    scaleY = visualScale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                y += child.height
                if (index < children.lastIndex) {
                    y += originalGapPx
                }
            }
        }
    }
}

@Composable
private fun PlayerAuxiliaryButtons(
    gold: Color,
    compact: Boolean,
    showAudio: Boolean,
    showSubtitles: Boolean,
    modifier: Modifier = Modifier,
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
    onAspect: () -> Unit,
    onSettings: () -> Unit
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (showAudio) {
            PlayerIconButton(PlayerGlyph.AUDIO, "Faixas de áudio", gold, compact = compact, onClick = onAudio)
            Spacer(Modifier.width(if (compact) 5.dp else 13.dp))
        }
        if (showSubtitles) {
            PlayerIconButton(PlayerGlyph.SUBTITLES, "Legendas", gold, compact = compact, onClick = onSubtitles)
            Spacer(Modifier.width(if (compact) 5.dp else 13.dp))
        }
        PlayerIconButton(PlayerGlyph.ASPECT, "Formato de tela", gold, compact = compact, onClick = onAspect)
        Spacer(Modifier.width(if (compact) 8.dp else 13.dp))
        PlayerIconButton(PlayerGlyph.SETTINGS, "Opções", gold, compact = compact, onClick = onSettings)
    }
}

internal enum class PlayerGlyph { PREVIOUS, NEXT, BACK, PLAY, PAUSE, FORWARD, AUDIO, SUBTITLES, ASPECT, SETTINGS }

/** Icones vetoriais proprios: nenhuma dependencia extra de Material Icons. */
@Composable
internal fun PlayerIconButton(
    icon: PlayerGlyph,
    label: String,
    gold: Color,
    focusRequester: FocusRequester? = null,
    prominent: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = CircleShape
    val requestModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else Modifier
    val buttonSize = when {
        prominent && compact -> 70.dp
        prominent -> 76.dp
        compact -> 49.dp
        else -> 54.dp
    }
    val activeGlow = focused
    Box(
        modifier = Modifier
            .then(requestModifier)
            .semantics { contentDescription = label }
            .size(buttonSize)
            .onFocusChanged { focused = it.isFocused }
            // Halo dourado curto: brilho discreto apenas ao redor da borda;
            // sem espalhar uma mancha de luz pelo painel inteiro.
            .drawBehind {
                if (activeGlow) {
                    val haloRadius = size.minDimension * 0.66f
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to gold.copy(alpha = if (prominent) 0.34f else 0.18f),
                            0.68f to gold.copy(alpha = if (prominent) 0.15f else 0.07f),
                            1.0f to Color.Transparent,
                            center = center,
                            radius = haloRadius
                        ),
                        radius = haloRadius
                    )
                }
            }
            .shadow(
                elevation = if (focused && prominent) 5.dp else if (focused) 3.dp else 0.dp,
                shape = shape,
                ambientColor = gold.copy(alpha = if (activeGlow) 0.24f else 0f),
                spotColor = gold.copy(alpha = if (activeGlow) 0.23f else 0f)
            )
            .clip(shape)
            .background(
                if (focused) {
                    // Fundo ambar radial, aro dourado iluminado: referencia.
                    playerGoldSelectionBrush()
                } else {
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .border(
                width = if (focused && prominent) 2.dp else if (focused) 1.7.dp else 1.2.dp,
                brush = playerGoldCircleBrush(),
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PlayerGlyphDrawing(
            glyph = icon,
            color = if (focused) PlayerChrome.color(0xFFFFF4BD) else PlayerChrome.color(0xFFFFE991),
            // Ícones a 90% do desenho anterior; área clicável permanece igual.
            modifier = Modifier.size(if (compact) 23.dp else 26.dp)
        )
    }
}

@Composable
private fun PlayerGlyphDrawing(
    glyph: PlayerGlyph,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val iconBrush = playerGoldIconBrush(color)
        val w = size.width
        val h = size.height
        fun triangle(points: List<Offset>) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                close()
            }
            drawPath(path, iconBrush)
        }
        when (glyph) {
            PlayerGlyph.PLAY -> triangle(
                listOf(Offset(w * .25f, h * .12f), Offset(w * .85f, h * .5f), Offset(w * .25f, h * .88f))
            )
            PlayerGlyph.PAUSE -> {
                drawRoundRect(iconBrush, Offset(w * .22f, h * .16f), Size(w * .20f, h * .68f), CornerRadius(w * .04f))
                drawRoundRect(iconBrush, Offset(w * .58f, h * .16f), Size(w * .20f, h * .68f), CornerRadius(w * .04f))
            }
            PlayerGlyph.PREVIOUS -> {
                drawRect(iconBrush, Offset(w * .10f, h * .17f), Size(w * .12f, h * .66f))
                triangle(listOf(Offset(w * .86f, h * .17f), Offset(w * .27f, h * .5f), Offset(w * .86f, h * .83f)))
            }
            PlayerGlyph.NEXT -> {
                triangle(listOf(Offset(w * .14f, h * .17f), Offset(w * .73f, h * .5f), Offset(w * .14f, h * .83f)))
                drawRect(iconBrush, Offset(w * .78f, h * .17f), Size(w * .12f, h * .66f))
            }
            PlayerGlyph.BACK -> {
                triangle(listOf(Offset(w * .47f, h * .17f), Offset(w * .10f, h * .5f), Offset(w * .47f, h * .83f)))
                triangle(listOf(Offset(w * .91f, h * .17f), Offset(w * .54f, h * .5f), Offset(w * .91f, h * .83f)))
            }
            PlayerGlyph.FORWARD -> {
                triangle(listOf(Offset(w * .09f, h * .17f), Offset(w * .46f, h * .5f), Offset(w * .09f, h * .83f)))
                triangle(listOf(Offset(w * .53f, h * .17f), Offset(w * .90f, h * .5f), Offset(w * .53f, h * .83f)))
            }
            PlayerGlyph.AUDIO -> {
                // Alto-falante com duas ondas: não depende de Material Icons.
                val speaker = Path().apply {
                    moveTo(w * .12f, h * .37f)
                    lineTo(w * .34f, h * .37f)
                    lineTo(w * .57f, h * .17f)
                    lineTo(w * .57f, h * .83f)
                    lineTo(w * .34f, h * .63f)
                    lineTo(w * .12f, h * .63f)
                    close()
                }
                drawPath(speaker, color)
                drawArc(iconBrush, -65f, 130f, false,
                    Offset(w * .44f, h * .22f), Size(w * .35f, h * .56f),
                    style = Stroke(width = w * .075f))
                drawArc(iconBrush, -60f, 120f, false,
                    Offset(w * .43f, h * .08f), Size(w * .53f, h * .84f),
                    style = Stroke(width = w * .065f))
            }
            PlayerGlyph.SUBTITLES -> {
                drawRoundRect(iconBrush, Offset(w * .07f, h * .18f), Size(w * .86f, h * .65f),
                    CornerRadius(w * .09f), style = Stroke(width = w * .085f))
                drawLine(iconBrush, Offset(w * .20f, h * .48f), Offset(w * .44f, h * .48f), w * .10f)
                drawLine(iconBrush, Offset(w * .56f, h * .48f), Offset(w * .80f, h * .48f), w * .10f)
                drawLine(iconBrush, Offset(w * .20f, h * .66f), Offset(w * .80f, h * .66f), w * .08f)
            }
            PlayerGlyph.ASPECT -> {
                // Quatro cantos de tela cheia, como na referencia aprovada.
                val thickness = w * .085f
                drawLine(iconBrush, Offset(w * .09f, h * .38f), Offset(w * .09f, h * .09f), thickness)
                drawLine(iconBrush, Offset(w * .09f, h * .09f), Offset(w * .38f, h * .09f), thickness)
                drawLine(iconBrush, Offset(w * .62f, h * .09f), Offset(w * .91f, h * .09f), thickness)
                drawLine(iconBrush, Offset(w * .91f, h * .09f), Offset(w * .91f, h * .38f), thickness)
                drawLine(iconBrush, Offset(w * .09f, h * .62f), Offset(w * .09f, h * .91f), thickness)
                drawLine(iconBrush, Offset(w * .09f, h * .91f), Offset(w * .38f, h * .91f), thickness)
                drawLine(iconBrush, Offset(w * .62f, h * .91f), Offset(w * .91f, h * .91f), thickness)
                drawLine(iconBrush, Offset(w * .91f, h * .62f), Offset(w * .91f, h * .91f), thickness)
            }
            PlayerGlyph.SETTINGS -> {
                drawCircle(iconBrush, radius = w * .28f, style = Stroke(width = w * .12f))
                drawCircle(iconBrush, radius = w * .10f)
                for (i in 0 until 8) {
                    val angle = (i * Math.PI / 4.0).toFloat()
                    val cx = w * .5f
                    val cy = h * .5f
                    val c = cos(angle)
                    val s = sin(angle)
                    drawLine(
                        color,
                        Offset(cx + c * w * .33f, cy + s * h * .33f),
                        Offset(cx + c * w * .45f, cy + s * h * .45f),
                        strokeWidth = w * .10f
                    )
                }
            }
        }
    }
}

private enum class PlayerPanel { NONE, MAIN, ENGINE, AUDIO, SUBTITLES, SPEED, ASPECT, MANUAL, INFO, SUBTITLE_STYLE }

@Composable
private fun PlayerControlButton(
    label: String,
    accent: Color,
    focusRequester: FocusRequester? = null,
    prominent: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    Box(
        modifier = Modifier
            .then(focusModifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(
                if (focused) br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color()
                else if (prominent) accent.copy(alpha = 0.24f)
                else Color.White.copy(alpha = 0.10f)
            )
            .border(1.dp, if (focused || prominent) accent else Color.White.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 19.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (focused) Color.White else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Simbolos vetoriais discretos nos mesmos circulos fumê do controle. */
internal enum class PlayerMenuGlyph { PLAYER, AUDIO, SUBTITLES, SPEED, SCREEN, INFO, CLOSE }

@Composable
private fun PlayerMenuGlyphDrawing(
    glyph: PlayerMenuGlyph,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (glyph == PlayerMenuGlyph.SCREEN || glyph == PlayerMenuGlyph.AUDIO || glyph == PlayerMenuGlyph.SUBTITLES) {
        PlayerGlyphDrawing(when(glyph){PlayerMenuGlyph.AUDIO->PlayerGlyph.AUDIO;PlayerMenuGlyph.SUBTITLES->PlayerGlyph.SUBTITLES;else->PlayerGlyph.ASPECT}, color, modifier)
        return
    }
    Canvas(modifier) {
        val iconBrush = playerGoldIconBrush(color)
        val w = size.width
        val h = size.height
        val stroke = w * 0.095f
        when (glyph) {
            PlayerMenuGlyph.PLAYER -> {
                drawRoundRect(
                    color, Offset(w * 0.10f, h * 0.16f),
                    Size(w * 0.80f, h * 0.58f),
                    CornerRadius(w * 0.085f), style = Stroke(stroke)
                )
                drawLine(iconBrush, Offset(w * 0.37f, h * 0.89f), Offset(w * 0.63f, h * 0.89f), stroke)
                drawLine(iconBrush, Offset(w * 0.50f, h * 0.74f), Offset(w * 0.50f, h * 0.88f), stroke)
                val play = Path().apply {
                    moveTo(w * 0.43f, h * 0.29f)
                    lineTo(w * 0.65f, h * 0.45f)
                    lineTo(w * 0.43f, h * 0.61f)
                    close()
                }
                drawPath(play, color)
            }
            PlayerMenuGlyph.SPEED -> {
                drawArc(
                    color, startAngle = 155f, sweepAngle = 230f,
                    useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.15f),
                    size = Size(w * 0.76f, h * 0.76f),
                    style = Stroke(width = stroke)
                )
                drawLine(iconBrush, Offset(w * 0.49f, h * 0.54f), Offset(w * 0.73f, h * 0.31f), stroke)
                drawCircle(iconBrush, radius = w * 0.075f, center = Offset(w * 0.49f, h * 0.54f))
            }
            PlayerMenuGlyph.INFO -> {
                drawCircle(iconBrush, radius = w * 0.39f, style = Stroke(width = stroke))
                drawCircle(iconBrush, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.30f))
                drawLine(iconBrush, Offset(w * 0.5f, h * 0.46f), Offset(w * 0.5f, h * 0.70f), stroke)
            }
            PlayerMenuGlyph.CLOSE -> {
                drawLine(iconBrush, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), stroke)
                drawLine(iconBrush, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), stroke)
            }
            PlayerMenuGlyph.SCREEN, PlayerMenuGlyph.AUDIO, PlayerMenuGlyph.SUBTITLES -> Unit
        }
    }
}

@Composable
internal fun PlayerMenuItem(
    label: String,
    description: String,
    accent: Color,
    focusRequester: FocusRequester? = null,
    glyph: PlayerMenuGlyph? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier else Modifier.graphicsLayer { alpha = 0.45f })
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            // O halo permanece pequeno e encostado no contorno, como no player.
            .shadow(
                elevation = if (focused) 3.dp else 0.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = if (focused) 0.18f else 0f),
                spotColor = accent.copy(alpha = if (focused) 0.18f else 0f)
            )
            .clip(shape)
            .background(
                if (focused) playerGoldSelectionBrush()
                else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .border(
                width = if (focused) 1.2.dp else 0.8.dp,
                brush = playerGoldFrameBrush(alpha = if (focused) 1f else if(PlayerChrome.kids)0.50f else 0.30f),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (glyph != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (focused) playerGoldSelectionBrush() else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)))
                        .border(
                            width = 0.8.dp,
                            brush = playerGoldCircleBrush(),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerMenuGlyphDrawing(
                        glyph = glyph,
                        color = if (focused) PlayerChrome.color(0xFFFFF4BD) else PlayerChrome.color(0xFFFFE991),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = if (focused) Color.White else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (description.isNotBlank()) {
                    Text(
                        description,
                        color = Color(0xFFF2F2F2),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if(!PlayerChrome.kids)Text("›",color=Color.White.copy(alpha=.9f),fontSize=24.sp)
        }
    }
}

@Composable
internal fun PlayerAdjustRow(
    label: String,
    value: Float,
    gold: Color,
    focusRequester: FocusRequester? = null,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "$label: ${String.format(Locale.ROOT, "%.2f", value)}",
            color = Color.White,
            fontSize = 13.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerControlButton("−", gold, focusRequester = focusRequester, onClick = onDecrease)
            PlayerControlButton("+", gold, onClick = onIncrease)
        }
    }
}

@Composable
private fun PlayerProgressBar(
    progress: Float,
    durationMs: Long,
    gold: Color,
    focusRequester: FocusRequester,
    onFocused: (Boolean) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    onScrubCancel: () -> Unit,
    onSeek: (Long) -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val currentSeek by rememberUpdatedState(onSeek)
    val currentStart by rememberUpdatedState(onScrubStart)
    val currentEnd by rememberUpdatedState(onScrubEnd)
    val currentCancel by rememberUpdatedState(onScrubCancel)
    Canvas(
        modifier = Modifier.fillMaxWidth()
            .height(24.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                hasFocus = it.isFocused
                onFocused(it.isFocused)
            }
            .focusable()
            .pointerInput(durationMs) {
                fun update(x: Float) {
                    if (durationMs > 0L && size.width > 0) {
                        val fraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
                        currentSeek((durationMs * fraction).toLong())
                    }
                }
                detectDragGestures(
                    onDragStart = { position -> currentStart(); update(position.x) },
                    onDragEnd = { currentEnd() },
                    onDragCancel = { currentCancel() },
                    onDrag = { change, _ -> change.consume(); update(change.position.x) }
                )
            }
            .pointerInput(durationMs) {
                detectTapGestures { tap ->
                    if (durationMs > 0L && size.width > 0) {
                        val fraction = (tap.x / size.width.toFloat()).coerceIn(0f, 1f)
                        currentStart()
                        currentSeek((durationMs * fraction).toLong())
                        currentEnd()
                    }
                }
            }
    ) {
        val fraction = progress.coerceIn(0f, 1f)
        val lineHeight = 2.7.dp.toPx()
        val lineTop = (size.height - lineHeight) / 2f
        val thumbRadius = 8.dp.toPx()
        val thumbCenter = Offset(
            x = (size.width * fraction).coerceIn(
                thumbRadius + 0.5.dp.toPx(),
                (size.width - thumbRadius - 0.5.dp.toPx()).coerceAtLeast(thumbRadius + 0.5.dp.toPx())
            ),
            y = size.height / 2f
        )
        // Trilha cinza bem fina, com pontas suavemente arredondadas.
        drawRoundRect(
            color = Color.White.copy(alpha = 0.48f),
            topLeft = Offset(0f, lineTop),
            size = Size(size.width, lineHeight),
            cornerRadius = CornerRadius(lineHeight / 2f)
        )
        if (fraction > 0f) {
            // Halo linear ao longo do trecho reproduzido.
            val glowHeight = 10.dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.5f to gold.copy(alpha = 0.42f),
                    1.0f to Color.Transparent,
                    startY = size.height / 2f - glowHeight / 2f,
                    endY = size.height / 2f + glowHeight / 2f
                ),
                topLeft = Offset(0f, size.height / 2f - glowHeight / 2f),
                size = Size(size.width * fraction, glowHeight),
                cornerRadius = CornerRadius(glowHeight / 2f)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(PlayerChrome.borderColor(0xFF7A5215), PlayerChrome.borderColor(0xFFFFE6A0), PlayerChrome.borderColor(0xFFD6A32E), PlayerChrome.borderColor(0xFFFFF5CD), PlayerChrome.borderColor(0xFF926315))
                ),
                topLeft = Offset(0f, lineTop),
                size = Size(size.width * fraction, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2f)
            )
        }
        // O foco muda apenas o halo; o acabamento metálico permanece.
        if (hasFocus) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to gold.copy(alpha = 0.32f),
                    0.55f to gold.copy(alpha = 0.17f),
                    1.0f to Color.Transparent,
                    center = thumbCenter,
                    radius = 19.dp.toPx()
                ),
                center = thumbCenter,
                radius = 19.dp.toPx()
            )
            drawCircle(
                color = PlayerChrome.color(0xFFFFE9AE).copy(alpha = 0.78f),
                radius = 12.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gold.copy(alpha = 0.48f), Color.Transparent),
                center = thumbCenter,
                radius = 11.dp.toPx()
            ),
            center = thumbCenter,
            radius = 11.dp.toPx()
        )
        val highlight = thumbCenter - Offset(thumbRadius * 0.30f, thumbRadius * 0.35f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to PlayerChrome.color(0xFFFFF5CD).copy(alpha = 0.98f),
                0.42f to PlayerChrome.color(0xFFFFE6A0).copy(alpha = 0.90f),
                0.76f to PlayerChrome.color(0xFFD6A32E).copy(alpha = 0.84f),
                1f to PlayerChrome.color(0xFF926315).copy(alpha = 0.70f),
                center = highlight,
                radius = thumbRadius * 1.65f
            ),
            radius = thumbRadius,
            center = thumbCenter
        )
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    PlayerChrome.color(0xFFFFF4BD).copy(alpha = 0.92f),
                    PlayerChrome.color(0xFFB78A25).copy(alpha = 0.70f),
                    PlayerChrome.color(0xFFFFF4BD).copy(alpha = 0.92f)
                )
            ),
            radius = thumbRadius,
            center = thumbCenter,
            style = Stroke(width = 0.6.dp.toPx())
        )
    }
}

private fun formatPlaybackTime(positionMs: Long): String {
    val seconds = (positionMs.coerceAtLeast(0L) / 1_000L)
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val remaining = seconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remaining)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, remaining)
    }
}

// IPTV_HDR_AUTONEXT_V1

// IPTV_PLAYER_RESUME_GOLD_V1

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_POLISH_LOADING_HOLD_V1

// IPTV_LISTS_SUBTITLES_CHANNELS_V1

// IPTV_SUBTITLE_READABILITY_V1

// IPTV_PORTAL_PRODUCTION_V1

// IPTV_LIST_LOADING_AND_PLAYER_INPUT_V1

// IPTV_AUTOMATIC_DOLBY_AND_EPISODE_CONTROLS_USER26_V2

// IPTV_PLAYER_SEEK_AND_EPISODES_V1

// IPTV_SETTINGS_APPROVED_GRID_V1
