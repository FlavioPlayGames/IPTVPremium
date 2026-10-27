package br.com.flaviotorres.iptvpremium.ui.live

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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

/** Shared fine controls and the same five-second idle rule as movies and series. */
@Composable
internal fun LiveImageSettings(layout:PlaybackVideoLayout,onChange:(PlaybackVideoLayout)->Unit,
    onClose:()->Unit,modifier:Modifier=Modifier) {
    ScreenFormatPanel(layout,onChange,onClose,modifier)
}

/** Always labelled; solid high-contrast focus, without a glow over the video. */
@Composable
internal fun LiveFormatButton(focusRequester: FocusRequester, onFocus: (Boolean)->Unit,
    onClick:()->Unit, modifier:Modifier=Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape=RoundedCornerShape(7.dp)
    val accent=CatalogStyle.accent
    val foreground=if(focused && accent.luminance()>.45f) Color.Black else Color.White
    Row(modifier.focusRequester(focusRequester)
        .onFocusChanged { focused=it.isFocused;onFocus(it.isFocused) }
        .semantics { contentDescription="Formato de tela. Pressione OK para ajustar a imagem." }
        .background(if(focused)accent else Color(0xF20D1012),shape)
        .border(if(focused)3.dp else 1.dp,if(focused)Color.White else Color(0xFF949AA3),shape)
        .clickable(onClick=onClick).heightIn(min=46.dp).padding(horizontal=14.dp,vertical=10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        PlayerGlyphDrawing(PlayerGlyph.ASPECT,foreground,Modifier.size(22.dp),monochrome=true)
        Text("Formato de tela",color=foreground,fontSize=14.sp,fontWeight=FontWeight.Bold)
        if(focused)Text("OK",color=foreground,fontSize=11.sp,fontWeight=FontWeight.Bold)
    }
}
