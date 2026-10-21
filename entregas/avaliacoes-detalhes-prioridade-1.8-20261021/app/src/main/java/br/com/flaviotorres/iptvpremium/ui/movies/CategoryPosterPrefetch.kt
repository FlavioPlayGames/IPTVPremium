package br.com.flaviotorres.iptvpremium.ui.movies

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import br.com.flaviotorres.iptvpremium.data.cache.*
import br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

private data class PosterViewport(val first: Int, val last: Int, val urls: List<String?>, val busy: Boolean)

/** Composition lifetime, not STARTED lifetime: backgrounding doesn't cancel the category. */
@Composable
internal fun <T : Any> CategoryPosterPrefetch(kind: PosterKind, category: String,
    grid: LazyGridState, paging: LazyPagingItems<T>?, url: (T) -> String?, blocked: Boolean,
    page: (suspend (Int) -> PosterPage)?) {
    val context = LocalContext.current.applicationContext
    val access by ParentalPolicy.accessFlow.collectAsState(initial = ParentalPolicy.access())
    val blockedNow by rememberUpdatedState(blocked)
    val readPage by rememberUpdatedState(page)
    val getUrl by rememberUpdatedState(url)
    DisposableEffect(kind) { onDispose { MoviePosterLoader.releaseVisible(context, kind) } }
    LaunchedEffect(paging, category, kind, access) {
        val items = paging ?: return@LaunchedEffect
        var cursor = -1
        var previousFirst = -1
        val viewports = snapshotFlow {
            val visible = grid.layoutInfo.visibleItemsInfo.sortedBy { it.index }
            PosterViewport(visible.firstOrNull()?.index ?: -1, visible.lastOrNull()?.index ?: -1,
                visible.map { item -> if (item.index in 0 until items.itemCount) items.peek(item.index)?.let(getUrl) else null },
                blockedNow || visible.isEmpty() || grid.isScrollInProgress || items.loadState.refresh is LoadState.Loading)
        }
        combine(viewports, CatalogMemoryPolicy.pressured, MediaOpeningPriority.busy) { viewport, pressure, selectedMedia ->
            viewport.copy(busy = viewport.busy || pressure || selectedMedia)
        }.collectLatest { viewport ->
            MoviePosterLoader.updateVisible(context, kind, viewport.urls)
            if (viewport.busy) return@collectLatest
            delay(100)
            // Complete all visible covers before asking for any off-screen row.
            for (batch in viewport.urls.chunked(PosterCategoryPolicy.GROUP)) {
                MoviePosterLoader.prefetchCategoryBatch(context, kind, batch, visible = true)
            }
            val row = PosterCategoryPolicy.nextRow(viewport.last + 1)
            if (cursor < row || previousFirst > viewport.first) cursor = row
            if (cursor < 0) cursor = row
            previousFirst = viewport.first
            val nextPage = readPage ?: return@collectLatest
            downloadPosterRows(cursor, nextPage,
                { batch -> MoviePosterLoader.prefetchCategoryBatch(context, kind, batch, visible = false) },
                { next -> cursor = next })
        }
    }
}
