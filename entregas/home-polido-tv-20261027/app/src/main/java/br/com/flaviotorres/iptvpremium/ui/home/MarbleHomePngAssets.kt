package br.com.flaviotorres.iptvpremium.ui.home

import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme

/** Smooth approved artwork; pre-coloured sheets shared by light/dark mode. */
internal data class MarblePngAsset(val resource:Int,val width:Int,val height:Int,val crop:MarbleRegion)
internal object MarbleHomePngAssets {
    private val crops=mapOf(
        R.drawable.home_corrected_gold to listOf(MarbleRegion(8,159,448,552),MarbleRegion(448,131,870,574),MarbleRegion(873,143,1301,572),MarbleRegion(1303,137,1706,593),MarbleRegion(1709,154,2161,571)),
        R.drawable.home_corrected_blue to listOf(MarbleRegion(7,156,451,555),MarbleRegion(451,128,871,577),MarbleRegion(873,141,1301,575),MarbleRegion(1301,134,1706,597),MarbleRegion(1709,152,2166,575)),
        R.drawable.home_corrected_pink to listOf(MarbleRegion(8,157,448,552),MarbleRegion(448,125,872,573),MarbleRegion(877,142,1303,572),MarbleRegion(1305,135,1709,594),MarbleRegion(1715,153,2167,570)),
        R.drawable.home_corrected_purple to listOf(MarbleRegion(8,158,449,551),MarbleRegion(449,130,870,573),MarbleRegion(872,143,1300,571),MarbleRegion(1301,137,1704,593),MarbleRegion(1707,153,2161,571)),
        R.drawable.home_corrected_red to listOf(MarbleRegion(7,155,450,553),MarbleRegion(450,128,871,574),MarbleRegion(872,141,1301,572),MarbleRegion(1302,135,1707,595),MarbleRegion(1710,150,2166,573)))
    private val dimensions=mapOf(
        R.drawable.home_corrected_gold to (2172 to 724),
        R.drawable.home_corrected_blue to (2170 to 725),
        R.drawable.home_corrected_pink to (2172 to 724),
        R.drawable.home_corrected_purple to (2172 to 724),
        R.drawable.home_corrected_red to (2172 to 724))
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
