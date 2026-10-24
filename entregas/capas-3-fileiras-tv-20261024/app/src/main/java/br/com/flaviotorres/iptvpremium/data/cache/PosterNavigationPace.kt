package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One timer per category. Only rapid steps extend the pause; slower navigation can resume it. */
internal class PosterNavigationPace(
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private val state = MutableStateFlow(false)
    val rapid = state.asStateFlow()
    private var previous: Long? = null
    private var shortSteps = 0
    private var resumeAt = 0L
    private var timer: Job? = null

    @Synchronized fun activity() {
        val now = nowMillis()
        val last = previous
        shortSteps = if (last != null && now - last in 0L..FAST_STEP_MILLIS) shortSteps + 1 else 0
        previous = now
        if (shortSteps < FAST_STREAK) return
        state.value = true
        resumeAt = now + NORMAL_SETTLE_MILLIS
        if (timer?.isActive == true) return
        timer = scope.launch(start = CoroutineStart.LAZY) {
            while (isActive) {
                val left = synchronized(this@PosterNavigationPace) {
                    val remaining = resumeAt - nowMillis()
                    if (remaining <= 0) {
                        state.value = false
                        timer = null
                        return@launch
                    }
                    remaining
                }
                delay(left)
            }
        }
        timer?.start()
    }

    companion object {
        const val FAST_STEP_MILLIS = 180L
        const val FAST_STREAK = 3
        const val NORMAL_SETTLE_MILLIS = 900L
    }
}
