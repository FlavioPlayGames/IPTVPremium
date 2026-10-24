package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** No UI waits/joins. Every real input immediately revokes optional background work. */
internal class IdleWorkGate(
    private val scope: CoroutineScope,
    private val quietMillis: Long = 10_000L,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L }
) {
    private val mutableIdle = MutableStateFlow(false)
    val idle = mutableIdle.asStateFlow()
    private var visible = false
    private var timer: Job? = null
    private var deadline = 0L

    @Synchronized fun foreground(value: Boolean) {
        visible = value
        restart()
    }
    @Synchronized fun activity() { restart() }

    private fun restart() {
        mutableIdle.value = false
        deadline = nowMillis() + quietMillis.coerceAtLeast(0L)
        if (!visible) {
            timer?.cancel()
            timer = null
            return
        }
        if (timer?.isActive == true) return
        // Repeated remote input only extends the deadline, without allocating another Job.
        timer = scope.launch(start = CoroutineStart.LAZY) {
            while (isActive) {
                val remaining = synchronized(this@IdleWorkGate) {
                    if (!visible) return@launch
                    val left = deadline - nowMillis()
                    if (left <= 0L) {
                        timer = null
                        mutableIdle.value = true
                        return@launch
                    }
                    left
                }
                delay(remaining)
            }
        }
        timer?.start()
    }
}

internal object UserActivityPriority {
    private val gate=IdleWorkGate(CoroutineScope(SupervisorJob()+Dispatchers.Default))
    private val cleanup=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    val idle=gate.idle
    fun interruptBackground(disconnect:()->Unit):Job = cleanup.launch {
        combine(idle,MediaOpeningPriority.busy,CatalogImportPriority.busy) {quiet,selected,importing->!quiet||selected||importing}.first{it}
        runCatching(disconnect)
    }
    fun activity()=gate.activity()
    fun foreground(visible:Boolean)=gate.foreground(visible)
}
