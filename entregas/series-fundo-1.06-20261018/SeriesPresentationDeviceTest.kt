package br.com.flaviotorres.iptvpremium.data.repository

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import br.com.flaviotorres.iptvpremium.ui.series.EpisodeCard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class SeriesPresentationDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun loadingTvWaitsForContentThenContinues() {
        val ready = mutableStateOf(false)
        val completed = java.util.concurrent.atomic.AtomicReference<Boolean?>(null)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            br.com.flaviotorres.iptvpremium.ui.series.SeriesLoadingOverlay("Fixture", ready.value,
                onComplete = { completed.set(it) }, detailsContent = {})
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithText("Carregando série").assertExists()
        compose.runOnIdle { assertNull(completed.get()); ready.value = true }
        compose.mainClock.autoAdvance = true
        compose.waitUntil(timeoutMillis = 8_000) { completed.get() != null }
        compose.waitForIdle()
        assertEquals(true, completed.get())
    }
    @Test fun missingEpisodePhotoUsesTheSeriesBackdropImmediately() {
        val backdrop = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
        compose.setContent { EpisodeCard(1, "Fixture", null, "fixture", backdrop.asImageBitmap(), null, false, onClick = {}) }
        compose.waitForIdle()
        val pixel = compose.onNodeWithContentDescription("Episódio 1: Fixture").captureToImage().toPixelMap()[16, 9]
        assertTrue(pixel.blue > .8f && pixel.red < .2f)
    }
    @Test fun actualEpisodePhotoReplacesTheSharedBackdrop() {
        val backdrop = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
        val photo = Bitmap.createBitmap(32, 18, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        val bytes = java.io.ByteArrayOutputStream().use { out -> photo.compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray() }
        photo.recycle()
        val socket = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        val release = CountDownLatch(1)
        val worker = thread(isDaemon = true) {
            try { socket.accept().use { client ->
                client.soTimeout = 3000
                val reader = client.getInputStream().bufferedReader()
                while (reader.readLine()?.takeIf { it.isNotEmpty() } != null) { }
                if (release.await(10, TimeUnit.SECONDS)) client.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray()); write(bytes); flush()
                }
            } } catch (_: java.io.IOException) { }
        }
        try {
            compose.setContent { EpisodeCard(1, "Fixture", "http://127.0.0.1:${socket.localPort}/photo.png", "fixture-photo", backdrop.asImageBitmap(), null, false, onClick = {}) }
            compose.waitForIdle()
            val node = compose.onNodeWithContentDescription("Episódio 1: Fixture")
            assertTrue(node.captureToImage().toPixelMap()[16, 9].blue > .8f)
            release.countDown()
            compose.waitUntil(timeoutMillis = 10_000) { node.captureToImage().toPixelMap()[16, 9].red > .8f }
        } finally { release.countDown(); socket.close(); worker.join(1000) }
    }
}
