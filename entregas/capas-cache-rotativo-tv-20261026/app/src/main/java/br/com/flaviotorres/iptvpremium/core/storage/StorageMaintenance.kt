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
    fun openingStarted() {
        Log.i("StorageMaintenancePerf", "phase=signal reason=opening_started")
        priority.openingStarted()
    }
    fun homeReady(context: Context) {
        Log.i("StorageMaintenancePerf", "phase=signal reason=home_ready")
        priority.homeReady(); request(context)
    }
    suspend fun <T> foregroundImport(block: suspend () -> T): T = br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.run { priority.foreground(block) }


    @Synchronized fun request(context: Context) {
        val app = context.applicationContext
        Log.i("StorageMaintenancePerf", "phase=request workerActive=${worker?.isActive == true}")
        if (worker?.isActive != true) {
            worker = scope.launch {
                var firstPass = true
                while (isActive) {
                    Log.i("StorageMaintenancePerf", "phase=waiting reason=request_or_periodic intervalMs=900000")
                    val requested = withTimeoutOrNull(15 * 60_000L) { requests.receive() }
                    Log.i("StorageMaintenancePerf", "phase=wake reason=${if (requested != null) "request" else "periodic"}")
                    priority.maintenanceAfterIdle(if (firstPass) 60_000L else 5_000L) {
                        while (requests.tryReceive().isSuccess) { /* coalesce notifications */ }
                        try {
                            val cleanupStarted = System.nanoTime()
                            val before = StorageDiagnostics.log(app, "before")
                            ensureActive()
                            val cacheAllowance = AppStoragePolicy.cacheAllowance(before)
                            AppCacheStorage.get(app).setTotalLimit(cacheAllowance)
                            Log.i("StorageCleanupPerf", "phase=cache_policy allowanceBytes=$cacheAllowance " +
                                "artworkPrefetch=${AppCacheStorage.get(app).allowsArtworkPrefetch()} " +
                                "workingCacheBytes=${AppStoragePolicy.WORKING_CACHE} targetBytes=${AppStoragePolicy.TARGET}")
                            ensureActive()
                            val images = ImageDiskCache.get(app)
                            images.cleanupAbandonedTemporaries(System.currentTimeMillis() - 24L * 60 * 60 * 1000)
                            images.maintain()
                            val dao = PlaylistCacheDatabase.getInstance(app).playlistCacheDao()
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
                            val after = StorageDiagnostics.log(app, "after")
                            ensureActive()
                            val restoredAllowance = AppStoragePolicy.cacheAllowance(after)
                            AppCacheStorage.get(app).setTotalLimit(restoredAllowance)
                            Log.i("StorageCleanupPerf", "durationMs=${(System.nanoTime()-cleanupStarted)/1_000_000} " +
                                "beforeBytes=${before.totalBytes} afterBytes=${after.totalBytes} " +
                                "totalDeltaBytes=${before.totalBytes-after.totalBytes} " +
                                "managedCacheDeltaBytes=${before.managedCacheBytes-after.managedCacheBytes} " +
                                "cacheAllowanceBytes=$cacheAllowance restoredCacheAllowanceBytes=$restoredAllowance targetBytes=${AppStoragePolicy.TARGET}")
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
