package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme

/** Smooth approved artwork; pre-coloured sheets shared by light/dark mode. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    private val crops=mapOf(
        R.drawable.home_corrected_gold to listOf(MarbleRegion(6,178,463,532),MarbleRegion(463,153,884,559),MarbleRegion(889,156,1314,557),MarbleRegion(1316,143,1730,576),MarbleRegion(1733,172,2169,560)),
        R.drawable.home_corrected_blue to listOf(MarbleRegion(6,178,462,533),MarbleRegion(462,152,884,560),MarbleRegion(889,155,1314,557),MarbleRegion(1316,142,1731,577),MarbleRegion(1733,172,2169,560)),
        R.drawable.home_corrected_pink to listOf(MarbleRegion(6,178,462,533),MarbleRegion(462,152,885,560),MarbleRegion(889,156,1314,557),MarbleRegion(1316,142,1731,577),MarbleRegion(1733,172,2169,561)),
        R.drawable.home_corrected_purple to listOf(MarbleRegion(5,175,456,528),MarbleRegion(456,150,873,555),MarbleRegion(878,153,1299,553),MarbleRegion(1301,139,1711,573),MarbleRegion(1713,168,2158,556)),
        R.drawable.home_corrected_red to listOf(MarbleRegion(6,178,463,532),MarbleRegion(463,152,885,559),MarbleRegion(889,156,1314,556),MarbleRegion(1317,142,1731,576),MarbleRegion(1734,172,2170,559)))
    private val dimensions=mapOf(
        R.drawable.home_corrected_gold to (2169 to 725),
        R.drawable.home_corrected_blue to (2169 to 725),
        R.drawable.home_corrected_pink to (2169 to 725),
        R.drawable.home_corrected_purple to (2169 to 725),
        R.drawable.home_corrected_red to (2170 to 725))
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
