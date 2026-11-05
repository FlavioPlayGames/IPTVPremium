package br.com.flaviotorres.iptvpremium.ui.kids
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
internal data class KidsPoster(val id: String, val name: String, val url: String?,
    val categoryId: String? = null, val rating: Double? = null, val progress: Float? = null, val year: Int? = null, val tmdbId: String? = null, val imdbId: String? = null)
internal fun kidsFocusAfterAction(ids: List<String>, current: String, removed: Boolean): String? {
    if (!removed) return current
    val index = ids.indexOf(current)
    if (index < 0) return ids.firstOrNull()
    return ids.getOrNull(index + 1) ?: ids.getOrNull(index - 1)
}

internal fun kidsContinuePosters(history: List<WatchHistoryEntry>, series: Boolean): List<KidsPoster> {
        val sorted = history.sortedByDescending { it.updatedAtMs }
        return if (series) br.com.flaviotorres.iptvpremium.data.history.latestHistorySeries(sorted).filter{it.inProgress}.map { KidsPoster(it.seriesId!!, it.seriesTitle?.takeIf { title -> title.isNotBlank() } ?: it.title, it.seriesPosterUrl?.takeIf { url -> url.isNotBlank() }, rating=it.catalogRating,year=it.year) }
        else sorted.filter { it.contentType == PlaybackContentType.MOVIE && it.inProgress }
            .map { KidsPoster(it.contentId, it.title, it.posterUrl, rating=it.catalogRating,progress=it.fraction,year=it.year) }
}

/** History is rendered locally while the retained Pager is kept on All. */
internal fun kidsPagingKey(menu: String, category: String?, query: String, sort: String): String =
    "${if (menu == "favorites") "favorites" else if (menu.startsWith("source:")) "source:${category.orEmpty()}" else "all"}|${query.trim()}|$sort"
