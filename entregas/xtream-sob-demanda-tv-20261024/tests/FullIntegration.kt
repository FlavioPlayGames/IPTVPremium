package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.data.cache.importer.M3uCacheImporter
import br.com.flaviotorres.iptvpremium.data.parser.*
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.repository.DefaultPlaylistRepository
import br.com.flaviotorres.iptvpremium.domain.model.*
import java.io.*
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*

private class Store {
    val states = ConcurrentHashMap<String,String>()
    val movies = ConcurrentHashMap<String,LinkedHashMap<String,CachedMovieEntity>>()
    val series = ConcurrentHashMap<String,LinkedHashMap<String,CachedSeriesEntity>>()
    val channels = ConcurrentHashMap<String,LinkedHashMap<String,CachedChannelEntity>>()
    val categories = ConcurrentHashMap<String,LinkedHashMap<String,CachedCategoryEntity>>()
    var failWrites = false
    var failSeriesMerge = false
    var maxBatch = 0
    var maxStages = 0
    val wrote = CompletableDeferred<Unit>()
    @Suppress("UNCHECKED_CAST")
    val dao = Proxy.newProxyInstance(PlaylistCacheDao::class.java.classLoader, arrayOf(PlaylistCacheDao::class.java)) { _, method, raw ->
        val a = raw.orEmpty()
        synchronized(this) {
            when (method.name) {
                "upsertGeneration" -> { val g=a[0] as PlaylistCacheGenerationEntity;states[g.generationId]=g.status;maxStages=maxOf(maxStages,states.values.count { it=="IMPORTING" });Unit }
                "updateGenerationStatus" -> {states[a[1] as String]=a[2] as String;Unit}
                "discardIncompleteImport" -> { val id=a[1] as String;check(states[id]!="ACTIVE");states.remove(id);movies.remove(id);series.remove(id);channels.remove(id);categories.remove(id);true }
                "insertMovies","insertMoviesFirst" -> {
                    if(failWrites) error("simulated write failure")
                    val rows=a[0] as List<CachedMovieEntity>;maxBatch=maxOf(maxBatch,rows.size)
                    rows.forEach { row -> val map=movies.getOrPut(row.generationId){linkedMapOf()};if(method.name.endsWith("First"))map.putIfAbsent(row.movieId,row) else map[row.movieId]=row }
                    if(rows.isNotEmpty())wrote.complete(Unit);Unit
                }
                "insertSeries","insertSeriesFirst" -> {
                    if(failWrites) error("simulated write failure")
                    if(failSeriesMerge && method.name=="insertSeries")error("simulated merge failure")
                    val rows=a[0] as List<CachedSeriesEntity>;maxBatch=maxOf(maxBatch,rows.size)
                    rows.forEach { row -> val map=series.getOrPut(row.generationId){linkedMapOf()};if(method.name.endsWith("First"))map.putIfAbsent(row.seriesId,row) else map[row.seriesId]=row };Unit
                }
                "insertChannels" -> { val rows=a[0] as List<CachedChannelEntity>;maxBatch=maxOf(maxBatch,rows.size);rows.forEach{channels.getOrPut(it.generationId){linkedMapOf()}[it.channelId]=it};Unit }
                "insertCategories" -> { val rows=a[0] as List<CachedCategoryEntity>;rows.forEach{categories.getOrPut(it.generationId){linkedMapOf()}[it.type+":"+it.categoryId]=it};Unit }
                "deleteMoviesForGeneration" -> {movies.remove(a[1] as String);Unit}
                "deleteSeriesForGeneration" -> {series.remove(a[1] as String);Unit}
                "streamMovieStage" -> movies[a[1]]?.values.orEmpty().sortedWith(compareBy<CachedMovieEntity>{it.providerOrder}.thenBy{it.movieId}).filter{(it.providerOrder ?: -1) > a[3] as Int}.take(a[2] as Int)
                "streamSeriesStage" -> series[a[1]]?.values.orEmpty().sortedWith(compareBy<CachedSeriesEntity>{it.providerOrder}.thenBy{it.seriesId}).filter{(it.providerOrder ?: -1) > a[3] as Int}.take(a[2] as Int)
                else -> error("Unexpected DAO call ${method.name}")
            }
        }
    } as PlaylistCacheDao
}
private class Http(val data: MutableMap<String,String>, val legacy: Boolean=false) : PlaylistHttpClient {
    val calls=ConcurrentHashMap<String,AtomicInteger>()
    val current=AtomicInteger();val max=AtomicInteger()
    var response: ((String,Int)->String)?=null
    var pauseAfterFirstBatch=false
    var holdResult=false
    fun key(url:String):String {
        val action=url.substringAfter("&action=", "auth").substringBefore('&')
        val cat=url.substringAfter("&category_id=", "")
        return if(cat.isEmpty()) action else "$action:$cat"
    }
    override suspend fun get(url:String):String {
        val k=key(url);if(k!="auth" && !legacy)error("Non-streaming catalog GET")
        return if(k=="auth") """{"user_info":{"auth":1}}""" else data[k] ?: "[]"
    }
    override suspend fun <T> withReader(url:String, consume:suspend(Reader)->T):T {
        val k=key(url);val attempt=calls.getOrPut(k){AtomicInteger()}.incrementAndGet()
        val n=current.incrementAndGet();max.updateAndGet { maxOf(it,n) }
        try {
            if(k.contains(':'))delay(20)
            val raw=response?.invoke(k,attempt) ?: data[k] ?: "[]"
            val reader=if(pauseAfterFirstBatch && k=="get_vod_streams") object:Reader(){
                val source=StringReader(raw);var count=0
                override fun read(b:CharArray,o:Int,l:Int):Int {
                    if(count>20_000)throw IOException("simulated transport failure")
                    return source.read(b,o,minOf(l,1000)).also{if(it>0)count+=it}
                }
                override fun close(){source.close()}
            } else StringReader(raw)
            val result=reader.use {consume(it)}
            if(holdResult && k=="get_vod_streams") awaitCancellation()
            return result
        }finally{current.decrementAndGet()}
    }
    override suspend fun readLines(url:String,onLine:suspend(String)->Unit){error("M3U not expected")}
}
private val dummyM3u=object:M3uParser{override fun parse(content:String)=PlaylistContent()}
private fun source(id:String)=PlaylistSource.XtreamCodes(id,"test",true,"http://provider.test","test_user_$id","secret_password")
private fun data(n:Int=650)=mutableMapOf(
 "get_live_categories" to """[{"category_id":1,"category_name":"TV"}]""",
 "get_live_streams" to """[{"stream_id":1,"name":"Channel","category_id":1,"epg_channel_id":"epg","direct_source":""}]""",
 "get_vod_categories" to """[{"category_id":1,"category_name":"Movies"},{"category_id":2,"category_name":"Other"},{"category_id":3,"category_name":"Third"}]""",
 "get_vod_streams" to (0 until n).joinToString(",","[","]"){"""{"stream_id":$it,"name":"Film $it","category_id":"1","stream_icon":"poster","backdrop_path":["back"],"year":"2024","rating":"8,5","duration":"01:02:03","added":"123"}"""},
 "get_series_categories" to """[{"category_id":1,"category_name":"Series"}]""",
 "get_series" to """[{"series_id":10,"name":"Série","category_id":1,"genre":"A, B","rating":8,"cover":"cover","backdrop_path":"back","release_date":"2021-02-03","duration_secs":120,"youtube_trailer":"x"}]"""
)
private fun sync(http:Http,store:Store):FullImportTestHarness {
 val manager=PlaylistCacheGenerationManager(store.dao)
 val repo=DefaultPlaylistRepository(http,dummyM3u,DefaultXtreamParser())
 return FullImportTestHarness(repo,M3uCacheImporter(http,store.dao,manager),store.dao,manager)
}
fun main()=runBlocking {
 var passed=0
 suspend fun test(name:String, block:suspend()->Unit){block();passed++;println("PASS $name")}
 test("streaming matches legacy fields, URLs, category types and provider order across batches") {
  val d=data();val expected=DefaultPlaylistRepository(Http(d,true),dummyM3u,DefaultXtreamParser()).loadContent(source("parity"))
  val store=Store();val http=Http(d);val generation=sync(http,store).sync(source("parity"))
  check(store.states[generation]=="READY" && store.states.size==1)
  val actual=store.movies[generation]!!.values.sortedBy{it.providerOrder}
  check(actual.size==expected.movies.size)
  actual.zip(expected.movies).forEachIndexed { index,(row,m) ->
   check(row.movieId==m.id && row.name==m.name && row.posterUrl==m.posterUrl && row.backdropUrl==m.backdropUrl && row.categoryId==m.categoryId && row.streamUrl==m.streamUrl && row.year==m.year && row.rating==m.rating && row.durationSeconds==m.durationSeconds && row.addedAtEpochSeconds==m.addedAtEpochSeconds && row.providerOrder==index)
  }
  check(store.channels[generation].isNullOrEmpty()) // Current app defers live catalogue until Canais opens.
  val s=store.series[generation]!!.values.single();val e=expected.series.single();check(s.genres==e.genres.joinToString("|") && s.year==e.year && s.posterUrl==e.posterUrl && s.rating==e.rating && s.providerOrder==0)
  check(store.categories[generation]!!.size==expected.categories.size);check(store.maxBatch<=2000)
 }
 test("missing outer bracket recovery preserved; mixed invalid records do not reject good later batch") {
  val d=data();d["get_vod_streams"]="["+(0 until 260).joinToString(","){"null"}+",{\"stream_id\":\"ok\",\"name\":\"last\"}"
  val store=Store();val id=sync(Http(d),store).sync(source("recover"));check(store.movies[id]!!.keys==setOf("ok"))
 }
 test("malformed full endpoint retries twice, staged categories preserve first occurrence and order") {
  val d=data();d["get_vod_streams"]="[{\"stream_id\":\"bad\""
  d["get_vod_streams:1"]="""[{"stream_id":"a","name":"first"},{"stream_id":"a","name":"second"},{"stream_id":"b"}]"""
  d["get_vod_streams:2"]="""[{"stream_id":"b","name":"duplicate"},{"stream_id":"c"}]"""
  d["get_vod_streams:3"]="[]"
  val http=Http(d);val store=Store();val id=sync(http,store).sync(source("fallback"))
  val rows=store.movies[id]!!.values.sortedBy{it.providerOrder};check(rows.map{it.movieId}==listOf("a","b","c") && rows.first().name=="first")
  check(rows.map{it.providerOrder}==listOf(0,1,2));check(http.calls["get_vod_streams"]!!.get()==2 && http.max.get()==3)
  check(store.states.size==1 && store.maxStages<=4)
 }
 test("category retry discards previous partial writes and only retries failing category") {
  val d=data();d["get_vod_streams"]="bad"
  d["get_vod_streams:1"]="""[{"stream_id":"good"}]""";d["get_vod_streams:2"]="[]";d["get_vod_streams:3"]="[]"
  val http=Http(d);http.response={key,attempt ->if(key=="get_vod_streams:1"&&attempt==1) data(300)["get_vod_streams"]!!.dropLast(1)+",{\"stream_id\":" else d[key]?:"[]"}
  val store=Store();val id=sync(http,store).sync(source("retry"));check(store.movies[id]!!.keys==setOf("good"))
  check(http.calls["get_vod_streams:1"]!!.get()==2 && http.calls["get_vod_streams:2"]!!.get()==1)
 }
 test("staged keyset merge crosses multiple batches with correct order") {
  val d=data(1300);val all=d["get_vod_streams"]!!;d["get_vod_streams"]="bad"
  d["get_vod_streams:1"]=all;d["get_vod_streams:2"]="[]";d["get_vod_streams:3"]="[]"
  val store=Store();val id=sync(Http(d),store).sync(source("keyset"))
  val rows=store.movies[id]!!.values.sortedBy{it.providerOrder};check(rows.size==1300)
  check(rows.map{it.providerOrder}==(0 until 1300).toList())
 }
 test("full-endpoint retry removes earlier persisted partial batches") {
  val d=data(1);d["get_vod_streams"]="""[{"stream_id":"final"}]"""
  val http=Http(d);http.response={k,attempt->if(k=="get_vod_streams"&&attempt==1)data(6000)["get_vod_streams"]!!.dropLast(1)+",{\"stream_id\":" else d[k]?:"[]"}
  val store=Store();val id=sync(http,store).sync(source("full_retry"));check(store.movies[id]!!.keys==setOf("final"));check(http.calls["get_vod_streams"]!!.get()==2);check(store.maxBatch==2000)
 }
 test("32 simultaneous callers share the same progressive import") {
  val store=Store();val http=Http(data(800));val synchronizer=sync(http,store)
  val ids=coroutineScope{(0 until 32).map{async{synchronizer.sync(source("concurrent_stream"))}}.awaitAll()}
  check(ids.distinct().size==1 && store.states.size==1 && http.calls["get_vod_streams"]!!.get()==1)
 }
 test("failed category preserves active generation and discards all staging imports") {
  val d=data();d["get_vod_streams"]="bad";val store=Store();store.states["old"]="ACTIVE"
  val http=Http(d);http.response={k,_ ->if(k.contains(':')) "bad" else d[k]?:"[]"}
  check(runCatching{sync(http,store).sync(source("fail"))}.isFailure);check(store.states==mapOf("old" to "ACTIVE"))
 }
 test("local write failure is not retried as provider error and leaves previous active intact") {
  val store=Store();store.states["old"]="ACTIVE";store.failWrites=true;val http=Http(data())
  check(runCatching{sync(http,store).sync(source("disk"))}.isFailure);check(store.states==mapOf("old" to "ACTIVE"))
  check(http.calls["get_vod_streams"]!!.get()==1)
 }
 test("transport failure after persisted batches discards partial generation") {
  val store=Store();store.states["old"]="ACTIVE";val http=Http(data(1000));http.pauseAfterFirstBatch=true
  check(runCatching{sync(http,store).sync(source("transport"))}.isFailure);check(store.states==mapOf("old" to "ACTIVE"))
 }
 test("cancellation during streamed writes preserves active and releases generation") {
  val store=Store();store.states["old"]="ACTIVE";val http=Http(data(4000));http.holdResult=true;val job=launch{sync(http,store).sync(source("cancel"))}
  store.wrote.await();job.cancelAndJoin();withTimeout(3000){while(store.states.values.any{it=="IMPORTING"})delay(5)};check(store.states==mapOf("old" to "ACTIVE"))
 }
 test("adaptive series category mode remains available on subsequent sync") {
  val d=data(1);d["get_series"]="bad";d["get_series:1"]="""[{"series_id":42}]"""
  val http=Http(d);val store=Store();val synchronizer=sync(http,store);synchronizer.sync(source("adaptive"))
  val first=http.calls["get_series"]!!.get();synchronizer.sync(source("adaptive"))
  check(first==2 && http.calls["get_series"]!!.get()==first && http.calls["get_series:1"]!!.get()==2)
 }
 test("learned provider category IDs remain part of later VOD fallback") {
  val d=data(1);d["get_vod_streams"]="""[{"stream_id":"outside","category_id":"4"}]"""
  val http=Http(d);val store=Store();val synchronizer=sync(http,store);synchronizer.sync(source("extras"))
  d["get_vod_streams"]="bad";d["get_vod_streams:1"]="[]";d["get_vod_streams:2"]="[]";d["get_vod_streams:3"]="[]"
  d["get_vod_streams:4"]="""[{"stream_id":"outside","category_id":"4"}]"""
  val id=synchronizer.sync(source("extras"));check(store.movies[id]!!.keys==setOf("outside"));check(http.calls["get_vod_streams:4"]!!.get()==1)
 }
 test("failed adaptive category mode recovers through full endpoint and cleans stages") {
  val d=data(1);d["get_series"]="bad";d["get_series:1"]="""[{"series_id":42}]"""
  val http=Http(d);val store=Store();val synchronizer=sync(http,store);synchronizer.sync(source("adaptive_recover"))
  d["get_series"]="""[{"series_id":77}]""";d["get_series:1"]="bad"
  val id=synchronizer.sync(source("adaptive_recover"));check(store.series[id]!!.keys==setOf("77"));check(store.states.values.none{it=="IMPORTING"})
 }
 test("adaptive category merge disk failure does not trigger full network fallback") {
  val d=data(1);d["get_series"]="bad";d["get_series:1"]="""[{"series_id":42}]"""
  val http=Http(d);val store=Store();val synchronizer=sync(http,store);synchronizer.sync(source("adaptive_disk"))
  val first=http.calls["get_series"]!!.get();store.failSeriesMerge=true
  check(runCatching{synchronizer.sync(source("adaptive_disk"))}.isFailure)
  check(http.calls["get_series"]!!.get()==first)
  check(store.states.values.none{it=="IMPORTING"})
 }
 test("alternate parser retains the legacy repository contract") {
  val store=Store();val http=Http(data(3),true);val manager=PlaylistCacheGenerationManager(store.dao)
  val parser=object:XtreamParser by DefaultXtreamParser(){}
  val repo=DefaultPlaylistRepository(http,dummyM3u,parser)
  val synchronizer=FullImportTestHarness(repo,M3uCacheImporter(http,store.dao,manager),store.dao,manager)
  val id=synchronizer.sync(source("custom_parser"));check(store.movies[id]!!.size==3 && store.states[id]=="READY")
 }
 check(android.util.Log.messages.none{it.contains("secret_password")||it.contains("http://provider.test")})
 println("$passed real repository/synchronizer/writer integration scenarios passed; persistence simulated")
}
