package br.com.flaviotorres.iptvpremium.core.storage

/** Android dataBytes contains cacheBytes. managedCacheBytes is the disposable subset we own. */
internal data class AppStorageSnapshot(val appBytes: Long, val dataBytes: Long, val cacheBytes: Long,
                                      val managedCacheBytes: Long, val exact: Boolean) {
    val totalBytes: Long get() = appBytes.coerceAtLeast(0L) + dataBytes.coerceAtLeast(0L)
}

internal object AppStoragePolicy {
    const val TARGET = 400_000_000L
    private const val RESERVE = 20_000_000L
    private const val ESTIMATE_RESERVE = 50_000_000L

    fun cacheAllowance(snapshot: AppStorageSnapshot): Long {
        val disposable = snapshot.managedCacheBytes.coerceIn(0L, snapshot.dataBytes.coerceAtLeast(0L))
        val fixed = snapshot.totalBytes - disposable
        val margin = if (snapshot.exact) RESERVE else ESTIMATE_RESERVE
        return (TARGET - fixed - margin).coerceIn(0L, CacheStorageLimits.TOTAL)
    }
}
