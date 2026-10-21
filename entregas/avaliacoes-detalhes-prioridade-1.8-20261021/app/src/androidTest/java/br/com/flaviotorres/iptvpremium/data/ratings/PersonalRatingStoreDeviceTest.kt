package br.com.flaviotorres.iptvpremium.data.ratings

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.core.storage.clearExperienceCache
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class PersonalRatingStoreDeviceTest {
    private fun fixture(test:(Context,PersonalRatingStore)->Unit) {
        val app=ApplicationProvider.getApplicationContext<Context>()
        val prefix="rating-test-${UUID.randomUUID()}-"
        val wrapped=object:ContextWrapper(app) {
            override fun getApplicationContext():Context=this
            override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=app.getSharedPreferences(prefix+name,mode)
        }
        try{test(wrapped,PersonalRatingStore(wrapped))}finally{app.deleteSharedPreferences(prefix+"iptv_personal_ratings_v1")}
    }
    private fun id(kind:String="movies",playlist:String="p",content:String="1",tmdb:String?="123")=PersonalRatingIdentity(kind,playlist,content,tmdb,null)
    @Test fun scoreSurvivesStoreRecreation()=fixture {context,store->store.save(id(),4);assertEquals(4,PersonalRatingStore(context).read(id()))}
    @Test fun moviesAndSeriesRemainIndependent()=fixture {_,store->store.save(id(),5);store.save(id("series"),2);assertEquals(5,store.read(id()));assertEquals(2,store.read(id("series")))}
    @Test fun ratingsWithOnlyProviderIdsStayScopedToPlaylist()=fixture {_,store->store.save(id(tmdb=null),3);assertNull(store.read(id(playlist="other",tmdb=null)));assertNull(store.read(id(content="other",tmdb=null)))}
    @Test fun sameOfficialMovieInAnotherListUsesPersonalScore()=fixture {_,store->store.save(id(),4);assertEquals(4,store.read(id(playlist="other",content="other")))}
    @Test fun metadataArrivingLaterKeepsLocalScore()=fixture {_,store->store.save(id(tmdb=null),2);assertEquals(2,store.read(id()))}
    @Test fun removingScoreDoesNotResurrectOlderAliasInAnotherList()=fixture {_,store->
        val another=id(playlist="other",content="other");store.save(id(),3);store.save(another,4);store.save(id(),null)
        assertNull(store.read(id()));assertNull(store.read(another))
    }
    @Test fun invalidScoresAreRejectedWithoutOverwritingExistingScore()=fixture {_,store->
        store.save(id(),4)
        for(value in listOf(0,6,-1)) {var rejected=false;try{store.save(id(),value)}catch(_:IllegalArgumentException){rejected=true};assertTrue(rejected)}
        assertEquals(4,store.read(id()))
    }
    @Test fun clearingDisposableCacheKeepsPersonalScore()=fixture {context,store->
        store.save(id(),5);val root=File(context.cacheDir,"ratings-cache-test-${UUID.randomUUID()}").apply{mkdirs()}
        try{File(root,"poster.img").writeText("fixture");clearExperienceCache(root,null);assertEquals(5,PersonalRatingStore(context).read(id()))}
        finally{root.deleteRecursively()}
    }
    @Test fun intermittentExternalMetadataKeepsTheNewestChoice()=fixture {_,store->
        val full=PersonalRatingIdentity("movies","p","1","123","tt123")
        store.save(full,4);store.save(id(),2);assertEquals(2,store.read(full))
        assertEquals(2,store.read(PersonalRatingIdentity("movies","another","other",null,"tt123")))
    }
    @Test fun lateMetadataPromotesLocalScoreToOfficialIdentity()=fixture {_,store->
        store.save(id(tmdb=null),3);assertEquals(3,store.read(id()))
        assertEquals(3,store.read(id(playlist="another",content="other")))
    }

    @Test fun reusedLocalIdCannotTransferRatingToAnotherMovie()=fixture {_,store->
        val before=PersonalRatingIdentity("movies","p","1","123",null,"Original film",2025)
        val after=PersonalRatingIdentity("movies","p","1","456",null,"Different film",2025)
        store.save(before,5);assertNull(store.read(after));assertEquals(5,store.read(before))
    }

}
