package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage
import br.com.flaviotorres.iptvpremium.data.repository.artworkTitle
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/** Shares the existing 5 MB subtitle file budget. Persist only validated, chosen SRTs. */
internal class ManualSubtitleStore(context:Context) {
    private val app=context.applicationContext
    private val root=File(app.filesDir,"iptv_subtitles_lru_5mb")
    private val prefs=app.getSharedPreferences("manual_subtitles_v1",Context.MODE_PRIVATE)
    private fun records():List<JSONObject> = runCatching{val a=JSONArray(prefs.getString("files","[]"));
        (0 until a.length()).map{a.getJSONObject(it)}.filter{it.optString("id").matches(Regex("[a-f0-9]{64}"))&&File(root,it.getString("id")+".srt").isFile}}.getOrDefault(emptyList())
    fun hasAny():Boolean=prefs.getString("files","[]")!="[]"
    private fun identity(kind:String,title:String,year:Int?,playlist:String?,id:String,kids:Boolean,season:Int?,episode:Int?):String =
        listOf(if(kids)"kids"else"normal",kind,playlist.orEmpty(),id,artworkTitle(title),season?.toString().orEmpty(),episode?.toString().orEmpty()).joinToString("\u0000")
    fun options(media:PlaybackMedia,playlist:String?,kids:Boolean):List<ExternalSubtitleOption> = synchronized(lock) {
        if(media.type==PlaybackContentType.LIVE_CHANNEL)return@synchronized emptyList()
        val kind=if(media.type==PlaybackContentType.MOVIE)"movie"else"episode"
        val key=identity(kind,media.subtitleSearchTitle?:media.title,media.year,playlist,
            if(kind=="movie")media.id else media.seriesId.orEmpty(),kids,if(kind=="movie")null else media.seasonNumber,if(kind=="movie")null else media.episodeNumber)
        records().filter{it.optString("identity")==key && (media.year==null || it.optInt("year",0)==0 || it.optInt("year")==media.year)}.map {
            ExternalSubtitleOption("Manual",it.getString("id"),"manual",it.getString("name"))
        }
    }
    fun read(id:String):ByteArray?=synchronized(lock){
        if(!id.matches(Regex("[a-f0-9]{64}"))||records().none{it.optString("id")==id})return@synchronized null
        val file=File(root,"$id.srt")
        AppCacheStorage.get(app).retain(file).use {if(file.length() in 1..ManualSubtitleArchive.MAX_FILE)file.readBytes()else null}
    }
    fun save(target:ManualSubtitleTarget,chosen:List<Pair<ManualSubtitleFile,Int>>):Int=synchronized(lock){
        require(chosen.isNotEmpty()&&chosen.map{it.second}.distinct().size==chosen.size){"Escolha uma legenda por episódio."}
        require(chosen.sumOf{it.first.bytes.size.toLong()}<=4_000_000L){"As legendas escolhidas excedem 4 MB. Envie a temporada em partes."}
        root.mkdirs();val budget=AppCacheStorage.get(app)
        // Reserve and validate everything before replacing the association index.
        val incoming=chosen.map{(file,number)->
            require(ManualSubtitleArchive.cues(file.bytes).isNotEmpty()){"Legenda inválida."}
            val key=identity(if(target.kind=="movie")"movie"else"episode",target.title,target.year,target.playlistId,target.contentId,
                target.kids,target.season,if(target.kind=="movie")null else number)
            val id=hex((key+"\u0000"+file.name).toByteArray()+file.bytes)
            Triple(id,key,file)
        }
        val leases=mutableListOf<br.com.flaviotorres.iptvpremium.core.storage.CacheStorageBudget.Reservation>()
        val created=mutableListOf<File>()
        try{
            incoming.forEach{(id,_,file)->leases+=budget.reserve(File(root,"$id.srt"),file.bytes.size.toLong())
                ?:error("Não há espaço disponível para todas as legendas. Envie em partes.")}
            incoming.forEachIndexed{index,(id,_,file)->
                val lease=leases[index];FileOutputStream(lease.temporary).use{it.write(file.bytes);it.fd.sync()}
                val destination=File(root,"$id.srt")
                if(destination.exists()){require(destination.readBytes().contentEquals(file.bytes));lease.temporary.delete()}
                else{check(lease.temporary.renameTo(destination));created+=destination}
            }
            val ids=incoming.map{it.first}.toSet()
            val next=records().filter{it.optString("id") !in ids}.toMutableList()
            incoming.forEach{(id,key,file)->next+=JSONObject().put("id",id).put("identity",key).put("name",file.name.take(200)).put("year",target.year)}
            check(next.size<=500){"Remova legendas antigas antes de enviar mais arquivos."}
            check(prefs.edit().putString("files",JSONArray(next).toString()).commit()){"Não foi possível salvar as associações."}
            SubtitleProviderUpdates.changed();incoming.size
        }catch(error:Throwable){created.forEach{it.delete()};throw error}
        finally{leases.forEach{it.close()}}
    }
    private companion object {
        val lock=Any()
        fun hex(data:ByteArray)=MessageDigest.getInstance("SHA-256").digest(data).joinToString(""){"%02x".format(it)}
    }
}
