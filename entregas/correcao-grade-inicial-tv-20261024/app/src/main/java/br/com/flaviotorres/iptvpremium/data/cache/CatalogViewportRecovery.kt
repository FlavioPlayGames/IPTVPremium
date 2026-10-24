package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class CatalogViewportSnapshot(
    val first: Int, val last: Int, val present: Int, val total: Int,
    val loading: Boolean, val error: Boolean
) {
    val stalled: Boolean get() = first >= 0 && last >= first && total > first && present == 0 && !loading
}

/** One local retry/refresh per category session, only after an idle empty viewport persists. */
internal suspend fun recoverCatalogViewport(states: Flow<CatalogViewportSnapshot>, recover: (CatalogViewportSnapshot) -> Unit) {
    var attempted = false
    states.distinctUntilChanged().collectLatest { state ->
        if (attempted || !state.stalled) return@collectLatest
        delay(1_500L)
        attempted = true
        recover(state)
    }
}
