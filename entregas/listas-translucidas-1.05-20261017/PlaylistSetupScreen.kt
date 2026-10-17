package br.com.flaviotorres.iptvpremium.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyEvent
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource as painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.components.AppKeyboard
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardType
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardDelete
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardMoveCursor
import br.com.flaviotorres.iptvpremium.ui.components.isPhysicalKeyboardKeyDown
import br.com.flaviotorres.iptvpremium.ui.components.physicalTypedCharacter

private fun usesPlaylistTvKeyboard(context: android.content.Context): Boolean {
    val mode = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
    return mode.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION ||
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
}

private enum class PlaylistType {
    XTREAM,
    M3U
}

private enum class SetupStep {
    ENTRY,
    TYPE,
    DATA,
    CONNECT
}

private enum class PlaylistInput {
    NAME,
    SERVER,
    USERNAME,
    PASSWORD,
    M3U_URL
}

private val MetallicGoldBrush get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF926315))
)

@Composable
fun PlaylistSetupScreen(
    onSaveXtream: (
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ) -> Unit = { _, _, _, _ -> },
    onSaveM3u: (
        name: String,
        playlistUrl: String
    ) -> Unit = { _, _ -> },
    isConnecting: Boolean = false,
    connectionError: String? = null
) {
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var currentStep by remember {
        mutableStateOf(SetupStep.ENTRY)
    }

    var selectedType by remember {
        mutableStateOf<PlaylistType?>(null)
    }

    var name by remember {
        mutableStateOf("")
    }

    var serverUrl by remember {
        mutableStateOf("")
    }

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var playlistUrl by remember {
        mutableStateOf("")
    }

    var activeInput by remember {
        mutableStateOf<PlaylistInput?>(null)
    }

    val inputContext = androidx.compose.ui.platform.LocalContext.current
    val tvInput = remember(inputContext) { usesPlaylistTvKeyboard(inputContext) }
    val customKeyboardVisible = tvInput &&
        currentStep == SetupStep.DATA && activeInput != null

    // O cursor pertence a cada campo, e nao ao teclado (que recebe o foco na TV).
    var cursorPositions by remember {
        mutableStateOf<Map<PlaylistInput, Int>>(emptyMap())
    }
    var restoreFieldFocus by remember {
        mutableStateOf<PlaylistInput?>(null)
    }
    val fieldFocusRequesters = remember {
        PlaylistInput.entries.associateWith { FocusRequester() }
    }

    fun currentValue(field: PlaylistInput): String = when (field) {
        PlaylistInput.NAME -> name
        PlaylistInput.SERVER -> serverUrl
        PlaylistInput.USERNAME -> username
        PlaylistInput.PASSWORD -> password
        PlaylistInput.M3U_URL -> playlistUrl
    }

    fun changeValue(field: PlaylistInput, text: String) {
        when (field) {
            PlaylistInput.NAME -> name = text
            PlaylistInput.SERVER -> serverUrl = text
            PlaylistInput.USERNAME -> username = text
            PlaylistInput.PASSWORD -> password = text
            PlaylistInput.M3U_URL -> playlistUrl = text
        }
    }

    fun cursorOf(field: PlaylistInput): Int {
        return (cursorPositions[field] ?: currentValue(field).length)
            .coerceIn(0, currentValue(field).length)
    }

    fun setCursor(field: PlaylistInput, position: Int) {
        cursorPositions = cursorPositions + (
                field to position.coerceIn(0, currentValue(field).length)
                )
    }

    fun insertIntoActiveInput(insertedText: String) {
        val field = activeInput ?: return
        // Quando a letra estiver destacada, substitui; no final, acrescenta.
        // Texto colado com varias letras e inserido sem perder o restante.
        val result = tvKeyboardType(currentValue(field), cursorOf(field), insertedText)
        changeValue(field, result.value)
        setCursor(field, result.cursor)
    }

    fun backspaceActiveInput() {
        val field = activeInput ?: return
        // Apaga a letra destacada; no fim apaga a anterior.
        val result = tvKeyboardDelete(currentValue(field), cursorOf(field))
        changeValue(field, result.value)
        setCursor(field, result.cursor)
    }

    fun moveCursor(direction: Int) {
        val field = activeInput ?: return
        setCursor(field, tvKeyboardMoveCursor(currentValue(field), cursorOf(field), direction))
    }

    fun pasteIntoActiveInput() {
        val clipboardText = clipboardManager.getText()?.text.orEmpty()
        if (clipboardText.isNotEmpty()) insertIntoActiveInput(clipboardText)
    }

    var physicallySelectedAllField by remember { mutableStateOf<PlaylistInput?>(null) }
    var pendingDeadAccent by remember { mutableIntStateOf(0) }

    /** Digitacao direta com foco no campo, sem obrigar a abrir o teclado de TV. */
    fun handleFieldKeyboard(field: PlaylistInput, event: KeyEvent): Boolean {
        if (!isPhysicalKeyboardKeyDown(event)) return false
        val native = event.nativeKeyEvent
        if (native.isCtrlPressed && !native.isAltPressed && !native.isMetaPressed) {
            return when (native.keyCode) {
                AndroidKeyEvent.KEYCODE_A -> {
                    physicallySelectedAllField = field
                    true
                }
                AndroidKeyEvent.KEYCODE_C -> {
                    // Nunca copia senha para a area de transferencia automaticamente.
                    if (field != PlaylistInput.PASSWORD)
                        clipboardManager.setText(AnnotatedString(currentValue(field)))
                    true
                }
                AndroidKeyEvent.KEYCODE_X -> {
                    if (field != PlaylistInput.PASSWORD) {
                        clipboardManager.setText(AnnotatedString(currentValue(field)))
                        changeValue(field, "")
                        setCursor(field, 0)
                    }
                    physicallySelectedAllField = null
                    true
                }
                AndroidKeyEvent.KEYCODE_V -> {
                    val pasted = clipboardManager.getText()?.text.orEmpty()
                    if (pasted.isNotEmpty()) {
                        val result = if (physicallySelectedAllField == field)
                            br.com.flaviotorres.iptvpremium.ui.components.TvKeyboardEditResult(pasted, pasted.length)
                        else tvKeyboardType(currentValue(field), cursorOf(field), pasted)
                        changeValue(field, result.value)
                        setCursor(field, result.cursor)
                    }
                    physicallySelectedAllField = null
                    true
                }
                else -> false
            }
        }
        if (native.isMetaPressed || (native.isCtrlPressed && !native.isAltPressed)) return false
        if (native.keyCode == AndroidKeyEvent.KEYCODE_DEL ||
            native.keyCode == AndroidKeyEvent.KEYCODE_FORWARD_DEL) {
            val result = if (physicallySelectedAllField == field)
                br.com.flaviotorres.iptvpremium.ui.components.TvKeyboardEditResult("", 0)
            else tvKeyboardDelete(currentValue(field), cursorOf(field))
            changeValue(field, result.value)
            setCursor(field, result.cursor)
            physicallySelectedAllField = null
            return true
        }
        val decoded = physicalTypedCharacter(event, pendingDeadAccent)
        pendingDeadAccent = decoded.deadAccent
        val typed = decoded.text ?: return pendingDeadAccent != 0
        val result = if (physicallySelectedAllField == field)
            br.com.flaviotorres.iptvpremium.ui.components.TvKeyboardEditResult(typed, typed.length)
        else tvKeyboardType(currentValue(field), cursorOf(field), typed)
        changeValue(field, result.value)
        setCursor(field, result.cursor)
        physicallySelectedAllField = null
        return true
    }

    fun dismissKeyboard() {
        restoreFieldFocus = activeInput
        activeInput = null
        focusManager.clearFocus()
    }

    LaunchedEffect(currentStep) {
        focusManager.clearFocus()
        activeInput = null
        restoreFieldFocus = null
        scrollState.scrollTo(0)
        if (currentStep == SetupStep.DATA) {
            withFrameNanos { }
            fieldFocusRequesters[PlaylistInput.NAME]?.requestFocus()
        }
    }

    // Back no controle remoto: primeiro fecha o teclado, depois volta um passo.
    // No passo TYPE, o BackHandler do NavHost permanece responsavel por sair.
    BackHandler(enabled = !isConnecting && currentStep != SetupStep.ENTRY) {
        when {
            currentStep == SetupStep.DATA && activeInput != null -> dismissKeyboard()
            currentStep == SetupStep.TYPE -> currentStep = SetupStep.ENTRY
            currentStep == SetupStep.DATA -> currentStep = SetupStep.TYPE
            currentStep == SetupStep.CONNECT -> currentStep = SetupStep.DATA
        }
    }

    LaunchedEffect(activeInput, restoreFieldFocus, currentStep) {
        val field = restoreFieldFocus
        if (currentStep == SetupStep.DATA && activeInput == null && field != null) {
            withFrameNanos { }
            fieldFocusRequesters[field]?.requestFocus()
            restoreFieldFocus = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(PlaylistGlass.surface(0xFF08090C))
    ) {
        PlaylistThemeBackdrop(Modifier.matchParentSize())
        /*
         * MESMO CONTORNO DA TELA "MINHAS LISTAS".
         * O tamanho não muda entre TIPO, DADOS e CONECTAR.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                )
                .border(
                    width = 1.5.dp,
                    brush = MetallicGoldBrush,
                    shape = RoundedCornerShape(22.dp)
                )
                .background(
                    color = PlaylistGlass.surface(0xC008090B),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        top = 16.dp,
                        end = 20.dp,
                        bottom =
                            if (customKeyboardVisible) {
                                8.dp
                            } else {
                                16.dp
                            }
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                /*
                 * Enquanto o teclado interno está aberto,
                 * recolhemos apenas o cabeçalho para dar
                 * mais espaço ao formulário.
                 */
                if (!customKeyboardVisible) {
                    Text(
                        text = if (currentStep == SetupStep.ENTRY) "ADICIONAR LISTA" else "CADASTRO MANUAL",
                        style = TextStyle(
                            brush = MetallicGoldBrushText,
                            fontSize = 22.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.cinzel_semibold)),
                            fontWeight =
                                FontWeight.SemiBold
                        ),
                        textAlign =
                            TextAlign.Center
                    )

                    if (currentStep != SetupStep.ENTRY) SetupStepIndicator(currentStep = currentStep)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment =
                        when {
                            customKeyboardVisible ->
                                Alignment.TopCenter

                            currentStep == SetupStep.TYPE ->
                                Alignment.Center

                            else ->
                                Alignment.TopCenter
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(
                                bottom =
                                    if (customKeyboardVisible) {
                                        4.dp
                                    } else {
                                        24.dp
                                    }
                            ),
                        verticalArrangement =
                            when {
                                customKeyboardVisible ->
                                    Arrangement.Top

                                currentStep == SetupStep.TYPE ->
                                    Arrangement.Center

                                else ->
                                    Arrangement.Top
                            }
                    ) {
                        when (currentStep) {
                            SetupStep.ENTRY -> PlaylistPortalEntry(onManual = { currentStep = SetupStep.TYPE })
                            SetupStep.TYPE -> {
                                TypeStep(
                                    onBack = { currentStep = SetupStep.ENTRY },
                                    selectedType =
                                        selectedType,
                                    onTypeSelected = {
                                        selectedType = it
                                    },
                                    onContinue = {
                                        if (
                                            selectedType !=
                                            null
                                        ) {
                                            currentStep =
                                                SetupStep.DATA
                                        }
                                    }
                                )
                            }

                            SetupStep.DATA -> {
                                DataStep(
                                    selectedType =
                                        selectedType
                                            ?: PlaylistType.XTREAM,
                                    name = name,
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                    playlistUrl = playlistUrl,
                                    activeInput =
                                        activeInput,
                                    keyboardVisible =
                                        customKeyboardVisible,
                                    onInputSelected = {
                                        restoreFieldFocus = null
                                        activeInput = it
                                    },
                                    onTextChange = { field, text, cursor ->
                                        changeValue(field, text)
                                        setCursor(field, cursor)
                                    },
                                    cursorPosition = { field -> cursorOf(field) },
                                    onCursorChange = { field, pos -> setCursor(field, pos) },
                                    fieldFocusRequesters = fieldFocusRequesters,
                                    onPaste = {
                                        pasteIntoActiveInput()
                                    },
                                    onHardwareKey = { field, event -> handleFieldKeyboard(field, event) },
                                    onBack = {
                                        activeInput = null
                                        currentStep =
                                            SetupStep.TYPE
                                    },
                                    onContinue = {
                                        activeInput = null
                                        focusManager.clearFocus()

                                        if (
                                            isDataValid(
                                                type =
                                                    selectedType
                                                        ?: PlaylistType.XTREAM,
                                                name = name,
                                                serverUrl =
                                                    serverUrl,
                                                username =
                                                    username,
                                                password =
                                                    password,
                                                playlistUrl =
                                                    playlistUrl
                                            )
                                        ) {
                                            currentStep =
                                                SetupStep.CONNECT
                                        }
                                    }
                                )
                            }

                            SetupStep.CONNECT -> {
                                ConnectStep(
                                    selectedType =
                                        selectedType
                                            ?: PlaylistType.XTREAM,
                                    name = name,
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                    playlistUrl =
                                        playlistUrl,
                                    isConnecting =
                                        isConnecting,
                                    connectionError =
                                        connectionError,
                                    onBack = {
                                        if (!isConnecting) {
                                            currentStep =
                                                SetupStep.DATA
                                        }
                                    },
                                    onSave = {
                                        focusManager.clearFocus()

                                        when (
                                            selectedType
                                                ?: PlaylistType.XTREAM
                                        ) {
                                            PlaylistType.XTREAM -> {
                                                onSaveXtream(
                                                    name.trim(),
                                                    serverUrl.trim(),
                                                    username.trim(),
                                                    password
                                                )
                                            }

                                            PlaylistType.M3U -> {
                                                onSaveM3u(
                                                    name.trim(),
                                                    playlistUrl.trim()
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                /*
                 * TECLADO DO PRÓPRIO APLICATIVO.
                 * O Gboard não é utilizado nos campos desta tela.
                 */
                if (customKeyboardVisible) {
                    androidx.compose.runtime.key(activeInput) {
                    AppKeyboard(
                        modifier =
                            Modifier.fillMaxWidth(),
                        actionText = "CONCLUIR",
                        currentText = activeInput?.let { currentValue(it) }.orEmpty(),
                        allowCopy = activeInput != PlaylistInput.PASSWORD,
                        onReplaceAll = { replacement ->
                            activeInput?.let { field ->
                                changeValue(field, replacement)
                                setCursor(field, replacement.length)
                            }
                        },
                        onCharacter = {
                            insertIntoActiveInput(it)
                        },
                        onBackspace = {
                            backspaceActiveInput()
                        },
                        onAction = {
                            dismissKeyboard()
                        },
                        onMoveCursorLeft = { moveCursor(-1) },
                        onMoveCursorRight = { moveCursor(1) },
                        onExitUp = { dismissKeyboard() }
                    )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepIndicator(
    currentStep: SetupStep
) {
    val currentIndex =
        when (currentStep) {
            SetupStep.ENTRY -> 0
            SetupStep.TYPE -> 1
            SetupStep.DATA -> 2
            SetupStep.CONNECT -> 3
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        StepIndicatorItem(
            number = 1,
            label = "TIPO",
            active = currentIndex >= 1
        )

        StepConnector(
            active = currentIndex >= 2
        )

        StepIndicatorItem(
            number = 2,
            label = "DADOS",
            active = currentIndex >= 2
        )

        StepConnector(
            active = currentIndex >= 3
        )

        StepIndicatorItem(
            number = 3,
            label = "CONECTAR",
            active = currentIndex >= 3
        )
    }
}

@Composable
private fun StepIndicatorItem(
    number: Int,
    label: String,
    active: Boolean
) {
    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, MetallicGoldBrush, CircleShape)
                .background(if (active) PlaylistGlass.selection else PlaylistGlass.surface(0x600B0C10), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color =
                    if (active) {
                        Color.White
                    } else {
                        Color(0xFFE5E5E5)
                    },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = label,
            color =
                if (active) {
                    Color.White
                } else {
                    Color(0xFFE5E5E5)
                },
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StepConnector(
    active: Boolean
) {
    Box(
        modifier = Modifier
            .width(58.dp)
            .height(2.dp)
            .background(
                if (active) { PlaylistGlass.selection } else {
                    Color(0xFF3C3E45)
                }
            )
    )
}

@Composable
private fun TypeStep(
    selectedType: PlaylistType?,
    onTypeSelected: (PlaylistType) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val continueFocus = remember { FocusRequester() }
    var continueFocusRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(continueFocusRequest, selectedType) {
        if (continueFocusRequest > 0 && selectedType != null) {
            androidx.compose.runtime.withFrameNanos { }
            continueFocus.requestFocus()
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Como deseja adicionar sua lista?",
                color = Color(0xFFF5F5F5),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Escolha uma das opções abaixo",
                color = Color(0xFFF2F2F2),
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            PlaylistTypeCard(
                modifier = Modifier.weight(1f),
                title = "XTREAM CODES",
                description = "Servidor, usuário e senha",
                selected =
                    selectedType ==
                            PlaylistType.XTREAM,
                onClick = {
                    onTypeSelected(
                        PlaylistType.XTREAM
                    )
                    continueFocusRequest += 1
                }
            )

            PlaylistTypeCard(
                modifier = Modifier.weight(1f),
                title = "M3U",
                description = "URL da playlist",
                selected =
                    selectedType ==
                            PlaylistType.M3U,
                onClick = {
                    onTypeSelected(
                        PlaylistType.M3U
                    )
                    continueFocusRequest += 1
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WizardSecondaryButton(text = "← Voltar", onClick = onBack)
            WizardPrimaryButton(
                text = "CONTINUAR →",
                enabled = selectedType != null,
                modifier = Modifier.focusRequester(continueFocus),
                onClick = onContinue
            )
        }
    }
}

@Composable
private fun PlaylistTypeCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val highlighted =
        selected || isFocused

    val shape =
        RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .height(84.dp)
            .then(
                if (highlighted) {
                    Modifier.border(
                        width = 2.dp,
                        brush = MetallicGoldBrush,
                        shape = shape
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        brush = MetallicGoldBrush,
                        shape = shape
                    )
                }
            )
            .background(
                color =
                    if (highlighted) { PlaylistGlass.selection } else {
                        PlaylistGlass.surface(0x600B0C10)
                    },
                shape = shape
            )
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .clickable(
                onClick = onClick
            )
            .focusable()
            .padding(12.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = title,
            style =
                if (highlighted) {
                    TextStyle(
                        brush = (if (selected || isFocused) Brush.linearGradient(listOf(Color.White, Color.White)) else MetallicGoldBrushText),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    TextStyle(
                        color = Color(0xFFF5F5F5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
        )

        Box(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = description,
            color = Color(0xFFF2F2F2),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DataStep(
    selectedType: PlaylistType,
    name: String,
    serverUrl: String,
    username: String,
    password: String,
    playlistUrl: String,
    activeInput: PlaylistInput?,
    keyboardVisible: Boolean,
    onInputSelected: (PlaylistInput) -> Unit,
    onTextChange: (PlaylistInput, String, Int) -> Unit,
    cursorPosition: (PlaylistInput) -> Int,
    onCursorChange: (PlaylistInput, Int) -> Unit,
    fieldFocusRequesters: Map<PlaylistInput, FocusRequester>,
    onPaste: () -> Unit,
    onHardwareKey: (PlaylistInput, KeyEvent) -> Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val inputOrder = if (selectedType == PlaylistType.XTREAM)
        listOf(PlaylistInput.NAME, PlaylistInput.SERVER, PlaylistInput.USERNAME, PlaylistInput.PASSWORD)
        else listOf(PlaylistInput.NAME, PlaylistInput.M3U_URL)
    val formFocus = LocalFocusManager.current
    fun navigateInput(field: PlaylistInput, direction: Int) {
        val next = adjacentPlaylistField(inputOrder, field, direction)
        if (next != null) fieldFocusRequesters[next]?.requestFocus()
        else formFocus.moveFocus(if (direction > 0) androidx.compose.ui.focus.FocusDirection.Down else androidx.compose.ui.focus.FocusDirection.Up)
    }

    val valid =
        isDataValid(
            type = selectedType,
            name = name,
            serverUrl = serverUrl,
            username = username,
            password = password,
            playlistUrl = playlistUrl
        )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                if (keyboardVisible) {
                    6.dp
                } else {
                    10.dp
                }
            )
    ) {
        if (!keyboardVisible) {
            Text(
                text =
                    if (
                        selectedType ==
                        PlaylistType.XTREAM
                    ) {
                        "Dados do Xtream Codes"
                    } else {
                        "Dados da lista M3U"
                    },
                color = Color(0xFFF5F5F5),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }

        PlaylistTextField(
            value = name,
            label = "Nome da lista",
            cursorIndex = cursorPosition(PlaylistInput.NAME),
            onCursorChange = { onCursorChange(PlaylistInput.NAME, it) },
            onTextChange = { text, cursor -> onTextChange(PlaylistInput.NAME, text, cursor) },
            onNavigate = { direction -> navigateInput(PlaylistInput.NAME, direction) },
            fieldFocusRequester = fieldFocusRequesters[PlaylistInput.NAME],
            onHardwareKey = { event -> onHardwareKey(PlaylistInput.NAME, event) },
            active =
                activeInput ==
                        PlaylistInput.NAME,
            onFocus = {
                onInputSelected(
                    PlaylistInput.NAME
                )
            },
            onPaste = onPaste
        )

        when (selectedType) {
            PlaylistType.XTREAM -> {
                PlaylistTextField(
                    value = serverUrl,
                    label = "Servidor",
                    cursorIndex = cursorPosition(PlaylistInput.SERVER),
                    onCursorChange = { onCursorChange(PlaylistInput.SERVER, it) },
            onTextChange = { text, cursor -> onTextChange(PlaylistInput.SERVER, text, cursor) },
            onNavigate = { direction -> navigateInput(PlaylistInput.SERVER, direction) },
                    fieldFocusRequester = fieldFocusRequesters[PlaylistInput.SERVER],
                    onHardwareKey = { event -> onHardwareKey(PlaylistInput.SERVER, event) },
                    active =
                        activeInput ==
                                PlaylistInput.SERVER,
                    onFocus = {
                        onInputSelected(
                            PlaylistInput.SERVER
                        )
                    },
                    onPaste = onPaste
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        PlaylistTextField(
                            value = username,
                            label = "Usuário",
                            cursorIndex = cursorPosition(PlaylistInput.USERNAME),
                            onCursorChange = { onCursorChange(PlaylistInput.USERNAME, it) },
            onTextChange = { text, cursor -> onTextChange(PlaylistInput.USERNAME, text, cursor) },
            onNavigate = { direction -> navigateInput(PlaylistInput.USERNAME, direction) },
                            fieldFocusRequester = fieldFocusRequesters[PlaylistInput.USERNAME],
                            onHardwareKey = { event -> onHardwareKey(PlaylistInput.USERNAME, event) },
                            active =
                                activeInput ==
                                        PlaylistInput.USERNAME,
                            onFocus = {
                                onInputSelected(
                                    PlaylistInput.USERNAME
                                )
                            },
                            onPaste = onPaste
                        )
                    }

                    Box(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        PlaylistTextField(
                            value = password,
                            label = "Senha",
                            cursorIndex = cursorPosition(PlaylistInput.PASSWORD),
                            onCursorChange = { onCursorChange(PlaylistInput.PASSWORD, it) },
            onTextChange = { text, cursor -> onTextChange(PlaylistInput.PASSWORD, text, cursor) },
            onNavigate = { direction -> navigateInput(PlaylistInput.PASSWORD, direction) },
                            fieldFocusRequester = fieldFocusRequesters[PlaylistInput.PASSWORD],
                            onHardwareKey = { event -> onHardwareKey(PlaylistInput.PASSWORD, event) },
                            active =
                                activeInput ==
                                        PlaylistInput.PASSWORD,
                            visualTransformation =
                                VisualTransformation.None,
                            onFocus = {
                                onInputSelected(
                                    PlaylistInput.PASSWORD
                                )
                            },
                            onPaste = onPaste
                        )
                    }
                }
            }

            PlaylistType.M3U -> {
                PlaylistTextField(
                    value = playlistUrl,
                    label = "URL da lista M3U",
                    cursorIndex = cursorPosition(PlaylistInput.M3U_URL),
                    onCursorChange = { onCursorChange(PlaylistInput.M3U_URL, it) },
            onTextChange = { text, cursor -> onTextChange(PlaylistInput.M3U_URL, text, cursor) },
            onNavigate = { direction -> navigateInput(PlaylistInput.M3U_URL, direction) },
                    fieldFocusRequester = fieldFocusRequesters[PlaylistInput.M3U_URL],
                    onHardwareKey = { event -> onHardwareKey(PlaylistInput.M3U_URL, event) },
                    active =
                        activeInput ==
                                PlaylistInput.M3U_URL,
                    onFocus = {
                        onInputSelected(
                            PlaylistInput.M3U_URL
                        )
                    },
                    onPaste = onPaste
                )
            }
        }

        if (!keyboardVisible) {
            WizardNavigationButtons(
                primaryText = "CONTINUAR →",
                primaryEnabled = valid,
                onBack = onBack,
                onPrimary = onContinue
            )
        }
    }
}

@Composable
private fun ConnectStep(
    selectedType: PlaylistType,
    name: String,
    serverUrl: String,
    username: String,
    password: String,
    playlistUrl: String,
    isConnecting: Boolean,
    connectionError: String?,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var visibleError by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(connectionError) {
        if (!connectionError.isNullOrBlank()) {
            visibleError = connectionError

            /*
             * O aviso é apenas temporário:
             * sinaliza o problema e some sozinho.
             */
            kotlinx.coroutines.delay(3_000L)

            if (visibleError == connectionError) {
                visibleError = null
            }
        } else {
            visibleError = null
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text =
                    if (isConnecting) {
                        "Conectando à sua lista"
                    } else {
                        "Tudo pronto para conectar"
                    },
                color = Color(0xFFF5F5F5),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text =
                    when {
                        isConnecting ->
                            "Validando acesso e carregando o conteúdo..."

                        !visibleError.isNullOrBlank() ->
                            "Erro: ${visibleError.orEmpty()}"

                        else ->
                            "Confira os dados antes de salvar."
                    },
                color =
                    if (!visibleError.isNullOrBlank()) {
                        Color(0xFFFFB4AB)
                    } else {
                        Color(0xFFF2F2F2)
                    },
                fontSize = 12.sp,
                fontWeight =
                    if (!visibleError.isNullOrBlank()) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                maxLines = 1,
                overflow =
                    androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width =
                        if (isConnecting) {
                            1.5.dp
                        } else {
                            1.dp
                        },
                    brush =
                        if (isConnecting) {
                            MetallicGoldBrush
                        } else {
                            MetallicGoldBrush
                        },
                    shape = RoundedCornerShape(14.dp)
                )
                .background(
                    color =
                        if (isConnecting) {
                            PlaylistGlass.surface(0xC008090B)
                        } else {
                            PlaylistGlass.surface(0xC008090B)
                        },
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(18.dp)
            ) {
                SummaryRow(
                    modifier = Modifier.weight(1f),
                    label = "Nome",
                    value = name
                )

                SummaryRow(
                    modifier = Modifier.weight(1f),
                    label = "Tipo",
                    value =
                        if (
                            selectedType ==
                            PlaylistType.XTREAM
                        ) {
                            "XTREAM CODES"
                        } else {
                            "M3U"
                        }
                )
            }

            when (selectedType) {
                PlaylistType.XTREAM -> {
                    SummaryRow(
                        label = "Servidor",
                        value = serverUrl
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(18.dp)
                    ) {
                        SummaryRow(
                            modifier = Modifier.weight(1f),
                            label = "Usuário",
                            value = username
                        )

                        SummaryRow(
                            modifier = Modifier.weight(1f),
                            label = "Senha",
                            value =
                                password
                        )
                    }
                }

                PlaylistType.M3U -> {
                    SummaryRow(
                        label = "URL",
                        value = playlistUrl,
                        valueMaxLines = 2
                    )
                }
            }
        }

        if (isConnecting) {
            ConnectingPanel()
        } else {
            WizardNavigationButtons(
                primaryText = "SALVAR E CONECTAR",
                primaryEnabled = true,
                secondaryEnabled = true,
                onBack = onBack,
                onPrimary = onSave
            )
        }

    }
}

@Composable
private fun ConnectingPanel() {
    val transition =
        rememberInfiniteTransition(
            label = "playlist_connection_loader"
        )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 850,
                easing = LinearEasing
            )
        ),
        label = "playlist_connection_rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .border(
                width = 1.dp,
                brush = MetallicGoldBrush,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = PlaylistGlass.surface(0x60111419),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(28.dp)
                .rotate(rotation)
        ) {
            val strokeWidth =
                3.dp.toPx()

            drawCircle(
                color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF3E3621),
                style = Stroke(
                    width = strokeWidth
                )
            )

            drawArc(
                brush = MetallicGoldBrush,
                startAngle = -90f,
                sweepAngle = 265f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth
                )
            )
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "CONECTANDO",
                style = TextStyle(
                    brush = MetallicGoldBrushText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "Aguarde enquanto validamos sua lista.",
                color = Color(0xFFF2F2F2),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 1
) {
    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Color(0xFFF2F2F2),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = value,
            color = Color(0xFFF1F1F3),
            fontSize = 12.sp,
            maxLines = valueMaxLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WizardNavigationButtons(
    primaryText: String,
    primaryEnabled: Boolean,
    secondaryEnabled: Boolean = true,
    onBack: () -> Unit,
    onPrimary: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        WizardSecondaryButton(
            text = "← VOLTAR",
            enabled = secondaryEnabled,
            onClick = onBack
        )

        WizardPrimaryButton(
            text = primaryText,
            enabled = primaryEnabled,
            onClick = onPrimary
        )
    }
}

@Composable
private fun WizardSecondaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val shape =
        RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .height(40.dp)
            .border(
                width =
                    if (isFocused) {
                        2.dp
                    } else {
                        1.dp
                    },
                brush = MetallicGoldBrush,
                shape = shape
            )
            .background(
                color = if (isFocused) PlaylistGlass.selection else PlaylistGlass.surface(0x60111419),
                shape = shape
            )
            .onFocusChanged {
                isFocused =
                    enabled && it.isFocused
            }
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .focusable(enabled)
            .padding(
                horizontal = 22.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = text,
            color =
                when {
                    !enabled ->
                        Color(0xFF66686E)

                    isFocused ->
                        Color.White

                    else ->
                        Color(0xFFF2F2F2)
                },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WizardPrimaryButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val shape =
        RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .height(40.dp)
            .background(
                color = if (enabled && isFocused) PlaylistGlass.selection else PlaylistGlass.surface(0x60111419),
                shape = shape
            )
            .border(
                width = if (enabled && isFocused) 2.dp else 1.dp,
                brush = MetallicGoldBrush,
                shape = shape
            )
            .onFocusChanged {
                isFocused = enabled && it.isFocused
            }
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .focusable(enabled)
            .padding(
                horizontal = 24.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = text,
            color =
                if (enabled && isFocused) {
                    Color.White
                } else if (enabled) {
                    Color.White
                } else {
                    Color.White
                },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Cursor virtual da TV: o teclado do aplicativo recebe o foco, portanto o
 * cursor nativo de um TextField em modo readOnly nao fica visivel.
 *
 * Marcamos visualmente o caractere logo a direita do ponto de insercao.
 * Quando o cursor chega ao fim, exibimos uma barra dourada pulsante.
 *
 * Importante: aplica primeiro a transformacao original (inclusive a da senha).
 * Assim nenhum caractere de senha e revelado pelo indicador visual.
 */
private class PlaylistVisibleCursorTransformation(
    private val delegate: VisualTransformation,
    private val cursorIndex: Int,
    private val blinkVisible: Boolean
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val base = delegate.filter(text)
        val output = base.text
        val visualPosition = base.offsetMapping
            .originalToTransformed(cursorIndex.coerceIn(0, text.length))
            .coerceIn(0, output.length)

        if (visualPosition < output.length) {
            val codePointLength = Character.charCount(
                Character.codePointAt(output.text, visualPosition)
            )
            val characterEnd = (visualPosition + codePointLength).coerceAtMost(output.length)
            val highlighted = buildAnnotatedString {
                append(output.subSequence(0, visualPosition))
                withStyle(
                    SpanStyle(
                        color = if (blinkVisible) br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE991)
                        else br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFB18B34),
                        background = if (blinkVisible) br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0x804B3910)
                        else br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0x303B2E13),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(output.subSequence(visualPosition, characterEnd))
                }
                append(output.subSequence(characterEnd, output.length))
            }
            return TransformedText(highlighted, base.offsetMapping)
        }

        // Nao existe caractere a direita: mostra onde o proximo sera digitado.
        // O indicador e virtual; a posicao real do texto continua inalterada.
        val trailingCursor = buildAnnotatedString {
            append(output)
            withStyle(
                SpanStyle(
                    color = if (blinkVisible) br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE991)
                    else Color.Transparent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("▌")
            }
        }
        return TransformedText(
            trailingCursor,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    base.offsetMapping.originalToTransformed(
                        offset.coerceIn(0, text.length)
                    ).coerceIn(0, trailingCursor.length)

                override fun transformedToOriginal(offset: Int): Int =
                    base.offsetMapping.transformedToOriginal(
                        offset.coerceIn(0, output.length)
                    ).coerceIn(0, text.length)
            }
        )
    }
}

@Composable
private fun PlaylistTextField(
    value: String,
    label: String,
    active: Boolean,
    cursorIndex: Int,
    onCursorChange: (Int) -> Unit,
    onHardwareKey: (KeyEvent) -> Boolean,
    fieldFocusRequester: FocusRequester?,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onFocus: () -> Unit,
    onPaste: () -> Unit,
    onTextChange: (String, Int) -> Unit,
    onNavigate: (Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tvInput = remember(context) { usesPlaylistTvKeyboard(context) }
    val focusManager = LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val bringIntoView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val shape = RoundedCornerShape(9.dp)
    var isFocused by remember { mutableStateOf(false) }
    var touchValue by remember(label) { mutableStateOf(TextFieldValue(value, TextRange(cursorIndex.coerceIn(0, value.length)))) }
    LaunchedEffect(value, cursorIndex, tvInput) {
        if (!tvInput && touchValue.text != value) {
            touchValue = TextFieldValue(value, TextRange(cursorIndex.coerceIn(0, value.length)))
        }
    }
    var blinkVisible by remember(active) { mutableStateOf(true) }
    LaunchedEffect(active, cursorIndex, value, tvInput) {
        blinkVisible = true
        if (active && tvInput) while (true) {
            delay(550L)
            blinkVisible = !blinkVisible
        }
    }
    LaunchedEffect(active, isFocused) {
        if (active || isFocused) {
            delay(100L)
            bringIntoView.bringIntoView()
        }
    }
    val transformation = if (active && tvInput) {
        PlaylistVisibleCursorTransformation(visualTransformation, cursorIndex, blinkVisible)
    } else visualTransformation
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = TextStyle(
            color = if (active || isFocused) Color.White else Color(0xFFF2F2F2),
            fontSize = 11.sp, fontWeight = FontWeight.Medium))
        BasicTextField(
            value = if (tvInput) TextFieldValue(value, TextRange(cursorIndex.coerceIn(0, value.length))) else touchValue,
            onValueChange = { updated ->
                if (tvInput) {
                    if (updated.text == value) onCursorChange(updated.selection.end)
                } else {
                    touchValue = updated
                    onTextChange(updated.text, updated.selection.end)
                }
            },
            readOnly = tvInput,
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFFF5F5F5), fontSize = 14.sp),
            visualTransformation = transformation,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0)),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = when {
                    label.contains("senha", true) -> androidx.compose.ui.text.input.KeyboardType.Password
                    label.contains("URL", true) || label.contains("servidor", true) -> androidx.compose.ui.text.input.KeyboardType.Uri
                    else -> androidx.compose.ui.text.input.KeyboardType.Text
                },
                imeAction = if (label.contains("senha", true) || label.contains("M3U", true))
                    androidx.compose.ui.text.input.ImeAction.Done else androidx.compose.ui.text.input.ImeAction.Next,
                autoCorrectEnabled = false
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onNext = { onNavigate(1) },
                onDone = { keyboard?.hide(); focusManager.clearFocus() }
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .then(if (fieldFocusRequester != null) Modifier.focusRequester(fieldFocusRequester) else Modifier)
                .bringIntoViewRequester(bringIntoView)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused && !tvInput) onFocus()
                }
                .onPreviewKeyEvent { event ->
                    if (!tvInput) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionUp -> { onNavigate(-1); return@onPreviewKeyEvent true }
                            Key.DirectionDown -> { onNavigate(1); return@onPreviewKeyEvent true }
                            Key.Tab -> { onNavigate(if (event.nativeKeyEvent.isShiftPressed) -1 else 1); return@onPreviewKeyEvent true }
                        }
                    }
                    if (onHardwareKey(event)) return@onPreviewKeyEvent true
                    val select = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                    if (select && event.type == KeyEventType.KeyUp) { onFocus(); true }
                    else select
                }
                .then(if (tvInput) Modifier.clickable(onClick = onFocus) else Modifier)
                .border(if (active || isFocused) 1.7.dp else 1.dp, MetallicGoldBrush, shape)
                .background(if (active || isFocused) PlaylistGlass.selection else PlaylistGlass.surface(0x600B0C10), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

private fun isDataValid(
    type: PlaylistType,
    name: String,
    serverUrl: String,
    username: String,
    password: String,
    playlistUrl: String
): Boolean {
    if (name.isBlank()) {
        return false
    }

    return when (type) {
        PlaylistType.XTREAM -> {
            serverUrl.isNotBlank() &&
                    username.isNotBlank() &&
                    password.isNotBlank()
        }

        PlaylistType.M3U -> {
            playlistUrl.isNotBlank()
        }
    }
}

/** First page contains only remote registration; manual input starts its own wizard. */
@Composable
private fun PlaylistPortalEntry(onManual: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val identity by androidx.compose.runtime.produceState<br.com.flaviotorres.iptvpremium.core.device.DeviceIdentity?>(null, context) {
        while (true) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { br.com.flaviotorres.iptvpremium.core.device.DeviceIdentityStore(context).getOrCreate() }.getOrNull()
            }
            kotlinx.coroutines.delay(5_000)
        }
    }
    var qrGeneration by remember { mutableIntStateOf(0) }
    val remote: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (identity != null && br.com.flaviotorres.iptvpremium.BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
                br.com.flaviotorres.iptvpremium.core.device.DevicePortalQr(compact = true, showRefreshButton = false, refreshKey = qrGeneration)
            } else {
                Text("Conectando à central para preparar o QR Code…", color = Color(0xFFF2F2F2), fontSize = 15.sp)
            }
        }
    }
    val identityPanel: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("SEU APARELHO", color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("MAC: ${identity?.displayId ?: "Aguardando cadastro"}", color = Color.White, fontSize = 15.sp)
            Text("Código: ${identity?.code ?: "—"}", color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Cadastre a lista no site e sincronize para recebê-la neste aparelho.", color = Color(0xFFF2F2F2), fontSize = 14.sp)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        androidx.compose.foundation.layout.BoxWithConstraints {
            if (maxWidth >= 600.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) { remote() }
                    Box(Modifier.weight(1f)) { identityPanel() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { remote(); identityPanel() }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) {
                br.com.flaviotorres.iptvpremium.core.device.PortalOutlineButton("Gerar novo QR Code", enabled = identity != null, onClick = { qrGeneration++ })
            }
            Box(Modifier.weight(1f)) {
                if (br.com.flaviotorres.iptvpremium.BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
                    br.com.flaviotorres.iptvpremium.core.device.PortalSyncControls(listSync = true)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Prefere digitar os dados neste aparelho?", color = Color(0xFFF2F2F2), fontSize = 14.sp)
            br.com.flaviotorres.iptvpremium.core.device.PortalOutlineButton("Inserir manualmente", modifier = Modifier.width(260.dp), onClick = onManual)
        }
    }
}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_POLISH_LOADING_HOLD_V1

// IPTV_LISTS_SUBTITLES_CHANNELS_V1

// IPTV_DEVICE_IDENTITY_V1

// IPTV_PLAYLIST_ENTRY_BRAND_V1

// IPTV_ENTRY_ALIGNED_BUTTONS_V1

// IPTV_LIST_LOADING_AND_PLAYER_INPUT_V1

// Text retains the approved palette; blue darkening applies to rims.
private val MetallicGoldBrushText get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF926315))
)
