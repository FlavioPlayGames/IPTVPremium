package br.com.flaviotorres.iptvpremium.ui.movies

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.domain.model.Movie
import br.com.flaviotorres.iptvpremium.domain.model.Series
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.series.SeriesScreen
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalTestApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35],qualifiers="w960dp-h540dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class CatalogReturnFocusHostTest {
    @get:Rule val compose=createComposeRule()
    @Before fun setup() {
        Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).eraseColor(android.graphics.Color.BLACK)
        val context=ApplicationProvider.getApplicationContext<Context>()
        KidsAppearance.apply(false,"blue");ThemeState.choose(context,PremiumTheme.GOLD)
        CatalogStyle.choose(context,CatalogBackground.BLACK)
        // Finish native decoding before Compose; no IO decoder may outlive a Robolectric sandbox.
        kotlinx.coroutines.runBlocking {
            MarbleArtworkCache.load(context,br.com.flaviotorres.iptvpremium.R.drawable.marble_wallpaper_black,wallpaper=true)
            MarbleArtworkCache.load(context,br.com.flaviotorres.iptvpremium.R.drawable.marble_brand_gold)
        }
    }
    @Test fun moviesDropPreviousSelectionAndReturnWithImmediateDpadControl()=checkReturn(false)
    @Test fun seriesDropPreviousSelectionAndReturnWithImmediateDpadControl()=checkReturn(true)
    private fun checkReturn(series:Boolean) {
        val movies=flowOf(PagingData.from(List(30){Movie("m$it","Titulo $it",null,null,null,"",2026,8.0,null)}))
        val shows=flowOf(PagingData.from(List(30){Series("s$it","Titulo $it",null,null,null,2026,8.0,null)}))
        val visible=mutableStateOf(true);var opens=0
        lateinit var view:android.view.View;lateinit var input:InputModeManager
        compose.setContent {
            view=LocalView.current;input=LocalInputModeManager.current
            val holder=rememberSaveableStateHolder()
            val movieItems=movies.collectAsLazyPagingItems();val seriesItems=shows.collectAsLazyPagingItems()
            if(visible.value)holder.SaveableStateProvider("catalog") {
                Box(Modifier.requiredSize(960.dp,540.dp)) {
                    if(series)SeriesScreen(pagedSeries=seriesItems,totalSeriesCount=30,onSeriesClick={opens++})
                    else MoviesScreen(pagedMovies=movieItems,totalMovieCount=30,onMovieClick={opens++})
                }
            }
        }
        compose.waitUntil(10000){compose.onAllNodesWithText("Titulo 0").fetchSemanticsNodes().isNotEmpty()}
        compose.runOnIdle{input.requestInputMode(InputMode.Keyboard)}
        compose.mainClock.advanceTimeBy(500);compose.waitForIdle()
        fun card(i:Int)=compose.onNode(hasAnyDescendant(hasText("Titulo $i")) and hasClickAction(),useUnmergedTree=true)
        fun focus(i:Int) {card(i).performSemanticsAction(SemanticsActions.RequestFocus){it()};compose.mainClock.advanceTimeBy(400);compose.waitForIdle();card(i).assertIsFocused()}
        fun captureFirst():IntArray {
            val r=card(0).fetchSemanticsNode().boundsInRoot
            lateinit var bitmap:Bitmap
            compose.runOnIdle {
                bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
            }
            val x=r.left.toInt().coerceAtLeast(0);val y=r.top.toInt().coerceAtLeast(0)
            val w=r.width.toInt().coerceAtMost(bitmap.width-x);val h=r.height.toInt().coerceAtMost(bitmap.height-y)
            return IntArray(w*h).also{bitmap.getPixels(it,0,w,x,y,w,h)}
        }
        focus(1);val before=captureFirst()
        focus(0);card(0).performClick();assertEquals(1,opens)
        focus(1);assertArrayEquals("Previous clicked card must not retain its highlight",before,captureFirst())
        focus(0);card(0).performClick()
        compose.runOnIdle{visible.value=false};compose.waitForIdle()
        compose.runOnIdle{visible.value=true};compose.waitForIdle();compose.mainClock.advanceTimeBy(64)
        card(0).assertIsFocused()
        compose.onRoot().performKeyInput{pressKey(Key.DirectionRight)}
        compose.mainClock.advanceTimeBy(160);compose.waitForIdle()
        card(1).assertIsFocused()
    }
}
