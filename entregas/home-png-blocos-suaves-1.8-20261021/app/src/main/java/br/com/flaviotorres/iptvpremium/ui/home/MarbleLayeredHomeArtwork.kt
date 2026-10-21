package br.com.flaviotorres.iptvpremium.ui.home

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
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle
import br.com.flaviotorres.iptvpremium.ui.theme.*
import kotlin.math.roundToInt

internal data class MarbleHomeLayers(val backdrop:ImageBitmap,val icons:List<ImageBitmap>)

/** Background and five shared PNGs decode on IO once, never per focus frame. */
@Composable
internal fun rememberMarbleHomeLayers():MarbleHomeLayers? {
    val light=CatalogStyle.light
    val context=LocalContext.current.applicationContext
    val epoch by ThemeArtworkEpoch.collectAsState()
    var layers by remember(light,epoch){mutableStateOf<MarbleHomeLayers?>(null)}
    LaunchedEffect(light,epoch) {
        val backdrop=MarbleArtworkCache.load(context,if(light)R.drawable.marble_home_layer_white else R.drawable.marble_home_layer_black)
        val icons=MarbleHomePngAssets.icons.map{MarbleArtworkCache.load(context,it.resource)}
        if(backdrop!=null&&icons.all{it!=null})layers=MarbleHomeLayers(backdrop,icons.filterNotNull())
    }
    return layers
}
private fun DrawScope.drawPng(layers:MarbleHomeLayers,index:Int,tint:ColorFilter?,sx:Float,sy:Float,alpha:Float=1f) {
    val bitmap=layers.icons[index];val asset=MarbleHomePngAssets.icons[index];val target=MarbleHomeGeometry.icons[index]
    val rx=bitmap.width.toFloat()/asset.width;val ry=bitmap.height.toFloat()/asset.height
    drawImage(bitmap,
        srcOffset=IntOffset((asset.crop.left*rx).roundToInt(),(asset.crop.top*ry).roundToInt()),
        srcSize=IntSize((asset.crop.width*rx).roundToInt().coerceAtLeast(1),(asset.crop.height*ry).roundToInt().coerceAtLeast(1)),
        dstOffset=IntOffset((target.left*sx).roundToInt(),(target.top*sy).roundToInt()),
        dstSize=IntSize((target.width*sx).roundToInt().coerceAtLeast(1),(target.height*sy).roundToInt().coerceAtLeast(1)),
        colorFilter=tint,alpha=alpha,filterQuality=FilterQuality.High)
}

/** Fixed architectural background; only alpha cutouts and their own reflections move. */
@Composable
internal fun MarbleLayeredHomePicture(layers:MarbleHomeLayers,focused:Int?,enlargement:Float,normalEnabled:Boolean,modifier:Modifier=Modifier) {
    val theme=ThemeState.selected
    val filter=remember(theme){marbleAccentFilter(theme)}
    val utility=remember(theme){marbleUtilityFocusFilter(theme)}
    val light=CatalogStyle.light
    BoxWithConstraints(modifier) {
        val pictureWidth=maxWidth;val pictureHeight=maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val full=IntSize(size.width.roundToInt(),size.height.roundToInt())
            val sx=size.width/MarbleHomeGeometry.width;val sy=size.height/MarbleHomeGeometry.height
            drawImage(layers.backdrop,dstSize=full,colorFilter=filter,filterQuality=FilterQuality.High)
            for(index in 0..4) {
                val r=MarbleHomeGeometry.icons[index]
                val z=if(focused==index)enlargement else 1f
                val pivot=Offset((r.left+r.right)*.5f*sx,r.bottom*sy)
                val opacity=if(!normalEnabled&&index<3).45f else 1f
                // Independent soft contact shadow and vertically compressed floor reflection.
                val floor=Rect((r.left-10)*sx,(r.bottom+2)*sy,(r.right+10)*sx,(r.bottom+115)*sy)
                drawContext.canvas.saveLayer(floor,Paint())
                clipRect(floor.left,floor.top,floor.right,floor.bottom) {
                    scale(z,-.32f*z,pivot) {drawPng(layers,index,filter,sx,sy,.20f*opacity)}
                    drawRect(Brush.verticalGradient(0f to Color.Black,.15f to Color.Black,.9f to Color.Transparent,startY=floor.top,endY=floor.bottom),floor.topLeft,floor.size,blendMode=BlendMode.DstIn)
                }
                drawContext.canvas.restore()
                drawOval(Color.Black.copy(alpha=(if(light).12f else .26f)*opacity),
                    Offset((r.left+r.width*.08f)*sx,(r.bottom-3)*sy),Size(r.width*.84f*sx,8*sy))
                // The base remains on the floor; the former crop never enlarges any marble.
                scale(z,z,pivot) {drawPng(layers,index,filter,sx,sy,opacity)}
            }
            if(focused!=null&&focused>=5&&enlargement>1.001f) {
                val r=MarbleHomeGeometry.icons[focused]
                clipRect(r.left*sx,r.top*sy,r.right*sx,r.bottom*sy) {
                    drawImage(layers.backdrop,dstSize=full,colorFilter=utility,alpha=((enlargement-1)/.10f).coerceIn(0f,1f),filterQuality=FilterQuality.High)
                }
            }
        }
        // Native text is re-rasterised at its current size, never enlarged from baked pixels.
        MarbleHomeGeometry.captions.forEachIndexed{index,r->
            val z=if(focused==index)enlargement else 1f
            Box(Modifier.offset(pictureWidth*(r.left.toFloat()/MarbleHomeGeometry.width),pictureHeight*(r.top.toFloat()/MarbleHomeGeometry.height))
                .size(pictureWidth*(r.width.toFloat()/MarbleHomeGeometry.width),pictureHeight*(r.height.toFloat()/MarbleHomeGeometry.height)),contentAlignment=Alignment.Center) {
                Text(MarbleHomeGeometry.labels[index].uppercase(),fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,
                    fontSize=(26f*pictureHeight.value/MarbleHomeGeometry.height*z).sp,
                    letterSpacing=(3.2f*pictureHeight.value/MarbleHomeGeometry.height*z).sp,
                    color=(if(light)Color(0xFF202124)else Color(0xFFF4F4F5)).copy(alpha=if(!normalEnabled&&index<3).45f else 1f),maxLines=1)
            }
        }
    }
}

@Composable
internal fun MarblePngIcon(layers:MarbleHomeLayers,index:Int,modifier:Modifier) {
    val image=if(index<5)layers.icons[index]else layers.backdrop
    val asset=if(index<5)MarbleHomePngAssets.icons[index]else MarblePngAsset(0,MarbleHomeGeometry.width,MarbleHomeGeometry.height,MarbleHomeGeometry.icons[index])
    val filter=marbleAccentFilter(ThemeState.selected)
    val painter=remember(image,asset,ThemeState.selected){object:Painter(){
        override val intrinsicSize=Size(asset.crop.width.toFloat(),asset.crop.height.toFloat())
        override fun DrawScope.onDraw(){
            val rx=image.width.toFloat()/asset.width;val ry=image.height.toFloat()/asset.height
            drawImage(image,srcOffset=IntOffset((asset.crop.left*rx).roundToInt(),(asset.crop.top*ry).roundToInt()),
                srcSize=IntSize((asset.crop.width*rx).roundToInt().coerceAtLeast(1),(asset.crop.height*ry).roundToInt().coerceAtLeast(1)),
                dstSize=IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1)),colorFilter=filter,filterQuality=FilterQuality.High)
        }
    }}
    Image(painter,null,modifier,contentScale=ContentScale.Fit)
}
