package br.com.flaviotorres.iptvpremium.core.player

import androidx.compose.ui.graphics.Color
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaAccent
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeState
import br.com.flaviotorres.iptvpremium.ui.theme.interfaceThemeAccent

/** Player colors are explicit: a Kids session never borrows the main account palette. */
internal fun playerThemeAccent():Color = if(KidsAppearance.active)kidsCinemaAccent() else interfaceThemeAccent(ThemeState.mainTheme)
internal const val PLAYER_FAVORITE_HOLD_MS=2_000L
internal fun playerFavoriteHoldComplete(elapsed:Long,focused:Boolean,windowFocused:Boolean):Boolean =
    elapsed>=PLAYER_FAVORITE_HOLD_MS && focused && windowFocused
