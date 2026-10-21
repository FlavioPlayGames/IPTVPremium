package br.com.flaviotorres.iptvpremium.data.cache

import org.junit.Assert.*
import org.junit.Test

class ImageRamPolicyHostTest {
    @Test fun lowRamUsesOneShared24MbBudget() {
        val budget=ImageRamPolicy.calculate(128,true)
        assertEquals(ImageRamBudget(10,10,4),budget)
        assertEquals(24,budget.totalMb)
    }
    @Test fun budgetsNeverExceedOneFifthOfHeapOrTheirCaps() {
        for(heap in listOf(16,32,64,128,256,512,1024)) for(low in listOf(false,true)) {
            val b=ImageRamPolicy.calculate(heap,low)
            assertTrue(b.totalMb<=heap/5)
            assertTrue(b.totalMb<=if(low)24 else 56)
            assertTrue(b.postersMb in 1..20);assertTrue(b.detailsMb in 1..24);assertTrue(b.carouselMb in 1..12)
        }
    }
    @Test fun backgroundTrimIsPartialAndCriticalTrimIsComplete() {
        assertEquals(.75f,ImageRamPolicy.retainedFraction(20),0f)
        assertEquals(.5f,ImageRamPolicy.retainedFraction(10),0f)
        assertEquals(.25f,ImageRamPolicy.retainedFraction(15),0f)
        assertEquals(0f,ImageRamPolicy.retainedFraction(80),0f)
        assertEquals(1f,ImageRamPolicy.retainedFraction(0),0f)
    }
    @Test fun heapPressureHasHysteresisAndSystemPressureWins() {
        assertFalse(ImageRamPolicy.heapPressure(false,79,100,false))
        assertTrue(ImageRamPolicy.heapPressure(false,80,100,false))
        assertTrue(ImageRamPolicy.heapPressure(true,70,100,false))
        assertFalse(ImageRamPolicy.heapPressure(true,64,100,false))
        assertTrue(ImageRamPolicy.heapPressure(false,1,100,true))
    }
    @Test fun recentKeysAreBoundedAndRejectPathsAndAddresses() {
        val keys=(0..9).map { it.toString().padStart(64,'0') }
        val encoded=RecentPosterKeys.encode(keys+keys+listOf("../secret","https://user:password@host/cover"))
        assertEquals(keys.take(5),RecentPosterKeys.decode(encoded))
        assertFalse(encoded.contains("secret"));assertFalse(encoded.contains("http"))
        assertTrue(RecentPosterKeys.decode("x".repeat(1025)).isEmpty())
    }
}
