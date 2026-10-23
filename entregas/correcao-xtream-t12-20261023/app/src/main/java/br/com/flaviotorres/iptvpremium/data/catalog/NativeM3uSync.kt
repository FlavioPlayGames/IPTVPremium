package br.com.flaviotorres.iptvpremium.data.catalog

import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.text.Normalizer

/** A single M3U stream feeds the stable catalogue; no section spools or whole-file lists. */
internal class NativeM3uSync(private val http:PlaylistHttpClient,private val cache:PlaylistCacheDao,
    private val dao:NativeCatalogDao,private val writer:Mutex) {
    suspend fun run(source:PlaylistSource.M3u,g:String,ready:suspend()->Unit,
        progress:suspend(Map<String,Long>,Boolean)->Unit) {
        val p=source.id;val run=UUID.randomUUID().toString();val kinds=listOf("LIVE_TV","MOVIES","SERIES","EPISODES")
        val prior=kinds.associateWith {when(it){"LIVE_TV"->dao.channelCount(p,g);"MOVIES"->dao.movieCount(p,g);"SERIES"->dao.seriesCount(p,g);else->dao.episodeCount(p,g)}}
        var opened=false;var pending:String?=null;var count=0L;var buffered=0;var textSize=0;var lastReport=0L
        val categories=kinds.associateWith{linkedMapOf<String,CachedCategoryEntity>()}
        val counts=kinds.associateWith{0L}.toMutableMap()
        val channels=ArrayList<CachedChannelEntity>();val movies=ArrayList<CachedMovieEntity>();val series=ArrayList<CachedSeriesEntity>();val episodes=ArrayList<CachedEpisodeEntity>()
        val seriesOrders=HashMap<String,Int>()
        suspend fun open() {
            if(opened)return
            ready();opened=true
            for(kind in kinds)dao.state(NativeCatalogState(p,g,kind,run,"RUNNING",0,System.currentTimeMillis()))
        }
        suspend fun flush() {
            if(buffered==0)return
            writer.withLock {
                val batches=listOf(NativeBatch(p,g,"LIVE_TV",run,channels=channels.toList()),NativeBatch(p,g,"MOVIES",run,movies=movies.toList()),
                    NativeBatch(p,g,"SERIES",run,series=series.toList()),NativeBatch(p,g,"EPISODES",run,episodes=episodes.toList()))
                for(batch in batches)if(batch.ids.isNotEmpty()) {
                    val result=dao.apply(batch,initial=prior.getValue(batch.kind)==0)
                    counts[batch.kind]=counts.getValue(batch.kind)+result.accepted
                }
            }
            channels.clear();movies.clear();series.clear();episodes.clear();buffered=0;textSize=0
            val now=android.os.SystemClock.elapsedRealtime()
            if(now-lastReport>1000){progress(counts.toMap(),false);lastReport=now}
        }
        try {
            http.readLines(source.playlistUrl) { raw->
                currentCoroutineContext().ensureActive()
                val line=raw.trim().removePrefix("\uFEFF")
                if(line.startsWith("#EXTM3U",true)){open();return@readLines}
                if(line.startsWith("#EXTINF",true)){open();pending=line;return@readLines}
                if(line.isBlank()||line.startsWith('#'))return@readLines
                val inf=pending?:return@readLines;pending=null
                require(line.startsWith("http://",true)||line.startsWith("https://",true)||line.startsWith("rtsp://",true)){"Endereço de mídia inválido."}
                count++
                val attrs=attribute.findAll(inf).associate{it.groupValues[1].lowercase() to it.groupValues[2]}
                val comma=displayComma(inf);val name=(if(comma>=0)inf.substring(comma+1).trim()else "").ifBlank{"Conteúdo"}
                val identityName=inf.substringAfterLast(',').trim().ifBlank{"Conteúdo"}
                val group=attrs["group-title"]?.trim()?.takeIf{it.isNotBlank()}?:"Sem categoria"
                val folded=Normalizer.normalize(group,Normalizer.Form.NFD).replace(marks,"").lowercase(java.util.Locale.ROOT)
                val episode=compact.find(name)?:verbose.find(name)
                val kind=when {
                    line.contains("/series/",true)||line.contains("/serie/",true)->"SERIES"
                    line.contains("/movie/",true)||line.contains("/movies/",true)||line.contains("/vod/",true)->"MOVIES"
                    line.contains("/live/",true)->"LIVE_TV"
                    "serie" in folded||"temporada" in folded||"season" in folded||episode!=null->"SERIES"
                    listOf("filme","movie","vod","cinema","lancamento").any{it in folded}->"MOVIES"
                    else->"LIVE_TV"
                }
                val cat=stable("category:$kind:$group");val cats=categories.getValue(kind)
                if(cat !in cats) {
                    val value=CachedCategoryEntity(p,g,cat,group,kind,"M3U")
                    cats[cat]=value;writer.withLock{cache.insertCategories(listOf(value))}
                }
                val logo=attrs["tvg-logo"]?.takeIf{it.isNotBlank()};val tvg=attrs["tvg-id"]?.takeIf{it.isNotBlank()}
                when(kind) {
                    "LIVE_TV"->channels+=CachedChannelEntity(p,g,stable("channel:$line:$identityName:${tvg.orEmpty()}"),name,cat,logo,line,tvg,(counts.getValue(kind)+channels.size+1).toInt())
                    "MOVIES"->movies+=CachedMovieEntity(p,g,stable("movie:$line:$identityName"),name,cat,logo,null,line,year.find(name)?.value?.toIntOrNull(),null,null,
                        providerOrder=(counts.getValue(kind)+movies.size).toInt())
                    else->{
                        val seriesName=(if(episode!=null)name.substring(0,episode.range.first).trim().trimEnd('-','|','_',' ')else name).ifBlank{name}
                        val id=stable("series:$group:$seriesName");val order=seriesOrders.getOrPut(id){seriesOrders.size}
                        series+=CachedSeriesEntity(p,g,id,seriesName,cat,logo,null,year.find(seriesName)?.value?.toIntOrNull(),null,null,null,null,null,null,null,null,null,order)
                        episodes+=CachedEpisodeEntity(p,g,stable("episode:$line:$identityName"),id,episode?.groupValues?.getOrNull(1)?.toIntOrNull(),episode?.groupValues?.getOrNull(2)?.toIntOrNull(),name,line,logo,null)
                    }
                }
                buffered++;textSize+=line.length+inf.length
                if(buffered>=400||textSize>=262144)flush()
            }
            check(opened){"A resposta não é uma lista M3U válida."}
            check(pending==null){"Lista M3U interrompida antes do endereço de mídia."}
            flush()
            check(count>0 || prior.values.sum()==0){"Lista vazia inesperada; catálogo anterior preservado."}
            writer.withLock {
                for(kind in kinds)dao.complete(p,g,kind,run,counts.getValue(kind),prior.getValue(kind),categories.getValue(kind).keys.toList(),full=true)
            }
            progress(counts.toMap(),true)
        }catch(error:Exception){
            withContext(NonCancellable){if(dao.isActive(p,g))for(kind in kinds)dao.state(NativeCatalogState(p,g,kind,run,"FAILED",counts.getValue(kind),System.currentTimeMillis()))}
            throw error
        }
    }
    private fun displayComma(text:String):Int {var quoted=false;for(i in text.indices){if(text[i]=='"')quoted=!quoted;if(text[i]==','&&!quoted)return i};return -1}
    private fun stable(value:String)=UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8)).toString()
    companion object {
        private val attribute=Regex("([A-Za-z0-9_-]+)\\s*=\\s*\"([^\"]*)\"")
        private val compact=Regex("(?i)\\bS(\\d{1,2})\\s*E(\\d{1,3})\\b")
        private val verbose=Regex("(?i)\\b(?:temporada|season)\\s*(\\d{1,2}).*?\\b(?:epis[oó]dio|episode|ep)\\s*(\\d{1,3})\\b")
        private val marks=Regex("\\p{M}+")
        private val year=Regex("\\b(?:19|20)\\d{2}\\b")
    }
}
