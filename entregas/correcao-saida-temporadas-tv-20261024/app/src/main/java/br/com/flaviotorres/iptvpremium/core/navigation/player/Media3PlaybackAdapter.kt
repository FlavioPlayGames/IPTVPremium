package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Motor principal do IPTVPremium: AndroidX Media3 / ExoPlayer.
 *
 * - Uma instância equivale a um único mecanismo ativo.
 * - Criar, comandar e liberar sempre na thread principal do Android.
 * - SurfaceView é de propriedade da tela; o adaptador recebe o SurfaceHolder.
 * - Não lê nem modifica Room, Xtream, cache de imagens ou configurações persistidas.
 * - Não registra URL, credenciais ou mensagens brutas de exceção.
 *
 * RECURSOS A IMPLEMENTAR EM OUTRAS CAMADAS:
 * - zoom/deslocamento/largura/altura da SurfaceView na UI;
 * - renderização de legendas entregue pela UI em uma SubtitleView;
 * - offset temporal de legendas, se houver integração realmente suportada;
 * - histórico, fallback para LibVLC e interface de controle remoto.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class Media3PlaybackAdapter(context: Context) : PlaybackEngineAdapter {

    override val engine: PlaybackEngine = PlaybackEngine.MEDIA3

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
    private fun supportedDisplayFormats() = displayHdrFormats(displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY))
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var player: ExoPlayer

    private val mutableState = MutableStateFlow(
        PlaybackUiState(engine = PlaybackEngine.MEDIA3)
    )
    override val state: StateFlow<PlaybackUiState> = mutableState.asStateFlow()

    private val mutableCapabilities = MutableStateFlow(PlaybackEngineCapabilities())
    override val capabilities: StateFlow<PlaybackEngineCapabilities> =
        mutableCapabilities.asStateFlow()

    private val mutableEvents = MutableSharedFlow<PlaybackEngineEvent>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events: SharedFlow<PlaybackEngineEvent> = mutableEvents.asSharedFlow()

    private var released = false
    private var opened = false
    private var surfaceHolder: SurfaceHolder? = null
    private var currentRequest: PlaybackOpenRequest? = null
    private var currentLayout = PlaybackVideoLayout()
    private var manualVariantId: String? = null
    private var suppressHdrRefresh = false
    private var tickerRunning = false

    private val positionTicker = object : Runnable {
        override fun run() {
            if (released || !opened) {
                tickerRunning = false
                return
            }
            refreshPlaybackState()
            mainHandler.postDelayed(this, POSITION_UPDATE_MS)
        }
    }

    private val surfaceListener = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            // O Media3 já observa o SurfaceHolder ligado via setVideoSurfaceHolder.
            // Não criar um segundo player nem uma segunda Surface aqui.
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) = Unit

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            if (!released && holder === surfaceHolder) {
                mutableEvents.tryEmit(PlaybackEngineEvent.VideoSurfaceLost)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (released) return
            refreshPlaybackState()
            refreshCapabilities()
        }

        override fun onCues(cueGroup: CueGroup) {
            if (!released) {
                val cues = cueGroup.cues
                if (mutableState.value.subtitleCues != cues) {
                    mutableState.value = mutableState.value.copy(subtitleCues = cues)
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (released) return
            refreshTrackInformation(tracks)
            if (!suppressHdrRefresh && manualVariantId == null) {
                applyAutomaticHdrSelection()
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (!released) refreshTechnicalInformation()
        }

        override fun onRenderedFirstFrame() {
            if (!released) mutableEvents.tryEmit(PlaybackEngineEvent.FirstVideoFrame)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (released) return
            if (playbackState == Player.STATE_ENDED) {
                mutableEvents.tryEmit(PlaybackEngineEvent.PlaybackEnded)
            }
            refreshPlaybackState()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (released) return
            refreshPlaybackState()
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                mutableEvents.tryEmit(
                    PlaybackEngineEvent.SeekCompleted(player.currentPosition.coerceAtLeast(0L))
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (released) return
            val failure = classifyFailure(error)
            mutableState.value = mutableState.value.copy(
                status = PlaybackStatus.ERROR,
                failure = failure
            )
            mutableEvents.tryEmit(PlaybackEngineEvent.Failure(failure))
        }
    }

    init {
        check(isMainThread()) {
            "Media3PlaybackAdapter precisa ser criado na thread principal."
        }
        val http = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val sources = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
            androidx.media3.datasource.DefaultDataSource.Factory(context.applicationContext, http))
            .setLoadErrorHandlingPolicy(object : androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy() {
                override fun getRetryDelayMsFor(info: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                    val denied = generateSequence(info.exception as Throwable?) { it.cause }
                        .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
                        .any { it.responseCode == 401 || it.responseCode == 403 }
                    return if (denied) C.TIME_UNSET else super.getRetryDelayMsFor(info)
                }
            })
        player = ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(sources)
            .setLoadControl(androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setTargetBufferBytes(br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.playerBufferBytes)
                .setPrioritizeTimeOverSizeThresholds(false)
                .build())
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply { addListener(playerListener) }
    }

    override fun attachVideoSurface(holder: SurfaceHolder): PlaybackCommandResult {
        if (!isUsable()) return PlaybackCommandResult.INVALID_STATE
        if (holder === surfaceHolder) return PlaybackCommandResult.ACCEPTED
        detachInternalSurface()
        surfaceHolder = holder
        holder.addCallback(surfaceListener)
        player.setVideoSurfaceHolder(holder)
        return PlaybackCommandResult.ACCEPTED
    }

    override fun detachVideoSurface(holder: SurfaceHolder?) {
        if (!isMainThread() || released) return
        if (holder != null && holder !== surfaceHolder) return
        detachInternalSurface()
    }

    private fun detachInternalSurface() {
        val oldHolder = surfaceHolder ?: return
        oldHolder.removeCallback(surfaceListener)
        player.clearVideoSurface()
        surfaceHolder = null
    }

    override fun prepare(request: PlaybackOpenRequest): PlaybackCommandResult {
        if (!isUsable()) return PlaybackCommandResult.INVALID_STATE

        val url = request.media.streamUrl.trim()
        if (url.isEmpty()) return PlaybackCommandResult.INVALID_ARGUMENT

        return try {
            currentRequest = request
            opened = true
            manualVariantId = null
            currentLayout = PlaybackVideoLayout()
            mutableCapabilities.value = PlaybackEngineCapabilities()
            mutableState.value = PlaybackUiState(
                media = request.media,
                engine = engine,
                status = PlaybackStatus.PREPARING,
                positionMs = if (request.media.type == PlaybackContentType.LIVE_CHANNEL) {
                    0L
                } else {
                    request.startPositionMs
                },
                isLive = request.media.type == PlaybackContentType.LIVE_CHANNEL
            )

            val itemBuilder = MediaItem.Builder().setUri(url)
            val mime = request.media.mimeType?.trim()?.takeIf { it.isNotEmpty() }
                ?: inferManifestMimeType(url)
            if (mime != null) itemBuilder.setMimeType(mime)
            val item = itemBuilder.build()

            player.stop()
            player.clearMediaItems()
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .clearOverrides()
                .apply {
                    setPreferredAudioLanguage(
                        request.settings.audioLanguageTag?.takeIf { it.isNotBlank() }
                    )
                    setPreferredTextLanguage(
                        request.settings.subtitleLanguageTag?.takeIf { it.isNotBlank() }
                    )
                    setTrackTypeDisabled(
                        C.TRACK_TYPE_TEXT,
                        !request.settings.subtitlesEnabled
                    )
                }.build()
            player.setPlaybackSpeed(request.settings.speed)
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            if (request.media.type == PlaybackContentType.LIVE_CHANNEL) {
                // No início da transmissão, usar a posição ao vivo definida pela fonte.
                player.setMediaItem(item)
            } else {
                player.setMediaItem(item, request.startPositionMs)
            }
            player.playWhenReady = request.playWhenReady
            player.prepare()
            startPositionTicker()
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            opened = false
            // Não deixar reprodução iniciada parcialmente após uma exceção.
            runCatching { player.stop() }
            runCatching { player.clearMediaItems() }
            mainHandler.removeCallbacks(positionTicker)
            tickerRunning = false
            val failure = PlaybackFailure(
                type = PlaybackFailureType.UNKNOWN,
                message = "Não foi possível preparar a mídia neste reprodutor.",
                isRecoverable = true,
                errorCode = "MEDIA3_PREPARE"
            )
            mutableState.value = mutableState.value.copy(
                status = PlaybackStatus.ERROR,
                failure = failure
            )
            mutableEvents.tryEmit(PlaybackEngineEvent.Failure(failure))
            PlaybackCommandResult.INVALID_STATE
        }
    }

    override fun play(): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        player.play()
        refreshPlaybackState()
        return PlaybackCommandResult.ACCEPTED
    }

    override fun pause(): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        if (!player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        player.pause()
        refreshPlaybackState()
        return PlaybackCommandResult.ACCEPTED
    }

    override fun stop(clearMedia: Boolean): PlaybackCommandResult {
        if (!isUsable()) return PlaybackCommandResult.INVALID_STATE
        mainHandler.removeCallbacks(positionTicker)
        tickerRunning = false
        player.stop()
        opened = false
        if (clearMedia) {
            player.clearMediaItems()
            currentRequest = null
            manualVariantId = null
            mutableState.value = PlaybackUiState(engine = engine)
        } else {
            mutableState.value = mutableState.value.copy(
                status = PlaybackStatus.IDLE,
                subtitleCues = emptyList(),
                failure = null
            )
        }
        mutableCapabilities.value = PlaybackEngineCapabilities()
        return PlaybackCommandResult.ACCEPTED
    }

    override fun seekTo(positionMs: Long): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        if (positionMs < 0L) return PlaybackCommandResult.INVALID_ARGUMENT
        if (!player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        if (!player.isCurrentMediaItemSeekable) return PlaybackCommandResult.UNSUPPORTED
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
        val target = duration?.let { positionMs.coerceAtMost(it) } ?: positionMs
        player.seekTo(target)
        refreshPlaybackState()
        return PlaybackCommandResult.ACCEPTED
    }

    override fun seekBy(deltaMs: Long): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        val now = player.currentPosition.coerceAtLeast(0L)
        // Evita overflow com valores extremos de deltaMs.
        val target = when {
            deltaMs >= 0L && now > Long.MAX_VALUE - deltaMs -> Long.MAX_VALUE
            deltaMs == Long.MIN_VALUE -> 0L
            else -> (now + deltaMs).coerceAtLeast(0L)
        }
        return seekTo(target)
    }

    override fun goToLiveEdge(): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        if (!mutableState.value.liveWindow.canSeekBack) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        if (!player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        player.seekToDefaultPosition()
        refreshPlaybackState()
        return PlaybackCommandResult.ACCEPTED
    }

    override fun selectTrack(
        type: PlaybackTrackType,
        trackId: String?
    ): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        val trackType = when (type) {
            PlaybackTrackType.AUDIO -> C.TRACK_TYPE_AUDIO
            PlaybackTrackType.SUBTITLE -> C.TRACK_TYPE_TEXT
            PlaybackTrackType.VIDEO -> return PlaybackCommandResult.UNSUPPORTED
        }
        if (trackId == null) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(trackType)
                .setTrackTypeDisabled(trackType, type == PlaybackTrackType.SUBTITLE)
                .build()
            if (type == PlaybackTrackType.SUBTITLE) {
                mutableState.value = mutableState.value.copy(subtitleCues = emptyList())
            }
            return PlaybackCommandResult.ACCEPTED
        }
        val found = findTrack(trackId, trackType)
            ?: return PlaybackCommandResult.NOT_AVAILABLE
        if (!found.group.isTrackSupported(found.trackIndex)) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        if (type == PlaybackTrackType.SUBTITLE) {
            mutableState.value = mutableState.value.copy(subtitleCues = emptyList())
        }
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(
                TrackSelectionOverride(found.group.mediaTrackGroup, listOf(found.trackIndex))
            ).build()
        return PlaybackCommandResult.ACCEPTED
    }

    override fun selectVideoVariant(variantId: String?): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        if (variantId == null) {
            manualVariantId = null
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .build()
            applyAutomaticHdrSelection()
            return PlaybackCommandResult.ACCEPTED
        }
        val found = findTrack(variantId, C.TRACK_TYPE_VIDEO)
            ?: return PlaybackCommandResult.NOT_AVAILABLE
        if (!found.group.isTrackSupported(found.trackIndex)) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        manualVariantId = variantId
        selectVideoIndices(found.group, listOf(found.trackIndex))
        return PlaybackCommandResult.ACCEPTED
    }

    override fun setPlaybackSpeed(speed: Float): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        if (speed !in setOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) {
            return PlaybackCommandResult.INVALID_ARGUMENT
        }
        if (!player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        player.setPlaybackSpeed(speed)
        return PlaybackCommandResult.ACCEPTED
    }

    override fun setVolume(volume: Float): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        if (volume.isNaN() || volume !in 0f..1f) {
            return PlaybackCommandResult.INVALID_ARGUMENT
        }
        if (!player.isCommandAvailable(Player.COMMAND_SET_VOLUME)) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        player.volume = volume
        return PlaybackCommandResult.ACCEPTED
    }

    override fun setVideoLayout(layout: PlaybackVideoLayout): PlaybackCommandResult {
        if (!isReadyForCommands()) return PlaybackCommandResult.INVALID_STATE
        // Não é possível modificar a View/Surface com zoom e posição usando
        // somente SurfaceHolder. O ajuste MANUAL será feito na tela dedicada.
        if (layout.mode == PlaybackAspectMode.MANUAL) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        val scalingMode = when (layout.mode) {
            PlaybackAspectMode.NORMAL -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            PlaybackAspectMode.FULL_SCREEN ->
                C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            PlaybackAspectMode.MANUAL -> return PlaybackCommandResult.UNSUPPORTED
        }
        player.setVideoScalingMode(scalingMode)
        currentLayout = layout
        return PlaybackCommandResult.ACCEPTED
    }

    override fun setSubtitleStyle(style: PlaybackSubtitleStyle): PlaybackCommandResult {
        // A SubtitleView é da UI, mas o estilo avançado ainda não é aplicado
        // por este adaptador. Não declarar suporte sem implementação.
        return if (!isReadyForCommands()) {
            PlaybackCommandResult.INVALID_STATE
        } else {
            PlaybackCommandResult.UNSUPPORTED
        }
    }

    override fun release() {
        if (released) return
        check(isMainThread()) { "release() deve ser chamado na thread principal." }
        released = true
        opened = false
        mainHandler.removeCallbacks(positionTicker)
        tickerRunning = false
        detachInternalSurface()
        player.removeListener(playerListener)
        player.release()
        currentRequest = null
        mutableCapabilities.value = PlaybackEngineCapabilities()
        mutableState.value = PlaybackUiState(engine = engine)
    }

    private fun refreshPlaybackState() {
        if (released || !opened) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
        val isLive = currentRequest?.media?.type == PlaybackContentType.LIVE_CHANNEL ||
                player.isCurrentMediaItemLive
        val seekable = player.isCurrentMediaItemSeekable &&
                player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        val liveWindow = if (isLive && seekable && duration != null) {
            PlaybackLiveWindow(
                supportsTimeshift = true,
                seekableStartMs = 0L,
                liveEdgeMs = duration,
                isAtLiveEdge = (duration - player.currentPosition) <= LIVE_EDGE_TOLERANCE_MS
            )
        } else {
            PlaybackLiveWindow()
        }
        val status = when {
            player.playerError != null -> PlaybackStatus.ERROR
            player.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            player.playbackState == Player.STATE_ENDED -> PlaybackStatus.ENDED
            player.playbackState == Player.STATE_READY && player.isPlaying -> PlaybackStatus.PLAYING
            player.playbackState == Player.STATE_READY && !player.playWhenReady -> PlaybackStatus.PAUSED
            player.playbackState == Player.STATE_READY -> PlaybackStatus.READY
            player.playbackState == Player.STATE_IDLE -> PlaybackStatus.IDLE
            else -> PlaybackStatus.PREPARING
        }
        mutableState.value = mutableState.value.copy(
            status = status,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            bufferedPositionMs = player.bufferedPosition.takeIf { it >= 0L },
            isLive = isLive,
            liveWindow = liveWindow,
            failure = if (status == PlaybackStatus.ERROR) {
                mutableState.value.failure
            } else {
                null
            }
        )
        refreshTechnicalInformation()
    }

    private fun refreshTrackInformation(tracks: Tracks) {
        val audio = ArrayList<PlaybackTrack>()
        val subtitles = ArrayList<PlaybackTrack>()
        val videos = ArrayList<PlaybackVideoVariant>()
        var audioSelected: String? = null
        var subtitleSelected: String? = null
        tracks.groups.forEachIndexed { groupIndex, group ->
            for (index in 0 until group.length) {
                if (!group.isTrackSupported(index)) continue
                val format = group.getTrackFormat(index)
                val id = trackId(group.type, groupIndex, index)
                val selected = group.isTrackSelected(index)
                when (group.type) {
                    C.TRACK_TYPE_AUDIO -> {
                        audio.add(
                            PlaybackTrack(
                                id = id,
                                type = PlaybackTrackType.AUDIO,
                                label = format.label?.takeIf { it.isNotBlank() }
                                    ?: format.language?.takeIf { it.isNotBlank() }
                                    ?: "Áudio ${audio.size + 1}",
                                languageTag = format.language,
                                codec = format.sampleMimeType,
                                isDefault = selected
                            )
                        )
                        if (selected && audioSelected == null) audioSelected = id
                    }
                    C.TRACK_TYPE_TEXT -> {
                        subtitles.add(
                            PlaybackTrack(
                                id = id,
                                type = PlaybackTrackType.SUBTITLE,
                                label = format.label?.takeIf { it.isNotBlank() }
                                    ?: format.language?.takeIf { it.isNotBlank() }
                                    ?: "Legenda ${subtitles.size + 1}",
                                languageTag = format.language,
                                codec = format.sampleMimeType,
                                isDefault = selected
                            )
                        )
                        if (selected && subtitleSelected == null) subtitleSelected = id
                    }
                    C.TRACK_TYPE_VIDEO -> {
                        val height = format.height.takeIf { it > 0 }
                        val width = format.width.takeIf { it > 0 }
                        val description = if (height != null) "${height}p" else "Vídeo"
                        val hdr = detectHdr(format)
                        videos.add(
                            PlaybackVideoVariant(
                                id = id,
                                label = if (hdr == PlaybackHdrFormat.UNKNOWN || hdr == PlaybackHdrFormat.SDR) {
                                    description
                                } else {
                                    "$description • ${hdr.displayName}"
                                },
                                width = width,
                                height = height,
                                bitrateBitsPerSecond =
                                    format.bitrate.takeIf { it > 0 }?.toLong(),
                                hdrFormat = hdr
                            )
                        )
                    }
                }
            }
        }
        mutableState.value = mutableState.value.copy(
            availableTracks = audio + subtitles,
            selectedAudioTrackId = audioSelected,
            selectedSubtitleTrackId = subtitleSelected,
            availableVideoVariants = videos
        )
        refreshTechnicalInformation()
        refreshCapabilities()
    }

    private fun refreshTechnicalInformation() {
        if (released || !opened) return
        val video = player.videoFormat
        val audio = player.audioFormat
        val current = mutableState.value
        val technical = PlaybackTechnicalInfo(
            width = player.videoSize.width.takeIf { it > 0 }
                ?: video?.width?.takeIf { it > 0 },
            height = player.videoSize.height.takeIf { it > 0 }
                ?: video?.height?.takeIf { it > 0 },
            framesPerSecond = video?.frameRate?.takeIf { it > 0f },
            videoCodec = video?.codecs ?: video?.sampleMimeType,
            audioCodec = audio?.codecs ?: audio?.sampleMimeType,
            hdrFormat = video?.let(::detectHdr) ?: PlaybackHdrFormat.UNKNOWN,
            bitrateBitsPerSecond = video?.bitrate?.takeIf { it > 0 }?.toLong(),
            bufferedDurationMs = player.totalBufferedDuration.coerceAtLeast(0L),
            // A existência de um codec com suporte não prova que o decoder
            // efetivamente usado é por hardware; Analytics detalhará depois.
            usingHardwareDecoder = null
        )
        if (current.technicalInfo != technical) {
            mutableState.value = current.copy(technicalInfo = technical)
        }
    }

    private fun refreshCapabilities() {
        if (released || !opened) return
        val current = mutableState.value
        mutableCapabilities.value = PlaybackEngineCapabilities(
            supportsSeeking = player.isCurrentMediaItemSeekable,
            supportsLiveTimeshift = current.liveWindow.canSeekBack,
            supportsGoLive = current.liveWindow.canSeekBack,
            supportsAudioTrackSelection = current.availableTracks.any {
                it.type == PlaybackTrackType.AUDIO
            },
            supportsSubtitleTrackSelection = current.availableTracks.any {
                it.type == PlaybackTrackType.SUBTITLE
            },
            supportsSubtitleStyling = false,
            supportsSubtitleTimingOffset = false,
            supportsManualQualitySelection = current.availableVideoVariants.size > 1,
            supportsPlaybackSpeed = player.isCommandAvailable(
                Player.COMMAND_SET_SPEED_AND_PITCH
            ),
            supportsVolumeControl = player.isCommandAvailable(Player.COMMAND_SET_VOLUME),
            supportsVideoAspectModes = true,
            supportsManualZoomAndPan = false,
            supportsIndependentWidthHeight = false,
            supportsTechnicalDiagnostics = true
        )
    }

    /** Referências de faixas valem somente para a sessão corrente. */
    private data class SelectedTrack(
        val group: Tracks.Group,
        val trackIndex: Int
    )

    private fun findTrack(id: String, wantedType: Int): SelectedTrack? {
        val prefix = when (wantedType) {
            C.TRACK_TYPE_AUDIO -> "audio"
            C.TRACK_TYPE_TEXT -> "subtitle"
            C.TRACK_TYPE_VIDEO -> "video"
            else -> return null
        }
        val parts = id.split(':')
        if (parts.size != 3 || parts[0] != prefix) return null
        val groupIndex = parts[1].toIntOrNull() ?: return null
        val trackIndex = parts[2].toIntOrNull() ?: return null
        val groups = player.currentTracks.groups
        if (groupIndex !in groups.indices) return null
        val group = groups[groupIndex]
        if (group.type != wantedType || trackIndex !in 0 until group.length) return null
        return SelectedTrack(group, trackIndex)
    }

    private fun trackId(type: Int, groupIndex: Int, index: Int): String =
        "${when (type) {
            C.TRACK_TYPE_AUDIO -> "audio"
            C.TRACK_TYPE_TEXT -> "subtitle"
            C.TRACK_TYPE_VIDEO -> "video"
            else -> "unknown"
        }}:$groupIndex:$index"

    private fun selectVideoIndices(group: Tracks.Group, indices: List<Int>) {
        suppressHdrRefresh = true
        try {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, indices)
                ).build()
        } finally {
            suppressHdrRefresh = false
        }
    }

    /**
     * Seleciona apenas formatos que o Media3 reporta como suportados.
     * Se uma mesma TrackGroup for adaptativa, mantém várias representações
     * do mesmo HDR para preservar a adaptação automática de bitrate.
     */
    private fun applyAutomaticHdrSelection(): PlaybackCommandResult {
        val candidates = player.currentTracks.groups.flatMap { group ->
            if (group.type != C.TRACK_TYPE_VIDEO) emptyList() else {
                (0 until group.length).filter { group.isTrackSupported(it) }
                    .map { index -> SelectedTrack(group, index) }
            }
        }
        if (candidates.isEmpty()) return PlaybackCommandResult.NOT_AVAILABLE
        val knownFormats = candidates.map { detectHdr(it.group.getTrackFormat(it.trackIndex)) }
            .filter { it != PlaybackHdrFormat.UNKNOWN && it in supportedDisplayFormats() }
            .distinct()
        if (knownFormats.isEmpty()) {
            // Fonte sem sinalização HDR confiável: deixar seleção automática.
            return PlaybackCommandResult.NOT_AVAILABLE
        }
        val chosenFormat = selectAutomaticHdr(knownFormats.toSet(), supportedDisplayFormats())
            ?: return PlaybackCommandResult.NOT_AVAILABLE
        val matching = candidates.filter {
            detectHdr(it.group.getTrackFormat(it.trackIndex)) == chosenFormat
        }
        if (matching.isEmpty()) return PlaybackCommandResult.NOT_AVAILABLE

        val selectedCurrently = matching.any {
            it.group.isTrackSelected(it.trackIndex)
        }
        val hasDifferentSelected = candidates.any {
            it.group.isTrackSelected(it.trackIndex) &&
                    detectHdr(it.group.getTrackFormat(it.trackIndex)) != chosenFormat
        }
        if (selectedCurrently && !hasDifferentSelected) return PlaybackCommandResult.ACCEPTED

        // Prefere um grupo com mais representações compatíveis com o HDR.
        val grouped = matching.groupBy { it.group }
        val bestGroup = grouped.maxByOrNull { (_, list) -> list.size }?.key
            ?: return PlaybackCommandResult.NOT_AVAILABLE
        val matchingIndices = grouped.getValue(bestGroup).map { it.trackIndex }
        val indices = if (bestGroup.isAdaptiveSupported) {
            matchingIndices
        } else {
            listOf(matchingIndices.maxByOrNull { index ->
                bestGroup.getTrackFormat(index).height
            } ?: matchingIndices.first())
        }
        selectVideoIndices(bestGroup, indices)
        return PlaybackCommandResult.ACCEPTED
    }

    private fun detectHdr(format: Format): PlaybackHdrFormat {
        val mime = format.sampleMimeType?.lowercase(Locale.ROOT)
        val codecs = format.codecs?.lowercase(Locale.ROOT).orEmpty()
        if (isDolbyVisionSource(mime, codecs)) return PlaybackHdrFormat.DOLBY_VISION

        // COLOR_TRANSFER_ST2084 confirma HDR PQ, mas não distingue
        // HDR10 de HDR10+ sem metadados dinâmicos adicionais.
        return when (format.colorInfo?.colorTransfer) {
            C.COLOR_TRANSFER_ST2084 -> PlaybackHdrFormat.HDR10
            C.COLOR_TRANSFER_HLG -> PlaybackHdrFormat.HLG
            C.COLOR_TRANSFER_SDR -> PlaybackHdrFormat.SDR
            else -> PlaybackHdrFormat.UNKNOWN
        }
    }

    private fun inferManifestMimeType(url: String): String? {
        // Apenas o caminho; nunca interpretar token ou senha na query string.
        val path = android.net.Uri.parse(url).path?.lowercase(Locale.ROOT) ?: return null
        return when {
            path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
            path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
            else -> null
        }
    }

    private fun classifyFailure(error: PlaybackException): PlaybackFailure {
        val code = error.errorCode
        val httpError = generateSequence(error as Throwable?) { it.cause }
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()
        val responseCode = httpError?.responseCode
        val type = when {
            responseCode == 401 || responseCode == 403 -> PlaybackFailureType.AUTHENTICATION
            responseCode == 404 || responseCode == 410 -> PlaybackFailureType.SOURCE_UNAVAILABLE
            code in 6000..6999 -> PlaybackFailureType.DRM
            code == PlaybackException.ERROR_CODE_TIMEOUT ||
                    code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                PlaybackFailureType.TIMEOUT
            code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    code == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ->
                PlaybackFailureType.NETWORK
            code == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    code == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
                    code == PlaybackException.ERROR_CODE_DECODING_FAILED ->
                PlaybackFailureType.DECODER
            code == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                    code == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ||
                    code == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                    code == PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
                PlaybackFailureType.UNSUPPORTED_FORMAT
            code == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                PlaybackFailureType.SOURCE_UNAVAILABLE
            else -> PlaybackFailureType.UNKNOWN
        }
        val message = when (type) {
            PlaybackFailureType.AUTHENTICATION -> "A transmissão não autorizou o acesso."
            PlaybackFailureType.SOURCE_UNAVAILABLE -> "Conteúdo indisponível na origem."
            PlaybackFailureType.DRM -> "Esta proteção de conteúdo não é compatível."
            PlaybackFailureType.TIMEOUT -> "A conexão demorou além do esperado."
            PlaybackFailureType.NETWORK -> "Falha de rede durante a transmissão."
            PlaybackFailureType.DECODER -> "Falha ao decodificar o vídeo neste aparelho."
            PlaybackFailureType.UNSUPPORTED_FORMAT -> "Formato não suportado neste reprodutor."
            PlaybackFailureType.UNKNOWN -> "Falha ao reproduzir o conteúdo."
        }
        return PlaybackFailure(
            type = type,
            message = message,
            isRecoverable = type !in setOf(
                PlaybackFailureType.DRM
            ),
            errorCode = when {
                responseCode in 300..399 -> "MEDIA3_HTTP_REDIRECT"
                responseCode != null -> "MEDIA3_HTTP_$responseCode"
                else -> "MEDIA3_$code"
            }
        )
    }

    private fun startPositionTicker() {
        if (tickerRunning) return
        tickerRunning = true
        mainHandler.post(positionTicker)
    }

    private fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()
    private fun isUsable(): Boolean = isMainThread() && !released
    private fun isReadyForCommands(): Boolean = isUsable() && opened

    private companion object {
        const val POSITION_UPDATE_MS = 500L
        const val LIVE_EDGE_TOLERANCE_MS = 3_000L

    }
}

// IPTV_HDR_AUTONEXT_V1

// IPTV_AUTOMATIC_DOLBY_AND_EPISODE_CONTROLS_USER26_V2
