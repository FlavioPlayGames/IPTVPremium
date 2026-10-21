package br.com.flaviotorres.iptvpremium.data.cache.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedCategoryEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedChannelEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedEpisodeEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedMovieEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedSeriesEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.PlaylistCacheGenerationEntity
import kotlinx.coroutines.flow.Flow


/*
 * Resultado agregado usado pelos contadores das categorias.
 *
 * Todo o catálogo continua armazenado no Room. A interface pode mostrar
 * a quantidade real de itens sem carregar os registros em memória.
 */
data class CacheCategoryCount(
    val categoryId: String?,
    val itemCount: Int
)

@Dao
interface PlaylistCacheDao {
    // Candidate-only pages: never materialize the whole catalogue for shared history.
    @Query("SELECT movieId AS contentId, name AS title, year, NULL AS imdb, NULL AS tmdb, posterUrl AS poster, providerOrder FROM cached_movies WHERE playlistId = :playlist AND generationId = :generation AND movieId > :after AND ((:pattern != '' AND name GLOB :pattern) OR movieId IN (:ids)) ORDER BY movieId LIMIT 256")
    suspend fun sharedMovieCandidates(playlist: String, generation: String, after: String, pattern: String, ids: List<String>): List<br.com.flaviotorres.iptvpremium.data.history.SharedCatalogIdentity>

