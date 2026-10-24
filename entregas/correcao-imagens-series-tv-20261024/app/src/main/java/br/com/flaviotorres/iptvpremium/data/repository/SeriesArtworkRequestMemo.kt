package br.com.flaviotorres.iptvpremium.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Owned by one route; no global scope, detached work, disk or bitmap retention. */
internal class SeriesArtworkRequestMemo(private val capacity: Int = 24) {
    private val lock = Mutex()
    private val mutableRevision = MutableStateFlow(0L)
    val revision = mutableRevision.asStateFlow()
    private val results = object : LinkedHashMap<Any, Any>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Any, Any>?) = size > capacity
    }
    init { require(capacity > 0) }
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> peek(key: Any): T? = synchronized(results) { results[key] as T? }

    suspend fun <T : Any> load(key: Any, request: suspend () -> T?): T? = lock.withLock {
        peek<T>(key) ?: request()?.also { value -> synchronized(results) { results[key] = value }; mutableRevision.update { it + 1 } }
    }
}
