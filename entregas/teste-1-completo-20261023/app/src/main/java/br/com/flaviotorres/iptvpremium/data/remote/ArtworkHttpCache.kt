package br.com.flaviotorres.iptvpremium.data.remote

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Fixed public endpoints, bounded JSON and expiring cache under cacheDir. No playlist credentials. */
internal class ArtworkHttpCache(context: Context) {
    private val budget = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
    private val directory = File(context.applicationContext.cacheDir, "series_public_artwork_v1")
    private val locks = Array(16) { Mutex() }
    private val absentUntil = java.util.concurrent.ConcurrentHashMap<String, Long>()
    suspend fun get(service: String, path: String, query: Map<String, String>): String? {
        val host = if (service == "tvmaze") "https://api.tvmaze.com" else return null
        require(path.matches(Regex("/[A-Za-z0-9/_-]+")))
        val values = query
        val address = host + path + if (values.isEmpty()) "" else "?" + values.toSortedMap().entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
        val key = MessageDigest.getInstance("SHA-256").digest(address.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        return locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            withContext(Dispatchers.IO) {
                val file = File(directory, "$key.json")
                val cached = budget.retain(file).use { runCatching {
                    val age = System.currentTimeMillis() - file.lastModified()
                    if (file.isFile && file.length() <= MAX_BYTES && age in 0..21_600_000L) file.readText().takeIf { validJson(it) } else null
                }.getOrNull() }
                if (cached != null) return@withContext cached
                if ((absentUntil[key] ?: 0L) > android.os.SystemClock.elapsedRealtime()) return@withContext null
                try {
                    val body=DefaultPlaylistHttpClient(maxTextChars=MAX_BYTES).get(address)
                    check(validJson(body)){"JSON de arte inválido."}
                    directory.mkdirs()
                    val data=body.toByteArray(Charsets.UTF_8)
                    budget.reserve(file,data.size.toLong())?.use{lease->
                        lease.temporary.writeBytes(data);currentCoroutineContext().ensureActive();check(lease.temporary.renameTo(file))
                    }
                    body
                }catch(cancelled:CancellationException){throw cancelled}
                catch(_:Exception){null}

            }
        }
    }
    companion object {
        private fun validJson(body: String): Boolean = runCatching {
            val parser = org.json.JSONTokener(body)
            val value = parser.nextValue()
            (value is org.json.JSONObject || value is org.json.JSONArray) && parser.nextClean() == '\u0000'
        }.getOrDefault(false)
        private const val MAX_BYTES = 4 * 1024 * 1024
        @Volatile private var instance: ArtworkHttpCache? = null
        fun getInstance(context: Context): ArtworkHttpCache = instance ?: synchronized(this) {
            instance ?: ArtworkHttpCache(context).also { instance = it }
        }
    }
}
