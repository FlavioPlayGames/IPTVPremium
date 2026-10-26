package br.com.flaviotorres.iptvpremium.ui.kids

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.ui.settings.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun KidsSettingsButton(onClick:()->Unit,modifier:Modifier=Modifier) {
    var focused by remember{mutableStateOf(false)}
    Row(modifier.size(46.dp).kidsCinemaToolbarFocus(focused).onFocusChanged{focused=it.isFocused}
        .kidsCinemaClickable(onClick).focusable().semantics{role=Role.Button;contentDescription="Configurações"}.padding(10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.size(24.dp)) {
            val gear=Path().apply {
                for(i in 0 until 32) {
                    val a=i*Math.PI/16
                    val radius=size.minDimension*(if(i%4 in 1..2).48f else .36f)
                    val x=size.width/2+(cos(a)*radius).toFloat();val y=size.height/2+(sin(a)*radius).toFloat()
                    if(i==0)moveTo(x,y)else lineTo(x,y)
                }
                close();addOval(androidx.compose.ui.geometry.Rect(size.width*.35f,size.height*.35f,size.width*.65f,size.height*.65f))
                fillType=PathFillType.EvenOdd
            }
            drawPath(gear,Brush.verticalGradient(listOf(Color(0xFFFFE4A1),Color(0xFFD4A443))))
        }
    }
}

@Composable
internal fun KidsSettingsAccess(required:Boolean,onBack:()->Unit,content:@Composable ()->Unit) {
    if(!required){content();return}
    val context=LocalContext.current.applicationContext
    val enabled by ParentalSession.enabled.collectAsState()
    val epoch by ParentalSession.epoch.collectAsState()
    // A Kids shortcut without a PIN leads to protected PIN creation, not settings.
    if(!enabled) { ParentalPreferencesSettings(onBack);return }
    val store=remember(context){ParentalPinStore(context)}
    val permit=remember(epoch,enabled){KidsSettingsPermit(epoch)}
    var authorized by remember(epoch,enabled){mutableStateOf(false)}
    var pin by remember(epoch,enabled){mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var message by remember(epoch,enabled){mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    fun leave(){permit.cancel();pin="";onBack()}
    if(authorized){content();return}
    BackHandler(onBack=::leave)
    Box(Modifier.fillMaxSize().onPreviewKeyEvent {
        if(it.key==Key.Escape || it.key==Key.Back) { if(it.type==KeyEventType.KeyUp)leave();true }else false
    },contentAlignment=Alignment.Center) {
        KidsNeutralLoadingBackdrop(Modifier.fillMaxSize())
        Column(Modifier.widthIn(max=500.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            KidsCinemaBrand(Modifier.height(106.dp).fillMaxWidth())
            Text("Área dos responsáveis",color=kidsCinemaText,fontWeight=FontWeight.Bold,fontSize=23.sp)
            Text("Digite o PIN do controle parental para abrir as configurações.",color=kidsCinemaText,fontSize=15.sp,
                textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            Column(Modifier.fillMaxWidth().background(br.com.flaviotorres.iptvpremium.ui.components.MarbleScreenPalette.surface(0xF2121419),RoundedCornerShape(16.dp)).padding(16.dp)) {
                PinField("PIN do responsável",pin,{pin=it},!busy,legacyAuth=true)
            }
            message?.let{Text(it,color=Color(0xFF9C382F),fontSize=14.sp)}
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ParentAction("Voltar",true,::leave)
                LaunchedEffect(pin) { if(pin.length==6) {
                    if(!busy) scope.launch {
                        busy=true
                        try {
                            val valid=withContext(Dispatchers.IO){store.enabled() && store.verify(pin)}
                            if(permit.approve(valid,ParentalSession.enabled.value,ParentalSession.epoch.value)) {
                                ParentalSession.unlocked.value=true
                                authorized=true
                            } else message="PIN incorreto ou sessão encerrada. Após cinco erros, aguarde 30 segundos."
                        } catch(cancelled:CancellationException){throw cancelled}
                        catch(_:Exception){message="Não foi possível validar. Se houve cinco erros, aguarde 30 segundos."}
                        finally{pin="";busy=false}
                    }
                
                }}
            }
        }
    }
}

@Composable
private fun ParentAction(label:String,enabled:Boolean,onClick:()->Unit) {
    var focused by remember{mutableStateOf(false)}
    val shape=RoundedCornerShape(12.dp)
    Box(Modifier.height(42.dp).background(if(focused)Color(0xFF8B631E)else Color(0xFFFFF4DF),shape)
        .border(1.dp,Color(0xFFD8B56D),shape).onFocusChanged{focused=it.isFocused}
        .clickable(enabled=enabled,interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick)
        .focusable(enabled).padding(horizontal=18.dp),contentAlignment=Alignment.Center) {
        Text(label,color=(if(focused)Color.White else kidsCinemaText).copy(alpha=if(enabled)1f else .45f),fontWeight=FontWeight.Bold,fontSize=14.sp)
    }
}
