package br.com.flaviotorres.iptvpremium.ui.series
import androidx.compose.runtime.*
import androidx.paging.compose.LazyPagingItems
import br.com.flaviotorres.iptvpremium.domain.model.*
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.data.cache.PosterKind

@Composable
fun SeriesScreen(
    favoriteSeriesIds: Set<String> = emptySet(),
    onSeriesClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    // Argumentos opcionais: o AppNavigation os conectara na proxima etapa.
    pagedSeries: LazyPagingItems<Series>? = null,
    pagingCategories: List<Category> = emptyList(),
    totalSeriesCount: Int = 0,
    pagingCategoryCounts: Map<String?, Int> = emptyMap(),
    onCategoryAccessRequested: (String?, String, () -> Unit) -> Unit = { _, _, proceed -> proceed() },
    onSourceCategorySelected: (String?) -> Unit = {},
    onFavoritesSelected: () -> Unit = {},
    onContinueWatchingSelected: () -> Unit = {},
    watchHistoryEntries: List<WatchHistoryEntry> = emptyList(),
    onSearchQueryChanged: (String) -> Unit = {},
    onSortModeChanged: (String) -> Unit = {},
    onSeriesFocused: (String) -> Unit = {},
    onSeriesBlurred: (String) -> Unit = {},
    onCatalogInteraction: (Boolean) -> Unit = {},
    posterPage: (suspend (Int) -> br.com.flaviotorres.iptvpremium.data.cache.PosterPage)? = null,
    onResolvePosterCategory: (suspend (String)->String?)? = null,
    onBack: () -> Unit = {}
) {
    if (KidsAppearance.active) {
        KidsSeriesCatalog(
            favoriteSeriesIds = favoriteSeriesIds,
            onSeriesClick = onSeriesClick,
            onFavoriteClick = onFavoriteClick,
            pagedSeries = pagedSeries,
            pagingCategories = pagingCategories,
            totalSeriesCount = totalSeriesCount,
            pagingCategoryCounts = pagingCategoryCounts,
            onCategoryAccessRequested = onCategoryAccessRequested,
            onSourceCategorySelected = onSourceCategorySelected,
            onFavoritesSelected = onFavoritesSelected,
            onContinueWatchingSelected = onContinueWatchingSelected,
            watchHistoryEntries = watchHistoryEntries,
            onSearchQueryChanged = onSearchQueryChanged,
            onSortModeChanged = onSortModeChanged,
            onSeriesFocused = onSeriesFocused,
            onSeriesBlurred = onSeriesBlurred,
            onCatalogInteraction = onCatalogInteraction,
            posterPage = posterPage,
            onResolvePosterCategory = onResolvePosterCategory
        )
        return
    }

    br.com.flaviotorres.iptvpremium.ui.theme.rememberSeriesRoomReady()
    UnifiedMediaCatalog(
        kind = PosterKind.SERIES, title = "Séries", paging = pagedSeries,
        item = { CatalogPoster(it.id, it.name, it.posterUrl, it.categoryId, it.rating) },
        categories = pagingCategories, totalCount = totalSeriesCount, categoryCounts = pagingCategoryCounts,
        favorites = favoriteSeriesIds, history = watchHistoryEntries,
        onCategoryAccess = onCategoryAccessRequested, onCategory = onSourceCategorySelected,
        onFavorites = onFavoritesSelected, onContinue = onContinueWatchingSelected,
        onSearch = onSearchQueryChanged, onSort = onSortModeChanged,
        onOpen = onSeriesClick, onFavorite = onFavoriteClick, onFocused = onSeriesFocused, onBlurred = onSeriesBlurred,
        onInteraction = onCatalogInteraction, posterPage = posterPage, resolveCategory = onResolvePosterCategory, onExit = onBack
    )
}
