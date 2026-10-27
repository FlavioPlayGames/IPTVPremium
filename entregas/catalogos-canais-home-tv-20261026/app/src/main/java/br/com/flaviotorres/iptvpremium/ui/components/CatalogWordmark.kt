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
internal fun CatalogWordmark(compact: Boolean, modifier: Modifier = Modifier, enlarged: Boolean = false) {
    br.com.flaviotorres.iptvpremium.ui.theme.MarbleLogoImage(
        modifier=modifier.width(if(enlarged) { if(compact)102.dp else 122.dp } else if(compact)82.dp else 98.dp)
            .height(if(enlarged) { if(compact)60.dp else 74.dp } else if(compact)52.dp else 62.dp),
        description="IPTV Premium")
}
