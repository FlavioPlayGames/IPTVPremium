package br.com.flaviotorres.iptvpremium.data.repository
import android.content.Context
import android.media.MediaMetadataRetriever as Native
import android.util.Log
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
fun main()=runBlocking {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0);val bytes=ByteArray(320000){1};var requests=0
 server.createContext("/episode") {e->requests++;e.sendResponseHeaders(200,bytes.size.toLong());runCatching{e.responseBody.use{it.write(bytes)}}};server.start()
 val dir=Files.createTempDirectory("native-fixture").toFile();val context=Context(dir);val url="http://127.0.0.1:${server.address.port}/episode?secret=fixture"
 try {
  Native.remoteMode="source-error";Native.localMode="success"
  val first=EpisodeFrameLoader.load(context,"source-fail",800,450){url}
  pass("RuntimeException da fonte remota aciona descritor local",first!=null && Native.remoteCalls==1 && Native.localCalls==1)
  pass("arquivo temporario nao permanece apos alternativa",dir.walk().none{it.name.endsWith(".tmp")})
  val old=requests;val local=Native.localCalls
  pass("JPEG obtido reutiliza cache sem nova extracao",EpisodeFrameLoader.load(context,"source-fail",800,450){error("cache")}!=null && requests==old && Native.localCalls==local)
  Native.remoteMode="empty"
  pass("frame remoto nulo tambem tenta arquivo",EpisodeFrameLoader.load(context,"empty",400,225){url}!=null && Native.localCalls==local+1)
  Native.remoteMode="success";val before=Native.localCalls
  pass("sucesso remoto dispensa alternativa local",EpisodeFrameLoader.load(context,"remote-ok",400,225){url}!=null && Native.localCalls==before)
  Native.remoteMode="source-error";Native.localMode="source-error"
  pass("falha de ambas alternativas nao quebra card",EpisodeFrameLoader.load(context,"both-fail",400,225){url}==null)
  val prior=Native.remoteCalls
  pass("falha repetida respeita intervalo sem martelar origem",EpisodeFrameLoader.load(context,"both-fail",400,225){url}==null && Native.remoteCalls==prior)
  pass("consulta somente cache nao inicia video",EpisodeFrameLoader.load(context,"cache-miss",400,225,true){error("nao deve resolver")}==null && Native.remoteCalls==prior)
  pass("logs identificam etapa sem mensagem privada ou URL",Log.messages.any{it.contains("etapa=fonte_remota")} && Log.messages.none{it.contains("secret") || it.contains("http") || it.contains("private")})
  pass("todas fontes nativas abertas foram liberadas",Native.releases==Native.remoteCalls+Native.localCalls)
 } finally {server.stop(0);dir.deleteRecursively()}
}
