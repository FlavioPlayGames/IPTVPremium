package br.com.flaviotorres.iptvpremium.data.repository
data class SeriesEpisodeInfo(val id:String,val seasonNumber:Int,val episodeNumber:Int,val title:String,val durationSeconds:Int?=null,val thumbnailUrl:String?=null,val streamUrl:String?=null,val containerExtension:String?=null)
