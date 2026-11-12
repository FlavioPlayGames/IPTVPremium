package br.com.flaviotorres.iptvpremium.core.player

import org.json.JSONArray
import org.json.JSONObject

internal data class ManualSubtitleTarget(val kind:String,val playlistId:String,val contentId:String,val title:String,
    val year:Int?=null,val tmdbId:String?=null,val season:Int?=null,val episode:Int?=null,
    val episodes:List<Int> = emptyList(),val kids:Boolean=false) {
    fun json():JSONObject=JSONObject().put("kind",kind).put("playlistId",playlistId).put("contentId",contentId)
        .put("title",title.take(200)).put("year",year).put("tmdbId",tmdbId).put("season",season).put("episode",episode)
        .put("episodes",JSONArray(episodes)).put("kids",kids)
    val description:String get()=when(kind){"movie"->"Filme";"episode"->"Temporada $season · Episódio $episode";else->"Temporada $season · ${episodes.size} episódios"}
    fun validEpisode(file:ManualSubtitleFile):Int?=when {
        kind=="movie"->if(file.season==null&&file.episode==null)0 else null
        kind=="episode"->if(file.season==null&&file.episode==null||file.season==season&&file.episode==episode)episode else null
        file.season==season&&file.episode in episodes->file.episode
        else->null
    }
}
