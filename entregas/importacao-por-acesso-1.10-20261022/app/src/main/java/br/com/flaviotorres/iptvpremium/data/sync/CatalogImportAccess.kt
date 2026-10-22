package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.domain.model.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UI signals only: no Activity, coroutine job, credentials or database retained here. */
internal object CatalogImportAccess {
    private val requested = MutableStateFlow<CatalogAccess?>(null)
    val access = requested.asStateFlow()
    val lastInteraction = MutableStateFlow(0L)
    fun interaction() { lastInteraction.value = android.os.SystemClock.elapsedRealtime() }
    val environment = MutableStateFlow(false)
    private val changed = MutableStateFlow(0L)
    val revision = changed.asStateFlow()
    private val _status = MutableStateFlow<CategoryImportStatus?>(null)
    val status = _status.asStateFlow()
    fun request(playlist: String?, type: CategoryType, category: String? = null) {
        if (!playlist.isNullOrBlank()) requested.value = CatalogAccess(playlist, type, category?.takeIf { it.isNotBlank() })
    }
    fun leave() { requested.value = null }
    fun completed() { synchronized(this) { changed.value += 1 } }
    fun report(value: CategoryImportStatus?) { _status.value = value }
}
internal data class CategoryImportStatus(val playlist: String, val type: CategoryType, val category: String?, val failed: Boolean)
