package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme

internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
/** Seven independent alpha cutouts share one decoded sheet, never the scenery. */
internal object MarbleHomePngAssets {
    private val regions=listOf(
        MarbleRegion(10,180,340,512), MarbleRegion(345,180,635,515),
        MarbleRegion(650,232,982,498), MarbleRegion(993,217,1338,513),
        MarbleRegion(1348,247,1674,514), MarbleRegion(1687,263,1896,480),
        MarbleRegion(1938,258,2177,503))
    fun forMode(lightMode:Boolean,theme:PremiumTheme=PremiumTheme.GOLD):List<MarblePngAsset> =
        regions.map { MarblePngAsset(R.drawable.home_free_icons,2192,717,it) }
    val icons:List<MarblePngAsset> get()=forMode(false)
    val resourceIds:Set<Int> get()=setOf(R.drawable.home_free_icons)
}
