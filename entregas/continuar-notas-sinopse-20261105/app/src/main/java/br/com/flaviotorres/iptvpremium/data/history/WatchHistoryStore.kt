package br.com.flaviotorres.iptvpremium.data.history

import android.content.Context
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import br.com.flaviotorres.iptvpremium.core.player.PlaybackMedia
import org.json.JSONArray
import org.json.JSONObject

/**
 * Historico local de reproducao, separado do cache descartavel do catalogo.
 * Armazena somente IDs, titulos e posicoes; NUNCA guarda streamUrl, senha ou token.
 * Chave composta por playlist, tipo de midia e ID real do provedor.
 */
data class WatchHistoryEntry(
    val playlistId: String,
    val contentType: PlaybackContentType,
    val contentId: String,
    val seriesId: String? = null,
    val title: String,
    val posterUrl: String? = null,
    val seriesTitle: String? = null,
    val seriesPosterUrl: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val positionMs: Long,
    val durationMs: Long?,
    val updatedAtMs: Long,
    val seriesOnly: Boolean = false,
    val sharedFromPlaylist: String? = null,
    val finished:Boolean=false,
    val pendingNext:Boolean=false,
    val year:Int?=null,
    val tmdbId:String?=null,
    // Presentation metadata from the active catalogue; deliberately not persisted as watch data.
    val catalogRating:Double?=null
) {
    val fraction: Float?
        get() = durationMs?.takeIf { it > 0L }
            ?.let { (positionMs.toFloat() / it).coerceIn(0f, 1f) }

    val completed: Boolean
        get() = finished || (fraction ?: 0f) >= 0.95f

    val inProgress: Boolean
        get() = !completed && (pendingNext || seriesOnly || positionMs >= 10_000L)
}

class WatchHistoryStore(context: Context, val kids:Boolean=false) {
    private val prefsContext = context.applicationContext
    private val prefs = prefsContext.getSharedPreferences(
        HistoryProfile.preferences("iptvpremium_watch_history_v1",kids), Context.MODE_PRIVATE
    )

    init { HistoryProfileMigration.run(prefsContext) }

    fun all(playlistId: String): List<WatchHistoryEntry> {
        val library=SharedLibraryStore.get(prefsContext,kids)
        return library.projectHistory(playlistId,library.effectiveHistory(readAll())).sortedByDescending{it.updatedAtMs}
    }
    fun allowLegacyResume(playlist:String,media:PlaybackMedia):Boolean=SharedLibraryStore.get(prefsContext,kids).allowLegacyResume(playlist,media,readAll())
    fun resumeEntry(playlist:String,media:PlaybackMedia):WatchHistoryEntry? = SharedLibraryStore.get(prefsContext,kids).resumeEntry(playlist,media,readAll())

    fun allLocal(): List<WatchHistoryEntry> = readAll()

    fun movie(playlistId: String, movieId: String): WatchHistoryEntry? =
        all(playlistId).firstOrNull {
            it.contentType == PlaybackContentType.MOVIE && it.contentId == movieId
        }

    fun lastEpisode(playlistId: String, seriesId: String): WatchHistoryEntry? =
        all(playlistId).firstOrNull {
            it.contentType == PlaybackContentType.EPISODE && it.seriesId == seriesId && !it.seriesOnly
        }

    fun save(
        playlistId: String,
        media: PlaybackMedia,
        positionMs: Long,
        durationMs: Long?,
        seriesTitle: String? = null,
        seriesPosterUrl: String? = null,
        endReached:Boolean=false
    ) = synchronized(historyLock) {
        if (playlistId.isBlank() || media.id.isBlank() ||
            media.type == PlaybackContentType.LIVE_CHANNEL || positionMs < 1_000L
        ) return@synchronized
        val previous = readAll().filter { it.playlistId == playlistId }.firstOrNull {
            it.contentType == media.type && it.contentId == media.id
        }
        val normalizedDuration = durationMs?.takeIf { it > 0L }
            ?: media.durationMs?.takeIf { it > 0L }
            ?: previous?.durationMs
        val entry = WatchHistoryEntry(
            playlistId = playlistId,
            contentType = media.type,
            contentId = media.id,
            seriesId = media.seriesId ?: previous?.seriesId,
            title = media.title,
            posterUrl = media.posterUrl ?: previous?.posterUrl,
            seriesTitle = seriesTitle ?: previous?.seriesTitle,
            seriesPosterUrl = seriesPosterUrl ?: previous?.seriesPosterUrl,
            seasonNumber = media.seasonNumber ?: previous?.seasonNumber,
            episodeNumber = media.episodeNumber ?: previous?.episodeNumber,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = normalizedDuration,
            updatedAtMs = maxOf(System.currentTimeMillis(),(readAll().maxOfOrNull{it.updatedAtMs}?:0L)+1),
            finished=endReached || (previous?.finished==true && positionMs>=previous.positionMs),
            year=media.year?:previous?.year,tmdbId=media.tmdbId?:previous?.tmdbId
        )
        val kept = readAll().filterNot {
            it.playlistId == playlistId &&
                    it.contentType == media.type && it.contentId == media.id
        }
        writeAll((listOf(entry) + kept).sortedByDescending { it.updatedAtMs })
    }

