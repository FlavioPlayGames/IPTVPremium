package br.com.flaviotorres.iptvpremium.ui.theme
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.R
import org.junit.Assert.*
import org.junit.Test
class StandardHomeArtworkDeviceTest {
    @Test fun everyThemeHasItsOwnTransparentFiveIconArtwork() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val ids=listOf(R.drawable.standard_home_gold,R.drawable.standard_home_blue,R.drawable.standard_home_purple,
            R.drawable.standard_home_red,R.drawable.standard_home_pink,R.drawable.standard_home_emerald,R.drawable.standard_home_cyan)
        assertEquals(7,ids.distinct().size)
        for(id in ids) {
            val image=BitmapFactory.decodeResource(context.resources,id)
            assertNotNull(image)
            try { assertEquals(2172,image.width);assertEquals(724,image.height);assertTrue(image.hasAlpha()) }
            finally {image.recycle()}
        }
    }
    @Test fun imageBordersAreTransparentAndPosterMemoryDoesNotDependOn125Files() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val image=BitmapFactory.decodeResource(context.resources,R.drawable.standard_home_red)
        try { assertEquals(0,android.graphics.Color.alpha(image.getPixel(0,0))) }
        finally {image.recycle()}
        val policy=br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.calculate(128,true)
        assertTrue(policy.postersMb<=20)
    }
}
