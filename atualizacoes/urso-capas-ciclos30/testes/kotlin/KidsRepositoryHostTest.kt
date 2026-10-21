package br.com.flaviotorres.iptvpremium.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.paging.testing.asSnapshot
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.ui.kids.*
import br.com.flaviotorres.iptvpremium.ui.settings.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35])
class KidsRepositoryHostTest {
    private lateinit var db:PlaylistCacheDatabase
    private lateinit var repo:PlaylistCatalogRepository
    @Before fun setup():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        db=Room.inMemoryDatabaseBuilder(context,PlaylistCacheDatabase::class.java).allowMainThreadQueries().build()
        val dao=db.playlistCacheDao();repo=PlaylistCatalogRepository(dao)
        ParentalSession.enabled.value=false;ParentalPolicy.rules.value=ParentalRules(general=false)
        ParentalPolicy.grants.value=emptySet();KidsSession.active.value=false
        dao.upsertGeneration(PlaylistCacheGenerationEntity("p","g","ACTIVE",1,2,3))
        dao.insertCategories(listOf("MOVIES","SERIES","LIVE_TV").flatMap { type -> listOf(
            CachedCategoryEntity("p","g","kid","Infantil",type,"PROVIDER"),
            CachedCategoryEntity("p","g","adult","Ação",type,"PROVIDER"),
            CachedCategoryEntity("p","g","anime","Animes",type,"PROVIDER"),
            CachedCategoryEntity("p","g","leg","Legendados",type,"PROVIDER")) })
        dao.insertMovies(listOf(movie("kidL","Toy [L]","kid"),movie("kidD","Toy Dublado","kid"),
            movie("adultL","Crime (L)","adult"),movie("animeL","Anime [L]","anime"),
            movie("legacy","Sem marcador","leg"),movie("unknown","Sem categoria [L]",null)))
        dao.insertSeries(listOf(series("kidL","Toy [L]","kid"),series("kidD","Toy Dublado","kid"),
            series("adultL","Crime (L)","adult"),series("legacy","Sem marcador","leg"),series("unknown","Sem categoria [L]",null)))
        dao.insertChannels(listOf(CachedChannelEntity("p","g","kid1","Kids 1","kid",null,"http://example/k1",null,1),
            CachedChannelEntity("p","g","kid2","Kids 2","kid",null,"http://example/k2",null,200),
            CachedChannelEntity("p","g","unknown","Infantil sem categoria",null,null,"http://example/u",null,300))+
            (2..120).map { CachedChannelEntity("p","g","adult$it","Canal $it","adult",null,"http://example/a$it",null,it) })
    }
    private fun movie(id:String,name:String,cat:String?)=CachedMovieEntity("p","g",id,name,cat,"http://example/$id.jpg",null,"http://example/$id",2026,8.0,90)
    private fun series(id:String,name:String,cat:String?)=CachedSeriesEntity("p","g",id,name,cat,"http://example/$id.jpg",null,2026,8.0,null,null,null,null,null,null,null,null)
    @After fun cleanup() { KidsSession.active.value=false;ParentalSession.enabled.value=false;ParentalPolicy.rules.value=ParentalRules();ParentalPolicy.grants.value=emptySet();db.close() }
    @Test fun kidsMoviesSeriesAndChannelsOnlyContainIdentifiedCategories():Unit=runBlocking {
        KidsSession.active.value=true
        assertEquals(setOf("kidL","kidD"),withTimeout(10000){repo.movies("p").asSnapshot()}.map {it.id}.toSet())
        assertEquals(setOf("kidL","kidD"),withTimeout(10000){repo.series("p").asSnapshot()}.map {it.id}.toSet())
        assertEquals(setOf("kid1","kid2"),withTimeout(10000){repo.channels("p").asSnapshot()}.map {it.id}.toSet())
        assertEquals(2,repo.getMovieCount("p"));assertEquals(2,repo.getSeriesCount("p"));assertEquals(2,repo.getChannelCount("p"))
    }
    @Test fun subtitleCatalogAggregatesMarkersAndProviderCategoryWithoutCopies():Unit=runBlocking {
        val before=db.playlistCacheDao().getMovieCount("p","g")
        val movies=withTimeout(10000){repo.movies("p",CatalogSections.SUBTITLED).asSnapshot()}
        assertEquals(setOf("kidL","adultL","animeL","legacy","unknown"),movies.map {it.id}.toSet())
        assertEquals(setOf("kidL","adultL","legacy","unknown"),withTimeout(10000){repo.series("p",CatalogSections.SUBTITLED).asSnapshot()}.map {it.id}.toSet())
        assertEquals(before,db.playlistCacheDao().getMovieCount("p","g"))
        KidsSession.active.value=true
        assertEquals(listOf("kidL"),withTimeout(10000){repo.movies("p",CatalogSections.SUBTITLED).asSnapshot()}.map {it.id})
        assertEquals(1,repo.getMovieCountsByCategory("p")[CatalogSections.SUBTITLED])
    }
    @Test fun directLinksFavoritesAndUnknownCategoriesCannotBypassKids():Unit=runBlocking {
        KidsSession.active.value=true
        assertNull(repo.getMovieById("p","adultL"));assertNull(repo.getSeriesById("p","adultL"));assertNull(repo.getChannelById("p","unknown"))
        assertTrue(repo.isParentalBlocked("p","MOVIES","unknown"));assertTrue(repo.isParentalBlocked("p","LIVE_TV","unknown"))
        assertEquals(listOf("kidL"),withTimeout(10000){repo.movies("p",favoritesOnly=true,favoriteMovieIds=setOf("kidL","adultL")).asSnapshot()}.map {it.id})
    }
    @Test fun channelUpAndDownSkipMoreThanSixtyFourNonKidsChannels():Unit=runBlocking {
        KidsSession.active.value=true
        assertEquals("kid2",repo.getAdjacentChannel("p","kid1",1)?.id)
        assertEquals("kid1",repo.getAdjacentChannel("p","kid2",-1)?.id)
        assertEquals("kid1",repo.getAdjacentChannel("p","kid2",1)?.id)
    }
    @Test fun enablingKidsLockBlocksNormalCatalogEvenWithGrants():Unit=runBlocking {
        ParentalSession.enabled.value=true;ParentalPolicy.rules.value=ParentalRules(general=false,kidsOnly=true)
        ParentalPolicy.grants.value=setOf("p/MOVIES","p/SERIES")
        assertTrue(withTimeout(10000){repo.movies("p").asSnapshot()}.isEmpty())
        assertNull(repo.getMovieById("p","kidL"))
        KidsSession.active.value=true
        assertEquals(2,withTimeout(10000){repo.movies("p").asSnapshot()}.size)
        KidsSession.active.value=false
        ParentalPolicy.rules.value=ParentalRules(general=false,kidsOnly=false)
        assertEquals(6,withTimeout(10000){repo.movies("p").asSnapshot()}.size)
    }
    @Test fun enteringAndLeavingKidsDoesNotDuplicateRows():Unit=runBlocking {
        repeat(3) { KidsSession.active.value=true;assertEquals(2,repo.getMovieCount("p"));KidsSession.active.value=false;assertEquals(6,repo.getMovieCount("p")) }
        assertEquals(6,db.playlistCacheDao().getMovieCount("p","g"))
        assertEquals(5,db.playlistCacheDao().getSeriesCount("p","g"))
    }
}
