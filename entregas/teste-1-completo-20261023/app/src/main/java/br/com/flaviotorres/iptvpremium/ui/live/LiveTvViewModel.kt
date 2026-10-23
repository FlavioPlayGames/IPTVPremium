package br.com.flaviotorres.iptvpremium.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.domain.model.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LiveTvViewModel(
    private val repository: PlaylistCatalogRepository
) : ViewModel() {

    private val playlistId =
        MutableStateFlow<String?>(null)

    private val selectedCategoryId =
        MutableStateFlow<String?>(null)

    private val _categories =
        MutableStateFlow<List<Category>>(emptyList())

    val categories: StateFlow<List<Category>> =
        _categories.asStateFlow()

    private val _channelCount =
        MutableStateFlow(0)

    val channelCount: StateFlow<Int> =
        _channelCount.asStateFlow()

    private val _categoryCounts =
        MutableStateFlow<Map<String?, Int>>(emptyMap())

    val categoryCounts: StateFlow<Map<String?, Int>> =
        _categoryCounts.asStateFlow()

    private val _isLoadingMetadata =
        MutableStateFlow(false)

    val isLoadingMetadata: StateFlow<Boolean> =
        _isLoadingMetadata.asStateFlow()

    private val _metadataError =
        MutableStateFlow<String?>(null)

    val metadataError: StateFlow<String?> =
        _metadataError.asStateFlow()

    /*
     * O Paging observa playlist + categoria.
     *
     * Quando a categoria muda, o Room fornece um novo PagingSource.
     * Todo o catálogo permanece armazenado no banco e somente as páginas
     * necessárias para a interface ficam na memória.
     */
    private data class BrowseFilter(val query:String="",val sort:Int=0,val ids:Set<String>?=null)
    private val browseFilter = MutableStateFlow(BrowseFilter())
    fun browse(query:String,sort:Int,ids:Set<String>?) { browseFilter.value=BrowseFilter(query,sort,ids) }
    val channels: Flow<PagingData<Channel>> =
        playlistId
            .flatMapLatest { currentPlaylistId ->
                if (currentPlaylistId.isNullOrBlank()) {
                    emptyFlow()
                } else {
                    combine(selectedCategoryId,browseFilter) { category, filter -> category to filter }
                        .distinctUntilChanged()
                        .flatMapLatest { (categoryId, filter) ->
                            repository.channels(
                                playlistId = currentPlaylistId,
                                categoryId = categoryId,
                                query = filter.query, sort = filter.sort, ids = filter.ids
                            )
                        }
                }
            }
            .cachedIn(viewModelScope)

    fun setPlaylist(
        playlistId: String
    ) {
        if (
            playlistId.isBlank() ||
            this.playlistId.value == playlistId
        ) {
            return
        }

        this.playlistId.value = playlistId
        selectedCategoryId.value = null
        browseFilter.value = BrowseFilter()

        refreshMetadata()
    }

    fun selectCategory(
        categoryId: String?
    ) {
        selectedCategoryId.value =
            categoryId
                ?.takeIf {
                    it.isNotBlank()
                }
    }

    private var metadataRefreshJob: Job? = null

    fun refreshMetadata(silent:Boolean=false) {
        val currentPlaylistId =
            playlistId.value
                ?: return

        metadataRefreshJob?.cancel()
        metadataRefreshJob = viewModelScope.launch {
            if(!silent) _isLoadingMetadata.value = true
            _metadataError.value = null

            runCatching {
                val categories =
                    repository.getLiveTvCategories(
                        playlistId = currentPlaylistId
                    )

                val totalCount =
                    repository.getChannelCount(
                        playlistId = currentPlaylistId
                    )

                val counts =
                    repository.getChannelCountsByCategory(
                        playlistId = currentPlaylistId
                    )

                Triple(
                    categories,
                    totalCount,
                    counts
                )
            }.onSuccess {
                    result ->

                if (playlistId.value != currentPlaylistId) return@onSuccess
                _categories.value =
                    result.first

                _channelCount.value =
                    result.second

                _categoryCounts.value =
                    result.third
            }.onFailure {
                    error ->

                if (error is CancellationException) throw error
                _metadataError.value =
                    error.message
                        ?: "Não foi possível carregar os canais."
            }

            _isLoadingMetadata.value = false
        }
    }

    fun clear() {
        metadataRefreshJob?.cancel()
        metadataRefreshJob = null
        playlistId.value = null
        selectedCategoryId.value = null
        _categories.value = emptyList()
        _channelCount.value = 0
        _categoryCounts.value = emptyMap()
        _metadataError.value = null
        _isLoadingMetadata.value = false
    }
}
