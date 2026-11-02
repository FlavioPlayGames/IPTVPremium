package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Episode favorites are scoped to a playlist/profile, never merged with whole-series favorites. */
internal data class PlayerFavoriteEpisode(val seriesId:String,val episodeId:String,val seriesName:String,
    val title:String,val season:Int,val number:Int,val image:String?,val seriesPoster:String?) {
    val key:String get()="$seriesId\u0000$episodeId"
}
internal class PlayerEpisodeFavorites(context:Context,playlist:String,kids:Boolean) {
    private val prefs=context.applicationContext.getSharedPreferences("iptvpremium_player_episode_favorites_v1",Context.MODE_PRIVATE)
    private val key=(if(kids)"kids:"else"normal:")+playlist
    fun all():List<PlayerFavoriteEpisode> = runCatching {
        val values=JSONArray(prefs.getString(key,"[]"))
        (0 until values.length()).mapNotNull { i->values.optJSONObject(i)?.let { v->
            val series=v.optString("series");val id=v.optString("id")
            if(series.isBlank()||id.isBlank())null else PlayerFavoriteEpisode(series,id,v.optString("seriesName"),v.optString("title"),
                v.optInt("season"),v.optInt("number"),v.optString("image").takeIf{it.isNotBlank()},v.optString("poster").takeIf{it.isNotBlank()})
        }}
    }.getOrDefault(emptyList())
    fun save(values:List<PlayerFavoriteEpisode>) {
        val array=JSONArray()
        values.distinctBy{it.key}.forEach { v->array.put(JSONObject().put("series",v.seriesId).put("id",v.episodeId)
            .put("seriesName",v.seriesName).put("title",v.title).put("season",v.season).put("number",v.number)
            .put("image",v.image.orEmpty()).put("poster",v.seriesPoster.orEmpty())) }
        prefs.edit().putString(key,array.toString()).apply()
    }
}
