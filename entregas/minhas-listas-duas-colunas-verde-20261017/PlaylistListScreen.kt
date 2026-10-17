package br.com.flaviotorres.iptvpremium.ui.playlist

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxHeight
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource as painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.delay

private val MetallicGoldBrush get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF926315))
)

// IPTV_PREMIUM_TWO_COLUMN_GREEN_V1
private val ListMint = Color(0xFF4BECC4)
private val ListNeon = Color(0xFF00EC93)
private val ListMuted = Color(0xFFA8BEC2)
private val ListShape = RoundedCornerShape(12.dp)

@Composable
fun PlaylistListScreen(
    playlists: List<PlaylistSource>,
    activePlaylistId: String? = null,
    loadingPlaylistId: String? = null,
    onPlaylistClick: (PlaylistSource) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (PlaylistSource) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<PlaylistSource?>(null) }
    val itemFocus = remember(playlists.map { it.id }) {
        playlists.associate { it.id to FocusRequester() }
    }
    val useFocus = remember { FocusRequester() }
    val deleteFocus = remember { FocusRequester() }
    LaunchedEffect(playlists.map { it.id }) {
        if (playlists.isNotEmpty()) {
            if (playlists.none { it.id == selectedId }) selectedId = playlists.first().id
            delay(80L)
            itemFocus[playlists.first().id]?.requestFocus()
        } else selectedId = null
    }
    val selected = playlists.firstOrNull { it.id == selectedId } ?: playlists.firstOrNull()
    val selectedLoading = selected != null && selected.id == loadingPlaylistId

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF02090B), Color(0xFF061516), Color(0xFF02090B))))
            .padding(horizontal = 28.dp, vertical = 22.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("IPTV", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("PREMIUM", color = Color(0xFFE5C05B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.width(1.dp).height(42.dp).background(Color(0xFF345258)))
                PlaylistGlyph(Modifier.size(28.dp), ListMint)
                Column(Modifier.weight(1f)) {
                    Text("MINHAS LISTAS", color = ListMint, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("Selecione uma lista para utilizar", color = Color(0xFFE6EBEC), fontSize = 12.sp)
                }
                PlaylistAction("Adicionar", false, true, onAddClick,
                    modifier = Modifier.width(145.dp),
                    symbol = "+")
            }
            Box(Modifier.height(16.dp))
            if (playlists.isEmpty()) {
                EmptyPlaylistState(Modifier.fillMaxSize())
            } else {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LazyColumn(
                        Modifier.weight(0.43f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            val selectedRow = selected?.id == playlist.id
                            val loading = loadingPlaylistId == playlist.id
                            Row(
                                Modifier.fillMaxWidth().height(88.dp)
                                    .border(if (selectedRow) 2.dp else 1.dp,
                                        if (selectedRow) ListNeon else Color(0xFF254147), ListShape)
                                    .background(
                                        if (selectedRow)
                                            Brush.horizontalGradient(listOf(Color(0xFF04221F), Color(0xFF073C32), Color(0xFF04221F)))
                                        else Brush.horizontalGradient(listOf(Color(0xFF0C181C), Color(0xFF081216))),
                                        ListShape
                                    )
                                    .focusRequester(itemFocus.getValue(playlist.id))
                                    .onFocusChanged { if (it.isFocused) selectedId = playlist.id }
                                    .onPreviewKeyEvent { event ->
                                        if (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT) {
                                            if (event.type == KeyEventType.KeyDown &&
                                                event.nativeKeyEvent.repeatCount == 0) {
                                                selectedId = playlist.id
                                                useFocus.requestFocus()
                                            }
                                            true
                                        } else false
                                    }
                                    .clickable(enabled = !loading, onClickLabel = "Usar lista") {
                                        selectedId = playlist.id
                                        onPlaylistClick(playlist)
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PlaylistDoc(
                                    if (playlist is PlaylistSource.M3u) "M3U" else "XTREAM",
                                    Modifier.width(46.dp).height(56.dp), Color(0xFFE8C45B)
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(playlist.name, color = Color.White, fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(
                                        if (loading) "Carregando..." else
                                            "Protocolo: " + if (playlist is PlaylistSource.M3u) "M3U" else "Xtream Codes",
                                        color = ListMuted, fontSize = 11.sp, maxLines = 1
                                    )
                                }
                                if (playlist.id == activePlaylistId) {
                                    Box(Modifier.border(1.dp, Color(0xFF247F55), RoundedCornerShape(7.dp))
                                        .background(Color(0xFF063827), RoundedCornerShape(7.dp))
                                        .padding(horizontal = 7.dp, vertical = 6.dp)) {
                                        Text("✓ Em uso", color = Color(0xFF61F2A1), fontSize = 10.sp,
                                            maxLines = 1, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        Modifier.weight(0.57f).fillMaxHeight()
                            .border(1.dp, Color(0xFF2C5358), ListShape)
                            .background(
                                Brush.verticalGradient(listOf(Color(0xFF0B2022), Color(0xFF071216))),
                                ListShape
                            )
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (selected != null) {
                            val type = if (selected is PlaylistSource.M3u) "M3U" else "Xtream Codes"
                            Row(
                                Modifier.fillMaxWidth().height(108.dp),
                                horizontalArrangement = Arrangement.spacedBy(22.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlaylistDoc(type, Modifier.size(78.dp), ListMint)
                                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(selected.name, color = Color.White, fontSize = 22.sp,
                                        maxLines = 2, fontWeight = FontWeight.Bold)
                                    Text("Tipo de lista", color = ListMuted, fontSize = 12.sp)
                                    Text(type, color = Color.White, fontSize = 15.sp)
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF24494E)))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PlaylistGlyph(Modifier.size(29.dp), ListMint)
                                    Column {
                                        Text("Protocolo", color = ListMuted, fontSize = 12.sp)
                                        Text(type, color = Color.White, fontSize = 14.sp)
                                    }
                                }
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PlaylistGlyph(Modifier.size(29.dp), ListMint)
                                    Column {
                                        Text("Status", color = ListMuted, fontSize = 12.sp)
                                        Text(when {
                                            selectedLoading -> "Carregando..."
                                            selected.id == activePlaylistId -> "Em uso"
                                            else -> "Disponível para uso"
                                        }, color = Color.White, fontSize = 13.sp, maxLines = 2)
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PlaylistAction(
                                    "Usar lista", true, !selectedLoading,
                                    { onPlaylistClick(selected) }, Modifier.weight(1.25f),
                                    requester = useFocus,
                                    left = { itemFocus[selected.id]?.requestFocus() },
                                    down = { deleteFocus.requestFocus() },
                                    symbol = "▶"
                                )
                                PlaylistAction(
                                    "Excluir", false, !selectedLoading,
                                    { pendingDelete = selected }, Modifier.weight(1f),
                                    requester = deleteFocus,
                                    left = { itemFocus[selected.id]?.requestFocus() },
                                    up = { useFocus.requestFocus() },
                                    symbol = "♧"
                                )
                            }
                        }
                    }
                }
            }
        }
        pendingDelete?.let { playlist ->
            ConfirmDeletePlaylistDialog(
                playlist.name,
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    pendingDelete = null
                    if (playlist.id != loadingPlaylistId) onDeleteClick(playlist)
                }
            )
        }
    }
}

@Composable
private fun PlaylistAction(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    requester: FocusRequester? = null,
    left: (() -> Unit)? = null,
    up: (() -> Unit)? = null,
    down: (() -> Unit)? = null,
    symbol: String
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(9.dp)
    Row(
        modifier.height(50.dp)
            .border(if (focused) 2.dp else 1.dp,
                if (focused || primary) ListNeon else Color(0xFF365359), shape)
            .background(
                if (primary) Brush.verticalGradient(listOf(Color(0xFF00C987), Color(0xFF05604C)))
                else Brush.verticalGradient(listOf(Color(0xFF14262C), Color(0xFF0A171D))),
                shape
            )
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false
                else when(event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT ->
                        if (left != null) { left(); true } else false
                    AndroidKeyEvent.KEYCODE_DPAD_UP ->
                        if (up != null) { up(); true } else false
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN ->
                        if (down != null) { down(); true } else false
                    else -> false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(symbol, color = if (label == "Adicionar") ListMint else Color.White,
            fontSize = if (label == "Adicionar") 25.sp else 17.sp)
        Box(Modifier.width(10.dp))
        Text(label, color = Color.White, fontSize = 14.sp, maxLines = 1,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlaylistDoc(type: String, modifier: Modifier, tint: Color) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.7.dp.toPx()
            drawRoundRect(tint, topLeft = Offset(size.width * .16f, size.height * .05f),
                size = Size(size.width * .68f, size.height * .88f),
                cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(stroke))
            for (i in 0..2) {
                val y = size.height * (.28f + i * .16f)
                drawLine(tint, Offset(size.width * .28f, y),
                    Offset(size.width * .67f, y), stroke * 1.3f, cap = StrokeCap.Round)
            }
        }
        Text(if (type.contains("M3U", true)) "M3U" else "XT", color = tint,
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
    }
}

@Composable
private fun PlaylistGlyph(modifier: Modifier, tint: Color) {
    Canvas(modifier) {
        val stroke = 1.8.dp.toPx()
        drawRoundRect(tint, topLeft = Offset(size.width * .14f, size.height * .08f),
            size = Size(size.width * .72f, size.height * .84f),
            cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(stroke))
        for(i in 0..2) {
            val y = size.height * (.3f + i * .19f)
            drawLine(tint, Offset(size.width * .3f, y),
                Offset(size.width * .7f, y), stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun AddPlaylistButton(
    onClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val shape =
        RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(
                if (isFocused) {
                    1.08f
                } else {
                    1f
                }
            )
            .border(
                width =
                    if (isFocused) {
                        2.dp
                    } else {
                        1.5.dp
                    },
                brush = MetallicGoldBrush,
                shape = shape
            )
            .background(
                color = if (isFocused) br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color() else Color(0x60111419),
                shape = shape
            )
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .clickable(
                onClickLabel = "Cadastrar nova lista",
                onClick = onClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            style = TextStyle(
                brush = MetallicGoldBrushText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
private fun PlaylistCard(
    initialFocusRequester: FocusRequester?,
    playlist: PlaylistSource,
    isActive: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val defaultSelectFocus = remember { FocusRequester() }
    val selectFocus = initialFocusRequester ?: defaultSelectFocus
    val deleteFocus = remember { FocusRequester() }
    val shape = RoundedCornerShape(14.dp)

    val typeText = when (playlist) {
        is PlaylistSource.XtreamCodes -> "XTREAM CODES"
        is PlaylistSource.M3u -> "M3U"
    }

    // O contorno pertence SOMENTE ao card; a lixeira e um controle separado.
    Row(
        modifier = Modifier.fillMaxWidth().height(82.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(
                    width = if (isFocused || isActive || isLoading) 2.dp else 1.dp,
                    brush = MetallicGoldBrush,
                    shape = shape
                )
                .background(
                    color = if (isActive || isFocused) {
                        br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color()
                    } else {
                        Color(0x600B0C10)
                    },
                    shape = shape
                )
                .padding(start = 20.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(selectFocus)
                        .focusProperties {
                            right = if (isLoading) FocusRequester.Cancel else deleteFocus
                        }
                        .onFocusChanged { isFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT) {
                                if (!isLoading && event.type == KeyEventType.KeyDown &&
                                    event.nativeKeyEvent.repeatCount == 0
                                ) {
                                    deleteFocus.requestFocus()
                                }
                                true
                            } else {
                                false
                            }
                        }
                        .clickable(
                            enabled = !isLoading,
                            onClickLabel = "Carregar lista",
                            onClick = onClick
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    PlaylistTypeBadge(
                        type = typeText,
                        highlighted = isFocused || isActive || isLoading
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = playlist.name,
                            color = Color(0xFFF5F5F5),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when {
                                isLoading && isActive -> "$typeText • ATUALIZANDO..."
                                isLoading -> "$typeText • CARREGANDO..."
                                isActive -> "$typeText • LISTA ATIVA"
                                else -> typeText
                            },
                            color = if (isActive || isLoading) {
                                br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFF1D77A)
                            } else {
                                Color(0xFFF2F2F2)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // O carregamento continua dentro do card, na extremidade direita.
                if (isLoading) {
                    PlaylistLoadingIndicator()
                }
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.size(40.dp))
        } else {
            DeletePlaylistButton(
                focusRequester = deleteFocus,
                selectFocusRequester = selectFocus,
                onMoveLeft = { selectFocus.requestFocus() },
                name = playlist.name,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun PlaylistLoadingIndicator() {
    val transition =
        rememberInfiniteTransition(
            label = "playlistLoading"
        )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = LinearEasing
            )
        ),
        label = "playlistLoadingRotation"
    )

    Canvas(
        modifier = Modifier.size(34.dp)
    ) {
        drawArc(
            brush = MetallicGoldBrush,
            startAngle = rotation,
            sweepAngle = 265f,
            useCenter = false,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun PlaylistTypeBadge(
    type: String,
    highlighted: Boolean
) {
    Box(
        modifier = Modifier
            .width(112.dp)
            .height(34.dp)
            .then(
                if (highlighted) {
                    Modifier.border(
                        width = 1.5.dp,
                        brush = MetallicGoldBrush,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color(0xFF454A55),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            )
            .background(
                color = Color(0x60111419),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment =
            Alignment.Center
    ) {
        if (highlighted) {
            Text(
                text = type,
                style = TextStyle(
                    brush = MetallicGoldBrushText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        } else {
            Text(
                text = type,
                color = Color(0xFFF2F2F2),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeletePlaylistButton(
    focusRequester: FocusRequester,
    selectFocusRequester: FocusRequester,
    onMoveLeft: () -> Unit,
    name: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(9.dp)
    val iconColor = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFF1D77A)

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(if (isFocused) 1.08f else 1f)
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                brush = MetallicGoldBrush,
                shape = shape
            )
            .background(
                color = if (isFocused) {
                    br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color()
                } else {
                    Color(0x60111419)
                },
                shape = shape
            )
            .focusRequester(focusRequester)
            // Seta para cima/baixo nunca entra em outra lixeira:
            // primeiro devolve o foco ao card principal da lista.
            .focusProperties {
                left = selectFocusRequester
                up = selectFocusRequester
                down = selectFocusRequester
            }
            .semantics { contentDescription = "Excluir lista $name" }
            .onPreviewKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                if (keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT ||
                    keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                ) {
                    if (event.type == KeyEventType.KeyDown &&
                        event.nativeKeyEvent.repeatCount == 0
                    ) onMoveLeft()
                    true
                } else false
            }
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClickLabel = "Excluir lista $name", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Lixeira vetorial: sem fonte ou dependencia adicional; segue ThemeColors.
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = 1.8.dp.toPx()
            drawLine(iconColor, Offset(w * .18f, h * .25f), Offset(w * .82f, h * .25f),
                strokeWidth, cap = StrokeCap.Round)
            drawLine(iconColor, Offset(w * .40f, h * .16f), Offset(w * .60f, h * .16f),
                strokeWidth, cap = StrokeCap.Round)
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(w * .27f, h * .34f),
                size = Size(w * .46f, h * .49f),
                cornerRadius = CornerRadius(w * .06f),
                style = Stroke(width = strokeWidth)
            )
            drawLine(iconColor, Offset(w * .43f, h * .44f), Offset(w * .43f, h * .72f),
                strokeWidth * .7f, cap = StrokeCap.Round)
            drawLine(iconColor, Offset(w * .57f, h * .44f), Offset(w * .57f, h * .72f),
                strokeWidth * .7f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun ConfirmDeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val cancelFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(.84f)
                .widthIn(max = 480.dp)
                .border(1.5.dp, MetallicGoldBrush, RoundedCornerShape(18.dp))
                .background(Color(0xFF141518), RoundedCornerShape(18.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "EXCLUIR LISTA?",
                style = TextStyle(
                    brush = MetallicGoldBrushText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "Deseja remover a lista \"$playlistName\"? O cadastro e o cache " +
                    "dessa lista serão excluídos do aparelho.",
                color = Color(0xFFF2F2F2),
                fontSize = 15.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeleteDialogButton(
                    label = "Cancelar",
                    modifier = Modifier.weight(1f),
                    focusRequester = cancelFocus,
                    onClick = onDismiss
                )
                DeleteDialogButton(
                    label = "Excluir",
                    modifier = Modifier.weight(1f),
                    onClick = onConfirm
                )
            }
        }
        LaunchedEffect(Unit) { cancelFocus.requestFocus() }
    }
}

@Composable
private fun DeleteDialogButton(
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .border(if (isFocused) 2.dp else 1.dp, MetallicGoldBrush, shape)
            .background(
                if (isFocused) {
                    br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color()
                } else {
                    Color(0x60111419)
                },
                shape
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClickLabel = label, onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFF1D77A),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyPlaylistState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nenhuma lista cadastrada",
                color = Color(0xFFF5F5F5),
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Use o botão + para adicionar sua primeira lista.",
                color = Color(0xFF9FA2AA),
                fontSize = 14.sp
            )
        }
    }
}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_POLISH_LOADING_HOLD_V1

// IPTV_LISTS_SUBTITLES_CHANNELS_V1

// IPTV_PLAYLIST_ENTRY_BRAND_V1

// IPTV_LIST_LOADING_AND_PLAYER_INPUT_V1

// Text retains the approved palette; blue darkening applies to rims.
private val MetallicGoldBrushText get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF926315))
)
