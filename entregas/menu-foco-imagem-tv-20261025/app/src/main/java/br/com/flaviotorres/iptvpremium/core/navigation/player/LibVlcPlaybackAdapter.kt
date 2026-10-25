package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.SurfaceHolder
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout

/**
 * Segundo mecanismo interno do IPTVPremium: LibVLC.
 *
 * Compartilha o contrato do Media3, mas NÃO promete Dolby Vision, HDR10+,
 * escolha de resolução ou Timeshift quando o VLC não informar suporte.
 *
 * O Android deve criar e comandar esta classe na thread principal.
 * Somente um mecanismo deve permanecer ativo por vez.
 * O adaptador não persiste dados, não altera o Room e não registra URLs.
 *
 * Algumas APIs de faixas mudaram entre LibVLC 3.x: os IDs podem ser Int ou
 * String. A pequena camada de compatibilidade no fim do arquivo evita que
 * essa diferença quebre a compilação. Recursos ausentes retornam UNSUPPORTED.
 */
class LibVlcPlaybackAdapter(context: Context) : PlaybackEngineAdapter {

    private companion object {
        const val AUDIO_TAG = "IPTVPremiumVlcAudio"
        const val VIDEO_TAG = "IPTVPremiumVlcVideo"
    }

    override val engine: PlaybackEngine = PlaybackEngine.VLC

    private val handler = Handler(Looper.getMainLooper())
    private val libVlc: LibVLC
    private lateinit var vlc: MediaPlayer
    private var released = false
    private var opened = false
    private val surfaceStart = VlcSurfaceStartGate()
    private var nativeSurfacesReady = false
    private var nativeHolder: SurfaceHolder? = null
    private var nativeSurface: android.view.Surface? = null
    private var wantsToPlay = true
    private var requestedPositionMs = 0L
    private var restorePositionPending = false
    private var surfaceHolder: SurfaceHolder? = null
    private var currentRequest: PlaybackOpenRequest? = null
    private var subtitleStyle = PlaybackSubtitleStyle()
    private var lastTracksSignature = ""
    private var lastTechnicalProbeAtMs = 0L
    private var startupAtMs = 0L
    private var lastStartupProbeMs = 0L
    private var startupReachedPlaying = false
    private var bufferingBucket = -1

