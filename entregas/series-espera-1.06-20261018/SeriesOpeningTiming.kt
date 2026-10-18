package br.com.flaviotorres.iptvpremium.ui.series

import kotlinx.coroutines.delay

internal const val SERIES_OPENING_TRANSITION_MS = 2_000L
internal const val SERIES_OPENING_MAX_ARTWORK_MS = 5_000L

/** Artwork cannot extend the existing two-second transition beyond a five-second window. */
internal suspend fun awaitSeriesArtworkWindow(
    openingStartedMs: Long,
    ready: () -> Boolean,
    failed: () -> Boolean = { false },
    clock: () -> Long = { System.nanoTime() / 1_000_000L }
): Boolean {
    val latestTransitionStart = openingStartedMs + SERIES_OPENING_MAX_ARTWORK_MS - SERIES_OPENING_TRANSITION_MS
    while (!ready() && !failed()) {
        val remaining = latestTransitionStart - clock()
        if (remaining <= 0L) return false
        delay(minOf(32L, remaining))
    }
    return ready() && !failed()
}
