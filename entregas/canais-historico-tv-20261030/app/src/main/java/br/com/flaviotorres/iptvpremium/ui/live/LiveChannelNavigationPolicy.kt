package br.com.flaviotorres.iptvpremium.ui.live

/** A profile never reads the old mixed history or another profile's keys. */
internal fun liveChannelHistoryKey(source:String,kids:Boolean,last:Boolean):String =
    "live_v2_${if(kids) "kids" else "normal"}_${if(last) "last" else "recent"}_$source"

/** Browsing a category moves focus; selecting a channel is a separate action. */
internal fun liveChannelEntryId(ids:List<String>, playing:String, saved:String?, preferPlaying:Boolean):String =
    if(preferPlaying) saved?.takeIf {it==playing} ?: ids.firstOrNull {it==playing} ?: ids.firstOrNull().orEmpty()
    else ids.firstOrNull().orEmpty()
