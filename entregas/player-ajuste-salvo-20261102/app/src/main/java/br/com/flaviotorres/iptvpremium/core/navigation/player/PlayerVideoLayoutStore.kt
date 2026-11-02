package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/** Provider identities stay local; filenames/keys never contain a stream URL. */
internal fun playerVideoLayoutKey(playlist:String?,kind:String,id:String,series:String?,season:Int?,kids:Boolean):String? {
    if(playlist.isNullOrBlank() || id.isBlank() || kind !in setOf("MOVIE","EPISODE"))return null
    val identity=if(kind=="MOVIE")listOf("movie",id)
        else if(!series.isNullOrBlank() && season!=null && season>=0)listOf("season",series,season.toString())
        else listOf("episode",series.orEmpty(),id)
    val raw=(listOf(if(kids)"kids"else"normal",playlist)+identity).joinToString(""){"${it.length}:$it"}
    return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
}
internal fun normalizedPlayerLayout(layout:PlaybackVideoLayout):PlaybackVideoLayout =
    if(layout.mode==PlaybackAspectMode.FULL_SCREEN)PlaybackVideoLayout(PlaybackAspectMode.STRETCH)else layout

internal class PlayerVideoLayoutStore(context:Context) {
    private val prefs=context.applicationContext.getSharedPreferences("iptvpremium_player_video_layout_v1",Context.MODE_PRIVATE)
    fun read(key:String?):PlaybackVideoLayout? {
        if(key==null)return null
        return runCatching {
            val raw=prefs.getString(key,null)?:return null
            val value=JSONObject(raw)
            require(value.getInt("version")==1)
            normalizedPlayerLayout(PlaybackVideoLayout(PlaybackAspectMode.valueOf(value.getString("mode")),
                PlaybackManualTransform(value.getDouble("zoom").toFloat(),value.getDouble("width").toFloat(),value.getDouble("height").toFloat(),
                    value.getDouble("x").toFloat(),value.getDouble("y").toFloat())))
        }.getOrNull()
    }
    fun save(key:String?,layout:PlaybackVideoLayout) {
        if(key==null)return
        val value=normalizedPlayerLayout(layout);val manual=value.manual
        prefs.edit().putString(key,JSONObject().put("version",1).put("mode",value.mode.name).put("zoom",manual.zoom)
            .put("width",manual.widthScale).put("height",manual.heightScale).put("x",manual.offsetX).put("y",manual.offsetY).toString()).apply()
    }
}
