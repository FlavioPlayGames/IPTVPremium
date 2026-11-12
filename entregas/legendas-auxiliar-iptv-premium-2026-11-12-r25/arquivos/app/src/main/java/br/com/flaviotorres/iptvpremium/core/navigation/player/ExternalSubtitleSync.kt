package br.com.flaviotorres.iptvpremium.core.player

/** Positive offset delays captions; use the video clock for ExoPlayer and VLC alike. */
internal const val EXTERNAL_SUBTITLE_OFFSET_LIMIT=120_000L
internal fun externalSubtitleClock(videoMs:Long,offsetMs:Long):Long =
    videoMs.coerceIn(0,Long.MAX_VALUE-EXTERNAL_SUBTITLE_OFFSET_LIMIT)-
        offsetMs.coerceIn(-EXTERNAL_SUBTITLE_OFFSET_LIMIT,EXTERNAL_SUBTITLE_OFFSET_LIMIT)

/** Work/season setting, independent of episode, subtitle file and subtitle provider. */
internal fun externalSubtitleSyncKey(kind:String,title:String,year:Int?,tmdb:String?,season:Int?,localScope:String,localId:String,kids:Boolean):String {
    require(kind in setOf("movie","tv"))
    val work=br.com.flaviotorres.iptvpremium.data.repository.catalogMetadataKey(kind,title,year,tmdb,null)
        ?: "$kind:local:$localScope:$localId"
    val part=if(kind=="tv")season?.takeIf{it>=0}?.let{"season:$it"}?:"unknown-season:$localId"else "movie"
    return java.security.MessageDigest.getInstance("SHA-256").digest(
        listOf("work_sync_v1",if(kids)"kids"else"normal",work,part).joinToString("\u0000").toByteArray(Charsets.UTF_8)
    ).joinToString(""){"%02x".format(it)}
}
internal fun externalSubtitleOffsetLabel(offset:Long):String =
    java.lang.String.format(java.util.Locale.forLanguageTag("pt-BR"),"%+.1f s",offset/1000.0)
internal val subtitleSyncSteps=listOf(100L,500L,1_000L,5_000L)

internal enum class SubtitleAssistantPhase { EXPLAIN, PREPARING, LISTENING }
/** An estimate from the first source cue, replacing (not adding to) the saved offset. */
internal fun subtitleAssistantOffset(clickVideoMs:Long,firstSourceCueMs:Long):Long {
    require(clickVideoMs>=0&&firstSourceCueMs>=0)
    val difference=(clickVideoMs-firstSourceCueMs)
        .coerceIn(-EXTERNAL_SUBTITLE_OFFSET_LIMIT,EXTERNAL_SUBTITLE_OFFSET_LIMIT)
    // Nearest whole second; half a second rounds away from zero in both directions.
    return if(difference>=0L)((difference+500L)/1_000L)*1_000L
        else -(((-difference+500L)/1_000L)*1_000L)
}
