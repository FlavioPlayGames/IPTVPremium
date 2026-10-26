package br.com.flaviotorres.iptvpremium.ui.components

import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.ui.movies.movieHoldDurationMs
import br.com.flaviotorres.iptvpremium.ui.movies.movieHoldReady

fun main() {
    check(catalogPagingKey("all", null, "", "DEFAULT") == catalogPagingKey("continue", null, "", "DEFAULT"))
    check(catalogPagingKey("all", null, "", "DEFAULT") != catalogPagingKey("source:1", "1", "", "DEFAULT"))
    check(catalogPagingKey("all", null, "", "DEFAULT") != catalogPagingKey("favorites", null, "", "DEFAULT"))
    check(catalogFocusAfterAction(listOf("a", "b", "c"), "b", false) == "b")
    check(catalogFocusAfterAction(listOf("a", "b", "c"), "b", true) == "c")
    check(catalogFocusAfterAction(listOf("a", "b", "c"), "c", true) == "b")
    check(catalogFocusAfterAction(listOf("a"), "a", true) == null)
    check(catalogFocusAfterAction(emptyList(), "a", true) == null)
    fun history(id: String, type: PlaybackContentType, updated: Long, series: String? = null,
        position: Long = 20_000L) = WatchHistoryEntry("playlist", type, id, series,
        "Title $id", "poster-$id", "Series $series", "series-poster-$series",
        positionMs = position, durationMs = 100_000L, updatedAtMs = updated)
    val data = listOf(history("movie1", PlaybackContentType.MOVIE, 10),
        history("episode1", PlaybackContentType.EPISODE, 20, "s1"),
        history("episode2", PlaybackContentType.EPISODE, 30, "s1"),
        history("episode3", PlaybackContentType.EPISODE, 40, "s2"),
        history("completed", PlaybackContentType.MOVIE, 50, position = 99_000),
        history("new", PlaybackContentType.MOVIE, 60, position = 1_000))
    val movies = catalogContinuePosters(data, false)
    check(movies.map { it.id } == listOf("movie1"))
    check(movies.single().progress == .2f)
    val series = catalogContinuePosters(data, true)
    check(series.map { it.id } == listOf("s2", "s1"))
    check(series[1].url == "series-poster-s1")
    check(series[1].name == "Series s1")
    check(movieHoldDurationMs(false, 1000) == 1000L)
    check(movieHoldDurationMs(true, 1000) == 10000L)
    check(!movieHoldReady(999, 1000, true, false))
    check(movieHoldReady(1000, 1000, true, false))
    check(!movieHoldReady(10000, 10000, false, false))
    check(!movieHoldReady(10000, 10000, true, true))
    println("PASS focus successor/empty history, newest series episode grouped, movie progress/completion, original OK thresholds and cancelled/focus-loss protection")
}
