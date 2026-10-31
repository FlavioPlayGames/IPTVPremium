package br.com.flaviotorres.iptvpremium.ui.movies
import androidx.compose.runtime.*
import androidx.paging.compose.LazyPagingItems
import br.com.flaviotorres.iptvpremium.domain.model.*
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.kids.*
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
    onResolvePosterCategory: (suspend (String)->String?)? = null,
    onBack: () -> Unit = {}
) {
    if (KidsAppearance.active) {
        KidsMediaCatalog(
        kind = PosterKind.MOVIES, title = "Filmes", paging = pagedMovies,
        item = { KidsPoster(it.id, it.name, it.posterUrl, it.categoryId, it.rating) },
        categories = pagingCategories, totalCount = totalMovieCount, categoryCounts = pagingCategoryCounts,
        favorites = favoriteMovieIds, history = watchHistoryEntries,
        onCategoryAccess = onCategoryAccessRequested, onCategory = onSourceCategorySelected,
        onFavorites = onFavoritesSelected, onContinue = onContinueWatchingSelected,
        onSearch = onSearchQueryChanged, onSort = onSortModeChanged,
        onOpen = onMovieClick, onFavorite = onFavoriteClick, onRemoveContinue = onRemoveFromContinue,
        onInteraction = onCatalogInteraction, posterPage = posterPage, resolveCategory = onResolvePosterCategory, onExit = onBack
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
        onInteraction = onCatalogInteraction, posterPage = posterPage, resolveCategory = onResolvePosterCategory, onExit = onBack
    )
}
