package br.com.flaviotorres.iptvpremium.data.repository

import android.content.Context
import br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage
import java.io.File
import br.com.flaviotorres.iptvpremium.core.storage.CacheStorageBudget
import java.security.MessageDigest
import org.json.JSONObject

/** Public TMDB responses survive process restarts, within the app's existing disk quota. IO only. */
internal object CatalogMetadataDiskCache {
    private data class Store(val root: File, val budget: CacheStorageBudget)
    @Volatile private var store: Store? = null
    fun initialize(value: Context) {
        val app = value.applicationContext
        store = Store(app.cacheDir, AppCacheStorage.get(app))
    }
    private fun file(store: Store, key: String): File {
        val name = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return File(File(store.root, "catalog_public_metadata_v1"), "$name.json")
    }
    fun read(key: String): JSONObject? {
        val store = store ?: return null
        val file = file(store, key)
        return store.budget.retain(file).use {
            runCatching {
                if (!file.isFile || file.length() !in 1..MAX_BYTES ||
                    System.currentTimeMillis() - file.lastModified() !in 0..TTL_MS) null
                else JSONObject(file.readText(Charsets.UTF_8)).takeIf { it.optInt("id", 0) > 0 }
            }.getOrNull()
        }
    }
    fun write(key: String, root: JSONObject) {
        val store = store ?: return
        runCatching {
            val bytes = root.toString().toByteArray(Charsets.UTF_8)
            if (bytes.size > MAX_BYTES) return
            val file = file(store, key)
            file.parentFile?.mkdirs()
            store.budget.reserve(file, bytes.size.toLong())?.use {
                it.temporary.writeBytes(bytes)
                check(it.temporary.renameTo(file))
            }
        }
    }
    private const val MAX_BYTES = 1_048_576L
    private const val TTL_MS = 6L * 60 * 60 * 1_000
}
