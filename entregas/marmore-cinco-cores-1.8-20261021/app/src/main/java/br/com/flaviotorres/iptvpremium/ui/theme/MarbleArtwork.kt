package br.com.flaviotorres.iptvpremium.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.components.CatalogBackground
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle
import br.com.flaviotorres.iptvpremium.ui.home.MarbleHomeGeometry
import br.com.flaviotorres.iptvpremium.ui.home.MarbleRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** One bounded cache for approved art, shared by Home, logo, settings preview and wallpaper. */
internal object MarbleArtworkCache {
    private val bitmaps=object:LruCache<Int,ImageBitmap>(8*1024) {
        override fun sizeOf(key:Int,value:ImageBitmap)=((value.asAndroidBitmap().allocationByteCount.toLong()+1023)/1024).toInt()
    }
    private val decode=Mutex()
    fun clear()=bitmaps.evictAll()
    fun peek(id:Int):ImageBitmap?=bitmaps.get(id)
    suspend fun load(context:Context,id:Int,wallpaper:Boolean=false):ImageBitmap?=withContext(Dispatchers.IO) {
        decode.withLock {
            peek(id)?:try {
                val options=BitmapFactory.Options().apply {inScaled=false;inSampleSize=if(wallpaper)2 else 1}
                BitmapFactory.decodeResource(context.resources,id,options)?.asImageBitmap()?.also {bitmaps.put(id,it)}
            } catch(_:OutOfMemoryError){clear();null}
        }
    }
}
internal fun marbleSceneAsset(light:Boolean)=if(light)R.drawable.marble_home_white else R.drawable.marble_home_black
internal fun marbleWallpaperAsset(light:Boolean)=if(light)R.drawable.marble_wallpaper_white else R.drawable.marble_wallpaper_black

/** Neutral marble and white reflections stay neutral; only the warm metal takes the theme hue. */
internal fun marbleAccentFilter(theme:PremiumTheme):ColorFilter? {
    if(theme==PremiumTheme.GOLD)return null
    val tint=Color.hsv(theme.hue,if(theme==PremiumTheme.PINK).70f else .88f,.98f)
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        tint.red,0f,1f-tint.red,0f,0f,
        tint.green,0f,1f-tint.green,0f,0f,
        tint.blue,0f,1f-tint.blue,0f,0f,
        0f,0f,0f,1f,0f)))
}
@Composable
internal fun rememberMarbleBitmap(wallpaper:Boolean=false):ImageBitmap? {
    val light=CatalogStyle.selected==CatalogBackground.WHITE
    val asset=if(wallpaper)marbleWallpaperAsset(light)else marbleSceneAsset(light)
    val context=LocalContext.current.applicationContext
    val epoch by ThemeArtworkEpoch.collectAsState()
    var image by remember(asset,epoch){mutableStateOf(MarbleArtworkCache.peek(asset))}
    LaunchedEffect(asset,epoch){image=MarbleArtworkCache.load(context,asset,wallpaper)}
    return image
}
private class MarbleRegionPainter(private val image:ImageBitmap,private val region:MarbleRegion?,private val tint:ColorFilter?):Painter() {
    override val intrinsicSize=if(region==null)Size(image.width.toFloat(),image.height.toFloat())else Size(region.width.toFloat(),region.height.toFloat())
    override fun DrawScope.onDraw() {
        val rect=region?:MarbleRegion(0,0,MarbleHomeGeometry.width,MarbleHomeGeometry.height)
        val sx=image.width.toFloat()/MarbleHomeGeometry.width
        val sy=image.height.toFloat()/MarbleHomeGeometry.height
        drawImage(image,srcOffset=IntOffset((rect.left*sx).roundToInt(),(rect.top*sy).roundToInt()),
            srcSize=IntSize((rect.width*sx).roundToInt().coerceAtLeast(1),(rect.height*sy).roundToInt().coerceAtLeast(1)),
            dstSize=IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1)),
            colorFilter=tint,filterQuality=FilterQuality.High)
    }
}
@Composable
internal fun MarbleSceneImage(modifier:Modifier=Modifier,region:MarbleRegion?=null,description:String?=null) {
    val bitmap=rememberMarbleBitmap()
    val theme=ThemeState.selected
    val painter=remember(bitmap,region,theme){bitmap?.let{MarbleRegionPainter(it,region,marbleAccentFilter(theme))}?:androidx.compose.ui.graphics.painter.ColorPainter(Color.Transparent)}
    Image(painter,description,modifier,contentScale=ContentScale.Fit)
}
@Composable
internal fun MarbleCatalogWallpaper(modifier:Modifier=Modifier) {
    val bitmap=rememberMarbleBitmap(wallpaper=true)
    val filter=remember(ThemeState.selected){marbleAccentFilter(ThemeState.selected)}
    if(bitmap==null) androidx.compose.foundation.layout.Box(modifier.background(CatalogStyle.background))
    else Image(bitmap,null,modifier,colorFilter=filter,contentScale=ContentScale.Crop)
}

/** Zoom the original painted icon without replacing its shape. Feather the surrounding stone. */
@Composable
internal fun MarbleHomeCanvas(bitmap:ImageBitmap,focused:Int?,artworkScale:Float,normalEnabled:Boolean,modifier:Modifier) {
    val filter=remember(ThemeState.selected){marbleAccentFilter(ThemeState.selected)}
    Canvas(modifier) {
        val full=IntSize(size.width.roundToInt(),size.height.roundToInt())
        drawImage(bitmap,dstSize=full,colorFilter=filter,filterQuality=FilterQuality.High)
        val sx=size.width/MarbleHomeGeometry.width
        val sy=size.height/MarbleHomeGeometry.height
        if(!normalEnabled) for(index in 0..2) {
            val r=MarbleHomeGeometry.hitAreas[index]
            drawRect(if(CatalogStyle.selected==CatalogBackground.WHITE)Color.White.copy(alpha=.55f)else Color.Black.copy(alpha=.55f),
                Offset(r.left*sx,r.top*sy),Size(r.width*sx,r.height*sy))
        }
        if(focused!=null&&artworkScale>1.001f) {
            val r=MarbleHomeGeometry.icons[focused]
            val center=Offset((r.left+r.right)*.5f*sx,(r.top+r.bottom)*.5f*sy)
            val bounds=Rect(r.left*sx-12*sx,r.top*sy-12*sy,r.right*sx+12*sx,r.bottom*sy+12*sy)
            val paint=Paint()
            drawContext.canvas.saveLayer(bounds.inflate(24*sx),paint)
            try {
                scale(artworkScale,artworkScale,center) {
                    clipRect(bounds.left,bounds.top,bounds.right,bounds.bottom) {
                        drawImage(bitmap,dstSize=full,colorFilter=filter,filterQuality=FilterQuality.High)
                    }
                }
                // Edge feathering avoids a hard rectangular patch in the marble around the icon.
                val expanded=bounds.inflate(16*sx)
                drawRect(Brush.horizontalGradient(0f to Color.Transparent,.04f to Color.Black,.96f to Color.Black,1f to Color.Transparent,
                    startX=expanded.left,endX=expanded.right),expanded.topLeft,expanded.size,blendMode=BlendMode.DstIn)
                drawRect(Brush.verticalGradient(0f to Color.Transparent,.04f to Color.Black,.96f to Color.Black,1f to Color.Transparent,
                    startY=expanded.top,endY=expanded.bottom),expanded.topLeft,expanded.size,blendMode=BlendMode.DstIn)
            } finally {drawContext.canvas.restore()}
        }
    }
}
