package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.text.Cue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView

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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun applySubtitleAppearance(view: SubtitleView, appearance: SubtitleAppearance) {
    if (view.tag == appearance) return
    view.setApplyEmbeddedStyles(false)
    view.setApplyEmbeddedFontSizes(false)
    view.setStyle(CaptionStyleCompat(appearance.textArgb, appearance.background.argb, android.graphics.Color.TRANSPARENT,
        when (appearance.edge) {
            SubtitleEdge.NONE -> CaptionStyleCompat.EDGE_TYPE_NONE
            SubtitleEdge.OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
            SubtitleEdge.SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        }, android.graphics.Color.BLACK, Typeface.create(appearance.typeface.family, Typeface.NORMAL)))
    view.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, appearance.sizeSp.toFloat())
    view.setBottomPaddingFraction(0.08f)
    view.tag = appearance
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun StyledSubtitleText(text: String, appearance: SubtitleAppearance, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context -> SubtitleView(context).apply { isClickable = false; isFocusable = false } },
        update = { view ->
            applySubtitleAppearance(view, appearance)
            view.setCues(listOf(Cue.Builder().setText(text).build()))
        },
        modifier = modifier
    )
}

// IPTV_SUBTITLE_READABILITY_V1

// IPTV_PORTAL_PRODUCTION_V1
