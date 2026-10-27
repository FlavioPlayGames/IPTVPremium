package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp

internal object SettingsLook {
    val background get()=CatalogControlLook.background
    val panel get()=if(CatalogStyle.light)Color(0xFFC3C6CA)else Color(0xFF35383C)
    val control get()=if(CatalogStyle.light)Color(0xFFB9BEC4)else Color(0xFF393D42)
    val edge get()=if(CatalogStyle.light)Color(0xFF929AA4)else Color(0xFF535A62)
    val selection get()=lerp(control,CatalogStyle.accent,if(CatalogStyle.light).10f else .14f)
    val text get()=CatalogStyle.text
    val secondary get()=CatalogStyle.secondary
    fun border(focused:Boolean)=SolidColor(if(focused)CatalogStyle.accent else edge)
}
@Composable
internal fun SettingsBackdrop(modifier:Modifier=Modifier) {
    CatalogThemeBackdrop(modifier)
}
