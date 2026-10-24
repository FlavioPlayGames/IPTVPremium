
package br.com.flaviotorres.iptvpremium.data.repository

import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.resolver.M3uXtreamResolver
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import android.util.Log
import kotlinx.coroutines.CoroutineName
import java.security.MessageDigest

/** Temporada realmente informada pelo provedor. */
data class SeriesSeasonInfo(
    val number: Int,
    val title: String,
    val coverUrl: String?,
    val episodeCount: Int
)

/** Episodio real do provedor: o ID nao e o numero do episodio. */
data class SeriesEpisodeInfo(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val thumbnailUrl: String?,
    val synopsis: String?,
    val durationSeconds: Int?,
    val containerExtension: String?
)

/** Dados reais de uma unica serie, carregados sob demanda. */
data class SeriesEpisodesResult(
    val seriesId: String,
    val title: String?,
    val plot: String?,
    val backdropUrl: String?,
    val posterUrl: String?,
    val trailerUrl: String?,
    val seasons: List<SeriesSeasonInfo>,
    val episodes: List<SeriesEpisodeInfo>
)

/**
 * Consulta apenas a serie aberta. Nunca baixa a lista de todas as series.
 *
 * - Xtream direto: get_series_info.
 * - M3U Xtream-compatible: mesmo endpoint, credenciais so em memoria.
 * - M3U generica: retorna null, pois nao ha endpoint estruturado.
 *
 * Nenhuma temporada ou episodio demonstrativo e criado.
 */
