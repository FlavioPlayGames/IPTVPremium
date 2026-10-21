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
    }
    @Test fun onlyFiveApprovedColorsAreSelectableAndDiscontinuedChoicesMigrate() {
        assertEquals(listOf("gold","blue","pink","purple","red"),PremiumTheme.entries.map{it.key})
        assertEquals(PremiumTheme.GOLD,PremiumTheme.fromKey("emerald"))
        assertEquals(PremiumTheme.GOLD,PremiumTheme.fromKey("cyan"))
        assertEquals(PremiumTheme.GOLD,PremiumTheme.fromKey(null))
        PremiumTheme.entries.forEach{assertEquals(it,PremiumTheme.fromKey(it.key))}
        val policy=br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.calculate(128,true)
        assertTrue(policy.postersMb<=20)
    }
}
