package br.com.flaviotorres.iptvpremium.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.util.concurrent.ConcurrentHashMap

/** Paletas da interface; as imagens de cada tema sao arquivos proprios. */
enum class PremiumTheme(val key: String, val label: String, val hue: Float) {
    GOLD("gold", "Amarelo", 45f),
    BLUE("blue", "Azul", 223.8f),
    PINK("pink", "Rosa", 345.45f),
    PURPLE("purple", "Roxo", 283.34f),
    RED("red", "Vermelho", 356.92f);
    companion object {
        // Supported aliases for existing device tests; these are not enum entries or menu choices.
        val EMERALD: PremiumTheme get() = GOLD
        val CYAN: PremiumTheme get() = GOLD
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: GOLD
    }
}

object ThemeState {
    private var mainSelected by mutableStateOf(PremiumTheme.GOLD)
    private var kidsPalette by mutableStateOf<PremiumTheme?>(null)
    val selected: PremiumTheme get() = kidsPalette ?: mainSelected
    internal fun useKidsPalette(theme: PremiumTheme?) { kidsPalette = theme }
    fun initialize(context: Context) {
        kidsPalette = null
        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.initialize(context)
        val prefs=context.applicationContext.getSharedPreferences("iptvpremium_settings_v2", Context.MODE_PRIVATE)
        val stored=prefs.getString("visual_theme", "gold")
        mainSelected = PremiumTheme.fromKey(stored)
        if(stored=="emerald" || stored=="cyan") prefs.edit().putString("visual_theme", "gold").apply()
    }
    fun choose(context: Context, theme: PremiumTheme) {
        context.applicationContext.getSharedPreferences("iptvpremium_settings_v2", Context.MODE_PRIVATE)
            .edit().putString("visual_theme", theme.key).apply()
        mainSelected = theme
    }
}

/** Cores de bordas/controles, sem qualquer filtro nos bitmaps, capas ou video. */
object ThemeColors {
    private val cache = ConcurrentHashMap<Pair<PremiumTheme, Long>, Color>()
    fun color(goldArgb: Long): Color {
        val theme = ThemeState.selected
        if (theme == PremiumTheme.GOLD) return Color(goldArgb)
        return cache.getOrPut(theme to goldArgb) { Color(accentArgb(goldArgb, theme)) }
    }
    /** Existing tinted player-glass shading; independent of metallic rim brightness. */
    fun glassColor(goldArgb: Long): Color {
        val color = color(goldArgb)
        val factor = when (ThemeState.selected) {
            PremiumTheme.GOLD -> 1f
            PremiumTheme.BLUE -> .94f
            else -> 1.10f
        }
        return color.copy(red = (color.red * factor).coerceAtMost(1f),
            green = (color.green * factor).coerceAtMost(1f), blue = (color.blue * factor).coerceAtMost(1f))
    }
    private val rims = ConcurrentHashMap<Pair<PremiumTheme, Long>, Color>()
    /** Brighter colored metal: preserve hue and a saturation floor instead of mixing white. */
    fun borderColor(goldArgb: Long): Color {
        val theme = ThemeState.selected
        if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) return interfaceThemeAccent(theme).copy(alpha=Color(goldArgb).alpha)
        val reference = Color(goldArgb)
        if (theme == PremiumTheme.GOLD) return reference
        return rims.getOrPut(theme to goldArgb) {
            val high = maxOf(reference.red, reference.green, reference.blue)
            val low = minOf(reference.red, reference.green, reference.blue)
            val saturation = if (high == 0f) 0f else ((high - low) / high * 1.10f).coerceIn(.68f, .95f)
            // Source shades keep their highlight/shadow ordering; bright stops remain colored.
            val shade = .2126f * reference.red + .7152f * reference.green + .0722f * reference.blue
            val value = ((high * .20f + shade * .80f) * .98f).coerceIn(0f, .98f)
            Color.hsv(theme.hue, saturation, value, reference.alpha)
        }
    }

    val metallicBorder get() = androidx.compose.ui.graphics.Brush.linearGradient(listOf(
        borderColor(0xFF7A5215), borderColor(0xFFFFE6A0), borderColor(0xFFD6A32E),
        borderColor(0xFFFFF5CD), borderColor(0xFF926315)))

    private val surfaces = ConcurrentHashMap<Pair<PremiumTheme, Long>, Color>()
    /** Uniform dark surfaces, tinted by the current theme. Preserves translucent overlays. */
    fun surface(source: Long): Color = surfaces.getOrPut(ThemeState.selected to source) {
        val r = ((source shr 16) and 255) / 255f
        val g = ((source shr 8) and 255) / 255f
        val b = (source and 255) / 255f
        val value = (.065f + maxOf(r, g, b) * .72f).coerceIn(.065f, .25f)
        tone(value, .68f, ((source shr 24) and 255) / 255f)
    }
    val background get() = tone(.115f, .68f)
    val panel get() = tone(.105f, .68f)
    val control get() = tone(.145f, .62f)
    /** Deliberately saturated dark fill, with white content for every selected control. */
    val selection get() = tone(.235f, .82f)
    fun tone(value: Float, saturation: Float, alpha: Float = 1f): Color =
        Color.hsv(ThemeState.selected.hue, saturation, value, alpha)
    internal fun accentArgb(source: Long, theme: PremiumTheme): Long {
        if (theme == PremiumTheme.GOLD) return source
        val r = ((source shr 16) and 255).toFloat() / 255f
        val g = ((source shr 8) and 255).toFloat() / 255f
        val b = (source and 255).toFloat() / 255f
        val originalValue = maxOf(r, g, b)
        // Compress highlights, retaining the order of the metallic gradient stops.
        // Gold returns above, before this curve. Alpha and hue remain unchanged.
        val value = originalValue * (0.96f - 0.22f * originalValue)
        val saturation = if (originalValue == 0f) 0f else ((originalValue - minOf(r, g, b)) / originalValue * 1.05f).coerceIn(0f, 1f)
        val c = value * saturation
        val h = theme.hue / 60f
        val x = c * (1f - kotlin.math.abs(h % 2f - 1f))
        val m = value - c
        val rgb = when (h.toInt()) {
            0 -> floatArrayOf(c,x,0f); 1 -> floatArrayOf(x,c,0f); 2 -> floatArrayOf(0f,c,x)
            3 -> floatArrayOf(0f,x,c); 4 -> floatArrayOf(x,0f,c); else -> floatArrayOf(c,0f,x)
        }
        fun channel(index: Int) = ((rgb[index] + m) * 255f + .5f).toLong().coerceIn(0L,255L)
        return (source and 0xFF000000L) or (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
    }
}

/** Vivid interface accents. Home bitmaps and their original tint are untouched. */
internal fun interfaceThemeAccent(theme: PremiumTheme = ThemeState.selected): Color = when(theme) {
    PremiumTheme.GOLD -> Color(0xFFFBBC04)
    PremiumTheme.BLUE -> Color(0xFF007AFF)
    PremiumTheme.PINK -> Color(0xFFFF2384)
    PremiumTheme.PURPLE -> Color(0xFFAF32FF)
    PremiumTheme.RED -> Color(0xFFFF3B30)
}
