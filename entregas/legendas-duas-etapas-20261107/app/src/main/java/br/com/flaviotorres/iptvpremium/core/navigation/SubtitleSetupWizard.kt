package br.com.flaviotorres.iptvpremium.ui.settings

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
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
import br.com.flaviotorres.iptvpremium.core.player.openSubtitlesClient
import br.com.flaviotorres.iptvpremium.ui.components.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Two separate pages: official signup, then device-bound portal connection. */
@Composable
fun SubdlAccountScreen(onBack:()->Unit) {
    var step by rememberSaveable{mutableIntStateOf(1)}
    var editor by rememberSaveable{mutableStateOf<String?>(null)}
    var qrGeneration by remember{mutableIntStateOf(0)}
    var localOptions by rememberSaveable{mutableStateOf(false)}
    val context=LocalContext.current.applicationContext
    val owner=LocalLifecycleOwner.current
    val subdl=remember(context){SubdlCredentialStore(context)}
    val open=remember(context){openSubtitlesClient(context)}
    var subdlReady by remember{mutableStateOf(subdl.isConfigured())}
    var openReady by remember{mutableStateOf(open.connected())}
    var pollMessage by remember{mutableStateOf("")}
    val nextFocus=remember{FocusRequester()}
    val back:()->Unit={if(step==2){step=1;localOptions=false}else onBack()}
    if(editor!=null){SubtitleCredentialsEditor(editor!!){editor=null};return}
    BackHandler(onBack=back)
    LaunchedEffect(step){runCatching{nextFocus.requestFocus()}}
    LaunchedEffect(step,qrGeneration,editor) {
        if(step!=2)return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val started=android.os.SystemClock.elapsedRealtime()
            while(android.os.SystemClock.elapsedRealtime()-started<900_000L) {
                if(BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
                    try {
                        withContext(Dispatchers.IO){DevicePortalSync(context).sync(force=true,includeSubtitleCommands=true)}
                        pollMessage=""
                    }catch(cancelled:CancellationException){throw cancelled}
                    catch(_:Exception){pollMessage="Aguardando conexão. Confira a internet ou gere um novo QR Code."}
                }
                subdlReady=subdl.isConfigured();openReady=open.connected()
                delay(8_000)
            }
            pollMessage="Gere um novo QR Code para continuar a conexão pelo celular."
        }
    }
    Box(Modifier.fillMaxSize()) {
        SettingsBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=28.dp,vertical=if(step==2)14.dp else 18.dp),
            verticalArrangement=Arrangement.spacedBy(if(step==2)8.dp else 14.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("LEGENDAS EXTERNAS",Modifier.weight(1f),color=SettingsLook.text,fontSize=21.sp,fontWeight=FontWeight.Bold)
                MarbleSettingsButton("Voltar",modifier=Modifier.width(108.dp),onClick=back)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsLook.edge))
            Row(Modifier.align(Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                Text("PASSO $step DE 2",color=SettingsLook.secondary,fontSize=12.sp)
                for(i in 1..2)Box(Modifier.size(9.dp).background(if(step==i)CatalogStyle.accent else SettingsLook.edge,CircleShape))
            }
            Text(if(step==1)"Crie sua conta" else "Conecte sua conta à TV",Modifier.align(Alignment.CenterHorizontally),
                color=SettingsLook.text,fontSize=27.sp,fontWeight=FontWeight.SemiBold)
            if(step==1) {
                Text("Se o seu provedor não oferecer legendas, use estas opções gratuitas. Recomendamos cadastrar-se nas duas, pois cada serviço tem limites de uso.",
                    color=SettingsLook.secondary,fontSize=14.sp)
                BoxWithConstraints {
                    if(maxWidth>=680.dp)Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        SignupCard("SubDL","Crie sua conta e gere sua chave de API.","https://subdl.com",Modifier.weight(1f))
                        SignupCard("OpenSubtitles","Crie sua conta. Você usará seu usuário e senha.","https://www.opensubtitles.com",Modifier.weight(1f))
                    }else Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        SignupCard("SubDL","Crie sua conta e gere sua chave de API.","https://subdl.com",Modifier.fillMaxWidth())
                        SignupCard("OpenSubtitles","Crie sua conta. Você usará seu usuário e senha.","https://www.opensubtitles.com",Modifier.fillMaxWidth())
                    }
                }
                MarbleSettingsButton("Já tenho uma conta · Continuar ›",modifier=Modifier.widthIn(max=480.dp).fillMaxWidth()
                    .align(Alignment.CenterHorizontally).focusRequester(nextFocus)){step=2}
            }else {
                Text("Agora use nosso site para enviar os dados à sua TV.",color=SettingsLook.secondary,fontSize=14.sp,
                    modifier=Modifier.align(Alignment.CenterHorizontally))
                Column(Modifier.fillMaxWidth().background(SettingsLook.panel,RoundedCornerShape(18.dp))
                    .border(1.dp,SettingsLook.edge,RoundedCornerShape(18.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    if(BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
                        DevicePortalQr(destination="subtitles",showRefreshButton=false,refreshKey=qrGeneration,wizard=true)
                        if(pollMessage.isNotBlank())Text(pollMessage,color=SettingsLook.secondary,fontSize=13.sp)
                        MarbleSettingsButton("Gerar novo QR Code",modifier=Modifier.widthIn(max=350.dp).fillMaxWidth()){qrGeneration++}
                    }else Text("O acesso pelo celular ainda não está disponível nesta versão. Você pode configurar pela TV abaixo.",color=SettingsLook.secondary,fontSize=14.sp)
                    Text("SubDL: ${if(subdlReady)"configurado" else "não configurado"} · OpenSubtitles: ${if(openReady)"conectado" else "não conectado"}",
                        color=SettingsLook.text,fontSize=13.sp)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    MarbleSettingsButton("Prefere configurar pela TV? ›",modifier=Modifier.weight(1f)){localOptions=!localOptions}
                    MarbleSettingsButton("Concluir",modifier=Modifier.width(170.dp).focusRequester(nextFocus),onClick=onBack)
                }
                if(localOptions)Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    MarbleSettingsButton("Digitar chave SubDL",modifier=Modifier.weight(1f)){editor="subdl"}
                    MarbleSettingsButton("Entrar no OpenSubtitles",modifier=Modifier.weight(1f)){editor="opensubtitles"}
                }
            }
        }
    }
}

