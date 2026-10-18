package br.com.flaviotorres.iptvpremium.data.repository
import kotlinx.coroutines.*
import org.json.JSONObject
import br.com.flaviotorres.iptvpremium.ui.movies.MovieOpeningPolicy
fun main()=runBlocking {
 fun ok(n:String,b:Boolean){check(b){n};println("PASS $n")}
 fun fixture(kind:String, original:String="Original"):JSONObject {
 val field=if(kind=="movie")"title" else "name"
 return JSONObject().put(if(kind=="movie")"original_title" else "original_name",original).put(field,"Brasileiro").put("translations",JSONObject("""{"translations":[{"iso_639_1":"en","iso_3166_1":"US","data":{"$field":"English"}},{"iso_639_1":"es","iso_3166_1":"ES","data":{"$field":"Español"}},{"iso_639_1":"pt","iso_3166_1":"BR","data":{"$field":"Brasileiro"}}]}"""))
 }
 for(kind in listOf("movie","tv")) {
 val root=fixture(kind)
 ok("$kind nomes original ingles espanhol brasileiro",preferredSeriesSearchNames(root,kind)==listOf("Original","English","Español","Brasileiro"))
 ok("$kind original espanhol nao repete busca",preferredSeriesSearchNames(fixture(kind,"Español"),kind)==listOf("Español","English","Brasileiro"))
 root.getJSONObject("translations").getJSONArray("translations").getJSONObject(1).put("iso_3166_1","MX")
 ok("$kind espanhol mexicano aceito sem es-ES",preferredSeriesSearchNames(root,kind)[2]=="Español")
 }
 val queries=mutableListOf<String>()
 val load:suspend(String,String,Map<String,String>)->String?={service,path,q->
 check(service=="tvmaze");queries+=q["q"].orEmpty()
 if(q["q"]=="brasileiro")"""[{"show":{"id":11,"name":"Brasileiro","premiered":"2026-01-01"}}]""" else "[]"
 }
 val art=SeriesArtworkFallback(load,false).load(ArtworkIdentity(listOf("Original","English","Español","Brasileiro"),2026),1,emptySet(),false,false)
 ok("TVmaze tenta quatro nomes na ordem e encontra brasileiro",art.matched && queries==listOf("original","english","espanol","brasileiro"))
 val locales=mutableListOf<String>()
 resolveMovieArtworkIdentity("Desconocida",2026,null,null){_,q->locales+=q["language"].orEmpty();"{\"results\":[]}"}
 ok("TMDB inclui locale espanhol antes do brasileiro",locales==listOf("en-US","es-ES","pt-BR","pt-PT"))
 ok("arte pronta dispensa espera minima",MovieOpeningPolicy.minimumWaitMs(100,true)==0L)
 ok("arte ausente aguarda dois segundos",MovieOpeningPolicy.minimumWaitMs(500,false)==1500L)
 ok("prazo passado nao soma outra espera",MovieOpeningPolicy.minimumWaitMs(3500,false)==0L)
 ok("imagens recebem apenas tempo restante",MovieOpeningPolicy.remainingMs(2500)==1000L && MovieOpeningPolicy.remainingMs(9000)==0L)
 val total=MovieOpeningPolicy.preparationMs+(MovieOpeningPolicy.alignNanos+MovieOpeningPolicy.pressNanos+MovieOpeningPolicy.expandNanos)/1_000_000L
 ok("preparo e animacao juntos cabem em cinco segundos",total<=5000L && total>=2000L)
}
