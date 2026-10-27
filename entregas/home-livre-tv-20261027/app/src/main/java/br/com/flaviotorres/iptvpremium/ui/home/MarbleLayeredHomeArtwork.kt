package br.com.flaviotorres.iptvpremium.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle
import br.com.flaviotorres.iptvpremium.ui.theme.*
import kotlin.math.roundToInt

// Theme filters are retained; neutral charcoal surfaces stay neutral.
private val homeIconFilters by lazy { PremiumTheme.entries.associateWith(::marbleAccentFilter) }
private fun homeIconSaturationFilter(theme:PremiumTheme):ColorFilter? = homeIconFilters[theme]
private fun homeBackdrop(light:Boolean)=if(light) R.drawable.home_free_pearl else R.drawable.home_free_graphite

internal data class MarbleHomeLayers(val backdrop:ImageBitmap,val icons:List<ImageBitmap>,val assets:List<MarblePngAsset>)

private fun cachedMarbleHomeLayers(light:Boolean,theme:PremiumTheme):MarbleHomeLayers? {
    val backdrop=MarbleArtworkCache.peek(homeBackdrop(light)) ?: return null
    val assets=MarbleHomePngAssets.forMode(light,theme)
    val icons=assets.map { MarbleArtworkCache.peek(it.resource) }
    return if(icons.all { it!=null }) MarbleHomeLayers(backdrop,icons.filterNotNull(),assets)else null
}

/** Scenery and a shared seven-icon sheet decode on IO once, never per focus frame. */
@Composable
internal fun rememberMarbleHomeLayers():MarbleHomeLayers? {
    val light=CatalogStyle.light
    val theme=ThemeState.selected
    val context=LocalContext.current.applicationContext
    val epoch by ThemeArtworkEpoch.collectAsState()
    var layers by remember(light,theme,epoch){mutableStateOf(cachedMarbleHomeLayers(light,theme))}
    LaunchedEffect(light,theme,epoch) {
        val backdrop=MarbleArtworkCache.load(context,homeBackdrop(light))
        val assets=MarbleHomePngAssets.forMode(light,theme)
        val icons=assets.map{MarbleArtworkCache.load(context,it.resource)}
        if(backdrop!=null&&icons.all{it!=null})
            layers=MarbleHomeLayers(backdrop,icons.filterNotNull(),assets)
    }
    return layers
}
/** Mode-specific artwork already contains its finished inner surfaces; no runtime masks. */
private fun DrawScope.drawClassicIcon(image:ImageBitmap,asset:MarblePngAsset,
    target:Rect,tint:ColorFilter?,opacity:Float=1f) {
    val rx=image.width.toFloat()/asset.width;val ry=image.height.toFloat()/asset.height
    val offset=IntOffset((asset.crop.left*rx).roundToInt(),(asset.crop.top*ry).roundToInt())
    val sourceSize=IntSize((asset.crop.width*rx).roundToInt().coerceAtLeast(1),(asset.crop.height*ry).roundToInt().coerceAtLeast(1))
    val destination=IntOffset(target.left.roundToInt(),target.top.roundToInt())
    val destinationSize=IntSize(target.width.roundToInt().coerceAtLeast(1),target.height.roundToInt().coerceAtLeast(1))
    drawImage(image,srcOffset=offset,srcSize=sourceSize,dstOffset=destination,dstSize=destinationSize,
        colorFilter=tint,alpha=opacity,filterQuality=FilterQuality.High)
}
private fun DrawScope.drawPng(layers:MarbleHomeLayers,index:Int,tint:ColorFilter?,sx:Float,sy:Float,
    light:Boolean,alpha:Float=1f) {
    val target=MarbleHomeGeometry.icons[index]
    val crop=layers.assets[index].crop
    val slot=Rect(target.left*sx,target.top*sy,target.right*sx,target.bottom*sy)
    val ratio=minOf(slot.width/crop.width,slot.height/crop.height)
    val width=crop.width*ratio;val height=crop.height*ratio
    val left=slot.center.x-width*.5f
    drawClassicIcon(layers.icons[index],layers.assets[index],
        Rect(left,slot.bottom-height,left+width,slot.bottom),tint,alpha)
}

