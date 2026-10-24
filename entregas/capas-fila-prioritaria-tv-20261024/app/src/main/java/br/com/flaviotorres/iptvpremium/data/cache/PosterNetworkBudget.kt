package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Visible requests precede anticipation. Recent visible work wins, with bounded overtaking. */
internal class PosterNetworkBudget(
    private val capacity: Int = TOTAL_REQUESTS,
    private val backgroundCapacity: Int = BACKGROUND_REQUESTS,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private class Waiter(val background: Boolean, val queuedAt: Long) {
        val ready = CompletableDeferred<Unit>()
        var granted = false
    }
    private val lock = Any()
    private val visible = java.util.ArrayDeque<Waiter>()
    private val background = java.util.ArrayDeque<Waiter>()
    private var active = 0
    private var activeBackground = 0
    init { require(capacity > 0 && backgroundCapacity in 0..capacity) }

    suspend fun <T> run(background: Boolean, operation: suspend () -> T): T {
        val waiter = Waiter(background, nowMillis())
        val granted = synchronized(lock) {
            (if (background) this.background else visible).addLast(waiter)
            dispatchLocked()
        }
        // Resume outside the monitor: an undispatched caller may immediately do blocking IO.
        granted.forEach { it.ready.complete(Unit) }
        try {
            waiter.ready.await()
            currentCoroutineContext().ensureActive()
            return operation()
        } finally {
            val next = synchronized(lock) {
                visible.remove(waiter)
                this.background.remove(waiter)
                if (waiter.granted) {
                    active--
                    if (waiter.background) activeBackground--
                }
                dispatchLocked()
            }
            next.forEach { it.ready.complete(Unit) }
        }
    }

    private fun dispatchLocked(): List<Waiter> {
        val granted = mutableListOf<Waiter>()
        while (active < capacity) {
            val next = when {
                visible.isNotEmpty() -> {
                    // Keep an older still-visible cover from being postponed indefinitely.
                    if (nowMillis() - visible.first.queuedAt >= MAX_OVERTAKE_MILLIS)
                        visible.removeFirst() else visible.removeLast()
                }
                activeBackground < backgroundCapacity && background.isNotEmpty() -> background.removeFirst()
                else -> break
            }
            next.granted = true
            active++
            if (next.background) activeBackground++
            granted += next
        }
        return granted
    }

    companion object {
        const val TOTAL_REQUESTS = 6
        const val BACKGROUND_REQUESTS = 2
        const val MAX_OVERTAKE_MILLIS = 750L
    }
}
