package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import android.os.Looper
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Eventos do controlador. Nunca incluem URLs ou credenciais da playlist. */
sealed interface PlaybackControllerEvent {
    data class Reconnecting(
        val engine: PlaybackEngine,
        val attempt: Int,
        val maximum: Int
    ) : PlaybackControllerEvent

    data class EngineChanged(
        val previous: PlaybackEngine?,
        val current: PlaybackEngine,
        val automatic: Boolean
    ) : PlaybackControllerEvent

    data class PlayerEvent(val event: PlaybackEngineEvent) : PlaybackControllerEvent
    data class UnrecoverableError(val failure: PlaybackFailure) : PlaybackControllerEvent
}

/**
 * Coordena Media3 e VLC sem manter dois decodificadores ativos.
 *
 * - Criar, chamar e liberar na thread PRINCIPAL.
 * - Media3 e o padrao (ou o motor configurado em PlaybackSettings).
 * - Erros temporarios: no maximo maxReconnectAttempts por motor.
 * - Erro de formato/decoder: tenta o outro motor, se permitido.
 * - No maximo UMA troca automatica por abertura de conteudo.
 * - Mudanca manual pelo usuario sempre continua disponivel.
 * - Nao faz historico Room, nem expoe links Xtream em logs.
 *
 * O controlador e dono dos adaptadores e do seu CoroutineScope. A SurfaceView
 * pertence a interface: o controlador recebe somente seu SurfaceHolder.
 */
