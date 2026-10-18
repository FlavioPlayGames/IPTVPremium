package br.com.flaviotorres.iptvpremium.data.repository
import kotlinx.coroutines.*
import org.json.JSONObject
fun main()=runBlocking {
 val root="""{"id":238,"title":"O Poderoso Chefão","original_title":"The Godfather","release_date":"1972-03-14","imdb_id":"tt0068646","translations":{"translations":[{"iso_639_1":"pt","iso_3166_1":"PT","data":{"title":"O Padrinho"}},{"iso_639_1":"pt","iso_3166_1":"BR","data":{"title":"O Poderoso Chefão"}},{"iso_639_1":"en","iso_3166_1":"US","data":{"title":"The Godfather"}}]},"alternative_titles":{"titles":[{"title":"Padrinho Oficial"}]}}"""
 val search="""{"results":[{"id":238,"title":"O Poderoso Chefão","original_title":"The Godfather","release_date":"1972-03-14"}]}"""
 val calls=mutableListOf<Pair<String,Map<String,String>>>()
 val get:suspend(String,Map<String,String>)->String={path,q-> calls+=path to q; when(path){"/search/movie"->search;"/movie/238"->root;"/find/tt0068646"->"""{"movie_results":[{"id":238}]}""";else->error("unexpected")}}
 fun ok(name:String,value:Boolean){check(value){name};println("PASS $name")}
 val result=resolveMovieArtworkIdentity("O Padrinho (1972) [L] 4K",1972,null,null,get)
 ok("filme identificado por traducao oficial pt-PT",result?.optInt("id")==238)
 ok("filme devolve titulo pt-BR",result?.optString("title")=="O Poderoso Chefão")
 ok("apenas endpoints de filmes e detalhes pt-BR",calls.all{it.first=="/search/movie" || it.first=="/movie/238"} && calls.filter{it.first=="/movie/238"}.all{it.second["language"]=="pt-BR"})
 ok("ordem original ingles pt-BR sem duplicar original igual ao ingles",preferredSeriesSearchNames(JSONObject(root),"movie")==listOf("The Godfather","O Poderoso Chefão"))
 val british=JSONObject(root).put("original_title","Der Pate")
 val ts=british.optJSONObject("translations")!!.optJSONArray("translations")!!
 for(i in 0 until ts.length()) if(ts.optJSONObject(i)!!.optString("iso_639_1")=="en") ts.optJSONObject(i)!!.put("iso_3166_1","GB")
 ok("ingles britanico usado quando nao ha traducao dos EUA",preferredSeriesSearchNames(british,"movie")==listOf("Der Pate","The Godfather","O Poderoso Chefão"))
 ok("titulo original identifica filme",resolveMovieArtworkIdentity("The Godfather",1972,null,null,get)?.optInt("id")==238)
 ok("titulo brasileiro identifica filme",resolveMovieArtworkIdentity("O Poderoso Chefão",1972,null,null,get)?.optInt("id")==238)
 ok("titulo alternativo oficial identifica filme",resolveMovieArtworkIdentity("Padrinho Oficial",1972,null,null,get)?.optInt("id")==238)
 ok("ano de remake diferente rejeitado",resolveMovieArtworkIdentity("O Padrinho",2000,null,null,get)==null)
 calls.clear();ok("IMDb evita busca de nomes",resolveMovieArtworkIdentity("Outro",null,null,"tt0068646",get)?.optInt("id")==238 && calls.none{it.first.startsWith("/search")})
 val cancel:suspend(String,Map<String,String>)->String={_,_->throw CancellationException()}
 ok("cancelamento de filme preservado",runCatching{resolveMovieArtworkIdentity("Filme",1972,null,null,cancel)}.exceptionOrNull() is CancellationException)
 val fanartCalls=mutableListOf<String>()
 val fanGet:suspend(String,String,Map<String,String>)->String?={service,path,_->fanartCalls+="$service$path";"""{"tmdb_id":"238","imdb_id":"tt0068646","hdmovielogo":[{"url":"https://logo/en","lang":"en"},{"url":"https://logo/pt","lang":"pt"}],"moviebackground":[{"url":"https://bg"}]}"""}
 val art=loadMovieFanartArtwork("238","tt0068646",fanGet)
 ok("Fanart movie por ID confirmado sem consultar TVmaze",fanartCalls==listOf("fanart/movies/238"))
 ok("logo Fanart em portugues tem prioridade",art.logos.firstOrNull()=="https://logo/pt" && art.backdrops==listOf("https://bg"))
 fanartCalls.clear();loadMovieFanartArtwork(null,null,fanGet);ok("sem ID nao adivinha filme no Fanart",fanartCalls.isEmpty())
 val fallbackCalls=mutableListOf<String>();val secondary:suspend(String,String,Map<String,String>)->String?={_,path,_->fallbackCalls+=path;if(path.endsWith("238"))null else """{"tmdb_id":"238","imdb_id":"tt0068646","movielogo":[{"url":"https://logo/ok"}]}"""}
 ok("Fanart tenta IMDb se TMDB nao retorna arte",loadMovieFanartArtwork("238","tt0068646",secondary).logos.isNotEmpty() && fallbackCalls==listOf("/movies/238","/movies/tt0068646"))
 val bad:suspend(String,String,Map<String,String>)->String?={_,_,_->"""{"tmdb_id":"999","movielogo":[{"url":"https://wrong"}]}"""}
 ok("Fanart com ID conflitante nao fornece arte",loadMovieFanartArtwork("238",null,bad).logos.isEmpty())
 val cancelArt:suspend(String,String,Map<String,String>)->String?={_,_,_->throw CancellationException()}
 ok("cancelamento de Fanart preservado",runCatching{loadMovieFanartArtwork("238",null,cancelArt)}.exceptionOrNull() is CancellationException)
}
