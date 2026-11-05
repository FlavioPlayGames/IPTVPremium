package br.com.flaviotorres.iptvpremium.data.history

import android.content.Context
import org.json.JSONArray

/** User approved discarding unmarked Continue once; favorites and completed marks stay intact. */
internal object HistoryProfileMigration {
    private val lock=Any()
    fun run(context:Context)=synchronized(lock) {
        val app=context.applicationContext
        val marker=app.getSharedPreferences("iptvpremium_history_profiles_v1",Context.MODE_PRIVATE)
        if(marker.getBoolean("continue_reset_v1",false))return@synchronized
        for(kids in listOf(false,true)) {
            val prefs=app.getSharedPreferences(HistoryProfile.preferences("iptvpremium_watch_history_v1",kids),Context.MODE_PRIVATE)
            val raw=runCatching{JSONArray(prefs.getString("items","[]"))}.getOrElse{JSONArray()}
            val retained=JSONArray()
            for(index in 0 until raw.length()) {
                val item=raw.optJSONObject(index)?:continue
                val position=item.optLong("positionMs");val duration=item.optLong("durationMs")
                val finished=item.optBoolean("finished") || duration>0 && position.toDouble()/duration>=.95
                val continuing=!finished && (item.optBoolean("seriesOnly") || item.optBoolean("pendingNext") || position>=10_000)
                if(!continuing)retained.put(item)
            }
            // Mark old imports cleared before removing the standalone resume files.
            check(prefs.edit().putString("items",retained.toString())
                .putBoolean("legacy_cleared_MOVIE",true).putBoolean("legacy_cleared_EPISODE",true).commit())
            for(base in listOf("iptvpremium_movies_assistir_v1","iptvpremium_series_assistir_v1"))
                check(app.getSharedPreferences(HistoryProfile.preferences(base,kids),Context.MODE_PRIVATE).edit().clear().commit())
        }
        check(marker.edit().putBoolean("continue_reset_v1",true).commit())
    }
}
