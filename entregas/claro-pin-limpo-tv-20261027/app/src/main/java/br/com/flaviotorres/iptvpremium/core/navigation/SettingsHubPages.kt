package br.com.flaviotorres.iptvpremium.ui.settings

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.BuildConfig
import br.com.flaviotorres.iptvpremium.core.device.*
import br.com.flaviotorres.iptvpremium.core.update.AppUpdateManager
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val pageGold get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.text
@Composable
private fun Detail(value: String) { Text(value, color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFCDCED1), fontSize = 16.sp) }

@Composable
internal fun DevicePreferencesSettings() {
    Detail("Informações do seu dispositivo e aplicativo.")
    DeviceIdentityCard(dashboard=true)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        SettingsCard(Modifier.weight(1f)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                SettingsSymbolIcon(SettingsSymbol.APP)
                Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    SettingsHeading("Aplicativo")
                    Detail("IPTV Premium · Versão ${BuildConfig.VERSION_NAME}")
                }
            }
        }
        SettingsCard(Modifier.weight(1f)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                SettingsSymbolIcon(SettingsSymbol.SYSTEM)
                Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    SettingsHeading("Sistema")
                    Detail("Android ${android.os.Build.VERSION.RELEASE}")
                    Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",color=SettingsLook.secondary,fontSize=12.sp)
                }
            }
        }
    }
}

@Composable
internal fun SubscriptionSettings() {
    val context=LocalContext.current.applicationContext
    val state by produceState<org.json.JSONObject?>(null,context) {
        value=withContext(Dispatchers.IO){runCatching{PortalIdentityStore(context).load()}.getOrNull()}
    }
    Detail("Gerencie sua assinatura e acesse a renovação pelo celular.")
    val summary:@Composable ()->Unit={
        SettingsCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().heightIn(min=190.dp),horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(14.dp,Alignment.CenterVertically)) {
                SettingsSymbolIcon(if(state?.optString("licenseState") in listOf("licensed","trial"))SettingsSymbol.CHECK else SettingsSymbol.DEVICE,Modifier.size(52.dp))
                val data=state
                if(data==null || !data.has("licenseState")) {
                    SettingsHeading("Aguardando informações")
                    Detail("Conecte o aplicativo à central.")
                } else {
                    val label=when(data.optString("licenseState")){"trial"->"Período de teste";"licensed"->"Assinatura ativa";"blocked"->"Bloqueado pelo administrador";else->"Acesso vencido"}
                    SettingsHeading(label)
                    if(data.optLong("validUntil")>0L) {
                        val date=java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM,java.util.Locale.forLanguageTag("pt-BR"))
                        Detail("Válida até ${date.format(java.util.Date(data.optLong("validUntil")))}")
                    }
                }
            }
        }
    }
    val renewal:@Composable ()->Unit={
        SettingsCard(Modifier.fillMaxWidth()) {
            if(BuildConfig.DEVICE_PORTAL_URL.isNotBlank())DevicePortalQr(destination="payment",compact=true,dashboard=true)
            else {SettingsHeading("Renovação");Detail("A central ainda não foi configurada.")}
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if(maxWidth>=620.dp)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(.42f)){summary()};Box(Modifier.weight(.58f)){renewal()}
        } else Column(verticalArrangement=Arrangement.spacedBy(14.dp)){summary();renewal()}
    }
    Text("A assinatura pertence a este aparelho. A renovação é confirmada pelo administrador após o pagamento.",color=SettingsLook.secondary,fontSize=12.sp)
}

@Composable
internal fun PlaybackPreferencesSettings() {
    val settings=rememberSettingsPreferences()
    val selected=settings.prefs.getString("engine","media3")
    Detail("Escolha o player e ajuste suas preferências de reprodução.")
    SettingsCard(Modifier.fillMaxWidth()) {
        SettingsHeading("Player preferido")
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            SettingsRadio("Media3",selected=="media3",Modifier.weight(1f),"Recomendado"){settings.setEngine("media3")}
            SettingsRadio("VLC",selected=="vlc",Modifier.weight(1f),"Alternativo"){settings.setEngine("vlc")}
        }
    }
    val next=settings.prefs.getBoolean("auto_next",true)
    SettingsToggle("Próximo episódio automático","Inicia automaticamente o próximo episódio da lista.",next){settings.setAutoNext(!next)}

}

