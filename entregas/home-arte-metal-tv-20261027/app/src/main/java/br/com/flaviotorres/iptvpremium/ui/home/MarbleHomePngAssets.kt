package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme

/** Corrected native artwork shared by light/dark mode; no runtime recolouring. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    private val crops=mapOf(
        R.drawable.home_corrected_gold to listOf(MarbleRegion(8,176,455,538),MarbleRegion(455,135,880,564),MarbleRegion(885,155,1308,561),MarbleRegion(1308,141,1735,582),MarbleRegion(1735,171,2171,565)),
        R.drawable.home_corrected_blue to listOf(MarbleRegion(6,178,455,532),MarbleRegion(455,141,885,560),MarbleRegion(885,157,1308,558),MarbleRegion(1308,144,1735,578),MarbleRegion(1735,174,2172,561)),
        R.drawable.home_corrected_pink to listOf(MarbleRegion(6,178,455,531),MarbleRegion(455,139,885,559),MarbleRegion(887,155,1308,556),MarbleRegion(1308,143,1735,576),MarbleRegion(1735,172,2172,560)),
        R.drawable.home_corrected_purple to listOf(MarbleRegion(6,179,455,532),MarbleRegion(455,141,885,560),MarbleRegion(885,156,1308,558),MarbleRegion(1308,144,1735,579),MarbleRegion(1735,172,2172,561)),
        R.drawable.home_corrected_red to listOf(MarbleRegion(4,169,455,520),MarbleRegion(455,133,885,553),MarbleRegion(885,148,1307,551),MarbleRegion(1312,137,1735,570),MarbleRegion(1735,162,2171,554)))
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
        return crops.getValue(id).map{MarblePngAsset(id,2172,724,it)}
    }
    val icons:List<MarblePngAsset> get()=forMode(false)
    val resourceIds:Set<Int> get()=crops.keys
}
