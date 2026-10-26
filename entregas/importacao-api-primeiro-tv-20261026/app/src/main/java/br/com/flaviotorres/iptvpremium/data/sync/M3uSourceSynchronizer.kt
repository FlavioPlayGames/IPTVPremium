package br.com.flaviotorres.iptvpremium.data.sync

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedCategoryEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedChannelEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedEpisodeEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedMovieEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedSeriesEntity
import br.com.flaviotorres.iptvpremium.data.cache.importer.M3uCacheImporter
import br.com.flaviotorres.iptvpremium.data.resolver.M3uXtreamResolver
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.domain.repository.PlaylistRepository

class M3uSourceSynchronizer(
    private val playlistRepository: PlaylistRepository,
    private val m3uCacheImporter: M3uCacheImporter,
    private val cacheDao: PlaylistCacheDao,
    private val generationManager: PlaylistCacheGenerationManager,
    private val m3uTransport: M3uTransportPolicy? = null
) {

    suspend fun sync(source: PlaylistSource.M3u): String = sharedSync(source) { syncM3u(source) }

    private suspend fun syncM3u(source: PlaylistSource.M3u): String {
        // First rollout: never change IDs underneath an existing native-M3U catalogue.
        // New entries exercise API-first; existing entries need an explicit data migration later.
        val existing = cacheDao.getActiveGeneration(source.id)
        val previouslySelected = m3uTransport?.hasApiSelection(source) == true
        val api = if (existing == null || previouslySelected) m3uTransport?.select(source) else null
        if (existing != null && !previouslySelected) {
            Log.i(TAG, "IMPORT_TRANSPORTE catalogo_existente=true migracao_automatica=false")
        }
        if (api != null) {
            Log.i(TAG, "IMPORT_TRANSPORTE origem=M3U transporte=XTREAM persistente=true episodios=SOB_DEMANDA")
            return syncXtreamWithRetry(api, includeLive = true)
        }
        Log.i(TAG, "IMPORT_TRANSPORTE origem=M3U transporte=M3U_NATIVA episodios=PLAYLIST")
        return medirEtapa("M3U_NATIVA") {
            m3uCacheImporter.import(
                playlistId = source.id,
                playlistUrl = source.playlistUrl
            )
        }
    }


    /*
     * XTREAM CODES DIRETO
     *
     * Usa exatamente a mesma persistência já utilizada quando uma M3U
     * é reconhecida como Xtream-compatible.
     *
     * Dessa forma M3U e Xtream Codes direto passam a terminar no mesmo
     * catálogo Room, pronto para ser consumido por Paging.
     */
    suspend fun sync(
        source: PlaylistSource.XtreamCodes
    ): String = sharedSync(source) {
        Log.i(
            TAG,
            "Xtream Codes direto; sincronizando catálogo para o Room."
        )

        // The format chosen in the portal is authoritative, including refreshes.
        // Keep the original M3U URL as metadata only; an Xtream error must not
        // silently switch the importer or replace the catalog with a M3U import.
        syncXtreamWithRetry(source).also {
            Log.i(TAG, "IMPORT_RESULTADO formato=XTREAM resultado=SUCESSO")
        }
    }

    /** Retry the chosen Xtream format; cancellation and local storage failures stay terminal. */
    private suspend fun syncXtreamWithRetry(source: PlaylistSource.XtreamCodes, includeLive: Boolean = false): String {
        var attempt = 1
        while (true) {
            try {
                return syncFromXtream(source.id, source, includeLive)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (attempt >= 4 || containsStorageFailure(error)) throw error
                val pauseMillis = 500L shl (attempt - 1)
                Log.w(TAG, "XTREAM_NOVA_TENTATIVA tentativa=${attempt + 1}/4 pausaMs=$pauseMillis motivo=${error.javaClass.simpleName}")
                delay(pauseMillis)
                attempt++
            }
        }
    }

    private suspend fun <T> localCatalogWrite(action: suspend () -> T): T = try {
        action()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: XtreamCatalogWriteException) { throw error }
    catch (error: Exception) { throw XtreamCatalogWriteException(error) }

    private fun containsStorageFailure(error: Throwable, depth: Int = 0): Boolean {
        if (error is XtreamCatalogWriteException || error is android.database.sqlite.SQLiteException) return true
        if (depth >= 5) return false
        return error.cause?.let { it !== error && containsStorageFailure(it, depth + 1) } == true ||
            error.suppressed.any { it !== error && containsStorageFailure(it, depth + 1) }
    }

    private suspend fun sharedSync(source: PlaylistSource, operation: suspend () -> String): String {
        // Display-name edits do not trigger another download; credentials/URL changes do.
        val configuration = when (source) {
            is PlaylistSource.M3u -> source.copy(name = "", isEnabled = true)
            is PlaylistSource.XtreamCodes -> source.copy(name = "", isEnabled = true)
        }
        return shared.run(source.id, configuration) {
            val started = SystemClock.elapsedRealtime()
            val heapBefore = usedHeapKb()
            var outcome = "ERRO"
            try {
                br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.foregroundImport { operation() }.also { outcome = "SUCESSO" }
            } catch (cancelled: CancellationException) {
                outcome = "CANCELADA"; throw cancelled
            } finally {
                val kind = if (source is PlaylistSource.M3u) "M3U" else "XTREAM"
                Log.i(TAG, "SYNC_RESUMO tipo=$kind resultado=$outcome duracaoMs=${SystemClock.elapsedRealtime() - started} heapAntesKb=$heapBefore heapDepoisKb=${usedHeapKb()}")
            }
        }
    }

    /** Called only by the Channels route. Complete a staged live catalogue atomically. */
    suspend fun ensureChannels(source: PlaylistSource): Unit = liveMutex.withLock {
        val xtream = when (source) {
            is PlaylistSource.XtreamCodes -> source
            is PlaylistSource.M3u -> m3uTransport?.selected(source)
        } ?: return@withLock
        val repository = playlistRepository as? ProgressiveXtreamRepository ?: return@withLock
        val active = cacheDao.getActiveGeneration(source.id)?.generationId ?: error("Catálogo indisponível.")
        if (!cacheDao.liveCatalogPending(source.id, active)) return@withLock
        br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.foregroundImport {
            val stage = generationManager.beginImport(source.id)
            val writer = XtreamCatalogWriter(cacheDao, generationManager, source.id, stage, includeOnDemand = false)
            try {
                repository.importXtream(xtream, writer)
                currentCoroutineContext().ensureActive()
                check(cacheDao.publishLiveCatalog(source.id, stage, active)) { "A lista foi atualizada. Abra Canais novamente." }
            } finally {
                writer.closeStages()
                generationManager.discardImport(source.id, stage)
            }
        }
    }

    private fun usedHeapKb(): Long = Runtime.getRuntime().let { (it.totalMemory() - it.freeMemory()) / 1024 }

    private suspend fun syncFromXtream(
        playlistId: String,
        xtreamSource: PlaylistSource.XtreamCodes,
        includeLive: Boolean = false
    ): String {
        val syncStartedAt = SystemClock.elapsedRealtime()
        if (playlistRepository is ProgressiveXtreamRepository && playlistRepository.supportsProgressiveXtream) {
            return withContext(Dispatchers.IO) {
                val generation = localCatalogWrite { generationManager.beginImport(playlistId) }
                val writer = XtreamCatalogWriter(cacheDao, generationManager, playlistId, generation, includeLive = includeLive, bufferedTransactions = true)
                try {
                    playlistRepository.importXtream(xtreamSource, writer)
                    writer.flushPending()
                    writer.closeStages()
                    if (!includeLive) localCatalogWrite { cacheDao.insertCategories(listOf(CachedCategoryEntity(
                        playlistId, generation, "__live_pending__", "", "__LIVE_PENDING__", "XTREAM"))) }
                    currentCoroutineContext().ensureActive()
                    localCatalogWrite { generationManager.markImportReady(playlistId, generation) }
                    Log.i(TAG, "PERF etapa=XTREAM_TOTAL modo=STREAM duracaoMs=${SystemClock.elapsedRealtime()-syncStartedAt}")
                    generation
                } catch (error: Throwable) {
                    try { writer.closeStages() } catch (cleanup: Throwable) { if (cleanup !== error) error.addSuppressed(cleanup) }
                    try { localCatalogWrite { generationManager.discardImport(playlistId, generation) } } catch (cleanup: Throwable) { if (cleanup !== error) error.addSuppressed(cleanup) }
                    throw error
                }
            }
        }
        /*
         * Nesta etapa o Xtream ainda usa o repositório atual, que monta
         * PlaylistContent em memória. Isso vale tanto para Xtream Codes direto
         * quanto para M3U reconhecida como Xtream-compatible.
         *
         * O conteúdo resultante é persistido em uma nova geração do Room,
         * permitindo que a interface use o mesmo catálogo via Paging.
         *
         * Mais adiante substituiremos esta parte por parsing Xtream em
         * streaming direto para o Room.
         */
        val content = medirEtapa("XTREAM_REDE_E_PARSE") {
            playlistRepository.loadContent(
                source = xtreamSource
            )
        }

        if (
            content.categories.isEmpty() &&
            content.channels.isEmpty() &&
            content.movies.isEmpty() &&
            content.series.isEmpty()
        ) {
            throw IllegalStateException(
                "A API Xtream não retornou conteúdo para a lista."
            )
        }

        val generationId = medirEtapa("ROOM_BEGIN_IMPORT") {
            localCatalogWrite { generationManager.beginImport(playlistId = playlistId) }
        }

        try {
            medirEtapa("ROOM_GRAVACAO_TOTAL") {
                localCatalogWrite { persistContent(playlistId, generationId, content) }
            }

            currentCoroutineContext().ensureActive()
            medirEtapa("ROOM_MARK_READY") {
                localCatalogWrite { generationManager.markImportReady(playlistId, generationId) }
            }

            Log.i(
                TAG,
                buildString {
                    append("Sincronização Xtream concluída. ")
                    append("canais=")
                    append(content.channels.size)
                    append(", filmes=")
                    append(content.movies.size)
                    append(", series=")
                    append(content.series.size)
                    append(", episodios=")
                    append(content.episodes.size)
                }
            )

            Log.i(
                TAG,
                "PERF etapa=XTREAM_TOTAL " +
                    "duracaoMs=${SystemClock.elapsedRealtime() - syncStartedAt}"
            )

            return generationId
        } catch (error: Throwable) {
            try { localCatalogWrite { generationManager.discardImport(playlistId, generationId) } }
            catch (cleanup: Throwable) { if (cleanup !== error) error.addSuppressed(cleanup) }

            throw error
        }
    }

    private suspend fun persistContent(
        playlistId: String,
        generationId: String,
        content: PlaylistContent
    ) {
        medirEtapa("ROOM_CATEGORIAS") {
            persistCategories(
                playlistId = playlistId,
                generationId = generationId,
                content = content
            )
        }

        medirEtapa("ROOM_CANAIS") {
            persistChannels(
                playlistId = playlistId,
                generationId = generationId,
                content = content
            )
        }

        medirEtapa("ROOM_FILMES") {
            persistMovies(
                playlistId = playlistId,
                generationId = generationId,
                content = content
            )
        }

        medirEtapa("ROOM_SERIES") {
            persistSeries(
                playlistId = playlistId,
                generationId = generationId,
                content = content
            )
        }

        medirEtapa("ROOM_EPISODIOS") {
            persistEpisodes(
                playlistId = playlistId,
                generationId = generationId,
                content = content
            )
        }
    }

    private suspend fun persistCategories(
        playlistId: String,
        generationId: String,
        content: PlaylistContent
    ) {
        val categories =
            content.categories
                .distinctBy { category ->
                    "${category.type.name}:${category.id}"
                }

        var startIndex = 0

        while (startIndex < categories.size) {
            currentCoroutineContext().ensureActive()
            val endIndex =
                minOf(
                    startIndex + BATCH_SIZE,
                    categories.size
                )

            val batch =
                ArrayList<CachedCategoryEntity>(
                    endIndex - startIndex
                )

            for (index in startIndex until endIndex) {
                val category =
                    categories[index]

                batch +=
                    CachedCategoryEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        categoryId = category.id,
                        name = category.name,
                        type = category.type.name,
                        source = SOURCE_XTREAM
                    )
            }

            cacheDao.insertCategories(
                batch
            )

            startIndex = endIndex
        }
    }

    private suspend fun persistChannels(
        playlistId: String,
        generationId: String,
        content: PlaylistContent
    ) {
        var startIndex = 0

        while (startIndex < content.channels.size) {
            currentCoroutineContext().ensureActive()
            val endIndex =
                minOf(
                    startIndex + BATCH_SIZE,
                    content.channels.size
                )

            val batch =
                ArrayList<CachedChannelEntity>(
                    endIndex - startIndex
                )

            for (index in startIndex until endIndex) {
                val channel =
                    content.channels[index]

                batch +=
                    CachedChannelEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        channelId = channel.id,
                        name = channel.name,
                        categoryId = channel.categoryId,
                        logoUrl = channel.logoUrl,
                        streamUrl = channel.streamUrl,
                        epgChannelId = channel.epgChannelId,
                        channelNumber = index + 1
                    )
            }

            cacheDao.insertChannels(
                batch
            )

            startIndex = endIndex
        }
    }

    private suspend fun persistMovies(
        playlistId: String,
        generationId: String,
        content: PlaylistContent
    ) {
        var startIndex = 0

        while (startIndex < content.movies.size) {
            currentCoroutineContext().ensureActive()
            val endIndex =
                minOf(
                    startIndex + BATCH_SIZE,
                    content.movies.size
                )

            val batch =
                ArrayList<CachedMovieEntity>(
                    endIndex - startIndex
                )

            for (index in startIndex until endIndex) {
                val movie =
                    content.movies[index]

                batch +=
                    CachedMovieEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        movieId = movie.id,
                        name = movie.name,
                        categoryId = movie.categoryId,
                        posterUrl = movie.posterUrl,
                        backdropUrl = movie.backdropUrl,
                        streamUrl = movie.streamUrl,
                        year = movie.year,
                        rating = movie.rating,
                        durationSeconds = movie.durationSeconds,
                        addedAtEpochSeconds =
                            movie.addedAtEpochSeconds,
                        providerOrder =
                            index
                    )
            }

            cacheDao.insertMovies(
                batch
            )

            startIndex = endIndex
        }
    }

    private suspend fun persistSeries(
        playlistId: String,
        generationId: String,
        content: PlaylistContent
    ) {
        var startIndex = 0

        while (startIndex < content.series.size) {
            currentCoroutineContext().ensureActive()
            val endIndex =
                minOf(
                    startIndex + BATCH_SIZE,
                    content.series.size
                )

            val batch =
                ArrayList<CachedSeriesEntity>(
                    endIndex - startIndex
                )

            for (index in startIndex until endIndex) {
                val series =
                    content.series[index]

                batch +=
                    CachedSeriesEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        seriesId = series.id,
                        name = series.name,
                        categoryId = series.categoryId,
                        posterUrl = series.posterUrl,
                        backdropUrl = series.backdropUrl,
                        year = series.year,
                        rating = series.rating,
                        plot = series.plot,
                        titleLogoUrl = series.titleLogoUrl,
                        imdbId = series.imdbId,
                        tmdbId = series.tmdbId,
                        ratingSource =
                            series.ratingSource
                                ?.name,
                        durationSeconds =
                            series.durationSeconds,
                        genres =
                            series.genres
                                .takeIf {
                                    it.isNotEmpty()
                                }
                                ?.joinToString("|"),
                        trailerUrl = series.trailerUrl,
                        providerOrder = index
                    )
            }

            cacheDao.insertSeries(
                batch
            )

            startIndex = endIndex
        }
    }

    private suspend fun persistEpisodes(
        playlistId: String,
        generationId: String,
        content: PlaylistContent
    ) {
        var startIndex = 0

        while (startIndex < content.episodes.size) {
            currentCoroutineContext().ensureActive()
            val endIndex =
                minOf(
                    startIndex + BATCH_SIZE,
                    content.episodes.size
                )

            val batch =
                ArrayList<CachedEpisodeEntity>(
                    endIndex - startIndex
                )

            for (index in startIndex until endIndex) {
                val episode =
                    content.episodes[index]

                batch +=
                    CachedEpisodeEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        episodeId = episode.id,
                        seriesId = episode.seriesId,
                        seasonNumber = episode.seasonNumber,
                        episodeNumber = episode.episodeNumber,
                        name = episode.name,
                        streamUrl = episode.streamUrl,
                        posterUrl = episode.posterUrl,
                        durationSeconds = episode.durationSeconds
                    )
            }

            cacheDao.insertEpisodes(
                batch
            )

            startIndex = endIndex
        }
    }

    /**
     * Mede apenas o tempo. Nao altera o retorno nem engole erros.
     * Nunca registra URLs, usuarios, senhas ou conteudo recebido.
     */
    private suspend fun <T> medirEtapa(
        nome: String,
        operacao: suspend () -> T
    ): T {
        val inicio = SystemClock.elapsedRealtime()

        return try {
            val resultado = operacao()

            Log.i(
                TAG,
                "PERF etapa=$nome " +
                    "duracaoMs=${SystemClock.elapsedRealtime() - inicio}"
            )

            resultado
        } catch (error: Throwable) {
            Log.w(
                TAG,
                "PERF etapa=$nome falhou " +
                    "tipo=${error.javaClass.simpleName} " +
                    "duracaoMs=${SystemClock.elapsedRealtime() - inicio}"
            )
            throw error
        }
    }
    private companion object {
        val shared = SharedSync<PlaylistSource, String>(CoroutineScope(SupervisorJob() + Dispatchers.IO)) {
            Log.i(TAG, "SYNC_COMPARTILHADA pedido repetido reutilizou operacao em andamento")
        }
        val liveMutex = Mutex()
        const val TAG = "M3uSourceSync"
        const val BATCH_SIZE = 500
        const val SOURCE_XTREAM = "XTREAM"
    }
}
