package br.com.flaviotorres.iptvpremium.data.history

import android.content.Context
import br.com.flaviotorres.iptvpremium.domain.model.FavoriteScope
import org.json.JSONArray

/** One atomic union of the former player movie/series sets; episode favorites remain individual. */
internal object PlayerFavoriteMigration {
    private val lock=Any()
    fun run(context:Context):Int=synchronized(lock) {
        val app=context.applicationContext
        val old=app.getSharedPreferences("iptvpremium_player_episode_favorites_v1",Context.MODE_PRIVATE)
        val target=app.getSharedPreferences("iptvpremium_favorites_v1",Context.MODE_PRIVATE)
        val edit=target.edit();var count=0
        var migrationStamp:Long?=null
        for((key,value)in old.all) {
            val kids=when {key.startsWith("normal:")->false;key.startsWith("kids:")->true;else->continue}
            val kind=when {key.endsWith(":movies")->"movies";key.endsWith(":series")->"series";else->continue}
            val marker="player_unified_v1:$key"
            if(target.getBoolean(marker,false))continue
            val playlist=key.removePrefix(if(kids)"kids:"else"normal:").removeSuffix(":$kind")
            if(playlist.isBlank() || value !is String)continue
            val array=runCatching{JSONArray(value)}.getOrNull()?:continue
            val ids=(0 until array.length()).mapNotNull{(array.opt(it) as? String)?.takeIf{v->v.isNotBlank()}}.toSet()
            val destination=FavoriteScope.key(playlist,kind,kids)
            val merged=target.getStringSet(destination,emptySet()).orEmpty()+ids
            edit.putStringSet(destination,merged).putBoolean(marker,true)
            if(!kids && ids.isNotEmpty()) {
                val pending="player_migration_pending:$kind:$playlist"
                if(migrationStamp==null) {
                    val records=runCatching{JSONArray(app.getSharedPreferences("iptvpremium_shared_library_v1",Context.MODE_PRIVATE).getString("identities","[]"))}.getOrNull()
                    val latest=(0 until (records?.length()?:0)).maxOfOrNull{records?.optJSONObject(it)?.optLong("changed")?:0L}?:0L
                    migrationStamp=maxOf(System.currentTimeMillis(),latest+1)
                }
                edit.putStringSet(pending,target.getStringSet(pending,emptySet()).orEmpty()+ids)
                    .putLong("player_migration_time:$kind:$playlist",migrationStamp!!)
            }
            count++
        }
        if(count>0)check(edit.commit()) {"Não foi possível unificar os favoritos do player"}
        count
    }
}
