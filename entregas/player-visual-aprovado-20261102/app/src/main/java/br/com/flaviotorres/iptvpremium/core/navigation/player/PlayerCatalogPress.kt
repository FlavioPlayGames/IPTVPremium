package br.com.flaviotorres.iptvpremium.core.player

internal enum class PlayerPressResult { NONE, CLICK, FAVORITE }
/** Repeated key downs (even remotes reporting repeatCount=0) cannot restart the hold clock. */
internal class PlayerCatalogPress {
    private var start:Long?=null
    private var consumed=false
    fun down(now:Long):Boolean {if(start!=null)return false;start=now;consumed=false;return true}
    fun hold(now:Long,canFavorite:Boolean):PlayerPressResult {
        val began=start?:return PlayerPressResult.NONE
        if(consumed || !canFavorite || now-began<PLAYER_FAVORITE_HOLD_MS)return PlayerPressResult.NONE
        consumed=true;return PlayerPressResult.FAVORITE
    }
    fun up(now:Long,canFavorite:Boolean,canceled:Boolean=false):PlayerPressResult {
        val began=start
        val result=when {began==null || canceled || consumed->PlayerPressResult.NONE
            canFavorite && now-began>=PLAYER_FAVORITE_HOLD_MS->PlayerPressResult.FAVORITE
            else->PlayerPressResult.CLICK}
        cancel();return result
    }
    fun cancel(){start=null;consumed=false}
}
