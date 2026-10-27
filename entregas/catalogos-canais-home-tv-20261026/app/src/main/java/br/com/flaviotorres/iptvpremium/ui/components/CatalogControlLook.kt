package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Alpha is on backgrounds only: never create a translucent group containing text/video. */
internal object CatalogControlLook {
    const val PANEL_OPACITY = .90f
    val panel get() = SolidColor(CatalogStyle.panel.copy(alpha = PANEL_OPACITY))
    val solidPanel get() = SolidColor(CatalogStyle.panel)
    fun fill(focused: Boolean, active: Boolean, translucent: Boolean): Brush {
        val alpha = if (translucent) PANEL_OPACITY else 1f
        return if (focused) Brush.verticalGradient(CatalogStyle.selectionColors().map { it.copy(alpha = alpha) })
        else SolidColor((if (active) lerp(CatalogStyle.categorySurface, CatalogStyle.accent, .10f)
            else CatalogStyle.categorySurface).copy(alpha = alpha))
    }
    fun edge(focused: Boolean, active: Boolean): Brush = if (focused) CatalogStyle.selectionBorder
        else SolidColor(if (active) CatalogStyle.accent else lerp(CatalogStyle.edge, CatalogStyle.secondary, .45f))
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
