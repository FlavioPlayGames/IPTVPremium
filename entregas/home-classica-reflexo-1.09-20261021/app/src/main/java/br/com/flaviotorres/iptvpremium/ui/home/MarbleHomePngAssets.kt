package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R

/** Original transparent artwork, cropped to the visible object; native proportions and approved slot widths. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    val icons=listOf(
        MarblePngAsset(R.drawable.home_classic_channels,1536,1024,MarbleRegion(119,97,1445,881)),
        MarblePngAsset(R.drawable.home_classic_movies,1536,1024,MarbleRegion(146,40,1402,986)),
        MarblePngAsset(R.drawable.home_classic_series,1536,1024,MarbleRegion(183,41,1397,962)),
        MarblePngAsset(R.drawable.home_classic_kids,1339,1175,MarbleRegion(62,52,1284,1160)),
        MarblePngAsset(R.drawable.home_classic_lists,1536,1024,MarbleRegion(157,45,1425,967)))
}
