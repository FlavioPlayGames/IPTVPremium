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
                BitmapFactory.decodeResource(context.resources,id,BitmapFactory.Options().apply{inScaled=false})
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
internal val kidsCinemaText=Color(0xFF24334A)
internal fun kidsCinemaInk(original:Long):Color=if(!KidsAppearance.active)
    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(original)
    else kidsCinemaText.copy(alpha=Color(original).alpha)
internal fun kidsCinemaPanelBrush():Brush=SolidColor(Color.Transparent)

@Composable
internal fun KidsCinemaBackdrop(style:KidsStyle=KidsAppearance.style,modifier:Modifier=Modifier,catalog:Boolean=false) {
    val image=rememberCinemaArt(if(catalog) { if(style==KidsStyle.BLUE) R.drawable.kids_catalog_blue else R.drawable.kids_catalog_pink } else if(style==KidsStyle.BLUE)R.drawable.kids_cinema_blue else R.drawable.kids_cinema_pink)
    Box(modifier.background(Color(0xFFF5E4C6))) {
        image?.let{Image(it,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,filterQuality=FilterQuality.High)}
    }
}
@Composable
internal fun KidsCinemaBrand(modifier:Modifier=Modifier) {
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val unit=minOf(maxHeight.value/95f,maxWidth.value/220f).coerceAtLeast(.2f)
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Canvas(Modifier.size((27*unit).dp,(18*unit).dp)) {
                val path=Path().apply{moveTo(size.width*.1f,size.height*.85f);lineTo(0f,size.height*.1f)
                    lineTo(size.width*.32f,size.height*.38f);lineTo(size.width*.5f,0f)
                    lineTo(size.width*.68f,size.height*.38f);lineTo(size.width,size.height*.1f)
                    lineTo(size.width*.9f,size.height*.85f);close()}
                drawPath(path,Color(0xFFE1A843))
            }
            Text("IPTV PREMIUM",color=kidsCinemaText,fontSize=(13*unit).sp,fontWeight=FontWeight.ExtraBold,
                fontFamily=FontFamily.SansSerif,letterSpacing=(.8f*unit).sp,maxLines=1)
            Row { "KIDS".forEachIndexed { index,letter->
                Text(letter.toString(),color=listOf(Color(0xFFE35F58),Color(0xFFE8B34A),Color(0xFF55A7DC),Color(0xFF62AA88))[index],
                    fontSize=(33*unit).sp,fontWeight=FontWeight.Black,fontFamily=FontFamily.SansSerif)
            } }
        }
    }
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
        Image(cinemaSprite(index),null,Modifier.fillMaxWidth().weight(1f).scale(growth),
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
    val logo=rememberCinemaArt(R.drawable.kids_cinema_brand)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(if(compact)100.dp else 116.dp),contentAlignment=Alignment.Center) {
            logo?.let{Image(it,"IPTV Premium Kids",Modifier.fillMaxSize(),contentScale=ContentScale.Fit,filterQuality=FilterQuality.High)}
        }
        Text(title,Modifier.padding(start=5.dp,top=3.dp,bottom=6.dp),color=kidsCinemaText,fontWeight=FontWeight.ExtraBold,fontSize=if(compact)13.sp else 15.sp)
    }
}

/** Static clay texture, no animation/blur; used only by movie/series catalogue controls. */
@Composable
internal fun Modifier.kidsCinemaFocus(focused:Boolean,selected:Boolean=false):Modifier {
    if(!KidsAppearance.active)return this
    val image=rememberCinemaArt(R.drawable.kids_cinema_sprites)
    val crop=cinemaCrop(if(KidsAppearance.style==KidsStyle.BLUE)3 else 4)
    return this.drawWithCache {
        onDrawWithContent {
            if(focused||selected) {
                // Pale cream overlay keeps existing dark labels legible over the clay capsule.
                image?.let{drawImage(it,srcOffset=IntOffset(crop.x,crop.y),srcSize=IntSize(crop.width,crop.height),
                    dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)),alpha=1f)}
            }
            drawContent()
        }
    }
}

internal fun kidsCinemaCategoryColors(selected:Boolean):List<Color> {
    if(!KidsAppearance.active) return if(selected) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categorySelectionColors() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categoryColors()
    return listOf(Color.Transparent,Color.Transparent)
}

internal fun kidsCinemaAccent():Color=if(KidsAppearance.style==KidsStyle.BLUE) Color(0xFF235E82) else Color(0xFF8A3967)

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
    return if(focused) this.border(2.dp,Color(0xFFE7B345),RoundedCornerShape(corner.dp)) else this
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
    return this.background(Color(0xB3FFF5E6),RoundedCornerShape(50)).kidsCinemaFocus(focused,selected)
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
