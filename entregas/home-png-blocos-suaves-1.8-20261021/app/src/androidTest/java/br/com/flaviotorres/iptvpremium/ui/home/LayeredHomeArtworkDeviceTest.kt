package br.com.flaviotorres.iptvpremium.ui.home

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import br.com.flaviotorres.iptvpremium.R
import org.junit.Assert.*
import org.junit.Test

class LayeredHomeArtworkDeviceTest {
    @Test fun fiveSeparateCutoutsHaveRealTransparencyAndValidBounds() {
        val resources=InstrumentationRegistry.getInstrumentation().targetContext.resources
        assertEquals(5,MarbleHomePngAssets.icons.size)
        MarbleHomePngAssets.icons.forEach { asset ->
            val bitmap=BitmapFactory.decodeResource(resources,asset.resource,BitmapFactory.Options().apply{inScaled=false})
            assertNotNull(bitmap)
            try {
                assertEquals(asset.width,bitmap.width);assertEquals(asset.height,bitmap.height)
                assertTrue(bitmap.hasAlpha());assertEquals(0,Color.alpha(bitmap.getPixel(0,0)))
                assertTrue(asset.crop.left>=0&&asset.crop.top>=0&&asset.crop.right<=bitmap.width&&asset.crop.bottom<=bitmap.height)
                assertTrue(Color.alpha(bitmap.getPixel((asset.crop.left+asset.crop.right)/2,(asset.crop.top+asset.crop.bottom)/2))>0)
            } finally {bitmap.recycle()}
        }
    }
    @Test fun cleanBackdropsFitTheOriginalGeometryAndHomeDecodeBudget() {
        val resources=InstrumentationRegistry.getInstrumentation().targetContext.resources
        for(id in listOf(R.drawable.marble_home_layer_black,R.drawable.marble_home_layer_white)) {
            val options=BitmapFactory.Options().apply{inJustDecodeBounds=true;inScaled=false}
            BitmapFactory.decodeResource(resources,id,options)
            assertEquals(MarbleHomeGeometry.width,options.outWidth);assertEquals(MarbleHomeGeometry.height,options.outHeight)
        }
        val decoded=MarbleHomeGeometry.width.toLong()*MarbleHomeGeometry.height*4+
            MarbleHomePngAssets.icons.sumOf{((it.width+1)/2).toLong()*((it.height+1)/2)*4}
        assertTrue("Home bitmap budget: $decoded",decoded<=18L*1024*1024)
        assertEquals(5,MarbleHomeGeometry.captions.size)
    }
}
