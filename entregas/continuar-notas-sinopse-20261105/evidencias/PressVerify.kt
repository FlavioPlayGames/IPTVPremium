package br.com.flaviotorres.iptvpremium.core.player
internal const val PLAYER_FAVORITE_HOLD_MS=2_000L
fun main(){
 val p=PlayerCatalogPress()
 check(p.down(0));check(!p.down(100));check(p.up(200,false)==PlayerPressResult.CLICK);check(p.up(201,false)==PlayerPressResult.NONE)
 p.down(1000);check(p.up(4000,false)==PlayerPressResult.CLICK)
 p.down(5000);p.cancel();check(p.up(5100,false)==PlayerPressResult.NONE)
 p.down(6000);check(p.up(6100,false,true)==PlayerPressResult.NONE)
 println("PASS synopsis confirmation: one action on key-up, repeats ignored, long press without favorite, focus cancellation and canceled key")
}
