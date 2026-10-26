
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

class M3uCacheImporter(
    private val httpClient: PlaylistHttpClient,
    private val cacheDao: PlaylistCacheDao,
    private val generationManager: PlaylistCacheGenerationManager,
    private val batchSize: Int = 2000,
    private val batchCharacters: Int = br.com.flaviotorres.iptvpremium.data.cache.ImportTuningState.profile.m3uBatchCharacters
) {

    suspend fun import(
        playlistId: String,
        playlistUrl: String
    ): String {
        require(playlistUrl.isNotBlank()) {
            "A URL da lista M3U está vazia."
        }

        val startedAt = SystemClock.elapsedRealtime()

        Log.i(PERF_TAG,"fase=INICIO formato=M3U revisao=M3U_METADATA_R3")
        val generationId = generationManager.beginImport(
            playlistId = playlistId
        )

        Log.i(PERF_TAG,"fase=GERACAO_CRIADA decorridoMs=${SystemClock.elapsedRealtime()-startedAt}")
        try {
            importIntoGeneration(
                playlistId = playlistId,
                generationId = generationId,
                playlistUrl = playlistUrl
            )

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
            Log.w(PERF_TAG,"fase=INTERROMPIDA erro=${error.javaClass.simpleName} decorridoMs=${SystemClock.elapsedRealtime()-startedAt}")
            try { generationManager.discardImport(playlistId, generationId) }
            catch (cleanup: Throwable) { if (cleanup !== error) error.addSuppressed(cleanup) }
            throw error
        }
    }

    private suspend fun importIntoGeneration(
        playlistId: String,
        generationId: String,
        playlistUrl: String
    ) = coroutineScope {
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

        require(batchSize > 0 && batchCharacters > 0)
        var previousSeriesGroup: String? = null
        var previousSeriesName: String? = null
        var previousSeriesId: String? = null
        var previousSeriesRecord: CachedSeriesEntity? = null
        var seriesIdReused = 0L
        var seriesUpserts = 0L
        var bufferedEntries = 0
        var peakBufferedEntries = 0
        var queueWaitMs = 0L
        var parsingNs = 0L
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

        val writes = Channel<ImportBatch>(capacity = 1)
        val writer = launch(Dispatchers.IO) {
            for(batch in writes) {
                currentCoroutineContext().ensureActive()
                val writeStartedAt=SystemClock.elapsedRealtime()
                cacheDao.insertImportBatch(batch.categories,batch.channels,batch.movies,batch.series,batch.episodes)
                val elapsed=SystemClock.elapsedRealtime()-writeStartedAt
                databaseWriteMs+=elapsed
                slowestWriteMs=maxOf(slowestWriteMs,elapsed)
                flushCount++
                Log.i(PERF_TAG,"fase=GRAVACAO lote=$flushCount entradas=${batch.channels.size+batch.movies.size+batch.episodes.size} series=${batch.series.size} canais=${batch.channels.size} filmes=${batch.movies.size} episodios=${batch.episodes.size} gravacaoMs=$elapsed decorridoMs=${SystemClock.elapsedRealtime()-startedAt}")
            }
        }
        Log.i(PERF_TAG,"config=M3U_METADATA_R3 recordsLimit=$batchSize charactersLimit=$batchCharacters filaGravacao=1")
        suspend fun flush() {
            if(categoryBatch.isEmpty() && channelBatch.isEmpty() && movieBatch.isEmpty() && seriesBatch.isEmpty() && episodeBatch.isEmpty())return
            val batch=ImportBatch(categoryBatch.values.toList(),channelBatch.toList(),movieBatch.toList(),seriesBatch.values.toList(),episodeBatch.toList())
            val waitStarted=SystemClock.elapsedRealtime()
            writes.send(batch)
            queueWaitMs+=SystemClock.elapsedRealtime()-waitStarted
            categoryBatch.clear();channelBatch.clear();movieBatch.clear();seriesBatch.clear();episodeBatch.clear()
            bufferedCharacters=0L;bufferedEntries=0
        }

        httpClient.readLines(
            url = playlistUrl
        ) { rawLine ->
            currentCoroutineContext().ensureActive()
            if (bufferedCharacters >= batchCharacters) flush()
            bufferedCharacters += rawLine.length
            val parseStarted=System.nanoTime()
            try {
            val line = rawLine
                .removePrefix("\uFEFF")
                .trim()

            if (line.isBlank()) {
                return@readLines
            }

            if (
                line.startsWith(
                    "#EXTINF",
                    ignoreCase = true
                )
            ) {
                foundExtInf = true
                pendingExtInf = line
                return@readLines
            }

            if (line.startsWith("#")) {
                return@readLines
            }

            val extInf = pendingExtInf
                ?: return@readLines

            pendingExtInf = null
            foundStream = true
            processedEntries++
            bufferedEntries++
            peakBufferedEntries=maxOf(peakBufferedEntries,bufferedEntries)

            val name = extractDisplayName(extInf)
                .ifBlank { "Conteúdo" }

            val groupName = extractAttribute(
                extInf = extInf,
                attributeName = "group-title"
            )
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: DEFAULT_CATEGORY_NAME

            val logoUrl = extractAttribute(
                extInf = extInf,
                attributeName = "tvg-logo"
            )
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            val rawTvgId = extractAttribute(extInf,"tvg-id")
            val tvgId = rawTvgId?.trim()?.takeIf { it.isNotBlank() }
            val hasXuiId = XUI_ID_ATTRIBUTE_REGEX.containsMatchIn(extInf)

            when (
                classifyEntry(
                    streamUrl = line,
                    groupName = groupName,
                    name = name,
                    hasXuiId = hasXuiId,
                    hasTvgIdAttribute = rawTvgId != null
                )
            ) {
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

                    val seriesId=if(previousSeriesGroup==groupName && previousSeriesName==seriesName) {
                        seriesIdReused++;checkNotNull(previousSeriesId)
                    } else stableId("series:$groupName:$seriesName").also {
                        previousSeriesGroup=groupName;previousSeriesName=seriesName;previousSeriesId=it
                    }

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

                    val previous=previousSeriesRecord
                    val unchanged=previous!=null && previous.seriesId==seriesId && previous.name==seriesName &&
                        previous.categoryId==categoryId && previous.posterUrl==logoUrl
                    if(!unchanged) {
                    val record=CachedSeriesEntity(
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
                    seriesBatch[seriesId]=record;previousSeriesRecord=record;seriesUpserts++
                    }

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

            } finally {parsingNs+=System.nanoTime()-parseStarted}
            if (
                bufferedEntries >= batchSize ||
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
        writes.close()
        writer.join()

        if (!foundExtInf || !foundStream) {
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
                    "parsingMs=${parsingNs/1_000_000} esperaFilaMs=$queueWaitMs sobreposicao=true " +
                    "lotes=$flushCount gravacaoPicoMs=$slowestWriteMs " +
                    "entradas=$processedEntries " +
                    "canais=$channelNumber " +
                    "filmes=$movieCount " +
                    "episodios=$seriesEpisodeCount " +
                    "seriesDistintas=${seriesFirstOrder.size} " +
                    "categoriasDistintas=${insertedCategoryKeys.size} " +
                    "seriesPreparadas=$seriesUpserts idsSerieReutilizados=$seriesIdReused entradasPicoLote=$peakBufferedEntries limiteEntradas=$batchSize limiteCaracteres=$batchCharacters"
        )
    }

    private data class ImportBatch(
        val categories:List<CachedCategoryEntity>,val channels:List<CachedChannelEntity>,
        val movies:List<CachedMovieEntity>,val series:List<CachedSeriesEntity>,val episodes:List<CachedEpisodeEntity>
    )

    private fun classifyEntry(streamUrl:String,groupName:String,name:String,hasXuiId:Boolean,hasTvgIdAttribute:Boolean):EntryType =
        when(M3uEntryClassifier.classify(streamUrl,groupName,name,hasXuiId,hasTvgIdAttribute)) {
            M3uEntryClassifier.Kind.LIVE_TV->EntryType.LIVE_TV
            M3uEntryClassifier.Kind.MOVIE->EntryType.MOVIE
            M3uEntryClassifier.Kind.SERIES->EntryType.SERIES
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
        val XUI_ID_ATTRIBUTE_REGEX = Regex("""(?:^|\s)xui-id\s*=""",RegexOption.IGNORE_CASE)
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
            """(?i)\bS(\d{1,3})\s*E(\d{1,6})\b"""
        )

        val VERBOSE_EPISODE_REGEX = Regex(
            """(?i)\b(?:temporada|season)\s*(\d{1,3}).*?\b(?:epis[oó]dio|episode|ep)\s*(\d{1,6})\b"""
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
