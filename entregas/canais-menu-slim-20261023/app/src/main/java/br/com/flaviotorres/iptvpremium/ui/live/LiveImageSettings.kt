package br.com.flaviotorres.iptvpremium.ui.live

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.core.player.*
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle

internal fun readLiveVideoLayout(p:SharedPreferences):PlaybackVideoLayout = runCatching {
    PlaybackVideoLayout(PlaybackAspectMode.valueOf(p.getString("live_layout_mode","NORMAL")!!),
        PlaybackManualTransform(p.getFloat("live_zoom",1f),p.getFloat("live_width",1f),p.getFloat("live_height",1f),
            p.getFloat("live_x",0f),p.getFloat("live_y",0f)))
}.getOrDefault(PlaybackVideoLayout())
internal fun saveLiveVideoLayout(p:SharedPreferences,layout:PlaybackVideoLayout) {
    p.edit().putString("live_layout_mode",layout.mode.name).putFloat("live_zoom",layout.manual.zoom)
        .putFloat("live_width",layout.manual.widthScale).putFloat("live_height",layout.manual.heightScale)
        .putFloat("live_x",layout.manual.offsetX).putFloat("live_y",layout.manual.offsetY).apply()
}

/** Uses the same glyph, buttons, labels, increments and limits as the movie player. */
@Composable
internal fun LiveImageSettings(layout:PlaybackVideoLayout,onChange:(PlaybackVideoLayout)->Unit,
    onClose:()->Unit,modifier:Modifier=Modifier) {
    var manual by remember { mutableStateOf(false) }
    val focus=remember(manual){FocusRequester()}
    LaunchedEffect(manual){withFrameNanos{};focus.requestFocus()}
    val accent=CatalogStyle.accent
    Column(modifier.background(Color(0xF215171B),RoundedCornerShape(10.dp))
        .border(1.dp,Color(0xFF52565D),RoundedCornerShape(10.dp)).padding(16.dp)
        .verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(if(manual)"Ajuste manual" else "Formato de tela",color=Color.White,fontSize=18.sp)
        if(!manual) {
            PlayerMenuItem("Normal","Proporção original, sem cortes",accent,focus) {
                onChange(layout.copy(mode=PlaybackAspectMode.NORMAL));onClose()
            }
            PlayerMenuItem("Tela cheia","Preenche a tela; pode cortar as bordas",accent) {
                onChange(layout.copy(mode=PlaybackAspectMode.FULL_SCREEN));onClose()
            }
            PlayerMenuItem("Ajuste manual","Zoom, largura, altura e posição",accent) {
                onChange(layout.copy(mode=PlaybackAspectMode.MANUAL));manual=true
            }
            PlayerMenuItem("Voltar","Voltar ao vídeo",accent,onClick=onClose)
        } else {
            val m=layout.manual
            fun set(value:PlaybackManualTransform){onChange(layout.copy(mode=PlaybackAspectMode.MANUAL,manual=value))}
            PlayerAdjustRow("Zoom",m.zoom,accent,focus,
                {set(m.copy(zoom=(m.zoom-.1f).coerceIn(.25f,4f)))},
                {set(m.copy(zoom=(m.zoom+.1f).coerceIn(.25f,4f)))})
            PlayerAdjustRow("Largura",m.widthScale,accent,onDecrease={set(m.copy(widthScale=(m.widthScale-.1f).coerceIn(.25f,4f)))},
                onIncrease={set(m.copy(widthScale=(m.widthScale+.1f).coerceIn(.25f,4f)))})
            PlayerAdjustRow("Altura",m.heightScale,accent,onDecrease={set(m.copy(heightScale=(m.heightScale-.1f).coerceIn(.25f,4f)))},
                onIncrease={set(m.copy(heightScale=(m.heightScale+.1f).coerceIn(.25f,4f)))})
            PlayerAdjustRow("Horizontal",m.offsetX,accent,onDecrease={set(m.copy(offsetX=(m.offsetX-.05f).coerceIn(-1f,1f)))},
                onIncrease={set(m.copy(offsetX=(m.offsetX+.05f).coerceIn(-1f,1f)))})
            PlayerAdjustRow("Vertical",m.offsetY,accent,onDecrease={set(m.copy(offsetY=(m.offsetY-.05f).coerceIn(-1f,1f)))},
                onIncrease={set(m.copy(offsetY=(m.offsetY+.05f).coerceIn(-1f,1f)))})
            PlayerMenuItem("Restaurar","Voltar ao enquadramento original",accent){onChange(PlaybackVideoLayout());manual=false}
            PlayerMenuItem("Concluído","Voltar ao vídeo",accent,onClick=onClose)
        }
    }
}
