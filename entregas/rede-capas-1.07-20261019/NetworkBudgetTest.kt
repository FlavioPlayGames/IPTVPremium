package br.com.flaviotorres.iptvpremium.data.cache
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

fun main()=runBlocking {
 var passed=0
 suspend fun test(name:String, body:suspend CoroutineScope.()->Unit) {withTimeout(3000){coroutineScope(body)};passed++;println("PASS $name")}
 test("six visible downloads can run concurrently; seventh waits") {
  val budget=PosterNetworkBudget();val arrived=Channel<Unit>(20);val release=CompletableDeferred<Unit>();val active=AtomicInteger();val peak=AtomicInteger()
  val jobs=(0 until 12).map{launch(Dispatchers.Default){budget.run(false){val n=active.incrementAndGet();peak.updateAndGet{maxOf(it,n)};arrived.send(Unit);try{release.await()}finally{active.decrementAndGet()}}}}
  repeat(6){arrived.receive()};check(active.get()==6);check(arrived.tryReceive().isFailure);release.complete(Unit);jobs.joinAll();check(peak.get()==6&&active.get()==0)
 }
 test("anticipation occupies at most two slots and leaves four for visible downloads") {
  val b=PosterNetworkBudget();val bg=Channel<Unit>(20);val fg=Channel<Unit>(20);val release=CompletableDeferred<Unit>();val count=AtomicInteger()
  val jobs=(0 until 8).map{launch(Dispatchers.Default){b.run(true){count.incrementAndGet();bg.send(Unit);try{release.await()}finally{count.decrementAndGet()}}}}
  repeat(2){bg.receive()};check(count.get()==2);check(bg.tryReceive().isFailure)
  val foreground=(0 until 4).map{launch(Dispatchers.Default){b.run(false){fg.send(Unit);release.await()}}};repeat(4){fg.receive()};release.complete(Unit);(jobs+foreground).joinAll()
 }
 test("a failed foreground request returns its permit") {
  val b=PosterNetworkBudget();check(runCatching{b.run(false){error("network failure")}}.isFailure);repeat(12){check(b.run(false){42}==42)}
 }
 test("a failed anticipation request returns both permits") {
  val b=PosterNetworkBudget();check(runCatching{b.run(true){error("network failure")}}.isFailure);repeat(12){check(b.run(true){42}==42)}
 }
 test("cancelling queued work does not consume a connection or run the download") {
  val b=PosterNetworkBudget();val entered=Channel<Unit>(6);val release=CompletableDeferred<Unit>();val jobs=(0 until 6).map{launch{b.run(false){entered.send(Unit);release.await()}}};repeat(6){entered.receive()}
  var ran=false;val queued=launch{b.run(false){ran=true}};yield();queued.cancelAndJoin();check(!ran);release.complete(Unit);jobs.joinAll();check(b.run(false){true})
 }
 test("cancelling an active background request frees capacity") {
  val b=PosterNetworkBudget();val entered=CompletableDeferred<Unit>();val active=launch{b.run(true){entered.complete(Unit);awaitCancellation()}};entered.await();active.cancelAndJoin();repeat(6){check(b.run(true){true})}
 }
 test("background cancellation never blocks subsequent visible work") {
  val b=PosterNetworkBudget();val started=Channel<Unit>(2);val holder=(0 until 2).map{launch{b.run(true){started.send(Unit);awaitCancellation()}}};repeat(2){started.receive()}
  val waiting=launch{b.run(true){error("cancelled queued work executed")}};yield();waiting.cancelAndJoin();holder.forEach{it.cancelAndJoin()};val done=AtomicInteger();(0 until 30).map{launch{b.run(false){done.incrementAndGet()}}}.joinAll();check(done.get()==30)
 }
 println("$passed checks passed")
}
