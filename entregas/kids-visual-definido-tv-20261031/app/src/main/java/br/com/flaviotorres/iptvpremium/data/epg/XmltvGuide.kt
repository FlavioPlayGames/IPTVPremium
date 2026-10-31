package br.com.flaviotorres.iptvpremium.data.epg

import android.content.Context
import android.util.Log
import br.com.flaviotorres.iptvpremium.core.player.PlaybackEpgProgram
import br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.resolver.M3uXtreamResolver
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import java.io.File
import java.io.FilterOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

/** XMLTV discovery is import metadata. Feed downloads never run in the import/player path. */
class XmltvGuide(context:Context,private val dao:PlaylistCacheDao,private val http:PlaylistHttpClient) {
    private val app=context.applicationContext
    private val prefs=app.getSharedPreferences("iptv_xmltv_sources_v1",Context.MODE_PRIVATE)
    private val directory=File(app.cacheDir,"xmltv_epg_v1")
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val downloadSlot=Semaphore(1)
    private val lock=Any()
    private val jobs=mutableSetOf<String>()
    private val downloadJobs=mutableMapOf<String,Job>()
    private val headerJobs=mutableMapOf<String,Pair<String,Job>>()
    private val failedUntil=object:LinkedHashMap<String,Long>(16,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,Long>?)=size>32}
    private data class Parsed(val at:Long,val entries:List<XmltvEntry>)
    private val parsed=object:LinkedHashMap<String,Parsed>(8,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,Parsed>?)=size>8}
    private val parseLock=kotlinx.coroutines.sync.Mutex()

    fun recordM3u(playlist:String,playlistUrl:String,urls:List<String>) {
        val safe=urls.filter(::xmltvHttpUrl).distinct().take(4)
        prefs.edit().putString("header_$playlist",JSONArray(safe).toString()).putString("source_$playlist",xmltvHash(playlistUrl))
            .putLong("probe_$playlist",System.currentTimeMillis()).putBoolean("probe_ok_$playlist",true).apply()
        Log.i("XmltvEpg","discovery=m3u sources=${safe.size}")
    }
    fun discover(source:PlaylistSource,refresh:Boolean=false) {
        if(!source.isEnabled)return
        if(source is PlaylistSource.M3u) {
            val fingerprint=xmltvHash(source.playlistUrl)
            val seen=prefs.getString("source_${source.id}",null)==fingerprint
            val recent=System.currentTimeMillis()-prefs.getLong("probe_${source.id}",0L)<60_000L
            val retry=!prefs.getBoolean("probe_ok_${source.id}",false) && System.currentTimeMillis()-prefs.getLong("probe_${source.id}",0L)>=300_000L
            if(!seen || (refresh && !recent) || retry) {
                val key="header:$fingerprint"
                synchronized(lock){if(!jobs.add(key))return}
                val job=scope.launch(start=CoroutineStart.LAZY) {
                    try {
                        withTimeout(6_000L) {
                            http.withReader(source.playlistUrl) {reader->
                                val header=StringBuilder()
                                while(header.length<16_384) {currentCoroutineContext().ensureActive();val c=reader.read();if(c<0 || c==10)break;header.append(c.toChar())}
                                val active=currentCoroutineContext()[Job]
                                synchronized(lock) {if(active?.isActive==true)recordM3u(source.id,source.playlistUrl,xmltvHeaderSources(header.toString(),source.playlistUrl))}
                            }
                        }
                        changes.update {it+1}
                    } catch(cancelled:CancellationException){throw cancelled}
                    catch(error:Exception){prefs.edit().also {if(!seen)it.remove("header_${source.id}")}.putString("source_${source.id}",fingerprint).putLong("probe_${source.id}",System.currentTimeMillis()).putBoolean("probe_ok_${source.id}",false).apply();Log.w("XmltvEpg","discovery=unavailable error=${error.javaClass.simpleName}")}
                    finally{synchronized(lock){jobs.remove(key);if(headerJobs[source.id]?.first==key)headerJobs.remove(source.id)}}
                }
                synchronized(lock){headerJobs.put(source.id,key to job)?.second?.cancel()}
                job.start()
            }
        }
        rememberFeeds(source.id,sources(source).map(::xmltvHash).toSet())
        // Xtream's URL is derived locally; availability is checked only when the guide is opened.
    }
    private fun sources(source:PlaylistSource):List<String> {
        val explicit=if(source is PlaylistSource.M3u && prefs.getString("source_${source.id}",null)==xmltvHash(source.playlistUrl))
            runCatching {val a=JSONArray(prefs.getString("header_${source.id}","[]"));(0 until a.length()).map {a.getString(it)}}.getOrDefault(emptyList()) else emptyList()
        val xtream=when(source){is PlaylistSource.XtreamCodes->source;is PlaylistSource.M3u->M3uXtreamResolver.resolve(source)}
        return (explicit+listOfNotNull(xtream?.let(::xmltvXtreamSource))).filter(::xmltvHttpUrl).distinct().take(4)
    }
    suspend fun merge(source:PlaylistSource,channelId:String,api:List<PlaybackEpgProgram>,limit:Int):List<PlaybackEpgProgram> = withContext(Dispatchers.IO) {
        discover(source)
        if(!source.isEnabled)return@withContext api
        val generation=dao.getActiveGeneration(source.id)?.generationId ?: return@withContext api
        val channel=dao.getChannelById(source.id,generation,channelId) ?: return@withContext api
        val combined=mutableListOf<XmltvEntry>()
        val now=System.currentTimeMillis()
        val available=sources(source)
        val hashes=available.map(::xmltvHash).toSet()
        rememberFeeds(source.id,hashes)
        for(url in available) {
            currentCoroutineContext().ensureActive()
            val hash=xmltvHash(url);val file=File(directory,"$hash.gz")
            if(!file.isFile || now-file.lastModified()>2*60*60_000L)refreshFeed(hash,url,file)
            if(!file.isFile)continue
            val key="$hash:${file.lastModified()}:${channel.epgChannelId}:${channel.name}"
            val cached=synchronized(lock){parsed[key]?.takeIf {now-it.at<300_000L}}
            if(cached!=null){combined+=cached.entries;continue}
            parseLock.lock()
            try {
                val current=currentCoroutineContext()
                val entries=AppCacheStorage.get(app).retain(file).use {
                    file.inputStream().use {XmltvReader.read(it,channel.epgChannelId.orEmpty(),channel.name,now){current.ensureActive()}}
                }
                synchronized(lock){parsed[key]=Parsed(now,entries)}
                combined+=entries
            }catch(cancelled:CancellationException){throw cancelled}
            catch(error:Exception){synchronized(lock){failedUntil[hash]=now+300_000L};file.delete();Log.w("XmltvEpg","parse=unavailable error=${error.javaClass.simpleName}")}
            finally{parseLock.unlock()}
        }
        if(combined.isEmpty())api else mergeXmltvPrograms(api,combined,limit)
    }
    private fun refreshFeed(hash:String,url:String,target:File) {
        synchronized(lock){if((failedUntil[hash] ?: 0)>System.currentTimeMillis() || !jobs.add(hash))return}
        val job=scope.launch(start=CoroutineStart.LAZY) {
            try {downloadSlot.withPermit {
                br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.awaitIdle()
                br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.busy.first {!it}
                withTimeout(45_000L) {download(url,target)}
                changes.update {it+1}
            }}catch(cancelled:CancellationException){synchronized(lock){failedUntil[hash]=System.currentTimeMillis()+60_000L};throw cancelled}
            catch(error:Exception){synchronized(lock){failedUntil[hash]=System.currentTimeMillis()+300_000L};Log.w("XmltvEpg","download=unavailable error=${error.javaClass.simpleName}")}
            finally{synchronized(lock){jobs.remove(hash);downloadJobs.remove(hash)}}
        }
        synchronized(lock){downloadJobs[hash]=job}
        job.start()
    }
    private fun rememberFeeds(playlist:String,hashes:Set<String>) {
        val old=prefs.getStringSet("feeds_$playlist",emptySet()).orEmpty().toSet()
        if(old==hashes)return
        prefs.edit().putStringSet("feeds_$playlist",hashes).apply()
        removeUnused(old-hashes)
    }
    fun forget(playlist:String) {
        val old=synchronized(lock) {
            headerJobs.remove(playlist)?.second?.cancel()
            val saved=prefs.getStringSet("feeds_$playlist",emptySet()).orEmpty().toSet()
            prefs.edit().remove("feeds_$playlist").remove("header_$playlist").remove("source_$playlist").remove("probe_$playlist").remove("probe_ok_$playlist").apply()
            saved
        }
        removeUnused(old)
    }
    private fun removeUnused(hashes:Set<String>) {scope.launch {
        for(hash in hashes) {
            val shared=prefs.all.any {(key,value)->key.startsWith("feeds_") && value is Set<*> && hash in value}
            if(!shared) {
                val job=synchronized(lock){parsed.keys.removeAll {it.startsWith("$hash:")};downloadJobs[hash]}
                job?.cancelAndJoin()
                val stillShared=prefs.all.any {(key,value)->key.startsWith("feeds_") && value is Set<*> && hash in value}
                if(!stillShared)File(directory,"$hash.gz").delete()
            }
        }
    }}
    private suspend fun download(url:String,target:File)=coroutineScope {
        val connection=URL(url).openConnection() as HttpURLConnection
        val closer=launch(Dispatchers.IO,start=CoroutineStart.UNDISPATCHED){try{awaitCancellation()}finally{connection.disconnect()}}
        try {
            connection.connectTimeout=8_000;connection.readTimeout=12_000;connection.instanceFollowRedirects=true
            connection.setRequestProperty("User-Agent","IPTVPremium/1.0")
            connection.setRequestProperty("Accept-Encoding","gzip")
            connection.setRequestProperty("Accept-Language","pt-BR,pt;q=0.9,en;q=0.7")
            val status=connection.responseCode
            Log.i("XmltvEpg","download=http status=$status")
            check(status in 200..299){"XMLTV HTTP unavailable"}
            check(directory.isDirectory || directory.mkdirs() || directory.isDirectory)
            val storage=AppCacheStorage.get(app)
            val reservation=storage.reserve(target,12L*1024*1024) ?: error("XMLTV cache budget unavailable")
            reservation.use {lease->
                lease.temporary.outputStream().use {raw->
                    val bounded=object:FilterOutputStream(raw){var count=0L
                        override fun write(b:Int){if(++count>lease.limit)error("XMLTV compressed limit");out.write(b)}
                        override fun write(b:ByteArray,o:Int,l:Int){count+=l;if(count>lease.limit)error("XMLTV compressed limit");out.write(b,o,l)}
                    }
                    GZIPOutputStream(bounded,32*1024).use {output->
                        connection.inputStream.use {input->val buffer=ByteArray(32*1024);var total=0L
                            while(true){currentCoroutineContext().ensureActive();if(br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.value || br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.busy.value)throw CancellationException("Foreground priority");val n=input.read(buffer);if(n<0)break;total+=n;if(total>48L*1024*1024)error("XMLTV network limit");output.write(buffer,0,n)}
                            check(total>0){"Empty XMLTV"}
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                check(lease.temporary.renameTo(target)){"XMLTV cache commit failed"}
            }
        }finally{closer.cancel();connection.disconnect()}
    }
    fun close(){scope.cancel()}
    companion object {val changes=MutableStateFlow(0L)}
}
