package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import br.com.flaviotorres.iptvpremium.ui.kids.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class KeyboardPage { LETTERS, ACCENTS, SYMBOLS }

private val MetallicGoldBrush get() = Brush.linearGradient(
    listOf(
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF8A6414), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFD4AF37), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFE991),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFF4BD), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFE3B83F), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF9E761B)
    )
)

private data class AppKeyboardButton(
    val text: String,
    val weight: Float = 1f,
    val highlighted: Boolean = false,
    val fontSize: TextUnit = 14.sp,
    val repeatOnHold: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Teclado para TV e celular.
 * - Apagar na ultima coluna da terceira linha, em TODAS as paginas.
 * - Pagina de acentos (ÁÉÍÓÚ) com cedilha, vogais acentuadas e maiusculas.
 * - Navegacao circular nas quatro extremidades com o controle remoto.
 * - CONCLUIR executa apenas onAction; o chamador decide se fecha o teclado.
 * - As setas de edicao aparecem quando a tela fornece os callbacks.
 * - Inserir/substituir/apagar o caractere destacado e controlado pela tela,
 *   que recebe as chamadas onCharacter / onBackspace.
 * - Voltar nao e interceptado aqui: fica sob responsabilidade da tela chamadora.
 */
@Composable
fun AppKeyboard(
    modifier: Modifier = Modifier,
    actionText: String = "PRÓXIMO",
    onCharacter: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit = { onCharacter(" ") },
    onAction: () -> Unit,
    onMoveCursorLeft: (() -> Unit)? = null,
    onMoveCursorRight: (() -> Unit)? = null,
    currentText: String = "",
    onReplaceAll: ((String) -> Unit)? = null,
    allowCopy: Boolean = true,
    onExitUp: (() -> Unit)? = null,
    enterSubmits: Boolean = true,
    themeBorders: Boolean = false
) {
    val clipboard = LocalClipboardManager.current
    // Ctrl+A seleciona todo o texto logico das buscas e campos customizados.
    var replaceSelection by remember { mutableStateOf(false) }
    var deadAccent by remember { mutableIntStateOf(0) }
    fun insertPhysicalText(text: String) {
        if (replaceSelection && onReplaceAll != null) onReplaceAll(text)
        else onCharacter(text)
        replaceSelection = false
    }
    fun handlePhysicalKey(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (!isPhysicalKeyboardKeyDown(event)) return false
        val native = event.nativeKeyEvent
        if (native.isCtrlPressed && !native.isAltPressed && !native.isMetaPressed) {
            return when (native.keyCode) {
                AndroidKeyEvent.KEYCODE_A -> {
                    replaceSelection = onReplaceAll != null
                    replaceSelection
                }
                AndroidKeyEvent.KEYCODE_C -> {
                    if (allowCopy) clipboard.setText(AnnotatedString(currentText))
                    true
                }
                AndroidKeyEvent.KEYCODE_X -> {
                    if (allowCopy && onReplaceAll != null) {
                        clipboard.setText(AnnotatedString(currentText))
                        onReplaceAll("")
                    }
                    replaceSelection = false
                    true
                }
                AndroidKeyEvent.KEYCODE_V -> {
                    val pasted = clipboard.getText()?.text.orEmpty()
                    if (pasted.isNotEmpty()) insertPhysicalText(pasted)
                    true
                }
                else -> false
            }
        }
        if (native.isMetaPressed || (native.isCtrlPressed && !native.isAltPressed)) return false
        return when (native.keyCode) {
            AndroidKeyEvent.KEYCODE_DEL, AndroidKeyEvent.KEYCODE_FORWARD_DEL -> {
                if (replaceSelection && onReplaceAll != null) onReplaceAll("")
                else onBackspace()
                replaceSelection = false
                true
            }
            AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                // In selectable-key mode, Enter belongs to the focused key, not CONCLUIR.
                if (enterSubmits) { onAction(); true } else false
            }
            AndroidKeyEvent.KEYCODE_MOVE_HOME -> {
                // Movimentacao nativa apenas quando ha cursor customizado.
                false
            }
            else -> {
                val decoded = physicalTypedCharacter(event, deadAccent)
                deadAccent = decoded.deadAccent
                if (decoded.text != null) { insertPhysicalText(decoded.text); true }
                else decoded.deadAccent != 0
            }
        }
    }

    var page by remember { mutableStateOf(KeyboardPage.LETTERS) }
    var shiftEnabled by remember { mutableStateOf(false) }

    val firstRow = when (page) {
        KeyboardPage.LETTERS ->
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        KeyboardPage.ACCENTS ->
            listOf("á", "à", "â", "ã", "ä", "é", "è", "ê", "ë", "í")
        KeyboardPage.SYMBOLS ->
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    }
    val secondRow = when (page) {
        KeyboardPage.LETTERS ->
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ç")
        KeyboardPage.ACCENTS ->
            listOf("ì", "î", "ï", "ó", "ò", "ô", "õ", "ö", "ú", "ù")
        KeyboardPage.SYMBOLS ->
            listOf("@", "#", "$", "%", "&", "*", "-", "_", "+", "=")
    }
    val thirdRow = when (page) {
        KeyboardPage.LETTERS ->
            listOf("z", "x", "c", "v", "b", "n", "m")
        KeyboardPage.ACCENTS ->
            listOf("û", "ü", "ñ", "ý", "ÿ", "å", "ç")
        KeyboardPage.SYMBOLS ->
            listOf(".", ",", ":", ";", "/", "\\", "?", "!", "'", "\"")
    }

    fun typeCharacter(character: String) {
        onCharacter(if (page != KeyboardPage.SYMBOLS && shiftEnabled) {
            character.uppercase()
        } else {
            character
        })
        if (shiftEnabled) shiftEnabled = false
    }

    val firstButtons = firstRow.map { character ->
        AppKeyboardButton(
            text = if (shiftEnabled && page != KeyboardPage.SYMBOLS) character.uppercase() else character,
            onClick = { typeCharacter(character) }
        )
    }
    val secondButtons = secondRow.map { character ->
        AppKeyboardButton(
            text = if (shiftEnabled && page != KeyboardPage.SYMBOLS) character.uppercase() else character,
            onClick = { typeCharacter(character) }
        )
    }
    val thirdButtons = buildList {
        if (page != KeyboardPage.SYMBOLS) {
            add(AppKeyboardButton("⇧", weight = 0.7f, highlighted = shiftEnabled) {
                shiftEnabled = !shiftEnabled
            })
        }
        thirdRow.forEach { character ->
            add(AppKeyboardButton(
                text = if (shiftEnabled && page != KeyboardPage.SYMBOLS) character.uppercase() else character,
                onClick = { typeCharacter(character) }
            ))
        }
        // Sempre na ultima posicao, inclusive no teclado numerico/simbolos.
        add(AppKeyboardButton("⌫", weight = 0.8f, repeatOnHold = true, onClick = onBackspace))
    }
    val bottomButtons = buildList {
        add(AppKeyboardButton(
            if (page == KeyboardPage.SYMBOLS) "ABC" else "123#+",
            weight = 0.85f,
            highlighted = page == KeyboardPage.SYMBOLS,
            fontSize = 11.sp
        ) {
            page = if (page == KeyboardPage.SYMBOLS) KeyboardPage.LETTERS else KeyboardPage.SYMBOLS
            shiftEnabled = false
        })
        add(AppKeyboardButton(
            if (page == KeyboardPage.ACCENTS) "ABC" else "ÁÉÍÓÚ",
            weight = 0.85f,
            highlighted = page == KeyboardPage.ACCENTS,
            fontSize = 11.sp
        ) {
            page = if (page == KeyboardPage.ACCENTS) KeyboardPage.LETTERS else KeyboardPage.ACCENTS
            shiftEnabled = false
        })
        add(AppKeyboardButton("/", weight = 0.4f) { onCharacter("/") })
        add(AppKeyboardButton(".", weight = 0.4f) { onCharacter(".") })
        if (onMoveCursorLeft != null && onMoveCursorRight != null) {
            add(AppKeyboardButton("←", weight = 0.55f) { onMoveCursorLeft() })
            add(AppKeyboardButton("→", weight = 0.55f) { onMoveCursorRight() })
        }
        add(AppKeyboardButton("ESPAÇO", weight = 2.6f, fontSize = 11.sp, onClick = onSpace))
        add(AppKeyboardButton(actionText, weight = 1.25f, highlighted = true, fontSize = 10.sp, onClick = onAction))
    }
    val rows = listOf(firstButtons, secondButtons, thirdButtons, bottomButtons)

    // Recriar apenas na troca de pagina; o foco nao depende do texto digitado.
    val focusRequesters = remember(page, onMoveCursorLeft != null && onMoveCursorRight != null) {
        rows.map { row -> List(row.size) { FocusRequester() } }
    }

    fun moveFocus(row: Int, column: Int, key: Key): Boolean {
        if (row == 0 && key == Key.DirectionUp && onExitUp != null) {
            onExitUp()
            return true
        }
        val target = when (key) {
            Key.DirectionLeft -> row to ((column - 1 + rows[row].size) % rows[row].size)
            Key.DirectionRight -> row to ((column + 1) % rows[row].size)
            Key.DirectionUp, Key.DirectionDown -> {
                val nextRow = if (key == Key.DirectionUp) {
                    (row - 1 + rows.size) % rows.size
                } else {
                    (row + 1) % rows.size
                }
                val sourceLast = (rows[row].size - 1).coerceAtLeast(1)
                val targetLast = (rows[nextRow].size - 1).coerceAtLeast(0)
                val nearest = ((column.toFloat() / sourceLast) * targetLast)
                    .roundToInt().coerceIn(0, targetLast)
                nextRow to nearest
            }
            else -> return false
        }
        focusRequesters[target.first][target.second].requestFocus()
        return true
    }

    LaunchedEffect(page) {
        withFrameNanos { }
        focusRequesters.firstOrNull()?.firstOrNull()?.requestFocus()
    }

    Column(
        modifier = modifier
            .onPreviewKeyEvent { handlePhysicalKey(it) }
            .fillMaxWidth()
            .background(if(KidsAppearance.active) Color(0xFFFFF1DD) else br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xF20A0C10), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .border(1.dp, if(KidsAppearance.active) androidx.compose.ui.graphics.SolidColor(kidsCinemaAccent()) else if (themeBorders) MetallicGoldBrush else Brush.linearGradient(listOf(Color(0xFF3B3E45), Color(0xFF3B3E45))), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        rows.forEachIndexed { rowIndex, buttons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                buttons.forEachIndexed { columnIndex, button ->
                    AppKeyboardKey(
                        modifier = Modifier.weight(button.weight),
                        text = button.text,
                        highlighted = button.highlighted,
                        themeBorders = themeBorders,
                        fontSize = button.fontSize,
                        repeatOnHold = button.repeatOnHold,
                        focusRequester = focusRequesters[rowIndex][columnIndex],
                        onDirection = { key -> moveFocus(rowIndex, columnIndex, key) },
                        onClick = button.onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AppKeyboardKey(
    modifier: Modifier = Modifier,
    text: String,
    highlighted: Boolean = false,
    themeBorders: Boolean = false,
    fontSize: TextUnit = 14.sp,
    repeatOnHold: Boolean = false,
    focusRequester: FocusRequester,
    onDirection: (Key) -> Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    val interactionModifier = if (repeatOnHold) {
        Modifier
            .pointerInput(onClick) {
                detectTapGestures(onPress = {
                    onClick()
                    coroutineScope {
                        val repeatJob = launch {
                            delay(350L)
                            while (isActive) {
                                onClick()
                                delay(65L)
                            }
                        }
                        tryAwaitRelease()
                        repeatJob.cancel()
                    }
                })
            }
            .onPreviewKeyEvent { event ->
                val isSelect = event.key == Key.DirectionCenter ||
                        event.key == Key.Enter || event.key == Key.NumPadEnter
                if (!isSelect) false else {
                    when (event.type) {
                        KeyEventType.KeyDown -> { onClick(); true }
                        KeyEventType.KeyUp -> true
                        else -> false
                    }
                }
            }
            .focusable()
    } else {
        Modifier.kidsCinemaClickable(onClick = onClick)
    }

    Box(
        modifier = modifier
            .height(34.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && onDirection(event.key)
            }
            .background(if(KidsAppearance.active) Color(0xFFF8E2C0) else if (highlighted || isFocused) br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color() else br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF17191E), shape)
            .kidsCinemaBorder(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (themeBorders || highlighted || isFocused) MetallicGoldBrush
                else Brush.linearGradient(listOf(Color(0xFF43464E), Color(0xFF43464E))),
                shape = shape
            )
            .kidsCinemaFocus(isFocused,highlighted)
            .then(interactionModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if(KidsAppearance.active) TextStyle(color=kidsCinemaRowInk(highlighted || isFocused,0xFFF3F3F5),fontSize=fontSize,fontWeight=FontWeight.Bold) else if (highlighted || isFocused) TextStyle(
                brush = (if (highlighted || isFocused) Brush.linearGradient(listOf(Color.White, Color.White)) else MetallicGoldBrushText),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            ) else TextStyle(
                color = Color(0xFFF3F3F5),
                fontSize = fontSize,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Operacoes compartilhadas pelos campos do cadastro e pelas tres buscas.
 * cursor aponta para o caractere DESTACADO, ou para o final do texto.
 */
internal data class TvKeyboardEditResult(
    val value: String,
    val cursor: Int
)

/** Substitui o caractere destacado; no final, acrescenta o novo texto. */
internal fun tvKeyboardType(
    value: String,
    cursor: Int,
    typed: String
): TvKeyboardEditResult {
    val index = cursor.coerceIn(0, value.length)
    if (typed.isEmpty()) return TvKeyboardEditResult(value, index)
    val shouldReplace = index < value.length && typed.codePointCount(0, typed.length) == 1
    val end = if (shouldReplace) {
        index + Character.charCount(Character.codePointAt(value, index))
    } else {
        index
    }
    return TvKeyboardEditResult(
        value = value.replaceRange(index, end, typed),
        cursor = index + typed.length
    )
}

/** Apaga o destacado. No fim do texto, apaga o caractere anterior. */
internal fun tvKeyboardDelete(
    value: String,
    cursor: Int
): TvKeyboardEditResult {
    val index = cursor.coerceIn(0, value.length)
    if (value.isEmpty()) return TvKeyboardEditResult(value, 0)
    return if (index < value.length) {
        val end = index + Character.charCount(Character.codePointAt(value, index))
        TvKeyboardEditResult(value.removeRange(index, end), index)
    } else if (index > 0) {
        val start = index - Character.charCount(Character.codePointBefore(value, index))
        TvKeyboardEditResult(value.removeRange(start, index), start)
    } else {
        TvKeyboardEditResult(value, 0)
    }
}

/** Move uma posicao visual para a esquerda/direita, sem partir um emoji. */
internal fun tvKeyboardMoveCursor(
    value: String,
    cursor: Int,
    direction: Int
): Int {
    val index = cursor.coerceIn(0, value.length)
    return when {
        direction < 0 && index > 0 ->
            index - Character.charCount(Character.codePointBefore(value, index))
        direction > 0 && index < value.length ->
            index + Character.charCount(Character.codePointAt(value, index))
        else -> index
    }
}


/**
 * Mesmo destaque do cursor do cadastro nas buscas de Filmes, Series e Canais.
 * Quando ativo, destaca a letra que sera substituida; no final, mostra a barra.
 */
@Composable
fun TvKeyboardSearchText(
    value: String,
    active: Boolean,
    cursorPosition: Int,
    placeholder: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    textColor: Color = Color(0xFFF5F5F5),
    placeholderColor: Color = Color(0xFF909096)
) {
    var blinkVisible by remember(active, value, cursorPosition) { mutableStateOf(true) }
    LaunchedEffect(active, value, cursorPosition) {
        blinkVisible = true
        while (active) {
            delay(550L)
            blinkVisible = !blinkVisible
        }
    }

    val formatted: AnnotatedString = remember(value, active, cursorPosition, blinkVisible) {
        if (!active) {
            AnnotatedString(value.ifEmpty { placeholder })
        } else {
            val index = cursorPosition.coerceIn(0, value.length)
            buildAnnotatedString {
                append(value.substring(0, index))
                if (index < value.length) {
                    val end = index + Character.charCount(Character.codePointAt(value, index))
                    withStyle(
                        SpanStyle(
                            color = if (blinkVisible) br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE991) else br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFB18B34),
                            background = if (blinkVisible) br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0x804B3910) else br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0x303B2E13),
                            fontSize = fontSize * 1.28f,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append(value.substring(index, end)) }
                    append(value.substring(end))
                } else {
                    withStyle(
                        SpanStyle(
                            color = if (blinkVisible) br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE991) else Color.Transparent,
                            fontSize = fontSize * 1.35f,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append("▌") }
                }
            }
        }
    }

    Text(
        text = formatted,
        modifier = modifier,
        color = if (!active && value.isEmpty()) placeholderColor else textColor,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_LISTS_SUBTITLES_CHANNELS_V1

// Text retains the approved palette; blue darkening applies to rims.
private val MetallicGoldBrushText get() = Brush.linearGradient(
    listOf(
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF8A6414), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD4AF37), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE991),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFF4BD), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFE3B83F), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF9E761B)
    )
)
