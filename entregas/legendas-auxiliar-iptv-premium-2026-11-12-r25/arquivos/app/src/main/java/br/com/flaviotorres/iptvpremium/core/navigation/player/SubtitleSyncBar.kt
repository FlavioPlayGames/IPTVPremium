package br.com.flaviotorres.iptvpremium.core.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

internal val subtitleSyncBarHeight=48.dp
internal val subtitleSyncBarBottom=12.dp
internal val subtitleSyncCaptionGap=12.dp

/** Four one-line controls; the caller places this strip below the external caption area. */
@Composable
internal fun SubtitleSyncBar(offsetMs:Long,stepMs:Long,onOffset:(Long,String)->Unit,onStep:()->Unit,onAssistant:()->Unit,
    focus:FocusRequester,modifier:Modifier=Modifier) {
    val accent=playerThemeAccent()
    val small=LocalConfiguration.current.screenWidthDp<700
    val font=if(small)12.sp else 14.sp
    val step=java.lang.String.format(java.util.Locale.forLanguageTag("pt-BR"),"%.1f s",stepMs/1000.0)
    Row(modifier.fillMaxWidth(.86f).height(subtitleSyncBarHeight)
        .background(Color(0xE6204668),RoundedCornerShape(12.dp))
        .border(1.dp,Color(0xFF6F9DBD),RoundedCornerShape(12.dp))
        .padding(6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        @Composable fun Button(label:String,description:String,initial:Boolean=false,weight:Float=1f,action:()->Unit) {
            var focused by remember{mutableStateOf(false)}
            val interaction=remember{MutableInteractionSource()}
            Box(Modifier.weight(weight).fillMaxHeight()
                .then(if(initial)Modifier.focusRequester(focus)else Modifier)
                .onFocusChanged{focused=it.isFocused}
                .background(if(focused)accent.copy(alpha=.5f)else Color(0xFF315C7D),RoundedCornerShape(8.dp))
                .border(if(focused)2.dp else 1.dp,if(focused)accent else Color(0xFF8BB4D1),RoundedCornerShape(8.dp))
                .semantics{contentDescription=description}
                .clickable(interactionSource=interaction,indication=null,onClick=action)
                .padding(horizontal=4.dp),contentAlignment=Alignment.Center) {
                Text(label,color=Color.White,fontSize=font,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
        Button("+ Atrasar","+ Atrasar legenda em $step",initial=true){onOffset(offsetMs+stepMs,"CHANGE")}
        Button("- Adiantar","- Adiantar legenda em $step"){onOffset(offsetMs-stepMs,"CHANGE")}
        Button("Ajuste: $step","Intervalo por clique: $step",weight=1.3f,action=onStep)
        Button("Zerar","Zerar ajuste salvo"){onOffset(0L,"RESET")}
        Button("Auxiliar","Auxiliar de sincronização",action=onAssistant)
        Text(externalSubtitleOffsetLabel(offsetMs),color=Color.White,fontSize=font,maxLines=1,modifier=Modifier.widthIn(min=58.dp).padding(horizontal=2.dp))
    }
}
