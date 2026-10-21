package br.com.flaviotorres.iptvpremium.data.storage

import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource

/** Exact connection identity; display names and enabled state don't invalidate a catalogue. */
internal fun sameCatalogSource(before: PlaylistSource?, after: PlaylistSource): Boolean = when {
    before == null || before.id != after.id -> false
    before is PlaylistSource.M3u && after is PlaylistSource.M3u -> before.playlistUrl == after.playlistUrl
    before is PlaylistSource.XtreamCodes && after is PlaylistSource.XtreamCodes ->
        before.serverUrl == after.serverUrl && before.username == after.username &&
            before.password == after.password && before.fallbackM3uUrl == after.fallbackM3uUrl
    else -> false
}

/** Automatic catalogue work gets a grace period after opening, regardless of APK version. */
internal object CatalogRefreshPolicy {
    const val OPENING_GRACE_MS = 60_000L
    fun defer(nowElapsed: Long, homeReadyElapsed: Long?): Boolean =
        homeReadyElapsed == null || nowElapsed - homeReadyElapsed < OPENING_GRACE_MS
}
