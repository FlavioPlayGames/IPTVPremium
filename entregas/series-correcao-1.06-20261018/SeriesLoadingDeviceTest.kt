package br.com.flaviotorres.iptvpremium.data.repository

import br.com.flaviotorres.iptvpremium.data.cache.CatalogDetailsWarmupCache
import br.com.flaviotorres.iptvpremium.data.remote.DefaultPlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/** Exercises Android's real HttpURLConnection/JSON implementation on loopback only. */
class SeriesLoadingDeviceTest {
    @Test fun validSeriesResponseAfterNineSecondsDoesNotRestartTheRequest() = runBlocking {
        FixtureServer(responseBody = ONE_EPISODE, bodyDelayMs = 9_000L).use { server ->
            val result = withTimeout(25_000L) {
                SeriesEpisodesRepository(DefaultPlaylistHttpClient()).load(server.source(), "7")
            }
            assertNotNull("A healthy response must survive the former 8s read timeout", result)
            assertEquals(listOf("11"), result!!.episodes.map { it.id })
            assertEquals("Do not restart a response that is still arriving", 1, server.requests.get())
        }
    }

    @Test fun phpSeasonObjectsAndNestedEpisodeArraysLoadOnTheFirstRequest() = runBlocking {
        FixtureServer(responseBody = " \r\n\uFEFF" + PHP_EPISODES).use { server ->
            val result = withTimeout(10_000L) {
                SeriesEpisodesRepository(DefaultPlaylistHttpClient()).load(server.source(), "7")
            }
            assertNotNull(result)
            assertEquals(listOf("11", "12", "21"), result!!.episodes.map { it.id })
            assertEquals(listOf(1, 2), result.seasons.map { it.number })
            assertEquals(listOf(2, 1), result.seasons.map { it.episodeCount })
            assertEquals(1, server.requests.get())
        }
    }

    @Test fun overlappingFocusAndOpeningShareOnePermanentHttpFailure() = runBlocking {
        FixtureServer(responseBody = "{}", status = 403, holdResponse = true).use { server ->
            val source = server.source()
            val repository = SeriesEpisodesRepository(DefaultPlaylistHttpClient())
            try {
                withTimeout(15_000L) {
                    val focus = launch {
                        CatalogDetailsWarmupCache.prefetchSeries(source.id, "fixture-generation", "7") {
                            repository.load(source, "7")
                        }
                    }
                    assertTrue("Focus request must reach the local server", withContext(Dispatchers.IO) {
                        server.requestReceived.await(5, TimeUnit.SECONDS)
                    })
                    // UNDISPATCHED enters the existing key's lock before its owner
                    // receives 403; both callers therefore belong to the same cohort.
                    val opening = async(start = CoroutineStart.UNDISPATCHED) {
                        runCatching {
                            CatalogDetailsWarmupCache.seriesOrLoad(source.id, "fixture-generation", "7") {
                                repository.load(source, "7")
                            }
                        }
                    }
                    server.releaseResponse.countDown()
                    val failure = opening.await().exceptionOrNull()
                    focus.join()
                    assertTrue("Opening must receive the permanent 403 failure", failure is IllegalStateException)
                    assertTrue(failure!!.message.orEmpty().contains("403"))
                    assertEquals("Focus and click must not each repeat the denied request", 1, server.requests.get())
                }
            } finally {
                server.releaseResponse.countDown()
                CatalogDetailsWarmupCache.clearPlaylist(source.id)
            }
        }
    }

    private class FixtureServer(
        responseBody: String,
        private val status: Int = 200,
        private val bodyDelayMs: Long = 0L,
        holdResponse: Boolean = false
    ) : Closeable {
        private val body = responseBody.toByteArray(Charsets.UTF_8)
        private val socket = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
        val requests = AtomicInteger(0)
        val requestReceived = CountDownLatch(1)
        val releaseResponse = CountDownLatch(if (holdResponse) 1 else 0)
        private val worker = thread(isDaemon = true, name = "SeriesLoadingFixtureHttp") {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { client ->
                        client.soTimeout = 3_000
                        val input = client.getInputStream().bufferedReader(Charsets.UTF_8)
                        input.readLine() ?: return@use
                        while (true) {
                            val line = input.readLine() ?: break
                            if (line.isEmpty()) break
                        }
                        requests.incrementAndGet()
                        requestReceived.countDown()
                        if (!releaseResponse.await(12, TimeUnit.SECONDS)) return@use
                        val reason = if (status == 200) "OK" else "Forbidden"
                        client.getOutputStream().apply {
                            write(("HTTP/1.1 $status $reason\r\n" +
                                "Content-Type: application/json; charset=utf-8\r\n" +
                                "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
                            flush()
                            // Headers arrive immediately; only the valid JSON body
                            // exceeds the old read timeout, reproducing that regression.
                            if (bodyDelayMs > 0L) Thread.sleep(bodyDelayMs)
                            write(body)
                            flush()
                        }
                    }
                } catch (_: IOException) {
                    if (socket.isClosed) break
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        fun source() = PlaylistSource.XtreamCodes(
            id = "series-device-test-${UUID.randomUUID()}",
            name = "Local fixture",
            serverUrl = "http://127.0.0.1:${socket.localPort}",
            username = "",
            password = ""
        )

        override fun close() {
            releaseResponse.countDown()
            socket.close()
            worker.interrupt()
            worker.join(2_000L)
        }
    }

    private companion object {
        const val ONE_EPISODE = """{"info":{"name":"Local fixture"},"episodes":{"1":[{"id":"11","episode_num":1,"season":1,"title":"Episode 1","container_extension":"mp4"}]}}"""
        const val PHP_EPISODES = """{"info":{"name":"Local fixture"},"episodes":{"1":{"0":{"id":"11","episode_num":1,"title":"Episode 1"},"3":{"episode_id":"12","episode_num":2,"title":"Episode 2"}},"2":[[{"id":"21","episode_num":1,"title":"Episode 1"}]]},"seasons":[{"season_number":1,"name":"Season 1"},{"season_number":2,"name":"Season 2"}]}"""
    }
}
