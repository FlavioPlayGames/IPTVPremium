package br.com.flaviotorres.iptvpremium.core.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.BuildConfig
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors
import br.com.flaviotorres.iptvpremium.ui.theme.themeBackdropPainter
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PortalTrialOverlay() {
    val current by PortalTrialState.snapshot.collectAsState()
    val access by PortalAccess.message.collectAsState()
    val snapshot = current ?: return
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var activation by rememberSaveable(snapshot.mac, snapshot.expiresAt) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // A visible foreground app also reaches the deadline without waiting for daily synchronization.
    LaunchedEffect(snapshot.mac, snapshot.expiresAt, snapshot.state) {
        while (snapshot.state == "trial") {
            if (snapshot.expired()) { PortalTrialState.expireIfDue(snapshot); break }
            delay((snapshot.expiresAt - snapshot.now()).coerceIn(250L, 30_000L))
        }
    }
    if (access != null || snapshot.state != "trial") return
    if (!activation && !snapshot.welcome()) return
    val choose: (String) -> Unit = { choice ->
        if (!saving) {
            saving = true
            error = null
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { TrialAcknowledgement.mark(context, snapshot, choice) }
                    activation = choice == "now"
                    PortalTrialState.acknowledge(snapshot)
                    if (choice == "due") Toast.makeText(context,
                        "Combinado. Avisaremos quando seu período de experiência terminar.", Toast.LENGTH_LONG).show()
                    launch(Dispatchers.IO) { DevicePortalSync(context).sendPendingTrialChoice() }
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Não foi possível salvar sua escolha. Tente novamente." }
                finally { saving = false }
            }
        }
    }
    ActivationDialogFrame(onDismiss = {
        if (!saving) { if (activation) activation = false else choose("due") }
    }, dismissEnabled = !saving) {
        if (activation) ActivationInstructions(snapshot, onBack = { activation = false })
        else {
            ActivationLogo()
            Text("Bem-vindo ao IPTV Premium", color = ThemeColors.color(0xFFFFE6A0),
                fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Seu aplicativo está liberado para um período de experiência de 7 dias.", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Você pode ativar agora ou decidir ao final desse período.", color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            val date = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
                .format(Date(snapshot.expiresAt))
            Text("Disponível até:\n$date", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(ThemeColors.control.copy(alpha = .6f), RoundedCornerShape(10.dp)).padding(14.dp))
            val focus = remember { FocusRequester() }
            PortalOutlineButton(textColor = Color.White, label = if (saving) "Registrando…" else "Ativar agora", enabled = !saving,
                modifier = Modifier.fillMaxWidth().focusRequester(focus), onClick = { choose("now") })
            PortalOutlineButton(textColor = Color.White, label = "Decidir no vencimento", enabled = !saving, onClick = { choose("due") })
            error?.let { Text(it, color = Color.White, fontSize = 14.sp) }
            LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
        }
    }
}

@Composable
internal fun TrialExpiredAccess() {
    val current by PortalTrialState.snapshot.collectAsState()
    val snapshot = current ?: return
    var activation by rememberSaveable(snapshot.mac, snapshot.expiresAt) { mutableStateOf(false) }
    ActivationDialogFrame(onDismiss = { if (activation) activation = false }, dismissEnabled = activation) {
        if (activation) ActivationInstructions(snapshot, onBack = { activation = false })
        else {
            ActivationLogo()
            Text("Seu período de experiência terminou", color = ThemeColors.color(0xFFFFE6A0), fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold)
            Text("Esperamos que tenha aproveitado o IPTV Premium.", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Para continuar usando o aplicativo, faça a ativação pela nossa central.", color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            val focus = remember { FocusRequester() }
            PortalOutlineButton(textColor = Color.White, label = "Ativar aplicativo", modifier = Modifier.fillMaxWidth().focusRequester(focus),
                onClick = { activation = true })
            LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
        }
    }
}

@Composable
private fun ActivationDialogFrame(onDismiss: () -> Unit, dismissEnabled: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false,
        dismissOnClickOutside = false, dismissOnBackPress = dismissEnabled)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(ThemeColors.background), contentAlignment = Alignment.Center) {
            br.com.flaviotorres.iptvpremium.ui.components.CatalogThemeBackdrop(Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .18f)))
            Column(Modifier.widthIn(max = 620.dp).fillMaxWidth(.92f)
                .heightIn(max = (maxHeight - 24.dp).coerceAtLeast(120.dp))
                .border(1.2.dp, ThemeColors.metallicBorder, RoundedCornerShape(18.dp))
                .background(ThemeColors.panel.copy(alpha = .60f), RoundedCornerShape(18.dp))
                .verticalScroll(rememberScrollState()).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    }
}

@Composable
private fun ActivationLogo() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Image(themePainterResource(R.drawable.iptv_premium_logo), "IPTV Premium",
            Modifier.size(180.dp, 94.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun ActivationInstructions(snapshot: TrialSnapshot, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var opening by remember { mutableStateOf(false) }
    ActivationLogo()
    Text("ATIVAÇÃO DO APLICATIVO", color = ThemeColors.color(0xFFFFE6A0), fontSize = 23.sp,
        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text("Escaneie o QR Code ou acesse o site abaixo.", color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    val qrContent: @Composable ColumnScope.() -> Unit = {
        DevicePortalQr(destination = "payment", compact = true, stacked = true)
        Text(BuildConfig.DEVICE_PORTAL_URL.removePrefix("https://").removeSuffix("/"), color = Color.White,
            fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Se estiver no celular, toque em Abrir site.", color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    val actions: @Composable ColumnScope.() -> Unit = {
        ActivationIdentityRow("MAC do aparelho", snapshot.mac)
        ActivationIdentityRow("Código de acesso", snapshot.code)
    val focus = remember { FocusRequester() }
    PortalOutlineButton(textColor = Color.White, label = if (opening) "Abrindo…" else "Abrir site", enabled = !opening,
        modifier = Modifier.fillMaxWidth().focusRequester(focus), onClick = {
            if (!opening) {
                opening = true
                scope.launch {
                    try {
                        // A fresh single-use link avoids typing on mobile; no permanent credential in the URL.
                        val link = withContext(Dispatchers.IO) {
                            try { DevicePortalSync(context).createQrLogin("payment").first }
                            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { BuildConfig.DEVICE_PORTAL_URL }
                        }
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { Toast.makeText(context, "Abra o endereço acima no navegador do celular.", Toast.LENGTH_LONG).show() }
                    finally { opening = false }
                }
            }
        })
    PortalOutlineButton(textColor = Color.White, label = "Voltar", onClick = onBack)
    LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        qrContent()
        actions()
    }

}

@Composable
private fun ActivationIdentityRow(label: String, value: String) {
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 420.dp
        Row(Modifier.fillMaxWidth().background(ThemeColors.control.copy(alpha = .55f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                if (compact) {
                    Text(label, color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                    Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$label:", color = Color.White.copy(alpha = .75f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.25f))
                }
            }
            PortalOutlineButton(textColor = Color.White, label = "Copiar", modifier = Modifier.width(90.dp), onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "$label copiado.", Toast.LENGTH_SHORT).show()
            })
        }
    }
}
