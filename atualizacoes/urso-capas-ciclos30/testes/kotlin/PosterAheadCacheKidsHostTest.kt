package br.com.flaviotorres.iptvpremium.core.storage

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[34])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class PosterAheadCacheKidsHostTest {
    @Test fun progressiveDiskQueuePreparesOnlyFiveAndReusesDiskOnRestart(): Unit = runBlocking {
        java.nio.FloatBuffer.allocate(1)
        val server=ServerSocket(0);val clients=ConcurrentLinkedQueue<Socket>();val requests=AtomicInteger()
        val image=java.io.ByteArrayOutputStream().also { out ->
            Bitmap.createBitmap(16,24,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
                .compress(Bitmap.CompressFormat.PNG,100,out)
        }.toByteArray()
        // Inicializa o decoder antes dos dois workers de rede.
        android.graphics.BitmapFactory.decodeByteArray(image,0,image.size)?.recycle()
        kotlin.concurrent.thread(isDaemon=true) {
            try { while(!server.isClosed) {
                val client=server.accept();clients.add(client)
                kotlin.concurrent.thread(isDaemon=true) {
                    try { client.use {
                        val r=it.getInputStream().bufferedReader();r.readLine()
                        while(!r.readLine().isNullOrEmpty()) { }
                        requests.incrementAndGet()
                        it.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\nContent-Length: ${image.size}\r\nContent-Type: image/png\r\nConnection: close\r\n\r\n".toByteArray())
                            write(image);flush()
                        }
                    } } catch (_:Exception) { }
                }
            } } catch (_:Exception) { }
        }
        val context=ApplicationProvider.getApplicationContext<Context>()
        val unique=java.util.UUID.randomUUID().toString()
        val posters=(0 until 80).map { "http://127.0.0.1:${server.localPort}/$unique/poster-$it.png" }
        val queue=br.com.flaviotorres.iptvpremium.data.cache.PosterAheadQueue()
        MoviePosterLoader.clearMemory()
        try {
            MoviePosterLoader.prefetchAhead(context,queue,10,0,posters)
            assertEquals(30,requests.get())
            for(index in 10..14) assertNotNull(MoviePosterLoader.peekBitmap(posters[index]))
            for(index in 15..39) assertNull(MoviePosterLoader.peekBitmap(posters[index]))
            val folder=java.io.File(context.cacheDir,"iptv_movie_posters_session")
            for (url in posters.subList(10,40)) {
                val hash=java.security.MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
                    .joinToString("") { "%02x".format(it.toInt() and 255) }
                assertTrue(java.io.File(folder,"$hash.img").isFile)
            }
            MoviePosterLoader.prefetchAhead(context,queue,14,0,posters)
            assertEquals(30,requests.get())
            MoviePosterLoader.prefetchAhead(context,queue,15,0,posters)
            assertEquals(60,requests.get())
            MoviePosterLoader.clearMemory()
            MoviePosterLoader.prefetchAhead(context,queue,15,0,posters)
            assertEquals(60,requests.get())
            for(index in 15..19) assertNotNull(MoviePosterLoader.peekBitmap(posters[index]))
            // Kids usa os mesmos arquivos e chaves, mesmo depois de esvaziar a RAM.
            br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value=true
            MoviePosterLoader.clearMemory()
            MoviePosterLoader.prefetchAhead(context,br.com.flaviotorres.iptvpremium.data.cache.PosterAheadQueue(),10,0,posters)
            assertEquals(60,requests.get())
            for (url in posters.take(5)) assertNotNull(MoviePosterLoader.load(context,url))
            assertEquals(65,requests.get())
            br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value=true
            try {
                MoviePosterLoader.prefetchAhead(context,br.com.flaviotorres.iptvpremium.data.cache.PosterAheadQueue(),30,0,posters)
                assertEquals(65,requests.get())
            } finally { br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value=false }
            val recent=java.io.File(folder,".recent-posters").readText()
            assertFalse(recent.contains("http"));assertFalse(recent.contains("127.0.0.1"))
            MoviePosterLoader.clearMemory()
            server.close()
            MoviePosterLoader.prepareRecentCachedPosters(context)
            for (url in posters.take(5)) assertNotNull(MoviePosterLoader.peekBitmap(url))
            assertEquals(65,requests.get())
            val field=MoviePosterLoader::class.java.getDeclaredField("memoryCache").apply { isAccessible=true }
            val cache=field.get(null) as android.util.LruCache<*,*>
            val maximum=cache.maxSize()
            try {
                cache.resize(16)
                MoviePosterLoader.trimMemory(.5f)
                assertTrue(cache.size()<=8);assertTrue(cache.size()>0)
                assertNotNull(MoviePosterLoader.peekBitmap(posters[4]))
                MoviePosterLoader.trimMemory(0f)
                assertEquals(0,cache.size())
            } finally { cache.resize(maximum) }
            assertNotNull(MoviePosterLoader.load(context,posters[0]))
            assertEquals(65,requests.get())
        } finally {
            br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value=false
            server.close();clients.forEach { runCatching { it.close() } } }
    }
}
