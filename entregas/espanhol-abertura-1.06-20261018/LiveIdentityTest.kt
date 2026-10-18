package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.data.remote.TmdbHttpClient
import java.io.File
import java.util.Properties
import kotlinx.coroutines.*
fun main()=runBlocking {
 val props=Properties().apply{File("/workspace/storage-work/local.properties").inputStream().use{load(it)}}
 val client=TmdbHttpClient(props.getProperty("TMDB_READ_TOKEN").trim())
 val root=withTimeout(12_000){resolveSeriesArtworkIdentity("A Armadilha (2026)",2026,null,null,client::get)}
 check(root?.optInt("id")==283371){"Identificacao real falhou"}
 check(root?.optString("name")=="Armadilha")
 check(preferredSeriesSearchNames(root!!)==listOf("Die Falle","The Trap","La trampa","Armadilha"))
 println("PASS API real: A Armadilha identificada como TMDB 283371 com ficha pt-BR")
 println("PASS API real: nomes na ordem Die Falle, The Trap, La trampa, Armadilha")
 val logos=root.optJSONObject("images")?.optJSONArray("logos")!!
 check((0 until logos.length()).any{logos.optJSONObject(it)?.optString("iso_639_1")=="pt"})
 println("PASS API real: identificacao devolve logo em portugues")
 val season=org.json.JSONObject(client.get("/tv/283371/season/1",mapOf("language" to "pt-BR")))
 val episodes=season.optJSONArray("episodes")!!
 check(episodes.length()==6 && (0 until episodes.length()).all{episodes.optJSONObject(it)!!.optString("still_path").startsWith("/")})
 check(episodes.optJSONObject(0)!!.optString("name")=="O monstro")
 println("PASS API real: seis miniaturas e nome de episodio em portugues")
}
