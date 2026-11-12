package br.com.flaviotorres.iptvpremium.ui.settings

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
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
import br.com.flaviotorres.iptvpremium.core.player.*
import br.com.flaviotorres.iptvpremium.ui.components.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.*

private object ManualSubtitleCleanup {val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)}

/** Immutable destination: changing it leaves this composition and invalidates the old request. */
@Composable
internal fun ManualSubtitleUploadScreen(target:ManualSubtitleTarget,allowed:suspend ()->Boolean,onBack:()->Unit) {
    val context=LocalContext.current.applicationContext
    val client=remember(context){ManualSubtitleClient(context)}
    val owner=LocalLifecycleOwner.current
    val scope=rememberCoroutineScope()
    val first=remember{FocusRequester()}
    var request by remember(target){mutableStateOf<ManualSubtitleRequest?>(null)}
    var qr by remember(target){mutableStateOf<Bitmap?>(null)}
    var message by remember(target){mutableStateOf("Preparando conexão…")}
    var files by remember(target){mutableStateOf<List<ManualSubtitleFile>>(emptyList())}
    var choices by remember(target){mutableStateOf<Map<Int,Int>>(emptyMap())}
    var assign by remember(target){mutableStateOf<Int?>(null)}
    var busy by remember{mutableStateOf(false)}
    var saved by remember{mutableIntStateOf(0)}
    var confirmed by remember{mutableStateOf(false)}
    var retry by remember{mutableIntStateOf(0)}
    var poll by remember{mutableIntStateOf(0)}
    val latestAllowed by rememberUpdatedState(allowed)
    val back:()->Unit={if(!busy)onBack()}
    BackHandler(onBack=back)
    LaunchedEffect(files.isNotEmpty(),saved>0){withFrameNanos{};runCatching{first.requestFocus()}}
    DisposableEffect(target) {
        onDispose{
            val previous=request
            if(previous!=null&&!confirmed) {
                val count=saved
                ManualSubtitleCleanup.scope.launch{runCatching{if(count>0)client.receipt(previous.id,count)else client.cancel(previous.id)}}
            }
        }
    }
    LaunchedEffect(target,retry) {
        request?.let{runCatching{client.cancel(it.id)}}
        request=null;qr=null;files=emptyList();choices=emptyMap();saved=0;confirmed=false
        try{
            check(latestAllowed()){ "Este conteúdo não está mais disponível nesta lista." }
            val created=client.create(target);request=created
            qr=withContext(Dispatchers.Default){
                val matrix=QRCodeWriter().encode(created.url,BarcodeFormat.QR_CODE,384,384)
                Bitmap.createBitmap(IntArray(384*384){i->if(matrix[i%384,i/384])android.graphics.Color.BLACK else android.graphics.Color.WHITE},384,384,Bitmap.Config.ARGB_8888)
            }
            message="Aguardando envio pelo site."
        }catch(cancelled:CancellationException){throw cancelled}
        catch(error:Exception){message=error.message?:"Não foi possível conectar. Tente novamente."}
    }
    LaunchedEffect(request?.id,poll) {
        val current=request?:return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(isActive&&saved==0&&files.isEmpty()) {
                if(System.currentTimeMillis()>=current.expires){qr=null;message="O pedido expirou. Gere outro acesso.";break}
                try{
                    val status=client.status(current.id)
                    when(status.getString("state")) {
                        "ready"->{
                            val release=br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.acquire()
                            val decoded=try{
                                val download=client.download(current.id)
                                withContext(Dispatchers.Default){ManualSubtitleArchive.read(download.bytes,download.name)}
                            }finally{release()}
                            files=decoded.filter{file->when(target.kind){"movie"->file.season==null
                                else->file.season==null||file.season==target.season}}
                            if(files.isEmpty())error("As legendas identificadas pertencem a outra temporada. Envie outro arquivo.")
                            choices=files.indices.mapNotNull{index->target.validEpisode(files[index])?.let{index to it}}
                                .groupBy{it.second}.values.filter{it.size==1}.flatten().toMap()
                            message="Confira as legendas antes de salvar. Arquivos desnecessários serão descartados."
                            withFrameNanos{};runCatching{first.requestFocus()}
                            break
                        }
                        "cancelled","expired"->{qr=null;message="O pedido terminou. Gere outro acesso.";break}
                        else->message=status.optString("message").takeIf{it.isNotBlank()}?:"Aguardando envio pelo site."
                    }
                }catch(cancelled:CancellationException){throw cancelled}
                catch(error:Exception){message=error.message?:"Falha ao receber o arquivo. Tente novamente.";break}
                delay(5_000L)
            }
        }
    }
    SubtitleSettingsFrame("ENVIO MANUAL",target.title,back) {
        SettingsHeading("Destino do envio",target.description)
        if(saved>0) {
            SettingsCard(Modifier.fillMaxWidth()) {
                Text("$saved legenda(s) salva(s) nesta TV.",color=SettingsLook.text,fontSize=20.sp)
                Text(message,color=SettingsLook.secondary,fontSize=14.sp)
                if(!confirmed)MarbleSettingsButton("Confirmar recebimento no site",enabled=!busy) {
                    scope.launch {try{client.receipt(request!!.id,saved);confirmed=true;message="Recebimento confirmado no site."}
                        catch(cancelled:CancellationException){throw cancelled}
                        catch(_:Exception){message="As legendas estão salvas. A confirmação no site continua pendente."}}
                }
                MarbleSettingsButton("Concluir",modifier=Modifier.focusRequester(first),onClick=back)
            }
        }else if(files.isNotEmpty()) {
            Text(message,color=SettingsLook.secondary,fontSize=14.sp)
            val assigning=assign
            if(assigning!=null) {
                Text("Escolha o episódio para ${files[assigning].name}",color=SettingsLook.text,fontSize=15.sp)
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(target.episodes){_,number->MarbleSettingsButton("Episódio $number") {
                        choices=choices.filterValues{it!=number}+(assigning to number);assign=null
                    }}
                }
                MarbleSettingsButton("Cancelar escolha"){assign=null}
            }else {
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(files){index,file->
                        val number=choices[index]
                        val label=when{number==null->"Selecionar / associar";target.kind=="movie"->"Selecionada";else->"Episódio $number"}
                        MarbleSettingsButton("${if(number!=null)"✓ "else""}${file.name} · $label",enabled=!busy,
                            modifier=if(index==0)Modifier.focusRequester(first)else Modifier) {
                            if(number!=null)choices=choices-index
                            else {
                                val known=target.validEpisode(file)
                                if(known!=null)choices=choices.filterValues{it!=known}+(index to known)
                                else if(target.kind=="season")assign=index
                            }
                        }
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    MarbleSettingsButton(if(busy)"Salvando…"else"Salvar ${choices.size} legenda(s)",enabled=choices.isNotEmpty()&&!busy,modifier=Modifier.weight(1f)) {
                        scope.launch {
                            busy=true
                            try{
                                check(latestAllowed()){ "O acesso ao conteúdo mudou. Inicie outro envio." }
                                val chosen=choices.map{(index,number)->files[index] to number}
                                val count=withContext(NonCancellable+Dispatchers.IO){ManualSubtitleStore(context).save(target,chosen)}
                                saved=count;files=emptyList();choices=emptyMap();busy=false
                                message="Legendas salvas. Confirmando o recebimento no site…"
                                try{client.receipt(request!!.id,count);confirmed=true;message="Recebimento confirmado no site."}
                                catch(cancelled:CancellationException){throw cancelled}
                                catch(_:Exception){message="Legendas salvas. Não foi possível confirmar no site; tente novamente."}
                            }catch(cancelled:CancellationException){throw cancelled}
                            catch(error:Exception){message=error.message?:"Não foi possível salvar."}
                            finally{busy=false}
                        }
                    }
                    MarbleSettingsButton("Enviar outro arquivo",enabled=!busy,modifier=Modifier.weight(1f)) {
                        scope.launch{try{client.receipt(request!!.id,0,false);files=emptyList();choices=emptyMap();poll++}
                            catch(cancelled:CancellationException){throw cancelled}
                            catch(_:Exception){message="Não foi possível liberar outro envio. Gere um novo acesso."}}
                    }
                }
            }
        }else {
            SettingsCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    Box(Modifier.size(150.dp).background(Color.White),contentAlignment=Alignment.Center) {
                        qr?.let{Image(it.asImageBitmap(),"Acesso para enviar legenda a esta TV",Modifier.fillMaxSize())}
                    }
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text("Abra o site pelo celular",color=SettingsLook.text,fontSize=21.sp)
                        Text("O destino já estará preenchido. Anexe SRT ou ZIP de até 8 MB.",color=SettingsLook.secondary,fontSize=14.sp)
                        Text(message,color=SettingsLook.text,fontSize=14.sp)
                    }
                }
            }
            MarbleSettingsButton("Gerar novo acesso",modifier=Modifier.focusRequester(first)){retry++}
            MarbleSettingsButton("Tentar receber novamente",enabled=request!=null){poll++}
        }
    }
}
