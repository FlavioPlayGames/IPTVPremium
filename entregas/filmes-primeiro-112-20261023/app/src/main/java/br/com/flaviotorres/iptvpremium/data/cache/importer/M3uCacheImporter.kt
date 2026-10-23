
package br.com.flaviotorres.iptvpremium.data.cache.importer

import android.os.SystemClock
import android.util.Log
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedCategoryEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedChannelEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedEpisodeEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedMovieEntity
import br.com.flaviotorres.iptvpremium.data.cache.entity.CachedSeriesEntity
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

class M3uCacheImporter(
    private val httpClient: PlaylistHttpClient,
    private val cacheDao: PlaylistCacheDao,
    private val generationManager: PlaylistCacheGenerationManager,
    private val deferredStore:M3uDeferredStore? = null,
    private val batchSize: Int = br.com.flaviotorres.iptvpremium.data.cache.ImportTuningState.profile.m3uBatchSize,
    private val batchCharacters: Int = br.com.flaviotorres.iptvpremium.data.cache.ImportTuningState.profile.m3uBatchCharacters
) {

    suspend fun import(
        playlistId: String,
        playlistUrl: String,
        deferOtherSections:Boolean = false
    ): String {
        require(playlistUrl.isNotBlank()) {
            "A URL da lista M3U está vazia."
        }

        val startedAt = SystemClock.elapsedRealtime()

        val generationId = generationManager.beginImport(
            playlistId = playlistId
        )

        var deferred:M3uDeferredStore.Writer?=null
        try {
            if(deferOtherSections)deferred=checkNotNull(deferredStore).writer(generationId)
            importIntoGeneration(
                playlistId = playlistId,
                generationId = generationId,
                playlistUrl = playlistUrl,
                deferred=deferred
            )
            deferred?.finish()
            if(deferred!=null)cacheDao.insertCategories(listOf(
                CachedCategoryEntity(playlistId,generationId,"__live_pending__","","__LIVE_PENDING__","M3U"),
                CachedCategoryEntity(playlistId,generationId,"__series_pending__","","__SERIES_PENDING__","M3U")
            ))

            currentCoroutineContext().ensureActive()
            generationManager.markImportReady(
                playlistId = playlistId,
                generationId = generationId
            )

            Log.i(
                PERF_TAG,
                "Importação M3U concluída. " +
                        "tempoTotalMs=${SystemClock.elapsedRealtime() - startedAt}"
            )

            return generationId
        } catch (error: Throwable) {
            try { generationManager.discardImport(playlistId, generationId) }
            catch (cleanup: Throwable) { if (cleanup !== error) error.addSuppressed(cleanup) }
            withContext(NonCancellable+Dispatchers.IO) {
                if(cacheDao.generationStatus(playlistId,generationId) !in listOf("READY","ACTIVE"))deferredStore?.discard(generationId)
            }
            throw error
        } finally { deferred?.close() }
    }

    suspend fun importDeferred(playlistId:String,active:String,stage:String,type:String,url:String) {
        val store=checkNotNull(deferredStore)
        importIntoGeneration(playlistId,stage,url,onlyType=type,allowEmpty=true,
            consumeLines={consume->if(store.exists(active,type))store.read(active,type,consume)
                else httpClient.readLines(url,consume)})
    }
    fun deferredConsumed(generation:String,type:String){deferredStore?.consumed(generation,type)}

    private suspend fun importIntoGeneration(
        playlistId: String,
        generationId: String,
        playlistUrl: String,
        deferred:M3uDeferredStore.Writer?=null,
        onlyType:String?=null,
        allowEmpty:Boolean=false,
        consumeLines:suspend(suspend(String)->Unit)->Unit={consume->httpClient.readLines(playlistUrl,consume)}
    ) {
        val startedAt = SystemClock.elapsedRealtime()

        val categoryBatch =
            LinkedHashMap<String, CachedCategoryEntity>()

        val channelBatch =
            ArrayList<CachedChannelEntity>(batchSize)

        val movieBatch =
            ArrayList<CachedMovieEntity>(batchSize)

        val seriesBatch =
            LinkedHashMap<String, CachedSeriesEntity>()

        val seriesFirstOrder = HashMap<String, Int>()

        val episodeBatch =
            ArrayList<CachedEpisodeEntity>(batchSize)

        // Categorias já incluídas nesta importação.
        // Evita regravá-las a cada lote de 500 itens.
        val insertedCategoryKeys = HashSet<String>()

        // Uma categoria conserva o mesmo ID por toda a importação.
        // Não precisamos recalcular seu UUID a cada item.
        val categoryIdCache = HashMap<String, String>()

        var pendingExtInf: String? = null
        var channelNumber = 0
        var foundExtInf = false
        var foundStream = false

        var processedEntries = 0L
        var movieCount = 0L
        var seriesEpisodeCount = 0L
        var databaseWriteMs = 0L
        var bufferedCharacters = 0L
        var flushCount = 0L
        var slowestWriteMs = 0L

        fun cachedCategoryId(
            type: String,
            groupName: String
        ): String {
            val key = "$type:$groupName"

            return categoryIdCache.getOrPut(key) {
                stableId("category:$type:$groupName")
            }
        }

        fun registerCategory(
            type: String,
            categoryId: String,
            groupName: String
        ) {
            val key = categoryKey(
                type = type,
                categoryId = categoryId
            )

            if (!insertedCategoryKeys.add(key)) {
                return
            }

            categoryBatch[key] = CachedCategoryEntity(
                playlistId = playlistId,
                generationId = generationId,
                categoryId = categoryId,
                name = groupName,
                type = type,
                source = SOURCE_M3U
            )
        }

        suspend fun flush() {
            if (
                categoryBatch.isEmpty() &&
                channelBatch.isEmpty() &&
                movieBatch.isEmpty() &&
                seriesBatch.isEmpty() &&
                episodeBatch.isEmpty()
            ) {
                return
            }

            val writeStartedAt = SystemClock.elapsedRealtime()

            try {
                currentCoroutineContext().ensureActive()
                cacheDao.insertImportBatch(
                    categoryBatch.values.toList(), channelBatch.toList(), movieBatch.toList(),
                    seriesBatch.values.toList(), episodeBatch.toList()
                )
                // Clear only after the whole transaction commits.
                categoryBatch.clear(); channelBatch.clear(); movieBatch.clear()
                seriesBatch.clear(); episodeBatch.clear()
            } finally {
                val elapsedWrite = SystemClock.elapsedRealtime() - writeStartedAt
                databaseWriteMs += elapsedWrite
                slowestWriteMs = maxOf(slowestWriteMs, elapsedWrite)
            }

            bufferedCharacters = 0L
            flushCount++
        }

        consumeLines { rawLine ->
            currentCoroutineContext().ensureActive()
            if (bufferedCharacters >= batchCharacters) flush()
            bufferedCharacters += rawLine.length
            val line = rawLine
                .removePrefix("\uFEFF")
                .trim()

            if (line.isBlank()) {
                return@consumeLines
            }

            if (
                line.startsWith(
                    "#EXTINF",
                    ignoreCase = true
                )
            ) {
                foundExtInf = true
                pendingExtInf = line
                return@consumeLines
            }

            if (line.startsWith("#")) {
                return@consumeLines
            }

            val extInf = pendingExtInf
                ?: return@consumeLines

            pendingExtInf = null
            foundStream = true
            processedEntries++

            val name = extractDisplayName(extInf)
                .ifBlank { "Conteúdo" }

            val groupName = extractAttribute(
                extInf = extInf,
                attributeName = "group-title"
            )
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_CATEGORY_NAME

            val entryType=classifyEntry(line,groupName,name)
            val entryTypeName=when(entryType){EntryType.LIVE_TV->TYPE_LIVE_TV;EntryType.MOVIE->TYPE_MOVIES;EntryType.SERIES->TYPE_SERIES}
            if(onlyType!=null && entryTypeName!=onlyType)return@consumeLines
            if(deferred!=null && entryType!=EntryType.MOVIE) {
                registerCategory(entryTypeName,cachedCategoryId(entryTypeName,groupName),groupName)
                deferred.append(entryTypeName,extInf,line)
                if(categoryBatch.size>=batchSize)flush()
                return@consumeLines
            }

            val logoUrl = extractAttribute(
                extInf = extInf,
                attributeName = "tvg-logo"
            )
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            val tvgId = extractAttribute(
                extInf = extInf,
                attributeName = "tvg-id"
            )
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            when (entryType) {
                EntryType.LIVE_TV -> {
                    channelNumber++

                    val categoryId = cachedCategoryId(
                        TYPE_LIVE_TV,
                        groupName
                    )

                    val channelId = stableId(
                        "channel:$line:$name:${tvgId.orEmpty()}"
                    )

                    registerCategory(
                        type = TYPE_LIVE_TV,
                        categoryId = categoryId,
                        groupName = groupName
                    )

                    channelBatch += CachedChannelEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        channelId = channelId,
                        name = name,
                        categoryId = categoryId,
                        logoUrl = logoUrl,
                        streamUrl = line,
                        epgChannelId = tvgId,
                        channelNumber = channelNumber
                    )
                }

                EntryType.MOVIE -> {
                    movieCount++

                    val categoryId = cachedCategoryId(
                        TYPE_MOVIES,
                        groupName
                    )

                    val movieId = stableId(
                        "movie:$line:$name"
                    )

                    registerCategory(
                        type = TYPE_MOVIES,
                        categoryId = categoryId,
                        groupName = groupName
                    )

                    movieBatch += CachedMovieEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        movieId = movieId,
                        name = name,
                        categoryId = categoryId,
                        posterUrl = logoUrl,
                        backdropUrl = null,
                        streamUrl = line,
                        year = extractYear(name),
                        rating = null,
                        durationSeconds = null
                    )
                }

                EntryType.SERIES -> {
                    seriesEpisodeCount++

                    val categoryId = cachedCategoryId(
                        TYPE_SERIES,
                        groupName
                    )

                    val episodeInfo = extractEpisodeInfo(name)

                    val seriesName = episodeInfo.seriesName
                        .ifBlank { name }

                    val seriesId = stableId(
                        "series:$groupName:$seriesName"
                    )

                    val episodeId = stableId(
                        "episode:$line:$name"
                    )

                    registerCategory(
                        type = TYPE_SERIES,
                        categoryId = categoryId,
                        groupName = groupName
                    )

                    val seriesProviderOrder =
                        seriesFirstOrder.getOrPut(seriesId) {
                            seriesFirstOrder.size
                        }

                    seriesBatch[seriesId] = CachedSeriesEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        seriesId = seriesId,
                        name = seriesName,
                        categoryId = categoryId,
                        posterUrl = logoUrl,
                        backdropUrl = null,
                        year = extractYear(seriesName),
                        rating = null,
                        plot = null,
                        titleLogoUrl = null,
                        imdbId = null,
                        tmdbId = null,
                        ratingSource = null,
                        durationSeconds = null,
                        genres = null,
                        trailerUrl = null,
                        providerOrder = seriesProviderOrder
                    )

                    episodeBatch += CachedEpisodeEntity(
                        playlistId = playlistId,
                        generationId = generationId,
                        episodeId = episodeId,
                        seriesId = seriesId,
                        seasonNumber = episodeInfo.seasonNumber,
                        episodeNumber = episodeInfo.episodeNumber,
                        name = name,
                        streamUrl = line,
                        posterUrl = logoUrl,
                        durationSeconds = null
                    )
                }
            }

            if (
                channelBatch.size >= batchSize ||
                movieBatch.size >= batchSize ||
                seriesBatch.size >= batchSize ||
                episodeBatch.size >= batchSize ||
                categoryBatch.size >= batchSize
            ) {
                flush()
            }
        }

        flush()

        if (!allowEmpty && (!foundExtInf || !foundStream)) {
            throw IllegalStateException(
                "O endereço informado não retornou uma lista M3U válida."
            )
        }

        val totalMs = SystemClock.elapsedRealtime() - startedAt

        Log.i(
            PERF_TAG,
            "M3U processada: " +
                    "tempoMs=$totalMs " +
                    "gravacaoRoomMs=$databaseWriteMs " +
                    "redeEParsingMs=${(totalMs - databaseWriteMs).coerceAtLeast(0L)} " +
                    "lotes=$flushCount gravacaoPicoMs=$slowestWriteMs " +
                    "entradas=$processedEntries " +
                    "canais=$channelNumber " +
                    "filmes=$movieCount " +
                    "episodios=$seriesEpisodeCount " +
                    "seriesDistintas=${seriesFirstOrder.size} " +
                    "categoriasDistintas=${insertedCategoryKeys.size}"
        )
    }

    private fun classifyEntry(
        streamUrl: String,
        groupName: String,
        name: String
    ): EntryType {
        val url = streamUrl.lowercase(Locale.ROOT)

        // Caminhos explícitos têm prioridade.
        // Evita normalizar títulos e categorias desnecessariamente.
        if (
            "/series/" in url ||
            "/serie/" in url
        ) {
            return EntryType.SERIES
        }

        if (
            "/movie/" in url ||
            "/movies/" in url ||
            "/vod/" in url
        ) {
            return EntryType.MOVIE
        }

        if ("/live/" in url) {
            return EntryType.LIVE_TV
        }

        val group = normalizeForClassification(groupName)

        if (containsAny(group, SERIES_KEYWORDS)) {
            return EntryType.SERIES
        }

        val title = normalizeForClassification(name)

        if (
            containsAny(title, SERIES_TITLE_KEYWORDS) ||
            looksLikeEpisode(name)
        ) {
            return EntryType.SERIES
        }

        if (containsAny(group, MOVIE_KEYWORDS)) {
            return EntryType.MOVIE
        }

        return EntryType.LIVE_TV
    }

    private fun extractEpisodeInfo(
        name: String
    ): EpisodeInfo {
        val compactMatch = COMPACT_EPISODE_REGEX.find(name)

        if (compactMatch != null) {
            val season = compactMatch.groupValues
                .getOrNull(1)
                ?.toIntOrNull()

            val episode = compactMatch.groupValues
                .getOrNull(2)
                ?.toIntOrNull()

            val cleanSeriesName = name
                .removeRange(compactMatch.range)
                .replace(TRAILING_SERIES_SEPARATOR_REGEX, "")
                .trim()

            return EpisodeInfo(
                seriesName = cleanSeriesName,
                seasonNumber = season,
                episodeNumber = episode
            )
        }

        val verboseMatch = VERBOSE_EPISODE_REGEX.find(name)

        if (verboseMatch != null) {
            val season = verboseMatch.groupValues
                .getOrNull(1)
                ?.toIntOrNull()

            val episode = verboseMatch.groupValues
                .getOrNull(2)
                ?.toIntOrNull()

            val cleanSeriesName = name
                .substring(0, verboseMatch.range.first)
                .replace(TRAILING_SERIES_SEPARATOR_REGEX, "")
                .trim()

            return EpisodeInfo(
                seriesName = cleanSeriesName,
                seasonNumber = season,
                episodeNumber = episode
            )
        }

        return EpisodeInfo(
            seriesName = name.trim(),
            seasonNumber = null,
            episodeNumber = null
        )
    }

    private fun looksLikeEpisode(
        name: String
    ): Boolean {
        return COMPACT_EPISODE_REGEX.containsMatchIn(name) ||
                VERBOSE_EPISODE_REGEX.containsMatchIn(name)
    }

    private fun extractYear(
        value: String
    ): Int? {
        return YEAR_REGEX.find(value)
            ?.value
            ?.toIntOrNull()
    }

    private fun normalizeForClassification(
        value: String
    ): String {
        return value
            .lowercase(Locale.ROOT)
            .replace('á', 'a')
            .replace('à', 'a')
            .replace('ã', 'a')
            .replace('â', 'a')
            .replace('ä', 'a')
            .replace('é', 'e')
            .replace('è', 'e')
            .replace('ê', 'e')
            .replace('ë', 'e')
            .replace('í', 'i')
            .replace('ì', 'i')
            .replace('î', 'i')
            .replace('ï', 'i')
            .replace('ó', 'o')
            .replace('ò', 'o')
            .replace('õ', 'o')
            .replace('ô', 'o')
            .replace('ö', 'o')
            .replace('ú', 'u')
            .replace('ù', 'u')
            .replace('û', 'u')
            .replace('ü', 'u')
            .replace('ç', 'c')
    }

    private fun containsAny(
        value: String,
        keywords: Set<String>
    ): Boolean {
        return keywords.any { keyword ->
            keyword in value
        }
    }

    private fun extractDisplayName(
        extInf: String
    ): String {
        val commaIndex = extInf.lastIndexOf(',')

        if (
            commaIndex < 0 ||
            commaIndex >= extInf.lastIndex
        ) {
            return ""
        }

        return extInf
            .substring(commaIndex + 1)
            .trim()
    }

    private fun extractAttribute(
        extInf: String,
        attributeName: String
    ): String? {
        val pattern = when (attributeName) {
            "group-title" -> GROUP_TITLE_ATTRIBUTE_REGEX
            "tvg-logo" -> TVG_LOGO_ATTRIBUTE_REGEX
            "tvg-id" -> TVG_ID_ATTRIBUTE_REGEX

            else -> Regex(
                """(?:^|\s)${Regex.escape(attributeName)}\s*=\s*"([^"]*)"""",
                RegexOption.IGNORE_CASE
            )
        }

        return pattern.find(extInf)
            ?.groupValues
            ?.getOrNull(1)
    }

    private fun stableId(
        value: String
    ): String {
        return UUID.nameUUIDFromBytes(
            value.toByteArray(StandardCharsets.UTF_8)
        ).toString()
    }

    private fun categoryKey(
        type: String,
        categoryId: String
    ): String {
        return "$type:$categoryId"
    }

    private data class EpisodeInfo(
        val seriesName: String,
        val seasonNumber: Int?,
        val episodeNumber: Int?
    )

    private enum class EntryType {
        LIVE_TV,
        MOVIE,
        SERIES
    }

    private companion object {
        const val PERF_TAG = "M3uImportPerf"
        const val DEFAULT_CATEGORY_NAME = "Sem categoria"

        const val TYPE_LIVE_TV = "LIVE_TV"
        const val TYPE_MOVIES = "MOVIES"
        const val TYPE_SERIES = "SERIES"

        const val SOURCE_M3U = "M3U"

        // Compiladas uma única vez, em vez de uma vez por entrada.
        val GROUP_TITLE_ATTRIBUTE_REGEX = Regex(
            "(?:^|\\s)group-title\\s*=\\s*\"([^\"]*)\"",
            RegexOption.IGNORE_CASE
        )

        val TVG_LOGO_ATTRIBUTE_REGEX = Regex(
            "(?:^|\\s)tvg-logo\\s*=\\s*\"([^\"]*)\"",
            RegexOption.IGNORE_CASE
        )

        val TVG_ID_ATTRIBUTE_REGEX = Regex(
            "(?:^|\\s)tvg-id\\s*=\\s*\"([^\"]*)\"",
            RegexOption.IGNORE_CASE
        )

        val COMPACT_EPISODE_REGEX = Regex(
            """(?i)\bS(\d{1,2})\s*E(\d{1,3})\b"""
        )

        val VERBOSE_EPISODE_REGEX = Regex(
            """(?i)\b(?:temporada|season)\s*(\d{1,2}).*?\b(?:epis[oó]dio|episode|ep)\s*(\d{1,3})\b"""
        )

        val TRAILING_SERIES_SEPARATOR_REGEX = Regex(
            """[\s._\-–—|]+$"""
        )

        val YEAR_REGEX = Regex(
            """\b(19|20)\d{2}\b"""
        )

        val MOVIE_KEYWORDS = setOf(
            "filme",
            "filmes",
            "movie",
            "movies",
            "cinema",
            "vod",
            "lancamentos",
            "lancamento"
        )

        val SERIES_KEYWORDS = setOf(
            "serie",
            "series",
            "seriado",
            "seriados",
            "temporada",
            "season"
        )

        val SERIES_TITLE_KEYWORDS = setOf(
            "temporada",
            "season",
            "episodio",
            "episode"
        )
    }
}
