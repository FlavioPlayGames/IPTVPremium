package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R

/** Measured alpha bounds, not resized source files; destination bounds stay exactly as approved. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    val icons=listOf(
        MarblePngAsset(R.drawable.marble_icon_channels,1254,1254,MarbleRegion(81,257,1175,1095)),
        MarblePngAsset(R.drawable.marble_icon_movies,1254,1254,MarbleRegion(102,88,1154,1156)),
        MarblePngAsset(R.drawable.marble_icon_series,1254,1254,MarbleRegion(85,125,1187,1148)),
        MarblePngAsset(R.drawable.marble_icon_kids,1254,1254,MarbleRegion(76,120,1177,1147)),
        MarblePngAsset(R.drawable.marble_icon_lists,1254,1254,MarbleRegion(66,219,1190,1073)))
}
