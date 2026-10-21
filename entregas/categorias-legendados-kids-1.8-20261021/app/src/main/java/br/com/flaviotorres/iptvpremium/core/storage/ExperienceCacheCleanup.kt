package br.com.flaviotorres.iptvpremium.core.storage

import java.io.File

/** Called on IO after the old process stops. Database and filesDir are deliberately excluded. */
internal fun clearExperienceCache(cacheDir: File, externalCacheDir: File?) {
    cacheDir.listFiles().orEmpty().forEach { check(it.deleteRecursively()) { "cache_cleanup_failed" } }
    externalCacheDir?.listFiles().orEmpty().forEach { it.deleteRecursively() }
}
