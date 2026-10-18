package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.data.history.ContentIdentity
import kotlinx.coroutines.runBlocking
fun main()=runBlocking {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val paths=mutableListOf<String>()
 val resolver=TitleAliasResolver { path, query ->
  paths.add(path)
  when {
   path=="/search/tv" -> { check(query["language"]=="en-US");"""{"results":[{"id":999001,"name":"Digimon Data Squad","original_name":"デジモンセイバーズ","first_air_date":"2006-04-02"}]}""" }
   path=="/tv/999001" -> """{"id":999001,"name":"Digimon Data Squad","original_name":"デジモンセイバーズ","first_air_date":"2006-04-02","external_ids":{"imdb_id":"tt1234567"},"alternative_titles":{"results":[{"title":"Digimon Savers"}]}}"""
   else -> error(path)
  }
 }
 val source=ContentIdentity("series","Digimon Savers 4K HDR",2006)
 val result=resolver.resolve(source)
 pass("nomes oficiais ingles original e alternativo aprendidos sem traduzir a interface",result?.title==source.title && result.alternateTitles.containsAll(listOf("Digimon Data Squad","Digimon Savers","デジモンセイバーズ")))
 pass("identificadores reais da resposta associados a serie confirmada",result?.tmdb=="999001" && result.imdb=="tt1234567")
 pass("ano divergente nao associa remake",resolver.resolve(source.copy(year=2020))==null)
 pass("resultado parecido sem nome exato nao associa",resolver.resolve(ContentIdentity("series","Digimon Adventure",2006))==null)
 paths.clear();resolver.resolve(source.copy(tmdb="999001"))
 pass("ID conhecido dispensa pesquisa por nome",paths==listOf("/tv/999001"))
 val ambiguous=TitleAliasResolver { path,_ -> if(path.startsWith("/search")) """{"results":[{"id":1},{"id":2}]}""" else """{"id":${path.substringAfterLast('/')},"name":"Same","first_air_date":"2000-01-01"}""" }
 pass("busca ambigua nao escolhe primeiro por popularidade",ambiguous.resolve(ContentIdentity("series","Same",2000))==null)
 val movie=TitleAliasResolver { path,_ -> if(path.startsWith("/search")) """{"results":[{"id":7,"title":"Spider-Man","original_title":"Spider-Man","release_date":"2002-01-01"}]}""" else """{"id":7,"title":"Spider-Man","original_title":"Spider-Man","release_date":"2002-01-01","imdb_id":"tt0145487","alternative_titles":{"titles":[{"title":"Homem-Aranha"}]}}""" }
 val film=movie.resolve(ContentIdentity("movies","Homem-Aranha [D]",2002))
 pass("filmes tambem comparam nome em ingles e portugues",film?.confidence(ContentIdentity("movies","Spider-Man 4K",2002))==1)
 val conflict=resolver.resolve(source.copy(imdb="tt9999999",tmdb="999001"))
 pass("identificador IMDb conflitante recusa metadados",conflict==null)
}
