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
                val options=BitmapFactory.Options().apply {inScaled=false;inSampleSize=if(id==R.drawable.marble_brand_gold)4 else if(wallpaper)2 else 1}
                BitmapFactory.decodeResource(context.resources,id,options)?.asImageBitmap()?.also {bitmaps.put(id,it)}
            } catch(_:OutOfMemoryError){clear();null}
        }
    }
}
internal fun marbleSceneAsset(light:Boolean)=if(light)R.drawable.marble_home_white else R.drawable.marble_home_black
internal fun marbleWallpaperAsset(light:Boolean)=if(light)R.drawable.marble_wallpaper_white else R.drawable.marble_wallpaper_black

/** Neutral marble and white reflections stay neutral; only the warm metal takes the theme hue. */
private fun marbleAccentMatrix(theme:PremiumTheme):FloatArray {
    if(theme==PremiumTheme.GOLD)return floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)
    if(theme==PremiumTheme.RED)return floatArrayOf(
        1f,0f,0f,0f,0f, -.17f,0f,1.17f,0f,0f, -.17f,0f,1.17f,0f,0f, 0f,0f,0f,1f,0f)
    if(theme==PremiumTheme.BLUE)return floatArrayOf(
        -.17f,0f,1.17f,0f,0f, .12f,0f,.88f,0f,0f, 1f,0f,0f,0f,0f, 0f,0f,0f,1f,0f)
    val tint=Color.hsv(theme.hue,if(theme==PremiumTheme.PINK).70f else .88f,.98f)
    return floatArrayOf(tint.red,0f,1f-tint.red,0f,0f,
        tint.green,0f,1f-tint.green,0f,0f, tint.blue,0f,1f-tint.blue,0f,0f, 0f,0f,0f,1f,0f)
}
/** The same gold material and colour transform used by the painted Home objects. */
internal fun marbleMetalColor(source:Long):Color {
    val original=Color(source)
    val m=marbleAccentMatrix(ThemeState.selected)
    fun channel(row:Int)=(m[row*5]*original.red+m[row*5+1]*original.green+m[row*5+2]*original.blue+m[row*5+4]/255f).coerceIn(0f,1f)
    return Color(channel(0),channel(1),channel(2),original.alpha)
}
internal fun marbleAccentFilter(theme:PremiumTheme):ColorFilter? =
    if(theme==PremiumTheme.GOLD)null else ColorFilter.colorMatrix(ColorMatrix(marbleAccentMatrix(theme)))

