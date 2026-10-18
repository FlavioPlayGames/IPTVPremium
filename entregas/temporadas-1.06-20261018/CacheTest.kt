package br.com.flaviotorres.iptvpremium.data.cache
import br.com.flaviotorres.iptvpremium.data.repository.*
import kotlinx.coroutines.*
fun main() = runBlocking {
    fun checkPass(name: String, condition: Boolean) { check(condition) { name }; println("PASS $name") }
    fun result(id: String, count: Int) = SeriesEpisodesResult(id,null,null,null,null,null,
        if(count>0) listOf(SeriesSeasonInfo(1,"Temporada 1",null,count)) else emptyList(),
        (1..count).map { SeriesEpisodeInfo("e$it",1,it,"Episodio $it",null,null,null,null) })
    var requests=0
    val cold=CatalogDetailsWarmupCache.seriesOrLoad("cold","g","123") { requests++;result("123",2) }
    checkPass("atalho com cache frio carrega temporadas e episodios",cold?.seasons?.size==1 && cold.episodes.size==2 && requests==1)
    val warm=CatalogDetailsWarmupCache.seriesOrLoad("cold","g","123") { error("rede desnecessaria") }
    checkPass("retorno reutiliza temporadas completas",warm==cold)
    CatalogDetailsWarmupCache.seriesOrLoad("empty","g","1") { result("1",0) }
    val recovered=CatalogDetailsWarmupCache.seriesOrLoad("empty","g","1") { result("1",3) }
    checkPass("resposta vazia nao bloqueia consulta posterior",recovered?.episodes?.size==3)
    val different=CatalogDetailsWarmupCache.seriesOrLoad("other","g","123") { result("123",4) }
    checkPass("mesmo ID em outra lista usa episodios da lista destino",different?.episodes?.size==4)
    val generation=CatalogDetailsWarmupCache.seriesOrLoad("cold","g2","123") { result("123",5) }
    checkPass("nova geracao nao reutiliza episodios antigos",generation?.episodes?.size==5)
    var parallelRequests=0
    val both=(1..2).map { async { CatalogDetailsWarmupCache.seriesOrLoad("parallel","g","9") { parallelRequests++;delay(50);result("9",1) } } }.awaitAll()
    checkPass("aberturas simultaneas compartilham consulta",parallelRequests==1 && both.all { it?.episodes?.size==1 })
    try { CatalogDetailsWarmupCache.seriesOrLoad("failure","g","1") { throw CancellationException("saiu") } } catch (_:CancellationException) {}
    val retry=CatalogDetailsWarmupCache.seriesOrLoad("failure","g","1") { result("1",1) }
    checkPass("saida durante carregamento permite reabrir",retry?.episodes?.size==1)
    CatalogDetailsWarmupCache.clearPlaylist("cold")
    checkPass("remocao de lista libera seus dados sem afetar outra",CatalogDetailsWarmupCache.getSeries("cold","g","123")==null && CatalogDetailsWarmupCache.getSeries("other","g","123")!=null)
}
