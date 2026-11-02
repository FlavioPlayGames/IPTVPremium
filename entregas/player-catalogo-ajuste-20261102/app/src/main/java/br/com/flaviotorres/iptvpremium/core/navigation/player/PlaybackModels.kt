package br.com.flaviotorres.iptvpremium.core.player

import androidx.media3.common.text.Cue

import br.com.flaviotorres.iptvpremium.domain.model.Channel
import br.com.flaviotorres.iptvpremium.domain.model.Episode
import br.com.flaviotorres.iptvpremium.domain.model.Movie

/**
 * Contratos compartilhados pelos mecanismos Media3 e LibVLC.
 *
 * Este arquivo não inicializa reprodutores, não acessa a rede e não modifica
 * o banco Room. A interface, os adaptadores e a persistência serão conectados
 * em etapas posteriores.
 */

enum class PlaybackEngine {
    MEDIA3,
    VLC;

    fun alternate(): PlaybackEngine =
        if (this == MEDIA3) VLC else MEDIA3
}

enum class PlaybackContentType {
    MOVIE,
    EPISODE,
    LIVE_CHANNEL
}

enum class PlaybackStatus {
    IDLE,
    PREPARING,
    READY,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

enum class PlaybackHdrFormat(val displayName: String) {
    DOLBY_VISION("Dolby Vision"),
    HDR10_PLUS("HDR10+"),
    HDR10("HDR10"),
    HLG("HLG"),
    SDR("SDR"),
    UNKNOWN("Não identificado")
}

enum class PlaybackAspectMode {
    NORMAL,
    FULL_SCREEN,
    MANUAL,
    STRETCH
}

enum class PlaybackTrackType {
    VIDEO,
    AUDIO,
    SUBTITLE
}

enum class PlaybackFailureType {
    NETWORK,
    TIMEOUT,
    SOURCE_UNAVAILABLE,
    AUTHENTICATION,
    DECODER,
    UNSUPPORTED_FORMAT,
    DRM,
    UNKNOWN
}

enum class PlaybackResumeBehavior {
    ASK,
    CONTINUE,
    RESTART
}

/**
 * Referência a um conteúdo. Nunca registre streamUrl em logs: links Xtream
 * podem conter credenciais.
 */
data class PlaybackMedia(
    val id: String,
    val type: PlaybackContentType,
    val title: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val categoryId: String? = null,
    val durationMs: Long? = null,
    val seriesId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val mimeType: String? = null,
    // Identificadores do catálogo para obter legendas externas ao abrir o player.
    val tmdbId: String? = null,
    val subtitleSearchTitle: String? = null,
    val year: Int? = null
) {
    init {
        require(id.isNotBlank()) { "PlaybackMedia.id não pode ser vazio" }
        require(title.isNotBlank()) { "PlaybackMedia.title não pode ser vazio" }
        require(streamUrl.isNotBlank()) { "PlaybackMedia.streamUrl não pode ser vazio" }
        require(durationMs == null || durationMs >= 0L)
    }

    // Evita vazar URLs e credenciais nos logs por meio de toString().
    override fun toString(): String =
        "PlaybackMedia(id=$id, type=$type, title=$title, streamUrl=[REDACTED])"
}

/** Converte os modelos já existentes no catálogo do IPTVPremium. */
fun Movie.toPlaybackMedia(): PlaybackMedia = PlaybackMedia(
    id = id,
    type = PlaybackContentType.MOVIE,
    title = name,
    streamUrl = streamUrl,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    categoryId = categoryId,
    durationMs = durationSeconds?.takeIf { it >= 0L }?.times(1_000L),
    year = year
)

fun Episode.toPlaybackMedia(): PlaybackMedia = PlaybackMedia(
    id = id,
    type = PlaybackContentType.EPISODE,
    title = name,
    streamUrl = streamUrl,
    posterUrl = posterUrl,
    durationMs = durationSeconds?.takeIf { it >= 0L }?.times(1_000L),
    seriesId = seriesId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber
)

fun Channel.toPlaybackMedia(): PlaybackMedia = PlaybackMedia(
    id = id,
    type = PlaybackContentType.LIVE_CHANNEL,
    title = name,
    streamUrl = streamUrl,
    posterUrl = logoUrl,
    categoryId = categoryId
)

/**
 * Faixa real identificada pelo mecanismo de reprodução.
 * Os identificadores podem mudar ao trocar de Media3 para VLC.
 */
data class PlaybackTrack(
    val id: String,
    val type: PlaybackTrackType,
    val label: String,
    val languageTag: String? = null,
    val codec: String? = null,
    val isDefault: Boolean = false
)

/**
 * Uma qualidade/variante anunciada pela fonte.
 * Uma qualidade só deve aparecer no menu se for detectada de verdade.
 */
data class PlaybackVideoVariant(
    val id: String,
    val label: String,
    val width: Int? = null,
    val height: Int? = null,
    val bitrateBitsPerSecond: Long? = null,
    val hdrFormat: PlaybackHdrFormat = PlaybackHdrFormat.UNKNOWN
)

/**
 * Transformação visual. zoom escala os dois eixos; widthScale e heightScale
 * permitem o ajuste independente aprovado pelo usuário.
 * offsetX/offsetY representam deslocamentos relativos à área de vídeo.
 */
data class PlaybackManualTransform(
    val zoom: Float = 1f,
    val widthScale: Float = 1f,
    val heightScale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    init {
        require(zoom in 0.25f..4f)
        require(widthScale in 0.25f..4f)
        require(heightScale in 0.25f..4f)
        require(offsetX in -1f..1f)
        require(offsetY in -1f..1f)
    }
}

data class PlaybackVideoLayout(
    val mode: PlaybackAspectMode = PlaybackAspectMode.NORMAL,
    val manual: PlaybackManualTransform = PlaybackManualTransform()
)

/** Cores ARGB. Os adaptadores de legenda aplicarão apenas opções suportadas. */
data class PlaybackSubtitleStyle(
    val sizeScale: Float = 1f,
    val textColorArgb: Int = -0x1,
    val backgroundColorArgb: Int = 0x99000000.toInt(),
    val bottomOffsetPercent: Int = 8,
    val synchronizationOffsetMs: Long = 0L
) {
    init {
        require(sizeScale in 0.5f..3f)
        require(bottomOffsetPercent in 0..45)
        require(synchronizationOffsetMs in -30_000L..30_000L)
    }
}

/**
 * Preferências de interface e reprodução; a gravação no DataStore será
 * implementada depois. Qualidade null = faixa original/adaptativa da fonte.
 */
data class PlaybackSettings(
    val preferredEngine: PlaybackEngine = PlaybackEngine.MEDIA3,
    val selectedVideoVariantId: String? = null,
    val audioLanguageTag: String? = null,
    val subtitleLanguageTag: String? = null,
    val subtitlesEnabled: Boolean = false,
    val controlsTimeoutSeconds: Int = 5,
    val skipIntervalSeconds: Int = 10,
    val speed: Float = 1f,
    val resumeBehavior: PlaybackResumeBehavior = PlaybackResumeBehavior.ASK,
    val nextEpisodeCountdownSeconds: Int = 10,
    val autoPlayNextEpisode: Boolean = true,
    val aspect: PlaybackVideoLayout = PlaybackVideoLayout(),
    val subtitleStyle: PlaybackSubtitleStyle = PlaybackSubtitleStyle()
) {
    init {
        require(controlsTimeoutSeconds in setOf(3, 5, 8, 10))
        require(skipIntervalSeconds > 0)
        require(speed in setOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f))
        require(nextEpisodeCountdownSeconds in setOf(5, 10, 15, 20))
    }
}

