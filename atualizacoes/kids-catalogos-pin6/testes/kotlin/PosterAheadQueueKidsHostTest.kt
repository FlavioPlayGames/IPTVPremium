package br.com.flaviotorres.iptvpremium.data.cache

import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class PosterAheadQueueKidsHostTest {
    private fun fill(queue: PosterAheadQueue, after: Int, start: Int = 0, end: Int = 100): List<IntRange> {
        val batches = mutableListOf<IntRange>()
        repeat(20) {
            val batch = queue.nextBatch(after, start, end) ?: return batches
            batches.add(batch); queue.completed(batch)
        }
        error("Unbounded queue")
    }
    @Test fun fillsThirtyFiveInSevenGroupsOfFive() {
        val queue=PosterAheadQueue()
        assertEquals(listOf(10..14,15..19,20..24,25..29,30..34,35..39,40..44),fill(queue,10))
        assertNull(queue.nextBatch(10,0,100))
    }
    @Test fun refillsExactlyFiveWhenThirtyRemain() {
        val queue=PosterAheadQueue();fill(queue,10)
        assertTrue(fill(queue,14).isEmpty())
        assertEquals(listOf(45..49),fill(queue,15))
        assertTrue(fill(queue,19).isEmpty())
        assertEquals(listOf(50..54),fill(queue,20))
    }
    @Test fun fourColumnLayoutNeedsNoSpecialCase() {
        val queue=PosterAheadQueue()
        assertEquals(listOf(8..12,13..17,18..22,23..27,28..32,33..37,38..42),fill(queue,8))
        assertTrue(fill(queue,12).isEmpty())
        assertEquals(listOf(43..47,48..50),fill(queue,16))
    }
    @Test fun cancellationDoesNotAdvanceUncompletedGroup() {
        val queue=PosterAheadQueue()
        assertEquals(10..14,queue.nextBatch(10,0,100))
        assertEquals(10..14,queue.nextBatch(10,0,100))
    }
    @Test fun waitsForPagingAndContinuesPartialGroup() {
        val queue=PosterAheadQueue()
        assertEquals(listOf(10..14,15..16),fill(queue,10,end=17))
        assertEquals(listOf(17..21,22..26,27..29),fill(queue,10,end=30))
    }
    @Test fun categoryEndAndLargeJumpAreBounded() {
        val queue=PosterAheadQueue()
        assertEquals(listOf(98..99),fill(queue,98))
        assertTrue(fill(queue,100).isEmpty())
        assertEquals(listOf(20..24,25..29,30..34,35..39,40..44,45..49,50..54),fill(queue,20))
    }
}
