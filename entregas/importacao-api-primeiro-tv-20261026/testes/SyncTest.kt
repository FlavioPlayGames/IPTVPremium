package br.com.flaviotorres.iptvpremium.data.sync
import br.com.flaviotorres.iptvpremium.data.cache.*
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.data.cache.importer.M3uCacheImporter
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.*
import br.com.flaviotorres.iptvpremium.domain.repository.PlaylistRepository
import java.lang.reflect.Proxy
import java.io.IOException
import kotlinx.coroutines.*
private class Db {
 val gen=mutableMapOf<String,String>();var active:String?=null
 var channels=0;var movies=0;var series=0;var episodes=0;var pending=false
 val episodeNumbers=mutableListOf<Int?>()
 val dao=Proxy.newProxyInstance(PlaylistCacheDao::class.java.classLoader,arrayOf(PlaylistCacheDao::class.java)){_,m,a0 ->
  val a=a0.orEmpty()
  when(m.name){
   "getActiveGeneration" -> active?.let{PlaylistCacheGenerationEntity("p",it,"ACTIVE",0L)}
   "upsertGeneration" -> {val g=a[0] as PlaylistCacheGenerationEntity;gen[g.generationId]=g.status;Unit}
   "updateGenerationStatus" -> {gen[a[1] as String]=a[2] as String;Unit}
   "discardIncompleteImport" -> {check(a[1]!=active);gen.remove(a[1]);true}
   "insertCategories" -> {pending=pending||(a[0] as List<*>).any{(it as CachedCategoryEntity).categoryId=="__live_pending__"};Unit}
   "insertChannels" -> {channels+=(a[0] as List<*>).size;Unit}
   "insertMovies" -> {movies+=(a[0] as List<*>).size;Unit}
   "insertSeries" -> {series+=(a[0] as List<*>).size;Unit}
   "insertEpisodes" -> {episodes+=(a[0] as List<*>).size;Unit}
   "insertImportBatch" -> {channels+=(a[1] as List<*>).size;movies+=(a[2] as List<*>).size;series+=(a[3] as List<*>).size;episodes+=(a[4] as List<*>).size;episodeNumbers+=(a[4] as List<*>).map{(it as CachedEpisodeEntity).episodeNumber};Unit}
   else -> error("Unexpected DAO ${m.name}")
  }
 } as PlaylistCacheDao
}
private class Http:PlaylistHttpClient {
 var nativeCalls=0
 var lines=listOf("#EXTM3U", "#EXTINF:-1,Live", "http://example.invalid/live/1")
 override suspend fun get(url:String)=error("No network here")
 override suspend fun readLines(url:String,onLine:suspend(String)->Unit){nativeCalls++;for(line in lines)onLine(line)}
}
private class Store:M3uTransportStore {
 val values=mutableMapOf<String,String>()
 override suspend fun read(key:String)=values[key]
 override suspend fun write(key:String,value:String){values[key]=value}
}
private class Api:PlaylistRepository,ProgressiveXtreamRepository {
 var calls=0;var fail=false;var hold:CompletableDeferred<Unit>?=null
 override suspend fun loadContent(source:PlaylistSource):PlaylistContent=error("Must use progressive import")
 override suspend fun importXtream(source:PlaylistSource.XtreamCodes,sink:XtreamCatalogSink) {
  calls++;check(sink.includeLive&&sink.includeOnDemand)
  hold?.let{it.complete(Unit);awaitCancellation()}
  if(fail)throw XtreamCatalogWriteException(IOException("disk failure"))
  sink.append("get_live_streams",PlaylistContent(channels=listOf(Channel("1","Live",null,null,"http://example.invalid/live/1",null))))
  sink.append("get_vod_streams",PlaylistContent(movies=listOf(Movie("11","Film",null,null,null,"http://example.invalid/movie/11",2024,7.0,null))))
  sink.append("get_series",PlaylistContent(series=listOf(Series("21","Show",null,null,null,2024,7.0,null))))
 }
}
private fun source()=PlaylistSource.M3u("p","Test",playlistUrl="http://example.invalid/get.php?username=u&password=p&type=m3u_plus&output=mpegts")
fun main()=runBlocking {
 var count=0
 suspend fun test(name:String,block:suspend()->Unit){block();println("PASS $name");count++}
 test("fresh candidate imports all three API catalogues, zero M3U reads, zero episodes, no live-pending marker"){
  val d=Db();val h=Http();val a=Api();val store=Store();var probes=0
  val policy=M3uTransportPolicy(store){probes++;true};val manager=PlaylistCacheGenerationManager(d.dao)
  val sync=M3uSourceSynchronizer(a,M3uCacheImporter(h,d.dao,manager),d.dao,manager,policy)
  val g=sync.sync(source());check(d.gen[g]=="READY"&&h.nativeCalls==0&&d.channels==1&&d.movies==1&&d.series==1&&d.episodes==0&&!d.pending)
  d.active=g;d.gen[g]="ACTIVE"
  val g2=sync.sync(source());check(g2!=g&&d.gen[g]=="ACTIVE"&&d.gen[g2]=="READY"&&probes==1&&a.calls==2&&h.nativeCalls==0)
  println("  selected transport survives refresh; previous generation stays ACTIVE until caller activates READY")
 }
 test("API write failure keeps ACTIVE generation and decision; never triggers M3U fallback"){
  val d=Db();val h=Http();val a=Api();val store=Store();val policy=M3uTransportPolicy(store){true};policy.select(source())
  d.active="old";d.gen["old"]="ACTIVE";a.fail=true;val manager=PlaylistCacheGenerationManager(d.dao)
  val sync=M3uSourceSynchronizer(a,M3uCacheImporter(h,d.dao,manager),d.dao,manager,policy)
  check(runCatching{sync.sync(source())}.isFailure);check(h.nativeCalls==0&&a.calls==1&&d.gen==mapOf("old" to "ACTIVE")&&policy.selected(source())!=null)
 }
 test("legacy M3U catalogue keeps original IDs/path without probing or conversion"){
  val d=Db();d.active="old";d.gen["old"]="ACTIVE";val h=Http();val a=Api();val manager=PlaylistCacheGenerationManager(d.dao)
  val sync=M3uSourceSynchronizer(a,M3uCacheImporter(h,d.dao,manager),d.dao,manager,M3uTransportPolicy(Store()){error("Must not probe existing M3U")})
  val g=sync.sync(source());check(a.calls==0&&h.nativeCalls==1&&d.gen["old"]=="ACTIVE"&&d.gen[g]=="READY")
 }
 test("cancelled API import discards incomplete generation and never starts M3U"){
  val d=Db();val h=Http();val a=Api();val manager=PlaylistCacheGenerationManager(d.dao);val entered=CompletableDeferred<Unit>();a.hold=entered
  val sync=M3uSourceSynchronizer(a,M3uCacheImporter(h,d.dao,manager),d.dao,manager,M3uTransportPolicy(Store()){true})
  val job=launch{sync.sync(source())};entered.await();job.cancelAndJoin();check(d.gen.isEmpty()&&h.nativeCalls==0)
 }
 test("native M3U recognises E1000/E1001 and preserves episode numbers instead of creating movies"){
  val d=Db();val h=Http();h.lines=listOf("#EXTM3U")+(1000..1001).flatMap{listOf("""#EXTINF:-1 xui-id="$it" group-title="Anime",Show S01E$it""","http://example.invalid/opaque$it")}
  val manager=PlaylistCacheGenerationManager(d.dao);val g=M3uCacheImporter(h,d.dao,manager).import("p","http://example.invalid/file.m3u")
  check(d.gen[g]=="READY"&&d.movies==0&&d.episodes==2&&d.episodeNumbers==listOf(1000,1001))
 }
 println("$count synchronizer scenarios passed (real manager/writer/policy; simulated DAO and HTTP)")
}
