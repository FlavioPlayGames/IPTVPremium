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

    fun log(context: Context, phase: String) {
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
        Log.i("IPTVStorage", "phase=$phase " + folders.entries.joinToString(" ") { (name, file) -> "$name=${bytes(file)}" })
        val dbFile = context.getDatabasePath(br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase.FILE_NAME)
        if (!dbFile.isFile) return
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
    }
}
