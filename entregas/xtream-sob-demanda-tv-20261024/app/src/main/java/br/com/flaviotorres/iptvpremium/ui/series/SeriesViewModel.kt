package br.com.flaviotorres.iptvpremium.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.domain.model.Series
import br.com.flaviotorres.iptvpremium.data.cache.settledCatalogSearch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Mesmo modelo de filtros paginados de Filmes, adaptado para Séries.
 * Categoria, busca, favoritos e ordenação são consultas SQL no Room.
 * O histórico "Continuar assistindo" será ligado ao player futuramente.
 */
private data class SeriesCategoryFilter(
        val selectionVersion: Int = 0,
    val categoryId: String? = null,
    val favoritesOnly: Boolean = false
)

private data class SeriesQueryOptions(
        val selectionVersion: Int = 0,
    val categoryId: String?,
    val searchQuery: String,
    val favoritesOnly: Boolean,
    val favoriteSeriesIds: Set<String>,
    val sortMode: String
)

private data class SeriesPagingRequest(
    val playlistId: String?,
    val options: SeriesQueryOptions
)

class SeriesViewModel(
    private val repository: PlaylistCatalogRepository
) : ViewModel() {

    private val catalogVisible = MutableStateFlow(true)
    private val _selectionRequired = MutableStateFlow(false)
    val selectionRequired: StateFlow<Boolean> = _selectionRequired.asStateFlow()
    fun setCatalogVisible(visible: Boolean) { catalogVisible.value = visible }

    private val playlistId = MutableStateFlow<String?>(null)
    private val categoryFilter = MutableStateFlow(SeriesCategoryFilter())
    private val searchQuery = MutableStateFlow("")
    private val favoriteSeriesIds = MutableStateFlow<Set<String>>(emptySet())
    private val sortMode = MutableStateFlow(SORT_DEFAULT)

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _seriesCount = MutableStateFlow(0)
    val seriesCount: StateFlow<Int> = _seriesCount.asStateFlow()

    private val _categoryCounts = MutableStateFlow<Map<String?, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<String?, Int>> = _categoryCounts.asStateFlow()

    private val _isLoadingMetadata = MutableStateFlow(false)
    val isLoadingMetadata: StateFlow<Boolean> = _isLoadingMetadata.asStateFlow()

    private val _metadataError = MutableStateFlow<String?>(null)
    val metadataError: StateFlow<String?> = _metadataError.asStateFlow()

    private var metadataRefreshJob: Job? = null

    // Um favorito só reinicia o Pager quando o filtro Favoritos estiver ativo.
    private val queryOptions = combine(
        searchQuery.settledCatalogSearch(), categoryFilter, favoriteSeriesIds, sortMode
    ) { currentSearch, currentCategory, currentFavorites, currentSort ->
        SeriesQueryOptions(
            selectionVersion = currentCategory.selectionVersion,
            categoryId = currentCategory.categoryId,
            searchQuery = currentSearch,
            favoritesOnly = currentCategory.favoritesOnly,
            favoriteSeriesIds = if (currentCategory.favoritesOnly) {
                currentFavorites
            } else {
                emptySet()
            },
            sortMode = currentSort
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val series: Flow<PagingData<Series>> = combine(
        playlistId, queryOptions
    ) { currentPlaylistId, options ->
        SeriesPagingRequest(currentPlaylistId, options)
    }
        .combine(catalogVisible) { request, visible -> request to visible }
        .flatMapLatest { (request, visible) ->
            val id = request.playlistId
            if (id == null) flowOf(Triple(request, null, visible))
            else repository.observeCatalogGeneration(id).map { generation -> Triple(request, generation, visible) }
        }
        .distinctUntilChanged()
        .flatMapLatest { (request, generation, visible) ->
            val explicitSelection = request.options.selectionVersion > 0
            br.com.flaviotorres.iptvpremium.data.cache.clearPagingBeforeLoad { 
            val currentPlaylistId = request.playlistId
            if (currentPlaylistId.isNullOrBlank() || !visible) {
                flowOf(PagingData.empty<Series>())
            } else {
                    val options = request.options
                    val full = explicitSelection || options.searchQuery.isNotBlank() || options.favoritesOnly
                    try {
                        val ready = repository.ensureOnDemand(currentPlaylistId,"SERIES",options.categoryId,full)
                        _selectionRequired.value = !ready
                        if(!ready) return@clearPagingBeforeLoad flowOf(PagingData.empty<Series>())
                        if (visible) refreshMetadata()
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) {
                        return@clearPagingBeforeLoad flowOf(PagingData.empty<Series>(sourceLoadStates = androidx.paging.LoadStates(
                            refresh=androidx.paging.LoadState.Error(error),
                            prepend=androidx.paging.LoadState.NotLoading(false),append=androidx.paging.LoadState.NotLoading(false))))
                    }
                repository.series(
                    playlistId = currentPlaylistId,
                    categoryId = request.options.categoryId,
                    searchQuery = request.options.searchQuery,
                    favoritesOnly = request.options.favoritesOnly,
                    favoriteSeriesIds = request.options.favoriteSeriesIds,
                    sortMode = request.options.sortMode
                )
            }
        
            }
        }
        .cachedIn(viewModelScope)

    fun setPlaylist(newPlaylistId: String) {
        val normalized = newPlaylistId.trim()
        if (normalized.isEmpty()) {
            clear()
            return
        }
        if (playlistId.value == normalized) return
        metadataRefreshJob?.cancel()
        categoryFilter.value = SeriesCategoryFilter()
        searchQuery.value = ""
        sortMode.value = SORT_DEFAULT
        clearMetadata()
        _selectionRequired.value = false
        playlistId.value = normalized
        refreshMetadata()
    }

    /** Ao escolher uma categoria, sai automaticamente do modo Favoritos. */
    fun selectCategory(categoryId: String?) {
        categoryFilter.value = SeriesCategoryFilter(
            selectionVersion = categoryFilter.value.selectionVersion + 1,
            categoryId = categoryId?.trim()?.takeIf { it.isNotEmpty() },
            favoritesOnly = false
        )
    }

    fun selectFavorites() {
        categoryFilter.value = SeriesCategoryFilter(favoritesOnly = true)
    }

    fun setFavoriteSeriesIds(seriesIds: Set<String>) {
        favoriteSeriesIds.value = seriesIds.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortMode(mode: String) {
        sortMode.value = when (mode.trim().uppercase()) {
            SORT_A_Z -> SORT_A_Z
            SORT_Z_A -> SORT_Z_A
            else -> SORT_DEFAULT
        }
    }

    internal suspend fun posterPage(offset: Int): br.com.flaviotorres.iptvpremium.data.cache.PosterPage {
        val playlist = playlistId.value ?: return br.com.flaviotorres.iptvpremium.data.cache.PosterPage(emptyList(), null)
        val filter = categoryFilter.value
        return repository.posterPage(br.com.flaviotorres.iptvpremium.data.cache.PosterKind.SERIES,
            playlist, filter.categoryId, searchQuery.value, filter.favoritesOnly, favoriteSeriesIds.value, sortMode.value, offset)
    }

    fun refreshMetadata() {
        val currentPlaylistId = playlistId.value ?: return
        metadataRefreshJob?.cancel()
        metadataRefreshJob = viewModelScope.launch {
            _isLoadingMetadata.value = true
            _metadataError.value = null
            try {
                val metadata = repository.getSeriesCategoryMetadata(currentPlaylistId)
                if (playlistId.value == currentPlaylistId) {
                    _categories.value = metadata.categories
                    _categoryCounts.value = metadata.counts
                    _seriesCount.value = metadata.total
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (playlistId.value == currentPlaylistId) {
                    _metadataError.value = error.message
                        ?: "Não foi possível carregar as séries."
                }
            } finally {
                if (playlistId.value == currentPlaylistId) {
                    _isLoadingMetadata.value = false
                }
            }
        }
    }

    fun clear() {
        metadataRefreshJob?.cancel()
        metadataRefreshJob = null
        playlistId.value = null
        categoryFilter.value = SeriesCategoryFilter()
        searchQuery.value = ""
        favoriteSeriesIds.value = emptySet()
        sortMode.value = SORT_DEFAULT
        clearMetadata()
    }

    private fun clearMetadata() {
        _categories.value = emptyList()
        _seriesCount.value = 0
        _categoryCounts.value = emptyMap()
        _isLoadingMetadata.value = false
        _metadataError.value = null
    }

    private companion object {
        const val SORT_DEFAULT = "DEFAULT"
        const val SORT_A_Z = "A_Z"
        const val SORT_Z_A = "Z_A"
    }
}
