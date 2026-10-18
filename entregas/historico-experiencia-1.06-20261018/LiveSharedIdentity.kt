package br.com.flaviotorres.iptvpremium.data.repository
import br.com.flaviotorres.iptvpremium.data.history.ContentIdentity
import br.com.flaviotorres.iptvpremium.data.remote.TmdbHttpClient
import kotlinx.coroutines.*
import java.io.File
import java.util.Properties
fun main()=runBlocking {
 val p=Properties().apply {File("/workspace/storage-work/local.properties").inputStream().use{load(it)}}
 val client=TmdbHttpClient(p.getProperty("TMDB_READ_TOKEN").trim())
 val calls=mutableListOf<Pair<String,Map<String,String>>>()
 val resolver=TitleAliasResolver { path,q->calls+=path to q;client.get(path,q) }
 val trap=withTimeout(12_000){resolver.resolve(ContentIdentity("series","A Armadilha (2026)",2026))}!!
 check(trap.tmdb=="283371" && trap.alternateTitles.take(4)==listOf("Die Falle","The Trap","La trampa","Armadilha"))
 check(trap.confidence(ContentIdentity("series","La trampa 4K",2026))>0)
 println("PASS API real: vinculo de serie aprende original ingles espanhol brasileiro na mesma regra das artes")
 check(calls.filter { it.first.startsWith("/tv/") }.all { it.second["language"]=="pt-BR" })
 println("PASS API real: metadados do historico tambem solicitam pt-BR")
 val film=withTimeout(12_000){resolver.resolve(ContentIdentity("movies","El laberinto del fauno",2006,tmdb="1417"))}!!
 check(film.alternateTitles.first()=="El laberinto del fauno" && film.alternateTitles.contains("O Labirinto do Fauno"))
 check(film.confidence(ContentIdentity("movies","O Labirinto do Fauno 4K HDR",2006))>0)
 println("PASS API real: filme de titulo espanhol compartilha com nome brasileiro oficial")
}
