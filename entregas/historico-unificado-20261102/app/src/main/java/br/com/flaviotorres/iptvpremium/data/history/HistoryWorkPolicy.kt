package br.com.flaviotorres.iptvpremium.data.history

internal fun historyIdentityConflict(a:ContentIdentity,b:ContentIdentity):Boolean {
    if(a.kind!=b.kind)return true
    if(a.imdbKey!=null && b.imdbKey!=null && a.imdbKey!=b.imdbKey)return true
    if(a.tmdbKey!=null && b.tmdbKey!=null && a.tmdbKey!=b.tmdbKey)return true
    val external=a.imdbKey!=null && a.imdbKey==b.imdbKey || a.tmdbKey!=null && a.tmdbKey==b.tmdbKey
    return !external && a.normalizedYear!=null && b.normalizedYear!=null && a.normalizedYear!=b.normalizedYear
}
internal fun sameHistoryWork(a:ContentIdentity,b:ContentIdentity):Boolean {
    if(a.kind!=b.kind)return false
    if(a.imdbKey!=null && b.imdbKey!=null && a.imdbKey!=b.imdbKey)return false
    if(a.tmdbKey!=null && b.tmdbKey!=null && a.tmdbKey!=b.tmdbKey)return false
    if(a.imdbKey!=null && a.imdbKey==b.imdbKey || a.tmdbKey!=null && a.tmdbKey==b.tmdbKey)return true
    if(a.normalizedYear!=null && b.normalizedYear!=null && a.normalizedYear!=b.normalizedYear)return false
    if(a.kind=="movies" && a.providerAssetKey!=null && a.providerAssetKey==b.providerAssetKey)return true
    return a.normalizedYear!=null && a.normalizedYear==b.normalizedYear && a.sharesTitle(b)
}
internal data class HistoryWorkLink(val key:String,val identity:ContentIdentity,val updated:Long)
/** All members must agree, so an incomplete title cannot bridge two remakes. */
internal fun canonicalHistoryRepresentatives(items:List<HistoryWorkLink>):Map<String,String> {
    val groups=mutableListOf<MutableList<HistoryWorkLink>>()
    for(item in items.sortedByDescending{it.updated}) {
        val group=groups.firstOrNull{members->members.all{sameHistoryWork(it.identity,item.identity)}}
        if(group==null)groups.add(mutableListOf(item))else group.add(item)
    }
    return groups.flatMap{group->group.map{it.key to group.first().key}}.toMap()
}
private fun historyQuality(title:String):Int? {
    val t=title.lowercase()
    return when {Regex("\\b(?:4k|uhd|2160p)\\b").containsMatchIn(t)->2160
        Regex("\\b(?:fhd|1080p)\\b").containsMatchIn(t)->1080
        Regex("\\b(?:hd|720p)\\b").containsMatchIn(t)->720
        Regex("\\bsd\\b").containsMatchIn(t)->480
        else->null}
}
internal fun preferredHistoryVariantIndex(source:String,candidates:List<Pair<String,Int?>>):Int? {
    val quality=historyQuality(source)
    val subtitled=ContentIdentity.isSubtitled(source)
    return candidates.indices.minWithOrNull(compareBy<Int>{ContentIdentity.isSubtitled(candidates[it].first)!=subtitled}
        .thenBy{quality!=null && historyQuality(candidates[it].first)!=quality}
        .thenBy{candidates[it].second?:Int.MAX_VALUE}.thenBy{it})
}
/** Select the newest event before filtering; finished events must supersede older unfinished episodes. */
fun latestHistorySeries(entries:List<WatchHistoryEntry>):List<WatchHistoryEntry> = entries
    .filter{it.contentType==br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType.EPISODE && !it.seriesId.isNullOrBlank()}
    .sortedByDescending{it.updatedAtMs}.distinctBy{it.seriesId}