internal class SeriesEpisodesRepository(
    private val httpClient: PlaylistHttpClient,
    private val monotonicNanos: () -> Long = System::nanoTime,
    private val diskCache: SeriesEpisodeDiskCache? = null
) {
    suspend fun load(
        source: PlaylistSource,
        seriesId: String,
        generationId: String? = null
    ): SeriesEpisodesResult? {
        val xtream = when (source) {
            is PlaylistSource.XtreamCodes -> source
            is PlaylistSource.M3u -> M3uXtreamResolver.resolve(source)
        } ?: run { Log.d("SeriesDetails", "episodios motivo=origem_sem_api_estruturada"); return null }

        val id = seriesId.trim()
        if (id.isEmpty() || !id.all(Char::isDigit)) {
            Log.d("SeriesDetails", "episodios motivo=id_provedor_incompativel")
            return null
        }

        val server = xtream.serverUrl.trim().trimEnd('/')
        if (server.isEmpty()) return null

        val baseUrl = when {
            server.startsWith("https://", ignoreCase = true) -> server
            server.startsWith("http://", ignoreCase = true) -> server
            else -> "http://$server"
        }

        // A URL contem credenciais: nunca registra-la em logs ou mensagens.
        val url = "$baseUrl/player_api.php" +
                "?username=${encode(xtream.username)}" +
                "&password=${encode(xtream.password)}" +
                "&action=get_series_info" +
                "&series_id=${encode(id)}"

        val trace = "serie-" + MessageDigest.getInstance("SHA-256")
            .digest("${source.id}\u0000$id".toByteArray()).take(4).joinToString("") { "%02x".format(it.toInt() and 255) }
        return withContext(CoroutineName(trace)) {
            val key = generationId?.let { "${source.id}\u0000$it\u0000$url" }
            if (key != null && diskCache != null) {
                val started = monotonicNanos()
                val cached = withContext(Dispatchers.IO) { diskCache.read(key) }
                if (cached != null) {
                    withContext(Dispatchers.Default) { parseResponse(id, cached) }
                        ?.takeIf { it.episodes.isNotEmpty() }?.let {
                            Log.d("SeriesDetails", "$trace origem=disco episodios=${it.episodes.size} totalMs=${(monotonicNanos()-started)/1_000_000}")
                            return@withContext it
                        }
                }
            }
            loadWithRetry(url, id, trace)?.also { result ->
                if (key != null && diskCache != null && result.episodes.isNotEmpty()) {
                    withContext(Dispatchers.IO) { diskCache.write(key, encodeSeriesEpisodes(result)) }
                }
            }
        }
    }

    /** Recuperacao automatica limitada; timeout tambem e uma falha transitoria.
     * A tela continua em Loading durante as pausas. Nunca repete autorizacao
     * negada nem cancelamento. O orcamento limita o inicio de novas chamadas,
     * sem interromper uma resposta valida que ja esteja chegando.
     */
    private suspend fun loadWithRetry(
        url: String,
        seriesId: String,
        trace: String
    ): SeriesEpisodesResult? {
        var last: SeriesEpisodesResult? = null
        val recoveryStarted = monotonicNanos()
        var slowFailures = 0
        for (attempt in 1..MAX_ATTEMPTS) {
            val startedNs = monotonicNanos()
            var emptyResponse = false
            try {
                val response = httpClient.get(url)
                val networkMs = (monotonicNanos() - startedNs) / 1_000_000L
                val parseStarted = monotonicNanos()
                var shape = "desconhecido"
                val parsed = withContext(Dispatchers.Default) {
                    parseResponse(seriesId, response) { shape = it }
                }
                Log.d("SeriesDetails", "$trace tentativa=$attempt origem=rede redeMs=$networkMs processamentoMs=${(monotonicNanos()-parseStarted)/1_000_000} formato=$shape episodios=${parsed?.episodes?.size ?: 0}")
                if (parsed != null && parsed.episodes.isNotEmpty()) return parsed
                last = parsed ?: last
                emptyResponse = parsed != null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.d("SeriesDetails", "$trace tentativa=$attempt falha=${error.javaClass.simpleName} duracaoMs=${(monotonicNanos()-startedNs)/1_000_000}")
                if (!isTransient(error)) throw error
                if ((monotonicNanos() - startedNs) / 1_000_000L >= SLOW_FAILURE_MS) slowFailures++
                if (!canRetry(attempt, recoveryStarted, slowFailures)) throw error
                delay(RETRY_DELAYS_MS[attempt - 1])
                continue
            }
            if ((monotonicNanos() - startedNs) / 1_000_000L >= SLOW_FAILURE_MS) slowFailures++
            if (emptyResponse && attempt >= EMPTY_RESPONSE_ATTEMPTS) break
            if (!canRetry(attempt, recoveryStarted, slowFailures)) break
            delay(RETRY_DELAYS_MS[attempt - 1])
        }
        return last
    }

    private fun canRetry(attempt: Int, recoveryStarted: Long, slowFailures: Int): Boolean =
        attempt < MAX_ATTEMPTS && slowFailures < MAX_SLOW_FAILURES &&
            (monotonicNanos() - recoveryStarted) / 1_000_000L + RETRY_DELAYS_MS[attempt - 1] < RETRY_START_BUDGET_MS

    private fun isTransient(error: Exception): Boolean {
        if (error is IOException) return true
        if (error is IllegalStateException) {
            val status = Regex("Erro HTTP (\\d{3})")
                .find(error.message.orEmpty())
                ?.groupValues?.get(1)?.toIntOrNull()
            return status != null && (status == 408 || status == 429 || status in 500..599)
        }
        return false
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
        const val EMPTY_RESPONSE_ATTEMPTS = 2
        const val SLOW_FAILURE_MS = 10_000L
        const val MAX_SLOW_FAILURES = 2
        const val RETRY_START_BUDGET_MS = 45_000L
        val RETRY_DELAYS_MS = longArrayOf(400L)
    }

    private fun parseResponse(
        seriesId: String,
        response: String,
        reportShape: ((String) -> Unit)? = null
    ): SeriesEpisodesResult? {
        val json = response.trimStart { it.isWhitespace() || it == '\uFEFF' }
        var recoveredMetadata = false
        val root = try {
            JSONObject(json)
        } catch (error: org.json.JSONException) {
            reportShape?.invoke(seriesJsonFailureSummary(json, error))
            val recovered = recoverCompleteSeriesEpisodes(json) ?: return null
            recoveredMetadata = true
            recovered
        }
        reportShape?.invoke((if (recoveredMetadata) "metadados_descartados_" else "") + episodeShape(root.opt("episodes")))

        val info = root.optJSONObject("info")
        val entries = ArrayList<SeriesEpisodeInfo>()
        val episodesNode = root.opt("episodes")

        // PHP panels may encode numeric season/index keys as either JSON
        // objects or arrays. Normalize only the episode tree, never metadata.
        // Limits also bound malformed deeply nested responses.
        val maxEpisodeCount = 10_000
        val maxVisitedNodes = 30_000
        val maxDepth = 6
        var visitedNodes = 0

        fun episodeId(item: JSONObject): String? =
            sequenceOf("id", "episode_id").mapNotNull { key ->
                when (val raw = item.opt(key)) {
                    is String -> raw.trim()
                    is Number -> raw.toString()
                    else -> null
                }?.takeIf { value -> value.isNotEmpty() && !value.equals("null", true) }
            }.firstOrNull()

        fun isEpisode(item: JSONObject): Boolean =
            episodeId(item) != null &&
                !item.has("episodes") && !item.has("seasons") &&
                (!item.has("episode_count") || item.has("episode_num") || item.has("episode_number"))

        fun addEpisode(
            item: JSONObject,
            groupSeason: Int?,
            index: Int
        ) {
            val episodeId = episodeId(item) ?: return

            val seasonNumber =
                item.intValue(
                    "season",
                    "season_num",
                    "season_number"
                ) ?: groupSeason ?: 1

            if (seasonNumber < 0) return

            val episodeNumber =
                item.intValue(
                    "episode_num",
                    "episode_number",
                    "number"
                ) ?: (index + 1)

            if (episodeNumber <= 0) return

            val itemInfo = item.optJSONObject("info")

            val rawTitle = item.stringValue(
                "title",
                "name",
                "episode_name"
            )

            val name = rawTitle
                ?.replace(Regex("^\\s*\\d+\\s*[-–.]\\s*"), "")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "Episódio ${episodeNumber.toString().padStart(2, '0')}"

            val duration =
                itemInfo?.durationSeconds()
                    ?: item.durationSeconds()

            entries += SeriesEpisodeInfo(
                id = episodeId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                title = name,
                thumbnailUrl = itemInfo?.image(
                    "movie_image", "still_path", "episode_image", "thumbnail", "thumbnail_url", "image",
                    "cover_big", "cover", "stream_icon"
                ) ?: item.image(
                    "movie_image", "still_path", "episode_image", "thumbnail", "thumbnail_url", "image",
                    "cover_big", "cover", "stream_icon"
                ),
                synopsis = itemInfo?.stringValue(
                    "plot",
                    "overview",
                    "description"
                ) ?: item.stringValue(
                    "plot",
                    "overview",
                    "description"
                ),
                durationSeconds = duration,
                containerExtension =
                    item.stringValue("container_extension")
            )
        }

        fun visitEpisodes(node: Any?, groupSeason: Int?, index: Int, depth: Int) {
            if (depth > maxDepth || visitedNodes >= maxVisitedNodes || entries.size >= maxEpisodeCount) return
            visitedNodes++
            when (node) {
                is JSONArray -> {
                    for (position in 0 until minOf(node.length(), maxVisitedNodes - visitedNodes)) {
                        if (entries.size >= maxEpisodeCount || visitedNodes >= maxVisitedNodes) break
                        val child = node.opt(position)
                        // At the root, a nested array is a season bucket. A flat
                        // array remains a list of episodes (default season 1).
                        val season = groupSeason ?: position.takeIf {
                            depth == 0 && (child is JSONArray || child is JSONObject && !isEpisode(child))
                        }
                        visitEpisodes(child, season, position, depth + 1)
                    }
                }
                is JSONObject -> {
                    if (isEpisode(node)) {
                        addEpisode(node, groupSeason, index)
                        return
                    }
                    val wrappedEpisodes = node.opt("episodes")
                    if (depth > 0 && (wrappedEpisodes is JSONObject || wrappedEpisodes is JSONArray)) {
                        visitEpisodes(wrappedEpisodes, node.intValue("season", "season_num", "season_number") ?: groupSeason, 0, depth + 1)
                        return
                    }
                    val keys = node.keys()
                    var position = 0
                    while (keys.hasNext() && visitedNodes < maxVisitedNodes && entries.size < maxEpisodeCount) {
                        val key = keys.next()
                        visitedNodes++
                        val number = key.toIntOrNull() ?: continue
                        if (number < 0) continue
                        val child = node.opt(key)
                        val season = groupSeason ?: number.takeIf {
                            depth == 0 && !(child is JSONObject && isEpisode(child))
                        }
                        visitEpisodes(child, season, position++, depth + 1)
                    }
                }
            }
        }
        visitEpisodes(episodesNode, null, 0, 0)

        val episodes = entries
            .distinctBy { it.seasonNumber to it.id }
            .sortedWith(
                compareBy<SeriesEpisodeInfo> { it.seasonNumber }
                    .thenBy { it.episodeNumber }
            )

        val seasonMetadata =
            LinkedHashMap<Int, Pair<String?, String?>>()

        val seasonArray =
            root.optJSONArray("seasons") ?: JSONArray()

        for (index in 0 until seasonArray.length()) {
            val season =
                seasonArray.optJSONObject(index) ?: continue

            val number =
                season.intValue("season_number", "number")
                    ?: continue

            if (number < 0) continue

            seasonMetadata[number] =
                season.stringValue("name") to season.image(
                    "cover_big",
                    "cover",
                    "poster_path"
                )
        }

        // Usa apenas temporadas existentes na resposta do provedor.
        val numbers = (
                episodes.map { it.seasonNumber }
                ).distinct().sorted()

        val counts =
            episodes.groupingBy { it.seasonNumber }.eachCount()

        val seasons = numbers.map { number ->
            val metadata = seasonMetadata[number]

            SeriesSeasonInfo(
                number = number,
                title = metadata?.first
                    ?: if (number == 0) {
                        "Especiais"
                    } else {
                        "Temporada $number"
                    },
                coverUrl = metadata?.second,
                episodeCount = counts[number] ?: 0
            )
        }

        return SeriesEpisodesResult(
            seriesId = seriesId,
            title = info?.stringValue("name", "title"),
            plot = info?.stringValue(
                "plot",
                "description",
                "overview"
            ),
            backdropUrl =
                info?.firstImage("backdrop_path")
                    ?: info?.image("backdrop"),
            posterUrl = info?.image(
                "cover_big",
                "cover",
                "stream_icon"
            ),
            trailerUrl = info?.stringValue(
                "youtube_trailer",
                "trailer",
                "trailer_url"
            ),
            seasons = seasons,
            episodes = episodes
        )
    }

    /** Descreve somente tipos/contagens; nunca corpo, titulo, URL ou credencial. */
    private fun episodeShape(node: Any?): String {
        fun kind(value: Any?): String = when (value) {
            is JSONArray -> "array"
            is JSONObject -> "objeto"
            null, JSONObject.NULL -> "ausente"
            else -> "escalar"
        }
        val children = when (node) {
            is JSONArray -> (0 until minOf(node.length(), 3)).map { node.opt(it) }
            is JSONObject -> node.keys().asSequence().take(3).map { node.opt(it) }.toList()
            else -> emptyList()
        }
        return "${kind(node)}_filhos_${children.joinToString("_") { kind(it) }}"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private fun JSONObject.stringValue(
        vararg keys: String
    ): String? {
        for (key in keys) {
            val value = optString(key).trim()

            if (
                value.isNotEmpty() &&
                !value.equals("null", ignoreCase = true)
            ) {
                return value
            }
        }
        return null
    }

    private fun JSONObject.intValue(
        vararg keys: String
    ): Int? =
        keys.firstNotNullOfOrNull { key ->
            stringValue(key)?.toIntOrNull()
        }

    private fun JSONObject.image(
        vararg keys: String
    ): String? =
        keys.asSequence().mapNotNull { stringValue(it) }.firstOrNull {
            it.startsWith("https://", true) || it.startsWith("http://", true)
        }

    private fun JSONObject.firstImage(
        key: String
    ): String? {
        val array = optJSONArray(key)

        if (array != null) {
            for (index in 0 until array.length()) {
                val value = array.optString(index).trim()

                if (
                    value.startsWith("http://", true) ||
                    value.startsWith("https://", true)
                ) {
                    return value
                }
            }
        }

        return image(key)
    }

    private fun JSONObject.durationSeconds(): Int? {
        intValue(
            "duration_secs",
            "duration_seconds",
            "runtime_secs"
        )?.takeIf { it > 0 }?.let {
            return it
        }

        val value =
            stringValue("duration", "runtime") ?: return null

        val parts =
            value.split(':').mapNotNull(String::toIntOrNull)

        return when (parts.size) {
            3 -> (
                    parts[0] * 3600 +
                            parts[1] * 60 +
                            parts[2]
                    ).takeIf { it > 0 }

            2 -> (
                    parts[0] * 60 +
                            parts[1]
                    ).takeIf { it > 0 }

            else ->
                value.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?.times(60)
        }
    }
}

/** Only a closed error category, offsets and JSON punctuation; never parser message/body values. */
internal fun seriesJsonFailureSummary(json: String, error: Exception): String {
    val message = error.message.orEmpty()
    val kind = when {
        message.startsWith("Unterminated string", true) -> "texto_incompleto"
        message.startsWith("Unterminated object", true) -> "objeto_incompleto"
        message.startsWith("Unterminated array", true) -> "array_incompleto"
        message.startsWith("Unterminated escape", true) -> "escape_incompleto"
        message.startsWith("Invalid escape", true) || message.startsWith("Illegal escape", true) -> "escape_invalido"
        message.startsWith("Expected ':'", true) || message.startsWith("Expected a ':'", true) -> "dois_pontos_ausente"
        message.startsWith("Expected literal", true) || message.startsWith("Missing value", true) -> "valor_ausente"
        message.startsWith("End of input", true) -> "fim_prematuro"
        message.startsWith("Expected", true) || message.startsWith("A JSONObject text must end", true) -> "separador_ou_fechamento"
        message.startsWith("Value", true) -> "tipo_raiz"
        else -> "sintaxe"
    }
    // Android JSONTokener reports "at character N of ..."; JSON-java uses "at N [character ...]".
    val position = (Regex("""at character (\d+)""").find(message)?.groupValues?.get(1)
        ?: Regex(""" at (\d+) \[character""").find(message)?.groupValues?.get(1))
        ?.toIntOrNull()?.coerceIn(0, json.length)
    fun punctuation(text: String): String = text.map { char ->
        when (char) {
            '{', '}', '[', ']', ':', ',', '\"', '\\' -> char
            else -> 'x'
        }
    }.joinToString("")
    val start = ((position ?: json.length) - 20).coerceAtLeast(0)
    val end = ((position ?: json.length) + 12).coerceAtMost(json.length)
    return "json_invalido tipo=$kind pos=${position ?: -1} tamanho=${json.length} " +
        "entorno=${punctuation(json.substring(start, end))} final=${punctuation(json.takeLast(32))}"
}
