package br.com.flaviotorres.iptvpremium.ui.movies

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import br.com.flaviotorres.iptvpremium.data.cache.*
import br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

private data class PosterViewport(val first: Int, val last: Int, val urls: List<String?>, val busy: Boolean, val scrolling: Boolean)

/** Composition lifetime, not STARTED lifetime: backgrounding doesn't cancel the category. */
@Composable
internal fun <T : Any> CategoryPosterPrefetch(kind: PosterKind, category: String,
    grid: LazyGridState, paging: LazyPagingItems<T>?, url: (T) -> String?, blocked: Boolean,
    page: (suspend (Int) -> PosterPage)?, navigationActivity: StateFlow<Long>? = null) {
    val context = LocalContext.current.applicationContext
    val access by ParentalPolicy.accessFlow.collectAsState(initial = ParentalPolicy.access())
    val blockedNow by rememberUpdatedState(blocked)
    val readPage by rememberUpdatedState(page)
    val getUrl by rememberUpdatedState(url)
    DisposableEffect(kind) { onDispose { MoviePosterLoader.releaseVisible(context, kind) } }
    LaunchedEffect(paging, category, kind, access, navigationActivity) {
        val items = paging ?: return@LaunchedEffect
        val plan = PosterReservePlan()
        val reserve = PosterReserve(this,
            fetch = { url -> MoviePosterLoader.prefetch(context, url, kind = kind, duringNavigation = true) },
            prepare = { urls -> MoviePosterLoader.prepareNearestCategoryRow(context, kind, urls) },
            report = { target, ready, active -> android.util.Log.d("PosterReservePerf", "kind=${kind.name} target=$target ready=$ready active=$active") })
        val pace = PosterNavigationPace(this)
        launch { navigationActivity?.drop(1)?.collect { pace.activity() } }
        launch { pace.rapid.collect { rapid ->
            android.util.Log.d("PosterAheadPerf", "kind=${kind.name} rapid=$rapid batch=${PosterReservePlan.BATCH} maxAhead=${PosterReservePlan.MAX_AHEAD}")
        } }
        val viewports = snapshotFlow {
            val visible = grid.layoutInfo.visibleItemsInfo.sortedBy { it.index }
            PosterViewport(visible.firstOrNull()?.index ?: -1, visible.lastOrNull()?.index ?: -1,
                visible.map { item -> if (item.index in 0 until items.itemCount) items.peek(item.index)?.let(getUrl) else null },
                blockedNow || visible.isEmpty() || items.loadState.refresh is LoadState.Loading, grid.isScrollInProgress)
        }
        combine(viewports, CatalogMemoryPolicy.pressured, MediaOpeningPriority.busy, CatalogImportPriority.busy, pace.rapid) { viewport, pressure, selectedMedia, importing, rapid ->
            viewport.copy(busy = viewport.busy || pressure || selectedMedia || importing || rapid)
        }.distinctUntilChanged().collectLatest { viewport ->
            MoviePosterLoader.updateVisible(context, kind, viewport.urls)
            if (viewport.busy) {
                reserve.pause(); plan.reset()
                return@collectLatest
            }
            // A normal one-row move retains overlapping downloads; only the target plan changes.
            if (viewport.scrolling) return@collectLatest
            delay(200L)
            val nextPage = readPage ?: return@collectLatest
            val limit = plan.size(viewport.last + 1)
            val urls = mutableListOf<String?>()
            var offset = viewport.last + 1
            try {
                repeat(3) { // At most three bounded local pages, including filtered/short pages.
                    if (urls.size < limit) {
                        val page = nextPage(offset)
                        urls += page.urls.take(limit - urls.size)
                        val next = page.next
                        if (next == null || next <= offset) {
                            reserve.update(urls, viewport.urls)
                            return@collectLatest
                        }
                        offset = next
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                android.util.Log.d("PosterQueuePerf", "stage=ahead_deferred type=${error.javaClass.simpleName}")
                return@collectLatest
            }
            reserve.update(urls, viewport.urls)
        }
    }
}
