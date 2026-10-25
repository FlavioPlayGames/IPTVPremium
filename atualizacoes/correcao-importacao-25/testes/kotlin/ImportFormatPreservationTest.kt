package br.com.flaviotorres.iptvpremium.data.sync

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import br.com.flaviotorres.iptvpremium.data.cache.importer.M3uCacheImporter
import br.com.flaviotorres.iptvpremium.data.parser.DefaultM3uParser
import br.com.flaviotorres.iptvpremium.data.parser.DefaultXtreamParser
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.repository.DefaultPlaylistRepository
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import java.io.StringReader
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class ImportFormatPreservationTest {
    private val source = PlaylistSource.XtreamCodes("test", "Test", true, "https://example.invalid", "demo", "demo", fallbackM3uUrl = "https://example.invalid/get.php?username=demo&password=demo")
    private class Client(val brokenFull: Boolean = false, val gateCategories: Boolean = false,
                         val fail: Boolean = false, val cancel: Boolean = false, val block: Boolean = false, val latencyMs: Long = 20, val records: Int = 1) : PlaylistHttpClient {
        val m3uReads = AtomicInteger()
        val xtreamReads = AtomicInteger()
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val entered = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        override suspend fun readLines(url: String, onLine: suspend (String) -> Unit) {
            m3uReads.incrementAndGet()
            listOf("#EXTM3U", "#EXTINF:-1 group-title=\"Live\",Channel", "https://example.invalid/live/1.ts").forEach { onLine(it) }
        }
        override suspend fun get(url: String): String = """{"user_info":{"auth":1,"status":"Active"}}"""
        override suspend fun <T> withReader(url: String, consume: suspend (java.io.Reader) -> T): T {
            xtreamReads.incrementAndGet()
            val n = active.incrementAndGet(); peak.updateAndGet { maxOf(it, n) }
            try {
                val uri = Uri.parse(url)
                val action = uri.getQueryParameter("action").orEmpty()
                if (action.endsWith("categories") && gateCategories) {
                    if (entered.incrementAndGet() == 3) gate.complete(Unit)
                    withTimeout(3_000) { gate.await() }
                }
                started.complete(Unit)
                if (cancel) throw CancellationException("Cancelled import")
                if (block) awaitCancellation()
                if (fail && action == "get_series") throw java.io.IOException("synthetic failure")
                delay(latencyMs)
                val json = when {
                    action.endsWith("categories") -> """[{"category_id":"1","category_name":"One"},{"category_id":"2","category_name":"Two"}]"""
                    brokenFull && uri.getQueryParameter("category_id") == null && action in listOf("get_vod_streams", "get_series") -> "invalid-json"
                    records > 1 -> (1..records).joinToString(",", "[", "]") { index ->
                        val id = if (action == "get_series") "series_id" else "stream_id"
                        """{"$id":$index,"name":"Item $index","category_id":"1"}"""
                    }
                    action == "get_live_streams" -> """[{"stream_id":7,"name":"Channel","category_id":"1"}]"""
                    action == "get_vod_streams" -> """[{"stream_id":7,"name":"Movie","category_id":"1","container_extension":"mp4"}]"""
                    else -> """[{"series_id":7,"name":"Series","category_id":"1"}]"""
                }
                return StringReader(json).use { consume(it) }
            } finally { active.decrementAndGet() }
        }
    }
    private suspend fun fixture(action: suspend (PlaylistCacheDatabase, PlaylistCacheGenerationManager) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PlaylistCacheDatabase::class.java)
            .allowMainThreadQueries().build()
        try { action(db, PlaylistCacheGenerationManager(db.playlistCacheDao())) } finally { db.close() }
    }
    private fun sync(client: Client, db: PlaylistCacheDatabase, manager: PlaylistCacheGenerationManager): M3uSourceSynchronizer {
        val dao = db.playlistCacheDao()
        return M3uSourceSynchronizer(DefaultPlaylistRepository(client, DefaultM3uParser(), DefaultXtreamParser()),
            M3uCacheImporter(client, dao, manager), dao, manager)
    }

    @Test fun convertedSourceRefreshUsesOnlyXtreamEvenWithAnOriginalM3uUrl()=runBlocking {
        fixture { db,manager ->
            val client=Client()
            val generation=sync(client,db,manager).sync(source)
            assertTrue(client.xtreamReads.get()>0)
            assertEquals(0,client.m3uReads.get())
            assertEquals(1,db.playlistCacheDao().getMovieCount(source.id,generation))
            assertEquals(1,db.playlistCacheDao().getSeriesCount(source.id,generation))
        }
    }
    @Test fun failedRefreshDoesNotDowngradeOrReplacePreviousActiveCatalog()=runBlocking {
        fixture { db,manager ->
            val original=sync(Client(),db,manager).sync(source)
            manager.activateReadyGenerationOnStartup(source.id)
            val client=Client(fail=true)
            try { sync(client,db,manager).sync(source); fail("Expected Xtream failure") }
            catch(expected:java.io.IOException) { }
            assertEquals(0,client.m3uReads.get())
            assertEquals(original,db.playlistCacheDao().getActiveGeneration(source.id)?.generationId)
            assertEquals(1,db.playlistCacheDao().getMovieCount(source.id,original))
            assertNull(db.playlistCacheDao().getReadyGeneration(source.id))
        }
    }
    @Test fun cancellationDoesNotDownloadM3uOrPublishAPartialCatalog()=runBlocking {
        fixture { db,manager ->
            val client=Client(cancel=true)
            try { sync(client,db,manager).sync(source); fail("Expected cancellation") }
            catch(expected:CancellationException) { }
            assertEquals(0,client.m3uReads.get())
            assertEquals(0,client.active.get())
            assertNull(db.playlistCacheDao().getReadyGeneration(source.id))
        }
    }
    @Test fun nativeM3uStillImportsAsM3u()=runBlocking {
        fixture { db,manager ->
            val client=Client()
            val native=PlaylistSource.M3u("native", "Native",true,source.fallbackM3uUrl!!)
            val generation=sync(client,db,manager).sync(native)
            assertEquals(1,client.m3uReads.get())
            assertEquals(0,client.xtreamReads.get())
            assertEquals(1,db.playlistCacheDao().getChannelCount(native.id,generation))
        }
    }
    @Test fun portalRoundTripRetainsXtreamChoiceAndOriginalUrl() {
        val encoded=br.com.flaviotorres.iptvpremium.core.device.encodePortalLists(listOf(source))
        val decoded=br.com.flaviotorres.iptvpremium.core.device.decodePortalLists(encoded)
        assertEquals(listOf(source),decoded)
        assertTrue(decoded.single() is PlaylistSource.XtreamCodes)
    }
    @Test fun fourthXtreamAttemptCanRecoverWithoutDownloadingM3u()=runBlocking {
        fixture { db,manager ->
            val client=Client();val calls=AtomicInteger()
            val repository=object:br.com.flaviotorres.iptvpremium.domain.repository.PlaylistRepository {
                override suspend fun loadContent(source:PlaylistSource):br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent {
                    assertTrue(source is PlaylistSource.XtreamCodes)
                    if(calls.incrementAndGet()<4)throw java.io.IOException("Temporary provider failure")
                    return br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent(categories=listOf(
                        br.com.flaviotorres.iptvpremium.domain.model.Category("one","One",
                            br.com.flaviotorres.iptvpremium.domain.model.CategoryType.LIVE_TV,
                            br.com.flaviotorres.iptvpremium.domain.model.CategorySource.PROVIDER)))
                }
            }
            val dao=db.playlistCacheDao()
            val generation=M3uSourceSynchronizer(repository,M3uCacheImporter(client,dao,manager),dao,manager).sync(source)
            assertEquals(4,calls.get());assertEquals(0,client.m3uReads.get())
            assertEquals(generation,dao.getReadyGeneration(source.id)?.generationId)
        }
    }
    @Test fun retryStopsAfterFourXtreamFailuresAndNeverSwitchesFormat()=runBlocking {
        fixture { db,manager ->
            val client=Client();val calls=AtomicInteger()
            val repository=object:br.com.flaviotorres.iptvpremium.domain.repository.PlaylistRepository {
                override suspend fun loadContent(source:PlaylistSource):br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent {
                    calls.incrementAndGet();throw java.io.IOException("Unavailable")
                }
            }
            val dao=db.playlistCacheDao()
            try { M3uSourceSynchronizer(repository,M3uCacheImporter(client,dao,manager),dao,manager).sync(source);fail("Expected failure") }
            catch(expected:java.io.IOException) { }
            assertEquals(4,calls.get());assertEquals(0,client.m3uReads.get());assertNull(dao.getReadyGeneration(source.id))
        }
    }
    @Test fun localStorageFailureIsNotMistakenForARetryableXtreamFailure()=runBlocking {
        fixture { db,manager ->
            val client=Client();val calls=AtomicInteger()
            val repository=object:br.com.flaviotorres.iptvpremium.domain.repository.PlaylistRepository {
                override suspend fun loadContent(source:PlaylistSource):br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent {
                    calls.incrementAndGet();throw android.database.sqlite.SQLiteException("Storage unavailable")
                }
            }
            val dao=db.playlistCacheDao()
            try { M3uSourceSynchronizer(repository,M3uCacheImporter(client,dao,manager),dao,manager).sync(source);fail("Expected storage failure") }
            catch(expected:android.database.sqlite.SQLiteException) { }
            assertEquals(1,calls.get());assertEquals(0,client.m3uReads.get())
        }
    }

}
