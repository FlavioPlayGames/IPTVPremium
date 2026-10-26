package br.com.flaviotorres.iptvpremium.data.remote
import java.net.ServerSocket
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.thread
import kotlinx.coroutines.*
fun main()=runBlocking {
 val client=DefaultPlaylistHttpClient()
 suspend fun serve(status:Int,body:String,gzip:Boolean=false,check:suspend(String)->Unit){
  val raw=body.toByteArray(Charsets.UTF_8);val encoded=if(gzip)ByteArrayOutputStream().also{b->GZIPOutputStream(b).use{it.write(raw)}}.toByteArray() else raw
  val server=ServerSocket(0);val worker=thread(isDaemon=true){server.accept().use{socket->
   val reader=socket.getInputStream().bufferedReader();while(reader.readLine()?.isNotEmpty()==true){}
   socket.getOutputStream().write(("HTTP/1.1 $status Test\r\nContent-Length: ${encoded.size}\r\nContent-Type: text/plain\r\n"+(if(gzip)"Content-Encoding: gzip\r\n"else "")+"Connection: close\r\n\r\n").toByteArray())
   socket.getOutputStream().write(encoded);socket.getOutputStream().flush()
  }}
  try{check("http://127.0.0.1:${server.localPort}/get.php?username=secret_user&password=secret_password")}finally{server.close();worker.join(1000)}
 }
 for(gzip in listOf(false,true)){
  val body="\uFEFF#EXTM3U\n#EXTINF:-1 group-title=\"Séries\",Coração S01E01\nhttp://x/series/1\n"
  serve(200,body,gzip){url->val lines=mutableListOf<String>();client.readLines(url){lines+=it};check(lines==body.lineSequence().toList().dropLast(1))}
  println("PASS M3U HTTP UTF-8/BOM lines unchanged, gzip=$gzip")
 }
 for(status in listOf(401,403))serve(status,"denied"){url->check(runCatching{client.readLines(url){}}.exceptionOrNull() is PlaylistAccessDeniedException)}
 println("PASS M3U denial classification unchanged")
 serve(200,"[1,2,3]"){url->check(client.withReader(url){it.readText()}=="[1,2,3]")}
 serve(200,"{\"auth\":1}"){url->check(client.get(url)=="{\"auth\":1}")}
 println("PASS Xtream withReader/get HTTP behavior retained")
 val stalled=ServerSocket(0);val release=java.util.concurrent.CountDownLatch(1);val bodyStarted=CompletableDeferred<Unit>()
 val worker=thread(isDaemon=true){stalled.accept().use {socket->
  val input=socket.getInputStream().bufferedReader();while(input.readLine()?.isNotEmpty()==true){}
  socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 999999999\r\nConnection: close\r\n\r\n".toByteArray())
  socket.getOutputStream().write(("line\n".repeat(300)).toByteArray());socket.getOutputStream().flush()
  release.await(5,java.util.concurrent.TimeUnit.SECONDS)
 }}
 val pending=launch {client.readLines("http://127.0.0.1:${stalled.localPort}/get.php") {bodyStarted.complete(Unit)}}
 var fast=false
 try {
  withTimeout(3000){bodyStarted.await()}
  fast=withTimeoutOrNull(1500){pending.cancelAndJoin();true}==true
 } finally {release.countDown();stalled.close();worker.join(2000);pending.cancelAndJoin()}
 check(fast){"Cancellation waited for stalled response"}
 println("PASS cancelling a stalled M3U body releases the import coroutine without waiting for the 30-second timeout")
 check(android.util.Log.messages.any{it.contains("STREAM_INICIO")})
 check(android.util.Log.messages.any{it.contains("STREAM_HEADERS")})
 check(android.util.Log.messages.none{it.contains("secret_user")||it.contains("secret_password")})
 println("PASS first-request/header diagnostics without logging credentials or full URL")
}
