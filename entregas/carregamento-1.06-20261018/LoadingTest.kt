package br.com.flaviotorres.iptvpremium.data.repository
import kotlinx.coroutines.*
fun main()=runBlocking {
 fun pass(n:String,b:Boolean){check(b){n};println("PASS $n")}
 val ready=SeriesEpisodesResult("s",null,null,null,null,null,listOf(SeriesSeasonInfo(1,"Um",null,1)),listOf(SeriesEpisodeInfo("e",1,1,"Um",null,null,null,null)))
 val empty=ready.copy(episodes=emptyList())
 var remoteCalls=0
 val local=loadSeriesEpisodeContent(local={ready},remote={remoteCalls++;delay(1000);ready})
 pass("episodios locais aparecem sem esperar rede",local==ready && remoteCalls==0)
 val events=mutableListOf<String>()
 val remote=loadSeriesEpisodeContent(local={events.add("local");null},remote={events.add("remote");ready})
 pass("cache ausente consulta provedor na mesma abertura",remote==ready && events==listOf("local","remote"))
 val afterEmpty=loadSeriesEpisodeContent(local={empty},remote={ready})
 pass("cache vazio nao impede consulta remota",afterEmpty==ready)
 pass("resposta vazia nao e considerada pronta",loadSeriesEpisodeContent(local={null},remote={empty})==null)
 var cancelled=false
 try {loadSeriesEpisodeContent(local={null},remote={throw CancellationException("sair")})}catch(_:CancellationException){cancelled=true}
 pass("saida da pagina propaga cancelamento",cancelled)
 pass("imagem do provedor nao espera TMDB",episodeReferenceImages(null,"https://provider.invalid/still.jpg")==listOf("https://provider.invalid/still.jpg"))
 pass("foto TMDB continua prioritaria quando disponivel",episodeReferenceImages("https://tmdb.invalid/still.jpg","https://provider.invalid/still.jpg").first()=="https://tmdb.invalid/still.jpg")
 pass("URLs invalidas e repetidas nao geram consultas extras",episodeReferenceImages("https://provider.invalid/still.jpg"," https://provider.invalid/still.jpg ").size==1 && episodeReferenceImages("null",null).isEmpty())
}
