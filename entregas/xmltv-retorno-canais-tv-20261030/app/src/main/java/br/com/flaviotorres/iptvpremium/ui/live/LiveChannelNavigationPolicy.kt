package br.com.flaviotorres.iptvpremium.ui.live

/** A profile never reads the old mixed history or another profile's keys. */
internal fun liveChannelHistoryKey(source:String,kids:Boolean,last:Boolean):String =
    "live_v2_${if(kids) "kids" else "normal"}_${if(last) "last" else "recent"}_$source"

/** Browsing a category moves focus; selecting a channel is a separate action. */
internal fun liveChannelEntryId(ids:List<String>, playing:String, saved:String?, preferPlaying:Boolean):String =
    if(preferPlaying) saved?.takeIf {it==playing} ?: ids.firstOrNull {it==playing} ?: ids.firstOrNull().orEmpty()
    else ids.firstOrNull().orEmpty()

/** Merely visiting other categories never changes the reference channel. */
internal fun liveRestorePlayingInCategory(playingCategory:String?,browsedCategory:String?,all:Boolean):Boolean =
    all || (playingCategory!=null && playingCategory==browsedCategory)

internal enum class LiveBackAction { CLOSE_PICTURE, CLOSE_ZAP, PLAYING_CHANNEL, CATEGORIES, EXIT, FOCUS_CATEGORIES }

/** One BACK decision is shared by hardware keys and Android's back dispatcher. */
internal fun liveBackAction(fullScreen:Boolean,picture:Boolean,zap:Boolean,fromPlaybackControls:Boolean,
    channelsShown:Boolean,categoryFocused:Boolean,kids:Boolean):LiveBackAction = when {
    fullScreen && picture -> LiveBackAction.CLOSE_PICTURE
    fullScreen && zap -> LiveBackAction.CLOSE_ZAP
    fullScreen || fromPlaybackControls -> LiveBackAction.PLAYING_CHANNEL
    channelsShown -> LiveBackAction.CATEGORIES
    kids || categoryFocused -> LiveBackAction.EXIT
    else -> LiveBackAction.FOCUS_CATEGORIES
}
