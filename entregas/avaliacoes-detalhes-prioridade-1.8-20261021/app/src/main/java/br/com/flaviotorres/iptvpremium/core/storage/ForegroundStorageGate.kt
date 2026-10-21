package br.com.flaviotorres.iptvpremium.core.storage

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Cleanup yields cooperatively; catalog imports wait only for cleanup cancellation. */
internal class ForegroundStorageGate {
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
            // collectLatest cancels a running maintenance pass when state changes.
            maintenanceLock.withLock { }
            return block()
        } finally {
            state.update { it.copy(imports = it.imports - 1, revision = it.revision + 1) }
        }
    }

    suspend fun maintenanceAfterIdle(quietMillis: Long, block: suspend () -> Unit) {
        channelFlow {
            combine(state, br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy) { observed, selectedMedia -> observed to selectedMedia }
                .collectLatest { (observed, selectedMedia) ->
                if (observed.idle && !selectedMedia) {
                    delay(quietMillis)
                    maintenanceLock.withLock {
                        if (state.value == observed) {
                            block()
                            send(Unit)
                        }
                    }
                }
            }
        }.first()
    }
}
