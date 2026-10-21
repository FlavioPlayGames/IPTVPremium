package br.com.flaviotorres.iptvpremium.data.cache
import org.junit.Assert.*
import org.junit.Test
class PosterAheadQueueKidsHostTest {
    private fun fill(q:PosterAheadQueue,after:Int,end:Int=200):List<IntRange> {
        val result=mutableListOf<IntRange>()
        repeat(30) {val b=q.nextBatch(after,0,end)?:return result;result+=b;q.completed(b)}
        error("Unbounded refill")
    }
    @Test fun initialThirtyInSixGroupsOfFive() {
        val q=PosterAheadQueue();val b=fill(q,10)
        assertEquals(6,b.size);assertEquals(10..14,b.first());assertEquals(35..39,b.last());assertTrue(fill(q,10).isEmpty())
    }
    @Test fun twentyFiveRemainingAddsThirtyRatherThanToppingUpToThirty() {
        val q=PosterAheadQueue();fill(q,10);assertTrue(fill(q,14).isEmpty())
        val b=fill(q,15);assertEquals(6,b.size);assertEquals(40..44,b.first());assertEquals(65..69,b.last())
        assertTrue(fill(q,15).isEmpty());assertTrue(fill(q,44).isEmpty())
        val next=fill(q,45);assertEquals(6,next.size);assertEquals(70..74,next.first());assertEquals(95..99,next.last())
    }
    @Test fun incompletePagingKeepsSameThirtyTargetWithoutAnotherCycle() {
        val q=PosterAheadQueue();assertEquals(listOf(10..14,15..16),fill(q,10,17))
        assertEquals(listOf(17..21,22..26,27..31,32..36,37..39),fill(q,10,80))
        assertTrue(fill(q,10).isEmpty())
    }
    @Test fun cancellationRetriesSameGroup() {
        val q=PosterAheadQueue();assertEquals(10..14,q.nextBatch(10,0,200));assertEquals(10..14,q.nextBatch(10,0,200))
    }
    @Test fun fourColumnsFollowSameCycle() {
        val q=PosterAheadQueue();assertEquals(6,fill(q,8).size);assertTrue(fill(q,12).isEmpty())
        val b=fill(q,16);assertEquals(6,b.size);assertEquals(38..42,b.first());assertEquals(63..67,b.last())
    }
    @Test fun endJumpAndBackwardRemainBounded() {
        val q=PosterAheadQueue();assertEquals(listOf(198..199),fill(q,198));assertTrue(fill(q,200).isEmpty())
        assertEquals(6,fill(q,20).size);assertEquals(6,fill(q,150).size);assertEquals(6,fill(q,140).size)
    }
}
