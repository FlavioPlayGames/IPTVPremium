package br.com.flaviotorres.iptvpremium.core.storage

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35])
class ActivePlaylistRefreshTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun clearRequest(){context.getSharedPreferences("active_playlist_refresh",Context.MODE_PRIVATE).edit().clear().commit()}
    @Test fun refreshTargetsOnlyTheActivePlaylistAndSurvivesRestart() {
        assertTrue(ActivePlaylistRefresh(context).request("active"))
        val afterRestart=ActivePlaylistRefresh(context)
        assertEquals("active",afterRestart.pendingPlaylistId())
        assertTrue(afterRestart.needsSync("active","old-generation"))
        assertFalse(afterRestart.needsSync("other","other-generation"))
        assertFalse(afterRestart.complete("other"))
        assertEquals("active",afterRestart.pendingPlaylistId())
    }
    @Test fun failedImportRemainsPendingButCompletedImportDoesNotDownloadTwice() {
        val request=ActivePlaylistRefresh(context);request.request("active")
        // Failure does not acknowledge the request or replace the previous active generation.
        assertTrue(ActivePlaylistRefresh(context).needsSync("active","old"))
        assertTrue(request.markImported("active","new"))
        assertFalse(ActivePlaylistRefresh(context).needsSync("active","new"))
        assertTrue(request.needsSync("active","different"))
        assertTrue(request.complete("active"));assertNull(request.pendingPlaylistId())
        assertFalse(request.needsSync("active","new"))
    }
    @Test fun cleanupKeepsCataloguePreferencesHistoryAndTheRefreshRequest() {
        val request=ActivePlaylistRefresh(context);request.request("active")
        val catalogue=context.getDatabasePath("preserved-catalogue.db").apply{parentFile!!.mkdirs();writeText("catalogue")}
        val history=java.io.File(context.filesDir,"preserved-history").apply{writeText("history")}
        val image=java.io.File(context.cacheDir,"disposable-preview").apply{writeText("cache")}
        clearExperienceCache(context.cacheDir,null)
        assertFalse(image.exists());assertEquals("catalogue",catalogue.readText());assertEquals("history",history.readText())
        assertEquals("active",ActivePlaylistRefresh(context).pendingPlaylistId())
    }
    @Test fun buttonPassesPlaylistThroughTheRestartIntent() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity=controller.get()
            CacheRestartActivity.restart(activity,"active")
            val intent=shadowOf(activity).nextStartedActivity
            assertEquals("active",intent.getStringExtra("refresh_playlist"))
            assertEquals(CacheRestartActivity::class.java.name,intent.component!!.className)
        } finally {controller.pause().stop().destroy()}
    }
}
