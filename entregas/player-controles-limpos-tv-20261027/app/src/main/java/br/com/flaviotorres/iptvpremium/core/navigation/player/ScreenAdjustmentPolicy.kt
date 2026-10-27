package br.com.flaviotorres.iptvpremium.core.player

/** Shared fine adjustment, identical for films, episodes and live channels. */
internal object ScreenAdjustmentPolicy {
    const val IDLE_MS=5_000L
    const val STEP=.02f
    /** Every entry into either editor starts from original geometry, not the last edit. */
    fun freshLayout()=PlaybackVideoLayout(PlaybackAspectMode.MANUAL,PlaybackManualTransform())
    fun step(value:Float,direction:Int):Float =
        ((kotlin.math.round(value*100f).toInt()+direction*2).coerceIn(25,400))/100f
}
