package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Opaque graphite catalogue surfaces; neutral base matches the dark Home. */
internal object CatalogControlLook {
    const val PANEL_OPACITY = 1f
    val background get() = if (CatalogStyle.light) Color(0xFFD0D1D3) else Color(0xFF303235)
    val panelColor get() = background
    private val controlColor get() = if (CatalogStyle.light) Color(0xFFB9BDC2) else Color(0xFF34383C)
    val divider get() = if (CatalogStyle.light) Color(0xFFA0A6AD) else Color(0xFF454B51)
    val controlEdge get() = if (CatalogStyle.light) Color(0xFF949CA6) else Color(0xFF4B535A)
    val panel get() = SolidColor(panelColor)
    val solidPanel get() = panel
    /** Static opaque colour stops produce the satin reflection without blur or animated layers. */
    fun fill(focused: Boolean, active: Boolean, translucent: Boolean): Brush =
        if (focused || active) {
            Brush.verticalGradient(
                0f to Color(0xFF747A80),
                .14f to Color(0xFF555C63),
                .48f to Color(0xFF363C43),
                1f to Color(0xFF252A30))
        } else SolidColor(if (translucent) controlColor else panelColor)
    fun foreground(selected: Boolean): Color = if (selected) Color.White else CatalogStyle.text
    fun edge(focused: Boolean, active: Boolean): Brush = if (focused) CatalogStyle.selectionBorder
        else SolidColor(if (active) CatalogStyle.accent else panelColor)
}

internal fun Modifier.catalogControlSurface(focused: Boolean, active: Boolean, shape: Shape,
    translucent: Boolean = false): Modifier {
    val outline = if (translucent) RoundedCornerShape(50) else shape
    return clip(outline)
        .background(CatalogControlLook.fill(focused, active, translucent), outline)
        .border(if (focused) 1.5.dp else 1.dp,
            if (!focused && !active && translucent) SolidColor(CatalogControlLook.controlEdge)
            else CatalogControlLook.edge(focused, active), outline)
}

/** A custom rounded focus already exists; suppress the default rectangular indication. */
@Composable
internal fun Modifier.catalogQuietClick(onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() }, indication = null,
    role = Role.Button, onClick = onClick)
