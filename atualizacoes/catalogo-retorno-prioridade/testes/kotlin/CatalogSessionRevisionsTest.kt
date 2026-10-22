package br.com.flaviotorres.iptvpremium.data.cache

import org.junit.Test
import org.junit.Assert.*

class CatalogSessionRevisionsTest {
    @Test fun revisitingSectionsKeepsLoadedMetadataAndPagingSession() {
        val sessions=CatalogSessionRevisions();val binds=mutableListOf<String>();val refreshes=mutableListOf<String>()
        repeat(4) {
            for(section in listOf("channels","movies","series")) sessions.bind(section,"playlist","g1","access",
                {binds+=section},{refreshes+=section})
        }
        assertEquals(listOf("channels","movies","series"),binds)
        assertTrue(refreshes.isEmpty())
    }
    @Test fun generationAndParentalChangesInvalidateButChangingPlaylistOnlyBinds() {
        val sessions=CatalogSessionRevisions();var binds=0;var refreshes=0
        fun visit(p:String,g:String,a:String)=sessions.bind("movies",p,g,a,{binds++},{refreshes++})
        visit("a","1","allowed");visit("a","2","allowed");visit("a","2","kids")
        assertEquals(3,binds);assertEquals(2,refreshes)
        visit("b","1","kids");assertEquals(4,binds);assertEquals(2,refreshes)
        sessions.clear("movies");visit("b","1","kids");assertEquals(5,binds)
    }
    @Test fun failedBindingDoesNotMarkUnloadedSessionReady() {
        val sessions=CatalogSessionRevisions()
        runCatching {sessions.bind("series","p","g",null,{error("failed")},{})}
        var recovered=false
        sessions.bind("series","p","g",null,{recovered=true},{})
        assertTrue(recovered)
    }
}
