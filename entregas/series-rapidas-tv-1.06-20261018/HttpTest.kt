package br.com.flaviotorres.iptvpremium.data.remote
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
fun main()=runBlocking {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0);server.executor=Executors.newCachedThreadPool{r->Thread(r).apply{isDaemon=true}}
 val entered=CountDownLatch(1);val release=CountDownLatch(1)
 server.createContext("/ok"){e->val bytes="{\"ok\":true}".toByteArray();e.sendResponseHeaders(200,bytes.size.toLong());e.responseBody.use{it.write(bytes)}}
 server.createContext("/player_api.php"){e->entered.countDown();release.await(15,TimeUnit.SECONDS);runCatching{e.sendResponseHeaders(200,2);e.responseBody.use{it.write("{}".toByteArray())}}}
 server.createContext("/denied"){e->e.sendResponseHeaders(403,-1);e.close()}
 server.start();val url="http://127.0.0.1:${server.address.port}"
 try {
  val client=DefaultPlaylistHttpClient()
  pass("GET normal conclui sem ficar preso no observador de cancelamento",withTimeout(3000){client.get("$url/ok")}=="{\"ok\":true}")
  val denied=runCatching{client.get("$url/denied")}
  pass("HTTP permanente preserva codigo para politica de retry",denied.exceptionOrNull()?.message?.contains("403")==true)
  val job=launch(Dispatchers.IO){client.get("$url/player_api.php?action=get_series_info&password=private_fixture")}
  check(entered.await(3,TimeUnit.SECONDS));val start=System.nanoTime();job.cancelAndJoin();val elapsed=(System.nanoTime()-start)/1_000_000
  pass("cancelar consulta fecha conexao antes do timeout",elapsed<4000)
  pass("logs nao expõem credenciais nem URL",android.util.Log.messages.none{it.contains("private_fixture") || it.contains("http://")})
 } finally {release.countDown();server.stop(0)}
}
