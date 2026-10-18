package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.data.parser.*
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.*
import kotlinx.coroutines.*
fun main()=runBlocking {
 var calls=0
 val http=object:PlaylistHttpClient {
  override suspend fun get(url:String):String { calls++;return """{"info":{"name":"O Padrinho","year":"1972","tmdb":"238","plot":"provider-english-fixture"},"movie_data":{"stream_id":77,"name":"O Padrinho"}}""" }
  override suspend fun readLines(url:String,onLine:suspend(String)->Unit) {error("not used")}
 }
 val repo=DefaultPlaylistRepository(http,DefaultM3uParser(),DefaultXtreamParser())
 val source=PlaylistSource.XtreamCodes("fixture","fixture",serverUrl="https://example.test",username="fixture",password="fixture")
 val result=withTimeout(20_000){repo.loadMovieDetails(source,"77")}
 check(result?.movieId=="77" && result.tmdbId=="238" && result.localizedTitle=="O Poderoso Chefão")
 println("PASS repositorio real enriquece filme Xtream por tmdb e preserva ID reproduzivel")
 check(result?.providerTitle=="O Padrinho" && result.plot?.isNotBlank()==true && result.plot!="provider-english-fixture" && result.castMembers.isNotEmpty())
 println("PASS repositorio real preserva nome da lista e usa sinopse pt-BR e elenco")
 check(calls==1);println("PASS enriquecimento nao repete consulta do provedor nem acessa video")
 var lookups=0
 val local=DefaultPlaylistRepository(http,DefaultM3uParser(),DefaultXtreamParser(),movieLookup={_,id->
  lookups++;Movie(id,"O Padrinho",null,null,null,"https://example.test/video",1972,null,null)
 })
 val native=withTimeout(20_000){local.loadMovieDetails(PlaylistSource.M3u("local","local",playlistUrl="https://example.test/playlist.m3u"),"local-77")}
 check(native?.movieId=="local-77" && native.localizedTitle=="O Poderoso Chefão" && lookups==1 && calls==1)
 println("PASS M3U nativa usa somente item local e mantem ID sem consultar Xtream ou videos")
}
