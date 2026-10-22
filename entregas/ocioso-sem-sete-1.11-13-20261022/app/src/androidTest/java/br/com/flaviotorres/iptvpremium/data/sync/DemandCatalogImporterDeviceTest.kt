package br.com.flaviotorres.iptvpremium.data.sync

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.data.cache.*
import br.com.flaviotorres.iptvpremium.data.storage.PlaylistStorage
import br.com.flaviotorres.iptvpremium.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.Collections

/** Real Room + DataStore + atomic checkpoints; fake transport makes priority/cancellation deterministic. */
class DemandCatalogImporterDeviceTest {
    private class Remote : CategoryXtreamRepository {
        val calls = Collections.synchronizedList(mutableListOf<CategoryImportTask>())
        var blocked: CategoryType? = null
        var fail = false
        var empty = false
        val hold = CompletableDeferred<Unit>()
        override suspend fun importCategoryIndex(source: PlaylistSource.XtreamCodes, sink: XtreamCatalogSink): List<Category> {
            val rows = CategoryType.entries.flatMap { kind -> (1..2).map { Category("${kind.name}-$it", "Categoria $it", kind, CategorySource.PROVIDER) } }
            sink.append("index", PlaylistContent(categories = rows))
            return rows
        }
        override suspend fun importCategory(source: PlaylistSource.XtreamCodes, task: CategoryImportTask, sink: XtreamCatalogSink) {
            calls.add(task)
            val id = task.categoryId ?: "uncategorized"
            if (empty) return
            sink.append(task.endpoint, when(task.type) {
                CategoryType.MOVIES -> PlaylistContent(movies=listOf(Movie(id,"Movie",id,null,null,"https://example.invalid/movie",2026,8.0,null)))
                CategoryType.SERIES -> PlaylistContent(series=listOf(Series(id,"Series",id,null,null,2026,8.0,null)))
                CategoryType.LIVE_TV -> PlaylistContent(channels=listOf(Channel(id,"Channel",id,null,"https://example.invalid/live",null)))
            })
            if (task.type == blocked) hold.await()
            if (fail) error("provider unavailable")
        }
    }
    private class Fixture(val context: Context, val id: String, val storage: PlaylistStorage,
        val database: PlaylistCacheDatabase, val remote: Remote) {
        val source = PlaylistSource.XtreamCodes(id,"Fixture",true,"https://example.invalid","fixture","fixture")
        val manager = PlaylistCacheGenerationManager(database.playlistCacheDao())
        val importers = mutableListOf<DemandCatalogImporter>()
        fun importer() = DemandCatalogImporter(context,remote,database,manager,storage).also { importers.add(it) }
    }
    private fun fixture(test: suspend Fixture.()->Unit) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = "demand-"+UUID.randomUUID()
        val file = File(context.cacheDir,"$id.preferences_pb")
        val dataScope = CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val storage = PlaylistStorage(PreferenceDataStoreFactory.create(scope=dataScope,produceFile={file}))
        val db = Room.inMemoryDatabaseBuilder(context,PlaylistCacheDatabase::class.java).build()
        val f = Fixture(context,id,storage,db,Remote())
        CatalogImportAccess.environment.value=false; CatalogImportAccess.leave(); MediaOpeningPriority.setBusy(false)
        try {
            storage.applyPortalOperation(id,f.source)
            f.test()
        } finally {
            CatalogImportAccess.environment.value=false; CatalogImportAccess.leave(); MediaOpeningPriority.setBusy(false)
            f.importers.forEach { it.closeAndJoin() }
            db.close(); dataScope.coroutineContext[Job]!!.cancelAndJoin(); file.delete()
        }
    }
    private suspend fun await(condition: suspend ()->Boolean) = withTimeout(12_000) { while(!condition()) delay(20) }
    @Test fun indexReleasesHomeWithoutDownloadingContent() = fixture {
        val generation=importer().prepare(source)
        assertEquals(generation,database.playlistCacheDao().getActiveGeneration(id)!!.generationId)
        assertEquals(0,database.playlistCacheDao().getMovieCount(id,generation))
        assertTrue(remote.calls.isEmpty())
        assertEquals(2,database.playlistCacheDao().getCategoriesByType(id,generation,"MOVIES").size)
    }
    @Test fun accessingChannelsPrioritizesChannelsBeforeMovies() = fixture {
        remote.blocked=CategoryType.LIVE_TV
        importer().prepare(source)
        CatalogImportAccess.request(id,CategoryType.LIVE_TV,"LIVE_TV-2")
        CatalogImportAccess.interaction(); CatalogImportAccess.environment.value=true
        await { remote.calls.isNotEmpty() }
        assertEquals(CategoryImportTask(CategoryType.LIVE_TV,null),remote.calls.first())
    }
    @Test fun selectedMediaPausesImportAndPreventsPartialCategory() = fixture {
        remote.blocked=CategoryType.MOVIES
        val generation=importer().prepare(source)
        CatalogImportAccess.request(id,CategoryType.MOVIES,"MOVIES-1")
        CatalogImportAccess.environment.value=true
        await { remote.calls.isNotEmpty() }
        MediaOpeningPriority.setBusy(true)
        delay(400)
        assertEquals(0,database.playlistCacheDao().getMovieCount(id,generation))
        assertEquals(1,remote.calls.size)
    }
    @Test fun failurePreservesTheActiveCatalogAndSourceType() = fixture {
        val initial=importer().prepare(source)
        val replacement=source.copy(name="Updated")
        remote.fail=true
        importer().prepare(replacement)
        CatalogImportAccess.request(id,CategoryType.MOVIES)
        CatalogImportAccess.environment.value=true
        await { remote.calls.isNotEmpty() }; delay(300)
        assertEquals(initial,database.playlistCacheDao().getActiveGeneration(id)!!.generationId)
        assertTrue(storage.selection.first().playlists.single() is PlaylistSource.XtreamCodes)
    }
    @Test fun restartReusesCompletedCategories() = fixture {
        val one=importer();val generation=one.prepare(source)
        CatalogImportAccess.request(id,CategoryType.MOVIES,"MOVIES-1")
        CatalogImportAccess.environment.value=true
        await { database.playlistCacheDao().getMovieCount(id,generation)>=1 }
        CatalogImportAccess.environment.value=false
        one.closeAndJoin()
        val done=remote.calls.toList().filter { it.type==CategoryType.MOVIES }.toSet()
        remote.calls.clear()
        val two=importer();two.resume(source)
        CatalogImportAccess.request(id,CategoryType.LIVE_TV)
        CatalogImportAccess.environment.value=true
        await { remote.calls.isNotEmpty() }
        assertEquals(generation,database.playlistCacheDao().getActiveGeneration(id)!!.generationId)
        assertFalse(remote.calls.any { it in done })
    }
    @Test fun sourceEditDuringDownloadCannotPublishOldCategory() = fixture {
        remote.blocked=CategoryType.MOVIES
        val generation=importer().prepare(source)
        CatalogImportAccess.request(id,CategoryType.MOVIES)
        CatalogImportAccess.environment.value=true
        await { remote.calls.isNotEmpty() }
        storage.applyPortalOperation(id,source.copy(password="changed"))
        remote.hold.complete(Unit); delay(500)
        assertEquals(0,database.playlistCacheDao().getMovieCount(id,generation))
    }
    private suspend fun Fixture.existingCatalog(): String {
        val generation=manager.beginImport(id)
        database.playlistCacheDao().insertMovies(listOf(br.com.flaviotorres.iptvpremium.data.cache.entity.CachedMovieEntity(
            id,generation,"old","Existing movie","MOVIES-1",null,null,"https://example.invalid/movie",2026,8.0,null)))
        manager.markImportReady(id,generation);manager.activateReadyGenerationOnStartup(id)
        return generation
    }
    @Test fun refreshSwapsOnlyAfterAllSectionsComplete() = fixture {
        val previous=existingCatalog()
        remote.blocked=CategoryType.SERIES
        assertEquals(previous,importer().prepare(source))
        CatalogImportAccess.lastInteraction.value=0L
        CatalogImportAccess.environment.value=true
        await { remote.calls.any { it.type==CategoryType.SERIES } }
        assertEquals(previous,database.playlistCacheDao().getActiveGeneration(id)!!.generationId)
        assertEquals(1,database.playlistCacheDao().getMovieCount(id,previous))
        remote.hold.complete(Unit)
        await { database.playlistCacheDao().getActiveGeneration(id)!!.generationId != previous }
        assertEquals("STALE",database.playlistCacheDao().generationStatus(id,previous))
    }
    @Test fun emptyRefreshKeepsExistingCatalog() = fixture {
        val previous=existingCatalog();remote.empty=true
        importer().prepare(source)
        CatalogImportAccess.lastInteraction.value=0L;CatalogImportAccess.environment.value=true
        await { remote.calls.size>=3 };delay(500)
        assertEquals(previous,database.playlistCacheDao().getActiveGeneration(id)!!.generationId)
        assertEquals(1,database.playlistCacheDao().getMovieCount(id,previous))
    }

    @Test fun navigationPausesIdleSectionWithoutPublishingPartialRows() = fixture {
        remote.blocked=CategoryType.MOVIES
        val generation=importer().prepare(source)
        CatalogImportAccess.leave();CatalogImportAccess.lastInteraction.value=0L
        CatalogImportAccess.environment.value=true
        await { remote.calls.isNotEmpty() }
        CatalogImportAccess.interaction()
        delay(500)
        assertEquals(1,remote.calls.size)
        assertEquals(0,database.playlistCacheDao().getMovieCount(id,generation))
    }

}