    fun remove(playlistId: String, type: PlaybackContentType, contentId: String) = synchronized(historyLock) {
        if (playlistId.isBlank() || contentId.isBlank()) return@synchronized
        SharedLibraryStore.get(prefsContext,kids).hide(playlistId, type, contentId, readAll())
        val remaining = readAll().filterNot {
            it.playlistId == playlistId && it.contentType == type && it.contentId == contentId
        }
        writeAll(remaining)
    }

    /** An actual provider episode, not a fabricated playback ID. Existing unfinished progress is retained. */
    fun queueNext(playlist:String,media:PlaybackMedia,next:br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeInfo,
        seriesTitle:String?,seriesPoster:String?) = synchronized(historyLock) {
        val series=media.seriesId?:return@synchronized
        val items=readAll()
        val known=SharedLibraryStore.get(prefsContext,kids).resumeEntry(playlist,media.copy(id=next.id,seasonNumber=next.seasonNumber,episodeNumber=next.episodeNumber),items)
        if(known?.completed==true)return@synchronized
        val previous=known?.takeIf{it.inProgress && !it.pendingNext}
        val stamp=maxOf(System.currentTimeMillis(),(items.maxOfOrNull{it.updatedAtMs}?:0L)+1)
        val queued=previous?.copy(updatedAtMs=stamp)?:WatchHistoryEntry(playlist,PlaybackContentType.EPISODE,next.id,series,next.title,
            next.thumbnailUrl,seriesTitle,seriesPoster,next.seasonNumber,next.episodeNumber,0L,next.durationSeconds?.toLong()?.times(1_000L),stamp,pendingNext=true,year=media.year,tmdbId=media.tmdbId)
        writeAll((listOf(queued)+items.filterNot{it.playlistId==playlist && it.contentType==PlaybackContentType.EPISODE && it.contentId==next.id}))
    }

    /** Acao explicita do usuario. Nao toca em favoritos, listas ou credenciais. */
    fun clearType(type: PlaybackContentType) = synchronized(historyLock) {
        require(type == PlaybackContentType.MOVIE || type == PlaybackContentType.EPISODE)
        check(prefs.edit().putBoolean("legacy_cleared_${type.name}", true).commit())
        // Apaga o formato legado antes do unificado para evitar reimportacao.
        val legacy = if (type == PlaybackContentType.MOVIE) "iptvpremium_movies_assistir_v1" else "iptvpremium_series_assistir_v1"
        check(prefsContext.getSharedPreferences(HistoryProfile.preferences(legacy,kids), Context.MODE_PRIVATE).edit().clear().commit())
        writeAll(readAll().filterNot { it.contentType == type })
        changes.value = changes.value + 1
    }

    /** Recupera posicoes da implementacao anterior sem alterar seus dados. */
    fun legacyMoviePositions(playlistId: String): List<Pair<String, Long>> {
        val old = prefsContext.getSharedPreferences(
            HistoryProfile.preferences("iptvpremium_movies_assistir_v1",kids), Context.MODE_PRIVATE
        )
        val prefix = "movie:${playlistId.length}:$playlistId:"
        return old.all.mapNotNull { (key, value) ->
            if (!key.startsWith(prefix)) return@mapNotNull null
            val tail = key.removePrefix(prefix)
            val declaredLength = tail.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
            val id = tail.substringAfter(':', "")
            val position = (value as? Long) ?: return@mapNotNull null
            if (id.isBlank() || id.length != declaredLength || position < 10_000L) null
            else id to position
        }
    }

    /** Recupera o ultimo episodio visto de cada serie. */
    fun legacySeriesPositions(playlistId: String): List<Triple<String, String, Long>> {
        val old = prefsContext.getSharedPreferences(
            HistoryProfile.preferences("iptvpremium_series_assistir_v1",kids), Context.MODE_PRIVATE
        )
        val prefix = "last:${playlistId.length}:$playlistId:"
        return old.all.mapNotNull { (key, value) ->
            if (!key.startsWith(prefix)) return@mapNotNull null
            val tail = key.removePrefix(prefix)
            val declaredLength = tail.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
            val seriesId = tail.substringAfter(':', "")
            val episodeId = (value as? String)?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            if (seriesId.isBlank() || seriesId.length != declaredLength) return@mapNotNull null
            val positionKey = "position:${playlistId.length}:$playlistId:" +
                    "${seriesId.length}:$seriesId:${episodeId.length}:$episodeId"
            val position = old.getLong(positionKey, 0L)
            if (position < 10_000L) null else Triple(seriesId, episodeId, position)
        }
    }

