package br.com.flaviotorres.iptvpremium.ui.movies

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SelectedDetailsPreloadTest {
    @Test fun selectedPageAwaitsPrincipalArtThenCastAndReusesItOnReopen()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.CYAN)
        val bytes=ByteArrayOutputStream().also{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray()
        // Initialize the native decoder on Robolectric's sandbox thread, before IO workers.
        assertNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size))
        val paths=CopyOnWriteArrayList<String>();val principal=AtomicInteger();val violations=AtomicInteger()
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        val executor=Executors.newCachedThreadPool();server.executor=executor
        server.createContext("/") { exchange->
            val path=exchange.requestURI.path;paths+=path
            if(path.startsWith("/actor") && principal.get()!=2)violations.incrementAndGet()
            exchange.responseHeaders.add("Content-Type","image/png")
            exchange.sendResponseHeaders(200,bytes.size.toLong())
            exchange.responseBody.use{it.write(bytes)}
            if(path=="/backdrop" || path=="/logo")principal.incrementAndGet()
            exchange.close()
        }
        server.start()
        val origin="http://127.0.0.1:${server.address.port}"
        try {
            MediaOpeningPriority.setBusy(true)
            val result=preloadMovieDetailsImages(context,"$origin/backdrop","$origin/logo",960,540,
                listOf("$origin/actor1","$origin/actor2","$origin/actor3"))
            assertTrue(result.backdropReady);assertTrue(result.titleLogoReady)
            assertEquals(3,result.castPhotosReady);assertEquals(0,violations.get())
            val count=paths.size
            val again=preloadMovieDetailsImages(context,"$origin/backdrop","$origin/logo",960,540,
                listOf("$origin/actor1","$origin/actor2","$origin/actor3"))
            assertEquals(3,again.castPhotosReady);assertEquals(count,paths.size)
            // Poster download must not use the network while the selected page owns priority.
            assertNull(MoviePosterLoader.load(context,"$origin/poster"))
            assertFalse(paths.contains("/poster"))
        } finally {MediaOpeningPriority.setBusy(false);server.stop(0);executor.shutdownNow()}
    }
}
