package br.com.flaviotorres.iptvpremium.core.storage
internal object AppCacheStorage {
    private var instance: CacheStorageBudget? = null
    fun get(context: android.content.Context): CacheStorageBudget = instance ?: CacheStorageBudget(context.cacheDir, java.io.File(context.cacheDir,"subtitles")).also {instance=it}
}
