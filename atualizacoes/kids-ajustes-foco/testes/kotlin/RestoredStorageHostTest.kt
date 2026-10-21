package br.com.flaviotorres.iptvpremium.core.storage

import java.io.File
import java.nio.file.Files
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class RestoredStorageHostTest {
    private fun fixture(block: (File, File, CacheStorageBudget) -> Unit) {
        val data = Files.createTempDirectory("restored-cache-").toFile()
        val cache = File(data,"cache").apply { mkdirs() }
        val subs = File(data,"files/iptv_subtitles_lru_5mb").apply { mkdirs() }
        val limits = CacheStorageKind.entries.associateWith { when(it) {
            CacheStorageKind.IMAGES -> 40L
            CacheStorageKind.EPISODES, CacheStorageKind.RESPONSES -> 25L
            CacheStorageKind.SUBTITLES -> 5L
            CacheStorageKind.OTHER -> 80L
        } }
        try { block(cache,subs,CacheStorageBudget(cache,subs,totalLimit=100,limits=limits)) }
        finally { data.deleteRecursively() }
    }
    private fun save(budget: CacheStorageBudget, file: File, size: Int): Boolean {
        file.parentFile!!.mkdirs()
        val lease = budget.reserve(file,size.toLong()) ?: return false
        lease.use { it.temporary.writeBytes(ByteArray(size)); assertTrue(it.temporary.renameTo(file)) }
        return true
    }
    @Test fun decimalStorageBudgetsAreSeparateFromRam() {
        assertEquals(350_000_000L,CacheStorageLimits.TOTAL)
        assertEquals(100_000_000L,CacheStorageLimits.IMAGES)
        assertEquals(25_000_000L,CacheStorageLimits.EPISODES)
        assertEquals(25_000_000L,CacheStorageLimits.RESPONSES)
        assertEquals(5_000_000L,CacheStorageLimits.SUBTITLES)
        assertEquals(CacheStorageLimits.TOTAL,CacheStorageLimits.IMAGES+CacheStorageLimits.EPISODES+CacheStorageLimits.RESPONSES+CacheStorageLimits.SUBTITLES+CacheStorageLimits.OTHER)
    }
    @Test fun imageBudgetIsSharedByPostersLogosAndCast() = fixture { root,_,budget ->
        val poster=File(root,"iptv_movie_posters_session/old.img")
        assertTrue(save(budget,poster,25))
        assertTrue(save(budget,File(root,"movie_details_images/cast.img"),25))
        assertFalse(poster.exists())
        assertEquals(25L,budget.accountedBytes())
    }
    @Test fun eachMetadataBucketHasItsOwnQuota() = fixture { root,subs,budget ->
        for ((path,amount) in listOf("series_episode_details" to 25,"series_public_artwork_v1" to 25)) {
            val old=File(root,"$path/old.json");assertTrue(save(budget,old,amount))
            assertTrue(save(budget,File(root,"$path/new.json"),5));assertFalse(old.exists())
        }
        val s=File(subs,"old.srt");assertTrue(save(budget,s,5))
        assertTrue(save(budget,File(subs,"new.srt"),1));assertFalse(s.exists())
    }
    @Test fun totalLimitAppliesEvenWhenEveryIndividualBucketFits() = fixture { root,_,budget ->
        val image=File(root,"iptv_movie_posters_session/a.img")
        assertTrue(save(budget,image,30));assertTrue(save(budget,File(root,"series_episode_details/e.json"),25))
        assertTrue(save(budget,File(root,"series_public_artwork_v1/r.json"),25))
        assertTrue(save(budget,File(root,"updates/app.apk"),40))
        assertTrue(budget.accountedBytes()<=100)
        assertFalse(image.exists())
    }
    @Test fun readersAreNeverEvictedAndFullPinnedBucketRefusesNewWrites() = fixture { root,_,budget ->
        val file=File(root,"iptv_movie_posters_session/a.img");assertTrue(save(budget,file,40))
        budget.retain(file).use {
            assertNull(budget.reserve(File(root,"movie_details_images/b.img"),1))
            assertTrue(file.exists());assertEquals(40L,budget.accountedBytes())
        }
        assertTrue(save(budget,File(root,"movie_details_images/b.img"),10))
        assertFalse(file.exists())
    }
    @Test fun failedReplacementKeepsThePreviousFileAndReleasesTemporarySpace() = fixture { root,_,budget ->
        val target=File(root,"iptv_movie_posters_session/a.img");assertTrue(save(budget,target,10))
        val lease=budget.reserve(target,15)!!;lease.temporary.writeBytes(ByteArray(8))
        assertEquals(25L,budget.accountedBytes())
        lease.close();lease.close()
        assertEquals(10L,target.length());assertFalse(lease.temporary.exists());assertEquals(10L,budget.accountedBytes())
    }
    @Test fun simultaneousWritersReserveTheirWholeMaximumBeforeWriting() = fixture { root,_,budget ->
        val executor=Executors.newFixedThreadPool(6)
        val attempted=CountDownLatch(6);val release=CountDownLatch(1);val accepted=AtomicInteger()
        val pending=(0..5).map { i -> executor.submit {
            val lease=budget.reserve(File(root,"iptv_movie_posters_session/$i.img"),8)
            if(lease!=null) accepted.incrementAndGet()
            attempted.countDown()
            try { release.await(3,TimeUnit.SECONDS) } finally { lease?.close() }
        } }
        try {
            assertTrue(attempted.await(3,TimeUnit.SECONDS));assertEquals(5,accepted.get());assertEquals(40L,budget.accountedBytes())
        } finally {release.countDown();pending.forEach { it.get(3,TimeUnit.SECONDS) };executor.shutdownNow()}
        assertEquals(0L,budget.accountedBytes())
    }
    @Test fun maintenanceDoesNotTouchFavoritesCredentialsOrTheCatalogDatabase() = fixture { root,_,budget ->
        val marker=File(root,"history-session.marker").apply { writeText("keep-marker") }
        val favorite=File(root.parentFile,"files/favorites.json").apply { parentFile!!.mkdirs();writeText("keep") }
        val db=File(root.parentFile,"databases/playlist_cache.db").apply { parentFile!!.mkdirs();writeText("keep-db") }
        val old=File(root,"movie_details_images/legacy.img").apply { parentFile!!.mkdirs();writeBytes(ByteArray(150)) }
        budget.maintain();assertFalse(old.exists());assertEquals("keep",favorite.readText());assertEquals("keep-db",db.readText());assertEquals("keep-marker",marker.readText())
        try { budget.reserve(db,2);fail("Persistent data cannot be used as a cache target") } catch (_: IllegalArgumentException) { }
    }
    @Test fun legacyOverBudgetIsTrimmedOnTheFirstCacheOperation() = fixture { root,_,budget ->
        val old=File(root,"iptv_movie_posters_session/oversize.img").apply { parentFile!!.mkdirs();writeBytes(ByteArray(60)) }
        val apk=File(root,"updates/legacy.apk").apply { parentFile!!.mkdirs();writeBytes(ByteArray(120)) }
        assertTrue(budget.accountedBytes()<=100)
        assertFalse(old.exists());assertFalse(apk.exists())
    }
    @Test fun abandonedTemporaryIsRemovedButAnActiveReservationIsKept() = fixture { root,_,budget ->
        val orphan=File(root,"iptv_movie_posters_session/a.img.11111111-1111-1111-1111-111111111111.part")
            .apply { parentFile!!.mkdirs();writeBytes(ByteArray(4));setLastModified(1) }
        val lease=budget.reserve(File(root,"iptv_movie_posters_session/current.img"),10)!!
        lease.temporary.writeBytes(ByteArray(5));lease.temporary.setLastModified(1)
        budget.maintain()
        assertFalse(orphan.exists());assertTrue(lease.temporary.exists());assertEquals(10L,budget.accountedBytes())
        lease.close();assertEquals(0L,budget.accountedBytes())
    }
    @Test fun importDefaultsStayAt250And2000() {
        val p=br.com.flaviotorres.iptvpremium.data.cache.ImportProfile.DEFAULT
        assertEquals(250,p.batchSize);assertEquals(2000,p.m3uBatchSize)
    }
}
