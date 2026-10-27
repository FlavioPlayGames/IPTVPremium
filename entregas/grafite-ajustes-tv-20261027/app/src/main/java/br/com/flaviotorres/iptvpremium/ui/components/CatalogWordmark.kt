package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Flat crowned wordmark: no raster recolouring, shine animation or extra network work. */
@Composable
internal fun CatalogWordmark(compact: Boolean, modifier: Modifier = Modifier, enlarged: Boolean = false, reference: Boolean = false) {
    if (reference) {
        Row(modifier.semantics { contentDescription = "IPTV Premium" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)) {
            val ink = CatalogStyle.text
            Canvas(Modifier.size(if (compact) 27.dp else 34.dp)) {
                val w = size.width; val h = size.height
                val crown = Path().apply {
                    moveTo(.08f*w,.26f*h); lineTo(.28f*w,.46f*h)
                    lineTo(.50f*w,.10f*h); lineTo(.72f*w,.46f*h)
                    lineTo(.92f*w,.26f*h); lineTo(.80f*w,.76f*h)
                    lineTo(.20f*w,.76f*h); close()
                }
                drawPath(crown, ink)
                drawLine(ink, androidx.compose.ui.geometry.Offset(.22f*w,.88f*h),
                    androidx.compose.ui.geometry.Offset(.78f*w,.88f*h), .055f*h)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("IPTV", color = ink, fontFamily = FontFamily.Serif,
                    fontSize = if (compact) 29.sp else 36.sp, letterSpacing = 2.sp)
                Text("PREMIUM", color = CatalogStyle.secondary,
                    fontSize = if (compact) 8.sp else 10.sp, letterSpacing = 3.sp)
            }
        }
        return
    }
    br.com.flaviotorres.iptvpremium.ui.theme.MarbleLogoImage(
        modifier=modifier.width(if(enlarged) { if(compact)102.dp else 122.dp } else if(compact)82.dp else 98.dp)
            .height(if(enlarged) { if(compact)60.dp else 74.dp } else if(compact)52.dp else 62.dp),
        description="IPTV Premium")
}
