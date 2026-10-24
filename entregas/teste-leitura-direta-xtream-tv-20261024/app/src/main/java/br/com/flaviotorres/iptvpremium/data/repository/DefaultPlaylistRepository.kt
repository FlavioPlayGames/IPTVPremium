package br.com.flaviotorres.iptvpremium.data.repository

import android.util.Log
import android.util.Base64
import android.os.SystemClock
import br.com.flaviotorres.iptvpremium.data.cache.importer.M3uCacheImporter
import br.com.flaviotorres.iptvpremium.data.parser.M3uParser
import br.com.flaviotorres.iptvpremium.data.parser.XtreamParser
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.remote.TmdbClientProvider
import br.com.flaviotorres.iptvpremium.data.resolver.M3uXtreamResolver
import br.com.flaviotorres.iptvpremium.core.player.PlaybackEpgProgram
import br.com.flaviotorres.iptvpremium.domain.model.MovieCastMemberDetails
import br.com.flaviotorres.iptvpremium.domain.model.MovieDetails
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.domain.model.RatingSource
import br.com.flaviotorres.iptvpremium.domain.repository.PlaylistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlin.coroutines.coroutineContext
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.nio.charset.StandardCharsets

private const val XTREAM_SYNC_TAG = "XtreamCatalogSync"

private const val TMDB_ENRICHMENT_TAG =
    "TmdbEnrichment"

private const val TMDB_IMAGE_BASE_URL =
    "https://image.tmdb.org/t/p"

