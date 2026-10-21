package br.com.flaviotorres.iptvpremium.data.repository

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import br.com.flaviotorres.iptvpremium.ui.movies.MoviesScreen
import br.com.flaviotorres.iptvpremium.ui.components.CatalogHardwareEscape
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalTestApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35], qualifiers = "w960dp-h540dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class CatalogScreenReturnHostTest {
    @get:Rule val compose = createComposeRule()
    @Test fun escapeRestoresTheSelectedCategoryItem() {
        val outer = AtomicInteger(0)
        compose.setContent {
            BackHandler { outer.incrementAndGet() }
            Box(Modifier.requiredSize(960.dp, 540.dp)) { MoviesScreen() }
        }
        compose.onNodeWithText("4K", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        compose.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.Escape) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("movie-category-UHD_4K").filter(isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, outer.get())
        compose.onNodeWithTag("movie-category-UHD_4K").performKeyInput { pressKey(Key.Escape) }
        compose.waitForIdle()
        assertEquals(1, outer.get())
    }
    @Test fun physicalEscapeUsesScreenActionWithoutDependingOnComposeFocusDispatch() {
        val outer = AtomicInteger(0)
        compose.setContent {
            BackHandler { outer.incrementAndGet() }
            Box(Modifier.requiredSize(960.dp, 540.dp)) { MoviesScreen() }
        }
        compose.onNodeWithText("4K", useUnmergedTree = true).performScrollTo().performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle {
            check(CatalogHardwareEscape.down(false))
            check(CatalogHardwareEscape.down(true))
            check(CatalogHardwareEscape.up())
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("movie-category-UHD_4K").filter(isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, outer.get())
    }
    @Test fun seriesProviderEscapeRestoresItemAndAllowsNextCategory() {
        val outer = AtomicInteger(0)
        val chosen = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val categories = List(30) { index -> br.com.flaviotorres.iptvpremium.domain.model.Category(
            "series-$index", "Categoria $index",
            br.com.flaviotorres.iptvpremium.domain.model.CategoryType.SERIES,
            br.com.flaviotorres.iptvpremium.domain.model.CategorySource.PROVIDER) }
        fun page() = androidx.paging.PagingData.from(List(20) { index ->
            br.com.flaviotorres.iptvpremium.domain.model.Series("s$index", "Série $index", null,
                null, null, 2026, 8.0, null) })
        val data = kotlinx.coroutines.flow.MutableStateFlow(page())
        compose.setContent {
            BackHandler { outer.incrementAndGet() }
            val paging = data.collectAsLazyPagingItems()
            Box(Modifier.requiredSize(960.dp, 540.dp)) {
                br.com.flaviotorres.iptvpremium.ui.series.SeriesScreen(pagedSeries = paging,
                    pagingCategories = categories, totalSeriesCount = 20,
                    onSourceCategorySelected = { chosen.set(it); data.value = page() })
            }
        }
        compose.onNodeWithTag("series-category-list").performScrollToKey("provider:series-20")
        compose.onNodeWithTag("series-category-Categoria 20").performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1000)
        // Physical ESC must also work while a Paging refresh leaves no focused card.
        compose.runOnIdle {
            check(CatalogHardwareEscape.down(false))
            check(CatalogHardwareEscape.up())
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("series-category-Categoria 20").filter(isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, outer.get())
        compose.onNodeWithTag("series-category-Categoria 20").performKeyInput {
            pressKey(Key.DirectionDown); pressKey(Key.DirectionCenter)
        }
        compose.waitForIdle()
        assertEquals("series-21", chosen.get())
    }

}
