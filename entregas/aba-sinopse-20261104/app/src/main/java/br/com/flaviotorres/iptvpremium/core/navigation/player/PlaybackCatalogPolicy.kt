package br.com.flaviotorres.iptvpremium.core.player

import android.view.KeyEvent

internal enum class PlayerCatalogLevel { ITEMS, CATEGORIES, SEASONS, EPISODES }
internal data class CatalogPlaybackIdentity(val type:PlaybackContentType,val id:String,val seriesId:String?=null)
internal fun isCurrentCatalogSelection(current:CatalogPlaybackIdentity,next:CatalogPlaybackIdentity):Boolean =
    current.type==next.type && current.id==next.id &&
        (current.type!=PlaybackContentType.EPISODE || current.seriesId==next.seriesId)
internal fun isPlaybackCatalogKey(code:Int):Boolean = when(code) {
    KeyEvent.KEYCODE_CHANNEL_UP,KeyEvent.KEYCODE_CHANNEL_DOWN,KeyEvent.KEYCODE_TV_CONTENTS_MENU -> true
    else -> false
}

/** Protect stream startup/rebuffering, but allow in-player covers and portal updates once ready. */
internal fun playbackNeedsNetworkPriority(status:PlaybackStatus):Boolean = when(status) {
    PlaybackStatus.IDLE,PlaybackStatus.PREPARING,PlaybackStatus.BUFFERING -> true
    else -> false
}

internal enum class CatalogBackStep { SYNOPSIS, SEASONS, COVERS, CATEGORIES, CLOSE }
internal fun catalogBackStep(level:PlayerCatalogLevel,synopsis:Boolean):CatalogBackStep =
    if(synopsis)CatalogBackStep.SYNOPSIS else when(level) {
        PlayerCatalogLevel.EPISODES->CatalogBackStep.SEASONS
        PlayerCatalogLevel.SEASONS->CatalogBackStep.COVERS
        PlayerCatalogLevel.ITEMS->CatalogBackStep.CATEGORIES
        PlayerCatalogLevel.CATEGORIES->CatalogBackStep.CLOSE
    }
