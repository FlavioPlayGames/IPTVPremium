package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.domain.model.CategoryType
import org.junit.Assert.*
import org.junit.Test

class CategoryImportPolicyDeviceTest {
    private val movies = List(35) { CategoryImportTask(CategoryType.MOVIES, "$it") }
    @Test fun thirtyFiveCategoriesProduceFiveWindowsOfSeven() {
        val done = linkedSetOf<String>()
        repeat(5) {
            val group = CategoryImportPolicy.next(movies, done, null)
            assertEquals(7, group.size); done.addAll(group.map { it.key })
        }
        assertTrue(CategoryImportPolicy.next(movies, done, null).isEmpty())
    }
    @Test fun openingSeriesMovesItAheadOfMovies() {
        val series = CategoryImportTask(CategoryType.SERIES, "series")
        assertEquals(series, CategoryImportPolicy.next(movies + series, emptySet(), CatalogAccess("p", CategoryType.SERIES)).first())
    }
    @Test fun selectedCategoryWinsWithinItsSection() {
        assertEquals(movies[30], CategoryImportPolicy.next(movies, emptySet(), CatalogAccess("p", CategoryType.MOVIES, "30")).first())
    }
    @Test fun importedCategoryIsNeverQueuedAgain() {
        val done = setOf(movies[30].key)
        assertFalse(CategoryImportPolicy.next(movies, done, CatalogAccess("p", CategoryType.MOVIES, "30")).contains(movies[30]))
    }
    @Test fun defaultOrderIsMoviesSeriesChannels() {
        assertEquals(listOf(CategoryType.MOVIES, CategoryType.SERIES, CategoryType.LIVE_TV), CategoryImportPolicy.order)
    }
    @Test fun equalIdsInDifferentSectionsAreIndependent() {
        val a = CategoryImportTask(CategoryType.MOVIES, "1")
        val b = CategoryImportTask(CategoryType.LIVE_TV, "1")
        assertEquals(listOf(b), CategoryImportPolicy.next(listOf(a,b), setOf(a.key), null))
    }
    @Test fun partialLastWindowHasNoDuplicates() {
        assertEquals(2, CategoryImportPolicy.next(movies, movies.take(33).map { it.key }.toSet(), null).size)
        assertEquals(7, CategoryImportPolicy.next(movies+movies, emptySet(), null).map { it.key }.distinct().size)
    }
    @Test fun sectionTransportRemainsXtream() {
        assertEquals("get_vod_streams", movies.first().endpoint)
        assertEquals("get_series", CategoryImportTask(CategoryType.SERIES, "1").endpoint)
        assertEquals("get_live_streams", CategoryImportTask(CategoryType.LIVE_TV, "1").endpoint)
    }
    @Test fun standardItemBatchesStay250And2000() {
        assertEquals(250, br.com.flaviotorres.iptvpremium.data.cache.ImportProfile.DEFAULT.batchSize)
        assertEquals(2000, br.com.flaviotorres.iptvpremium.data.cache.ImportProfile.DEFAULT.m3uBatchSize)
    }
}
