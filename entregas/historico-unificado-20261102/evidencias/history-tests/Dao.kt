package br.com.flaviotorres.iptvpremium.data.cache.dao
import br.com.flaviotorres.iptvpremium.data.history.*
data class MovieRow(val name:String,val year:Int?,val streamUrl:String?)
data class SeriesRow(val name:String,val year:Int?,val imdbId:String?,val tmdbId:String?)
data class Generation(val generationId:String)
class PlaylistCacheDao {
 val movies=mutableMapOf<String,List<SharedCatalogIdentity>>()
 val series=mutableMapOf<String,List<SharedCatalogIdentity>>()
 var generation="g1";var requests=mutableListOf<String>()
 suspend fun getActiveGeneration(p:String)=if(p in movies || p in series)Generation(generation)else null
 suspend fun getActiveMovieById(p:String,id:String)=movies[p]?.firstOrNull{it.contentId==id}?.let{MovieRow(it.title,it.year,it.streamUrl)}
 suspend fun getParentalSeriesById(p:String,g:String,id:String)=series[p]?.firstOrNull{it.contentId==id}?.let{SeriesRow(it.title,it.year,it.imdb,it.tmdb)}
 private fun glob(pattern:String,text:String):Boolean {
  val regex=StringBuilder("^");var i=0
  while(i<pattern.length){val c=pattern[i];when(c){'*'->regex.append(".*");'?'->regex.append('.');'['->{val end=pattern.indexOf(']',i+1);if(end>i){regex.append(pattern.substring(i,end+1));i=end}else regex.append("\\[")};else->regex.append(Regex.escape(c.toString()))};i++}
  return Regex(regex.append('$').toString()).matches(text)
 }
 suspend fun sharedMovieCandidates(p:String,g:String,after:String,pattern:String,ids:List<String>,streamPattern:String=""):List<SharedCatalogIdentity> {
  requests+="movies:$p:$pattern:${ids.joinToString()}:$streamPattern"
  return movies[p].orEmpty().filter{it.contentId>after && (it.contentId in ids || pattern.isNotEmpty()&&glob(pattern,it.title) || streamPattern.isNotEmpty()&&glob(streamPattern,it.streamUrl.orEmpty()))}.sortedBy{it.contentId}.take(256)
 }
 suspend fun sharedSeriesCandidates(p:String,g:String,after:String,pattern:String,ids:List<String>,imdbs:List<String>,tmdbs:List<String>)=series[p].orEmpty().filter{it.contentId>after && (it.contentId in ids || pattern.isNotEmpty()&&glob(pattern,it.title) || it.imdb in imdbs || it.tmdb in tmdbs)}.sortedBy{it.contentId}.take(256)
 suspend fun sharedSeriesEpisodeCandidates(p:String,g:String,season:Int,episode:Int,pattern:String):List<SharedSeriesEpisodeIdentity> = emptyList()
}
