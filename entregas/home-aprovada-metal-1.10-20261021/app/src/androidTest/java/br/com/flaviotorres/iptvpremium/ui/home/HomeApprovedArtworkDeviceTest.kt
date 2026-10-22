package br.com.flaviotorres.iptvpremium.ui.home

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import br.com.flaviotorres.iptvpremium.ui.theme.MarbleArtworkCache
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class HomeApprovedArtworkDeviceTest {
    @Test fun bothModesKeepTheirNeutralInteriorsWhenMetalChangesTheme() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        MarbleArtworkCache.clear()
        try {
            for(light in listOf(true,false)) {
                val asset=MarbleHomePngAssets.forMode(light).first()
                val original=MarbleArtworkCache.loadHomeIcon(context,asset.resource,PremiumTheme.GOLD)!!.asAndroidBitmap()
                for(theme in listOf(PremiumTheme.BLUE,PremiumTheme.PINK,PremiumTheme.PURPLE,PremiumTheme.RED)) {
                    val tinted=MarbleArtworkCache.loadHomeIcon(context,asset.resource,theme)!!.asAndroidBitmap()
                    var samples=0
                    for(y in original.height/4 until original.height*2/3 step 9)
                        for(x in original.width/4 until original.width*3/4 step 9) {
                            val a=original.getPixel(x,y);val red=(a ushr 16)and 255;val green=(a ushr 8)and 255;val blue=a and 255
                            if(a ushr 24<250||red !in 50..215||maxOf(red,green,blue)-minOf(red,green,blue)>12)continue
                            val b=tinted.getPixel(x,y)
                            assertEquals(a ushr 24,b ushr 24)
                            for(shift in listOf(0,8,16))assertTrue(abs(((a ushr shift)and 255)-((b ushr shift)and 255))<=1)
                            samples++
                        }
                    assertTrue("Neutral interior samples in $theme / light=$light",samples>20)
                }
            }
        }finally{MarbleArtworkCache.clear()}
    }
    @Test fun cachedMetalVariantsNeverMutateOriginalArtwork() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        MarbleArtworkCache.clear()
        try {
            val asset=MarbleHomePngAssets.forMode(true)[1]
            val original=MarbleArtworkCache.loadHomeIcon(context,asset.resource,PremiumTheme.GOLD)!!.asAndroidBitmap()
            val before=IntArray(original.width*original.height)
            original.getPixels(before,0,original.width,0,0,original.width,original.height)
            val first=MarbleArtworkCache.loadHomeIcon(context,asset.resource,PremiumTheme.BLUE)
            val second=MarbleArtworkCache.loadHomeIcon(context,asset.resource,PremiumTheme.BLUE)
            assertNotNull(first);assertSame(first,second)
            val after=IntArray(original.width*original.height)
            original.getPixels(after,0,original.width,0,0,original.width,original.height)
            assertArrayEquals(before,after)
        }finally{MarbleArtworkCache.clear()}
    }
}
