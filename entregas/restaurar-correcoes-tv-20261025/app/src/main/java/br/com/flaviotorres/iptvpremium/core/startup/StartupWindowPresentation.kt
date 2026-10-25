package br.com.flaviotorres.iptvpremium.core.startup

import android.os.SystemClock
import android.util.Log
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/** Keep the launch background until the first real window layout has settled. No timers or sleeps. */
internal object StartupWindowPresentation {
    fun install(activity: ComponentActivity) {
        val decor = activity.window.decorView
        val observer = decor.viewTreeObserver
        val gate = StartupWindowLayoutGate()
        val started = SystemClock.elapsedRealtime()
        var passes = 0
        var closed = false
        val listener = object : ViewTreeObserver.OnPreDrawListener, DefaultLifecycleObserver {
            fun close() {
                if (closed) return
                closed = true
                if (observer.isAlive) observer.removeOnPreDrawListener(this)
                activity.lifecycle.removeObserver(this)
            }

            override fun onPreDraw(): Boolean {
                if (closed) return true
                passes++
                val rootInsets = ViewCompat.getRootWindowInsets(decor)
                val bars = rootInsets?.getInsets(WindowInsetsCompat.Type.systemBars())
                val layout = StartupWindowLayout(decor.width, decor.height, rootInsets != null,
                    bars?.left ?: 0, bars?.top ?: 0, bars?.right ?: 0, bars?.bottom ?: 0)
                if (gate.ready(layout)) {
                    Log.i("StartupLayoutPerf", "phase=ready passes=$passes width=${layout.width} height=${layout.height} elapsedMs=${SystemClock.elapsedRealtime() - started}")
                    close()
                    return true
                }
                if (passes == 1) Log.d("StartupLayoutPerf", "phase=waiting width=${layout.width} height=${layout.height}")
                // Cancel this provisional draw and ask for another traversal on the next frame.
                ViewCompat.postInvalidateOnAnimation(decor)
                return false
            }

            override fun onDestroy(owner: LifecycleOwner) { close() }
        }
        observer.addOnPreDrawListener(listener)
        activity.lifecycle.addObserver(listener)
    }
}
