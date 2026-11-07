package br.com.flaviotorres.iptvpremium.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.core.player.OpenSubtitleException
import br.com.flaviotorres.iptvpremium.core.player.OpenSubtitleProblem
import br.com.flaviotorres.iptvpremium.core.player.openSubtitlesClient
import br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun OpenSubtitlesAccountPanel(onChanged:()->Unit) {
    val context=LocalContext.current
    val client=remember(context){openSubtitlesClient(context)}
    val scope=rememberCoroutineScope()
    var username by remember{mutableStateOf("")}
    var password by remember{mutableStateOf("")}
    var visible by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)}
    var connected by remember{mutableStateOf(client.connected())}
    var message by remember{mutableStateOf<String?>(null)}
    Text("CONTA OPENSUBTITLES",color=MarbleScreenPalette.ink(0xFFFFFFFF),fontSize=18.sp)
    Text("Entre com sua conta do OpenSubtitles.com. A pesquisa é feita no menu Legendas; o download ocorre somente ao escolher uma opção e está sujeito à cota da sua conta.",
        color=MarbleScreenPalette.ink(0xFFF2F2F2),fontSize=14.sp)
    SettingsAction("Criar conta no OpenSubtitles.com","Abrir o site oficial",enabled=!busy,onClick={
        try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.opensubtitles.com"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}catch(_:Exception){message="Acesse opensubtitles.com no celular ou computador."}
    })
    if(!client.appAvailable) {
        Text(OpenSubtitleProblem.APP_UNAVAILABLE.text,color=MarbleScreenPalette.ink(0xFFFFFFFF),fontSize=14.sp)
        Text("O SubDL continua disponível nesta tela.",color=MarbleScreenPalette.ink(0xFFF2F2F2),fontSize=14.sp)
    }else {
        if(connected) Text("Conta conectada",color=MarbleScreenPalette.ink(0xFFFFFFFF),fontSize=15.sp)
        OpenAccountField("Usuário do OpenSubtitles.com",username,{username=it.take(320)},false,false,!busy)
        OpenAccountField("Senha",password,{password=it.take(1024)},true,visible,!busy)
        SettingsAction(if(visible)"Ocultar senha" else "Mostrar senha","",enabled=!busy,onClick={visible=!visible})
        SettingsAction(if(busy)"Conectando…" else if(connected)"Entrar com outra conta" else "Conectar OpenSubtitles",
            "A senha é usada no login e não fica salva no aparelho.",
            enabled=!busy && username.isNotBlank() && password.isNotEmpty(),onClick={
                if(!busy) {
                    busy=true;message=null
                    val typedUser=username;val typedPassword=password
                    scope.launch {
                        try {
                            client.login(typedUser,typedPassword)
                            connected=client.connected();username=""
                            message="Conta conectada. As legendas serão pesquisadas no menu do player."
                        }catch(cancelled:CancellationException){throw cancelled}
                        catch(error:OpenSubtitleException){message=error.problem.text}
                        catch(_:Exception){message=OpenSubtitleProblem.UNAVAILABLE.text}
                        finally {password="";visible=false;busy=false;onChanged()}
                    }
                }
            })
        if(connected)SettingsAction("Desconectar OpenSubtitles","Mantém o SubDL e os arquivos já baixados.",enabled=!busy,onClick={
            client.disconnect();connected=false;password="";username="";visible=false
            message="Conta desconectada deste aparelho.";onChanged()
        })
    }
    message?.let{Text(it,color=MarbleScreenPalette.ink(0xFFFFFFFF),fontSize=14.sp)}
}

@Composable
private fun OpenAccountField(label:String,value:String,onChange:(String)->Unit,password:Boolean,visible:Boolean,enabled:Boolean) {
    var focused by remember{mutableStateOf(false)}
    val ink=MarbleScreenPalette.ink(0xFFFFFFFF)
    val accent=MarbleScreenPalette.color(0xFFFFE6A0)
    Text(label,color=ink,fontSize=15.sp)
    BasicTextField(value,onChange,singleLine=true,enabled=enabled,
        keyboardOptions=KeyboardOptions(keyboardType=if(password)KeyboardType.Password else KeyboardType.Text),
        visualTransformation=if(password && !visible)PasswordVisualTransformation() else VisualTransformation.None,
        cursorBrush=SolidColor(accent),textStyle=TextStyle(color=ink,fontSize=17.sp),
        modifier=Modifier.fillMaxWidth().onFocusChanged{focused=it.isFocused}
            .background(MarbleScreenPalette.surface(0x600B0C10),RoundedCornerShape(10.dp))
            .border(1.5.dp,if(focused)accent else ink.copy(alpha=.35f),RoundedCornerShape(10.dp))
            .padding(horizontal=15.dp,vertical=12.dp))
}
