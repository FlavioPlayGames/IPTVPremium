package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme

/** Smooth approved artwork; pre-coloured sheets selected by theme and background mode. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    private val crops=mapOf(
        R.drawable.home_corrected_gold to listOf(MarbleRegion(16,169,465,535),MarbleRegion(473,139,880,556),MarbleRegion(884,149,1283,557),MarbleRegion(1285,136,1670,569),MarbleRegion(1670,170,2126,558)),
        R.drawable.home_corrected_blue to listOf(MarbleRegion(6,205,443,549),MarbleRegion(443,163,839,569),MarbleRegion(846,176,1246,570),MarbleRegion(1252,163,1633,582),MarbleRegion(1639,201,2069,570)),
        R.drawable.home_corrected_pink to listOf(MarbleRegion(13,196,449,533),MarbleRegion(458,163,866,555),MarbleRegion(876,169,1283,554),MarbleRegion(1289,166,1674,566),MarbleRegion(1683,196,2129,554)),
        R.drawable.home_corrected_purple to listOf(MarbleRegion(12,196,452,527),MarbleRegion(460,161,863,549),MarbleRegion(866,163,1276,549),MarbleRegion(1276,150,1668,566),MarbleRegion(1673,187,2127,550)),
        R.drawable.home_corrected_red to listOf(MarbleRegion(8,205,450,532),MarbleRegion(451,174,842,549),MarbleRegion(847,185,1238,549),MarbleRegion(1242,177,1619,560),MarbleRegion(1623,201,2065,549)),
        R.drawable.home_corrected_gold_white to listOf(MarbleRegion(16,163,459,526),MarbleRegion(466,134,869,547),MarbleRegion(873,144,1266,548),MarbleRegion(1266,131,1656,559),MarbleRegion(1656,163,2097,548)),
        R.drawable.home_corrected_blue_white to listOf(MarbleRegion(5,203,441,551),MarbleRegion(441,161,840,572),MarbleRegion(845,174,1246,572),MarbleRegion(1250,160,1634,584),MarbleRegion(1638,199,2070,572)),
        R.drawable.home_corrected_pink_white to listOf(MarbleRegion(13,187,447,530),MarbleRegion(455,156,863,552),MarbleRegion(870,159,1277,551),MarbleRegion(1281,157,1664,564),MarbleRegion(1672,187,2118,552)),
        R.drawable.home_corrected_purple_white to listOf(MarbleRegion(12,190,450,522),MarbleRegion(455,156,858,544),MarbleRegion(860,158,1266,545),MarbleRegion(1266,145,1656,561),MarbleRegion(1660,182,2110,545)),
        R.drawable.home_corrected_red_white to listOf(MarbleRegion(6,203,450,534),MarbleRegion(450,172,845,551),MarbleRegion(847,182,1241,551),MarbleRegion(1242,174,1621,562),MarbleRegion(1622,199,2069,551)))
    private val dimensions=mapOf(
        R.drawable.home_corrected_gold to (2141 to 734),
        R.drawable.home_corrected_blue to (2073 to 758),
        R.drawable.home_corrected_pink to (2142 to 734),
        R.drawable.home_corrected_purple to (2141 to 734),
        R.drawable.home_corrected_red to (2073 to 758),
        R.drawable.home_corrected_gold_white to (2141 to 734),
        R.drawable.home_corrected_blue_white to (2073 to 759),
        R.drawable.home_corrected_pink_white to (2142 to 734),
        R.drawable.home_corrected_purple_white to (2141 to 734),
        R.drawable.home_corrected_red_white to (2073 to 759))
    private fun resource(lightMode:Boolean,theme:PremiumTheme):Int = when(theme) {
        PremiumTheme.GOLD -> if(lightMode) R.drawable.home_corrected_gold_white else R.drawable.home_corrected_gold
        PremiumTheme.BLUE -> if(lightMode) R.drawable.home_corrected_blue_white else R.drawable.home_corrected_blue
        PremiumTheme.PINK -> if(lightMode) R.drawable.home_corrected_pink_white else R.drawable.home_corrected_pink
        PremiumTheme.PURPLE -> if(lightMode) R.drawable.home_corrected_purple_white else R.drawable.home_corrected_purple
        PremiumTheme.RED -> if(lightMode) R.drawable.home_corrected_red_white else R.drawable.home_corrected_red
    }
    fun forMode(lightMode:Boolean,theme:PremiumTheme=PremiumTheme.GOLD):List<MarblePngAsset> {
        val id=resource(lightMode,theme)
        val (width,height)=dimensions.getValue(id)
        return crops.getValue(id).map{MarblePngAsset(id,width,height,it)}
    }
    val icons:List<MarblePngAsset> get()=forMode(false)
    val resourceIds:Set<Int> get()=crops.keys
}
