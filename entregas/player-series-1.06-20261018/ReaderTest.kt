package br.com.flaviotorres.iptvpremium.data.repository
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import br.com.flaviotorres.iptvpremium.data.cache.categoryPosterBatch
import br.com.flaviotorres.iptvpremium.data.cache.CATEGORY_POSTER_BATCH_SIZE
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
fun main() {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val data=ByteArray(1024*1024){(it%251).toByte()};var calls=0
 val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
 server.createContext("/range") { e ->
  calls++;val range=e.requestHeaders.getFirst("Range").substringAfter("bytes=").split('-');val a=range[0].toInt();val b=minOf(range[1].toInt(),data.lastIndex)
  e.responseHeaders.add("Content-Range","bytes $a-$b/${data.size}");e.sendResponseHeaders(206,(b-a+1).toLong());e.responseBody.use{it.write(data,a,b-a+1)}
 }
 server.createContext("/full") { e -> e.sendResponseHeaders(200,data.size.toLong());e.responseBody.close() }
 server.start();val base="http://127.0.0.1:${server.address.port}"
 try {
  BoundedVideoReader("$base/range",blockSize=1024).use { r ->
   pass("tamanho do video obtido sem download completo",r.size()==data.size.toLong())
   val bytes=ByteArray(30);val n=r.readAt(900000,bytes,0,bytes.size)
   pass("leitura aleatoria retorna bytes corretos",n==30 && bytes.contentEquals(data.copyOfRange(900000,900030)))
   val old=calls;r.readAt(900010,bytes,0,10);pass("bloco ja lido reutiliza cache pequeno",calls==old)
   pass("fim do arquivo nao gera nova requisicao",r.readAt(data.size.toLong(),bytes,0,30)==-1)
  }
  val full=runCatching { BoundedVideoReader("$base/full").use { it.size() } }
  pass("servidor sem Range nao provoca download integral",full.isFailure)
  val limited=runCatching { BoundedVideoReader("$base/range",maxBytes=1024,blockSize=1024).use { val b=ByteArray(2);it.readAt(0,b,0,2);it.readAt(5000,b,0,2) } }
  pass("orcamento de bytes interrompe novas leituras",limited.isFailure)
  val cancelled=runCatching { BoundedVideoReader("$base/range",active={false}).use { it.size() } }
  pass("cancelamento impede acesso ao provedor",cancelled.isFailure)
  val seasons=listOf(SeriesSeasonInfo(1,"Um",null,9),SeriesSeasonInfo(2,"Vazia",null,8))
  val episodes=listOf(SeriesEpisodeInfo("e1",1,1,"Um",null,null,null,null))
  val shown=playableSeriesSeasons(seasons,episodes)
  pass("temporada declarada mas sem episodio real fica oculta",shown.size==1 && shown[0].number==1 && shown[0].episodeCount==1)
  pass("sem episodios nao mostra temporada informativa",playableSeriesSeasons(seasons,emptyList()).isEmpty())
  pass("abertura prioriza primeiro bloco de 15",categoryPosterBatch(0)==0 && CATEGORY_POSTER_BATCH_SIZE==15)
  pass("proxima linha antecipa bloco 15 a 29",categoryPosterBatch(10)==1)
  pass("navegacao longa usa blocos seguintes",categoryPosterBatch(25)==2 && categoryPosterBatch(40)==3)
  pass("voltar ao inicio retoma primeiro bloco",categoryPosterBatch(-1)==0 && categoryPosterBatch(0)==0)
  runBlocking {
   val client=object:PlaylistHttpClient {
    override suspend fun get(url:String)="""{"seasons":[{"season_number":1},{"season_number":2}],"episodes":{"1":[{"id":"e1","episode_num":1,"season":1,"title":"Um"}],"2":[]}}"""
    override suspend fun readLines(url:String,onLine:suspend(String)->Unit){}
   }
   val result=SeriesEpisodesRepository(client).load(PlaylistSource.XtreamCodes("p","Fixture",serverUrl="https://example.invalid",username="test",password="test"),"123")
   pass("parser ignora temporada vazia informada pelo provedor",result?.seasons?.map { it.number }==listOf(1))
  }
 } finally { server.stop(0) }
}
