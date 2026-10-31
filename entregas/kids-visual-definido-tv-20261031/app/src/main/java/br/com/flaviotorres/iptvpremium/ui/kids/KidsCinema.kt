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

/** Bounded decoded-art pool; active Full HD backdrop and shared atlas fit within 16 MiB. */
private object KidsCinemaArt {
    private val cache=object:LruCache<Int,ImageBitmap>(16*1024) {
        override fun sizeOf(key:Int,value:ImageBitmap)=((value.width.toLong()*value.height*4+1023)/1024).toInt()
    }
    private val decode=Semaphore(1)
    fun peek(id:Int)=cache.get(id)
    suspend fun load(context:Context,id:Int):ImageBitmap? {
        peek(id)?.let{return it}
        return withContext(Dispatchers.Default) { decode.withPermit {
            peek(id) ?: try {
                val standalone=id in setOf(R.drawable.kids_cloud_channels,R.drawable.kids_cloud_movies,
                    R.drawable.kids_cloud_series,R.drawable.kids_cloud_brand)
                val options=BitmapFactory.Options().apply {
                    inScaled=false
                    // Four independent high-resolution assets share the same bounded pool as the focus atlas.
                    inSampleSize=if(standalone)2 else 1
                }
                BitmapFactory.decodeResource(context.resources,id,options)
                    ?.also{it.prepareToDraw()}?.asImageBitmap()?.also{cache.put(id,it)}
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
private fun cinemaCrop(index:Int):CinemaCrop=when(index) {
    3->CinemaCrop(28,672,456,192)
    4->CinemaCrop(540,672,456,192)
    5->CinemaCrop(1170,658,220,220)
    else->CinemaCrop(index*512,0,512,512)
}
private class CinemaSpritePainter(private val image:ImageBitmap,private val crop:CinemaCrop):Painter() {
    override val intrinsicSize=Size(crop.width.toFloat(),crop.height.toFloat())
    override fun DrawScope.onDraw() {
        drawImage(image,srcOffset=IntOffset(crop.x,crop.y),srcSize=IntSize(crop.width,crop.height),
            dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)),filterQuality=FilterQuality.High)
    }
}
@Composable
private fun cinemaSprite(index:Int):Painter {
    val image=rememberCinemaArt(R.drawable.kids_cinema_sprites)
    return remember(image,index){image?.let{CinemaSpritePainter(it,cinemaCrop(index))}?:ColorPainter(Color.Transparent)}
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
    val pink=style==KidsStyle.PINK
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(if(pink)Color(0xFFFFE2EE)else Color(0xFFDFF1FF),
            Color(0xFFFFFEF8),if(pink)Color(0xFFFFF3F8)else Color(0xFFF4FAFF))))
        val unit=size.minDimension
        fun cloud(x:Float,mirror:Boolean) {
            val direction=if(mirror)-1f else 1f
            val white=Brush.verticalGradient(listOf(Color.White,if(pink)Color(0xFFFFF0F6)else Color(0xFFEDF7FF)))
            for((dx,dy,r) in listOf(Triple(0f,.015f,.075f),Triple(.052f,.025f,.056f),Triple(.092f,.005f,.046f)))
                drawCircle(white,unit*r,Offset(x+direction*unit*dx,unit*dy))
        }
        cloud(0f,false);cloud(size.width,true)
        val colors=listOf(Color(0xFFFFEBA4),Color(0xFFFFD0E4),Color(0xFFB6E6FA))
        for(i in 0..5) {
            val right=i>=3;val j=i%3
            val x=if(right)size.width-unit*(.038f+j*.014f)else unit*(.036f+j*.012f)
            val y=unit*(.105f+j*.072f)
            val radius=unit*(if(catalog).009f else .014f)
            val path=Path()
            for(k in 0..9) {
                val angle=-Math.PI/2+k*Math.PI/5
                val r=radius*(if(k%2==0)1f else .45f)
                val px=x+kotlin.math.cos(angle).toFloat()*r;val py=y+kotlin.math.sin(angle).toFloat()*r
                if(k==0)path.moveTo(px,py)else path.lineTo(px,py)
            }
            path.close();drawPath(path,colors[j].copy(alpha=.65f))
        }
    }
}
@Composable
internal fun KidsCinemaBrand(modifier:Modifier=Modifier) {
    Image(standalonePainter(R.drawable.kids_cloud_brand),"IPTV Premium Kids",modifier,
        contentScale=ContentScale.Fit)
}

@Composable
internal fun KidsCinemaHomeArtwork(style:KidsStyle,modifier:Modifier=Modifier) {
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
            null,Modifier.fillMaxWidth().weight(1f).scale(growth),
            contentScale=ContentScale.Fit,alignment=Alignment.BottomCenter)
        Box(Modifier.width(height*.30f).height(height*.07f).testTag("kids-home-focus-$index"),contentAlignment=Alignment.Center) {
            if(focused)Image(cinemaSprite(if(style==KidsStyle.BLUE)3 else 4),null,Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
            Text(labels[index],color=if(focused)Color.White else kidsCinemaText,
                fontSize=(height.value*.036f).coerceIn(11f,29f).sp,fontWeight=FontWeight.ExtraBold,
                fontFamily=FontFamily.SansSerif,maxLines=1,textAlign=TextAlign.Center)
        }
        Box(Modifier.height(height*.047f).fillMaxWidth(),contentAlignment=Alignment.Center) {
            if(focused)Image(cinemaSprite(5),null,Modifier.size(height*.038f),contentScale=ContentScale.Fit)
        }
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
@Composable
internal fun KidsCinemaPosterStar(modifier:Modifier=Modifier) {
    Image(cinemaSprite(5),null,modifier.size(21.dp),contentScale=ContentScale.Fit)
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
