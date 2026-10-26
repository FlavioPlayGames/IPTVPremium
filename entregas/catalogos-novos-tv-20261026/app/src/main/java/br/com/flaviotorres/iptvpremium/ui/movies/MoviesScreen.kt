package br.com.flaviotorres.iptvpremium.ui.movies
import androidx.compose.runtime.*
import androidx.paging.compose.LazyPagingItems
import br.com.flaviotorres.iptvpremium.domain.model.*
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.data.cache.PosterKind

@Composable
fun MoviesScreen(
    favoriteMovieIds: Set<String> = emptySet(),
    onMovieClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    pagedMovies: LazyPagingItems<Movie>? = null,
    pagingCategories: List<Category> = emptyList(),
    totalMovieCount: Int = 0,
    pagingCategoryCounts: Map<String?, Int> = emptyMap(),
    onCategoryAccessRequested: (String?, String, () -> Unit) -> Unit = { _, _, proceed -> proceed() },
    onSourceCategorySelected: (String?) -> Unit = {},
    onContinueWatchingSelected: () -> Unit = {},
    watchHistoryEntries: List<WatchHistoryEntry> = emptyList(),
    onFavoritesSelected: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSortModeChanged: (String) -> Unit = {},
    onRemoveFromContinue: (String) -> Unit = {},
    onCatalogInteraction: (Boolean) -> Unit = {},
    posterPage: (suspend (Int) -> br.com.flaviotorres.iptvpremium.data.cache.PosterPage)? = null,
    onResolvePosterCategory: (suspend (String)->String?)? = null
) {
    if (KidsAppearance.active) {
        KidsMoviesCatalog(
            favoriteMovieIds = favoriteMovieIds,
            onMovieClick = onMovieClick,
            onFavoriteClick = onFavoriteClick,
            pagedMovies = pagedMovies,
            pagingCategories = pagingCategories,
            totalMovieCount = totalMovieCount,
            pagingCategoryCounts = pagingCategoryCounts,
            onCategoryAccessRequested = onCategoryAccessRequested,
            onSourceCategorySelected = onSourceCategorySelected,
            onContinueWatchingSelected = onContinueWatchingSelected,
            watchHistoryEntries = watchHistoryEntries,
            onFavoritesSelected = onFavoritesSelected,
            onSearchQueryChanged = onSearchQueryChanged,
            onSortModeChanged = onSortModeChanged,
            onRemoveFromContinue = onRemoveFromContinue,
            onCatalogInteraction = onCatalogInteraction,
            posterPage = posterPage,
            onResolvePosterCategory = onResolvePosterCategory
        )
        return
    }

    UnifiedMediaCatalog(
        kind = PosterKind.MOVIES, title = "Filmes", paging = pagedMovies,
        item = { CatalogPoster(it.id, it.name, it.posterUrl, it.categoryId, it.rating) },
        categories = pagingCategories, totalCount = totalMovieCount, categoryCounts = pagingCategoryCounts,
        favorites = favoriteMovieIds, history = watchHistoryEntries,
        onCategoryAccess = onCategoryAccessRequested, onCategory = onSourceCategorySelected,
        onFavorites = onFavoritesSelected, onContinue = onContinueWatchingSelected,
        onSearch = onSearchQueryChanged, onSort = onSortModeChanged,
        onOpen = onMovieClick, onFavorite = onFavoriteClick, onRemoveContinue = onRemoveFromContinue,
        onInteraction = onCatalogInteraction, posterPage = posterPage, resolveCategory = onResolvePosterCategory
    )
}
