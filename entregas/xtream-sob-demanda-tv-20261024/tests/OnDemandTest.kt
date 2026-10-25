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
    val playlistByGeneration = ConcurrentHashMap<String,String>()
    var rejectPublish=false
    var publications=0
    var failSeriesMerge = false
    var maxBatch = 0
    var maxStages = 0
    val wrote = CompletableDeferred<Unit>()
    @Suppress("UNCHECKED_CAST")
    val dao = Proxy.newProxyInstance(PlaylistCacheDao::class.java.classLoader, arrayOf(PlaylistCacheDao::class.java)) { _, method, raw ->
        val a = raw.orEmpty()
        synchronized(this) {
            when (method.name) {
                "getActiveGeneration" -> states.entries.firstOrNull { it.value=="ACTIVE" && playlistByGeneration[it.key]==a[0] }?.let { PlaylistCacheGenerationEntity(a[0] as String,it.key,"ACTIVE",0) }
                "hasCatalogMarker" -> categories[a[1]]?.values.orEmpty().any {it.playlistId==a[0] && it.type==a[2]}
                "hasCatalogScope" -> categories[a[1]]?.values.orEmpty().any {it.playlistId==a[0] && it.type==a[2] && it.categoryId==a[3]}
                "publishLazyCatalog" -> {
                    val playlist=a[0] as String;val stage=a[1] as String;val gen=a[2] as String;val type=a[3] as String;val cat=a[4] as String?
                    if(rejectPublish || states[gen]!="ACTIVE")false else {
                        if(type=="MOVIES") {
                            val target=movies.getOrPut(gen){linkedMapOf()};target.entries.removeIf {cat==null || it.value.categoryId==cat}
                            movies[stage]?.values?.forEach {target[it.movieId]=it.copy(generationId=gen)}
                        } else {
                            val target=series.getOrPut(gen){linkedMapOf()};target.entries.removeIf {cat==null || it.value.categoryId==cat}
                            series[stage]?.values?.forEach {target[it.seriesId]=it.copy(generationId=gen)}
                        }
                        val m=CachedCategoryEntity(playlist,gen,cat?.let {"category:"+it}?:"all","","__DONE_${type}__","XTREAM")
                        categories.getOrPut(gen){linkedMapOf()}[m.type+":"+m.categoryId]=m
                        if(cat==null)categories[gen]?.entries?.removeIf {it.value.type=="__LAZY_${type}__"}
                        publications++;true
                    }
                }
                "upsertGeneration" -> { val g=a[0] as PlaylistCacheGenerationEntity;states[g.generationId]=g.status;playlistByGeneration[g.generationId]=g.playlistId;maxStages=maxOf(maxStages,states.values.count { it=="IMPORTING" });Unit }
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
            if(holdResult && k.startsWith("get_vod_streams")) awaitCancellation()
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
private fun sync(http:Http,store:Store):M3uSourceSynchronizer {
 val manager=PlaylistCacheGenerationManager(store.dao)
 val repo=DefaultPlaylistRepository(http,dummyM3u,DefaultXtreamParser())
 return M3uSourceSynchronizer(repo,M3uCacheImporter(http,store.dao,manager),store.dao,manager)
}

fun main()=runBlocking {
 var passed=0
 suspend fun test(name:String, block:suspend()->Unit){withTimeout(15000){block()};passed++;println("PASS $name")}
 suspend fun fixture(id:String="lazy"):Triple<Store,Http,String>{
  val store=Store();val http=Http(data());val gen=sync(http,store).sync(source(id));store.states[gen]="ACTIVE"
  http.data["get_vod_streams:1"]=http.data["get_vod_streams"]!!
  http.data["get_vod_streams:2"]="""[{"stream_id":9000,"name":"Other","category_id":"2"}]"""
  http.data["get_series:1"]=http.data["get_series"]!!
  return Triple(store,http,gen)
 }
 fun loader(store:Store,http:Http)=OnDemandXtreamCatalog(store.dao,PlaylistCacheGenerationManager(store.dao),DefaultPlaylistRepository(http,dummyM3u,DefaultXtreamParser()))
 test("bootstrap requests categories only and preserves pending markers"){
  val (s,h,g)=fixture();check(s.movies[g].isNullOrEmpty() && s.series[g].isNullOrEmpty() && s.channels[g].isNullOrEmpty())
  check(h.calls.keys==setOf("get_live_categories","get_vod_categories","get_series_categories"))
  check(s.categories[g]!!.values.map{it.type}.containsAll(listOf("MOVIES","SERIES","LIVE_TV","__LAZY_MOVIES__","__LAZY_SERIES__","__LIVE_PENDING__")))
 }
 test("opening untouched catalogue prompts instead of implicitly fetching All"){
  val(s,h,g)=fixture();check(!loader(s,h).ensure(source("lazy"),"MOVIES",null,false));check(h.calls.size==3 && s.publications==0)
 }
 test("category loads once, keeps images, URLs, order and survives loader recreation"){
  val(s,h,g)=fixture();loader(s,h).ensure(source("lazy"),"MOVIES","1",false)
  val rows=s.movies[g]!!.values.sortedBy{it.providerOrder};check(rows.size==650 && rows.first().posterUrl=="poster" && rows.first().backdropUrl=="back")
  check(rows.map{it.providerOrder}==(0 until 650).toList() && rows.all{it.streamUrl.isNotBlank()})
  loader(s,h).ensure(source("lazy"),"MOVIES","1",false);check(h.calls["get_vod_streams:1"]!!.get()==1)
  check(h.calls["get_vod_streams"]==null && s.states.values.none{it=="IMPORTING"})
 }
 test("separate categories do not replace each other; series import is independent"){
  val(s,h,g)=fixture();val l=loader(s,h);l.ensure(source("lazy"),"MOVIES","1",false);l.ensure(source("lazy"),"MOVIES","2",false)
  l.ensure(source("lazy"),"SERIES","1",false)
  check(s.movies[g]!!.size==651 && s.series[g]!!.size==1 && s.states.size==1)
 }
 test("ignored category parameter cannot publish another category"){
  val(s,h,g)=fixture();h.data["get_vod_streams:2"]=h.data["get_vod_streams"]!!
  loader(s,h).ensure(source("lazy"),"MOVIES","2",false);check(s.movies[g].isNullOrEmpty())
 }
 test("empty category is remembered, not requested on every return"){
  val(s,h,g)=fixture();h.data["get_vod_streams:2"]="[]";val l=loader(s,h)
  repeat(2){l.ensure(source("lazy"),"MOVIES","2",false)};check(h.calls["get_vod_streams:2"]!!.get()==1)
 }
 test("explicit All replaces partial rows and completes only that media type"){
  val(s,h,g)=fixture();val l=loader(s,h);l.ensure(source("lazy"),"MOVIES","2",false)
  l.ensure(source("lazy"),"MOVIES",null,true);check(s.movies[g]!!.size==650 && "9000" !in s.movies[g]!!)
  check(!s.dao.hasCatalogMarker("lazy",g,"__LAZY_MOVIES__") && s.dao.hasCatalogMarker("lazy",g,"__LAZY_SERIES__"))
  l.ensure(source("lazy"),"MOVIES",null,true);check(h.calls["get_vod_streams"]!!.get()==1)
 }
 test("failure leaves previous categories intact and never marks failed scope ready"){
  val(s,h,g)=fixture();val l=loader(s,h);l.ensure(source("lazy"),"MOVIES","1",false);h.data["get_vod_streams:2"]="bad"
  check(runCatching{l.ensure(source("lazy"),"MOVIES","2",false)}.isFailure)
  check(s.movies[g]!!.size==650 && !s.dao.hasCatalogScope("lazy",g,"__DONE_MOVIES__","category:2"))
  check(s.states.size==1 && h.calls["get_vod_streams"]==null)
 }
 test("cancellation after writes discards staging and joins HTTP producer"){
  val(s,h,g)=fixture();h.data["get_vod_streams:1"]=data(5000)["get_vod_streams"]!!;h.holdResult=true
  val job=launch{loader(s,h).ensure(source("lazy"),"MOVIES","1",false)};s.wrote.await();job.cancelAndJoin()
  check(s.movies[g].isNullOrEmpty() && s.states.size==1 && h.current.get()==0)
  check(!s.dao.hasCatalogScope("lazy",g,"__DONE_MOVIES__","category:1"))
 }
 test("generation switch cannot publish stale response"){
  val(s,h,g)=fixture();s.rejectPublish=true;check(runCatching{loader(s,h).ensure(source("lazy"),"MOVIES","1",false)}.isFailure)
  check(s.movies[g].isNullOrEmpty() && s.states.size==1)
 }
 test("concurrent request for same category only downloads once"){
  val(s,h,g)=fixture();val l=loader(s,h);coroutineScope{(0 until 16).map{async{l.ensure(source("lazy"),"MOVIES","1",false)}}.awaitAll()}
  check(h.calls["get_vod_streams:1"]!!.get()==1 && s.publications==1)
 }
 test("legacy full generation and M3U bypass remote lazy loader"){
  val(s,h,g)=fixture();s.categories[g]!!.entries.removeIf{it.value.type.startsWith("__LAZY_")}
  val l=loader(s,h);check(l.ensure(source("lazy"),"MOVIES",null,false));check(h.calls.size==3)
  check(l.ensure(PlaylistSource.M3u("m","M3U",true,"http://m3u.test/list"),"MOVIES",null,true))
 }
 test("new refresh gets fresh category markers without overwriting previous generation"){
  val(s,h,g)=fixture();val l=loader(s,h);l.ensure(source("lazy"),"MOVIES","1",false)
  val next=sync(h,s).sync(source("lazy"));check(next!=g && s.movies[g]!!.size==650 && s.movies[next].isNullOrEmpty())
  check(s.states[next]=="READY" && s.dao.hasCatalogMarker("lazy",next,"__LAZY_MOVIES__"))
 }
 test("different playlists cannot reuse another playlist's category rows or credentials"){
  val(s,h,g)=fixture();val g2=sync(h,s).sync(source("other"));s.states[g2]="ACTIVE";val l=loader(s,h)
  l.ensure(source("lazy"),"MOVIES","1",false)
  h.data["get_vod_streams:1"]="""[{"stream_id":999,"name":"Other list","category_id":"1","stream_icon":"other-poster"}]"""
  l.ensure(source("other"),"MOVIES","1",false)
  check(s.movies[g]!!.size==650 && s.movies[g2]!!.values.single().posterUrl=="other-poster")
  check(s.movies[g2]!!.values.single().streamUrl.contains("test_user_other"))
 }
 test("All series requests only series and preserves movie pending state"){
  val(s,h,g)=fixture();loader(s,h).ensure(source("lazy"),"SERIES",null,true)
  check(h.calls["get_series"]!!.get()==1 && h.calls["get_vod_streams"]==null)
  check(s.dao.hasCatalogMarker("lazy",g,"__LAZY_MOVIES__") && !s.dao.hasCatalogMarker("lazy",g,"__LAZY_SERIES__"))
 }
 test("All series keeps malformed-full recovery through category staging"){
  val(s,h,g)=fixture();h.data["get_series"]="bad"
  loader(s,h).ensure(source("lazy"),"SERIES",null,true)
  check(s.series[g]!!.size==1 && h.calls["get_series"]!!.get()==2 && h.calls["get_series:1"]!!.get()==1)
  check(s.states.size==1)
 }
 test("failed category bootstrap keeps previous active data and never falls back to M3U"){
  val(s,h,g)=fixture();loader(s,h).ensure(source("lazy"),"MOVIES","1",false)
  h.response={key,_ ->if(key=="get_series_categories")throw IOException("bootstrap failure") else h.data[key]?:"[]"}
  check(runCatching{sync(h,s).sync(source("lazy"))}.isFailure)
  check(s.states==mapOf(g to "ACTIVE") && s.movies[g]!!.size==650)
 }
 check(android.util.Log.messages.none{it.contains("secret_password")||it.contains("http://provider.test")})
 println("$passed on-demand real repository / writer / synchronizer scenarios; DAO persistence simulated")
}
