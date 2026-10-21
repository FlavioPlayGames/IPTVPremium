package br.com.flaviotorres.iptvpremium.core.storage

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import br.com.flaviotorres.iptvpremium.MainActivity
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Runs in :cache_restart, so no old downloader can recreate files during cleanup. */
class CacheRestartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeState.initialize(applicationContext)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.rgb(5, 8, 14))
        }
        layout.addView(ProgressBar(this).apply {
            indeterminateTintList = ColorStateList.valueOf(ThemeColors.color(0xFFFFD54A).let {
                android.graphics.Color.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt())
            })
        })
        layout.addView(TextView(this).apply {
            text = "Atualizando sua experiência…"
            textSize = 22f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(24, 24, 24, 24)
        })
        setContentView(layout)
        val previousPid = intent.getIntExtra("previous_pid", -1)
        if (previousPid > 0 && previousPid != Process.myPid()) Process.killProcess(previousPid)
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    // Clear disposable files only, like Android's Clear cache action.
                    // Keep the Room catalogue: rebuilding it would require a full import.
                    // Preferences, registry, access, favorites and history stay untouched.
                    clearExperienceCache(applicationContext.cacheDir, applicationContext.externalCacheDir)
                }.isSuccess
            }
            if (!success) Toast.makeText(this@CacheRestartActivity,
                "Parte do cache não pôde ser limpa. O aplicativo será reaberto.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this@CacheRestartActivity, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finishAndRemoveTask()
            Process.killProcess(Process.myPid())
        }
    }

    companion object {
        fun restart(activity: Activity) {
            activity.startActivity(Intent(activity, CacheRestartActivity::class.java)
                .putExtra("previous_pid", Process.myPid()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
