package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine

/** Selected media wins over category anticipation; never waits on a background job on the UI thread. */
internal class MediaPriorityGate {
    private val state=MutableStateFlow(false)
    val busy=state.asStateFlow()
    fun setBusy(value:Boolean) {state.value=value}
    suspend fun awaitIdle() {busy.first { !it }}
}
internal object MediaOpeningPriority {
    private val gate=MediaPriorityGate()
    val busy=gate.busy
    fun setBusy(value:Boolean)=gate.setBusy(value)
    suspend fun awaitIdle()=gate.awaitIdle()
    /** Watch only off-screen network IO; disconnect is never performed by the click handler. */
    fun interruptBackgroundOnSelection(disconnect:()->Unit):Job = scope.launch {
        combine(busy, CatalogImportPriority.busy) { selected, importing -> selected || importing }.first {it}
        runCatching(disconnect)
    }
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
}
