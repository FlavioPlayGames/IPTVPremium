package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent
import kotlinx.coroutines.*

internal class XtreamCatalogWriter(
    private val cacheDao: PlaylistCacheDao,
    private val manager: PlaylistCacheGenerationManager,
    private val playlistId: String,
    private val generationId: String,
    private val firstWins: Boolean = false,
    override val includeLive: Boolean = true,
    override val includeOnDemand: Boolean = true,
    private val bufferedTransactions: Boolean = false
) : XtreamCatalogSink {
    // Bounded entity buffers only; no transaction remains open while receiving HTTP data.
    private val pendingMovies = ArrayList<CachedMovieEntity>()
    private val pendingSeries = ArrayList<CachedSeriesEntity>()

    suspend fun flushPending(): Unit = localWrite {
        flushMovies()
        flushSeries()
    }

    private suspend fun flushMovies() {
        if (pendingMovies.isEmpty()) return
        currentCoroutineContext().ensureActive()
        val start = System.nanoTime()
        val count = pendingMovies.size
        if (firstWins) cacheDao.insertMoviesFirst(pendingMovies) else cacheDao.insertMovies(pendingMovies)
        pendingMovies.clear()
        android.util.Log.i("XtreamWritePerf", "type=movies rows=$count transactionLimit=2000 durationMs=${(System.nanoTime()-start)/1_000_000}")
    }

    private suspend fun flushSeries() {
        if (pendingSeries.isEmpty()) return
        currentCoroutineContext().ensureActive()
        val start = System.nanoTime()
        val count = pendingSeries.size
        if (firstWins) cacheDao.insertSeriesFirst(pendingSeries) else cacheDao.insertSeries(pendingSeries)
        pendingSeries.clear()
        android.util.Log.i("XtreamWritePerf", "type=series rows=$count transactionLimit=2000 durationMs=${(System.nanoTime()-start)/1_000_000}")
    }

    private var channelOffset = 0
    private var movieOffset = 0
    private var seriesOffset = 0
    private val seenCategories = mutableSetOf<String>()
    private val stages = java.util.concurrent.ConcurrentHashMap<String, XtreamCatalogStage>()
    private val mergedIds = mutableSetOf<String>()

    private suspend fun <T> localWrite(action: suspend () -> T): T = try {
        action()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: XtreamCatalogWriteException) { throw error }
    catch (error: Exception) { throw XtreamCatalogWriteException(error) }

    override suspend fun append(endpoint: String, content: PlaylistContent) {
        try {
            currentCoroutineContext().ensureActive()
            persistCategories(playlistId, generationId, content)
            persistChannels(playlistId, generationId, content)
            persistMovies(playlistId, generationId, content)
            persistSeries(playlistId, generationId, content)
            channelOffset += content.channels.size
            movieOffset += content.movies.size
            seriesOffset += content.series.size
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { throw XtreamCatalogWriteException(error) }
    }

    override suspend fun reset(endpoint: String): Unit = localWrite {
        when (endpoint) {
            "get_vod_streams" -> { pendingMovies.clear(); cacheDao.deleteMoviesForGeneration(playlistId, generationId); movieOffset = 0 }
            "get_series" -> { pendingSeries.clear(); cacheDao.deleteSeriesForGeneration(playlistId, generationId); seriesOffset = 0 }
            else -> error("Unsupported reset")
        }
        mergedIds.clear()
    }

    override suspend fun stage(endpoint: String): XtreamCatalogStage = localWrite {
        // No cancellation gap between registering the import lease and registering its owner.
        withContext(NonCancellable) {
            val id = manager.beginImport(playlistId)
            XtreamCatalogStage(id, endpoint, XtreamCatalogWriter(cacheDao, manager, playlistId, id, true))
                .also { stages[id] = it }
        }
    }

    override suspend fun merge(stage: XtreamCatalogStage): Unit = localWrite {
        check(stages[stage.generationId] === stage)
        var afterOrder = -1
        while (true) {
            currentCoroutineContext().ensureActive()
            val size = when (stage.endpoint) {
                "get_vod_streams" -> {
                    val rows = cacheDao.streamMovieStage(playlistId, stage.generationId, BATCH_SIZE, afterOrder)
                    val accepted = rows.filter { mergedIds.add(it.movieId) }.mapIndexed { i, row ->
                        row.copy(generationId = generationId, providerOrder = movieOffset + i)
                    }
                    if (accepted.isNotEmpty()) cacheDao.insertMovies(accepted)
                    movieOffset += accepted.size
                    rows.lastOrNull()?.let { afterOrder = checkNotNull(it.providerOrder) }
                    rows.size
                }
                "get_series" -> {
                    val rows = cacheDao.streamSeriesStage(playlistId, stage.generationId, BATCH_SIZE, afterOrder)
                    val accepted = rows.filter { mergedIds.add(it.seriesId) }.mapIndexed { i, row ->
                        row.copy(generationId = generationId, providerOrder = seriesOffset + i)
                    }
                    if (accepted.isNotEmpty()) cacheDao.insertSeries(accepted)
                    seriesOffset += accepted.size
                    rows.lastOrNull()?.let { afterOrder = checkNotNull(it.providerOrder) }
                    rows.size
                }
                else -> error("Unsupported stage")
            }
            if (size == 0) break
        }
    }

    override suspend fun discard(stage: XtreamCatalogStage): Unit = localWrite {
        if (stages[stage.generationId] === stage) {
            manager.discardImport(playlistId, stage.generationId)
            stages.remove(stage.generationId, stage)
        }
    }

    suspend fun closeStages() = withContext(NonCancellable + Dispatchers.IO) {
        var failure: Throwable? = null
        for (stage in stages.values.toList()) {
            try { discard(stage) }
            catch (error: Throwable) { if (failure == null) failure = error else failure.addSuppressed(error) }
        }
        failure?.let { throw it }
    }

    companion object { private const val BATCH_SIZE: Int = 500; private const val SOURCE_XTREAM = "XTREAM" }
    private suspend fun persistCategories(
        playlistId: String,
        generationId: String,
        content: PlaylistContent
    ) {
        val categories =
            content.categories
                .filter { seenCategories.add("${it.type.name}:${it.id}") }
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
                        channelNumber = channelOffset + index + 1
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
                            movieOffset + index
                    )
            }

            if (bufferedTransactions) {
                for (row in batch) {
                    pendingMovies.add(row)
                    if (pendingMovies.size == 2000) flushMovies()
                }
            } else if (firstWins) cacheDao.insertMoviesFirst(batch) else cacheDao.insertMovies(batch)

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
                        providerOrder = seriesOffset + index
                    )
            }

            if (bufferedTransactions) {
                for (row in batch) {
                    pendingSeries.add(row)
                    if (pendingSeries.size == 2000) flushSeries()
                }
            } else if (firstWins) cacheDao.insertSeriesFirst(batch) else cacheDao.insertSeries(batch)

            startIndex = endIndex
        }
    }


}
