package br.com.flaviotorres.iptvpremium.ui.series

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SeriesOpeningReadinessTest {
    @Test fun openingWaitsForSlowVisibleArtworkBeforeStartingTheUnchangedTransition()=runTest {
        var ready=false
        launch {delay(6000);ready=true}
        assertTrue(awaitSeriesArtworkWindow(0,{ready},clock={testScheduler.currentTime}))
        assertTrue(testScheduler.currentTime>=6000)
        assertEquals(2000L,SERIES_OPENING_TRANSITION_MS)
    }
    @Test fun unreachableArtworkHasABoundedWaitAndFailureCanExitImmediately()=runTest {
        assertFalse(awaitSeriesArtworkWindow(0,{false},clock={testScheduler.currentTime}))
        assertEquals(SERIES_OPENING_MAX_ARTWORK_MS-SERIES_OPENING_TRANSITION_MS,testScheduler.currentTime)
        val before=testScheduler.currentTime
        assertFalse(awaitSeriesArtworkWindow(before,{false},{true},clock={testScheduler.currentTime}))
        assertEquals(before,testScheduler.currentTime)
    }
}
