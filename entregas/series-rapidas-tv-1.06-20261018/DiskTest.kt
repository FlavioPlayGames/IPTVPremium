package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.io.File
fun main()=runBlocking {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val dir=Files.createTempDirectory("series-disk").toFile();var clock=System.currentTimeMillis();var calls=0
 var response="""{"info":{"name":"Serie","plot":"Plot","backdrop_path":["https://example.invalid/backdrop"],"cover":"https://example.invalid/poster","youtube_trailer":"abc"},"episodes":{"1":[{"id":"42","episode_num":3,"season":1,"container_extension":"mkv","title":"Teste","info":{"movie_image":"https://example.invalid/still","plot":"Episodio","duration_secs":120}}]}}"""
 val http=object:PlaylistHttpClient {override suspend fun get(url:String):String{calls++;return response};override suspend fun readLines(url:String,onLine:suspend(String)->Unit){}}
 val source=PlaylistSource.XtreamCodes("p","Fixture",serverUrl="https://example.invalid",username="private_user",password="private_password")
 fun repo()=SeriesEpisodesRepository(http,diskCache=SeriesEpisodeDiskCache(dir,{clock}))
 try {
  val first=repo().load(source,"1","g1")!!
  pass("primeira consulta valida e guardada",calls==1 && dir.listFiles()!!.size==1)
  val second=repo().load(source,"1","g1")!!
  pass("nova instancia abre temporadas sem rede",calls==1 && first==second)
  pass("formato e thumbnail especifica sobrevivem ao cache",second.episodes.single().containerExtension=="mkv" && second.episodes.single().thumbnailUrl=="https://example.invalid/still")
  pass("nomes de arquivos nao revelam credenciais",dir.listFiles()!!.all{it.name.matches(Regex("[0-9a-f]{64}\\.json"))} && dir.listFiles()!!.none{it.readText().contains("private_")})
  repo().load(source,"1","g2");pass("nova geracao nao reutiliza temporadas antigas",calls==2)
  repo().load(source.copy(password="changed"),"1","g2");pass("troca de credenciais invalida a chave",calls==3)
  clock+=6*60*60*1000L+1;repo().load(source,"1","g1");pass("apos seis horas faz consulta nova",calls==4)
  dir.listFiles()!!.forEach{it.writeText("corrupt")};repo().load(source,"1","g1");pass("arquivo corrompido recupera pela rede",calls==5)
  dir.deleteRecursively();repo().load(source,"1","g1");pass("limpar cache permite recriar detalhes",calls==6 && dir.isDirectory)
  val none=File(dir,"empty");response="""{"episodes":{}}"""
  SeriesEpisodesRepository(http,diskCache=SeriesEpisodeDiskCache(none)).load(source,"1","g1")
  pass("resposta vazia nao fica gravada como definitiva",!none.exists())
  val small=File(dir,"bounded");val bounded=SeriesEpisodeDiskCache(small,{clock},maxBytes=12,maxEntries=2)
  bounded.write("a","aaaa");clock++;bounded.write("b","bbbb");clock++;bounded.write("c","cccc")
  pass("limite de entradas remove a mais antiga",bounded.read("a")==null && small.listFiles()!!.size==2)
  bounded.write("large","x".repeat(13));pass("item maior que orcamento nao remove pequenos",small.listFiles()!!.size==2)
  val cap=SeriesEpisodeDiskCache(File(dir,"byte-cap"),maxBytes=7,maxEntries=40)
  cap.write("a","aaaa");cap.write("b","bbbb");pass("limite de bytes e respeitado",File(dir,"byte-cap").listFiles()!!.sumOf{it.length()}<=7)
 } finally {dir.deleteRecursively()}
}
