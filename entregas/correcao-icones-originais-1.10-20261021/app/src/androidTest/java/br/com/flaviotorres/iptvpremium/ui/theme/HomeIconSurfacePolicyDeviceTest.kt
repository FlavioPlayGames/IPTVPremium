package br.com.flaviotorres.iptvpremium.ui.theme

import org.junit.Assert.*
import org.junit.Test

class HomeIconSurfacePolicyDeviceTest {
    @Test fun transparentPixelsStayTransparent() {
        assertEquals(0, homeIconSurfaceMaskAlpha(0x00303030))
    }
    @Test fun neutralSurfacesRetainTheirAlpha() {
        assertEquals(255, homeIconSurfaceMaskAlpha(0xff101010.toInt()))
        assertEquals(128, homeIconSurfaceMaskAlpha(0x80404040.toInt()))
    }
    @Test fun colouredMetalAndWarmShadowsAreExcluded() {
        for (pixel in listOf(0xffffbf40, 0xff201a14, 0xff153aba, 0xffac2070, 0xff9b24da, 0xffc02020))
            assertEquals(0, homeIconSurfaceMaskAlpha(pixel.toInt()))
    }
    @Test fun neutralFringeFadesBeforeChromaticMetal() {
        val neutral = homeIconSurfaceMaskAlpha(0xff303030.toInt())
        val fringe = homeIconSurfaceMaskAlpha(0xff303038.toInt())
        assertTrue(fringe in 1 until neutral)
        assertEquals(0, homeIconSurfaceMaskAlpha(0xff303040.toInt()))
    }
    @Test fun maskNeverIncreasesSourceOpacity() {
        for (alpha in 0..255) for (blue in 48..68) {
            val mask = homeIconSurfaceMaskAlpha((alpha shl 24) or 0x303000 or blue)
            assertTrue(mask in 0..alpha)
        }
    }
}
