package br.com.flaviotorres.iptvpremium.data.repository

import java.text.Normalizer
import java.util.Locale

// Release tags belong to an edition, not to the work. Keep numbers in real titles/sequels.
private val releaseTag = Regex("\\[(?:L|D|LEG|DUB|DUAL|MULTI|PT-BR|EN)\\]|\\b(?:4k|8k|hdr(?:10)?|fhd|uhd|hd|sd|2160p|1080[pi]|720p|480p|h[ .-]?26[45]|x26[45]|hevc|av1|dub|leg|dublado|legendado|dual(?:[ .-]?audio)?|multi[ .-]?audio|pt[ .-]?br|blu[ .-]?ray|web[ .-]?dl|web[ .-]?rip)\\b", RegexOption.IGNORE_CASE)
private val bracketYear = Regex("[\\[(]((?:19|20)[0-9]{2})[\\])]")
internal fun catalogTitleYear(title: String, year: Int?): Int? = year?.takeIf { it in 1888..2100 }
    ?: bracketYear.find(title)?.groupValues?.get(1)?.toIntOrNull()

internal fun artworkTitle(value: String): String {
    val text = Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT).replace(releaseTag, " ").replace(bracketYear, " ").trim()
    return text.replace(Regex("[^a-z0-9]+"), " ").trim()
}

internal fun catalogMetadataKey(kind: String, title: String, year: Int?, tmdb: String?, imdb: String?): String? {
    require(kind == "movie" || kind == "tv")
    tmdb?.trim()?.toIntOrNull()?.takeIf { it > 0 }?.let { return "$kind:tmdb:$it" }
    imdb?.trim()?.takeIf { it.matches(Regex("tt[0-9]+")) }?.let { return "$kind:imdb:$it" }
    val name = artworkTitle(title).takeIf { it.isNotBlank() } ?: return null
    val resolvedYear = catalogTitleYear(title, year) ?: return null
    return "$kind:title:$resolvedYear:$name"
}
