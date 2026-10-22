package br.com.flaviotorres.iptvpremium.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import br.com.flaviotorres.iptvpremium.ui.theme.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w960dp-h540dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeUtilityZoomTest {
    @get:Rule val compose=createComposeRule()
    @Test fun bothUtilityGlyphsEnlargeWithoutChangingColourOrTheOtherControl() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        KidsAppearance.apply(false,"blue")
        ThemeState.choose(context,PremiumTheme.GOLD)
        CatalogStyle.choose(context,CatalogBackground.BLACK)
        val backdrop=Bitmap.createBitmap(1672,941,Bitmap.Config.ARGB_8888)
        backdrop.eraseColor(AndroidColor.BLACK)
        val color=AndroidColor.rgb(180,120,40)
        for(i in 5..6) {
            val r=MarbleHomeGeometry.icons[i]
            for(y in r.top+12 until r.bottom-12) for(x in r.left+12 until r.right-12) backdrop.setPixel(x,y,color)
        }
        val transparent=ImageBitmap(1,1)
        val layers=MarbleHomeLayers(backdrop.asImageBitmap(),List(5){transparent},List(5){MarblePngAsset(0,1,1,MarbleRegion(0,0,1,1))})
        val focus=mutableStateOf<Int?>(null)
        lateinit var view:android.view.View
        compose.setContent {view=LocalView.current;MarbleLayeredHomePicture(layers,focus.value,1.10f,true,Modifier.requiredSize(960.dp,540.dp))}
        fun shot():Bitmap {
            compose.waitForIdle()
            lateinit var bitmap:Bitmap
            compose.runOnIdle {
                bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
            }
            return bitmap
        }
        fun bounds(bitmap:Bitmap,index:Int):List<Int> {
            val r=MarbleHomeGeometry.icons[index];val sx=bitmap.width/1672f;val sy=bitmap.height/941f
            val xs=mutableListOf<Int>();val ys=mutableListOf<Int>()
            for(y in ((r.top-10)*sy).toInt() until ((r.bottom+10)*sy).toInt())
                for(x in ((r.left-10)*sx).toInt() until ((r.right+10)*sx).toInt()) {
                    val p=bitmap.getPixel(x,y)
                    if(AndroidColor.red(p)>140 && AndroidColor.green(p)>90 && AndroidColor.blue(p)<65) {xs+=x;ys+=y}
                }
            return listOf(xs.min(),ys.min(),xs.max(),ys.max())
        }
        val before=shot()
        for(index in 5..6) {
            compose.runOnIdle{focus.value=index};compose.waitForIdle()
            val after=shot();val old=bounds(before,index);val enlarged=bounds(after,index)
            assertTrue(enlarged[2]-enlarged[0]>old[2]-old[0])
            assertTrue(enlarged[3]-enlarged[1]>old[3]-old[1])
            assertEquals(color,after.getPixel((enlarged[0]+enlarged[2])/2,(enlarged[1]+enlarged[3])/2))
            val other=if(index==5)6 else 5
            assertEquals(bounds(before,other),bounds(after,other))
        }
    }
}