@Composable
internal fun CategoryPreferencesSettings() {
    val settings=rememberSettingsPreferences()
    Detail("Defina a ordem das categorias em cada seção.")
    val entries=listOf(Triple("LIVE_TV","Canais",SettingsSymbol.LIVE),Triple("MOVIES","Filmes",SettingsSymbol.MOVIE),Triple("SERIES","Séries",SettingsSymbol.SERIES))
    val card:@Composable (String,String,SettingsSymbol)->Unit={type,label,icon->
        SettingsCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                SettingsSymbolIcon(icon);SettingsHeading(label)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsLook.edge.copy(alpha=.5f)))
            val current=settings.categoryOrder(type)
            listOf("default" to "Padrão","az" to "Nome A–Z","za" to "Nome Z–A").forEach{(value,title)->
                SettingsRadio(title,current==value,Modifier.fillMaxWidth()){settings.setCategoryOrder(type,value)}
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if(maxWidth>=660.dp)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            entries.forEach{(type,label,icon)->Box(Modifier.weight(1f)){card(type,label,icon)}}
        } else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){entries.forEach{(type,label,icon)->card(type,label,icon)}}
    }
    Text("A alteração aparece ao abrir novamente a seção. Nenhum conteúdo será excluído.",color=SettingsLook.secondary,fontSize=12.sp)
}

@Composable
internal fun AppearanceSettingsScreen(onBack:()->Unit) {
    val initial=remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { androidx.compose.runtime.withFrameNanos {}; initial.requestFocus() }
    Box(Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.background)) {
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Text("APARÊNCIA",color=pageGold,fontSize=26.sp,modifier=Modifier.weight(1f))
                br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("‹ Voltar",modifier=Modifier.width(108.dp),onClick=onBack)
            }
            AppearancePreferencesSettings(initial,Modifier.weight(1f))
        }
    }
}

@Composable
internal fun AppearancePreferencesSettings(initialFocus: androidx.compose.ui.focus.FocusRequester,modifier:Modifier=Modifier) {
    val settings=rememberSettingsPreferences()
    val context=LocalContext.current.applicationContext
    val selectedTheme=br.com.flaviotorres.iptvpremium.ui.theme.ThemeState.selected
    val controls:@Composable ()->Unit={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Tema",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme.entries.forEach { theme ->
                AppearanceChoice(theme.label,selectedTheme==theme,
                    Modifier.fillMaxWidth().then(if(selectedTheme==theme)Modifier.focusRequester(initialFocus)else Modifier),
                    br.com.flaviotorres.iptvpremium.ui.theme.homeThemeAccent(theme)) {
                    br.com.flaviotorres.iptvpremium.ui.theme.ThemeState.choose(context,theme)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Modo de exibição",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.components.CatalogBackground.entries.forEach { background ->
                AppearanceChoice(background.label,br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selected==background,Modifier.fillMaxWidth()) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.choose(context,background)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Visual do Kids",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.entries.forEach { style ->
                AppearanceChoice(style.label,settings.kidsStyle()==style.key,Modifier.fillMaxWidth(),style.accent) {settings.setKidsStyle(style.key)}
            }
            Text("Tamanho dos textos",color=pageGold,fontSize=18.sp)
            listOf(1f to "Padrão",1.1f to "Maior",1.2f to "Extra grande").forEach { (scale,title) ->
                AppearanceChoice(title,settings.textScale()==scale,Modifier.fillMaxWidth()){settings.setTextScale(scale)}
            }
            Text("Capas por fileira",color=pageGold,fontSize=18.sp)
            listOf(4,5).forEach { columns ->
                AppearanceChoice("$columns por fileira · ${columns*2} capas",settings.catalogColumns()==columns,Modifier.fillMaxWidth()){settings.setCatalogColumns(columns)}
            }
        }
    }
    val previews:@Composable ()->Unit={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Prévia da Home",color=pageGold,fontSize=20.sp)
            br.com.flaviotorres.iptvpremium.ui.home.MarbleHomePreview(Modifier.fillMaxWidth().aspectRatio(16f/9f)
                .clip(RoundedCornerShape(12.dp)).border(1.dp,br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.edge,RoundedCornerShape(12.dp)))
            Detail("As alterações são salvas automaticamente.")
            Text("Kids",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.kids.KidsHomeArtwork(
                br.com.flaviotorres.iptvpremium.ui.kids.KidsStyle.fromKey(settings.kidsStyle()),
                Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(12.dp)),labels=true)
            Text("Grade · ${settings.catalogColumns()} por fileira",color=pageGold,fontSize=18.sp)
            br.com.flaviotorres.iptvpremium.ui.components.CatalogLayoutPreview(settings.catalogColumns())
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if(maxWidth>=660.dp) Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
            Box(Modifier.weight(.30f).fillMaxHeight().verticalScroll(androidx.compose.foundation.rememberScrollState())){controls()}
            Box(Modifier.weight(.70f).fillMaxHeight().verticalScroll(androidx.compose.foundation.rememberScrollState())){previews()}
        } else Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            controls();previews()
        }
    }
}

