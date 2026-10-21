package br.com.flaviotorres.iptvpremium.ui.kids

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.ui.settings.SettingsPreferences
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalTestApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35],qualifiers="w960dp-h540dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class KidsPolishHostTest {
    @get:Rule val compose=createComposeRule()
    @After fun cleanup(){KidsAppearance.apply(false,"blue")}
    @Test fun focusedSurfaceHasClearWhiteContrastInBothThemes(){
        for(style in KidsStyle.entries)for(focused in listOf(false,true)) {
            val surface=kidsFocusFill(style,focused)
            val contrast=(Color.White.luminance()+.05f)/(surface.luminance()+.05f)
            assertTrue("White border/text contrast in $style: $contrast",contrast>7f)
        }
    }
    @Test fun blueLabelsShareBaselineHaveGapBelowFocusBarAndRemoteFocusMoves()=home("blue")
    @Test fun pinkLabelsShareBaselineHaveGapBelowFocusBarAndRemoteFocusMoves()=home("pink")
    private fun home(style:String){
        SettingsPreferences(ApplicationProvider.getApplicationContext<Context>()).setKidsStyle(style)
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){KidsScreen({},{},{},{})}}
        val labels=listOf("CANAIS","FILMES","SÉRIES")
        val bounds=labels.map{compose.onNodeWithText(it,useUnmergedTree=true).fetchSemanticsNode().boundsInRoot}
        bounds.drop(1).forEach {assertEquals(bounds[0].top,it.top,.5f);assertEquals(bounds[0].bottom,it.bottom,.5f)}
        labels.forEachIndexed{i,title->
            val bar=compose.onNodeWithTag("kids-home-focus-$i",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val label=bounds[i]
            assertTrue("Label separated from focus bar",label.top-bar.bottom>5f)
            assertEquals("Label centered",bar.center.x,label.center.x,1f)
        }
        compose.onNodeWithText("CANAIS").assertIsFocused()
        compose.onRoot().performKeyInput{pressKey(Key.DirectionRight)}
        compose.onNodeWithText("FILMES").assertIsFocused()
        compose.onRoot().performKeyInput{pressKey(Key.DirectionRight)}
        compose.onNodeWithText("SÉRIES").assertIsFocused()
    }
    @Test fun kidsChannelsHideGuideAndDays(){
        KidsAppearance.apply(true,"pink")
        compose.setContent{Box(Modifier.requiredSize(960.dp,540.dp)){br.com.flaviotorres.iptvpremium.ui.live.LiveTvScreen()}}
        compose.onNodeWithText("GUIA DE PROGRAMAÇÃO",substring=true).assertDoesNotExist()
        compose.onNodeWithText("DIAS").assertDoesNotExist()
        compose.onNodeWithText("Nenhum canal disponível.").assertExists()
        compose.onNodeWithText("Qual canal vamos assistir?").assertExists()
    }
    @Test fun kidsVideoUsesFullHeightAndDoesNotRequestEpg(){
        KidsAppearance.apply(true,"blue")
        val calls=java.util.concurrent.atomic.AtomicInteger(0)
        val content=br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent(channels=listOf(
            br.com.flaviotorres.iptvpremium.domain.model.Channel("kids-1","Canal infantil",null,null,"",null)))
        compose.setContent{Box(Modifier.requiredSize(960.dp,540.dp)){
            br.com.flaviotorres.iptvpremium.ui.live.LiveTvScreen(playlistContent=content,onLoadChannelEpg={calls.incrementAndGet();emptyList()})}}
        compose.waitForIdle()
        assertEquals("Kids does not query unused EPG",0,calls.get())
        val viewport=compose.onNodeWithTag("kids-live-viewport",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        assertTrue("Video takes freed guide height",viewport.height>400f)
        assertTrue("Video reclaims day column",viewport.width>570f)
        compose.onNodeWithText("DIAS").assertDoesNotExist()
    }
    @Test fun normalChannelsKeepGuideAndDays(){
        KidsAppearance.apply(false,"pink")
        compose.setContent{Box(Modifier.requiredSize(960.dp,540.dp)){br.com.flaviotorres.iptvpremium.ui.live.LiveTvScreen()}}
        compose.onNodeWithText("GUIA DE PROGRAMAÇÃO",substring=true).assertExists()
        compose.onNodeWithText("DIAS").assertExists()
    }
}
