package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R

/** Finished transparent PNGs: only the inner material differs between light and dark modes. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    private val light=listOf(
        MarblePngAsset(R.drawable.home_finished_channels_light,1536,1024,MarbleRegion(121,99,1444,880)),
        MarblePngAsset(R.drawable.home_finished_movies_light,1536,1024,MarbleRegion(147,42,1400,985)),
        MarblePngAsset(R.drawable.home_finished_series_light,1536,1024,MarbleRegion(141,32,1445,989)),
        MarblePngAsset(R.drawable.home_finished_kids_light,1338,1175,MarbleRegion(64,52,1283,1159)),
        MarblePngAsset(R.drawable.home_finished_lists_light,1536,1024,MarbleRegion(158,51,1423,967)))
    private val dark=listOf(
        MarblePngAsset(R.drawable.home_finished_channels_dark,1536,1024,MarbleRegion(73,68,1486,906)),
        MarblePngAsset(R.drawable.home_finished_movies_dark,1536,1024,MarbleRegion(147,42,1401,984)),
        MarblePngAsset(R.drawable.home_finished_series_dark,1536,1024,MarbleRegion(183,48,1397,962)),
        MarblePngAsset(R.drawable.home_finished_kids_dark,1338,1175,MarbleRegion(62,52,1287,1159)),
        MarblePngAsset(R.drawable.home_finished_lists_dark,1536,1024,MarbleRegion(158,51,1423,966)))
    fun forMode(lightMode:Boolean):List<MarblePngAsset> = if(lightMode)light else dark
    val icons:List<MarblePngAsset> get()=dark
    val resourceIds:Set<Int> = (light+dark).map{it.resource}.toSet()
}
