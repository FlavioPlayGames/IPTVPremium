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
                val started = System.nanoTime()
                val connection = URL(address).openConnection() as HttpURLConnection
                coroutineScope {
                    val closer = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                        try { awaitCancellation() } finally { connection.disconnect() }
                    }
                    try {
                        connection.connectTimeout = 3_000; connection.readTimeout = 4_000
                        connection.setRequestProperty("Accept", "application/json")
                        connection.setRequestProperty("User-Agent", "IPTVPremium/1.06")
                        val status = connection.responseCode
                        Log.d("SeriesArtwork", "fonte=$service http=$status tempoMs=${(System.nanoTime()-started)/1_000_000}")
                        if (status == 404) {
                            if (absentUntil.size >= 128) absentUntil.clear()
                            absentUntil[key] = android.os.SystemClock.elapsedRealtime() + 300_000L
                        }
                        if (status !in 200..299) return@coroutineScope null
                        val body = connection.inputStream.use { stream ->
                            val buffer = ByteArray(8192); val out = java.io.ByteArrayOutputStream()
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = stream.read(buffer); if (read < 0) break
                                check(out.size() + read <= MAX_BYTES) { "Arte excede limite" }; out.write(buffer, 0, read)
                            }
                            out.toString("UTF-8")
                        }
                        currentCoroutineContext().ensureActive()
                        // Reject malformed JSON before caching, including HTML with status 200.
                        check(validJson(body)) { "JSON de arte invalido" }
                        runCatching {
                            directory.mkdirs()
                            val data = body.toByteArray(Charsets.UTF_8)
                            val reservation = budget.reserve(file, data.size.toLong())
                            if (reservation != null) reservation.use {
                                it.temporary.writeBytes(data)
                                check(it.temporary.renameTo(file))
                            }
                        }
                        body
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) {
                        currentCoroutineContext().ensureActive()
                        Log.d("SeriesArtwork", "fonte=$service falha=${error.javaClass.simpleName}"); null
                    } finally { closer.cancel(); connection.disconnect() }
                }
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
