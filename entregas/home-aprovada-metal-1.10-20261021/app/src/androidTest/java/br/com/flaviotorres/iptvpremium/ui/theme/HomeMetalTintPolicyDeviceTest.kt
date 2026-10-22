package br.com.flaviotorres.iptvpremium.ui.theme

import org.junit.Assert.*
import org.junit.Test

class HomeMetalTintPolicyDeviceTest {
    private val blue=floatArrayOf(-.17f,0f,1.17f,0f,0f, .12f,0f,.88f,0f,0f, 1f,0f,0f,0f,0f, 0f,0f,0f,1f,0f)
    private val red=floatArrayOf(1f,0f,0f,0f,0f, -.17f,0f,1.17f,0f,0f, -.17f,0f,1.17f,0f,0f, 0f,0f,0f,1f,0f)
    @Test fun approvedPearlAndGrayInteriorsRemainExactlyUnchanged() {
        for(matrix in listOf(blue,red))for(rgb in listOf(0xc6c4be,0x7b7e82,0x55585b,0xd1c7bb,0x202020,0x11100f))
            for(alpha in listOf(1,128,255)) {
                val pixel=(alpha shl 24)or rgb
                assertEquals(pixel,homeMetalTintPixel(pixel,matrix))
            }
    }
    @Test fun transparentPixelsStayExactlyUnchanged() {
        assertEquals(0x00daa54a,homeMetalTintPixel(0x00daa54a,blue))
    }
    @Test fun warmGoldChangesHueWithoutChangingOpacity() {
        val source=0x80daa54a.toInt();val changed=homeMetalTintPixel(source,blue)
        assertNotEquals(source,changed);assertEquals(source ushr 24,changed ushr 24)
        assertTrue((changed and 255)>((changed ushr 16)and 255))
        val ruby=homeMetalTintPixel(source,red)
        assertTrue(((ruby ushr 16)and 255)>(ruby and 255))
    }
    @Test fun otherColoredMaterialsAreNeverMistakenForGold() {
        for(pixel in listOf(0xff2460bd,0xffab3070,0xff3038ab,0xff446a65))
            assertEquals(pixel.toInt(),homeMetalTintPixel(pixel.toInt(),blue))
    }
    @Test fun transitionsBlendColorWithoutIntroducingOpacityEdges() {
        val source=0xffc8b492.toInt();val changed=homeMetalTintPixel(source,blue)
        assertNotEquals(source,changed);assertEquals(255,changed ushr 24)
        assertTrue(((changed ushr 16)and 255)in 0..200)
        assertTrue((changed and 255)in 146..200)
    }
}
