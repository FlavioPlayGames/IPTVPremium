package br.com.flaviotorres.iptvpremium.data.diagnostics

import android.util.Log
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

/** Closed labels only. Never accept URLs, identifiers, exception messages or content. */
object ImportDiagnostics {
    enum class Phase { HEADERS, READ, PARSE, MAP_AND_WRITE, MERGE, READY }
    enum class Kind { XTREAM, XTREAM_INDEX, XTREAM_MOVIES, XTREAM_SERIES, XTREAM_CHANNELS, M3U }
    private val sequence = AtomicLong()
    private class Trace(val id: Long) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Trace>
        val totals = Array(Phase.values().size) { AtomicLong() }
        val calls = Array(Phase.values().size) { AtomicLong() }
    }
    suspend fun record(phase: Phase, nanos: Long) {
        currentCoroutineContext()[Trace]?.let {
            it.totals[phase.ordinal].addAndGet(nanos)
            it.calls[phase.ordinal].incrementAndGet()
        }
    }
    suspend fun <T> measure(phase: Phase, block: suspend () -> T): T {
        val start = System.nanoTime()
        try { return block() } finally { record(phase, System.nanoTime() - start) }
    }
    suspend fun <T> session(kind: Kind, block: suspend () -> T): T {
        val trace = Trace(sequence.incrementAndGet())
        return withContext(trace) {
            val start = System.nanoTime()
            var outcome = "ERROR"
            try { block().also { outcome = "OK" } }
            catch (cancelled: CancellationException) { outcome = "CANCELLED"; throw cancelled }
            finally {
                val phases = Phase.values().joinToString(" ") {
                    "${it.name}Ms=${trace.totals[it.ordinal].get()/1_000_000} ${it.name}Calls=${trace.calls[it.ordinal].get()}"
                }
                Log.i("ImportTiming", "v=1 run=${trace.id} kind=${kind.name} outcome=$outcome totalMs=${(System.nanoTime()-start)/1_000_000} $phases")
            }
        }
    }
}
