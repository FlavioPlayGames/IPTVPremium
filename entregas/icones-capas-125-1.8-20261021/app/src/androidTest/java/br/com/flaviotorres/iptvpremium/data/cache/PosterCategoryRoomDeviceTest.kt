package br.com.flaviotorres.iptvpremium.data.cache

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.ui.kids.KidsSession
import br.com.flaviotorres.iptvpremium.ui.settings.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PosterCategoryRoomDeviceTest {
    private fun fixture(test:suspend(PlaylistCatalogRepository)->Unit)=runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val db=Room.inMemoryDatabaseBuilder(context,PlaylistCacheDatabase::class.java).build()
        val oldKids=KidsSession.active.value;val oldEnabled=ParentalSession.enabled.value
        val oldRules=ParentalPolicy.rules.value;val oldGrants=ParentalPolicy.grants.value
        try {
            KidsSession.active.value=false;ParentalSession.enabled.value=false
            ParentalPolicy.rules.value=ParentalRules();ParentalPolicy.grants.value=emptySet()
            val dao=db.playlistCacheDao()
            dao.upsertGeneration(PlaylistCacheGenerationEntity("fixture","generation","ACTIVE",0))
            dao.insertCategories(listOf("MOVIES","SERIES").flatMap { type->listOf(
                CachedCategoryEntity("fixture","generation","kids","KIDS Infantil",type,"XTREAM"),
                CachedCategoryEntity("fixture","generation","other","Drama",type,"XTREAM")) })
            dao.insertMovies((0 until 160).map { n->CachedMovieEntity("fixture","generation",n.toString(),
                "Title "+(159-n).toString().padStart(3,'0'),if(n%2==0)"kids"else"other",
                "https://example.invalid/m/$n",null,"https://example.invalid/movie/$n",2025,null,null,providerOrder=n) })
            dao.insertSeries((0 until 160).map { n->CachedSeriesEntity("fixture","generation",n.toString(),
                "Title "+(159-n).toString().padStart(3,'0'),if(n%2==0)"kids"else"other",
                "https://example.invalid/s/$n",null,2025,null,null,null,null,null,null,null,null,null,n) })
            test(PlaylistCatalogRepository(dao))
        } finally {
            KidsSession.active.value=oldKids;ParentalSession.enabled.value=oldEnabled
            ParentalPolicy.rules.value=oldRules;ParentalPolicy.grants.value=oldGrants
            db.close()
        }
    }
    private suspend fun all(repo:PlaylistCatalogRepository,kind:PosterKind,category:String?=null,
        search:String="",favorites:Boolean=false,ids:Set<String> = emptySet(),sort:String="DEFAULT"):List<String?> {
        val output=mutableListOf<String?>();var next:Int?=0
        while(next!=null) { val page=repo.posterPage(kind,"fixture",category,search,favorites,ids,sort,next)
            assertTrue(page.urls.size<=50);output.addAll(page.urls);next=page.next }
        return output
    }
    @Test fun moviesReadWholeCategoryInProviderOrderWithoutAdvancingVisiblePager()=fixture { repo->
        assertEquals((0 until 160).map { "https://example.invalid/m/$it" },all(repo,PosterKind.MOVIES))
    }
    @Test fun seriesReadWholeCategoryInProviderOrder()=fixture { repo->
        assertEquals((0 until 160).map { "https://example.invalid/s/$it" },all(repo,PosterKind.SERIES))
    }
    @Test fun categoryAndFavoritesOnlyDownloadMatchingPosters()=fixture { repo->
        val result=all(repo,PosterKind.MOVIES,"kids",favorites=true,ids=setOf("1","2","6","9"))
        assertEquals(listOf("https://example.invalid/m/2","https://example.invalid/m/6"),result)
    }
    @Test fun alphabeticalOrderMatchesCurrentGridOrder()=fixture { repo->
        assertEquals((159 downTo 0).map { "https://example.invalid/m/$it" },all(repo,PosterKind.MOVIES,sort="A_Z"))
    }
    @Test fun kidsNeverPrefetchesOutsideChildrensCategories()=fixture { repo->
        KidsSession.active.value=true
        for(kind in PosterKind.entries) {
            val result=all(repo,kind)
            assertEquals(80,result.size)
            assertTrue(result.all { it!!.substringAfterLast('/').toInt()%2==0 })
        }
    }
    @Test fun parentalRestrictionsApplyToBackgroundDownloads()=fixture { repo->
        ParentalSession.enabled.value=true
        ParentalPolicy.rules.value=ParentalRules(categories=mapOf("fixture/MOVIES" to setOf("kids")))
        val result=all(repo,PosterKind.MOVIES)
        assertEquals(80,result.size)
        assertTrue(result.all { it!!.substringAfterLast('/').toInt()%2==1 })
    }
}
