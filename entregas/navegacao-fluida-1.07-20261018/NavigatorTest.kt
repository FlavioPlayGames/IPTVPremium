package br.com.flaviotorres.iptvpremium.ui.components
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.*
private class Fixture(val scope:CoroutineScope,val columns:Int=5,val count:Int=200) {
 val grid=LazyGridState();var focus=2;var hosts=0;var manuals=0;var searches=0;var categories=0
 val requesters=mutableMapOf<Int,FocusRequester>()
 val nav=CatalogGridNavigator(grid,scope,columns,{count},{},{manuals++},
  onExitUp={searches++;focus=-2;navExit()},onExitLeft={categories++;focus=-1;navExit()})
 fun navExit(){nav.hostFocusChanged(false);nav.focusTreeChanged(false)}
 init {
  nav.host.onRequest={hosts++;nav.hostFocusChanged(true);nav.focusTreeChanged(true);true}
  grid.onScroll={show(it)};show(0);nav.focused(2);nav.focusTreeChanged(true)
 }
 fun show(first:Int){
  requesters.forEach{(i,r)->nav.detach(i,r)};requesters.clear()
  grid.layoutInfo.visibleItemsInfo=(first until minOf(count,first+columns*3)).map{Item(it,Coord(((it-first)/columns)*60),Coord(60))}
  grid.layoutInfo.visibleItemsInfo.forEach{item->
   val r=FocusRequester();r.onRequest={focus=item.index;nav.focused(item.index);nav.hostFocusChanged(false);nav.focusTreeChanged(true);true}
   requesters[item.index]=r;nav.attach(item.index,r)
  }
 }
 fun press(key:Key){check(nav.key(focus,KeyEvent(key,KeyEventType.KeyDown)));nav.key(focus,KeyEvent(key,KeyEventType.KeyUp))}
 fun start(index:Int){show((index/columns)*columns);focus=index;nav.focused(index)}
}
fun main()=runBlocking {
 suspend fun test(name:String,block:suspend ()->Unit){block();println("PASS $name")}
 val scope=CoroutineScope(coroutineContext+SupervisorJob())
 test("capas visiveis recebem foco sem transferencia intermediaria ou frames extras") {
  val f=Fixture(scope);f.press(Key.DirectionDown);f.press(Key.DirectionRight);check(f.focus==8&&f.hosts==0&&f.grid.scrollCalls==0)
 }
 test("vinte comandos rapidos mantem terceira coluna com um trabalhador de rolagem") {
  val f=Fixture(scope);repeat(20){f.press(Key.DirectionDown)};delay(40);check(f.focus==102&&f.hosts==1&&f.grid.scrollCalls==1)
 }
 test("inverter sentido antes da composicao mantem destino mais recente e coluna") {
  val f=Fixture(scope);repeat(20){f.press(Key.DirectionDown)};repeat(10){f.press(Key.DirectionUp)};delay(40);check(f.focus==52&&f.categories==0)
 }
 test("primeira fileira sobe explicitamente ao buscador sem rolagem") {
  val f=Fixture(scope);f.press(Key.DirectionUp);check(f.searches==1&&f.focus == -2&&f.grid.scrollCalls==0)
 }
 test("cima em fileira intermediaria move somente uma fileira") {
  val f=Fixture(scope);f.start(12);f.press(Key.DirectionUp);delay(40);check(f.focus==7&&f.searches==0)
 }
 test("esquerda sai para categorias somente na primeira coluna") {
  val f=Fixture(scope);f.press(Key.DirectionLeft);check(f.focus==1&&f.categories==0);f.press(Key.DirectionLeft);check(f.focus==0);f.press(Key.DirectionLeft);check(f.focus == -1&&f.categories==1)
 }
 test("direita na borda nao pula para a linha seguinte") {
  val f=Fixture(scope);f.press(Key.DirectionRight);f.press(Key.DirectionRight);repeat(5){f.press(Key.DirectionRight)};check(f.focus==4&&f.hosts==0)
 }
 test("voltar cancela destino pendente e nao rouba foco da categoria depois") {
  val f=Fixture(scope);repeat(20){f.press(Key.DirectionDown)};f.nav.leave();f.categories++;f.focus=-1;f.navExit();delay(40);check(f.focus == -1&&f.grid.scrollCalls==0)
 }
 test("fileira final incompleta nao desvia para primeira coluna") {
  val f=Fixture(scope,count=52);f.start(47);repeat(10){f.press(Key.DirectionDown)};delay(20);check(f.focus==47&&f.categories==0)
 }
 test("quatro colunas mantem a terceira durante repeticao rapida") {
  val f=Fixture(scope,columns=4);repeat(20){f.press(Key.DirectionDown)};delay(40);check(f.focus==82&&f.focus%4==2)
 }
 test("novos comandos durante rolagem substituem apenas o destino pendente") {
  val f=Fixture(scope);repeat(10){f.press(Key.DirectionDown)};yield();repeat(5){f.press(Key.DirectionDown)};delay(50);check(f.focus==77&&f.categories==0)
 }
 test("seta durante entrada da grade substitui restauracao inicial e nao fica presa no host") {
  val f=Fixture(scope)
  f.requesters.forEach { (i,r) -> f.nav.detach(i,r) };f.requesters.clear()
  f.nav.host.requestFocus()
  f.press(Key.DirectionDown)
  delay(5);f.show(0);delay(40)
  check(f.focus==7)
 }
 scope.cancel();Unit
}