class DefaultPlaylistRepository(
    private val httpClient: PlaylistHttpClient,
    private val m3uParser: M3uParser,
    private val xtreamParser: XtreamParser,
    private val m3uCacheImporter: M3uCacheImporter? = null,
    private val movieLookup: (suspend (String, String) -> br.com.flaviotorres.iptvpremium.domain.model.Movie?)? = null
) : PlaylistRepository, br.com.flaviotorres.iptvpremium.data.sync.ProgressiveXtreamRepository {

    private data class VodFullSnapshot(
        val sourceKey: String,
        val allIds: Set<String>,
        val withoutCategoryIds: Set<String>,
        val unknownCategoryIds: Set<String>,
        val extraCategoryIds: Set<String>,
        val savedAtMs: Long
    )

    private companion object {
        private const val VOD_AUDIT_WINDOW_MS =
            10L * 60L * 1000L

        private val vodAuditLock = Any()

        // Guarda somente IDs da ultima resposta completa.
        // Nunca guarda URLs de reproducao ou filmes completos.
        private var lastVodFullSnapshot: VodFullSnapshot? = null

        private const val SERIES_FULL_RECHECK_MS =
            10L * 60L * 1000L

        // Memoriza somente a preferencia de recuperacao.
        // Maximo de 32 servidores/contas em memoria.
        private val seriesFallbackUntilMs =
            object : LinkedHashMap<String, Long>(32, 0.75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<String, Long>?
                ): Boolean = size > 32
            }
    }

    override suspend fun loadContent(
        source: PlaylistSource
    ): PlaylistContent {
        return when (source) {
            is PlaylistSource.M3u -> {
                loadM3uContent(source)
            }

            is PlaylistSource.XtreamCodes -> {
                loadXtreamContent(source)
            }
        }
    }

    /*
     * EPG REAL DOS CANAIS — consulta sob demanda (sem migração Room).
     *
     * O Xtream fornece a programação pelos endpoints de um único stream:
     * get_simple_data_table (grade do canal) e get_short_epg (fallback).
     *
     * Uma M3U compatível com get.php é resolvida em memória pelo mesmo
     * M3uXtreamResolver já utilizado para os catálogos. Uma M3U genérica
     * necessita de uma fonte XMLTV independente; não inventamos horários.
     *
     * Nunca registramos URL, usuário ou senha em logs. A busca fica fora
     * do player e usa o cliente HTTP assíncrono já existente no projeto.
     */
    suspend fun loadChannelEpg(
        source: PlaylistSource,
        channelId: String,
        limit: Int = 80
    ): List<PlaybackEpgProgram> {
        val safeId = channelId.trim()
        if (safeId.isEmpty()) return emptyList()

        val xtream = when (source) {
            is PlaylistSource.XtreamCodes -> source
            is PlaylistSource.M3u -> M3uXtreamResolver.resolve(source)
        } ?: return emptyList()

        if (!xtream.isEnabled ||
            xtream.username.isBlank() ||
            xtream.password.isBlank()
        ) return emptyList()

        val safeLimit = limit.coerceIn(1, 200)
        val cacheKey = buildString {
            append(source.id.length).append(':').append(source.id)
            append(':').append(safeId.length).append(':').append(safeId)
            append(':').append(safeLimit)
        }
        val elapsedNow = SystemClock.elapsedRealtime()
        val cached = synchronized(epgCache) {
            epgCache[cacheKey]
                ?.takeIf { it.expiresElapsedMs > elapsedNow }
                ?.programs
        }
        if (cached != null) return cached.take(safeLimit)

        val baseUrl = normalizeServerUrl(xtream.serverUrl)
        val encodedUser = encodeQueryValue(xtream.username.trim())
        val encodedPassword = encodeQueryValue(xtream.password)
        val encodedChannel = encodeQueryValue(safeId)

        var available = emptyList<PlaybackEpgProgram>()
        for (action in listOf("get_simple_data_table", "get_short_epg")) {
            coroutineContext.ensureActive()
            val url = buildPlayerApiUrl(
                baseUrl = baseUrl,
                username = encodedUser,
                password = encodedPassword,
                action = action
            ) + "&stream_id=$encodedChannel" +
                    if (action == "get_short_epg") "&limit=$safeLimit" else ""

            available = try {
                val json = httpClient.get(url)
                withContext(Dispatchers.Default) {
                    parseXtreamChannelEpg(json, System.currentTimeMillis()).take(200)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Nunca incluir error.message: alguns servidores ecoam a URL.
                Log.w("LiveEpg", "Consulta EPG indisponível ($action): " +
                        error.javaClass.simpleName)
                emptyList()
            }

            if (available.isNotEmpty()) break
        }

        // Mantém memória limitada mesmo após passar por muitos canais.
        val ttlMs = if (available.isEmpty()) 60_000L else 300_000L
        synchronized(epgCache) {
            epgCache[cacheKey] = CachedEpg(
                programs = available,
                expiresElapsedMs = SystemClock.elapsedRealtime() + ttlMs
            )
        }
        return available.take(safeLimit)
    }

    private data class CachedEpg(
        val programs: List<PlaybackEpgProgram>,
        val expiresElapsedMs: Long
    )

    private val epgCache = object : LinkedHashMap<String, CachedEpg>(64, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, CachedEpg>?
        ): Boolean = size > 64
    }

    private fun parseXtreamChannelEpg(
        rawJson: String,
        currentEpochMs: Long
    ): List<PlaybackEpgProgram> {
        val root = JSONObject(rawJson)
        val array = root.optJSONArray("epg_listings")
            ?: root.optJSONObject("data")?.optJSONArray("epg_listings")
            ?: return emptyList()

        val results = ArrayList<PlaybackEpgProgram>()
        val seen = HashSet<String>()
        val earliestEnd = currentEpochMs - 60L * 60L * 1000L
        val latestStart = currentEpochMs + 7L * 24L * 60L * 60L * 1000L

        // Limite defensivo: nunca materializar guias gigantes no app.
        for (index in 0 until minOf(array.length(), 1500)) {
            val item = array.optJSONObject(index) ?: continue
            val start = parseEpgTime(item.opt("start_timestamp"))
                ?: parseEpgTime(item.opt("start"))
                ?: continue
            val end = parseEpgTime(item.opt("stop_timestamp"))
                ?: parseEpgTime(item.opt("end_timestamp"))
                ?: parseEpgTime(item.opt("end"))
                ?: continue
            if (end <= start || end < earliestEnd || start > latestStart) continue

            val title = decodeEpgLabel(item.optString("title")).trim()
                .takeIf { it.isNotEmpty() }
                ?: continue
            val key = "$start:$end:$title"
            if (!seen.add(key)) continue

            val description = decodeEpgLabel(item.optString("description"))
                .trim().takeIf { it.isNotEmpty() }?.take(1200)
            results.add(
                PlaybackEpgProgram(
                    title = title.take(300),
                    startEpochMs = start,
                    endEpochMs = end,
                    description = description
                )
            )
        }
        return results.sortedWith(
            compareBy<PlaybackEpgProgram> { it.startEpochMs }
                .thenBy { it.endEpochMs }
        )
    }

    private fun parseEpgTime(value: Any?): Long? {
        val raw = value?.toString()?.trim().orEmpty()
        if (raw.isEmpty() || raw.equals("null", true)) return null

        // Xtream costuma enviar segundos Unix como String.
        val numeric = raw.toLongOrNull()
        if (numeric != null) {
            return when (numeric) {
                in 946_684_800L..4_102_444_800L -> numeric * 1000L
                in 946_684_800_000L..4_102_444_800_000L -> numeric
                else -> null
            }
        }

        // Preferir datas com fuso explícito. Sem fuso, somente fallback
        // (o servidor pode usar um fuso diferente do dispositivo).
        try {
            return Instant.parse(raw).toEpochMilli()
        } catch (_: Exception) {
        }
        try {
            return OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        } catch (_: Exception) {
        }
        try {
            val formatter = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm:ss", Locale.ROOT
            )
            return LocalDateTime.parse(raw, formatter)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            return null
        }
    }

    private fun decodeEpgLabel(value: String): String {
        val text = value.trim()
        if (text.length < 8 ||
            text.any { !it.isLetterOrDigit() && it !in "+/=_-" }
        ) return text

        return try {
            val bytes = Base64.decode(text, Base64.DEFAULT)
            val decoded = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString().trim()
            if (decoded.isNotBlank() && decoded.none {
                    it.isISOControl() && it != '\n' && it != '\t' && it != '\r'
                }) decoded else text
        } catch (_: Exception) {
            text
        }
    }

    /*
     * DETALHES DE UM ÚNICO FILME
     *
     * A consulta é feita somente quando o filme é aberto.
     * Nenhum metadado enriquecido é baixado em massa durante a
     * sincronização do catálogo.
     *
     * - Xtream Codes direto: usa get_vod_info;
     * - M3U Xtream-compatible: resolve as credenciais apenas em memória
     *   e usa o mesmo endpoint;
     * - M3U genérica: não possui endpoint estruturado de detalhes.
     *
     * Quando o provedor entrega um tmdbId válido, os dados do TMDB são
     * consultados somente para o filme aberto e usados como enriquecimento.
     * Os dados do provedor continuam tendo prioridade quando já existem.
     */
    suspend fun loadMovieDetails(
        source: PlaylistSource,
        movieId: String
    ): MovieDetails? {
        require(movieId.isNotBlank()) {
            "O ID do filme está vazio."
        }

        return when (source) {
            is PlaylistSource.XtreamCodes -> {
                loadXtreamMovieDetails(
                    source = source,
                    movieId = movieId
                )
            }

            is PlaylistSource.M3u -> {
                loadM3uMovieDetails(
                    source = source,
                    movieId = movieId
                )
            }
        }
    }

    /** Identity comparison needs only the provider payload, without TMDB artwork/cast enrichment. */
    suspend fun loadMovieIdentityDetails(source: PlaylistSource, movieId: String): MovieDetails? {
        require(movieId.isNotBlank())
        val xtream = when (source) {
            is PlaylistSource.XtreamCodes -> source
            is PlaylistSource.M3u -> M3uXtreamResolver.resolve(source) ?: return null
        }
        return loadXtreamMovieDetails(xtream, movieId, enrich = false)
    }

    private suspend fun loadM3uMovieDetails(
        source: PlaylistSource.M3u,
        movieId: String
    ): MovieDetails? {
        val resolvedXtreamSource =
            M3uXtreamResolver.resolve(
                source = source
            ) ?: return movieLookup?.invoke(source.id, movieId)?.let { movie ->
            enrichMovieDetailsWithTmdb(MovieDetails(movieId = movieId, providerTitle = movie.name,
                year = movie.year, posterUrl = movie.posterUrl, backdropUrl = movie.backdropUrl))
        }

        return loadXtreamMovieDetails(
            source = resolvedXtreamSource,
            movieId = movieId
        )
    }

    private suspend fun loadM3uContent(
        source: PlaylistSource.M3u
    ): PlaylistContent {
        val playlistUrl =
            source.playlistUrl.trim()

        require(playlistUrl.isNotBlank()) {
            "A URL da lista M3U está vazia."
        }

        /*
         * CAMINHO PREFERENCIAL:
         *
         * Se a URL M3U for, na verdade, um endpoint get.php de um servidor
         * Xtream-compatible, extraímos servidor/usuário/senha somente em
         * memória e usamos a API Xtream.
         *
         * Isso evita baixar e percorrer uma M3U+ enorme contendo canais,
         * filmes e episódios quando o mesmo servidor já oferece dados
         * estruturados pela player_api.php.
         *
         * Se a detecção não for possível ou a API Xtream falhar, seguimos
         * normalmente para o importador M3U nativo abaixo.
         */
        val resolvedXtreamSource =
            M3uXtreamResolver.resolve(
                source = source
            )

        if (resolvedXtreamSource != null) {
            val xtreamContent =
                runCatching {
                    loadXtreamContent(
                        source = resolvedXtreamSource
                    )
                }.getOrNull()

            if (xtreamContent != null) {
                return xtreamContent
            }
        }

        /*
         * CAMINHO M3U NATIVO:
         *
         * Se a fonte não puder ser tratada como Xtream, a lista é lida
         * em streaming e gravada no Room em lotes, sem criar uma String
         * gigante na memória.
         */
        val cacheImporter =
            m3uCacheImporter

        if (cacheImporter != null) {
            cacheImporter.import(
                playlistId = source.id,
                playlistUrl = playlistUrl
            )

            /*
             * Neste ponto o conteúdo já está persistido no Room.
             *
             * O conteúdo M3U nativo permanece persistido no Room.
             * As telas conectadas ao catálogo local fazem a leitura via
             * Repository + Paging, sem reconstruir tudo em memória.
             */
            return PlaylistContent(
                categories = emptyList(),
                channels = emptyList(),
                movies = emptyList(),
                series = emptyList()
            )
        }

        /*
         * CAMINHO ANTIGO TEMPORÁRIO:
         *
         * Mantido somente para não quebrar o aplicativo enquanto
         * terminamos a integração do Room com as telas.
         *
         * Assim que o leitor do cache estiver pronto, o AppNavigation
         * passará o M3uCacheImporter e este bloco deixará de ser usado.
         */
        val rawContent =
            httpClient.get(playlistUrl)

        val normalizedContent =
            rawContent
                .removePrefix("\uFEFF")
                .trimStart()

        if (
            !normalizedContent.startsWith("#EXTM3U") &&
            !normalizedContent.contains("#EXTINF")
        ) {
            throw IllegalStateException(
                "O endereço informado não retornou uma lista M3U válida."
            )
        }

        return withContext(Dispatchers.Default) {
            m3uParser.parse(
                rawContent
            )
        }
    }

    private suspend fun loadXtreamMovieDetails(
        source: PlaylistSource.XtreamCodes,
        movieId: String,
        enrich: Boolean = true
    ): MovieDetails? {
        val baseUrl =
            normalizeServerUrl(
                source.serverUrl
            )

        val username =
            source.username.trim()

        val password =
            source.password

        require(username.isNotBlank()) {
            "O usuário do Xtream Codes está vazio."
        }

        require(password.isNotBlank()) {
            "A senha do Xtream Codes está vazia."
        }

        val encodedUsername =
            encodeQueryValue(
                username
            )

        val encodedPassword =
            encodeQueryValue(
                password
            )

        val providerDetails =
            fetchAndParse(
                url =
                    buildVodInfoUrl(
                        baseUrl = baseUrl,
                        username = encodedUsername,
                        password = encodedPassword,
                        movieId = movieId
                    )
            ) { content ->
                xtreamParser.parseMovieDetails(
                    content = content,
                    movieId = movieId
                )
            } ?: return null

        if (!enrich) return providerDetails
        val local = if (providerDetails.providerTitle.isNullOrBlank() || providerDetails.year == null)
            movieLookup?.invoke(source.id, movieId) else null
        return enrichMovieDetailsWithTmdb(providerDetails.copy(
            providerTitle = providerDetails.providerTitle ?: local?.name, year = providerDetails.year ?: local?.year))
    }

    /*
     * ENRIQUECIMENTO TMDB SOB DEMANDA
     *
     * O Xtream continua sendo a fonte primária.
     * O TMDB somente completa informações ausentes e acrescenta:
     *
     * - logo oficial do título;
     * - elenco estruturado;
     * - foto dos atores;
     * - personagem;
     * - alguns fallbacks de metadados.
     *
     * Se o TMDB falhar, o filme continua abrindo normalmente apenas
     * com os dados do provedor.
     */
    private suspend fun enrichMovieDetailsWithTmdb(
        details: MovieDetails
    ): MovieDetails {
        val root = try {
            kotlinx.coroutines.withTimeoutOrNull(5_000L) {
                resolveMovieArtworkIdentity(details.providerTitle.orEmpty(), details.year,
                    details.tmdbId, details.imdbId, TmdbClientProvider.client::get)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            Log.w(TMDB_ENRICHMENT_TAG, "Metadados de filme indisponiveis: ${error.javaClass.simpleName}")
            null
        } ?: return details

        val credits =
            root.optJSONObject("credits")

        val castMembers =
            extractTmdbCastMembers(
                credits = credits
            )

        val tmdbCastNames =
            castMembers
                .map { it.name }
                .distinct()

        val tmdbDirector =
            extractTmdbDirectors(
                credits = credits
            )

        val tmdbLogoUrl =
            extractTmdbTitleLogoUrl(
                images = root.optJSONObject("images")
            )

        val tmdbPosterUrl =
            buildTmdbImageUrl(
                size = "w500",
                path = root.optString("poster_path")
            )

        val tmdbBackdropUrl =
            buildTmdbImageUrl(
                size = "w1280",
                path = root.optString("backdrop_path")
            )

        val tmdbPlot =
            root
                .optString("overview")
                .trim()
                .takeIf { it.isNotBlank() }

        val tmdbYear =
            extractYear(
                root.optString("release_date")
            )

        val tmdbRating =
            root
                .optDouble("vote_average", Double.NaN)
                .takeIf {
                    !it.isNaN() &&
                            it > 0.0
                }

        val tmdbDurationSeconds =
            root
                .optInt("runtime", 0)
                .takeIf { it > 0 }
                ?.times(60)

        val tmdbImdbId =
            root
                .optString("imdb_id")
                .trim()
                .takeIf {
                    it.isNotBlank() &&
                            !it.equals(
                                "null",
                                ignoreCase = true
                            )
                }

        return details.copy(
            tmdbId = root.optInt("id", -1).takeIf { it > 0 }?.toString() ?: details.tmdbId,
            localizedTitle = root.optString("title").takeIf { it.isNotBlank() },
            genres = root.optJSONArray("genres")?.let { array -> (0 until array.length()).mapNotNull {
                array.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) } }?.takeIf { it.isNotEmpty() } ?: details.genres,
            plot = tmdbPlot ?: details.plot,

            cast =
                details.cast
                    .takeIf { it.isNotEmpty() }
                    ?: tmdbCastNames,

            castMembers =
                castMembers
                    .takeIf { it.isNotEmpty() }
                    ?: details.castMembers,

            director =
                details.director
                    ?.takeIf { it.isNotBlank() }
                    ?: tmdbDirector,

            posterUrl =
                details.posterUrl
                    ?.takeIf { it.isNotBlank() }
                    ?: tmdbPosterUrl,

            backdropUrl =
                details.backdropUrl
                    ?.takeIf { it.isNotBlank() }
                    ?: tmdbBackdropUrl,

            titleLogoUrl =
                details.titleLogoUrl
                    ?.takeIf { it.isNotBlank() }
                    ?: tmdbLogoUrl,

            year =
                details.year
                    ?: tmdbYear,

            rating =
                details.rating
                    ?: tmdbRating,

            ratingSource =
                details.ratingSource
                    ?: if (tmdbRating != null) {
                        RatingSource.TMDB
                    } else {
                        null
                    },

            durationSeconds =
                details.durationSeconds
                    ?: tmdbDurationSeconds,

            imdbId =
                details.imdbId
                    ?.takeIf { it.isNotBlank() }
                    ?: tmdbImdbId
        )
    }

    private fun extractTmdbCastMembers(
        credits: JSONObject?
    ): List<MovieCastMemberDetails> {
        val castArray =
            credits
                ?.optJSONArray("cast")
                ?: return emptyList()

        val result =
            ArrayList<MovieCastMemberDetails>(
                minOf(
                    castArray.length(),
                    20
                )
            )

        for (
        index in 0 until minOf(
            castArray.length(),
            20
        )
        ) {
            val item =
                castArray.optJSONObject(index)
                    ?: continue

            val personId =
                item.optLong(
                    "id",
                    -1L
                )

            val name =
                item
                    .optString("name")
                    .trim()

            if (
                personId <= 0L ||
                name.isBlank()
            ) {
                continue
            }

            val profileUrl =
                buildTmdbImageUrl(
                    size = "w342",
                    path =
                        item.optString(
                            "profile_path"
                        )
                )

            val characterName =
                item
                    .optString("character")
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    }

            result +=
                MovieCastMemberDetails(
                    id = personId.toString(),
                    name = name,
                    profileUrl = profileUrl,
                    characterName = characterName,
                    tmdbId = personId.toString()
                )
        }

        return result
    }

    private fun extractTmdbDirectors(
        credits: JSONObject?
    ): String? {
        val crewArray =
            credits
                ?.optJSONArray("crew")
                ?: return null

        val directors =
            LinkedHashSet<String>()

        for (index in 0 until crewArray.length()) {
            val item =
                crewArray.optJSONObject(index)
                    ?: continue

            val job =
                item
                    .optString("job")
                    .trim()

            if (
                !job.equals(
                    "Director",
                    ignoreCase = true
                )
            ) {
                continue
            }

            val name =
                item
                    .optString("name")
                    .trim()

            if (name.isNotBlank()) {
                directors += name
            }
        }

        return directors
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
    }

    private fun extractTmdbTitleLogoUrl(
        images: JSONObject?
    ): String? {
        val logos =
            images
                ?.optJSONArray("logos")
                ?: return null

        var selectedPath: String? = null
        var selectedPriority = Int.MAX_VALUE
        var selectedVoteAverage = Double.NEGATIVE_INFINITY

        for (index in 0 until logos.length()) {
            val item =
                logos.optJSONObject(index)
                    ?: continue

            val filePath =
                item
                    .optString("file_path")
                    .trim()

            if (filePath.isBlank()) {
                continue
            }

            val language =
                if (
                    item.has("iso_639_1") &&
                    !item.isNull("iso_639_1")
                ) {
                    item
                        .optString("iso_639_1")
                        .trim()
                        .lowercase()
                } else {
                    ""
                }

            val priority =
                when (language) {
                    "pt" -> 0
                    "" -> 1
                    "en" -> 2
                    else -> 3
                }

            val voteAverage =
                item.optDouble(
                    "vote_average",
                    0.0
                )

            val shouldSelect =
                priority < selectedPriority ||
                        (
                                priority == selectedPriority &&
                                        voteAverage > selectedVoteAverage
                                )

            if (shouldSelect) {
                selectedPath = filePath
                selectedPriority = priority
                selectedVoteAverage = voteAverage
            }
        }

        return buildTmdbImageUrl(
            size = "w500",
            path = selectedPath
        )
    }

    private fun buildTmdbImageUrl(
        size: String,
        path: String?
    ): String? {
        val normalizedPath =
            path
                ?.trim()
                .orEmpty()

        if (
            normalizedPath.isBlank() ||
            normalizedPath.equals(
                "null",
                ignoreCase = true
            )
        ) {
            return null
        }

        return "$TMDB_IMAGE_BASE_URL/$size/" +
                normalizedPath.removePrefix("/")
    }

    private fun extractYear(
        date: String?
    ): Int? {
        val normalized =
            date
                ?.trim()
                .orEmpty()

        if (normalized.length < 4) {
            return null
        }

        return normalized
            .take(4)
            .toIntOrNull()
    }

    override val supportsProgressiveXtream: Boolean
        get() = xtreamParser is br.com.flaviotorres.iptvpremium.data.parser.DefaultXtreamParser

    /** The active catalog is never touched; sink writes only into an IMPORTING generation. */
    override suspend fun importXtream(
        source: PlaylistSource.XtreamCodes,
        sink: br.com.flaviotorres.iptvpremium.data.sync.XtreamCatalogSink
    ) {
        check(xtreamParser is br.com.flaviotorres.iptvpremium.data.parser.DefaultXtreamParser)
        val base = normalizeServerUrl(source.serverUrl)
        val user = source.username.trim()
        require(user.isNotBlank()) { "O usuário do Xtream Codes está vazio." }
        require(source.password.isNotBlank()) { "A senha do Xtream Codes está vazia." }
        val encodedUser = encodeQueryValue(user)
        val encodedPassword = encodeQueryValue(source.password)
        fun url(action: String) = buildPlayerApiUrl(base, encodedUser, encodedPassword, action)
        authenticateXtream(buildAuthenticationUrl(base, encodedUser, encodedPassword))
        suspend fun endpoint(action: String, address: String = url(action),
                             receive: suspend (PlaylistContent) -> Unit): StreamCatalogStats =
            streamCatalogEndpoint(action, address) { batch ->
                val mapped = when (action) {
                    "get_live_streams" -> batch.copy(channels = batch.channels.map {
                        if (it.streamUrl.isNotBlank()) it else it.copy(streamUrl = buildLiveStreamUrl(base, user, source.password, it.id))
                    })
                    "get_vod_streams" -> batch.copy(movies = batch.movies.map {
                        if (it.streamUrl.isNotBlank()) it else it.copy(streamUrl = buildMovieStreamUrl(base, user, source.password, it.id))
                    })
                    else -> batch
                }
                receive(mapped)
            }
        suspend fun categories(action: String): List<br.com.flaviotorres.iptvpremium.domain.model.Category> {
            val result = ArrayList<br.com.flaviotorres.iptvpremium.domain.model.Category>()
            endpoint(action) { batch -> sink.append(action, batch); result.addAll(batch.categories) }
            return result
        }
        var total = 0L
        if (sink.includeLive) {
            total += categories("get_live_categories").size
            total += endpoint("get_live_streams") { sink.append("get_live_streams", it) }.accepted
        }
        if (!sink.includeOnDemand) return
        val vodCategories = categories("get_vod_categories")
        total += vodCategories.size
        val key = "${base.length}:$base:${encodedUser.length}:$encodedUser"
        val known = vodCategories.map { it.id.trim() }.filter { it.isNotEmpty() }.toSet()
        val ids = HashSet<String>()
        val uncategorized = HashSet<String>()
        val unknown = HashSet<String>()
        val extras = LinkedHashSet<String>()
        val full = streamFullWithRetry("get_vod_streams", before = {
            sink.reset("get_vod_streams"); ids.clear(); uncategorized.clear(); unknown.clear(); extras.clear()
        }) {
            endpoint("get_vod_streams") { batch ->
                for (movie in batch.movies) {
                    ids.add(movie.id)
                    val category = movie.categoryId?.trim().orEmpty()
                    if (category.isEmpty()) uncategorized.add(movie.id)
                    else if (category !in known) { unknown.add(movie.id); extras.add(category) }
                }
                sink.append("get_vod_streams", batch)
            }
        }
        if (full != null) {
            synchronized(vodAuditLock) {
                lastVodFullSnapshot = VodFullSnapshot(key, ids.toSet(), uncategorized.toSet(), unknown.toSet(), extras.toSet(), SystemClock.elapsedRealtime())
            }
            total += full.accepted
        } else {
            sink.reset("get_vod_streams")
            val previous = synchronized(vodAuditLock) { lastVodFullSnapshot }
            val now = SystemClock.elapsedRealtime()
            val recent = previous?.takeIf { it.sourceKey == key && now >= it.savedAtMs && now - it.savedAtMs <= VOD_AUDIT_WINDOW_MS }
            val recovered = streamCategoriesOrdered("get_vod_streams", vodCategories.map { it.id } + recent?.extraCategoryIds.orEmpty(), url("get_vod_streams"), sink) { action, address, consumer -> endpoint(action, address, consumer) }
            if (recent != null) Log.i(XTREAM_SYNC_TAG, "VOD_AUDIT comparacao completoAnterior=${recent.allIds.size} segmentadoAtual=${recovered.size} idsAusentes=${recent.allIds.count { it !in recovered }} idsNovos=${recovered.count { it !in recent.allIds }}")
            total += recovered.size
        }
        val seriesCategories = categories("get_series_categories")
        total += seriesCategories.size
        suspend fun segmentedSeries(): Set<String> {
            sink.reset("get_series")
            return streamCategoriesOrdered("get_series", seriesCategories.map { it.id }, url("get_series"), sink) { action, address, consumer -> endpoint(action, address, consumer) }
        }
        val prefer = synchronized(seriesFallbackUntilMs) {
            val until = seriesFallbackUntilMs[key]
            if (until != null && until > SystemClock.elapsedRealtime()) true
            else { seriesFallbackUntilMs.remove(key); false }
        }
        var recoveredSeries: Set<String>? = null
        var categoryFailure: Exception? = null
        if (prefer) {
            try { recoveredSeries = segmentedSeries() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: br.com.flaviotorres.iptvpremium.data.sync.XtreamCatalogWriteException) { throw error }
            catch (error: Exception) {
                categoryFailure = error
                synchronized(seriesFallbackUntilMs) { seriesFallbackUntilMs.remove(key) }
            }
        }
        if (recoveredSeries != null) total += recoveredSeries.size
        else {
            val seriesFull = streamFullWithRetry("get_series", before = { sink.reset("get_series") }) {
                endpoint("get_series") { sink.append("get_series", it) }
            }
            if (seriesFull != null) {
                synchronized(seriesFallbackUntilMs) { seriesFallbackUntilMs.remove(key) }
                total += seriesFull.accepted
            } else {
                categoryFailure?.let { throw it }
                val recovered = segmentedSeries()
                synchronized(seriesFallbackUntilMs) { seriesFallbackUntilMs[key] = SystemClock.elapsedRealtime() + SERIES_FULL_RECHECK_MS }
                total += recovered.size
            }
        }
        check(total > 0 || !sink.includeLive) { "A API Xtream não retornou conteúdo para a lista." }
    }

    private val legacyCatalogEndpoints = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private data class StreamCatalogStats(val accepted: Long)

    private suspend fun streamCatalogEndpoint(endpoint: String, url: String,
        receive: suspend (PlaylistContent) -> Unit): StreamCatalogStats {
        val parser = xtreamParser as br.com.flaviotorres.iptvpremium.data.parser.DefaultXtreamParser
        val direct = (endpoint == "get_vod_streams" || endpoint == "get_series") && endpoint !in legacyCatalogEndpoints
        val start = SystemClock.elapsedRealtime()
        var accepted = 0L
        var outcome = "FALHA"
        var parsingMs = 0L
        var writeMs = 0L
        var batches = 0L
        var slowestWriteMs = 0L
        var sampledPeakKb = 0L
        fun sampleHeap() {
            val runtime = Runtime.getRuntime()
            sampledPeakKb = maxOf(sampledPeakKb, (runtime.totalMemory()-runtime.freeMemory())/1024)
        }
        try {
            suspend fun receiveMeasured(batch: PlaylistContent) {
                    sampleHeap()
                    accepted += batch.categories.size + batch.channels.size + batch.movies.size + batch.series.size
                    val writeStart = SystemClock.elapsedRealtime()
                    receive(batch)
                    val elapsedWrite = SystemClock.elapsedRealtime() - writeStart
                    writeMs += elapsedWrite
                    slowestWriteMs = maxOf(slowestWriteMs, elapsedWrite)
                    batches++
                    sampleHeap()
            }
            val stats = httpClient.withReader(url) { reader ->
                if (direct) {
                    val measured = br.com.flaviotorres.iptvpremium.data.parser.XtreamDirectCatalogReader.read(reader, endpoint, parser) { batch -> receiveMeasured(batch) }
                    parsingMs = measured.decodeMs
                    Log.i("XtreamDirectPerf", "endpoint=$endpoint records=${measured.records} characters=${measured.characters} decodeMs=${measured.decodeMs} readerMs=${measured.readerMs}")
                    br.com.flaviotorres.iptvpremium.data.parser.XtreamArrayStream.Stats(measured.records, measured.characters, measured.recoveredClosingBracket)
                } else {
                    br.com.flaviotorres.iptvpremium.data.parser.XtreamArrayStream.read(reader, endpoint) { chunk, _ ->
                        val parseStart = SystemClock.elapsedRealtime()
                        val batch = withContext(Dispatchers.Default) { parser.parseCatalogBatch(endpoint, chunk) }
                        parsingMs += SystemClock.elapsedRealtime() - parseStart
                        receiveMeasured(batch)
                    }
                }
            }
            if (stats.records > 0 && accepted == 0L && endpoint != "get_live_streams") {
                throw IllegalStateException("A API Xtream retornou itens sem identificadores válidos em $endpoint.")
            }
            if (stats.recoveredClosingBracket) Log.w(XTREAM_SYNC_TAG, "$endpoint ARRAY_RECUPERADO motivo=COLCHETE_FINAL_AUSENTE")
            outcome = "OK"
            return StreamCatalogStats(accepted)
        } catch (cancelled: CancellationException) { outcome = "CANCELADO"; throw cancelled }
        catch (compatibility: br.com.flaviotorres.iptvpremium.data.parser.XtreamJsonCompatibilityException) {
            legacyCatalogEndpoints.add(endpoint)
            Log.w("XtreamDirectPerf", "endpoint=$endpoint compatibility=LEGACY_NEXT_ATTEMPT")
            throw compatibility
        }
        finally {
            val runtime = Runtime.getRuntime()
            Log.i("XtreamEndpointPerf", "endpoint=$endpoint modo=STREAM leitor=${if (direct) "JSON_READER" else "LEGADO"} aceitos=$accepted parsingMs=$parsingMs gravacaoMs=$writeMs lotes=$batches gravacaoPicoMs=$slowestWriteMs totalMs=${SystemClock.elapsedRealtime()-start} heapKb=${(runtime.totalMemory()-runtime.freeMemory())/1024} heapPicoObservadoKb=$sampledPeakKb status=$outcome")
        }
    }

    private suspend fun streamFullWithRetry(endpoint: String, before: suspend () -> Unit,
        operation: suspend () -> StreamCatalogStats): StreamCatalogStats? {
        repeat(2) { attempt ->
            before()
            try { return operation() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (!isInvalidXtreamArray(error, endpoint)) throw error
                Log.w(XTREAM_SYNC_TAG, "$endpoint JSON_INVALIDO tentativa=${attempt+1}/2 modo=STREAM")
            }
        }
        return null
    }

    private suspend fun streamCategoriesOrdered(endpoint: String, rawIds: Collection<String>, baseUrl: String,
        sink: br.com.flaviotorres.iptvpremium.data.sync.XtreamCatalogSink,
        read: suspend (String, String, suspend (PlaylistContent) -> Unit) -> StreamCatalogStats): Set<String> {
        val owned = java.util.concurrent.ConcurrentHashMap<String, br.com.flaviotorres.iptvpremium.data.sync.XtreamCatalogStage>()
        var primary: Throwable? = null
        try {
        return coroutineScope {
        val ids = rawIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        check(ids.isNotEmpty()) { "Falha ao recuperar catálogo Xtream: não há categorias válidas." }
        data class Result(val stage: br.com.flaviotorres.iptvpremium.data.sync.XtreamCatalogStage, val ids: Set<String>)
        suspend fun fetch(index: Int): Result {
            val stage = sink.stage(endpoint).also { owned[it.generationId] = it }
            val seen = HashSet<String>()
            repeat(3) { attempt ->
                stage.sink.reset(endpoint); seen.clear()
                try {
                    read(endpoint, baseUrl + "&category_id=${encodeQueryValue(ids[index])}") { batch ->
                        if (endpoint == "get_vod_streams") seen.addAll(batch.movies.map { it.id })
                        else seen.addAll(batch.series.map { it.id })
                        stage.sink.append(endpoint, batch)
                    }
                    return Result(stage, seen)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: br.com.flaviotorres.iptvpremium.data.sync.XtreamCatalogWriteException) { throw error }
                catch (error: Exception) {
                    if (error is br.com.flaviotorres.iptvpremium.data.remote.PlaylistAccessDeniedException || attempt == 2) throw error
                    kotlinx.coroutines.delay(300L * (attempt + 1))
                }
            }
            error("Unreachable")
        }
        // At most three response stages exist ahead of the ordered consumer.
        val window = java.util.ArrayDeque<kotlinx.coroutines.Deferred<Result>>()
        var next = 0
        repeat(minOf(3, ids.size)) { val index = next++; window.add(async { fetch(index) }) }
        val merged = HashSet<String>()
        repeat(ids.size) {
            val result = window.removeFirst().await()
            sink.merge(result.stage)
            merged.addAll(result.ids)
            sink.discard(result.stage)
            owned.remove(result.stage.generationId)
            if (next < ids.size) { val index = next++; window.add(async { fetch(index) }) }
        }
        check(merged.isNotEmpty()) { "Nenhum item foi retornado pelas consultas segmentadas. A biblioteca anterior será preservada." }
        merged
        }
        } catch (error: Throwable) { primary = error; throw error }
        finally {
            withContext(kotlinx.coroutines.NonCancellable) {
                for (stage in owned.values) {
                    try { sink.discard(stage) }
                    catch (cleanup: Throwable) {
                        if (primary != null) { if (primary !== cleanup) primary.addSuppressed(cleanup) } else throw cleanup
                    }
                }
            }
        }
    }

    private suspend fun loadXtreamContent(
        source: PlaylistSource.XtreamCodes
    ): PlaylistContent {
        val baseUrl =
            normalizeServerUrl(
                source.serverUrl
            )

        val username =
            source.username.trim()

        val password =
            source.password

        require(username.isNotBlank()) {
            "O usuário do Xtream Codes está vazio."
        }

        require(password.isNotBlank()) {
            "A senha do Xtream Codes está vazia."
        }

        val encodedUsername =
            encodeQueryValue(username)

        val encodedPassword =
            encodeQueryValue(password)

        /*
         * O PlaylistHttpClient já executa a rede em Dispatchers.IO.
         *
         * O trabalho de CPU pesado — JSONObject/JSONArray, loops sobre
         * dezenas de milhares de itens e criação dos modelos — é movido
         * para Dispatchers.Default para não bloquear a Main Thread.
         *
         * Também baixamos e processamos cada endpoint antes de buscar o
         * próximo. Assim evitamos manter simultaneamente em memória todas
         * as grandes Strings JSON do catálogo Xtream.
         */

        authenticateXtream(buildAuthenticationUrl(baseUrl, encodedUsername, encodedPassword))

        val liveCategories =
            fetchAndParse(
                url =
                    buildPlayerApiUrl(
                        baseUrl = baseUrl,
                        username = encodedUsername,
                        password = encodedPassword,
                        action = "get_live_categories"
                    )
            ) { content ->
                xtreamParser.parseLiveCategories(
                    content
                )
            }

        val liveStreams =
            fetchAndParse(
                url =
                    buildPlayerApiUrl(
                        baseUrl = baseUrl,
                        username = encodedUsername,
                        password = encodedPassword,
                        action = "get_live_streams"
                    )
            ) { content ->
                xtreamParser
                    .parseLiveStreams(
                        content
                    )
                    .map { channel ->
                        if (channel.streamUrl.isNotBlank()) {
                            channel
                        } else {
                            channel.copy(
                                streamUrl =
                                    buildLiveStreamUrl(
                                        baseUrl = baseUrl,
                                        username = username,
                                        password = password,
                                        streamId = channel.id
                                    )
                            )
                        }
                    }
            }

        val movieCategories =
            fetchAndParse(
                url =
                    buildPlayerApiUrl(
                        baseUrl = baseUrl,
                        username = encodedUsername,
                        password = encodedPassword,
                        action = "get_vod_categories"
                    )
            ) { content ->
                xtreamParser.parseMovieCategories(
                    content
                )
            }

        // Caso o endpoint VOD completo contenha JSON inválido, tenta
        // consultas segmentadas por categoria. Não aceita importação parcial.
        val movies = loadMoviesWithCategoryFallback(
            baseUrl = baseUrl,
            username = encodedUsername,
            password = encodedPassword,
            originalUsername = username,
            originalPassword = password,
            categories = movieCategories
        )

        val seriesCategories =
            fetchAndParse(
                url =
                    buildPlayerApiUrl(
                        baseUrl = baseUrl,
                        username = encodedUsername,
                        password = encodedPassword,
                        action = "get_series_categories"
                    )
            ) { content ->
                xtreamParser.parseSeriesCategories(
                    content
                )
            }

        val series = loadSeriesWithCategoryFallback(
            baseUrl = baseUrl,
            username = encodedUsername,
            password = encodedPassword,
            categories = seriesCategories
        )

        return PlaylistContent(
            categories =
                liveCategories +
                        movieCategories +
                        seriesCategories,
            channels = liveStreams,
            movies = movies,
            series = series
        )
    }

    /*
     * Faz somente duas coisas:
     *
     * 1. baixa a resposta usando o PlaylistHttpClient, que já trabalha em IO;
     * 2. entrega o parsing CPU-bound para Dispatchers.Default.
     *
     * A String JSON fica confinada a esta função e pode ser liberada assim
     * que o parser terminar, reduzindo o pico de memória do fluxo Xtream.
     */
    private suspend fun <T> fetchAndParse(
        url: String,
        parser: (String) -> T
    ): T {
        // Usa apenas nomes de endpoint conhecidos nos logs.
        // Nunca imprime URL, usuario, senha ou resposta JSON.
        val action = url
            .substringAfter("&action=", "")
            .substringBefore("&")

        val endpoint = when (action) {
            "" -> "authenticate"
            "get_live_categories",
            "get_live_streams",
            "get_vod_categories",
            "get_vod_streams",
            "get_series_categories",
            "get_series",
            "get_vod_info" -> action
            else -> "outro"
        }

        val scope = if (url.contains("&category_id=")) {
            "categoria"
        } else {
            "completo"
        }

        val startedAt = SystemClock.elapsedRealtime()

        var httpMs = -1L
        var parsingMs = -1L
        var phase = "http"
        var status = "OK"
        var failureType = "nenhum"

        try {
            val httpStartedAt = SystemClock.elapsedRealtime()

            val content = httpClient.get(url)

            httpMs = SystemClock.elapsedRealtime() - httpStartedAt

            phase = "parsing"

            val parsingStartedAt = SystemClock.elapsedRealtime()

            return try {
                withContext(Dispatchers.Default) {
                    parser(content)
                }
            } finally {
                parsingMs =
                    SystemClock.elapsedRealtime() - parsingStartedAt
            }

        } catch (cancelled: CancellationException) {
            status = "CANCELADO"
            failureType = "CancellationException"
            throw cancelled

        } catch (error: Exception) {
            status = "FALHA"
            failureType = error.javaClass.simpleName
            throw error

        } finally {
            Log.i(
                "XtreamEndpointPerf",
                "endpoint=$endpoint " +
                    "escopo=$scope " +
                    "httpMs=$httpMs " +
                    "parsingMs=$parsingMs " +
                    "totalMs=" +
                    "${SystemClock.elapsedRealtime() - startedAt} " +
                    "fase=$phase " +
                    "status=$status " +
                    "erro=$failureType"
            )
        }
    }

    /**
     * Primeiro tenta o get_vod_streams completo. Se o parser denunciar JSON
     * inválido, usa get_vod_streams&category_id=... para reduzir o tamanho
     * e isolar a categoria problemática, sem aceitar dados parciais.
     *
     * Nenhum conteúdo, URL ou credencial da playlist aparece em logs.
     */
    /**
     * Tenta o catálogo completo até duas vezes quando o JSON é inválido.
     * Outros erros continuam sendo propagados, sem esconder falhas.
     */
    private suspend fun <T> fetchCatalogWithSingleRetry(
        url: String,
        endpoint: String,
        parser: (String) -> T
    ): T? {
        for (attempt in 1..2) {
            val startedAt = SystemClock.elapsedRealtime()

            try {
                val result = fetchAndParse(
                    url = url,
                    parser = parser
                )

                Log.i(
                    XTREAM_SYNC_TAG,
                    "$endpoint completo: tentativa=$attempt " +
                        "duracaoMs=${SystemClock.elapsedRealtime() - startedAt}"
                )

                return result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!isInvalidXtreamArray(error, endpoint)) {
                    throw error
                }

                Log.w(
                    XTREAM_SYNC_TAG,
                    "$endpoint: JSON inválido na tentativa=$attempt/2 " +
                        "duracaoMs=${SystemClock.elapsedRealtime() - startedAt}"
                )
            }
        }

        return null
    }

    /**
     * Prefere o catálogo completo. Somente após duas respostas JSON
     * inválidas utiliza a recuperação por categoria.
     */
    private suspend fun loadMoviesWithCategoryFallback(
        baseUrl: String,
        username: String,
        password: String,
        originalUsername: String,
        originalPassword: String,
        categories: List<br.com.flaviotorres.iptvpremium.domain.model.Category>
    ): List<br.com.flaviotorres.iptvpremium.domain.model.Movie> {
        val url = buildPlayerApiUrl(
            baseUrl = baseUrl,
            username = username,
            password = password,
            action = "get_vod_streams"
        )

        val complete = fetchCatalogWithSingleRetry(
            url = url,
            endpoint = "get_vod_streams"
        ) { content ->
            xtreamParser.parseMovies(content)
        }

        // Auditoria somente em memoria, sem gravacao adicional.
        val knownCategoryIds = categories
            .map { it.id.trim() }
            .filter { it.isNotEmpty() }
            .toHashSet()

        val auditKey = buildString {
            append(baseUrl.length)
            append(':')
            append(baseUrl)
            append(':')
            append(username.length)
            append(':')
            append(username)
        }

        val result = if (complete != null) {

            val allIds = complete
                .mapTo(HashSet<String>(complete.size)) {
                    it.id
                }

            val withoutCategoryIds = complete
                .filter { it.categoryId.isNullOrBlank() }
                .mapTo(HashSet<String>()) {
                    it.id
                }

            val unknownCategoryIds = complete
                .filter { movie ->
                    val categoryId =
                        movie.categoryId?.trim().orEmpty()

                    categoryId.isNotEmpty() &&
                        categoryId !in knownCategoryIds
                }
                .mapTo(HashSet<String>()) {
                    it.id
                }

            // Identifica categorias usadas por filmes, mas que nao
            // aparecem em get_vod_categories.
            val extraCategoryIds = complete
                .asSequence()
                .mapNotNull { movie ->
                    movie.categoryId
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                }
                .filter { it !in knownCategoryIds }
                .toCollection(LinkedHashSet<String>())

            synchronized(vodAuditLock) {
                lastVodFullSnapshot = VodFullSnapshot(
                    sourceKey = auditKey,
                    allIds = allIds,
                    withoutCategoryIds = withoutCategoryIds,
                    unknownCategoryIds = unknownCategoryIds,
                    extraCategoryIds = extraCategoryIds,
                    savedAtMs = SystemClock.elapsedRealtime()
                )
            }

            Log.i(
                XTREAM_SYNC_TAG,
                "VOD_AUDIT completo " +
                    "recebidos=${complete.size} " +
                    "idsDistintos=${allIds.size} " +
                    "semCategoria=${withoutCategoryIds.size} " +
                    "categoriaForaLista=${unknownCategoryIds.size} " +
                    "categoriasExtras=${extraCategoryIds.size} " +
                    "categoriasConhecidas=${knownCategoryIds.size}"
            )

            complete

        } else {
            Log.w(
                XTREAM_SYNC_TAG,
                "VOD completo falhou duas vezes. " +
                    "Recuperando ${categories.size} categorias " +
                    "com concorrência máxima de 3."
            )

            // Usar somente informacoes recentes da mesma fonte.
            val snapshot = synchronized(vodAuditLock) {
                lastVodFullSnapshot
            }

            val elapsedNow = SystemClock.elapsedRealtime()

            val canCompare =
                snapshot != null &&
                    snapshot.sourceKey == auditKey &&
                    elapsedNow >= snapshot.savedAtMs &&
                    elapsedNow - snapshot.savedAtMs <=
                        VOD_AUDIT_WINDOW_MS

            val extraCategoryIds = if (canCompare) {
                checkNotNull(snapshot).extraCategoryIds
            } else {
                emptySet()
            }

            Log.i(
                XTREAM_SYNC_TAG,
                "VOD_AUDIT recuperacao " +
                    "categoriasAnunciadas=${knownCategoryIds.size} " +
                    "categoriasExtrasConsultadas=${extraCategoryIds.size}"
            )

            val segmented = loadMoviesIndividuallyByCategory(
                baseUrl = baseUrl,
                username = username,
                password = password,
                categories = categories,
                extraCategoryIds = extraCategoryIds
            )

            val segmentedIds = segmented
                .mapTo(HashSet<String>(segmented.size)) {
                    it.id
                }

            if (canCompare) {
                val previous = checkNotNull(snapshot)

                val missingIds = previous.allIds.count {
                    it !in segmentedIds
                }

                val newIds = segmentedIds.count {
                    it !in previous.allIds
                }

                val missingWithoutCategory =
                    previous.withoutCategoryIds.count {
                        it !in segmentedIds
                    }

                val missingUnknownCategory =
                    previous.unknownCategoryIds.count {
                        it !in segmentedIds
                    }

                Log.w(
                    XTREAM_SYNC_TAG,
                    "VOD_AUDIT comparacao " +
                        "completoAnterior=${previous.allIds.size} " +
                        "segmentadoAtual=${segmentedIds.size} " +
                        "idsAusentes=$missingIds " +
                        "idsNovos=$newIds " +
                        "ausentesSemCategoria=$missingWithoutCategory " +
                        "ausentesCategoriaForaLista=$missingUnknownCategory"
                )

                // Catalogos obtidos em momentos diferentes podem
                // ter IDs diferentes por inclusoes ou remocoes
                // realizadas pelo proprio provedor.
                //
                // Essa comparacao e diagnostica: nao deve rejeitar
                // uma recuperacao em que todas as categorias
                // responderam corretamente.
                //
                // Falhas reais de consultas/JSON continuam sendo
                // propagadas pela recuperacao segmentada.
                if (missingIds > 0 || newIds > 0) {
                    Log.w(
                        XTREAM_SYNC_TAG,
                        "VOD_AUDIT DIFERENCA_TEMPORAL: " +
                            "idsAusentes=$missingIds " +
                            "idsNovos=$newIds. " +
                            "Todas as categorias foram consultadas; " +
                            "diferenca anterior apenas informativa."
                    )
                } else {
                    Log.i(
                        XTREAM_SYNC_TAG,
                        "VOD_AUDIT SEM_DIFERENCAS: " +
                            "IDs iguais aos do catalogo anterior."
                    )
                }
            } else {
                Log.i(
                    XTREAM_SYNC_TAG,
                    "VOD_AUDIT segmentado " +
                        "idsDistintos=${segmentedIds.size} " +
                        "comparacao=semBaselineRecente"
                )
            }

            segmented
        }

        // Preserva a construção original das URLs de reprodução.
        return withContext(Dispatchers.Default) {
            result.map { movie ->
                if (movie.streamUrl.isNotBlank()) {
                    movie
                } else {
                    movie.copy(
                        streamUrl = buildMovieStreamUrl(
                            baseUrl = baseUrl,
                            username = originalUsername,
                            password = originalPassword,
                            streamId = movie.id
                        )
                    )
                }
            }
        }
    }

    /**
     * Consulta até 3 categorias ao mesmo tempo.
     * awaitAll preserva a ordem original dos Deferred.
     * Uma falha cancela as demais tarefas e impede importação parcial.
     */
    private suspend fun loadMoviesIndividuallyByCategory(
        baseUrl: String,
        username: String,
        password: String,
        categories: List<br.com.flaviotorres.iptvpremium.domain.model.Category>,
        extraCategoryIds: Set<String>
    ): List<br.com.flaviotorres.iptvpremium.domain.model.Movie> {
        // Preserva a ordem das categorias anunciadas e inclui
        // ao final as categorias extras aprendidas anteriormente.
        val ids = (
            categories.map { it.id.trim() } +
                extraCategoryIds.map { it.trim() }
        )
            .filter { it.isNotBlank() }
            .distinct()

        if (ids.isEmpty()) {
            throw IllegalStateException(
                "Falha ao importar Filmes: JSON completo inválido e " +
                    "não há categorias válidas para a consulta segmentada."
            )
        }

        val startedAt = SystemClock.elapsedRealtime()

        return coroutineScope {
            val limiter = Semaphore(3)

            val results = ids.mapIndexed { index, categoryId ->
                async {
                    limiter.withPermit {
                        val url = buildPlayerApiUrl(
                            baseUrl = baseUrl,
                            username = username,
                            password = password,
                            action = "get_vod_streams"
                        ) + "&category_id=${encodeQueryValue(categoryId)}"

                        val movies = try {
                            fetchXtreamCategoryWithRetry(
                                url = url,
                                endpoint = "get_vod_streams",
                                categoryPosition = index + 1,
                                categoryCount = ids.size
                            ) { content ->
                                xtreamParser.parseMovies(content)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Log.e(
                                XTREAM_SYNC_TAG,
                                "Falha VOD por categoria " +
                                    "${index + 1}/${ids.size}; " +
                                    "erro=${error.javaClass.simpleName}"
                            )

                            throw IllegalStateException(
                                "A importação de Filmes não pôde ser concluída: " +
                                    "falha na categoria ${index + 1} de ${ids.size}. " +
                                    "A biblioteca anterior será preservada.",
                                error
                            )
                        }

                        // O servidor pode retornar a categoria principal
                        // em category_id mesmo quando a consulta utiliza
                        // uma categoria secundaria.
                        // Uma divergencia isolada nao prova JSON incorreto.
                        Log.d(
                            XTREAM_SYNC_TAG,
                            "VOD categoria ${index + 1}/${ids.size}: " +
                                "itens=${movies.size}"
                        )

                        movies
                    }
                }
            }.awaitAll()

            // Mesma ordem do provedor, mesmo com downloads paralelos.
            val uniqueMovies = LinkedHashMap<
                String,
                br.com.flaviotorres.iptvpremium.domain.model.Movie
            >()

            for (movies in results) {
                for (movie in movies) {
                    uniqueMovies.putIfAbsent(movie.id, movie)
                }
            }

            if (uniqueMovies.isEmpty()) {
                throw IllegalStateException(
                    "Nenhum filme foi retornado pelas consultas segmentadas. " +
                        "A importação incompleta não será ativada."
                )
            }

            Log.i(
                XTREAM_SYNC_TAG,
                "VOD segmentado concluído: categorias=${ids.size}, " +
                    "filmesDistintos=${uniqueMovies.size}, " +
                    "duracaoMs=${SystemClock.elapsedRealtime() - startedAt}"
            )

            uniqueMovies.values.toList()
        }
    }

    /**
     * Mesmo mecanismo de recuperação segura para séries.
     */
    private suspend fun loadSeriesWithCategoryFallback(
        baseUrl: String,
        username: String,
        password: String,
        categories: List<br.com.flaviotorres.iptvpremium.domain.model.Category>
    ): List<br.com.flaviotorres.iptvpremium.domain.model.Series> {

        // Chave por servidor e usuario. Nao inclui senha.
        // Nunca e escrita no Logcat.
        val catalogKey = buildString {
            append(baseUrl.length)
            append(':')
            append(baseUrl)
            append(':')
            append(username.length)
            append(':')
            append(username)
        }

        val agora = SystemClock.elapsedRealtime()

        val preferirCategorias = synchronized(seriesFallbackUntilMs) {
            val ate = seriesFallbackUntilMs[catalogKey]

            if (ate != null && ate > agora) {
                true
            } else {
                seriesFallbackUntilMs.remove(catalogKey)
                false
            }
        }

        var falhaAnteriorCategorias: Exception? = null

        if (preferirCategorias) {
            Log.i(
                XTREAM_SYNC_TAG,
                "get_series: modo adaptativo ativo. " +
                    "Consultando categorias sem repetir JSON completo."
            )

            try {
                return loadSeriesIndividuallyByCategory(
                    baseUrl = baseUrl,
                    username = username,
                    password = password,
                    categories = categories
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Se a recuperacao por categoria falhar,
                // volta a tentar o endpoint completo.
                falhaAnteriorCategorias = error

                synchronized(seriesFallbackUntilMs) {
                    seriesFallbackUntilMs.remove(catalogKey)
                }

                Log.w(
                    XTREAM_SYNC_TAG,
                    "get_series: modo por categorias falhou; " +
                        "testando novamente o catalogo completo. " +
                        "erro=${error.javaClass.simpleName}"
                )
            }
        }

        val url = buildPlayerApiUrl(
            baseUrl = baseUrl,
            username = username,
            password = password,
            action = "get_series"
        )

        // Mantem as duas tentativas atuais para JSON completo.
        val completo = fetchCatalogWithSingleRetry(
            url = url,
            endpoint = "get_series"
        ) { content ->
            xtreamParser.parseSeries(content)
        }

        if (completo != null) {
            // Endpoint voltou a funcionar: desativa a preferencia.
            synchronized(seriesFallbackUntilMs) {
                seriesFallbackUntilMs.remove(catalogKey)
            }

            return completo
        }

        // Nao repetimos todas as categorias se elas ja falharam
        // nesta mesma sincronizacao.
        falhaAnteriorCategorias?.let { throw it }

        Log.w(
            XTREAM_SYNC_TAG,
            "get_series: JSON completo falhou duas vezes. " +
                "Recuperando ${categories.size} categorias " +
                "com concorrencia maxima de 3."
        )

        // So aceita a recuperacao se TODAS as consultas
        // exigidas pelo importador forem concluidas.
        val recuperadas = loadSeriesIndividuallyByCategory(
            baseUrl = baseUrl,
            username = username,
            password = password,
            categories = categories
        )

        // Registra preferencia somente depois do sucesso.
        synchronized(seriesFallbackUntilMs) {
            seriesFallbackUntilMs[catalogKey] =
                SystemClock.elapsedRealtime() +
                    SERIES_FULL_RECHECK_MS
        }

        Log.i(
            XTREAM_SYNC_TAG,
            "get_series: recuperacao segmentada concluida. " +
                "Modo adaptativo ativado por 10 minutos."
        )

        return recuperadas
    }
    private suspend fun loadSeriesIndividuallyByCategory(
        baseUrl: String,
        username: String,
        password: String,
        categories: List<br.com.flaviotorres.iptvpremium.domain.model.Category>
    ): List<br.com.flaviotorres.iptvpremium.domain.model.Series> {
        val ids = categories
            .map { it.id.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (ids.isEmpty()) {
            throw IllegalStateException(
                "Falha ao importar Séries: JSON completo inválido e " +
                    "não há categorias válidas para a consulta segmentada."
            )
        }

        val startedAt = SystemClock.elapsedRealtime()

        return coroutineScope {
            val limiter = Semaphore(3)

            val results = ids.mapIndexed { index, categoryId ->
                async {
                    limiter.withPermit {
                        val url = buildPlayerApiUrl(
                            baseUrl = baseUrl,
                            username = username,
                            password = password,
                            action = "get_series"
                        ) + "&category_id=${encodeQueryValue(categoryId)}"

                        val series = try {
                            fetchXtreamCategoryWithRetry(
                                url = url,
                                endpoint = "get_series",
                                categoryPosition = index + 1,
                                categoryCount = ids.size
                            ) { content ->
                                xtreamParser.parseSeries(content)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Log.e(
                                XTREAM_SYNC_TAG,
                                "Falha Séries por categoria " +
                                    "${index + 1}/${ids.size}; " +
                                    "erro=${error.javaClass.simpleName}"
                            )

                            throw IllegalStateException(
                                "A importação de Séries não pôde ser concluída: " +
                                    "falha na categoria ${index + 1} de ${ids.size}. " +
                                    "A biblioteca anterior será preservada.",
                                error
                            )
                        }

                        // O servidor pode retornar a categoria principal
                        // em category_id mesmo quando a consulta utiliza
                        // uma categoria secundaria.
                        // Uma divergencia isolada nao prova JSON incorreto.
                        Log.d(
                            XTREAM_SYNC_TAG,
                            "Séries categoria ${index + 1}/${ids.size}: " +
                                "itens=${series.size}"
                        )

                        series
                    }
                }
            }.awaitAll()

            val uniqueSeries = LinkedHashMap<
                String,
                br.com.flaviotorres.iptvpremium.domain.model.Series
            >()

            for (series in results) {
                for (item in series) {
                    uniqueSeries.putIfAbsent(item.id, item)
                }
            }

            if (uniqueSeries.isEmpty()) {
                throw IllegalStateException(
                    "Nenhuma série foi retornada pelas consultas segmentadas. " +
                        "A importação incompleta não será ativada."
                )
            }

            Log.i(
                XTREAM_SYNC_TAG,
                "Séries segmentadas concluídas: categorias=${ids.size}, " +
                    "seriesDistintas=${uniqueSeries.size}, " +
                    "duracaoMs=${SystemClock.elapsedRealtime() - startedAt}"
            )

            uniqueSeries.values.toList()
        }
    }

    /**
     * Recupera somente a categoria que falhou.
     *
     * - Ate 3 tentativas por categoria.
     * - Mantem as 3 requisicoes simultaneas ja configuradas.
     * - Nao reinicia categorias que tiveram sucesso.
     * - Propaga cancelamentos.
     * - Nunca aceita um erro como lista vazia.
     * - Nao registra URL ou credenciais.
     */
    private suspend fun <T> fetchXtreamCategoryWithRetry(
        url: String,
        endpoint: String,
        categoryPosition: Int,
        categoryCount: Int,
        parser: (String) -> T
    ): T {
        val maximumAttempts = 3

        for (attempt in 1..maximumAttempts) {
            coroutineContext.ensureActive()

            val startedAt = SystemClock.elapsedRealtime()

            try {
                val result = fetchAndParse(
                    url = url,
                    parser = parser
                )

                if (attempt > 1) {
                    Log.i(
                        XTREAM_SYNC_TAG,
                        "$endpoint categoria " +
                            "$categoryPosition/$categoryCount recuperada " +
                            "na tentativa=$attempt; " +
                            "duracaoMs=" +
                            "${SystemClock.elapsedRealtime() - startedAt}"
                    )
                }

                return result

            } catch (cancelled: CancellationException) {
                throw cancelled

            } catch (error: Exception) {
                val failureType =
                    if (isInvalidXtreamArray(error, endpoint)) {
                        "JSON_INVALIDO"
                    } else {
                        error.javaClass.simpleName
                    }

                Log.w(
                    XTREAM_SYNC_TAG,
                    "$endpoint categoria " +
                        "$categoryPosition/$categoryCount: " +
                        "tentativa=$attempt/$maximumAttempts; " +
                        "erro=$failureType; " +
                        "duracaoMs=" +
                        "${SystemClock.elapsedRealtime() - startedAt}"
                )

                if (attempt == maximumAttempts) {
                    throw error
                }

                // Pequeno intervalo para evitar repetir
                // imediatamente uma resposta defeituosa.
                // Mantem a cooperacao com o cancelamento.
                kotlinx.coroutines.delay(
                    300L * attempt
                )
            }
        }

        throw IllegalStateException(
            "Falha inesperada na recuperacao " +
                "de categoria Xtream."
        )
    }

    private fun isInvalidXtreamArray(error: Exception, endpoint: String): Boolean {
        return error is IllegalStateException &&
                error.message.orEmpty().startsWith("Resposta Xtream inválida em $endpoint:")
    }

    private fun normalizeServerUrl(
        value: String
    ): String {
        val trimmed =
            value
                .trim()
                .removeSuffix("/")

        require(trimmed.isNotBlank()) {
            "O endereço do servidor está vazio."
        }

        return when {
            trimmed.startsWith(
                "http://",
                ignoreCase = true
            ) -> trimmed

            trimmed.startsWith(
                "https://",
                ignoreCase = true
            ) -> trimmed

            else -> {
                "http://$trimmed"
            }
        }
    }

    private suspend fun authenticateXtream(url: String) {
        XtreamAuthenticationRecovery.confirm(
            load = { fetchAndParse(url) { it } },
            validate = ::validateXtreamAuthentication,
            onRetry = { Log.w(XTREAM_SYNC_TAG, "AUTH resposta_invalida tentativa_automatica=2") }
        )
    }

    private fun validateXtreamAuthentication(content: String) =
        XtreamAuthenticationRecovery.validateResponse(content)

    private fun buildAuthenticationUrl(
        baseUrl: String,
        username: String,
        password: String
    ): String {
        return "$baseUrl/player_api.php" +
                "?username=$username" +
                "&password=$password"
    }

    private fun buildPlayerApiUrl(
        baseUrl: String,
        username: String,
        password: String,
        action: String
    ): String {
        return "$baseUrl/player_api.php" +
                "?username=$username" +
                "&password=$password" +
                "&action=$action"
    }

    private fun buildVodInfoUrl(
        baseUrl: String,
        username: String,
        password: String,
        movieId: String
    ): String {
        return buildPlayerApiUrl(
            baseUrl = baseUrl,
            username = username,
            password = password,
            action = "get_vod_info"
        ) +
                "&vod_id=${
                    encodeQueryValue(
                        movieId
                    )
                }"
    }

    private fun buildLiveStreamUrl(
        baseUrl: String,
        username: String,
        password: String,
        streamId: String
    ): String {
        return "$baseUrl/live/" +
                "${encodePathSegment(username)}/" +
                "${encodePathSegment(password)}/" +
                "${encodePathSegment(streamId)}.ts"
    }

    private fun buildMovieStreamUrl(
        baseUrl: String,
        username: String,
        password: String,
        streamId: String
    ): String {
        return "$baseUrl/movie/" +
                "${encodePathSegment(username)}/" +
                "${encodePathSegment(password)}/" +
                "${encodePathSegment(streamId)}.mp4"
    }

    private fun encodeQueryValue(
        value: String
    ): String {
        return URLEncoder.encode(
            value,
            StandardCharsets.UTF_8.toString()
        )
    }

    private fun encodePathSegment(
        value: String
    ): String {
        return URLEncoder.encode(
            value,
            StandardCharsets.UTF_8.toString()
        ).replace(
            "+",
            "%20"
        )
    }
}
