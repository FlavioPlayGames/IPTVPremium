package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** The selected lazy item must exist before asking its own focus target to resume. */
internal suspend fun restoreCatalogCategoryFocus(state: LazyListState, index: Int,
    selected: FocusRequester, fallback: FocusRequester, inputMode: InputModeManager) {
    // ESC after a mouse/touch selection does not automatically leave touch mode.
    // Clickable category rows cannot receive keyboard focus until this transition.
    inputMode.requestInputMode(InputMode.Keyboard)
    val target = index.coerceAtLeast(0)
    val layout = state.layoutInfo
    val row = layout.visibleItemsInfo.firstOrNull { it.index == target }
    // Returning focus must not reposition a category which is already visible.
    // scrollToItem(target) would snap it to the top before focus reveals it again.
    if (row == null || !catalogCategoryFullyVisible(row.offset, row.size,
            layout.viewportStartOffset, layout.viewportEndOffset)) {
        state.scrollToItem(target)
    }
    withTimeoutOrNull(1_500L) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.any { it.index == target } }.first { it }
        repeat(4) {
            withFrameNanos { }
            if (runCatching { selected.requestFocus() }.getOrDefault(false)) return@withTimeoutOrNull
        }
        fallback.requestFocus()
    }
}
