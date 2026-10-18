package br.com.flaviotorres.iptvpremium.data.repository

import android.os.Bundle
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.platform.app.InstrumentationRegistry
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.remote.DefaultPlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.storage.PlaylistStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

/** Explicit opt-in. Reads the configured source/catalog and up to three series; never changes them. */
class SeriesProviderDeviceTest {
    @Test fun configuredSeriesLoadWithoutManualRetry() = runBlocking {
        withTimeout(240_000L) {
            val instrument = InstrumentationRegistry.getInstrumentation()
            val context = instrument.targetContext.applicationContext
            val selection = PlaylistStorage(context).selection.first()
            val source = selection.playlists.firstOrNull { it.id == selection.activeId }
            assertTrue("Selecione uma lista cadastrada antes do teste.", source != null)
            val active = source!!
            val db = PlaylistCacheDatabase.getInstance(context)
            val generation = db.playlistCacheDao().getActiveGeneration(active.id)?.generationId
            assertTrue("Aguarde a importacao da lista antes do teste.", generation != null)
            val targets = withContext(Dispatchers.IO) {
                listOf("american_hostage" to "%american%hostage%", "volte_sempre" to "%volte sempre%", "marilia" to "%mendon%").mapNotNull { (label, pattern) ->
                    db.openHelper.readableDatabase.query(SimpleSQLiteQuery(
                        "SELECT seriesId FROM cached_series WHERE playlistId=? AND generationId=? AND LOWER(name) LIKE ? ORDER BY CASE WHEN name LIKE '%[L]%' THEN 1 ELSE 0 END, providerOrder LIMIT 1",
                        arrayOf(active.id, generation, pattern)
                    )).use { cursor -> if (cursor.moveToFirst()) label to cursor.getString(0) else null }
                }
            }
            assertTrue("Nenhuma das series de referencia foi encontrada nesta lista.", targets.isNotEmpty())
            val failed = ArrayList<String>()
            for ((label, id) in targets) {
                var calls = 0
                val base = DefaultPlaylistHttpClient()
                val http = object : PlaylistHttpClient by base {
                    override suspend fun get(url: String): String { calls++; return base.get(url) }
                }
                val started = android.os.SystemClock.elapsedRealtime()
                val result = runCatching { SeriesEpisodesRepository(http).load(active, id) }
                val count = result.getOrNull()?.episodes?.size ?: 0
                val line = "ProviderSeries alvo=$label episodios=$count chamadas=$calls duracaoMs=${android.os.SystemClock.elapsedRealtime()-started} erro=${result.exceptionOrNull()?.javaClass?.simpleName ?: "nenhum"}"
                instrument.sendStatus(2, Bundle().apply { putString("stream", "$line\n") })
                if (count == 0) failed += label
            }
            assertTrue("Series sem episodios: ${failed.joinToString()}. Envie o diagnostico do teste e o logcat.", failed.isEmpty())
        }
    }
}
