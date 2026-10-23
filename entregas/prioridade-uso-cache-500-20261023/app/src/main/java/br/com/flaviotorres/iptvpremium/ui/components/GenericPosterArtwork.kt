package br.com.flaviotorres.iptvpremium.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.data.cache.GenericPosterKind
import br.com.flaviotorres.iptvpremium.data.cache.GenericPosterPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

internal data class PosterArtworkState(val bitmap:ImageBitmap?,val finished:Boolean)

/** Exactly four APK-resident images. Decode once off main, retain ~1.5 MiB for this process. */
internal object GenericPosterArtworkCache {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val images=java.util.concurrent.ConcurrentHashMap<Int,ImageBitmap>()
    val revision=MutableStateFlow(0)
    @Volatile private var started=false
    fun peek(resource:Int):ImageBitmap?=images[resource]
    fun trimMemory()=Unit // Tiny shared fallback set is intentionally independent of poster LRU eviction.
    @Synchronized fun prepare(context:Context) {
        if(started)return
        started=true
        val app=context.applicationContext
        scope.launch {
            for(resource in listOf(R.drawable.poster_generic_cinema,R.drawable.poster_generic_frames,
                R.drawable.poster_generic_anime,R.drawable.poster_generic_animation)) {
                val bitmap=runCatching {
                    val options=BitmapFactory.Options().apply {inScaled=false;inSampleSize=2;inPreferredConfig=Bitmap.Config.ARGB_8888}
                    BitmapFactory.decodeResource(app.resources,resource,options)?.also {it.prepareToDraw()}?.asImageBitmap()
                }.getOrNull()
                if(bitmap!=null){images[resource]=bitmap;revision.value++}
                yield()
            }
        }
    }
}

/** Metadata already on the card is sufficient; no per-card Room query or decode coroutine. */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun GenericPosterArtwork(mediaKind:String,id:String,title:String,categoryHint:String="",
    resolveCategory:(suspend (String)->String?)?=null,modifier:Modifier=Modifier) {
    val context=LocalContext.current.applicationContext
    SideEffect {GenericPosterArtworkCache.prepare(context)}
    val revision by GenericPosterArtworkCache.revision.collectAsState()
    val kind=remember(mediaKind,id,categoryHint,title) {GenericPosterPolicy.select(mediaKind,id,"$categoryHint $title")}
    val resource=when(kind) {
        GenericPosterKind.CINEMA->R.drawable.poster_generic_cinema
        GenericPosterKind.FRAMES->R.drawable.poster_generic_frames
        GenericPosterKind.ANIME->R.drawable.poster_generic_anime
        GenericPosterKind.ANIMATION->R.drawable.poster_generic_animation
    }
    val image=remember(resource,revision){GenericPosterArtworkCache.peek(resource)}
    if(image!=null)Image(image,contentDescription="Capa genérica — $title",modifier=modifier,
        contentScale=ContentScale.Crop,filterQuality=FilterQuality.Low)
    else Canvas(modifier) {
        drawRect(Color(0xFF1A1D24))
        drawRect(Color(0xFF737883),Offset(size.width*.20f,size.height*.34f),Size(size.width*.60f,size.height*.30f),style=Stroke(2f))
        drawLine(Color(0xFF737883),Offset(size.width*.32f,size.height*.69f),Offset(size.width*.68f,size.height*.69f),2f)
    }
}
