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
    private var routeBusy=false
    private val owners=hashSetOf<Any>()
    @Synchronized fun setBusy(value:Boolean) {routeBusy=value;publish()}
    @Synchronized fun acquire():()->Unit {
        val owner=Any();owners.add(owner);publish()
        return { release(owner) }
    }
    @Synchronized private fun release(owner:Any) {owners.remove(owner);publish()}
    private fun publish(){state.value=routeBusy || owners.isNotEmpty()}
    suspend fun awaitIdle() {busy.first { !it }}
}
internal object MediaOpeningPriority {
    private val gate=MediaPriorityGate()
    val busy=gate.busy
    fun setBusy(value:Boolean)=gate.setBusy(value)
    fun acquire():()->Unit=gate.acquire()
    suspend fun awaitIdle()=gate.awaitIdle()
    /** Watch only off-screen network IO; disconnect is never performed by the click handler. */
    fun interruptBackgroundOnSelection(disconnect:()->Unit):Job = scope.launch {
        combine(busy, CatalogImportPriority.busy) { selected, importing -> selected || importing }.first {it}
        runCatching(disconnect)
    }
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
}
