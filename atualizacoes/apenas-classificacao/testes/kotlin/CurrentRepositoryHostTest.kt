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
class CurrentRepositoryHostTest {
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
    @Test fun repositoryUsesSameOrderingForMoviesSeriesAndChannels():Unit=runBlocking {
        val names=listOf("XXX","Cursos","★ Especiais","4K","Legendados","Comédia","Ação","Lançamentos 2024","Lançamentos 2026")
        val dao=db.playlistCacheDao()
        for(type in listOf("MOVIES","SERIES","LIVE_TV")) dao.insertCategories(names.mapIndexed {i,name->
            CachedCategoryEntity("p","g","order-$i",name,type,"PROVIDER")
        })
        val expected=listOf("order-8","order-7","order-4","order-3","order-6","order-5","order-2","order-1","order-0")
        assertEquals(expected,repo.getLiveTvCategories("p").map{it.id}.filter{it.startsWith("order-")})
        assertEquals(expected.filter{it!="order-4"},repo.getMovieCategories("p").map{it.id}.filter{it.startsWith("order-")})
        assertEquals(expected.filter{it!="order-4"},repo.getSeriesCategories("p").map{it.id}.filter{it.startsWith("order-")})
    }

}
