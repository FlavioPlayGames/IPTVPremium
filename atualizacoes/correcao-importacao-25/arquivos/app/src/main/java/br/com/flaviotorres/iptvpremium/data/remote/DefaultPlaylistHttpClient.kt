package br.com.flaviotorres.iptvpremium.data.remote

import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineName
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Cliente HTTP do catálogo IPTVPremium.
 *
 * Diagnóstico seguro de conexão:
 *  - código HTTP e MIME retornado;
 *  - tipo geral da resposta (JSON, HTML, M3U, vazia, outro);
 *  - tempo, tamanho e classe de erro;
 *  - tipo do endpoint, SEM registrar a URL completa ou as credenciais.
 *
 * IMPORTANTE: não habilitar logs do corpo HTTP nem imprimir URLs contendo
 * usuário/senha (Xtream costuma carregar credenciais nos query params).
 *
 * O contrato PlaylistHttpClient permanece inalterado.
 */
class PlaylistAccessDeniedException(message: String) : java.io.IOException(message)

class DefaultPlaylistHttpClient : PlaylistHttpClient {

    override suspend fun <T> withReader(url: String, consume: suspend (java.io.Reader) -> T): T =
        readImport(url, 10_000, 15_000, consume)

    private suspend fun <T> readImport(
        url: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        consume: suspend (java.io.Reader) -> T
    ): T = withContext(Dispatchers.IO) { coroutineScope {
            val endpoint = catalogTraceEndpoint(url)
            val started = System.nanoTime()
            val connection = URL(url).openConnection() as HttpURLConnection
            val disconnectOnCancel = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { connection.disconnect() }
            }
            var status = -1
            var headersMs = -1L
            var readerReadyMs = -1L
            var responseBody: PlaylistResponseBody? = null
            try {
                currentCoroutineContext().ensureActive()
                configure(connection)
                connection.connectTimeout = connectTimeoutMs
                connection.readTimeout = readTimeoutMs
                status = connection.responseCode
                headersMs = elapsedMillis(started)
                Log.i(TAG, "READER_HEADERS endpoint=$endpoint http=$status ateCabecalhosMs=$headersMs")
                if (status == 401 || status == 403) throw PlaylistAccessDeniedException("Acesso negado pelo provedor. Confira a lista e suas credenciais.")
                check(status in 200..299) { "Erro HTTP $status ao acessar a lista." }
                PlaylistResponseBody(connection).also { responseBody = it }.stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    readerReadyMs = elapsedMillis(started)
                    consume(reader)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "READER endpoint=$endpoint http=$status erro=${error.javaClass.simpleName}")
                throw error
            } finally {
                disconnectOnCancel.cancel()
                connection.disconnect()
                responseBody?.let { Log.d(TAG, "COMPACTACAO endpoint=$endpoint ${it.statistics()}") }
                Log.i(TAG, "READER endpoint=$endpoint http=$status ateCabecalhosMs=$headersMs leitorProntoMs=$readerReadyMs duracaoMs=${elapsedMillis(started)}")
            }
        } }

    // Only fixed action names may enter logs; never include host, credentials or arbitrary query values.
    private fun catalogTraceEndpoint(rawUrl: String): String {
        val allowed = setOf("get_vod_categories", "get_vod_streams", "get_series_categories",
            "get_series", "get_live_categories", "get_live_streams")
        val action = runCatching {
            java.net.URI(rawUrl).rawQuery.orEmpty().split('&')
                .firstOrNull { it.startsWith("action=") }?.substringAfter('=')
        }.getOrNull()
        return action?.takeIf { it in allowed } ?: endpointKind(rawUrl)
    }

    override suspend fun get(url: String): String = withContext(Dispatchers.IO) { coroutineScope {
        val trace = currentCoroutineContext()[CoroutineName]?.name?.takeIf { it.matches(Regex("serie-[0-9a-f]{8}")) }
        val endpoint = endpointKind(url) + (trace?.let { " trace=$it" } ?: "")
        val startedNs = System.nanoTime()
        val connection = URL(url).openConnection() as HttpURLConnection
        var status = -1
        var responseBody: PlaylistResponseBody? = null
        var phase = "CONNECT"
        // A perda de foco deve liberar a conexao, sem esperar o readTimeout.
        val disconnectOnCancel = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { connection.disconnect() }
        }

        try {
            currentCoroutineContext().ensureActive()
            configure(connection)
            status = connection.responseCode
            phase = "STATUS"

            if (status == 401 || status == 403) throw PlaylistAccessDeniedException("Acesso negado pelo provedor. Confira a lista e suas credenciais.")
            if (status !in 200..299) {
                logResult(
                    endpoint = endpoint,
                    status = status,
                    mime = safeMime(connection.contentType),
                    bodyKind = "HTTP_ERROR",
                    bodyLength = 0,
                    durationMillis = elapsedMillis(startedNs)
                )
                throw IllegalStateException(
                    "Erro HTTP $status ao acessar a lista."
                )
            }

            phase = "READ"
            val response = PlaylistResponseBody(connection).also { responseBody = it }.stream
                .bufferedReader(Charsets.UTF_8)
                .use { reader ->
                    val result = StringBuilder()
                    val buffer = CharArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = reader.read(buffer)
                        if (read < 0) break
                        result.append(buffer, 0, read)
                    }
                    currentCoroutineContext().ensureActive()
                    result.toString()
                }

            logResult(
                endpoint = endpoint,
                status = status,
                mime = safeMime(connection.contentType),
                bodyKind = bodyKind(response),
                bodyLength = response.length,
                durationMillis = elapsedMillis(startedNs)
            )

            response
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive() // Socket fechado pelo cancelamento nao e erro da lista.
            if (status in 200..299 || status == -1) {
                Log.w(
                    TAG,
                    "GET etapa=$phase endpoint=$endpoint " +
                            "http=${if (status == -1) "SEM_RESPOSTA" else status} " +
                            "erro=${error.javaClass.simpleName} " +
                            "duracaoMs=${elapsedMillis(startedNs)}"
                )
            }
            // Não transformamos/capturamos CancellationException: a coroutine
            // que foi cancelada precisa ser encerrada normalmente.
            throw error
        } finally {
            disconnectOnCancel.cancel()
            connection.disconnect()
            responseBody?.let { Log.d(TAG, "COMPACTACAO endpoint=$endpoint ${it.statistics()}") }
        }
    } }

    override suspend fun readLines(
        url: String,
        onLine: suspend (String) -> Unit
    ) {
        val endpoint = endpointKind(url)
        val started = System.nanoTime()
        var lines = 0L
        var lastProgress = started
        // Use the import reader's cancellation/connection cleanup, gzip handling and header diagnostics.
        // get() and the movie/series detail request paths are deliberately untouched.
        readImport(url, 15_000, 30_000) { source ->
            val reader = if (source is java.io.BufferedReader) source else source.buffered()
            while (true) {
                currentCoroutineContext().ensureActive()
                val line = reader.readLine() ?: break
                lines++
                onLine(line)
                if (lines == 1L || lines % 512L == 0L) {
                    val now = System.nanoTime()
                    if (lines == 1L || now - lastProgress >= 5_000_000_000L) {
                        Log.i(TAG, "STREAM_PROGRESS endpoint=$endpoint linhas=$lines duracaoMs=${elapsedMillis(started)}")
                        lastProgress = now
                    }
                }
            }
        }
        Log.i(TAG, "STREAM endpoint=$endpoint linhas=$lines duracaoMs=${elapsedMillis(started)}")
    }

    private fun configure(connection: HttpURLConnection) {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.useCaches = false
        connection.setRequestProperty("Accept", "*/*")
        // Explicit negotiation disables Android's implicit decoding, allowing safe
        // wire-byte measurement and exactly one streaming decompression below.
        connection.setRequestProperty("Accept-Encoding", "gzip")
        connection.setRequestProperty("User-Agent", "IPTVPremium/1.0")
    }

    private fun logResult(
        endpoint: String,
        status: Int,
        mime: String,
        bodyKind: String,
        bodyLength: Int,
        durationMillis: Long
    ) {
        val message =
            "GET endpoint=$endpoint http=$status mime=$mime " +
                    "resposta=$bodyKind caracteres=$bodyLength " +
                    "duracaoMs=$durationMillis"

        if (status !in 200..299 || bodyKind == "HTML" || bodyKind == "EMPTY") {
            Log.w(TAG, message)
        } else if (endpoint == "XTREAM_AUTH") {
            Log.i(TAG, message)
        } else {
            Log.d(TAG, message)
        }
    }

    /** Devolve um rótulo fechado: nunca URL, path ou query fornecidos pelo servidor. */
    private fun endpointKind(rawUrl: String): String = try {
        val url = URL(rawUrl)
        val path = url.path.lowercase(Locale.ROOT)
        when {
            path.endsWith("/player_api.php") || path == "/player_api.php" -> {
                val action = url.query.orEmpty().split('&').firstOrNull { it.startsWith("action=", true) }?.substringAfter('=')
                when (action?.lowercase(Locale.ROOT)) {
                    "get_series_info" -> "XTREAM_SERIES_INFO"
                    "get_vod_info" -> "XTREAM_VOD_INFO"
                    null -> "XTREAM_AUTH"
                    else -> "XTREAM_DATA"
                }
            }
            path.endsWith("/get.php") || path == "/get.php" -> "M3U_GET"
            else -> "OTHER"
        }
    } catch (_: Exception) {
        "UNKNOWN"
    }

    private fun bodyKind(raw: String): String {
        val value = raw.trimStart { it.isWhitespace() || it == '\uFEFF' }
        if (value.isEmpty()) return "EMPTY"
        return when {
            value.startsWith("{") -> "JSON_OBJECT_CANDIDATE"
            value.startsWith("[") -> "JSON_ARRAY_CANDIDATE"
            value.startsWith("#EXTM3U", ignoreCase = true) -> "M3U"
            value.startsWith("<!DOCTYPE", ignoreCase = true) ||
                    value.startsWith("<html", ignoreCase = true) ||
                    value.startsWith("<head", ignoreCase = true) -> "HTML"
            value.startsWith("<?xml", ignoreCase = true) -> "XML"
            value.startsWith("<") -> "MARKUP_OR_HTML"
            else -> "OTHER_TEXT"
        }
    }

    private fun safeMime(contentType: String?): String {
        val mime = contentType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        val safe = mime.filter { char ->
            char in 'a'..'z' || char in '0'..'9' ||
                    char == '/' || char == '.' || char == '+' || char == '-'
        }
        return safe.take(60).ifBlank { "DESCONHECIDO" }
    }

    private fun elapsedMillis(startedNs: Long): Long =
        (System.nanoTime() - startedNs) / 1_000_000L

    companion object {
        private const val TAG = "PlaylistConnection"
    }
}
