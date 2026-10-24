package br.com.flaviotorres.iptvpremium.core.player

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/** Serial native shutdown outlives the screen; new controllers await it without blocking Main. */
internal object NativePlayerReleaseQueue {
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "iptv-native-release").apply { isDaemon = true }
    }
    @Volatile var pending: Deferred<Unit>? = null
        private set

    @Synchronized fun enqueue(shutdown: () -> Unit) {
        val completion = CompletableDeferred<Unit>()
        pending = completion
        executor.execute {
            val started = SystemClock.elapsedRealtime()
            try {
                shutdown()
            } finally {
                Log.i("PlayerReleasePerf", "nativeMs=${SystemClock.elapsedRealtime() - started}")
                completion.complete(Unit)
            }
        }
    }
}
