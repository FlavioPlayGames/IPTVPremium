package br.com.flaviotorres.iptvpremium.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

/** Resolve official names; presentation always comes from the pt-BR details endpoint. */
internal suspend fun resolveSeriesArtworkIdentity(
    title: String, year: Int?, tmdb: String?, imdb: String?,
    get: suspend (String, Map<String, String>) -> String,
    kind: String = "tv",
    referenceImages: List<String> = emptyList()
): JSONObject? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    require(kind == "tv" || kind == "movie")
    val nameField = if (kind == "movie") "title" else "name"
    val originalField = if (kind == "movie") "original_title" else "original_name"
    val dateField = if (kind == "movie") "release_date" else "first_air_date"
    suspend fun json(path: String, query: Map<String, String> = emptyMap()): JSONObject? = try {
        JSONObject(get(path, query))
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }
    suspend fun details(id: Int): JSONObject? = json("/$kind/$id", mapOf(
        "language" to "pt-BR", "append_to_response" to if (kind == "movie") "credits,images,translations,external_ids,alternative_titles" else "images,translations,external_ids,alternative_titles",
        "include_image_language" to "pt,null,en"
    ))?.takeIf { it.optInt("id", -1) == id }
    tmdb?.trim()?.toIntOrNull()?.takeIf { it > 0 }?.let { id -> details(id)?.let { return@withContext it } }
    imdb?.trim()?.takeIf { it.matches(Regex("tt[0-9]+")) }?.let { id ->
        val found = json("/find/$id", mapOf("external_source" to "imdb_id"))?.optJSONArray(if (kind == "movie") "movie_results" else "tv_results")
        if (found?.length() == 1) found.optJSONObject(0)?.optInt("id", -1)?.takeIf { it > 0 }
            ?.let { resolved -> details(resolved)?.let { return@withContext it } }
    }
    val wanted = artworkTitle(title)
    if (wanted.isBlank()) return@withContext null
    val trustedImages = referenceImages.mapNotNull(::trustedTmdbImagePath).toSet()
    val tried = mutableSetOf<Int>()
    for (language in listOf("en-US", "es-ES", "pt-BR", "pt-PT")) {
        val response = json("/search/$kind", mapOf("query" to wanted, "language" to language, "include_adult" to "false")) ?: continue
        val allCandidates = response.optJSONArray("results").objects()
            .filter { !it.optBoolean("adult", false) && it.optInt("id", -1) > 0 }
            .distinctBy { it.optInt("id") }
        // An exact official image path plus an official matching title can disambiguate homonyms.
        // A third-party poster, fuzzy title, or search popularity cannot identify a series.
        if (trustedImages.isNotEmpty()) {
            val byImage = allCandidates.filter { candidate ->
                listOf("poster_path", "backdrop_path").any { candidate.optString(it) in trustedImages }
            }
            if (byImage.size == 1) details(byImage.single().optInt("id"))?.let { root ->
                // Details may expose a different language-specific poster for this same ID.
                if (officialSeriesNames(root, kind).any { artworkTitle(it) == wanted })
                    return@withContext root
            }
        }
        val candidates = allCandidates.filter { year == null || it.optString(dateField).take(4).toIntOrNull() == year }
        val exact = candidates.filter { candidate ->
            listOf(candidate.optString(nameField), candidate.optString(originalField)).any { artworkTitle(it) == wanted }
        }
        if (exact.size > 1) return@withContext null
        if (exact.size == 1) details(exact.single().optInt("id"))?.let { return@withContext it }
        // Without year or too many plausible shows, do not guess by a translated title.
        if (year == null || candidates.size > 4) continue
        val checks = candidates.filter { tried.add(it.optInt("id")) }
        val matching = coroutineScope {
            checks.map { candidate -> async {
                details(candidate.optInt("id"))?.takeIf { root ->
                    root.optString(dateField).take(4).toIntOrNull() == year &&
                        officialSeriesNames(root, kind).any { artworkTitle(it) == wanted }
                }
            } }.awaitAll().filterNotNull()
        }
        if (matching.size > 1) return@withContext null
        if (matching.size == 1) return@withContext matching.single()
    }
    return@withContext null
}

private fun JSONArray?.objects(): List<JSONObject> = this?.let { a ->
    (0 until a.length()).mapNotNull { a.optJSONObject(it) }
}.orEmpty()

internal fun officialSeriesNames(root: JSONObject, kind: String = "tv"): List<String> = (
    listOf(root.optString(if (kind == "movie") "original_title" else "original_name"), root.optString(if (kind == "movie") "title" else "name")) +
        root.optJSONObject("translations")?.optJSONArray("translations").objects()
            .mapNotNull { it.optJSONObject("data")?.optString(if (kind == "movie") "title" else "name") } +
        root.optJSONObject("alternative_titles")?.optJSONArray(if (kind == "movie") "titles" else "results").objects()
            .map { it.optString("title") }
    ).map(String::trim).filter(String::isNotEmpty).distinct()

/** Query names and response language are separate: original -> English -> Spanish -> pt-BR. */
internal fun preferredSeriesSearchNames(root: JSONObject, kind: String = "tv"): List<String> {
    val translations = root.optJSONObject("translations")?.optJSONArray("translations").objects()
    fun translated(language: String, country: String): String? = translations
        .let { items -> items.firstOrNull { it.optString("iso_639_1") == language && it.optString("iso_3166_1") == country }
            ?: items.firstOrNull { language in listOf("en", "es") && it.optString("iso_639_1") == language } }
        ?.optJSONObject("data")?.optString(if (kind == "movie") "title" else "name")?.trim()?.takeIf { it.isNotEmpty() }
    return listOfNotNull(root.optString(if (kind == "movie") "original_title" else "original_name").takeIf { it.isNotBlank() },
        translated("en", "US"), translated("es", "ES"), translated("pt", "BR") ?: root.optString(if (kind == "movie") "title" else "name").takeIf { it.isNotBlank() })
        .distinct()
}

internal suspend fun resolveMovieArtworkIdentity(title: String, year: Int?, tmdb: String?, imdb: String?,
    get: suspend (String, Map<String, String>) -> String): JSONObject? =
    resolveSeriesArtworkIdentity(title, year, tmdb, imdb, get, "movie")

/** Accept only actual TMDB image URLs; never infer identity from a provider file name. */
internal fun trustedTmdbImagePath(value: String): String? = try {
    val uri = java.net.URI(value.trim())
    if (uri.scheme !in listOf("https", "http") || !uri.host.equals("image.tmdb.org", true)) null
    else Regex("^/t/p/[^/]+/([A-Za-z0-9_-]+\\.(?:jpg|png|webp))$")
        .matchEntire(uri.path.orEmpty())?.groupValues?.get(1)?.let { "/$it" }
} catch (_: Exception) { null }
