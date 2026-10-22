package br.com.flaviotorres.iptvpremium.ui.movies

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieOpeningDeadlineTest {
 @Test fun thirdAttemptCanRecoverWithinOneDeadline()=runTest {
  var calls=0
  val result=MovieOpeningPolicy.prepare {calls++;delay(1000);if(calls==3) "ready" else null}
  assertEquals("ready",result);assertEquals(3,calls);assertEquals(3000,currentTime)
 }
 @Test fun stalledProviderGetsThreeAttemptsAndFifteenSecondsTotal()=runTest {
  var calls=0
  assertNull(MovieOpeningPolicy.prepare<String> {calls++;awaitCancellation()})
  assertEquals(3,calls);assertEquals(15000,currentTime)
 }
 @Test fun firstSuccessStopsRetriesAndManualRetryHasAFreshBudget()=runTest {
  var calls=0
  assertNull(MovieOpeningPolicy.prepare<String>{calls++;null});assertEquals(3,calls)
  assertEquals("ready",MovieOpeningPolicy.prepare {calls++;"ready"});assertEquals(4,calls)
 }
 @Test fun navigationCancellationNeverStartsAnotherAttempt()=runTest {
  var calls=0
  val job=launch {MovieOpeningPolicy.prepare<String>{calls++;awaitCancellation()}}
  runCurrent();job.cancelAndJoin();advanceUntilIdle();assertEquals(1,calls)
 }
}
