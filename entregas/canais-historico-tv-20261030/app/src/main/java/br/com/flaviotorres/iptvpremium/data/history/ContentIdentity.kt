package br.com.flaviotorres.iptvpremium.data.history

import java.text.Normalizer
import java.util.Locale

/** Provider IDs are scoped to their playlist, never treated as external identifiers. */
data class ContentIdentity(
    val kind: String,
    val title: String,
    val year: Int? = null,
    val imdb: String? = null,
    val tmdb: String? = null,
    val alternateTitles: List<String> = emptyList(),
    val providerAssetKey:String?=null,
    val providerAssetId:String?=null
) {
    val normalizedTitle: String by lazy(LazyThreadSafetyMode.PUBLICATION) { normalizeTitle(title, normalizedYear) }
    val normalizedTitles: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        (listOf(title) + alternateTitles.take(32)).map { normalizeTitle(it, normalizedYear) }
            .filter { it.isNotBlank() }.toSet()
    }
    fun sharesTitle(other: ContentIdentity): Boolean = normalizedTitles.any { it in other.normalizedTitles }
    val normalizedYear: Int? get() = year?.takeIf { it in 1888..2100 }
        ?: releaseYearPattern.find(title)?.groupValues?.get(1)?.toIntOrNull()
    val imdbKey: String? get() = imdb?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.matches(imdbPattern) }
    val tmdbKey: String? get() = tmdb?.trim()?.toLongOrNull()?.takeIf { it > 0 }?.toString()

    fun confidence(other: ContentIdentity): Int {
        if (kind != other.kind) return 0
        if (imdbKey != null && other.imdbKey != null && imdbKey != other.imdbKey) return 0
        if (tmdbKey != null && other.tmdbKey != null && tmdbKey != other.tmdbKey) return 0
        if ((imdbKey != null && imdbKey == other.imdbKey) || (tmdbKey != null && tmdbKey == other.tmdbKey)) return 2
        if (kind=="movies" && providerAssetKey!=null && providerAssetKey==other.providerAssetKey) return 3
        if (!sharesTitle(other)) return 0
        // Nomes alternativos exigem ano confirmado nos dois lados, salvo ID igual acima.
        if (normalizedTitle != other.normalizedTitle &&
            (normalizedYear == null || other.normalizedYear == null)) return 0
        if (normalizedYear != null && other.normalizedYear != null) return if (normalizedYear == other.normalizedYear) 1 else 0
        // A unique exact series name can supply a series-only shortcut. Films require a year.
        return if (kind == "series") 1 else 0
    }
    companion object {
        private val spaces = Regex("\\s+")
        private val punctuation = Regex("[^\\p{L}\\p{N}]+")
        private val accentMarks = Regex("\\p{M}+")
        private val qualityWords = Regex("\\b(?:4k|2160p|1080p|720p|uhd|fhd|hdr10|hdr|dublado|legendado)\\b")
        private val qualityTags = Regex("\\[(?:4k|uhd|fhd|hd|sd|dv|hdr|hdr10|l|d|dub|leg|dual|dual audio|dublado|legendado)\\]")
        private val subtitlePattern = Regex("(?:^|[\\s\\[(])(?:l|leg|legendado)(?:$|[\\s\\])])", RegexOption.IGNORE_CASE)
        private val imdbPattern = Regex("tt\\d{5,12}")
        private val releaseYearPattern = Regex("(?:\\(|\\[|\\s)((?:19|20)\\d{2})(?:\\)|\\]|\\s*$)")
        fun isSubtitled(title: String): Boolean = subtitlePattern.containsMatchIn(title)
        fun preferredVariantIndex(candidates: List<Pair<String, Int?>>): Int? = candidates.indices.minWithOrNull(
            compareBy<Int> { isSubtitled(candidates[it].first) }.thenBy { candidates[it].second ?: Int.MAX_VALUE }.thenBy { it })
        fun compatibleVariants(source: ContentIdentity, candidates: List<ContentIdentity>, score: Int): Boolean {
            if (candidates.isEmpty()) return false
            if (candidates.mapNotNull { it.imdbKey }.distinct().size > 1 || candidates.mapNotNull { it.tmdbKey }.distinct().size > 1) return false
            if (candidates.size == 1 || score == 2) return true
            if(score==3 && source.providerAssetKey!=null && candidates.all {it.providerAssetKey==source.providerAssetKey}) return true
            val year = source.normalizedYear ?: return false
            return candidates.all { it.normalizedYear == year && it.sharesTitle(source) }
        }

        fun normalizeTitle(raw: String, releaseYear: Int? = null): String {
            var title = raw.lowercase(Locale.ROOT)
            title = title.replace(qualityTags, " ")
            title = title.replace(qualityWords, " ")
            if (releaseYear != null) {
                title = title.replace(Regex("(?:\\(|\\[)$releaseYear(?:\\)|\\])"), " ")
                    .replace(Regex("\\s+$releaseYear\\s*$"), " ")
            }
            return Normalizer.normalize(title, Normalizer.Form.NFD).replace(accentMarks, "")
                .replace(punctuation, " ").trim().replace(spaces, " ")
        }
    }
}

/** Transient bounded Room projection; stream URL is reduced to a credential-free asset key before persistence. */
data class SharedCatalogIdentity(
    val contentId: String, val title: String, val year: Int?,
    val imdb: String?, val tmdb: String?, val poster: String?, val providerOrder: Int? = null, val streamUrl:String?=null
)

/** Bounded transient join used only for exact provider episode matching. */
data class SharedSeriesEpisodeIdentity(
    val seriesId:String,val title:String,val year:Int?,val imdb:String?,val tmdb:String?,val poster:String?,val providerOrder:Int?,
    val episodeId:String,val seasonNumber:Int?,val episodeNumber:Int?,val episodeTitle:String,val streamUrl:String
)
