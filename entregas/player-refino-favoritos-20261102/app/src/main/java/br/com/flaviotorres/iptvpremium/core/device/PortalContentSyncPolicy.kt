package br.com.flaviotorres.iptvpremium.core.device

/** Foreground fallback complements FCM; it never runs in a stopped Activity. */
internal const val PORTAL_CONTENT_POLL_MS = 120_000L
internal fun portalContentRetryDelay(failures:Int):Long =
    (PORTAL_CONTENT_POLL_MS * (1L shl failures.coerceIn(0,3))).coerceAtMost(900_000L)
internal fun portalPullPath(automatic:Boolean,includeSubtitleCommands:Boolean):String =
    if(automatic && !includeSubtitleCommands)"/api/device/pull?mode=auto"else"/api/device/pull"
internal fun isPortalContentHint(type:String?,version:String?):Boolean = version=="1" && type in setOf(
    "lists_changed","subtitles_changed","subtitle_key_changed","settings_changed")
