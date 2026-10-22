package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/** Drawn over Home, before the catalogue opens. Does not animate/recolour any artwork. */
@Composable
internal fun FirstSectionLoadingDialog(onCancel: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var dots by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        // Avoid a flash when the saved catalogue is already ready.
        delay(120)
        visible = true
        while (true) { delay(420); dots = dots % 3 + 1 }
    }
    if (!visible) return
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.padding(bottom = 24.dp).width(336.dp)
                .background(CatalogStyle.panel.copy(alpha = 0.96f), RoundedCornerShape(14.dp))
                .border(1.dp, CatalogStyle.accent.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val angle by rememberInfiniteTransition(label = "first-section-spinner").animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)), label = "first-section-angle")
                val accent = CatalogStyle.accent
                Canvas(Modifier.size(24.dp)) {
                    val stroke = 2.dp.toPx()
                    drawCircle(accent.copy(alpha = 0.18f), radius = (size.minDimension - stroke) / 2f, style = Stroke(stroke))
                    drawArc(accent, startAngle = angle, sweepAngle = 240f, useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f), size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Carregando seu conteúdo", color = CatalogStyle.text, fontSize = 16.sp)
                    Text(".".repeat(dots), color = CatalogStyle.text, fontSize = 16.sp,
                        modifier = Modifier.width(20.dp).clearAndSetSemantics { })
                }
            }
        }
    }
}
