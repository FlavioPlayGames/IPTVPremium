package br.com.flaviotorres.iptvpremium.data.cache.importer

import java.util.Locale

/** M3U has no required content-type field. Prefer explicit signals; ambiguous streams remain live. */
internal object M3uEntryClassifier {
    enum class Kind { LIVE_TV, MOVIE, SERIES }
    private val episode = Regex("""(?i)\bS\d{1,2}\s*E\d{1,3}\b|\b(?:temporada|season)\s*\d{1,2}.*?\b(?:epis[oó]dio|episode|ep)\s*\d{1,3}\b""")
    private val liveGroup = Regex("""(?:^|[^a-z0-9])(?:canais?|channels?|live|radio|radios|24\s*h(?:oras)?|ao\s+vivo)(?:$|[^a-z0-9])""")
    private val seriesGroup = Regex("""(?:^|[^a-z0-9])(?:serie|series|seriado|seriados|minisserie|minisseries|temporada|season)(?:$|[^a-z0-9])""")
    private val movieGroup = Regex("""(?:^|[^a-z0-9])(?:filme|filmes|movie|movies|cinema|vod|lancamento|lancamentos)(?:$|[^a-z0-9])""")
    private val vodExtensions = setOf("mp4","mkv","avi","mov","m4v","webm","wmv","mpg","mpeg","flv","divx","3gp","vob")

    fun classify(streamUrl:String,groupName:String,name:String,hasXuiId:Boolean=false,hasTvgIdAttribute:Boolean=false):Kind {
        // Query/fragment may contain a title, logo URL or signed token; those are not stream paths.
        val end=minOf(streamUrl.indexOf('?').takeIf{it>=0}?:streamUrl.length,
            streamUrl.indexOf('#').takeIf{it>=0}?:streamUrl.length)
        val scheme=streamUrl.indexOf("://")
        val start=if(scheme>=0)streamUrl.indexOf('/',scheme+3).takeIf{it in 0 until end}?:end else 0
        val path=streamUrl.substring(start,end).lowercase(Locale.ROOT)
        if("/series/" in path || "/serie/" in path)return Kind.SERIES
        if("/movie/" in path || "/movies/" in path || "/vod/" in path)return Kind.MOVIE
        if("/live/" in path)return Kind.LIVE_TV

        val isEpisode=episode.containsMatchIn(name)
        // A labelled VOD entry remains VOD even when another XUI exporter includes an empty tvg-id.
        // "Filmes e Séries" is a mixed live-TV group, not an explicit VOD type.
        val group=fold(groupName)
        if(liveGroup.containsMatchIn(group))return Kind.LIVE_TV
        if(isEpisode)return Kind.SERIES
        val series=seriesGroup.containsMatchIn(group)
        val movie=movieGroup.containsMatchIn(group)
        if(series && !movie)return Kind.SERIES
        if(movie && !series)return Kind.MOVIE
        // Qualified XUI exports use tvg-id for EPG-bearing live rows, including empty IDs.
        // Its absence is a VOD hint only in the corresponding xui-id metadata layout.
        if(hasXuiId && hasTvgIdAttribute)return Kind.LIVE_TV
        val file=path.substringAfterLast('/')
        if(file.substringAfterLast('.',"") in vodExtensions)return Kind.MOVIE
        if(hasXuiId && !hasTvgIdAttribute)return Kind.MOVIE
        // .ts and .m3u8 are used by both VOD and live; never guess from those extensions alone.
        return Kind.LIVE_TV
    }
    private fun fold(value:String):String {
        val lower=value.lowercase(Locale.ROOT)
        return buildString(lower.length) {
            lower.forEach {append(when(it){
                'á','à','ã','â','ä'->'a';'é','è','ê','ë'->'e';'í','ì','î','ï'->'i'
                'ó','ò','õ','ô','ö'->'o';'ú','ù','û','ü'->'u';'ç'->'c';else->it
            })}
        }
    }
}
