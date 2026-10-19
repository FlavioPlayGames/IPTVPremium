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
    private fun grid(count: Int, columns: Int = 5, enterHost: Boolean = false): AtomicInteger {
        val focused = AtomicInteger(-1)
        compose.setContent {
            val state = rememberLazyGridState()
            val scope = rememberCoroutineScope()
            val navigator = remember { CatalogGridNavigator(state, scope, columns, { count }, {}, {}) }
            Row {
            Box(Modifier.width(60.dp).height(180.dp).testTag("category").focusable())
            LazyVerticalGrid(GridCells.Fixed(columns), Modifier.width(500.dp).height(180.dp).catalogGridHost(navigator).testTag("grid"), state) {
                items(count, key = { it }) { index ->
                    val requester = remember { FocusRequester() }
                    BindCatalogFocus(navigator, index, requester)
                    LaunchedEffect(index) { if (enterHost && index == 0) { withFrameNanos {}; navigator.host.requestFocus() }
                        else if (!enterHost && index == 2 && focused.get() < 0) { withFrameNanos {}; requester.requestFocus() } }
                    Box(Modifier.height(60.dp).testTag("cell-$index")
                        .focusRequester(requester)
                        .catalogGridNavigation(navigator, index, requester)
                        .onFocusChanged { if(it.isFocused) { focused.set(index); navigator.focused(index) } }
                        .focusable())
                }
            }
            }
        }
        compose.waitUntil(5_000) { focused.get() == if (enterHost) 0 else 2 }
        return focused
    }
    @Test fun enteringGridHostTransfersFocusToFirstCard() {
        grid(100, enterHost = true)
        compose.onNodeWithTag("cell-0").assertIsFocused()
        compose.onNodeWithTag("category").assertIsNotFocused()
    }
    @Test fun rapidDownKeepsThirdColumnAcrossUncomposedRows() {
        val focused = grid(100)
        compose.onNodeWithTag("cell-2").performKeyInput { repeat(8) { pressKey(Key.DirectionDown) } }
        compose.waitUntil(5_000) { focused.get() == 42 }
        assertEquals(2, focused.get() % 5)
        compose.onNodeWithTag("cell-42").assertIsFocused()
        compose.onNodeWithTag("category").assertIsNotFocused()
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
    @Test fun fourColumnsKeepThirdColumnDuringRapidKeys() {
        val focused = grid(100, 4)
        compose.onNodeWithTag("cell-2").performKeyInput { repeat(15) { pressKey(Key.DirectionDown) } }
        compose.waitUntil(5_000) { focused.get() == 62 }
        assertEquals(2, focused.get() % 4)
    }
    @Test fun horizontalCommandsMoveExactlyOneColumnWithoutWrapping() {
        val focused = grid(100)
        compose.onNodeWithTag("cell-2").performKeyInput { repeat(5) { pressKey(Key.DirectionRight) } }
        compose.waitUntil(5_000) { focused.get() == 4 }
        compose.onNodeWithTag("cell-4").performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.DirectionLeft) }
        compose.waitUntil(5_000) { focused.get() == 8 }
        assertEquals(3, focused.get() % 5)
    }
    @Test fun heldDownRepeatsKeepThirdColumnAndDoNotEnterCategories() {
        val focused = grid(200)
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val start = android.os.SystemClock.uptimeMillis()
        repeat(20) { repeat ->
            instrumentation.sendKeySync(android.view.KeyEvent(start, android.os.SystemClock.uptimeMillis(),
                android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_DOWN, repeat))
        }
        instrumentation.sendKeySync(android.view.KeyEvent(start, android.os.SystemClock.uptimeMillis(),
            android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN, 0))
        compose.waitUntil(5_000) { focused.get() == 102 }
        compose.onNodeWithTag("cell-102").assertIsFocused()
        compose.onNodeWithTag("category").assertIsNotFocused()
    }
    @Test fun onlyLeftFromFirstColumnCanLeaveForCategories() {
        grid(100)
        compose.onNodeWithTag("cell-2").performKeyInput { repeat(3) { pressKey(Key.DirectionUp) } }
        compose.onNodeWithTag("cell-2").assertIsFocused()
        compose.onNodeWithTag("cell-2").performKeyInput { repeat(3) { pressKey(Key.DirectionLeft) } }
        compose.waitForIdle()
        compose.onNodeWithTag("category").assertIsFocused()
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
