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
    val background get() = if (light) Color(0xFFF4F4F5) else Color(0xFF17191B)
    val panel get() = if (light) Color(0xFFFAFAFB) else Color(0xFF1C1E20)
    val text get() = if (light) Color(0xFF292B30) else Color(0xFFE8E8EA)
    val secondary get() = if (light) Color(0xFF60636C) else Color(0xFFAEB0B7)
    val edge get() = if (light) Color(0xFFE2E2E6) else Color(0xFF323438)
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
            androidx.compose.ui.graphics.lerp(panel,accent,if(light).27f else .36f).copy(alpha=.98f*alpha),
            androidx.compose.ui.graphics.lerp(panel,accent,if(light).18f else .25f).copy(alpha=.98f*alpha))
    fun selectionColor(alpha:Float=1f):Color = selectionColors(alpha).first()
    fun selectionBrush(alpha:Float=1f):Brush = Brush.horizontalGradient(selectionColors(alpha))
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
