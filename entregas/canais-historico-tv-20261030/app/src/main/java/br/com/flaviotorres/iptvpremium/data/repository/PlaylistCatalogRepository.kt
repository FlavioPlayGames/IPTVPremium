package br.com.flaviotorres.iptvpremium.data.repository

import br.com.flaviotorres.iptvpremium.ui.kids.CatalogSections
import br.com.flaviotorres.iptvpremium.ui.kids.KidsContent
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.paging.filter
import kotlinx.coroutines.flow.combine
import br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy
import br.com.flaviotorres.iptvpremium.ui.settings.ParentalAccessState
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedChannelEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedMovieEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedSeriesEntity
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.domain.model.CategorySource
import br.com.flaviotorres.iptvpremium.domain.model.CategoryType
import br.com.flaviotorres.iptvpremium.domain.model.Channel
import br.com.flaviotorres.iptvpremium.domain.model.Movie
import br.com.flaviotorres.iptvpremium.domain.model.RatingSource
import br.com.flaviotorres.iptvpremium.domain.model.Series
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal data class CatalogCategoryMetadata(
    val categories: List<Category>,
    val counts: Map<String?, Int>,
    val total: Int
)

// GROUP BY includes unassigned categories; no second COUNT(*) scan is necessary.
internal fun catalogCategoryTotal(counts: Map<String?, Int>, kids: Boolean): Int =
    counts.asSequence().filter { !kids || it.key != CatalogSections.SUBTITLED }.sumOf { it.value }

internal data class ParentalTarget(val categoryId: String?)

