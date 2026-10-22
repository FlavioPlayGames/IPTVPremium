package br.com.flaviotorres.iptvpremium.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35],qualifiers="w1672dp-h941dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class HomeRealArtworkPreviewTest {
    @get:Rule val compose=createComposeRule()
    @Test fun renderDarkWithProvidedBackdrop()=render(false)
    @Test fun renderLightWithProvidedBackdrop()=render(true)
    private fun render(light:Boolean) {
        Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).eraseColor(android.graphics.Color.BLACK)
        val context=ApplicationProvider.getApplicationContext<Context>()
        KidsAppearance.apply(false,"blue");ThemeState.choose(context,PremiumTheme.GOLD)
        CatalogStyle.choose(context,if(light)CatalogBackground.WHITE else CatalogBackground.BLACK)
        BitmapFactory.decodeResource(context.resources,R.drawable.home_original_gold_white,BitmapFactory.Options().apply{inScaled=false})!!.recycle()
        val assets=MarbleHomePngAssets.forMode(light,PremiumTheme.GOLD)
        val layers=runBlocking {
            MarbleArtworkCache.clear()
            val backdrop=MarbleArtworkCache.load(context,if(light)R.drawable.marble_home_layer_white else R.drawable.marble_home_layer_black)!!
            val icons=assets.map{MarbleArtworkCache.load(context,it.resource)!!}
            assertSame(icons[0],icons[4])
            MarbleHomeLayers(backdrop,icons,assets)
        }
        lateinit var view:android.view.View
        compose.setContent {
            view=LocalView.current
            MarbleLayeredHomePicture(layers,null,1f,true,Modifier.requiredSize(1672.dp,941.dp))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val screenshot=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(screenshot))
            java.io.File(System.getProperty("java.io.tmpdir"),"home-hq-render-${if(light)"light" else "dark"}.png").outputStream().use{
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,it))
            }
        }
    }
}
