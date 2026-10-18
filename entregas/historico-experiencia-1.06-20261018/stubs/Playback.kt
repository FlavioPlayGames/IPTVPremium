package br.com.flaviotorres.iptvpremium.core.player
enum class PlaybackContentType {MOVIE,EPISODE,LIVE_CHANNEL}
data class PlaybackMedia(val id:String,val type:PlaybackContentType,val title:String,val posterUrl:String?=null,val durationMs:Long?=null,val seriesId:String?=null,val seasonNumber:Int?=null,val episodeNumber:Int?=null)
