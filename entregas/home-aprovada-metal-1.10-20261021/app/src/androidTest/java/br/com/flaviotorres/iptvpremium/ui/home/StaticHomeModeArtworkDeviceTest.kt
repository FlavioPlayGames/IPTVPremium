package br.com.flaviotorres.iptvpremium.ui.home

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class StaticHomeModeArtworkDeviceTest {
    @Test fun eachModeUsesItsOwnFiveFinishedTransparentImages() {
        val resources=InstrumentationRegistry.getInstrumentation().targetContext.resources
        val light=MarbleHomePngAssets.forMode(true)
        val dark=MarbleHomePngAssets.forMode(false)
        assertEquals(5,light.size);assertEquals(5,dark.size)
        assertEquals(10,(light+dark).map{it.resource}.toSet().size)
        for(asset in light+dark) {
            val bitmap=BitmapFactory.decodeResource(resources,asset.resource,BitmapFactory.Options().apply{inScaled=false})
            assertNotNull(bitmap)
            try {
                assertEquals(asset.width,bitmap.width);assertEquals(asset.height,bitmap.height)
                assertTrue(bitmap.hasAlpha());assertEquals(0,Color.alpha(bitmap.getPixel(0,0)))
                assertTrue(asset.crop.left>=0&&asset.crop.top>=0&&asset.crop.right<=bitmap.width&&asset.crop.bottom<=bitmap.height)
                assertTrue(Color.alpha(bitmap.getPixel((asset.crop.left+asset.crop.right)/2,(asset.crop.top+asset.crop.bottom)/2))>200)
            }finally{bitmap.recycle()}
        }
    }
    @Test fun bothModesFitSharedDecodeBudgetAndKeepTheApprovedSlots() {
        for(light in listOf(false,true)) {
            val decoded=MarbleHomeGeometry.width.toLong()*MarbleHomeGeometry.height*4+
                MarbleHomePngAssets.forMode(light).sumOf{((it.width+1)/2).toLong()*((it.height+1)/2)*4}
            assertTrue("Home bitmap budget: $decoded",decoded<=18L*1024*1024)
        }
        assertEquals(5,MarbleHomeGeometry.captions.size)
        assertEquals(listOf("Canais","Filmes","Séries","Kids","Listas"),MarbleHomeGeometry.labels.take(5))
    }
}
