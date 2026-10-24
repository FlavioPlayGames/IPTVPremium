package br.com.flaviotorres.iptvpremium.data.cache

import android.util.Log
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Fixed SQL only. Primary keys and providerOrder indexes stay available for recovery by category. */
internal object XtreamBulkIndexPlan {
    val names = listOf(
        "index_cached_movies_playlistId_generationId_categoryId",
        "index_cached_movies_playlistId_generationId_name",
        "index_cached_movies_playlistId_generationId_addedAtEpochSeconds",
        "index_cached_series_playlistId_generationId_categoryId",
        "index_cached_series_playlistId_generationId_name"
    )
    val create = listOf(
        "CREATE INDEX IF NOT EXISTS ${names[0]} ON cached_movies(playlistId,generationId,categoryId)",
        "CREATE INDEX IF NOT EXISTS ${names[1]} ON cached_movies(playlistId,generationId,name)",
        "CREATE INDEX IF NOT EXISTS ${names[2]} ON cached_movies(playlistId,generationId,addedAtEpochSeconds)",
        "CREATE INDEX IF NOT EXISTS ${names[3]} ON cached_series(playlistId,generationId,categoryId)",
        "CREATE INDEX IF NOT EXISTS ${names[4]} ON cached_series(playlistId,generationId,name)"
    )
    val drop = names.map { "DROP INDEX IF EXISTS $it" }

    fun repair(db: SupportSQLiteDatabase) {
        // Normal openings incur no rebuild. This also makes rollback to the preceding app safe
        // after the current app has been opened once following an interrupted import.
        val missing = names.toMutableSet()
        db.query("SELECT name FROM sqlite_master WHERE type='index'").use { cursor ->
            while (cursor.moveToNext()) missing.remove(cursor.getString(0))
        }
        if (missing.isEmpty()) return
        val start = System.nanoTime()
        db.beginTransaction()
        try {
            create.forEach { db.execSQL(it) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        Log.i("XtreamBulkPerf", "phase=open_repair restored=${missing.size} durationMs=${(System.nanoTime()-start)/1_000_000}")
    }
}

/** One owner across all playlists; network work never runs inside a SQLite transaction. */
internal class XtreamBulkIndexCoordinator(
    private val transaction: suspend (List<String>) -> Unit
) {
    private val lock = Mutex()

    suspend fun <T> around(block: suspend () -> T): T = lock.withLock {
        var primary: Throwable? = null
        try {
            val start = System.nanoTime()
            // Inside the try: even cancellation on return from a committed DROP must repair.
            transaction(XtreamBulkIndexPlan.drop)
            Log.i("XtreamBulkPerf", "phase=load indexMode=deferred5 durationMs=${(System.nanoTime()-start)/1_000_000}")
            block()
        } catch (error: Throwable) {
            primary = error
            throw error
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                val start = System.nanoTime()
                try {
                    transaction(XtreamBulkIndexPlan.create)
                    Log.i("XtreamBulkPerf", "phase=restored indexes=5 cancelledOrFailed=${primary != null} durationMs=${(System.nanoTime()-start)/1_000_000}")
                } catch (repair: Throwable) {
                    Log.e("XtreamBulkPerf", "phase=restore_failed error=${repair.javaClass.simpleName}")
                    val original = primary
                    if (original == null) throw repair
                    if (original !== repair) original.addSuppressed(repair)
                }
            }
        }
    }
}

internal object XtreamBulkIndexes {
    @Volatile private var database: PlaylistCacheDatabase? = null
    fun attach(value: PlaylistCacheDatabase) { database = value }
    private val coordinator = XtreamBulkIndexCoordinator { statements ->
        val db = checkNotNull(database) { "Banco do catálogo não inicializado." }
        db.withTransaction {
            val connection = db.openHelper.writableDatabase
            statements.forEach { connection.execSQL(it) }
        }
    }
    suspend fun <T> around(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        coordinator.around(block)
    }
}