@Composable
private fun SignupCard(name:String,description:String,url:String,modifier:Modifier) {
    val context=LocalContext.current
    var bitmap by remember(url){mutableStateOf<Bitmap?>(null)}
    var notice by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(url) {
        bitmap=withContext(Dispatchers.Default) {
            val matrix=QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,256,256)
            Bitmap.createBitmap(IntArray(256*256){i->if(matrix[i%256,i/256])android.graphics.Color.BLACK else android.graphics.Color.WHITE},
                256,256,Bitmap.Config.ARGB_8888)
        }
    }
    Column(modifier.background(SettingsLook.panel,RoundedCornerShape(18.dp)).border(1.dp,SettingsLook.edge,RoundedCornerShape(18.dp))
        .padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(112.dp).background(Color.White),contentAlignment=Alignment.Center) {
                bitmap?.let{Image(it.asImageBitmap(),"QR Code do site oficial do $name",Modifier.fillMaxSize())}
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(name,color=SettingsLook.text,fontSize=21.sp,fontWeight=FontWeight.SemiBold)
                Text("Site oficial do $name",color=SettingsLook.secondary,fontSize=12.sp)
                Text(description,color=SettingsLook.text,fontSize=14.sp)
            }
        }
        MarbleSettingsButton("Acessar site do $name") {
            try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
            catch(_:Exception){notice="Escaneie o QR Code pelo celular para acessar o site oficial."}
        }
        notice?.let{Text(it,color=SettingsLook.secondary,fontSize=12.sp)}
    }
}
