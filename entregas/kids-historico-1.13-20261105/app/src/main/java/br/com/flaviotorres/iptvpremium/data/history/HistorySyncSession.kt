package br.com.flaviotorres.iptvpremium.data.history

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/** Process-local run ledger: background/resume never creates another import. No disk/user data. */
internal class HistorySyncSession(private val kids:Boolean=false) {
    enum class Phase { PENDING, RUNNING, COMPLETED, FAILED }
    data class Run(val id: Long, val playlist: String, val announce: Boolean,
        val phase: Phase = Phase.PENDING, val announced: Boolean = false, val origin:String?=null)
    private var serial = 0L
    private var cacheObserved = false
    private val mutableState = MutableStateFlow<Run?>(null)
    val state = mutableState.asStateFlow()

    @Synchronized fun request(playlist: String, announce: Boolean = false, restart: Boolean = false, origin:String?=null): Run {
        require(playlist.isNotBlank())
        val existing = mutableState.value
        if (!restart && existing?.playlist == playlist) return existing
        return Run(++serial, playlist, announce, origin=origin).also { mutableState.value = it }
    }
    @Synchronized fun start(id: Long): Boolean {
        val run = mutableState.value ?: return false
        if (run.id != id || run.phase != Phase.PENDING) return false
        mutableState.value = run.copy(phase = Phase.RUNNING)
        return true
    }
    /** A replacement screen waits for the canceled executor to release, rather than losing the run. */
    suspend fun awaitStart(id: Long): Boolean {
        while (true) {
            val run = state.first { it?.id != id || it.phase != Phase.RUNNING }
            if (run?.id != id || run.phase != Phase.PENDING) return false
            if (start(id)) return true
        }
    }
    @Synchronized fun complete(id: Long, checkpoint:()->Unit = {}): Boolean {
        val run=mutableState.value?:return false
        if(run.id!=id || run.phase!=Phase.RUNNING)return false
        checkpoint()
        return finish(id,Phase.COMPLETED)
    }
    @Synchronized fun fail(id: Long): Boolean = finish(id, Phase.FAILED)
    private fun finish(id: Long, phase: Phase): Boolean {
        val run = mutableState.value ?: return false
        if (run.id != id || run.phase != Phase.RUNNING) return false
        mutableState.value = run.copy(phase = phase)
        return true
    }
    @Synchronized fun release(id: Long) {
        val run = mutableState.value ?: return
        if (run.id == id && run.phase == Phase.RUNNING) mutableState.value = run.copy(phase = Phase.PENDING)
    }
    @Synchronized fun consumeCompletion(id: Long): Boolean {
        val run = mutableState.value ?: return false
        if (run.id != id || !run.announce || run.announced || run.phase != Phase.COMPLETED) return false
        mutableState.value = run.copy(announced = true)
        return true
    }
    /** Only presence is stored in disposable cache, never a persisted completion flag. */
    @Synchronized fun cacheWasCleared(cacheDir: java.io.File): Boolean {
        val marker = java.io.File(cacheDir, HistoryProfile.preferences("history-session",kids)+".marker")
        if (marker.isFile) { cacheObserved = true; return false }
        return try {
            cacheDir.mkdirs()
            marker.writeText("1")
            val cleared = cacheObserved
            cacheObserved = true
            cleared
        } catch (_: Exception) { false }
    }
    @Synchronized fun reset() { ++serial; mutableState.value = null; cacheObserved = false }

    companion object { val process = HistorySyncSession(); val kidsProcess = HistorySyncSession(true) }
}
