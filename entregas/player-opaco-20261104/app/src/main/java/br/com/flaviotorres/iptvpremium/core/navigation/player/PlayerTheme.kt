package br.com.flaviotorres.iptvpremium.core.player

import androidx.compose.ui.graphics.Color
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeState
import br.com.flaviotorres.iptvpremium.ui.theme.interfaceThemeAccent

/** Player colors are explicit: a Kids session never borrows the main account palette. */
internal fun playerThemeAccent():Color = if(KidsAppearance.active)KidsAppearance.accent else interfaceThemeAccent(ThemeState.mainTheme)
internal const val PLAYER_FAVORITE_HOLD_MS=2_000L
internal fun playerFavoriteHoldComplete(elapsed:Long,focused:Boolean,windowFocused:Boolean):Boolean =
    elapsed>=PLAYER_FAVORITE_HOLD_MS && focused && windowFocused

// Same neutral translucent surface across normal/Kids player overlays.
internal const val PLAYER_OVERLAY_ARGB=0xD9111417L
// Rating stars never inherit the selected theme.
internal const val PLAYER_RATING_STAR_ARGB=0xFFFFD600L
