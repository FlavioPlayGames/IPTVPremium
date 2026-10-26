package br.com.flaviotorres.iptvpremium.data.cache.importer
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import kotlinx.coroutines.*
import java.io.Reader
import java.lang.reflect.Proxy
import java.io.IOException

private class Db {
 val generations=linkedMapOf<String,String>("active" to "ACTIVE")
 val categories=linkedMapOf<String,CachedCategoryEntity>();val channels=linkedMapOf<String,CachedChannelEntity>()
 val movies=linkedMapOf<String,CachedMovieEntity>();val series=linkedMapOf<String,CachedSeriesEntity>();val episodes=linkedMapOf<String,CachedEpisodeEntity>()
 var transactions=0;var maxEntries=0;var seriesWrites=0;var fail=false;var beforeWrite:(()->Unit)?=null
 @Suppress("UNCHECKED_CAST")
 val dao=Proxy.newProxyInstance(PlaylistCacheDao::class.java.classLoader,arrayOf(PlaylistCacheDao::class.java)){_,m,raw ->
  val a=raw.orEmpty()
  when(m.name){
   "upsertGeneration" -> {val g=a[0] as PlaylistCacheGenerationEntity;generations[g.generationId]=g.status;Unit}
   "updateGenerationStatus" -> {generations[a[1] as String]=a[2] as String;Unit}
   "discardIncompleteImport" -> {val id=a[1] as String;check(generations[id]!="ACTIVE");generations.remove(id);categories.entries.removeIf{it.value.generationId==id};channels.entries.removeIf{it.value.generationId==id};movies.entries.removeIf{it.value.generationId==id};series.entries.removeIf{it.value.generationId==id};episodes.entries.removeIf{it.value.generationId==id};true}
   "insertImportBatch" -> {
    beforeWrite?.invoke()
    if(fail)throw IOException("simulated disk failure")
    transactions++;val cat=a[0] as List<CachedCategoryEntity>;val ch=a[1] as List<CachedChannelEntity>;val mo=a[2] as List<CachedMovieEntity>;val se=a[3] as List<CachedSeriesEntity>;val ep=a[4] as List<CachedEpisodeEntity>
    maxEntries=maxOf(maxEntries,ch.size+mo.size+ep.size);seriesWrites+=se.size
    cat.forEach{categories[it.generationId+it.type+it.categoryId]=it};ch.forEach{channels[it.generationId+it.channelId]=it};mo.forEach{movies[it.generationId+it.movieId]=it};se.forEach{series[it.generationId+it.seriesId]=it};ep.forEach{episodes[it.generationId+it.episodeId]=it};Unit
   }
   else -> error("Unexpected DAO ${m.name}")
  }
 } as PlaylistCacheDao
 fun canonical()=listOf(categories.values.map{it.copy(generationId="g")}.toSet(),channels.values.map{it.copy(generationId="g")}.toSet(),movies.values.map{it.copy(generationId="g")}.toSet(),series.values.map{it.copy(generationId="g")}.toSet(),episodes.values.map{it.copy(generationId="g")}.toSet())
}
private class Http(var lines:List<String>):PlaylistHttpClient {
 var perBlockDelayMs=0L;var failAt:Int?=null;var holdAt:Int?=null;val held=CompletableDeferred<Unit>();var calls=0
 override suspend fun get(url:String):String=error("M3U must stream")
 override suspend fun <T> withReader(url:String,consume:suspend(Reader)->T):T=error("M3U must read lines")
 override suspend fun readLines(url:String,onLine:suspend(String)->Unit){calls++;for((i,line)in lines.withIndex()){
  if(i>0 && i%200==0 && perBlockDelayMs>0)delay(perBlockDelayMs)
  if(i==failAt)throw IOException("simulated network failure")
  if(i==holdAt){held.complete(Unit);awaitCancellation()}
  onLine(line);if(i%500==0)yield()
 }}
}
private fun entry(name:String,group:String,url:String,poster:String="poster")=listOf("""#EXTINF:-1 tvg-id="epg" group-title="$group" tvg-logo="$poster",$name""",url)
private suspend fun parity(lines:List<String>,batch:Int?=null,chars:Int=524288):Pair<Db,Db>{
 val before=Db();val after=Db()
 val old=BaselineM3uCacheImporter(Http(lines),before.dao,PlaylistCacheGenerationManager(before.dao),batch?:500,chars)
 val new=if(batch==null)M3uCacheImporter(Http(lines),after.dao,PlaylistCacheGenerationManager(after.dao),batchCharacters=chars)else M3uCacheImporter(Http(lines),after.dao,PlaylistCacheGenerationManager(after.dao),batch,chars)
 old.import("p","http://provider.test/list");new.import("p","http://provider.test/list")
 check(before.canonical()==after.canonical()){"M3U final fields differ"}
 check(after.generations.values.count{it=="READY"}==1 && after.generations["active"]=="ACTIVE")
 return before to after
}
fun main()=runBlocking {
 var n=0
 suspend fun test(name:String,block:suspend()->Unit){block();n++;println("PASS $name")}
 test("mixed M3U preserves every final field, stable IDs, categories, episode grouping and order"){
  val lines=mutableListOf("\uFEFF#EXTM3U", "", "# comment")
  repeat(100){lines+=entry("Canal $it","Notícias","http://x/live/u/p/$it.ts")}
  repeat(1500){lines+=entry("Filme $it (2024)","Filmes","http://x/movie/u/p/$it.mp4")}
  repeat(30){series->repeat(120){ep->lines+=entry("Série $series - S01E${(ep+1).toString().padStart(3,'0')}","Séries","http://x/series/u/p/${series}_${ep}.mkv")}}
  val(old,new)=parity(lines);check(new.transactions<old.transactions && new.maxEntries<=2000)
  println("  fixture transactions ${old.transactions} -> ${new.transactions}; series writes ${old.seriesWrites} -> ${new.seriesWrites} (counts, not TV timings)")
 }
 test("same series across transaction boundaries avoids identical upserts"){
  val lines=listOf("#EXTM3U")+(1..30).flatMap{entry("Show S01E${it.toString().padStart(3,'0')}","Séries","http://x/series/$it")}
  val(old,new)=parity(lines,batch=2);check(old.seriesWrites==15 && new.seriesWrites==1)
 }
 test("changing and empty posters keep last-occurrence metadata across batches"){
  val lines=listOf("#EXTM3U")+(1..20).flatMap{entry("Show S01E${it.toString().padStart(3,'0')}","Series","http://x/series/$it",when(it%3){0->"";1->"first";else->"second"})}
  val(_,new)=parity(lines,batch=2);check(new.series.values.single().posterUrl=="second")
 }
 test("interleaved repeated series and groups preserve first provider order and final metadata"){
  val lines=mutableListOf("#EXTM3U")
  repeat(8){i->lines+=entry("Show A S01E${i+1}","Series A","http://x/series/a$i",if(i%2==0)"a" else "b");lines+=entry("Show B S01E${i+1}","Series B","http://x/series/b$i")}
  parity(lines,batch=3)
 }
 test("title-based classification, missing attributes, Unicode, comments, BOM and duplicate URLs unchanged"){
  val lines=listOf("\uFEFF#EXTM3U","#EXTINF:-1,Sem categoria","http://x/unknown","#EXTINF:-1 group-title=\"Animação\",Coração (2020)","#comment","http://x/a", "#EXTINF:-1 group-title=\"Series\",Título Temporada 2 Episódio 15","http://x/z")+entry("SeriesName S01E02","Series","http://x/z")+entry("SeriesName S01E02","Series","http://x/z","last")
  parity(lines,batch=2)
 }
 test("character bound still flushes large metadata before record limit"){
  val lines=listOf("#EXTM3U")+(1..30).flatMap{entry("Movie $it","Filmes","http://x/movie/$it", "x".repeat(3000))}
  val(_,new)=parity(lines,chars=10000);check(new.transactions>1 && new.maxEntries<=4)
 }
 test("invalid list leaves active generation intact"){
  val db=Db();check(runCatching{M3uCacheImporter(Http(listOf("<html>error</html>")),db.dao,PlaylistCacheGenerationManager(db.dao)).import("p","http://x")}.isFailure)
  check(db.generations==mapOf("active" to "ACTIVE"))
 }
 test("transport failure discards committed partial generation"){
  val lines=listOf("#EXTM3U")+(1..100).flatMap{entry("Film $it","Filmes","http://x/movie/$it")}
  val db=Db();val http=Http(lines);http.failAt=80
  check(runCatching{M3uCacheImporter(http,db.dao,PlaylistCacheGenerationManager(db.dao),10).import("p","http://x")}.isFailure)
  check(db.movies.isEmpty() && db.generations==mapOf("active" to "ACTIVE"))
 }
 test("write failure is terminal and does not publish partial data"){
  val db=Db();db.fail=true
  check(runCatching{M3uCacheImporter(Http(listOf("#EXTM3U")+entry("F","Filmes","http://x/movie/1")),db.dao,PlaylistCacheGenerationManager(db.dao),1).import("p","http://x")}.isFailure)
  check(db.generations==mapOf("active" to "ACTIVE"))
 }
 test("cancellation during body consumption cleans staging and preserves active"){
  val db=Db();val http=Http(listOf("#EXTM3U")+(1..100).flatMap{entry("F$it","Filmes","http://x/movie/$it")});http.holdAt=80
  val job=launch{M3uCacheImporter(http,db.dao,PlaylistCacheGenerationManager(db.dao),10).import("p","http://x")};http.held.await();job.cancelAndJoin()
  check(db.movies.isEmpty() && db.generations==mapOf("active" to "ACTIVE"))
 }
 test("READY is withheld until queued writes commit") {
  val db=Db();val reached=java.util.concurrent.CountDownLatch(1);val release=java.util.concurrent.CountDownLatch(1)
  db.beforeWrite={reached.countDown();check(release.await(5,java.util.concurrent.TimeUnit.SECONDS))}
  val lines=listOf("#EXTM3U")+(1..30).flatMap{entry("Film $it","Filmes","http://x/movie/$it")}
  val job=launch {M3uCacheImporter(Http(lines),db.dao,PlaylistCacheGenerationManager(db.dao),10).import("p","http://x")}
  try {
   check(withContext(Dispatchers.IO){reached.await(3,java.util.concurrent.TimeUnit.SECONDS)})
   check(db.generations.values.none{it=="READY"})
  } finally {release.countDown()}
  job.join();check(db.generations.values.count{it=="READY"}==1 && db.movies.size==30)
 }
 test("bounded writer failure cancels producer rather than consuming complete playlist") {
  val db=Db();db.fail=true;var delivered=0
  val http=object:PlaylistHttpClient {
   override suspend fun get(url:String):String=error("stream only")
   override suspend fun readLines(url:String,onLine:suspend(String)->Unit) {
    onLine("#EXTM3U")
    repeat(10000) {i->for(line in entry("Film $i","Filmes","http://x/movie/$i")){delivered++;onLine(line)}}
   }
  }
  check(runCatching{M3uCacheImporter(http,db.dao,PlaylistCacheGenerationManager(db.dao),10).import("p","http://x")}.isFailure)
  check(delivered<20000);check(db.generations==mapOf("active" to "ACTIVE"))
 }
 test("read and write stages overlap with a bounded queue (simulated latency, not TV timing)") {
  val lines=listOf("#EXTM3U")+(1..1000).flatMap{entry("Film $it","Filmes","http://x/movie/$it")}
  val oldDb=Db();oldDb.beforeWrite={Thread.sleep(40)}
  val newDb=Db();newDb.beforeWrite={Thread.sleep(40)}
  val a=Http(lines).apply{perBlockDelayMs=40};val b=Http(lines).apply{perBlockDelayMs=40}
  val start=System.nanoTime();BaselineM3uCacheImporter(a,oldDb.dao,PlaylistCacheGenerationManager(oldDb.dao),100).import("p","http://x");val oldMs=(System.nanoTime()-start)/1_000_000
  val next=System.nanoTime();M3uCacheImporter(b,newDb.dao,PlaylistCacheGenerationManager(newDb.dao),100).import("p","http://x");val newMs=(System.nanoTime()-next)/1_000_000
  check(oldDb.canonical()==newDb.canonical())
  println("  synthetic sequentialMs=$oldMs pipelineMs=$newMs; same 10 writes and same records, simulated 40ms read/write blocks")
 }
 test("XUI entries without extensions distinguish films, EPG channels and grouped episodes") {
  val lines=listOf("#EXTM3U",
   "#EXTINF:-1 xui-id=\"1\" group-title=\"Ação\",Filme A","http://x/play?id=1",
   "#EXTINF:-1 xui-id=\"2\" group-title=\"UHD 4K\",Filme B","http://x/play?id=2",
   "#EXTINF:-1 xui-id=\"3\" group-title=\"Curtas\",Filme C","http://x/play?id=3",
   "#EXTINF:-1 xui-id=\"4\" tvg-id=\"epg\" group-title=\"Filmes e Séries\",Canal A","http://x/play?id=4",
   "#EXTINF:-1 xui-id=\"5\" tvg-id=\"\" group-title=\"Seriados 24H\",Canal B","http://x/play?id=5",
   "#EXTINF:-1 xui-id=\"6\" group-title=\"Plataforma\",Show S01E01","http://x/play?id=6",
   "#EXTINF:-1 xui-id=\"7\" group-title=\"Plataforma\",Show S01E02","http://x/play?id=7")
  val db=Db();M3uCacheImporter(Http(lines),db.dao,PlaylistCacheGenerationManager(db.dao),2).import("p","http://x")
  check(db.movies.size==3 && db.channels.size==2 && db.episodes.size==2 && db.series.size==1)
  check(db.movies.values.map{it.name}.toSet()==setOf("Filme A","Filme B","Filme C"))
  check(db.channels.values.map{it.name}.toSet()==setOf("Canal A","Canal B"))
  check(db.series.values.single().name=="Show")
  check(db.episodes.values.map{it.episodeNumber}.toSet()==setOf(1,2))
  check(db.generations.values.count{it=="READY"}==1)
 }
 check(android.util.Log.messages.any{it.contains("fase=GRAVACAO")})
 check(android.util.Log.messages.none{it.contains("http://provider.test")})
 println("$n M3U importer parity/failure/cancellation scenarios; DAO persistence simulated")
}
