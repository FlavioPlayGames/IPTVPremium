package br.com.flaviotorres.iptvpremium.ui.theme

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.R
import org.junit.Assert.*
import org.junit.Test

/** The previous seven icon sheets are superseded by the two actual approved scene paintings. */
class StandardHomeArtworkDeviceTest {
    @Test fun bothApprovedScenesAndStaticWallpapersAreBundled() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val ids=listOf(R.drawable.marble_home_white,R.drawable.marble_home_black,
            R.drawable.marble_wallpaper_white,R.drawable.marble_wallpaper_black)
        assertEquals(4,ids.distinct().size)
        for(id in ids) {
            val options=BitmapFactory.Options().apply{inScaled=false;inJustDecodeBounds=true}
            BitmapFactory.decodeResource(context.resources,id,options)
            assertEquals(1672,options.outWidth);assertEquals(941,options.outHeight)
        }
        val logo=BitmapFactory.decodeResource(context.resources,R.drawable.marble_brand_gold,
            BitmapFactory.Options().apply{inScaled=false;inSampleSize=4})
        try {assertTrue(logo.hasAlpha());assertEquals(0,android.graphics.Color.alpha(logo.getPixel(0,0)))}
        finally {logo.recycle()}
    }
    @Test fun onlyFiveApprovedColorsAreSelectableAndDiscontinuedChoicesMigrate() {
        assertEquals(listOf("gold","blue","pink","purple","red"),PremiumTheme.entries.map{it.key})
        @Suppress("DEPRECATION") val legacyNames=listOf(PremiumTheme.EMERALD,PremiumTheme.CYAN)
        assertEquals(listOf(PremiumTheme.GOLD,PremiumTheme.GOLD),legacyNames)
        assertEquals(5,PremiumTheme.entries.size)
        assertEquals(PremiumTheme.GOLD,PremiumTheme.fromKey("emerald"))
        assertEquals(PremiumTheme.GOLD,PremiumTheme.fromKey("cyan"))
        assertEquals(PremiumTheme.GOLD,PremiumTheme.fromKey(null))
        PremiumTheme.entries.forEach{assertEquals(it,PremiumTheme.fromKey(it.key))}
        val policy=br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.calculate(128,true)
        assertTrue(policy.postersMb<=20)
    }
}
