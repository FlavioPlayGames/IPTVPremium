package br.com.flaviotorres.iptvpremium.core.player
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

