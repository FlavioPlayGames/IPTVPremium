package br.com.flaviotorres.iptvpremium.ui.series

import androidx.activity.compose.BackHandler
import android.util.Log
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.data.remote.TmdbClientProvider
import br.com.flaviotorres.iptvpremium.data.repository.SeriesSeasonInfo
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeInfo
import br.com.flaviotorres.iptvpremium.ui.movies.MovieDetailsImageLoader
import br.com.flaviotorres.iptvpremium.domain.model.RatingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

private val CinzelSemiBold = FontFamily(
    Font(
        resId = R.font.cinzel_semibold,
        weight = FontWeight.SemiBold
    )
)

private val MetallicGoldBrush get() = Brush.linearGradient(
    colors = listOf(
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF8A6414),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFD4AF37),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFE991),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFF4BD),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFE3B83F),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF9E761B)
    )
)

/**
 * Conteudo publico do TMDB usado SOMENTE para enriquecer a apresentacao.
 * Nunca representa disponibilidade de episodios na playlist IPTV.
 */
private data class SeriesTmdbVisuals(
    val logoUrls: List<String>,
    val backdropUrl: String?,
    val synopsis: String?,
    val year: Int?,
    val rating: Double?,
    val genres: List<String>,
    val seasonNumbers: List<Int>,
    val resolvedTmdbId: Int?,
    val seasonPosterUrls: Map<Int, String>,
    val alternativeBackdropUrls: List<String>,
    val originalTitle: String? = null,
    val imdb: String? = null,
    val officialNames: List<String> = emptyList(),
    val searchNames: List<String> = emptyList(),
    val localizedTitle: String? = null
)

/** A pesquisa no TMDB precisa terminar antes que a miniatura seja escolhida. */
private sealed interface SeriesTmdbLoadState {
    data object Loading : SeriesTmdbLoadState
    data class Ready(val visuals: SeriesTmdbVisuals?) : SeriesTmdbLoadState
}

/**
 * Fotos oficiais da temporada consultada. O numero do episodio é a chave;
 * nenhuma informacao do TMDB altera os IDs reproduziveis da playlist.
 */
private data class SeriesTmdbSeasonArtwork(
    val episodeStills: Map<Int, String>,
    val posterUrl: String?,
    val episodeNames: Map<Int, String> = emptyMap()
)

/**
 * Uma consulta por temporada visitada, em vez de uma por card.
 * O resultado é guardado durante a permanencia nesta tela.
 */
private suspend fun fetchSeriesTmdbSeasonArtwork(
    tmdbSeriesId: Int,
    seasonNumber: Int
): SeriesTmdbSeasonArtwork {
    val response = JSONObject(
        TmdbClientProvider.client.get(
            path = "/tv/$tmdbSeriesId/season/$seasonNumber",
            queryParameters = mapOf("language" to "pt-BR")
        )
    )
    val episodes = response.optJSONArray("episodes") ?: JSONArray()
    val stills = buildMap<Int, String> {
        for (index in 0 until episodes.length()) {
            val item = episodes.optJSONObject(index) ?: continue
            val episodeNumber = item.optInt("episode_number", -1)
            if (episodeNumber < 1) continue
            val still = tmdbImageUrl("w780", item.optString("still_path"))
            if (still != null) put(episodeNumber, still)
        }
    }
    return SeriesTmdbSeasonArtwork(
        episodeStills = stills,
        posterUrl = tmdbImageUrl("w500", response.optString("poster_path")),
        episodeNames = (0 until episodes.length()).mapNotNull { index ->
            val item = episodes.optJSONObject(index) ?: return@mapNotNull null
            val number = item.optInt("episode_number", -1)
            item.optString("name").takeIf { number > 0 && it.isNotBlank() }?.let { number to it }
        }.toMap()
    )
}

private fun cleanSeriesTitle(value: String): String =
    br.com.flaviotorres.iptvpremium.data.repository.artworkTitle(value)

private fun tmdbImageUrl(size: String, path: String?): String? =
    path?.trim()?.takeIf { it.startsWith("/") }
        ?.let { "https://image.tmdb.org/t/p/$size$it" }

/**
 * Replica a prioridade de logo aprovada em Filmes: pt -> sem idioma -> en.
 * Mantemos outras linguas como ultimo recurso, sem fabricar uma imagem.
 */
private fun prioritizedSeriesLogoUrls(items: List<JSONObject>): List<String> =
    items.asSequence()
        .filter { it.optString("file_path").trim().startsWith("/") }
        .sortedWith(
            compareBy<JSONObject> { item ->
                val language = if (item.isNull("iso_639_1")) ""
                else item.optString("iso_639_1").trim().lowercase(Locale.ROOT)
                when (language) {
                    "pt" -> 0
                    "", "null" -> 1
                    "en" -> 2
                    else -> 3
                }
            }.thenByDescending { it.optDouble("vote_average", 0.0) }
        )
        .mapNotNull { tmdbImageUrl("w500", it.optString("file_path")) }
        .distinct()
        .take(6)
        .toList()

private fun JSONObject.tmdbLogoItems(): List<JSONObject> {
    val logos = optJSONArray("logos") ?: return emptyList()
    return (0 until logos.length()).mapNotNull { logos.optJSONObject(it) }
}

/**
 * Consulta a serie pelo ID, quando confiavel, ou pelo nome/ano.
 * O bloco `images` anexado nem sempre fornece logos; por isso a rota
 * /tv/{id}/images e consultada separadamente, sem alterar o catálogo.
 */
