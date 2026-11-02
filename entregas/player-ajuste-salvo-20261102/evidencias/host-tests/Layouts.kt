package br.com.flaviotorres.iptvpremium.core.player
enum class PlaybackAspectMode {
    NORMAL,
    FULL_SCREEN,
    MANUAL,
    STRETCH
}
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

