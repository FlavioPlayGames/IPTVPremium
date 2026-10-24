package br.com.flaviotorres.iptvpremium.core.player

/** Main-thread state only: native playback needs both media and confirmed video surfaces. */
internal class VlcSurfaceStartGate {
    private var opened = false
    private var ready = false
    private var requested = false
    var hasStarted = false
        private set
    fun prepare() { opened = true; hasStarted = false; requested = true }
    fun surfaceReady() { ready = true }
    fun surfaceLost(resume: Boolean) { ready = false; if (opened && resume) requested = true }
    fun requestPlay() { if (opened) requested = true }
    fun pause() { if (hasStarted) requested = false }
    fun takeStart(): Boolean {
        if (!opened || !ready || !requested) return false
        requested = false
        hasStarted = true
        return true
    }
    fun close() { opened = false; requested = false; ready = false; hasStarted = false }
}
