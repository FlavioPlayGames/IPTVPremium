package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme

/** Smooth approved artwork; pre-coloured sheets shared by light/dark mode. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    private val crops=mapOf(
        R.drawable.home_corrected_gold to listOf(MarbleRegion(16,169,465,535),MarbleRegion(473,139,880,556),MarbleRegion(884,149,1283,557),MarbleRegion(1285,136,1670,569),MarbleRegion(1670,170,2126,558)),
        R.drawable.home_corrected_blue to listOf(MarbleRegion(6,205,443,549),MarbleRegion(443,163,839,569),MarbleRegion(846,176,1246,570),MarbleRegion(1252,163,1633,582),MarbleRegion(1639,201,2069,570)),
        R.drawable.home_corrected_pink to listOf(MarbleRegion(13,196,449,533),MarbleRegion(458,163,866,555),MarbleRegion(876,169,1283,554),MarbleRegion(1289,166,1674,566),MarbleRegion(1683,196,2129,554)),
        R.drawable.home_corrected_purple to listOf(MarbleRegion(12,196,452,527),MarbleRegion(460,161,863,549),MarbleRegion(866,163,1276,549),MarbleRegion(1276,150,1668,566),MarbleRegion(1673,187,2127,550)),
        R.drawable.home_corrected_red to listOf(MarbleRegion(8,205,450,532),MarbleRegion(451,174,842,549),MarbleRegion(847,185,1238,549),MarbleRegion(1242,177,1619,560),MarbleRegion(1623,201,2065,549)))
    private val dimensions=mapOf(
        R.drawable.home_corrected_gold to (2141 to 734),
        R.drawable.home_corrected_blue to (2073 to 758),
        R.drawable.home_corrected_pink to (2142 to 734),
        R.drawable.home_corrected_purple to (2141 to 734),
        R.drawable.home_corrected_red to (2073 to 758))
    private fun resource(theme:PremiumTheme):Int = when(theme) {
        PremiumTheme.GOLD -> R.drawable.home_corrected_gold
        PremiumTheme.BLUE -> R.drawable.home_corrected_blue
        PremiumTheme.PINK -> R.drawable.home_corrected_pink
        PremiumTheme.PURPLE -> R.drawable.home_corrected_purple
        PremiumTheme.RED -> R.drawable.home_corrected_red
    }
    @Suppress("UNUSED_PARAMETER")
    fun forMode(lightMode:Boolean,theme:PremiumTheme=PremiumTheme.GOLD):List<MarblePngAsset> {
        val id=resource(theme)
        val (width,height)=dimensions.getValue(id)
        return crops.getValue(id).map{MarblePngAsset(id,width,height,it)}
    }
    val icons:List<MarblePngAsset> get()=forMode(false)
    val resourceIds:Set<Int> get()=crops.keys
}
