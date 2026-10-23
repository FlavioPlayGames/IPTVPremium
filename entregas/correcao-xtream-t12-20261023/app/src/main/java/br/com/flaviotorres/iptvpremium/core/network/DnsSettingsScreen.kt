package br.com.flaviotorres.iptvpremium.core.network

import androidx.compose.runtime.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.ui.components.MarbleSettingsButton
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle

@Composable
internal fun DnsSettingsScreen() {
    var selected by remember{mutableStateOf(AppNetwork.config())}
    var address by remember{mutableStateOf(selected.address)}
    var bootstrap by remember{mutableStateOf(selected.bootstrap)}
    var message by remember{mutableStateOf("")}
    Text("Escolha o DNS usado nas listas, imagens e no player Media3.",color=CatalogStyle.text,fontSize=16.sp)
    for((mode,label)in listOf("system" to "Automático do aparelho","google" to "Google · 8.8.8.8","cloudflare" to "Cloudflare · 1.1.1.1","quad9" to "Quad9 · 9.9.9.9","custom" to "Servidor DNS próprio","doh" to "DNS sobre HTTPS (DoH)")) {
        MarbleSettingsButton((if(selected.mode==mode)"✓  "else"")+label) {
            selected=selected.copy(mode=mode)
            if(mode in listOf("system","google","cloudflare","quad9")){AppNetwork.save(selected);message="DNS salvo para as próximas conexões."}
        }
    }
    if(selected.mode=="custom"||selected.mode=="doh") {
        Text(if(selected.mode=="doh")"URL HTTPS do servidor DoH"else"IP do servidor DNS",color=CatalogStyle.text,fontSize=16.sp)
        BasicTextField(address,{address=it},textStyle=TextStyle(color=CatalogStyle.text,fontSize=18.sp),singleLine=true,
            modifier=Modifier.fillMaxWidth().background(CatalogStyle.background).border(androidx.compose.foundation.BorderStroke(1.dp,CatalogStyle.selectionBorder),androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(12.dp))
        if(selected.mode=="doh") {
            Text("IP de inicialização do DoH (evita depender de um DNS que já falhou)",color=CatalogStyle.text,fontSize=15.sp)
            BasicTextField(bootstrap,{bootstrap=it},textStyle=TextStyle(color=CatalogStyle.text,fontSize=18.sp),singleLine=true,
                modifier=Modifier.fillMaxWidth().border(androidx.compose.foundation.BorderStroke(1.dp,CatalogStyle.selectionBorder),androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).padding(12.dp))
            MarbleSettingsButton("Usar Cloudflare DoH") {address="https://cloudflare-dns.com/dns-query";bootstrap="1.1.1.1,1.0.0.1"}
            MarbleSettingsButton("Usar Google DoH") {address="https://dns.google/dns-query";bootstrap="8.8.8.8,8.8.4.4"}
            MarbleSettingsButton("Usar Quad9 DoH") {address="https://dns.quad9.net/dns-query";bootstrap="9.9.9.9,149.112.112.112"}
        }
        MarbleSettingsButton("Salvar DNS") {
            val value=selected.copy(address=address.trim(),bootstrap=bootstrap.trim())
            try{AppNetwork.save(value);selected=value;message="DNS salvo para as próximas conexões."}
            catch(_:Exception){message="Confira o IP ou a URL HTTPS e o IP de inicialização."}
        }
    }
    if(message.isNotBlank())Text(message,color=CatalogStyle.accent,fontSize=15.sp)
    Text("Após mudar, abra novamente o vídeo ou atualize a lista. VLC usa a resolução de nomes do aparelho. Se o servidor estiver fora do ar, trocar DNS não o recupera.",color=CatalogStyle.text,fontSize=14.sp)
}
