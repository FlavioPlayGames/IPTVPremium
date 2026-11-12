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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
internal fun SubtitleSyncAssistantBar(phase:SubtitleAssistantPhase,onStart:()->Unit,onMark:()->Unit,
    onCancel:()->Unit,focus:FocusRequester,modifier:Modifier=Modifier) {
    val accent=playerThemeAccent()
    Column(modifier.fillMaxWidth(.86f).height(if(phase==SubtitleAssistantPhase.EXPLAIN)112.dp else subtitleSyncBarHeight)
        .background(Color(0xED204668),RoundedCornerShape(12.dp))
        .border(1.dp,Color(0xFF6F9DBD),RoundedCornerShape(12.dp)).padding(6.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)) {
        if(phase==SubtitleAssistantPhase.EXPLAIN)Text(
            "O vídeo voltará ao início. Pressione OK na primeira fala. O cálculo usa o primeiro horário da legenda, sem desconto de reação, e arredonda para o segundo mais próximo. É um auxílio: créditos e versões diferentes podem exigir ajuste manual. Depois, voltaremos ao ponto anterior.",
            color=Color.White,fontSize=13.sp,modifier=Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().height(36.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            @Composable fun Button(label:String,initial:Boolean,action:()->Unit) {
                var selected by remember{mutableStateOf(false)}
                val interaction=remember{MutableInteractionSource()}
                Box(Modifier.weight(1f).fillMaxHeight().then(if(initial)Modifier.focusRequester(focus)else Modifier)
                    .onFocusChanged{selected=it.isFocused}
                    .background(if(selected)accent.copy(alpha=.5f)else Color(0xFF315C7D),RoundedCornerShape(8.dp))
                    .border(if(selected)2.dp else 1.dp,if(selected)accent else Color(0xFF8BB4D1),RoundedCornerShape(8.dp))
                    .clickable(interactionSource=interaction,indication=null,onClick=action),contentAlignment=Alignment.Center) {
                    Text(label,color=Color.White,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                }
            }
            Button(when(phase){SubtitleAssistantPhase.EXPLAIN->"Iniciar";SubtitleAssistantPhase.PREPARING->"Preparando o início…";SubtitleAssistantPhase.LISTENING->"Primeira fala: pressione OK"},true) {
                when(phase){SubtitleAssistantPhase.EXPLAIN->onStart();SubtitleAssistantPhase.LISTENING->onMark();else->Unit}
            }
            Button("Cancelar",false,onCancel)
        }
    }
}
