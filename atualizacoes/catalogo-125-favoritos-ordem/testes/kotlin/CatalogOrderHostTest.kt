package br.com.flaviotorres.iptvpremium.ui.kids
import org.junit.Test
import org.junit.Assert.*
class CatalogOrderHostTest {
    @Test fun allTypesUseMovieRulesWithYearsAndSpecialNamesLast(){
        val expected=listOf("Lançamentos 2026","Lançamentos 2024","Legendados","4K","Ação","Comédia","2025","★ Especiais","Cursos","XXX")
        // Numeric catalogue names sort alphabetically in the ordinary group.
        val actualExpected=expected.toMutableList().apply{remove("2025");add(4,"2025")}
        for(prefix in listOf("Filmes | ","Séries | ","Canais | ")) {
            val names=expected.reversed().map {prefix+it}
            val actual=names.sortedWith(CatalogSections.categoryOrder<String>{it}).map {it.removePrefix(prefix)}
            assertEquals(actualExpected,actual)
        }
    }
    @Test fun sortingPreservesDuplicateDisplayNamesAndProviderIds(){
        data class Item(val id:String,val name:String)
        val items=listOf(Item("b","Comédia"),Item("a","Ação"),Item("a2","Ação"))
        assertEquals(listOf("a","a2","b"),items.sortedWith(CatalogSections.categoryOrder<Item>{it.name}).map {it.id})
    }
    @Test fun locksAccentsAndFullWidthSeparatorDontChangeOrdering(){
        assertEquals(CatalogSections.orderName("Filmes | Ação"),CatalogSections.orderName("🔒 Séries｜Ação"))
        assertEquals(listOf("Água","Bichos"),listOf("Bichos","Água").sortedWith(CatalogSections.categoryOrder<String>{it}))
    }
}
