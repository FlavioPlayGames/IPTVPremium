package br.com.flaviotorres.iptvpremium.data.cache

internal enum class PosterKind(val directory: String) {
    MOVIES("iptv_movie_posters_125"), SERIES("iptv_series_posters_125")
}
data class PosterPage(val urls: List<String?>, val next: Int?)
internal object PosterCategoryPolicy {
    const val GROUP = 5
    const val CAPACITY = 125
    fun nextRow(afterVisible: Int): Int = afterVisible.coerceAtLeast(0) / GROUP * GROUP
}
