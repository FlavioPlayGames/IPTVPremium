package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.data.remote.TmdbHttpClient
import java.io.File
import java.util.Properties
import kotlinx.coroutines.*
fun main()=runBlocking {
 val props=Properties().apply{File("/workspace/storage-work/local.properties").inputStream().use{load(it)}}
 val client=TmdbHttpClient(props.getProperty("TMDB_READ_TOKEN").trim())
 val root=withTimeout(15_000){resolveMovieArtworkIdentity("O Padrinho (1972)",1972,null,null,client::get)}
 check(root?.optInt("id")==238 && root.optString("title")=="O Poderoso Chefão")
 println("PASS API real: nome pt-PT identifica filme 238 e retorna titulo pt-BR")
 val names=preferredSeriesSearchNames(root!!,"movie")
 check(names.first()=="The Godfather" && names.last()=="O Poderoso Chefão")
 println("PASS API real: nomes original ingles pt-BR preservados sem repeticoes")
 check(root.optJSONObject("credits")!!.optJSONArray("cast")!!.length()>0 && root.optString("overview").isNotBlank())
 println("PASS API real: ficha inclui sinopse e elenco localizado")
 val spanish=withTimeout(12_000){resolveMovieArtworkIdentity("El laberinto del fauno",2006,"1417",null,client::get)}
 check(spanish?.optString("original_title")=="El laberinto del fauno" && spanish.optString("title")=="O Labirinto do Fauno")
 val spanishNames=preferredSeriesSearchNames(spanish!!,"movie")
 check(spanishNames.first()=="El laberinto del fauno" && spanishNames.count{it=="El laberinto del fauno"}==1 && spanishNames.last()=="O Labirinto do Fauno")
 println("PASS API real: filme de original espanhol retorna ficha pt-BR sem duplicar espanhol")
 val spanishSeries=withTimeout(12_000){resolveSeriesArtworkIdentity("La casa de papel",2017,"71446",null,client::get)}
 check(spanishSeries?.optString("original_name")=="La casa de papel")
 val seriesNames=preferredSeriesSearchNames(spanishSeries!!)
 println("DIAGNOSTICO nomes oficiais serie espanhola=$seriesNames localizado=${spanishSeries.optString("name")}")
 check(seriesNames.first()=="La casa de papel" && seriesNames.any{it=="Money Heist"} && spanishSeries.optString("overview").isNotBlank())
 println("PASS API real: serie de original espanhol identifica ingles e titulo brasileiro")
}
