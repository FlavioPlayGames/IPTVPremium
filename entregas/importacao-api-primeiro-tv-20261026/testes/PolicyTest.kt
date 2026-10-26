package br.com.flaviotorres.iptvpremium.data.sync
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import kotlinx.coroutines.*
import java.io.Reader
import java.io.StringReader
import java.io.IOException
private class Store: M3uTransportStore {
 val values=mutableMapOf<String,String>(); var writes=0
 override suspend fun read(key:String)=values[key]
 override suspend fun write(key:String,value:String){values[key]=value;writes++}
}
private class Http: PlaylistHttpClient {
 var body="""{"user_info":{"auth":1,"status":"Active"}}""";var failure:Exception?=null;var calls=0
 override suspend fun get(url:String):String {calls++;failure?.let{throw it};return body}
 override suspend fun readLines(url:String,onLine:suspend(String)->Unit)=error("Unexpected M3U")
}
private fun source(url:String="https://example.invalid/panel/get.php?username=test&password=hidden&type=m3u_plus&output=mpegts")=PlaylistSource.M3u("p","Name",playlistUrl=url)
fun main()=runBlocking {
 var n=0
 suspend fun test(name:String,block:suspend()->Unit){block();println("PASS $name");n++}
 test("verified API selection persists across instances and refresh without new probe") {
  val s=Store();var probes=0
  val p=M3uTransportPolicy(s){probes++;check(it.id=="p"&&it.serverUrl=="https://example.invalid/panel");true}
  check(p.select(source())!=null);check(p.select(source().copy(name="Renamed"))!=null)
  check(M3uTransportPolicy(s){error("must reuse decision")}.select(source())!=null)
  check(probes==1&&s.writes==1&&s.values.toString().let{"hidden" !in it&&"example.invalid" !in it})
 }
 test("configuration change renegotiates; no navigation probe") {
  val s=Store();var probes=0;val p=M3uTransportPolicy(s){probes++;true}
  check(p.selected(source())==null&&probes==0);p.select(source())
  val changed=source(source().playlistUrl.replace("hidden","newsecret"))
  check(p.selected(changed)==null);p.select(changed);check(probes==2)
 }
 test("generic, filtered, signed, conflicting and unsupported-format URLs remain native") {
  val p=M3uTransportPolicy(Store()){error("must not probe")}
  for(url in listOf("https://example.invalid/file.m3u",source().playlistUrl+"&category_id=3",source().playlistUrl+"&token=signed",source().playlistUrl+"&username=other",source().playlistUrl+"#fragment",source().playlistUrl.replace("m3u_plus","m3u_plus_live"),source().playlistUrl.replace("mpegts","m3u8"),source().playlistUrl.replace("mpegts","unknown")))check(p.select(source(url))==null)
 }
 test("previous API choice cannot become native after a configuration change or negative probe") {
  val s=Store();var supported=true;val p=M3uTransportPolicy(s){supported}
  p.select(source());val saved=s.values.toMap();supported=false
  val changed=source(source().playlistUrl.replace("hidden","newsecret"))
  check(runCatching{p.select(changed)}.isFailure&&s.values==saved)
  check(runCatching{p.select(source(source().playlistUrl+"&category_id=3"))}.isFailure&&s.values==saved)
  check(p.hasApiSelection(source())&&p.selected(source())!=null)
 }
 test("explicit unsupported API selection persists as native") {
  val s=Store();var calls=0;val p=M3uTransportPolicy(s){calls++;false}
  check(p.select(source())==null&&p.select(source())==null&&calls==1&&s.writes==1)
 }
 test("network failure and cancellation do not store a fallback decision") {
  val s=Store();check(runCatching{M3uTransportPolicy(s){throw IOException("failure")}.select(source())}.isFailure);check(s.writes==0)
  val ready=CompletableDeferred<Unit>();val job=launch{M3uTransportPolicy(s){ready.complete(Unit);awaitCancellation()}.select(source())}
  ready.await();job.cancelAndJoin();check(s.writes==0)
 }
 test("account probe accepts active auth and rejects denied, malformed, truncated and oversized responses") {
  val h=Http();val probe=M3uAccountProbe(h);val x=br.com.flaviotorres.iptvpremium.data.resolver.M3uXtreamResolver.resolve(source())!!
  check(probe.verify(x))
  for(body in listOf("""{"user_info":{"auth":0,"status":"Active"}}""","""{"user_info":{"auth":1,"status":"Expired"}}""","<html>error</html>","{\"user_info\":", "x".repeat(70_000))) {
   h.body=body;val error=runCatching{probe.verify(x)}.exceptionOrNull();check(error!=null&&"hidden" !in error.message.orEmpty())
  }
  h.body="{}";check(runCatching{probe.verify(x)}.isFailure)
  for(code in listOf(404,405)){h.failure=IllegalStateException("Erro HTTP $code ao acessar a lista.");check(!probe.verify(x))}
  h.failure=IOException("https://secret.invalid?password=hidden");val e=runCatching{probe.verify(x)}.exceptionOrNull()!!;check(e.message?.contains("hidden")==false&&e.cause==null)
 }
 println("$n policy/probe scenarios passed")
}
