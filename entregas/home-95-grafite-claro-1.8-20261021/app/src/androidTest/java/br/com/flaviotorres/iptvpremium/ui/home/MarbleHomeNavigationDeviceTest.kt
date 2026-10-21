package br.com.flaviotorres.iptvpremium.ui.home

import org.junit.Assert.*
import org.junit.Test

class MarbleHomeNavigationDeviceTest {
    @Test fun horizontalKeysFollowTheFiveVisibleIconsWithoutWrapping() {
        var index=0
        repeat(4){index=MarbleHomeNavigation.next(index,MarbleDirection.RIGHT,true)}
        assertEquals(4,index)
        assertEquals(4,MarbleHomeNavigation.next(index,MarbleDirection.RIGHT,true))
        repeat(4){index=MarbleHomeNavigation.next(index,MarbleDirection.LEFT,true)}
        assertEquals(0,index)
        assertEquals(0,MarbleHomeNavigation.next(index,MarbleDirection.LEFT,true))
    }
    @Test fun upReachesUtilitiesAndDownRestoresLastMainDestination() {
        assertEquals(5,MarbleHomeNavigation.next(0,MarbleDirection.UP,true))
        assertEquals(6,MarbleHomeNavigation.next(3,MarbleDirection.UP,true))
        assertEquals(5,MarbleHomeNavigation.next(6,MarbleDirection.LEFT,true))
        assertEquals(6,MarbleHomeNavigation.next(5,MarbleDirection.RIGHT,true))
        for(index in 0..4)assertEquals(index,MarbleHomeNavigation.next(6,MarbleDirection.DOWN,true,index))
    }
    @Test fun kidsOnlyNeverFocusesBlockedRegularDestinations() {
        assertEquals(3,MarbleHomeNavigation.initial(false))
        for(index in 3..6)for(direction in MarbleDirection.entries) {
            val result=MarbleHomeNavigation.next(index,direction,false,lastMain=1)
            assertTrue("$index $direction -> $result",MarbleHomeNavigation.enabled(result,false))
            assertTrue(result>=3)
        }
        assertFalse(MarbleHomeNavigation.enabled(0,false));assertFalse(MarbleHomeNavigation.enabled(1,false))
        assertFalse(MarbleHomeNavigation.enabled(2,false));assertTrue(MarbleHomeNavigation.enabled(3,false))
    }
    @Test fun artworkTargetsStayInsideFrameAndDoNotCoverNeighborActions() {
        val areas=MarbleHomeGeometry.hitAreas
        assertEquals(7,areas.size);assertEquals(7,MarbleHomeGeometry.labels.distinct().size)
        areas.forEach{assertTrue(it.left>=0&&it.top>=0&&it.width>0&&it.height>0)
            assertTrue(it.right<=MarbleHomeGeometry.width&&it.bottom<=MarbleHomeGeometry.height)}
        for(i in areas.indices)for(j in i+1 until areas.size) {
            val a=areas[i];val b=areas[j]
            assertTrue("Overlapping actions $i/$j",a.right<=b.left||b.right<=a.left||a.bottom<=b.top||b.bottom<=a.top)
        }
        MarbleHomeGeometry.icons.take(5).forEach{assertTrue("Focus crop includes caption",it.bottom<700)}
        val main=MarbleHomeGeometry.icons.take(5)
        val gaps=main.zipWithNext{a,b->b.left-a.right}
        assertTrue("Uniform space between PNGs",gaps.max()-gaps.min()<=1)
        assertTrue("Visible space remains when a neighbour grows",gaps.min()>=40)
        for(i in 0..4) {
            val icon=main[i];val hit=areas[i];val grow=icon.width*.05f
            assertTrue("Selected PNG fits its action $i",icon.left-grow>=hit.left&&icon.right+grow<=hit.right)
            assertTrue("Selected PNG stays below utilities",icon.bottom-icon.height*1.1f>=hit.top)
        }
        assertEquals(5,MarbleHomeGeometry.captions.size)
        for(i in 0..4) {
            val label=MarbleHomeGeometry.captions[i];val hit=areas[i];val icon=MarbleHomeGeometry.icons[i]
            assertTrue(label.left>=hit.left&&label.right<=hit.right&&label.top>=icon.bottom&&label.bottom<=hit.bottom)
        }
    }
    @Test fun invalidOrRemovedFocusReturnsToPermittedHomeDestination() {
        assertEquals(0,MarbleHomeNavigation.next(-1,MarbleDirection.LEFT,true))
        assertEquals(3,MarbleHomeNavigation.next(1,MarbleDirection.LEFT,false))
        assertEquals(3,MarbleHomeNavigation.next(5,MarbleDirection.DOWN,false,-99))
        assertFalse(MarbleHomeNavigation.enabled(7,true))
    }
}