/**
 * Métricas opcionais: null significa que o mecanismo não informou o dado.
 * Não transformar métricas ausentes em rótulos inventados.
 */
data class PlaybackTechnicalInfo(
    val width: Int? = null,
    val height: Int? = null,
    val framesPerSecond: Float? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val hdrFormat: PlaybackHdrFormat = PlaybackHdrFormat.UNKNOWN,
    val bitrateBitsPerSecond: Long? = null,
    val bufferedDurationMs: Long? = null,
    val usingHardwareDecoder: Boolean? = null
)

data class PlaybackFailure(
    val type: PlaybackFailureType,
    val message: String,
    val isRecoverable: Boolean,
    val errorCode: String? = null
)

/**
 * Limites para evitar o loop Media3 -> VLC -> Media3.
 * A lógica de tentativas será feita pelo gerenciador, não por este modelo.
 */
data class PlaybackRecoveryPolicy(
    val maxReconnectAttempts: Int = 2,
    val maxAutomaticEngineSwitches: Int = 1
) {
    init {
        require(maxReconnectAttempts in 0..5)
        require(maxAutomaticEngineSwitches in 0..1)
    }

    fun canTryAlternateEngine(type: PlaybackFailureType): Boolean = when (type) {
        PlaybackFailureType.DRM -> false
        else -> true
    }
}

/** Estado único exibido pela interface, independente do motor utilizado. */
data class PlaybackUiState(
    val media: PlaybackMedia? = null,
    val engine: PlaybackEngine = PlaybackEngine.MEDIA3,
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val bufferedPositionMs: Long? = null,
    val isLive: Boolean = false,
    val availableTracks: List<PlaybackTrack> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    // O Media3 fornece Cue (texto ou imagem) para a SubtitleView na camada visual.
    // No LibVLC, as legendas são desenhadas pelo próprio motor na Surface.
    val subtitleCues: List<Cue> = emptyList(),
    val availableVideoVariants: List<PlaybackVideoVariant> = emptyList(),
    val liveWindow: PlaybackLiveWindow = PlaybackLiveWindow(),
    val currentEpgProgram: PlaybackEpgProgram? = null,
    val nextEpgProgram: PlaybackEpgProgram? = null,
    val technicalInfo: PlaybackTechnicalInfo = PlaybackTechnicalInfo(),
    val failure: PlaybackFailure? = null,
    val automaticEngineSwitchCount: Int = 0
) {
    val isBuffering: Boolean
        get() = status == PlaybackStatus.BUFFERING || status == PlaybackStatus.PREPARING

    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING

    val canSeek: Boolean
        get() = if (isLive) {
            liveWindow.canSeekBack
        } else {
            durationMs != null && durationMs > 0L
        }

    val progressFraction: Float
        get() {
            val duration = durationMs?.takeIf { it > 0L } ?: return 0f
            return (positionMs.toDouble() / duration.toDouble())
                .coerceIn(0.0, 1.0)
                .toFloat()
        }
}

/** Programação EPG, quando fornecida pela fonte. */
data class PlaybackEpgProgram(
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val description: String? = null
) {
    init {
        require(endEpochMs > startEpochMs)
    }
}

/** Timeshift apenas quando o player/servidor anunciar janela navegável. */
data class PlaybackLiveWindow(
    val supportsTimeshift: Boolean = false,
    val seekableStartMs: Long? = null,
    val liveEdgeMs: Long? = null,
    val isAtLiveEdge: Boolean = true
) {
    val canSeekBack: Boolean
        get() = supportsTimeshift &&
                seekableStartMs != null &&
                liveEdgeMs != null &&
                liveEdgeMs > seekableStartMs
}


// IPTV_AUTOMATIC_DOLBY_AND_EPISODE_CONTROLS_USER26_V2
