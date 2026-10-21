package br.com.flaviotorres.iptvpremium.ui.live
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalTestApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35],qualifiers="w960dp-h540dp")
class ChannelFavoriteHostTest {
    @get:Rule val compose=createComposeRule()
    private var opened=0;private var favorites=0
    private fun content(){compose.setContent{Column(Modifier.width(350.dp)){
        var favorite by remember{mutableStateOf(false)}
        ChannelRow(PreviewChannel("c1",1,"Canal teste",null,null,""),true,favorite,true,{opened++},{favorite=!favorite;favorites++})
    }};compose.onNodeWithTag("channel-row-c1").performSemanticsAction(SemanticsActions.RequestFocus){it()}}
    @Test fun shortOkOnlyOpensAndHeartClickTogglesWithoutStealingFocus(){
        content();compose.onNodeWithTag("channel-row-c1").performKeyInput{pressKey(Key.DirectionCenter)}
        assertEquals(1,opened);assertEquals(0,favorites)
        compose.onNode(hasStateDescription("Não favorito")).performClick()
        compose.onNode(hasStateDescription("Favorito")).assertExists()
        assertEquals(1,favorites);assertEquals(1,opened)
        compose.onNodeWithTag("channel-row-c1").assertIsFocused()
    }
    @Test fun holdOkOneSecondTogglesOnceWithoutOpeningOnRelease(){
        content();compose.onNodeWithTag("channel-row-c1").performKeyInput{keyDown(Key.DirectionCenter)}
        compose.waitForIdle()
        org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(1100))
        compose.mainClock.advanceTimeBy(1100);compose.waitForIdle()
        assertEquals(1,favorites);assertEquals(0,opened)
        compose.mainClock.advanceTimeBy(1500)
        compose.onNodeWithTag("channel-row-c1").performKeyInput{keyUp(Key.DirectionCenter)}
        assertEquals(1,favorites);assertEquals(0,opened)
    }
    @Test fun heartDoesNotCreateSeparateDpadFocusStop(){
        content();compose.onNodeWithTag("channel-row-c1").performKeyInput{pressKey(Key.DirectionRight)}
        compose.onNodeWithTag("channel-row-c1").assertIsFocused()
        assertEquals(0,opened);assertEquals(0,favorites)
    }
}
