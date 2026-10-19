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
 checkCase("serie sem episodio fica em favoritos sem continuar inventado") {
  check(history.all("B").none{it.seriesId=="bs1"})
  check("bs1" in store.favorites("B","series",emptySet()))
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
 checkCase("episodios duplicados ficam apenas em favoritos") {check(history.all("B").none{it.seriesId=="bs1"});check("bs1" in store.favorites("B","series",emptySet()))}
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
 checkCase("nome alternativo ingles exige ano ou identificador confirmado") {
  val translated=ContentIdentity("series","Digimon Savers",2006,alternateTitles=listOf("Digimon Data Squad"))
  check(translated.confidence(ContentIdentity("series","Digimon Data Squad 4K",2006))==1)
  check(translated.confidence(ContentIdentity("series","Digimon Data Squad",2020))==0)
  check(translated.confidence(ContentIdentity("series","Digimon Data Squad"))==0)
  check(translated.copy(tmdb="1").confidence(ContentIdentity("series","Digimon Data Squad",2006,tmdb="2"))==0)
 }
 dao.movies["AliasA"]=emptyList();dao.movies["AliasB"]=emptyList()
 dao.series["AliasA"]=listOf(row("savers","Digimon Savers",2006))
 dao.series["AliasB"]=listOf(row("squadL","Digimon Data Squad [L]",2006,order=0),row("squad","Digimon Data Squad 4K",2006,order=1))
 store.setFavorite(dao,"AliasA","series","savers",true)
 history.save("AliasA",PlaybackMedia("ep-digi",PlaybackContentType.EPISODE,"Episodio",seriesId="savers",seasonNumber=1,episodeNumber=2,durationMs=600000),120000,600000,"Digimon Savers")
 store.rememberExternal("AliasA","savers",ContentIdentity("series","Digimon Savers",2006,tmdb="999001",alternateTitles=listOf("Digimon Data Squad")))
 store.reconcile(dao,"AliasB",history.allLocal(),force=true)
 checkCase("serie com titulo ingles compartilha favorito na primeira variante sem L") {check(store.favorites("AliasB","series",emptySet())==setOf("squad"))}
 checkCase("titulo alternativo conserva favorito sem inventar episodio") {
  check(history.all("AliasB").none { it.seriesId=="squad" })
 }
 val field=SharedLibraryStore::class.java.getDeclaredField("instance");field.isAccessible=true;field.set(null,null)
 store=SharedLibraryStore.get(Context(folder));store.reconcile(dao,"AliasB",history.allLocal(),force=true)
 checkCase("nomes alternativos persistem apos recriar a instancia do armazenamento") {check(store.favorites("AliasB","series",emptySet())==setOf("squad"))}
 store.setFavorite(dao,"AliasB","series","squad",false);store.reconcile(dao,"AliasA",history.allLocal(),force=true)
 checkCase("remocao propaga tambem entre nomes alternativos") {check(store.favorites("AliasA","series",setOf("savers")).isEmpty())}

 // Only a watched series, not an explicit favorite: missing target episode creates a favorite.
 dao.movies["ProgressA"]=emptyList();dao.movies["ProgressB"]=emptyList()
 dao.series["ProgressA"]=listOf(row("orig","Original español",2026,tmdb="71234"))
 dao.series["ProgressB"]=listOf(row("dest","Brazilian",2026,tmdb="71234"))
 history.save("ProgressA",PlaybackMedia("foreign-id",PlaybackContentType.EPISODE,"Episode",seriesId="orig",seasonNumber=2,episodeNumber=3,durationMs=600000),99000,600000,"Original español")
 store.reconcile(dao,"ProgressB",history.allLocal(),force=true)
 checkCase("progresso sem favorito explicito cria favorito quando falta episodio") {
  check("dest" in store.favorites("ProgressB","series",emptySet()))
  check(history.lastEpisode("ProgressB","dest")==null)
 }
 var loads=0
 store.importEpisodeLinks("ProgressB") { id -> loads++;check(id=="dest");listOf(SeriesEpisodeInfo("target-id",2,3,"Third")) }
 checkCase("importa episodio sem abrir detalhes e usa ID do provedor destino") {
  val h=history.lastEpisode("ProgressB","dest")!!
  check(h.contentId=="target-id" && h.positionMs==99000L && loads==1)
  check("dest" !in store.favorites("ProgressB","series",emptySet()))
 }
 store.importEpisodeLinks("ProgressB") { loads++;emptyList() }
 checkCase("episodio exato ja importado nao repete rede") {check(loads==1)}
 store.registerEpisodes("ProgressB","dest",listOf(SeriesEpisodeInfo("foreign-id",3,2,"Wrong coordinates")))
 store.importEpisodeLinks("ProgressB",refresh=true) { emptyList() }
 checkCase("ID igual sem temporada e numero iguais nao transfere progresso") {
  check(history.lastEpisode("ProgressB","dest")==null && "dest" in store.favorites("ProgressB","series",emptySet()))
 }
 store.setFavorite(dao,"ProgressB","series","dest",false)
 store.reconcile(dao,"ProgressB",history.allLocal(),force=true)
 checkCase("favorito de recuperacao respeita desfavoritar explicito") {check("dest" !in store.favorites("ProgressB","series",emptySet()))}
 // Updated progress must be projected without re-scanning a preloaded catalogue.
 history.save("ProgressA",PlaybackMedia("foreign-id",PlaybackContentType.EPISODE,"Episode",seriesId="orig",seasonNumber=2,episodeNumber=3,durationMs=600000),199000,600000,"Original español")
 val noScanPages=dao.pages
 store.reconcile(dao,"ProgressB",history.allLocal())
 store.registerEpisodes("ProgressB","dest",listOf(SeriesEpisodeInfo("target-id",2,3,"Third")))
 checkCase("atualizacao silenciosa usa historico novo sem varrer catalogo de novo") {
  check(dao.pages==noScanPages && history.lastEpisode("ProgressB","dest")?.positionMs==199000L)
 }
 // More than four unmatched favorites must all receive official aliases, with at most two requests.
 dao.movies["NamesA"]=emptyList();dao.movies["NamesB"]=emptyList()
 dao.series["NamesA"]=(1..8).map{row("source$it","Original $it",2026)}
 dao.series["NamesB"]=(1..8).map{row("target$it","Español $it",2026)}
 for(i in 1..8)store.setFavorite(dao,"NamesA","series","source$i",true)
 store.reconcile(dao,"NamesB",history.allLocal(),force=true)
 var concurrent=0;var peak=0;var resolvedCount=0
 store.enrichAlternateTitles("NamesB") { identity ->
  if(identity.title.matches(Regex("Original [0-9]+"))) {
   concurrent++;peak=maxOf(peak,concurrent);delay(10);concurrent--;resolvedCount++
   identity.copy(alternateTitles=listOf(identity.title,"English "+identity.title.substringAfterLast(' '),"Español "+identity.title.substringAfterLast(' ')))
  } else null
 }
 store.reconcile(dao,"NamesB",history.allLocal())
 checkCase("todos favoritos aprendem aliases sem limite antigo de quatro itens") {
  check(resolvedCount==8 && peak<=2 && store.favorites("NamesB","series",emptySet()).size==8)
 }
 var repeatedAliases=0
 store.enrichAlternateTitles("NamesB") { repeatedAliases++;null }
 checkCase("aliases oficiais persistidos nao repetem consulta a cada troca") {check(repeatedAliases==0)}
 // Cross-title series with missing target year can be resolved safely before linking.
 dao.movies["NamesC"]=emptyList();dao.series["NamesC"]=listOf(row("missing-year","Español 1"))
 store.reconcile(dao,"NamesC",history.allLocal(),force=true)
 check("missing-year" !in store.favorites("NamesC","series",emptySet()))
 store.enrichMatchingSeries("NamesC") { it.copy(year=2026,alternateTitles=listOf("Original 1","Español 1")) }
 store.reconcile(dao,"NamesC",history.allLocal())
 checkCase("serie de nome espanhol sem ano enriquece antes de associar") {check("missing-year" in store.favorites("NamesC","series",emptySet()))}

 val batchFolder=Files.createTempDirectory("shared-batch").toFile()
 field.set(null,null)
 val batchCtx=Context(batchFolder);val batchStore=SharedLibraryStore.get(batchCtx);val batchHistory=WatchHistoryStore(batchCtx);val batchDao=PlaylistCacheDao()
 batchDao.movies["batchA"]=emptyList();batchDao.movies["batchB"]=emptyList()
 batchDao.series["batchA"]=(1..8).map{row("s$it","Source $it",2026,tmdb=(8400+it).toString())}
 batchDao.series["batchB"]=(1..8).map{row("d$it","Destino $it",2026,tmdb=(8400+it).toString())}
 for(i in 1..8)batchHistory.save("batchA",PlaybackMedia("ep$i",PlaybackContentType.EPISODE,"Episodio",seriesId="s$i",seasonNumber=1,episodeNumber=1,durationMs=600000),15000L+i,600000,"Source $i")
 batchStore.reconcile(batchDao,"batchB",batchHistory.allLocal(),force=true)
 var episodeConcurrent=0;var episodePeak=0;var episodeRequests=0
 batchStore.importEpisodeLinks("batchB",refresh=true) { id ->
  episodeConcurrent++;episodePeak=maxOf(episodePeak,episodeConcurrent);episodeRequests++;delay(10);episodeConcurrent--
  if(id=="d8") error("transient provider failure")
  listOf(SeriesEpisodeInfo("real-$id",1,1,"Episode"))
 }
 checkCase("importa todas as series em pares e uma falha nao bloqueia as demais") {
  check(episodeRequests==8 && episodePeak==2 && batchHistory.all("batchB").size==7)
  check(batchStore.favorites("batchB","series",emptySet())==setOf("d8"))
 }
 batchHistory.save("batchA",PlaybackMedia("newest",PlaybackContentType.EPISODE,"Latest",seriesId="s1",seasonNumber=2,episodeNumber=4,durationMs=600000),35000,600000,"Source 1")
 batchStore.reconcile(batchDao,"batchB",batchHistory.allLocal(),force=true)
 checkCase("episodio anterior disponivel nao substitui ultimo episodio ausente") {
  check(batchHistory.lastEpisode("batchB","d1")==null && "d1" in batchStore.favorites("batchB","series",emptySet()))
 }
 batchHistory.save("batchA",PlaybackMedia("unknown-coordinates",PlaybackContentType.EPISODE,"Unknown",seriesId="s2",durationMs=600000),45000,600000,"Source 2")
 batchStore.reconcile(batchDao,"batchB",batchHistory.allLocal())
 val attemptedIds=mutableListOf<String>()
 batchStore.importEpisodeLinks("batchB",refresh=true) { id -> attemptedIds+=id;emptyList() }
 checkCase("sem coordenadas nao busca episodio inventado e deixa apenas favorito") {
  check("d2" !in attemptedIds && batchHistory.lastEpisode("batchB","d2")==null && "d2" in batchStore.favorites("batchB","series",emptySet()))
 }
 batchStore.setFavorite(batchDao,"batchA","series","s3",true)
 batchDao.movies.remove("batchA");batchDao.series.remove("batchA");batchDao.movies.remove("batchB");batchDao.series.remove("batchB")
 batchDao.movies["newC"]=emptyList();batchDao.series["newC"]=(1..8).map{row("new$it","Other name $it",2026,tmdb=(8400+it).toString())}
 batchStore.reconcile(batchDao,"newC",batchHistory.allLocal(),force=true)
 checkCase("nova lista aproveita favoritos e historico mesmo apos excluir catalogos antigos") {
  check("new3" in batchStore.favorites("newC","series",emptySet()))
 }

 batchDao.movies["newC"]=listOf(row("local-only","No year"))
 batchStore.setFavorite(batchDao,"newC","movies","local-only",true)
 batchStore.reconcile(batchDao,"newC",batchHistory.allLocal(),force=true)
 var unnecessaryRequests=0
 batchStore.enrichMatchingMovies("newC") { unnecessaryRequests++;null }
 checkCase("favorito apenas local nao gera consulta desnecessaria ao provedor") {check(unnecessaryRequests==0)}
 val revision=batchStore.changes.value
 val newEpisodes=listOf(SeriesEpisodeInfo("new-source-id",1,1,"First"))
 batchStore.registerEpisodes("newC","new3",newEpisodes)
 val updatedRevision=batchStore.changes.value
 batchStore.registerEpisodes("newC","new3",newEpisodes)
 checkCase("episodios descobertos depois notificam interface sem criar loop de atualizacao") {check(updatedRevision==revision+1 && batchStore.changes.value==updatedRevision)}

 batchStore.registerEpisodes("newC","new3",(1..3000).map{SeriesEpisodeInfo("long-$it",1,it,"Episode $it")})
 val maps=SharedLibraryStore::class.java.getDeclaredField("episodeMaps").apply{isAccessible=true}.get(batchStore) as Map<*,*>
 checkCase("serie longa conserva apenas coordenadas necessarias ao historico na RAM") {check((maps["new3"] as Map<*,*>).size==1)}

 delay(5)
 batchHistory.save("newC",PlaybackMedia("own-recent",PlaybackContentType.EPISODE,"Own recent",seriesId="new8",seasonNumber=3,episodeNumber=9,durationMs=600000),65000,600000,"Other name 8")
 batchStore.reconcile(batchDao,"newC",batchHistory.allLocal())
 checkCase("progresso local mais recente impede favorito de recuperacao por episodio antigo de outra lista") {
  check("new8" !in batchStore.favorites("newC","series",emptySet()))
  check(batchHistory.lastEpisode("newC","new8")?.contentId=="own-recent")
 }

 batchDao.movies["id-only"]=emptyList();batchDao.series["id-only"]=listOf(row("id-only-target","No translated year",tmdb="8403"))
 batchStore.reconcile(batchDao,"id-only",batchHistory.allLocal(),force=true)
 var redundantMetadata=0
 batchStore.enrichMatchingSeries("id-only") { redundantMetadata++;null }
 checkCase("serie ja confirmada por ID dispensa busca adicional mesmo sem ano no destino") {
  check(redundantMetadata==0 && "id-only-target" in batchStore.favorites("id-only","series",emptySet()))
 }
 val perfFolder=Files.createTempDirectory("startup-history").toFile()
 SharedLibraryStore::class.java.getDeclaredField("instance").apply{isAccessible=true}.set(null,null)
 val perfStore=SharedLibraryStore.get(Context(perfFolder));val perfDao=PlaylistCacheDao()
 perfDao.movies["perf"]=emptyList();perfDao.series["perf"]=listOf(row("s1","Serie performance",2020))
 val template=batchHistory.allLocal().first{it.contentType==PlaybackContentType.EPISODE}
 val many=(1..500).map{template.copy(playlistId="perf",seriesId="s1",seriesTitle="Serie performance",contentId="ep-$it",updatedAtMs=it.toLong())}
 val mixed=listOf(template.copy(playlistId="perf",contentType=PlaybackContentType.LIVE_CHANNEL,seriesId=null,contentId="s1"))+many
 perfStore.reconcile(perfDao,"perf",mixed,force=true,reuseUnchanged=true)
 checkCase("quinhentos episodios da mesma serie fazem uma unica consulta de identidade") {check(perfDao.recordLookups==1)}
 val pagesBefore=perfDao.pages
 perfStore.reconcile(perfDao,"perf",mixed.map{it.copy(positionMs=77000)},force=true,reuseUnchanged=true)
 checkCase("troca forcada com geracao e identidades iguais nao revarre catalogo") {check(perfDao.pages==pagesBefore)}
 val savedHistory=SharedLibraryStore::class.java.getDeclaredField("linkedHistory").apply{isAccessible=true}.get(perfStore) as List<*>
 checkCase("deduplicacao de identidades preserva todos os eventos de progresso") {check(savedHistory.size==501 && (savedHistory.last() as WatchHistoryEntry).positionMs==77000L)}
 perfDao.generation="g2";perfStore.reconcile(perfDao,"perf",mixed,force=true,reuseUnchanged=true)
 checkCase("nova geracao invalida reaproveitamento de vinculos e refaz comparacao") {check(perfDao.pages>pagesBefore)}
 perfFolder.deleteRecursively()
 batchFolder.deleteRecursively()
 folder.deleteRecursively()
 Unit
}