/** Fixed architectural background; only alpha cutouts and their own reflections move. */
@Composable
internal fun MarbleLayeredHomePicture(layers:MarbleHomeLayers,focused:Int?,enlargement:Float,normalEnabled:Boolean,modifier:Modifier=Modifier) {
    val theme=ThemeState.selected
    val light=CatalogStyle.light
    val filter=homeIconSaturationFilter(theme)
    val backdropFilter=remember(theme,light){marbleAccentFilter(theme)}
    BoxWithConstraints(modifier) {
        val pictureWidth=maxWidth;val pictureHeight=maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val full=IntSize(size.width.roundToInt(),size.height.roundToInt())
            val sx=size.width/MarbleHomeGeometry.width;val sy=size.height/MarbleHomeGeometry.height
            drawImage(layers.backdrop,dstSize=full,colorFilter=backdropFilter,filterQuality=FilterQuality.High)
            for(index in 0..4) {
                val r=MarbleHomeGeometry.icons[index]
                val pivot=Offset((r.left+r.right)*.5f*sx,r.bottom*sy)
                val opacity=if(!normalEnabled&&index<3).45f else 1f
                // Independent soft contact shadow and vertically compressed floor reflection.
                val floor=Rect((r.left-20)*sx,(r.bottom+2)*sy,(r.right+20)*sx,(r.bottom+125)*sy)
                drawContext.canvas.saveLayer(floor,Paint())
                clipRect(floor.left,floor.top,floor.right,floor.bottom) {
                    scale(1f,-.42f,pivot) {drawPng(layers,index,filter,sx,sy,light,(if(light).20f else .28f)*opacity)}
                    drawRect(Brush.verticalGradient(0f to Color.Black,.15f to Color.Black,.9f to Color.Transparent,startY=floor.top,endY=floor.bottom),floor.topLeft,floor.size,blendMode=BlendMode.DstIn)
                }
                drawContext.canvas.restore()
                drawOval(Color.Black.copy(alpha=(if(light).12f else .26f)*opacity),
                    Offset((r.left+r.width*.08f)*sx,(r.bottom-3)*sy),Size(r.width*.84f*sx,8*sy))
            }
        }
        // Independent layers: scale cached artwork about its centre. Pixel crop/position,
        // captions, contact shadows and reflections stay fixed throughout the animation.
        MarbleHomeGeometry.icons.forEachIndexed { index, slot ->
            val crop = layers.assets[index].crop
            val slotWidth = pictureWidth * (slot.width.toFloat() / MarbleHomeGeometry.width)
            val slotHeight = pictureHeight * (slot.height.toFloat() / MarbleHomeGeometry.height)
            val ratio = minOf(slotWidth.value / crop.width, slotHeight.value / crop.height)
            val width = (crop.width * ratio).dp
            val height = (crop.height * ratio).dp
            val left = pictureWidth * ((slot.left + slot.right) * .5f / MarbleHomeGeometry.width) - width * .5f
            val top = pictureHeight * (slot.bottom.toFloat() / MarbleHomeGeometry.height) - height
            val zoom = animateFloatAsState(if (focused == index) 1.10f else 1f, tween(180), label = "home-centred-$index")
            MarblePngIcon(layers, index, Modifier.offset(left, top).size(width, height).graphicsLayer {
                scaleX = zoom.value; scaleY = zoom.value
                transformOrigin = TransformOrigin.Center
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = if (!normalEnabled && index < 3) .45f else 1f
            })
        }
        // Captions remain at their native size while only the artwork grows.
        MarbleHomeGeometry.captions.forEachIndexed{index,r->
            Box(Modifier.offset(pictureWidth*(r.left.toFloat()/MarbleHomeGeometry.width),pictureHeight*(r.top.toFloat()/MarbleHomeGeometry.height))
                .size(pictureWidth*(r.width.toFloat()/MarbleHomeGeometry.width),pictureHeight*(r.height.toFloat()/MarbleHomeGeometry.height)),contentAlignment=Alignment.Center) {
                Text(MarbleHomeGeometry.labels[index].uppercase(),fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,
                    fontSize=(26f*pictureHeight.value/MarbleHomeGeometry.height).sp,
                    letterSpacing=(3.2f*pictureHeight.value/MarbleHomeGeometry.height).sp,
                    color=(if(light)Color(0xFF202124)else Color(0xFFF4F4F5)).copy(alpha=if(!normalEnabled&&index<3).45f else 1f),maxLines=1)
            }
        }
    }
}

@Composable
internal fun MarblePngIcon(layers:MarbleHomeLayers,index:Int,modifier:Modifier) {
    val image=layers.icons[index]
    val asset=layers.assets[index]
    val light=CatalogStyle.light
    val filter=homeIconSaturationFilter(ThemeState.selected)
    val painter=remember(image,asset,ThemeState.selected,light){object:Painter(){
        override val intrinsicSize=Size(asset.crop.width.toFloat(),asset.crop.height.toFloat())
        override fun DrawScope.onDraw(){
            drawClassicIcon(image,asset,Rect(0f,0f,size.width,size.height),filter)
        }
    }}
    Image(painter,null,modifier,contentScale=ContentScale.Fit)
}
