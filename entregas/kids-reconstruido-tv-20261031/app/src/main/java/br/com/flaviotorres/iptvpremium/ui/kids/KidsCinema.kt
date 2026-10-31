package br.com.flaviotorres.iptvpremium.ui.kids

import android.content.Context
import br.com.flaviotorres.iptvpremium.ui.components.posterSelectionFrame
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Native mascot artwork and the existing focus atlas share a bounded 32 MiB decoded pool. */
private object KidsCinemaArt {
    private val cache=object:LruCache<Int,ImageBitmap>(32*1024) {
        override fun sizeOf(key:Int,value:ImageBitmap)=((value.width.toLong()*value.height*4+1023)/1024).toInt()
    }
    private val decode=Semaphore(1)
    private val homeIds=setOf(R.drawable.kids_cloud_channels,R.drawable.kids_cloud_movies,
        R.drawable.kids_cloud_series)
    private val homeOwners=hashSetOf<Any>()
    @Synchronized fun retainHome():()->Unit {
        val owner=Any();homeOwners.add(owner)
        return { releaseHome(owner) }
    }
    @Synchronized private fun releaseHome(owner:Any) {
        if(homeOwners.remove(owner) && homeOwners.isEmpty())homeIds.forEach {cache.remove(it)}
    }
    @Synchronized private fun cacheDecoded(id:Int,image:ImageBitmap) {
        if(id !in homeIds || homeOwners.isNotEmpty())cache.put(id,image)
    }
    fun peek(id:Int)=cache.get(id)
    suspend fun load(context:Context,id:Int):ImageBitmap? {
        peek(id)?.let{return it}
        return withContext(Dispatchers.Default) { decode.withPermit {
            peek(id) ?: try {
                val options=BitmapFactory.Options().apply {
                    inScaled=false
                    // Mascots retain native pixels; only the smaller logo is sampled.
                    inPreferredConfig=android.graphics.Bitmap.Config.ARGB_8888
                    inSampleSize=if(id==R.drawable.kids_cloud_brand)2 else 1
                }
                BitmapFactory.decodeResource(context.resources,id,options)
                    ?.also{it.prepareToDraw()}?.asImageBitmap()?.also{cacheDecoded(id,it)}
            } catch(_:OutOfMemoryError) { cache.evictAll();null }
        } }
    }
}
@Composable
private fun rememberCinemaArt(id:Int):ImageBitmap? {
    val context=LocalContext.current.applicationContext
    var image by remember(id){mutableStateOf(KidsCinemaArt.peek(id))}
    LaunchedEffect(id) { if(image==null)image=KidsCinemaArt.load(context,id) }
    return image
}
private data class CinemaCrop(val x:Int,val y:Int,val width:Int,val height:Int)
private class CinemaSpritePainter(private val image:ImageBitmap,private val crop:CinemaCrop):Painter() {
    override val intrinsicSize=Size(crop.width.toFloat(),crop.height.toFloat())
    override fun DrawScope.onDraw() {
        drawImage(image,srcOffset=IntOffset(crop.x,crop.y),srcSize=IntSize(crop.width,crop.height),
            dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)),filterQuality=FilterQuality.High)
    }
}
private fun standaloneCrop(id:Int,image:ImageBitmap):CinemaCrop {
    val bounds=when(id) {
        R.drawable.kids_cloud_channels -> intArrayOf(39,87,1205,1179)
        R.drawable.kids_cloud_movies -> intArrayOf(37,29,1151,1206)
        R.drawable.kids_cloud_series -> intArrayOf(27,121,1228,1112)
        else -> intArrayOf(58,45,1205,1237)
    }
    val x=bounds[0]*image.width/1254;val y=bounds[1]*image.height/1254
    return CinemaCrop(x,y,(bounds[2]*image.width/1254-x).coerceAtLeast(1),
        (bounds[3]*image.height/1254-y).coerceAtLeast(1))
}
@Composable
private fun standalonePainter(id:Int):Painter {
    val image=rememberCinemaArt(id)
    return remember(image,id){image?.let {CinemaSpritePainter(it,standaloneCrop(id,it))} ?: ColorPainter(Color.Transparent)}
}
internal val kidsCinemaText get()=if(KidsAppearance.style==KidsStyle.BLUE)Color(0xFF172E50)else Color(0xFF45233E)
internal fun kidsCinemaInk(original:Long):Color=if(!KidsAppearance.active)
    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(original)
    else kidsCinemaText.copy(alpha=Color(original).alpha)
internal fun kidsCinemaPanelBrush():Brush=SolidColor(Color.Transparent)

