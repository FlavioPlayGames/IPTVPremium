package br.com.flaviotorres.iptvpremium.core.startup

/** Physical window measurements, never a guessed TV resolution or a saved screen size. */
internal data class StartupWindowLayout(
    val width: Int,
    val height: Int,
    val insetsKnown: Boolean,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
)

/** One opening only: release after two matching, usable layouts, then stay released. */
internal class StartupWindowLayoutGate {
    private var previous: StartupWindowLayout? = null
    private var released = false

    fun ready(layout: StartupWindowLayout): Boolean {
        if (released) return true
        if (layout.width <= 0 || layout.height <= 0) {
            previous = null
            return false
        }
        released = previous == layout
        previous = layout
        return released
    }
}
