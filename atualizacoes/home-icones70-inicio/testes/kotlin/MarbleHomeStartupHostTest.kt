package br.com.flaviotorres.iptvpremium.ui.home

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.ui.theme.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalTestApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35],qualifiers="w960dp-h540dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class MarbleHomeStartupHostTest {
    @get:Rule val compose=createComposeRule()
    @Before fun setup(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        KidsAppearance.apply(false,"blue");CatalogStyle.choose(context,CatalogBackground.BLACK)
        ThemeState.choose(context,PremiumTheme.GOLD);MarbleArtworkCache.clear()
    }
    private fun tiny()=ImageBitmap(32,32)
    @Test fun mainIconsAreThirtyPercentSmallerWithSameAnchorsAndUtilities(){
        val original=listOf(MarbleRegion(64,453,357,690),MarbleRegion(399,395,686,690),MarbleRegion(729,446,986,690),MarbleRegion(1028,443,1284,690),MarbleRegion(1326,457,1608,690))
        original.forEachIndexed {i,old->
            val current=MarbleHomeGeometry.icons[i]
            assertEquals(old.width*.7f,current.width.toFloat(),.51f)
            assertEquals(old.height*.7f,current.height.toFloat(),.51f)
            assertEquals(old.bottom,current.bottom)
            assertEquals((old.left+old.right)/2f,(current.left+current.right)/2f,.51f)
        }
        assertEquals(MarbleRegion(1472,50,1537,111),MarbleHomeGeometry.icons[5])
        assertEquals(MarbleRegion(1570,49,1642,115),MarbleHomeGeometry.icons[6])
        assertEquals(MarbleRegion(44,352,378,762),MarbleHomeGeometry.hitAreas[0])
        assertEquals(700,MarbleHomeGeometry.captions[0].top)
    }
    @Test fun coldStartShowsLoadingThenNormalHomeAndPreservesDpadActions(){
        val release=CompletableDeferred<Unit>();val image=tiny();var movies=0
        lateinit var inputMode:InputModeManager
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){
            inputMode=LocalInputModeManager.current
            MarbledHomeScreen({}, {movies++},{},{},{},{},{},true,artworkLoader={release.await();image})
        }}
        compose.onNodeWithTag("marble-home-loading").assertExists()
        compose.onNodeWithText("IPTV PREMIUM").assertDoesNotExist()
        compose.onNodeWithText("Canais").assertDoesNotExist()
        compose.onNodeWithTag("marble-home-0").assertDoesNotExist()
        compose.runOnIdle {inputMode.requestInputMode(InputMode.Keyboard)}
        release.complete(Unit)
        compose.waitUntil(10000){compose.onAllNodesWithTag("marble-home-0").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("marble-home-loading").assertDoesNotExist()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("marble-home-0").assertIsFocused()
        compose.onRoot().performKeyInput{pressKey(Key.DirectionRight);pressKey(Key.Enter)}
        assertEquals(1,movies)
    }
    @Test fun backdropAppearsBeforeIconsAndMenuWaitsForAllFive(){
        val release=CompletableDeferred<Unit>();val image=tiny();val calls=AtomicInteger()
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){
            MarbledHomeScreen({},{},{},{},{},{},{},true,artworkLoader={
                if(calls.incrementAndGet()>1)release.await()
                image
            })
        }}
        compose.waitUntil(10000){calls.get()>=2}
        compose.onNodeWithTag("marble-home-loading-backdrop").assertExists()
        compose.onNodeWithTag("marble-home-0").assertDoesNotExist()
        release.complete(Unit)
        compose.waitUntil(10000){compose.onAllNodesWithTag("marble-home-0").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(6,calls.get())
    }
    @Test fun failedDecodingRetriesAreBoundedAndRetryButtonRecovers(){
        val enabled=AtomicBoolean(false);val calls=AtomicInteger();val image=tiny()
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){
            MarbledHomeScreen({},{},{},{},{},{},{},true,artworkLoader={calls.incrementAndGet();if(enabled.get())image else null})
        }}
        compose.waitForIdle();compose.mainClock.advanceTimeBy(1500)
        compose.waitUntil(10000){compose.onAllNodesWithText("Tentar novamente").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(3,calls.get())
        compose.onNodeWithText("IPTV PREMIUM").assertDoesNotExist()
        enabled.set(true);compose.onNodeWithText("Tentar novamente").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithTag("marble-home-0").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(9,calls.get())
    }
    @Test fun leavingHomeCancelsPendingLoading(){
        val entered=AtomicBoolean();val cancelled=AtomicBoolean();var visible by mutableStateOf(true)
        compose.setContent {if(visible)Box(Modifier.requiredSize(960.dp,540.dp)){
            MarbledHomeScreen({},{},{},{},{},{},{},true,artworkLoader={
                entered.set(true);try{awaitCancellation()}finally{cancelled.set(true)}
            })
        }}
        compose.waitUntil(10000){entered.get()}
        compose.runOnIdle{visible=false}
        compose.waitForIdle()
        compose.waitUntil(10000){cancelled.get()}
        compose.onNodeWithTag("marble-home-loading").assertDoesNotExist()
    }
}
