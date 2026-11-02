package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Mounted with the playback menu only: no focus target, per-second timer or player interaction. */
@Composable
internal fun PlaybackMenuClock(modifier: Modifier = Modifier, compact: Boolean = false, minimal: Boolean = false) {
    val format = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var time by remember { mutableStateOf(LocalTime.now().format(format)) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            time = LocalTime.now().format(format)
        }
    }
    Text(time, modifier.background(if(minimal)Color.Transparent else Color(0xB3000000), RoundedCornerShape(8.dp))
        .padding(horizontal=12.dp, vertical=7.dp), color=Color.White,
        fontSize=if(compact)18.sp else 22.sp, fontWeight=FontWeight.SemiBold, maxLines=1)
}
