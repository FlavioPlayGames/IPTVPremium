package br.com.flaviotorres.iptvpremium.core.images

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.*
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.toBitmap
import br.com.flaviotorres.iptvpremium.core.network.AppNetwork
import kotlinx.coroutines.*
import java.io.File
import okio.Path.Companion.toOkioPath

internal class AppImages(private val context:Context) {
    val loader=ImageLoader.Builder(context).components {add(OkHttpNetworkFetcherFactory(callFactory={AppNetwork.client()}))}
        .memoryCache{MemoryCache.Builder().maxSizePercent(context,0.10).build()}
        .diskCache{DiskCache.Builder().directory(File(context.cacheDir,"coil_images_v3").toOkioPath()).maxSizeBytes(96L*1024*1024).build()}
        .build()
    private fun key(url:String,width:Int,height:Int)="$url|${width}x${height}"
    fun peek(url:String?,width:Int=300,height:Int=450):ImageBitmap? {
        if(url.isNullOrBlank())return null
        return loader.memoryCache?.get(MemoryCache.Key(key(url,width,height)))?.image?.toBitmap()?.asImageBitmap()
    }
    suspend fun load(url:String?,width:Int=300,height:Int=450,cacheOnly:Boolean=false):ImageBitmap? {
        if(url.isNullOrBlank()||(!url.startsWith("http://",true)&&!url.startsWith("https://",true)))return null
        peek(url,width,height)?.let{return it}
        return try {
            val request=ImageRequest.Builder(context).data(url).size(width.coerceAtLeast(1),height.coerceAtLeast(1))
                .memoryCacheKey(key(url,width,height)).diskCacheKey(url).allowHardware(false)
                .networkCachePolicy(if(cacheOnly)CachePolicy.DISABLED else CachePolicy.ENABLED).build()
            (loader.execute(request)as?SuccessResult)?.image?.toBitmap()?.asImageBitmap()
        }catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){null}
    }
    fun trim(fraction:Float){loader.memoryCache?.let{it.trimToSize((it.size*fraction.coerceIn(0f,1f)).toLong())}}
    companion object {
        @Volatile private var last:AppImages?=null
        fun get(context:Context):AppImages=br.com.flaviotorres.iptvpremium.core.di.AppServices.get<AppImages>(context).also{last=it}
        fun peek(url:String?,w:Int=300,h:Int=450)=last?.peek(url,w,h)
        fun trimMemory(fraction:Float){last?.trim(fraction)}
    }
}
