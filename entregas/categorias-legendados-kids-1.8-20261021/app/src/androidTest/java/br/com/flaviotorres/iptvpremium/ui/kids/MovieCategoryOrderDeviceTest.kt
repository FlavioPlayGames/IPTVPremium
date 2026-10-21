package br.com.flaviotorres.iptvpremium.ui.kids

import org.junit.Assert.*
import org.junit.Test

class MovieCategoryOrderDeviceTest {
    @Test fun launchesSubtitlesAnd4kLeadTheMenu() {
        assertEquals(listOf("Lançamentos", "Legendados", "4K", "Ação"),
            listOf("Ação", "4K", "Legendados", "Lançamentos").sortedBy(CatalogSections::moviePriority))
    }
    @Test fun adultsAndCoursesAlwaysTrailEvenWithReleaseWords() {
        for(name in listOf("XXX","Adultos","Adultas","+18","18+","Cursos", "Cursos Lançamentos", "Adultos 4K")) assertEquals(name,4,CatalogSections.moviePriority(name))
    }
    @Test fun leadingSymbolsOverrideTopPriority() {
        for(name in listOf("★ Lançamentos", "[4K]", "+ Legendados", "🍿 Cinema", "| Filmes")) assertEquals(name,4,CatalogSections.moviePriority(name))
    }
    @Test fun accentedLettersAndNumbersAreNotSymbols() {
        assertEquals(3,CatalogSections.moviePriority("Áção"));assertEquals(3,CatalogSections.moviePriority("2020"))
    }
    @Test fun mediaPrefixDoesNotHideSymbolsOrPriority() {
        assertEquals(0,CatalogSections.moviePriority("FILMES | Lançamentos"))
        assertEquals(4,CatalogSections.moviePriority("Filmes | ★ Ação"))
        assertEquals(2,CatalogSections.moviePriority("Filmes – 4K"))
    }
    @Test fun appLockIndicatorDoesNotChangeCategoryRank() {
        assertEquals(0,CatalogSections.moviePriority("🔒 Lançamentos"))
        assertEquals(3,CatalogSections.moviePriority("🔒 Ação"))
    }
    @Test fun seriesPriorityAndKidsRecognitionRemainAvailable() {
        assertEquals(1,CatalogSections.priority("4K"));assertTrue(KidsContent.category("ANIMAÇÃO / INFANTIL"))
        assertFalse(KidsContent.category("Adultos Kids"))
    }
}
