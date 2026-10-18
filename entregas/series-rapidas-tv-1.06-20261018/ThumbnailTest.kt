package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import kotlinx.coroutines.*
fun main()=runBlocking {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val shown=mutableListOf<String>();var extracted=0
 suspend fun resolve(images:List<String>,ready:Boolean=true,cached:String?=null, photo:suspend(String)->String?={null}) {
  loadEpisodeThumbnail(images,ready,{cached},photo,{extracted++;"frame-real"},{shown.add(it)})
 }
 resolve(listOf("ok"),photo={"foto-existente"})
 pass("foto existente permanece e nao extrai video",shown==listOf("foto-existente") && extracted==0)
 shown.clear();resolve(listOf("bad"));pass("ausencia de foto usa frame do proprio episodio",shown==listOf("frame-real") && extracted==1)
 shown.clear();val before=extracted;resolve(listOf("bad"),cached="frame-cache")
 pass("frame em cache aparece sem extrair novamente",shown==listOf("frame-cache") && extracted==before)
 shown.clear();resolve(listOf("ok"),cached="frame-cache",photo={"foto-oficial"})
 pass("foto oficial substitui somente frame provisorio em cache",shown==listOf("frame-cache","foto-oficial") && extracted==before)
 shown.clear();resolve(emptyList(),ready=false);pass("aguarda verificacao de imagens antes de extrair",shown.isEmpty() && extracted==before)
 var cancel=false
 try { resolve(listOf("bad"),photo={throw CancellationException("saiu")}) }catch(_:CancellationException){cancel=true}
 pass("sair da tela cancela antes da extracao",cancel && extracted==before)
 shown.clear()
 loadEpisodeThumbnail<String>(emptyList(),true,{null},{null},{null},{shown.add(it)})
 pass("falha de extracao nao inventa foto nem substitui por arte generica",shown.isEmpty())
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
