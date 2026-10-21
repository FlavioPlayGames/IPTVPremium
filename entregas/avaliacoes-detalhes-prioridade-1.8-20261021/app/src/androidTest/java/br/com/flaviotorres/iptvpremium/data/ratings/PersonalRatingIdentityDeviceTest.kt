package br.com.flaviotorres.iptvpremium.data.ratings

import org.junit.Assert.*
import org.junit.Test

class PersonalRatingIdentityDeviceTest {
    @Test fun officialIdentityIsPreferredAndIsStableAcrossLists() {
        val a=PersonalRatingIdentity("movies","a","1","100","tt123").keys()
        val b=PersonalRatingIdentity("movies","b","9","100","tt123").keys()
        assertEquals(3,a.size);assertEquals(a.take(2),b.take(2));assertNotEquals(a.last(),b.last())
    }
    @Test fun movieAndSeriesScoresNeverShareAKey() {
        assertTrue(PersonalRatingIdentity("movies","p","1","1","tt1").keys().toSet()
            .intersect(PersonalRatingIdentity("series","p","1","1","tt1").keys().toSet()).isEmpty())
    }
    @Test fun localScopeDoesNotCollideAcrossPlaylistsOrProviderIds() {
        val keys=listOf("a" to "1","a" to "2","b" to "1").map {(p,id)->PersonalRatingIdentity("movies",p,id,null,null).keys().single()}
        assertEquals(3,keys.distinct().size)
    }
    @Test fun noIdentifierDoesNotInventTitleBasedRatings() {
        assertTrue(PersonalRatingIdentity("movies",null,null,"0","invalid").keys().isEmpty())
    }
    @Test fun storedKeysAreHashesWithoutCredentialsOrRawIds() {
        val keys=PersonalRatingIdentity("movies","private-playlist","private-provider","123","tt123").keys()
        assertTrue(keys.all {it.matches(Regex("[a-f0-9]{64}"))})
    }
    @Test fun numericIdsAndImdbCaseAreNormalized() {
        assertEquals(PersonalRatingIdentity("movies",null,null,"00123","TT123").keys(),PersonalRatingIdentity("movies",null,null,"123","tt123").keys())
    }
    @Test fun reusedProviderIdDoesNotMatchAnotherTitle() {
        val first=PersonalRatingIdentity("movies","p","1",null,null,"First film",2025).keys()
        val second=PersonalRatingIdentity("movies","p","1",null,null,"Another film",2025).keys()
        assertTrue(first.toSet().intersect(second.toSet()).isEmpty())
    }

}
