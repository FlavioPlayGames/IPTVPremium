package br.com.flaviotorres.iptvpremium.data.catalog

import androidx.room.*
import br.com.flaviotorres.iptvpremium.data.cache.entity.*

@Dao
internal interface NativeCatalogDao {
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_cache_generations WHERE playlistId=:p AND generationId=:g AND status='ACTIVE')")
    suspend fun isActive(p:String,g:String):Boolean
    @Query("SELECT * FROM native_catalog_state WHERE playlistId=:p AND generationId=:g")
    suspend fun states(p:String,g:String):List<NativeCatalogState>
    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun state(value:NativeCatalogState)
    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun mark(values:List<NativeCatalogSeen>)
    @Query("SELECT * FROM native_catalog_seen WHERE playlistId=:p AND generationId=:g AND kind=:k AND itemId IN (:ids)")
    suspend fun seen(p:String,g:String,k:String,ids:List<String>):List<NativeCatalogSeen>
    @Query("SELECT * FROM cached_channels WHERE playlistId=:p AND generationId=:g AND channelId IN (:ids)")
    suspend fun channels(p:String,g:String,ids:List<String>):List<CachedChannelEntity>
    @Query("SELECT * FROM cached_movies WHERE playlistId=:p AND generationId=:g AND movieId IN (:ids)")
    suspend fun movies(p:String,g:String,ids:List<String>):List<CachedMovieEntity>
    @Query("SELECT * FROM cached_series WHERE playlistId=:p AND generationId=:g AND seriesId IN (:ids)")
    suspend fun series(p:String,g:String,ids:List<String>):List<CachedSeriesEntity>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun writeChannels(rows:List<CachedChannelEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun writeMovies(rows:List<CachedMovieEntity>)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun writeSeries(rows:List<CachedSeriesEntity>)
    @Query("UPDATE cached_channels SET channelNumber=:position WHERE playlistId=:p AND generationId=:g AND channelId=:id")
    suspend fun channelPosition(p:String,g:String,id:String,position:Int)
    @Query("UPDATE cached_movies SET providerOrder=:position WHERE playlistId=:p AND generationId=:g AND movieId=:id")
    suspend fun moviePosition(p:String,g:String,id:String,position:Int?)
    @Query("UPDATE cached_series SET providerOrder=:position WHERE playlistId=:p AND generationId=:g AND seriesId=:id")
    suspend fun seriesPosition(p:String,g:String,id:String,position:Int?)

    @Transaction
    suspend fun apply(batch:NativeBatch,initial:Boolean):NativeWriteStats {
        val (p,g,k,run)=batch
        check(isActive(p,g)){"A lista foi removida ou substituída."}
        val ids=batch.ids.distinct()
        if(ids.isEmpty())return NativeWriteStats()
        val already=seen(p,g,k,ids).asSequence().filter{it.runId==run}.map{it.itemId}.toHashSet()
        val incoming=ids.filterNot{it in already}
        if(incoming.isEmpty())return NativeWriteStats(duplicate=batch.ids.size)
        var inserted=0;var changed=0;var moved=0;var unchanged=0
        when(k) {
            "LIVE_TV"->{
                val old=if(initial)emptyMap()else channels(p,g,incoming).associateBy{it.channelId}
                val writes=ArrayList<CachedChannelEntity>()
                for(row in batch.channels.distinctBy{it.channelId}.filterNot{it.channelId in already}) {
                    val before=old[row.channelId]
                    when {
                        before==null->{writes+=row;inserted++}
                        before==row->unchanged++
                        before.copy(channelNumber=row.channelNumber)==row->{channelPosition(p,g,row.channelId,row.channelNumber);moved++}
                        else->{writes+=row;changed++}
                    }
                }
                if(writes.isNotEmpty())writeChannels(writes)
            }
            "MOVIES"->{
                val old=if(initial)emptyMap()else movies(p,g,incoming).associateBy{it.movieId}
                val writes=ArrayList<CachedMovieEntity>()
                for(row in batch.movies.distinctBy{it.movieId}.filterNot{it.movieId in already}) {
                    val before=old[row.movieId]
                    when {
                        before==null->{writes+=row;inserted++}
                        before==row->unchanged++
                        before.copy(providerOrder=row.providerOrder)==row->{moviePosition(p,g,row.movieId,row.providerOrder);moved++}
                        else->{writes+=row;changed++}
                    }
                }
                if(writes.isNotEmpty())writeMovies(writes)
            }
            else->{
                val old=if(initial)emptyMap()else series(p,g,incoming).associateBy{it.seriesId}
                val writes=ArrayList<CachedSeriesEntity>()
                for(row in batch.series.distinctBy{it.seriesId}.filterNot{it.seriesId in already}) {
                    val before=old[row.seriesId]
                    when {
                        before==null->{writes+=row;inserted++}
                        before==row->unchanged++
                        before.copy(providerOrder=row.providerOrder)==row->{seriesPosition(p,g,row.seriesId,row.providerOrder);moved++}
                        else->{writes+=row;changed++}
                    }
                }
                if(writes.isNotEmpty())writeSeries(writes)
            }
        }
        mark(incoming.map{NativeCatalogSeen(p,g,k,it,run)})
        return NativeWriteStats(inserted,changed,moved,unchanged,batch.ids.size-incoming.size)
    }

    @Query("DELETE FROM cached_channels WHERE playlistId=:p AND generationId=:g AND NOT EXISTS (SELECT 1 FROM native_catalog_seen n WHERE n.playlistId=:p AND n.generationId=:g AND n.kind='LIVE_TV' AND n.itemId=cached_channels.channelId AND n.runId=:run)")
    suspend fun pruneChannels(p:String,g:String,run:String):Int
    @Query("DELETE FROM cached_movies WHERE playlistId=:p AND generationId=:g AND NOT EXISTS (SELECT 1 FROM native_catalog_seen n WHERE n.playlistId=:p AND n.generationId=:g AND n.kind='MOVIES' AND n.itemId=cached_movies.movieId AND n.runId=:run)")
    suspend fun pruneMovies(p:String,g:String,run:String):Int
    @Query("DELETE FROM cached_series WHERE playlistId=:p AND generationId=:g AND NOT EXISTS (SELECT 1 FROM native_catalog_seen n WHERE n.playlistId=:p AND n.generationId=:g AND n.kind='SERIES' AND n.itemId=cached_series.seriesId AND n.runId=:run)")
    suspend fun pruneSeries(p:String,g:String,run:String):Int
    @Query("DELETE FROM cached_channels WHERE playlistId=:p AND generationId=:g AND categoryId IN (:scope) AND NOT EXISTS (SELECT 1 FROM native_catalog_seen n WHERE n.playlistId=:p AND n.generationId=:g AND n.kind='LIVE_TV' AND n.itemId=cached_channels.channelId AND n.runId=:run)")
    suspend fun pruneScopedChannels(p:String,g:String,run:String,scope:List<String>):Int
    @Query("DELETE FROM cached_movies WHERE playlistId=:p AND generationId=:g AND categoryId IN (:scope) AND NOT EXISTS (SELECT 1 FROM native_catalog_seen n WHERE n.playlistId=:p AND n.generationId=:g AND n.kind='MOVIES' AND n.itemId=cached_movies.movieId AND n.runId=:run)")
    suspend fun pruneScopedMovies(p:String,g:String,run:String,scope:List<String>):Int
    @Query("DELETE FROM cached_series WHERE playlistId=:p AND generationId=:g AND categoryId IN (:scope) AND NOT EXISTS (SELECT 1 FROM native_catalog_seen n WHERE n.playlistId=:p AND n.generationId=:g AND n.kind='SERIES' AND n.itemId=cached_series.seriesId AND n.runId=:run)")
    suspend fun pruneScopedSeries(p:String,g:String,run:String,scope:List<String>):Int
    @Query("DELETE FROM cached_episodes WHERE playlistId=:p AND generationId=:g AND NOT EXISTS (SELECT 1 FROM cached_series s WHERE s.playlistId=:p AND s.generationId=:g AND s.seriesId=cached_episodes.seriesId)")
    suspend fun pruneEpisodes(p:String,g:String)
    @Query("DELETE FROM native_catalog_seen WHERE playlistId=:p AND generationId=:g AND kind=:k AND runId!=:run")
    suspend fun clearOldSeen(p:String,g:String,k:String,run:String)
    @Query("SELECT COUNT(*) FROM cached_channels WHERE playlistId=:p AND generationId=:g") suspend fun channelCount(p:String,g:String):Int
    @Query("SELECT COUNT(*) FROM cached_movies WHERE playlistId=:p AND generationId=:g") suspend fun movieCount(p:String,g:String):Int
    @Query("SELECT COUNT(*) FROM cached_series WHERE playlistId=:p AND generationId=:g") suspend fun seriesCount(p:String,g:String):Int
    @Query("DELETE FROM cached_categories WHERE playlistId=:p AND generationId=:g AND type=:k AND categoryId NOT IN (:ids)")
    suspend fun pruneCategories(p:String,g:String,k:String,ids:List<String>)

    @Query("DELETE FROM cached_categories WHERE playlistId=:p AND generationId=:g AND type=:k AND NOT EXISTS (SELECT 1 FROM native_catalog_seen n WHERE n.playlistId=:p AND n.generationId=:g AND n.kind=('CAT:' || :k) AND n.itemId=cached_categories.categoryId AND n.runId=:run)")
    suspend fun pruneNativeCategories(p:String,g:String,k:String,run:String)

    /** Never called on a failed/truncated response. Empty replacement is deliberately guarded. */
    @Transaction
    suspend fun complete(p:String,g:String,kind:String,run:String,count:Long,priorCount:Int,categories:List<String>,full:Boolean):Int {
        check(isActive(p,g)){"A lista foi removida ou substituída."}
        check(count>0 || priorCount==0){"Resposta vazia inesperada; catálogo anterior preservado."}
        val deleted=when(kind){
            "LIVE_TV"->if(full)pruneChannels(p,g,run)else categories.chunked(800).sumOf{pruneScopedChannels(p,g,run,it)}
            "MOVIES"->if(full)pruneMovies(p,g,run)else categories.chunked(800).sumOf{pruneScopedMovies(p,g,run,it)}
            else->(if(full)pruneSeries(p,g,run)else categories.chunked(800).sumOf{pruneScopedSeries(p,g,run,it)}).also{pruneEpisodes(p,g)}
        }
        mark(categories.filterNot{it=="__none__"}.map{NativeCatalogSeen(p,g,"CAT:"+kind,it,run)})
        pruneNativeCategories(p,g,kind,run)
        clearOldSeen(p,g,kind,run)
        clearOldSeen(p,g,"CAT:"+kind,run)
        state(NativeCatalogState(p,g,kind,run,"COMPLETE",count,System.currentTimeMillis()))
        return deleted
    }
}
