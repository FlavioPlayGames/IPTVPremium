package br.com.flaviotorres.iptvpremium.core.player
internal const val PLAYER_FAVORITE_HOLD_MS=2_000L
internal fun playerFavoriteHoldComplete(elapsed:Long,focused:Boolean,windowFocused:Boolean):Boolean =
    elapsed>=PLAYER_FAVORITE_HOLD_MS && focused && windowFocused
