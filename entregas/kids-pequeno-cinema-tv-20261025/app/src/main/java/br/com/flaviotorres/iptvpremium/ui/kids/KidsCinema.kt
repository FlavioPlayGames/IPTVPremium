package br.com.flaviotorres.iptvpremium.ui.kids

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

/** Three compressed local assets; two backgrounds + one shared atlas fit in this 16 MiB pool. */
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
            dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)),filterQuality=FilterQuality.Medium)
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
internal fun kidsCinemaPanelBrush():Brush=Brush.verticalGradient(listOf(Color(0xD9FFF3DE),Color(0xC4F9E4C4)))

@Composable
internal fun KidsCinemaBackdrop(style:KidsStyle=KidsAppearance.style,modifier:Modifier=Modifier) {
    val image=rememberCinemaArt(if(style==KidsStyle.BLUE)R.drawable.kids_cinema_blue else R.drawable.kids_cinema_pink)
    Box(modifier.background(Color(0xFFF5E4C6))) {
        image?.let{Image(it,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
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
internal fun KidsCinemaCatalogHeader(compact:Boolean) {
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
        KidsCinemaBrand(Modifier.fillMaxWidth().height(if(compact)78.dp else 98.dp))
        Text("CATEGORIAS",color=kidsCinemaText,fontWeight=FontWeight.ExtraBold,fontSize=if(compact)12.sp else 14.sp)
        Spacer(Modifier.height(6.dp))
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
                    dstSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1)),alpha=.28f)}
            }
            drawContent()
        }
    }
}

internal fun kidsCinemaCategoryColors(selected:Boolean):List<Color> {
    if(!KidsAppearance.active) return if(selected) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categorySelectionColors() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categoryColors()
    return if(!selected) listOf(Color(0x99FFF9EC),Color(0x88F8EBD7))
    else if(KidsAppearance.style==KidsStyle.BLUE) listOf(Color(0xFFD1EAF4),Color(0xFFB1D6EB))
    else listOf(Color(0xFFF7D8E7),Color(0xFFECC0D9))
}

internal fun kidsCinemaAccent():Color=if(KidsAppearance.style==KidsStyle.BLUE) Color(0xFF235E82) else Color(0xFF8A3967)

internal fun kidsCinemaSelectionColors():List<Color> = if(KidsAppearance.active) kidsCinemaCategoryColors(true) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColors()
