package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val background get() = if (CatalogStyle.light) CatalogStyle.background else Color(0xFF303235)
    private val panelColor get() = if (CatalogStyle.light) CatalogStyle.panel else Color(0xFF282B2E)
    private val controlColor get() = if (CatalogStyle.light) CatalogStyle.categorySurface else Color(0xFF34383C)
    val panel get() = SolidColor(panelColor)
    val solidPanel get() = panel
    @Suppress("UNUSED_PARAMETER")
    fun fill(focused: Boolean, active: Boolean, translucent: Boolean): Brush =
        if (focused) Brush.verticalGradient(listOf(
            lerp(controlColor, CatalogStyle.accent, .30f),
            lerp(panelColor, CatalogStyle.accent, .13f)))
        else SolidColor(if (active) lerp(panelColor, CatalogStyle.accent, .08f) else panelColor)
    fun edge(focused: Boolean, active: Boolean): Brush = if (focused) CatalogStyle.selectionBorder
        else SolidColor(if (active) CatalogStyle.accent else panelColor)
}

internal fun Modifier.catalogControlSurface(focused: Boolean, active: Boolean, shape: Shape,
    translucent: Boolean = false): Modifier = clip(shape)
    .background(CatalogControlLook.fill(focused, active, translucent), shape)
    .border(if (focused) 2.dp else 1.dp, CatalogControlLook.edge(focused, active), shape)

/** A custom rounded focus already exists; suppress the default rectangular indication. */
@Composable
internal fun Modifier.catalogQuietClick(onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() }, indication = null,
    role = Role.Button, onClick = onClick)
