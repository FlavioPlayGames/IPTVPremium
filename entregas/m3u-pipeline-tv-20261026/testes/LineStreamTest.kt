package br.com.flaviotorres.iptvpremium.data.remote
import java.io.BufferedReader
import java.io.StringReader
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*

fun main()=runBlocking {
 val text=(0..1000).joinToString("\n"){if(it==0)"\uFEFF#EXTM3U" else if(it%5==0)"" else "Série Coração $it"}
 val lines=mutableListOf<String>();var first=0
 val metrics=M3uLineStream.consume(text.reader().buffered(),{first++}) {lines.add(it)}
 check(lines==text.reader().buffered().readLines());check(first==1);check(metrics.peakLines<=128)
 println("PASS bounded line pipeline preserves order, BOM, UTF-8, empty lines and first-line callback")
 val reads=AtomicInteger();val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
 val reader=object:BufferedReader(StringReader("")) {
  override fun readLine():String?=if(reads.incrementAndGet()<=10000)"line" else null
 }
 val job=launch {M3uLineStream.consume(reader) {entered.complete(Unit);release.await()}}
 entered.await();delay(100)
 check(reads.get()<=384){"Unbounded lookahead ${reads.get()}"}
 withTimeout(1500){job.cancelAndJoin()}
 val ended=reads.get();delay(50);check(reads.get()==ended)
 println("PASS reader backpressure at <=384 short lines and cancellation releases queued work")
 val broken=object:BufferedReader(StringReader("")) {
  var n=0
  override fun readLine():String? {if(n++==150)throw IOException("test read error");return "line"}
 }
 check(runCatching{M3uLineStream.consume(broken){delay(1)}}.exceptionOrNull() is IOException)
 println("PASS producer read error propagates and cancels parsing")
}
