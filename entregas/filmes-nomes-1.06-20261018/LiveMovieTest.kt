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
}
