package br.com.flaviotorres.iptvpremium.data.catalog

import android.os.SystemClock
import android.util.Log
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistAccessDeniedException
import br.com.flaviotorres.iptvpremium.data.repository.XtreamAuthenticationRecovery
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID

/** Public UI state is deliberately small and contains no addresses, credentials or provider bodies. */
data class NativeCatalogProgress(val playlistId:String="",val running:Boolean=false,val revision:Long=0,
    val counts:Map<String,Long> = emptyMap(),val errors:Set<String> = emptySet(),val complete:Set<String> = emptySet())

/** Test 1: new lifecycle and writer; the generation column is a stable read namespace, not a snapshot. */
class NativeXtreamCatalog internal constructor(
    private val http:PlaylistHttpClient,private val cache:PlaylistCacheDao,private val dao:NativeCatalogDao,
    private val scope:CoroutineScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
) {
    private class Run(val source:PlaylistSource,val generation:String,val refresh:Boolean) {
        val ready=CompletableDeferred<String>()
        lateinit var job:Job
    }
    private val lock=Mutex()
    private val publication=Mutex()
    private val writer=Mutex()
    private val networkSlots=Semaphore(2)
    private val runs=mutableMapOf<String,Run>()
    private val mutableProgress=MutableStateFlow(NativeCatalogProgress())
    val progress:StateFlow<NativeCatalogProgress> = mutableProgress.asStateFlow()
    @Volatile private var activeSource:String?=null

    /** Source switches cancel the old import even when the new source already has a cached catalog. */
    suspend fun select(playlist:String?)=lock.withLock {
        activeSource=playlist
        for((id,run)in runs)if(id!=playlist && run.job.isActive)run.job.cancel()
        if(mutableProgress.value.playlistId!=playlist)mutableProgress.value=NativeCatalogProgress(playlistId=playlist.orEmpty())
    }

    suspend fun start(source:PlaylistSource,refresh:Boolean=true):String {
        val run=lock.withLock {
            if(activeSource==null)activeSource=source.id
            if(activeSource==source.id)for((id,other)in runs)if(id!=source.id && other.job.isActive)other.job.cancel()
            val generation=namespace(source)
            val existing=runs[source.id]
            if(existing?.generation==generation && existing.job.isActive)return@withLock existing
            if(!refresh && cache.getActiveGeneration(source.id)?.generationId==generation) {
                val states=dao.states(source.id,generation)
                if(states.map{it.kind}.containsAll(NativeKind.entries.map{it.wire}) && states.all{it.status=="COMPLETE"})return generation
            }
            // Joining a cancelled writer prevents a source's old request from landing after a new refresh.
            existing?.job?.cancelAndJoin()
            Run(source,generation,refresh).also { next->
                runs[source.id]=next
                next.job=scope.launch(CoroutineName("native-catalog-test1")){
                    br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.foregroundImport {execute(next)}
                }
                next.job.invokeOnCompletion { cause->
                    if(!next.ready.isCompleted)next.ready.completeExceptionally(cause?:IllegalStateException("Importação encerrada antes da abertura."))
                }
            }
        }
        return run.ready.await()
    }

    /** Cached rows are immediately usable. First import waits for a committed batch, never the full section. */
    suspend fun ensure(source:PlaylistSource,kind:String) {
        val generation=start(source,refresh=false)
        if(count(source.id,generation,kind)>0)return
        while(true) {
            currentCoroutineContext().ensureActive()
            val state=dao.states(source.id,generation).firstOrNull{it.kind==kind}
            if(state?.status=="COMPLETE" || count(source.id,generation,kind)>0)return
            check(state?.status!="FAILED" && state?.status!="CANCELLED"){"Não foi possível carregar esta seção. Atualize a lista para tentar novamente."}
            val running=lock.withLock{runs[source.id]?.job?.isActive==true}
            check(running){"A importação foi interrompida. Atualize a lista."}
            delay(100)
        }
    }

    private suspend fun execute(run:Run) {
        val source=run.source;val p=source.id;val g=run.generation
        val started=SystemClock.elapsedRealtime()
        try {
            report(p){NativeCatalogProgress(playlistId=p,running=true,revision=it.revision+1)}
            if(source is PlaylistSource.M3u) {
                NativeM3uSync(http,cache,dao,writer).run(source,g,ready={
                    if(cache.getActiveGeneration(p)?.generationId!=g){
                        cache.upsertGeneration(PlaylistCacheGenerationEntity(p,g,"READY",System.currentTimeMillis()))
                        cache.switchToReadyGeneration(p,g,System.currentTimeMillis())
                    }
                    run.ready.complete(g)
                },progress={counts,done->report(p){it.copy(counts=counts,complete=if(done)NativeKind.entries.map{v->v.wire}.toSet()else emptySet(),running=!done)}})
                return
            }
            source as PlaylistSource.XtreamCodes
            val previous=dao.states(p,g)
            val todo=NativeKind.entries.filter {kind->run.refresh || previous.none{it.kind==kind.wire&&it.status=="COMPLETE"}}
            report(p){it.copy(complete=if(run.refresh)emptySet()else previous.filter{v->v.status=="COMPLETE"}.map{v->v.kind}.toSet())}
            val address=Address(source)
            XtreamAuthenticationRecovery.confirm(load={http.get(address.auth)},
                validate=XtreamAuthenticationRecovery::validateResponse,onRetry={})
            val categories=coroutineScope {
                todo.map{kind->async{kind to loadCategories(address,kind,p,g)}}.awaitAll().toMap()
            }
            val before=cache.getActiveGeneration(p)
            if(before?.generationId!=g) {
                cache.upsertGeneration(PlaylistCacheGenerationEntity(p,g,"READY",System.currentTimeMillis()))
                cache.switchToReadyGeneration(p,g,System.currentTimeMillis())
            }
            // Small setup complete: the Home can open; all three sections proceed independently.
            run.ready.complete(g)
            coroutineScope {
                todo.map { kind->launch {
                    networkSlots.withPermit {
                        try {syncKind(source,address,g,kind,categories.getValue(kind))}
                        catch(cancelled:CancellationException){throw cancelled}
                        catch(error:Exception) {
                            markFailed(p,g,kind,"FAILED")
                            report(p){it.copy(errors=it.errors+kind.wire)}
                            Log.w(TAG,"fase=${kind.wire} resultado=ERRO classe=${error.javaClass.simpleName}")
                        }
                    }
                }}.joinAll()
            }
            report(p){it.copy(running=false)}
        }catch(cancelled:CancellationException) {
            withContext(NonCancellable) {
                for(kind in NativeKind.entries) {
                    val state=dao.states(p,g).firstOrNull{it.kind==kind.wire}
                    if(state?.status!="COMPLETE")markFailed(p,g,kind,"CANCELLED")
                }
                report(p){it.copy(running=false)}
            }
            run.ready.completeExceptionally(cancelled);throw cancelled
        }catch(error:Exception) {
            report(p){it.copy(running=false,errors=NativeKind.entries.map{v->v.wire}.toSet())}
            run.ready.completeExceptionally(error)
            Log.w(TAG,"abertura=ERRO classe=${error.javaClass.simpleName}")
        }finally {
            Log.i(TAG,"metodo=T1 fim=true duracaoMs=${SystemClock.elapsedRealtime()-started} heapKb=${heapKb()}")
        }
    }

    private suspend fun loadCategories(address:Address,kind:NativeKind,p:String,g:String):List<CachedCategoryEntity> {
        val rows=LinkedHashMap<String,CachedCategoryEntity>()
        http.withReader(address.api(kind.categoryAction)){reader->
            NativeXtreamReader.read(reader,kind.categoryAction,category=true){f->
                val id=checkNotNull(f.id)
                rows.putIfAbsent(id,CachedCategoryEntity(p,g,id,f.name?:"Categoria",kind.wire,"XTREAM"))
            }
        }
        val old=cache.getCategoriesByType(p,g,kind.wire).associateBy{it.categoryId}
        val changed=rows.values.filter{old[it.categoryId]!=it}
        if(changed.isNotEmpty())writer.withLock{cache.insertCategories(changed)}
        return rows.values.toList()
    }

    private suspend fun syncKind(source:PlaylistSource.XtreamCodes,address:Address,g:String,kind:NativeKind,categories:List<CachedCategoryEntity>) {
        val p=source.id;val token=UUID.randomUUID().toString();val prior=count(p,g,kind.wire)
        dao.state(NativeCatalogState(p,g,kind.wire,token,"RUNNING",0,System.currentTimeMillis()))
        var totals=NativeWriteStats();var lastUi=0L;var dbMs=0L;var first=true;var order=0
        val phaseStart=SystemClock.elapsedRealtime();val seenCategories=HashSet<String>()
        suspend fun endpoint(category:String?=null) = coroutineScope {
            val queue=Channel<NativeBatch>(1)
            val consumer=launch {
                for(batch in queue) {
                    val began=SystemClock.elapsedRealtime()
                    val stats=try {writer.withLock{dao.apply(batch,initial=prior==0)}}
                    catch(cancelled:CancellationException){throw cancelled}
                    catch(error:Exception){throw br.com.flaviotorres.iptvpremium.data.sync.XtreamCatalogWriteException(error)}
                    dbMs+=SystemClock.elapsedRealtime()-began;totals+=stats
                    val now=SystemClock.elapsedRealtime()
                    if(first || now-lastUi>=1000) {
                        report(p){it.copy(counts=it.counts+(kind.wire to totals.accepted.toLong()))}
                        lastUi=now
                    }
                    if(first){first=false;Log.i(TAG,"fase=${kind.wire} primeiroLoteMs=${now-phaseStart}")}
                }
            }
            val channels=ArrayList<CachedChannelEntity>();val movies=ArrayList<CachedMovieEntity>();val series=ArrayList<CachedSeriesEntity>()
            var bytes=0
            suspend fun flush() {
                if(channels.isEmpty()&&movies.isEmpty()&&series.isEmpty())return
                queue.send(NativeBatch(p,g,kind.wire,token,channels.toList(),movies.toList(),series.toList()))
                channels.clear();movies.clear();series.clear();bytes=0
            }
            val read=http.withReader(address.api(kind.itemAction,category)){reader->
                NativeXtreamReader.read(reader,kind.itemAction){f->
                    val id=checkNotNull(f.id);val cat=f.category?:category
                    if(cat!=null)seenCategories.add(cat)
                    val name=f.name?:when(kind){NativeKind.LIVE->"Canal";NativeKind.MOVIES->"Filme";else->"Série"}
                    when(kind) {
                        NativeKind.LIVE->channels+=CachedChannelEntity(p,g,id,name,cat,f.poster,
                            f.direct?.takeIf{it.startsWith("http://")||it.startsWith("https://")}?:address.stream("live",id,"ts"),f.epg,order+1)
                        NativeKind.MOVIES->movies+=CachedMovieEntity(p,g,id,name,cat,f.poster,f.backdrop,
                            f.direct?.takeIf{it.startsWith("http://")||it.startsWith("https://")}?:address.stream("movie",id,f.extension?:"mp4"),
                            f.year,f.rating,f.duration,f.added,order)
                        NativeKind.SERIES->series+=CachedSeriesEntity(p,g,id,name,cat,f.poster,f.backdrop,f.year,f.rating,f.plot,
                            f.logo,f.imdb,f.tmdb,null,f.duration?.takeIf{it in 0..Int.MAX_VALUE.toLong()}?.toInt(),f.genres,f.trailer,order)
                    }
                    order++
                    bytes+=name.length+(f.poster?.length?:0)+(f.plot?.length?:0)+(f.direct?.length?:0)+256
                    // Count and byte limits: large metadata cannot inflate a 1000-row buffer without bound.
                    if(channels.size+movies.size+series.size>=800 || bytes>=512*1024)flush()
                }
            }
            flush();queue.close();consumer.join();read
        }
        var bulkComplete=false
        try {endpoint();bulkComplete=true}
        catch(cancelled:CancellationException){throw cancelled}
        catch(denied:PlaylistAccessDeniedException){throw denied}
        catch(error:Exception) {
            if(error is br.com.flaviotorres.iptvpremium.data.sync.XtreamCatalogWriteException)throw error
            Log.w(TAG,"fase=${kind.wire} recuperacao=CATEGORIAS classe=${error.javaClass.simpleName}")
        }
        val missing=if(bulkComplete)categories.filter{it.categoryId !in seenCategories}else categories
        check(bulkComplete || categories.isNotEmpty()){"Falha no catálogo completo e nenhuma categoria disponível."}
        var fallbackSafe=true
        for(category in missing) {
            currentCoroutineContext().ensureActive()
            val beforeAccepted=totals.accepted
            endpoint(category.categoryId)
            // A panel ignoring category_id must not trigger repeated full downloads of the same payload.
            if(!bulkComplete && totals.accepted==beforeAccepted && totals.accepted>0 && missing.size>1) {
                fallbackSafe=false;break
            }
        }
        check(fallbackSafe){"O provedor ignorou o filtro de categoria; catálogo parcial preservado."}
        check(totals.accepted>0 || prior==0){"Resposta vazia inesperada; catálogo anterior preservado."}
        val deleted=writer.withLock{dao.complete(p,g,kind.wire,token,totals.accepted.toLong(),prior,categories.map{it.categoryId}.ifEmpty{listOf("__none__")},full=bulkComplete )}
        report(p){it.copy(counts=it.counts+(kind.wire to totals.accepted.toLong()),complete=it.complete+kind.wire)}
        Log.i(TAG,"fase=${kind.wire} resultado=COMPLETO novos=${totals.inserted} alterados=${totals.changed} reposicionados=${totals.moved} iguais=${totals.unchanged} duplicados=${totals.duplicate} removidos=$deleted bancoMs=$dbMs totalMs=${SystemClock.elapsedRealtime()-phaseStart} heapKb=${heapKb()}")
    }

    private suspend fun report(p:String,update:(NativeCatalogProgress)->NativeCatalogProgress)=publication.withLock {
        if(activeSource==p) {
            val before=mutableProgress.value
            mutableProgress.value=update(if(before.playlistId==p)before else NativeCatalogProgress(playlistId=p)).let{it.copy(revision=before.revision+1)}
        }
    }
    private suspend fun markFailed(p:String,g:String,kind:NativeKind,status:String) {
        if(!dao.isActive(p,g))return
        val old=dao.states(p,g).firstOrNull{it.kind==kind.wire}
        dao.state(NativeCatalogState(p,g,kind.wire,old?.runId.orEmpty(),status,old?.itemCount?:0,System.currentTimeMillis()))
    }
    private suspend fun count(p:String,g:String,kind:String)=when(kind){"LIVE_TV"->dao.channelCount(p,g);"MOVIES"->dao.movieCount(p,g);else->dao.seriesCount(p,g)}
    internal suspend fun awaitFinished(id:String){lock.withLock{runs[id]?.job}?.join()}

    private class Address(source:PlaylistSource.XtreamCodes) {
        val base:String
        private val user=encode(source.username.trim());private val password=encode(source.password)
        init {
            var raw=source.serverUrl.trim().trimEnd('/')
            if(!raw.startsWith("http://",true)&&!raw.startsWith("https://",true))raw="http://$raw"
            val uri=URI(raw)
            require(uri.scheme in listOf("http","https") && !uri.host.isNullOrBlank()){"Servidor inválido."}
            base=raw.substringBefore('?').trimEnd('/').removeSuffix("/player_api.php").removeSuffix("/get.php")
            require(source.username.isNotBlank()&&source.password.isNotBlank()){"Credenciais ausentes."}
        }
        val auth:String get()="$base/player_api.php?username=$user&password=$password"
        fun api(action:String,category:String?=null)=auth+"&action=$action"+(category?.let{"&category_id=${encode(it)}"}?:"")
        fun stream(type:String,id:String,extension:String):String {
            val ext=extension.takeIf{it.matches(Regex("[A-Za-z0-9]{1,8}"))}?:"mp4"
            return "$base/$type/${path(user)}/${path(password)}/${encode(id).replace("+","%20")}.$ext"
        }
        private fun path(encoded:String)=encoded.replace("+","%20")
    }
    companion object {
        private const val TAG="NativeCatalogT1"
        private fun encode(value:String)=URLEncoder.encode(value,"UTF-8")
        private fun heapKb():Long=Runtime.getRuntime().let{(it.totalMemory()-it.freeMemory())/1024}
        internal fun namespace(source:PlaylistSource):String {
            val text=when(source){
                is PlaylistSource.XtreamCodes->listOf(source.serverUrl.trim(),source.username.trim(),source.password).joinToString("\u0000")
                is PlaylistSource.M3u->"m3u:"+source.playlistUrl.trim()
            }
            return "native-t1-"+MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).take(12).joinToString(""){"%02x".format(it)}
        }
        @Volatile private var instance:NativeXtreamCatalog?=null
        internal fun getInstance(http:PlaylistHttpClient,db:PlaylistCacheDatabase):NativeXtreamCatalog = instance?:synchronized(this) {
            instance?:NativeXtreamCatalog(http,db.playlistCacheDao(),db.nativeCatalogDao()).also{instance=it}
        }
    }
}
