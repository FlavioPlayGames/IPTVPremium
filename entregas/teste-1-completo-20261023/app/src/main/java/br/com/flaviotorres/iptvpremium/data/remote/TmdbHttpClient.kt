package br.com.flaviotorres.iptvpremium.data.remote

import android.util.Log
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TmdbHttpClient(
    private val readAccessToken: String,
    private val baseUrl: String = "https://api.themoviedb.org/3"
) {
    private data class Cached(val body: String, val created: Long)
    private val responseCache = LinkedHashMap<String, Cached>(16, 0.75f, true)
    private val requestLocks = Array(32) { Mutex() }
    /** Cancellation releases the UI immediately even on platforms whose disconnect blocks. */
    suspend fun get(path: String, queryParameters: Map<String, String> = emptyMap()): String {
        check(readAccessToken.isNotBlank()) { "TMDB_READ_TOKEN nao foi configurado." }
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val query = queryParameters.filterValues { it.isNotBlank() }.entries.joinToString("&") {
            encode(it.key) + "=" + encode(it.value)
        }
        val address = baseUrl + normalizedPath + if (query.isEmpty()) "" else "?$query"
        return requestLocks[(address.hashCode() and Int.MAX_VALUE) % requestLocks.size].withLock {
            val now = System.nanoTime()
            synchronized(responseCache) { responseCache[address] }
                ?.takeIf { now - it.created in 0..600_000_000_000L }
                ?.let { return@withLock it.body }
            val body = DefaultPlaylistHttpClient(clients={
                br.com.flaviotorres.iptvpremium.core.network.AppNetwork.client().newBuilder().addInterceptor {chain->
                    chain.proceed(chain.request().newBuilder().header("Authorization","Bearer $readAccessToken")
                        .header("Accept","application/json").build())
                }.build()
            },maxTextChars=8*1024*1024).get(address)
            synchronized(responseCache) {
                responseCache[address] = Cached(body, System.nanoTime())
                var bytes = responseCache.values.sumOf { it.body.length.toLong() * 2 }
                while (responseCache.size > 64 || bytes > 8L * 1024 * 1024) {
                    val entry = responseCache.entries.iterator()
                    if (!entry.hasNext()) break
                    bytes -= entry.next().value.body.length.toLong() * 2
                    entry.remove()
                }
            }
            body
        }
    }
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    companion object {
        private fun pool(size: Int, name: String) = ThreadPoolExecutor(size, size, 30L, TimeUnit.SECONDS,
            ArrayBlockingQueue<Runnable>(16), java.util.concurrent.ThreadFactory { action ->
                Thread(action, name).apply { isDaemon = true }
            }).apply { allowCoreThreadTimeOut(true) }
        private val requests = pool(2, "tmdb-request")
        private val cleanup = pool(1, "tmdb-cleanup")
    }
}
