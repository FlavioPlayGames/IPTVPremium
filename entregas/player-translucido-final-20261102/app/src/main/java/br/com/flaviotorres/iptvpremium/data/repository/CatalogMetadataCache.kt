package br.com.flaviotorres.iptvpremium.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import br.com.flaviotorres.iptvpremium.data.cache.PosterRequestLocks
import org.json.JSONObject

/** Shared public metadata only. Provider IDs, stream URLs and episodes never enter this cache. */
internal object CatalogMetadataCache {
    private data class Entry(val json: String?, val created: Long)
    private val entries = LinkedHashMap<String, Entry>(32, .75f, true)
    private val flights = PosterRequestLocks()
    private const val MAX_BYTES = 8L * 1024 * 1024
    private const val TTL = 6L * 60 * 60 * 1_000_000_000
    private const val MISS_TTL = 60L * 1_000_000_000

    private fun entry(key: String): Entry? = synchronized(entries) {
        entries[key]?.takeIf { System.nanoTime() - it.created in 0..if (it.json == null) MISS_TTL else TTL }
            ?: run { entries.remove(key); null }
    }
    fun peek(key: String?): JSONObject? = key?.let { entry(it)?.json?.let(::JSONObject) }

    /** A repository may reuse an existing work before asking another provider edition for info. */
    suspend fun cached(key: String?): JSONObject? = withContext(Dispatchers.IO) {
        if (key == null) null else peek(key) ?: CatalogMetadataDiskCache.read(key)
    }

    suspend fun load(key: String?, loader: suspend () -> JSONObject?): JSONObject? = withContext(Dispatchers.IO) {
        if (key == null) return@withContext loader()
        entry(key)?.let { return@withContext it.json?.let(::JSONObject) }
        flights.withLock(key) {
                entry(key)?.let { return@withLock it.json?.let(::JSONObject) }
                // Cancellation is never a cached failure. A waiting foreground request can retry.
                val disk = CatalogMetadataDiskCache.read(key)
                val result = disk ?: loader()
                val json = result?.toString()
                synchronized(entries) {
                    if (json == null || json.length * 2L <= MAX_BYTES) entries[key] = Entry(json, System.nanoTime())
                    var bytes = entries.values.sumOf { (it.json?.length ?: 0) * 2L }
                    while (entries.size > 128 || bytes > MAX_BYTES) {
                        val iterator = entries.entries.iterator()
                        if (!iterator.hasNext()) break
                        bytes -= (iterator.next().value.json?.length ?: 0) * 2L
                        iterator.remove()
                    }
                }
                if (result != null && disk == null) CatalogMetadataDiskCache.write(key, result)
                result
        }
    }
}
