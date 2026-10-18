package br.com.flaviotorres.iptvpremium.data.repository
import kotlinx.coroutines.*
import org.json.JSONObject
private fun ok(name:String, value:Boolean) { check(value) { name }; println("PASS $name") }
private val show="""{"id":5,"name":"The Show","premiered":"2026-01-01","externals":{"imdb":"tt123","thetvdb":15}}"""
private val eps="""[{"season":1,"number":1,"image":{"original":"https://img/one"}},{"season":2,"number":1,"image":{"original":"https://img/two"}},{"season":1,"number":2,"image":null}]"""
fun main()= runBlocking {
 ok("ordem provedor TMDB TVmaze preservada",episodeReferenceImages("https://tmdb", "https://lista", "https://tvmaze")==listOf("https://lista","https://tmdb","https://tvmaze"))
 ok("URL repetida nao duplica download",episodeReferenceImages("https://a", "https://a", "https://a")==listOf("https://a"))
 val calls=mutableListOf<String>()
 val repo=SeriesArtworkFallback({service,path,_ -> calls += "$service$path"; when(path) {
  "/lookup/shows"->show; "/shows/5/episodes"->eps; "/tv/15"->"""{"hdtvlogo":[{"url":"https://img/en","lang":"en"},{"url":"https://img/pt","lang":"pt"}],"showbackground":[{"url":"https://img/bg"}]}""";else->null
 } },true)
 val r=repo.load(ArtworkIdentity(listOf("Outro nome"),null,"tt123"),1,setOf(1,2),true,true)
 ok("IMDb exato associa sem depender de nome ou ano",r.matched)
 ok("numero de episodio e temporada exatos",r.episodeStills==mapOf(1 to "https://img/one"))
 ok("Fanart por TVDB confirmado e logo portuguesa prioritaria",r.logoUrls==listOf("https://img/pt","https://img/en") && r.backdropUrl=="https://img/bg")
 calls.clear();repo.load(ArtworkIdentity(listOf("The Show"),2026,"tt123"),2,emptySet(),false,false)
 ok("nao consulta episodios ou Fanart sem lacunas",calls==listOf("tvmaze/lookup/shows"))
 val noKeyCalls=mutableListOf<String>()
 val noKey=SeriesArtworkFallback({_,path,_->noKeyCalls+=path; if(path=="/lookup/shows")show else eps},false)
 val nr=noKey.load(ArtworkIdentity(emptyList(),null,"tt123"),1,setOf(1),true,true)
 ok("TVmaze funciona sem chave Fanart",nr.episodeStills.isNotEmpty() && nr.logoUrls.isEmpty() && "/tv/15" !in noKeyCalls)
 val byName=SeriesArtworkFallback({_,path,_->if(path=="/search/shows") "[{\"show\":$show}]" else eps},false)
 ok("busca nome normalizado e ano exato",byName.load(ArtworkIdentity(listOf("The Show (2026) [L] 4K HDR"),2026),1,setOf(1),false,false).matched)
 ok("ano divergente rejeitado",!byName.load(ArtworkIdentity(listOf("The Show"),2025),1,setOf(1),false,false).matched)
 ok("nome sem ano nao identifica por suposicao",!byName.load(ArtworkIdentity(listOf("The Show"),null),1,setOf(1),false,false).matched)
 val ambiguous=SeriesArtworkFallback({_,_,_->"[{\"show\":$show},{\"show\":${show.replace("\"id\":5","\"id\":6")}}]"},false)
 ok("dois shows com mesmo nome e ano sao rejeitados",!ambiguous.load(ArtworkIdentity(listOf("The Show"),2026),1,setOf(1),false,false).matched)
 val conflicting=SeriesArtworkFallback({_,path,_->if(path=="/lookup/shows")show else "[{\"show\":$show}]"},false)
 ok("IMDb conflitante nao e substituido por busca de nome",!conflicting.load(ArtworkIdentity(listOf("The Show"),2026,"tt999"),1,setOf(1),true,true).matched)
 ok("numeracao duplicada nao atribui foto ao episodio errado",parseTvmazeEpisodeImages("[$eps]",1,setOf(1)).isEmpty())
 val duplicate="""[{"season":1,"number":1,"image":{"original":"https://a"}},{"season":1,"number":1,"image":{"original":"https://b"}}]"""
 ok("duplicatas reais rejeitadas",parseTvmazeEpisodeImages(duplicate,1,setOf(1)).isEmpty())
 ok("resposta malformada nao cria imagens",parseTvmazeEpisodeImages("<html>",1,setOf(1)).isEmpty())
 ok("imagem ausente nao usa poster da serie como foto oficial",parseTvmazeEpisodeImages(eps,1,setOf(2)).isEmpty())
 val broken=SeriesArtworkFallback({_,_,_->error("falha temporaria")},true)
 ok("erro de rede alternativo nao quebra ficha",!broken.load(ArtworkIdentity(listOf("The Show"),2026),1,setOf(1),true,true).matched)
 val canceled=SeriesArtworkFallback({_,_,_->throw CancellationException()},false)
 ok("cancelamento preservado",runCatching { canceled.load(ArtworkIdentity(emptyList(),null,"tt123"),1,setOf(1),true,true) }.exceptionOrNull() is CancellationException)
 ok("URLs invalidas ou locais nao viram imagens",prioritizedFanartImages(JSONObject("""{"clearlogo":[{"url":"file:///bad"},{"url":"null"}]}"""),listOf("clearlogo")).isEmpty())
}
