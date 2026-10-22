package br.com.flaviotorres.iptvpremium.ui.components

import android.content.Context
import androidx.compose.ui.graphics.*
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.BuildConfig
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.ui.playlist.PlaylistGlass
import br.com.flaviotorres.iptvpremium.ui.theme.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Light111PaletteTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun sample(filter:ColorFilter?):Int {
        val bitmap=ImageBitmap(1,1)
        Canvas(bitmap).drawRect(0f,0f,1f,1f,Paint().apply{color=Color.White;colorFilter=filter})
        return bitmap.asAndroidBitmap().getPixel(0,0)
    }
    @Test fun lightPaletteIsSharedByCataloguesListsAndSettingsForEveryAccent() {
        KidsAppearance.apply(false,"blue");CatalogStyle.choose(context,CatalogBackground.WHITE)
        for(theme in PremiumTheme.entries) {
            ThemeState.choose(context,theme)
            val neutral=Color(0xFFCDCDD2)
            assertEquals(neutral,CatalogStyle.background);assertEquals(neutral,CatalogStyle.panel)
            assertEquals(neutral,MarbleScreenPalette.background);assertEquals(neutral,MarbleScreenPalette.control)
            assertEquals(neutral,PlaylistGlass.panel.copy(alpha=1f));assertEquals(neutral,PlaylistGlass.dialog.copy(alpha=1f))
            val pixel=sample(marbleBackdropFilter(theme,true))
            assertEquals(205.0,(pixel ushr 16 and 255).toDouble(),1.0)
            assertEquals(205.0,(pixel ushr 8 and 255).toDouble(),1.0)
            assertEquals(210.0,(pixel and 255).toDouble(),1.0)
            assertTrue(CatalogStyle.text.luminance()<.05f)
        }
    }
    @Test fun darkPaletteAndBackdropRemainUnchangedForEveryAccent() {
        KidsAppearance.apply(false,"blue");CatalogStyle.choose(context,CatalogBackground.BLACK)
        for(theme in PremiumTheme.entries) {
            ThemeState.choose(context,theme)
            assertEquals(Color(0xFF17191B),CatalogStyle.background)
            assertEquals(Color(0xFF1C1E20),CatalogStyle.panel)
            assertEquals(Color(0xFF24272A),CatalogStyle.categorySurface)
            assertEquals(sample(marbleAccentFilter(theme)),sample(marbleBackdropFilter(theme,false)))
        }
    }
    @Test fun kidsKeepsItsOwnColoursEvenWithLightSelected() {
        CatalogStyle.choose(context,CatalogBackground.WHITE)
        for(style in listOf("blue","pink")) {
            KidsAppearance.apply(true,style)
            assertFalse(CatalogStyle.light)
            assertEquals(ThemeColors.background,MarbleScreenPalette.background)
            assertEquals(ThemeColors.panel.copy(alpha=.60f),PlaylistGlass.panel)
        }
        KidsAppearance.apply(false,"blue")
    }
    @Test fun generatedVersionIsOneEleven() {
        assertEquals("1.11",BuildConfig.VERSION_NAME)
        assertTrue(BuildConfig.VERSION_CODE>=11)
    }
}
