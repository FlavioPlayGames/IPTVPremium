package br.com.flaviotorres.iptvpremium.core.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.withContext

/** The light settings/list panels share the catalogue palette; dark and Kids stay unchanged. */
private object DeviceCardPalette {
    private val p get()=br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette
    fun surface(source:Long)=p.surface(source)
    fun borderColor(source:Long)=p.borderColor(source)
    fun color(source:Long)=p.ink(source)
    fun ink(source:Long)=p.ink(source)
    fun selection()=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.selection
}

@Composable
fun DeviceIdentityCard(compact: Boolean = false, showSyncButton: Boolean = true) {
    val context = LocalContext.current.applicationContext
    val result by produceState<Result<DeviceIdentity>?>(null, context) {
        while (true) {
            value = withContext(Dispatchers.IO) { runCatching { DeviceIdentityStore(context).getOrCreate() } }
            kotlinx.coroutines.delay(5_000)
        }
    }
    val rim = Brush.linearGradient(listOf(DeviceCardPalette.borderColor(0xFF7A5215), DeviceCardPalette.borderColor(0xFFFFE6A0), DeviceCardPalette.borderColor(0xFFD6A32E), DeviceCardPalette.borderColor(0xFFFFF5CD), DeviceCardPalette.borderColor(0xFF926315)))
    Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(DeviceCardPalette.surface(0x50111419), DeviceCardPalette.surface(0x600B0C10))), RoundedCornerShape(10.dp))
        .border(1.dp, rim, RoundedCornerShape(10.dp)).padding(if (compact) 12.dp else 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 10.dp)) {
        Text("IDENTIFICAÇÃO DO APARELHO", color = DeviceCardPalette.color(0xFFFFE6A0), fontSize = if (compact) 11.sp else 14.sp, fontWeight = FontWeight.SemiBold)
        val identity = result?.getOrNull()
        if (identity == null) {
            Text(if (result == null) "Carregando identificação…" else if (br.com.flaviotorres.iptvpremium.BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) "Aguardando conexão com o portal para registrar o MAC…" else "Não foi possível ler a identificação do aparelho.", color = DeviceCardPalette.ink(0xFFF2F2F2), fontSize = 12.sp)
        } else {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 10.dp)) {
                    Text(if (br.com.flaviotorres.iptvpremium.BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) "MAC do aplicativo" else "ID do aparelho · MAC", color = DeviceCardPalette.ink(0xFFF2F2F2), fontSize = 11.sp)
                    Text(identity.displayId, color = DeviceCardPalette.ink(0xFFFFFFFF), fontWeight = FontWeight.SemiBold, fontSize = if (compact) 12.sp else 16.sp)
                    Text("Código: ${identity.code}", color = DeviceCardPalette.color(0xFFFFE6A0), fontSize = if (compact) 16.sp else 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (identity.physicalMac == null) Text("O Android não permitiu ler o MAC físico deste aparelho.", color = DeviceCardPalette.ink(0xFFF2F2F2), fontSize = 11.sp)

        }
        if (br.com.flaviotorres.iptvpremium.BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
            if (showSyncButton) PortalSyncControls()
            if (compact && identity != null) DevicePortalQr()
        }

    }
}

/** Shared action used inside both list registration and subtitle settings. */
@Composable
internal fun PortalSyncControls(label: String = "Sincronizar agora", listSync: Boolean = false, onSynced: () -> Unit = {}) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    val rim = Brush.linearGradient(listOf(DeviceCardPalette.borderColor(0xFF7A5215), DeviceCardPalette.borderColor(0xFFFFE6A0), DeviceCardPalette.borderColor(0xFFD6A32E), DeviceCardPalette.borderColor(0xFFFFF5CD), DeviceCardPalette.borderColor(0xFF926315)))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val synchronize: () -> Unit = {
                if (!syncing) {
                    syncing = true
                    if (listSync) PortalListPreparation.begin()
                    syncMessage = "Conectando à central…"
                    scope.launch {
                        try {
                            val message = withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                                DevicePortalSync(context).sync(force = true, refreshCatalog = listSync)
                            }
                            syncMessage = message
                            onSynced()
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            val message = when (error) {
                                is java.net.SocketTimeoutException -> "A conexão demorou demais. Tente novamente."
                                is java.net.UnknownHostException -> "Sem conexão com a central. Confira a internet."
                                else -> "Não foi possível sincronizar. Confira a internet e tente novamente."
                            }
                            syncMessage = message
                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            if (listSync) PortalListPreparation.finish()
                            syncing = false
                        }
                    }
                }
            }
            PortalOutlineButton(label = if (syncing) "Sincronizando…" else label, enabled = !syncing, onClick = synchronize)
            syncMessage?.let { Text(it, color = DeviceCardPalette.ink(0xFFFFFFFF), fontSize = 14.sp) }
    }
}

/** One visual and interaction contract for portal actions on TV and touch. */
@Composable
internal fun PortalOutlineButton(label: String, enabled: Boolean = true, modifier: Modifier = Modifier.fillMaxWidth(), textColor: Color? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val rim = Brush.linearGradient(listOf(DeviceCardPalette.borderColor(0xFF7A5215), DeviceCardPalette.borderColor(0xFFFFE6A0), DeviceCardPalette.borderColor(0xFFD6A32E), DeviceCardPalette.borderColor(0xFFFFF5CD), DeviceCardPalette.borderColor(0xFF926315)))
    Box(modifier = modifier.height(48.dp)
        .background(if (focused) DeviceCardPalette.selection() else Color.Transparent, shape)
        .border(1.dp, rim, shape)
        .onFocusChanged { focused = it.isFocused }
        .onPreviewKeyEvent { event ->
            if (event.nativeKeyEvent.keyCode in listOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                if (enabled && event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) onClick()
                true
            } else false
        }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .focusable(enabled)
        .padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, color = textColor ?: if (enabled) (if (focused) DeviceCardPalette.ink(0xFFFFFFFF) else DeviceCardPalette.color(0xFFFFE6A0)) else DeviceCardPalette.ink(0xFFF2F2F2), fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

// IPTV_PHYSICAL_MAC_ONLY_V1

// IPTV_PORTAL_PRODUCTION_V1

// IPTV_PORTAL_SYNC_BUTTON_V1

// IPTV_SUBTITLE_LAYOUT_PORTAL_LOAD_V1

// IPTV_ENTRY_ALIGNED_BUTTONS_V1

// IPTV_RENEWAL_AND_DAILY_SYNC_V1

// IPTV_LIST_LOADING_AND_PLAYER_INPUT_V1
