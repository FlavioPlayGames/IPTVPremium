package br.com.flaviotorres.iptvpremium.data.history
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
data class WatchHistoryEntry(
    val playlistId: String,
    val contentType: PlaybackContentType,
    val contentId: String,
    val seriesId: String? = null,
    val title: String,
    val posterUrl: String? = null,
    val seriesTitle: String? = null,
    val seriesPosterUrl: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val positionMs: Long,
    val durationMs: Long?,
    val updatedAtMs: Long,
    val seriesOnly: Boolean = false,
    val sharedFromPlaylist: String? = null,
    val finished:Boolean=false,
    val pendingNext:Boolean=false,
    val year:Int?=null,
    val tmdbId:String?=null,
    // Presentation metadata from the active catalogue; deliberately not persisted as watch data.
    val catalogRating:Double?=null
) {
    val fraction: Float?
        get() = durationMs?.takeIf { it > 0L }
            ?.let { (positionMs.toFloat() / it).coerceIn(0f, 1f) }

    val completed: Boolean
        get() = finished || (fraction ?: 0f) >= 0.95f

    val inProgress: Boolean
        get() = !completed && (pendingNext || seriesOnly || positionMs >= 10_000L)
}
