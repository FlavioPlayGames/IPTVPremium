package br.com.flaviotorres.iptvpremium.core.player
import android.content.Context
internal class SubtitleAppearanceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("subtitle_appearance_v1", Context.MODE_PRIVATE)
    fun load(): SubtitleAppearance {
        if(!preferences.getBoolean("yellow_shadow_default_v2",false)) {
            val legacy=preferences.getInt("color",-1) in listOf(-1,0xFFFFFFFF.toInt()) &&
                preferences.getInt("size",22)==22 && preferences.getString("edge","OUTLINE")=="OUTLINE"
            preferences.edit().apply {
                if(legacy){putInt("color",0xFFFFFF00.toInt());putString("edge","SHADOW")}
                putBoolean("yellow_shadow_default_v2",true)
            }.apply()
        }
        val savedColor = preferences.getInt("color", 0xFFFFFF00.toInt())
        // Migrate the former cream option to the new neutral white palette.
        val migratedColor = if (savedColor == 0xFFFFF4BD.toInt()) 0xFFF2F2F2.toInt() else savedColor
        val color = migratedColor.takeIf { argb -> subtitleTextColors.any { it.argb == argb } } ?: 0xFFFFFF00.toInt()
        return SubtitleAppearance(
            textArgb = color,
            background = runCatching { SubtitleBackground.valueOf(preferences.getString("background", null).orEmpty()) }.getOrDefault(SubtitleBackground.TRANSLUCENT),
            sizeSp = preferences.getInt("size", 22).coerceIn(16, 40),
            typeface = runCatching { SubtitleTypeface.valueOf(preferences.getString("font", null).orEmpty()) }.getOrDefault(SubtitleTypeface.DEFAULT),
            edge = runCatching { SubtitleEdge.valueOf(preferences.getString("edge", null).orEmpty()) }.getOrDefault(SubtitleEdge.SHADOW)
        )
    }
    fun observe(onChange: () -> Unit): AutoCloseable {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange() }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return AutoCloseable { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun save(value: SubtitleAppearance) {
        preferences.edit().putInt("color", value.textArgb).putString("background", value.background.name)
            .putInt("size", value.sizeSp).putString("font", value.typeface.name).putString("edge", value.edge.name).apply()
    }
}
