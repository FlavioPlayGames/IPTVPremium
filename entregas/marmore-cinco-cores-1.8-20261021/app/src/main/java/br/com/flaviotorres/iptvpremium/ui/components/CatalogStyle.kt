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
    BLACK("black", "Preto · Mármore"), WHITE("white", "Branco · Mármore");
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
    val accent get() = if (!regular) ThemeColors.color(0xFFD6A32E) else when (ThemeState.selected) {
        PremiumTheme.GOLD -> if(light) Color(0xFF88631E) else Color(0xFFC7A55F)
        PremiumTheme.BLUE -> if(light) Color(0xFF315DA8) else Color(0xFF7EA2D9)
        PremiumTheme.PURPLE -> if(light) Color(0xFF8050A3) else Color(0xFFB38CCF)
        PremiumTheme.RED -> if(light) Color(0xFFA72E43) else Color(0xFFD77F8B)
        PremiumTheme.PINK -> if(light) Color(0xFFAE3B60) else Color(0xFFD994A6)
    }
    /** Satin metal on the selected outline only; same theme hue, restrained reflection. */
    val selectionBorder get(): Brush {
        val base=accent
        fun mixWhite(amount:Float)=Color(base.red+(1f-base.red)*amount,base.green+(1f-base.green)*amount,base.blue+(1f-base.blue)*amount)
        fun shadow(amount:Float)=Color(base.red*amount,base.green*amount,base.blue*amount)
        return Brush.linearGradient(listOf(shadow(.72f),mixWhite(.40f),base,mixWhite(.25f),shadow(.78f)))
    }
    fun outline(active:Boolean, legacy:Brush):Brush = if(!regular)legacy else
        if(active)selectionBorder else androidx.compose.ui.graphics.SolidColor(edge)
    fun selectionColors(alpha:Float=1f):List<Color> = if(!regular)
        br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.colors(alpha)
        else listOf(accent.copy(alpha=.20f*alpha),accent.copy(alpha=.12f*alpha))
    fun selectionColor(alpha:Float=1f):Color = selectionColors(alpha).first()
    fun selectionBrush(alpha:Float=1f):Brush = Brush.horizontalGradient(selectionColors(alpha))
    fun color(source: Long): Color = if (!regular) ThemeColors.color(source) else {
        val original=Color(source)
        // Dark cut-outs in category glyphs must remain neutral rather than solid accent.
        if(maxOf(original.red,original.green,original.blue)<.3f) panel.copy(alpha=original.alpha)
        else accent.copy(alpha=original.alpha)
    }
    fun borderColor(source: Long): Color {
        if(!regular) return ThemeColors.borderColor(source)
        val reference=Color(source)
        val shade=maxOf(reference.red,reference.green,reference.blue)
        val base=accent
        val factor=.58f+.42f*shade
        val reflection=((shade-.78f)/.22f).coerceIn(0f,1f)*.35f
        return Color(base.red*factor+(1f-base.red*factor)*reflection,
            base.green*factor+(1f-base.green*factor)*reflection,
            base.blue*factor+(1f-base.blue*factor)*reflection,reference.alpha)
    }
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
