package br.com.flaviotorres.iptvpremium.data.ratings

import java.security.MessageDigest

internal data class PersonalRatingIdentity(val kind: String, val playlist: String?, val content: String?,
    val tmdb: String?, val imdb: String?, val title: String? = null, val year: Int? = null) {
    init { require(kind == "movies" || kind == "series") }
    private val hashedKeys: List<String> by lazy {
        val identities = buildList {
            imdb?.trim()?.lowercase()?.takeIf { it.matches(Regex("tt[0-9]+")) }?.let { add("$kind|imdb|$it") }
            tmdb?.trim()?.toLongOrNull()?.takeIf { it > 0 }?.let { add("$kind|tmdb|$it") }
            if (!playlist.isNullOrBlank() && !content.isNullOrBlank()) {
                val foldedTitle=title?.trim()?.takeIf {it.isNotEmpty()}?.let {
                    java.text.Normalizer.normalize(it,java.text.Normalizer.Form.NFKD)
                        .replace(Regex("\\p{M}+"),"").lowercase(java.util.Locale.ROOT)
                }
                // Provider IDs can be reused for another film after a list/account change.
                add("$kind|local|${playlist.length}:$playlist|${content.length}:$content|${foldedTitle.orEmpty()}|${year?:0}")
            }
        }
        identities.map { value -> MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) } }
    }
    fun keys(): List<String> = hashedKeys
}
