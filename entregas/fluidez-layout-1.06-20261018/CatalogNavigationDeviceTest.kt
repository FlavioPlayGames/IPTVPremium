package br.com.flaviotorres.iptvpremium.data.repository

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import br.com.flaviotorres.iptvpremium.ui.components.*
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

/** Isolated fixtures: never imports/removes the user's playlists or history. */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class CatalogNavigationDeviceTest {
    @get:Rule val compose = createComposeRule()
    private fun grid(count: Int): AtomicInteger {
        val focused = AtomicInteger(-1)
        compose.setContent {
            val state = rememberLazyGridState()
            val scope = rememberCoroutineScope()
            val navigator = remember { CatalogGridNavigator(state, scope, 5, { count }, {}, {}) }
            LazyVerticalGrid(GridCells.Fixed(5), Modifier.width(500.dp).height(180.dp), state) {
                items(count, key = { it }) { index ->
                    val requester = remember { FocusRequester() }
                    BindCatalogFocus(navigator, index, requester)
                    LaunchedEffect(index) { if(index == 2 && focused.get() < 0) { withFrameNanos {}; requester.requestFocus() } }
                    Box(Modifier.height(60.dp).testTag("cell-$index")
                        .focusRequester(requester)
                        .catalogGridNavigation(navigator, index, requester)
                        .onFocusChanged { if(it.isFocused) { focused.set(index); navigator.focused(index) } }
                        .focusable())
                }
            }
        }
        compose.waitUntil(5_000) { focused.get() == 2 }
        return focused
    }
    @Test fun rapidDownKeepsThirdColumnAcrossUncomposedRows() {
        val focused = grid(100)
        compose.onNodeWithTag("cell-2").performKeyInput { repeat(8) { pressKey(Key.DirectionDown) } }
        compose.waitUntil(5_000) { focused.get() == 42 }
        assertEquals(2, focused.get() % 5)
    }
    @Test fun reversingDirectionKeepsThirdColumn() {
        val focused = grid(100)
        compose.onNodeWithTag("cell-2").performKeyInput { repeat(8) { pressKey(Key.DirectionDown) } }
        compose.waitUntil(5_000) { focused.get() == 42 }
        compose.onNodeWithTag("cell-42").performKeyInput { repeat(6) { pressKey(Key.DirectionUp) } }
        compose.waitUntil(5_000) { focused.get() == 12 }
        assertEquals(2, focused.get() % 5)
    }
    @Test fun incompleteLastRowDoesNotRedirectToFirstColumn() {
        val focused = grid(52)
        compose.onNodeWithTag("cell-2").performKeyInput { repeat(9) { pressKey(Key.DirectionDown) } }
        compose.waitUntil(5_000) { focused.get() == 47 }
        compose.onNodeWithTag("cell-47").performKeyInput { repeat(4) { pressKey(Key.DirectionDown) } }
        compose.waitForIdle()
        assertEquals(47, focused.get())
    }
    @Test fun keyboardRetainsFocusAcrossCharacterRecompositions() {
        val typed = AtomicInteger(0)
        compose.setContent {
            var value by remember { mutableStateOf("") }
            Column {
                androidx.compose.foundation.text.BasicText(value)
                AppKeyboard(onCharacter = { value += it; typed.set(value.length) },
                    onBackspace = { value = value.dropLast(1) }, onAction = {})
            }
        }
        compose.onNodeWithText("q").assertIsFocused()
        compose.onNodeWithText("q").performKeyInput { repeat(12) { pressKey(Key.DirectionCenter) } }
        compose.waitUntil(5_000) { typed.get() == 12 }
        compose.onNodeWithText("q").assertIsFocused()
    }
}
