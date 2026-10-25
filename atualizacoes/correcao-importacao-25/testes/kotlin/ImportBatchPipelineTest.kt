package br.com.flaviotorres.iptvpremium.data.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ImportBatchPipelineTest {
 @Test fun orderedWritesOverlapParsingButFinalResultWaitsForLastWrite()=runTest {
  val written=mutableListOf<Int>();val dispatcher=StandardTestDispatcher(testScheduler)
  val result=ImportBatchPipeline.run<Int,String>(produce={send->repeat(10){delay(20);send(it)};"ready"},consume={delay(40);written+=it},writerDispatcher=dispatcher)
  assertEquals("ready",result);assertEquals((0..9).toList(),written)
  assertEquals(420,currentTime);assertTrue(currentTime<600)
 }
 @Test fun atMostOneBatchWaitsForWriter()=runTest {
  val gate=CompletableDeferred<Unit>();var built=0;var writing=0;val written=mutableListOf<Int>()
  val work=async {ImportBatchPipeline.run<Int,Unit>(produce={send->repeat(100){built++;send(it)}},consume={writing++;gate.await();written+=it},writerDispatcher=StandardTestDispatcher(testScheduler))}
  runCurrent();assertEquals(1,writing);assertEquals(3,built)
  gate.complete(Unit);work.await();assertEquals((0..99).toList(),written)
 }
 @Test fun writerFailureIsNotHiddenAsSuccessfulParseOrCancellation()=runTest {
  val original=IOException("disk failure")
  try {
   ImportBatchPipeline.run<Int,String>(produce={send->repeat(100){send(it)};"must not activate"},consume={throw original},writerDispatcher=StandardTestDispatcher(testScheduler))
   fail("Must fail")
  }catch(actual:IOException){assertEquals(original.message,actual.message);assertTrue(generateSequence<Throwable>(actual){it.cause}.any{it===original})}
 }
 @Test fun parserFailureCancelsQueuedWritesAndPreservesOriginalCause()=runTest {
  val original=IOException("incomplete json");var cancelled=false
  try {
   ImportBatchPipeline.run<Int,Unit>(produce={send->send(1);yield();throw original},consume={try{awaitCancellation()}finally{cancelled=true}},writerDispatcher=StandardTestDispatcher(testScheduler))
   fail("Must fail")
  }catch(actual:IOException){assertEquals(original.message,actual.message);assertTrue(generateSequence<Throwable>(actual){it.cause}.any{it===original})}
  assertTrue(cancelled)
 }
 @Test fun callerCancellationCancelsReaderAndWriter()=runTest {
  var readerStopped=false;var writerStopped=false
  val task=launch {ImportBatchPipeline.run<Int,Unit>(produce={send->try{repeat(100){send(it)}}finally{readerStopped=true}},consume={try{awaitCancellation()}finally{writerStopped=true}},writerDispatcher=StandardTestDispatcher(testScheduler))}
  runCurrent();task.cancelAndJoin();assertTrue(readerStopped);assertTrue(writerStopped)
 }
 @Test fun emptyCatalogStillCompletesWithoutWrites()=runTest {
  var writes=0
  assertEquals(0,ImportBatchPipeline.run<Int,Int>(produce={0},consume={writes++},writerDispatcher=StandardTestDispatcher(testScheduler)))
  assertEquals(0,writes)
 }
}
