package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Reference counted: overlapping/cancelled imports cannot release another import's priority. */
internal object CatalogImportPriority {
    private val count = MutableStateFlow(0)
    private val active = MutableStateFlow(false)
    val busy = active.asStateFlow()
    @Synchronized private fun enter() { count.update { it + 1 }; active.value = true }
    @Synchronized private fun leave() { count.update { it - 1 }; active.value = count.value > 0 }
    suspend fun <T> run(block: suspend () -> T): T {
        enter()
        try { return block() } finally { leave() }
    }
}
