package br.com.flaviotorres.iptvpremium.ui.settings

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import br.com.flaviotorres.iptvpremium.R
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

/** Mesmas regras para toque, controle remoto e teclas numericas fisicas. */
internal object PinDigits {
    const val maximum = 6
    fun normalize(value: String) = value.filter { it in '0'..'9' }.take(maximum)
    fun append(value: String, digit: Int) = if (digit in 0..9 && value.length < maximum) value + digit else value
    fun erase(value: String) = value.dropLast(1)
    fun ready(value: String) = value.length == maximum && value.all { it in '0'..'9' }
    fun digit(keyCode: Int): Int? = when (keyCode) {
        in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9 -> keyCode - AndroidKeyEvent.KEYCODE_0
        in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> keyCode - AndroidKeyEvent.KEYCODE_NUMPAD_0
        else -> null
    }
}

private fun numericEvent(event: KeyEvent, value: String, onValue: (String) -> Unit): Boolean {
    val native = event.nativeKeyEvent
    val digit = PinDigits.digit(native.keyCode)
    val erase = native.keyCode == AndroidKeyEvent.KEYCODE_DEL || native.keyCode == AndroidKeyEvent.KEYCODE_FORWARD_DEL
    if (digit == null && !erase) return false
    // Consume tambem o key-up para nao executar a tecla que esta com foco.
    if (native.action == AndroidKeyEvent.ACTION_DOWN && native.repeatCount == 0)
        onValue(if (digit != null) PinDigits.append(value, digit) else PinDigits.erase(value))
    return true
}

private val pinAccent get() = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.color(0xFFFFE6A0)
private val pinRim get() = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.borderColor(0xFFB99245)

/** Campo sem editor/IME nativo: o OK abre somente o teclado do aplicativo. */
@Composable
internal fun NumericPinField(label: String, value: String, onValue: (String) -> Unit, enabled: Boolean, minimum: Int = 6, autoFocus:Boolean=false) {
    val epoch by ParentalSession.epoch.collectAsState()
    var open by remember(epoch) { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var hardwareValue by remember(value) { mutableStateOf(PinDigits.normalize(value)) }
    var restoreFocus by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    // Only the primary PIN field opts in. Confirmation must not steal initial focus.
    LaunchedEffect(autoFocus,enabled) {
        if(autoFocus && enabled) { withFrameNanos {}; focus.requestFocus() }
    }
    LaunchedEffect(restoreFocus) {
        if (restoreFocus > 0 && enabled) { withFrameNanos { }; focus.requestFocus() }
    }
    LaunchedEffect(enabled) { if (!enabled) open = false }
    Text(label, color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF), fontSize = 16.sp)
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.fillMaxWidth().heightIn(min = 46.dp).focusRequester(focus)
        .onFocusChanged { focused = it.isFocused }
        .semantics(mergeDescendants = true) {
            contentDescription = label
            password()
            stateDescription = "${value.length} números digitados. OK abre o teclado numérico."
        }
        .onPreviewKeyEvent { event ->
            if (!enabled) false
            else if (numericEvent(event, hardwareValue) { hardwareValue = it; onValue(it) }) true
            else if (event.nativeKeyEvent.keyCode in listOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)) {
                if (event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_UP) open = true
                true
            } else false
        }
        .background(if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor() else br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF101112), shape)
        .border(if (focused) 2.dp else 1.dp, br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.metallicBorder, shape)
        .clickable(enabled = enabled, role = Role.Button) { open = true }
        .padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(6) { index ->
                Box(Modifier.size(10.dp).background(
                    if (index < value.length) pinAccent else Color.Transparent, RoundedCornerShape(50))
                    .border(1.dp, br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.metallicBorder, RoundedCornerShape(50)))
            }
        }
    }
    if (open && enabled) {
        PinKeyboardDialog(label, value, minimum, onCancel = { open = false; restoreFocus += 1 }) { entered ->
            onValue(entered)
            open = false
            restoreFocus += 1
        }
    }
}

