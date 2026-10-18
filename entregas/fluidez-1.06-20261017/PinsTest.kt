package br.com.flaviotorres.iptvpremium.ui.theme
import kotlinx.coroutines.*
fun main()=runBlocking {
 fun test(name:String, f:()->Unit){f();println("PASS $name")}
 val pins=ActiveThemePins<Int>(100){it};pins.activate("green")
 test("icones e cena permanecem retidos no tema ativo") {
  repeat(6){check(pins.put("green","icon$it","icon$it",5))};check(pins.put("green","room","room",40))
  repeat(1000){repeat(6){check(pins.get("green","icon$it")==5)};check(pins.get("green","room")==40)}
  check(pins.size()==70)
 }
 test("reentrada no mesmo tema conserva as artes") {check(!pins.activate("green"));check(pins.get("green","room")==40)}
 test("troca de tema remove as referencias anteriores") {check(pins.activate("blue"));check(pins.size()==0);check(pins.get("green","room")==null)}
 test("decodificacao atrasada nao recoloca tema anterior") {check(!pins.put("green","old","old",1));check(pins.size()==0)}
 test("orcamento recusa excesso sem descartar icones reservados") {
  check(pins.put("blue","icon","icon",20));check(pins.put("blue","room","room",60));check(!pins.put("blue","extra","extra",30));check(pins.size()==80 && pins.get("blue","icon")==20)
 }
 test("troca de tamanho substitui apenas o mesmo grupo") {check(pins.put("blue","icon-large","icon",30));check(pins.get("blue","icon")==null);check(pins.get("blue","room")==60);check(pins.size()==90)}
 test("limpeza explicita remove reservas mesmo sem trocar tema") {
  pins.reset();check(pins.size()==0);check(pins.get("blue","room")==null)
  check(!pins.put("blue","late","late",1));pins.activate("blue");check(pins.put("blue","rebuilt","rebuilt",2))
 }
 test("limpeza do cache Android detectada uma vez sem apagar preferencias") {
  val folder=java.nio.file.Files.createTempDirectory("cache-reset").toFile()
  try {
   val markerFile=java.io.File(folder,"cache/.iptv_theme_cache")
   val marker=ArtworkCacheResetMarker(markerFile)
   val preferences=java.io.File(folder,"preferences.json");preferences.writeText("favoritos e historico")
   check(!marker.check());check(markerFile.isFile);check(!marker.check())
   markerFile.parentFile!!.deleteRecursively()
   check(marker.check());check(markerFile.isFile);check(!marker.check())
   check(preferences.readText()=="favoritos e historico")
  } finally { folder.deleteRecursively() }
 }
 test("todos os requisitos sao necessarios para ligar a TV") {
  for(a in listOf(false,true))for(b in listOf(false,true))for(c in listOf(false,true))check(seriesOpeningCanStart(a,b,c)==(a&&b&&c))
 }
 check(withTimeout(100) { awaitSeriesOpeningReady(1000,{true},{false}) })
 println("PASS conteudo ja pronto nao introduz espera")
 var ready=false;var failed=false
 val waiting=async { awaitSeriesOpeningReady(1000,{ready},{failed}) }
 delay(65);check(!waiting.isCompleted);ready=true;check(waiting.await());println("PASS TV aguarda conteudo real antes de ligar")
 ready=false;failed=false
 val failure=async { awaitSeriesOpeningReady(1000,{ready},{failed}) };delay(40);failed=true
 check(!failure.await());println("PASS falha libera pagina sem ligar TV")
 check(!awaitSeriesOpeningReady(50,{false},{false}));println("PASS timeout nao simula conteudo pronto")
 val cancelled=async{awaitSeriesOpeningReady(1000,{false},{false})};delay(25);cancelled.cancelAndJoin();check(cancelled.isCancelled);println("PASS voltar cancela espera pela serie")
 Unit
}
