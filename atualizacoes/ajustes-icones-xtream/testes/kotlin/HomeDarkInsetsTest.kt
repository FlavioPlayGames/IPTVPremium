package br.com.flaviotorres.iptvpremium.ui.home

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeDarkInsetsTest {
    @Test fun darkNeutralInsetsLiftFifteenPercentWithoutRepaintingMetalOrAlpha()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        for(theme in PremiumTheme.entries) {
            val asset=MarbleHomePngAssets.forMode(false,theme).first()
            val original=BitmapFactory.decodeResource(context.resources,asset.resource,BitmapFactory.Options().apply{inScaled=false})!!
            MarbleArtworkCache.clear()
            val actual=MarbleArtworkCache.load(context,asset.resource)!!.asAndroidBitmap()
            val x=asset.crop.left+asset.crop.width/4;val y=asset.crop.top+asset.crop.height*40/100
            val before=original.getPixel(x,y);val after=actual.getPixel(x,y)
            for(shift in listOf(0,8,16))assertEquals(((before ushr shift and 255)*1.15f).roundToInt().toDouble(),(after ushr shift and 255).toDouble(),2.0)
            assertEquals(before ushr 24,after ushr 24)
            var checked=0
            for(py in 0 until original.height step 13)for(px in 0 until original.width step 17) {
                val p=original.getPixel(px,py);val r=p ushr 16 and 255;val g=p ushr 8 and 255;val b=p and 255
                if(maxOf(r,g,b)-minOf(r,g,b)>40 && p ushr 24>240) {
                    assertEquals("Metal colour changed: $theme",p,actual.getPixel(px,py));checked++
                }
            }
            assertTrue(checked>100)
            assertEquals(original.getPixel(0,0),actual.getPixel(0,0))
            original.recycle()
        }
    }
    @Test fun lightModePngDecodesWithoutAnyInsetChange()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        for(theme in PremiumTheme.entries) {
            val asset=MarbleHomePngAssets.forMode(true,theme).first()
            val original=BitmapFactory.decodeResource(context.resources,asset.resource,BitmapFactory.Options().apply{inScaled=false})!!
            MarbleArtworkCache.clear()
            val actual=MarbleArtworkCache.load(context,asset.resource)!!.asAndroidBitmap()
            assertTrue("Light artwork changed: $theme",original.sameAs(actual))
            original.recycle()
        }
    }
}