/** A small specular lift masked to the painted metal; the surrounding stone never moves. */
private fun marbleUtilityFocusFilter(theme:PremiumTheme):ColorFilter {
    val matrix=marbleAccentMatrix(theme)
    for(row in 0..2)for(column in 0..3)matrix[row*5+column]*=1.12f
    matrix[15]=4f;matrix[16]=0f;matrix[17]=-4f;matrix[18]=0f;matrix[19]=-48f
    return ColorFilter.colorMatrix(ColorMatrix(matrix))
}
/** Mask only the original caption ink, preserving its position and leaving the floor unchanged. */
private fun marbleCaptionFocusFilter(light:Boolean):ColorFilter {
    val tint=marbleMetalColor(if(light)0xFF95601F else 0xFFFFE8AA)
    val sign=if(light)-3f else 3f
    val offset=if(light)550f else -1650f
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        0f,0f,0f,0f,tint.red*255f,
        0f,0f,0f,0f,tint.green*255f,
        0f,0f,0f,0f,tint.blue*255f,
        sign,sign,sign,0f,offset)))
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
private class MarbleRegionPainter(private val image:ImageBitmap,private val region:MarbleRegion?,private val tint:ColorFilter?,private val focusTint:ColorFilter?=null,private val focusAmount:Float=0f):Painter() {
    override val intrinsicSize=if(region==null)Size(image.width.toFloat(),image.height.toFloat())else Size(region.width.toFloat(),region.height.toFloat())
    override fun DrawScope.onDraw() {
        val rect=region?:MarbleRegion(0,0,MarbleHomeGeometry.width,MarbleHomeGeometry.height)
        val sx=image.width.toFloat()/MarbleHomeGeometry.width
        val sy=image.height.toFloat()/MarbleHomeGeometry.height
        drawImage(image,srcOffset=IntOffset((rect.left*sx).roundToInt(),(rect.top*sy).roundToInt()),
            srcSize=IntSize((rect.width*sx).roundToInt().coerceAtLeast(1),(rect.height*sy).roundToInt().coerceAtLeast(1)),
            dstSize=IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1)),
            colorFilter=tint,filterQuality=FilterQuality.High)
        if(focusTint!=null&&focusAmount>0f)drawImage(image,
            srcOffset=IntOffset((rect.left*sx).roundToInt(),(rect.top*sy).roundToInt()),
            srcSize=IntSize((rect.width*sx).roundToInt().coerceAtLeast(1),(rect.height*sy).roundToInt().coerceAtLeast(1)),
            dstSize=IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1)),
            colorFilter=focusTint,alpha=focusAmount,filterQuality=FilterQuality.High)
    }
}
@Composable
internal fun MarbleSceneImage(modifier:Modifier=Modifier,region:MarbleRegion?=null,description:String?=null,focusAmount:Float=0f) {
    val bitmap=rememberMarbleBitmap()
    val theme=ThemeState.selected
    val painter=remember(bitmap,region,theme,focusAmount){bitmap?.let{MarbleRegionPainter(it,region,marbleAccentFilter(theme),marbleUtilityFocusFilter(theme),focusAmount)}?:androidx.compose.ui.graphics.painter.ColorPainter(Color.Transparent)}
    Image(painter,description,modifier,contentScale=ContentScale.Fit)
}
@Composable
internal fun MarbleCatalogWallpaper(modifier:Modifier=Modifier) {
    val bitmap=rememberMarbleBitmap(wallpaper=true)
    val filter=remember(ThemeState.selected){marbleAccentFilter(ThemeState.selected)}
    if(bitmap==null) androidx.compose.foundation.layout.Box(modifier.background(CatalogStyle.background))
    else Image(bitmap,null,modifier,colorFilter=filter,contentScale=ContentScale.Crop)
}

/** Focus only lights the original icon and caption ink; the surrounding marble never moves. */
@Composable
internal fun MarbleHomeCanvas(bitmap:ImageBitmap,focused:Int?,artworkScale:Float,normalEnabled:Boolean,modifier:Modifier) {
    val filter=remember(ThemeState.selected){marbleAccentFilter(ThemeState.selected)}
    val utilityFocus=remember(ThemeState.selected){marbleUtilityFocusFilter(ThemeState.selected)}
    val light=CatalogStyle.selected==CatalogBackground.WHITE
    val captionFocus=remember(ThemeState.selected,light){marbleCaptionFocusFilter(light)}
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
            val intensity=((artworkScale-1f)/.10f).coerceIn(0f,1f)
            val icon=MarbleHomeGeometry.icons[focused]
            clipRect(icon.left*sx,icon.top*sy,icon.right*sx,icon.bottom*sy) {
                drawImage(bitmap,dstSize=full,colorFilter=utilityFocus,alpha=intensity,filterQuality=FilterQuality.High)
            }
            if(focused<5) {
                val label=MarbleHomeGeometry.captions[focused]
                clipRect(label.left*sx,label.top*sy,label.right*sx,label.bottom*sy) {
                    drawImage(bitmap,dstSize=full,colorFilter=captionFocus,alpha=intensity,filterQuality=FilterQuality.High)
                }
            }
        }
    }
}

/** A real alpha PNG: the logo carries no rectangle of the former marble scene. */
@Composable
internal fun MarbleLogoImage(modifier:Modifier=Modifier,description:String?="IPTV Premium") {
    val context=LocalContext.current.applicationContext
    val epoch by ThemeArtworkEpoch.collectAsState()
    val asset=R.drawable.marble_brand_gold
    var image by remember(epoch){mutableStateOf(MarbleArtworkCache.peek(asset))}
    LaunchedEffect(epoch){image=MarbleArtworkCache.load(context,asset)}
    val filter=remember(ThemeState.selected){marbleAccentFilter(ThemeState.selected)}
    image?.let{Image(it,description,modifier,colorFilter=filter,contentScale=ContentScale.Fit)}
        ?:androidx.compose.foundation.layout.Box(modifier)
}
