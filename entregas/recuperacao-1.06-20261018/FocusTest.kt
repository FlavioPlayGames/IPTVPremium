package br.com.flaviotorres.iptvpremium.data.cache
import br.com.flaviotorres.iptvpremium.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.util.concurrent.atomic.AtomicInteger
@OptIn(ExperimentalCoroutinesApi::class)
fun main()=runTest {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val calls=mutableListOf<String>();val focus=SeriesFocusPreloader(this){calls.add(it)}
 focus.focus("a");advanceTimeBy(200);focus.focus("b");focus.blur("a");advanceTimeBy(449);runCurrent()
 pass("passagem rapida e perda de foco antigo nao carregam card errado",calls.isEmpty())
 advanceTimeBy(1);runCurrent();pass("somente card parado por 450ms inicia carga",calls==listOf("b"));focus.dispose()
 val lost=mutableListOf<String>();val blur=SeriesFocusPreloader(this){lost.add(it)};blur.focus("x");advanceTimeBy(200);blur.blur("x");advanceTimeBy(1000);runCurrent();pass("sair do card antes do prazo cancela consulta pendente",lost.isEmpty())
 val exit=SeriesFocusPreloader(this){error("nao deveria iniciar")};exit.focus("x");exit.dispose();advanceTimeBy(1000);runCurrent();pass("sair da grade cancela aquecimento pendente",true)
 val began=CompletableDeferred<Unit>();val end=CompletableDeferred<Unit>();val finished=CompletableDeferred<Unit>()
 val promoted=SeriesFocusPreloader(this){began.complete(Unit);end.await();finished.complete(Unit)}
 promoted.focus("x");advanceTimeBy(450);runCurrent();began.await();promoted.commit("x");promoted.blur("x");promoted.dispose();runCurrent();pass("clique preserva consulta que o foco ja iniciou",!finished.isCompleted);end.complete(Unit);finished.await()
 fun result(id:String,n:Int)=SeriesEpisodesResult(id,null,null,null,null,null,listOf(SeriesSeasonInfo(1,"Um",null,n)),(1..n).map{SeriesEpisodeInfo("$it",1,it,"Episodio",null,null,null,null)})
 val entered=CompletableDeferred<Unit>();val response=CompletableDeferred<Unit>();val loads=AtomicInteger(0)
 val warm=launch { CatalogDetailsWarmupCache.prefetchSeries("share","g","1") {loads.incrementAndGet();entered.complete(Unit);response.await();result("1",1)} }
 entered.await();val click=async {CatalogDetailsWarmupCache.seriesOrLoad("share","g","1"){loads.incrementAndGet();result("1",1)}}
 runCurrent();advanceTimeBy(13000);runCurrent();response.complete(Unit);warm.join();val value=click.await()
 pass("clique compartilha consulta iniciada ha mais de 12s sem reiniciar",loads.get()==1 && value?.episodes?.size==1)
 CatalogDetailsWarmupCache.clearPlaylist("share")
 CatalogDetailsWarmupCache.seriesOrLoad("big","g","a"){result("a",3000)}
 pass("serie de 3000 episodios fica em cache",CatalogDetailsWarmupCache.getSeries("big","g","a")?.episodes?.size==3000)
 CatalogDetailsWarmupCache.seriesOrLoad("big","g","b"){result("b",3000)}
 CatalogDetailsWarmupCache.seriesOrLoad("big","g","c"){result("c",2000)}
 pass("total de 8000 episodios e permitido",listOf("a","b","c").all{CatalogDetailsWarmupCache.getSeries("big","g",it)!=null})
 CatalogDetailsWarmupCache.seriesOrLoad("big","g","d"){result("d",1)}
 pass("acima do total limite remove serie menos recente",CatalogDetailsWarmupCache.getSeries("big","g","a")==null && CatalogDetailsWarmupCache.getSeries("big","g","d")!=null)
 CatalogDetailsWarmupCache.clearPlaylist("big")
 CatalogDetailsWarmupCache.seriesOrLoad("memory","g","small"){result("small",1)}
 CatalogDetailsWarmupCache.seriesOrLoad("memory","g","heavy"){result("heavy",3000).let{r->r.copy(episodes=r.episodes.map{it.copy(synopsis="X".repeat(10000))})}}
 pass("texto excessivo nao ocupa cache nem remove series pequenas",CatalogDetailsWarmupCache.getSeries("memory","g","heavy")==null && CatalogDetailsWarmupCache.getSeries("memory","g","small")!=null)
 CatalogDetailsWarmupCache.clearPlaylist("memory")
 val release=CompletableDeferred<Unit>();val twoStarted=CompletableDeferred<Unit>();val active=AtomicInteger(0);val peak=AtomicInteger(0)
 val jobs=(1..3).map{id->launch{CatalogDetailsWarmupCache.prefetchSeries("limit","g","$id"){val n=active.incrementAndGet();peak.updateAndGet{maxOf(it,n)};if(n==2)twoStarted.complete(Unit);try{release.await();result("$id",1)}finally{active.decrementAndGet()}}}}
 twoStarted.await();runCurrent();pass("aquecimento limita a duas consultas simultaneas",peak.get()==2);release.complete(Unit);jobs.joinAll();CatalogDetailsWarmupCache.clearPlaylist("limit")
 // Aa e BB possuem o mesmo hash Java; nao devem dividir o carregamento.
 val gate=CompletableDeferred<Unit>();val startedA=CompletableDeferred<Unit>()
 val a=launch{CatalogDetailsWarmupCache.seriesOrLoad("collision","g","Aa"){startedA.complete(Unit);gate.await();result("Aa",1)}}
 startedA.await()
 val b=async{CatalogDetailsWarmupCache.seriesOrLoad("collision","g","BB"){result("BB",1)}}
 pass("serie lenta nao bloqueia outra com hash identico",withContext(Dispatchers.Default){withTimeout(3000){b.await()}}?.seriesId=="BB")
 val waiter=launch{CatalogDetailsWarmupCache.seriesOrLoad("collision","g","Aa"){error("nao deve duplicar")}}
 runCurrent();waiter.cancelAndJoin();gate.complete(Unit);a.join()
 pass("cancelar espera nao prejudica resultado do dono",CatalogDetailsWarmupCache.getSeries("collision","g","Aa")!=null)
 CatalogDetailsWarmupCache.clearPlaylist("collision")
 val after=CatalogDetailsWarmupCache.seriesOrLoad("collision","g","Aa"){result("Aa",1)}
 pass("lock liberado pode ser usado novamente apos limpar cache",after?.episodes?.size==1)

}
