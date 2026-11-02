package br.com.flaviotorres.iptvpremium.data.repository
import org.json.JSONObject
internal data class SeriesSeasonArtwork(val episodeStills:Map<Int,String>,val posterUrl:String?,val episodeNames:Map<Int,String> = emptyMap())
internal fun seriesArtworkImage(size:String,path:String?):String? = path?.trim()?.takeIf {
    Regex("^/[A-Za-z0-9_-]+\\.(?:jpg|png|webp)$").matches(it)
}?.let{"https://image.tmdb.org/t/p/$size$it"}
internal fun parseSeriesSeasonArtwork(root:JSONObject):SeriesSeasonArtwork {
    val episodes=root.optJSONArray("episodes")
    val rows=(0 until (episodes?.length()?:0)).mapNotNull{episodes?.optJSONObject(it)}.filter{it.optInt("episode_number",-1)>0}
    return SeriesSeasonArtwork(rows.mapNotNull {row->seriesArtworkImage("w780",row.optString("still_path"))?.let{row.optInt("episode_number") to it}}.toMap(),
        seriesArtworkImage("w500",root.optString("poster_path")),rows.mapNotNull { row->row.optString("name").takeIf{it.isNotBlank()}?.let{row.optInt("episode_number") to it}}.toMap())
}
