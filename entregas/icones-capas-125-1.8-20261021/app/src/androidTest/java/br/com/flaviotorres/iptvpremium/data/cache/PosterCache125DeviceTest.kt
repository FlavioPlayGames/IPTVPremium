package br.com.flaviotorres.iptvpremium.data.cache

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*

class PosterCache125DeviceTest {
    private fun key(n: Int) = n.toString(16).padStart(64, '0')
    private fun fixture(check: (File, PosterFileLru) -> Unit) {
        val root = Files.createTempDirectory("poster-125-").toFile()
        try { check(root, PosterFileLru(root, { it.delete() })) } finally { root.deleteRecursively() }
    }
    private fun write(root: File, lru: PosterFileLru, n: Int) {
        val lease = lru.acquire(key(n)) ?: error("No slot")
        lease.use { File(root, key(n) + ".img").writeBytes(byteArrayOf(1,2,3)) }
    }
    @Test fun rollingCacheKeepsExactly125AndReplacesOldest() = fixture { root,lru ->
        repeat(200) { write(root,lru,it) }
        assertEquals(125,lru.keys().size)
        assertEquals((75 until 200).map(::key).toSet(),lru.keys())
        assertEquals(125,root.listFiles()!!.count { it.extension=="img" })
    }
    @Test fun visibleCoversSurviveLongBackgroundDownload() = fixture { root,lru ->
        repeat(10) { write(root,lru,it) };lru.setVisible((0 until 10).map(::key).toSet())
        for(i in 10 until 300)write(root,lru,i)
        assertTrue(lru.keys().containsAll((0 until 10).map(::key)))
        assertEquals(125,lru.keys().size)
    }
    @Test fun activeWritersCannotBeEvictedAndLimitIsNeverExceeded() = fixture { _,lru ->
        val slots=(0 until 125).map { lru.acquire(key(it))!! }
        assertNull(lru.acquire(key(126)))
        slots.first().close()
        val next=lru.acquire(key(126));assertNotNull(next)
        next!!.close();slots.drop(1).forEach {it.close()}
        assertTrue(lru.keys().isEmpty())
    }
    @Test fun failedDownloadsReleaseTheirSlot() = fixture { _,lru ->
        repeat(300) { lru.acquire(key(it))!!.close() }
        assertTrue(lru.keys().isEmpty())
    }
    @Test fun recentAccessChangesEvictionOrder() = fixture { root,lru ->
        repeat(125) { write(root,lru,it) };lru.acquire(key(0))!!.close();write(root,lru,125)
        assertTrue(key(0)in lru.keys());assertFalse(key(1)in lru.keys())
    }
    @Test fun moviesAndSeriesHaveIndependent125Limits() {
        val root=Files.createTempDirectory("poster-kinds-").toFile()
        try {
            val movie=File(root,PosterKind.MOVIES.directory).apply {mkdirs()}
            val series=File(root,PosterKind.SERIES.directory).apply {mkdirs()}
            val a=PosterFileLru(movie,{it.delete()});val b=PosterFileLru(series,{it.delete()})
            repeat(125){write(movie,a,it);write(series,b,it)}
            write(movie,a,125)
            assertFalse(File(movie,key(0)+".img").exists());assertTrue(File(series,key(0)+".img").exists())
            assertEquals(250,root.walkTopDown().count {it.isFile&&it.extension=="img"})
        }finally{root.deleteRecursively()}
    }
    @Test fun reopeningUsesPersistentLastAccessOrder() = fixture { root,lru ->
        repeat(125){write(root,lru,it);File(root,key(it)+".img").setLastModified(1000L+it)}
        File(root,key(0)+".img").setLastModified(9999L)
        val reopened=PosterFileLru(root,{it.delete()});write(root,reopened,125)
        assertTrue(key(0)in reopened.keys());assertFalse(key(1)in reopened.keys())
    }
    @Test fun protectedGlobalCacheReaderIsSkipped() = fixture { root,_ ->
        val lru=PosterFileLru(root,{ if(it.name==key(0)+".img")false else it.delete() })
        repeat(125){write(root,lru,it)};write(root,lru,125)
        assertTrue(key(0)in lru.keys());assertFalse(key(1)in lru.keys())
    }
    @Test fun unrelatedAndTemporaryFilesAreNeverCountedOrDeleted() = fixture { root,lru ->
        val unrelated=File(root,"settings.json").apply {writeText("keep")}
        val partial=File(root,key(999)+".img.part").apply {writeText("pending")}
        repeat(150){write(root,lru,it)}
        assertEquals("keep",unrelated.readText());assertEquals("pending",partial.readText())
    }
    @Test fun cacheKeyCannotEscapeItsDirectory() = fixture { _,lru ->
        try{lru.acquire("../../private");fail()}catch(_:IllegalArgumentException){}
    }
    @Test fun rowStartAlignsToFiveWithoutSkippingPartialRow() {
        assertEquals(0,PosterCategoryPolicy.nextRow(0));assertEquals(5,PosterCategoryPolicy.nextRow(9))
        assertEquals(10,PosterCategoryPolicy.nextRow(10));assertEquals(10,PosterCategoryPolicy.nextRow(13))
        assertEquals(125,PosterCategoryPolicy.nextRow(125))
    }
    @Test fun downloadFinishesEachGroupBeforeStartingNextRow() = runBlocking {
        val requests=mutableListOf<Int>();val batches=mutableListOf<List<String?>>()
        downloadPosterRows(0,{offset->requests.add(offset);PosterPage((offset until offset+10).map{it.toString()},if(offset==0)10 else null)},
            {batches.add(it)},{})
        assertEquals(listOf(0,10),requests)
        assertEquals(listOf((0..4).map{it.toString()},(5..9).map{it.toString()},(10..14).map{it.toString()},(15..19).map{it.toString()}),batches)
    }
    @Test fun cancellationDoesNotAdvanceCursorOrStartAnotherRow() = runBlocking {
        val entered=CompletableDeferred<Unit>();var reads=0;var advanced=false
        val job=launch { downloadPosterRows(0,{reads++;PosterPage((0..9).map{it.toString()},10)},
            {entered.complete(Unit);awaitCancellation()},{advanced=true}) }
        entered.await();job.cancelAndJoin();assertEquals(1,reads);assertFalse(advanced)
    }
    @Test fun transientQueryFailureRetriesSamePage() = runBlocking {
        var reads=0;val delivered=mutableListOf<String?>()
        downloadPosterRows(5,{offset->assertEquals(5,offset);if(reads++==0)throw java.io.IOException();PosterPage(listOf("a"),null)},
            {delivered.addAll(it)},{fail("Terminal page advanced")})
        assertEquals(2,reads);assertEquals(listOf("a"),delivered)
    }
}
