package br.com.flaviotorres.iptvpremium.core.player

/** Transition-only timings: no URL, title, credentials or per-frame polling. */
internal class PlaybackLoadTrace(private val startedAtMs: Long, private val emit: (String) -> Unit) {
    private var lastStatus: PlaybackStatus? = null
    private var statusAtMs = startedAtMs
    private var rendered = false
    fun adapterReady(nowMs: Long) { emit("phase=adapter_ready elapsedMs=${(nowMs-startedAtMs).coerceAtLeast(0)}") }
    fun status(status: PlaybackStatus, nowMs: Long, bufferedMs: Long?) {
        if (lastStatus == status) return
        emit("phase=state state=$status previous=${lastStatus ?: "NONE"} elapsedMs=${(nowMs-startedAtMs).coerceAtLeast(0)} previousMs=${(nowMs-statusAtMs).coerceAtLeast(0)} bufferedMs=${bufferedMs ?: -1}")
        lastStatus = status
        statusAtMs = nowMs
    }
    fun firstFrame(nowMs: Long) {
        if (rendered) return
        rendered = true
        emit("phase=first_frame elapsedMs=${(nowMs-startedAtMs).coerceAtLeast(0)}")
    }
}
