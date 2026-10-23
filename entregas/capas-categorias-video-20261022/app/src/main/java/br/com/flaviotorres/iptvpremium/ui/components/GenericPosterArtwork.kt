package br.com.flaviotorres.iptvpremium.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.data.cache.GenericPosterKind
import br.com.flaviotorres.iptvpremium.data.cache.GenericPosterPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class PosterArtworkState(val bitmap:ImageBitmap?,val finished:Boolean)

/** Four shared, local images; never one decoded bitmap per missing catalogue entry. */
internal object GenericPosterArtworkCache {
    private val cache=object:LruCache<Int,ImageBitmap>(6*1024){
        override fun sizeOf(key:Int,value:ImageBitmap)=((value.asAndroidBitmap().allocationByteCount+1023)/1024).coerceAtLeast(1)
    }
    private val lock=Mutex()
    fun peek(resource:Int):ImageBitmap?=cache.get(resource)
    fun trimMemory(){cache.evictAll()}
    suspend fun load(context:Context,resource:Int):ImageBitmap?=withContext(Dispatchers.IO) {
        lock.withLock {
            cache.get(resource)?:runCatching {
                val options=BitmapFactory.Options().apply{inScaled=false;inPreferredConfig=Bitmap.Config.ARGB_8888}
                BitmapFactory.decodeResource(context.resources,resource,options)?.asImageBitmap()?.also{cache.put(resource,it)}
            }.getOrNull()
        }
    }
}

/** Called only after the real poster is absent or its existing retry cycle finishes unsuccessfully. */
@Composable
internal fun GenericPosterArtwork(mediaKind:String,id:String,title:String,categoryHint:String="",
    resolveCategory:(suspend (String)->String?)?=null,modifier:Modifier=Modifier) {
    val context=LocalContext.current.applicationContext
    val latestResolver by rememberUpdatedState(resolveCategory)
    val category by key(mediaKind,id,categoryHint) {
        produceState<String?>(if(categoryHint.isNotBlank()||resolveCategory==null)categoryHint else null) {
            if(value==null)value=try{latestResolver?.invoke(id).orEmpty()}
                catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){""}
        }
    }
    val resolved=category?:return
    val kind=remember(mediaKind,id,resolved){GenericPosterPolicy.select(mediaKind,id,resolved)}
    val resource=when(kind){
        GenericPosterKind.CINEMA->R.drawable.poster_generic_cinema
        GenericPosterKind.FRAMES->R.drawable.poster_generic_frames
        GenericPosterKind.ANIME->R.drawable.poster_generic_anime
        GenericPosterKind.ANIMATION->R.drawable.poster_generic_animation
    }
    val image by key(resource) {
        produceState(GenericPosterArtworkCache.peek(resource)) {value=GenericPosterArtworkCache.load(context,resource)}
    }
    image?.let{Image(it,contentDescription="Capa genérica — $title",modifier=modifier,
        contentScale=ContentScale.Crop,filterQuality=FilterQuality.High)}
}