@Composable
private fun AppearanceChoice(label:String,selected:Boolean,modifier:Modifier=Modifier,swatch:Color?=null,onClick:()->Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape=RoundedCornerShape(10.dp)
    Row(modifier.heightIn(min=46.dp).semantics { this.selected=selected;role=androidx.compose.ui.semantics.Role.Button }
        .onFocusChanged { focused=it.isFocused }.clip(shape)
        .background(if(focused||selected)br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.selection else br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.control)
        .border(if(focused)2.dp else 1.dp,br.com.flaviotorres.iptvpremium.ui.components.SettingsLook.border(focused||selected),shape)
        .clickable(interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        if(swatch!=null)Box(Modifier.size(22.dp).background(swatch,androidx.compose.foundation.shape.CircleShape))
        Text(label,color=pageGold,fontSize=15.sp,modifier=Modifier.weight(1f))
        if(selected)Text("✓",color=pageGold,fontSize=15.sp)
    }
}

@Composable
internal fun UpdateSettings() {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Detail("Mantenha o aplicativo sempre atualizado.")
    Box(Modifier.fillMaxWidth().padding(top=12.dp),contentAlignment=Alignment.TopCenter) {
    SettingsCard(Modifier.widthIn(max=570.dp).fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
            SettingsSymbolIcon(SettingsSymbol.UPDATE,Modifier.size(44.dp))
            Text("Versão instalada",color=SettingsLook.secondary,fontSize=14.sp)
            Text(BuildConfig.VERSION_NAME,color=SettingsLook.text,fontSize=26.sp,fontWeight=FontWeight.SemiBold)
            Text("Número interno ${BuildConfig.VERSION_CODE}",color=SettingsLook.secondary,fontSize=13.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsLook.edge.copy(alpha=.5f)))
            Text("O aplicativo também verifica novas versões ao abrir ou voltar para ele.",color=SettingsLook.secondary,fontSize=13.sp)
        }
    br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton(if (busy) "Verificando…" else "Verificar atualização", enabled = !busy && BuildConfig.DEVICE_PORTAL_URL.isNotBlank()) {
        scope.launch {
            busy = true; message = null
            try {
                AppUpdateManager.refresh()
                message = if (AppUpdateManager.state.value.policy == null) "Seu aplicativo está atualizado conforme a versão publicada na central." else "Atualização disponível. Siga as instruções de instalação."
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Não foi possível consultar a central. Confira sua conexão e tente novamente." }
            finally { busy = false }
        }
    }
    if (BuildConfig.DEVICE_PORTAL_URL.isBlank()) Detail("A central de atualização ainda não foi configurada nesta instalação.")
    message?.let { Detail(it) }
    }
    }
}

@Composable
internal fun HistorySettings() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<PlaybackContentType?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Detail("Apague o histórico de filmes ou séries deste aparelho, em todas as listas. Favoritos, listas e dados de acesso serão preservados.")
    Detail("Essa ação também remove as posições salvas em Continuar assistindo.")
    listOf(PlaybackContentType.MOVIE to "filmes", PlaybackContentType.EPISODE to "séries").forEach { (type, title) ->
        br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Limpar histórico de $title", enabled = !busy) { confirm = type; message = null }
    }
    if (confirm != null) {
        Text("Confirma a exclusão do histórico de ${if (confirm == PlaybackContentType.MOVIE) "filmes" else "séries"}?", color = pageGold, fontSize = 18.sp)
        br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton(if (busy) "Apagando…" else "Confirmar exclusão", enabled = !busy) {
            val type = confirm ?: return@MarbleSettingsButton
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { br.com.flaviotorres.iptvpremium.data.history.WatchHistoryStore(context).clearType(type) }
                    confirm = null; message = "Histórico apagado. Seus favoritos e listas foram preservados."
                } catch (_: Exception) { message = "Não foi possível concluir a limpeza. Tente novamente." }
                finally { busy = false }
            }
        }
        br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton("Cancelar", enabled = !busy) { confirm = null }
    }
    message?.let { Detail(it) }
}
