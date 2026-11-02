package br.com.flaviotorres.iptvpremium.ui.movies

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import br.com.flaviotorres.iptvpremium.data.cache.PosterKind
import br.com.flaviotorres.iptvpremium.data.remote.TmdbClientProvider
import br.com.flaviotorres.iptvpremium.data.repository.resolveSeriesArtworkIdentity
import br.com.flaviotorres.iptvpremium.data.repository.webImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

private val missingPosterSlots = Semaphore(2)

/** Only called while the card is visible. No full-catalog scan and no work on the UI thread. */
internal suspend fun loadCatalogPoster(context: Context, url: String?, kind: PosterKind,
    name: String, year: Int?, tmdbId: String?, imdbId: String?): ImageBitmap? {
    br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.awaitIdle()
    br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.busy.first { !it }
    val providerUrl = webImage(url)
    if (providerUrl != null) MoviePosterLoader.loadWithRetry(context, providerUrl, kind)?.let { return it }
    if (br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.value ||
        br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.busy.value) return null
    return missingPosterSlots.withPermit {
        try {
            withTimeoutOrNull(6_000L) {
                val root = resolveSeriesArtworkIdentity(name, year, tmdbId, imdbId,
                    TmdbClientProvider.client::get, if (kind == PosterKind.SERIES) "tv" else "movie",
                    referenceImages = listOfNotNull(providerUrl)) ?: return@withTimeoutOrNull null
                val path = root.optString("poster_path").takeIf { it.startsWith("/") && it.length > 1 }
                    ?: return@withTimeoutOrNull null
                MoviePosterLoader.loadWithRetry(context, "https://image.tmdb.org/t/p/w500$path", kind)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
    }
}
