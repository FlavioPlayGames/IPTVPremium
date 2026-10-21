package br.com.flaviotorres.iptvpremium.ui.kids

import br.com.flaviotorres.iptvpremium.ui.movies.movieHoldReady
import org.junit.Assert.*
import org.junit.Test

class CatalogCategoryOrderDeviceTest {
    private data class Entry(val id: Int, val name: String)
    private fun ordered(names: List<String>) = names.sortedWith(CatalogSections.categoryComparator { it })

    @Test fun launchesLeadThenSubtitles4kAndAlphabeticalCategories() {
        assertEquals(listOf("Lançamentos 2026", "Lançamentos 2025", "Legendados", "4K", "Ação", "Comédia"),
            ordered(listOf("Comédia", "4K", "Legendados", "Lançamentos 2025", "Ação", "Lançamentos 2026")))
    }
    @Test fun everyMediaPrefixUsesTheSameRankAndName() {
        for (prefix in listOf("Filmes | ", "Séries | ", "Canais | ", "Channels | ")) {
            assertEquals(listOf(prefix+"Lançamentos",prefix+"4K",prefix+"Ação",prefix+"Comédia"),
                ordered(listOf(prefix+"Comédia",prefix+"Ação",prefix+"4K",prefix+"Lançamentos")))
        }
    }
    @Test fun restrictedTrainingAndLeadingSymbolsTrailOrdinaryCategories() {
        val trailing=listOf("XXX", "+18", "Adultos 4K", "Cursos Lançamentos", "★ Lançamentos", "[4K]")
        val result=ordered(trailing+listOf("Ação", "4K", "Lançamentos"))
        assertEquals(listOf("Lançamentos","4K","Ação"),result.take(3))
        assertEquals(trailing.toSet(),result.drop(3).toSet())
    }
    @Test fun lockBadgeDoesNotPushOrdinaryCategoryToTheEnd() {
        assertEquals(listOf("🔒 Lançamentos", "4K", "🔒 Ação", "Comédia", "🔒 XXX"),
            ordered(listOf("Comédia", "🔒 XXX", "🔒 Ação", "4K", "🔒 Lançamentos")))
    }
    @Test fun sortingPreservesProviderIdsDuplicatesAndStableEqualNames() {
        val input=listOf(Entry(3,"Comédia"),Entry(2,"Ação"),Entry(1,"Ação"))
        val result=input.sortedWith(CatalogSections.categoryComparator { it.name })
        assertEquals(listOf(2,1,3),result.map { it.id })
        assertEquals(3,result.size)
    }
    @Test fun channelFavoriteGateRequiresAnEntireSecond() {
        for(elapsed in listOf(0L,100L,500L,600L,999L)) assertFalse(movieHoldReady(elapsed,1000L,true,false))
        assertTrue(movieHoldReady(1000L,1000L,true,false))
        assertTrue(movieHoldReady(2000L,1000L,true,false))
    }
    @Test fun cancelledOrUnfocusedPressCannotFavorite() {
        assertFalse(movieHoldReady(2000L,1000L,false,false))
        assertFalse(movieHoldReady(2000L,1000L,true,true))
    }
}
