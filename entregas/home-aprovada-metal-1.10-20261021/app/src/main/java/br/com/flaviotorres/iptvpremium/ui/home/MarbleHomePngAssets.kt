package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R

/** Approved pearl/gray PNGs with a flat TV antenna; only their metal takes the selected theme hue. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    private val light=listOf(
        MarblePngAsset(R.drawable.home_approved_channels_light,1536,1024,MarbleRegion(62,86,1492,915)),
        MarblePngAsset(R.drawable.home_approved_movies_light,1536,1024,MarbleRegion(144,40,1405,990)),
        MarblePngAsset(R.drawable.home_approved_series_light,1536,1024,MarbleRegion(136,38,1451,991)),
        MarblePngAsset(R.drawable.home_approved_kids_light,1536,1024,MarbleRegion(146,36,1392,1001)),
        MarblePngAsset(R.drawable.home_approved_lists_light,1536,1024,MarbleRegion(119,34,1445,971)))
    private val dark=listOf(
        MarblePngAsset(R.drawable.home_approved_channels_dark,1536,1024,MarbleRegion(102,97,1457,901)),
        MarblePngAsset(R.drawable.home_approved_movies_dark,1536,1024,MarbleRegion(109,39,1450,994)),
        MarblePngAsset(R.drawable.home_approved_series_dark,1536,1024,MarbleRegion(130,20,1455,1002)),
        MarblePngAsset(R.drawable.home_approved_kids_dark,1385,1136,MarbleRegion(52,26,1347,1130)),
        MarblePngAsset(R.drawable.home_approved_lists_dark,1536,1024,MarbleRegion(106,28,1467,992)))
    fun forMode(lightMode:Boolean):List<MarblePngAsset> = if(lightMode)light else dark
    val icons:List<MarblePngAsset> get()=dark
    val resourceIds:Set<Int> = (light+dark).map{it.resource}.toSet()
}
