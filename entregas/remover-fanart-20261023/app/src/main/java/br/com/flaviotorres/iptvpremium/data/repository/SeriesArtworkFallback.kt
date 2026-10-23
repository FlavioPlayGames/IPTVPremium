package br.com.flaviotorres.iptvpremium.data.repository

import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

internal data class ArtworkIdentity(val titles: List<String>, val year: Int?, val imdb: String? = null)
internal data class SupplementalSeriesArt(
    val backdropUrl: String? = null,
    val episodeStills: Map<Int, String> = emptyMap(),
    val matched: Boolean = false
)

/** Only artwork. Never adds episodes, changes provider IDs or touches shared history. */
internal class SeriesArtworkFallback(
    private val get: suspend (service: String, path: String, query: Map<String, String>) -> String?
) {
    private suspend fun json(service: String, path: String, query: Map<String, String> = emptyMap()): String? =
        try { get(service, path, query) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }

    suspend fun load(identity: ArtworkIdentity, season: Int, episodes: Set<Int>,
                     needBackdrop: Boolean): SupplementalSeriesArt {
        val imdb = identity.imdb?.trim()?.takeIf { it.matches(Regex("tt[0-9]+")) }
        val exact = imdb?.let { json("tvmaze", "/lookup/shows", mapOf("imdb" to it)) }
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?.takeIf { it.optJSONObject("externals")?.optString("imdb") == imdb }
        val show = exact ?: findByName(identity) ?: return SupplementalSeriesArt()
        val id = show.optInt("id", -1).takeIf { it > 0 } ?: return SupplementalSeriesArt()
        val stills = if (season >= 0 && episodes.isNotEmpty()) {
            val raw = json("tvmaze", "/shows/$id/episodes", mapOf("specials" to "1"))
            parseTvmazeEpisodeImages(raw, season, episodes)
        } else emptyMap()
        return SupplementalSeriesArt(
            backdropUrl = if (needBackdrop) webImage(show.optJSONObject("image")?.optString("original")) else null,
            episodeStills = stills, matched = true
        )
    }

    private suspend fun findByName(identity: ArtworkIdentity): JSONObject? {
        if (identity.year == null) return null // Name alone can match a remake or another show.
        val names = identity.titles.map(::artworkTitle).filter { it.isNotBlank() }.toSet()
        for (title in identity.titles.distinct().take(4)) {
            val raw = json("tvmaze", "/search/shows", mapOf("q" to artworkTitle(title))) ?: continue
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: continue
            val matches = (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optJSONObject("show") }
                .filter { artworkTitle(it.optString("name")) in names &&
                    it.optString("premiered").take(4).toIntOrNull() == identity.year &&
                    (identity.imdb.isNullOrBlank() || it.optJSONObject("externals")?.optString("imdb").isNullOrBlank() ||
                        it.optJSONObject("externals")?.optString("imdb") == identity.imdb) }
                .distinctBy { it.optInt("id") }
            if (matches.size == 1) return matches.single()
            if (matches.size > 1) return null
        }
        return null
    }
}

internal fun artworkTitle(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)
    .replace(Regex("\\[(?:L|D|LEG|DUB)\\]|\\b(?:4k|hdr|fhd|uhd|1080p|720p|dublado|legendado)\\b", RegexOption.IGNORE_CASE), " ")
    .replace(Regex("\\((?:19|20)[0-9]{2}\\)"), " ")
    .replace(Regex("[^a-z0-9]+"), " ").trim()

internal fun webImage(value: String?): String? = value?.trim()?.takeIf {
    it.startsWith("https://", true) || it.startsWith("http://", true)
}
internal fun parseTvmazeEpisodeImages(raw: String?, season: Int, wanted: Set<Int>): Map<Int, String> {
    val array = raw?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return emptyMap()
    val grouped = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
        .filter { it.optInt("season", -1) == season && it.optInt("number", -1) in wanted }
        .groupBy { it.optInt("number") }
    return grouped.mapNotNull { (number, entries) ->
        if (entries.size != 1) null else webImage(entries.single().optJSONObject("image")?.optString("original"))
            ?.let { number to it }
    }.toMap()
}
