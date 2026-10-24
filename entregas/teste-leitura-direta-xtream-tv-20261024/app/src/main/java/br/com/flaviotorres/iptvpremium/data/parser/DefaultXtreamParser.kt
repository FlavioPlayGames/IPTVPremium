package br.com.flaviotorres.iptvpremium.data.parser

import android.util.Log
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.domain.model.CategorySource
import br.com.flaviotorres.iptvpremium.domain.model.CategoryType
import br.com.flaviotorres.iptvpremium.domain.model.Channel
import br.com.flaviotorres.iptvpremium.domain.model.Movie
import br.com.flaviotorres.iptvpremium.domain.model.MovieDetails
import br.com.flaviotorres.iptvpremium.domain.model.MovieCastMemberDetails
import br.com.flaviotorres.iptvpremium.domain.model.RatingSource
import br.com.flaviotorres.iptvpremium.domain.model.Series
import org.json.JSONArray
import org.json.JSONObject

class DefaultXtreamParser : XtreamParser {

    internal fun parseCatalogBatch(endpoint: String, content: String): br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent {
        val empty = br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent()
        return when (endpoint) {
            "get_live_categories" -> empty.copy(categories = parseCategories(content, CategoryType.LIVE_TV, false))
            "get_vod_categories" -> empty.copy(categories = parseCategories(content, CategoryType.MOVIES, false))
            "get_series_categories" -> empty.copy(categories = parseCategories(content, CategoryType.SERIES, false))
            "get_live_streams" -> empty.copy(channels = parseLiveStreams(content))
            "get_vod_streams" -> empty.copy(movies = parseMoviesBatch(content, false))
            "get_series" -> empty.copy(series = parseSeriesBatch(content, false))
            else -> error("Unsupported catalog endpoint")
        }
    }

    internal fun movieFromCatalogItem(item: JSONObject): Movie? {
        val id = item.optStringOrNull("stream_id") ?: return null
        val name = item
            .optStringOrNull("name")
            ?: "Filme"
        return Movie(
            id = id,
            name = name,
            categoryId = item.optStringOrNull(
                "category_id"
            ),
            posterUrl = item.optStringOrNull(
                "stream_icon"
            ),
            backdropUrl = extractFirstBackdrop(
                item
            ),
            streamUrl = item.optStringOrNull(
                "direct_source"
            ).orEmpty(),
            year = extractYear(item),
            rating = item.optDoubleOrNull(
                "rating"
            ),
            durationSeconds = extractDurationSeconds(
                item
            )?.toLong(),
            addedAtEpochSeconds = item.optLongOrNull(
                "added"
            )
        )
    }

    internal fun seriesFromCatalogItem(item: JSONObject): Series? {
        val id = item.optStringOrNull("series_id") ?: return null
        val name = item
            .optStringOrNull("name")
            ?: "Série"

        val genreText = item.optStringOrNull(
            "genre"
        )

        val genres = genreText
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return Series(
            id = id,
            name = name,
            categoryId = item.optStringOrNull(
                "category_id"
            ),
            posterUrl = item.optStringOrNull(
                "cover"
            ),
            backdropUrl = extractFirstBackdrop(
                item
            ),
            year = extractYear(item),
            rating = item.optDoubleOrNull(
                "rating"
            ),
            plot = item.optStringOrNull(
                "plot"
            ),
            titleLogoUrl = extractTitleLogoUrl(item),
            imdbId = item.optStringOrNull(
                "imdb_id"
            ),
            tmdbId = listOf("tmdb_id", "tmdb").firstNotNullOfOrNull { key ->
                item.optStringOrNull(key)?.takeIf { raw ->
                    raw.all(Char::isDigit) && raw.toIntOrNull()?.let { it > 0 } == true
                }
    },
    ratingSource = detectRatingSource(
        item
    ),
    durationSeconds = extractDurationSeconds(
        item
    ),
    genres = genres,
    trailerUrl = item.optStringOrNull(
        "youtube_trailer"
    )
)
    }

    override fun parseLiveCategories(
        content: String
    ): List<Category> {
        return parseCategories(
            content = content,
            type = CategoryType.LIVE_TV
        )
    }

