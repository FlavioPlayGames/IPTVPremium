package br.com.flaviotorres.iptvpremium.data.remote
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlinx.coroutines.*
fun main()=runBlocking {
 val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
 val executor=Executors.newCachedThreadPool();server.executor=executor
 server.createContext("/ok") { x -> val b="{\"ok\":true}".toByteArray();x.sendResponseHeaders(200,b.size.toLong());x.responseBody.use{it.write(b)} }
 server.createContext("/bad") { x -> val b="private-error-body".toByteArray();x.sendResponseHeaders(401,b.size.toLong());x.responseBody.use{it.write(b)} }
 server.createContext("/slow") { x -> try {x.sendResponseHeaders(200,0);x.responseBody.use{it.write('{'.code);it.flush();Thread.sleep(2500);it.write('}'.code)}}catch(_:Exception){} }
 server.start()
 try {
  val client=TmdbHttpClient("fixture-token","http://127.0.0.1:${server.address.port}")
  check(client.get("/ok")=="{\"ok\":true}");println("PASS TMDB consulta HTTP local e resposta completa")
  val failure=runCatching{client.get("/bad")}.exceptionOrNull();check(failure!=null && "401" in failure.message.orEmpty() && "private-error-body" !in failure.message.orEmpty());println("PASS erro HTTP informa status sem corpo ou credenciais")
  check(runCatching { TmdbHttpClient("", "http://127.0.0.1:${server.address.port}").get("/ok") }.isFailure);println("PASS chave ausente nao faz consulta sem autenticacao")
  val started=System.nanoTime();val result=withTimeoutOrNull(200){client.get("/slow")};val elapsed=(System.nanoTime()-started)/1_000_000;check(result==null && elapsed<2300){"cancelamento lento: $elapsed"};println("PASS prazo libera chamador durante consulta TMDB lenta tempoMs=$elapsed")
 } finally {server.stop(0);executor.shutdownNow()}
}
