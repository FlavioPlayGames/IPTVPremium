package br.com.flaviotorres.iptvpremium.core.diagnostics

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Debug-only stack samples during sustained UI stalls; does not log content or addresses. */
internal class UiStallMonitor private constructor() : DefaultLifecycleObserver {
    companion object {
        fun install(lifecycle: Lifecycle) { lifecycle.addObserver(UiStallMonitor()) }
    }
    private val main = Handler(Looper.getMainLooper())
    private val mainThread = Looper.getMainLooper().thread
    private val lastBeat = AtomicLong()
    private val executor = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "iptv-ui-stall-sampler").apply { isDaemon = true }
    }
    private var sample: ScheduledFuture<*>? = null
    private var lastSampleAt = 0L
    private val heartbeat = object : Runnable {
        override fun run() {
            lastBeat.set(SystemClock.uptimeMillis())
            main.postDelayed(this, 200L)
        }
    }
    override fun onStart(owner: LifecycleOwner) {
        lastBeat.set(SystemClock.uptimeMillis())
        main.post(heartbeat)
        sample = executor.scheduleWithFixedDelay({
            val now = SystemClock.uptimeMillis()
            val delay = now - lastBeat.get()
            if (delay >= 400L && now - lastSampleAt >= 1_000L) {
                lastSampleAt = now
                val stack = mainThread.stackTrace.take(18).joinToString(" <- ") {
                    "${it.className}.${it.methodName}:${it.lineNumber}"
                }
                Log.i("UiStallPerf", "heartbeatDelayMs=$delay state=${mainThread.state} stack=$stack")
            }
        }, 200L, 200L, TimeUnit.MILLISECONDS)
    }
    override fun onStop(owner: LifecycleOwner) {
        sample?.cancel(false)
        sample = null
        main.removeCallbacks(heartbeat)
    }
    override fun onDestroy(owner: LifecycleOwner) {
        onStop(owner)
        executor.shutdownNow()
    }
}