    override fun parseLiveStreams(
        content: String
    ): List<Channel> {
        val array = parseArray(content, endpoint = "get_live_streams")

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                    ?: continue

                val id = item
                    .optStringOrNull("stream_id")
                    ?: continue

                val name = item
                    .optStringOrNull("name")
                    ?: "Canal"

                add(
                    Channel(
                        id = id,
                        name = name,
                        categoryId = item.optStringOrNull(
                            "category_id"
                        ),
                        logoUrl = item.optStringOrNull(
                            "stream_icon"
                        ),
                        streamUrl = item.optStringOrNull(
                            "direct_source"
                        ).orEmpty(),
                        epgChannelId = item.optStringOrNull(
                            "epg_channel_id"
                        )
                    )
                )
            }
        }
    }

    override fun parseMovieCategories(
        content: String
    ): List<Category> {
        return parseCategories(
            content = content,
            type = CategoryType.MOVIES
        )
    }

    override fun parseMovies(
        content: String
    ): List<Movie> = parseMoviesBatch(content, true)

    private fun parseMoviesBatch(content: String, requireRecognized: Boolean): List<Movie> {
        val array = parseArray(content, endpoint = "get_vod_streams")

        var ignoredNonObjects = 0
        var ignoredWithoutId = 0

        val parsed = buildList<Movie> {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                if (item == null) {
                    ignoredNonObjects++
                    continue
                }

                val mapped = movieFromCatalogItem(item)
                if (mapped == null) { ignoredWithoutId++; continue }
                add(mapped)
            }
        }

        if (requireRecognized) logCatalogParseSummary(
            endpoint = "get_vod_streams",
            array = array,
            accepted = parsed.size,
            ignoredNonObjects = ignoredNonObjects,
            ignoredWithoutId = ignoredWithoutId
        )

        if (requireRecognized && array.length() > 0 && parsed.isEmpty()) {
            throw IllegalStateException(
                "A API Xtream retornou filmes, mas nenhum item foi reconhecido. " +
                        "Verifique o Logcat: XtreamCatalogParser."
            )
        }

        return parsed
    }

    override fun parseMovieDetails(
        content: String,
        movieId: String
    ): MovieDetails? {
        if (
            content.isBlank() ||
            movieId.isBlank()
        ) {
            return null
        }

        val root =
            runCatching {
                JSONObject(content)
            }.getOrNull()
                ?: return null

        val info =
            root.optJSONObject(
                "info"
            )

        val movieData =
            root.optJSONObject(
                "movie_data"
            )

        if (
            info == null &&
            movieData == null
        ) {
            return null
        }

        val metadata =
            info ?: movieData
            ?: return null

        val fallback =
            movieData ?: info

        val genres =
            extractStringList(
                item = metadata,
                key = "genre"
            ).ifEmpty {
                extractStringList(
                    item = metadata,
                    key = "genres"
                )
            }.ifEmpty {
                fallback
                    ?.let {
                        extractStringList(
                            item = it,
                            key = "genre"
                        )
                    }
                    .orEmpty()
            }

        val cast =
            extractStringList(
                item = metadata,
                key = "cast"
            ).ifEmpty {
                fallback
                    ?.let {
                        extractStringList(
                            item = it,
                            key = "cast"
                        )
                    }
                    .orEmpty()
            }

        val castMembers =
            extractCastMembers(
                item = metadata,
                key = "cast"
            ).ifEmpty {
                extractCastMembers(
                    item = metadata,
                    key = "actors"
                )
            }.ifEmpty {
                fallback
                    ?.let {
                        extractCastMembers(
                            item = it,
                            key = "cast"
                        )
                    }
                    .orEmpty()
            }.ifEmpty {
                fallback
                    ?.let {
                        extractCastMembers(
                            item = it,
                            key = "actors"
                        )
                    }
                    .orEmpty()
            }

        val year =
            extractYear(
                metadata
            )
                ?: metadata
                    .optStringOrNull(
                        "releasedate"
                    )
                    ?.take(4)
                    ?.toIntOrNull()
                ?: fallback
                    ?.let {
                        extractYear(
                            it
                        )
                    }
                ?: fallback
                    ?.optStringOrNull(
                        "releasedate"
                    )
                    ?.take(4)
                    ?.toIntOrNull()

        val rating =
            metadata.optDoubleOrNull(
                "rating"
            )
                ?: fallback
                    ?.optDoubleOrNull(
                        "rating"
                    )

        val ratingSource =
            detectRatingSource(
                metadata
            )
                ?: fallback
                    ?.let {
                        detectRatingSource(
                            it
                        )
                    }

        val durationSeconds =
            extractDurationSeconds(
                metadata
            )
                ?: fallback
                    ?.let {
                        extractDurationSeconds(
                            it
                        )
                    }

        val posterUrl =
            metadata.optStringOrNull(
                "movie_image"
            )
                ?: metadata.optStringOrNull(
                    "cover_big"
                )
                ?: metadata.optStringOrNull(
                    "cover"
                )
                ?: metadata.optStringOrNull(
                    "stream_icon"
                )
                ?: fallback
                    ?.optStringOrNull(
                        "movie_image"
                    )
                ?: fallback
                    ?.optStringOrNull(
                        "cover_big"
                    )
                ?: fallback
                    ?.optStringOrNull(
                        "cover"
                    )
                ?: fallback
                    ?.optStringOrNull(
                        "stream_icon"
                    )

        val backdropUrl =
            extractFirstBackdrop(
                metadata
            )
                ?: fallback
                    ?.let {
                        extractFirstBackdrop(
                            it
                        )
                    }

        val plot =
            metadata.optStringOrNull(
                "plot"
            )
                ?: metadata.optStringOrNull(
                    "description"
                )
                ?: fallback
                    ?.optStringOrNull(
                        "plot"
                    )
                ?: fallback
                    ?.optStringOrNull(
                        "description"
                    )

        val director =
            metadata.optStringOrNull(
                "director"
            )
                ?: fallback
                    ?.optStringOrNull(
                        "director"
                    )

        val trailerUrl =
            metadata.optStringOrNull(
                "youtube_trailer"
            )
                ?: metadata.optStringOrNull(
                    "trailer"
                )
                ?: metadata.optStringOrNull(
                    "trailer_url"
                )
                ?: fallback
                    ?.optStringOrNull(
                        "youtube_trailer"
                    )
                ?: fallback
                    ?.optStringOrNull(
                        "trailer"
                    )
                ?: fallback
                    ?.optStringOrNull(
                        "trailer_url"
                    )

        val imdbId =
            metadata.optStringOrNull(
                "imdb_id"
            )
                ?: fallback
                    ?.optStringOrNull(
                        "imdb_id"
                    )

        val tmdbId = listOfNotNull(metadata, fallback).firstNotNullOfOrNull { item ->
            listOf("tmdb_id", "tmdb").firstNotNullOfOrNull { key ->
                item.optStringOrNull(key)?.takeIf { it.all(Char::isDigit) && it.toIntOrNull()?.let { n -> n > 0 } == true }
            }
        }

        val titleLogoUrl =
            extractTitleLogoUrl(
                metadata
            )
                ?: fallback
                    ?.let {
                        extractTitleLogoUrl(
                            it
                        )
                    }

        return MovieDetails(
            movieId = movieId,
            providerTitle = metadata.optStringOrNull("name") ?: metadata.optStringOrNull("title") ?: fallback?.optStringOrNull("name"),
            plot = plot,
            genres = genres,
            cast = cast,
            castMembers = castMembers,
            director = director,
            trailerUrl = trailerUrl,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            titleLogoUrl = titleLogoUrl,
            year = year,
            rating = rating,
            ratingSource = ratingSource,
            durationSeconds = durationSeconds,
            imdbId = imdbId,
            tmdbId = tmdbId
        )
    }

    override fun parseSeriesCategories(
        content: String
    ): List<Category> {
        return parseCategories(
            content = content,
            type = CategoryType.SERIES
        )
    }

    override fun parseSeries(
        content: String
    ): List<Series> = parseSeriesBatch(content, true)

    private fun parseSeriesBatch(content: String, requireRecognized: Boolean): List<Series> {
        val array = parseArray(content, endpoint = "get_series")

        var ignoredNonObjects = 0
        var ignoredWithoutId = 0

        val parsed = buildList<Series> {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                if (item == null) {
                    ignoredNonObjects++
                    continue
                }

                val mapped = seriesFromCatalogItem(item)
                if (mapped == null) { ignoredWithoutId++; continue }
                add(mapped)
            }
        }

        if (requireRecognized) logCatalogParseSummary(
            endpoint = "get_series",
            array = array,
            accepted = parsed.size,
            ignoredNonObjects = ignoredNonObjects,
            ignoredWithoutId = ignoredWithoutId
        )

        if (requireRecognized && array.length() > 0 && parsed.isEmpty()) {
            throw IllegalStateException(
                "A API Xtream retornou séries, mas nenhum item foi reconhecido. " +
                        "Verifique o Logcat: XtreamCatalogParser."
            )
        }

        return parsed
    }

    private fun extractStringList(
        item: JSONObject,
        key: String
    ): List<String> {
        if (
            !item.has(key) ||
            item.isNull(key)
        ) {
            return emptyList()
        }

        return when (
            val value = item.opt(key)
        ) {
            is JSONArray -> {
                buildList {
                    for (
                    index in
                    0 until value.length()
                    ) {
                        val rawItem =
                            value.opt(index)

                        val name =
                            when (rawItem) {
                                is JSONObject -> {
                                    rawItem.optStringOrNull(
                                        "name"
                                    )
                                        ?: rawItem.optStringOrNull(
                                            "original_name"
                                        )
                                }

                                else -> {
                                    rawItem
                                        ?.toString()
                                        ?.trim()
                                        ?.takeIf {
                                            it.isNotBlank() &&
                                                    !it.equals(
                                                        "null",
                                                        ignoreCase = true
                                                    )
                                        }
                                }
                            }

                        name
                            ?.let {
                                add(it)
                            }
                    }
                }
                    .distinct()
            }

            is String -> {
                value
                    .split(
                        ",",
                        "|"
                    )
                    .map {
                        it.trim()
                    }
                    .filter {
                        it.isNotBlank() &&
                                !it.equals(
                                    "null",
                                    ignoreCase = true
                                )
                    }
                    .distinct()
            }

            else -> emptyList()
        }
    }

    private fun extractCastMembers(
        item: JSONObject,
        key: String
    ): List<MovieCastMemberDetails> {
        if (
            !item.has(key) ||
            item.isNull(key)
        ) {
            return emptyList()
        }

        val value =
            item.opt(key)

        if (value !is JSONArray) {
            return emptyList()
        }

        return buildList {
            for (index in 0 until value.length()) {
                val actor =
                    value.optJSONObject(index)
                        ?: continue

                val name =
                    actor.optStringOrNull(
                        "name"
                    )
                        ?: actor.optStringOrNull(
                            "original_name"
                        )
                        ?: continue

                val tmdbId =
                    actor.optStringOrNull(
                        "tmdb_id"
                    )
                        ?: actor.optStringOrNull(
                            "person_id"
                        )

                val id =
                    actor.optStringOrNull(
                        "id"
                    )
                        ?: tmdbId
                        ?: actor.optStringOrNull(
                            "cast_id"
                        )
                        ?: "$name-$index"

                val profileUrl =
                    actor.optStringOrNull(
                        "profile_url"
                    )
                        ?: actor.optStringOrNull(
                            "profile_path"
                        )
                        ?: actor.optStringOrNull(
                            "photo_url"
                        )
                        ?: actor.optStringOrNull(
                            "photo"
                        )
                        ?: actor.optStringOrNull(
                            "image"
                        )
                        ?: actor.optStringOrNull(
                            "avatar"
                        )

                val characterName =
                    actor.optStringOrNull(
                        "character"
                    )
                        ?: actor.optStringOrNull(
                            "role"
                        )

                add(
                    MovieCastMemberDetails(
                        id = id,
                        name = name,
                        profileUrl = profileUrl,
                        characterName = characterName,
                        tmdbId = tmdbId
                    )
                )
            }
        }
            .distinctBy {
                it.id
            }
    }

    private fun extractTitleLogoUrl(
        item: JSONObject
    ): String? {
        val keys =
            listOf(
                "title_logo",
                "title_logo_url",
                "clearlogo",
                "clear_logo",
                "movie_logo",
                "logo"
            )

        for (key in keys) {
            item.optStringOrNull(
                key
            )
                ?.let {
                    return it
                }
        }

        return null
    }

    private fun parseCategories(
        content: String,
        type: CategoryType,
        requireRecognized: Boolean = true
    ): List<Category> {
        val endpoint = when (type) {
            CategoryType.LIVE_TV -> "get_live_categories"
            CategoryType.MOVIES -> "get_vod_categories"
            CategoryType.SERIES -> "get_series_categories"
        }

        val array = parseArray(content, endpoint = endpoint)

        val categories = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                    ?: continue

                val id = item
                    .optStringOrNull("category_id")
                    ?: continue

                val name = item
                    .optStringOrNull("category_name")
                    ?: continue

                add(
                    Category(
                        id = id,
                        name = name,
                        type = type,
                        source = CategorySource.PROVIDER
                    )
                )
            }
        }

        if (requireRecognized) Log.i(
            TAG_CATALOG,
            "$endpoint: categoriasJSON=${array.length()} " +
                    "categoriasReconhecidas=${categories.size} " +
                    "ignoradas=${array.length() - categories.size}"
        )

        if (requireRecognized && array.length() > 0 && categories.isEmpty()) {
            throw IllegalStateException(
                "A API Xtream retornou categorias para $endpoint, " +
                        "mas nenhuma tinha identificador e nome válidos."
            )
        }

        return categories
    }

    /**
     * Nunca converte um JSON malformado em uma lista vazia silenciosamente.
     * Captura apenas Exception; erros graves de VM continuam propagando.
     *
     * Nenhum conteúdo da resposta, URL ou credencial é registrado em logs.
     */
    private fun parseArray(
        content: String,
        endpoint: String = "categoria/outra_lista"
    ): JSONArray {
        if (content.isBlank()) {
            Log.w(
                TAG_CATALOG,
                "$endpoint: resposta vazia; itens=0"
            )
            return JSONArray()
        }

        // Verifica primeiro a recuperação estrita para evitar construir e
        // descartar uma árvore JSON inteira antes de interpretar a correção.
        //
        // Só recupera o colchete externo quando todos os objetos e strings
        // estão completos. A resposta corrigida inteira passa pelo parser.
        recoverArrayMissingClosingBracket(content, endpoint)?.let { recovered ->
            Log.w(
                TAG_CATALOG,
                "$endpoint: ARRAY_RECUPERADO motivo=COLCHETE_FINAL_AUSENTE " +
                        "caracteresOriginais=${content.length} " +
                        "itensJSON=${recovered.length()}"
            )
            return recovered
        }

        return try {
            JSONArray(content)
        } catch (error: Exception) {
            // A mensagem crua pode conter parte da resposta.
            // Não imprime error.message nem a exceção completa.
            val structural = inspectArrayStructure(content)
            val failurePosition = safeJsonFailurePosition(error.message)

            Log.e(
                TAG_CATALOG,
                "$endpoint: JSON_ARRAY_INVALIDO caracteres=${content.length} " +
                        "tipoErro=${error.javaClass.simpleName} " +
                        "posicaoErro=${failurePosition ?: "desconhecida"} " +
                        structural
            )

            throw IllegalStateException(
                "Resposta Xtream inválida em $endpoint: " +
                        "esperado um array JSON válido (${content.length} caracteres)."
            )
        }
    }

    /**
     * Recupera arrays no formato `[ {...}, {...} ` quando falta apenas
     * o fechamento do array externo.
     *
     * Não reconstrói dados ausentes nem fecha objetos ou strings incompletos.
     * A resposta corrigida inteira precisa ser aceita pelo JSONArray.
     *
     * Limites:
     * - categorias: 256 mil caracteres;
     * - canais, filmes e séries: 8 milhões de caracteres.
     *
     * Acima do limite, permanece a recuperação por categoria do Repository.
     */
    private fun recoverArrayMissingClosingBracket(
        content: String,
        endpoint: String
    ): JSONArray? {
        val maxCharacters = when (endpoint) {
            "get_live_categories",
            "get_vod_categories",
            "get_series_categories" -> 256_000

            "get_live_streams",
            "get_vod_streams",
            "get_series" -> 8_000_000

            else -> return null
        }

        if (content.length > maxCharacters) return null

        val value = content.trim().removePrefix("\uFEFF")

        if (
            value.length < 3 ||
            value.first() != '[' ||
            value.last() != '}'
        ) {
            return null
        }

        var openArrays = 0
        var openObjects = 0
        var inString = false
        var escaped = false

        for (character in value) {
            if (inString) {
                if (escaped) {
                    escaped = false
                    continue
                }

                when (character) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                    else -> if (character.code < 0x20) return null
                }
            } else {
                when (character) {
                    '"' -> inString = true
                    '[' -> openArrays++

                    ']' -> {
                        openArrays--
                        if (openArrays < 1) return null
                    }

                    '{' -> openObjects++

                    '}' -> {
                        openObjects--
                        if (openObjects < 0) return null
                    }
                }
            }
        }

        if (
            inString ||
            escaped ||
            openArrays != 1 ||
            openObjects != 0
        ) {
            return null
        }

        return try {
            JSONArray(value + "]")
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extrai apenas a posição numérica do erro, sem expor a resposta.
     */
    private fun safeJsonFailurePosition(message: String?): Int? {
        if (message.isNullOrBlank()) return null

        val patterns = listOf(
            Regex("at\\s+character\\s+(\\d+)", RegexOption.IGNORE_CASE),
            Regex("at\\s+position\\s+(\\d+)", RegexOption.IGNORE_CASE),
            Regex("at\\s+offset\\s+(\\d+)", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val index = pattern.find(message)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()

            if (index != null) return index
        }

        return null
    }

    /**
     * Diagnóstico estrutural sem registrar dados pessoais, títulos ou URLs.
     * Apenas contadores, delimitadores e classificação de caracteres.
     */
    private fun inspectArrayStructure(content: String): String {
        var inString = false
        var escaped = false
        var unescapedControls = 0
        var invalidEscapes = 0
        var incompleteUnicodeEscapes = 0
        var trailingCommas = 0
        var braces = 0
        var brackets = 0
        var unmatchedClosings = 0
        var previousSignificant: Char? = null
        var index = 0

        while (index < content.length) {
            val current = content[index]

            if (inString) {
                if (escaped) {
                    when (current) {
                        '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> Unit

                        'u' -> {
                            val enough = index + 4 < content.length &&
                                    (1..4).all { shift ->
                                        content[index + shift].isDigit() ||
                                                content[index + shift].lowercaseChar() in 'a'..'f'
                                    }

                            if (!enough) incompleteUnicodeEscapes++
                        }

                        else -> invalidEscapes++
                    }

                    escaped = false
                } else {
                    when {
                        current == '\\' -> escaped = true
                        current == '"' -> inString = false
                        current.code < 0x20 -> unescapedControls++
                    }
                }
            } else {
                when (current) {
                    '"' -> inString = true
                    '{' -> braces++

                    '}' -> {
                        if (previousSignificant == ',') trailingCommas++
                        braces--
                        if (braces < 0) unmatchedClosings++
                    }

                    '[' -> brackets++

                    ']' -> {
                        if (previousSignificant == ',') trailingCommas++
                        brackets--
                        if (brackets < 0) unmatchedClosings++
                    }
                }

                if (!current.isWhitespace()) {
                    previousSignificant = current
                }
            }

            index++
        }

        val first = content.firstOrNull { !it.isWhitespace() }
        val last = content.lastOrNull { !it.isWhitespace() }

        fun typeOf(character: Char?): String = when (character) {
            '[' -> "ABRE_ARRAY"
            ']' -> "FECHA_ARRAY"
            '{' -> "ABRE_OBJETO"
            '}' -> "FECHA_OBJETO"
            null -> "AUSENTE"
            else -> "OUTRO"
        }

        return "inicio=${typeOf(first)} fim=${typeOf(last)} " +
                "stringAberta=$inString escapePendente=$escaped " +
                "controlesEmString=$unescapedControls escapesInvalidos=$invalidEscapes " +
                "unicodeInvalido=$incompleteUnicodeEscapes virgulaFinal=$trailingCommas " +
                "colchetesSaldo=$brackets chavesSaldo=$braces " +
                "fechamentosExtras=$unmatchedClosings"
    }

    /**
     * Diagnóstico agregado sem imprimir IDs, nomes, links ou valores.
     */
    private fun logCatalogParseSummary(
        endpoint: String,
        array: JSONArray,
        accepted: Int,
        ignoredNonObjects: Int,
        ignoredWithoutId: Int
    ) {
        val firstItem = (0 until array.length())
            .asSequence()
            .mapNotNull { index -> array.optJSONObject(index) }
            .firstOrNull()

        val firstKeys = firstItem
            ?.keys()
            ?.asSequence()
            ?.take(24)
            ?.toList()
            ?.sorted()
            ?.joinToString(separator = ",")
            .orEmpty()

        Log.i(
            TAG_CATALOG,
            "$endpoint: itensJSON=${array.length()} " +
                    "aceitos=$accepted naoObjetos=$ignoredNonObjects " +
                    "semIdentificador=$ignoredWithoutId " +
                    "chavesPrimeiroObjeto=[$firstKeys]"
        )
    }

    private companion object {
        const val TAG_CATALOG = "XtreamCatalogParser"
    }

    private fun extractFirstBackdrop(
        item: JSONObject
    ): String? {
        val backdrop = item.opt(
            "backdrop_path"
        )

        return when (backdrop) {
            is JSONArray -> {
                if (backdrop.length() > 0) {
                    backdrop.optString(0)
                        .takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }

            is String -> {
                backdrop.takeIf {
                    it.isNotBlank()
                }
            }

            else -> null
        }
    }

    private fun extractYear(
        item: JSONObject
    ): Int? {
        val year = item.optIntOrNull(
            "year"
        )

        if (year != null && year > 0) {
            return year
        }

        val releaseDate =
            item.optStringOrNull(
                "releaseDate"
            )
                ?: item.optStringOrNull(
                    "release_date"
                )

        return releaseDate
            ?.take(4)
            ?.toIntOrNull()
    }

    private fun extractDurationSeconds(
        item: JSONObject
    ): Int? {
        val durationSeconds =
            item.optIntOrNull(
                "duration_secs"
            )

        if (
            durationSeconds != null &&
            durationSeconds > 0
        ) {
            return durationSeconds
        }

        val duration =
            item.optStringOrNull(
                "duration"
            ) ?: return null

        return parseDurationToSeconds(
            duration
        )
    }

    private fun parseDurationToSeconds(
        duration: String
    ): Int? {
        val parts = duration
            .trim()
            .split(":")
            .mapNotNull {
                it.toIntOrNull()
            }

        return when (parts.size) {
            3 -> {
                parts[0] * 3600 +
                        parts[1] * 60 +
                        parts[2]
            }

            2 -> {
                parts[0] * 60 +
                        parts[1]
            }

            1 -> {
                parts[0]
            }

            else -> null
        }
    }

    private fun detectRatingSource(
        item: JSONObject
    ): RatingSource? {
        return when {
            !item.optStringOrNull(
                "imdb_id"
            ).isNullOrBlank() -> {
                RatingSource.IMDB
            }

            !item.optStringOrNull(
                "tmdb_id"
            ).isNullOrBlank() -> {
                RatingSource.TMDB
            }

            item.has("rating") -> {
                RatingSource.PROVIDER
            }

            else -> null
        }
    }

    private fun JSONObject.optStringOrNull(
        key: String
    ): String? {
        if (!has(key) || isNull(key)) {
            return null
        }

        val value = optString(
            key,
            ""
        ).trim()

        return value.takeIf {
            it.isNotEmpty() &&
                    !it.equals(
                        "null",
                        ignoreCase = true
                    )
        }
    }

    private fun JSONObject.optIntOrNull(
        key: String
    ): Int? {
        if (!has(key) || isNull(key)) {
            return null
        }

        val value = opt(key)

        return when (value) {
            is Number -> value.toInt()

            is String -> value
                .trim()
                .toIntOrNull()

            else -> null
        }
    }

    private fun JSONObject.optLongOrNull(
        key: String
    ): Long? {
        if (!has(key) || isNull(key)) {
            return null
        }

        val value = opt(key)

        return when (value) {
            is Number -> value.toLong()

            is String -> value
                .trim()
                .toLongOrNull()

            else -> null
        }
    }

    private fun JSONObject.optDoubleOrNull(
        key: String
    ): Double? {
        if (!has(key) || isNull(key)) {
            return null
        }

        val value = opt(key)

        return when (value) {
            is Number -> value.toDouble()

            is String -> value
                .trim()
                .replace(",", ".")
                .toDoubleOrNull()

            else -> null
        }
    }
}