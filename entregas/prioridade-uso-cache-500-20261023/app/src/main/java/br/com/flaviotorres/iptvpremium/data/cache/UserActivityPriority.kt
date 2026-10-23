package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** No UI waits/joins. Every real input immediately revokes optional background work. */
internal class IdleWorkGate(private val scope:CoroutineScope,private val quietMillis:Long=10_000L) {
    private val mutableIdle=MutableStateFlow(false)
    val idle=mutableIdle.asStateFlow()
    private var visible=false
    private var timer:Job?=null
    private var revision=0L
    @Synchronized fun foreground(value:Boolean) { visible=value;restart() }
    @Synchronized fun activity() { restart() }
    private fun restart() {
        val token=++revision
        mutableIdle.value=false
        timer?.cancel()
        timer=if(visible) scope.launch {
            delay(quietMillis)
            synchronized(this@IdleWorkGate) {
                if(visible&&revision==token)mutableIdle.value=true
            }
        } else null
    }
}
internal object UserActivityPriority {
    private val gate=IdleWorkGate(CoroutineScope(SupervisorJob()+Dispatchers.Default))
    private val cleanup=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    val idle=gate.idle
    fun interruptBackground(disconnect:()->Unit):Job = cleanup.launch {
        combine(idle,MediaOpeningPriority.busy) {quiet,selected->!quiet||selected}.first{it}
        runCatching(disconnect)
    }
    fun activity()=gate.activity()
    fun foreground(visible:Boolean)=gate.foreground(visible)
}
