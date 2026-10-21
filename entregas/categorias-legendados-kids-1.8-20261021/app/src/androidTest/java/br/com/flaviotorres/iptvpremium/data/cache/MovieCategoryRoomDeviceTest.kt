package br.com.flaviotorres.iptvpremium.data.cache

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import br.com.flaviotorres.iptvpremium.ui.settings.*
import br.com.flaviotorres.iptvpremium.ui.kids.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MovieCategoryRoomDeviceTest {
    private fun fixture(test:suspend(PlaylistCacheDatabase,PlaylistCatalogRepository)->Unit)=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),PlaylistCacheDatabase::class.java).build()
        val oldKids=KidsSession.active.value;val oldEnabled=ParentalSession.enabled.value
        val oldRules=ParentalPolicy.rules.value;val oldGrants=ParentalPolicy.grants.value
        try {
            KidsSession.active.value=false;ParentalSession.enabled.value=false
            ParentalPolicy.rules.value=ParentalRules();ParentalPolicy.grants.value=emptySet()
            val dao=db.playlistCacheDao();dao.upsertGeneration(PlaylistCacheGenerationEntity("p","g","ACTIVE",0))
            dao.insertCategories(listOf("MOVIES","SERIES").flatMap { type->listOf(
                CachedCategoryEntity("p","g","action","Ação",type,"XTREAM"),
                CachedCategoryEntity("p","g","kids","Kids Infantil",type,"XTREAM"),
                CachedCategoryEntity("p","g","leg","Filmes | Legendados",type,"XTREAM")) })
            val titles=listOf("Filme normal","Filme [L]","Filme [LEG]","Filme Legendado","Sem marca","Kids normal","Kids [L]","Sem categoria")
            dao.insertMovies(titles.mapIndexed { n,title->CachedMovieEntity("p","g",n.toString(),title,
                when(n){4->"leg";5,6->"kids";7->null;else->"action"},"https://example.invalid/m/$n",null,"https://example.invalid/play/$n",2025,null,null,providerOrder=n) })
            dao.insertSeries(listOf("kids","action").mapIndexed { n,cat->CachedSeriesEntity("p","g",n.toString(),"Series $n",cat,null,null,2025,null,null,null,null,null,null,null,null,null,n) })
            test(db,PlaylistCatalogRepository(dao))
        } finally {KidsSession.active.value=oldKids;ParentalSession.enabled.value=oldEnabled;ParentalPolicy.rules.value=oldRules;ParentalPolicy.grants.value=oldGrants;db.close()}
    }
    private suspend fun <T:Any> read(source:PagingSource<Int,T>):List<T> = try {
        when(val result=source.load(PagingSource.LoadParams.Append(0,50,true))) {
            is PagingSource.LoadResult.Page->result.data
            is PagingSource.LoadResult.Error->throw result.throwable
            else->error("Invalid query")
        }
    } finally {source.invalidate()}
    @Test fun allAndGenreSimpleQueriesExcludeSubtitlesIncludingNullCategory()=fixture { db,_->
        val dao=db.playlistCacheDao()
        assertEquals(listOf("0","5","7"),read(dao.getMoviesPagingSource("p","g")).map {it.movieId})
        assertEquals(listOf("0"),read(dao.getMoviesPagingSourceByCategory("p","g","action")).map {it.movieId})
    }
    @Test fun searchAndSortingStillExcludeSubtitlesOutsideOwnCategory()=fixture { db,_->
        val dao=db.playlistCacheDao()
        assertEquals(listOf("0"),read(dao.getMoviesPagingSourceFiltered("p","g","action","Filme",false,listOf("none"),"A_Z")).map {it.movieId})
    }
    @Test fun dedicatedSubtitleCategoryFindsAllMarkersAndProviderCategory()=fixture { db,_->
        assertEquals(listOf("1","2","3","4","6"),read(db.playlistCacheDao().getMoviesPagingSourceFiltered("p","g",null,"",false,listOf("none"),"DEFAULT",subtitledOnly=true)).map {it.movieId})
    }
    @Test fun personalFavoritesAndIdentityLookupStillIncludeSubtitleMovies()=fixture { db,_->
        val dao=db.playlistCacheDao()
        assertEquals(listOf("1","4"),read(dao.getMoviesPagingSourceFiltered("p","g",null,"",true,listOf("1","4"),"DEFAULT")).map {it.movieId})
        assertEquals(listOf("1","6"),read(dao.getMoviesPagingSourceFiltered("p","g",null,"[L]",false,listOf("none"),"DEFAULT",includeSubtitled=true)).map {it.movieId})
    }
    @Test fun countsMatchExclusiveGridAndDedicatedSubtitleCategory()=fixture { db,_->
        val dao=db.playlistCacheDao();assertEquals(3,dao.getMovieCount("p","g"))
        assertEquals(5,dao.getSubtitledMovieCount("p","g",emptyList(),emptyList(),false))
        assertEquals(mapOf(null to 1,"action" to 1,"kids" to 1),dao.getMovieCountsByCategory("p","g").associate {it.categoryId to it.itemCount})
    }
    @Test fun prefetchObeysSameExclusivityAndFavoritesException()=fixture { _,repo->
        assertEquals(listOf(0,5,7).map {"https://example.invalid/m/$it"},repo.posterPage(PosterKind.MOVIES,"p",null,"",false,emptySet(),"DEFAULT",0).urls)
        assertEquals(listOf("https://example.invalid/m/1"),repo.posterPage(PosterKind.MOVIES,"p",null,"",true,setOf("1"),"DEFAULT",0).urls)
    }
    private fun histories()=listOf(
        WatchHistoryEntry("p",PlaybackContentType.MOVIE,"0",title="Normal",positionMs=20000,durationMs=null,updatedAtMs=0),
        WatchHistoryEntry("p",PlaybackContentType.MOVIE,"5",title="Kids",positionMs=20000,durationMs=null,updatedAtMs=0),
        WatchHistoryEntry("p",PlaybackContentType.MOVIE,"6",title="Kids legendado",positionMs=20000,durationMs=null,updatedAtMs=0),
        WatchHistoryEntry("p",PlaybackContentType.EPISODE,"episode-kids",seriesId="0",title="Kids episode",positionMs=20000,durationMs=null,updatedAtMs=0),
        WatchHistoryEntry("p",PlaybackContentType.EPISODE,"episode-normal",seriesId="1",title="Normal episode",positionMs=20000,durationMs=null,updatedAtMs=0),
        WatchHistoryEntry("p",PlaybackContentType.EPISODE,"missing",seriesId="missing",title="Unknown",positionMs=20000,durationMs=null,updatedAtMs=0))
    @Test fun kidsContinueContainsOnlyKidsEvenWithoutParentalPin()=fixture { _,repo->
        KidsSession.active.value=true;ParentalSession.enabled.value=false
        assertEquals(listOf("5","6","episode-kids"),repo.filterHistoryForCatalog("p",histories()).map {it.contentId})
    }
    @Test fun normalHistoryRetainsExistingProgressAndFavoritesInKidsStayFiltered()=fixture { db,repo->
        assertEquals(histories(),repo.filterHistoryForCatalog("p",histories()))
        KidsSession.active.value=true
        val page=repo.posterPage(PosterKind.MOVIES,"p",null,"",true,setOf("0","5","6"),"DEFAULT",0)
        assertEquals(listOf("https://example.invalid/m/5","https://example.invalid/m/6"),page.urls)
    }
}
