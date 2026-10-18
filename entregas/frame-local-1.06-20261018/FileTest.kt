package br.com.flaviotorres.iptvpremium.data.repository
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.io.File
import java.nio.file.Files
import java.io.IOException
fun main() {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val data=ByteArray(7*1024*1024){(it%251).toByte()}
 var calls=0;var served=0L
 val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
 server.createContext("/range") { e ->
  calls++;val range=e.requestHeaders.getFirst("Range").substringAfter("bytes=").split('-');val a=range[0].toInt();val b=minOf(range[1].toInt(),data.lastIndex)
  e.responseHeaders.add("Content-Range","bytes $a-$b/${data.size}");e.sendResponseHeaders(206,(b-a+1).toLong());e.responseBody.use{it.write(data,a,b-a+1)};served+=b-a+1
 }
 val small=data.copyOf(320000)
 server.createContext("/small") { e -> e.sendResponseHeaders(200,small.size.toLong());runCatching{e.responseBody.use{it.write(small)}} }
 server.createContext("/error") { e ->e.sendResponseHeaders(503,-1);e.close() }
 server.start();val base="http://127.0.0.1:${server.address.port}"
 val folder=Files.createTempDirectory("episode-file-test").toFile();val file=File(folder,"loading_episode_frame.tmp")
 try {
  val decoded=withEpisodePreviewFile(file,BoundedVideoReader("$base/range"),{}) { f->
   pass("arquivo so contem prefixo limitado de seis MiB",f.length()==EPISODE_PREVIEW_FILE_LIMIT)
   pass("descritor local le os bytes reais do episodio",f.readBytes().contentEquals(data.copyOf(EPISODE_PREVIEW_FILE_LIMIT.toInt())))
   "decoded"
  }
  pass("resultado do decodificador e devolvido",decoded=="decoded")
  pass("temporario removido depois do sucesso",!file.exists())
  pass("video maior nao e baixado por completo",served==EPISODE_PREVIEW_FILE_LIMIT && calls==24)
  withEpisodePreviewFile(file,BoundedVideoReader("$base/small"),{}) { f->pass("servidor sem Range e arquivo pequeno preservados",f.readBytes().contentEquals(small)) }
  val failed=runCatching { withEpisodePreviewFile(file,BoundedVideoReader("$base/small"),{}) { throw RuntimeException("native fixture") } }
  pass("falha do decodificador remove arquivo e propaga erro",failed.isFailure && !file.exists())
  var checks=0
  val cancel=runCatching { withEpisodePreviewFile(file,BoundedVideoReader("$base/small"),{if(++checks==2)throw IOException("cancel fixture")}) { error("nao deve decodificar") } }
  pass("cancelamento durante copia remove temporario",cancel.isFailure && !file.exists())
  val network=runCatching { withEpisodePreviewFile(file,BoundedVideoReader("$base/error"),{}) { error("nao deve decodificar") } }
  pass("falha HTTP limpa temporario sem decodificar",network.isFailure && !file.exists())
  val nil=withEpisodePreviewFile(file,BoundedVideoReader("$base/small"),{}) {null as String?}
  pass("ausencia de frame tambem limpa temporario",nil==null && !file.exists())
 } finally {server.stop(0);folder.deleteRecursively()}
}
