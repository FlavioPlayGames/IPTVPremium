package br.com.flaviotorres.iptvpremium.data.cache.dao
import br.com.flaviotorres.iptvpremium.data.history.SharedCatalogIdentity
data class MovieRow(val name:String,val year:Int?)
data class SeriesRow(val name:String,val year:Int?,val imdbId:String?,val tmdbId:String?)
data class Generation(val generationId:String)
class PlaylistCacheDao {
 val movies=mutableMapOf<String,List<SharedCatalogIdentity>>()
 val series=mutableMapOf<String,List<SharedCatalogIdentity>>()
 var generation="g1"
 var maxBatch=0;var pages=0
 suspend fun getActiveGeneration(p:String)=if(p in movies || p in series)Generation(generation)else null
 suspend fun getActiveMovieById(p:String,id:String)=movies[p]?.firstOrNull{it.contentId==id}?.let{MovieRow(it.title,it.year)}
 suspend fun getParentalSeriesById(p:String,g:String,id:String)=series[p]?.firstOrNull{it.contentId==id}?.let{SeriesRow(it.title,it.year,it.imdb,it.tmdb)}
 suspend fun sharedMoviePage(p:String,g:String,after:String)=page(movies[p].orEmpty(),after)
 suspend fun sharedSeriesPage(p:String,g:String,after:String)=page(series[p].orEmpty(),after)
 private fun page(rows:List<SharedCatalogIdentity>,after:String):List<SharedCatalogIdentity>{val page=rows.filter{it.contentId>after}.sortedBy{it.contentId}.take(256);maxBatch=maxOf(maxBatch,page.size);pages++;return page}
}
