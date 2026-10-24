package br.com.flaviotorres.iptvpremium.core.storage

import android.util.Log
import br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority
import br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority
import br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Imports preempt maintenance; visible interaction also cancels the current cleanup pass. */
internal class ForegroundStorageGate(
    private val userIdle: StateFlow<Boolean> = UserActivityPriority.idle,
    private val foregroundVisible: StateFlow<Boolean> = UserActivityPriority.foregroundVisible,
    private val mediaBusy: StateFlow<Boolean> = MediaOpeningPriority.busy,
    private val importBusy: StateFlow<Boolean> = CatalogImportPriority.busy
) {
    private data class State(val opening: Boolean = true, val imports: Int = 0, val revision: Long = 0) {
        val idle get() = !opening && imports == 0
    }
    private val state = MutableStateFlow(State())
    private val maintenanceLock = Mutex()

    fun openingStarted() { state.update { it.copy(opening = true, revision = it.revision + 1) } }
    fun homeReady() { state.update { if (it.opening) it.copy(opening = false, revision = it.revision + 1) else it } }

    suspend fun <T> foreground(block: suspend () -> T): T {
        state.update { it.copy(imports = it.imports + 1, revision = it.revision + 1) }
        try {
            maintenanceLock.withLock { }
            return block()
        } finally {
            state.update { it.copy(imports = it.imports - 1, revision = it.revision + 1) }
        }
    }

    suspend fun maintenanceAfterIdle(quietMillis: Long, block: suspend () -> Unit) {
        channelFlow {
            combine(state, mediaBusy, importBusy) { observed, selectedMedia, importing ->
                Triple(observed, selectedMedia, importing)
            }.collectLatest { (observed, selectedMedia, importing) ->
                if (observed.idle && !selectedMedia && !importing) {
                    // Keep the existing opening/import grace period. Input only gates the
                    // work itself; it does not restart the entire 60-second initial grace.
                    delay(quietMillis)
                    combine(userIdle, foregroundVisible) { idle, visible -> !visible || idle }
                        .distinctUntilChanged()
                        .collectLatest { allowed ->
                            if (allowed) maintenanceLock.withLock {
                                if (state.value == observed && !mediaBusy.value && !importBusy.value &&
                                    (!foregroundVisible.value || userIdle.value)) {
                                    var completed = false
                                    Log.i("StorageMaintenancePerf", "phase=start foreground=${foregroundVisible.value}")
                                    try {
                                        block()
                                        completed = true
                                        send(Unit)
                                    } finally {
                                        Log.i("StorageMaintenancePerf", if (completed) "phase=complete" else "phase=paused")
                                    }
                                }
                            }
                        }
                }
            }
        }.first()
    }
}
