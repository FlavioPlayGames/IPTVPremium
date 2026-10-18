package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import kotlinx.coroutines.*
fun main()=runBlocking {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val shown=mutableListOf<String>()
 var requests=mutableListOf<String>()
 loadEpisodeThumbnail(listOf("tmdb","provider"), {url->requests.add(url);"photo"},{shown.add(it)})
 pass("foto especifica tem prioridade e encerra consulta",shown==listOf("photo") && requests==listOf("tmdb"))
 shown.clear();requests.clear()
 loadEpisodeThumbnail(listOf("bad","good"), {url->requests.add(url);if(url=="bad") null else "photo"},{shown.add(it)})
 pass("imagem indisponivel tenta proxima referencia",shown==listOf("photo") && requests==listOf("bad","good"))
 shown.clear()
 loadEpisodeThumbnail(listOf("bad","good"), {url->if(url=="bad") throw java.io.IOException() else "photo"},{shown.add(it)})
 pass("erro de imagem permite alternativa sem interromper card",shown==listOf("photo"))
 shown.clear();requests.clear()
 loadEpisodeThumbnail<String>(emptyList(), {url->requests.add(url);null},{shown.add(it)})
 pass("sem foto nao inicia extracao ou qualquer consulta de video",shown.isEmpty() && requests.isEmpty())
 var cancelled=false
 try {loadEpisodeThumbnail<String>(listOf("cancel"), {throw CancellationException()},{shown.add(it)})} catch(_:CancellationException){cancelled=true}
 pass("cancelamento e preservado ao sair da tela",cancelled)
 requests.clear()
 loadEpisodeThumbnail<String>(listOf("a","a"), {url->requests.add(url);null},{shown.add(it)})
 pass("referencias duplicadas consultadas apenas uma vez",requests==listOf("a"))
 val source=PlaylistSource.XtreamCodes("p","Fixture",serverUrl="https://example.invalid",username="test",password="test")
 val client=object:PlaylistHttpClient{
  override suspend fun get(url:String)="""{"episodes":{"1":[{"id":"1","info":{"movie_image":"0","cover":"https://fixture.invalid/cover.jpg"}},{"id":"2","thumbnail_url":"https://fixture.invalid/ep2.jpg"},{"id":"3","info":{"movie_image":"[]","thumbnail":"https://fixture.invalid/ep3.jpg"}},{"id":"4","still_path":"https://fixture.invalid/ep4.jpg"}]}}"""
  override suspend fun readLines(url:String,onLine:suspend(String)->Unit){}
 }
 val eps=SeriesEpisodesRepository(client).load(source,"123")!!.episodes
 pass("imagem invalida no primeiro campo nao esconde campo valido",eps[0].thumbnailUrl=="https://fixture.invalid/cover.jpg")
 pass("thumbnail_url na raiz do episodio reconhecida",eps[1].thumbnailUrl=="https://fixture.invalid/ep2.jpg")
 pass("thumbnail no info reconhecida apos valor invalido",eps[2].thumbnailUrl=="https://fixture.invalid/ep3.jpg")
 pass("still_path absoluto na raiz reconhecido",eps[3].thumbnailUrl=="https://fixture.invalid/ep4.jpg")
}