    fun importLegacy(entry: WatchHistoryEntry): Boolean = synchronized(historyLock) {
        if (prefs.getBoolean("legacy_cleared_${entry.contentType.name}", false)) return@synchronized false
        if (entry.playlistId.isBlank() || !entry.inProgress ||
            all(entry.playlistId).any { it.contentType == entry.contentType &&
                    it.contentId == entry.contentId }
        ) return@synchronized false
        writeAll((listOf(entry) + readAll()).sortedByDescending { it.updatedAtMs })
        return@synchronized true
    }

    internal fun checkpointPlaylist(playlist:String, incoming:List<WatchHistoryEntry>) = synchronized(historyLock) {
        require(incoming.all { it.playlistId==playlist })
        val combined=(readAll()+incoming).sortedByDescending { it.updatedAtMs }
            .distinctBy { Triple(it.playlistId,it.contentType,it.contentId) }
        writeAll(combined,durable=true)
    }

    private fun readAll(): List<WatchHistoryEntry> = runCatching {
        val array = JSONArray(prefs.getString("items", "[]") ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val type = runCatching {
                    PlaybackContentType.valueOf(obj.optString("type"))
                }.getOrNull() ?: continue
                val playlist = obj.optString("playlistId")
                val id = obj.optString("contentId")
                if (playlist.isBlank() || id.isBlank()) continue
                add(WatchHistoryEntry(
                    playlistId = playlist,
                    contentType = type,
                    contentId = id,
                    seriesId = obj.optString("seriesId").takeIf { it.isNotBlank() },
                    title = obj.optString("title"),
                    posterUrl = obj.optString("posterUrl").takeIf { it.isNotBlank() },
                    seriesTitle = obj.optString("seriesTitle").takeIf { it.isNotBlank() },
                    seriesPosterUrl = obj.optString("seriesPosterUrl").takeIf { it.isNotBlank() },
                    seasonNumber = obj.optInt("seasonNumber", -1).takeIf { it >= 0 },
                    episodeNumber = obj.optInt("episodeNumber", -1).takeIf { it >= 0 },
                    positionMs = obj.optLong("positionMs").coerceAtLeast(0L),
                    durationMs = obj.optLong("durationMs").takeIf { it > 0L },
                    updatedAtMs = obj.optLong("updatedAtMs"),
                    sharedFromPlaylist = obj.optString("sharedFromPlaylist").takeIf { it.isNotBlank() },
                    seriesOnly=obj.optBoolean("seriesOnly"),finished=obj.optBoolean("finished"),pendingNext=obj.optBoolean("pendingNext"),year=obj.optInt("year").takeIf{it>0},tmdbId=obj.optString("tmdbId").takeIf{it.isNotBlank()}
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun writeAll(items: List<WatchHistoryEntry>, durable:Boolean=false) {
        val array = JSONArray()
        SharedLibraryStore.get(prefsContext,kids).compactHistory(items).take(400).forEach { item ->
            array.put(JSONObject().apply {
                put("playlistId", item.playlistId)
                put("type", item.contentType.name)
                put("contentId", item.contentId)
                put("seriesId", item.seriesId ?: "")
                put("title", item.title)
                put("posterUrl", item.posterUrl ?: "")
                put("seriesTitle", item.seriesTitle ?: "")
                put("seriesPosterUrl", item.seriesPosterUrl ?: "")
                put("seasonNumber", item.seasonNumber ?: -1)
                put("episodeNumber", item.episodeNumber ?: -1)
                put("positionMs", item.positionMs)
                put("durationMs", item.durationMs ?: 0L)
                put("updatedAtMs", item.updatedAtMs)
                put("sharedFromPlaylist",item.sharedFromPlaylist?:"")
                put("seriesOnly",item.seriesOnly);put("finished",item.finished);put("pendingNext",item.pendingNext);put("year",item.year?:0);put("tmdbId",item.tmdbId?:"")
            })
        }
        val edit=prefs.edit().putString("items",array.toString())
        if(durable)check(edit.commit()) { "Falha ao salvar histórico" } else edit.apply()
    }
    companion object {
        private val historyLock = Any()
        val changes = kotlinx.coroutines.flow.MutableStateFlow(0)
    }

}

// IPTV_SETTINGS_APPROVED_GRID_V1
