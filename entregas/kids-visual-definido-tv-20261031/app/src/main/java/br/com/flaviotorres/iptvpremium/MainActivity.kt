package br.com.flaviotorres.iptvpremium

import android.os.Bundle
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.flaviotorres.iptvpremium.core.navigation.AppNavigation
import br.com.flaviotorres.iptvpremium.ui.theme.IPTVPremiumTheme

class MainActivity : ComponentActivity() {

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if(event.action == android.view.KeyEvent.ACTION_DOWN) br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.activity()
        if (br.com.flaviotorres.iptvpremium.ui.components.dispatchCatalogBackKey(event)) return true
        if (super.dispatchKeyEvent(event)) return true
        if (event.keyCode != android.view.KeyEvent.KEYCODE_ESCAPE) return false
        val escape = br.com.flaviotorres.iptvpremium.ui.components.CatalogHardwareEscape
        return when (event.action) {
            android.view.KeyEvent.ACTION_DOWN -> escape.down(event.repeatCount > 0)
            android.view.KeyEvent.ACTION_UP -> escape.up()
            else -> false
        }
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if(event.actionMasked in listOf(android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_MOVE))
            br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.activity()
        return super.dispatchTouchEvent(event)
    }
    override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.activity()
        return super.dispatchGenericMotionEvent(event)
    }
    override fun onPause() {
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.foreground(false)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.begin()
        br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.openingStarted()
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "desempenho") {
            br.com.flaviotorres.iptvpremium.core.diagnostics.UiStallMonitor.install(lifecycle)
            android.util.Log.i("BuildPerf", "variant=${BuildConfig.BUILD_TYPE} debuggable=${(applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0}")
        }
        window.decorView.setBackgroundColor(android.graphics.Color.rgb(12,14,17))
        br.com.flaviotorres.iptvpremium.ui.components.GenericPosterArtworkCache.prepare(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) { br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader.prepareFailures(applicationContext) }
        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measure("init_tema") { br.com.flaviotorres.iptvpremium.ui.theme.ThemeState.initialize(applicationContext) }
        br.com.flaviotorres.iptvpremium.core.device.PortalAccess.checking.value = true
        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measure("init_atualizador") { br.com.flaviotorres.iptvpremium.core.update.AppUpdateManager.initialize(applicationContext) }
        lifecycleScope.launch {
            try {
                br.com.flaviotorres.iptvpremium.core.device.DevicePortalSync(applicationContext).restoreAccessForOpening()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                android.util.Log.w("StartupPerf", "etapa=acesso_local indisponivel")
            }
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    br.com.flaviotorres.iptvpremium.core.device.PortalPush.observeWhileStarted(applicationContext)
                }
                var force = true
                var failures = 0
                while (true) {
                    val syncStartedAt = android.os.SystemClock.elapsedRealtime()
                    var nextCheck = 60_000L
                    try {
                        kotlinx.coroutines.withContext(Dispatchers.IO) {
                            br.com.flaviotorres.iptvpremium.core.device.DevicePortalSync(applicationContext).sync(force = force, automatic = true)
                        }
                        force = false
                        failures = 0
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failures = (failures + 1).coerceAtMost(4)
                        force = true
                        nextCheck = when (failures) { 1 -> 60_000L; 2 -> 300_000L; 3 -> 1_800_000L; else -> 3_600_000L }
                        br.com.flaviotorres.iptvpremium.core.device.PortalAccess.connectionFailed()
                    } finally {
                        br.com.flaviotorres.iptvpremium.core.device.PortalAccess.checking.value = false
                        android.util.Log.i("StartupPerf", "etapa=central_rede duracaoMs=${android.os.SystemClock.elapsedRealtime()-syncStartedAt}")
                    }
                    // This local tick checks cached expiry; HTTP is limited to startup/resume and once per 24 hours.
                    kotlinx.coroutines.delay(nextCheck)
                }
            }
        }

        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measure("init_push") { br.com.flaviotorres.iptvpremium.core.device.PortalPush.initialize(applicationContext) }

        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measure("init_categorias") { br.com.flaviotorres.iptvpremium.ui.settings.CategoryOrderSettings.initialize(applicationContext) }
        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measure("init_parental") { br.com.flaviotorres.iptvpremium.ui.settings.ParentalSession.initialize(applicationContext) }

        enableImmersiveMode()
        br.com.flaviotorres.iptvpremium.core.startup.StartupWindowPresentation.install(this)

        setContent {
            val preferences = br.com.flaviotorres.iptvpremium.ui.settings.rememberSettingsPreferences()
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, density.fontScale * preferences.textScale())
            ) {
            IPTVPremiumTheme {
                val checkingAccess = br.com.flaviotorres.iptvpremium.core.device.PortalAccess.checking.collectAsState()
                val portalAccess = br.com.flaviotorres.iptvpremium.core.device.PortalAccess.message.collectAsState()
                val appUpdate = br.com.flaviotorres.iptvpremium.core.update.AppUpdateManager.state.collectAsState()
                androidx.compose.foundation.layout.Box {
                if (checkingAccess.value) br.com.flaviotorres.iptvpremium.ui.home.AppStartupLoadingScreen(
                    state = br.com.flaviotorres.iptvpremium.ui.home.AppCatalogLoadingState())
                else {
                    if (portalAccess.value == null) {
                        br.com.flaviotorres.iptvpremium.ui.settings.ParentalAccess {
                            AppNavigation()
                        }
                        br.com.flaviotorres.iptvpremium.core.device.PortalRenewalOverlay()
                        if (!appUpdate.value.showPrompt) br.com.flaviotorres.iptvpremium.core.device.PortalTrialOverlay()
                    }
                    br.com.flaviotorres.iptvpremium.core.device.PortalAccessOverlay()
                }
                if (appUpdate.value.policy != null && appUpdate.value.showPrompt) br.com.flaviotorres.iptvpremium.core.update.AppUpdateScreen()
                }
            }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Android releases this window flag when the app leaves the foreground.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.foreground(true)
        br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.request(applicationContext)
        lifecycleScope.launch {
            br.com.flaviotorres.iptvpremium.ui.theme.refreshThemeArtworkCache(applicationContext)
        }
    }

    override fun onStop() {
        super.onStop()
        br.com.flaviotorres.iptvpremium.ui.settings.ParentalSession.lock()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            enableImmersiveMode()
        } else {
            br.com.flaviotorres.iptvpremium.ui.components.CatalogHardwareEscape.reset()
            br.com.flaviotorres.iptvpremium.ui.components.CatalogHardwareBack.reset()
        }
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {
            hide(
                WindowInsetsCompat.Type.statusBars() or
                        WindowInsetsCompat.Type.navigationBars()
            )

            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
// IPTV_DEVICE_IDENTITY_V1

// IPTV_PORTAL_PRODUCTION_V1

// IPTV_MANDATORY_UPDATE_V1

// IPTV_RENEWAL_AND_DAILY_SYNC_V1

// IPTV_FIREBASE_LIST_HINTS_V1

// IPTV_FCM_REPEATED_HINTS_V2

// IPTV_SETTINGS_APPROVED_GRID_V1
