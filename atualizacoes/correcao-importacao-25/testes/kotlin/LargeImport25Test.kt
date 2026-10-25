package br.com.flaviotorres.iptvpremium.data.sync

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.data.cache.*
import br.com.flaviotorres.iptvpremium.data.cache.importer.M3uCacheImporter
import br.com.flaviotorres.iptvpremium.data.parser.*
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.repository.DefaultPlaylistRepository
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class LargeImport25Test {
 @Test fun sameVolumeAsTvKeepsEveryRecordAndPublishesOnlyCompletedGeneration()=runBlocking {
  val source=PlaylistSource.XtreamCodes("large", "Large",true,"https://example.invalid","demo","demo")
  val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PlaylistCacheDatabase::class.java).build()
  try {
   val dao=db.playlistCacheDao();val manager=PlaylistCacheGenerationManager(dao)
   val client=object:PlaylistHttpClient {
    override suspend fun get(url:String)="""{"user_info":{"auth":1,"status":"Active"}}"""
    override suspend fun readLines(url:String,onLine:suspend(String)->Unit){error("No format fallback")}
    override suspend fun<T> withReader(url:String,consume:suspend(java.io.Reader)->T):T {
     val action=Uri.parse(url).getQueryParameter("action")
     val data=if(action?.endsWith("categories")==true)"""[{"category_id":"1","category_name":"One"}]""" else {
      val count=if(action=="get_vod_streams")44724 else 20170
      val key=if(action=="get_vod_streams")"stream_id"else"series_id"
      (1..count).joinToString(",","[","]"){"""{"$key":$it,"name":"Item $it","category_id":"1","year":2025,"rating":"7.1"}"""}
     }
     return StringReader(data).use{consume(it)}
    }
   }
   val synchronizer=M3uSourceSynchronizer(DefaultPlaylistRepository(client,DefaultM3uParser(),DefaultXtreamParser()),M3uCacheImporter(client,dao,manager),dao,manager)
   val generation=withTimeout(120_000){synchronizer.sync(source)}
   assertEquals(44724,dao.getMovieCount(source.id,generation));assertEquals(20170,dao.getSeriesCount(source.id,generation))
   assertNull(dao.observeActiveGenerationId(source.id).first())
   manager.activateReadyGenerationOnStartup(source.id)
   assertEquals(generation,dao.observeActiveGenerationId(source.id).first())
  }finally{db.close()}
 }
}
