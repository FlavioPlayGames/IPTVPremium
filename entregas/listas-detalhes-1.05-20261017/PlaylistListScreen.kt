package br.com.flaviotorres.iptvpremium.ui.playlist

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors
import br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme

private val listShape = RoundedCornerShape(11.dp)
private fun playlistProtocol(playlist: PlaylistSource) = if (playlist is PlaylistSource.M3u) "M3U" else "Xtream Codes"

/** Selection only previews a list; loading and confirmed deletion are explicit actions. */
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
    val ids = playlists.map { it.id }
    val requesters = remember(ids) { ids.associateWith { FocusRequester() } }
    val addFocus = remember { FocusRequester() }
    val useFocus = remember { FocusRequester() }
    val deleteFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val selected = playlists.firstOrNull { it.id == selectedId }
        ?: playlists.firstOrNull { it.id == activePlaylistId } ?: playlists.firstOrNull()
    LaunchedEffect(ids) {
        if (pendingDelete?.id !in ids) pendingDelete = null
        if (selectedId !in ids) {
            selectedId = selected?.id
            if (selected != null) {
                listState.scrollToItem(ids.indexOf(selected.id).coerceAtLeast(0))
                withFrameNanos { }
                requesters[selected.id]?.requestFocus()
            } else {
                withFrameNanos { }
                addFocus.requestFocus()
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(ThemeColors.background).padding(18.dp)) {
        val wide = maxWidth >= 580.dp
        Column(Modifier.fillMaxSize()
            .border(1.2.dp, ThemeColors.metallicBorder, RoundedCornerShape(20.dp))
            .background(ThemeColors.panel, RoundedCornerShape(20.dp))
            .padding(if (wide) 22.dp else 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("MINHAS LISTAS", color = ThemeColors.color(0xFFFFE6A0),
                        fontSize = if (wide) 24.sp else 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Selecione uma lista para utilizar", color = Color.White,
                        fontSize = 12.sp)
                }
                ListDetailAction("Adicionar", "+", false, true, onAddClick,
                    Modifier.width(if (wide) 142.dp else 118.dp).focusRequester(addFocus)
                        .focusProperties { down = selected?.id?.let { requesters[it] } ?: FocusRequester.Default })
            }
            Spacer(Modifier.height(if (wide) 22.dp else 14.dp))
            if (selected == null) {
                Column(Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    ListDocumentIcon(Modifier.size(54.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Nenhuma lista cadastrada", color = Color.White, fontSize = 20.sp)
                    Text("Use Adicionar para cadastrar sua primeira lista.", color = Color.White, fontSize = 13.sp)
                }
            } else {
                val rows: @Composable (Modifier) -> Unit = { modifier ->
                    LazyColumn(modifier, state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(playlists, key = { _, item -> item.id }) { index, playlist ->
                            val highlighted = selected.id == playlist.id
                            Row(Modifier.fillMaxWidth().heightIn(min = 76.dp)
                                .border(if (highlighted) 1.8.dp else 1.dp, ThemeColors.metallicBorder, listShape)
                                .background(if (highlighted) SelectionTheme.color() else ThemeColors.control, listShape)
                                .focusRequester(requesters.getValue(playlist.id))
                                .focusProperties { if (index == 0) up = addFocus }
                                .onFocusChanged { if (it.isFocused) selectedId = playlist.id }
                                .onPreviewKeyEvent { event ->
                                    if (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT) {
                                        if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) {
                                            selectedId = playlist.id
                                            useFocus.requestFocus()
                                        }
                                        true
                                    } else false
                                }
                                .semantics { this.selected = highlighted }
                                .clickable(role = Role.Button, onClickLabel = "Selecionar lista") { selectedId = playlist.id }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.width(if (wide) 94.dp else 76.dp).height(36.dp)
                                    .border(1.dp, ThemeColors.metallicBorder, RoundedCornerShape(7.dp)),
                                    contentAlignment = Alignment.Center) {
                                    Text(playlistProtocol(playlist).uppercase(), color = Color.White,
                                        fontSize = if (playlist is PlaylistSource.M3u) 12.sp else 9.sp,
                                        maxLines = 1, fontWeight = FontWeight.SemiBold)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(playlist.name, color = Color.White, fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(if (playlist.id == loadingPlaylistId) "Carregando..." else playlistProtocol(playlist),
                                            color = Color.White, fontSize = 12.sp, maxLines = 1)
                                        if (playlist.id == activePlaylistId) {
                                            Text("Em uso", color = Color.White, fontSize = 10.sp,
                                                modifier = Modifier.border(1.dp, ThemeColors.metallicBorder, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                val details: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 24.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ListDocumentIcon(Modifier.size(if (wide) 62.dp else 42.dp))
                        Text(selected.name, color = Color.White, fontSize = 25.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Lista " + playlistProtocol(selected), color = Color.White, fontSize = 14.sp)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(ThemeColors.metallicBorder))
                        ListDetailAction(if (selected.id == loadingPlaylistId) "Carregando..." else "Usar lista", "▶", true,
                            selected.id != loadingPlaylistId, { onPlaylistClick(selected) },
                            Modifier.fillMaxWidth().focusRequester(useFocus)
                                .focusProperties { up = addFocus; down = deleteFocus },
                            onLeft = { requesters[selected.id]?.requestFocus() })
                        ListDetailAction("Excluir lista", "", false, selected.id != loadingPlaylistId,
                            { pendingDelete = selected }, Modifier.fillMaxWidth().focusRequester(deleteFocus)
                                .focusProperties { up = useFocus },
                            onLeft = { requesters[selected.id]?.requestFocus() }, trash = true)
                    }
                }
                if (wide) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        rows(Modifier.weight(.57f).fillMaxHeight())
                        Box(Modifier.width(1.dp).fillMaxHeight().background(ThemeColors.metallicBorder))
                        details(Modifier.weight(.43f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        rows(Modifier.weight(.55f).fillMaxWidth())
                        Box(Modifier.fillMaxWidth().height(1.dp).background(ThemeColors.metallicBorder))
                        details(Modifier.weight(.45f).fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("${playlists.size} " + if (playlists.size == 1) "lista cadastrada" else "listas cadastradas",
                color = Color.White, fontSize = 12.sp)
        }
    }
    pendingDelete?.let { playlist ->
        ConfirmListDeletion(playlist.name, onDismiss = {
            pendingDelete = null
            deleteFocus.requestFocus()
        }, onConfirm = {
            pendingDelete = null
            if (playlist.id != loadingPlaylistId && playlists.any { it.id == playlist.id }) onDeleteClick(playlist)
        })
    }
}

@Composable
private fun ListDetailAction(label: String, symbol: String, primary: Boolean, enabled: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier, onLeft: (() -> Unit)? = null, trash: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    Row(modifier.heightIn(min = 46.dp)
        .border(if (focused) 1.8.dp else 1.dp, ThemeColors.metallicBorder, listShape)
        .background(if (focused || primary) SelectionTheme.color() else ThemeColors.control, listShape)
        .onFocusChanged { focused = it.isFocused }
        .onPreviewKeyEvent { event ->
            if (onLeft != null && event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT) {
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) onLeft()
                true
            } else false
        }
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
        .then(if (!enabled) Modifier.focusable() else Modifier)
        .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        if (trash) ListTrashIcon(Modifier.size(18.dp))
        else Text(symbol, color = Color.White, fontSize = if (symbol == "+") 25.sp else 16.sp)
        Text(label, color = Color.White.copy(alpha = if (enabled) 1f else .6f), fontSize = 14.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ListDocumentIcon(modifier: Modifier) {
    Canvas(modifier) {
        val tint = ThemeColors.metallicBorder
        val stroke = Stroke(1.7.dp.toPx())
        drawRoundRect(tint, cornerRadius = CornerRadius(10.dp.toPx()), style = stroke)
        drawRoundRect(tint, topLeft = Offset(size.width*.29f, size.height*.22f),
            size = Size(size.width*.42f, size.height*.56f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
        for (i in 0..2) drawLine(tint, Offset(size.width*.38f, size.height*(.38f+i*.12f)),
            Offset(size.width*.62f, size.height*(.38f+i*.12f)), 1.7.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun ListTrashIcon(modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        drawLine(Color.White, Offset(size.width*.2f,size.height*.25f), Offset(size.width*.8f,size.height*.25f),stroke)
        drawLine(Color.White, Offset(size.width*.4f,size.height*.12f), Offset(size.width*.6f,size.height*.12f),stroke)
        drawRoundRect(Color.White, topLeft=Offset(size.width*.27f,size.height*.35f), size=Size(size.width*.46f,size.height*.48f),
            cornerRadius=CornerRadius(2.dp.toPx()),style=Stroke(stroke))
    }
}

@Composable
private fun ConfirmListDeletion(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(.9f)
            .border(1.2.dp, ThemeColors.metallicBorder, listShape).background(ThemeColors.panel, listShape).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("EXCLUIR LISTA?", color = ThemeColors.color(0xFFFFE6A0), fontSize = 22.sp)
            Text("Deseja remover a lista \"$name\"? O cadastro e o cache dessa lista serão excluídos do aparelho.",
                color = Color.White, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ListDetailAction("Cancelar", "", false, true, onDismiss, Modifier.weight(1f).focusRequester(cancelFocus))
                ListDetailAction("Excluir", "", false, true, onConfirm, Modifier.weight(1f), trash = true)
            }
        }
        LaunchedEffect(Unit) { withFrameNanos { }; cancelFocus.requestFocus() }
    }
}
