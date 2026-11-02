package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/** A removed lazy row must not leave a TV screen without a DPAD target. */
@Composable
internal fun Modifier.catalogFocusRecovery(enabled: Boolean, recover: () -> Unit): Modifier {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestRecover by rememberUpdatedState(recover)
    var hasFocus by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(enabled, hasFocus) {
        if (!enabled || hasFocus || !entered) return@LaunchedEffect
        // Let an intentional transfer to another child finish before recovering.
        repeat(2) { withFrameNanos { } }
        if (!hasFocus && view.hasWindowFocus() && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            latestRecover()
        }
    }
    return onFocusChanged {
        hasFocus = it.hasFocus
        if (it.hasFocus) entered = true
    }.focusGroup()
}
