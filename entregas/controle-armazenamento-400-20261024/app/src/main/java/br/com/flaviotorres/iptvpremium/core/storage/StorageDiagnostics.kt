package br.com.flaviotorres.iptvpremium.core.storage

import android.content.Context
import android.util.Log
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import java.io.File

/** Sizes only: no filenames from providers, URLs, playlist contents or preferences are logged. */
internal object StorageDiagnostics {
    private fun bytes(file: File): Long {
        if (!file.exists() || java.nio.file.Files.isSymbolicLink(file.toPath())) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { bytes(it) } ?: 0L
    }

    fun log(context: Context, phase: String): AppStorageSnapshot {
        val data = File(context.applicationInfo.dataDir)
        val folders = linkedMapOf(
            "databases" to File(data, "databases"),
            "files" to context.filesDir,
            "preferences" to File(data, "shared_prefs"),
            "no_backup" to context.noBackupFilesDir,
            "cache_total" to context.cacheDir,
            "code_cache" to context.codeCacheDir,
            "posters" to File(context.cacheDir, "iptv_movie_posters_session"),
            "details_home" to File(context.cacheDir, "movie_details_images"),
            "updates" to File(context.cacheDir, "updates"),
            "subtitles" to File(context.filesDir, "iptv_subtitles_lru_5mb")
        )
        val sizes = folders.mapValues { (_, file) -> bytes(file) }
        Log.i("IPTVStorage", "phase=$phase " + sizes.entries.joinToString(" ") { (name, size) -> "$name=$size" })
        val managed = sizes.getValue("cache_total") + sizes.getValue("subtitles")
        val apkBytes = (listOf(context.applicationInfo.sourceDir) + context.applicationInfo.splitSourceDirs.orEmpty())
            .distinct().sumOf { bytes(File(it)) }
        val nativeBytes = context.applicationInfo.nativeLibraryDir?.let { bytes(File(it)) } ?: 0L
        val estimatedData = listOf("databases", "files", "preferences", "no_backup", "cache_total", "code_cache")
            .sumOf { sizes.getValue(it) } + bytes(context.externalCacheDir ?: File(data, "__absent_external_cache__")) +
            bytes(context.getExternalFilesDir(null) ?: File(data, "__absent_external_files__"))
        val snapshot = try {
            val stats = context.getSystemService(android.app.usage.StorageStatsManager::class.java)
                .queryStatsForUid(android.os.storage.StorageManager.UUID_DEFAULT, android.os.Process.myUid())
            // Android's dataBytes ALREADY includes cacheBytes. Do not add cache twice.
            AppStorageSnapshot(stats.appBytes, stats.dataBytes, stats.cacheBytes, managed, true)
        } catch (error: Exception) {
            Log.i("AppStoragePerf", "phase=$phase source=estimate reason=${error.javaClass.simpleName}")
            AppStorageSnapshot(apkBytes + nativeBytes, estimatedData,
                sizes.getValue("cache_total") + sizes.getValue("code_cache"), managed, false)
        }
        Log.i("AppStoragePerf", "phase=$phase source=${if (snapshot.exact) "android" else "estimate"} " +
            "appBytes=${snapshot.appBytes} dataBytes=${snapshot.dataBytes} cacheIncludedBytes=${snapshot.cacheBytes} " +
            "managedCacheBytes=$managed totalBytes=${snapshot.totalBytes} targetBytes=${AppStoragePolicy.TARGET} " +
            "overTargetBytes=${(snapshot.totalBytes-AppStoragePolicy.TARGET).coerceAtLeast(0L)} " +
            "apkBytes=$apkBytes nativeBytes=$nativeBytes cacheAllowanceBytes=${AppStoragePolicy.cacheAllowance(snapshot)}")
        val dbFile = context.getDatabasePath("playlist_cache.db")
        if (!dbFile.isFile) return snapshot
        val db = PlaylistCacheDatabase.getInstance(context).openHelper.readableDatabase
        fun pragma(name: String): Long = db.query("PRAGMA $name").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        val counts = mutableMapOf<String, Long>()
        db.query("SELECT status, COUNT(*) FROM playlist_cache_generations GROUP BY status").use { cursor ->
            while (cursor.moveToNext()) {
                val status = cursor.getString(0)
                if (status in setOf("ACTIVE", "READY", "STALE", "IMPORTING")) counts[status] = cursor.getLong(1)
            }
        }
        val pageSize = pragma("page_size")
        Log.i("IPTVStorage", "phase=$phase db_main=${dbFile.length()} " +
            "db_wal=${File(dbFile.path + "-wal").length()} " +
            "db_pages=${pragma("page_count")} db_free_pages=${pragma("freelist_count")} db_page_size=$pageSize " +
            "generations_active=${counts["ACTIVE"] ?: 0} generations_ready=${counts["READY"] ?: 0} " +
            "generations_stale=${counts["STALE"] ?: 0} generations_importing=${counts["IMPORTING"] ?: 0}")
        return snapshot
    }
}
