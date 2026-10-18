package br.com.flaviotorres.iptvpremium.data.repository
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
fun main(args:Array<String>)=runBlocking {
 val root=JSONObject(File(args[0],"details-fixture.json").readText())
 val search=File(args[0],"search-fixture.json").readText()
 val calls=mutableListOf<Pair<String,Map<String,String>>>()
 val get:suspend(String,Map<String,String>)->String={path,q->
  calls+=path to q
  when(path){"/search/tv"->search;"/tv/283371"->root.toString();"/find/tt36470958"->"""{"tv_results":[{"id":283371}]}""";else->"""{"id":326991,"name":"Outra Serie","original_name":"Other","first_air_date":"2026-08-10"}"""}
 }
 fun ok(name:String,value:Boolean){check(value){name};println("PASS $name")}
 val result=resolveSeriesArtworkIdentity("A Armadilha (2026) [L] 4K",2026,null,null,get)
 ok("caso real reconhece traducao pt-PT sem rejeitar pt-BR",result?.optInt("id")==283371)
 ok("ficha retornada continua em portugues brasileiro",result?.optString("name")=="Armadilha")
 ok("todas consultas de detalhes pedem pt-BR",calls.filter{it.first.startsWith("/tv/")}.all{it.second["language"]=="pt-BR"})
 ok("ordem original ingles espanhol portugues brasileiro",preferredSeriesSearchNames(root)==listOf("Die Falle","The Trap","La trampa","Armadilha"))
 ok("original alem de todas traducoes oficiais disponivel",officialSeriesNames(root).containsAll(listOf("Die Falle","The Trap","A Armadilha","Armadilha")))
 calls.clear();ok("nome original encontra mesma producao",resolveSeriesArtworkIdentity("Die Falle",2026,null,null,get)?.optInt("id")==283371)
 ok("nao pesquisa outras linguas apos correspondencia unica",calls.count{it.first=="/search/tv"}==1)
 ok("nome ingles encontra mesma producao",resolveSeriesArtworkIdentity("The Trap",2026,null,null,get)?.optInt("id")==283371)
 ok("ano divergente nao confunde producoes",resolveSeriesArtworkIdentity("A Armadilha",2025,null,null,get)==null)
 ok("sem ano nao adivinha por traducao",resolveSeriesArtworkIdentity("A Armadilha",null,null,null,get)==null)
 calls.clear();ok("ID TMDB evita buscas por nome",resolveSeriesArtworkIdentity("Qualquer",null,"283371",null,get)?.optInt("id")==283371 && calls.size==1)
 calls.clear();ok("IMDb confirma ID antes de buscar nomes",resolveSeriesArtworkIdentity("Qualquer",null,null,"tt36470958",get)?.optInt("id")==283371 && calls.none{it.first=="/search/tv"})
 val ambiguous:suspend(String,Map<String,String>)->String={path,_-> if(path=="/search/tv")search else JSONObject(root.toString()).put("id",path.substringAfterLast('/').toInt()).toString() }
 ok("duas obras com mesmo alias e ano sao rejeitadas",resolveSeriesArtworkIdentity("A Armadilha",2026,null,null,ambiguous)==null)
 ok("titulo apenas semelhante nao e aceito",resolveSeriesArtworkIdentity("Armadilha Mortal",2026,null,null,get)==null)
 val canceled:suspend(String,Map<String,String>)->String={_,_->throw CancellationException()}
 ok("cancelamento e preservado",runCatching{resolveSeriesArtworkIdentity("A Armadilha",2026,null,null,canceled)}.exceptionOrNull() is CancellationException)
 val failed:suspend(String,Map<String,String>)->String={_,_->error("indisponivel")}
 ok("falha de rede nao inventa correspondencia",resolveSeriesArtworkIdentity("A Armadilha",2026,null,null,failed)==null)
}
