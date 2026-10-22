package br.com.flaviotorres.iptvpremium.ui.movies

import br.com.flaviotorres.iptvpremium.core.network.MovieOpeningAttempt
import br.com.flaviotorres.iptvpremium.data.cache.CatalogDetailsWarmupCache
import br.com.flaviotorres.iptvpremium.data.remote.DefaultPlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.MovieDetails
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.net.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class RealMovieRetryTest {
 @Test fun fiveSecondBoundariesDoNotWaitForOldCleanupAndDoNotAcceptLateResults()=runTest {
  val starts=mutableListOf<Long>();val cancelled=mutableListOf<Int>();val cleaned=mutableListOf<Int>()
  val result=MovieOpeningPolicy.prepare {
   val attempt=currentCoroutineContext()[MovieOpeningAttempt]!!.number
   starts+=currentTime
   if(attempt<3) {
    try { awaitCancellation() }
    finally {
     cancelled+=attempt
     withContext(NonCancellable) { delay(20_000);cleaned+=attempt }
    }
   } else {delay(100);"third request"}
  }
  assertEquals("third request",result)
  assertEquals(listOf(0L,5_000L,10_000L),starts)
  assertEquals(listOf(1,2),cancelled)
  assertTrue(cleaned.isEmpty());assertEquals(10_100L,currentTime)
  advanceUntilIdle();assertEquals(listOf(1,2),cleaned)
 }
 @Test fun foregroundDoesNotWaitForBackgroundMutexAndManualRetryKeepsItsOwnBudget()=runBlocking {
  val id="foreground-${System.nanoTime()}";val entered=CompletableDeferred<Unit>();val hold=CompletableDeferred<Unit>()
  val warm=launch(Dispatchers.IO) {
   CatalogDetailsWarmupCache.prefetchMovie(id,"g","movie") {entered.complete(Unit);hold.await();MovieDetails("old")}
  }
  entered.await()
  try {
   val result=withTimeout(2_000) {MovieOpeningPolicy.prepare {
    CatalogDetailsWarmupCache.movieOrLoad(id,"g","movie") {MovieDetails("new")}
   }}
   assertEquals("new",result?.movieId)
   val cached=CatalogDetailsWarmupCache.movieOrLoad(id,"g","movie") {error("cache should survive")}
   assertEquals("new",cached?.movieId)
  } finally {warm.cancelAndJoin();CatalogDetailsWarmupCache.clearPlaylist(id)}
 }
 @Test fun timedOutPreparationReissuesProviderEvenIfMetadataWasCached()=runBlocking {
  val id="cached-${System.nanoTime()}";var providerCalls=0
  CatalogDetailsWarmupCache.movieOrLoad(id,"g","movie") {MovieDetails("cached")}
  try {
   val result=MovieOpeningPolicy.prepare {
    val details=CatalogDetailsWarmupCache.movieOrLoad(id,"g","movie") {providerCalls++;MovieDetails("new")}
    if(currentCoroutineContext()[MovieOpeningAttempt]!!.number==1)awaitCancellation()
    details
   }
   assertEquals("new",result?.movieId);assertEquals(1,providerCalls)
  } finally {CatalogDetailsWarmupCache.clearPlaylist(id)}
 }
 @Test fun actualHttpClientCreatesThreeDistinctRequestsDespiteBlockedDisconnect()=runBlocking {
  val release=CountDownLatch(1);val calls=AtomicInteger();val closing=CopyOnWriteArrayList<Int>()
  val starts=CopyOnWriteArrayList<Long>();val epoch=System.nanoTime()
  fun elapsed()=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-epoch)
  URL.setURLStreamHandlerFactory { protocol ->
   if(protocol!="retrytest") null else object:URLStreamHandler() {
    override fun openConnection(url:URL):URLConnection {
     val number=calls.incrementAndGet();starts.add(elapsed())
     return object:HttpURLConnection(url) {
      override fun connect() {}
      override fun usingProxy()=false
      override fun getResponseCode():Int {
       if(number<=2)check(release.await(35,TimeUnit.SECONDS))
       return 200
      }
      override fun getInputStream()=ByteArrayInputStream("fresh response $number".toByteArray())
      override fun getContentType()="application/json"
      override fun getHeaderField(name:String?):String?=null
      override fun disconnect() {
       closing.add(number)
       if(number<=2)release.await(35,TimeUnit.SECONDS)
      }
     }
    }
   }
  }
  val client=DefaultPlaylistHttpClient()
  try {
   val result=MovieOpeningPolicy.prepare {client.get("retrytest://provider/player_api.php?action=get_vod_info")}
   assertEquals("fresh response 3",result);assertEquals(3,calls.get())
   assertTrue("second request at 5s: $starts",starts[1] in 4_500L..6_500L)
   assertTrue("third request at 10s: $starts",starts[2] in 9_500L..12_000L)
   assertTrue("closing first was requested",closing.contains(1))
   assertTrue("closing second was requested",closing.contains(2))
   assertTrue("must not consume full 15s",elapsed()<15_000)
  } finally {release.countDown()}
 }
}
