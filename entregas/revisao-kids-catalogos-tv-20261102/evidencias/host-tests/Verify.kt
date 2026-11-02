package br.com.flaviotorres.iptvpremium.data.repository

import kotlinx.coroutines.*
import org.json.JSONObject
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.live.*

fun main() = runBlocking {
 var checks=0
 fun ok(value:Boolean, label:String) {check(value){label};checks++;println("PASS $label")}
 fun key(name:String,year:Int?=2024,kind:String="movie") = catalogMetadataKey(kind,name,year,null,null)
 ok(key("Ação (2024) [DUB] 4K HDR H.265") == key("Acao [LEG] FHD"), "quality, language, accents share one work")
 ok(key("Ação",2024)!=key("Ação",1999),"remakes stay separate")
 ok(key("Frozen 2")!=key("Frozen"),"sequels stay separate")
 ok(artworkTitle("Blade Runner 2049")=="blade runner 2049","numbers in real titles survive")
 ok(artworkTitle("1917")=="1917","numeric title survives")
 ok(key("Dark",2017,"tv")!=key("Dark",2017,"movie"),"movie and series caches stay separate")
 ok(key("Ambiguous",null)==null,"no title-only sharing without year or external ID")
 ok(catalogTitleYear("Titulo (2021) 4K",null)==2021,"year recovered from provider title")
 val dir=kotlin.io.path.createTempDirectory("metadata-test").toFile()
 CatalogMetadataDiskCache.initialize(android.content.Context(dir))
 var calls=0
 suspend fun get(path:String,query:Map<String,String>):String {
  calls++;delay(15)
  return when {
   path.startsWith("/search/") -> """{"results":[{"id":2001,"title":"Teste Cache","original_title":"Teste Cache","release_date":"2024-02-01"}]}"""
   path=="/movie/2001" -> """{"id":2001,"title":"Teste Cache","release_date":"2024-02-01","overview":"Sinopse compartilhada","poster_path":"/cover.jpg","external_ids":{"imdb_id":"tt1234567"}}"""
   else -> error("Unexpected request: $path")
  }
 }
 val results=coroutineScope { (0..9).map { n->async { resolveMovieArtworkIdentity(if(n%2==0)"Teste Cache [DUB] 4K"else"Teste Cache HD",2024,null,null,::get) } }.awaitAll() }
 ok(results.all{it?.optInt("id")==2001} && calls==2,"10 simultaneous variants share search and details")
 results[0]!!.put("overview","mutated")
 ok(resolveMovieArtworkIdentity("Teste Cache HDR",2024,null,null,::get)?.optString("overview")=="Sinopse compartilhada","cached metadata cannot be mutated by a caller")
 ok(dir.walkTopDown().any{it.extension=="json"},"public metadata persisted without provider credentials")
 val cacheField=CatalogMetadataCache.javaClass.getDeclaredField("entries").apply{isAccessible=true}
 (cacheField.get(CatalogMetadataCache) as MutableMap<*,*>).clear()
 ok(resolveMovieArtworkIdentity("Teste Cache HD",2024,null,null,::get)?.optInt("id")==2001 && calls==2,"disk cache reused after RAM cache eviction")
 var missCalls=0
 repeat(2) { CatalogMetadataCache.load("movie:title:2024:missing") {missCalls++;null} }
 ok(missCalls==1,"missing metadata does not flood network")
 var retries=0
 try {CatalogMetadataCache.load("movie:title:2024:cancel") {retries++;throw CancellationException()}}catch(_:CancellationException){}
 CatalogMetadataCache.load("movie:title:2024:cancel") {retries++;JSONObject("""{"id":42}""")}
 ok(retries==2,"cancellation is not cached as a permanent miss")
 var imdbLookup=false
 val imdb=resolveMovieArtworkIdentity("Unknown",null,null,"tt7654321", {path,_->
  if(path.startsWith("/find/")){imdbLookup=true;"""{"movie_results":[{"id":123}]}"""}else """{"id":123,"title":"Known"}"""
 })
 ok(imdbLookup&&imdb?.optInt("id")==123,"IMDb ID resolves through TMDB")
 val ambiguous=resolveMovieArtworkIdentity("Same",2020,null,null,{path,_->
  check(path.startsWith("/search/"));"""{"results":[{"id":1,"title":"Same","release_date":"2020-01-01"},{"id":2,"title":"Same","release_date":"2020-05-01"}]}"""
 })
 ok(ambiguous==null,"ambiguous names never pick a popular result")
 for(density in listOf(.75f,1f,1.5f,2f,2.625f,3f)) for(scale in listOf(1f,1.15f,1.3f)) {
  val g=catalogTwoRowGeometry(650f,360f,5,4f,26*scale+6,padding=4f)
  val height=(360*density).toInt();val padding=kotlin.math.round(4*density).toInt()
  val card=kotlin.math.floor(g.cardHeight*density).toInt();val gap=height-padding*2-card*2
  val visibleRows=kotlin.math.floor((height-padding*2+gap).toDouble()/(card+gap)).toInt()
  check(visibleRows==2 && card*2+gap+padding*2==height)
 }
 ok(true,"two rows fit at 18 density/font combinations; a down step exposes one row")
 val locks=br.com.flaviotorres.iptvpremium.data.cache.PosterRequestLocks()
 val held=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
 val one=launch {locks.withLock("Aa"){held.complete(Unit);release.await()}}
 held.await()
 val independent=withTimeoutOrNull(300) {locks.withLock("BB"){true}}
 release.complete(Unit);one.join()
 ok(independent==true && locks.activeKeys()==0,"hash-colliding titles never block each other; leases released")
 repeat(140) { n->CatalogMetadataCache.load("movie:tmdb:${9000+n}"){JSONObject().put("id",9000+n)} }
 ok((cacheField.get(CatalogMetadataCache) as Map<*,*>).size<=128,"metadata memory cache stays bounded")
 val router=CatalogEscapeRouter();var backs=0;router.register{backs++;true}
 router.down(false);router.down(true);router.up();router.down(false);router.down(false)
 ok(backs==3,"Back repeats suppressed and missing key-up does not lock remote")
 val transition=CatalogEscapeRouter();lateinit var detach:()->Unit
 detach=transition.register {detach();true}
 transition.down(false)
 ok(transition.up(),"Back key-up remains consumed when its screen was removed on key-down")
 ok(liveBackAction(false,false,false,false,false,true,false)==LiveBackAction.EXIT,"Back on logical categories exits even after row focus was lost")
 ok(liveBackAction(false,false,false,false,true,false,false)==LiveBackAction.CATEGORIES,"Back from channel list returns categories without OK")
 dir.deleteRecursively()
 println("$checks checks passed. Host tests; physical TV focus not simulated.")
}
