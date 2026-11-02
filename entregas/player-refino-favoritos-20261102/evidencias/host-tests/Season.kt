package br.com.flaviotorres.iptvpremium.data.repository
import org.json.JSONObject
internal fun catalogSeasonPosterPaths(root:JSONObject):Map<Int,String> {
    val values=root.optJSONArray("seasons")?:return emptyMap()
    return (0 until values.length()).mapNotNull { index->
        val row=values.optJSONObject(index)?:return@mapNotNull null
        val number=row.optInt("season_number",-1)
        val path=row.optString("poster_path")
        if(number<0 || !Regex("^/[A-Za-z0-9_-]+\\.(?:jpg|png|webp)$").matches(path))null
        else number to "https://image.tmdb.org/t/p/w500$path"
    }.toMap()
}
