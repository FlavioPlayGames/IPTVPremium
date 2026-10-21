package br.com.flaviotorres.iptvpremium.ui.kids

import br.com.flaviotorres.iptvpremium.ui.settings.*
import br.com.flaviotorres.iptvpremium.data.cache.PosterAheadQueue
import org.junit.Assert.*
import org.junit.Test

class KidsPolicyHostTest {
    @Test fun categoryClassificationIsConservativeAndAccentInsensitive() {
        for(name in listOf("FILMES | INFANTIL", "DESENHOS", "Kids", "Família", "Crianças", "Cartoon")) assertTrue(name,KidsContent.category(name))
        for(name in listOf("Animes", "Animação", "Disney+", "Ação", "", "INFANTIL XXX", "Kids 18+")) assertFalse(name,KidsContent.category(name))
    }
    @Test fun isolatedSubtitleMarkerDoesNotMatchLettersInsideWords() {
        for(name in listOf("Filme [L]", "Série (L)", "Filme - L", "Filme Legendado")) assertTrue(name,CatalogSections.subtitled(name))
        for(name in listOf("Lorne", "ALMA", "Lilo & Stitch", "Filme Dublado")) assertFalse(name,CatalogSections.subtitled(name))
    }
    @Test fun releaseYearsThenFourKThenAlphabetical() {
        val names=listOf("Filmes | Zumbi", "4K", "Lançamentos 2024", "Lançamentos 2026", "Ação", "Legendados")
        val ordered=names.sortedWith(compareBy<String>{CatalogSections.priority(it)}.thenByDescending { if(CatalogSections.priority(it)==0)CatalogSections.year(it) else 0 }.thenBy { CatalogSections.orderName(it) })
        assertEquals(listOf("Lançamentos 2026","Lançamentos 2024","4K","Ação","Legendados","Filmes | Zumbi"),ordered)
    }
    @Test fun kidsLockCannotBeBypassedBySectionGrants() {
        val outside=ParentalAccessState(true,ParentalRules(kidsOnly=true),setOf("p/MOVIES"),false)
        assertTrue(outside.blocked("p","MOVIES","kids",categoryName="Infantil"))
        val inside=outside.copy(kidsCatalog=true)
        assertFalse(inside.blocked("p","MOVIES","kids",categoryName="Infantil"))
        assertTrue(inside.blocked("p","MOVIES","adult",categoryName="Ação"))
    }
    @Test fun kidsFilteringAlsoAppliesWhenPinIsDisabled() {
        val access=ParentalAccessState(false,ParentalRules(),emptySet(),true)
        assertTrue(access.hasRestrictions("p","MOVIES"))
        assertTrue(access.blocked("p","MOVIES","a",categoryName="Adultos"))
    }
    @Test fun initialThirtyThenAddsThirtyInGroupsOfFive() {
        val queue=PosterAheadQueue();val batches=mutableListOf<IntRange>()
        while(true) { val b=queue.nextBatch(10,0,100)?:break;batches+=b;queue.completed(b) }
        assertEquals(6,batches.size);assertEquals(10..14,batches.first());assertEquals(35..39,batches.last())
        assertNull(queue.nextBatch(14,0,100));assertEquals(40..44,queue.nextBatch(15,0,100))
    }
    @Test fun sixDigitPinHasHardLimitOnNormalizeAppendAndReady() {
        assertEquals("123456",PinDigits.normalize("a12x3456789"))
        assertEquals("123456",PinDigits.append("123456",7))
        assertTrue(PinDigits.ready("123456"));assertFalse(PinDigits.ready("12345"));assertFalse(PinDigits.ready("1234567"))
    }
}
