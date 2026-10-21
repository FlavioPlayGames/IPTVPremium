package br.com.flaviotorres.iptvpremium.ui.components

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeState

enum class CatalogBackground(val key: String, val label: String) {
    BLACK("black", "Escuro"), WHITE("white", "Claro");
    companion object { fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: BLACK }
}

/** The regular Home and catalogues share the approved marble selection. Kids stays independent. */
object CatalogStyle {
    var selected by mutableStateOf(CatalogBackground.BLACK)
        private set
    fun initialize(context: Context) {
        selected = CatalogBackground.fromKey(context.applicationContext.getSharedPreferences(
            "iptvpremium_settings_v2", Context.MODE_PRIVATE).getString("catalog_background", "black"))
    }
    fun choose(context: Context, value: CatalogBackground) {
        context.applicationContext.getSharedPreferences("iptvpremium_settings_v2", Context.MODE_PRIVATE)
            .edit().putString("catalog_background", value.key).apply()
        selected = value
    }
    val regular get() = !KidsAppearance.active
    val light get() = regular && selected == CatalogBackground.WHITE
    val background get() = if (light) Color(0xFFC1C5C8) else Color(0xFF17191B)
    val panel get() = if (light) Color(0xFFCFD2D4) else Color(0xFF1C1E20)
    val text get() = if (light) Color(0xFF292B30) else Color(0xFFE8E8EA)
    val secondary get() = if (light) Color(0xFF60636C) else Color(0xFFAEB0B7)
    val edge get() = if (light) Color(0xFFA8ADB4) else Color(0xFF323438)
    /** Always-visible category cards, independent of the current focus or active category. */
    val categorySurface get()=if(light)Color(0xFFC9CDD0)else Color(0xFF24272A)
    val categoryEdge get()=if(light)Color(0xFFBAC0C6).copy(alpha=.5f)else Color(0xFF3C4046).copy(alpha=.5f)
    fun categoryColors():List<Color> = if(!regular)listOf(surface(0x50111419),surface(0x600B0C10))
        else listOf(categorySurface.copy(alpha=.60f),categorySurface.copy(alpha=.45f))
    // Deliberately restrained solid colours, shared by logo, icons, focus and Watch.
    val accent get() = if(!regular)ThemeColors.color(0xFFD6A32E)
        else br.com.flaviotorres.iptvpremium.ui.theme.marbleMetalColor(0xFFD6A32E)
    /** Satin metal on the selected outline only; same theme hue, restrained reflection. */
    val selectionBorder get(): Brush = Brush.linearGradient(if(light)listOf(
        borderColor(0xFF694019),borderColor(0xFFB77D25),borderColor(0xFF82501A),
        borderColor(0xFFD6A32E),borderColor(0xFF694019))else listOf(
        borderColor(0xFF95601F),borderColor(0xFFFFE8AA),borderColor(0xFFDFA135),
        borderColor(0xFFFFF3CE),borderColor(0xFFB77D25)))
    fun outline(active:Boolean, legacy:Brush):Brush = if(!regular)legacy else
        if(active)selectionBorder else androidx.compose.ui.graphics.SolidColor(edge)
    fun selectionColors(alpha:Float=1f):List<Color> = if(!regular)
        br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.colors(alpha)
        else listOf(
            androidx.compose.ui.graphics.lerp(panel,accent,if(light).14f else .20f).copy(alpha=.98f*alpha),
            androidx.compose.ui.graphics.lerp(panel,accent,if(light).09f else .14f).copy(alpha=.98f*alpha))
    fun selectionColor(alpha:Float=1f):Color = selectionColors(alpha).first()
    fun selectionBrush(alpha:Float=1f):Brush = Brush.horizontalGradient(selectionColors(alpha))
    /** Small category glyphs use satin graphite on the light panel, independent of theme accents. */
    fun categoryIconBrush(legacy:Brush):Brush=if(light)Brush.linearGradient(listOf(
        Color(0xFF3A414A),Color(0xFF68727E),Color(0xFF353C45)))else legacy
    fun color(source: Long): Color = if (!regular) ThemeColors.color(source) else {
        val original=Color(source)
        // Dark cut-outs in category glyphs must remain neutral rather than solid accent.
        if(maxOf(original.red,original.green,original.blue)<.3f) panel.copy(alpha=original.alpha)
        else accent.copy(alpha=original.alpha)
    }
    fun borderColor(source:Long):Color = if(!regular)ThemeColors.borderColor(source)
        else br.com.flaviotorres.iptvpremium.ui.theme.marbleMetalColor(source)
    fun ink(source: Long): Color {
        val original=Color(source)
        if(!regular) return original
        return (if(minOf(original.red,original.green,original.blue)>.82f) text else secondary).copy(alpha=original.alpha)
    }
    fun surface(source: Long): Color {
        val original=Color(source)
        if(!regular) return original
        return panel.copy(alpha=original.alpha*.91f)
    }
}
