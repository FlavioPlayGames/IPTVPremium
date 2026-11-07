package br.com.flaviotorres.iptvpremium.core.player

import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

internal data class OpenSubtitleSession(
    val token: String, val apiBase: String, val scope: String,
    val expiresAt: Long, val appFingerprint: String
)
internal fun openSubtitleDigest(value:String):String=java.security.MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}


internal data class OpenSubtitleQuery(
    val title: String, val year: Int?, val tmdbId: String?,
    val season: Int? = null, val episode: Int? = null
) {
    val isEpisode get() = season != null && episode != null
    fun parameters(language:String="pt-br,en,es"): String {
        require(language in setOf("pt-br,en,es","pt-br","en","es"))
        require(title.isNotBlank() && (season == null) == (episode == null))
        val values = linkedMapOf("languages" to language, "type" to if (isEpisode) "episode" else "movie")
        if (!tmdbId.isNullOrBlank() && tmdbId.toLongOrNull()?.let { it > 0 } == true)
            values[if (isEpisode) "parent_tmdb_id" else "tmdb_id"] = tmdbId
        else values["query"] = title.trim()
        if (!isEpisode && year != null) values["year"] = year.toString()
        if (isEpisode) {
            require(season!! >= 0 && episode!! > 0)
            values["season_number"] = season.toString()
            values["episode_number"] = episode.toString()
        }
        return values.entries.joinToString("&") { (k,v) -> "$k=${URLEncoder.encode(v,"UTF-8")}" }
    }
}

internal data class OpenSubtitleHit(val fileId: String, val language: String, val release: String)
internal fun openSubtitleTitleKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "").lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

/** Read only verified work/episode matches. Never offer another episode or language. */
internal fun decodeOpenSubtitleHits(json: JSONObject, wanted: OpenSubtitleQuery): List<OpenSubtitleHit> {
    val data = json.optJSONArray("data") ?: return emptyList()
    val result = ArrayList<OpenSubtitleHit>(); val ids = HashSet<String>()
    for (i in 0 until minOf(data.length(), 100)) {
        val attributes = data.optJSONObject(i)?.optJSONObject("attributes") ?: continue
        val language = attributes.optString("language").lowercase(Locale.ROOT)
        if (language !in setOf("pt-br", "en", "es")) continue
        val feature = attributes.optJSONObject("feature_details") ?: continue
        if (wanted.isEpisode) {
            if (!feature.optString("feature_type").equals("Episode",true)) continue
            if (feature.optInt("season_number", -1) != wanted.season || feature.optInt("episode_number", -1) != wanted.episode) continue
        } else {
            if (!feature.optString("feature_type").equals("Movie", true)) continue
            if (wanted.year != null && feature.optInt("year", -1) != wanted.year) continue
        }
        val expectedId = wanted.tmdbId?.toLongOrNull()?.takeIf { it > 0 }
        val id = feature.optLong(if (wanted.isEpisode) "parent_tmdb_id" else "tmdb_id", 0)
        if (expectedId != null) {
            if (id != expectedId) continue
        } else {
            val names = if (wanted.isEpisode) listOf(feature.optString("parent_title"))
                else listOf(feature.optString("title"),feature.optString("original_title"))
            if (names.none { openSubtitleTitleKey(it) == openSubtitleTitleKey(wanted.title) }) continue
        }
        val files = attributes.optJSONArray("files") ?: continue
        for (j in 0 until minOf(files.length(), 10)) {
            val file = files.optJSONObject(j) ?: continue
            val fileId = file.optString("file_id")
            if (!fileId.matches(Regex("[1-9][0-9]{0,17}")) || !ids.add(fileId)) continue
            val release = attributes.optString("release").ifBlank { file.optString("file_name") }
                .replace(Regex("[\\r\\n\\t]"), " ").take(240)
            result.add(OpenSubtitleHit(fileId, when(language){"pt-br"->"pt-BR";"en"->"en";else->"es"},release))
        }
    }
    return result.sortedBy { when(it.language){"pt-BR"->0;"en"->1;else->2} }.take(100)
}

/** A login may choose the official VIP API host, never a third-party host. */
internal fun openSubtitleApiBase(raw: String): String? = runCatching {
    val uri = URI(if(raw.startsWith("https://")) raw else "https://$raw")
    if (uri.scheme != "https" || uri.userInfo != null || uri.port !in setOf(-1,443) ||
        uri.rawQuery != null || uri.rawFragment != null || uri.path !in listOf("","/")) return null
    val host=uri.host?.lowercase(Locale.ROOT) ?: return null
    if(host !in setOf("api.opensubtitles.com","vip-api.opensubtitles.com")) return null
    "https://$host/api/v1"
}.getOrNull()

internal fun openSubtitleDownloadUrl(raw: String): Boolean = runCatching {
    val uri=URI(raw); val host=uri.host?.lowercase(Locale.ROOT) ?: return false
    uri.scheme=="https" && uri.userInfo==null && uri.port in setOf(-1,443) && uri.rawFragment==null &&
        (host=="opensubtitles.com" || host.endsWith(".opensubtitles.com"))
}.getOrDefault(false)

internal enum class OpenSubtitleProblem(val text: String) {
    APP_UNAVAILABLE("OpenSubtitles ainda não está disponível nesta versão."),
    LOGIN_REQUIRED("Conecte sua conta do OpenSubtitles em Configurações > Legendas."),
    INVALID_LOGIN("Confira seu usuário e senha do OpenSubtitles.com."),
    SESSION_EXPIRED("A sessão do OpenSubtitles expirou. Entre novamente em Configurações > Legendas."),
    LIMITED("Limite do OpenSubtitles atingido. Aguarde a liberação da sua conta."),
    UNAVAILABLE("OpenSubtitles indisponível. Tente novamente mais tarde."),
    INVALID_RESPONSE("O OpenSubtitles retornou uma resposta incompatível."),
    STORAGE("Não foi possível salvar a sessão neste aparelho.")
}
internal class OpenSubtitleException(val problem: OpenSubtitleProblem) : Exception(problem.text)
