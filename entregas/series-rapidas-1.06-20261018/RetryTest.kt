package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.io.IOException
@OptIn(ExperimentalCoroutinesApi::class)
fun main()=runTest {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val source=PlaylistSource.XtreamCodes("p","Fixture",serverUrl="https://example.invalid",username="test",password="test")
 val good="""{"episodes":{"1":[{"id":"100","season":1,"episode_num":1}]}}"""
 fun client(action:suspend(Int)->String):Pair<PlaylistHttpClient,()->Int> {
  var calls=0
  return object:PlaylistHttpClient {
   override suspend fun get(url:String):String=action(++calls)
   override suspend fun readLines(url:String,onLine:suspend(String)->Unit){}
  } to {calls}
 }
 val(first,count)=client{if(it==1)throw IllegalStateException("Erro HTTP 429 ao acessar a lista.") else good}
 val start=testScheduler.currentTime
 pass("429 rapido recupera na segunda tentativa",SeriesEpisodesRepository(first).load(source,"1")?.episodes?.size==1 && count()==2 && testScheduler.currentTime-start==400L)
 val(reset,rc)=client{if(it<3)throw IOException("reset") else good};val t=testScheduler.currentTime
 pass("reset rapido usa ate tres tentativas com pausas 400 e 1200ms",SeriesEpisodesRepository(reset).load(source,"1")?.episodes?.size==1 && rc()==3 && testScheduler.currentTime-t==1600L)
 for(status in listOf(401,403,404)) {
  val(c,n)=client{throw IllegalStateException("Erro HTTP $status ao acessar a lista.")}
  val result=runCatching{SeriesEpisodesRepository(c).load(source,"1")}
  pass("HTTP $status permanente nao repete",result.isFailure && n()==1)
 }
 val(html,hc)=client{if(it==1)"<html>overload</html>" else good}
 pass("HTML temporario nao vira serie vazia definitiva",SeriesEpisodesRepository(html).load(source,"1")?.episodes?.size==1 && hc()==2)
 val(empty,ec)=client{"""{"episodes":{}}"""}
 pass("JSON vazio tenta recuperar automaticamente tres vezes",SeriesEpisodesRepository(empty).load(source,"1")?.episodes?.isEmpty()==true && ec()==3)
 var clock=0L;val(slow,sc)=client{clock+=10_000_000_000;throw IOException("timeout")}
 pass("timeout lento persistente recebe uma recuperacao automatica",runCatching{SeriesEpisodesRepository(slow,{clock}).load(source,"1")}.isFailure && sc()==2)
 val(cancel,cc)=client{throw CancellationException("saiu")}
 var wasCancelled=false;try{SeriesEpisodesRepository(cancel).load(source,"1")}catch(_:CancellationException){wasCancelled=true}
 pass("cancelamento nao e tratado como falha transitoria",wasCancelled && cc()==1)
 val(service,svc)=client{if(it==1)throw IllegalStateException("Erro HTTP 503 ao acessar a lista.") else good}
 pass("HTTP 503 temporario repete e recupera",SeriesEpisodesRepository(service).load(source,"1")?.episodes?.size==1 && svc()==2)
 val(always,ac)=client{throw IOException("reset")}
 pass("falha persistente termina na quarta tentativa",runCatching{SeriesEpisodesRepository(always).load(source,"1")}.isFailure && ac()==4)
 var slowClock=0L;val(slowBody,sbc)=client{slowClock+=10_000_000_000;"<html>overload</html>"}
 pass("resposta invalida lenta recebe uma recuperacao automatica",SeriesEpisodesRepository(slowBody,{slowClock}).load(source,"1")==null && sbc()==2)
 val(success,suc)=client{good}
 pass("primeira resposta valida nao adiciona pausa nem chamadas",SeriesEpisodesRepository(success).load(source,"1")?.episodes?.size==1 && suc()==1)
 var recoveredClock=0L
 val(slowRecover,src)=client{if(it==1){recoveredClock+=30_000_000_000;throw IOException("timeout")}else good}
 pass("timeout de 30s recupera sem botao Tentar",SeriesEpisodesRepository(slowRecover,{recoveredClock}).load(source,"1")?.episodes?.size==1 && src()==2)
 var htmlClock=0L
 val(htmlRecover,hrc)=client{if(it==1){htmlClock+=12_000_000_000;"<html>ocupado</html>"}else good}
 pass("HTML lento recupera sem intervencao",SeriesEpisodesRepository(htmlRecover,{htmlClock}).load(source,"1")?.episodes?.size==1 && hrc()==2)
 val(emptyRecover,erc)=client{if(it<3)"""{"episodes":{}}""" else good}
 pass("vazio temporario recupera na terceira resposta",SeriesEpisodesRepository(emptyRecover).load(source,"1")?.episodes?.size==1 && erc()==3)
 val(fourth,fc)=client{if(it<4)throw IOException("reset")else good}
 pass("quarta chamada recupera falha antes dependente do usuario",SeriesEpisodesRepository(fourth).load(source,"1")?.episodes?.size==1 && fc()==4)
 var budgetClock=0L
 val(budget,bc)=client{budgetClock+=45_000_000_000;throw IOException("timeout")}
 pass("orcamento nao inicia outra chamada depois de 45s",runCatching{SeriesEpisodesRepository(budget,{budgetClock}).load(source,"1")}.isFailure && bc()==1)
 val(pause,pc)=client{throw IOException("reset")}
 val pauseJob=launch{SeriesEpisodesRepository(pause).load(source,"1")}
 runCurrent();pauseJob.cancelAndJoin();advanceTimeBy(5000)
 pass("sair durante pausa impede novas chamadas",pc()==1)

}