/** Static vector background is independent from all four transparent foreground assets. */
@Composable
internal fun KidsCinemaBackdrop(style:KidsStyle=KidsAppearance.style,modifier:Modifier=Modifier,catalog:Boolean=false) {
    val image=rememberCinemaArt(if(style==KidsStyle.BLUE)R.drawable.kids_sky_blue else R.drawable.kids_sky_pink)
    Box(modifier.background(if(style==KidsStyle.BLUE)Color(0xFFCEE8FF)else Color(0xFFFFD6E8))) {
        image?.let {Image(it,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,filterQuality=FilterQuality.High)}
    }
}
@Composable
internal fun KidsCinemaBrand(modifier:Modifier=Modifier) {
    Image(standalonePainter(R.drawable.kids_cloud_brand),"IPTV Premium Kids",modifier,
        contentScale=ContentScale.Fit)
}

@Composable
internal fun KidsCinemaHomeArtwork(style:KidsStyle,modifier:Modifier=Modifier) {
    DisposableEffect(Unit) {
        val release=KidsCinemaArt.retainHome()
        onDispose {release()}
    }
    BoxWithConstraints(modifier) {
        KidsCinemaBackdrop(style,Modifier.fillMaxSize())
        KidsCinemaBrand(Modifier.align(Alignment.TopCenter).padding(top=maxHeight*.055f)
            .width(maxWidth*.31f).height(maxHeight*.18f))
    }
}
@Composable
internal fun KidsCinemaHomeCell(style:KidsStyle,index:Int,focused:Boolean,height:Dp,modifier:Modifier) {
    val growth by animateFloatAsState(if(focused)1.08f else 1f,tween(140),label="KidsCinemaFocus")
    val labels=listOf("CANAIS","FILMES","SÉRIES")
    Column(modifier.semantics{contentDescription=labels[index];stateDescription=if(focused)"Selecionado"else""},
        horizontalAlignment=Alignment.CenterHorizontally) {
        Image(standalonePainter(when(index){0->R.drawable.kids_cloud_channels;1->R.drawable.kids_cloud_movies;else->R.drawable.kids_cloud_series}),
            null,Modifier.fillMaxWidth().weight(1f).drawWithContent {
                // Draw the high-resolution bitmap at its final scale, avoiding a second enlarged texture pass.
                scale(growth) { this@drawWithContent.drawContent() }
            },
            contentScale=ContentScale.Fit,alignment=Alignment.BottomCenter)
        Box(Modifier.width(height*.30f).height(height*.07f).testTag("kids-home-focus-$index"),contentAlignment=Alignment.Center) {
            if(focused)Box(Modifier.fillMaxSize().background(
                if(style==KidsStyle.BLUE)Color(0xFF008CF5)else Color(0xFFEF3F86),RoundedCornerShape(50)))
            Text(labels[index],color=if(focused)Color.White else kidsCinemaText,
                fontSize=(height.value*.036f).coerceIn(11f,29f).sp,fontWeight=FontWeight.ExtraBold,
                fontFamily=FontFamily.SansSerif,maxLines=1,textAlign=TextAlign.Center)
        }
        Spacer(Modifier.height(height*.047f))
    }
}
@Composable
internal fun KidsCinemaCatalogHeader(compact:Boolean,title:String="Categorias") {
    Column(Modifier.fillMaxWidth()) {
        KidsCinemaBrand(Modifier.fillMaxWidth().height(if(compact)72.dp else 88.dp))
        val channels=title.contains("Canais",ignoreCase=true)
        Row(Modifier.fillMaxWidth().padding(top=3.dp,bottom=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
            if(channels) {
                Canvas(Modifier.width(24.dp).height(18.dp)) {
                    val mid=size.height*.5f
                    val stroke=3.dp.toPx()
                    drawLine(kidsCinemaText,Offset(size.width*.92f,mid),Offset(size.width*.12f,mid),stroke,cap=StrokeCap.Round)
                    drawLine(kidsCinemaText,Offset(size.width*.12f,mid),Offset(size.width*.40f,size.height*.15f),stroke,cap=StrokeCap.Round)
                    drawLine(kidsCinemaText,Offset(size.width*.12f,mid),Offset(size.width*.40f,size.height*.85f),stroke,cap=StrokeCap.Round)
                }
                Spacer(Modifier.width(7.dp))
            }
            Text(if(channels) "CANAIS" else title.uppercase(java.util.Locale.ROOT),color=kidsCinemaText,fontWeight=FontWeight.ExtraBold,
                fontSize=if(compact)16.sp else 18.sp,maxLines=1,textAlign=TextAlign.Center,
                style=androidx.compose.ui.text.TextStyle(platformStyle=androidx.compose.ui.text.PlatformTextStyle(includeFontPadding=false)))
            if(channels)Spacer(Modifier.width(31.dp))
        }
    }
}

/** Static clay texture, no animation/blur; used only by movie/series catalogue controls. */
@Composable
internal fun Modifier.kidsCinemaFocus(focused:Boolean,selected:Boolean=false):Modifier {
    if(!KidsAppearance.active)return this
    val accent=kidsCinemaAccent()
    return this.drawWithCache {
        val radius=androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
        onDrawWithContent {
            if(focused || selected)drawRoundRect(Brush.verticalGradient(listOf(accent,accent.copy(alpha=.90f))),cornerRadius=radius)
            drawContent()
            if(focused) {
                val stroke=2.dp.toPx()
                drawRoundRect(accent,topLeft=Offset(stroke/2,stroke/2),
                    size=Size((size.width-stroke).coerceAtLeast(0f),(size.height-stroke).coerceAtLeast(0f)),
                    cornerRadius=radius,style=androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            }
        }
    }
}

internal fun kidsCinemaCategoryColors(selected:Boolean):List<Color> {
    if(!KidsAppearance.active) return if(selected) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categorySelectionColors() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categoryColors()
    return if(selected)listOf(kidsCinemaAccent(),kidsCinemaAccent().copy(alpha=.9f))
        else listOf(Color.White.copy(alpha=.38f),Color.White.copy(alpha=.20f))
}

internal fun kidsCinemaAccent():Color=if(KidsAppearance.style==KidsStyle.BLUE) Color(0xFF008CF5) else Color(0xFFEF3F86)

internal fun kidsCinemaSelectionColors():List<Color> = if(KidsAppearance.active) kidsCinemaCategoryColors(true) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColors()

/** Category/card foreground follows its blue/pink clay selection. */
internal fun kidsCinemaRowInk(selected:Boolean,source:Long):Color =
    if(KidsAppearance.active && selected) Color.White else kidsCinemaInk(source)
@Composable
internal fun KidsCinemaSectionPlaque(title:String,modifier:Modifier=Modifier) {
    val shape=RoundedCornerShape(9.dp)
    Box(modifier.width(190.dp).height(28.dp).background(kidsCinemaAccent(),shape)
        .border(1.5.dp,Color(0xFFE4B45C),shape),contentAlignment=Alignment.Center) {
        Text(title,color=Color(0xFFFFF1CC),fontSize=14.sp,fontWeight=FontWeight.ExtraBold)
    }
}
@Composable
internal fun KidsCinemaChannelLogo(url:String?,modifier:Modifier=Modifier) {
    val context=LocalContext.current.applicationContext
    val bitmap by produceState(br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader.peekBitmap(url),url) {
        value=br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader.loadWithRetry(context,url)
    }
    Box(modifier,contentAlignment=Alignment.Center) {
        if(bitmap!=null) Image(bitmap!!,null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
        else Text("▶",color=kidsCinemaAccent(),fontSize=18.sp)
    }
}

internal fun Modifier.kidsCinemaPosterFrame(focused:Boolean,corner:Float):Modifier {
    if(!KidsAppearance.active) return this.posterSelectionFrame(focused,corner)
    return if(focused) this.border(2.dp,kidsCinemaAccent(),RoundedCornerShape(corner.dp)) else this
}

// Theatre catalogues have no enclosing boxes. Normal catalogue styling is retained.
internal fun Modifier.kidsCinemaBorder(border:androidx.compose.foundation.BorderStroke,shape:Shape=RectangleShape):Modifier =
    if(KidsAppearance.active) this else this.border(border,shape)
internal fun Modifier.kidsCinemaBorder(width:Dp,color:Color,shape:Shape=RectangleShape):Modifier =
    if(KidsAppearance.active) this else this.border(width,color,shape)
internal fun Modifier.kidsCinemaBorder(width:Dp,brush:Brush,shape:Shape=RectangleShape):Modifier =
    if(KidsAppearance.active) this else this.border(width,brush,shape)

@Composable
internal fun Modifier.kidsCinemaToolbarFocus(focused:Boolean,selected:Boolean=false):Modifier {
    if(!KidsAppearance.active) return this.kidsCinemaFocus(focused,selected)
    return this.background(Color.White.copy(alpha=.55f),RoundedCornerShape(25))
        .border(1.dp,kidsCinemaAccent().copy(alpha=.18f),RoundedCornerShape(25)).kidsCinemaFocus(focused,selected)
}
@Composable
internal fun KidsCinemaClock(time:String) {
    Text(time,Modifier.padding(horizontal=5.dp),color=kidsCinemaText,fontWeight=FontWeight.ExtraBold,
        fontFamily=FontFamily.SansSerif,fontSize=16.sp,maxLines=1)
}
internal fun kidsCinemaCategoryLabel(value:String):String {
    if(!KidsAppearance.active)return value
    return value.trim { it.isWhitespace() || it in "♦◆♥♡✔✓✅▶️" ||
        Character.getType(it)==Character.OTHER_SYMBOL.toInt() || Character.isSurrogate(it) || it=='\uFE0F' }
}

@Composable
internal fun Modifier.kidsCinemaClickable(onClick:()->Unit):Modifier {
    if(!KidsAppearance.active) return this.clickable(onClick=onClick)
    return this.clickable(interactionSource=remember { androidx.compose.foundation.interaction.MutableInteractionSource() },indication=null,onClick=onClick)
}

@Composable
internal fun KidsNeutralLoadingBackdrop(modifier:Modifier=Modifier) {
    KidsCinemaBackdrop(modifier=modifier,catalog=true)
}
