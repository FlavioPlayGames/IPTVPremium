package br.com.flaviotorres.iptvpremium.ui.home

import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeFinishedIconsTest {
    @Test fun tenDistinctFinishedStripsCoverBothModesAndEveryTheme() {
        val ids=mutableSetOf<Int>()
        for(theme in PremiumTheme.entries) for(light in listOf(true,false)) {
            val assets=MarbleHomePngAssets.forMode(light,theme)
            assertEquals(5,assets.size)
            assertEquals(1,assets.map{it.resource}.toSet().size)
            assertTrue(ids.add(assets.first().resource))
            assets.forEach {
                assertTrue(it.crop.left>=0 && it.crop.top>=0)
                assertTrue(it.crop.right<=it.width && it.crop.bottom<=it.height)
                assertTrue(it.crop.width>0 && it.crop.height>0)
            }
        }
        assertEquals(10,ids.size)
        assertEquals(ids,MarbleHomePngAssets.resourceIds)
    }
    @Test fun actualPngsShareTheBoundedCacheWithoutRecolouring()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        for(theme in PremiumTheme.entries) for(light in listOf(true,false)) {
            MarbleArtworkCache.clear()
            val assets=MarbleHomePngAssets.forMode(light,theme)
            val bitmap=MarbleArtworkCache.load(context,assets.first().resource)!!
            assertTrue(bitmap.asAndroidBitmap().hasAlpha())
            assets.forEach { assertSame(bitmap,MarbleArtworkCache.load(context,it.resource)) }
            assertSame(bitmap,MarbleArtworkCache.peek(assets.first().resource))
        }
    }
    @Test fun utilityNavigationAndOriginalTargetsRemainIntact() {
        assertEquals(MarbleRegion(1472,50,1537,111),MarbleHomeGeometry.icons[5])
        assertEquals(MarbleRegion(1570,49,1642,115),MarbleHomeGeometry.icons[6])
        assertEquals(6,MarbleHomeNavigation.next(5,MarbleDirection.RIGHT,true))
        assertEquals(5,MarbleHomeNavigation.next(6,MarbleDirection.LEFT,true))
        assertEquals(3,MarbleHomeNavigation.next(6,MarbleDirection.DOWN,true,3))
    }
}