@Composable
private fun PinKeyboardDialog(label: String, initial: String, minimum: Int, onCancel: () -> Unit, onDone: (String) -> Unit) {
    var digits by remember { mutableStateOf(PinDigits.normalize(initial)) }
    LaunchedEffect(digits) { if(PinDigits.ready(digits)) onDone(digits) }
    val keys = remember { List(13) { FocusRequester() } }
    var focusedIndex by remember { mutableIntStateOf(0) }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * .9f).dp
    LaunchedEffect(Unit) { withFrameNanos { }; keys[0].requestFocus() }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 340.dp).fillMaxWidth(.92f).heightIn(max = maxHeight)
            .clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF191919), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF080A0C))))
            .border(1.dp, Brush.linearGradient(listOf(pinRim, pinAccent, pinRim)), RoundedCornerShape(16.dp))
            .onPreviewKeyEvent { numericEvent(it, digits) { next -> digits = next } }
            .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PIN DE ACESSO", color = pinAccent, fontSize = 20.sp,
                fontFamily = if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular)androidx.compose.ui.text.font.FontFamily.SansSerif else FontFamily(Font(R.font.cinzel_semibold)))
            Text(label, color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFCDCDD0), fontSize = 13.sp)
            Text(if (digits.isEmpty()) "○ ○ ○ ○ ○ ○" else "•".repeat(digits.length),
                color = pinAccent, fontSize = 24.sp, letterSpacing = 5.sp,
                modifier = Modifier.semantics { password(); stateDescription = "${digits.length} números digitados" })
            Text("Exatamente 6 números · validação automática", color = br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFF9D9FA5), fontSize = 12.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(pinRim.copy(alpha = .45f)))
            repeat(4) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { column ->
                        val index = row * 3 + column
                        val label = when (index) { 9 -> "Apagar"; 10 -> "0"; 11 -> "Limpar"; else -> (index + 1).toString() }
                        PinKey(label, true,
                            Modifier.weight(1f).focusRequester(keys[index]).focusProperties {
                                left = keys[if (column == 0) index else index - 1]
                                right = keys[if (column == 2) index else index + 1]
                                up = keys[if (row == 0) index else index - 3]
                                down = keys[if (row == 3) 12
                                    else index + 3]
                            }, onFocused = { focusedIndex = index }) {
                            when (index) {
                                9 -> digits = PinDigits.erase(digits)
                                10 -> digits = PinDigits.append(digits, 0)
                                11 -> digits = ""
                                else -> digits = PinDigits.append(digits, index + 1)
                            }
                        }
                    }
                }
            }
            PinKey("Cancelar", true, Modifier.fillMaxWidth().focusRequester(keys[12]).focusProperties {
                up = keys[10]; left = keys[12]; right = keys[12]; down = keys[12]
            }, onFocused = { focusedIndex = 12 }, onClick = onCancel)
        }
    }
}

@Composable
private fun PinKey(label: String, enabled: Boolean, modifier: Modifier, onFocused: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(9.dp)
    Box(modifier.heightIn(min = 40.dp).clip(shape)
        .background(Brush.verticalGradient(if (focused)
            br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColors()
            else listOf(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF1C1C1C), br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xFF111213))))
        .border(if (focused) 2.dp else 1.dp, br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.metallicBorder, shape)
        .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
        .onPreviewKeyEvent { event ->
            if (event.nativeKeyEvent.keyCode in listOf(AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)) {
                if (enabled && event.nativeKeyEvent.action == AndroidKeyEvent.ACTION_UP) onClick()
                true
            } else false
        }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, color = (if (focused) br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.ink(0xFFFFFFFF) else pinAccent).copy(alpha = if (enabled) 1f else .4f), fontSize = if (label.length == 1) 22.sp else 13.sp, fontWeight = FontWeight.Medium)
    }
}
