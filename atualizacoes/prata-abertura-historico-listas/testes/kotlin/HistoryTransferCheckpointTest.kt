package br.com.flaviotorres.iptvpremium.data.history

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import br.com.flaviotorres.iptvpremium.data.cache.*
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedMovieEntity
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class HistoryTransferCheckpointTest {
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 private fun resetInstance() {SharedLibraryStore::class.java.getDeclaredField("instance").apply{isAccessible=true}.set(null,null)}
 @Before fun clean() {
  listOf("iptvpremium_shared_library_v1","iptvpremium_favorites_v1","iptvpremium_watch_history_v1").forEach {context.getSharedPreferences(it,0).edit().clear().commit()}
  resetInstance()
 }
 private suspend fun fixture(body:suspend(PlaylistCacheDatabase,SharedLibraryStore,WatchHistoryStore)->Unit) {
  val db=Room.inMemoryDatabaseBuilder(context,PlaylistCacheDatabase::class.java).allowMainThreadQueries().build()
  val manager=PlaylistCacheGenerationManager(db.playlistCacheDao())
  try {
   for(list in listOf("A","B","C","D")) {
    val generation=manager.beginImport(list)
    db.playlistCacheDao().insertMovies((1..2).map {CachedMovieEntity(list,generation,"$list$it","Movie $it",null,null,null,"https://example.invalid/$list/$it",2024,7.0,1000)})
    manager.markImportReady(list,generation);manager.activateReadyGenerationOnStartup(list)
   }
   body(db,SharedLibraryStore.get(context),WatchHistoryStore(context))
  }finally{db.close()}
 }
 private fun entry(list:String,id:String="${list}1",position:Long=150_000,stamp:Long=1000)=WatchHistoryEntry(list,PlaybackContentType.MOVIE,id,title="Movie ${id.last()}",positionMs=position,durationMs=1_000_000,updatedAtMs=stamp)
 private suspend fun prepare(db:PlaylistCacheDatabase,store:SharedLibraryStore,history:WatchHistoryStore,target:String,origin:String) {
  for(kind in listOf("movies","series"))store.reconcile(db.playlistCacheDao(),target,history.allLocal().filter{it.playlistId in setOf(target,origin)},kind=kind,origin=origin)
 }
 private suspend fun transfer(db:PlaylistCacheDatabase,store:SharedLibraryStore,history:WatchHistoryStore,target:String,origin:String) {
  prepare(db,store,history,target,origin);store.checkpointCompletedHistory(target,history)
 }
 @Test fun completedCBecomesTheOriginForBIncludingNewerProgressAndFavoriteRemoval()=runBlocking {
  fixture {db,store,history->
   store.seedHistorySource("A");store.setFavorite(db.playlistCacheDao(),"A","movies","A1",true)
   history.checkpointPlaylist("A",listOf(entry("A")))
   transfer(db,store,history,"C","A");assertEquals("C",store.historySource())
   store.setFavorite(db.playlistCacheDao(),"C","movies","C1",false)
   store.setFavorite(db.playlistCacheDao(),"C","movies","C2",true)
   history.checkpointPlaylist("C",listOf(entry("C",position=600_000,stamp=2000)))
   transfer(db,store,history,"B","C")
   assertEquals("B",store.historySource())
   val prefs=context.getSharedPreferences("iptvpremium_favorites_v1",0)
   assertEquals(setOf("B2"),prefs.getStringSet("movies_B",emptySet()))
   assertEquals(600_000,history.allLocal().first{it.playlistId=="B" && it.contentId=="B1"}.positionMs)
  }
 }
 @Test fun rapidCThenDSkipsIncompleteCAndDoesNotPublishItsCheckpoint()=runBlocking {
  fixture {db,store,history->
   store.seedHistorySource("A");store.setFavorite(db.playlistCacheDao(),"A","movies","A1",true)
   history.checkpointPlaylist("A",listOf(entry("A")))
   val session=HistorySyncSession();val c=session.request("C",origin=store.historySource());assertTrue(session.start(c.id))
   prepare(db,store,history,"C",c.origin!!)
   store.setFavorite(db.playlistCacheDao(),"C","movies","C2",true)
   val d=session.request("D",origin=store.historySource())
   assertEquals("A",d.origin)
   assertFalse(session.complete(c.id){store.checkpointCompletedHistory("C",history)})
   assertEquals("A",store.historySource());assertTrue(session.start(d.id))
   prepare(db,store,history,"D",d.origin!!)
   assertTrue(session.complete(d.id){store.checkpointCompletedHistory("D",history)})
   assertEquals(setOf("D1"),context.getSharedPreferences("iptvpremium_favorites_v1",0).getStringSet("movies_D",emptySet()))
   assertEquals("D",store.historySource())
  }
 }
 @Test fun unrelatedListsAreNotScannedAsOriginsAndAreNotDeleted()=runBlocking {
  fixture {db,store,history->
   store.setFavorite(db.playlistCacheDao(),"A","movies","A1",true)
   store.setFavorite(db.playlistCacheDao(),"B","movies","B2",true)
   history.checkpointPlaylist("B",listOf(entry("B","B2")))
   transfer(db,store,history,"C","A")
   assertEquals(setOf("C1"),context.getSharedPreferences("iptvpremium_favorites_v1",0).getStringSet("movies_C",emptySet()))
   assertTrue(history.allLocal().any{it.playlistId=="B"})
   assertFalse(history.allLocal().any{it.playlistId=="C" && it.contentId=="C2"})
  }
 }
 @Test fun completedSourceAndMappedHistorySurviveProcessRecreation()=runBlocking {
  fixture {db,store,history->
   store.seedHistorySource("A");store.setFavorite(db.playlistCacheDao(),"A","movies","A1",true)
   history.checkpointPlaylist("A",listOf(entry("A")))
   transfer(db,store,history,"C","A");resetInstance()
   val reopened=SharedLibraryStore.get(context);assertEquals("C",reopened.historySource())
   transfer(db,reopened,WatchHistoryStore(context),"B",reopened.historySource()!!)
   assertEquals(150_000,history.allLocal().first{it.playlistId=="B"}.positionMs)
  }
 }
 @Test fun failedOrReleasedRunsCannotAdvanceTheLastCompletedSource() {
  val s=HistorySyncSession();val c=s.request("C",origin="A");assertTrue(s.start(c.id));assertTrue(s.fail(c.id))
  var saved=false;assertFalse(s.complete(c.id){saved=true});assertFalse(saved)
  val d=s.request("D",origin="A");assertEquals("A",d.origin)
 }
 @Test fun missingEpisodeLookupKeepsAAsOriginUntilTheTargetEpisodeIsReady()=runBlocking {
  fixture {db,store,history->
   val dao=db.playlistCacheDao()
   for(list in listOf("A","C")) {
    val generation=dao.getActiveGeneration(list)!!.generationId
    dao.insertSeries(listOf(br.com.flaviotorres.iptvpremium.data.cache.entity.CachedSeriesEntity(
      playlistId=list,generationId=generation,seriesId="${list}s",name="Shared series",categoryId=null,
      posterUrl=null,backdropUrl=null,year=2024,rating=null,plot=null,titleLogoUrl=null,
      imdbId=null,tmdbId=null,ratingSource=null,durationSeconds=null,genres=null,trailerUrl=null)))
   }
   store.seedHistorySource("A")
   store.setFavorite(dao,"A","series","As",true)
   history.checkpointPlaylist("A",listOf(WatchHistoryEntry("A",PlaybackContentType.EPISODE,"Ae",seriesId="As",
      title="Episode",seriesTitle="Shared series",seasonNumber=1,episodeNumber=2,positionMs=150_000,durationMs=1_000_000,updatedAtMs=2000)))
   prepare(db,store,history,"C","A")
   try {store.checkpointCompletedHistory("C",history);fail("Incomplete episode lookup must not complete")}
   catch(expected:IllegalStateException) { }
   assertEquals("A",store.historySource())
   store.registerEpisodes("C","Cs",listOf(br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeInfo("Ce",1,2,"Episode",null,null,1000,null)))
   store.checkpointCompletedHistory("C",history)
   assertEquals("C",store.historySource())
   assertEquals("Ce",history.allLocal().first{it.playlistId=="C"}.contentId)
  }
 }

 @Test fun creatingTheStoreDoesNotDecodeHistoryBeforeTheCatalogueLoads() {
  val store=SharedLibraryStore.get(context)
  store.changes.value
  val field=SharedLibraryStore::class.java.getDeclaredField("loadedRecords").apply{isAccessible=true}
  assertNull(field.get(store))
  store.seedHistorySource("A")
  assertNull(field.get(store))
 }

 @Test fun inheritedHiddenProgressDoesNotReappearOnTheNextList()=runBlocking {
  fixture {db,store,history->
   val dao=db.playlistCacheDao()
   store.setFavorite(dao,"A","movies","A1",true)
   store.setFavorite(dao,"C","movies","C1",true)
   history.checkpointPlaylist("A",listOf(entry("A")))
   history.checkpointPlaylist("C",listOf(entry("C")))
   prepare(db,store,history,"A","A")
   store.hide("A",PlaybackContentType.MOVIE,"A1",history.allLocal())
   transfer(db,store,history,"C","A")
   transfer(db,store,history,"B","C")
   assertFalse(history.all("B").any{it.contentId=="B1"})
  }
 }

}
