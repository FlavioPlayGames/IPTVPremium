package br.com.flaviotorres.iptvpremium.core.storage

import android.util.Log
import br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority
import br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority
import br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            val phase = MutableStateFlow("gating")
            // Lightweight state logs only: no filesystem/SQLite work while blocked.
            val diagnostics = launch {
                combine(state, mediaBusy, importBusy, userIdle, foregroundVisible) { observed, media, importing, idle, visible ->
                    StorageMaintenanceWait(observed.opening, observed.imports > 0 || importing, media, idle, visible)
                }.combine(phase) { waiting, stage -> waiting to stage }
                    .collectLatest { (waiting, stage) ->
                        val reason = waiting.reason(stage)
                        if (stage != "running") {
                            while (true) {
                                Log.i("StorageMaintenancePerf", "phase=waiting reason=$reason " +
                                    "opening=${waiting.opening} importing=${waiting.importing} mediaBusy=${waiting.media} " +
                                    "userIdle=${waiting.idle} foreground=${waiting.visible} graceMs=$quietMillis")
                                delay(30_000L)
                            }
                        }
                    }
            }
            try {
            combine(state, mediaBusy, importBusy) { observed, selectedMedia, importing ->
                Triple(observed, selectedMedia, importing)
            }.collectLatest { (observed, selectedMedia, importing) ->
                if (observed.idle && !selectedMedia && !importing) {
                    // Keep the existing opening/import grace period. Input only gates the
                    // work itself; it does not restart the entire 60-second initial grace.
                    phase.value = "grace"
                    delay(quietMillis)
                    phase.value = "idle"
                    combine(userIdle, foregroundVisible) { idle, visible -> !visible || idle }
                        .distinctUntilChanged()
                        .collectLatest { allowed ->
                            if (allowed) maintenanceLock.withLock {
                                if (state.value == observed && !mediaBusy.value && !importBusy.value &&
                                    (!foregroundVisible.value || userIdle.value)) {
                                    phase.value = "running"
                                    var completed = false
                                    Log.i("StorageMaintenancePerf", "phase=start foreground=${foregroundVisible.value}")
                                    try {
                                        block()
                                        completed = true
                                        send(Unit)
                                    } finally {
                                        Log.i("StorageMaintenancePerf", if (completed) "phase=complete" else "phase=paused")
                                        phase.value = "gating"
                                    }
                                }
                            }
                        }
                } else {
                    phase.value = "gating"
                }
            }
            } finally { diagnostics.cancel() }
        }.first()
    }
}


/** Reason precedence is diagnostic only; it never grants permission to run cleanup. */
internal data class StorageMaintenanceWait(
    val opening: Boolean,
    val importing: Boolean,
    val media: Boolean,
    val idle: Boolean,
    val visible: Boolean
) {
    fun reason(phase: String): String = when {
        opening -> "opening"
        importing -> "import"
        media -> "media"
        phase == "grace" -> "grace"
        visible && !idle -> "user_activity"
        else -> "eligible"
    }
}
