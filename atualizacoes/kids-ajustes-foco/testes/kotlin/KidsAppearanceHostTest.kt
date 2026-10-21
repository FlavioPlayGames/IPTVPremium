package br.com.flaviotorres.iptvpremium.ui.kids

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.core.app.ApplicationProvider
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.flaviotorres.iptvpremium.ui.settings.*
import br.com.flaviotorres.iptvpremium.ui.theme.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalTestApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35],qualifiers="w960dp-h540dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class KidsAppearanceHostTest {
    @get:Rule val compose=createComposeRule()
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun setup() {KidsAppearance.apply(false,"blue");ThemeState.choose(context,PremiumTheme.GOLD);SettingsPreferences(context).setKidsStyle("blue")}
    @After fun cleanup() {KidsAppearance.apply(false,"blue");ThemeState.choose(context,PremiumTheme.GOLD)}

    @Test fun personalisationPersistsPinkChoiceAndKeepsMainTheme() {
        val settings=SettingsPreferences(context);ThemeState.choose(context,PremiumTheme.EMERALD)
        compose.setContent {Column(Modifier.requiredSize(960.dp,540.dp)){AppearancePreferencesSettings(FocusRequester())}}
        compose.onNode(hasText("Kids · Rosa",substring=true) and hasClickAction()).performClick()
        compose.onNode(hasText("Kids · Rosa",substring=true) and hasClickAction()).assertIsSelected()
        assertEquals("pink",SettingsPreferences(context).kidsStyle())
        assertEquals(PremiumTheme.EMERALD,ThemeState.selected)
    }
    @Test fun enteringKidsUsesItsPaletteAndExitRestoresMainThemeWithoutOverwritingPrefs() {
        ThemeState.choose(context,PremiumTheme.RED)
        KidsAppearance.apply(true,"blue");assertEquals(PremiumTheme.BLUE,ThemeState.selected)
        KidsAppearance.apply(true,"pink");assertEquals(PremiumTheme.PINK,ThemeState.selected)
        assertEquals("red",SettingsPreferences(context).prefs.getString("visual_theme",null))
        KidsAppearance.apply(false,"pink");assertEquals(PremiumTheme.RED,ThemeState.selected)
    }
    @Test fun unknownPreferenceDefaultsToBlueAndInvalidInputIsRejected() {
        val settings=SettingsPreferences(context);settings.prefs.edit().putString("kids_style","unknown").commit()
        assertEquals("blue",settings.kidsStyle())
        try {settings.setKidsStyle("red");fail("Unknown style accepted")}catch(_:IllegalArgumentException){}
    }
    @Test fun categoriesUseMeaningfulSymbolsAndUnknownNamesStayStable() {
        assertEquals(0,kidsCategorySymbol("Animação e Infantil"));assertEquals(1,kidsCategorySymbol("Aventuras"))
        assertEquals(2,kidsCategorySymbol("Halloween Kids"));assertEquals(3,kidsCategorySymbol("Fantasminhas"))
        assertEquals(4,kidsCategorySymbol("Princesas"));assertEquals(5,kidsCategorySymbol("Fundo do Mar"))
        assertEquals(7,kidsCategorySymbol("Pets"));assertEquals(6,kidsCategorySymbol("Todos"))
        val names=(1..40).map {"Canal desconhecido $it"}
        for(n in names) {val first=kidsCategorySymbol(n);assertTrue(first in 0..7);assertEquals(first,kidsCategorySymbol(n.uppercase()))}
        assertTrue(names.map(::kidsCategorySymbol).toSet().size>4)
    }
    @Test fun kidsHomeHasThreeRemoteActionsAndNoCornerButtons() {
        var movies=0;var back=0
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){KidsScreen({}, {movies++},{},{back++})}}
        compose.onNodeWithText("CANAIS").assertIsFocused()
        compose.onRoot().performKeyInput {pressKey(Key.DirectionRight);pressKey(Key.Enter)}
        assertEquals(1,movies)
        compose.onNodeWithContentDescription("Home").assertDoesNotExist()
        compose.onNodeWithContentDescription("Controle parental").assertDoesNotExist()
        compose.onRoot().performKeyInput {pressKey(Key.Escape)};assertEquals(1,back)
    }
    @Test fun blueHomeArtworkLoadsWithLandscapeShape() = homeArtwork(KidsStyle.BLUE)
    @Test fun pinkHomeArtworkLoadsWithLandscapeShape() = homeArtwork(KidsStyle.PINK)
    private fun homeArtwork(style:KidsStyle) {
        var painter:Painter?=null
        compose.setContent {painter=themePainterResource(style.home,IntSize(960,540));Image(painter!!,null,Modifier.size(640.dp,360.dp))}
        compose.waitUntil(10000){painter?.javaClass?.simpleName=="ArtworkPainter"}
        val pixels=render(painter!!,320,180).toPixelMap();var colored=0
        for(y in 0 until pixels.height step 4)for(x in 0 until pixels.width step 4) {
            val c=pixels[x,y]
            if(style==KidsStyle.BLUE && c.blue>c.red*1.25f)colored++
            if(style==KidsStyle.PINK && c.red>c.green*1.25f)colored++
        }
        assertTrue("Selected Kids art must be visibly colored",colored>300)
    }
    private fun render(painter:Painter,width:Int,height:Int):ImageBitmap {
        val bitmap=ImageBitmap(width,height)
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),androidx.compose.ui.geometry.Size(width.toFloat(),height.toFloat())) {
            with(painter){draw(size)}
        }
        return bitmap
    }
    @Test fun kidsMovieSidebarAndSearchAreThemedWithoutChangingCategories() {
        KidsAppearance.apply(true,"blue")
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){br.com.flaviotorres.iptvpremium.ui.movies.MoviesScreen()}}
        compose.onNodeWithText("MINHAS AVENTURAS").assertExists()
        compose.onNodeWithText("Qual aventura vamos assistir?").assertExists()
        compose.onNodeWithText("FILMES KIDS").assertExists()
        compose.onNodeWithTag("movie-category-ALL").assertExists()
    }
    @Test fun normalMovieSidebarRetainsNormalHeadingAndSearch() {
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){br.com.flaviotorres.iptvpremium.ui.movies.MoviesScreen()}}
        compose.onNodeWithText("CATEGORIAS").assertExists()
        compose.onNodeWithText("Pesquisar filmes...").assertExists()
        compose.onNodeWithText("MINHAS AVENTURAS").assertDoesNotExist()
    }
    @Test fun kidsSeriesProviderEscapeRestoresCategoryAndPreservesItsId() {
        KidsAppearance.apply(true,"pink")
        val chosen=java.util.concurrent.atomic.AtomicReference<String?>(null)
        val categories=List(24) { i->br.com.flaviotorres.iptvpremium.domain.model.Category("kids-$i","Infantil $i",
            br.com.flaviotorres.iptvpremium.domain.model.CategoryType.SERIES,
            br.com.flaviotorres.iptvpremium.domain.model.CategorySource.PROVIDER) }
        fun page()=androidx.paging.PagingData.from(List(20) {i->br.com.flaviotorres.iptvpremium.domain.model.Series("s$i","Série $i",null,null,null,2026,8.0,null)})
        val data=kotlinx.coroutines.flow.MutableStateFlow(page())
        compose.setContent {
            val paging=data.collectAsLazyPagingItems()
            Box(Modifier.requiredSize(960.dp,540.dp)) {br.com.flaviotorres.iptvpremium.ui.series.SeriesScreen(
                pagedSeries=paging,pagingCategories=categories,totalSeriesCount=20,
                onSourceCategorySelected={chosen.set(it);data.value=page()})}
        }
        compose.onNodeWithText("SÉRIES KIDS").assertExists()
        compose.onNodeWithTag("series-category-list").performScrollToKey("provider:kids-20")
        compose.onNodeWithTag("series-category-Infantil 20").performClick()
        compose.waitForIdle();compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle {
            check(br.com.flaviotorres.iptvpremium.ui.components.CatalogHardwareEscape.down(false))
            check(br.com.flaviotorres.iptvpremium.ui.components.CatalogHardwareEscape.up())
        }
        compose.waitUntil(5000) {compose.onAllNodesWithTag("series-category-Infantil 20").filter(isFocused()).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("series-category-Infantil 20").performKeyInput {pressKey(Key.DirectionDown);pressKey(Key.DirectionCenter)}
        compose.waitForIdle();assertEquals("kids-21",chosen.get())
    }
    @Test fun pinkSidebarUsesPinkBrandAndPreservesProviderNames() {
        KidsAppearance.apply(true,"pink")
        compose.setContent {Column {KidsCatalogHeader(true);KidsCategoryIcon("Halloween Kids")}}
        compose.onNodeWithContentDescription("IPTV Premium Kids").assertExists()
        compose.onNodeWithText("MINHAS AVENTURAS").assertExists()
        assertEquals(KidsStyle.PINK,KidsAppearance.style)
    }
}
