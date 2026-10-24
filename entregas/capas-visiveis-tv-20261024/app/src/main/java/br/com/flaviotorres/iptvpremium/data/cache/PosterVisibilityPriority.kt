package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Restricts grid work to the current viewport; no owned/detached download scope. */
internal class PosterVisibilityPriority {
    private val views = MutableStateFlow<Map<PosterKind, Set<String>>>(emptyMap())
    fun update(kind: PosterKind, urls: List<String?>) {
        val visible = urls.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.toSet()
        views.update { it + (kind to visible) }
    }
    fun release(kind: PosterKind) { views.update { it - kind } }
    suspend fun <T> run(kind: PosterKind, url: String, load: suspend () -> T): T = channelFlow {
        views.map { it[kind]?.contains(url) ?: true }.distinctUntilChanged().collectLatest { allowed ->
            if (allowed) send(load())
        }
    }.first()
}

/** Disconnect blocking HTTP on coroutine cancellation, including a category/viewport change. */
internal fun CoroutineScope.interruptPosterDownload(disconnect: () -> Unit): Job =
    launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() }
        finally { runCatching(disconnect) }
    }
