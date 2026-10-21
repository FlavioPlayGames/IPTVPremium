package br.com.flaviotorres.iptvpremium.data.storage

import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.core.storage.clearExperienceCache
import java.nio.file.Files
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class CatalogReusePolicyDeviceTest {
    private val xtream=PlaylistSource.XtreamCodes("same","Fixture",true,"https://example.invalid","fixture","fixture","https://example.invalid/get.php")
    @Test fun repeatedXtreamAndRenameReuseCatalog() {
        assertTrue(sameCatalogSource(xtream,xtream.copy()))
        assertTrue(sameCatalogSource(xtream,xtream.copy(name="Renamed",isEnabled=false)))
    }
    @Test fun credentialChangesRequireNewImport() {
        assertFalse(sameCatalogSource(xtream,xtream.copy(username="changed")))
        assertFalse(sameCatalogSource(xtream,xtream.copy(password="changed")))
    }
    @Test fun serverAndFallbackChangesRequireNewImport() {
        assertFalse(sameCatalogSource(xtream,xtream.copy(serverUrl="https://other.invalid")))
        assertFalse(sameCatalogSource(xtream,xtream.copy(fallbackM3uUrl=null)))
    }
    @Test fun m3uRenameAndEnableDoNotChangeConnection() {
        val m3u=PlaylistSource.M3u("same","Fixture",true,"https://example.invalid/list.m3u")
        assertTrue(sameCatalogSource(m3u,m3u.copy(name="Changed",isEnabled=false)))
        assertFalse(sameCatalogSource(m3u,m3u.copy(playlistUrl="https://other.invalid/list.m3u")))
    }
    @Test fun missingSourceNewIdAndDifferentFormatRequireImport() {
        assertFalse(sameCatalogSource(null,xtream))
        assertFalse(sameCatalogSource(xtream,xtream.copy(id="new")))
        assertFalse(sameCatalogSource(xtream,PlaylistSource.M3u("same","Fixture",true,"https://example.invalid/list.m3u")))
    }
    @Test fun automaticCatalogWorkWaitsUntilHomeIsReady() {
        assertTrue(CatalogRefreshPolicy.defer(100000,null))
    }
    @Test fun automaticCatalogWorkWaitsFullGracePeriod() {
        assertTrue(CatalogRefreshPolicy.defer(60999,1000))
        assertFalse(CatalogRefreshPolicy.defer(61000,1000))
        assertFalse(CatalogRefreshPolicy.defer(100000,1000))
    }
    @Test fun cacheCleanupKeepsDatabasePreferencesAndHistory() {
        val root=Files.createTempDirectory("home-cache-").toFile()
        try {
            val cache=File(root,"cache").apply { mkdirs() };val external=File(root,"external-cache").apply { mkdirs() }
            File(cache,"covers/nested").mkdirs();File(cache,"covers/nested/poster.img").writeText("poster")
            File(external,"temporary").writeText("temporary")
            val kept=listOf("databases/playlist_cache.db","databases/playlist_cache.db-wal","files/history.json","shared_prefs/settings.xml").map { File(root,it).apply {parentFile!!.mkdirs();writeText("preserve:$it")} }
            val bytes=kept.map { it.readBytes() }
            clearExperienceCache(cache,external)
            assertTrue(cache.listFiles()!!.isEmpty());assertTrue(external.listFiles()!!.isEmpty())
            kept.zip(bytes).forEach { (file,before)->assertArrayEquals(before,file.readBytes()) }
        } finally { root.deleteRecursively() }
    }
    @Test fun cleanupHandlesAbsentExternalCache() {
        val root=Files.createTempDirectory("home-cache-null-").toFile()
        try { File(root,"temporary").writeText("temporary");clearExperienceCache(root,null);assertTrue(root.listFiles()!!.isEmpty()) }
        finally {root.deleteRecursively()}
    }
}