class PlaylistCatalogRepository(
    private val cacheDao: PlaylistCacheDao,
    context: android.content.Context? = null
) {

    private val appContext = context?.applicationContext
    private val categoryCountCache = br.com.flaviotorres.iptvpremium.data.cache.CatalogCategoryCountCache(
        appContext?.cacheDir?.let { java.io.File(it, "catalog_category_counts_v1") },
        storage = { appContext?.let { br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(it) } },
        report = { type, source, groups, elapsed ->
            Log.i("CategoryCountCachePerf", "kind=$type source=$source groups=$groups totalMs=$elapsed")
        },
        reportStage = { type, stage, elapsed ->
            Log.d("CategoryCountStagePerf", "kind=$type stage=$stage totalMs=$elapsed")
        }
    )

    /*
     * CANAIS
     *
     * Todo o catálogo permanece no Room.
     * O Paging solicita somente os registros necessários para a tela.
     *
     * Agora observa a geração ACTIVE.
     *
     * Quando a primeira importação terminar, o Room notificará
     * o Flow e um novo Pager será criado automaticamente.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun channels(
        playlistId: String,
        categoryId: String? = null,
        query: String = "",
        sort: Int = 0,
        ids: Set<String>? = null
    ): Flow<PagingData<Channel>> {
        return cacheDao
            .observeActiveGenerationId(playlistId)
            .distinctUntilChanged()
            .combine(ParentalPolicy.accessFlow) { generation, access -> generation to access }
            .flatMapLatest { (generationId, access) ->
                if (generationId == null) {
                    flowOf(PagingData.empty<Channel>())
                } else {
                    val categoryNames = categoryNames(playlistId, generationId)
                    val parentalSql = parentalSql(access, playlistId, "LIVE_TV", categoryNames)
                    Pager(
                        config = pagingConfig(),
                        pagingSourceFactory = {
                            val pattern = query.trim().takeIf { it.isNotEmpty() }?.let {
                                "%" + it.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
                            }.orEmpty()
                            cacheDao.filteredChannels(playlistId,generationId,categoryId,pattern,sort,
                                ids != null,ids.orEmpty().toList(),parentalSql.excluded,parentalSql.allowed,parentalSql.restrict)
                        }
                    ).flow.map { pagingData ->
                        pagingData.map { entity ->
                            entity.toDomain()
                        }.filter { !access.blocked(playlistId, "LIVE_TV", it.categoryId, it.name, categoryNames[it.categoryId].orEmpty()) }
                    }
                }
            }.flowOn(Dispatchers.IO)
    }

    /** Read only the first allowed row in Todos, independent of the current UI filter. */
    suspend fun getFirstAllowedChannel(playlistId: String): Channel? = catalogRead("getFirstAllowedChannel") {
        val generation = getActiveGenerationId(playlistId) ?: return@catalogRead null
        val access = ParentalPolicy.access()
        val names = categoryNames(playlistId, generation)
        val sql = parentalSql(access, playlistId, "LIVE_TV", names)
        val source = cacheDao.filteredChannels(playlistId, generation, null, "", 0,
            false, emptyList(), sql.excluded, sql.allowed, sql.restrict)
        try {
            var key: Int? = null
            do {
                val result = source.load(androidx.paging.PagingSource.LoadParams.Refresh(
                    key = key, loadSize = 32, placeholdersEnabled = false))
                when (result) {
                    is androidx.paging.PagingSource.LoadResult.Page -> {
                        result.data.asSequence().map { it.toDomain() }.firstOrNull {
                            it.streamUrl.isNotBlank() &&
                                (!access.kidsCatalog || KidsContent.category(names[it.categoryId].orEmpty())) &&
                                !access.blocked(playlistId, "LIVE_TV", it.categoryId, it.name, names[it.categoryId].orEmpty())
                        }?.let { return@catalogRead it }
                        key = result.nextKey
                    }
                    is androidx.paging.PagingSource.LoadResult.Error -> throw result.throwable
                    else -> return@catalogRead null
                }
            } while (key != null)
            null
        } finally { source.invalidate() }
    }

    /**
     * Busca apenas o canal solicitado na geracao ACTIVE, sem varrer
     * as paginas do catalogo nem expor o endereco na navegacao.
     */
    suspend fun getChannelById(
        playlistId: String,
        channelId: String
    ): Channel? = catalogRead("getChannelById") {
        if (playlistId.isBlank() || channelId.isBlank()) {
            return@catalogRead null
        }

        val generationId = getActiveGenerationId(playlistId)
            ?: return@catalogRead null

        return@catalogRead cacheDao.getChannelById(
            playlistId = playlistId,
            generationId = generationId,
            channelId = channelId
        )?.toDomain()?.takeUnless {
            val access=ParentalPolicy.access()
            val name=categoryNames(playlistId,generationId)[it.categoryId].orEmpty()
            (access.kidsCatalog && !KidsContent.category(name)) ||
                access.blocked(playlistId, "LIVE_TV", it.categoryId, it.name, name)
        }
    }

    /*
     * TROCA DE CANAL PELOS BOTOES CH+ / CH- DA TV
     *
     * direction = +1: canal seguinte; -1: canal anterior.
     * Consulta apenas um registro da geracao ACTIVE no Room, sem
     * percorrer o Paging nem carregar todos os canais na memoria.
     * O DAO faz a volta do ultimo ao primeiro (ou do primeiro ao ultimo).
     * Se a playlist tiver um unico canal reproduzivel ou o canal atual
     * nao existir, retorna null; a navegacao decide como reagir.
     */
    suspend fun getAdjacentChannel(playlistId: String, currentChannelId: String, direction: Int): Channel? = catalogRead("getAdjacentChannel") {
        var cursor = currentChannelId
        val seen = hashSetOf(currentChannelId)
        repeat(64) {
            val next = getAdjacentChannelCandidate(playlistId, cursor, direction) ?: return@catalogRead null
            if (!seen.add(next.id)) return@catalogRead null
            if (!isParentalBlocked(playlistId, "LIVE_TV", next.id)) return@catalogRead next
            cursor = next.id
        }
        return@catalogRead null
    }

    /** No imports or URL probes: candidates are streamed lazily from existing Room generations. */
    fun channelAlternatives(playlistId:String,origin:Channel,categoryOnly:Boolean):Flow<Channel> = kotlinx.coroutines.flow.flow {
        val generation=getActiveGenerationId(playlistId)?:return@flow
        val names=categoryNames(playlistId,generation)
        val token=br.com.flaviotorres.iptvpremium.domain.model.ChannelIdentity.searchToken(origin)
        var offset=0
        do {
            val page=cacheDao.findChannelAlternatives(playlistId,generation,categoryOnly,origin.categoryId,
                token,origin.epgChannelId.orEmpty(),128,offset)
            for(entity in page) {
                val candidate=entity.toDomain()
                val access=ParentalPolicy.access()
                val category=names[candidate.categoryId].orEmpty()
                if(candidate.streamUrl.isNotBlank() && br.com.flaviotorres.iptvpremium.domain.model.ChannelIdentity.same(origin,candidate) &&
                    (!access.kidsCatalog||KidsContent.category(category)) &&
                    !access.blocked(playlistId,"LIVE_TV",candidate.categoryId,candidate.name,category))emit(candidate)
            }
            offset+=page.size
        }while(page.size==128)
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    suspend fun channelNeighbours(playlistId:String,currentId:String,direction:Int,categoryId:String?,categoryOnly:Boolean,
        ids:Set<String>?,limit:Int=3):List<Channel> = catalogRead("channelNeighbours") {
        val generation=getActiveGenerationId(playlistId)?:return@catalogRead emptyList()
        val access=ParentalPolicy.access();val names=categoryNames(playlistId,generation)
        val sql=parentalSql(access,playlistId,"LIVE_TV",names)
        return@catalogRead cacheDao.getChannelNeighbours(playlistId,generation,currentId,direction,categoryOnly,categoryId,
            ids!=null,ids.orEmpty().toList(),sql.excluded,sql.allowed,sql.restrict,32).map{it.toDomain()}.filter {
            (!access.kidsCatalog||KidsContent.category(names[it.categoryId].orEmpty())) &&
                !access.blocked(playlistId,"LIVE_TV",it.categoryId,it.name,names[it.categoryId].orEmpty())
        }.take(limit)
    }

    private suspend fun getAdjacentChannelCandidate(
        playlistId: String,
        currentChannelId: String,
        direction: Int
    ): Channel? = catalogRead("getAdjacentChannelCandidate") {
        if (playlistId.isBlank() || currentChannelId.isBlank()) {
            return@catalogRead null
        }
        if (direction != 1 && direction != -1) {
            return@catalogRead null
        }

        val generationId = getActiveGenerationId(playlistId)
            ?: return@catalogRead null

        val access=ParentalPolicy.access()
        val kidsAllowed=if(access.kidsCatalog) categoryNames(playlistId,generationId)
            .filterValues { KidsContent.category(it) }.keys.toList() else emptyList()
        val entity = if (direction > 0) {
            cacheDao.getNextPlayableChannel(
                playlistId = playlistId,
                generationId = generationId,
                channelId = currentChannelId,
                kidsOnly = access.kidsCatalog,
                kidsCategoryIds = kidsAllowed
            )
        } else {
            cacheDao.getPreviousPlayableChannel(
                playlistId = playlistId,
                generationId = generationId,
                channelId = currentChannelId,
                kidsOnly = access.kidsCatalog,
                kidsCategoryIds = kidsAllowed
            )
        }

        return@catalogRead entity?.toDomain()
    }

    /*
     * FILMES
     *
     * Busca, categoria, favoritos e ordenação são aplicados diretamente
     * pelo SQLite antes de o Paging entregar os itens para a interface.
     *
     * Os valores padrão preservam compatibilidade com chamadas antigas.
     *
     * A geração ACTIVE agora é observada continuamente.
     * Quando uma nova geração for ativada, o Pager passa a utilizá-la.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun movies(
        playlistId: String,
        categoryId: String? = null,
        searchQuery: String = "",
        favoritesOnly: Boolean = false,
        favoriteMovieIds: Set<String> = emptySet(),
        sortMode: String = SORT_DEFAULT
    ): Flow<PagingData<Movie>> {
        val subtitledOnly = false // Dedicated subtitle tab removed; legacy saved ID resolves to All.
        val normalizedCategoryId =
            categoryId
                ?.trim()
                ?.takeIf { it.isNotBlank() && it != CatalogSections.SUBTITLED }

        val normalizedSearchQuery = searchQuery.trim()

        val normalizedSortMode =
            when (sortMode.trim().uppercase()) {
                SORT_A_Z -> SORT_A_Z
                SORT_Z_A -> SORT_Z_A
                else -> SORT_DEFAULT
            }

        /*
         * Room/SQLite recebe sempre pelo menos um valor no parâmetro IN.
         *
         * O sentinela não corresponde a nenhum movieId real e é ignorado
         * pelo SQL quando favoritesOnly=false.
         */
        val normalizedFavoriteMovieIds =
            if (favoritesOnly) {
                favoriteMovieIds
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList()
                    .ifEmpty { listOf(NO_FAVORITE_MOVIE_ID) }
            } else {
                // Sem o filtro de favoritos, não percorremos todo o conjunto
                // só para criar um parâmetro IN que o SQLite ignorará.
                listOf(NO_FAVORITE_MOVIE_ID)
            }

        // A navegação normal dispensa os CASE e filtros opcionais da
        // consulta genérica. As consultas especializadas já estão no DAO
        // e conservam exatamente a ordenação DEFAULT do provedor.
        val useSimplePagingQuery =
            !subtitledOnly && !favoritesOnly &&
                    normalizedSearchQuery.isEmpty() &&
                    normalizedSortMode == SORT_DEFAULT

        return cacheDao
            .observeActiveGenerationId(playlistId)
            .distinctUntilChanged()
            .combine(ParentalPolicy.accessFlow) { generation, access -> generation to access }
            .flatMapLatest { (generationId, access) ->
                br.com.flaviotorres.iptvpremium.data.cache.clearPagingBeforeLoad { 
                if (generationId == null) {
                    flowOf(PagingData.empty<Movie>())
                } else {
                    val categoryNames = categoryNames(playlistId, generationId, "MOVIES")
                    val parentalSql = parentalSql(access, playlistId, "MOVIES", categoryNames)
                    Pager(
                        config = pagingConfig(grid = true),
                        pagingSourceFactory = {
                            if (useSimplePagingQuery) {
                                if (normalizedCategoryId == null) {
                                    cacheDao.getMoviesPagingSource(
                                        playlistId = playlistId,
                                        generationId = generationId,
                                    parentalExcludedIds = parentalSql.excluded,
                                    parentalAllowedIds = parentalSql.allowed,
                                    parentalRestrict = parentalSql.restrict
                                )
                                } else {
                                    cacheDao.getMoviesPagingSourceByCategory(
                                        playlistId = playlistId,
                                        generationId = generationId,
                                        categoryId = normalizedCategoryId,
                                    parentalExcludedIds = parentalSql.excluded,
                                    parentalAllowedIds = parentalSql.allowed,
                                    parentalRestrict = parentalSql.restrict
                                )
                                }
                            } else {
                                cacheDao.getMoviesPagingSourceFiltered(
                                    playlistId = playlistId,
                                    generationId = generationId,
                                    categoryId = normalizedCategoryId,
                                    searchQuery = normalizedSearchQuery,
                                    favoritesOnly = favoritesOnly,
                                    favoriteMovieIds = normalizedFavoriteMovieIds,
                                    sortMode = normalizedSortMode,
                                    subtitledOnly = subtitledOnly,
                                    parentalExcludedIds = parentalSql.excluded,
                                    parentalAllowedIds = parentalSql.allowed,
                                    parentalRestrict = parentalSql.restrict
                                )
                            }
                        }
                    ).flow.map { pagingData ->
                        pagingData.map { entity ->
                            entity.toDomain()
                        }.filter { !access.blocked(playlistId, "MOVIES", it.categoryId, it.name, categoryNames[it.categoryId].orEmpty()) }
                    }
                }
            
                }
            }.flowOn(Dispatchers.IO)
    }

    /*
     * SÉRIES — MESMA ESTRATÉGIA PAGINADA DOS FILMES.
     * O filtro de favoritos opera no SQLite; mudanças de busca/filtro
     * não carregam toda a biblioteca para a memória.
     * DEFAULT mantém a ordenação já existente: por nome.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun series(
        playlistId: String,
        categoryId: String? = null,
        searchQuery: String = "",
        favoritesOnly: Boolean = false,
        favoriteSeriesIds: Set<String> = emptySet(),
        sortMode: String = SORT_DEFAULT
    ): Flow<PagingData<Series>> {
        val subtitledOnly = false // Dedicated subtitle tab removed; legacy saved ID resolves to All.
        val normalizedCategoryId = categoryId?.trim()?.takeIf { it.isNotEmpty() && it != CatalogSections.SUBTITLED }
        val normalizedSearchQuery = searchQuery.trim()
        val normalizedSortMode = when (sortMode.trim().uppercase()) {
            SORT_A_Z -> SORT_A_Z
            SORT_Z_A -> SORT_Z_A
            else -> SORT_DEFAULT
        }
        val normalizedFavorites = if (favoritesOnly) {
            favoriteSeriesIds.asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
                .ifEmpty { listOf(NO_FAVORITE_SERIES_ID) }
        } else {
            listOf(NO_FAVORITE_SERIES_ID)
        }
        val useSimpleQuery = !subtitledOnly && !favoritesOnly &&
                normalizedSearchQuery.isEmpty() &&
                normalizedSortMode == SORT_DEFAULT

        return cacheDao.observeActiveGenerationId(playlistId)
            .distinctUntilChanged()
            .combine(ParentalPolicy.accessFlow) { generation, access -> generation to access }
            .flatMapLatest { (generationId, access) ->
                br.com.flaviotorres.iptvpremium.data.cache.clearPagingBeforeLoad { 
                if (generationId == null) {
                    flowOf(PagingData.empty<Series>())
                } else {
                    val categoryNames = categoryNames(playlistId, generationId, "SERIES")
                    val parentalSql = parentalSql(access, playlistId, "SERIES", categoryNames)
                    Pager(
                        config = pagingConfig(grid = true),
                        pagingSourceFactory = {
                            if (useSimpleQuery) {
                                if (normalizedCategoryId == null) {
                                    cacheDao.getSeriesPagingSource(
                                        playlistId = playlistId,
                                        generationId = generationId,
                                    parentalExcludedIds = parentalSql.excluded,
                                    parentalAllowedIds = parentalSql.allowed,
                                    parentalRestrict = parentalSql.restrict
                                )
                                } else {
                                    cacheDao.getSeriesPagingSourceByCategory(
                                        playlistId = playlistId,
                                        generationId = generationId,
                                        categoryId = normalizedCategoryId,
                                    parentalExcludedIds = parentalSql.excluded,
                                    parentalAllowedIds = parentalSql.allowed,
                                    parentalRestrict = parentalSql.restrict
                                )
                                }
                            } else {
                                cacheDao.getSeriesPagingSourceFiltered(
                                    playlistId = playlistId,
                                    generationId = generationId,
                                    categoryId = normalizedCategoryId,
                                    searchQuery = normalizedSearchQuery,
                                    favoritesOnly = favoritesOnly,
                                    favoriteSeriesIds = normalizedFavorites,
                                    sortMode = normalizedSortMode,
                                    subtitledOnly = subtitledOnly,
                                    parentalExcludedIds = parentalSql.excluded,
                                    parentalAllowedIds = parentalSql.allowed,
                                    parentalRestrict = parentalSql.restrict
                                )
                            }
                        }
                    ).flow.map { pagingData ->
                        pagingData.map { entity -> entity.toDomain() }.filter { !access.blocked(playlistId, "SERIES", it.categoryId, it.name, categoryNames[it.categoryId].orEmpty()) }
                    }
                }
            
                }
            }.flowOn(Dispatchers.IO)
    }

    /** Independent five-row query: never advances or drops pages from the visible grid. */
    internal suspend fun posterPage(kind: br.com.flaviotorres.iptvpremium.data.cache.PosterKind,
        playlist: String, category: String?, search: String, favorites: Boolean, ids: Set<String>,
        sort: String, offset: Int): br.com.flaviotorres.iptvpremium.data.cache.PosterPage =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val generation = getActiveGenerationId(playlist)
                ?: return@withContext br.com.flaviotorres.iptvpremium.data.cache.PosterPage(emptyList(), null)
            val type = if (kind == br.com.flaviotorres.iptvpremium.data.cache.PosterKind.MOVIES) "MOVIES" else "SERIES"
            val names = categoryNames(playlist, generation, type)
            val access = ParentalPolicy.access()
            val policy = parentalSql(access, playlist, type, names)
            val categoryId = category?.takeIf { it.isNotBlank() && it != CatalogSections.SUBTITLED }
            val selectedIds = if (favorites) ids.toList().ifEmpty { listOf("__no_favorites__") } else listOf("__no_favorites__")
            val mode = sort.takeIf { it in listOf(SORT_A_Z, SORT_Z_A) } ?: SORT_DEFAULT
            suspend fun <T : Any> load(source: androidx.paging.PagingSource<Int, T>,
                allowed: (T) -> Boolean, url: (T) -> String?): br.com.flaviotorres.iptvpremium.data.cache.PosterPage {
                try {
                    // Room Refresh shifts the final short page backwards to fill loadSize.
                    // This is a forward-only scan: Append preserves the exact requested offset.
                    val request = androidx.paging.PagingSource.LoadParams.Append(
                        key = offset.coerceAtLeast(0), loadSize = 50, placeholdersEnabled = true)
                    return when (val result = source.load(request)) {
                        is androidx.paging.PagingSource.LoadResult.Page -> br.com.flaviotorres.iptvpremium.data.cache.PosterPage(
                            result.data.filter(allowed).map(url), result.nextKey)
                        is androidx.paging.PagingSource.LoadResult.Error -> throw result.throwable
                        is androidx.paging.PagingSource.LoadResult.Invalid -> throw java.io.IOException("Catalog updated")
                    }
                } finally { source.invalidate() }
            }
            if (type == "MOVIES") load(cacheDao.getMoviesPagingSourceFiltered(playlist, generation, categoryId,
                search.trim(), favorites, selectedIds, mode, false,
                policy.excluded, policy.allowed, policy.restrict),
                { !access.blocked(playlist, type, it.categoryId, it.name, names[it.categoryId].orEmpty()) }, { it.posterUrl })
            else load(cacheDao.getSeriesPagingSourceFiltered(playlist, generation, categoryId,
                search.trim(), favorites, selectedIds, mode, false,
                policy.excluded, policy.allowed, policy.restrict),
                { !access.blocked(playlist, type, it.categoryId, it.name, names[it.categoryId].orEmpty()) }, { it.posterUrl })
        }

    /*
     * CATEGORIAS
     *
     * Categorias são pequenas em comparação com o catálogo e podem ser
     * lidas diretamente sem Paging.
     */
    suspend fun getLiveTvCategories(
        playlistId: String
    ): List<Category> = catalogRead("getLiveTvCategories") {
        return@catalogRead getCategories(
            playlistId = playlistId,
            type = TYPE_LIVE_TV,
            categoryType = CategoryType.LIVE_TV
        )
    }

    suspend fun getMovieCategories(
        playlistId: String
    ): List<Category> = catalogRead("getMovieCategories") {
        return@catalogRead getCategories(
            playlistId = playlistId,
            type = TYPE_MOVIES,
            categoryType = CategoryType.MOVIES
        )
    }

    suspend fun getSeriesCategories(
        playlistId: String
    ): List<Category> = catalogRead("getSeriesCategories") {
        return@catalogRead getCategories(
            playlistId = playlistId,
            type = TYPE_SERIES,
            categoryType = CategoryType.SERIES
        )
    }

    internal suspend fun getMovieCategoryMetadata(playlistId: String): CatalogCategoryMetadata =
        categoryMetadata(playlistId, TYPE_MOVIES)

    internal suspend fun getSeriesCategoryMetadata(playlistId: String): CatalogCategoryMetadata =
        categoryMetadata(playlistId, TYPE_SERIES)

    private suspend fun categoryMetadata(playlistId: String, type: String): CatalogCategoryMetadata =
        catalogRead("categoryMetadata_$type") {
            val generation = getActiveGenerationId(playlistId)
                ?: return@catalogRead CatalogCategoryMetadata(emptyList(), emptyMap(), 0)
            val access = ParentalPolicy.access()
            val counts = categoryCounts(playlistId, generation, type, access)
            val categories = getCategories(playlistId, type,
                if (type == TYPE_MOVIES) CategoryType.MOVIES else CategoryType.SERIES,
                generation, access).let { all ->
                    if (type == TYPE_SERIES) all.filter { (counts[it.id] ?: 0) > 0 } else all
                }
            CatalogCategoryMetadata(categories, counts, catalogCategoryTotal(counts,
                access.kidsCatalog || (access.enabled && access.rules.kidsOnly)))
        }

    /*
     * CONTADORES GERAIS
     *
     * COUNT(*) é executado pelo SQLite, sem trazer os registros para RAM.
     */
    suspend fun getChannelCount(
        playlistId: String
    ): Int = catalogRead("getChannelCount") {
        if (ParentalPolicy.access().kidsCatalog || (ParentalPolicy.access().enabled && ParentalPolicy.access().rules.kidsOnly))
            return@catalogRead getChannelCountsByCategory(playlistId).filterKeys { it != CatalogSections.SUBTITLED }.values.sum()

        val generationId =
            getActiveGenerationId(
                playlistId = playlistId
            ) ?: return@catalogRead 0

        return@catalogRead cacheDao.getChannelCount(
            playlistId = playlistId,
            generationId = generationId
        )
    }

    suspend fun getMovieCount(
        playlistId: String
    ): Int = catalogRead("getMovieCount") {
        if (ParentalPolicy.access().kidsCatalog || (ParentalPolicy.access().enabled && ParentalPolicy.access().rules.kidsOnly))
            return@catalogRead getMovieCountsByCategory(playlistId).filterKeys { it != CatalogSections.SUBTITLED }.values.sum()

        val generationId =
            getActiveGenerationId(
                playlistId = playlistId
            ) ?: return@catalogRead 0

        return@catalogRead cacheDao.getMovieCount(
            playlistId = playlistId,
            generationId = generationId
        )
    }

    /*
     * DETALHE DE UM FILME
     *
     * Busca somente o registro solicitado na geração ACTIVE do Room
     * e converte a entidade para o modelo de domínio.
     */
    suspend fun getMovieById(
        playlistId: String,
        movieId: String
    ): Movie? = catalogRead("getMovieById") {
        if (isParentalBlocked(playlistId, "MOVIES", movieId)) return@catalogRead null
        return@catalogRead cacheDao
            .getActiveMovieById(
                playlistId = playlistId,
                movieId = movieId
            )
            ?.toDomain()?.takeUnless { ParentalPolicy.access().blocked(playlistId, "MOVIES", it.categoryId) }
    }

    /** Resolve tambem atalhos de favoritos/historico, sem depender da pagina visivel. */
    suspend fun getSeriesById(playlistId: String, seriesId: String): Series? = catalogRead("getSeriesById") {
        if (playlistId.isBlank() || seriesId.isBlank()) return@catalogRead null
        if (isParentalBlocked(playlistId, "SERIES", seriesId)) return@catalogRead null
        val generationId = getActiveGenerationId(playlistId) ?: return@catalogRead null
        return@catalogRead cacheDao.getParentalSeriesById(playlistId, generationId, seriesId)
            ?.toDomain()?.takeUnless {
                ParentalPolicy.access().blocked(playlistId, "SERIES", it.categoryId)
            }
    }

    suspend fun getSeriesCount(
        playlistId: String
    ): Int = catalogRead("getSeriesCount") {
        if (ParentalPolicy.access().kidsCatalog || (ParentalPolicy.access().enabled && ParentalPolicy.access().rules.kidsOnly))
            return@catalogRead getSeriesCountsByCategory(playlistId).filterKeys { it != CatalogSections.SUBTITLED }.values.sum()

        val generationId =
            getActiveGenerationId(
                playlistId = playlistId
            ) ?: return@catalogRead 0

        return@catalogRead cacheDao.getSeriesCount(
            playlistId = playlistId,
            generationId = generationId
        )
    }

    /*
     * CONTADORES POR CATEGORIA
     */
    suspend fun getChannelCountsByCategory(
        playlistId: String
    ): Map<String?, Int> = catalogRead("getChannelCountsByCategory") {
        val generationId =
            getActiveGenerationId(
                playlistId = playlistId
            ) ?: return@catalogRead emptyMap()

        return@catalogRead cacheDao
            .getChannelCountsByCategory(
                playlistId = playlistId,
                generationId = generationId
            )
            .associate { result ->
                result.categoryId to result.itemCount
            }
            .filterKeys { id ->
                val access = ParentalPolicy.access()
                !(access.enabled && access.rules.kidsOnly && !access.kidsCatalog) &&
                    (!access.kidsCatalog || KidsContent.category(categoryNames(playlistId,generationId,"LIVE_TV")[id].orEmpty()))
            }
    }

    suspend fun getMovieCountsByCategory(
        playlistId: String
    ): Map<String?, Int> = catalogRead("getMovieCountsByCategory") {
        val generation = getActiveGenerationId(playlistId) ?: return@catalogRead emptyMap()
        categoryCounts(playlistId, generation, TYPE_MOVIES, ParentalPolicy.access())
    }

    suspend fun getSeriesCountsByCategory(
        playlistId: String
    ): Map<String?, Int> = catalogRead("getSeriesCountsByCategory") {
        val generation = getActiveGenerationId(playlistId) ?: return@catalogRead emptyMap()
        categoryCounts(playlistId, generation, TYPE_SERIES, ParentalPolicy.access())
    }

    private suspend fun categoryCounts(playlistId: String, generation: String, type: String,
                                       access: ParentalAccessState): Map<String?, Int> {
        val raw = categoryCountCache.get(
            br.com.flaviotorres.iptvpremium.data.cache.CategoryCountKey(playlistId, generation, type)
        ) {
            val rows = if (type == TYPE_MOVIES) cacheDao.getMovieCountsByCategory(playlistId, generation)
                else cacheDao.getSeriesCountsByCategory(playlistId, generation)
            rows.associate { it.categoryId to it.itemCount }
        }
        // Cache only raw numbers: never persist a Kids/parental projection.
        val names = if (access.kidsCatalog) categoryNames(playlistId, generation, type) else emptyMap()
        return raw.filterKeys { id ->
            !(access.enabled && access.rules.kidsOnly && !access.kidsCatalog) &&
                (!access.kidsCatalog || KidsContent.category(names[id].orEmpty()))
        }
    }

    suspend fun hasActiveCache(
        playlistId: String
    ): Boolean = catalogRead("hasActiveCache") {
        return@catalogRead getActiveGenerationId(
            playlistId = playlistId
        ) != null
    }

    private suspend fun getCategories(
        playlistId: String,
        type: String,
        categoryType: CategoryType,
        knownGeneration: String? = null,
        access: ParentalAccessState = ParentalPolicy.access()
    ): List<Category> = catalogRead("getCategories") {
        val generationId = knownGeneration ?: getActiveGenerationId(playlistId)
            ?: return@catalogRead emptyList()

        return@catalogRead cacheDao
            .getCategoriesByType(
                playlistId = playlistId,
                generationId = generationId,
                type = type
            )
            .map { entity ->
                Category(
                    id = entity.categoryId,
                    name = entity.name,
                    type = categoryType,
                    source = CategorySource.PROVIDER
                )
            }
            .filter { category ->
                !(access.enabled && access.rules.kidsOnly && !access.kidsCatalog) &&
                    (!access.kidsCatalog || KidsContent.category(category.name))
            }
            .map { category ->
                if (access.blocked(playlistId, type, category.id, categoryName = category.name)) category.copy(name = "🔒 " + category.name.replaceFirst(categoryMediaPrefix, "")) else category
            }
            .let { categories ->
                // Preserve the existing subtitle visibility policy; only unify category order.
                val visible = if (categoryType == CategoryType.LIVE_TV) categories
                    else categories.filterNot { CatalogSections.subtitled(it.name) }
                visible.sortedWith(CatalogSections.categoryComparator { it.name })
            }
    }

    // O catalogo apresenta "Filmes | ..." / "Series | ..." sem o prefixo.
    // A prioridade deve usar o MESMO nome visivel, inclusive quando existem
    // duas categorias com nomes iguais, mas IDs diferentes no provedor.
    private val categoryMediaPrefix = Regex(
        pattern = """^\s*(?:filmes?|s[ée]ries?)\s*[|｜:–—-]\s*""",
        option = RegexOption.IGNORE_CASE
    )

    private val adultCategoryMarker = Regex(
        pattern = """\bxxx\b|\badult(?:o|os|a|as)?\b|\b18\s*\+|\+\s*18\b""",
        option = RegexOption.IGNORE_CASE
    )

    private fun categoryGroupPriority(name: String): Int {
        val visibleName = name.trim()
            .replaceFirst(categoryMediaPrefix, "")
            .trim()
        val normalized = visibleName.lowercase(java.util.Locale.ROOT)

        return when {
            // Cursos sao sempre o ultimo grupo.
            normalized.contains("curso") -> 3
            // "[XXX]" e categorias de adultos devem ir para o fim,
            // mesmo quando o nome original possui um prefixo do provedor.
            adultCategoryMarker.containsMatchIn(visibleName) -> 1
            // Outros nomes que comecam por simbolos vao depois dos adultos.
            visibleName.firstOrNull()?.let { !it.isLetterOrDigit() } == true -> 2
            else -> 0
        }
    }

    private data class ParentalSql(val excluded: List<String>, val allowed: List<String>, val restrict: Boolean)
    private fun parentalSql(access: ParentalAccessState, playlist: String, type: String, categoryNames: Map<String, String> = emptyMap()): ParentalSql {
        if (access.enabled && access.rules.kidsOnly && !access.kidsCatalog)
            return ParentalSql(emptyList(), emptyList(), true)
        if (access.kidsCatalog) {
            val allowed = categoryNames.filter { (id, name) -> KidsContent.category(name) &&
                !access.blocked(playlist, type, id, categoryName = name) }.keys.toList()
            return ParentalSql(emptyList(), allowed, true)
        }
        if (!access.enabled) return ParentalSql(emptyList(), emptyList(), false)
        if ("__invalid__" in access.rules.all) return ParentalSql(emptyList(), emptyList(), true)
        if (type == "LIVE_TV") {
            val excluded = if (access.rules.adultChannels && "adult_live" !in access.grants)
                categoryNames.filterValues { br.com.flaviotorres.iptvpremium.ui.settings.AdultChannelMarker.matches(it) }.keys.take(400) else emptyList()
            return ParentalSql(excluded, emptyList(), false)
        }
        val scope = access.scope(playlist, type)
        if (scope in access.grants) return ParentalSql(emptyList(), emptyList(), false)
        val unlocked = access.grants.filter { it.startsWith("$scope/") }.map { it.removePrefix("$scope/") }.toSet()
        return if (scope in access.rules.all) ParentalSql(emptyList(), unlocked.take(400), true)
            else ParentalSql((access.rules.categories[scope].orEmpty() - unlocked).take(400), emptyList(), false)
    }

    fun invalidateLiveCategories(playlistId: String) {
        parentalCategoryNames.keys.removeAll { it.startsWith("$playlistId/") && it.endsWith("/LIVE_TV") }
    }
    private val parentalCategoryNames = java.util.concurrent.ConcurrentHashMap<String, Map<String, String>>()
    private suspend fun categoryNames(playlistId: String, generation: String, type: String = "LIVE_TV"): Map<String, String> = catalogRead("categoryNames") {
        val key = "$playlistId/$generation/$type"
        parentalCategoryNames[key]?.let { return@catalogRead it }
        val names = cacheDao.getCategoriesByType(playlistId, generation, type).associate { it.categoryId to it.name }
        if (parentalCategoryNames.size > 12) parentalCategoryNames.clear()
        parentalCategoryNames[key] = names
        return@catalogRead names
    }

    internal suspend fun parentalTarget(playlistId: String, type: String, id: String): ParentalTarget? = catalogRead("parentalTarget") {
        val generation = getActiveGenerationId(playlistId) ?: return@catalogRead null
        return@catalogRead when (type) {
            "MOVIES" -> cacheDao.getActiveMovieById(playlistId, id)?.let { ParentalTarget(it.categoryId) }
            "SERIES" -> cacheDao.getParentalSeriesById(playlistId, generation, id)?.let { ParentalTarget(it.categoryId) }
            "LIVE_TV" -> cacheDao.getChannelById(playlistId, generation, id)?.let { ParentalTarget(it.categoryId) }
            else -> null
        }
    }

    /** Filter projected history even when Kids is active without a parental PIN. */
    internal suspend fun filterHistoryForCatalog(playlist: String,
        entries: List<br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry>
    ): List<br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry> = catalogRead("filterHistoryForCatalog") {
        // Only saved series IDs are looked up; never scan the catalogue or use an episode backdrop as a poster.
        val generation=getActiveGenerationId(playlist)
        val posters=mutableMapOf<String,String?>()
        val titles=mutableMapOf<String,String?>()
        val decorated=entries.map { entry ->
            if(entry.contentType!=br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType.EPISODE || entry.seriesId.isNullOrBlank()) entry
            else {
                val id=entry.seriesId
                if(!posters.containsKey(id)) {
                    val series=generation?.let {cacheDao.getParentalSeriesById(playlist,it,id)}
                    posters[id]=series?.posterUrl?.takeIf {it.isNotBlank()}
                    titles[id]=series?.name
                }
                entry.copy(seriesPosterUrl=posters[id] ?: entry.seriesPosterUrl?.takeIf {it.isNotBlank() && it!=entry.posterUrl},
                    seriesTitle=entry.seriesTitle?.takeIf {it.isNotBlank()} ?: titles[id])
            }
        }
        val access = ParentalPolicy.access()
        if (!access.hasRestrictions(playlist, "MOVIES") && !access.hasRestrictions(playlist, "SERIES")) return@catalogRead decorated
        val permissions = mutableMapOf<Pair<String, String>, Boolean>()
        return@catalogRead decorated.filter { entry ->
            val type = when (entry.contentType) {
                br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType.MOVIE -> "MOVIES"
                br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType.EPISODE -> "SERIES"
                else -> return@filter false
            }
            val id = if (type == "SERIES") entry.seriesId.orEmpty() else entry.contentId
            permissions.getOrPut(type to id) { !isParentalBlocked(playlist, type, id) }
        }
    }

    internal suspend fun isParentalBlocked(playlistId: String, type: String, id: String): Boolean = catalogRead("isParentalBlocked") {
        val access = ParentalPolicy.access()
        if (!access.hasRestrictions(playlistId, type)) return@catalogRead false
        val generation = getActiveGenerationId(playlistId) ?: return@catalogRead true
        val names = categoryNames(playlistId, generation, type)
        fun blocked(category: String?, name: String = ""): Boolean =
            (access.kidsCatalog && !KidsContent.category(names[category].orEmpty())) ||
                access.blocked(playlistId, type, category, name, names[category].orEmpty())
        return@catalogRead when (type) {
            "MOVIES" -> cacheDao.getActiveMovieById(playlistId, id)?.let { blocked(it.categoryId, it.name) } ?: true
            "SERIES" -> cacheDao.getParentalSeriesById(playlistId, generation, id)?.let { blocked(it.categoryId, it.name) } ?: true
            "LIVE_TV" -> cacheDao.getChannelById(playlistId, generation, id)?.let {
                blocked(it.categoryId,it.name)
            } ?: true
            else -> true
        }
    }

    private suspend fun getActiveGenerationId(
        playlistId: String
    ): String? = catalogRead("getActiveGenerationId") {
        return@catalogRead cacheDao
            .getActiveGeneration(
                playlistId = playlistId
            )
            ?.generationId
    }

    private fun pagingConfig(grid: Boolean = false): PagingConfig {
        if (grid) return br.com.flaviotorres.iptvpremium.data.cache.catalogGridPagingConfig()
        return PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = if (grid) 30 else INITIAL_LOAD_SIZE,
            prefetchDistance = if (grid) 15 else PREFETCH_DISTANCE,
            enablePlaceholders = grid,
            maxSize = if (grid) br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pagingWindow else PagingConfig.MAX_SIZE_UNBOUNDED
        )
    }

    private fun CachedChannelEntity.toDomain(): Channel {
        return Channel(
            id = channelId,
            name = name,
            categoryId = categoryId,
            logoUrl = logoUrl,
            streamUrl = streamUrl,
            epgChannelId = epgChannelId
        )
    }

    private fun CachedMovieEntity.toDomain(): Movie {
        return Movie(
            id = movieId,
            name = name,
            categoryId = categoryId,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            streamUrl = streamUrl,
            year = year,
            rating = rating,
            durationSeconds = durationSeconds,
            addedAtEpochSeconds = addedAtEpochSeconds
        )
    }

    private fun CachedSeriesEntity.toDomain(): Series {
        return Series(
            id = seriesId,
            name = name,
            categoryId = categoryId,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            year = year,
            rating = rating,
            plot = plot,
            titleLogoUrl = titleLogoUrl,
            imdbId = imdbId,
            tmdbId = tmdbId,
            ratingSource = parseRatingSource(
                ratingSource
            ),
            durationSeconds = durationSeconds,
            genres = parseGenres(
                genres
            ),
            trailerUrl = trailerUrl
        )
    }

    private fun parseRatingSource(
        value: String?
    ): RatingSource? {
        return when (
            value
                ?.trim()
                ?.uppercase()
        ) {
            "IMDB" -> RatingSource.IMDB
            "TMDB" -> RatingSource.TMDB
            "PROVIDER" -> RatingSource.PROVIDER
            else -> null
        }
    }

    private fun parseGenres(
        value: String?
    ): List<String> {
        if (value.isNullOrBlank()) {
            return emptyList()
        }

        return value
            .split(
                "|",
                ","
            )
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
    }

    /** Room suspends SQL; the continuation and Kotlin result processing also stay off Main. */
    private suspend fun <T> catalogRead(operation: String, block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            val started = SystemClock.elapsedRealtime()
            try {
                block()
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - started
                if (elapsed >= 100) Log.i("CatalogReadPerf",
                    "operation=$operation totalMs=$elapsed thread=${Thread.currentThread().name}")
            }
        }

    companion object {
        private const val PAGE_SIZE = 60
        // Primeira emissão menor para reduzir mapeamento e recomposições
        // na TV. O restante do catálogo continua vindo do Paging sob demanda.
        // pageSize permanece em 60; prefetchDistance permanece em 20.
        private const val INITIAL_LOAD_SIZE = 60
        private const val PREFETCH_DISTANCE = 20

        private const val TYPE_LIVE_TV = "LIVE_TV"
        private const val TYPE_MOVIES = "MOVIES"
        private const val TYPE_SERIES = "SERIES"

        private const val SORT_DEFAULT = "DEFAULT"
        private const val SORT_A_Z = "A_Z"
        private const val SORT_Z_A = "Z_A"

        private const val NO_FAVORITE_MOVIE_ID =
            "__NO_FAVORITE_MOVIE_ID__"
        private const val NO_FAVORITE_SERIES_ID =
            "__NO_FAVORITE_SERIES_ID__"
    }
}

// IPTV_SETTINGS_APPROVED_GRID_V1
