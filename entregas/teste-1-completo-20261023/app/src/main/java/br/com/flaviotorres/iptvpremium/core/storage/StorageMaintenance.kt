package br.com.flaviotorres.iptvpremium.core.storage

import android.content.Context
import android.util.Log
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** One process-wide worker: UI navigation and repeated imports do not cancel pending cleanup. */
internal object StorageMaintenance {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var worker: Job? = null
    private val priority = ForegroundStorageGate()
    fun openingStarted() { priority.openingStarted() }
    fun homeReady(context: Context) { priority.homeReady(); request(context) }
    suspend fun <T> foregroundImport(block: suspend () -> T): T = br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.run { priority.foreground(block) }


    @Synchronized fun request(context: Context) {
        val app = context.applicationContext
        if (worker?.isActive != true) {
            worker = scope.launch {
                var firstPass = true
                while (isActive) {
                    withTimeoutOrNull(15 * 60_000L) { requests.receive() }
                    priority.maintenanceAfterIdle(if (firstPass) 60_000L else 5_000L) {
                        while (requests.tryReceive().isSuccess) { /* coalesce notifications */ }
                        try {
                            StorageDiagnostics.log(app, "before")
                            val images = ImageDiskCache.get(app)
                            images.cleanupAbandonedTemporaries(System.currentTimeMillis() - 24L * 60 * 60 * 1000)
                            images.maintain()
                            val dao = PlaylistCacheDatabase.getInstance(app).playlistCacheDao()
                            br.com.flaviotorres.iptvpremium.data.cache.importer.M3uDeferredStore(
                                java.io.File(app.noBackupFilesDir,"m3u_deferred_native_test1")
                            ).prune(dao.protectedDeferredGenerations().toSet())
                            val manager = PlaylistCacheGenerationManager(dao)
                            try {
                                br.com.flaviotorres.iptvpremium.data.storage.PlaylistStorage(app)
                                    .withProtectedCatalogIds { protected, registeredCount ->
                                        val catalogIds = dao.catalogPlaylistIds()
                                        val retired = manager.retireUnregisteredCatalogs(
                                            protected, System.currentTimeMillis() - 15L * 60 * 1000
                                        )
                                        Log.i("IPTVStorage", "phase=registry registered_lists=$registeredCount " +
                                            "catalog_playlists=${catalogIds.size} " +
                                            "unregistered_catalogs=${catalogIds.count { it !in protected }} " +
                                            "retired_generations=$retired")
                                    }
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (error: Exception) {
                                // Continue existing STALE cleanup, but never infer an empty registry from failure.
                                Log.w("IPTVStorage", "orphan_check_deferred type=${error.javaClass.simpleName}")
                            }
                            val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
                            for (playlist in dao.playlistsPendingStorageCleanup(cutoff)) {
                                ensureActive()
                                try {
                                    while (manager.cleanupOldestStaleGeneration(playlist)) {
                                        delay(250L)
                                    }
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Exception) {
                                    Log.w("IPTVStorage", "cleanup_deferred type=${error.javaClass.simpleName}")
                                }
                            }
                            StorageDiagnostics.log(app, "after")
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (error: Exception) {
                            Log.w("IPTVStorage", "maintenance_deferred type=${error.javaClass.simpleName}")
                        }
                        firstPass = false
                    }
                }
            }
        }
        requests.trySend(Unit)
    }
}
