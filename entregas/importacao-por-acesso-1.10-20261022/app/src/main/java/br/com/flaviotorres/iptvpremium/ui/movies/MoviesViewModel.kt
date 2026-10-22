package br.com.flaviotorres.iptvpremium.ui.movies

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.domain.model.Movie
import br.com.flaviotorres.iptvpremium.domain.model.MovieDetails
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.data.cache.settledCatalogSearch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pacote preparado para a transicao da grade para os detalhes.
 *
 * O filme basico vem do Room. Os detalhes remotos e as imagens
 * podem estar ausentes quando a rede nao responde dentro do prazo.
 *
 * Nao guarda bitmaps nem credenciais:
 * as imagens ficam no cache existente.
 */
data class PreparedMovieDetails(
    val playlistId: String,
    val movieId: String,
    val movie: Movie,
    val details: MovieDetails?,
    val images: MovieDetailsPreloadResult?
)

class MoviesViewModel(
    private val repository: PlaylistCatalogRepository
) : ViewModel() {

    private val playlistId =
        MutableStateFlow<String?>(null)

    // Categoria e modo Favoritos mudam em uma unica emissao.
    // Isso evita solicitar uma pagina intermediaria ao trocar o filtro.
    private val categoryFilter =
        MutableStateFlow(MovieCategoryFilter())

    /*
     * FILTROS E ORDENACAO DA GRADE
     *
     * Estes estados alimentam diretamente o Pager do Room.
     * Nenhuma filtragem do catalogo completo e feita em memoria.
     */
    private val searchQuery =
        MutableStateFlow("")

    private val favoriteMovieIds =
        MutableStateFlow<Set<String>>(
            emptySet()
        )

    private val sortMode =
        MutableStateFlow(SORT_DEFAULT)

    private val _categories =
        MutableStateFlow<List<Category>>(
            emptyList()
        )

    val categories: StateFlow<List<Category>> =
        _categories.asStateFlow()

    private val _movieCount =
        MutableStateFlow(0)

    val movieCount: StateFlow<Int> =
        _movieCount.asStateFlow()

    private val _categoryCounts =
        MutableStateFlow<Map<String?, Int>>(
            emptyMap()
        )

    val categoryCounts: StateFlow<Map<String?, Int>> =
        _categoryCounts.asStateFlow()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()

    /*
     * DETALHE BASICO DO FILME SELECIONADO
     *
     * Vem diretamente do Room e permanece leve.
     */
    private val _selectedMovie =
        MutableStateFlow<Movie?>(null)

    val selectedMovie: StateFlow<Movie?> =
        _selectedMovie.asStateFlow()

    /*
     * DETALHES ENRIQUECIDOS
     *
     * Sao carregados somente para o filme aberto.
     * Nenhum metadado enriquecido e buscado em massa.
     */
    private val _selectedMovieDetails =
        MutableStateFlow<MovieDetails?>(null)

    val selectedMovieDetails: StateFlow<MovieDetails?> =
        _selectedMovieDetails.asStateFlow()

    private val _isMovieDetailsLoading =
        MutableStateFlow(false)

    val isMovieDetailsLoading: StateFlow<Boolean> =
        _isMovieDetailsLoading.asStateFlow()

    private val _movieDetailsError =
        MutableStateFlow<String?>(null)

    val movieDetailsError: StateFlow<String?> =
        _movieDetailsError.asStateFlow()

    private var movieDetailsJob: Job? = null
    private var metadataRefreshJob: Job? = null

    /*
     * FILMES PAGINADOS
     *
     * O Pager reage a playlist, categoria, busca,
     * favoritos e ordenacao.
     *
     * Toda a filtragem continua sendo executada pelo SQLite.
     */
    private val movieQueryOptions =
        combine(
            searchQuery.settledCatalogSearch(),
            categoryFilter,
            favoriteMovieIds,
            sortMode
        ) {
                currentSearchQuery,
                currentFilter,
                currentFavoriteMovieIds,
                currentSortMode ->

            MovieQueryOptions(
                categoryId = currentFilter.categoryId,
                searchQuery = currentSearchQuery,
                favoritesOnly = currentFilter.favoritesOnly,
                // Alterar um favorito fora do filtro Favoritos nao
                // precisa reconstruir o Pager nem recarregar a grade.
                favoriteMovieIds = if (currentFilter.favoritesOnly) {
                    currentFavoriteMovieIds
                } else {
                    emptySet()
                },
                sortMode = currentSortMode
            )
        }

    val movies: Flow<PagingData<Movie>> =
        combine(
            playlistId,
            movieQueryOptions
        ) { currentPlaylistId, options ->
            MoviePagingRequest(
                playlistId = currentPlaylistId,
                options = options
            )
        }
            // Mudancas irrelevantes dos IDs favoritos e atualizacoes
            // repetidas nao devem abrir um novo fluxo de Paging.
            .distinctUntilChanged()
            .flatMapLatest { request ->
                val currentPlaylistId =
                    request.playlistId

                if (currentPlaylistId.isNullOrBlank()) {
                    flowOf(
                        PagingData.empty<Movie>()
                    )
                } else {
                    repository.movies(
                        playlistId = currentPlaylistId,
                        categoryId = request.options.categoryId,
                        searchQuery = request.options.searchQuery,
                        favoritesOnly = request.options.favoritesOnly,
                        favoriteMovieIds = request.options.favoriteMovieIds,
                        sortMode = request.options.sortMode
                    )
                }
            }
            .cachedIn(
                viewModelScope
            )

    /*
     * DEFINE A PLAYLIST DA GRADE
     *
     * Preservamos o funcionamento original.
     */
    fun setPlaylist(
        newPlaylistId: String
    ) {
        if (
            playlistId.value ==
            newPlaylistId
        ) {
            return
        }

        playlistId.value =
            newPlaylistId

        categoryFilter.value = MovieCategoryFilter()

        clearMovieDetails()
        refreshMetadata()
    }

    /*
     * PREPARACAO ANTES DA NAVEGACAO
     *
     * Deve ser chamada pela grade em uma coroutine cancelavel,
     * somente quando o usuario selecionar um filme.
     *
     * Enquanto ela executa, a grade continua visivel.
     *
     * 1. Busca um unico registro no Room;
     * 2. consulta Xtream e TMDB somente para esse filme;
     * 3. prepara backdrop, logo e as primeiras fotos do elenco
     *    usando os caches existentes.
     *
     * A navegacao sera conectada a este metodo
     * na proxima etapa do AppNavigation.
     */
    suspend fun prepareMovieDetails(
        movieId: String,
        source: PlaylistSource?,
        context: Context,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        detailsLoader: (
        suspend (
            PlaylistSource,
            String
        ) -> MovieDetails?
        )? = null
    ): PreparedMovieDetails? {

        val currentPlaylistId =
            playlistId.value
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null

        if (movieId.isBlank()) {
            return null
        }

        val startedAt =
            SystemClock.elapsedRealtime()

        /*
         * ETAPA 1 - ROOM
         *
         * Busca somente o filme selecionado.
         */
        val localMovie =
            try {
                repository.getMovieById(
                    playlistId = currentPlaylistId,
                    movieId = movieId
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(
                    MOVIE_DETAILS_PERF_TAG,
                    "Falha na preparacao local: " +
                            error.javaClass.simpleName
                )
                null
            } ?: return null

        val roomMs =
            SystemClock.elapsedRealtime() -
                    startedAt

        /*
         * ETAPA 2 - DADOS REMOTOS
         *
         * Xtream e TMDB continuam sob demanda.
         *
         * Um timeout impede que uma resposta remota muito lenta
         * mantenha a grade aguardando indefinidamente.
         */
        val remoteStartedAt =
            SystemClock.elapsedRealtime()

        val remoteDetails =
            if (
                source != null &&
                detailsLoader != null
            ) {
                try {
                    val completed = withTimeoutOrNull(
                        MovieOpeningPolicy.remoteMs.coerceAtMost(MovieOpeningPolicy.remainingMs(SystemClock.elapsedRealtime() - startedAt))
                    ) { Pair(true, detailsLoader(source, movieId)) } ?: return null
                    completed.second
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w(
                        MOVIE_DETAILS_PERF_TAG,
                        "Detalhes remotos indisponiveis: " +
                                error.javaClass.simpleName
                    )
                    return null
                }
            } else {
                null
            }

        val remoteMs =
            SystemClock.elapsedRealtime() -
                    remoteStartedAt

        /*
         * URLs VALIDAS
         *
         * Strings vazias nao impedem o fallback para os dados
         * que ja existem no catalogo local.
         */
        val backdropUrl =
            remoteDetails
                ?.backdropUrl
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: localMovie
                    .backdropUrl
                    ?.takeIf {
                        it.isNotBlank()
                    }

        /*
         * Regra do titulo:
         *
         * O logo oficial encontrado pelo enriquecimento TMDB
         * tem prioridade. O fallback visual em Cinzel dourado
         * permanece na MovieDetailsScreen.
         */
        val logoUrl =
            remoteDetails
                ?.titleLogoUrl
                ?.takeIf {
                    it.isNotBlank()
                }

        /*
         * Apenas fotos iniciais do elenco.
         *
         * Evita esperar downloads de todos os 20 atores
         * para abrir um unico filme.
         */
        val visibleCastUrls =
            remoteDetails
                ?.castMembers
                .orEmpty()
                .mapNotNull { member ->
                    member.profileUrl
                        ?.takeIf {
                            it.isNotBlank()
                        }
                }
                .take(INITIAL_CAST_PHOTOS)

        /*
         * ETAPA 3 - PREPARACAO DAS IMAGENS
         *
         * Usa preloadMovieDetailsImages(), ja adicionada
         * ao MovieDetailsScreen.kt na etapa anterior.
         *
         * Essa funcao utiliza os mesmos caches RAM e disco
         * da tela de detalhes.
         */
        val imageStartedAt =
            SystemClock.elapsedRealtime()

        val images =
            try {
                withTimeoutOrNull(
                    MovieOpeningPolicy.remainingMs(SystemClock.elapsedRealtime() - startedAt)
                ) {
                    preloadMovieDetailsImages(
                        context =
                            context.applicationContext,
                        backdropUrl =
                            backdropUrl,
                        titleLogoUrl =
                            logoUrl,
                        viewportWidthPx =
                            viewportWidthPx.coerceAtLeast(1),
                        viewportHeightPx =
                            viewportHeightPx.coerceAtLeast(1),
                        castProfileUrls =
                            visibleCastUrls
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(
                    MOVIE_DETAILS_PERF_TAG,
                    "Falha no preparo de imagens: " +
                            error.javaClass.simpleName
                )
                null
            }

        if (images == null) return null // Keep the loading/retry page instead of revealing unfinished work.

        /*
         * DIAGNOSTICO DE DESEMPENHO
         *
         * Nao registra URLs, usuarios ou senhas.
         */
        Log.i(
            MOVIE_DETAILS_PERF_TAG,
            "preparoRoomMs=$roomMs " +
                    "preparoRemotoMs=$remoteMs " +
                    "preparoImagensMs=${
                        SystemClock.elapsedRealtime() -
                                imageStartedAt
                    } " +
                    "preparoTotalMs=${
                        SystemClock.elapsedRealtime() -
                                startedAt
                    } " +
                    "backdropPronto=${
                        images?.backdropReady == true
                    } " +
                    "logoPronto=${
                        images?.titleLogoReady == true
                    }"
        )

        /*
         * O pacote contem os dados.
         *
         * As imagens permanecem nos caches;
         * nao copiamos bitmaps para o ViewModel.
         */
        return PreparedMovieDetails(
            playlistId = currentPlaylistId,
            movieId = movieId,
            movie = localMovie,
            details = remoteDetails,
            images = images
        )
    }

    /*
     * PLAYLIST ESPECIFICA DOS DETALHES
     *
     * Evita consultas de categorias e contadores,
     * desnecessarias nesta tela.
     *
     * setPlaylist() continua disponivel e inalterada
     * para a grade de Filmes.
     */
    fun setPlaylistForDetails(
        newPlaylistId: String
    ) {
        if (
            playlistId.value ==
            newPlaylistId
        ) {
            return
        }

        metadataRefreshJob?.cancel()
        metadataRefreshJob = null

        playlistId.value =
            newPlaylistId

        categoryFilter.value = MovieCategoryFilter()

        clearMetadata()
        clearMovieDetails()
    }

    /*
     * APLICA OS DETALHES PREPARADOS
     *
     * Reaproveita os dados sem repetir as consultas
     * ao Room, Xtream e TMDB.
     *
     * O AppNavigation chamara este metodo quando
     * a playlist e o filme corresponderem ao pacote.
     */
    fun applyPreparedMovieDetails(
        prepared: PreparedMovieDetails
    ) {
        if (
            playlistId.value !=
            prepared.playlistId
        ) {
            return
        }

        movieDetailsJob?.cancel()
        movieDetailsJob = null

        metadataRefreshJob?.cancel()
        metadataRefreshJob = null

        _selectedMovie.value =
            prepared.movie

        _selectedMovieDetails.value =
            prepared.details

        _isMovieDetailsLoading.value =
            false

        _movieDetailsError.value =
            null
    }

    /*
     * FILTRO DE CATEGORIAS
     *
     * null representa "Todos".
     */
    fun selectCategory(
        categoryId: String?
    ) {
        br.com.flaviotorres.iptvpremium.data.sync.CatalogImportAccess.request(playlistId.value, br.com.flaviotorres.iptvpremium.domain.model.CategoryType.MOVIES, categoryId)
        categoryFilter.value = MovieCategoryFilter(
            categoryId = categoryId?.takeIf { it.isNotBlank() },
            favoritesOnly = false
        )
    }

    fun selectFavorites() {
        categoryFilter.value = MovieCategoryFilter(
            categoryId = null,
            favoritesOnly = true
        )
    }

    fun setFavoriteMovieIds(
        movieIds: Set<String>
    ) {
        favoriteMovieIds.value =
            movieIds
                .asSequence()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .toSet()
    }

    fun setSearchQuery(
        query: String
    ) {
        searchQuery.value = query
    }

    fun setSortMode(
        mode: String
    ) {
        sortMode.value =
            when (
                mode
                    .trim()
                    .uppercase()
            ) {
                SORT_A_Z -> SORT_A_Z
                SORT_Z_A -> SORT_Z_A
                else -> SORT_DEFAULT
            }
    }

    /*
     * CARREGAMENTO TRADICIONAL DOS DETALHES
     *
     * Preservado para compatibilidade com a navegacao atual.
     *
     * Etapa 1: dados basicos do Room.
     * Etapa 2: detalhes enriquecidos sob demanda.
     *
     * Apos conectarmos a preparacao antecipada no AppNavigation,
     * sera possivel evitar a repeticao desse processo.
     */
    fun loadMovieDetails(
        movieId: String,
        source: PlaylistSource? = null,
        detailsLoader: (
        suspend (
            PlaylistSource,
            String
        ) -> MovieDetails?
        )? = null
    ) {
        val currentPlaylistId =
            playlistId.value

        if (currentPlaylistId.isNullOrBlank()) {
            _selectedMovie.value = null
            _selectedMovieDetails.value = null
            _isMovieDetailsLoading.value = false

            _movieDetailsError.value =
                "Nenhuma playlist ativa para carregar o filme."

            return
        }

        if (movieId.isBlank()) {
            _selectedMovie.value = null
            _selectedMovieDetails.value = null
            _isMovieDetailsLoading.value = false

            _movieDetailsError.value =
                "Filme inválido."

            return
        }

        movieDetailsJob?.cancel()

        /*
         * A rota de detalhes possui seu proprio ViewModel.
         *
         * Suas consultas de categorias e contadores
         * nao sao necessarias aqui.
         */
        metadataRefreshJob?.cancel()
        metadataRefreshJob = null

        val requestStartedAt =
            SystemClock.elapsedRealtime()

        movieDetailsJob =
            viewModelScope.launch {
                _isMovieDetailsLoading.value = true
                _movieDetailsError.value = null
                _selectedMovieDetails.value = null

                try {
                    /*
                     * ETAPA 1 - ROOM
                     */
                    val roomStartedAt =
                        SystemClock.elapsedRealtime()

                    val movie =
                        repository.getMovieById(
                            playlistId =
                                currentPlaylistId,
                            movieId =
                                movieId
                        )

                    _selectedMovie.value =
                        movie

                    Log.i(
                        MOVIE_DETAILS_PERF_TAG,
                        "roomMs=${
                            SystemClock.elapsedRealtime() -
                                    roomStartedAt
                        }, filmeLocal=${movie != null}"
                    )

                    if (movie == null) {
                        _movieDetailsError.value =
                            "Não foi possível localizar este filme no catálogo ativo."
                        return@launch
                    }

                    /*
                     * ETAPA 2 - REMOTO
                     *
                     * O carregador pode envolver Xtream e TMDB.
                     */
                    if (
                        source != null &&
                        detailsLoader != null
                    ) {
                        val remoteStartedAt =
                            SystemClock.elapsedRealtime()

                        val details =
                            detailsLoader(
                                source,
                                movieId
                            )

                        _selectedMovieDetails.value =
                            details

                        Log.i(
                            MOVIE_DETAILS_PERF_TAG,
                            "detalhesRemotosMs=${
                                SystemClock.elapsedRealtime() -
                                        remoteStartedAt
                            }, detalhesRecebidos=${details != null}"
                        )

                        /*
                         * Diagnostico TMDB existente.
                         */
                        Log.i(
                            MOVIE_DETAILS_DIAGNOSTIC_TAG,
                            buildString {
                                append(
                                    "MOVIE_DETAILS_IDS: movieId="
                                )
                                append(movieId)
                                append(", tmdbId=")
                                append(details?.tmdbId)
                                append(", imdbId=")
                                append(details?.imdbId)
                                append(", castNames=")
                                append(
                                    details?.cast?.size ?: 0
                                )
                                append(", castMembers=")
                                append(
                                    details?.castMembers?.size ?: 0
                                )
                                append(", titleLogo=")
                                append(
                                    if (
                                        details
                                            ?.titleLogoUrl
                                            .isNullOrBlank()
                                    ) {
                                        "NAO"
                                    } else {
                                        "SIM"
                                    }
                                )
                            }
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Log.e(
                        MOVIE_DETAILS_PERF_TAG,
                        "Falha ao carregar detalhes após ${
                            SystemClock.elapsedRealtime() -
                                    requestStartedAt
                        } ms",
                        error
                    )

                    /*
                     * Preserva dados basicos do Room
                     * se o enriquecimento remoto falhar.
                     */
                    if (_selectedMovie.value == null) {
                        _selectedMovie.value = null
                    }

                    _selectedMovieDetails.value = null

                    _movieDetailsError.value =
                        error.message
                            ?: "Não foi possível carregar os detalhes do filme."
                } finally {
                    _isMovieDetailsLoading.value = false

                    Log.i(
                        MOVIE_DETAILS_PERF_TAG,
                        "totalMs=${
                            SystemClock.elapsedRealtime() -
                                    requestStartedAt
                        }"
                    )
                }
            }
    }

    fun clearMovieDetails() {
        movieDetailsJob?.cancel()
        movieDetailsJob = null

        _selectedMovie.value = null
        _selectedMovieDetails.value = null
        _isMovieDetailsLoading.value = false
        _movieDetailsError.value = null
    }

    /*
     * METADADOS DA GRADE DE FILMES
     *
     * Categorias e contagens sao consultas SQL.
     * Nao carregamos o catalogo inteiro em memoria.
     *
     * Cancelamos uma consulta anterior antes de iniciar outra.
     */
    internal suspend fun posterPage(offset: Int): br.com.flaviotorres.iptvpremium.data.cache.PosterPage {
        val playlist = playlistId.value ?: return br.com.flaviotorres.iptvpremium.data.cache.PosterPage(emptyList(), null)
        val filter = categoryFilter.value
        return repository.posterPage(br.com.flaviotorres.iptvpremium.data.cache.PosterKind.MOVIES,
            playlist, filter.categoryId, searchQuery.value, filter.favoritesOnly, favoriteMovieIds.value, sortMode.value, offset)
    }

    fun refreshMetadata() {
        val currentPlaylistId =
            playlistId.value
                ?: return clearMetadata()

        metadataRefreshJob?.cancel()

        metadataRefreshJob =
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null

                try {
                    _categories.value =
                        repository.getMovieCategories(
                            playlistId =
                                currentPlaylistId
                        )

                    _movieCount.value =
                        repository.getMovieCount(
                            playlistId =
                                currentPlaylistId
                        )

                    _categoryCounts.value =
                        repository.getMovieCountsByCategory(
                            playlistId =
                                currentPlaylistId
                        )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    _categories.value =
                        emptyList()

                    _movieCount.value = 0

                    _categoryCounts.value =
                        emptyMap()

                    _errorMessage.value =
                        error.message
                            ?: "Não foi possível carregar os filmes."
                } finally {
                    _isLoading.value = false
                }
            }
    }

    fun clear() {
        metadataRefreshJob?.cancel()
        metadataRefreshJob = null

        playlistId.value = null
        categoryFilter.value = MovieCategoryFilter()
        searchQuery.value = ""
        favoriteMovieIds.value = emptySet()
        sortMode.value = SORT_DEFAULT

        clearMetadata()
        clearMovieDetails()
    }

    private fun clearMetadata() {
        _categories.value =
            emptyList()

        _movieCount.value = 0

        _categoryCounts.value =
            emptyMap()

        _isLoading.value = false
        _errorMessage.value = null
    }

    private data class MovieCategoryFilter(
        val categoryId: String? = null,
        val favoritesOnly: Boolean = false
    )

    private data class MovieQueryOptions(
        val categoryId: String?,
        val searchQuery: String,
        val favoritesOnly: Boolean,
        val favoriteMovieIds: Set<String>,
        val sortMode: String
    )

    private data class MoviePagingRequest(
        val playlistId: String?,
        val options: MovieQueryOptions
    )

    companion object {
        private const val SORT_DEFAULT = "DEFAULT"
        private const val SORT_A_Z = "A_Z"
        private const val SORT_Z_A = "Z_A"

        private const val MOVIE_DETAILS_DIAGNOSTIC_TAG =
            "MovieDetailsDiagnostic"

        private const val MOVIE_DETAILS_PERF_TAG =
            "MovieDetailsPerf"

        private const val INITIAL_CAST_PHOTOS =
            6
    }
}
