package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Each card observes its own placement, independently of paging/history/category snapshots. */
internal suspend fun <T> loadWhilePosterVisible(visible: Flow<Boolean>, load: suspend () -> T): T = channelFlow {
    visible.distinctUntilChanged().collectLatest { placed ->
        if (placed) send(load())
    }
}.first()

/** Disconnect blocking HTTP on coroutine cancellation, including a category/viewport change. */
internal fun CoroutineScope.interruptPosterDownload(disconnect: () -> Unit): Job =
    launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() }
        finally { runCatching(disconnect) }
    }