class PlaybackController(
    context: Context,
    private val recoveryPolicy: PlaybackRecoveryPolicy = PlaybackRecoveryPolicy(),
    factory: PlaybackEngineAdapterFactory? = null
) {
    private val applicationContext = context.applicationContext
    private val adapterFactory: PlaybackEngineAdapterFactory = factory
        ?: PlaybackEngineAdapterFactory { engine ->
            when (engine) {
                PlaybackEngine.MEDIA3 -> Media3PlaybackAdapter(applicationContext)
                PlaybackEngine.VLC -> LibVlcPlaybackAdapter(applicationContext)
            }
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = mutableState.asStateFlow()

    private val mutableCapabilities = MutableStateFlow(PlaybackEngineCapabilities())
    val capabilities: StateFlow<PlaybackEngineCapabilities> =
        mutableCapabilities.asStateFlow()

    private val mutableEvents = MutableSharedFlow<PlaybackControllerEvent>(
        replay = 0,
        extraBufferCapacity = 32
    )
    val events: SharedFlow<PlaybackControllerEvent> = mutableEvents.asSharedFlow()

    private var adapter: PlaybackEngineAdapter? = null
    private var activeEngine: PlaybackEngine? = null
    private var request: PlaybackOpenRequest? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var released = false
    private var generation = 0L

    private var stateJob: Job? = null
    private var capabilitiesJob: Job? = null
    private var eventsJob: Job? = null
    private var recoveryJob: Job? = null
    private var activationJob: Job? = null

    private var reconnectAttempts = 0
    private var automaticSwitches = 0
    private var lastKnownPositionMs = 0L
    private var wantsToPlay = true
    private var currentVolume = 1f
    private var handledFailureGeneration = -1L
    private var preferencesAppliedGeneration = -1L
    private val restoredTrackKeys = mutableSetOf<String>()

    init {
        check(onMainThread()) {
            "PlaybackController deve ser criado na thread principal."
        }
    }

    /** Abre uma nova midia, zerando somente os contadores de recuperacao. */
    fun open(openRequest: PlaybackOpenRequest): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        request = openRequest
        wantsToPlay = openRequest.playWhenReady
        reconnectAttempts = 0
        automaticSwitches = 0
        lastKnownPositionMs = if (openRequest.media.type == PlaybackContentType.LIVE_CHANNEL) {
            0L
        } else {
            openRequest.startPositionMs
        }
        return activate(openRequest.settings.preferredEngine, automatic = false)
    }

    /** Troca solicitada pelo usuario; pode voltar ao motor anterior. */
    fun switchEngine(engine: PlaybackEngine): PlaybackCommandResult {
        if (!usable() || request == null) return PlaybackCommandResult.INVALID_STATE
        if (activeEngine == engine && adapter != null) return PlaybackCommandResult.ACCEPTED
        reconnectAttempts = 0
        automaticSwitches = 0
        request = request?.copy(
            settings = request!!.settings.copy(preferredEngine = engine)
        )
        return activate(engine, automatic = false)
    }

    /** Conecta a unica superficie compartilhada pela interface. */
    fun attachVideoSurface(holder: SurfaceHolder): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        if (holder === surfaceHolder) return PlaybackCommandResult.ACCEPTED
        adapter?.detachVideoSurface()
        surfaceHolder = holder
        return adapter?.attachVideoSurface(holder) ?: PlaybackCommandResult.ACCEPTED
    }

    fun detachVideoSurface(holder: SurfaceHolder? = null) {
        if (!usable()) return
        if (holder != null && holder !== surfaceHolder) return
        adapter?.detachVideoSurface()
        surfaceHolder = null
    }

    fun play(): PlaybackCommandResult = forward {
        val result = it.play()
        if (result == PlaybackCommandResult.ACCEPTED) wantsToPlay = true
        result
    }

    fun pause(): PlaybackCommandResult = forward {
        val result = it.pause()
        if (result == PlaybackCommandResult.ACCEPTED) wantsToPlay = false
        result
    }

    fun seekTo(positionMs: Long): PlaybackCommandResult = forward { engine ->
        val result = engine.seekTo(positionMs)
        if (result == PlaybackCommandResult.ACCEPTED &&
            request?.media?.type != PlaybackContentType.LIVE_CHANNEL
        ) {
            lastKnownPositionMs = positionMs.coerceAtLeast(0L)
        }
        result
    }

    fun seekBy(deltaMs: Long): PlaybackCommandResult = forward { engine ->
        val before = engine.state.value.positionMs
        val result = engine.seekBy(deltaMs)
        if (result == PlaybackCommandResult.ACCEPTED &&
            request?.media?.type != PlaybackContentType.LIVE_CHANNEL
        ) {
            val target = (before.toDouble() + deltaMs.toDouble())
                .coerceIn(0.0, Long.MAX_VALUE.toDouble()).toLong()
            lastKnownPositionMs = target
        }
        result
    }
    fun goToLiveEdge(): PlaybackCommandResult = forward { it.goToLiveEdge() }

    fun selectTrack(type: PlaybackTrackType, trackId: String?): PlaybackCommandResult =
        forward { engine ->
            val selected = engine.state.value.availableTracks.firstOrNull {
                it.type == type && it.id == trackId
            }
            val result = engine.selectTrack(type, trackId)
            if (result == PlaybackCommandResult.ACCEPTED) {
                val settings = request?.settings
                if (settings != null) {
                    val changed = when (type) {
                        // "Automático" precisa remover uma preferência anterior;
                        // do contrário ela seria aplicada de novo numa troca de motor.
                        PlaybackTrackType.AUDIO -> settings.copy(
                            audioLanguageTag = selected?.languageTag
                        )
                        PlaybackTrackType.SUBTITLE -> settings.copy(
                            subtitlesEnabled = trackId != null,
                            subtitleLanguageTag = selected?.languageTag
                                ?: settings.subtitleLanguageTag
                        )
                        PlaybackTrackType.VIDEO -> settings
                    }
                    request = request?.copy(settings = changed)
                    // Uma escolha manual deve prevalecer sobre a preferência por
                    // idioma restaurada automaticamente nesta mesma sessão.
                    // Na troca de motor as chaves são limpas e a preferência
                    // de idioma volta a ser usada (IDs de tracks são diferentes).
                    val chosenLanguage = when (type) {
                        PlaybackTrackType.AUDIO -> changed.audioLanguageTag
                        PlaybackTrackType.SUBTITLE -> changed.subtitleLanguageTag
                        PlaybackTrackType.VIDEO -> null
                    }
                    val enabled = type != PlaybackTrackType.SUBTITLE || trackId != null
                    restoredTrackKeys.add("${type.name}:$enabled:${chosenLanguage.orEmpty()}")
                }
            }
            result
        }

    fun selectVideoVariant(variantId: String?): PlaybackCommandResult = forward {
        val result = it.selectVideoVariant(variantId)
        if (result == PlaybackCommandResult.ACCEPTED) {
            request = request?.copy(settings = request!!.settings.copy(
                selectedVideoVariantId = variantId
            ))
        }
        result
    }

    fun setPlaybackSpeed(speed: Float): PlaybackCommandResult = forward {
        val result = it.setPlaybackSpeed(speed)
        if (result == PlaybackCommandResult.ACCEPTED) {
            request = request?.copy(settings = request!!.settings.copy(speed = speed))
        }
        result
    }

    fun setVolume(volume: Float): PlaybackCommandResult = forward {
        val result = it.setVolume(volume)
        if (result == PlaybackCommandResult.ACCEPTED) currentVolume = volume
        result
    }

    fun setVideoLayout(layout: PlaybackVideoLayout): PlaybackCommandResult = forward {
        val result = it.setVideoLayout(layout)
        if (result == PlaybackCommandResult.ACCEPTED) {
            request = request?.copy(settings = request!!.settings.copy(aspect = layout))
        }
        result
    }

    fun setSubtitleStyle(style: PlaybackSubtitleStyle): PlaybackCommandResult = forward {
        val result = it.setSubtitleStyle(style)
        if (result == PlaybackCommandResult.ACCEPTED) {
            request = request?.copy(settings = request!!.settings.copy(subtitleStyle = style))
        }
        result
    }

    /** Interrompe; open() pode ser chamado novamente depois. */
    fun stop(clearMedia: Boolean = true): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        invalidateSession()
        // release() also stops playback; do not call the blocking native stop twice.
        discardAdapter()
        if (clearMedia) request = null
        mutableState.value = PlaybackUiState(media = request?.media)
        mutableCapabilities.value = PlaybackEngineCapabilities()
        return PlaybackCommandResult.ACCEPTED
    }

    /** Libera definitivamente todos os recursos. Chamar na thread principal. */
    fun release() {
        if (released) return
        check(onMainThread()) { "release() deve ser executado na thread principal." }
        released = true
        invalidateSession()
        discardAdapter()
        surfaceHolder = null
        request = null
        mutableState.value = PlaybackUiState()
        mutableCapabilities.value = PlaybackEngineCapabilities()
        scope.cancel()
    }

    private fun activate(
        target: PlaybackEngine,
        automatic: Boolean
    ): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        val previous = activeEngine
        val original = request ?: return PlaybackCommandResult.INVALID_STATE
        val position = if (original.media.type == PlaybackContentType.LIVE_CHANNEL) 0L
        else lastKnownPositionMs.coerceAtLeast(0L)
        val nextRequest = original.copy(
            startPositionMs = position,
            playWhenReady = wantsToPlay
        )

        // Primeiro encerra a sessao anterior; nunca mantem dois decoders abertos.
        invalidateSession()
        discardAdapter()
        val session = generation
        activeEngine = target
        mutableState.value = PlaybackUiState(
            media = original.media,
            engine = target,
            status = PlaybackStatus.PREPARING,
            positionMs = position,
            isLive = original.media.type == PlaybackContentType.LIVE_CHANNEL
        )
        mutableCapabilities.value = PlaybackEngineCapabilities()

        val pending = NativePlayerReleaseQueue.pending
        if (target == PlaybackEngine.VLC || (pending != null && !pending.isCompleted)) {
            activationJob = scope.launch {
                // A second screen may have queued another native shutdown meanwhile.
                while (true) {
                    val cleanup = NativePlayerReleaseQueue.pending ?: break
                    if (cleanup.isCompleted) break
                    cleanup.await()
                }
                if (target == PlaybackEngine.VLC) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        VlcFontConfiguration.prepare(applicationContext)
                    }
                }
                if (isCurrentSession(session)) {
                    startAdapter(target, automatic, previous, original, nextRequest, session)
                }
            }
            return PlaybackCommandResult.ACCEPTED
        }
        return startAdapter(target, automatic, previous, original, nextRequest, session)
    }

    private fun startAdapter(target: PlaybackEngine, automatic: Boolean, previous: PlaybackEngine?,
        original: PlaybackOpenRequest, nextRequest: PlaybackOpenRequest, session: Long): PlaybackCommandResult {
        val created = try {
            adapterFactory.create(target)
        } catch (_: Exception) {
            handleFailure(session, unavailableFailure())
            return PlaybackCommandResult.NOT_AVAILABLE
        }
        if (created.engine != target) {
            created.release()
            handleFailure(session, unavailableFailure())
            return PlaybackCommandResult.INVALID_STATE
        }
        adapter = created
        surfaceHolder?.let { holder ->
            // A superficie pode estar sendo recriada pelo Android TV.
            try {
                created.attachVideoSurface(holder)
            } catch (_: Exception) {
                // A reproduçao pode prosseguir e esperar uma nova superficie.
            }
        }

        // Inscreve nos Flows antes de prepare(), para nao perder erros imediatos.
        stateJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            created.state.collect { snapshot ->
                if (!isCurrent(session, created)) return@collect
                // O posicionamento pode diminuir depois de retroceder. Nunca
                // usar max(posicaoAtual, posicaoAntiga): isso perderia o seek.
                if (original.media.type != PlaybackContentType.LIVE_CHANNEL &&
                    snapshot.positionMs > 0L &&
                    snapshot.status in setOf(
                        PlaybackStatus.READY,
                        PlaybackStatus.PLAYING,
                        PlaybackStatus.PAUSED,
                        PlaybackStatus.BUFFERING
                    )
                ) {
                    lastKnownPositionMs = snapshot.positionMs
                }
                // IDLE inicial do adaptador nao deve apagar a midia da UI.
                if (snapshot.status == PlaybackStatus.IDLE && snapshot.media == null) {
                    return@collect
                }
                // Tickers repetem ERROR enquanto a recuperacao esta agendada.
                // Nao deixam a UI oferecer erro definitivo durante a troca.
                mutableState.value = if (handledFailureGeneration == session && recoveryJob?.isActive == true) {
                    snapshot.copy(engine = target, status = PlaybackStatus.BUFFERING, failure = null)
                } else snapshot.copy(engine = target)
                if (snapshot.status == PlaybackStatus.ERROR) {
                    handleFailure(session, snapshot.failure ?: unavailableFailure())
                } else if (snapshot.status in setOf(
                        PlaybackStatus.READY,
                        PlaybackStatus.PLAYING,
                        PlaybackStatus.PAUSED
                    )
                ) {
                    applyPlaybackPreferences(session, created, snapshot)
                }
            }
        }
        capabilitiesJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            created.capabilities.collect { value ->
                if (isCurrent(session, created)) mutableCapabilities.value = value
            }
        }
        eventsJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            created.events.collect { event ->
                if (!isCurrent(session, created)) return@collect
                // O StateFlow pode confluir um erro rapido com outro estado.
                // A geracao deduplica erro publicado tanto no evento como no estado.
                if (event is PlaybackEngineEvent.Failure) {
                    handleFailure(session, event.failure)
                } else {
                    mutableEvents.tryEmit(PlaybackControllerEvent.PlayerEvent(event))
                }
            }
        }

        val prepared = try {
            created.prepare(nextRequest)
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
        if (prepared != PlaybackCommandResult.ACCEPTED) {
            handleFailure(session, unavailableFailure())
        } else if (previous != target) {
            mutableEvents.tryEmit(
                PlaybackControllerEvent.EngineChanged(previous, target, automatic)
            )
        }
        return prepared
    }

    private fun handleFailure(session: Long, failure: PlaybackFailure) {
        if (!isCurrentSession(session) || handledFailureGeneration == session) return
        handledFailureGeneration = session
        recoveryJob?.cancel()
        recoveryJob = null

        // An explicit refusal is not a transient network or decoder failure.
        if (failure.errorCode in setOf("MEDIA3_HTTP_401", "MEDIA3_HTTP_403")) {
            finishWithFailure(failure)
            return
        }
        val canRecover = failure.isRecoverable &&
                recoveryPolicy.canTryAlternateEngine(failure.type)
        if (!canRecover) {
            finishWithFailure(failure)
            return
        }
        val current = activeEngine ?: run {
            finishWithFailure(failure)
            return
        }
        val mayReconnect = failure.errorCode != "MEDIA3_HTTP_REDIRECT" &&
                !(current == PlaybackEngine.VLC && failure.type == PlaybackFailureType.UNKNOWN) &&
                failure.type in setOf(
            PlaybackFailureType.NETWORK,
            PlaybackFailureType.TIMEOUT,
            PlaybackFailureType.UNKNOWN
        )
        if (mayReconnect && reconnectAttempts < recoveryPolicy.maxReconnectAttempts) {
            reconnectAttempts++
            val attempt = reconnectAttempts
            mutableEvents.tryEmit(PlaybackControllerEvent.Reconnecting(
                current, attempt, recoveryPolicy.maxReconnectAttempts
            ))
            mutableState.value = mutableState.value.copy(
                status = PlaybackStatus.BUFFERING,
                failure = null
            )
            recoveryJob = scope.launch {
                delay(if (attempt == 1) 600L else 1400L)
                if (!isCurrentSession(session)) return@launch
                recoveryJob = null
                activate(current, automatic = true)
            }
            return
        }
        if (automaticSwitches < recoveryPolicy.maxAutomaticEngineSwitches) {
            automaticSwitches++
            reconnectAttempts = 0
            mutableState.value = mutableState.value.copy(
                status = PlaybackStatus.BUFFERING,
                failure = null
            )
            recoveryJob = scope.launch {
                delay(250L)
                if (!isCurrentSession(session)) return@launch
                recoveryJob = null
                activate(current.alternate(), automatic = true)
            }
            return
        }
        finishWithFailure(failure)
    }

    private fun applyPlaybackPreferences(
        session: Long,
        engine: PlaybackEngineAdapter,
        snapshot: PlaybackUiState
    ) {
        val settings = request?.settings ?: return
        if (preferencesAppliedGeneration != session) {
            preferencesAppliedGeneration = session
            engine.setVolume(currentVolume)
            engine.setPlaybackSpeed(settings.speed)
            engine.setVideoLayout(settings.aspect)
            engine.setSubtitleStyle(settings.subtitleStyle)
            settings.selectedVideoVariantId?.let { variantId ->
                if (snapshot.availableVideoVariants.any { it.id == variantId }) {
                    engine.selectVideoVariant(variantId)
                }
            }
        }
        if (snapshot.availableTracks.isEmpty()) return
        restoreTrack(
            engine, snapshot, PlaybackTrackType.AUDIO,
            settings.audioLanguageTag, enabled = true
        )
        restoreTrack(
            engine, snapshot, PlaybackTrackType.SUBTITLE,
            settings.subtitleLanguageTag, enabled = settings.subtitlesEnabled
        )
    }

    private fun restoreTrack(
        engine: PlaybackEngineAdapter,
        snapshot: PlaybackUiState,
        type: PlaybackTrackType,
        preferredLanguage: String?,
        enabled: Boolean
    ) {
        val key = "${type.name}:$enabled:${preferredLanguage.orEmpty()}"
        if (key in restoredTrackKeys) return
        val available = snapshot.availableTracks.filter { it.type == type }
        if (available.isEmpty()) return
        if (type == PlaybackTrackType.SUBTITLE && !enabled) {
            restoredTrackKeys.add(key)
            engine.selectTrack(type, null)
            return
        }
        val language = preferredLanguage?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val selected = available.firstOrNull {
            it.languageTag?.equals(language, ignoreCase = true) == true ||
                    it.languageTag?.substringBefore('-')
                        ?.equals(language.substringBefore('-'), ignoreCase = true) == true
        } ?: return
        restoredTrackKeys.add(key)
        engine.selectTrack(type, selected.id)
    }

    private fun finishWithFailure(failure: PlaybackFailure) {
        mutableState.value = mutableState.value.copy(
            status = PlaybackStatus.ERROR,
            failure = failure
        )
        mutableEvents.tryEmit(PlaybackControllerEvent.UnrecoverableError(failure))
    }

    private fun unavailableFailure() = PlaybackFailure(
        type = PlaybackFailureType.UNKNOWN,
        message = "Nao foi possivel iniciar este reprodutor.",
        isRecoverable = true
    )

    private inline fun forward(
        command: (PlaybackEngineAdapter) -> PlaybackCommandResult
    ): PlaybackCommandResult {
        if (!usable()) return PlaybackCommandResult.INVALID_STATE
        val engine = adapter ?: return PlaybackCommandResult.INVALID_STATE
        return try {
            command(engine)
        } catch (_: Exception) {
            PlaybackCommandResult.NOT_AVAILABLE
        }
    }

    private fun invalidateSession() {
        generation++
        activationJob?.cancel()
        activationJob = null
        recoveryJob?.cancel()
        recoveryJob = null
        stateJob?.cancel()
        capabilitiesJob?.cancel()
        eventsJob?.cancel()
        stateJob = null
        capabilitiesJob = null
        eventsJob = null
        restoredTrackKeys.clear()
        handledFailureGeneration = -1L
        preferencesAppliedGeneration = -1L
    }

    private fun discardAdapter() {
        val old = adapter
        adapter = null
        activeEngine = null
        if (old != null) {
            old.detachVideoSurface()
            old.release()
        }
    }

    private fun isCurrentSession(session: Long) = !released && generation == session
    private fun isCurrent(session: Long, engine: PlaybackEngineAdapter) =
        isCurrentSession(session) && adapter === engine
    private fun onMainThread() = Looper.myLooper() == Looper.getMainLooper()
    private fun usable() = !released && onMainThread()
}

// IPTV_AUTOMATIC_DOLBY_AND_EPISODE_CONTROLS_USER26_V2