private suspend fun fetchSeriesTmdbVisuals(
    seriesName: String,
    year: Int?,
    tmdbId: String?,
    imdbId: String?
): SeriesTmdbVisuals? {
    val client = TmdbClientProvider.client
    val root = br.com.flaviotorres.iptvpremium.data.repository.resolveSeriesArtworkIdentity(
        seriesName, year, tmdbId, imdbId, client::get
    )
    val details = root ?: return null
    val resolvedId = details.optInt("id", -1).takeIf { it > 0 }

    // Reforco: o endpoint dedicado de imagens pode retornar logos que nao
    // vieram na resposta de detalhes. Nao depende de existir backdrop/poster.
    val appendedLogos = details.optJSONObject("images")?.tmdbLogoItems().orEmpty()
    val dedicatedImages = if (resolvedId != null) {
        try {
            JSONObject(client.get(
                path = "/tv/$resolvedId/images",
                queryParameters = mapOf(
                    "language" to "pt-BR",
                    "include_image_language" to "pt,null,en"
                )
            ))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("SeriesDetailsTmdb", "Imagens indisponiveis: ${error.javaClass.simpleName}")
            null
        }
    } else null
    val dedicatedLogos = dedicatedImages?.tmdbLogoItems().orEmpty()

    // Artes secundarias distintas da imagem principal, para usar SOMENTE
    // quando nenhuma miniatura especifica do episodio puder ser carregada.
    // Reaproveita a consulta de imagens das logos, sem requests por card.
    val mainBackdropPath = details.optString("backdrop_path").trim()
    val allBackdrops = listOfNotNull(
        dedicatedImages?.optJSONArray("backdrops"),
        details.optJSONObject("images")?.optJSONArray("backdrops")
    ).flatMap { array ->
        (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }
    val secondaryBackdrops = allBackdrops
        .asSequence()
        .filter { it.optString("file_path").startsWith("/") }
        .filter { it.optString("file_path") != mainBackdropPath }
        .sortedByDescending { it.optDouble("vote_average", 0.0) }
        .mapNotNull { tmdbImageUrl("w780", it.optString("file_path")) }
        .distinct()
        .take(10)
        .toList()

    var allLogos = appendedLogos + dedicatedLogos
    if (allLogos.isEmpty() && resolvedId != null) {
        // Ultimo recurso: sem filtro por idioma, caso so existam outras versoes.
        val unrestricted = try {
            JSONObject(client.get(path = "/tv/$resolvedId/images"))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        allLogos = unrestricted?.tmdbLogoItems().orEmpty()
    }
    val logoUrls = prioritizedSeriesLogoUrls(allLogos)
    Log.d("SeriesDetailsTmdb", "Logos oficiais encontrados: ${logoUrls.size}")

    val seasonItems = details.optJSONArray("seasons") ?: JSONArray()
    val seasonNumbers = (0 until seasonItems.length())
        .mapNotNull { seasonItems.optJSONObject(it)?.optInt("season_number", -1) }
        .filter { it >= 0 }
        .distinct()
        .sorted()
    val seasonPosterUrls = buildMap<Int, String> {
        for (index in 0 until seasonItems.length()) {
            val season = seasonItems.optJSONObject(index) ?: continue
            val number = season.optInt("season_number", -1)
            if (number < 0) continue
            tmdbImageUrl("w500", season.optString("poster_path"))?.let {
                put(number, it)
            }
        }
    }

    val genreItems = details.optJSONArray("genres") ?: JSONArray()
    val tmdbGenres = (0 until genreItems.length())
        .mapNotNull { genreItems.optJSONObject(it)?.optString("name") }
        .filter { it.isNotBlank() }

    val translations = details.optJSONObject("translations")
        ?.optJSONArray("translations") ?: JSONArray()
    fun translation(language: String, country: String? = null): String? =
        (0 until translations.length())
            .mapNotNull { translations.optJSONObject(it) }
            .firstOrNull {
                it.optString("iso_639_1") == language &&
                        (country == null || it.optString("iso_3166_1") == country)
            }
            ?.optJSONObject("data")
            ?.optString("overview")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    val synopsis = details.optString("overview").trim().takeIf { it.isNotEmpty() }
        ?: translation("pt", "BR")
        ?: translation("pt", "PT")
        ?: translation("en", "US")
        ?: translation("en")

    return SeriesTmdbVisuals(
        logoUrls = logoUrls,
        backdropUrl = tmdbImageUrl("original", details.optString("backdrop_path")),
        synopsis = synopsis,
        year = details.optString("first_air_date").take(4).toIntOrNull(),
        rating = details.optDouble("vote_average", 0.0)
            .takeIf { it.isFinite() && it > 0.0 },
        genres = tmdbGenres,
        seasonNumbers = seasonNumbers,
        resolvedTmdbId = resolvedId,
        seasonPosterUrls = seasonPosterUrls,
        alternativeBackdropUrls = secondaryBackdrops,
        originalTitle = details.optString("original_name").takeIf { it.isNotBlank() },
        officialNames = br.com.flaviotorres.iptvpremium.data.repository.officialSeriesNames(details),
        searchNames = br.com.flaviotorres.iptvpremium.data.repository.preferredSeriesSearchNames(details),
        localizedTitle = details.optString("name").trim().takeIf { it.isNotBlank() },
        imdb = details.optJSONObject("external_ids")?.optString("imdb_id")?.takeIf { it.startsWith("tt") }
    )
}

/**
 * Detalhes de Séries: apresentação e interação espelhadas nos detalhes de Filmes.
 * Temporadas e episódios vêm da playlist; o TMDB complementa apenas metadados.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SeriesDetailsScreen(
    seriesName: String = "Série",
    tmdbId: String? = null,
    imdbId: String? = null,
    backdropUrl: String? = null,
    titleLogoUrl: String? = null,
    rating: Double? = null,
    ratingSource: RatingSource? = null,
    year: Int? = null,
    durationSeconds: Int? = null,
    genres: List<String> = emptyList(),
    plot: String = "",
    seasons: List<SeriesSeasonInfo> = emptyList(),
    episodes: List<SeriesEpisodeInfo> = emptyList(),
    isLoadingEpisodes: Boolean = false,
    episodesMessage: String? = null,
    onRetryEpisodes: () -> Unit = {},
    trailerAvailable: Boolean = false,
    isFavorite: Boolean = false,
    episodeProgress: Map<String, Float> = emptyMap(),
    preferredSeasonNumber: Int? = null,
    focusEpisodeId: String? = null,
    episodeImageKey: String = "",
    onArtworkReady: (Boolean) -> Unit = {},
    onSeasonSelected: (Int) -> Unit = {},
    onWatchClick: () -> Unit = {},
    onTrailerClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onEpisodeClick: (String) -> Unit = {},
    ratingPlaylistId: String? = null,
    ratingContentId: String? = null,
    ratingIdentityTitle: String? = null,
    ratingIdentityYear: Int? = null
) {
    val availableSeasons = remember(seasons, episodes) {
        br.com.flaviotorres.iptvpremium.data.repository.playableSeriesSeasons(seasons, episodes)
    }
    var favoriteState by remember(isFavorite) { mutableStateOf(isFavorite) }

    // Metadados enriquecem a ficha; o fundo ja carregado tambem serve aos
    // episodios sem foto, sem consultar nem decodificar seus videos.
    val tmdbLoadState = key(seriesName, year, tmdbId, imdbId) {
        val loadState by produceState<SeriesTmdbLoadState>(
            initialValue = SeriesTmdbLoadState.Loading
        ) {
            if (seriesName == "Série" && episodes.isEmpty() && tmdbId.isNullOrBlank() && imdbId.isNullOrBlank()) {
                value = SeriesTmdbLoadState.Ready(null)
                return@produceState
            }
            Log.d("SeriesArtwork", "fonte=tmdb etapa=inicio configurado=${br.com.flaviotorres.iptvpremium.BuildConfig.TMDB_READ_TOKEN.isNotBlank()} id_tmdb=${!tmdbId.isNullOrBlank()} id_imdb=${!imdbId.isNullOrBlank()}")
            var lookupCompleted = false
            val visuals = try {
                kotlinx.coroutines.withTimeoutOrNull(4_000L) { fetchSeriesTmdbVisuals(seriesName, year, tmdbId, imdbId).also { lookupCompleted = true } }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Não registrar URLs/credenciais da playlist.
                Log.w(
                    "SeriesDetailsTmdb",
                    "Metadados indisponíveis: ${error.javaClass.simpleName}"
                )
                null
            }
            Log.d("SeriesArtwork", "fonte=tmdb etapa=fim concluido=$lookupCompleted encontrado=${visuals != null} logos=${visuals?.logoUrls?.size ?: 0}")
            value = SeriesTmdbLoadState.Ready(visuals)
        }
        loadState
    }
    val tmdbVisuals = (tmdbLoadState as? SeriesTmdbLoadState.Ready)?.visuals

    val displayTitle = tmdbVisuals?.localizedTitle ?: seriesName
    val displayPlot = tmdbVisuals?.synopsis?.takeIf(String::isNotBlank) ?: plot
    val displayRating = rating ?: tmdbVisuals?.rating
    val displayRatingSource = ratingSource ?: if (rating == null && tmdbVisuals?.rating != null) {
        RatingSource.TMDB
    } else null
    val displayYear = year ?: tmdbVisuals?.year
    val displayGenres = tmdbVisuals?.genres?.takeIf { it.isNotEmpty() } ?: genres
    val informationalSeasons = emptyList<Int>()

    var showFullSynopsis by remember(seriesName) { mutableStateOf(false) }
    var synopsisPreviewTruncated by remember(seriesName, displayPlot) { mutableStateOf(false) }
    BackHandler(enabled = showFullSynopsis) { showFullSynopsis = false }

    // Episódios reais sempre prevalecem. Especiais só abre primeiro se necessário.
    var selectedSeason by rememberSaveable(seriesName) {
        mutableIntStateOf(preferredSeasonNumber ?: -1)
    }
    // Refaz a temporada gravada na rota mesmo quando o NavHost recria o
    // composable apos a reproducao. Nao reseta temporada escolhida a mao.
    LaunchedEffect(preferredSeasonNumber) {
        if (preferredSeasonNumber != null && selectedSeason != preferredSeasonNumber) {
            selectedSeason = preferredSeasonNumber
        }
    }
    val firstAvailableSeason = remember(availableSeasons, episodes) {
        val withEpisodes = episodes.map { it.seasonNumber }.toSet()
        availableSeasons.firstOrNull {
            it.number > 0 && (it.episodeCount > 0 || it.number in withEpisodes)
        }?.number ?: availableSeasons.firstOrNull {
            it.episodeCount > 0 || it.number in withEpisodes
        }?.number ?: availableSeasons.firstOrNull { it.number > 0 }?.number
        ?: availableSeasons.firstOrNull()?.number
    }
    val currentSeason = when {
        availableSeasons.any { it.number == selectedSeason } -> selectedSeason
        preferredSeasonNumber != null && availableSeasons.any { it.number == preferredSeasonNumber } -> preferredSeasonNumber
        firstAvailableSeason != null -> firstAvailableSeason
        else -> -1
    }
    val displayedEpisodes = remember(episodes, currentSeason) {
        episodes.filter { it.seasonNumber == currentSeason }
            .sortedWith(compareBy<SeriesEpisodeInfo> { it.episodeNumber }.thenBy { it.id })
    }
    val episodeListState = rememberLazyListState()
    val resumedEpisodeFocusRequester = remember(seriesName) { FocusRequester() }
    // Rola ate o episodio que acabou de ser aberto e recupera o foco nele,
    // ao inves de jogar o usuario no primeiro botao de toda a tela.
    LaunchedEffect(focusEpisodeId, currentSeason, displayedEpisodes) {
        if (focusEpisodeId.isNullOrBlank()) return@LaunchedEffect
        val episodeIndex = displayedEpisodes.indexOfFirst { it.id == focusEpisodeId }
        if (episodeIndex >= 0) {
            episodeListState.scrollToItem(episodeIndex)
            withFrameNanos { }
            runCatching { resumedEpisodeFocusRequester.requestFocus() }
        }
    }

    // Consulta as fotos somente da temporada selecionada. O still oficial
    // tem prioridade; só usamos miniatura específica do provedor como alternativa.
    val seasonArtworks = remember(seriesName, tmdbId, tmdbVisuals?.resolvedTmdbId) {
        mutableStateMapOf<Int, SeriesTmdbSeasonArtwork>()
    }
    LaunchedEffect(tmdbVisuals?.resolvedTmdbId, currentSeason, displayedEpisodes.isNotEmpty()) {
        val resolvedId = tmdbVisuals?.resolvedTmdbId ?: return@LaunchedEffect
        if (currentSeason < 0 || displayedEpisodes.isEmpty() ||
            seasonArtworks.containsKey(currentSeason)
        ) return@LaunchedEffect
        seasonArtworks[currentSeason] = try {
            kotlinx.coroutines.withTimeoutOrNull(3_000L) { fetchSeriesTmdbSeasonArtwork(resolvedId, currentSeason) }
                ?: SeriesTmdbSeasonArtwork(emptyMap(), null)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Ausencia no TMDB nao impede a exibicao dos episodios da playlist.
            Log.w("SeriesDetailsTmdb", "Imagens da temporada indisponiveis: ${error.javaClass.simpleName}")
            SeriesTmdbSeasonArtwork(emptyMap(), null)
        }
    }
    val currentSeasonArtwork = seasonArtworks[currentSeason]
    val artworkContext = LocalContext.current.applicationContext
    val extraClient = remember(artworkContext) { br.com.flaviotorres.iptvpremium.data.remote.ArtworkHttpCache.getInstance(artworkContext) }
    val extraRepository = remember(extraClient) {
        br.com.flaviotorres.iptvpremium.data.repository.SeriesArtworkFallback(extraClient::get,
            br.com.flaviotorres.iptvpremium.BuildConfig.FANART_API_KEY.isNotBlank())
    }
    var extraArt by remember(episodeImageKey, seriesName, currentSeason) {
        mutableStateOf(br.com.flaviotorres.iptvpremium.data.repository.SupplementalSeriesArt())
    }
    var extraSettled by remember(episodeImageKey, currentSeason) { mutableStateOf(false) }
    var primaryLogoFailed by remember(seriesName, tmdbVisuals?.logoUrls, titleLogoUrl) { mutableStateOf(false) }
    var primaryBackdropFailed by remember(seriesName, tmdbVisuals?.backdropUrl, backdropUrl) { mutableStateOf(false) }
    val primaryPhotoFailures = remember(episodeImageKey, currentSeason) { mutableStateMapOf<String, Boolean>() }
    val primaryMetadataSettled = tmdbLoadState is SeriesTmdbLoadState.Ready &&
        (tmdbVisuals?.resolvedTmdbId == null || currentSeasonArtwork != null)
    val missingEpisodeNumbers = displayedEpisodes.filter {
        br.com.flaviotorres.iptvpremium.data.repository.episodeReferenceImages(
            currentSeasonArtwork?.episodeStills?.get(it.episodeNumber), it.thumbnailUrl).isEmpty() ||
                primaryPhotoFailures[it.id] == true
    }.map { it.episodeNumber }.filter { it > 0 }.toSet()
    val needExtraLogo = (titleLogoUrl.isNullOrBlank() && tmdbVisuals?.logoUrls.isNullOrEmpty()) || primaryLogoFailed
    val needExtraBackdrop = (backdropUrl.isNullOrBlank() && tmdbVisuals?.backdropUrl.isNullOrBlank()) || primaryBackdropFailed
    LaunchedEffect(episodeImageKey, currentSeason, primaryMetadataSettled, missingEpisodeNumbers.isNotEmpty(),
        needExtraLogo, needExtraBackdrop, tmdbVisuals?.originalTitle) {
        extraSettled = false
        if (!primaryMetadataSettled) return@LaunchedEffect
        if (missingEpisodeNumbers.isEmpty() && !needExtraLogo && !needExtraBackdrop) {
            extraSettled = true
            return@LaunchedEffect
        }
        Log.d("SeriesArtwork", "etapa=alternativas inicio miniaturas=${missingEpisodeNumbers.size} logo=$needExtraLogo fundo=$needExtraBackdrop")
        val result = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
            extraRepository.load(br.com.flaviotorres.iptvpremium.data.repository.ArtworkIdentity(
                (tmdbVisuals?.searchNames.orEmpty() + seriesName).distinct(),
                year ?: tmdbVisuals?.year, imdbId ?: tmdbVisuals?.imdb),
                currentSeason,
                if (missingEpisodeNumbers.isNotEmpty()) displayedEpisodes.map { it.episodeNumber }.filter { it > 0 }.toSet() else emptySet(),
                needExtraLogo, needExtraBackdrop)
        }
        if (result != null) extraArt = extraArt.copy(
            logoUrls = result.logoUrls.ifEmpty { extraArt.logoUrls },
            backdropUrl = result.backdropUrl ?: extraArt.backdropUrl,
            episodeStills = extraArt.episodeStills + result.episodeStills,
            matched = extraArt.matched || result.matched
        )
        extraSettled = true
        Log.d("SeriesArtwork", "etapa=alternativas fim identificado=${extraArt.matched} fotos=${extraArt.episodeStills.size} logos=${extraArt.logoUrls.size}")
    }

    val completedEpisodeImages = remember(episodeImageKey, currentSeason) {
        mutableStateMapOf<String, List<String>>()
    }
    val visibleEpisodeIds by remember(episodeListState, displayedEpisodes) {
        derivedStateOf {
            episodeListState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
                displayedEpisodes.getOrNull(item.index)?.id
            }
        }
    }
    val context = LocalContext.current.applicationContext
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val compactLayout = configuration.screenHeightDp < 500 || configuration.screenWidthDp < 800
    val backdropWidthPx = with(density) {
        configuration.screenWidthDp.dp.roundToPx().coerceAtLeast(1)
    }
    val backdropHeightPx = with(density) {
        configuration.screenHeightDp.dp.roundToPx().coerceAtLeast(1)
    }
    val logoWidth = if (compactLayout) 250.dp else 300.dp
    val logoHeight = if (compactLayout) 50.dp else 72.dp
    val logoWidthPx = with(density) { logoWidth.roundToPx().coerceAtLeast(1) }
    val logoHeightPx = with(density) { logoHeight.roundToPx().coerceAtLeast(1) }

    val effectiveBackdropUrl = (if (primaryBackdropFailed) extraArt.backdropUrl else null) ?: backdropUrl?.takeIf {
        it.startsWith("https://", true) || it.startsWith("http://", true)
    } ?: tmdbVisuals?.backdropUrl ?: extraArt.backdropUrl
    var backdropFinishedFor by remember(seriesName, tmdbId) { mutableStateOf<String?>(null) }
    val backdropBitmap by produceState<ImageBitmap?>(
        initialValue = remember(effectiveBackdropUrl, backdropWidthPx, backdropHeightPx) {
            MovieDetailsImageLoader.peekFromMemory(
                context, effectiveBackdropUrl, backdropWidthPx, backdropHeightPx
            )
        },
        key1 = effectiveBackdropUrl,
        key2 = backdropWidthPx to backdropHeightPx
    ) {
        value = MovieDetailsImageLoader.load(
            context, effectiveBackdropUrl, backdropWidthPx, backdropHeightPx, "series_backdrop"
        )
        if (value == null && effectiveBackdropUrl != null && effectiveBackdropUrl != extraArt.backdropUrl) primaryBackdropFailed = true
        backdropFinishedFor = effectiveBackdropUrl
    }

    // Mesmo criterio de Filmes: logo do provedor quando valida,
    // depois TMDB em portugues, neutra, ingles e outros idiomas.
    val providerLogoUrl = titleLogoUrl?.trim()?.takeIf {
        it.startsWith("https://", true) || it.startsWith("http://", true)
    }
    val logoCandidates = remember(tmdbVisuals?.logoUrls, providerLogoUrl, extraArt.logoUrls) {
        (listOfNotNull(providerLogoUrl) + tmdbVisuals?.logoUrls.orEmpty() + extraArt.logoUrls).distinct()
    }
    var logoFinishedFor by remember(seriesName, tmdbId) { mutableStateOf<List<String>?>(null) }
    val logoBitmap by produceState<ImageBitmap?>(
        initialValue = remember(logoCandidates, logoWidthPx, logoHeightPx) {
            logoCandidates.firstNotNullOfOrNull { url ->
                MovieDetailsImageLoader.peekFromMemory(context, url, logoWidthPx, logoHeightPx)
            }
        },
        key1 = logoCandidates,
        key2 = logoWidthPx to logoHeightPx
    ) {
        for (url in logoCandidates) {
            val bitmap = MovieDetailsImageLoader.load(
                context, url, logoWidthPx, logoHeightPx, "series_logo"
            )
            if (bitmap != null) {
                value = bitmap
                break
            }
        }
        if (value == null && logoCandidates.isNotEmpty()) {
            primaryLogoFailed = true
            Log.w("SeriesDetailsTmdb", "Nenhuma logo candidata foi decodificada")
        }
        logoFinishedFor = logoCandidates
    }

    val tmdbSettled = tmdbLoadState is SeriesTmdbLoadState.Ready
    val seasonSettled = tmdbVisuals?.resolvedTmdbId == null || currentSeasonArtwork != null
    val visiblePhotosSettled = displayedEpisodes.isEmpty() || (visibleEpisodeIds.isNotEmpty() &&
        visibleEpisodeIds.all { id ->
            val episode = displayedEpisodes.firstOrNull { it.id == id }
            episode != null && completedEpisodeImages[id] ==
                br.com.flaviotorres.iptvpremium.data.repository.episodeReferenceImages(
                    currentSeasonArtwork?.episodeStills?.get(episode.episodeNumber), episode.thumbnailUrl, extraArt.episodeStills[episode.episodeNumber])
        })
    val artworkReady = tmdbSettled && seasonSettled && extraSettled && logoFinishedFor == logoCandidates &&
        (effectiveBackdropUrl.isNullOrBlank() || backdropFinishedFor == effectiveBackdropUrl) && visiblePhotosSettled
    val latestArtworkReady by rememberUpdatedState(onArtworkReady)
    LaunchedEffect(artworkReady) { latestArtworkReady(artworkReady) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF070708))
    ) {
        // Gradientes, foto e FillBounds copiados da apresentação de Filmes.
        Box(
            modifier = Modifier.fillMaxSize().then(
                if (showFullSynopsis) Modifier.blur(16.dp) else Modifier
            )
        ) {
            if (backdropBitmap != null) {
                Image(
                    bitmap = backdropBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF080809), Color(0xFF0B0B0D), Color(0xFF111113))
                        )
                    )
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xE6000000),
                            0.38f to Color(0xB3000000),
                            0.68f to Color(0x3D000000),
                            1.00f to Color(0x10000000)
                        )
                    )
                )
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x10000000), Color.Transparent, Color(0x66000000)
                        )
                    )
                )
            )

            // O topo sobe discretamente; o rodape preserva margem de segurança.
            // Duas regiões fixas impedem que o texto de sinopses longas empurre
            // as fileiras. Episodios mantêm o tamanho aprovado anteriormente.
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(
                    start = if (compactLayout) 12.dp else 16.dp,
                    end = if (compactLayout) 12.dp else 16.dp,
                    top = if (compactLayout) 8.dp else 10.dp,
                    bottom = if (compactLayout) 20.dp else 28.dp
                )
            ) {
                // A parte inferior reserva pelo menos o espaço necessário para
                // o seletor e para os cards integrais, com rótulo e foco visíveis.
                val minimumLowerHeight = if (compactLayout) 194.dp else 215.dp
                val heroHeight = (maxHeight * 0.54f)
                    .coerceAtMost((maxHeight - minimumLowerHeight).coerceAtLeast(0.dp))

                val denseHeader = heroHeight < 170.dp
                Column(modifier = Modifier.fillMaxSize()) {
                    // Cabecalho invariavel: logo, metadados, sinopse e acoes.
                    Column(
                        modifier = Modifier.fillMaxWidth().height(heroHeight).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap!!,
                                contentDescription = seriesName,
                                modifier = Modifier.width(logoWidth).height(if(denseHeader)32.dp else logoHeight),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart
                            )
                        } else {
                            Text(
                                text = displayTitle.uppercase(),
                                modifier = Modifier.fillMaxWidth(
                                    if (compactLayout) 0.78f else 0.60f
                                ),
                                style = TextStyle(
                                    fontFamily = if(br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active)CinzelSemiBold else FontFamily.SansSerif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = if(denseHeader)20.sp else if (compactLayout) 28.sp else 32.sp,
                                    brush = MetallicGoldBrushText
                                ),
                                maxLines = if(denseHeader)1 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(if(denseHeader)3.dp else if (compactLayout) 7.dp else 9.dp))
                        br.com.flaviotorres.iptvpremium.ui.components.ReferenceMetadataRow(
                            rating = displayRating, year = displayYear, durationSeconds = durationSeconds,
                            genres = displayGenres, compact = compactLayout, dense = denseHeader
                        )
                        Spacer(Modifier.height(if(denseHeader)3.dp else if (compactLayout) 8.dp else 10.dp))
                        if (displayPlot.isNotBlank()) {
                            Text(
                                text = displayPlot,
                                modifier = Modifier.fillMaxWidth(
                                    if (compactLayout) 0.78f else 0.60f
                                ).then(if(denseHeader) Modifier.clickable(onClickLabel="Ler sinopse completa") {showFullSynopsis=true} else Modifier),
                                color = br.com.flaviotorres.iptvpremium.ui.components.ReferenceDetailsPalette.text,
                                fontSize = if (compactLayout) 13.sp else 14.sp,
                                lineHeight = if(denseHeader)14.sp else if (compactLayout) 15.sp else 18.sp,
                                maxLines = if(denseHeader)1 else if (compactLayout) 2 else 3,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { layout ->
                                    if (synopsisPreviewTruncated != layout.hasVisualOverflow) {
                                        synopsisPreviewTruncated = layout.hasVisualOverflow
                                    }
                                }
                            )
                            if (synopsisPreviewTruncated && !denseHeader) {
                                Spacer(Modifier.height(3.dp))
                                SeriesTextAction(
                                    text = "Ler sinopse completa",
                                    onClick = { showFullSynopsis = true }
                                )
                            }
                            Spacer(Modifier.height(if(denseHeader)3.dp else if (compactLayout) 8.dp else 10.dp))
                        }
                        br.com.flaviotorres.iptvpremium.ui.components.ReferenceDetailActions(
                            watchText = if (!isLoadingEpisodes && episodes.isEmpty()) "TENTAR NOVAMENTE" else "ASSISTIR", compact = compactLayout, isFavorite = favoriteState,
                            onWatch = if (!isLoadingEpisodes && episodes.isEmpty()) onRetryEpisodes else onWatchClick,
                            onFavorite = { favoriteState = !favoriteState; onFavoriteClick() },
                            kind = "series", playlist = ratingPlaylistId, content = ratingContentId,
                            tmdb = tmdbVisuals?.resolvedTmdbId?.toString() ?: tmdbId, imdb = tmdbVisuals?.imdb ?: imdbId, dense = denseHeader, identityTitle = ratingIdentityTitle, identityYear = ratingIdentityYear
                        )
                    }

                    // Zona inferior começa na mesma altura para qualquer serie,
                    // com a lista de episodios próxima ao rodape, sem colar nele.
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = "TEMPORADAS",
                            style = TextStyle(
                                fontFamily = if(br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active)CinzelSemiBold else FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (compactLayout) 16.sp else 18.sp,
                                brush = MetallicGoldBrushText
                            )
                        )
                        Spacer(Modifier.height(if (compactLayout) 5.dp else 7.dp))
                        when {
                            availableSeasons.isNotEmpty() -> LazyRow(
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(
                                    if (compactLayout) 9.dp else 11.dp
                                )
                            ) {
                                items(availableSeasons, key = { it.number }) { season ->
                                    SeasonButton(
                                        seasonNumber = season.number,
                                        isSelected = currentSeason == season.number,
                                        compactLayout = compactLayout,
                                        onClick = {
                                            selectedSeason = season.number
                                            onSeasonSelected(season.number)
                                        }
                                    )
                                }
                            }
                            informationalSeasons.isNotEmpty() -> LazyRow(
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(informationalSeasons, key = { it }) { number ->
                                    TmdbSeasonInfoChip(number, compactLayout)
                                }
                            }
                            else -> Spacer(Modifier.height(38.dp))
                        }
                        // O título "EPISÓDIOS" foi removido a pedido. A identificação
                        // EPISÓDIO XX permanece sob cada miniatura.
                        Spacer(Modifier.height(if (compactLayout) 7.dp else 10.dp))
                        // O espaço livre entre temporadas e a grade empurra os cards
                        // para baixo, preservando integralmente suas dimensões.
                        if (displayedEpisodes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = when {
                                        isLoadingEpisodes -> "Carregando temporadas e episódios..."
                                        availableSeasons.isEmpty() && informationalSeasons.isNotEmpty() ->
                                            "Nenhum episódio disponível para reprodução nesta playlist."
                                        episodesMessage != null -> episodesMessage
                                        availableSeasons.isEmpty() -> "Nenhuma temporada disponível para esta série."
                                        episodes.isEmpty() ->
                                            "O provedor informou temporadas, mas não retornou episódios reproduzíveis."
                                        else -> "Não há episódios nesta temporada. Selecione outra temporada."
                                    },
                                    color = Color(0xFFB7B7BD),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                state = episodeListState,
                                // Respiro vertical também para a borda ampliada no foco.
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(
                                    if (compactLayout) 9.dp else 11.dp
                                ),
                                verticalAlignment = Alignment.Top
                            ) {
                                items(
                                    displayedEpisodes,
                                    key = { episode -> "$currentSeason:${episode.id}" }
                                ) { episode ->
                                    EpisodeCard(
                                        episodeNumber = episode.episodeNumber,
                                        name = currentSeasonArtwork?.episodeNames?.get(episode.episodeNumber) ?: episode.title,
                                        imageKey = "$episodeImageKey:${episode.id}",
                                        seriesBackdrop = backdropBitmap,
                                        onPhotoResolved = { images, success ->
                                            completedEpisodeImages[episode.id] = images
                                            if (!success && extraArt.episodeStills[episode.episodeNumber] == null)
                                                primaryPhotoFailures[episode.id] = true
                                        },
                                        supplementalStillUrl = extraArt.episodeStills[episode.episodeNumber],
                                        thumbnailUrl = episode.thumbnailUrl,
                                        tmdbEpisodeStillUrl = currentSeasonArtwork
                                            ?.episodeStills?.get(episode.episodeNumber),
                                        compactLayout = compactLayout,
                                        watchProgress = episodeProgress[episode.id],
                                        focusRequester = if (episode.id == focusEpisodeId)
                                            resumedEpisodeFocusRequester else null,
                                        onClick = { onEpisodeClick(episode.id) }
                                    )
                                }
                            }
                        }
                    } // Zona inferior
                } // Column principal
            } // BoxWithConstraints: alturas proporcionais
        }

        if (extraArt.episodeStills.isNotEmpty() || extraArt.logoUrls.isNotEmpty() || extraArt.backdropUrl != null) {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Row(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                Text("TVmaze", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp,
                    modifier = Modifier.clickable { uriHandler.openUri("https://www.tvmaze.com") })
                if (extraArt.logoUrls.isNotEmpty()) Text(" · fanart.tv", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp,
                    modifier = Modifier.clickable { uriHandler.openUri("https://fanart.tv") })
            }
        }

        if (showFullSynopsis && displayPlot.isNotBlank()) {
            Dialog(
                onDismissRequest = { showFullSynopsis = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                SeriesSynopsisOverlay(
                    title = displayTitle,
                    synopsis = displayPlot,
                    onClose = { showFullSynopsis = false }
                )
            }
        }
    }
}

@Composable
private fun TmdbSeasonInfoChip(number: Int, compactLayout: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .width(if (compactLayout) 118.dp else 130.dp)
            .height(if (compactLayout) 28.dp else 30.dp)
            .background(Color.Transparent, shape)
            .border(BorderStroke(1.3.dp, MetallicGoldBrush), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (number == 0) "ESPECIAIS" else "TEMPORADA $number",
            color = Color(0xFFFFD66B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SeriesTextAction(
    text: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    Text(
        text = text,
        style = if (isFocused) TextStyle.Default else TextStyle(brush = (if (isFocused) Brush.linearGradient(listOf(Color.White, Color.White)) else MetallicGoldBrushText)),
        color = if (isFocused) Color.White else Color.Unspecified,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                if (isFocused) 1.dp else 0.dp,
                MetallicGoldBrush,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
private fun SeriesSynopsisOverlay(
    title: String,
    synopsis: String,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scrollScope = rememberCoroutineScope()
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(closeFocus) {
        withFrameNanos { }
        closeFocus.requestFocus()
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color(0x5203070D)),
        contentAlignment = Alignment.Center
    ) {
        val tight = maxWidth < 860.dp || maxHeight < 550.dp
        val scrollMaxHeight = maxHeight * 0.53f
        Column(
            modifier = Modifier
                .fillMaxWidth(if (tight) 0.90f else 0.82f)
                .widthIn(max = 1050.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) false
                    else when (event.key) {
                        Key.DirectionDown -> {
                            if (scrollState.value < scrollState.maxValue) {
                                scrollScope.launch { scrollState.scrollBy(110f) }
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (scrollState.value > 0) {
                                scrollScope.launch { scrollState.scrollBy(-110f) }
                            }
                            true
                        }
                        else -> false
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title, color = Color.White,
                fontSize = if (tight) 22.sp else 30.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(if (tight) 14.dp else 22.dp))
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = scrollMaxHeight)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = synopsis, color = Color.White,
                    fontSize = if (tight) 14.sp else 17.sp,
                    lineHeight = if (tight) 19.sp else 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(if (tight) 12.dp else 20.dp))
            SeriesTextAction(text = "FECHAR", onClick = onClose, focusRequester = closeFocus)
        }
    }
}

@Composable
private fun SeriesMetadata(
    rating: Double?,
    ratingSource: RatingSource?,
    year: Int?,
    durationSeconds: Int?,
    genres: List<String>,
    compactLayout: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 5.dp else 6.dp)
    ) {
        if (rating != null) {
            Text(
                text = seriesRatingStars(rating),
                modifier = Modifier,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFE6A0),
                fontSize = if (compactLayout) 12.sp else 13.sp,
                lineHeight = if (compactLayout) 12.sp else 13.sp
            )
            Text(
                text = seriesRatingValue(rating),
                modifier = Modifier,
                color = Color.White,
                fontSize = if (compactLayout) 12.sp else 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (ratingSource == RatingSource.IMDB) {
                Box(
                    modifier = Modifier.background(
                        color = Color(0xFFD7AF2D), shape = RoundedCornerShape(4.dp)
                    ).padding(horizontal = if (compactLayout) 7.dp else 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "IMDb", color = Color(0xFF111113),
                        fontSize = if (compactLayout) 8.sp else 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (year != null || durationSeconds != null || genres.isNotEmpty()) {
                MetadataSeparator()
            }
        }
        if (year != null) {
            Text(
                text = year.toString(), color = Color.White,
                fontSize = if (compactLayout) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (durationSeconds != null || genres.isNotEmpty()) MetadataSeparator()
        }
        val durationText = formatDuration(durationSeconds)
        if (durationText != null) {
            Text(
                text = durationText, color = Color.White,
                fontSize = if (compactLayout) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (genres.isNotEmpty()) MetadataSeparator()
        }
        if (genres.isNotEmpty()) {
            Text(
                text = genres.joinToString(" • "), color = Color.White,
                fontSize = if (compactLayout) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun seriesRatingStars(rating: Double): String {
    val normalized = if (rating > 5.0) {
        (rating / 2.0).coerceIn(0.0, 5.0)
    } else rating.coerceIn(0.0, 5.0)
    val whole = normalized.toInt()
    return "★".repeat(whole) + "☆".repeat(5 - whole)
}

private fun seriesRatingValue(rating: Double): String =
    if (rating % 1.0 == 0.0) rating.toInt().toString() else "%.1f".format(rating)

@Composable
private fun MetadataSeparator() {
    Text(
        text = "•",
        color = Color(0xFF69696E),
        fontSize = 13.sp
    )
}

private fun formatDuration(
    durationSeconds: Int?
): String? {
    if (
        durationSeconds == null ||
        durationSeconds <= 0
    ) {
        return null
    }

    val totalMinutes =
        durationSeconds / 60

    val hours =
        totalMinutes / 60

    val minutes =
        totalMinutes % 60

    return if (hours > 0) {
        if (minutes > 0) {
            "${hours}h ${minutes}min"
        } else {
            "${hours}h"
        }
    } else {
        "${totalMinutes}min"
    }
}

/**
 * ASSISTIR e TRAILER: proporcao compacta conforme a marcacao na TV.
 * Preserva o mesmo acabamento dourado, foco e acionamento de Filmes.
 * Apenas a largura e altura foram reduzidas para liberar mais espaco.
 */
@Composable
private fun ActionButton(
    text: String,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    br.com.flaviotorres.iptvpremium.ui.components.MediaActionButton(text, compactLayout, onClick)
}

/**
 * Temporada selecionada: realce dourado sobre fundo escuro.
 * Temporadas NAO selecionadas: fundo TRANSPARENTE, contorno e texto dourados.
 * Foco: aumenta ligeiramente o contorno, sem apagar a selecao persistente.
 */
@Composable
private fun SeasonButton(
    seasonNumber: Int,
    isSelected: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            // Exatamente as dimensões responsivas de ASSISTIR/TRAILER.
            .width(if (compactLayout) 118.dp else 130.dp)
            .height(if (compactLayout) 28.dp else 30.dp)
            .scale(if (isFocused) 1.035f else 1f)
            .clip(shape)
            .background(
                color = if (isSelected || isFocused) br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color() else Color.Transparent,
                shape = shape
            )
            .border(
                BorderStroke(
                    width = if (isSelected || isFocused) 2.dp else 1.3.dp,
                    brush = MetallicGoldBrush
                ),
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (seasonNumber == 0) "ESPECIAIS" else "TEMPORADA $seasonNumber",
            color = if (isFocused) Color.White else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Cada card mostra só a miniatura e, abaixo, "EPISÓDIO 01" etc.
 * A imagem preserva o contorno metálico por cima (mesma técnica de Filmes).
 */
@Composable
internal fun EpisodeCard(
    episodeNumber: Int,
    name: String,
    thumbnailUrl: String?,
    imageKey: String,
    seriesBackdrop: ImageBitmap?,
    tmdbEpisodeStillUrl: String?,
    compactLayout: Boolean,
    watchProgress: Float? = null,
    focusRequester: FocusRequester? = null,
    supplementalStillUrl: String? = null,
    onPhotoResolved: (List<String>, Boolean) -> Unit = { _, _ -> },
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    // Restauradas as dimensões originais aprovadas na TV.
    val cardWidth = if (compactLayout) 186.dp else 200.dp
    val cardHeight = if (compactLayout) 104.dp else 112.dp
    val imageWidthPx = with(density) { cardWidth.roundToPx().coerceAtLeast(1) }
    val imageHeightPx = with(density) { cardHeight.roundToPx().coerceAtLeast(1) }

    // Fotos do episodio permanecem prioritarias. O fundo e a mesma instancia
    // da ficha: aparece sem baixar a mesma imagem para cada card.
    val specificImages = remember(tmdbEpisodeStillUrl, thumbnailUrl, supplementalStillUrl) {
        br.com.flaviotorres.iptvpremium.data.repository.episodeReferenceImages(tmdbEpisodeStillUrl, thumbnailUrl, supplementalStillUrl)
    }
    val latestPhotoResolved by rememberUpdatedState(onPhotoResolved)
    val thumbnail: ImageBitmap? = key(specificImages, imageWidthPx, imageHeightPx, imageKey) {
        val loaded by produceState<ImageBitmap?>(
            initialValue = specificImages.firstNotNullOfOrNull { url ->
                MovieDetailsImageLoader.peekFromMemory(context, url, imageWidthPx, imageHeightPx)
            }
        ) {
            br.com.flaviotorres.iptvpremium.data.repository.loadEpisodeThumbnail(
                images = specificImages,
                image = { url -> MovieDetailsImageLoader.load(context, url, imageWidthPx, imageHeightPx,
                    "series_episode") },
                show = { value = it }
            )
            // Cancelled work never marks a newer reference as completed.
            latestPhotoResolved(specificImages, value != null)
        }
        loaded
    }
    val cardImage = thumbnail ?: seriesBackdrop
    val shape = RoundedCornerShape(if (compactLayout) 9.dp else 12.dp)
    Column(
        modifier = Modifier
            .width(cardWidth)
            .scale(if (isFocused) 1.035f else 1f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth().height(cardHeight)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        if (isFocused) { br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.colors() } else {
                            listOf(Color(0xFF242428), Color(0xFF151518))
                        }
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (cardImage != null) {
                Image(
                    bitmap = cardImage,
                    contentDescription = "Episódio $episodeNumber: $name",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Mesmo amarelo do progresso do player, com halo controlado.
            // A linha acompanha a posicao REAL dividida pela duracao do episodio.
            watchProgress?.takeIf { it > 0f }?.let { progress ->
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    val fraction = progress.coerceIn(0f, 1f)
                    val barHeight = 3.dp.toPx()
                    val y = size.height - barHeight
                    val playedWidth = size.width * fraction
                    drawRect(
                        Color(0x99090909),
                        topLeft = Offset(0f, y - 1.dp.toPx()),
                        size = Size(size.width, barHeight + 2.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.44f),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2f)
                    )
                    if (playedWidth > 0f) {
                        val glowHeight = 8.dp.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.55f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0x88FFC72C),
                                1f to Color.Transparent,
                                startY = size.height - glowHeight,
                                endY = size.height
                            ),
                            topLeft = Offset(0f, size.height - glowHeight),
                            size = Size(playedWidth, glowHeight),
                            cornerRadius = CornerRadius(glowHeight / 2f)
                        )
                        drawRoundRect(
                            color = Color(0xFFFFC72C),
                            topLeft = Offset(0f, y),
                            size = Size(playedWidth, barHeight),
                            cornerRadius = CornerRadius(barHeight / 2f)
                        )
                    }
                }
            }
            // Se nem o provedor nem o TMDB fornecerem arte, preserva fundo neutro.
            Box(
                modifier = Modifier.matchParentSize().border(
                    BorderStroke(
                        if (isFocused) 2.dp else 1.dp,
                        if (isFocused) MetallicGoldBrush else Brush.linearGradient(
                            listOf(Color(0xFF3D3D42), Color(0xFF3D3D42))
                        )
                    ),
                    shape
                )
            )
        }
        Spacer(Modifier.height(if (compactLayout) 4.dp else 7.dp))
        Text(
            text = "EPISÓDIO ${episodeNumber.toString().padStart(2, '0')}",
            color = if (isFocused) Color.White else Color.White,
            fontSize = if (compactLayout) 11.sp else 14.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Mesmo coração sem moldura quadrada de MovieDetailsScreen. */
@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val buttonSize = if (compactLayout) 36.dp else 40.dp
    Box(
        modifier = Modifier.size(buttonSize)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        FavoriteHeart(
            isFavorite = isFavorite,
            isFocused = isFocused,
            size = if (compactLayout) 26 else 29
        )
    }
}

@Composable
private fun FavoriteHeart(
    isFavorite: Boolean,
    isFocused: Boolean,
    size: Int
) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val heart = Path()
        val w = this.size.width
        val h = this.size.height
        heart.moveTo(w * 0.50f, h * 0.88f)
        heart.cubicTo(w * 0.42f, h * 0.80f, w * 0.08f, h * 0.58f, w * 0.08f, h * 0.32f)
        heart.cubicTo(w * 0.08f, h * 0.12f, w * 0.23f, h * 0.05f, w * 0.36f, h * 0.05f)
        heart.cubicTo(w * 0.44f, h * 0.05f, w * 0.49f, h * 0.10f, w * 0.50f, h * 0.17f)
        heart.cubicTo(w * 0.51f, h * 0.10f, w * 0.56f, h * 0.05f, w * 0.64f, h * 0.05f)
        heart.cubicTo(w * 0.77f, h * 0.05f, w * 0.92f, h * 0.12f, w * 0.92f, h * 0.32f)
        heart.cubicTo(w * 0.92f, h * 0.58f, w * 0.58f, h * 0.80f, w * 0.50f, h * 0.88f)
        heart.close()
        if (isFavorite) {
            drawPath(path = heart, brush = MetallicGoldBrush)
            if (isFocused) {
                drawPath(
                    path = heart, color = Color.White,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        } else if (isFocused) {
            drawPath(
                path = heart, brush = MetallicGoldBrush,
                style = Stroke(width = 2.6.dp.toPx())
            )
        } else {
            drawPath(
                path = heart, color = Color.White,
                style = Stroke(width = 1.9.dp.toPx())
            )
        }
    }
}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_MEDIA_ACTION_BUTTONS_V1

// IPTV_POLISH_LOADING_HOLD_V1

// Text retains the approved palette; blue darkening applies to rims.
private val MetallicGoldBrushText get() = Brush.linearGradient(
    colors = listOf(
        Color(0xFF8A6414),
        Color(0xFFD4AF37),
        Color(0xFFFFE991),
        Color(0xFFFFF4BD),
        Color(0xFFE3B83F),
        Color(0xFF9E761B)
    )
)
