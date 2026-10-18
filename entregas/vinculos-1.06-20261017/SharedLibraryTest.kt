package br.com.flaviotorres.iptvpremium.data.history
import android.content.Context
import br.com.flaviotorres.iptvpremium.core.player.*
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeInfo
import kotlinx.coroutines.*
import java.nio.file.Files

fun row(id:String,title:String,year:Int?=null,imdb:String?=null,tmdb:String?=null,order:Int?=null)=SharedCatalogIdentity(id,title,year,imdb,tmdb,null,order)
fun checkCase(name:String,body:()->Unit){body();println("PASS $name")}
fun main()= runBlocking {
 checkCase("identificador externo liga nomes diferentes") {check(ContentIdentity("movies","Alien",1979,tmdb="348").confidence(ContentIdentity("movies","O Oitavo Passageiro",1979,tmdb="348"))==2)}
 checkCase("IMDb valido e conflito entre identificadores") {
  check(ContentIdentity("series","A",imdb="tt1234567").confidence(ContentIdentity("series","B",imdb="tt1234567"))==2)
  check(ContentIdentity("movies","Alien",1979,tmdb="348").confidence(ContentIdentity("movies","Alien",1979,tmdb="999"))==0)
  check(ContentIdentity("movies","A",imdb="tt1234567",tmdb="1").confidence(ContentIdentity("movies","B",imdb="tt1234567",tmdb="2"))==0)
 }
 checkCase("fallback nome ano normalizado sem confundir remakes") {
  check(ContentIdentity("movies","Ação (2020) [DV] 4K").confidence(ContentIdentity("movies","Acao",2020))==1)
  check(ContentIdentity("movies","Acao",2020).confidence(ContentIdentity("movies","Acao",2010))==0)
  check(ContentIdentity("movies","Acao").confidence(ContentIdentity("movies","Acao"))==0)
  check(ContentIdentity("movies","Acao",2020).confidence(ContentIdentity("series","Acao",2020))==0)
 }
 checkCase("numero integrante do titulo nao e removido como ano diferente") {
  check(ContentIdentity("movies","Blade Runner 2049",2017).normalizedTitle == "blade runner 2049")
 }
 val folder=Files.createTempDirectory("shared-library").toFile();var ctx=Context(folder)
 val dao=PlaylistCacheDao()
 dao.movies["A"]=listOf(row("m1","Filme Original",2020),row("m2","Outro Filme",2021))
 dao.series["A"]=listOf(row("s1","Serie Original",2019,tmdb="55"))
 dao.movies["B"]=listOf(row("b1","Nome Traduzido",2020),row("b2","Outro Filme [L]",2021))
 dao.series["B"]=listOf(row("bs1","Serie Traduzida",2019,tmdb="55"))
 ctx.getSharedPreferences("iptvpremium_favorites_v1",0).edit().putStringSet("movies_A",setOf("m1","m2")).putStringSet("series_A",setOf("s1")).apply()
 var store=SharedLibraryStore.get(ctx)
 val history=WatchHistoryStore(ctx)
 history.save("A",PlaybackMedia("m1",PlaybackContentType.MOVIE,"Filme Original",durationMs=600000),90000,600000)
 history.save("A",PlaybackMedia("e12",PlaybackContentType.EPISODE,"Episodio 2",durationMs=1200000,seriesId="s1",seasonNumber=1,episodeNumber=2),125000,1200000,"Serie Original")
 store.rememberExternal("A","m1",ContentIdentity("movies","Filme Original",2020,tmdb="123"))
 store.rememberExternal("B","b1",ContentIdentity("movies","Nome Traduzido",2020,tmdb="123"))
 store.reconcile(dao,"B",history.allLocal())
 checkCase("favoritos legados aparecem na outra lista por ID e por nome ano") {
  check(store.favorites("B","movies",emptySet())==setOf("b1","b2"))
  check(store.favorites("B","series",emptySet())==setOf("bs1"))
 }
 checkCase("filme compartilha posicao com ID real da lista destino") {val h=history.movie("B","b1")!!;check(h.positionMs==90000L && h.sharedFromPlaylist=="A")}
 checkCase("serie sem episodio gera atalho parcial sem retomar episodio inventado") {
  val h=history.all("B").first{it.seriesId=="bs1"};check(h.seriesOnly && h.inProgress && h.positionMs==0L && h.seasonNumber==null)
  check(history.lastEpisode("B","bs1")==null)
 }
 checkCase("duracao incompativel nao aplica posicao em outra edicao do filme") {
  val h=history.movie("B","b1")!!
  check(SharedLibraryStore.resumePosition(h,h.positionMs,600000)==90000L)
  check(SharedLibraryStore.resumePosition(h,h.positionMs,900000)==0L)
  check(SharedLibraryStore.resumePosition(h,h.positionMs,80000)==0L)
  check(SharedLibraryStore.resumePosition(h,h.positionMs,null)==90000L)
 }
 store.registerEpisodes("B","bs1",listOf(SeriesEpisodeInfo("ep-destino",1,2,"Episodio destino")))
 checkCase("temporada e episodio exatos recuperam posicao") {val h=history.lastEpisode("B","bs1")!!;check(h.contentId=="ep-destino" && h.positionMs==125000L && !h.seriesOnly)}
 store.registerEpisodes("B","bs1",listOf(SeriesEpisodeInfo("e12",2,1,"Outro")))
 checkCase("ID de episodio igual entre provedores nao substitui temporada e numero") {check(history.lastEpisode("B","bs1")==null)}
 store.registerEpisodes("B","bs1",listOf(SeriesEpisodeInfo("x",1,2,"A"),SeriesEpisodeInfo("y",1,2,"B")))
 checkCase("episodios duplicados ficam no atalho da serie") {check(history.all("B").first{it.seriesId=="bs1"}.seriesOnly)}
 store.setFavorite(dao,"B","series","bs1",false)
 store.reconcile(dao,"A",history.allLocal())
 checkCase("desfavoritar em uma lista remove na outra") {check("s1" !in store.favorites("A","series",setOf("s1")))}
 store.reconcile(dao,"B",history.allLocal())
 history.remove("B",PlaybackContentType.MOVIE,"b1")
 store.reconcile(dao,"A",history.allLocal())
 checkCase("remover continuar evita reaparecimento do mesmo progresso compartilhado") {check(history.movie("A","m1")==null)}
 delay(5)
 history.save("A",PlaybackMedia("m1",PlaybackContentType.MOVIE,"Filme Original",durationMs=600000),180000,600000)
 store.reconcile(dao,"B",history.allLocal())
 checkCase("nova reproducao apos remover volta com progresso novo") {check(history.movie("B","b1")?.positionMs==180000L)}
 dao.movies["C"]=listOf(row("c1","Outro Filme",2021,tmdb="111"),row("c2","Outro Filme 4K",2021,tmdb="222"))
 dao.series["C"]=emptyList()
 store.reconcile(dao,"C",history.allLocal())
 checkCase("fallback ambiguo nao marca dois filmes diferentes") {check(store.favorites("C","movies",emptySet()).isEmpty())}
 dao.movies["D"]=listOf(row("d1","Outro Filme",2021));dao.series["D"]=emptyList()
 store.reconcile(dao,"D",history.allLocal())
 checkCase("lista adicionada depois recebe favorito existente") {check(store.favorites("D","movies",emptySet())==setOf("d1"))}
 // Simulate process death: only file-backed preference records remain, maps/QR/network state do not.
 SharedLibraryStore::class.java.getDeclaredField("instance").apply{isAccessible=true}.set(null,null)
 ctx=Context(folder);store=SharedLibraryStore.get(ctx);store.reconcile(dao,"A",history.allLocal())
 checkCase("reinicio preserva remocao de favorito e dados antigos") {check("s1" !in store.favorites("A","series",setOf("s1")));check(history.allLocal().size==2)}
 history.clearType(PlaybackContentType.MOVIE)
 store.reconcile(dao,"B",history.allLocal())
 checkCase("limpar historico nao recupera progresso excluido e preserva favoritos") {check(history.movie("B","b1")==null);check("b1" in store.favorites("B","movies",emptySet()))}
 // Large catalogue traversed only via 256-item metadata projections, no URLs or eager episode fetch.
 dao.movies["E"]=(0 until 10000).map{row("id%05d".format(it),"Filme $it",2000)}+row("z1","Outro Filme",2021)
 dao.series["E"]=emptyList();store.reconcile(dao,"E",history.allLocal())
 checkCase("catalogo grande usa lotes limitados") {check(dao.maxBatch==256 && "z1" in store.favorites("E","movies",emptySet()))}
 ctx.failCommit=true
 val failed=runCatching { store.setFavorite(dao,"E","movies","z1",false) }.isFailure
 ctx.failCommit=false
 checkCase("falha de gravacao nao confirma nem substitui vinculos persistidos") { check(failed && "z1" in store.favorites("E","movies",emptySet())) }
 val pages=dao.pages;store.reconcile(dao,"E",history.allLocal())
 checkCase("reconciliacao sem mudanca nao repete varredura") {check(dao.pages==pages)}
 checkCase("preferencias de vinculos nao guardam enderecos de video ou credenciais") {val text=ctx.getSharedPreferences("iptvpremium_shared_library_v1",0).getString("identities","")!!;check("streamUrl" !in text && "password" !in text && "token" !in text)}

 dao.movies["H"]=listOf(row("h1","Homem-Aranha 4K HDR"));dao.series["H"]=emptyList()
 dao.movies["I"]=listOf(row("iL","Homem-Aranha 4K [L]",order=0),row("iD","Homem-Aranha 4K",order=4),row("iD2","Homem-Aranha 4K HDR",order=9));dao.series["I"]=emptyList()
 ctx.getSharedPreferences("iptvpremium_favorites_v1",0).edit().putStringSet("movies_H",setOf("h1")).apply()
 history.save("H",PlaybackMedia("h1",PlaybackContentType.MOVIE,"Homem-Aranha 4K HDR",durationMs=600000),65000,600000)
 store.reconcile(dao,"I",history.allLocal(),force=true)
 checkCase("nota ou nome sem ano nao sao tratados como IMDb inventado") {check(store.favorites("I","movies",emptySet()).isEmpty())}
 val fetched=mutableListOf<String>()
 val enriched=store.enrichMatchingMovies("I") { request ->
  fetched.add(request.playlist+":"+request.id)
  ContentIdentity("movies",request.title,2002,"tt0145487","557")
 }
 store.reconcile(dao,"I",history.allLocal(),force=true)
 checkCase("Homem Aranha HDR e 4K vinculados apos obter IDs reais dos candidatos") {check(enriched && "H:h1" in fetched && "I:iD" in fetched)}
 checkCase("entre variantes escolhe primeira sem L na ordem do provedor") {check(store.favorites("I","movies",emptySet())==setOf("iD"));check(history.movie("I","iD")?.positionMs==65000L);check(history.movie("I","iL")==null)}
 checkCase("marcadores L e Legendado reconhecidos sem confundir titulos comuns") {check(ContentIdentity.isSubtitled("Homem-Aranha [L]"));check(ContentIdentity.isSubtitled("Homem Aranha (Legendado)"));check(!ContentIdentity.isSubtitled("Lorne 4K"))}
 history.save("H",PlaybackMedia("h1",PlaybackContentType.MOVIE,"Homem-Aranha 4K HDR",durationMs=600000),155000,600000)
 val previousPages=dao.pages;store.reconcile(dao,"I",history.allLocal(),force=true)
 checkCase("troca de lista pre carregada refaz reconciliacao e usa progresso novo") {check(dao.pages>previousPages);check(history.movie("I","iD")?.positionMs==155000L)}
 store.setFavorite(dao,"I","movies","iD",false);store.reconcile(dao,"H",history.allLocal(),force=true)
 checkCase("desfavoritar variante escolhida propaga ao retornar para origem") {check("h1" !in store.favorites("H","movies",setOf("h1")))}
 store.reconcile(dao,"I",history.allLocal(),force=true)
 var retries=0;store.enrichMatchingMovies("I") { retries++;null }
 checkCase("detalhes resolvidos nao geram consultas repetidas") {check(retries==0)}

 dao.movies["J"]=listOf(row("jL","Homem-Aranha [L]",2002,tmdb="557",order=0),row("jD","Homem-Aranha 4K HDR",2002,order=1));dao.series["J"]=emptyList()
 store.reconcile(dao,"J",history.allLocal(),force=true)
 checkCase("prefere nao legendado confirmado por nome ano mesmo com ID na variante L") {check(history.movie("J","jD")?.positionMs==155000L && history.movie("J","jL")==null)}
 checkCase("se todas forem legendadas mantem a primeira disponivel") {check(ContentIdentity.preferredVariantIndex(listOf("Filme [L]" to 9,"Filme Legendado" to 2))==1)}
 dao.movies["P"]=listOf(row("p1","Filme Novo"));dao.series["P"]=emptyList()
 dao.movies["Q"]=listOf(row("q1","Filme Novo",2020));dao.series["Q"]=emptyList()
 ctx.getSharedPreferences("iptvpremium_favorites_v1",0).edit().putStringSet("movies_P",setOf("p1")).apply()
 store.reconcile(dao,"Q",history.allLocal(),force=true)
 check(store.favorites("Q","movies",emptySet()).isEmpty())
 dao.movies["P"]=listOf(row("p1","Filme Novo",2020))
 store.reconcile(dao,"Q",history.allLocal(),force=true)
 checkCase("identidade incompleta e atualizada quando o cache local ganha o ano") {check(store.favorites("Q","movies",emptySet())==setOf("q1"))}
 folder.deleteRecursively()
 Unit
}
