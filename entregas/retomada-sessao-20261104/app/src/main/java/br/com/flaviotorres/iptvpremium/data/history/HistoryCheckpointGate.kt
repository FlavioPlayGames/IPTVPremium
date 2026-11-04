package br.com.flaviotorres.iptvpremium.data.history

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** One playback owner. Closing joins an active local write, so a replacement cannot be overwritten. */
internal class HistoryCheckpointGate {
    val lock=Any()
    val sequence=AtomicLong(0L)
    val closed=AtomicBoolean(false)
    private var applied=0L
    fun accept(event:Long):Boolean=synchronized(lock) {
        if(closed.get() || event<applied)false else {applied=event;true}
    }
    fun close(){closed.set(true);synchronized(lock){}}
    fun reopen(){closed.set(false)}
}
