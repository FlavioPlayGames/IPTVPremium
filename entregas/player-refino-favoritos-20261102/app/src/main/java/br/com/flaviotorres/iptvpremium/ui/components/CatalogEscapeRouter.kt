package br.com.flaviotorres.iptvpremium.ui.components

/** One action per physical ESC press, including a consumed key-up after focus moves. */
internal class CatalogEscapeRouter {
    private val handlers = mutableListOf<() -> Boolean>()
    private var pressed = false
    fun register(handler: () -> Boolean): () -> Unit {
        handlers.add(handler)
        // A screen may disappear on key-down; still consume the matching key-up.
        return { handlers.remove(handler) }
    }
    fun down(repeated: Boolean): Boolean {
        if (pressed && repeated) return true
        // Some TV remotes omit key-up. A fresh down is still a new press.
        if (pressed) pressed = false
        if (repeated) return false
        val handled = handlers.toList().asReversed().any { it() }
        pressed = handled
        return handled
    }
    fun up(): Boolean = pressed.also { pressed = false }
    fun reset() { pressed = false }
}

internal val CatalogHardwareEscape = CatalogEscapeRouter()

/** Android remote BACK is handled before a focused view can consume it. */
internal val CatalogHardwareBack = CatalogEscapeRouter()

internal fun dispatchCatalogBackKey(event: android.view.KeyEvent): Boolean {
    if (event.keyCode != android.view.KeyEvent.KEYCODE_BACK) return false
    return when (event.action) {
        android.view.KeyEvent.ACTION_DOWN -> CatalogHardwareBack.down(event.repeatCount > 0)
        android.view.KeyEvent.ACTION_UP -> CatalogHardwareBack.up()
        else -> false
    }
}
