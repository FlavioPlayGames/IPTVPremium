package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.ui.graphics.Color
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors

/** Presentation-only adapter for Lists and settings; leaves Kids colours independent. */
internal object MarbleScreenPalette {
    val background get()=if(CatalogStyle.regular)CatalogStyle.background else ThemeColors.background
    val panel get()=if(CatalogStyle.regular)CatalogStyle.panel else ThemeColors.panel
    val control get()=if(CatalogStyle.regular)CatalogStyle.panel else ThemeColors.control
    val metallicBorder get()=if(CatalogStyle.regular)CatalogStyle.selectionBorder else ThemeColors.metallicBorder
    fun color(source:Long)=CatalogStyle.color(source)
    fun borderColor(source:Long)=CatalogStyle.borderColor(source)
    fun surface(source:Long)=CatalogStyle.surface(source)
    fun glassColor(source:Long)=if(CatalogStyle.regular)CatalogStyle.surface(source)else ThemeColors.glassColor(source)
    fun ink(source:Long)=CatalogStyle.ink(source)
    fun flatSurface(source:Long)=if(CatalogStyle.regular)CatalogStyle.panel.copy(alpha=Color(source).alpha)else Color(source)
}