    // Perfil conservador: AudioTrack / PCM estereo, sem passthrough automatico.
    // Alterar apenas no VLC: o Media3 continua com HDR e audio nativos.
    private val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusGranted = false
    private var preferredVolume = 1f
    private var audioOutputConfigured = false
    private var audioStereoConfigured = false
    private var audioSessionId = 0L
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (released) return@OnAudioFocusChangeListener
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusGranted = true
                Log.i(AUDIO_TAG, "FOCO=GANHO")
                if (opened && wantsToPlay) { surfaceStart.requestPlay(); startWhenSurfaceReady() }
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocusGranted = false
                Log.w(AUDIO_TAG, "FOCO=PERDIDO codigo=$change")
                if (opened) runCatching { vlc.pause() }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.i(AUDIO_TAG, "FOCO=DUCK")
            }
        }
    }

    private val mutableState = MutableStateFlow(
        PlaybackUiState(engine = PlaybackEngine.VLC)
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
    override val events: SharedFlow<PlaybackEngineEvent> =
        mutableEvents.asSharedFlow()

    private val positionTicker = object : Runnable {
        override fun run() {
            if (released || !opened) return
            val now=SystemClock.elapsedRealtime()
            if(!startupReachedPlaying && now-lastStartupProbeMs>=2_400L) {
                lastStartupProbeMs=now
                traceNativeStartup("waiting")
            }
            refreshProgress()
            refreshTracksIfChanged()
            refreshTechnicalInfo()
            handler.postDelayed(this, 800L)
        }
    }

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            if (!released && holder === surfaceHolder) attachNativeSurface(holder)
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            if (!released && holder === surfaceHolder) {
                updateWindowSize(width, height)
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            if (!released && holder === surfaceHolder) {
                surfaceStart.surfaceLost(opened && wantsToPlay)
                detachNativeSurface()
                mutableEvents.tryEmit(PlaybackEngineEvent.VideoSurfaceLost)
            }
        }
    }

    private val videoOutputCallback = object : IVLCVout.Callback {
        override fun onSurfacesCreated(output: IVLCVout) {
            if (released || nativeHolder !== surfaceHolder || nativeSurface?.isValid != true) return
            nativeSurfacesReady = true
            surfaceStart.surfaceReady()
            Log.i("VlcSurfacePerf", "READY")
            startWhenSurfaceReady()
        }
        override fun onSurfacesDestroyed(output: IVLCVout) {
            nativeSurfacesReady = false
            surfaceStart.surfaceLost(opened && wantsToPlay)
        }
    }

    private fun startWhenSurfaceReady() {
        if (released || !opened || !surfaceStart.takeStart()) return
        runCatching { vlc.play() }.onFailure {
            reportLocalFailure("Não foi possível iniciar o vídeo no VLC.")
        }
    }

    init {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "LibVlcPlaybackAdapter deve ser criado na thread principal."
        }
        libVlc = LibVLC(context.applicationContext, arrayListOf<String>())
        vlc = MediaPlayer(libVlc)
        vlc.vlcVout.addCallback(videoOutputCallback)
        configureAudioOutput()
        vlc.setEventListener(MediaPlayer.EventListener { event ->
            if (Looper.myLooper() == Looper.getMainLooper()) {
                handleEvent(event)
            } else {
                handler.post { handleEvent(event) }
            }
        })
    }

    override fun attachVideoSurface(holder: SurfaceHolder): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        if (surfaceHolder === holder) return PlaybackCommandResult.ACCEPTED
        detachVideoSurface()
        surfaceHolder = holder
        holder.addCallback(surfaceCallback)
        if (holder.surface.isValid) attachNativeSurface(holder)
        return PlaybackCommandResult.ACCEPTED
    }

    override fun detachVideoSurface(holder: SurfaceHolder?) {
        if (!onMainThread() || released) return
        if (holder != null && holder !== surfaceHolder) return
        surfaceHolder?.removeCallback(surfaceCallback)
        surfaceHolder = null
        detachNativeSurface()
    }

    private fun attachNativeSurface(holder: SurfaceHolder) {
        if (released || !holder.surface.isValid) return
        runCatching {
            val output = vlc.vlcVout
            if (nativeHolder === holder && nativeSurface === holder.surface && output.areViewsAttached()) {
                updateWindowSize()
                return
            }
            if (output.areViewsAttached()) output.detachViews()
            surfaceStart.surfaceLost(opened && wantsToPlay)
            nativeSurfacesReady = false
            nativeHolder = holder
            nativeSurface = holder.surface
            output.setVideoSurface(holder.surface, holder)
            output.attachViews()
            Log.i("VlcSurfacePerf", "ATTACHED")
            updateWindowSize(
                width = holder.surfaceFrame.width(),
                height = holder.surfaceFrame.height()
            )
            applyBestFit()
        }.onFailure { reportLocalFailure("Não foi possível conectar a superfície de vídeo.") }
    }

    private fun detachNativeSurface() {
        if (released) return
        // Temporary surface loss must not leave a decoder running without an output.
        if (opened && surfaceStart.hasStarted && nativeHolder != null) runCatching { vlc.pause() }
        surfaceStart.surfaceLost(opened && wantsToPlay)
        nativeSurfacesReady = false
        nativeHolder = null
        nativeSurface = null
        val started = SystemClock.elapsedRealtime()
        try {
            runCatching {
                val output = vlc.vlcVout
                if (output.areViewsAttached()) output.detachViews()
            }
        } finally {
            Log.i("PlayerReleasePerf", "detachSurfaceMs=${SystemClock.elapsedRealtime() - started}")
        }
    }

    private fun updateWindowSize(width: Int? = null, height: Int? = null) {
        if (released) return
        val resolvedWidth = width?.takeIf { it > 0 }
            ?: surfaceHolder?.surfaceFrame?.width()?.takeIf { it > 0 }
            ?: return
        val resolvedHeight = height?.takeIf { it > 0 }
            ?: surfaceHolder?.surfaceFrame?.height()?.takeIf { it > 0 }
            ?: return

        runCatching {
            vlc.vlcVout.setWindowSize(resolvedWidth, resolvedHeight)
        }.onFailure {
            Log.w(
                VIDEO_TAG,
                "WINDOW_SIZE_FALHOU largura=$resolvedWidth altura=$resolvedHeight",
                it
            )
        }
    }

    private fun applyBestFit() {
        if (released) return
        runCatching {
            vlc.scale = 0f
            vlc.aspectRatio = null
            updateWindowSize()
        }.onFailure {
            Log.w(VIDEO_TAG, "BEST_FIT_FALHOU", it)
        }
    }

    override fun prepare(request: PlaybackOpenRequest): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        if (request.media.streamUrl.isBlank()) return PlaybackCommandResult.INVALID_ARGUMENT

        return try {
            handler.removeCallbacks(positionTicker)
            if (opened) vlc.stop()
            audioSessionId++
            configureAudioOutput()
            requestAudioFocus()
            applyBestFit()
            currentRequest = request
            opened = true
            startupAtMs=SystemClock.elapsedRealtime()
            lastStartupProbeMs=0L
            startupReachedPlaying=false
            bufferingBucket=-1
            wantsToPlay = request.playWhenReady
            surfaceStart.prepare()
            if (nativeSurfacesReady) surfaceStart.surfaceReady()
            requestedPositionMs = request.startPositionMs
            restorePositionPending = requestedPositionMs > 0L &&
                    request.media.type != PlaybackContentType.LIVE_CHANNEL
            lastTracksSignature = ""
            lastTechnicalProbeAtMs = 0L
            subtitleStyle = request.settings.subtitleStyle

            mutableState.value = PlaybackUiState(
                media = request.media,
                engine = engine,
                status = PlaybackStatus.PREPARING,
                positionMs = if (restorePositionPending) requestedPositionMs else 0L,
                isLive = request.media.type == PlaybackContentType.LIVE_CHANNEL
            )
            mutableCapabilities.value = PlaybackEngineCapabilities()

            // O MediaPlayer mantém uma referência própria à Media após setMedia.
            val media = Media(libVlc, Uri.parse(request.media.streamUrl))
            try {
                // The app draws its own title; do not ask the native subtitle renderer for another one.
                media.addOption(":no-video-title-show")
                vlc.media = media
            } finally {
                media.release()
            }

            traceNativeStartup("prepared")

            // LibVLC inicia a preparação chamando play(). Se a sessão deve
            // começar pausada, pausamos assim que recebermos o evento Playing.
            startWhenSurfaceReady()
            applyAudioVolume()
            handler.post(positionTicker)
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            opened = false
            abandonAudioFocus()
            reportLocalFailure("Não foi possível preparar o vídeo no VLC.")
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    override fun play(): PlaybackCommandResult {
        if (!usable() || !opened) return PlaybackCommandResult.INVALID_STATE
        return try {
            wantsToPlay = true
            requestAudioFocus()
            surfaceStart.requestPlay()
            startWhenSurfaceReady()
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    override fun pause(): PlaybackCommandResult {
        if (!usable() || !opened) return PlaybackCommandResult.INVALID_STATE
        return try {
            wantsToPlay = false
            surfaceStart.pause()
            if (surfaceStart.hasStarted) vlc.pause()
            abandonAudioFocus()
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    override fun stop(clearMedia: Boolean): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        return try {
            opened = false
            surfaceStart.close()
            audioSessionId++
            restorePositionPending = false
            handler.removeCallbacks(positionTicker)
            abandonAudioFocus()
            vlc.stop()
            lastTracksSignature = ""
            lastTechnicalProbeAtMs = 0L
            mutableCapabilities.value = PlaybackEngineCapabilities()
            mutableState.value = PlaybackUiState(
                media = if (clearMedia) null else currentRequest?.media,
                engine = engine,
                status = PlaybackStatus.IDLE
            )
            if (clearMedia) currentRequest = null
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    override fun seekTo(positionMs: Long): PlaybackCommandResult {
        if (!usable() || !opened) return PlaybackCommandResult.INVALID_STATE
        if (positionMs < 0L) return PlaybackCommandResult.INVALID_ARGUMENT
        if (mutableState.value.isLive) return PlaybackCommandResult.UNSUPPORTED
        if (!vlc.isSeekable) return PlaybackCommandResult.UNSUPPORTED

        return try {
            val duration = vlc.length.takeIf { it > 0L }
            val target = duration?.let { positionMs.coerceAtMost(it) } ?: positionMs
            vlc.time = target
            refreshProgress()
            mutableEvents.tryEmit(
                PlaybackEngineEvent.SeekCompleted(vlc.time.coerceAtLeast(0L))
            )
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    override fun seekBy(deltaMs: Long): PlaybackCommandResult {
        if (!usable() || !opened) return PlaybackCommandResult.INVALID_STATE
        val current = vlc.time.coerceAtLeast(0L)
        val target = (current.toDouble() + deltaMs.toDouble())
            .coerceIn(0.0, Long.MAX_VALUE.toDouble()).toLong()
        return seekTo(target)
    }

    override fun goToLiveEdge(): PlaybackCommandResult =
        PlaybackCommandResult.UNSUPPORTED // Janela de Timeshift ainda não verificada.

    override fun selectTrack(
        type: PlaybackTrackType,
        trackId: String?
    ): PlaybackCommandResult {
        if (!usable() || !opened) return PlaybackCommandResult.INVALID_STATE
        if (type == PlaybackTrackType.VIDEO) return PlaybackCommandResult.UNSUPPORTED
        val available = mutableState.value.availableTracks.filter { it.type == type }
        if (trackId != null && available.none { it.id == trackId }) {
            return PlaybackCommandResult.INVALID_ARGUMENT
        }
        val methodName = if (type == PlaybackTrackType.AUDIO) {
            "setAudioTrack"
        } else {
            "setSpuTrack"
        }
        val value = when {
            trackId != null -> trackId.substringAfter(':')
            type == PlaybackTrackType.SUBTITLE -> "-1"
            else -> available.firstOrNull()?.id?.substringAfter(':')
                ?: return PlaybackCommandResult.NOT_AVAILABLE
        }

        return if (invokeTrackSetter(methodName, value)) {
            refreshTracksIfChanged(force = true)
            PlaybackCommandResult.ACCEPTED
        } else {
            PlaybackCommandResult.UNSUPPORTED
        }
    }

    override fun selectVideoVariant(variantId: String?): PlaybackCommandResult =
        PlaybackCommandResult.UNSUPPORTED

    override fun setPlaybackSpeed(speed: Float): PlaybackCommandResult {
        if (!usable() || !opened) return PlaybackCommandResult.INVALID_STATE
        if (speed !in setOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) {
            return PlaybackCommandResult.INVALID_ARGUMENT
        }
        return try {
            vlc.rate = speed
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    override fun setVolume(volume: Float): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        if (!volume.isFinite() || volume !in 0f..1f) {
            return PlaybackCommandResult.INVALID_ARGUMENT
        }
        return try {
            preferredVolume = volume
            vlc.volume = (volume * 100f).toInt()
            Log.i(AUDIO_TAG, "VOLUME_DEFINIDO percentual=${(volume * 100f).toInt()}")
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    override fun setVideoLayout(layout: PlaybackVideoLayout): PlaybackCommandResult {
        if (!usable() || !opened) return PlaybackCommandResult.INVALID_STATE
        // Corte/zoom/largura/altura serão aplicados pela SurfaceView na UI.
        // Não simular a funcionalidade ou forçar composição que destrua o HDR.
        if (layout.mode != PlaybackAspectMode.NORMAL) {
            return PlaybackCommandResult.UNSUPPORTED
        }
        return try {
            applyBestFit()
            PlaybackCommandResult.ACCEPTED
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    override fun setSubtitleStyle(style: PlaybackSubtitleStyle): PlaybackCommandResult {
        if (!usable() || !opened) return PlaybackCommandResult.INVALID_STATE
        // A superfície VLC não expõe neste adaptador cor, fonte ou posição.
        if (style.sizeScale != 1f || style.textColorArgb != -0x1 ||
            style.backgroundColorArgb != 0x99000000.toInt() ||
            style.bottomOffsetPercent != 8
        ) return PlaybackCommandResult.UNSUPPORTED

        return try {
            // LibVLC utiliza microssegundos para o atraso de legendas.
            val applied = vlc.setSpuDelay(style.synchronizationOffsetMs * 1_000L)
            if (applied) {
                subtitleStyle = style
                PlaybackCommandResult.ACCEPTED
            } else {
                PlaybackCommandResult.UNSUPPORTED
            }
        } catch (_: Exception) {
            PlaybackCommandResult.UNSUPPORTED
        }
    }

    override fun release() {
        if (!onMainThread() || released) return
        Log.i("VlcNativePerf", "phase=exit_requested elapsedMs=${SystemClock.elapsedRealtime()-startupAtMs} reachedPlaying=$startupReachedPlaying")
        opened = false
        surfaceStart.close()
        audioSessionId++
        handler.removeCallbacks(positionTicker)
        abandonAudioFocus()
        detachVideoSurface()
        released = true
        runCatching { vlc.setEventListener(null) }
        runCatching { vlc.vlcVout.removeCallback(videoOutputCallback) }
        val releaseKind = currentRequest?.media?.type?.name ?: "UNKNOWN"
        NativePlayerReleaseQueue.enqueue {
            // Native shutdown can wait for a decoder/network thread. Never wait on Main.
            val stopStarted = SystemClock.elapsedRealtime()
            Log.i("VlcReleaseStepPerf", "kind=$releaseKind operation=stop phase=start")
            runCatching { vlc.stop() }.onFailure { Log.w(VIDEO_TAG, "ENCERRAMENTO_STOP_FALHOU") }
            Log.i("VlcReleaseStepPerf", "kind=$releaseKind operation=stop phase=complete durationMs=${SystemClock.elapsedRealtime() - stopStarted}")
            val player_releaseStarted = SystemClock.elapsedRealtime()
            Log.i("VlcReleaseStepPerf", "kind=$releaseKind operation=player_release phase=start")
            runCatching { vlc.release() }.onFailure { Log.w(VIDEO_TAG, "ENCERRAMENTO_PLAYER_FALHOU") }
            Log.i("VlcReleaseStepPerf", "kind=$releaseKind operation=player_release phase=complete durationMs=${SystemClock.elapsedRealtime() - player_releaseStarted}")
            val library_releaseStarted = SystemClock.elapsedRealtime()
            Log.i("VlcReleaseStepPerf", "kind=$releaseKind operation=library_release phase=start")
            runCatching { libVlc.release() }.onFailure { Log.w(VIDEO_TAG, "ENCERRAMENTO_VLC_FALHOU") }
            Log.i("VlcReleaseStepPerf", "kind=$releaseKind operation=library_release phase=complete durationMs=${SystemClock.elapsedRealtime() - library_releaseStarted}")
        }
        currentRequest = null
        mutableCapabilities.value = PlaybackEngineCapabilities()
        mutableState.value = PlaybackUiState(engine = engine)
    }

    /** Counters only: no native verbose logging, media addresses or playlist credentials. */
    private fun traceNativeStartup(phase:String) {
        if(released || !opened)return
        runCatching {
            val media=vlc.media
            val stats=try { media?.stats } finally { media?.release() }
            Log.i("VlcNativePerf", "phase=$phase elapsedMs=${SystemClock.elapsedRealtime()-startupAtMs} " +
                "surfaceReady=$nativeSurfacesReady nativeState=${vlc.playerState} stats=${stats!=null} " +
                "readBytes=${stats?.readBytes?:-1} demuxBytes=${stats?.demuxReadBytes?:-1} " +
                "decodedVideo=${stats?.decodedVideo?:-1} decodedAudio=${stats?.decodedAudio?:-1} " +
                "displayed=${stats?.displayedPictures?:-1} audioBuffers=${stats?.playedAbuffers?:-1}")
        }.onFailure { Log.w("VlcNativePerf", "phase=stats_unavailable") }
    }

    private fun handleEvent(event: MediaPlayer.Event) {
        if (released || !opened) return
        when (event.type) {
            MediaPlayer.Event.Opening -> {
                Log.i("VlcNativePerf", "phase=opening elapsedMs=${SystemClock.elapsedRealtime()-startupAtMs}")
                changeStatus(PlaybackStatus.PREPARING)
            }
            MediaPlayer.Event.Buffering -> {
                val percent=event.buffering.toInt().coerceIn(0,100)
                val bucket=percent/20
                if(!startupReachedPlaying && bucket!=bufferingBucket) {
                    bufferingBucket=bucket
                    Log.i("VlcNativePerf", "phase=buffering elapsedMs=${SystemClock.elapsedRealtime()-startupAtMs} percent=$percent")
                }
                if (vlc.isPlaying) changeStatus(PlaybackStatus.PLAYING)
                else changeStatus(PlaybackStatus.BUFFERING)
            }
            MediaPlayer.Event.Playing -> {
                startupReachedPlaying=true
                traceNativeStartup("playing")
                restorePositionIfPossible()
                applyAudioVolume()
                applyBestFit()
                if (!wantsToPlay) {
                    vlc.pause()
                    changeStatus(PlaybackStatus.PAUSED)
                } else {
                    changeStatus(PlaybackStatus.PLAYING)
                }
                refreshTracksIfChanged(force = true)
                refreshTechnicalInfo(force = true)
                probeAudio("PLAYING")
                val expectedSession = audioSessionId
                handler.postDelayed({
                    if (!released && opened && expectedSession == audioSessionId) {
                        refreshTracksIfChanged(force = true)
                        refreshTechnicalInfo(force = true)
                        ensureAudioTrackSelected()
                        probeAudio("APOS_1500MS")
                    }
                }, 1_500L)
            }
            MediaPlayer.Event.Paused -> changeStatus(PlaybackStatus.PAUSED)
            MediaPlayer.Event.Stopped -> changeStatus(PlaybackStatus.IDLE)
            MediaPlayer.Event.EndReached -> {
                changeStatus(PlaybackStatus.ENDED)
                mutableEvents.tryEmit(PlaybackEngineEvent.PlaybackEnded)
            }
            MediaPlayer.Event.EncounteredError ->
                reportLocalFailure("Falha de reprodução no VLC.")
            MediaPlayer.Event.Vout -> {
                // Vout confirma criação da saída de vídeo. Os metadados podem
                // chegar neste momento, mesmo que não existissem no Playing.
                refreshTechnicalInfo(force = true)
            }
            MediaPlayer.Event.ESAdded,
            MediaPlayer.Event.ESDeleted -> {
                refreshTracksIfChanged(force = true)
                refreshTechnicalInfo(force = true)
            }
        }
        refreshProgress()
    }

    private fun restorePositionIfPossible() {
        if (!restorePositionPending || !vlc.isSeekable) return
        val duration = vlc.length.takeIf { it > 0L }
        val target = duration?.let { requestedPositionMs.coerceAtMost(it) }
            ?: requestedPositionMs
        runCatching {
            vlc.time = target
            restorePositionPending = false
        }
    }

    private fun refreshProgress() {
        if (released || !opened) return
        restorePositionIfPossible()
        val live = currentRequest?.media?.type == PlaybackContentType.LIVE_CHANNEL
        val duration = if (live) null else vlc.length.takeIf { it > 0L }
        val position = vlc.time.coerceAtLeast(0L)
        mutableState.value = mutableState.value.copy(
            positionMs = position,
            durationMs = duration,
            isLive = live,
            bufferedPositionMs = null,
            // A janela live permanece desabilitada até termos Timeshift verificável.
            liveWindow = PlaybackLiveWindow()
        )
        val tracks = mutableState.value.availableTracks
        mutableCapabilities.value = PlaybackEngineCapabilities(
            supportsSeeking = !live && vlc.isSeekable,
            supportsAudioTrackSelection = tracks.any { it.type == PlaybackTrackType.AUDIO },
            supportsSubtitleTrackSelection = tracks.any { it.type == PlaybackTrackType.SUBTITLE },
            supportsSubtitleTimingOffset = tracks.any { it.type == PlaybackTrackType.SUBTITLE },
            supportsPlaybackSpeed = true,
            supportsVolumeControl = true
        )
    }

    private fun changeStatus(status: PlaybackStatus) {
        mutableState.value = mutableState.value.copy(status = status, failure = null)
    }

    private fun reportLocalFailure(message: String) {
        if (released) return
        val failure = PlaybackFailure(
            type = PlaybackFailureType.UNKNOWN,
            message = message,
            isRecoverable = true
        )
        mutableState.value = mutableState.value.copy(
            status = PlaybackStatus.ERROR,
            failure = failure
        )
        mutableEvents.tryEmit(PlaybackEngineEvent.Failure(failure))
    }

    /**
     * Dados REAIS publicados pela LibVLC. Sem abrir outro player ou stream.
     * Alguns servidores só entregam dimensões e codecs após ESAdded/Vout.
     * O ticker repete a consulta moderadamente para esses casos.
     *
     * getMedia() incrementa a referência nativa: liberar sempre a referência
     * recebida, sem modificar a Media associada ao reprodutor.
     */
    private fun refreshTechnicalInfo(force: Boolean = false) {
        if (released || !opened) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastTechnicalProbeAtMs < 2_400L) return
        lastTechnicalProbeAtMs = now

        var video: Any? = invokeNoArgs("getCurrentVideoTrack")
        var audio: Any? = null
        var statsBitrate: Long? = null

        val media = runCatching { vlc.media }.getOrNull()
        if (media != null) {
            try {
                val tracks = readTechnicalTracks(media)
                if (video == null) video = tracks.firstOrNull { isTrackOfType(it, "VideoTrack", 1) }
                audio = tracks.firstOrNull { isTrackOfType(it, "AudioTrack", 0) }

                val stats = invokeObjectMethod(media, "getStats")
                // As estatísticas de entrada do VLC são taxas de kB/s.
                // Convertê-las para bits/s apenas se houver medição positiva.
                val demuxRate = (readTechnicalMember(stats, "demuxBitrate") as? Number)
                    ?.toDouble()?.takeIf { it.isFinite() && it > 0.0 }
                val inputRate = (readTechnicalMember(stats, "inputBitrate") as? Number)
                    ?.toDouble()?.takeIf { it.isFinite() && it > 0.0 }
                val measuredRate = demuxRate ?: inputRate
                statsBitrate = measuredRate?.times(8_000.0)
                    ?.takeIf { it < Long.MAX_VALUE.toDouble() }
                    ?.toLong()
            } catch (failure: Exception) {
                Log.d(VIDEO_TAG, "Metadados VLC ainda indisponiveis: ${failure.javaClass.simpleName}")
            } finally {
                runCatching { media.release() }
            }
        }

        // Em algumas versões o VLC mantém faixas selecionadas fora da Media.
        if (video == null) video = invokeIntMethod(vlc, "getSelectedTrack", 1)
        if (audio == null) audio = invokeIntMethod(vlc, "getSelectedTrack", 0)

        val old = mutableState.value.technicalInfo
        val width = numberFromMember(video, "width")?.toInt()?.takeIf { it in 1..16_384 }
        val height = numberFromMember(video, "height")?.toInt()?.takeIf { it in 1..16_384 }
        val fpsNum = numberFromMember(video, "frameRateNum")?.toDouble()
        val fpsDen = numberFromMember(video, "frameRateDen")?.toDouble()
        val fps = if (fpsNum != null && fpsDen != null && fpsNum > 0 && fpsDen > 0) {
            (fpsNum / fpsDen).takeIf { it.isFinite() && it in 1.0..240.0 }?.toFloat()
        } else null

        val videoBitrate = numberFromMember(video, "bitrate")?.toLong()?.takeIf { it > 0L }
        val audioBitrate = numberFromMember(audio, "bitrate")?.toLong()?.takeIf { it > 0L }
        // Bitrate de trilha vem do arquivo; estatística é a medição corrente.
        val trackBitrate = videoBitrate?.let { it + (audioBitrate ?: 0L) }
        val info = old.copy(
            width = width ?: old.width,
            height = height ?: old.height,
            framesPerSecond = fps ?: old.framesPerSecond,
            videoCodec = codecFromTrack(video) ?: old.videoCodec,
            audioCodec = codecFromTrack(audio) ?: old.audioCodec,
            bitrateBitsPerSecond = trackBitrate ?: statsBitrate ?: old.bitrateBitsPerSecond
            // Não deduzir HDR por codec ou resolução: manter UNKNOWN.
        )
        if (info != old) {
            mutableState.value = mutableState.value.copy(technicalInfo = info)
            Log.d(VIDEO_TAG, "METADADOS_ATUALIZADOS resolucao=${info.width}x${info.height} " +
                    "video=${info.videoCodec != null} audio=${info.audioCodec != null} " +
                    "fps=${info.framesPerSecond != null} bitrate=${info.bitrateBitsPerSecond != null}")
        }
    }

    private fun readTechnicalTracks(media: Any): List<Any> {
        val allTracks = invokeObjectMethod(media, "getTracks")
        val fromArray = when (allTracks) {
            is Array<*> -> allTracks.filterNotNull()
            is Collection<*> -> allTracks.filterNotNull()
            else -> emptyList()
        }
        if (fromArray.isNotEmpty()) return fromArray

        // Algumas versões 3.x publicam getTrackCount()/getTrack(index).
        val count = (invokeObjectMethod(media, "getTrackCount") as? Number)
            ?.toInt()?.coerceIn(0, 64) ?: return emptyList()
        val getTrack = media.javaClass.methods.firstOrNull {
            it.name == "getTrack" && it.parameterTypes.size == 1 &&
                    (it.parameterTypes[0] == Int::class.javaPrimitiveType ||
                            it.parameterTypes[0] == Int::class.javaObjectType)
        } ?: return emptyList()
        return (0 until count).mapNotNull { index ->
            runCatching { getTrack.invoke(media, index) }.getOrNull()
        }
    }

    private fun isTrackOfType(track: Any, className: String, code: Int): Boolean {
        if (track.javaClass.simpleName == className) return true
        return (readTechnicalMember(track, "type") as? Number)?.toInt() == code
    }

    private fun numberFromMember(target: Any?, name: String): Number? =
        readTechnicalMember(target, name) as? Number

    private fun codecFromTrack(track: Any?): String? =
        (readTechnicalMember(track, "codec") as? String)
            ?.trim()?.takeIf { it.isNotBlank() && it != "0" }

    /** Trata campos públicos do LibVLC 3.x e getters do LibVLC mais recente. */
    private fun readTechnicalMember(target: Any?, name: String): Any? {
        if (target == null) return null
        val field = runCatching { target.javaClass.getField(name).get(target) }.getOrNull()
        if (field != null) return field
        val getter = "get" + name.replaceFirstChar { it.uppercaseChar() }
        return invokeObjectMethod(target, getter)
    }

    private fun invokeObjectMethod(target: Any, name: String): Any? = runCatching {
        target.javaClass.methods.firstOrNull {
            it.name == name && it.parameterTypes.isEmpty()
        }?.invoke(target)
    }.getOrNull()

    private fun invokeIntMethod(target: Any, name: String, argument: Int): Any? = runCatching {
        target.javaClass.methods.firstOrNull {
            it.name == name && it.parameterTypes.size == 1 &&
                    (it.parameterTypes[0] == Int::class.javaPrimitiveType ||
                            it.parameterTypes[0] == Int::class.javaObjectType)
        }?.invoke(target, argument)
    }.getOrNull()

    /**
     * As versões LibVLC 3.x expõem faixas como TrackDescription (IDs Int)
     * ou VlcTrack (IDs String). Inspecionamos somente esse pequeno conjunto
     * de métodos públicos. Se não existir, a opção fica indisponível.
     */
    private fun refreshTracksIfChanged(force: Boolean = false) {
        if (released || !opened) return
        val audio = readTracks("getAllAudioTracks", "getAudioTracks", PlaybackTrackType.AUDIO)
        val subtitles = readTracks("getAllSpuTracks", "getSpuTracks", PlaybackTrackType.SUBTITLE)
        val result = audio + subtitles
        val signature = result.joinToString("|") { it.id + "=" + it.label }
        if (!force && signature == lastTracksSignature) return
        lastTracksSignature = signature
        mutableState.value = mutableState.value.copy(
            availableTracks = result,
            selectedAudioTrackId = readSelectedTrackId(
                "getSelectedAudioTrack", "getAudioTrack", "audio:"
            ),
            selectedSubtitleTrackId = readSelectedTrackId(
                "getSelectedSpuTrack", "getSpuTrack", "subtitle:"
            )
        )
    }

    private fun readTracks(
        modernName: String,
        legacyName: String,
        type: PlaybackTrackType
    ): List<PlaybackTrack> {
        val raw = invokeNoArgs(modernName) ?: invokeNoArgs(legacyName) ?: return emptyList()
        val entries = when (raw) {
            is Array<*> -> raw.toList()
            is Collection<*> -> raw.toList()
            else -> emptyList()
        }
        val prefix = if (type == PlaybackTrackType.AUDIO) "audio:" else "subtitle:"
        return entries.mapNotNull { entry ->
            entry ?: return@mapNotNull null
            val id = extractMember(entry, "getId", "id")?.toString() ?: return@mapNotNull null
            if (id == "-1") return@mapNotNull null
            val label = extractMember(entry, "getName", "name")?.toString()
                ?.takeIf { it.isNotBlank() } ?: "$type $id"
            val language = extractMember(entry, "getLanguage", "language")?.toString()
            PlaybackTrack(
                id = "$prefix$id",
                type = type,
                label = label,
                languageTag = language
            )
        }
    }

    private fun readSelectedTrackId(
        modernName: String,
        legacyName: String,
        prefix: String
    ): String? {
        val value = invokeNoArgs(modernName) ?: invokeNoArgs(legacyName) ?: return null
        val id = if (value is Number || value is String) value.toString()
        else extractMember(value, "getId", "id")?.toString() ?: return null
        return if (id == "-1") null else prefix + id
    }

    private fun invokeTrackSetter(name: String, id: String): Boolean {
        val methods = vlc.javaClass.methods.filter { it.name == name && it.parameterTypes.size == 1 }
        for (method in methods) {
            val arg: Any = when (method.parameterTypes[0]) {
                String::class.java -> id
                Int::class.javaPrimitiveType, Int::class.javaObjectType ->
                    id.toIntOrNull() ?: continue
                else -> continue
            }
            val invocation = runCatching { method.invoke(vlc, arg) }
            if (invocation.isFailure) continue
            val result = invocation.getOrNull()
            return result !is Boolean || result
        }
        return false
    }

    private fun invokeNoArgs(methodName: String): Any? = runCatching {
        vlc.javaClass.methods.firstOrNull {
            it.name == methodName && it.parameterTypes.isEmpty()
        }?.invoke(vlc)
    }.getOrNull()

    private fun extractMember(value: Any, getter: String, field: String): Any? {
        val method = value.javaClass.methods.firstOrNull {
            it.name == getter && it.parameterTypes.isEmpty()
        }
        val fromMethod = runCatching { method?.invoke(value) }.getOrNull()
        if (fromMethod != null) return fromMethod
        return runCatching { value.javaClass.getField(field).get(value) }.getOrNull()
    }

    private fun configureAudioOutput() {
        // VideoLAN recomenda android_audiotrack, e "stereo" forca saida PCM
        // compatível quando o sistema sinaliza passthrough nao utilizavel.
        // Aplicar antes de iniciar a reproducao.
        audioOutputConfigured = runCatching {
            vlc.setAudioOutput("android_audiotrack")
        }.getOrDefault(false)
        audioStereoConfigured = runCatching {
            vlc.setAudioOutputDevice("stereo")
        }.getOrDefault(false)
        Log.i(
            AUDIO_TAG,
            "SAIDA_AUDIO AudioTrack=$audioOutputConfigured " +
                    "estereoPCM=$audioStereoConfigured"
        )
    }

    private fun requestAudioFocus() {
        if (audioFocusGranted) return
        if (audioFocusRequest == null) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(audioFocusListener, handler)
                .build()
        }
        val focus = runCatching {
            audioManager.requestAudioFocus(audioFocusRequest!!)
        }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        audioFocusGranted = focus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.i(AUDIO_TAG, "FOCO_SOLICITADO concedido=$audioFocusGranted")
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { request ->
            runCatching { audioManager.abandonAudioFocusRequest(request) }
        }
        audioFocusGranted = false
    }

    private fun applyAudioVolume() {
        runCatching { vlc.volume = (preferredVolume * 100f).toInt() }
    }

    /**
     * Ativa a primeira trilha somente se o LibVLC detectar trilhas mas nao
     * tiver nenhuma selecionada. Nao modifica trilhas ja selecionadas.
     */
    private fun ensureAudioTrackSelected() {
        val tracks = mutableState.value.availableTracks
            .filter { it.type == PlaybackTrackType.AUDIO }
        if (tracks.isEmpty()) return
        val selected = readSelectedTrackId(
            "getSelectedAudioTrack", "getAudioTrack", "audio:"
        )
        if (selected != null) return
        val candidate = tracks.first().id.substringAfter(':')
        val accepted = invokeTrackSetter("setAudioTrack", candidate)
        Log.i(AUDIO_TAG, "FAIXA_AUTO_SELECIONADA resultado=$accepted")
        if (accepted) refreshTracksIfChanged(force = true)
    }

    private fun probeAudio(stage: String) {
        val trackCount = mutableState.value.availableTracks
            .count { it.type == PlaybackTrackType.AUDIO }
        val trackId = mutableState.value.selectedAudioTrackId
        val volume = runCatching { vlc.volume }.getOrDefault(-1)
        val playing = runCatching { vlc.isPlaying }.getOrDefault(false)
        Log.i(
            AUDIO_TAG,
            "DIAGNOSTICO etapa=$stage tocando=$playing " +
                    "volume=$volume trilhas=$trackCount " +
                    "trilhaAtiva=${if (trackId == null) "NAO" else "SIM"} " +
                    "foco=$audioFocusGranted AudioTrack=$audioOutputConfigured " +
                    "estereoPCM=$audioStereoConfigured"
        )
    }

    private fun onMainThread() = Looper.myLooper() == Looper.getMainLooper()
    private fun usable() = onMainThread() && !released
}

// IPTV_AUTOMATIC_DOLBY_AND_EPISODE_CONTROLS_USER26_V2
