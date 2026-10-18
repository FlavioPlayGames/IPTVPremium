package br.com.flaviotorres.iptvpremium.ui.series
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
@OptIn(ExperimentalCoroutinesApi::class)
fun main() {
 fun pass(name:String,ok:Boolean){check(ok){name};println("PASS $name")}
 runTest {
  val result=awaitSeriesArtworkWindow(0,{true},clock={testScheduler.currentTime})
  pass("artes em cache mantem abertura minima de dois segundos",result && testScheduler.currentTime+SERIES_OPENING_TRANSITION_MS==2000L)
 }
 runTest {
  var ready=false;launch{delay(1000);ready=true}
  val result=awaitSeriesArtworkWindow(0,{ready},clock={testScheduler.currentTime})
  pass("artes prontas liberam sem esperar prazo maximo",result && testScheduler.currentTime in 1000L..1032L && testScheduler.currentTime+SERIES_OPENING_TRANSITION_MS<=5000L)
 }
 runTest {
  val result=awaitSeriesArtworkWindow(0,{false},clock={testScheduler.currentTime})
  pass("artes lentas liberam com fundo no limite total de cinco segundos",!result && testScheduler.currentTime+SERIES_OPENING_TRANSITION_MS==5000L)
 }
 runTest {
  testScheduler.advanceTimeBy(1500);var ready=false;launch{delay(500);ready=true}
  val result=awaitSeriesArtworkWindow(0,{ready},clock={testScheduler.currentTime})
  pass("consulta dos episodios nao reinicia a janela das artes",result && testScheduler.currentTime<=2032L)
 }
 runTest {
  testScheduler.advanceTimeBy(2500)
  val result=awaitSeriesArtworkWindow(0,{false},clock={testScheduler.currentTime})
  pass("prazo restante respeita tempo desde o clique",!result && testScheduler.currentTime==3000L)
 }
 runTest {
  testScheduler.advanceTimeBy(4000)
  val result=awaitSeriesArtworkWindow(0,{false},clock={testScheduler.currentTime})
  pass("dados essenciais lentos nao recebem nova espera opcional",!result && testScheduler.currentTime==4000L)
 }
 runTest {
  var failed=false;launch{delay(100);failed=true}
  val result=awaitSeriesArtworkWindow(0,{false},{failed},{testScheduler.currentTime})
  pass("falha essencial interrompe espera das imagens",!result && testScheduler.currentTime<=132L)
 }
 runTest {
  var finished=false
  val job=launch{awaitSeriesArtworkWindow(0,{false},clock={testScheduler.currentTime});finished=true}
  testScheduler.advanceTimeBy(100);job.cancelAndJoin();testScheduler.advanceUntilIdle()
  pass("voltar cancela espera sem concluir abertura",job.isCancelled && !finished)
 }
}
