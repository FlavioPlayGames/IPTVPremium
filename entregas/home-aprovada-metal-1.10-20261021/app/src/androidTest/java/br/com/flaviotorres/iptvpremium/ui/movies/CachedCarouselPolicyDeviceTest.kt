package br.com.flaviotorres.iptvpremium.ui.movies

import org.junit.Assert.*
import org.junit.Test

class CachedCarouselPolicyDeviceTest {
    private data class Poster(val id: String, val url: String?)
    private fun select(items: List<Poster>, ram: Set<String> = emptySet(), disk: Set<String> = emptySet(), limit: Int = 20) =
        CachedCarouselPolicy.select("selected", items, { it.id }, { it.url }, ram, disk, limit)

    @Test fun selectedTitleRemainsFirstEvenWhenItsPosterIsNotCached() {
        val result=select(listOf(Poster("ram","ram-url"),Poster("selected",null)),setOf("ram-url"))
        assertEquals(listOf("selected","ram"),result.map { it.id })
    }
    @Test fun readyMemoryPostersPrecedeDiskOnlyPosters() {
        val result=select(listOf(Poster("selected","chosen"),Poster("disk","disk-url"),Poster("ram","ram-url")),
            setOf("ram-url"),setOf("disk-url"))
        assertEquals(listOf("selected","ram","disk"),result.map { it.id })
    }
    @Test fun missingPostersAreOmittedAndCacheCannotIntroduceUnrelatedTitles() {
        val result=select(listOf(Poster("selected","chosen"),Poster("missing","absent"),Poster("local","local-url")),
            disk=setOf("local-url","outside-current-catalog"))
        assertEquals(listOf("selected","local"),result.map { it.id })
    }
    @Test fun duplicateIdsAndUrlsNeverRepeatOrDisplaceTheSelectedTitle() {
        val result=select(listOf(Poster("selected","chosen"),Poster("other","chosen"),Poster("a","a-url"),
            Poster("a","b-url"),Poster("b","a-url")),setOf("chosen","a-url","b-url"))
        assertEquals(listOf("selected","a"),result.map { it.id })
    }
    @Test fun decorationIsBoundedToTwentyCards() {
        val items=listOf(Poster("selected","chosen"))+(1..125).map { Poster("$it","url-$it") }
        val result=select(items,disk=(1..125).map { "url-$it" }.toSet())
        assertEquals(20,result.size);assertEquals("selected",result.first().id)
    }
    @Test fun emptyUrlsAndEmptyProviderIdsAreIgnored() {
        val result=select(listOf(Poster("selected",null),Poster("empty",null),Poster("blank"," "),
            Poster("","known"),Poster("valid"," known ")),setOf("known"))
        assertEquals(listOf("selected","valid"),result.map { it.id })
    }
    @Test fun absentSelectionOrNonpositiveLimitProducesNoDecoration() {
        assertTrue(select(listOf(Poster("other","url")),setOf("url")).isEmpty())
        assertTrue(select(listOf(Poster("selected",null)),limit=0).isEmpty())
        assertTrue(select(listOf(Poster("selected",null)),limit=-1).isEmpty())
    }
}
