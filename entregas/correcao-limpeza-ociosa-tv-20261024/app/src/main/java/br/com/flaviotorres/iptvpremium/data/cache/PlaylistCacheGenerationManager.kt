package br.com.flaviotorres.iptvpremium.data.cache

import android.util.Log
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.PlaylistCacheGenerationEntity
import kotlinx.coroutines.*
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaylistCacheGenerationManager(
    private val cacheDao: PlaylistCacheDao
) {

    suspend fun beginImport(
        playlistId: String
    ): String = lifecycleMutex.withLock {
        val generationId = UUID
            .randomUUID()
            .toString()

        val token = playlistId to generationId
        synchronized(importLock) { liveImports.add(token) }
        try {
            currentCoroutineContext().ensureActive()
            cacheDao.upsertGeneration(PlaylistCacheGenerationEntity(
                playlistId = playlistId, generationId = generationId,
                status = PlaylistCacheGenerationEntity.STATUS_IMPORTING,
                createdAtEpochMillis = System.currentTimeMillis()
            ))
            generationId
        } catch (error: Throwable) {
            try { discardImport(playlistId, generationId) }
            catch (cleanup: Throwable) { if (cleanup !== error) error.addSuppressed(cleanup) }
            throw error
        }
    }

    /** Caller holds the registry edit guard. Import starts share the lifecycle mutex. */
    suspend fun retireUnregisteredCatalogs(registered: Set<String>, beforeMillis: Long): Int =
        lifecycleMutex.withLock {
            val importing = synchronized(importLock) { liveImports.map { it.first }.toSet() }
            val protected = (registered + importing).toList().ifEmpty { listOf("") }
            cacheDao.retireUnregisteredActiveGenerations(protected, beforeMillis)
        }

    suspend fun markImportReady(
        playlistId: String,
        generationId: String
    ) {
        currentCoroutineContext().ensureActive()
        cacheDao.updateGenerationStatus(
            playlistId = playlistId,
            generationId = generationId,
            status = PlaylistCacheGenerationEntity.STATUS_READY,
            completedAtEpochMillis =
                System.currentTimeMillis()
        )
        synchronized(importLock) { liveImports.remove(playlistId to generationId) }
    }

    suspend fun discardImport(
        playlistId: String,
        generationId: String
    ) {
        withContext(NonCancellable + Dispatchers.IO) {
            try { cacheDao.discardIncompleteImport(playlistId, generationId) }
            finally { synchronized(importLock) { liveImports.remove(playlistId to generationId) } }
        }
    }

    /*
     * Ativa a geração READY mais recente.
     *
     * A troca ACTIVE -> STALE e READY -> ACTIVE continua atômica dentro
     * de PlaylistCacheDao.switchToReadyGeneration().
     *
     * A geração ACTIVE anterior NÃO é apagada aqui. Ela fica STALE e será
     * removida depois por cleanupOldestStaleGeneration(), fora do caminho
     * crítico da atualização da playlist.
     */
    suspend fun activateReadyGenerationOnStartup(
        playlistId: String
    ): String? {
        val readyGeneration =
            cacheDao.getReadyGeneration(
                playlistId = playlistId
            ) ?: return cacheDao
                .getActiveGeneration(
                    playlistId = playlistId
                )
                ?.generationId

        /*
         * MEDIÇÃO DA TROCA READY -> ACTIVE
         *
         * Estes logs existem somente para medir quanto tempo a transação
         * switchToReadyGeneration() leva. Assim conseguimos separar o custo
         * da troca no Room/SQLite do que acontece depois dela na interface.
         */
        val switchStartedAt =
            System.currentTimeMillis()

        Log.i(
            TAG,
            "Troca READY -> ACTIVE iniciada."
        )

        cacheDao.switchToReadyGeneration(
            playlistId = playlistId,
            generationId =
                readyGeneration.generationId,
            activatedAtEpochMillis =
                System.currentTimeMillis()
        )

        val switchDurationMillis =
            System.currentTimeMillis() -
                    switchStartedAt

        Log.i(
            TAG,
            "Troca READY -> ACTIVE concluída. duracaoMs=$switchDurationMillis"
        )

        return cacheDao.getActiveGeneration(playlistId)?.generationId
    }

    /*
     * Remove no máximo UMA geração por chamada: IMPORTING abandonada ou STALE.
     *
     * A exclusão é dividida em pequenos lotes para evitar uma transação
     * gigantesca e reduzir picos de WAL/SQLite.
     *
     * Retorna true quando uma geração abandonada ou STALE foi completamente removida.
     * Retorna false quando não havia geração elegível para limpeza.
     */
    suspend fun cleanupOldestStaleGeneration(playlistId: String): Boolean =
        cleanupMutex.withLock { cleanupOneGeneration(playlistId) }

    private suspend fun cleanupOneGeneration(playlistId: String): Boolean {
        if (cleanupAbandonedImport(playlistId)) return true
        val generationId =
            cacheDao.getOldestStaleGenerationId(
                playlistId = playlistId
            ) ?: run {
                Log.d(
                    TAG,
                    "Nenhuma geração STALE pendente para limpeza."
                )
                return false
            }

        val startedAt =
            System.currentTimeMillis()

        Log.i(
            TAG,
            "Limpeza STALE iniciada."
        )

        val deletedEpisodes =
            deleteInBatches {
                cacheDao.deleteEpisodesBatchForGeneration(
                    playlistId = playlistId,
                    generationId = generationId,
                    limit = CLEANUP_BATCH_SIZE
                )
            }

        val deletedSeries =
            deleteInBatches {
                cacheDao.deleteSeriesBatchForGeneration(
                    playlistId = playlistId,
                    generationId = generationId,
                    limit = CLEANUP_BATCH_SIZE
                )
            }

        val deletedMovies =
            deleteInBatches {
                cacheDao.deleteMoviesBatchForGeneration(
                    playlistId = playlistId,
                    generationId = generationId,
                    limit = CLEANUP_BATCH_SIZE
                )
            }

        val deletedChannels =
            deleteInBatches {
                cacheDao.deleteChannelsBatchForGeneration(
                    playlistId = playlistId,
                    generationId = generationId,
                    limit = CLEANUP_BATCH_SIZE
                )
            }

        val deletedCategories =
            deleteInBatches {
                cacheDao.deleteCategoriesBatchForGeneration(
                    playlistId = playlistId,
                    generationId = generationId,
                    limit = CLEANUP_BATCH_SIZE
                )
            }

        cacheDao.deleteGenerationRow(
            playlistId = playlistId,
            generationId = generationId
        )

        val durationMillis =
            System.currentTimeMillis() -
                    startedAt

        Log.i(
            TAG,
            buildString {
                append("Limpeza STALE concluída. ")
                append("categorias=")
                append(deletedCategories)
                append(", canais=")
                append(deletedChannels)
                append(", filmes=")
                append(deletedMovies)
                append(", series=")
                append(deletedSeries)
                append(", episodios=")
                append(deletedEpisodes)
                append(", duracaoMs=")
                append(durationMillis)
            }
        )

        return true
    }

    /** Called by the existing deferred maintenance job, never while downloading a new list. */
    private suspend fun cleanupAbandonedImport(playlistId: String): Boolean {
        val generationId = cacheDao.oldestAbandonedImport(playlistId, System.currentTimeMillis() - ABANDONED_AGE_MS) ?: return false
        val token = playlistId to generationId
        // The process lease also protects unusually long imports and wall-clock jumps.
        if (!synchronized(importLock) { liveImports.add(token) }) return false
        try {
            if (cacheDao.generationStatus(playlistId, generationId) != "IMPORTING") return false
            val started = android.os.SystemClock.elapsedRealtime()
            val episodes = deleteInBatches { cacheDao.deleteEpisodesBatchForGeneration(playlistId, generationId, CLEANUP_BATCH_SIZE) }
            val series = deleteInBatches { cacheDao.deleteSeriesBatchForGeneration(playlistId, generationId, CLEANUP_BATCH_SIZE) }
            val movies = deleteInBatches { cacheDao.deleteMoviesBatchForGeneration(playlistId, generationId, CLEANUP_BATCH_SIZE) }
            val channels = deleteInBatches { cacheDao.deleteChannelsBatchForGeneration(playlistId, generationId, CLEANUP_BATCH_SIZE) }
            val categories = deleteInBatches { cacheDao.deleteCategoriesBatchForGeneration(playlistId, generationId, CLEANUP_BATCH_SIZE) }
            cacheDao.deleteGenerationRow(playlistId, generationId)
            Log.i(TAG, "LIMPEZA_IMPORTACAO_ABANDONADA registros=${episodes + series + movies + channels + categories} duracaoMs=${android.os.SystemClock.elapsedRealtime() - started}")
            return true
        } finally { synchronized(importLock) { liveImports.remove(token) } }
    }

    private suspend fun deleteInBatches(
        deleteBatch: suspend () -> Int
    ): Int {
        var totalDeleted = 0

        while (true) {
            currentCoroutineContext().ensureActive()
            val deletedRows =
                deleteBatch()

            if (deletedRows <= 0) {
                return totalDeleted
            }

            totalDeleted +=
                deletedRows

            /*
             * Pausa curta entre lotes pequenos.
             *
             * O objetivo desta configuração é priorizar a fluidez da interface:
             * cada transação apaga menos linhas e o SQLite recebe mais tempo
             * entre as escritas para reduzir picos durante a limpeza STALE.
             */
            delay(CLEANUP_PAUSE_MILLIS)
        }
    }

    suspend fun getActiveGenerationId(
        playlistId: String
    ): String? {
        return cacheDao
            .getActiveGeneration(
                playlistId = playlistId
            )
            ?.generationId
    }

    suspend fun deletePlaylistCache(
        playlistId: String
    ) {
        cacheDao.deletePlaylistCache(
            playlistId = playlistId
        )
    }

    private companion object {
        private val cleanupMutex = Mutex()
        private val lifecycleMutex = Mutex()

        val importLock = Any()
        val liveImports = mutableSetOf<Pair<String, String>>()
        const val ABANDONED_AGE_MS = 24L * 60 * 60 * 1000
        const val TAG = "CacheGeneration"
        const val CLEANUP_BATCH_SIZE = 100
        const val CLEANUP_PAUSE_MILLIS = 25L
    }
}
