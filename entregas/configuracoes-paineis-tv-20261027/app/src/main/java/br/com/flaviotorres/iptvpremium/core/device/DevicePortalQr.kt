package br.com.flaviotorres.iptvpremium.core.device

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Short-lived bearer link, never the installation credential or the six-digit code. */
@Composable
internal fun DevicePortalQr(destination: String = "lists", compact: Boolean = false, showRefreshButton: Boolean = true, refreshKey: Int = 0, heading: String? = null, stacked: Boolean = false, dashboard:Boolean=false) {
    val context = LocalContext.current.applicationContext
    var generation by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("Preparando acesso…") }
    LaunchedEffect(generation, destination, refreshKey) {
        bitmap = null
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val (url, expires) = DevicePortalSync(context).createQrLogin(destination)
                val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 512, 512)
                val pixels = IntArray(512 * 512) { i -> if (matrix[i % 512, i / 512]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
                val image = Bitmap.createBitmap(pixels, 512, 512, Bitmap.Config.ARGB_8888)
                image to expires
            }
        }
        loading = false
        result.onFailure { message = "Não foi possível gerar o QR. Sincronize e tente novamente." }
        val data = result.getOrNull() ?: return@LaunchedEffect
        bitmap = data.first
        message = "Aponte a câmera do celular para entrar no site. Acesso único, válido por 15 minutos."
        delay((data.second - System.currentTimeMillis()).coerceIn(0L, 900_000L))
        bitmap = null
        message = "QR Code expirado. Gere outro para entrar."
    }
    if(dashboard) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(112.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
                bitmap?.let { image->Image(image.asImageBitmap(),contentDescription="QR Code para renovar a assinatura",modifier=Modifier.fillMaxSize().background(Color.White)) }
                if(bitmap==null)br.com.flaviotorres.iptvpremium.ui.components.SettingsSymbolIcon(br.com.flaviotorres.iptvpremium.ui.components.SettingsSymbol.UPDATE,Modifier.size(40.dp))
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Renovar pelo celular",color=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.text,fontSize=17.sp)
                Text(if(loading)"Preparando acesso…" else message,color=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.secondary,fontSize=12.sp)
                if(showRefreshButton)br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton(if(loading)"Gerando…" else "Gerar novo QR Code",enabled=!loading,modifier=Modifier.widthIn(max=240.dp).fillMaxWidth()){generation++}
                Text("Não compartilhe este acesso.",color=br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.secondary,fontSize=11.sp)
            }
        }
        return
    }
    if (stacked) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            bitmap?.let { image -> Image(image.asImageBitmap(), contentDescription = "QR Code para ativação do aplicativo",
                modifier = Modifier.size(156.dp).background(Color.White)) }
            Text(if (loading) "Preparando QR Code…" else message, color = Color.White, fontSize = 12.sp)
            if (!loading && bitmap == null && showRefreshButton)
                PortalOutlineButton("Gerar novo QR Code", onClick = { generation++ })
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = androidx.compose.ui.Alignment.Top) {
        bitmap?.let { image ->
            Image(image.asImageBitmap(), contentDescription = "QR Code de acesso à conta deste aparelho", modifier = Modifier.size(if (compact) 128.dp else 156.dp).background(Color.White))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(heading ?: when (destination) { "subtitles" -> "LEGENDAS PELO CELULAR"; "payment" -> "RENOVAR PELO CELULAR"; else -> "CADASTRAR PELO CELULAR" }, color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE6A0), fontSize = 14.sp)
            Text(message, color = Color(0xFFF2F2F2), fontSize = 13.sp)
            if (showRefreshButton) PortalOutlineButton(label = if (loading) "Gerando…" else "Gerar novo QR Code", enabled = !loading, onClick = { generation++ })
            Text("Não compartilhe: este QR dá acesso à conta do aparelho.", color = Color(0xFFF2F2F2), fontSize = 11.sp)
        }
    }
}

// IPTV_SUBTITLE_LAYOUT_PORTAL_LOAD_V1

// IPTV_QR_15_MINUTES_V1

// IPTV_PLAYLIST_ENTRY_BRAND_V1

// IPTV_ENTRY_ALIGNED_BUTTONS_V1

// IPTV_RENEWAL_AND_DAILY_SYNC_V1
