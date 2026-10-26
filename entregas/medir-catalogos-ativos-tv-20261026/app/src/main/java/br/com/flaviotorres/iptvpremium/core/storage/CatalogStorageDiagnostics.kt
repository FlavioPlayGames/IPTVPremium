package br.com.flaviotorres.iptvpremium.core.storage

import android.util.Log
import android.database.Cursor
import android.os.CancellationSignal
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Deferred, read-only survey. Labels are local ordinals, never provider IDs or URLs. */
internal object CatalogStorageDiagnostics {
    private var completed = false
    private val tables = listOf("cached_categories", "cached_channels", "cached_movies", "cached_series", "cached_episodes")

    suspend fun logOnce(db: SupportSQLiteDatabase) {
        if (completed) return
        val started = System.nanoTime()
        val generations = mutableListOf<Triple<String, String, String>>()
        read(db, "SELECT playlistId, generationId, status FROM playlist_cache_generations ORDER BY playlistId, generationId") { cursor ->
            while (cursor.moveToNext()) generations += Triple(cursor.getString(0), cursor.getString(1), cursor.getString(2))
        }
        val labels = generations.map { it.first }.distinct().withIndex().associate { it.value to it.index + 1 }
        for ((index, generation) in generations.withIndex()) {
            val (playlist, id, status) = generation
            if (status !in setOf("ACTIVE", "READY", "STALE", "IMPORTING")) continue
            for (table in tables) {
                currentCoroutineContext().ensureActive()
                val queryStarted = System.nanoTime()
                val rows = read(db, "SELECT COUNT(*) FROM $table WHERE playlistId=? AND generationId=?", arrayOf(playlist, id)) { cursor ->
                    check(cursor.moveToFirst()); cursor.getLong(0)
                }
                Log.i("CatalogStoragePerf", "phase=rows list=${labels.getValue(playlist)} generation=${index + 1} " +
                    "status=$status table=$table rows=$rows queryMs=${(System.nanoTime() - queryStarted) / 1_000_000}")
            }
        }
        currentCoroutineContext().ensureActive()
        // dbstat is optional in Android's SQLite build. Never invent a per-table byte estimate.
        val statSql = "SELECT s.tbl_name, s.type, s.name, SUM(d.pgsize) FROM dbstat AS d " +
            "JOIN sqlite_master AS s ON s.name=d.name " +
            "WHERE s.type IN ('table','index') GROUP BY s.tbl_name,s.type,s.name"
        try {
            read(db, statSql) { cursor ->
                while (cursor.moveToNext()) {
                    val table = cursor.getString(0)
                    if (table in tables || table == "playlist_cache_generations") {
                        Log.i("CatalogStoragePerf", "phase=pages table=$table kind=${cursor.getString(1)} " +
                            "object=${cursor.getString(2)} bytes=${cursor.getLong(3)}")
                    }
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            Log.i("CatalogStoragePerf", "phase=pages unavailable=true reason=${error.javaClass.simpleName}")
        }
        currentCoroutineContext().ensureActive()
        completed = true
        Log.i("CatalogStoragePerf", "phase=complete lists=${labels.size} generations=${generations.size} " +
            "durationMs=${(System.nanoTime() - started) / 1_000_000}")
    }

    /** Cancels SQLite work itself if navigation takes priority, not only the next query. */
    private suspend fun <T> read(
        db: SupportSQLiteDatabase,
        sql: String,
        args: Array<out Any?> = emptyArray(),
        consume: (Cursor) -> T
    ): T = suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        try {
            signal.throwIfCanceled()
            val result = db.query(SimpleSQLiteQuery(sql, args), signal).use(consume)
            continuation.resume(result)
        } catch (error: Exception) {
            continuation.resumeWithException(error)
        }
    }

}
