package br.com.flaviotorres.iptvpremium.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.BuildConfig
import br.com.flaviotorres.iptvpremium.core.device.DevicePortalQr
import br.com.flaviotorres.iptvpremium.core.device.DevicePortalSync
import br.com.flaviotorres.iptvpremium.core.player.SubdlCredentialStore
import br.com.flaviotorres.iptvpremium.core.player.SubsourceCredentialStore
import br.com.flaviotorres.iptvpremium.core.player.openSubtitlesClient
import br.com.flaviotorres.iptvpremium.ui.components.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.*

private data class SubtitleProviderChoice(val key:String,val title:String,val description:String,val url:String)
private val subtitleProviders=listOf(
    SubtitleProviderChoice("subdl","SubDL","Use sua chave de API pessoal.","https://subdl.com"),
    SubtitleProviderChoice("opensubtitles","OpenSubtitles","Entre com sua conta do serviço.","https://www.opensubtitles.com"),
    SubtitleProviderChoice("subsource","SubSource","Use sua chave de API pessoal.","https://subsource.net")
)

/** No registration QR or device link exists until the corresponding action is chosen. */
@Composable
internal fun SubtitleAccountsScreen(onBack:()->Unit) {
    var providerKey by rememberSaveable{mutableStateOf<String?>(null)}
    var action by rememberSaveable{mutableStateOf<String?>(null)}
    var lastProvider by rememberSaveable{mutableIntStateOf(0)}
    var qrGeneration by remember{mutableIntStateOf(0)}
    val context=LocalContext.current.applicationContext
    val owner=LocalLifecycleOwner.current
    val subdl=remember(context){SubdlCredentialStore(context)}
    val subsource=remember(context){SubsourceCredentialStore(context)}
    val open=remember(context){openSubtitlesClient(context)}
    var statuses by remember{mutableStateOf(mapOf<String,Boolean>())}
    var message by remember{mutableStateOf("")}
    val first=remember(providerKey,action){FocusRequester()}
    val back:()->Unit={when{action!=null->action=null;providerKey!=null->providerKey=null;else->onBack()}}
    BackHandler(onBack=back)
    LaunchedEffect(providerKey,action) {
        statuses=withContext(Dispatchers.IO){mapOf("subdl" to subdl.isConfigured(),"opensubtitles" to open.connected(),"subsource" to subsource.isConfigured())}
        if(action!="local"){withFrameNanos{};runCatching{first.requestFocus()}}
    }
    LaunchedEffect(action,qrGeneration) {
        if(action!="portal"||BuildConfig.DEVICE_PORTAL_URL.isBlank())return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val started=android.os.SystemClock.elapsedRealtime()
            while(android.os.SystemClock.elapsedRealtime()-started<900_000L) {
                try {
                    withContext(Dispatchers.IO){DevicePortalSync(context).sync(force=true,includeSubtitleCommands=true)}
                    statuses=withContext(Dispatchers.IO){mapOf("subdl" to subdl.isConfigured(),"opensubtitles" to open.connected(),"subsource" to subsource.isConfigured())}
                    message=""
                }catch(cancelled:CancellationException){throw cancelled}
                catch(_:Exception){message="Aguardando conexão. Confira a internet ou gere outro QR Code."}
                delay(8_000L)
            }
            message="Gere outro QR Code para continuar."
        }
    }
    val provider=subtitleProviders.firstOrNull{it.key==providerKey}
    if(provider!=null&&action=="local") {SubtitleCredentialsEditor(provider.key){action=null};return}
    SubtitleSettingsFrame(provider?.title?.uppercase()?:"CONTAS DE LEGENDAS",
        if(provider==null)"Escolha o serviço que deseja configurar."else "${provider.description} Cada serviço possui seus próprios limites.",back) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            if(provider==null) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide=maxWidth>=660.dp
                    val cell:@Composable (Int,Modifier)->Unit={index,modifier->
                        val p=subtitleProviders[index]
                        SubtitleLandingCard(p.title,if(statuses[p.key]==true)"Conectado nesta TV"else"Não conectado",
                            "Gerenciar conexão",SubtitleSettingsIcon.ACCOUNTS,
                            modifier.height(190.dp).then(if(index==lastProvider)Modifier.focusRequester(first)else Modifier),
                            onClick={lastProvider=index;providerKey=p.key})
                    }
                    if(wide)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        subtitleProviders.indices.forEach{cell(it,Modifier.weight(1f))}
                    }else Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        subtitleProviders.indices.forEach{cell(it,Modifier.fillMaxWidth())}
                    }
                }
            }else when(action) {
                "signup" -> {
                    SettingsCard(Modifier.fillMaxWidth()) {
                        SubtitleSignupQr(provider.url,provider.title)
                        Text(if(provider.key=="opensubtitles")"Crie sua conta no site oficial. Depois, volte e escolha conectar."
                            else "Crie sua conta no site oficial e gere a chave pessoal de API. Depois, volte e escolha conectar.",color=SettingsLook.text,fontSize=15.sp)
                        SubtitleActionButton("Acessar site oficial",modifier=Modifier.focusRequester(first)) {
                            try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(provider.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
                            catch(_:Exception){message="Use a câmera do celular para abrir o site oficial."}
                        }
                    }
                }
                "portal" -> {
                    SettingsCard(Modifier.fillMaxWidth()) {
                        DevicePortalQr(destination="subtitles",showRefreshButton=false,refreshKey=qrGeneration,wizard=true)
                        Text("No site, selecione ${provider.title} e envie os dados para esta TV.",color=SettingsLook.text,fontSize=15.sp)
                        Text(if(statuses[provider.key]==true)"${provider.title}: conectado nesta TV"else "Aguardando conexão de ${provider.title}…",color=SettingsLook.secondary,fontSize=14.sp)
                        SubtitleActionButton("Gerar novo QR Code",modifier=Modifier.focusRequester(first)){qrGeneration++}
                    }
                }
                else -> {
                    SettingsCard(Modifier.fillMaxWidth()) {
                        Text(if(statuses[provider.key]==true)"Conta conectada nesta TV"else "Ainda não conectado nesta TV",color=SettingsLook.text,fontSize=17.sp)
                        SubtitleActionButton("Criar conta no site oficial",modifier=Modifier.focusRequester(first)){message="";action="signup"}
                        if(provider.key!="subsource"&&BuildConfig.DEVICE_PORTAL_URL.isNotBlank())
                            SubtitleActionButton("Conectar pelo celular ou PC"){message="";action="portal"}
                        SubtitleActionButton(if(provider.key=="opensubtitles")"Entrar pela TV"else"Digitar chave na TV"){message="";action="local"}
                    }
                }
            }
            if(message.isNotBlank())Text(message,color=SettingsLook.secondary,fontSize=13.sp)
        }
    }
}

@Composable
private fun SubtitleSignupQr(url:String,title:String) {
    var bitmap by remember(url){mutableStateOf<Bitmap?>(null)}
    LaunchedEffect(url){bitmap=withContext(Dispatchers.Default){
        val matrix=QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,256,256)
        Bitmap.createBitmap(IntArray(256*256){i->if(matrix[i%256,i/256])android.graphics.Color.BLACK else android.graphics.Color.WHITE},256,256,Bitmap.Config.ARGB_8888)
    }}
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(140.dp).background(Color.White),contentAlignment=Alignment.Center){bitmap?.let{Image(it.asImageBitmap(),"Site oficial do $title",Modifier.fillMaxSize())}}
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Site oficial do $title",color=SettingsLook.text,fontSize=20.sp)
            Text(url,color=SettingsLook.secondary,fontSize=14.sp)
        }
    }
}
