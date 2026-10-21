package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MediaOpeningPriorityDeviceTest {
    @Test fun selectedMediaPausesBackgroundUntilReturnToCategory()=runBlocking {
        val gate=MediaPriorityGate();gate.setBusy(true);var ran=false
        val job=launch {gate.awaitIdle();ran=true};yield();assertFalse(ran)
        gate.setBusy(false);job.join();assertTrue(ran)
    }
    @Test fun settingPriorityDoesNotWaitForBackgroundJob()=runBlocking {
        val gate=MediaPriorityGate();val never=CompletableDeferred<Unit>()
        val job=launch {never.await()};gate.setBusy(true)
        assertTrue(gate.busy.value);assertTrue(job.isActive);job.cancelAndJoin()
    }
    @Test fun cancellationNeverExecutesDeferredWork()=runBlocking {
        val gate=MediaPriorityGate();gate.setBusy(true);var ran=false
        val job=launch {gate.awaitIdle();ran=true};yield();job.cancelAndJoin();gate.setBusy(false);assertFalse(ran)
    }
    @Test fun backgroundRequestInterruptsAsynchronouslyOnClick()=runBlocking {
        MediaOpeningPriority.setBusy(false)
        val interrupted=CompletableDeferred<Unit>()
        val watcher=MediaOpeningPriority.interruptBackgroundOnSelection{interrupted.complete(Unit)}
        try {MediaOpeningPriority.setBusy(true);withTimeout(2000){interrupted.await()};assertTrue(interrupted.isCompleted)}
        finally {watcher.cancelAndJoin();MediaOpeningPriority.setBusy(false)}
    }
    @Test fun completedBackgroundRequestDoesNotGetInterruptedLater()=runBlocking {
        MediaOpeningPriority.setBusy(false);var interrupted=false
        val watcher=MediaOpeningPriority.interruptBackgroundOnSelection{interrupted=true}
        watcher.cancelAndJoin();MediaOpeningPriority.setBusy(true)
        try {delay(20);assertFalse(interrupted)}finally{MediaOpeningPriority.setBusy(false)}
    }
}