    @Query("SELECT seriesId AS contentId, name AS title, year, imdbId AS imdb, tmdbId AS tmdb, posterUrl AS poster, providerOrder FROM cached_series WHERE playlistId = :playlist AND generationId = :generation AND seriesId > :after AND ((:pattern != '' AND name GLOB :pattern) OR seriesId IN (:ids) OR lower(trim(imdbId)) IN (:imdbs) OR ltrim(trim(tmdbId), '+0') IN (:tmdbs)) ORDER BY seriesId LIMIT 256")
    suspend fun sharedSeriesCandidates(playlist: String, generation: String, after: String, pattern: String, ids: List<String>, imdbs: List<String>, tmdbs: List<String>): List<br.com.flaviotorres.iptvpremium.data.history.SharedCatalogIdentity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMoviesFirst(items: List<CachedMovieEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeriesFirst(items: List<CachedSeriesEntity>)
    @Query("SELECT * FROM cached_movies WHERE playlistId = :playlistId AND generationId = :generationId AND providerOrder > :afterOrder ORDER BY providerOrder, movieId LIMIT :limit")
    suspend fun streamMovieStage(playlistId: String, generationId: String, limit: Int, afterOrder: Int): List<CachedMovieEntity>
    @Query("SELECT * FROM cached_series WHERE playlistId = :playlistId AND generationId = :generationId AND providerOrder > :afterOrder ORDER BY providerOrder, seriesId LIMIT :limit")
    suspend fun streamSeriesStage(playlistId: String, generationId: String, limit: Int, afterOrder: Int): List<CachedSeriesEntity>

    @Query("SELECT DISTINCT playlistId FROM playlist_cache_generations")
    suspend fun catalogPlaylistIds(): List<String>

    @Query("""
        UPDATE playlist_cache_generations SET status = 'STALE'
        WHERE status = 'ACTIVE' AND playlistId NOT IN (:protectedIds)
        AND COALESCE(activatedAtEpochMillis, completedAtEpochMillis, createdAtEpochMillis) < :beforeMillis
    """)
    suspend fun retireUnregisteredActiveGenerations(protectedIds: List<String>, beforeMillis: Long): Int

    @Query("""
        SELECT DISTINCT playlistId FROM playlist_cache_generations
        WHERE status = 'STALE' OR (status = 'IMPORTING' AND createdAtEpochMillis < :abandonedBefore)
        ORDER BY playlistId
    """)
    suspend fun playlistsPendingStorageCleanup(abandonedBefore: Long): List<String>


    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertGeneration(
        generation: PlaylistCacheGenerationEntity
    )

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertCategories(
        items: List<CachedCategoryEntity>
    )

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertChannels(
        items: List<CachedChannelEntity>
    )

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertMovies(
        items: List<CachedMovieEntity>
    )

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertSeries(
        items: List<CachedSeriesEntity>
    )

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertEpisodes(
        items: List<CachedEpisodeEntity>
    )

    /** One Room transaction per existing M3U batch; never includes network work. */
    @Transaction
    suspend fun insertImportBatch(
        categories: List<CachedCategoryEntity>, channels: List<CachedChannelEntity>,
        movies: List<CachedMovieEntity>, series: List<CachedSeriesEntity>, episodes: List<CachedEpisodeEntity>
    ) {
        if (categories.isNotEmpty()) insertCategories(categories)
        if (channels.isNotEmpty()) insertChannels(channels)
        if (movies.isNotEmpty()) insertMovies(movies)
        if (series.isNotEmpty()) insertSeries(series)
        if (episodes.isNotEmpty()) insertEpisodes(episodes)
    }

    @Query("SELECT status FROM playlist_cache_generations WHERE playlistId = :playlistId AND generationId = :generationId LIMIT 1")
    suspend fun generationStatus(playlistId: String, generationId: String): String?

    @Query("SELECT generationId FROM playlist_cache_generations WHERE playlistId = :playlistId AND status = 'IMPORTING' AND createdAtEpochMillis < :before ORDER BY createdAtEpochMillis ASC LIMIT 1")
    suspend fun oldestAbandonedImport(playlistId: String, before: Long): String?

    /** Failed work must never erase READY/ACTIVE data, even if cancellation races with completion. */
    @Transaction
    suspend fun discardIncompleteImport(playlistId: String, generationId: String): Boolean {
        if (generationStatus(playlistId, generationId) != "IMPORTING") return false
        deleteGeneration(playlistId, generationId)
        return true
    }

    @Query("SELECT * FROM cached_series WHERE playlistId = :playlistId AND generationId = :generationId AND seriesId = :seriesId LIMIT 1")
    suspend fun getParentalSeriesById(playlistId: String, generationId: String, seriesId: String): CachedSeriesEntity?

    @Query(
        """
        SELECT *
        FROM playlist_cache_generations
        WHERE playlistId = :playlistId
          AND status = 'ACTIVE'
        ORDER BY activatedAtEpochMillis DESC
        LIMIT 1
        """
    )
    suspend fun getActiveGeneration(
        playlistId: String
    ): PlaylistCacheGenerationEntity?

    /*
     * Observa a geração ACTIVE do Room em tempo real.
     *
     * O Flow notifica os coletores quando a tabela de gerações muda.
     * O repositório poderá trocar de PagingSource quando uma primeira
     * importação terminar ou quando uma nova geração for ativada.
     *
     * Retorna null enquanto ainda não existir uma geração ACTIVE.
     * Não carrega canais, filmes ou séries em memória.
     */
    @Query(
        """
        SELECT generationId
        FROM playlist_cache_generations
        WHERE playlistId = :playlistId
          AND status = 'ACTIVE'
        ORDER BY activatedAtEpochMillis DESC
        LIMIT 1
        """
    )
    fun observeActiveGenerationId(
        playlistId: String
    ): Flow<String?>

    @Query(
        """
        SELECT *
        FROM playlist_cache_generations
        WHERE playlistId = :playlistId
          AND status = 'READY'
        ORDER BY completedAtEpochMillis DESC
        LIMIT 1
        """
    )
    suspend fun getReadyGeneration(
        playlistId: String
    ): PlaylistCacheGenerationEntity?


    @Query(
        """
        SELECT *
        FROM cached_categories
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND type = :type
        ORDER BY name COLLATE NOCASE ASC
        """
    )
    suspend fun getCategoriesByType(
        playlistId: String,
        generationId: String,
        type: String
    ): List<CachedCategoryEntity>

    /**
     * Busca um canal diretamente pela chave primaria na geracao ativa.
     * Evita percorrer as paginas do catalogo para abrir uma transmissao.
     * A URL permanece no registro local; nunca vai para a rota de navegacao.
     */
    @Query(
        """
        SELECT *
        FROM cached_channels
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND channelId = :channelId
        LIMIT 1
        """
    )
    suspend fun getChannelById(
        playlistId: String,
        generationId: String,
        channelId: String
    ): CachedChannelEntity?

    /*
     * TROCA RAPIDA DE CANAIS (CH+ / CH-)
     *
     * Respeita a ordem channelNumber do catalogo e usa channelId como
     * desempate estavel. Quando chega ao fim, volta ao comeco (e vice-versa).
     * Nao materializa a playlist em memoria e ignora streams vazios.
     *
     * A consulta so localiza o vizinho na geracao informada: a navegacao
     * ficara responsavel por escolher a geracao ACTIVE e abrir a midia.
     */
    @Query(
        """
        SELECT candidate.*
        FROM cached_channels AS candidate
        INNER JOIN cached_channels AS origin
            ON origin.playlistId = :playlistId
           AND origin.generationId = :generationId
           AND origin.channelId = :channelId
        WHERE candidate.playlistId = :playlistId
          AND candidate.generationId = :generationId
          AND candidate.channelId != origin.channelId
          AND TRIM(candidate.streamUrl) != ''
          AND (:kidsOnly=0 OR candidate.categoryId IN (:kidsCategoryIds))
        ORDER BY
            CASE
                WHEN candidate.channelNumber > origin.channelNumber
                  OR (
                      candidate.channelNumber = origin.channelNumber
                      AND candidate.channelId > origin.channelId
                  )
                THEN 0 ELSE 1
            END ASC,
            candidate.channelNumber ASC,
            candidate.channelId ASC
        LIMIT 1
        """
    )
    suspend fun getNextPlayableChannel(
        playlistId: String,
        generationId: String,
        channelId: String,
        kidsOnly: Boolean = false,
        kidsCategoryIds: List<String> = emptyList()
    ): CachedChannelEntity?

    @Query(
        """
        SELECT candidate.*
        FROM cached_channels AS candidate
        INNER JOIN cached_channels AS origin
            ON origin.playlistId = :playlistId
           AND origin.generationId = :generationId
           AND origin.channelId = :channelId
        WHERE candidate.playlistId = :playlistId
          AND candidate.generationId = :generationId
          AND candidate.channelId != origin.channelId
          AND TRIM(candidate.streamUrl) != ''
          AND (:kidsOnly=0 OR candidate.categoryId IN (:kidsCategoryIds))
        ORDER BY
            CASE
                WHEN candidate.channelNumber < origin.channelNumber
                  OR (
                      candidate.channelNumber = origin.channelNumber
                      AND candidate.channelId < origin.channelId
                  )
                THEN 0 ELSE 1
            END ASC,
            candidate.channelNumber DESC,
            candidate.channelId DESC
        LIMIT 1
        """
    )
    suspend fun getPreviousPlayableChannel(
        playlistId: String,
        generationId: String,
        channelId: String,
        kidsOnly: Boolean = false,
        kidsCategoryIds: List<String> = emptyList()
    ): CachedChannelEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM cached_channels
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun getChannelCount(
        playlistId: String,
        generationId: String
    ): Int

    @Query(
        """
        SELECT categoryId,
               COUNT(*) AS itemCount
        FROM cached_channels
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        GROUP BY categoryId
        """
    )
    suspend fun getChannelCountsByCategory(
        playlistId: String,
        generationId: String
    ): List<CacheCategoryCount>

    @Query(
        """
        SELECT *
        FROM cached_channels
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND COALESCE(NULLIF(categoryId, ''), '__uncategorized__') NOT IN (:parentalExcludedIds)
          AND (:parentalRestrict = 0 OR COALESCE(NULLIF(categoryId, ''), '__uncategorized__') IN (:parentalAllowedIds))
        ORDER BY channelNumber ASC
        """
    )
    fun getChannelsPagingSource(
        playlistId: String,
        generationId: String,
        parentalExcludedIds: List<String> = emptyList(),
        parentalAllowedIds: List<String> = emptyList(),
        parentalRestrict: Boolean = false
    ): PagingSource<Int, CachedChannelEntity>

    @Query(
        """
        SELECT *
        FROM cached_channels
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND COALESCE(NULLIF(categoryId, ''), '__uncategorized__') NOT IN (:parentalExcludedIds)
          AND (:parentalRestrict = 0 OR COALESCE(NULLIF(categoryId, ''), '__uncategorized__') IN (:parentalAllowedIds))
          AND categoryId = :categoryId
        ORDER BY channelNumber ASC
        """
    )
    fun getChannelsPagingSourceByCategory(
        playlistId: String,
        generationId: String,
        categoryId: String,
        parentalExcludedIds: List<String> = emptyList(),
        parentalAllowedIds: List<String> = emptyList(),
        parentalRestrict: Boolean = false
    ): PagingSource<Int, CachedChannelEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM cached_movies
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND NOT """ + MOVIE_IS_SUBTITLED_SQL + """
        """
    )
    suspend fun getMovieCount(
        playlistId: String,
        generationId: String
    ): Int

    @Query("""SELECT COUNT(*) FROM cached_movies WHERE playlistId=:playlistId AND generationId=:generationId
        AND COALESCE(NULLIF(categoryId,''),'__uncategorized__') NOT IN (:excluded)
        AND (:restrict=0 OR COALESCE(NULLIF(categoryId,''),'__uncategorized__') IN (:allowed))
        AND """ + MOVIE_IS_SUBTITLED_SQL)
    suspend fun getSubtitledMovieCount(playlistId:String,generationId:String,excluded:List<String>,allowed:List<String>,restrict:Boolean):Int

    @Query("""SELECT COUNT(*) FROM cached_series WHERE playlistId=:playlistId AND generationId=:generationId
        AND COALESCE(NULLIF(categoryId,''),'__uncategorized__') NOT IN (:excluded)
        AND (:restrict=0 OR COALESCE(NULLIF(categoryId,''),'__uncategorized__') IN (:allowed))
        AND (UPPER(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace((' ' || name || ' '), '[', ' '), ']', ' '), '(', ' '), ')', ' '), '|', ' '), '-', ' '), '_', ' '), '/', ' '), '{', ' '), '}', ' ')) LIKE '% L %' OR UPPER(name) LIKE '%LEGENDAD%' OR categoryId IN (SELECT categoryId FROM cached_categories WHERE playlistId = :playlistId AND generationId = :generationId AND type = 'SERIES' AND LOWER(name) LIKE '%legendad%'))""")
    suspend fun getSubtitledSeriesCount(playlistId:String,generationId:String,excluded:List<String>,allowed:List<String>,restrict:Boolean):Int

    /*
     * CACHE PRIORITARIO NA ABERTURA — FAVORITOS DE FILMES.
     *
     * A consulta e feita no Room, nao carrega todas as categorias.
     * Usa os mesmos IDs de favoritos do AppNavigation e a ordem da grade.
     * A chamada deve ser ignorada quando favoriteMovieIds estiver vazia.
     */
    @Query(
        """
        SELECT *
        FROM cached_movies
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND movieId IN (:favoriteMovieIds)
        ORDER BY
            CASE
                WHEN providerOrder IS NULL OR providerOrder < 0 THEN 1
                ELSE 0
            END ASC,
            providerOrder ASC,
            name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun getFavoriteMoviesForStartup(
        playlistId: String,
        generationId: String,
        favoriteMovieIds: List<String>,
        limit: Int
    ): List<CachedMovieEntity>

    /*
     * Busca um único filme na geração ACTIVE da playlist.
     *
     * Usado pela tela de detalhes para não carregar uma página inteira
     * nem reconstruir o catálogo em memória.
     */
    @Query(
        """
        SELECT *
        FROM cached_movies
        WHERE playlistId = :playlistId
          AND generationId = (
              SELECT generationId
              FROM playlist_cache_generations
              WHERE playlistId = :playlistId
                AND status = 'ACTIVE'
              ORDER BY activatedAtEpochMillis DESC
              LIMIT 1
          )
          AND movieId = :movieId
        LIMIT 1
        """
    )
    suspend fun getActiveMovieById(
        playlistId: String,
        movieId: String
    ): CachedMovieEntity?

    @Query(
        """
        SELECT categoryId,
               COUNT(*) AS itemCount
        FROM cached_movies
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND NOT """ + MOVIE_IS_SUBTITLED_SQL + """
        GROUP BY categoryId
        """
    )
    suspend fun getMovieCountsByCategory(
        playlistId: String,
        generationId: String
    ): List<CacheCategoryCount>

    @Query(
        """
        SELECT *
        FROM cached_movies
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND NOT """ + MOVIE_IS_SUBTITLED_SQL + """
          AND COALESCE(NULLIF(categoryId, ''), '__uncategorized__') NOT IN (:parentalExcludedIds)
          AND (:parentalRestrict = 0 OR COALESCE(NULLIF(categoryId, ''), '__uncategorized__') IN (:parentalAllowedIds))
        ORDER BY
            CASE
                WHEN providerOrder IS NULL
                     OR providerOrder < 0
                THEN 1
                ELSE 0
            END ASC,
            providerOrder ASC,
            name COLLATE NOCASE ASC
        """
    )
    fun getMoviesPagingSource(
        playlistId: String,
        generationId: String,
        parentalExcludedIds: List<String> = emptyList(),
        parentalAllowedIds: List<String> = emptyList(),
        parentalRestrict: Boolean = false
    ): PagingSource<Int, CachedMovieEntity>

    @Query(
        """
        SELECT *
        FROM cached_movies
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND NOT """ + MOVIE_IS_SUBTITLED_SQL + """
          AND COALESCE(NULLIF(categoryId, ''), '__uncategorized__') NOT IN (:parentalExcludedIds)
          AND (:parentalRestrict = 0 OR COALESCE(NULLIF(categoryId, ''), '__uncategorized__') IN (:parentalAllowedIds))
          AND categoryId = :categoryId
        ORDER BY
            CASE
                WHEN providerOrder IS NULL
                     OR providerOrder < 0
                THEN 1
                ELSE 0
            END ASC,
            providerOrder ASC,
            name COLLATE NOCASE ASC
        """
    )
    fun getMoviesPagingSourceByCategory(
        playlistId: String,
        generationId: String,
        categoryId: String,
        parentalExcludedIds: List<String> = emptyList(),
        parentalAllowedIds: List<String> = emptyList(),
        parentalRestrict: Boolean = false
    ): PagingSource<Int, CachedMovieEntity>


    /*
     * CONSULTA PAGINADA DE FILMES COM FILTROS
     *
     * Esta consulta mantém todo o trabalho no SQLite:
     *
     * - categoria opcional;
     * - busca pelo nome;
     * - somente favoritos, quando solicitado;
     * - DEFAULT pela ordem original enviada pelo provedor;
     * - A–Z e Z–A sem carregar o catálogo em memória.
     *
     * Filmes sem providerOrder válido ficam no final do modo DEFAULT.
     * O nome é usado apenas como desempate estável.
     */
    @Query(
        """
        SELECT *
        FROM cached_movies
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND COALESCE(NULLIF(categoryId, ''), '__uncategorized__') NOT IN (:parentalExcludedIds)
          AND (:parentalRestrict = 0 OR COALESCE(NULLIF(categoryId, ''), '__uncategorized__') IN (:parentalAllowedIds))
          AND (
              :categoryId IS NULL
              OR categoryId = :categoryId
          )
          AND (
              :searchQuery = ''
              OR name LIKE '%' || :searchQuery || '%' COLLATE NOCASE
          )
          AND (
              :favoritesOnly = 0
              OR movieId IN (:favoriteMovieIds)
          )
          AND (:subtitledOnly = 0 OR """ + MOVIE_IS_SUBTITLED_SQL + """)
          AND (:subtitledOnly = 1 OR :favoritesOnly = 1 OR :includeSubtitled = 1 OR NOT """ + MOVIE_IS_SUBTITLED_SQL + """)
        ORDER BY
            CASE
                WHEN :sortMode = 'DEFAULT'
                    AND (
                         providerOrder IS NULL
                         OR providerOrder < 0
                    )
                THEN 1
                WHEN :sortMode = 'DEFAULT'
                THEN 0
            END ASC,
            CASE
                WHEN :sortMode = 'DEFAULT'
                THEN providerOrder
            END ASC,
            CASE
                WHEN :sortMode = 'A_Z'
                THEN name
            END COLLATE NOCASE ASC,
            CASE
                WHEN :sortMode = 'Z_A'
                THEN name
            END COLLATE NOCASE DESC,
            name COLLATE NOCASE ASC
        """
    )
    fun getMoviesPagingSourceFiltered(
        playlistId: String,
        generationId: String,
        categoryId: String?,
        searchQuery: String,
        favoritesOnly: Boolean,
        favoriteMovieIds: List<String>,
        sortMode: String,
        subtitledOnly: Boolean = false,
        parentalExcludedIds: List<String> = emptyList(),
        parentalAllowedIds: List<String> = emptyList(),
        parentalRestrict: Boolean = false,
        includeSubtitled: Boolean = false
    ): PagingSource<Int, CachedMovieEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM cached_series
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun getSeriesCount(
        playlistId: String,
        generationId: String
    ): Int

    @Query(
        """
        SELECT categoryId,
               COUNT(*) AS itemCount
        FROM cached_series
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        GROUP BY categoryId
        """
    )
    suspend fun getSeriesCountsByCategory(
        playlistId: String,
        generationId: String
    ): List<CacheCategoryCount>

    @Query(
        """
        SELECT *
        FROM cached_series
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND COALESCE(NULLIF(categoryId, ''), '__uncategorized__') NOT IN (:parentalExcludedIds)
          AND (:parentalRestrict = 0 OR COALESCE(NULLIF(categoryId, ''), '__uncategorized__') IN (:parentalAllowedIds))
        ORDER BY
            CASE WHEN providerOrder IS NULL OR providerOrder < 0 THEN 1 ELSE 0 END ASC,
            providerOrder ASC,
            name COLLATE NOCASE ASC,
            seriesId ASC
        """
    )
    fun getSeriesPagingSource(
        playlistId: String,
        generationId: String,
        parentalExcludedIds: List<String> = emptyList(),
        parentalAllowedIds: List<String> = emptyList(),
        parentalRestrict: Boolean = false
    ): PagingSource<Int, CachedSeriesEntity>

    @Query(
        """
        SELECT *
        FROM cached_series
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND COALESCE(NULLIF(categoryId, ''), '__uncategorized__') NOT IN (:parentalExcludedIds)
          AND (:parentalRestrict = 0 OR COALESCE(NULLIF(categoryId, ''), '__uncategorized__') IN (:parentalAllowedIds))
          AND categoryId = :categoryId
        ORDER BY
            CASE WHEN providerOrder IS NULL OR providerOrder < 0 THEN 1 ELSE 0 END ASC,
            providerOrder ASC,
            name COLLATE NOCASE ASC,
            seriesId ASC
        """
    )
    fun getSeriesPagingSourceByCategory(
        playlistId: String,
        generationId: String,
        categoryId: String,
        parentalExcludedIds: List<String> = emptyList(),
        parentalAllowedIds: List<String> = emptyList(),
        parentalRestrict: Boolean = false
    ): PagingSource<Int, CachedSeriesEntity>

    /*
     * Mesmo contrato de filtragem SQL dos Filmes:
     * categoria, busca, favoritos e ordem são aplicados ANTES do Paging.
     * O catálogo completo não é materializado na memória.
     * DEFAULT = ordem original do provedor, A_Z e Z_A por nome.
     */
    @Query(
        """
        SELECT *
        FROM cached_series
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND COALESCE(NULLIF(categoryId, ''), '__uncategorized__') NOT IN (:parentalExcludedIds)
          AND (:parentalRestrict = 0 OR COALESCE(NULLIF(categoryId, ''), '__uncategorized__') IN (:parentalAllowedIds))
          AND (:subtitledOnly = 0 OR (UPPER(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace((' ' || name || ' '), '[', ' '), ']', ' '), '(', ' '), ')', ' '), '|', ' '), '-', ' '), '_', ' '), '/', ' '), '{', ' '), '}', ' ')) LIKE '% L %' OR UPPER(name) LIKE '%LEGENDAD%' OR categoryId IN (SELECT categoryId FROM cached_categories WHERE playlistId = :playlistId AND generationId = :generationId AND type = 'SERIES' AND LOWER(name) LIKE '%legendad%')))
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:searchQuery = '' OR name LIKE '%' || :searchQuery || '%' COLLATE NOCASE)
          AND (:favoritesOnly = 0 OR seriesId IN (:favoriteSeriesIds))
        ORDER BY
            CASE
                WHEN :sortMode = 'DEFAULT' AND (providerOrder IS NULL OR providerOrder < 0)
                THEN 1
                WHEN :sortMode = 'DEFAULT' THEN 0
            END ASC,
            CASE WHEN :sortMode = 'DEFAULT' THEN providerOrder END ASC,
            CASE WHEN :sortMode = 'A_Z' THEN name END COLLATE NOCASE ASC,
            CASE WHEN :sortMode = 'Z_A' THEN name END COLLATE NOCASE DESC,
            name COLLATE NOCASE ASC,
            seriesId ASC
        """
    )
    fun getSeriesPagingSourceFiltered(
        playlistId: String,
        generationId: String,
        categoryId: String?,
        searchQuery: String,
        favoritesOnly: Boolean,
        favoriteSeriesIds: List<String>,
        sortMode: String,
        subtitledOnly: Boolean = false,
        parentalExcludedIds: List<String> = emptyList(),
        parentalAllowedIds: List<String> = emptyList(),
        parentalRestrict: Boolean = false
    ): PagingSource<Int, CachedSeriesEntity>

    /*
     * CACHE PRIORITARIO NA ABERTURA — FAVORITOS DE SERIES.
     *
     * Retorna so os titulos salvos como favoritos para a playlist ativa,
     * respeitando a ordem alfabetica atual da grade de Series.
     */
    @Query(
        """
        SELECT *
        FROM cached_series
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND seriesId IN (:favoriteSeriesIds)
        ORDER BY name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    suspend fun getFavoriteSeriesForStartup(
        playlistId: String,
        generationId: String,
        favoriteSeriesIds: List<String>,
        limit: Int
    ): List<CachedSeriesEntity>

    @Query("SELECT streamUrl FROM cached_episodes WHERE playlistId = :playlistId AND generationId = :generationId AND seriesId = :seriesId AND episodeId = :episodeId LIMIT 1")
    suspend fun episodePreviewUrl(playlistId: String, generationId: String, seriesId: String, episodeId: String): String?

    @Query(
        """
        SELECT *
        FROM cached_episodes
        WHERE playlistId = :playlistId
          AND generationId = :generationId
          AND seriesId = :seriesId
        ORDER BY seasonNumber ASC, episodeNumber ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getEpisodesPage(
        playlistId: String,
        generationId: String,
        seriesId: String,
        limit: Int,
        offset: Int
    ): List<CachedEpisodeEntity>

    @Query(
        """
        UPDATE playlist_cache_generations
        SET status = :status,
            completedAtEpochMillis = :completedAtEpochMillis
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun updateGenerationStatus(
        playlistId: String,
        generationId: String,
        status: String,
        completedAtEpochMillis: Long?
    )

    @Query(
        """
        UPDATE playlist_cache_generations
        SET status = 'STALE',
            activatedAtEpochMillis = NULL
        WHERE playlistId = :playlistId
          AND status = 'ACTIVE'
        """
    )
    suspend fun demoteActiveGeneration(
        playlistId: String
    )

    @Query(
        """
        UPDATE playlist_cache_generations
        SET status = 'ACTIVE',
            activatedAtEpochMillis = :activatedAtEpochMillis
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun activateGeneration(
        playlistId: String,
        generationId: String,
        activatedAtEpochMillis: Long
    )

    @Transaction
    suspend fun switchToReadyGeneration(
        playlistId: String,
        generationId: String,
        activatedAtEpochMillis: Long
    ) {
        if (generationStatus(playlistId, generationId) != "READY") return
        demoteActiveGeneration(
            playlistId = playlistId
        )

        activateGeneration(
            playlistId = playlistId,
            generationId = generationId,
            activatedAtEpochMillis =
                activatedAtEpochMillis
        )
    }

    /*
     * LIMPEZA CONTROLADA DE GERAÇÕES STALE
     *
     * O catálogo ACTIVE nunca entra nestas consultas.
     *
     * A limpeza pesada é feita em pequenos lotes usando rowid, evitando
     * uma única transação gigantesca para dezenas de milhares de registros.
     * O GenerationManager fará essa limpeza fora do caminho crítico da
     * atualização da playlist.
     */
    @Query(
        """
        SELECT generationId
        FROM playlist_cache_generations
        WHERE playlistId = :playlistId
          AND status = 'STALE'
        ORDER BY
            COALESCE(
                activatedAtEpochMillis,
                completedAtEpochMillis,
                createdAtEpochMillis
            ) ASC
        LIMIT 1
        """
    )
    suspend fun getOldestStaleGenerationId(
        playlistId: String
    ): String?

    @Query(
        """
        DELETE FROM cached_categories
        WHERE rowid IN (
            SELECT rowid
            FROM cached_categories
            WHERE playlistId = :playlistId
              AND generationId = :generationId
            ORDER BY rowid
            LIMIT :limit
        )
        """
    )
    suspend fun deleteCategoriesBatchForGeneration(
        playlistId: String,
        generationId: String,
        limit: Int
    ): Int

    @Query(
        """
        DELETE FROM cached_channels
        WHERE rowid IN (
            SELECT rowid
            FROM cached_channels
            WHERE playlistId = :playlistId
              AND generationId = :generationId
            ORDER BY rowid
            LIMIT :limit
        )
        """
    )
    suspend fun deleteChannelsBatchForGeneration(
        playlistId: String,
        generationId: String,
        limit: Int
    ): Int

    @Query(
        """
        DELETE FROM cached_movies
        WHERE rowid IN (
            SELECT rowid
            FROM cached_movies
            WHERE playlistId = :playlistId
              AND generationId = :generationId
            ORDER BY rowid
            LIMIT :limit
        )
        """
    )
    suspend fun deleteMoviesBatchForGeneration(
        playlistId: String,
        generationId: String,
        limit: Int
    ): Int

    @Query(
        """
        DELETE FROM cached_series
        WHERE rowid IN (
            SELECT rowid
            FROM cached_series
            WHERE playlistId = :playlistId
              AND generationId = :generationId
            ORDER BY rowid
            LIMIT :limit
        )
        """
    )
    suspend fun deleteSeriesBatchForGeneration(
        playlistId: String,
        generationId: String,
        limit: Int
    ): Int

    @Query(
        """
        DELETE FROM cached_episodes
        WHERE rowid IN (
            SELECT rowid
            FROM cached_episodes
            WHERE playlistId = :playlistId
              AND generationId = :generationId
            ORDER BY rowid
            LIMIT :limit
        )
        """
    )
    suspend fun deleteEpisodesBatchForGeneration(
        playlistId: String,
        generationId: String,
        limit: Int
    ): Int

    @Query(
        """
        DELETE FROM cached_categories
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun deleteCategoriesForGeneration(
        playlistId: String,
        generationId: String
    )

    @Query(
        """
        DELETE FROM cached_channels
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun deleteChannelsForGeneration(
        playlistId: String,
        generationId: String
    )

    @Query(
        """
        DELETE FROM cached_movies
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun deleteMoviesForGeneration(
        playlistId: String,
        generationId: String
    )

    @Query(
        """
        DELETE FROM cached_series
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun deleteSeriesForGeneration(
        playlistId: String,
        generationId: String
    )

    @Query(
        """
        DELETE FROM cached_episodes
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun deleteEpisodesForGeneration(
        playlistId: String,
        generationId: String
    )

    @Query(
        """
        DELETE FROM playlist_cache_generations
        WHERE playlistId = :playlistId
          AND generationId = :generationId
        """
    )
    suspend fun deleteGenerationRow(
        playlistId: String,
        generationId: String
    )

    @Transaction
    suspend fun deleteGeneration(
        playlistId: String,
        generationId: String
    ) {
        deleteCategoriesForGeneration(
            playlistId,
            generationId
        )

        deleteChannelsForGeneration(
            playlistId,
            generationId
        )

        deleteMoviesForGeneration(
            playlistId,
            generationId
        )

        deleteSeriesForGeneration(
            playlistId,
            generationId
        )

        deleteEpisodesForGeneration(
            playlistId,
            generationId
        )

        deleteGenerationRow(
            playlistId,
            generationId
        )
    }

    @Query(
        """
        DELETE FROM cached_categories
        WHERE playlistId = :playlistId
        """
    )
    suspend fun deleteAllCategories(
        playlistId: String
    )

    @Query(
        """
        DELETE FROM cached_channels
        WHERE playlistId = :playlistId
        """
    )
    suspend fun deleteAllChannels(
        playlistId: String
    )

    @Query(
        """
        DELETE FROM cached_movies
        WHERE playlistId = :playlistId
        """
    )
    suspend fun deleteAllMovies(
        playlistId: String
    )

    @Query(
        """
        DELETE FROM cached_series
        WHERE playlistId = :playlistId
        """
    )
    suspend fun deleteAllSeries(
        playlistId: String
    )

    @Query(
        """
        DELETE FROM cached_episodes
        WHERE playlistId = :playlistId
        """
    )
    suspend fun deleteAllEpisodes(
        playlistId: String
    )

    @Query(
        """
        DELETE FROM playlist_cache_generations
        WHERE playlistId = :playlistId
        """
    )
    suspend fun deleteAllGenerations(
        playlistId: String
    )

    @Transaction
    suspend fun deletePlaylistCache(
        playlistId: String
    ) {
        deleteAllCategories(
            playlistId
        )

        deleteAllChannels(
            playlistId
        )

        deleteAllMovies(
            playlistId
        )

        deleteAllSeries(
            playlistId
        )

        deleteAllEpisodes(
            playlistId
        )

        deleteAllGenerations(
            playlistId
        )
    }
}
