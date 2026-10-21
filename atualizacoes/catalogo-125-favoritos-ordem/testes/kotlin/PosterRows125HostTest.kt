package br.com.flaviotorres.iptvpremium.data.cache
import java.io.File
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*
class PosterRows125HostTest {
    private fun page(offset:Int)=PosterPage((offset until offset+50).map {"poster-$it"},offset+50)
    @Test fun stopsAt125IncludingVisibleAndRefillsOnlyConsumedRow()=runBlocking {
        val batches=mutableListOf<List<String?>>()
        var cursor=10;val reads=mutableListOf<Int>()
        downloadPosterRows(cursor,{reads+=it;page(it)},{batches+=it},{cursor=it},endExclusive=125)
        assertEquals(125,cursor);assertEquals((10 until 125).map {"poster-$it"},batches.flatten())
        assertEquals(listOf(10,60,110),reads);assertTrue(batches.all {it.size==5})
        batches.clear()
        downloadPosterRows(cursor,::page,{batches+=it},{cursor=it},endExclusive=130)
        assertEquals(listOf((125..129).map {"poster-$it"}),batches);assertEquals(130,cursor)
    }
    @Test fun fourColumnsNeverStartNextRowBeforePreviousCompletes()=runBlocking {
        val batches=mutableListOf<List<String?>>()
        downloadPosterRows(8,::page,{batches+=it},{},endExclusive=125,columns=4)
        val indices=batches.flatten().map {it!!.removePrefix("poster-").toInt()}
        assertEquals((8 until 125).toList(),indices)
        for(b in batches){val n=b.map {it!!.removePrefix("poster-").toInt()};assertTrue(n.size<=4);assertEquals(n.first()/4,n.last()/4)}
    }
    @Test fun cancellationStopsBeforeAnotherBatchAndKeepsLastCompletedCursor()=runBlocking {
        val entered=CompletableDeferred<Unit>();var count=0;var cursor=10
        val job=launch(Dispatchers.Default){downloadPosterRows(10,::page,{count++;entered.complete(Unit);awaitCancellation()},{cursor=it},125)}
        withTimeout(5000){entered.await()};job.cancelAndJoin()
        assertEquals(1,count);assertEquals(10,cursor)
    }
    @Test fun queryFailureRetriesAreBoundedAndEndNeverLoops()=runBlocking {
        var reads=0
        downloadPosterRows(0,{reads++;throw java.io.IOException()},{fail("No rows")},{},125)
        assertEquals(3,reads)
        downloadPosterRows(0,{PosterPage(emptyList(),0)},{fail("No rows")},{},125)
    }
    @Test fun moviesAndSeriesHaveIndependent125FileLrusAndProtectVisible() {
        val root=kotlin.io.path.createTempDirectory("posters125").toFile()
        try {
            val ledgers=PosterKind.entries.associateWith { kind->
                val dir=File(root,kind.directory).apply{mkdirs()}
                PosterFileLru(dir,File::delete)
            }
            fun key(i:Int)=i.toString(16).padStart(64,'0')
            for((kind,lru) in ledgers) for(i in 0 until 125) lru.acquire(key(i))!!.use {
                File(File(root,kind.directory),"${key(i)}.img").writeBytes(byteArrayOf(1))
            }
            val movie=ledgers.getValue(PosterKind.MOVIES)
            movie.setVisible((0..9).map(::key).toSet())
            for(i in 125..129)movie.acquire(key(i))!!.use{File(File(root,PosterKind.MOVIES.directory),"${key(i)}.img").writeBytes(byteArrayOf(1))}
            assertEquals(125,movie.keys().size);assertTrue((0..9).all {key(it) in movie.keys()})
            assertTrue((10..14).none {key(it) in movie.keys()})
            assertEquals((0 until 125).map(::key).toSet(),ledgers.getValue(PosterKind.SERIES).keys())
            for(kind in PosterKind.entries)assertEquals(125,File(root,kind.directory).listFiles()!!.size)
        }finally{root.deleteRecursively()}
    }
    @Test fun restartReusesExistingFilesAndDoesNotEvictActiveReaders(){
        val dir=kotlin.io.path.createTempDirectory("posters125restart").toFile()
        try {
            fun key(i:Int)=i.toString(16).padStart(64,'0')
            for(i in 0 until 125)File(dir,"${key(i)}.img").writeText("cached")
            val lru=PosterFileLru(dir,File::delete);val leases=(0 until 125).map {lru.acquire(key(it))!!}
            assertNull(lru.acquire(key(125)));assertEquals(125,dir.listFiles()!!.size)
            leases.forEach{it.close()};assertNotNull(lru.acquire(key(125))?.also{it.close()})
            assertEquals(124,dir.listFiles()!!.size)
        }finally{dir.deleteRecursively()}
    }
}
