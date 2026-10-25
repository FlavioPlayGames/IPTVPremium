package br.com.flaviotorres.iptvpremium.core.navigation
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource as painterResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.produceState
import androidx.paging.PagingSource
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.ui.movies.ActorFilmographyCredit
import java.text.Normalizer
import java.util.Locale
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.abs
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxWidth
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import br.com.flaviotorres.iptvpremium.core.player.PlaybackMedia
import br.com.flaviotorres.iptvpremium.core.player.PlaybackScreen
import br.com.flaviotorres.iptvpremium.core.player.toPlaybackMedia
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import br.com.flaviotorres.iptvpremium.data.cache.importer.M3uCacheImporter
import br.com.flaviotorres.iptvpremium.data.parser.DefaultM3uParser
import br.com.flaviotorres.iptvpremium.data.parser.DefaultXtreamParser
import br.com.flaviotorres.iptvpremium.data.remote.TmdbClientProvider
import br.com.flaviotorres.iptvpremium.data.remote.DefaultPlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.repository.DefaultPlaylistRepository
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodesRepository
import br.com.flaviotorres.iptvpremium.data.history.SharedLibraryStore
import br.com.flaviotorres.iptvpremium.data.history.ContentIdentity
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryStore
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.data.resolver.M3uXtreamResolver
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeInfo
import br.com.flaviotorres.iptvpremium.data.repository.SeriesSeasonInfo
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodesResult
import br.com.flaviotorres.iptvpremium.data.storage.PlaylistStorage
import br.com.flaviotorres.iptvpremium.data.sync.M3uSourceSynchronizer
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.domain.model.Series
import br.com.flaviotorres.iptvpremium.domain.model.RatingSource
import br.com.flaviotorres.iptvpremium.ui.home.HomeScreen
import br.com.flaviotorres.iptvpremium.ui.settings.IPTVSettingsScreen
import br.com.flaviotorres.iptvpremium.ui.settings.SubdlAccountScreen
import br.com.flaviotorres.iptvpremium.ui.home.AppCatalogLoadingPhase
import br.com.flaviotorres.iptvpremium.ui.home.AppCatalogLoadingState
import br.com.flaviotorres.iptvpremium.ui.home.AppStartupLoadingScreen
import br.com.flaviotorres.iptvpremium.ui.live.LiveTvScreen
import br.com.flaviotorres.iptvpremium.ui.live.LiveTvViewModel
import br.com.flaviotorres.iptvpremium.ui.movies.MovieCastMember
import br.com.flaviotorres.iptvpremium.ui.movies.ActorDetailsScreen
import br.com.flaviotorres.iptvpremium.ui.movies.PreparedMovieDetails
import br.com.flaviotorres.iptvpremium.ui.movies.MovieDetailsScreen
import br.com.flaviotorres.iptvpremium.ui.movies.MovieRatingSource
import br.com.flaviotorres.iptvpremium.ui.movies.MoviesScreen
import br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader
import br.com.flaviotorres.iptvpremium.ui.movies.MovieDetailsImageLoader
import br.com.flaviotorres.iptvpremium.ui.movies.MoviesViewModel
import br.com.flaviotorres.iptvpremium.ui.playlist.PlaylistListScreen
import br.com.flaviotorres.iptvpremium.ui.playlist.PlaylistSetupScreen
import br.com.flaviotorres.iptvpremium.ui.series.SeriesDetailsScreen
import br.com.flaviotorres.iptvpremium.ui.series.SeriesLoadingOverlay
import br.com.flaviotorres.iptvpremium.ui.series.SeriesScreen
import br.com.flaviotorres.iptvpremium.ui.series.SeriesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import br.com.flaviotorres.iptvpremium.data.cache.CatalogDetailsWarmupCache
import java.util.UUID
import org.json.JSONObject

/** Estado de resolucao da rota de reproducao; nao inclui URL nos logs. */
private sealed interface MoviePlaybackRouteState {
    data object Loading : MoviePlaybackRouteState
    data class Ready(val media: PlaybackMedia) : MoviePlaybackRouteState
    data class Error(val message: String) : MoviePlaybackRouteState
}

private const val PLAYLIST_LIST_ROUTE = "playlist_list"
private const val PLAYLIST_SETUP_ROUTE = "playlist_setup"
private const val NAVIGATION_PERF_TAG = "NavigationPerf"

// IPTV_AUTO_SYNC_V1
private const val AUTO_SYNC_TAG = "IptvAutoSync"
private const val AUTO_SYNC_XTREAM_MS = 2L * 60L * 60L * 1000L
private const val AUTO_SYNC_M3U_MS = 6L * 60L * 60L * 1000L
private const val AUTO_SYNC_RETRY_MS = 30L * 60L * 1000L
private const val AUTO_SYNC_CHECK_MS = 30L * 60L * 1000L
private const val MOVIE_PREPARATION_ROUTE = "movie_preparation/{movieId}"
private const val ACTOR_DETAILS_ROUTE = "actor_details/{personId}"
private const val MIN_MOVIE_PREPARATION_MS = 3_000L
private const val MAX_CAROUSEL_POSTERS = 20

// Estados da rota de detalhes que devem sobreviver a ida ao player.
private const val SERIES_OPENING_COMPLETED_KEY = "iptvpremium_series_opening_completed"
private const val SERIES_CURRENT_SEASON_KEY = "iptvpremium_series_selected_season"
private const val SERIES_LAST_EPISODE_KEY = "iptvpremium_series_last_played_episode"
private const val SERIES_RETURN_FROM_PLAYER_KEY = "iptvpremium_series_returning_from_player"


/** Metadados da SÉRIE escolhida, nunca do título de um episódio individual. */
private data class EpisodeSubtitleIdentity(
    val playlistId: String,
    val seriesId: String,
    val seriesTitle: String?,
    val tmdbId: String?,
    val year: Int?
)

/**
 * Normalização usada somente para conferir correspondências retornadas pelo TMDB.
 * Não adivinha IDs nem altera o catálogo do provedor.
 */
private fun subtitleSeriesTitleKey(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

private fun cleanSubtitleSeriesTitle(value: String): String = value.trim()
    .replace(Regex("(?i)\\s*\\[(?:4k|uhd|fhd|full\\s*hd|hd|sd|l)\\]"), " ")
    .replace(Regex("(?i)\\s*\\((?:19|20)\\d{2}\\)\\s*$"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

/**
 * Busca TMDB só no momento de ABRIR um episódio e somente quando a playlist
 * não trouxe ID. Não pesquisa o SubDL, não consulta outros episódios, não
 * mexe no Media3 e não registra URLs ou credenciais.
 */
private suspend fun resolveEpisodeSeriesTmdbId(
    seriesTitle: String,
    year: Int?
): String? {
    val cleaned = cleanSubtitleSeriesTitle(seriesTitle)
    val wanted = subtitleSeriesTitleKey(cleaned)
    if (wanted.length < 2) return null

    // Até duas traduções; interrompe assim que encontra uma correspondência.
    for (language in listOf("pt-BR", "en-US")) {
        val json = try {
            JSONObject(
                TmdbClientProvider.client.get(
                    path = "/search/tv",
                    queryParameters = mapOf(
                        "query" to cleaned,
                        "language" to language,
                        "include_adult" to "false"
                    )
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // O TMDB é opcional: uma falha nunca cancela o player.
            Log.d("SubdlIdentity", "TMDB indisponível: ${error.javaClass.simpleName}")
            continue
        }
        val results = json.optJSONArray("results") ?: continue
        for (i in 0 until minOf(results.length(), 20)) {
            val candidate = results.optJSONObject(i) ?: continue
            if (candidate.optBoolean("adult", false)) continue
            val matches = listOf(candidate.optString("name"), candidate.optString("original_name"))
                .any { subtitleSeriesTitleKey(it) == wanted }
            if (!matches) continue

            val candidateYear = candidate.optString("first_air_date").take(4).toIntOrNull()
            if (year != null && candidateYear != null &&
                kotlin.math.abs(candidateYear - year) > 1) continue

            val resolved = candidate.optLong("id", 0L)
            if (resolved > 0) return resolved.toString()
        }
    }
    return null
}

private sealed interface SeriesEpisodesLoadState {
    data object Loading : SeriesEpisodesLoadState
    data object Unavailable : SeriesEpisodesLoadState
    data object Error : SeriesEpisodesLoadState
    data class Ready(val result: SeriesEpisodesResult) : SeriesEpisodesLoadState
}

internal data class CinematicCarouselPoster(
    val movieId: String,
    val title: String,
    val posterUrl: String?
)

/**
 * Lista pequena, apenas a partir dos filmes carregados pelo Paging.
 * Reserva o filme clicado e elimina repeticoes tanto por ID como por URL.
 * Nao faz busca em massa, nao altera a ordem do catalogo original.
 */
private fun uniqueCarouselPosters(
    movieId: String,
    movies: List<br.com.flaviotorres.iptvpremium.domain.model.Movie>,
    maximum: Int = MAX_CAROUSEL_POSTERS
): List<CinematicCarouselPoster> {
    val chosen = movies.firstOrNull { it.id == movieId }
    val seenIds = HashSet<String>()
    val seenUrls = HashSet<String>()
    val result = ArrayList<CinematicCarouselPoster>(maximum)

    fun add(id: String, title: String, poster: String?, allowMissing: Boolean) {
        val cleanUrl = poster?.trim()?.takeIf {
            it.startsWith("https://", true) || it.startsWith("http://", true)
        }
        if (id.isBlank() || id in seenIds) return
        if (!allowMissing && cleanUrl == null) return
        if (cleanUrl != null && cleanUrl in seenUrls) return
        seenIds += id
        if (cleanUrl != null) seenUrls += cleanUrl
        result += CinematicCarouselPoster(id, title, cleanUrl)
    }

    add(movieId, chosen?.name ?: "FILME SELECIONADO", chosen?.posterUrl, true)
    val selectedIndex = movies.indexOfFirst { it.id == movieId }.coerceAtLeast(0)
    val neighbors = (movies.drop(selectedIndex + 1) + movies.take(selectedIndex))
    for (movie in neighbors) {
        if (result.size >= maximum) break
        add(movie.id, movie.name, movie.posterUrl, false)
    }
    return result
}

// Mesmo conjunto de reflexos do dourado metalico usado nos detalhes.
private val LoadingMetallicGold get() = Brush.linearGradient(
    colorStops = arrayOf(
        0.00f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF78530F),
        0.18f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFD4AF37),
        0.37f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFE991),
        0.52f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFF7CE),
        0.70f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFE3B83F),
        1.00f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF916914)
    )
)

private val LoadingCinzel = FontFamily(
    Font(R.font.cinzel_semibold, weight = FontWeight.SemiBold)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val playlistStorage =
        remember(context) {
            PlaylistStorage(
                context.applicationContext
            )
        }

    val storedSelection by playlistStorage.selection.collectAsState(initial = null)
    val storedPlaylists = storedSelection?.playlists.orEmpty()
    val storedActivePlaylistId = storedSelection?.activeId
    val portalLoadRequest = storedSelection?.loadRequest
    val portalSyncRequests by br.com.flaviotorres.iptvpremium.core.device.PortalListPreparation.activeRequests.collectAsState()

    /*
     * Cliente HTTP compartilhado.
     *
     * O Xtream continua usando get().
     * A M3U passa a usar readLines() por meio do M3uCacheImporter.
     */
    val playlistHttpClient =
        remember {
            DefaultPlaylistHttpClient()
        }

    val seriesEpisodesRepository = remember(playlistHttpClient) {
        SeriesEpisodesRepository(playlistHttpClient, diskCache =
            br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeDiskCache(
                java.io.File(context.cacheDir, "series_episode_details"),
                storage = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)))
    }

    /*
     * Banco local permanente do catálogo.
     */
    val playlistCacheDatabase =
        remember(context) {
            PlaylistCacheDatabase.getInstance(
                context.applicationContext
            )
        }

    val playlistCacheDao =
        remember(playlistCacheDatabase) {
            playlistCacheDatabase.playlistCacheDao()
        }

    val playlistCacheGenerationManager =
        remember(playlistCacheDao) {
            PlaylistCacheGenerationManager(
                cacheDao = playlistCacheDao
            )
        }

    val m3uCacheImporter =
        remember(
            playlistHttpClient,
            playlistCacheDao,
            playlistCacheGenerationManager
        ) {
            M3uCacheImporter(
                httpClient = playlistHttpClient,
                cacheDao = playlistCacheDao,
                generationManager =
                    playlistCacheGenerationManager
            )
        }

    /*
     * NOVA FONTE DO CATÁLOGO LOCAL.
     *
     * Canais, filmes e séries armazenados no Room passam a ser lidos
     * por Paging, sem reconstruir o catálogo inteiro em memória.
     */
    val playlistCatalogRepository =
        remember(playlistCacheDao) {
            PlaylistCatalogRepository(
                cacheDao = playlistCacheDao,
                context = context.applicationContext
            )
        }

    /*
     * Factory do ViewModel de Canais.
     * O ViewModel recebe o repository local sem colocar dependências
     * de banco dentro da própria tela.
     */
    val liveTvViewModelFactory =
        remember(playlistCatalogRepository) {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    if (
                        modelClass.isAssignableFrom(
                            LiveTvViewModel::class.java
                        )
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        return LiveTvViewModel(
                            repository =
                                playlistCatalogRepository
                        ) as T
                    }

                    throw IllegalArgumentException(
                        "ViewModel desconhecido: ${modelClass.name}"
                    )
                }
            }
        }

    /*
     * Factory do ViewModel de Filmes.
     *
     * Usa o mesmo PlaylistCatalogRepository do catálogo local,
     * mantendo Room + Paging fora da tela.
     */
    val moviesViewModelFactory =
        remember(playlistCatalogRepository) {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    if (
                        modelClass.isAssignableFrom(
                            MoviesViewModel::class.java
                        )
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        return MoviesViewModel(
                            repository =
                                playlistCatalogRepository
                        ) as T
                    }

                    throw IllegalArgumentException(
                        "ViewModel desconhecido: ${modelClass.name}"
                    )
                }
            }
        }

    /*
     * Factory para Series. O catalogo inteiro continua no Room/Paging;
     * nao reutilizamos o ViewModel dos Filmes nem seus filtros.
     */
    val seriesViewModelFactory = remember(playlistCatalogRepository) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SeriesViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return SeriesViewModel(playlistCatalogRepository) as T
                }
                throw IllegalArgumentException(
                    "ViewModel desconhecido: ${modelClass.name}"
                )
            }
        }
    }

    val playlistRepository =
        remember(
            playlistHttpClient,
            m3uCacheImporter
        ) {
            DefaultPlaylistRepository(
                httpClient =
                    playlistHttpClient,
                m3uParser =
                    DefaultM3uParser(),
                xtreamParser =
                    DefaultXtreamParser(),
                m3uCacheImporter = m3uCacheImporter,
                movieLookup = { playlist, id -> playlistCatalogRepository.getMovieById(playlist, id) }
            )
        }

    /*
     * SINCRONIZAÇÃO UNIFICADA:
     *
     * - M3U compatível com Xtream usa a API Xtream;
     * - M3U genérica usa o importador nativo como fallback;
     * - Xtream Codes direto também é persistido no Room.
     *
     * Todos os caminhos terminam no mesmo catálogo local,
     * preservando Room + Paging para as telas.
     */
    val m3uSourceSynchronizer =
        remember(
            playlistRepository,
            m3uCacheImporter,
            playlistCacheDao,
            playlistCacheGenerationManager
        ) {
            M3uSourceSynchronizer(
                playlistRepository =
                    playlistRepository,
                m3uCacheImporter =
                    m3uCacheImporter,
                cacheDao =
                    playlistCacheDao,
                generationManager =
                    playlistCacheGenerationManager
            )
        }

    // Favorites are published after catalogue readiness; do not read/search them during import.
    // IDs dos favoritos reais do provedor, separados por playlist e persistidos.
    // A colecao armazenada nunca e exposta diretamente: sempre copiamos o Set.
    val favoritePreferences = remember(context) {
        context.applicationContext.getSharedPreferences(
            "iptvpremium_favorites_v1",
            Context.MODE_PRIVATE
        )
    }
    val movieFavoritesKey = remember(storedActivePlaylistId) {
        "movies_${storedActivePlaylistId.orEmpty()}"
    }
    var favoriteMovieIds by remember(storedActivePlaylistId) {
        mutableStateOf(emptySet<String>())
    }
    // Favoritos de Series: mesmo armazenamento persistente por playlist dos Filmes.
    val seriesFavoritesKey = remember(storedActivePlaylistId) {
        "series_${storedActivePlaylistId.orEmpty()}"
    }
    var favoriteSeriesIds by remember(storedActivePlaylistId) {
        mutableStateOf(emptySet<String>())
    }
    // Current-list favourites are local state, not a cross-list history import.
    LaunchedEffect(storedActivePlaylistId) {
        val playlist=storedActivePlaylistId?:return@LaunchedEffect
        val own=withContext(Dispatchers.IO) {
            favoritePreferences.getStringSet("movies_$playlist",emptySet()).orEmpty().toSet() to
                favoritePreferences.getStringSet("series_$playlist",emptySet()).orEmpty().toSet()
        }
        favoriteMovieIds=own.first;favoriteSeriesIds=own.second
    }
    DisposableEffect(context.applicationContext) {
        val app = context.applicationContext
        br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.forContext(app)
        val callback = object : android.content.ComponentCallbacks2 {
            override fun onConfigurationChanged(config: android.content.res.Configuration) = Unit
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onLowMemory() { onTrimMemory(80) }
            override fun onTrimMemory(level: Int) {
                br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.signal(level)
                if (level >= 5 && level != 20)
                    br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value = true
                val fraction = br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.retainedFraction(level)
                MoviePosterLoader.trimMemory(fraction)
                MovieDetailsImageLoader.trimMemory(fraction)
                FilmstripPosterCache.trimMemory(fraction)
            }
        }
        app.registerComponentCallbacks(callback)
        onDispose { app.unregisterComponentCallbacks(callback) }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            var lastTrim = 0L
            while (true) {
                delay(5_000L)
                val previous = br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value
                val pressured = br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.pressured(context, previous)
                br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value = pressured
                val now = SystemClock.elapsedRealtime()
                if (pressured && now - lastTrim >= 30_000L) {
                    MoviePosterLoader.trimMemory()
                    MovieDetailsImageLoader.trimMemory()
                    FilmstripPosterCache.trimMemory()
                    lastTrim = now
                }
                if (br.com.flaviotorres.iptvpremium.BuildConfig.DEBUG) {
                    val budget = br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.forContext(context)
                    Log.d("CatalogMemory", "cacheBudgetMb=${budget.totalMb} antecipacaoPausada=$pressured")
                }
            }
        }
    }
    val sharedLibrary = remember(context) { SharedLibraryStore.get(context) }
    val sharedLibraryRevision by sharedLibrary.changes.collectAsState()
    var sharedReadyRevision by remember { mutableIntStateOf(0) }
    var sharedSelectionRevision by remember { mutableIntStateOf(0) }
    fun toggleSharedFavorite(kind: String, id: String) {
        val playlist = storedActivePlaylistId ?: return
        val selected = if (kind == "movies") id !in favoriteMovieIds else id !in favoriteSeriesIds
        if (kind == "movies") favoriteMovieIds = if (selected) favoriteMovieIds + id else favoriteMovieIds - id
        else favoriteSeriesIds = if (selected) favoriteSeriesIds + id else favoriteSeriesIds - id
        favoritePreferences.edit().putStringSet("${kind}_$playlist",
            (if (kind == "movies") favoriteMovieIds else favoriteSeriesIds).toHashSet()).apply()
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) { sharedLibrary.setFavorite(playlistCacheDao, playlist, kind, id, selected) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { Toast.makeText(context, "Favorito salvo nesta lista; não foi possível compartilhar agora.", Toast.LENGTH_LONG).show() }
        }
    }



    // Dados ja presentes na pagina do Paging para os detalhes da serie clicada.
    // O ViewModel/Room continua sendo a fonte do catalogo; isto NAO carrega
    // milhares de series nem altera a identidade por ID do provedor.
    var selectedSeriesForDetails by remember {
        mutableStateOf<Series?>(null)
    }
    // Conserva somente os episodios da ULTIMA serie aberta em memoria.
    // Ao voltar do player, a tela reaparece ja pronta, sem refazer a consulta
    // nem mostrar outra tela de carregamento. Nenhuma URL e salva em disco.
    var lastSeriesDetailsResult by remember {
        mutableStateOf<Triple<String, String, SeriesEpisodesResult>?>(null)
    }

    // Guarda apenas IDs de episodios entre a tela da serie e o player.
    // A URL real nunca e inserida na rota de navegacao.
    var autoNextEpisodeId by remember { mutableStateOf<String?>(null) }
    var selectedEpisodeToPlay by remember { mutableStateOf<SeriesEpisodeInfo?>(null) }
    var selectedEpisodeSeriesId by remember { mutableStateOf<String?>(null) }
    // Um único episódio selecionado; metadados da série capturados no clique.
    var selectedEpisodeSubtitleIdentity by remember {
        mutableStateOf<EpisodeSubtitleIdentity?>(null)
    }
    // IDs TMDB validados e identificados apenas durante a reprodução.
    // Chave = playlist + série, para não misturar provedores.
    val episodeTmdbIds = remember { mutableMapOf<String, String>() }
    val seriesResumeStore = remember(context) {
        SeriesAssistirHistory(context.applicationContext)
    }
    // Historico minimo dos filmes: mesmo principio validado para episodios.
    // Nao persiste endereco de video, senha nem qualquer dado da playlist.
    val movieResumeStore = remember(context) {
        MovieAssistirHistory(context.applicationContext)
    }

    // Grava alteracoes da grade, detalhes e janelas dos atores usando a MESMA
    // colecao de IDs reais do filme. Series usam uma chave separada.


    /*
     * O pacote preparado fica no nivel do AppNavigation para ser entregue
     * do ViewModel da grade ao ViewModel independente da rota de detalhes.
     * Nao contem bitmaps, apenas dados e indicadores de imagem em cache.
     */
    var preparedMovieDetails by remember {
        mutableStateOf<PreparedMovieDetails?>(null)
    }

    // URL da arte do filme ja resolvida nos detalhes. Guardamos somente a
    // referencia para o player, associada a playlist e ID; nunca um Bitmap.
    // Assim, a abertura do video nao precisa refazer consultas de metadados.
    // The exact panoramic artwork visible in details, scoped to this movie and playlist.
    var movieDurationForPlayback by remember {
        mutableStateOf<Triple<String, String, Long>?>(null)
    }
    var movieBackdropForPlayback by remember {
        mutableStateOf<Triple<String, String, String>?>(null)
    }
    var movieTitleLogoForPlayback by remember {
        mutableStateOf<Triple<String, String, String>?>(null)
    }

    // Nome e foto ja conhecidos aparecem imediatamente ao abrir a pessoa.
    // O ID na rota permite recuperar os detalhes apos recriar a Activity.
    var selectedActorForDetails by remember {
        mutableStateOf<MovieCastMember?>(null)
    }

    // Ate 20 posters unicos da pagina ja carregada (sem buscar o catalogo inteiro).
    var loadingCarouselCandidates by remember { mutableStateOf<List<CinematicCarouselPoster>>(emptyList()) }
    var loadingCarouselPosters by remember {
        mutableStateOf<List<CinematicCarouselPoster>>(emptyList())
    }

    LaunchedEffect(loadingCarouselPosters) {
        MoviePosterLoader.protectHomePosters(loadingCarouselPosters.map { it.posterUrl })
    }

    /*
     * LISTAS CADASTRADAS + LISTA ATIVA
     *
     * Agora vêm do DataStore e permanecem salvas
     * mesmo depois de fechar ou reiniciar o app.
     */
    val playlists =
        storedPlaylists

    val activePlaylistId =
        storedActivePlaylistId

    val onDemandSourceSnapshot = rememberUpdatedState(playlists)
    val onDemandCatalog = remember(playlistCacheDao, playlistRepository) {
        br.com.flaviotorres.iptvpremium.data.sync.OnDemandXtreamCatalog(
            playlistCacheDao,playlistCacheGenerationManager,
            playlistRepository as br.com.flaviotorres.iptvpremium.data.sync.ProgressiveXtreamRepository)
    }
    androidx.compose.runtime.SideEffect {
        playlistCatalogRepository.ensureOnDemand = { playlist, type, category, full ->
            val source = onDemandSourceSnapshot.value.firstOrNull { it.id == playlist }
                ?: error("A lista não está mais disponível.")
            onDemandCatalog.ensure(source,type,category,full)
        }
    }


    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route.orEmpty()
            if (route == "home") {
                br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.request(context.applicationContext)
            }
            br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.setBusy(
                route.startsWith("movie_preparation") || route.startsWith("movie_details") ||
                    route.startsWith("series_details") || route.startsWith("playback"))
            if (route == "home" || route.startsWith("settings") || route.startsWith("playlist")) {
                br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = false
            } else {
                val kids = route == "kids" || entry.savedStateHandle.get<Boolean>("kids_catalog") == true ||
                    br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value
                if (kids) entry.savedStateHandle["kids_catalog"] = true
                br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = kids
            }
        }
    }

    val parentalAccessState by br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.accessFlow.collectAsState(
        initial = br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access())
    val kidsStyle = br.com.flaviotorres.iptvpremium.ui.settings.rememberSettingsPreferences().kidsStyle()
    androidx.compose.runtime.SideEffect {
        br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.apply(parentalAccessState.kidsCatalog, kidsStyle)
    }
    DisposableEffect(Unit) { onDispose { br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.apply(false, kidsStyle) } }
    val parentalEpoch by br.com.flaviotorres.iptvpremium.ui.settings.ParentalSession.epoch.collectAsState()
    var parentalCategoryRequest by remember(parentalEpoch, activePlaylistId) {
        mutableStateOf<br.com.flaviotorres.iptvpremium.ui.settings.ParentalCategoryRequest?>(null)
    }
    fun requestParentalCategory(type: String, id: String?, title: String, proceed: () -> Unit) {
        val playlist = activePlaylistId.orEmpty()
        if (id == br.com.flaviotorres.iptvpremium.ui.kids.CatalogSections.SUBTITLED) { proceed(); return }
        if (id != null && br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access()
                .blocked(playlist, type, id, categoryName = title)) {
            parentalCategoryRequest = br.com.flaviotorres.iptvpremium.ui.settings.ParentalCategoryRequest(playlist, type, id, title.removePrefix("🔒 "), proceed)
        } else proceed()
    }

    // O historico da categoria Continuar Assistindo e independente do cache.
    // IDs, capas e posicoes sao salvos; URLs e credenciais nunca sao persistidos.
    var catalogLoadingState by remember {
        mutableStateOf<AppCatalogLoadingState?>(AppCatalogLoadingState())
    }
    val watchHistoryStore = remember(context) {
        WatchHistoryStore(context.applicationContext)
    }
    var historyRefreshSerial by remember { mutableIntStateOf(0) }
    val settingsHistoryRevision by WatchHistoryStore.changes.collectAsState()
    var sharedSelectionInFlight by remember { mutableStateOf<String?>(null) }
    val sharedImportMutex = remember { kotlinx.coroutines.sync.Mutex() }
    val historySyncSession = remember { br.com.flaviotorres.iptvpremium.data.history.HistorySyncSession.process }
    val historySyncRun by historySyncSession.state.collectAsState()
    var historyExecutorJob by remember {mutableStateOf<Job?>(null)}
    val enrichmentEntry by navController.currentBackStackEntryAsState()
    val enrichmentRoute = enrichmentEntry?.destination?.route
    val userIdle by br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.idle.collectAsState()
    val selectedMediaBusy by br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.collectAsState()
    DisposableEffect(Unit) { onDispose { br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.setBusy(false) } }
    suspend fun refreshSharedPlaylist(source: PlaylistSource, kind: String, force: Boolean, includeNetwork: Boolean = true) {
        val sharedGeneration = withContext(Dispatchers.IO) { playlistCacheDao.getActiveGeneration(source.id)?.generationId }
        if(sharedGeneration != null && withContext(Dispatchers.IO) {
            playlistCacheDao.hasCatalogMarker(source.id,sharedGeneration,"__LAZY_${kind.uppercase()}__")
        }) return

        if (br.com.flaviotorres.iptvpremium.BuildConfig.DEBUG &&
            br.com.flaviotorres.iptvpremium.BuildConfig.DEBUG_PAUSE_SHARED_LIBRARY) return
        val importStartedAt = SystemClock.elapsedRealtime()
        sharedImportMutex.lock()
        try {
            withContext(Dispatchers.IO) {
                val origin=historySyncRun?.takeIf {it.playlist==source.id && it.phase!=br.com.flaviotorres.iptvpremium.data.history.HistorySyncSession.Phase.COMPLETED}?.origin
                    ?:sharedLibrary.historySource()?:source.id
                val history = watchHistoryStore.allLocal().filter {it.playlistId==source.id || it.playlistId==origin}
                suspend fun reconcile(forceScan: Boolean = false) {
                    sharedLibrary.reconcile(playlistCacheDao, source.id, history, force = forceScan, reuseUnchanged = true, kind = kind, origin=origin)
                }
                reconcile(force)
                if (!includeNetwork) return@withContext
                br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.awaitIdle()
                val resolver = br.com.flaviotorres.iptvpremium.data.repository.TitleAliasResolver { path, query ->
                    br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.awaitIdle()
                    TmdbClientProvider.client.get(path, query)
                }
                if (sharedLibrary.enrichAlternateTitles(source.id, kind) { resolver.resolve(it) }) reconcile()
                if (kind == "series" && sharedLibrary.enrichMatchingSeries(source.id) { resolver.resolve(it) }) reconcile()
                if (kind == "movies" && sharedLibrary.enrichMatchingMovies(source.id) { request ->
                    br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.awaitIdle()
                    val origin = (if (request.playlist == source.id) source else playlists.firstOrNull { it.id == request.playlist }) ?: return@enrichMatchingMovies null
                    val generation = playlistCacheDao.getActiveGeneration(origin.id)?.generationId ?: return@enrichMatchingMovies null
                    val details = CatalogDetailsWarmupCache.getMovie(origin.id, generation, request.id)
                        ?: playlistRepository.loadMovieIdentityDetails(origin, request.id)
                    details?.let { ContentIdentity("movies", request.title, it.year ?: request.year, it.imdbId, it.tmdbId) }
                }) reconcile()
                if (kind == "series") sharedLibrary.importEpisodeLinks(source.id, refresh = force) { seriesId ->
                    br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.awaitIdle()
                    loadSeriesForOpening(playlistCacheDao, seriesEpisodesRepository, source, seriesId)?.episodes.orEmpty()
                }
            }
            Log.d("SharedLibrary", "${if (includeNetwork) "historico_sincronizado" else "historico_local"} secao=$kind selecao=$force duracaoMs=${SystemClock.elapsedRealtime() - importStartedAt}")
        } finally { sharedImportMutex.unlock() }
    }
    suspend fun publishSharedPlaylist(playlist: String) {
        val favorites=withContext(Dispatchers.IO) {
            sharedLibrary.favorites(playlist,"movies",favoritePreferences.getStringSet("movies_$playlist",emptySet()).orEmpty().toSet()) to
                sharedLibrary.favorites(playlist,"series",favoritePreferences.getStringSet("series_$playlist",emptySet()).orEmpty().toSet())
        }
        withContext(Dispatchers.Main) {
            if(activePlaylistId==playlist) {
                favoriteMovieIds=favorites.first;favoriteSeriesIds=favorites.second;sharedReadyRevision++
            }
        }
    }
    LaunchedEffect(onDemandCatalog) {
        onDemandCatalog.completed.collect { (playlist,kind) ->
            val source = onDemandSourceSnapshot.value.firstOrNull { it.id == playlist } ?: return@collect
            try {
                refreshSharedPlaylist(source,kind,force=true,includeNetwork=false)
                publishSharedPlaylist(playlist)
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(_: Exception) { Log.w("SharedLibrary","catalogo_concluido reconciliacao_adiada=true") }
        }
    }
    // Only actual local progress edits reconcile saved data. Opening a route/idle never starts history work.
    LaunchedEffect(activePlaylistId) {
        snapshotFlow { historyRefreshSerial to settingsHistoryRevision }.drop(1).collectLatest {
            if (catalogLoadingState != null || sharedSelectionInFlight != null) return@collectLatest
            val source = playlists.firstOrNull { it.id == activePlaylistId } ?: return@collectLatest
            try {
                for (kind in listOf("movies", "series")) {
                    refreshSharedPlaylist(source, kind, force = false, includeNetwork = false)
                }
                publishSharedPlaylist(source.id)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { Log.w("SharedLibrary", "Atualizacao local adiada; dados preservados") }
        }
    }

    var watchHistoryEntries by remember(activePlaylistId, historyRefreshSerial, settingsHistoryRevision, sharedReadyRevision, parentalAccessState) {
        mutableStateOf<List<WatchHistoryEntry>>(emptyList())
    }
    LaunchedEffect(activePlaylistId, historyRefreshSerial, settingsHistoryRevision, sharedReadyRevision, parentalAccessState,catalogLoadingState,sharedSelectionInFlight) {
        if(catalogLoadingState!=null || sharedSelectionInFlight!=null)return@LaunchedEffect
        val playlist = activePlaylistId ?: return@LaunchedEffect
        watchHistoryEntries = withContext(Dispatchers.IO) {
            playlistCatalogRepository.filterHistoryForCatalog(playlist, watchHistoryStore.all(playlist))
        }
    }

    suspend fun importLegacyHistory(source: PlaylistSource, kind: String) {
        val playlistId = source.id
        val imported = withContext(Dispatchers.IO) {
            var count = 0
            if (kind == "movies") watchHistoryStore.legacyMoviePositions(playlistId).take(40)
                .forEach { (movieId, position) ->
                    if (watchHistoryStore.movie(playlistId, movieId) != null) return@forEach
                    val movie = try {
                        playlistCatalogRepository.getMovieById(playlistId, movieId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) { null } ?: return@forEach
                    val duration = movie.durationSeconds?.times(1_000L)
                    if (watchHistoryStore.importLegacy(
                            WatchHistoryEntry(
                                playlistId = playlistId,
                                contentType = PlaybackContentType.MOVIE,
                                contentId = movie.id,
                                title = movie.name,
                                posterUrl = movie.posterUrl,
                                positionMs = position,
                                durationMs = duration,
                                updatedAtMs = System.currentTimeMillis()
                            )
                        )) count++
                }
            if (kind == "series") watchHistoryStore.legacySeriesPositions(playlistId).take(25)
                .forEach { (seriesId, episodeId, position) ->
                    if (watchHistoryStore.lastEpisode(playlistId, seriesId) != null) return@forEach
                    val result = try {
                        withTimeoutOrNull(3_000L) {
                            seriesEpisodesRepository.load(source, seriesId)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) { null } ?: return@forEach
                    val episode = result.episodes.firstOrNull { it.id == episodeId }
                        ?: return@forEach
                    if (watchHistoryStore.importLegacy(
                            WatchHistoryEntry(
                                playlistId = playlistId,
                                contentType = PlaybackContentType.EPISODE,
                                contentId = episode.id,
                                seriesId = seriesId,
                                title = episode.title,
                                posterUrl = episode.thumbnailUrl,
                                seriesTitle = result.title,
                                seriesPosterUrl = result.posterUrl,
                                seasonNumber = episode.seasonNumber,
                                episodeNumber = episode.episodeNumber,
                                positionMs = position,
                                durationMs = episode.durationSeconds?.toLong()?.times(1_000L),
                                updatedAtMs = System.currentTimeMillis()
                            )
                        )) count++
                }
            count
        }
        if (imported > 0) historyRefreshSerial++
    }


    // One import per process/list selection. Navigation and lifecycle resume are not keys.
    LaunchedEffect(activePlaylistId, sharedSelectionInFlight, playlists, historySyncRun?.id,catalogLoadingState) {
        if (catalogLoadingState!=null || sharedSelectionInFlight != null) return@LaunchedEffect
        if (br.com.flaviotorres.iptvpremium.BuildConfig.DEBUG &&
            br.com.flaviotorres.iptvpremium.BuildConfig.DEBUG_PAUSE_SHARED_LIBRARY) return@LaunchedEffect
        val source = playlists.firstOrNull { it.id == activePlaylistId } ?: return@LaunchedEffect
        val run = historySyncRun?.takeIf { it.playlist == source.id } ?: return@LaunchedEffect
        kotlinx.coroutines.flow.combine(
            br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.idle,
            br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy,
            br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.busy
        ) { idle, selected, importing -> idle && !selected && !importing }.distinctUntilChanged().collectLatest { allowed ->
            if(!allowed) return@collectLatest
            if (!historySyncSession.awaitStart(run.id)) return@collectLatest
            val executor=currentCoroutineContext()[Job]
            historyExecutorJob=executor
            try {
                withContext(Dispatchers.IO) {
                    playlistCacheDao.observeActiveGenerationId(source.id).first { it != null }
                    // Partial catalogues cannot authoritatively remap shared favourites/history.
                    val historyGeneration = playlistCacheDao.getActiveGeneration(source.id)?.generationId
                    if(historyGeneration != null) playlistCacheDao.observeLazyCatalogPending(source.id,historyGeneration).first { !it }
                    for (kind in listOf("movies", "series")) {
                        currentCoroutineContext().ensureActive()
                        importLegacyHistory(source, kind)
                        refreshSharedPlaylist(source, kind, force = false, includeNetwork = false)
                        currentCoroutineContext().ensureActive()
                        withContext(Dispatchers.Main) { publishSharedPlaylist(source.id) }
                        refreshSharedPlaylist(source, kind, force = false)
                        currentCoroutineContext().ensureActive()
                        withContext(Dispatchers.Main) { publishSharedPlaylist(source.id) }
                    }
                    currentCoroutineContext().ensureActive()
                    historySyncSession.complete(run.id) {sharedLibrary.checkpointCompletedHistory(source.id,watchHistoryStore)}
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                historySyncSession.fail(run.id)
                Log.w("SharedLibrary", "historico_sessao=incompleto dados_preservados=true")
            } finally {
                if(historyExecutorJob===executor)historyExecutorJob=null
                historySyncSession.release(run.id)
            }
        }
    }
    val startupPreferencesResolved = storedSelection != null
    val manualCatalogRefresh = remember(context) {
        br.com.flaviotorres.iptvpremium.core.storage.ActivePlaylistRefresh(context)
    }
    var catalogStartupRetry by remember { mutableStateOf(0) }
    var startupCatalogJob by remember { mutableStateOf<Job?>(null) }
    var manualCatalogJob by remember { mutableStateOf<Job?>(null) }
    var catalogLoadingDismissed by remember { mutableStateOf(false) }
    var dismissedCatalogPlaylistId by remember { mutableStateOf<String?>(null) }
    var manualPlaylistOperationId by remember { mutableStateOf<String?>(null) }

    val automaticSyncPreferences = remember(context) {
        context.applicationContext.getSharedPreferences(
            "iptvpremium_automatic_sync_v1",
            Context.MODE_PRIVATE
        )
    }

    var catalogueHomeReadyAt by remember { mutableStateOf<Long?>(null) }
    var automaticSyncJob by remember {
        mutableStateOf<Job?>(null)
    }

    var automaticCheckSerial by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(startupPreferencesResolved) {
        if (startupPreferencesResolved) br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.checkpoint("preferencias_resolvidas")
    }

    // O catalogo ja foi importado e ativado antes desta barreira.
    // Capas sao carregadas pelas telas, sem downloads antecipados na abertura
    // nem uma fila de pre-carregamento apos a sincronizacao.
    suspend fun prepareCatalogBeforeHome(source: PlaylistSource, isRefresh: Boolean, showHistoryImport: Boolean = false) {
        playlistCacheDao.observeActiveGenerationId(source.id)
            .first()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("O catalogo ativo nao esta disponivel.")

        br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.activity()
        // History is scheduled, never awaited before Home or during list selection.
        val historyOrigin = sharedLibrary.historySource() ?: historySyncRun?.origin ?: activePlaylistId ?: source.id
        if (isRefresh || showHistoryImport) {
            historySyncSession.request(source.id, announce = true, restart = true, origin = historyOrigin)
            Log.i("HistoryTriggerPerf", "event=${if (isRefresh) "import_or_update" else "list_switch"}")
        }
        val savedFavorites = withContext(Dispatchers.IO) {
            sharedLibrary.favorites(source.id, "movies", favoritePreferences.getStringSet("movies_${source.id}", emptySet()).orEmpty().toSet()) to
                sharedLibrary.favorites(source.id, "series", favoritePreferences.getStringSet("series_${source.id}", emptySet()).orEmpty().toSet())
        }
        favoriteMovieIds = savedFavorites.first
        favoriteSeriesIds = savedFavorites.second
    }

    // Nunca reutilizar os dados preparados ao mudar a lista ativa.
    LaunchedEffect(activePlaylistId) {
        preparedMovieDetails = null
        movieTitleLogoForPlayback = null
        movieBackdropForPlayback = null
        movieDurationForPlayback = null
        loadingCarouselPosters = emptyList()
        loadingCarouselCandidates = emptyList()
        selectedSeriesForDetails = null
        selectedEpisodeSubtitleIdentity = null
        episodeTmdbIds.clear()
        if (
            navController.currentDestination?.route ==
            MOVIE_PREPARATION_ROUTE
        ) {
            navController.popBackStack()
        }
    }

    /*
     * CONTEÚDO REAL DA LISTA ATIVA
     *
     * Nesta etapa já guardamos tudo que o repositório conseguiu
     * carregar. No próximo passo ligaremos este conteúdo às telas
     * de Canais, Filmes e Séries.
     */
    var activePlaylistContent by remember {
        mutableStateOf<PlaylistContent?>(null)
    }

    /*
     * Guarda de qual playlist veio o conteúdo atualmente carregado.
     * Isso evita baixar novamente a mesma lista sem necessidade.
     */
    var activePlaylistContentSourceId by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * ERRO DA ÚLTIMA TENTATIVA DE CONEXÃO.
     *
     * Ainda não mostramos este texto na interface porque vamos
     * cuidar do estado visual no PlaylistSetupScreen no próximo
     * passo. Ele já fica preparado aqui para isso.
     */
    var playlistConnectionError by remember {
        mutableStateOf<String?>(null)
    }

    var isPlaylistConnecting by remember {
        mutableStateOf(false)
    }

    /*
     * Identifica qual lista está sendo carregada/atualizada na tela
     * MINHAS LISTAS. Enquanto este ID estiver preenchido, a própria
     * PlaylistListScreen mostra o indicador animado.
     */
    var loadingPlaylistId by remember {
        mutableStateOf<String?>(null)
    }

    fun leaveCatalogImport(message: String? = null) {
        // Release the interface immediately; cancellation/Room cleanup continues safely.
        dismissedCatalogPlaylistId = activePlaylistId
        catalogLoadingDismissed = true
        startupCatalogJob?.cancel()
        manualCatalogJob?.cancel()
        catalogLoadingState = null
        isPlaylistConnecting = false
        loadingPlaylistId = null
        manualPlaylistOperationId = null
        sharedSelectionInFlight = null
        message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        val pendingPortalRequest = portalLoadRequest
        if (pendingPortalRequest != null) coroutineScope.launch {
            playlistStorage.finishPortalLoad(pendingPortalRequest)
        }
        navController.navigate(AppDestination.Home.route) {
            popUpTo(AppDestination.Home.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    /*
     * LIMPEZA DE GERAÇÕES ANTIGAS
     *
     * Mantemos no máximo uma limpeza STALE agendada/em execução.
     *
     * Antes de iniciar uma nova sincronização M3U, cancelamos essa limpeza
     * para não disputar escrita no Room/SQLite com a importação.
     *
     * Depois que a nova geração já virou ACTIVE, aguardamos alguns segundos
     * antes de começar a limpeza. Isso deixa a navegação e a interface
     * estabilizarem primeiro.
     */
    fun scheduleStaleGenerationCleanup(
        @Suppress("UNUSED_PARAMETER") playlistId: String
    ) {
        br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.request(context.applicationContext)
    }

    /*
     * RECARREGA AUTOMATICAMENTE A LISTA ATIVA.
     *
     * Funciona em dois casos:
     * 1. ao abrir novamente o aplicativo;
     * 2. ao selecionar outra lista em MINHAS LISTAS.
     *
     * O DataStore guarda a configuração da playlist.
     * M3U e Xtream Codes direto usam o catálogo persistido no Room.
     */
    LaunchedEffect(
        activePlaylistId,
        playlists,
        startupPreferencesResolved,
        catalogStartupRetry,
        portalLoadRequest
    ) {
        if (!startupPreferencesResolved) return@LaunchedEffect
        val playlistId = activePlaylistId

        if (playlistId == null) {
            activePlaylistContent = null
            activePlaylistContentSourceId = null
            // Sem playlist cadastrada, a Home permanece acessivel para
            // permitir o cadastro. Nao existe catalogo para pre-carregar.
            catalogLoadingState = null
            return@LaunchedEffect
        }
        val remoteRequest = portalLoadRequest?.takeIf { request ->
            runCatching { org.json.JSONObject(request).getString("id") == playlistId }.getOrDefault(false)
        }
        val manualRefreshRequested = manualCatalogRefresh.pendingPlaylistId() == playlistId
        // Operacao manual ja esta cuidando do seu proprio progresso.
        if (manualPlaylistOperationId == playlistId) return@LaunchedEffect

        if (
            activePlaylistContentSourceId == playlistId &&
            catalogLoadingState == null && remoteRequest == null && !manualRefreshRequested
        ) {
            return@LaunchedEffect
        }

        val activePlaylist =
            playlists.firstOrNull {
                it.id == playlistId
            } ?: return@LaunchedEffect

        if (catalogLoadingDismissed && (dismissedCatalogPlaylistId == null || dismissedCatalogPlaylistId == playlistId) && remoteRequest == null && !manualRefreshRequested) return@LaunchedEffect
        catalogLoadingDismissed = false
        startupCatalogJob = currentCoroutineContext()[Job]
        val openingStartedAt = SystemClock.elapsedRealtime()
        val showHistoryImport = activePlaylistContentSourceId != null && activePlaylistContentSourceId != playlistId
        catalogLoadingState = AppCatalogLoadingState(
            phase = AppCatalogLoadingPhase.READING_CATALOG
        )
        var catalogImported = false
        try {
            val remoteCatalogReady = remoteRequest?.let { playlistStorage.isPortalCatalogReady(it) } ?: false
            automaticSyncJob?.cancel()
            automaticSyncJob = null
            val content = when (activePlaylist) {
                is PlaylistSource.M3u -> {
                    /*
                     * M3U:
                     *
                     * 1. tenta ativar uma geração READY já existente;
                     * 2. se ainda não houver ACTIVE, sincroniza pela melhor fonte:
                     *    Xtream automático quando compatível, M3U como fallback;
                     * 3. ativa a nova geração;
                     * 4. NÃO reconstrói o catálogo inteiro em memória.
                     *
                     * A tela de Canais lerá o Room diretamente via Paging.
                     */
                    br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("cache_ativacao") {
                        playlistCacheGenerationManager.activateReadyGenerationOnStartup(playlistId)
                    }

                    val cacheAvailable = br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("cache_consulta") {
                        playlistCatalogRepository.hasActiveCache(playlistId)
                    }
                    Log.i("StartupPerf", "etapa=catalogo_abertura cache=$cacheAvailable remoto=${remoteRequest != null} pedidoJaImportado=$remoteCatalogReady")
                    val manualSyncNeeded = manualRefreshRequested && manualCatalogRefresh.needsSync(
                        playlistId, playlistCacheDao.getActiveGeneration(playlistId)?.generationId)
                    if (!cacheAvailable || remoteRequest != null && !remoteCatalogReady || manualSyncNeeded) {


                        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("catalogo_importacao") {
                            m3uSourceSynchronizer.sync(source = activePlaylist)
                            catalogImported = true
                        }
                        automaticSyncPreferences.edit()
                            .putLong("success_$playlistId", System.currentTimeMillis())
                            .apply()

                        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("cache_ativacao") {
                            playlistCacheGenerationManager.activateReadyGenerationOnStartup(playlistId)
                        }
                    }

                    /*
                     * Se alguma atualização anterior deixou uma geração STALE,
                     * ela é limpa fora do caminho crítico da abertura do app.
                     */
                    scheduleStaleGenerationCleanup(
                        playlistId = playlistId
                    )

                    null
                }

                is PlaylistSource.XtreamCodes -> {
                    /*
                     * XTREAM CODES DIRETO:
                     *
                     * 1. tenta aproveitar uma geração READY já existente;
                     * 2. se ainda não houver ACTIVE, sincroniza o Xtream para o Room;
                     * 3. ativa a nova geração;
                     * 4. agenda a limpeza STALE fora do caminho crítico;
                     * 5. a interface passa a ler o catálogo por Paging.
                     */
                    br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("cache_ativacao") {
                        playlistCacheGenerationManager.activateReadyGenerationOnStartup(playlistId)
                    }

                    val cacheAvailable = br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("cache_consulta") {
                        playlistCatalogRepository.hasActiveCache(playlistId)
                    }
                    Log.i("StartupPerf", "etapa=catalogo_abertura cache=$cacheAvailable remoto=${remoteRequest != null} pedidoJaImportado=$remoteCatalogReady")
                    val manualSyncNeeded = manualRefreshRequested && manualCatalogRefresh.needsSync(
                        playlistId, playlistCacheDao.getActiveGeneration(playlistId)?.generationId)
                    if (!cacheAvailable || remoteRequest != null && !remoteCatalogReady || manualSyncNeeded) {


                        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("catalogo_importacao") {
                            m3uSourceSynchronizer.sync(source = activePlaylist)
                            catalogImported = true
                        }
                        automaticSyncPreferences.edit()
                            .putLong("success_$playlistId", System.currentTimeMillis())
                            .apply()

                        br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("cache_ativacao") {
                            playlistCacheGenerationManager.activateReadyGenerationOnStartup(playlistId)
                        }
                    }

                    scheduleStaleGenerationCleanup(
                        playlistId = playlistId
                    )

                    null
                }
            }
            // Record only after ACTIVE has been committed; the request remains pending
            // until preparation/navigation finishes, without downloading it twice.
            if (remoteRequest != null) playlistStorage.markPortalCatalogReady(remoteRequest)
            if (manualRefreshRequested) {
                val generation = playlistCacheDao.getActiveGeneration(playlistId)?.generationId
                    ?: error("A lista atualizada ainda nao esta ativa.")
                withContext(Dispatchers.IO) { check(manualCatalogRefresh.markImported(playlistId, generation)) }
            }
            activePlaylistContent = content
            activePlaylistContentSourceId = playlistId
            br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.measureSuspend("catalogo_finalizacao") {
                prepareCatalogBeforeHome(activePlaylist, isRefresh = manualRefreshRequested || catalogImported || remoteRequest != null, showHistoryImport = showHistoryImport)
            }
            if (manualRefreshRequested) {
                withContext(Dispatchers.IO) { check(manualCatalogRefresh.complete(playlistId)) }
            }
            catalogLoadingState = null
            Log.i("StartupPerf", "etapa=abertura_concluida duracaoMs=${SystemClock.elapsedRealtime()-openingStartedAt}")
            if (remoteRequest != null) {
                navController.navigate(AppDestination.Home.route) {
                    popUpTo(AppDestination.Home.route) { inclusive = true }
                    launchSingleTop = true
                }
                playlistStorage.finishPortalLoad(remoteRequest)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            activePlaylistContent = null
            activePlaylistContentSourceId = null
            val previous = catalogLoadingState
            catalogLoadingState = (previous ?: AppCatalogLoadingState()).copy(
                phase = AppCatalogLoadingPhase.ERROR,
                errorMessage = error.message ?: "Nao foi possivel preparar o catalogo."
            )
        }

    }

    LaunchedEffect(startupPreferencesResolved, catalogLoadingState, portalSyncRequests) {
        if (startupPreferencesResolved && catalogLoadingState == null && portalSyncRequests == 0) {
            withFrameNanos { }
            if (catalogueHomeReadyAt == null) catalogueHomeReadyAt = SystemClock.elapsedRealtime()
            br.com.flaviotorres.iptvpremium.core.startup.StartupTiming.homeReady()
            br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.homeReady(context.applicationContext)
        }
    }

    /*
     * ATUALIZACAO AUTOMATICA EM PRIMEIRO PLANO
     *
     * Nao modifica catalogLoadingState e nao bloqueia a Home.
     * Mantem a geracao ACTIVE ate outra estar pronta.
     * Uma falha nao deve apagar o catalogo anterior.
     *
     * A sincronizacao automatica Xtream-compatible nao usa
     * o fallback de download da M3U completa.
     */

    val foregroundLifecycleOwner = remember(context) {
        var candidate: Context? = context
        var owner: LifecycleOwner? = null

        while (candidate != null) {
            val current = candidate

            if (current is LifecycleOwner) {
                owner = current
                break
            }

            val next = (current as? ContextWrapper)?.baseContext

            if (next == current) break

            candidate = next
        }

        owner
    }

    // Reavaliar quando o aplicativo voltar ao primeiro plano.
    DisposableEffect(foregroundLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                automaticCheckSerial++

            }
        }

        foregroundLifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {
            foregroundLifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    DisposableEffect(foregroundLifecycleOwner, historySyncSession) {
        onDispose {
            // Finish resets a session; going to the background or recreating a screen does not.
            if ((foregroundLifecycleOwner as? android.app.Activity)?.isFinishing == true) historySyncSession.reset()
        }
    }

    LaunchedEffect(historySyncRun, automaticCheckSerial, foregroundLifecycleOwner) {
        val run = historySyncRun ?: return@LaunchedEffect
        if (run.playlist == activePlaylistId &&
            foregroundLifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true &&
            historySyncSession.consumeCompletion(run.id)) {
            Toast.makeText(context, "Histórico carregado.", Toast.LENGTH_SHORT).show()
        }
    }

    // Enquanto estiver aberto, verificar periodicamente.
    // Esta verificacao nao faz requisicoes por si so.
    LaunchedEffect(foregroundLifecycleOwner) {
        while (true) {
            delay(AUTO_SYNC_CHECK_MS)

            if (
                foregroundLifecycleOwner?.lifecycle
                    ?.currentState
                    ?.isAtLeast(Lifecycle.State.RESUMED) == true
            ) {
                automaticCheckSerial++
            }
        }
    }

    LaunchedEffect(
        activePlaylistId,
        playlists,
        startupPreferencesResolved,
        catalogLoadingState,
        manualPlaylistOperationId,
        automaticCheckSerial,
        selectedMediaBusy,
        userIdle
    ) {
        if (!startupPreferencesResolved || !userIdle) return@LaunchedEffect
        if (catalogLoadingState != null) return@LaunchedEffect
        if (manualPlaylistOperationId != null) return@LaunchedEffect
        if (loadingPlaylistId != null) return@LaunchedEffect
        if (automaticSyncJob?.isActive == true || selectedMediaBusy) return@LaunchedEffect
        if (br.com.flaviotorres.iptvpremium.data.storage.CatalogRefreshPolicy.defer(
                SystemClock.elapsedRealtime(), catalogueHomeReadyAt)) return@LaunchedEffect

        val playlistId = activePlaylistId
            ?: return@LaunchedEffect

        val source = playlists.firstOrNull {
            it.id == playlistId
        } ?: return@LaunchedEffect

        // O primeiro carregamento, sem cache, continua sendo
        // responsabilidade do fluxo de inicializacao existente.
        val hasCache = try {
            playlistCatalogRepository.hasActiveCache(
                playlistId = playlistId
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(
                AUTO_SYNC_TAG,
                "Cache indisponivel: ${error.javaClass.simpleName}"
            )
            false
        }

        if (!hasCache) return@LaunchedEffect

        val resolvedXtream = when (source) {
            is PlaylistSource.XtreamCodes -> source
            is PlaylistSource.M3u -> null
        }

        val intervalMs =
            if (resolvedXtream != null) {
                AUTO_SYNC_XTREAM_MS
            } else {
                AUTO_SYNC_M3U_MS
            }

        val nowMs = System.currentTimeMillis()

        val lastSuccess = automaticSyncPreferences.getLong(
            "success_$playlistId",
            0L
        )

        val lastAttempt = automaticSyncPreferences.getLong(
            "attempt_$playlistId",
            0L
        )

        // Existing cache with no scheduler timestamp (for example after an APK update)
        // is usable. Adopt it without treating the missing timestamp as an expired import.
        if (lastSuccess <= 0L) {
            automaticSyncPreferences.edit().putLong("success_$playlistId", nowMs).apply()
            return@LaunchedEffect
        }
        if (
            lastSuccess > 0L &&
            nowMs >= lastSuccess &&
            nowMs - lastSuccess < intervalMs
        ) {
            return@LaunchedEffect
        }

        if (
            lastAttempt > 0L &&
            nowMs >= lastAttempt &&
            nowMs - lastAttempt < AUTO_SYNC_RETRY_MS
        ) {
            return@LaunchedEffect
        }

        automaticSyncPreferences.edit()
            .putLong("attempt_$playlistId", nowMs)
            .apply()

        automaticSyncJob = coroutineScope.launch {
            val startedAt = SystemClock.elapsedRealtime()

            Log.i(
                AUTO_SYNC_TAG,
                "AUTO_INICIO tipo=" +
                    if (resolvedXtream != null) "XTREAM" else "M3U"
            )

            try {


                when {
                    resolvedXtream != null -> {
                        // Respeitar o ID da playlist original para
                        // nao gravar conteudo na fonte incorreta.
                        check(resolvedXtream.id == playlistId) {
                            "ID de fonte Xtream inconsistente."
                        }

                        // Usa a API Xtream diretamente.
                        // Se falhar, nao baixa M3U gigante.
                        m3uSourceSynchronizer.sync(
                            source = resolvedXtream
                        )
                    }

                    source is PlaylistSource.M3u -> {
                        // M3U generica: importacao nativa
                        // com intervalo maior (6 horas).
                        m3uSourceSynchronizer.sync(
                            source = source
                        )
                    }

                    else -> error("Tipo de playlist nao suportado.")
                }

                currentCoroutineContext().ensureActive()

                playlistCacheGenerationManager
                    .activateReadyGenerationOnStartup(
                        playlistId = playlistId
                    )

                automaticSyncPreferences.edit()
                    .putLong(
                        "success_$playlistId",
                        System.currentTimeMillis()
                    )
                    .apply()

                scheduleStaleGenerationCleanup(
                    playlistId = playlistId
                )
                if (activePlaylistId == playlistId) {
                    historySyncSession.request(playlistId, restart = true, origin = sharedLibrary.historySource() ?: playlistId)
                    Log.i("HistoryTriggerPerf", "event=automatic_list_update")
                }

                Log.i(
                    AUTO_SYNC_TAG,
                    "AUTO_SUCESSO duracaoMs=" +
                        "${SystemClock.elapsedRealtime() - startedAt}"
                )
            } catch (cancelled: CancellationException) {
                Log.i(
                    AUTO_SYNC_TAG,
                    "AUTO_CANCELADA prioridade para outra operacao"
                )
                throw cancelled
            } catch (error: Exception) {
                // A geracao anterior continua ativa.
                // Evitar mensagens com URL ou credenciais.
                Log.w(
                    AUTO_SYNC_TAG,
                    "AUTO_FALHA tipo=${error.javaClass.simpleName} " +
                        "duracaoMs=" +
                        "${SystemClock.elapsedRealtime() - startedAt}; " +
                        "catalogo anterior preservado"
                )
            }
        }
    }
    // Keep bounded Paging sessions across Home/catalogue round trips; never share details VMs.
    val catalogOwner = remember(activePlaylistId, parentalAccessState) {
        object : androidx.lifecycle.ViewModelStoreOwner {
            override val viewModelStore = androidx.lifecycle.ViewModelStore()
        }
    }
    DisposableEffect(catalogOwner) {
        onDispose { catalogOwner.viewModelStore.clear() }
    }
    val retainedMovies: MoviesViewModel = viewModel(catalogOwner, key = "catalog-movies", factory = moviesViewModelFactory)
    val retainedSeries: SeriesViewModel = viewModel(catalogOwner, key = "catalog-series", factory = seriesViewModelFactory)
    val retainedChannels: LiveTvViewModel = viewModel(catalogOwner, key = "catalog-channels", factory = liveTvViewModelFactory)
    val catalogSessions = remember(catalogOwner) { br.com.flaviotorres.iptvpremium.data.cache.CatalogSessionRevisions() }
    val catalogGeneration by key(activePlaylistId) {
        remember(activePlaylistId) {
            activePlaylistId?.let { playlistCacheDao.observeActiveGenerationId(it) }
                ?: kotlinx.coroutines.flow.flowOf(null)
        }.collectAsState(initial = null)
    }

    LaunchedEffect(userIdle) {
        if(!userIdle) { automaticSyncJob?.cancel(); automaticSyncJob=null }
    }
    LaunchedEffect(activePlaylistId) {
        automaticSyncJob?.cancel();automaticSyncJob=null
    }

    // O NavHost continua existindo, preservando todo o foco e back stack,
    // mas a tela de abertura fica ACIMA dele ate concluir os trabalhos.
    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(180)) }
        ) {

            /*
             * HOME
             */
            composable(
                route = AppDestination.Home.route,
                enterTransition = { androidx.compose.animation.EnterTransition.None },
                exitTransition = { androidx.compose.animation.ExitTransition.None },
                popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                popExitTransition = { androidx.compose.animation.ExitTransition.None }
            ) {
                LaunchedEffect(activePlaylistId, catalogLoadingState, loadingPlaylistId) {
                    if (activePlaylistId == null || catalogLoadingState != null || loadingPlaylistId != null)
                        return@LaunchedEffect
                    br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.idle.collectLatest { idle ->
                        if(idle) {
                            try { MoviePosterLoader.prepareRecentCachedPosters(context.applicationContext) }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { /* Preparo opcional nunca compete com o controle. */ }
                        }
                    }
                }
                LaunchedEffect(Unit) { br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = false }
                HomeScreen(
                    normalEnabled = !(parentalAccessState.enabled && parentalAccessState.rules.kidsOnly),
                    onKidsClick = {
                        br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = true
                        navController.navigate("kids") { launchSingleTop = true }
                    },
                    onLiveTvClick = {
                        br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = false
                        navController.navigate(
                            AppDestination.LiveTv.route
                        )
                    },
                    onMoviesClick = {
                        br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = false
                        navController.navigate(
                            AppDestination.Movies.route
                        )
                    },
                    onSeriesClick = {
                        br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = false
                        navController.navigate(
                            AppDestination.Series.route
                        )
                    },
                    onListaClick = {
                        navController.navigate(PLAYLIST_LIST_ROUTE) { launchSingleTop = true }
                    },
                    onAtualizarClick = {
                        var owner: Context? = context
                        while (owner is ContextWrapper && owner !is android.app.Activity) owner = owner.baseContext
                        (owner as? android.app.Activity)?.let {
                            br.com.flaviotorres.iptvpremium.core.storage.CacheRestartActivity.restart(it)
                        }
                    },
                    onConfiguracoesClick = {
                        navController.navigate(AppDestination.Settings.route)
                    }
                )
            }

            /* Configurações do aplicativo: conta de legendas por usuário. */
            composable("kids") { entry ->
                LaunchedEffect(Unit) { br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = true }
                br.com.flaviotorres.iptvpremium.ui.kids.KidsScreen(
                    onChannels = {
                        br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = true
                        navController.navigate(AppDestination.LiveTv.route)
                        navController.currentBackStackEntry?.savedStateHandle?.set("kids_catalog",true)
                    },
                    onMovies = {
                        br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = true
                        navController.navigate(AppDestination.Movies.route)
                        navController.currentBackStackEntry?.savedStateHandle?.set("kids_catalog",true)
                    },
                    onSeries = {
                        br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value = true
                        navController.navigate(AppDestination.Series.route)
                        navController.currentBackStackEntry?.savedStateHandle?.set("kids_catalog",true)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(route = AppDestination.Settings.route) {
                IPTVSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onSubdlClick = { navController.navigate(AppDestination.SubdlSettings.route) }
                )
            }

            composable(route = AppDestination.SubdlSettings.route) {
                SubdlAccountScreen(onBack = { navController.popBackStack() })
            }

            /*
             * MINHAS LISTAS
             */
            composable(
                route = PLAYLIST_LIST_ROUTE,
                enterTransition = { androidx.compose.animation.EnterTransition.None },
                exitTransition = { androidx.compose.animation.ExitTransition.None },
                popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                popExitTransition = { androidx.compose.animation.ExitTransition.None }
            ) {
                val selectPlaylist:(PlaylistSource,Boolean)->Unit = {playlist,forceRefresh->
                        if (loadingPlaylistId != playlist.id || manualCatalogJob?.isActive != true) {
                            historyExecutorJob?.cancel()
                            automaticSyncJob?.cancel()
                            startupCatalogJob?.cancel()
                            manualCatalogJob?.cancel()
                            val previousPlaylistId = activePlaylistId
                            loadingPlaylistId = playlist.id
                            sharedSelectionInFlight = playlist.id
                            manualPlaylistOperationId = playlist.id
                            catalogLoadingState = AppCatalogLoadingState(
                                phase = AppCatalogLoadingPhase.READING_CATALOG,
                                isPlaylistRefresh = true
                            )

                            catalogLoadingDismissed = false
                            dismissedCatalogPlaylistId = null
                            manualCatalogJob = coroutineScope.launch {
                                historyExecutorJob?.cancel()
                                // Espera o encerramento seguro de uma
                                // eventual sincronizacao automatica antes
                                // de iniciar uma importacao manual.
                                automaticSyncJob?.cancel()
                                automaticSyncJob = null

                                /*
                                 * A lista ativa também pode ser selecionada.
                                 *
                                 * Usar lista reaproveita o catálogo; Atualizar lista
                                 * solicita explicitamente uma nova importação.
                                 *
                                 * O indicador permanece visível até o processo
                                 * terminar. Em caso de sucesso, voltamos
                                 * automaticamente para a Home.
                                 */
                                val isAlreadyActive =
                                    playlist.id == previousPlaylistId

                                val updateFlowStartedAt =
                                    SystemClock.elapsedRealtime()

                                Log.i(
                                    NAVIGATION_PERF_TAG,
                                    "Fluxo da playlist iniciado. isAlreadyActive=$isAlreadyActive"
                                )

                                var didSynchronize = false

                                try {
                                    // SharedPreferences.commit and the history lock must not block input.
                                    playlistCacheGenerationManager.activateReadyGenerationOnStartup(playlist.id)
                                    val cached = playlistCatalogRepository.hasActiveCache(playlist.id)
                                    Log.i("StartupPerf", "etapa=selecao_catalogo cache=$cached")
                                    if (!cached || forceRefresh) {
                                        when (playlist) {
                                            is PlaylistSource.M3u -> m3uSourceSynchronizer.sync(playlist)
                                            is PlaylistSource.XtreamCodes -> m3uSourceSynchronizer.sync(playlist)
                                        }
                                        didSynchronize = true
                                        playlistCacheGenerationManager.activateReadyGenerationOnStartup(playlist.id)
                                    }
                                    scheduleStaleGenerationCleanup(playlist.id)
                                    // Updating an inactive list does not select it or import its history.
                                    if(forceRefresh && !isAlreadyActive) {
                                        automaticSyncPreferences.edit().putLong("success_${playlist.id}",System.currentTimeMillis()).apply()
                                        catalogLoadingState=null
                                        return@launch
                                    }
                                    val content: PlaylistContent? = null

                                    // Atualizacao: aguarda a sincronizacao Room e
                                    // a pequena etapa inicial de favoritos. TMDB,
                                    // elenco e demais capas seguem em background.
                                    // Nao aguardamos centenas de titulos.
                                    prepareCatalogBeforeHome(playlist, isRefresh = didSynchronize || forceRefresh, showHistoryImport = !isAlreadyActive)

                                    val saveActiveIdStartedAt =
                                        SystemClock.elapsedRealtime()

                                    Log.i(
                                        NAVIGATION_PERF_TAG,
                                        "saveActivePlaylistId() iniciada."
                                    )

                                    playlistStorage
                                        .saveActivePlaylistId(
                                            playlist.id
                                        )
                                    sharedSelectionRevision++

                                    Log.i(
                                        NAVIGATION_PERF_TAG,
                                        "saveActivePlaylistId() concluída. duracaoMs=${
                                            SystemClock.elapsedRealtime() -
                                                    saveActiveIdStartedAt
                                        }"
                                    )

                                    val stateUpdateStartedAt =
                                        SystemClock.elapsedRealtime()

                                    activePlaylistContent = content
                                    activePlaylistContentSourceId =
                                        playlist.id
                                    playlistConnectionError = null

                                    Log.i(
                                        NAVIGATION_PERF_TAG,
                                        "Estados Compose atualizados. duracaoMs=${
                                            SystemClock.elapsedRealtime() -
                                                    stateUpdateStartedAt
                                        }"
                                    )

                                    /*
                                     * Terminou com sucesso:
                                     * retorna automaticamente para a Home.
                                     */
                                    val navigationStartedAt =
                                        SystemClock.elapsedRealtime()

                                    Log.i(
                                        NAVIGATION_PERF_TAG,
                                        "navigate(Home) iniciada."
                                    )

                                    // Confirm the persisted selection before exposing its Home/catalogues.
                                    playlistStorage.selection.first { it.activeId == playlist.id }
                                    withFrameNanos { }
                                    loadingPlaylistId = null
                                    manualPlaylistOperationId = null
                                    sharedSelectionInFlight = null
                                    catalogLoadingState = null
                                    navController.navigate(
                                        AppDestination.Home.route
                                    ) {
                                        popUpTo(
                                            AppDestination.Home.route
                                        ) {
                                            inclusive = false
                                        }

                                        launchSingleTop = true
                                    }

                                    if (didSynchronize) {
                                        automaticSyncPreferences.edit()
                                            .putLong(
                                                "success_${playlist.id}",
                                                System.currentTimeMillis()
                                            )
                                            .apply()
                                    }

                                    catalogLoadingState = null
                                    Log.i(
                                        NAVIGATION_PERF_TAG,
                                        "navigate(Home) retornou. duracaoMs=${
                                            SystemClock.elapsedRealtime() -
                                                    navigationStartedAt
                                        }; fluxoTotalMs=${
                                            SystemClock.elapsedRealtime() -
                                                    updateFlowStartedAt
                                        }"
                                    )
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    val previous = catalogLoadingState
                                    catalogLoadingState = (previous ?: AppCatalogLoadingState()).copy(
                                        phase = AppCatalogLoadingPhase.ERROR,
                                        errorMessage = error.message
                                            ?: "A atualizacao ou a preparacao do catalogo falhou.",
                                        isPlaylistRefresh = true
                                    )
                                    /*
                                     * Uma atualização M3U que falhar não apaga
                                     * a geração ACTIVE anterior.
                                     */
                                    /*
                                     * Como a troca de lista só é confirmada
                                     * depois do sucesso, uma falha ao selecionar
                                     * OUTRA playlist não pode apagar o conteúdo
                                     * da playlist que já estava ativa.
                                     */
                                    if (isAlreadyActive) {
                                        if (
                                            playlist is PlaylistSource.M3u ||
                                            playlist is PlaylistSource.XtreamCodes
                                        ) {
                                            /*
                                             * A geração ACTIVE anterior continua
                                             * no Room e será lida por Paging,
                                             * tanto para M3U quanto para Xtream.
                                             */
                                            activePlaylistContent = null
                                            activePlaylistContentSourceId =
                                                playlist.id
                                        } else {
                                            activePlaylistContent = null
                                            activePlaylistContentSourceId = null
                                        }
                                    }

                                    playlistConnectionError =
                                        "Não foi possível atualizar a lista agora. Tente novamente."
                                } finally {
                                    val finallyStartedAt =
                                        SystemClock.elapsedRealtime()

                                    if (manualCatalogJob === currentCoroutineContext()[Job]) {
                                        loadingPlaylistId = null
                                        manualPlaylistOperationId = null
                                        sharedSelectionInFlight = null
                                    }

                                    Log.i(
                                        NAVIGATION_PERF_TAG,
                                        "finally concluído. loadingPlaylistId=null; duracaoMs=${
                                            SystemClock.elapsedRealtime() -
                                                    finallyStartedAt
                                        }; fluxoTotalMs=${
                                            SystemClock.elapsedRealtime() -
                                                    updateFlowStartedAt
                                        }"
                                    )
                                }
                            }
                        }
                }
                PlaylistListScreen(
                    playlists = playlists,
                    activePlaylistId = activePlaylistId,
                    loadingPlaylistId = loadingPlaylistId,
                    onPlaylistClick = {selectPlaylist(it,false)},
                    onRefreshClick = {selectPlaylist(it,true)},
                    onAddClick = {
                        navController.navigate(
                            PLAYLIST_SETUP_ROUTE
                        )
                    },
                    onDeleteClick = { playlist ->
                        val updatedPlaylists =
                            playlists.filterNot {
                                it.id == playlist.id
                            }

                        coroutineScope.launch {
                            playlistStorage.savePlaylists(
                                updatedPlaylists
                            )

                            /*
                             * O cache existe somente enquanto a playlist
                             * cadastrada existir.
                             */


                            playlistCacheGenerationManager
                                .deletePlaylistCache(
                                    playlistId = playlist.id
                                )

                            CatalogDetailsWarmupCache.clearPlaylist(playlist.id)

                            if (
                                activePlaylistId ==
                                playlist.id
                            ) {
                                playlistStorage
                                    .saveActivePlaylistId(
                                        null
                                    )

                                activePlaylistContent = null
                                activePlaylistContentSourceId = null
                            }
                        }
                    }
                )
            }

            /*
             * CADASTRAR NOVA LISTA
             *
             * Agora o cadastro NÃO é confirmado imediatamente.
             * Primeiro o repositório tenta conectar e carregar o
             * conteúdo real. Só depois de sucesso a lista é salva.
             * Após o sucesso, voltamos explicitamente para MINHAS LISTAS.
             */
            composable(
                route = PLAYLIST_SETUP_ROUTE
            ) {
                PlaylistSetupScreen(
                    isConnecting =
                        isPlaylistConnecting,
                    connectionError =
                        playlistConnectionError,
                    onSaveXtream = {
                            name,
                            serverUrl,
                            username,
                            password ->

                        val newPlaylist =
                            PlaylistSource.XtreamCodes(
                                id = UUID
                                    .randomUUID()
                                    .toString(),
                                name =
                                    name.ifBlank {
                                        "Minha Lista"
                                    },
                                serverUrl =
                                    serverUrl,
                                username =
                                    username,
                                password =
                                    password
                            )

                        playlistConnectionError = null
                        isPlaylistConnecting = true
                        sharedSelectionInFlight = newPlaylist.id
                        catalogLoadingState = AppCatalogLoadingState(isPlaylistRefresh = true)

                        catalogLoadingDismissed = false
                        dismissedCatalogPlaylistId = null
                        manualCatalogJob = coroutineScope.launch {
                            runCatching {
                                /*
                                 * SINCRONIZAÇÃO INICIAL DO XTREAM DIRETO:
                                 *
                                 * O catálogo é persistido no Room e a leitura
                                 * das telas passa a acontecer por Paging.
                                 */


                                m3uSourceSynchronizer.sync(
                                    source = newPlaylist
                                )

                                playlistCacheGenerationManager
                                    .activateReadyGenerationOnStartup(
                                        playlistId =
                                            newPlaylist.id
                                    )

                                scheduleStaleGenerationCleanup(
                                    playlistId =
                                        newPlaylist.id
                                )

                                prepareCatalogBeforeHome(newPlaylist, isRefresh = true, showHistoryImport = true)
                                null
                            }.onSuccess { content ->
                                playlistStorage.savePlaylists(
                                    playlists + newPlaylist
                                )

                                playlistStorage.saveActivePlaylistId(
                                    newPlaylist.id
                                )

                                activePlaylistContent =
                                    content

                                activePlaylistContentSourceId =
                                    newPlaylist.id

                                isPlaylistConnecting = false
                                catalogLoadingState = null
                                sharedSelectionInFlight = null
                                sharedSelectionRevision++
                                playlistConnectionError = null

                                navController.navigate(
                                    PLAYLIST_LIST_ROUTE
                                ) {
                                    popUpTo(
                                        PLAYLIST_LIST_ROUTE
                                    ) {
                                        inclusive = true
                                    }

                                    launchSingleTop = true
                                }
                            }.onFailure { error ->
                                if (error is CancellationException) throw error
                                isPlaylistConnecting = false
                                catalogLoadingState = null
                                sharedSelectionInFlight = null

                                playlistConnectionError = "Não foi possível conectar à lista."
                                catalogLoadingState = AppCatalogLoadingState(
                                    phase = AppCatalogLoadingPhase.ERROR,
                                    errorMessage = error.message ?: "Não foi possível importar a lista. Confira o link ou tente outra lista.",
                                    isPlaylistRefresh = true
                                )
                            }
                        }
                    },
                    onSaveM3u = {
                            name,
                            playlistUrl ->

                        val newPlaylist =
                            PlaylistSource.M3u(
                                id = UUID
                                    .randomUUID()
                                    .toString(),
                                name =
                                    name.ifBlank {
                                        "Minha Lista"
                                    },
                                playlistUrl =
                                    playlistUrl
                            )

                        playlistConnectionError = null
                        isPlaylistConnecting = true
                        sharedSelectionInFlight = newPlaylist.id
                        catalogLoadingState = AppCatalogLoadingState(isPlaylistRefresh = true)

                        catalogLoadingDismissed = false
                        dismissedCatalogPlaylistId = null
                        manualCatalogJob = coroutineScope.launch {
                            runCatching {
                                /*
                                 * SINCRONIZAÇÃO INICIAL DA M3U:
                                 *
                                 * Se a URL for Xtream-compatible, usamos a API
                                 * e persistimos o catálogo no Room. Caso contrário,
                                 * usamos a importação M3U nativa em streaming.
                                 */


                                m3uSourceSynchronizer.sync(
                                    source = newPlaylist
                                )

                                /*
                                 * Como esta é a primeira importação de uma
                                 * playlist nova, ela pode virar ACTIVE
                                 * imediatamente após terminar com sucesso.
                                 */
                                playlistCacheGenerationManager
                                    .activateReadyGenerationOnStartup(
                                        playlistId =
                                            newPlaylist.id
                                    )

                                scheduleStaleGenerationCleanup(
                                    playlistId =
                                        newPlaylist.id
                                )

                                /*
                                 * O catálogo completo já está persistido no Room.
                                 * A leitura será feita sob demanda via Paging.
                                 */
                                prepareCatalogBeforeHome(newPlaylist, isRefresh = true, showHistoryImport = true)
                                null
                            }.onSuccess { content ->
                                playlistStorage.savePlaylists(
                                    playlists + newPlaylist
                                )

                                playlistStorage.saveActivePlaylistId(
                                    newPlaylist.id
                                )

                                activePlaylistContent =
                                    content

                                activePlaylistContentSourceId =
                                    newPlaylist.id

                                isPlaylistConnecting = false
                                catalogLoadingState = null
                                sharedSelectionInFlight = null
                                sharedSelectionRevision++
                                playlistConnectionError = null

                                navController.navigate(
                                    PLAYLIST_LIST_ROUTE
                                ) {
                                    popUpTo(
                                        PLAYLIST_LIST_ROUTE
                                    ) {
                                        inclusive = true
                                    }

                                    launchSingleTop = true
                                }
                            }.onFailure { error ->
                                if (error is CancellationException) throw error
                                isPlaylistConnecting = false
                                catalogLoadingState = null
                                sharedSelectionInFlight = null

                                playlistConnectionError = "Não foi possível conectar à lista."
                                catalogLoadingState = AppCatalogLoadingState(
                                    phase = AppCatalogLoadingPhase.ERROR,
                                    errorMessage = error.message ?: "Não foi possível importar a lista. Confira o link ou tente outra lista.",
                                    isPlaylistRefresh = true
                                )
                            }
                        }
                    }
                )
            }

            /*
             * CANAIS
             */
            composable(
                route = AppDestination.LiveTv.route
            ) {
                if (!br.com.flaviotorres.iptvpremium.ui.kids.KidsRouteAllowed { navController.popBackStack() }) return@composable
                val activePlaylist =
                    playlists.firstOrNull {
                        it.id == activePlaylistId
                    }

                val liveTvViewModel: LiveTvViewModel = retainedChannels

                /*
                 * M3U e Xtream Codes direto usam o mesmo catálogo Room + Paging.
                 */
                val useRoomPaging =
                    activePlaylistId != null &&
                            (
                                    activePlaylist is PlaylistSource.M3u ||
                                            activePlaylist is PlaylistSource.XtreamCodes
                                    )

                var channelsBound by remember(liveTvViewModel) { mutableStateOf(false) }
                val channelsMetadataLoading by liveTvViewModel.isLoadingMetadata.collectAsState()
                var channelsCatalogError by remember(activePlaylistId) { mutableStateOf<String?>(null) }
                var channelsRetry by remember(activePlaylistId) { mutableIntStateOf(0) }
                LaunchedEffect(activePlaylistId, useRoomPaging, catalogGeneration, parentalAccessState, channelsRetry) {
                    channelsBound = false
                    channelsCatalogError = null
                    val playlistId = activePlaylistId
                    try {
                        if (useRoomPaging && playlistId != null && catalogGeneration != null && activePlaylist != null) {
                            // Only this route completes deferred live endpoints. Home/list switching never does.
                            m3uSourceSynchronizer.ensureChannels(activePlaylist)
                            playlistCatalogRepository.invalidateLiveCategories(playlistId)
                            catalogSessions.bind("channels", playlistId, catalogGeneration, parentalAccessState,
                                { liveTvViewModel.setPlaylist(playlistId) }, { liveTvViewModel.refreshMetadata() })
                            if (liveTvViewModel.metadataError.value != null) liveTvViewModel.refreshMetadata()
                            channelsBound = true
                        } else if (!useRoomPaging) {
                            liveTvViewModel.clear()
                            catalogSessions.clear("channels")
                            channelsBound = true
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { channelsCatalogError = "Não foi possível carregar os canais. Pressione OK para tentar novamente." }
                }


                val liveCategories by
                liveTvViewModel.categories
                    .collectAsState()

                val totalChannelCount by
                liveTvViewModel.channelCount
                    .collectAsState()

                val channelCountsByCategory by
                liveTvViewModel.categoryCounts
                    .collectAsState()

                val pagedChannels =
                    liveTvViewModel.channels
                        .collectAsLazyPagingItems()

                LiveTvScreen(
                    onExit = { navController.popBackStack(AppDestination.Home.route, false) },
                    catalogLoading = useRoomPaging && (!channelsBound || channelsMetadataLoading) && channelsCatalogError == null,
                    catalogError = channelsCatalogError,
                    onRetryCatalog = { channelsRetry++ },
                    onBrowseChanged = { query, sort, ids -> liveTvViewModel.browse(query, sort, ids) },
                    onCategoryAccessRequested = { id, title, proceed -> requestParentalCategory("LIVE_TV", id, title, proceed) },
                    playlistContent =
                        if (useRoomPaging) {
                            null
                        } else {
                            activePlaylistContent
                        },
                    pagedChannels =
                        if (useRoomPaging) {
                            pagedChannels
                        } else {
                            null
                        },
                    pagingCategories =
                        if (useRoomPaging) {
                            liveCategories
                        } else {
                            emptyList()
                        },
                    totalChannelCount =
                        if (useRoomPaging) {
                            totalChannelCount
                        } else {
                            0
                        },
                    pagingCategoryCounts =
                        if (useRoomPaging) {
                            channelCountsByCategory
                        } else {
                            emptyMap()
                        },
                    onSourceCategorySelected = {
                            categoryId ->
                        liveTvViewModel
                            .selectCategory(
                                categoryId
                            )
                    },
                    // EPG real: consultar somente a programação do canal selecionado.
                    // Se não houver fonte Xtream compatível, a tela mostrará
                    // programação indisponível, sem inserir dados fictícios.
                    onLoadChannelEpg = if (activePlaylist != null) {
                        { channelId ->
                            playlistRepository.loadChannelEpg(
                                source = activePlaylist,
                                channelId = channelId
                            )
                        }
                    } else {
                        null
                    },
                    onLoadFavoriteChannels = if (activePlaylist != null) {
                        { ids ->
                            ids.mapNotNull { id -> playlistCatalogRepository.getChannelById(activePlaylist.id, id) }
                        }
                    } else null,
                    onFindChannelAlternatives = if(activePlaylist!=null) { channelId ->
                        kotlinx.coroutines.flow.flow {
                            val origin=playlistCatalogRepository.getChannelById(activePlaylist.id,channelId)?:return@flow
                            // Preserve category priority even from Favourites or Continue watching.
                            playlistCatalogRepository.channelAlternatives(activePlaylist.id,origin,true).collect {
                                emit(br.com.flaviotorres.iptvpremium.ui.live.LiveStreamCandidate(activePlaylist.id,it))
                            }
                            playlistCatalogRepository.channelAlternatives(activePlaylist.id,origin,false).collect {
                                emit(br.com.flaviotorres.iptvpremium.ui.live.LiveStreamCandidate(activePlaylist.id,it))
                            }
                            // Only existing imported catalogues; this never imports or activates another list.
                            for(other in playlists.filter{it.id!=activePlaylist.id}) {
                                playlistCatalogRepository.channelAlternatives(other.id,origin,false).collect {
                                    emit(br.com.flaviotorres.iptvpremium.ui.live.LiveStreamCandidate(other.id,it))
                                }
                            }
                        }
                    } else null,
                    onLoadChannelNeighbours = if(activePlaylist!=null) { id,direction,categoryId,categoryOnly,ids ->
                        playlistCatalogRepository.channelNeighbours(activePlaylist.id,id,direction,categoryId,categoryOnly,ids)
                    } else null,
                    // Impede reaproveitar o EPG ao alternar entre playlists.
                    epgSourceKey = activePlaylist?.id
                )
            }

            /*
             * FILMES
             */
            composable(
                route = AppDestination.Movies.route,
                popEnterTransition = { androidx.compose.animation.EnterTransition.None }
            ) {
                if (!br.com.flaviotorres.iptvpremium.ui.kids.KidsRouteAllowed { navController.popBackStack() }) return@composable
                val activePlaylist =
                    playlists.firstOrNull {
                        it.id == activePlaylistId
                    }

                val moviesViewModel: MoviesViewModel = retainedMovies

                /*
                 * M3U e Xtream Codes direto usam o mesmo catálogo Room + Paging.
                 */
                val useRoomPaging =
                    activePlaylistId != null &&
                            (
                                    activePlaylist is PlaylistSource.M3u ||
                                            activePlaylist is PlaylistSource.XtreamCodes
                                    )

                LaunchedEffect(activePlaylistId, useRoomPaging, catalogGeneration, parentalAccessState) {
                    val playlistId = activePlaylistId
                    if (useRoomPaging && playlistId != null && catalogGeneration != null) {
                        catalogSessions.bind("movies", playlistId, catalogGeneration, parentalAccessState,
                            { moviesViewModel.setPlaylist(playlistId) }, { moviesViewModel.refreshMetadata() })
                        if (moviesViewModel.errorMessage.value != null) moviesViewModel.refreshMetadata()
                    } else if (!useRoomPaging) {
                        moviesViewModel.clear()
                        catalogSessions.clear("movies")
                    }
                }

                /*
                 * Mantém o conjunto de favoritos do AppNavigation sincronizado
                 * com o filtro paginado do MoviesViewModel.
                 */
                LaunchedEffect(
                    moviesViewModel, favoriteMovieIds
                ) {
                    moviesViewModel.setFavoriteMovieIds(
                        favoriteMovieIds
                    )
                }


                DisposableEffect(moviesViewModel) {
                    moviesViewModel.setCatalogVisible(true)
                    onDispose { moviesViewModel.setCatalogVisible(false) }
                }
                val movieSelectionRequired by moviesViewModel.selectionRequired.collectAsState()
                val movieCategories by
                moviesViewModel.categories
                    .collectAsState()

                val totalMovieCount by
                moviesViewModel.movieCount
                    .collectAsState()

                val movieCountsByCategory by
                moviesViewModel.categoryCounts
                    .collectAsState()

                val pagedMovies =
                    moviesViewModel.movies
                        .collectAsLazyPagingItems()

                // Na categoria, somente as capas sao antecipadas. Detalhes no clique.
                // Mesma area disponivel da rota de detalhes, para usar
                // dimensoes identicas ao decodificar backdrop e logo no cache RAM.
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val viewportWidthPx = with(density) {
                        maxWidth.roundToPx().coerceAtLeast(1)
                    }
                    val viewportHeightPx = with(density) {
                        maxHeight.roundToPx().coerceAtLeast(1)
                    }

                    MoviesScreen(
                        onResolvePosterCategory = { id ->
                            val playlist=activePlaylistId
                            if(playlist==null)null else {
                                val categoryId=playlistCatalogRepository.getMovieById(playlist,id)?.categoryId
                                movieCategories.firstOrNull{it.id==categoryId}?.name ?:
                                    playlistCatalogRepository.getMovieCategories(playlist).firstOrNull{it.id==categoryId}?.name
                            }
                        },
                        posterPage = { moviesViewModel.posterPage(it) },
                    onCategoryAccessRequested = { id, title, proceed -> requestParentalCategory("MOVIES", id, title, proceed) },
                        onRemoveFromContinue = { movieId ->
                            val playlistId = activePlaylistId
                            if (!playlistId.isNullOrBlank()) {
                                watchHistoryStore.remove(playlistId, PlaybackContentType.MOVIE, movieId)
                                historyRefreshSerial++
                                Toast.makeText(context, "Removido de Continuar assistindo", Toast.LENGTH_SHORT).show()
                            }
                        },
                        favoriteMovieIds = favoriteMovieIds,
                        watchHistoryEntries = watchHistoryEntries,
                        pagedMovies =
                            if (useRoomPaging) {
                                pagedMovies
                            } else {
                                null
                            },
                        pagingCategories =
                            if (useRoomPaging) {
                                movieCategories
                            } else {
                                emptyList()
                            },
                        categorySelectionRequired = movieSelectionRequired,
                        totalMovieCount =
                            if (useRoomPaging) {
                                totalMovieCount
                            } else {
                                0
                            },
                        pagingCategoryCounts =
                            if (useRoomPaging) {
                                movieCountsByCategory
                            } else {
                                emptyMap()
                            },
                        onSourceCategorySelected = {
                                categoryId ->
                            moviesViewModel
                                .selectCategory(
                                    categoryId
                                )
                        },
                        onFavoritesSelected = {
                            moviesViewModel
                                .selectFavorites()
                        },
                        onSearchQueryChanged = {
                                query ->
                            moviesViewModel
                                .setSearchQuery(
                                    query
                                )
                        },
                        onSortModeChanged = {
                                mode ->
                            moviesViewModel
                                .setSortMode(
                                    mode
                                )
                        },
                        onMovieClick = { movieId ->
                            // A rota de preparacao entra imediatamente;
                            // Room, Xtream, TMDB e imagens sao preparados nela.
                            if (activePlaylistId != null && movieId.isNotBlank()) {
                                br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.setBusy(true)
                                preparedMovieDetails = null
                                // So usamos os itens JA carregados pelo Paging.
                                // O filme clicado fica na lista, sem repetir capas.
                                loadingCarouselCandidates = uniqueCarouselPosters(
                                    movieId = movieId,
                                    movies = pagedMovies.itemSnapshotList.items,
                                    maximum = 125
                                )
                                val memoryUrls = loadingCarouselCandidates.mapNotNull { it.posterUrl }
                                    .filter { MoviePosterLoader.peekBitmap(it) != null }.toSet()
                                loadingCarouselPosters = br.com.flaviotorres.iptvpremium.ui.movies.CachedCarouselPolicy.select(
                                    movieId, loadingCarouselCandidates, { it.movieId }, { it.posterUrl }, memoryUrls, emptySet())
                                navController.navigate(
                                    "movie_preparation/${android.net.Uri.encode(movieId)}"
                                ) {
                                    launchSingleTop = true
                                }
                            } else {
                                Log.w(
                                    NAVIGATION_PERF_TAG,
                                    "Filme ou playlist invalida para preparacao."
                                )
                            }
                        },
                        onFavoriteClick = { movieId ->
                            toggleSharedFavorite("movies", movieId)
                        }
                    )

                }
            }

            /*
             * PREPARACAO PREMIUM DO FILME
             *
             * Ao selecionar um filme, esta rota surge imediatamente.
             * A grade continua no back stack. A coroutine pertence a esta
             * rota e e cancelada ao voltar, sem navegacao atrasada.
             *
             * A animacao permanece por pelo menos 3 segundos, mas continua
             * ate terminar a preparacao, caso esta seja mais demorada.
             */
            composable(
                route = MOVIE_PREPARATION_ROUTE,
                popExitTransition = { androidx.compose.animation.ExitTransition.None },
                exitTransition = { fadeOut(animationSpec = tween(340)) }
            ) { entry ->
                val movieId = entry.arguments
                    ?.getString("movieId")
                    .orEmpty()

                if (!br.com.flaviotorres.iptvpremium.ui.settings.ParentalItemAllowed(activePlaylistId, "MOVIES", movieId, playlistCatalogRepository) { navController.popBackStack() }) return@composable
                val selectedPlaylistId = activePlaylistId
                val activeSource = playlists.firstOrNull {
                    it.id == selectedPlaylistId
                }

                // ViewModel independente e leve para a rota de carregamento:
                // sem atualizar categorias e contadores da grade.
                val preparationViewModel: MoviesViewModel = viewModel(
                    factory = moviesViewModelFactory
                )

                var errorMessage by remember(movieId) {
                    mutableStateOf<String?>(null)
                }
                var retryCount by remember(movieId) {
                    mutableStateOf(0)
                }
                var finishRequested by remember(movieId) {
                    mutableStateOf(false)
                }

                // Fill decoration only with already cached eligible titles; never delays selected details.
                LaunchedEffect(movieId, selectedPlaylistId) {
                    val candidates = loadingCarouselCandidates
                    val diskUrls = MoviePosterLoader.cachedPosterUrls(context.applicationContext, candidates.map { it.posterUrl })
                    val selected = loadingCarouselPosters.firstOrNull { it.movieId == movieId }
                        ?: candidates.firstOrNull { it.movieId == movieId }
                    if (selected != null && selectedPlaylistId == activePlaylistId) {
                        val eligible = listOf(selected) + candidates.filterNot { it.movieId == movieId }
                        val memoryUrls = eligible.mapNotNull { it.posterUrl }
                            .filter { MoviePosterLoader.peekBitmap(it) != null }.toSet()
                        loadingCarouselPosters = br.com.flaviotorres.iptvpremium.ui.movies.CachedCarouselPolicy.select(
                            movieId, eligible, { it.movieId }, { it.posterUrl }, memoryUrls, diskUrls)
                    }
                }

                // Se o item clicado nao apareceu na amostra do Paging,
                // completamos somente o poster do filme escolhido pelo Room.
                LaunchedEffect(movieId, selectedPlaylistId) {
                    val selected = loadingCarouselPosters.firstOrNull { it.movieId == movieId }
                    if (!selectedPlaylistId.isNullOrBlank() && movieId.isNotBlank() &&
                        (selected == null || selected.posterUrl.isNullOrBlank())
                    ) {
                        try {
                            val movie = playlistCatalogRepository.getMovieById(
                                playlistId = selectedPlaylistId,
                                movieId = movieId
                            )
                            if (movie != null) {
                                val original = loadingCarouselPosters.filterNot {
                                    it.movieId == movieId ||
                                            (movie.posterUrl?.isNotBlank() == true &&
                                                    it.posterUrl == movie.posterUrl)
                                }
                                loadingCarouselPosters =
                                    (listOf(CinematicCarouselPoster(movieId, movie.name, movie.posterUrl)) +
                                            original).take(MAX_CAROUSEL_POSTERS)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // A animacao permanece utilizavel sem a imagem.
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val viewportWidthPx = with(density) {
                        maxWidth.roundToPx().coerceAtLeast(1)
                    }
                    val viewportHeightPx = with(density) {
                        maxHeight.roundToPx().coerceAtLeast(1)
                    }

                    LaunchedEffect(
                        movieId,
                        selectedPlaylistId,
                        retryCount,
                        viewportWidthPx,
                        viewportHeightPx
                    ) {
                        val startedAt = SystemClock.elapsedRealtime()
                        finishRequested = false
                        errorMessage = null

                        try {
                            val prepared =
                                if (
                                    !selectedPlaylistId.isNullOrBlank() &&
                                    movieId.isNotBlank()
                                ) {
                                    preparationViewModel.setPlaylistForDetails(
                                        selectedPlaylistId
                                    )
                                    preparationViewModel.prepareMovieDetails(
                                        movieId = movieId,
                                        source = activeSource,
                                        context = context.applicationContext,
                                        viewportWidthPx = viewportWidthPx,
                                        viewportHeightPx = viewportHeightPx,
                                        detailsLoader = { source, requestedMovieId ->
                                            val generation = playlistCacheDao
                                                .observeActiveGenerationId(source.id).first()
                                            if (generation.isNullOrBlank()) {
                                                playlistRepository.loadMovieDetails(source, requestedMovieId)
                                            } else {
                                                CatalogDetailsWarmupCache.movieOrLoad(
                                                    playlistId = source.id,
                                                    generationId = generation,
                                                    movieId = requestedMovieId
                                                ) {
                                                    playlistRepository.loadMovieDetails(source, requestedMovieId)
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    null
                                }

                            // Evita piscadas mesmo quando Room e cache sao rapidos.
                            val elapsed = SystemClock.elapsedRealtime() - startedAt
                            delay(br.com.flaviotorres.iptvpremium.ui.movies.MovieOpeningPolicy.minimumWaitMs(
                                elapsed, prepared?.images?.backdropReady == true && prepared.images?.titleLogoReady == true
                            ))
                            currentCoroutineContext().ensureActive()

                            if (
                                prepared != null &&
                                prepared.movieId == movieId &&
                                prepared.playlistId == selectedPlaylistId &&
                                selectedPlaylistId == activePlaylistId
                            ) {
                                // Usa o poster remoto somente se o Room nao
                                // tiver capa. Mantemos a selecao sempre no carousel.
                                val chosen = loadingCarouselPosters.firstOrNull {
                                    it.movieId == movieId
                                }
                                if (chosen?.posterUrl.isNullOrBlank()) {
                                    val enrichedPoster = prepared.details?.posterUrl
                                        ?.takeIf { it.isNotBlank() }
                                        ?: prepared.movie.posterUrl?.takeIf { it.isNotBlank() }
                                    if (enrichedPoster != null) {
                                        loadingCarouselPosters =
                                            (listOf(CinematicCarouselPoster(
                                                movieId, prepared.movie.name, enrichedPoster
                                            )) + loadingCarouselPosters.filterNot {
                                                it.movieId == movieId ||
                                                        it.posterUrl == enrichedPoster
                                            }).take(MAX_CAROUSEL_POSTERS)
                                    }
                                }
                                // Dados e imagens preparados ANTES da navegacao.
                                preparedMovieDetails = prepared

                                Log.i(
                                    NAVIGATION_PERF_TAG,
                                    "Filme preparado: movieId=$movieId " +
                                            "duracaoMs=${SystemClock.elapsedRealtime() - startedAt} " +
                                            "backdropPronto=${prepared.images?.backdropReady} " +
                                            "logoPronto=${prepared.images?.titleLogoReady}"
                                )

                                // A tela continua ativa: primeiro desacelera o
                                // carrossel, centraliza e amplia o filme escolhido.
                                // So depois a navegacao e efetuada.
                                finishRequested = true
                            } else {
                                errorMessage =
                                    "Houve uma falha ao conectar o filme."
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            val elapsed = SystemClock.elapsedRealtime() - startedAt
                            if (elapsed < MIN_MOVIE_PREPARATION_MS) {
                                delay(MIN_MOVIE_PREPARATION_MS - elapsed)
                            }
                            currentCoroutineContext().ensureActive()
                            Log.e(
                                NAVIGATION_PERF_TAG,
                                "Falha na preparação do filme: movieId=$movieId",
                                error
                            )
                            errorMessage =
                                "Houve uma falha ao conectar o filme."
                        }
                    }

                    CinematicCarouselLoadingScreen(
                        posters = loadingCarouselPosters,
                        selectedMovieId = movieId,
                        finishRequested = finishRequested,
                        errorMessage = errorMessage,
                        onTransitionFinished = {
                            val prepared = preparedMovieDetails
                            if (prepared != null && prepared.movieId == movieId &&
                                prepared.playlistId == activePlaylistId
                            ) {
                                navController.navigate(
                                    AppDestination.MovieDetails.createRoute(movieId)
                                ) {
                                    popUpTo(entry.destination.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onRetry = {
                            finishRequested = false
                            errorMessage = null
                            retryCount++
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            /*
             * DETALHES DO FILME
             *
             * A rota transporta somente o movieId.
             *
             * O dado básico vem do Room e os metadados enriquecidos são
             * consultados sob demanda somente para o filme aberto.
             */
            composable(
                route = AppDestination.MovieDetails.route,
                enterTransition = { fadeIn(animationSpec = tween(380)) },
                popExitTransition = { androidx.compose.animation.ExitTransition.None }
            ) { backStackEntry ->
                BackHandler {
                    br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.setBusy(false)
                    navController.popBackStack()
                }
                val movieId =
                    backStackEntry.arguments
                        ?.getString("movieId")
                        .orEmpty()


                if (!br.com.flaviotorres.iptvpremium.ui.settings.ParentalItemAllowed(activePlaylistId, "MOVIES", movieId, playlistCatalogRepository) { navController.popBackStack() }) return@composable
                val activePlaylist =
                    playlists.firstOrNull {
                        it.id == activePlaylistId
                    }

                val movieDetailsViewModel: MoviesViewModel =
                    viewModel(
                        factory =
                            moviesViewModelFactory
                    )

                /*
                 * O ViewModel desta rota é independente do ViewModel da grade.
                 * Isso preserva o estado da tela de Filmes no back stack.
                 */
                LaunchedEffect(
                    activePlaylistId,
                    activePlaylist,
                    movieId
                ) {
                    val playlistId =
                        activePlaylistId

                    if (
                        playlistId != null &&
                        movieId.isNotBlank()
                    ) {
                        movieDetailsViewModel.setPlaylistForDetails(
                            playlistId
                        )

                        // O pacote foi preparado ANTES da navegacao. A rota nao
                        // repete consultas ao Room, Xtream ou TMDB se ele corresponde.
                        val prepared = preparedMovieDetails
                            ?.takeIf {
                                it.playlistId == playlistId &&
                                        it.movieId == movieId
                            }

                        if (prepared != null) {
                            movieDetailsViewModel.applyPreparedMovieDetails(prepared)
                        }
                        if (activePlaylist != null && prepared == null) {
                            // Retoma metadados apos o limite de entrada, sem bloquear a tela.
                            movieDetailsViewModel.loadMovieDetails(
                                movieId = movieId,
                                source = activePlaylist,
                                detailsLoader = {
                                        source,
                                        requestedMovieId ->

                                    val generation = playlistCacheDao
                                        .observeActiveGenerationId(source.id).first()
                                    if (generation.isNullOrBlank()) {
                                        playlistRepository.loadMovieDetails(source, requestedMovieId)
                                    } else {
                                        CatalogDetailsWarmupCache.movieOrLoad(
                                            playlistId = source.id,
                                            generationId = generation,
                                            movieId = requestedMovieId
                                        ) {
                                            playlistRepository.loadMovieDetails(source, requestedMovieId)
                                        }
                                    }
                                }
                            )
                        } else if (prepared == null) {
                            /*
                             * Se a configuração da fonte ainda não estiver disponível,
                             * preservamos pelo menos os dados básicos já existentes
                             * no Room.
                             */
                            movieDetailsViewModel.loadMovieDetails(
                                movieId = movieId
                            )
                        }
                    } else {
                        movieDetailsViewModel.clearMovieDetails()
                    }
                }

                val selectedMovie by
                movieDetailsViewModel.selectedMovie
                    .collectAsState()

                val selectedMovieDetails by
                movieDetailsViewModel
                    .selectedMovieDetails
                    .collectAsState()

                val isMovieDetailsLoading by
                movieDetailsViewModel
                    .isMovieDetailsLoading
                    .collectAsState()

                val movieDetailsError by
                movieDetailsViewModel
                    .movieDetailsError
                    .collectAsState()

                // A primeira composicao ja recebe os detalhes prontos, sem
                // depender do LaunchedEffect para mostrar a arte oficial.
                val routePrepared = preparedMovieDetails?.takeIf {
                    it.playlistId == activePlaylistId && it.movieId == movieId
                }
                val displayMovie = routePrepared?.movie ?: selectedMovie
                val displayDetails = routePrepared?.details ?: selectedMovieDetails
                LaunchedEffect(activePlaylistId, movieId, displayMovie?.name, displayDetails) {
                    val playlist = activePlaylistId ?: return@LaunchedEffect
                    val title = displayMovie?.name ?: return@LaunchedEffect
                    withContext(Dispatchers.IO) {
                        sharedLibrary.rememberExternal(playlist, movieId, ContentIdentity("movies", title,
                            displayDetails?.year ?: displayMovie?.year, displayDetails?.imdbId, displayDetails?.tmdbId))
                    }
                }


                /*
                 * Os dados enriquecidos têm prioridade quando o provedor os
                 * disponibiliza. Caso algum campo não venha no get_vod_info,
                 * usamos o valor básico já persistido no Room.
                 */
                val movieRating =
                    displayDetails?.rating
                        ?: displayMovie?.rating

                val movieRatingSource =
                    when (
                        displayDetails?.ratingSource
                    ) {
                        RatingSource.IMDB ->
                            MovieRatingSource.IMDB

                        RatingSource.TMDB ->
                            MovieRatingSource.TMDB

                        RatingSource.PROVIDER ->
                            MovieRatingSource.PROVIDER

                        null ->
                            displayMovie?.rating
                                ?.let {
                                    MovieRatingSource.PROVIDER
                                }
                    }

                val movieDurationSeconds =
                    displayDetails
                        ?.durationSeconds
                        ?: displayMovie
                            ?.durationSeconds
                            ?.coerceAtMost(
                                Int.MAX_VALUE.toLong()
                            )
                            ?.toInt()

                /*
                 * ELENCO DO FILME
                 *
                 * Quando o provedor entrega elenco estruturado, preservamos o ID
                 * real de cada pessoa. Se ele entregar apenas nomes, mantemos o
                 * fallback antigo para não perder o elenco já existente.
                 *
                 * A foto/personagem serão ligados visualmente no próximo passo,
                 * quando MovieCastMember passar a receber profileUrl/characterName.
                 */
                val movieCast =
                    displayDetails
                        ?.castMembers
                        .orEmpty()
                        .takeIf {
                            it.isNotEmpty()
                        }
                        ?.map { member ->
                            MovieCastMember(
                                id = member.id,
                                name = member.name,
                                profileUrl =
                                    member.profileUrl,
                                characterName =
                                    member.characterName
                            )
                        }
                        ?: displayDetails
                            ?.cast
                            .orEmpty()
                            .mapIndexed {
                                    index,
                                    name ->

                                MovieCastMember(
                                    id =
                                        "$movieId-$index",
                                    name =
                                        name
                                )
                            }

                if (displayMovie != null) {
                    MovieDetailsScreen(
                        ratingPlaylistId = activePlaylistId,
                        ratingContentId = movieId,
                        ratingIdentityTitle = displayMovie?.name,
                        ratingIdentityYear = displayMovie?.year,
                        tmdbId = displayDetails?.tmdbId,
                        imdbId = displayDetails?.imdbId,
                        movieName =
                            displayDetails?.localizedTitle ?: displayMovie?.name
                                ?: if (isMovieDetailsLoading) {
                                    "Carregando..."
                                } else {
                                    "Filme"
                                },
                        backdropUrl =
                            displayDetails
                                ?.backdropUrl
                                ?.takeIf { it.isNotBlank() }
                                ?: displayMovie
                                    ?.backdropUrl
                                    ?.takeIf { it.isNotBlank() },
                        titleLogoUrl =
                            displayDetails
                                ?.titleLogoUrl
                                ?.takeIf { it.isNotBlank() },
                        rating =
                            movieRating,
                        ratingSource =
                            movieRatingSource,
                        year =
                            displayDetails
                                ?.year
                                ?: displayMovie
                                    ?.year,
                        durationSeconds =
                            movieDurationSeconds,
                        genres =
                            displayDetails
                                ?.genres
                                .orEmpty(),
                        plot =
                            displayDetails
                                ?.plot,
                        cast =
                            movieCast,
                        isFavorite =
                            movieId in favoriteMovieIds,
                        onWatchClick = {
                            if (activePlaylistId.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    "Selecione uma playlist antes de reproduzir.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // A mesma URL oficial exibida nos detalhes do filme.
                                // Sem logo, PlaybackScreen mostrara o cartao existente.
                                movieDurationForPlayback = movieDurationSeconds?.takeIf { it > 0 }
                                    ?.let { Triple(activePlaylistId.orEmpty(), movieId, it.toLong() * 1_000L) }
                                movieBackdropForPlayback = (displayDetails?.backdropUrl
                                    ?.takeIf { it.isNotBlank() }
                                    ?: displayMovie?.backdropUrl?.takeIf { it.isNotBlank() })
                                    ?.let { Triple(activePlaylistId.orEmpty(), movieId, it) }
                                movieTitleLogoForPlayback = displayDetails
                                    ?.titleLogoUrl
                                    ?.trim()
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { logoUrl ->
                                        Triple(activePlaylistId.orEmpty(), movieId, logoUrl)
                                    }
                                navController.navigate(
                                    AppDestination.Playback.createRoute(
                                        contentType = PlaybackContentType.MOVIE,
                                        contentId = movieId
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onTrailerClick = {
                            br.com.flaviotorres.iptvpremium.core.media.openTrailer(context,
                                displayDetails?.trailerUrl, displayMovie?.name.orEmpty(),
                                tmdbId = displayDetails?.tmdbId, year = displayMovie?.year)
                        },
                        onFavoriteClick = {
                            toggleSharedFavorite("movies", movieId)
                        },
                        onCastMemberClick = { member ->
                            // Somente IDs numericos reais de pessoa do TMDB.
                            // O fallback de nomes usa "$movieId-indice": nao e um ID de pessoa.
                            val personId = member.id.trim().toIntOrNull()?.takeIf { it > 0 }
                            if (personId != null) {
                                selectedActorForDetails = member
                                navController.navigate("actor_details/$personId") {
                                    launchSingleTop = true
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Dados deste ator indisponíveis no TMDB.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                movieDetailsError
            }

            /*
             * REPRODUCAO DE FILMES, EPISODIOS E CANAIS — MEDIA3 / VLC
             *
             * Apenas tipo e ID passam pela rota, nunca a URL com credenciais.
             * Filmes e canais sao consultados por ID na geracao ACTIVE do Room.
             * O fluxo existente de episodios permanece preservado.
             */
            composable(
                route = AppDestination.Playback.route
            ) { entry ->
                val contentType = entry.arguments
                    ?.getString("contentType")
                    .orEmpty()
                // Navigation ja decodifica os argumentos inseridos via Uri.encode.
                val contentId = entry.arguments
                    ?.getString("contentId")
                    .orEmpty()
                val currentPlaylistId = activePlaylistId

                // CH+ / CH- atualizam somente a midia ao vivo neste destino.
                // Nenhuma nova rota e empilhada, mantendo Voltar para o catalogo.
                val liveChannelSwitchScope = rememberCoroutineScope()
                var liveChannelOverride by remember(
                    contentType, contentId, currentPlaylistId
                ) { mutableStateOf<PlaybackMedia?>(null) }
                var liveChannelSwitchBusy by remember(
                    contentType, contentId, currentPlaylistId
                ) { mutableStateOf(false) }

                val parentalPlaybackType = when (contentType) {
                    PlaybackContentType.MOVIE.name -> "MOVIES"
                    PlaybackContentType.EPISODE.name -> "SERIES"
                    else -> "LIVE_TV"
                }
                val parentalPlaybackId = if (parentalPlaybackType == "SERIES") selectedEpisodeSeriesId
                    else if (parentalPlaybackType == "LIVE_TV") liveChannelOverride?.id ?: contentId else contentId
                if (!br.com.flaviotorres.iptvpremium.ui.settings.ParentalItemAllowed(currentPlaylistId, parentalPlaybackType,
                        parentalPlaybackId, playlistCatalogRepository) { navController.popBackStack() }) return@composable

                val playbackRouteState by produceState<MoviePlaybackRouteState>(
                    initialValue = MoviePlaybackRouteState.Loading,
                    key1 = contentType,
                    key2 = contentId,
                    key3 = currentPlaylistId
                ) {
                    value = MoviePlaybackRouteState.Loading

                    if (contentType != PlaybackContentType.MOVIE.name &&
                        contentType != PlaybackContentType.EPISODE.name &&
                        contentType != PlaybackContentType.LIVE_CHANNEL.name) {
                        value = MoviePlaybackRouteState.Error(
                            "Este tipo de conteúdo ainda não é suportado pelo player."
                        )
                    } else if (currentPlaylistId.isNullOrBlank()) {
                        value = MoviePlaybackRouteState.Error(
                            "Nenhuma playlist ativa foi encontrada."
                        )
                    } else if (contentId.isBlank()) {
                        value = MoviePlaybackRouteState.Error(
                            "O identificador do conteúdo está vazio."
                        )
                    } else {
                        value = try {
                            if (contentType == PlaybackContentType.MOVIE.name) {
                                // Fluxo de filmes preservado, sem alteracoes.
                                val movie = withContext(Dispatchers.IO) {
                                    playlistCatalogRepository.getMovieById(
                                        playlistId = currentPlaylistId,
                                        movieId = contentId
                                    )
                                }
                                when {
                                    movie == null -> MoviePlaybackRouteState.Error(
                                        "Este filme nao foi encontrado no catalogo ativo."
                                    )
                                    movie.streamUrl.isBlank() -> MoviePlaybackRouteState.Error(
                                        "Este filme nao possui um endereco de reproducao."
                                    )
                                    else -> MoviePlaybackRouteState.Ready(movie.toPlaybackMedia())
                                }
                            } else if (contentType == PlaybackContentType.LIVE_CHANNEL.name) {
                                // Consulta somente a geracao ACTIVE, sem carregar paginas
                                // do catalogo nem passar a URL pela rota de navegacao.
                                val channel = withContext(Dispatchers.IO) {
                                    playlistCatalogRepository.getChannelById(
                                        playlistId = currentPlaylistId,
                                        channelId = contentId
                                    )
                                }
                                when {
                                    channel == null -> MoviePlaybackRouteState.Error(
                                        "Este canal não foi encontrado no catálogo ativo."
                                    )
                                    channel.streamUrl.isBlank() -> MoviePlaybackRouteState.Error(
                                        "Este canal não possui um endereço de reprodução."
                                    )
                                    else -> MoviePlaybackRouteState.Ready(channel.toPlaybackMedia())
                                }
                            } else {
                                // Episodios: fluxo existente preservado.
                                val selectedEpisode = selectedEpisodeToPlay
                                    ?.takeIf { it.id == contentId }
                                val seriesId = selectedEpisodeSeriesId
                                if (selectedEpisode == null || seriesId.isNullOrBlank()) {
                                    MoviePlaybackRouteState.Error(
                                        "Selecione o episódio pela tela de detalhes da série."
                                    )
                                } else {
                                    val currentSource = playlists.firstOrNull {
                                        it.id == currentPlaylistId
                                    }
                                    // URLs importadas de M3U têm prioridade; Xtream usa
                                    // o ID real e a extensão informada pelo provedor.
                                    val cachedStreamUrl = withContext(Dispatchers.IO) {
                                        val generation = playlistCacheDao
                                            .getActiveGeneration(currentPlaylistId)?.generationId
                                        if (generation.isNullOrBlank()) null else {
                                            var offset = 0
                                            var stream: String? = null
                                            while (offset < 5_000 && stream == null) {
                                                val records = playlistCacheDao.getEpisodesPage(
                                                    playlistId = currentPlaylistId,
                                                    generationId = generation,
                                                    seriesId = seriesId,
                                                    limit = 200,
                                                    offset = offset
                                                )
                                                stream = records.firstOrNull {
                                                    it.episodeId == selectedEpisode.id
                                                }?.streamUrl?.takeIf { it.isNotBlank() }
                                                if (records.size < 200) break
                                                offset += records.size
                                            }
                                            stream
                                        }
                                    }
                                    val xtream = when (currentSource) {
                                        is PlaylistSource.XtreamCodes -> currentSource
                                        is PlaylistSource.M3u -> M3uXtreamResolver.resolve(currentSource)
                                        null -> null
                                    }
                                    val extension = selectedEpisode.containerExtension
                                        ?.trim()?.lowercase(Locale.ROOT)
                                        ?.takeIf { it.matches(Regex("[a-z0-9]{2,5}")) }
                                        ?: "mp4"
                                    val xtreamStreamUrl = xtream?.let { credentials ->
                                        val baseUrl = credentials.serverUrl.trim().trimEnd('/')
                                            .let { url ->
                                                if (url.startsWith("http://", true) ||
                                                    url.startsWith("https://", true)) url
                                                else "http://$url"
                                            }
                                        "$baseUrl/series/" +
                                                "${android.net.Uri.encode(credentials.username)}/" +
                                                "${android.net.Uri.encode(credentials.password)}/" +
                                                "${android.net.Uri.encode(selectedEpisode.id)}.$extension"
                                    }
                                    val streamUrl = cachedStreamUrl ?: xtreamStreamUrl
                                    if (streamUrl.isNullOrBlank()) {
                                        MoviePlaybackRouteState.Error(
                                            "Não existe um endereço de reprodução disponível para este episódio."
                                        )
                                    } else {
                                        // Nunca usar o título do episódio como título da série.
                                        val identity = selectedEpisodeSubtitleIdentity?.takeIf {
                                            it.playlistId == currentPlaylistId && it.seriesId == seriesId
                                        }
                                        val selectedSeries = selectedSeriesForDetails?.takeIf {
                                            it.id == seriesId
                                        }
                                        val loadedSeries = lastSeriesDetailsResult?.takeIf {
                                            it.first == currentPlaylistId && it.second == seriesId
                                        }?.third
                                        val seriesTitle = listOfNotNull(
                                            identity?.seriesTitle,
                                            selectedSeries?.name,
                                            loadedSeries?.title
                                        ).map { it.trim() }.firstOrNull { it.isNotBlank() }
                                        val subtitleSeriesTitle = seriesTitle?.let(::cleanSubtitleSeriesTitle)
                                            ?.takeIf { it.isNotBlank() }
                                        val seriesYear = identity?.year ?: selectedSeries?.year
                                        val validProviderTmdbId = listOfNotNull(
                                            identity?.tmdbId,
                                            selectedSeries?.tmdbId
                                        ).firstOrNull { it.toLongOrNull()?.let { id -> id > 0 } == true }

                                        val tmdbCacheKey = "$currentPlaylistId|$seriesId"
                                        val resolvedTmdbId = validProviderTmdbId
                                            ?: episodeTmdbIds[tmdbCacheKey]
                                            ?: subtitleSeriesTitle?.let { title ->
                                                // Falha/timeout não impedem a reprodução.
                                                withTimeoutOrNull(5_000L) {
                                                    resolveEpisodeSeriesTmdbId(title, seriesYear)
                                                }
                                            }
                                        if (resolvedTmdbId != null) {
                                            episodeTmdbIds[tmdbCacheKey] = resolvedTmdbId
                                        }

                                        Log.d(
                                            "SubdlIdentity",
                                            "Episódio preparado: série=${!subtitleSeriesTitle.isNullOrBlank()}, " +
                                                    "tmdb=${resolvedTmdbId != null}, " +
                                                    "temporada=${selectedEpisode.seasonNumber}, " +
                                                    "episódio=${selectedEpisode.episodeNumber}"
                                        )
                                        MoviePlaybackRouteState.Ready(
                                            PlaybackMedia(
                                                id = selectedEpisode.id,
                                                type = PlaybackContentType.EPISODE,
                                                title = selectedEpisode.title,
                                                streamUrl = streamUrl,
                                                posterUrl = selectedEpisode.thumbnailUrl,
                                                durationMs = selectedEpisode.durationSeconds
                                                    ?.toLong()?.times(1_000L),
                                                seriesId = seriesId,
                                                seasonNumber = selectedEpisode.seasonNumber,
                                                episodeNumber = selectedEpisode.episodeNumber,
                                                subtitleSearchTitle = subtitleSeriesTitle,
                                                tmdbId = resolvedTmdbId,
                                                year = seriesYear
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Excecoes podem conter enderecos Xtream com credenciais.
                            MoviePlaybackRouteState.Error(
                                "Não foi possível preparar a reprodução deste conteúdo."
                            )
                        }
                    }
                }

                when (val state = playbackRouteState) {
                    MoviePlaybackRouteState.Loading -> {
                        BackHandler { navController.popBackStack() }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Preparando reproducao...",
                                color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD4AF37),
                                fontSize = 18.sp
                            )
                        }
                    }
                    is MoviePlaybackRouteState.Error -> {
                        BackHandler { navController.popBackStack() }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.message,
                                color = Color.White,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD4AF37))
                                    .clickable { navController.popBackStack() }
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Voltar",
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is MoviePlaybackRouteState.Ready -> {
                        // Filmes e episodios usam a midia original sem alteracoes.
                        // Somente canais podem ser substituidos em tempo real.
                        val media = if (state.media.type == PlaybackContentType.LIVE_CHANNEL) {
                            liveChannelOverride ?: state.media
                        } else {
                            state.media
                        }
                        val seriesId = media.seriesId
                        var measuredDurationMs by remember(media.id, media.type, media.streamUrl) {
                            mutableStateOf<Long?>(null)
                        }
                        var episodeSwitchBusy by remember(entry) { mutableStateOf(false) }
                        fun changeEpisode(direction: Int, positionMs: Long, completed: Boolean) {
                            if (episodeSwitchBusy || direction !in listOf(-1, 1) ||
                                currentPlaylistId.isNullOrBlank() || seriesId.isNullOrBlank()) return
                            episodeSwitchBusy = true
                            liveChannelSwitchScope.launch {
                                try {
                                    val source = playlists.firstOrNull { it.id == currentPlaylistId }
                                    val result = lastSeriesDetailsResult?.takeIf {
                                        it.first == currentPlaylistId && it.second == seriesId
                                    }?.third ?: source?.let {
                                        withContext(Dispatchers.IO) {
                                            loadCachedSeriesEpisodes(playlistCacheDao, currentPlaylistId, seriesId)
                                                ?: seriesEpisodesRepository.load(it, seriesId)
                                        }
                                    }
                                    if (navController.currentBackStackEntry != entry || activePlaylistId != currentPlaylistId) return@launch
                                    if (completed) {
                                        seriesResumeStore.clearPosition(currentPlaylistId, seriesId, media.id)
                                        watchHistoryStore.remove(currentPlaylistId, media.type, media.id)
                                        historyRefreshSerial++
                                    }
                                    val next = br.com.flaviotorres.iptvpremium.core.player.adjacentSeriesEpisode(
                                        result?.episodes.orEmpty(), media.id, direction
                                    )
                                    if (next == null) {
                                        Toast.makeText(context, if (result == null) "Não foi possível consultar os episódios"
                                            else if (direction < 0) "Não há episódio anterior no catálogo desta série"
                                            else "Não há próximo episódio no catálogo desta série", Toast.LENGTH_LONG).show()
                                    } else {
                                        if (!completed) {
                                            seriesResumeStore.savePosition(currentPlaylistId, seriesId, media.id, positionMs)
                                            if (positionMs >= 10_000L) {
                                                watchHistoryStore.save(currentPlaylistId, media, positionMs, measuredDurationMs ?: media.durationMs,
                                                    selectedSeriesForDetails?.takeIf { it.id == seriesId }?.name,
                                                    selectedSeriesForDetails?.takeIf { it.id == seriesId }?.posterUrl)
                                                historyRefreshSerial++
                                            }
                                        }
                                        lastSeriesDetailsResult = Triple(currentPlaylistId, seriesId, result!!)
                                        selectedEpisodeToPlay = next
                                        selectedEpisodeSeriesId = seriesId
                                        autoNextEpisodeId = next.id
                                        seriesResumeStore.rememberEpisode(currentPlaylistId, seriesId, next.id)
                                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                                            set(SERIES_CURRENT_SEASON_KEY, next.seasonNumber)
                                            set(SERIES_LAST_EPISODE_KEY, next.id)
                                            set(SERIES_RETURN_FROM_PLAYER_KEY, true)
                                        }
                                        navController.navigate(AppDestination.Playback.createRoute(PlaybackContentType.EPISODE, next.id)) {
                                            popUpTo(entry.destination.id) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) {
                                    Toast.makeText(context, "Não foi possível abrir o episódio", Toast.LENGTH_LONG).show()
                                } finally { episodeSwitchBusy = false }
                            }
                        }
                        // Lido apenas na primeira composicao dessa rota. A janela de
                        // escolha nao reaparece durante recomposicoes do player.
                        val savedStartMs = remember(
                            currentPlaylistId, media.id, media.type, seriesId
                        ) {
                            if (currentPlaylistId.isNullOrBlank()) 0L else {
                                val history = watchHistoryStore.all(currentPlaylistId).firstOrNull {
                                    it.contentType == media.type && it.contentId == media.id && !it.seriesOnly
                                }
                                val position = history?.positionMs ?: when (media.type) {
                                    PlaybackContentType.MOVIE -> movieResumeStore.getPosition(currentPlaylistId, media.id)
                                    PlaybackContentType.EPISODE -> if (!seriesId.isNullOrBlank()) seriesResumeStore.getPosition(currentPlaylistId, seriesId, media.id) else 0L
                                    PlaybackContentType.LIVE_CHANNEL -> 0L
                                }
                                SharedLibraryStore.resumePosition(history, position, media.durationMs)
                            }
                        }
                        // null = ainda sem escolha; 0 = iniciar novamente.
                        var selectedStartMs by remember(
                            currentPlaylistId, media.id, media.type, seriesId
                        ) { mutableStateOf<Long?>(if (autoNextEpisodeId == media.id && media.type == PlaybackContentType.EPISODE) 0L else null) }
                        LaunchedEffect(media.id, media.type) {
                            if (autoNextEpisodeId == media.id) autoNextEpisodeId = null
                        }
                        // Oferece retomada so com pelo menos 10s de progresso, e
                        // evita exibir a escolha perto do fim quando ha duracao.
                        val hasResume = media.type != PlaybackContentType.LIVE_CHANNEL &&
                                savedStartMs >= 10_000L &&
                                (media.durationMs == null || media.durationMs <= 0L ||
                                        savedStartMs < media.durationMs - 20_000L)

                        if (hasResume && selectedStartMs == null) {
                            br.com.flaviotorres.iptvpremium.core.player.PlaybackResumeCinema(
                                media = media,
                                backgroundUrl = if (media.type == PlaybackContentType.EPISODE) {
                                    lastSeriesDetailsResult?.takeIf {
                                        it.first == currentPlaylistId && it.second == seriesId
                                    }?.third?.backdropUrl ?: selectedSeriesForDetails?.takeIf {
                                        it.id == seriesId
                                    }?.backdropUrl ?: media.backdropUrl
                                } else movieBackdropForPlayback?.takeIf {
                                    it.first == currentPlaylistId && it.second == media.id
                                }?.third ?: preparedMovieDetails?.takeIf {
                                    it.playlistId == currentPlaylistId && it.movieId == media.id
                                }?.details?.backdropUrl?.takeIf { it.isNotBlank() }
                                    ?: media.backdropUrl,
                                durationMs = if (media.type == PlaybackContentType.MOVIE) {
                                    movieDurationForPlayback?.takeIf {
                                        it.first == currentPlaylistId && it.second == media.id
                                    }?.third ?: media.durationMs
                                } else media.durationMs,
                                seriesTitle = if (media.type == PlaybackContentType.EPISODE) {
                                    selectedSeriesForDetails?.takeIf { it.id == seriesId }?.name
                                        ?: lastSeriesDetailsResult?.takeIf {
                                            it.first == currentPlaylistId && it.second == seriesId
                                        }?.third?.title ?: media.subtitleSearchTitle
                                } else null,
                                positionMs = savedStartMs,
                                onContinue = { selectedStartMs = savedStartMs },
                                onRestart = {
                                    if (!currentPlaylistId.isNullOrBlank()) {
                                        when (media.type) {
                                            PlaybackContentType.MOVIE -> movieResumeStore.clearPosition(
                                                currentPlaylistId, media.id
                                            )
                                            PlaybackContentType.EPISODE -> {
                                                if (!seriesId.isNullOrBlank()) {
                                                    seriesResumeStore.clearPosition(
                                                        currentPlaylistId, seriesId, media.id
                                                    )
                                                }
                                            }
                                            PlaybackContentType.LIVE_CHANNEL -> Unit
                                        }
                                    }
                                    if (!currentPlaylistId.isNullOrBlank()) {
                                        watchHistoryStore.remove(currentPlaylistId, media.type, media.id)
                                        historyRefreshSerial++
                                    }
                                    selectedStartMs = 0L
                                },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            PlaybackScreen(
                                media = media,
                                onDurationKnown = { measuredDurationMs = it },
                                // Filmes usam a arte resolvida nos detalhes; o pacote
                                // preparado serve como fallback sem acessar a rede.
                                // Series usam o logo de seu proprio catalogo.
                                // Canais continuam usando apenas o cartao AO VIVO.
                                titleLogoUrl = when (media.type) {
                                    PlaybackContentType.MOVIE ->
                                        movieTitleLogoForPlayback?.takeIf {
                                            it.first == currentPlaylistId && it.second == media.id
                                        }?.third ?: preparedMovieDetails?.takeIf {
                                            it.playlistId == currentPlaylistId && it.movieId == media.id
                                        }?.details?.titleLogoUrl
                                    PlaybackContentType.EPISODE ->
                                        selectedSeriesForDetails?.takeIf {
                                            it.id == seriesId
                                        }?.titleLogoUrl
                                    PlaybackContentType.LIVE_CHANNEL -> null
                                },
                                startPositionMs = selectedStartMs ?: when (media.type) {
                                    // Mantem para series a retomada ja validada,
                                    // inclusive para posicoes menores que 10s.
                                    PlaybackContentType.EPISODE -> savedStartMs
                                    PlaybackContentType.MOVIE -> 0L
                                    PlaybackContentType.LIVE_CHANNEL -> 0L
                                },
                                onExit = { positionMs ->
                                    if (!currentPlaylistId.isNullOrBlank()) {
                                        if (positionMs >= 10_000L) {
                                            watchHistoryStore.save(
                                                playlistId = currentPlaylistId,
                                                media = media,
                                                positionMs = positionMs,
                                                durationMs = measuredDurationMs ?: media.durationMs,
                                                seriesTitle = if (media.type == PlaybackContentType.EPISODE)
                                                    selectedSeriesForDetails?.takeIf { it.id == seriesId }?.name else null,
                                                seriesPosterUrl = if (media.type == PlaybackContentType.EPISODE)
                                                    selectedSeriesForDetails?.takeIf { it.id == seriesId }?.posterUrl else null
                                            )
                                            historyRefreshSerial++
                                        }
                                        when (media.type) {
                                            PlaybackContentType.EPISODE -> {
                                                if (!seriesId.isNullOrBlank()) {
                                                    seriesResumeStore.savePosition(
                                                        currentPlaylistId,
                                                        seriesId,
                                                        media.id,
                                                        positionMs
                                                    )
                                                }
                                            }
                                            PlaybackContentType.MOVIE -> {
                                                movieResumeStore.savePosition(
                                                    currentPlaylistId,
                                                    media.id,
                                                    positionMs
                                                )
                                            }
                                            PlaybackContentType.LIVE_CHANNEL -> Unit
                                        }
                                    }
                                    navController.popBackStack()
                                },
                                onPlaybackEnded = if (media.type == PlaybackContentType.EPISODE) {
                                    { changeEpisode(1, 0L, completed = true) }
                                } else null,
                                onChangeEpisode = if (media.type == PlaybackContentType.EPISODE) {
                                    { direction, positionMs -> changeEpisode(direction, positionMs, completed = false) }
                                } else null,
                                onChangeLiveChannel = if (media.type == PlaybackContentType.LIVE_CHANNEL) {
                                    { direction: Int ->
                                        // Uma consulta de cada vez: evita abrir diversas
                                        // transmissões ao pressionar CH repetidamente.
                                        if (!liveChannelSwitchBusy && (direction == 1 || direction == -1)) {
                                            liveChannelSwitchBusy = true
                                            liveChannelSwitchScope.launch {
                                                try {
                                                    val nextChannel = withContext(Dispatchers.IO) {
                                                        playlistCatalogRepository.getAdjacentChannel(
                                                            playlistId = currentPlaylistId.orEmpty(),
                                                            currentChannelId = (liveChannelOverride ?: state.media).id,
                                                            direction = direction
                                                        )
                                                    }
                                                    if (nextChannel != null && nextChannel.streamUrl.isNotBlank()) {
                                                        // PlaybackScreen observa o novo media.id
                                                        // e usa o mesmo PlaybackController/SurfaceView.
                                                        liveChannelOverride = nextChannel.toPlaybackMedia()
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Nenhum outro canal disponível.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } catch (cancelled: CancellationException) {
                                                    throw cancelled
                                                } catch (_: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Não foi possível trocar de canal.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } finally {
                                                    liveChannelSwitchBusy = false
                                                }
                                            }
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            /*
             * DETALHES DO ATOR
             *
             * Os creditos do TMDB permanecem informativos. Um clique em Filme
             * consulta apenas a geracao ACTIVE da playlist, sem percorrer
             * todo o catalogo ou confundir ID do TMDB com ID do provedor.
             * Filmes nao encontrados nao abrem uma tela com dados errados.
             * Series continuam informativas ate a integracao do catalogo real.
             */
            composable(
                route = ACTOR_DETAILS_ROUTE
            ) { entry ->
                if (br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value ||
                    (parentalAccessState.enabled && parentalAccessState.rules.kidsOnly)) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                val personId = entry.arguments
                    ?.getString("personId")
                    .orEmpty()
                val preview = selectedActorForDetails?.takeIf {
                    it.id == personId
                }
                val actorMovieLookupScope = rememberCoroutineScope()
                var openingActorMovie by remember(personId, activePlaylistId) {
                    mutableStateOf(false)
                }
                // Correspondencia segura do credito TMDB com o movieId da playlist.
                // Apenas IDs confirmados podem ser utilizados para favoritos.
                val actorMovieIdMatches = remember(personId, activePlaylistId) {
                    mutableStateMapOf<Int, String>()
                }
                val favoriteLookupInProgress = remember(personId, activePlaylistId) {
                    mutableStateMapOf<Int, Boolean>()
                }

                ActorDetailsScreen(
                    personTmdbId = personId,
                    initialName = preview?.name.orEmpty(),
                    initialProfileUrl = preview?.profileUrl,
                    onBackClick = { navController.popBackStack() },
                    onMovieSelected = { credit ->
                        val playlistId = activePlaylistId
                        if (!openingActorMovie) {
                            if (playlistId.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    "Selecione uma playlist antes de abrir o filme.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                openingActorMovie = true
                                actorMovieLookupScope.launch {
                                    try {
                                        val foundMovieId = actorMovieIdMatches[credit.tmdbId]
                                            ?: findMovieIdForActorCredit(
                                                cacheDao = playlistCacheDao,
                                                playlistId = playlistId,
                                                credit = credit
                                            )
                                        if (activePlaylistId == playlistId) {
                                            if (foundMovieId != null) {
                                                actorMovieIdMatches[credit.tmdbId] = foundMovieId
                                                // Entrada direta: nao altera o carrossel aprovado.
                                                preparedMovieDetails = null
                                                navController.navigate(
                                                    AppDestination.MovieDetails.createRoute(foundMovieId)
                                                ) { launchSingleTop = true }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Este filme não foi encontrado na playlist ativa.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        Log.w(NAVIGATION_PERF_TAG, "Erro ao vincular filme do ator", error)
                                        Toast.makeText(
                                            context,
                                            "Não foi possível verificar este filme. Tente novamente.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        openingActorMovie = false
                                    }
                                }
                            }
                        }
                    },
                    // O favorito usa somente o ID do filme do provedor, nunca o
                    // credito do TMDB. O estado se atualiza apos a identificacao.
                    isMovieFavorite = { credit ->
                        val linkedId = actorMovieIdMatches[credit.tmdbId]
                        linkedId != null && linkedId in favoriteMovieIds
                    },
                    // A identificacao ocorre ANTES de abrir a previa do filme,
                    // em paralelo com os dados e as imagens, sem baixar o catalogo.
                    // Assim, se ja for favorito, o coracao abre preenchido.
                    // Se nao existir correspondencia, a previa segue informativa.
                    onPrepareMoviePreview = { credit ->
                        val playlistId = activePlaylistId
                        if (
                            !playlistId.isNullOrBlank() &&
                            credit.mediaType == "movie" &&
                            !actorMovieIdMatches.containsKey(credit.tmdbId)
                        ) {
                            val foundMovieId = findMovieIdForActorCredit(
                                cacheDao = playlistCacheDao,
                                playlistId = playlistId,
                                credit = credit
                            )
                            // Nao associar resultado de uma playlist antiga.
                            if (activePlaylistId == playlistId && foundMovieId != null) {
                                actorMovieIdMatches[credit.tmdbId] = foundMovieId
                            }
                        }
                    },
                    onToggleMovieFavorite = { credit ->
                        val playlistId = activePlaylistId
                        when {
                            playlistId.isNullOrBlank() -> {
                                Toast.makeText(
                                    context,
                                    "Selecione uma playlist para favoritar.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            favoriteLookupInProgress[credit.tmdbId] == true -> Unit
                            else -> {
                                favoriteLookupInProgress[credit.tmdbId] = true
                                actorMovieLookupScope.launch {
                                    try {
                                        val foundMovieId = actorMovieIdMatches[credit.tmdbId]
                                            ?: findMovieIdForActorCredit(
                                                cacheDao = playlistCacheDao,
                                                playlistId = playlistId,
                                                credit = credit
                                            )
                                        if (activePlaylistId == playlistId) {
                                            if (foundMovieId == null) {
                                                Toast.makeText(
                                                    context,
                                                    "Filme não encontrado na playlist ativa.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                // Antes da primeira correspondencia a previa nao
                                                // sabe se o item ja era favorito. Se era, revelar
                                                // o estado real sem desfavoritar por engano.
                                                val matchedBefore =
                                                    actorMovieIdMatches.containsKey(credit.tmdbId)
                                                val alreadyFavorite = foundMovieId in favoriteMovieIds
                                                actorMovieIdMatches[credit.tmdbId] = foundMovieId
                                                if (matchedBefore || !alreadyFavorite) {
                                                    toggleSharedFavorite("movies", foundMovieId)
                                                }
                                            }
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        Log.w(NAVIGATION_PERF_TAG, "Falha ao favoritar filme do ator", error)
                                        Toast.makeText(
                                            context,
                                            "Não foi possível atualizar o favorito.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        favoriteLookupInProgress.remove(credit.tmdbId)
                                    }
                                }
                            }
                        }
                    }
                    // Series ainda usam catalogo demonstrativo: nao associamos
                    // indevidamente IDs de TMDB aos IDs de series do provedor.
                )
            }

            /*
             * SÉRIES REAIS — ROOM + PAGING 3
             *
             * A fonte da tela é SEMPRE o catalogo local. Mesmo sem playlist
             * ativa, passamos um PagingData vazio; nunca mostramos prototipos.
             * A lista e as categorias preservam providerOrder no Repository.
             */
            composable(route = AppDestination.Series.route,
                popEnterTransition = { androidx.compose.animation.EnterTransition.None }) {
                if (!br.com.flaviotorres.iptvpremium.ui.kids.KidsRouteAllowed { navController.popBackStack() }) return@composable
                val seriesViewModel: SeriesViewModel = retainedSeries

                LaunchedEffect(activePlaylistId, catalogGeneration, parentalAccessState) {
                    val playlistId = activePlaylistId
                    if (playlistId.isNullOrBlank()) {
                        seriesViewModel.clear()
                        catalogSessions.clear("series")
                    } else if (catalogGeneration != null) {
                        catalogSessions.bind("series", playlistId, catalogGeneration, parentalAccessState,
                            { seriesViewModel.setPlaylist(playlistId) }, { seriesViewModel.refreshMetadata() })
                        if (seriesViewModel.metadataError.value != null) seriesViewModel.refreshMetadata()
                    }
                }

                // Mesma sincronização de favoritos usada na rota de Filmes.
                LaunchedEffect(seriesViewModel, favoriteSeriesIds) {
                    seriesViewModel.setFavoriteSeriesIds(favoriteSeriesIds)
                }


                DisposableEffect(seriesViewModel) {
                    seriesViewModel.setCatalogVisible(true)
                    onDispose { seriesViewModel.setCatalogVisible(false) }
                }
                val seriesSelectionRequired by seriesViewModel.selectionRequired.collectAsState()
                val seriesCategories by seriesViewModel.categories.collectAsState()
                val totalSeriesCount by seriesViewModel.seriesCount.collectAsState()
                val seriesCountsByCategory by seriesViewModel.categoryCounts.collectAsState()
                val pagedSeries = seriesViewModel.series.collectAsLazyPagingItems()

                // Capas tem prioridade; logos, fundos e episodios ficam para a abertura.
                SeriesScreen(
                    onResolvePosterCategory = { id ->
                        val playlist=activePlaylistId
                        if(playlist==null)null else {
                            val item=playlistCatalogRepository.getSeriesById(playlist,id)
                            val category=seriesCategories.firstOrNull{it.id==item?.categoryId}?.name ?:
                                playlistCatalogRepository.getSeriesCategories(playlist).firstOrNull{it.id==item?.categoryId}?.name
                            category.orEmpty()+" "+item?.genres.orEmpty().joinToString(" ")
                        }
                    },
                    posterPage = { seriesViewModel.posterPage(it) },
                    onCategoryAccessRequested = { id, title, proceed -> requestParentalCategory("SERIES", id, title, proceed) },
                    watchHistoryEntries = watchHistoryEntries,
                    pagedSeries = pagedSeries,
                    pagingCategories = seriesCategories,
                    categorySelectionRequired = seriesSelectionRequired,
                    totalSeriesCount = totalSeriesCount,
                    pagingCategoryCounts = seriesCountsByCategory,
                    onSourceCategorySelected = { categoryId ->
                        seriesViewModel.selectCategory(categoryId)
                    },
                    onFavoritesSelected = {
                        seriesViewModel.selectFavorites()
                    },
                    onSearchQueryChanged = { query ->
                        seriesViewModel.setSearchQuery(query)
                    },
                    onSortModeChanged = { mode ->
                        seriesViewModel.setSortMode(mode)
                    },
                    favoriteSeriesIds = favoriteSeriesIds,
                    onSeriesClick = { seriesId ->
                        br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.setBusy(true)
                        // Somente a serie selecionada, ja carregada pelo Paging.
                        // Nenhum SELECT extra nem reconstrução de toda a lista.
                        selectedSeriesForDetails = pagedSeries.itemSnapshotList.items
                            .firstOrNull { it.id == seriesId }
                        // A tela de detalhes e dona da abertura. O foco ja iniciado
                        // e reutilizado pelo cache; nao cria outra tarefa solta no clique.
                        navController.navigate(
                            AppDestination.SeriesDetails.createRoute(seriesId)
                        ) { launchSingleTop = true }
                    },
                    onFavoriteClick = { seriesId ->
                        toggleSharedFavorite("series", seriesId)
                    }
                )
            }

            /*
             * DETALHES DE SERIES — consulta somente a serie selecionada.
             * A lista de temporadas e episodios vem exclusivamente do provedor,
             * sem IDs inventados nem requisicao do catalogo completo.
             */
            composable(route = AppDestination.SeriesDetails.route,
                popExitTransition = { androidx.compose.animation.ExitTransition.None }) { backStackEntry ->
                BackHandler {
                    br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.setBusy(false)
                    navController.popBackStack()
                }
                val seriesId = backStackEntry.arguments
                    ?.getString("seriesId")
                    .orEmpty()

                if (!br.com.flaviotorres.iptvpremium.ui.settings.ParentalItemAllowed(activePlaylistId, "SERIES", seriesId, playlistCatalogRepository) { navController.popBackStack() }) return@composable
                val source = playlists.firstOrNull { it.id == activePlaylistId }
                val activeSeriesGeneration by remember(source?.id) {
                    source?.let { playlistCacheDao.observeActiveGenerationId(it.id) }
                        ?: kotlinx.coroutines.flow.flowOf(null)
                }.collectAsState(initial = null)
                // O historico nao pertence a pagina atual do Paging. Resolve o
                // registro por playlist + ID antes de montar a ficha do atalho.
                val series by produceState<Series?>(
                    initialValue = selectedSeriesForDetails?.takeIf { it.id == seriesId },
                    key1 = source?.id,
                    key2 = seriesId,
                    key3 = activeSeriesGeneration
                ) {
                    value = source?.let {
                        playlistCatalogRepository.getSeriesById(it.id, seriesId)
                    }
                }
                val cachedDetails = lastSeriesDetailsResult?.takeIf {
                    it.first == source?.id && it.second == seriesId
                }?.third
                val returningFromPlayer = backStackEntry.savedStateHandle.get<Boolean>(
                    SERIES_RETURN_FROM_PLAYER_KEY
                ) == true
                var episodeRetrySerial by remember(source?.id, seriesId) { mutableIntStateOf(0) }
                val readyFromMemory = if (returningFromPlayer) cachedDetails else if (episodeRetrySerial == 0) {
                    source?.id?.let { playlist -> activeSeriesGeneration?.let { generation ->
                        CatalogDetailsWarmupCache.getSeries(playlist, generation, seriesId)
                    } }
                } else null
                val loaded by produceState<SeriesEpisodesLoadState>(
                    initialValue = readyFromMemory?.let { SeriesEpisodesLoadState.Ready(it) }
                        ?: SeriesEpisodesLoadState.Loading,
                    key1 = seriesId,
                    key2 = source?.id,
                    key3 = activeSeriesGeneration to episodeRetrySerial
                ) {
                    // Preserva a mesma temporada e os mesmos episodios ja abertos.
                    // No retorno do player nao pisca "Carregando temporadas...".
                    if (readyFromMemory != null) {
                        value = SeriesEpisodesLoadState.Ready(readyFromMemory)
                        return@produceState
                    }
                    // produceState conserva o valor anterior quando uma chave muda.
                    // Limpa mensagens antigas ANTES de esperar IO/rede.
                    value = SeriesEpisodesLoadState.Loading
                    value = if (source == null || seriesId.isBlank()) {
                        SeriesEpisodesLoadState.Unavailable
                    } else if (activeSeriesGeneration == null) {
                        // A emissao do Room reinicia a consulta com a geracao
                        // correta; nao dispara rede com um catalogo indefinido.
                        SeriesEpisodesLoadState.Loading
                    } else {
                        try {
                            val result = loadSeriesForOpening(playlistCacheDao, seriesEpisodesRepository, source, seriesId)
                            if (result != null) {
                                lastSeriesDetailsResult = Triple(source.id, seriesId, result)
                                SeriesEpisodesLoadState.Ready(result)
                            } else SeriesEpisodesLoadState.Error

                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Log.w("SeriesDetails", "Falha ao carregar episodios (${error.javaClass.simpleName})")
                            SeriesEpisodesLoadState.Error
                        }
                    }
                }
                val episodeData = (loaded as? SeriesEpisodesLoadState.Ready)?.result
                val episodes = episodeData?.episodes.orEmpty()
                remember(activePlaylistId, seriesId, episodes, sharedReadyRevision) {
                    activePlaylistId?.let { sharedLibrary.registerEpisodes(it, seriesId, episodes) }
                    true
                }


                // O historico ja atualizado ao sair do player alimenta as linhas de
                // progresso com posicao/duracao REAIS, por ID de episodio e playlist.
                // Se o provedor nao informou duracao, nao inventamos porcentagens.
                val episodeProgress = remember(activePlaylistId, seriesId, historyRefreshSerial, episodes, sharedReadyRevision) {
                    val playlistId = activePlaylistId
                    if (playlistId.isNullOrBlank()) emptyMap<String, Float>() else {
                        val episodeDurations = episodes.associate { episode ->
                            episode.id to (episode.durationSeconds?.toLong()?.times(1_000L))
                        }
                        watchHistoryStore.all(playlistId).asSequence()
                            .filter { it.contentType == PlaybackContentType.EPISODE &&
                                    it.seriesId == seriesId && it.positionMs > 0L }
                            .mapNotNull { watched ->
                                val durationMs = watched.durationMs?.takeIf { it > 0L }
                                    ?: episodeDurations[watched.contentId]?.takeIf { it > 0L }
                                durationMs?.let { duration ->
                                    watched.contentId to
                                            (watched.positionMs.toFloat() / duration.toFloat())
                                                .coerceIn(0f, 1f)
                                }
                            }.toMap()
                    }
                }

                // O retorno do player reabre os detalhes SEM a animacao de entrada.
                // SavedStateHandle e proprio da rota, nao polui outras series.
                var showSeriesOpening by remember(seriesId) {
                    mutableStateOf(
                        backStackEntry.savedStateHandle.get<Boolean>(
                            SERIES_OPENING_COMPLETED_KEY
                        ) != true
                    )
                }
                val selectedSeasonInRoute = backStackEntry.savedStateHandle.get<Int>(SERIES_CURRENT_SEASON_KEY)
                val lastWatchedEpisodeId = remember(activePlaylistId, seriesId, historyRefreshSerial, sharedReadyRevision) {
                    activePlaylistId?.let { playlistId ->
                        val latest = watchHistoryStore.all(playlistId).filter {
                            it.contentType == PlaybackContentType.EPISODE && it.seriesId == seriesId
                        }.maxByOrNull { it.updatedAtMs }
                        when {
                            latest?.seriesOnly == true -> null
                            latest != null -> latest.contentId
                            else -> seriesResumeStore.getLastEpisodeId(playlistId, seriesId)
                        }
                    }
                }
                val preferredSeason = remember(episodes, selectedSeasonInRoute, lastWatchedEpisodeId) {
                    br.com.flaviotorres.iptvpremium.data.repository.preferredSeriesSeason(
                        episodes, selectedSeasonInRoute, lastWatchedEpisodeId)
                }
                val returnedEpisodeId = if (
                    backStackEntry.savedStateHandle.get<Boolean>(
                        SERIES_RETURN_FROM_PLAYER_KEY
                    ) == true
                ) backStackEntry.savedStateHandle.get<String>(SERIES_LAST_EPISODE_KEY)
                else null
                val openingFocusRequester = remember(seriesId) { FocusRequester() }
                val focusManager = LocalFocusManager.current

                // O mesmo cache usado em Filmes e Series antecipa a imagem visivel.
                // A URL final do provedor pode chegar depois da consulta de episodios.
                val openingBackdropUrl = episodeData?.backdropUrl ?: series?.backdropUrl
                val configuration = LocalConfiguration.current
                val density = LocalDensity.current
                val openingBackdropWidth = with(density) {
                    configuration.screenWidthDp.dp.roundToPx().coerceAtLeast(1)
                }
                val openingBackdropHeight = with(density) {
                    configuration.screenHeightDp.dp.roundToPx().coerceAtLeast(1)
                }

                // O segundo valor diferencia "sem foto" de "ainda tentando".
                // Falha da imagem nao bloqueia nem estende indefinidamente a animacao.
                val openingBackdrop by produceState<Pair<ImageBitmap?, Boolean>>(
                    initialValue = MovieDetailsImageLoader.peekFromMemory(
                        context.applicationContext,
                        openingBackdropUrl,
                        openingBackdropWidth,
                        openingBackdropHeight
                    ).let { cached ->
                        cached to (cached != null || openingBackdropUrl.isNullOrBlank())
                    },
                    key1 = seriesId,
                    key2 = openingBackdropUrl,
                    key3 = openingBackdropWidth to openingBackdropHeight
                ) {
                    val cached = MovieDetailsImageLoader.peekFromMemory(
                        context.applicationContext,
                        openingBackdropUrl,
                        openingBackdropWidth,
                        openingBackdropHeight
                    )
                    if (cached != null || openingBackdropUrl.isNullOrBlank()) {
                        value = cached to true
                    } else {
                        val image = MovieDetailsImageLoader.load(
                            context = context.applicationContext,
                            imageUrl = openingBackdropUrl,
                            targetWidthPx = openingBackdropWidth,
                            targetHeightPx = openingBackdropHeight,
                            imageType = "series_opening_backdrop"
                        )
                        value = image to true
                    }
                }

                val seriesRoomReady = br.com.flaviotorres.iptvpremium.ui.theme.rememberSeriesRoomReady()
                // Artwork has its own bounded window in the overlay. Real episode
                // data and the themed room remain essential before revealing a page.
                var seriesArtworkReady by remember(source?.id, seriesId) { mutableStateOf(false) }
                val seriesEssentialsReady = br.com.flaviotorres.iptvpremium.ui.theme.seriesOpeningCanStart(
                    loaded is SeriesEpisodesLoadState.Ready, true, seriesRoomReady)

                LaunchedEffect(showSeriesOpening) {
                    withFrameNanos { }
                    if (showSeriesOpening) {
                        openingFocusRequester.requestFocus()
                    } else if (returnedEpisodeId.isNullOrBlank()) {
                        // Na abertura normal inicia no primeiro comando.
                        // No retorno do player, o proprio card do episodio
                        // recupera o foco e esta chamada nao deve desloca-lo.
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                }

                // BACK durante a animacao cancela a entrada e volta a grade.
                BackHandler(enabled = showSeriesOpening) {
                    navController.popBackStack()
                }

                // Both opening preview and final page share metadata for this route.
                val seriesArtworkSession = remember(source?.id, seriesId) {
                    br.com.flaviotorres.iptvpremium.ui.series.SeriesArtworkSession()
                }
                // A mesma interface e desenhada na pagina e na TV.
                val seriesDetailsPage: @Composable (Boolean) -> Unit = { reportOpeningArtwork ->
                    SeriesDetailsScreen(
                        artworkSession = seriesArtworkSession,
                        ratingPlaylistId = activePlaylistId,
                        ratingContentId = seriesId,
                        ratingIdentityTitle = series?.name,
                        ratingIdentityYear = series?.year,
                        seriesName = series?.name ?: episodeData?.title ?: "Série",
                        identityPosterUrl = episodeData?.posterUrl ?: series?.posterUrl,
                        // Prioriza o ID oficial quando a playlist fornece esse vínculo.
                        // Sem ID, SeriesDetailsScreen mantém a busca segura pelo título.
                        tmdbId = series?.tmdbId?.trim()?.takeIf { it.isNotBlank() },
                        imdbId = series?.imdbId,
                        backdropUrl = episodeData?.backdropUrl ?: series?.backdropUrl,
                        titleLogoUrl = series?.titleLogoUrl,
                        rating = series?.rating,
                        ratingSource = series?.ratingSource,
                        year = series?.year,
                        durationSeconds = series?.durationSeconds,
                        genres = series?.genres.orEmpty(),
                        plot = episodeData?.plot?.takeIf { it.isNotBlank() }
                            ?: series?.plot.orEmpty(),
                        seasons = episodeData?.seasons.orEmpty(),
                        episodes = episodes,
                        episodeImageKey = "${source?.id}:$activeSeriesGeneration:$seriesId",
                        onArtworkReady = { ready -> if (reportOpeningArtwork) seriesArtworkReady = ready },
                        episodeProgress = episodeProgress,
                        preferredSeasonNumber = preferredSeason,
                        focusEpisodeId = returnedEpisodeId,
                        onSeasonSelected = { seasonNumber ->
                            backStackEntry.savedStateHandle[SERIES_CURRENT_SEASON_KEY] = seasonNumber
                        },
                        isLoadingEpisodes = loaded == SeriesEpisodesLoadState.Loading,
                        onRetryEpisodes = { episodeRetrySerial++ },
                        episodesMessage = when (loaded) {
                            SeriesEpisodesLoadState.Unavailable ->
                                "Não foi possível carregar os episódios. Tente novamente."
                            SeriesEpisodesLoadState.Error ->
                                "Não foi possível carregar os episódios."
                            else -> null
                        },
                        trailerAvailable = !episodeData?.trailerUrl.isNullOrBlank() ||
                                !series?.trailerUrl.isNullOrBlank(),
                        isFavorite = seriesId in favoriteSeriesIds,
                        onFavoriteClick = {
                            toggleSharedFavorite("series", seriesId)
                        },
                        onWatchClick = {
                            val playlistId = activePlaylistId
                            val orderedEpisodes = episodes.sortedWith(
                                compareBy<SeriesEpisodeInfo> { it.seasonNumber }
                                    .thenBy { it.episodeNumber }
                            )
                            if (playlistId.isNullOrBlank()) {
                                Toast.makeText(
                                    context, "Selecione uma playlist antes de reproduzir.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (orderedEpisodes.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    if (loaded == SeriesEpisodesLoadState.Loading)
                                        "Aguarde o carregamento dos episódios."
                                    else "Nenhum episódio disponível.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val sharedEntries = watchHistoryStore.all(playlistId).filter {
                                    it.contentType == PlaybackContentType.EPISODE && it.seriesId == seriesId
                                }
                                val latest = sharedEntries.maxByOrNull { it.updatedAtMs }
                                if (latest?.seriesOnly == true) {
                                    Toast.makeText(context, "Série encontrada. Escolha a temporada e o episódio em que parou.", Toast.LENGTH_LONG).show()
                                } else {
                                val lastEpisodeId = latest?.contentId ?: seriesResumeStore.getLastEpisodeId(
                                    playlistId, seriesId
                                )
                                val chosenEpisode = orderedEpisodes.firstOrNull {
                                    it.id == lastEpisodeId
                                } ?: orderedEpisodes.firstOrNull {
                                    it.seasonNumber == 1 && it.episodeNumber == 1
                                } ?: orderedEpisodes.firstOrNull { it.seasonNumber >= 1 }
                                ?: orderedEpisodes.first()
                                selectedEpisodeToPlay = chosenEpisode
                                selectedEpisodeSeriesId = seriesId
                                selectedEpisodeSubtitleIdentity = EpisodeSubtitleIdentity(
                                    playlistId = playlistId,
                                    seriesId = seriesId,
                                    seriesTitle = (series?.name ?: episodeData?.title)
                                        ?.trim()?.takeIf { it.isNotBlank() },
                                    tmdbId = series?.tmdbId?.trim()?.takeIf {
                                        it.toLongOrNull()?.let { id -> id > 0L } == true
                                    },
                                    year = series?.year
                                )
                                backStackEntry.savedStateHandle[SERIES_OPENING_COMPLETED_KEY] = true
                                backStackEntry.savedStateHandle[SERIES_CURRENT_SEASON_KEY] =
                                    chosenEpisode.seasonNumber
                                backStackEntry.savedStateHandle[SERIES_LAST_EPISODE_KEY] =
                                    chosenEpisode.id
                                backStackEntry.savedStateHandle[SERIES_RETURN_FROM_PLAYER_KEY] = true
                                showSeriesOpening = false
                                seriesResumeStore.rememberEpisode(
                                    playlistId, seriesId, chosenEpisode.id
                                )
                                navController.navigate(
                                    AppDestination.Playback.createRoute(
                                        PlaybackContentType.EPISODE, chosenEpisode.id
                                    )
                                ) { launchSingleTop = true }
                                }
                            }
                        },
                        onTrailerClick = {
                            br.com.flaviotorres.iptvpremium.core.media.openTrailer(context,
                                episodeData?.trailerUrl?.takeIf { it.isNotBlank() } ?: series?.trailerUrl,
                                series?.name ?: episodeData?.title.orEmpty(), kind = "tv", tmdbId = series?.tmdbId, year = series?.year)
                        },
                        onEpisodeClick = { episodeId ->
                            val playlistId = activePlaylistId
                            val selected = episodes.firstOrNull { it.id == episodeId }
                            if (playlistId.isNullOrBlank()) {
                                Toast.makeText(
                                    context, "Selecione uma playlist antes de reproduzir.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (selected != null) {
                                selectedEpisodeToPlay = selected
                                selectedEpisodeSeriesId = seriesId
                                selectedEpisodeSubtitleIdentity = EpisodeSubtitleIdentity(
                                    playlistId = playlistId,
                                    seriesId = seriesId,
                                    seriesTitle = (series?.name ?: episodeData?.title)
                                        ?.trim()?.takeIf { it.isNotBlank() },
                                    tmdbId = series?.tmdbId?.trim()?.takeIf {
                                        it.toLongOrNull()?.let { id -> id > 0L } == true
                                    },
                                    year = series?.year
                                )
                                backStackEntry.savedStateHandle[SERIES_OPENING_COMPLETED_KEY] = true
                                backStackEntry.savedStateHandle[SERIES_CURRENT_SEASON_KEY] =
                                    selected.seasonNumber
                                backStackEntry.savedStateHandle[SERIES_LAST_EPISODE_KEY] = selected.id
                                backStackEntry.savedStateHandle[SERIES_RETURN_FROM_PLAYER_KEY] = true
                                showSeriesOpening = false
                                seriesResumeStore.rememberEpisode(
                                    playlistId, seriesId, selected.id
                                )
                                navController.navigate(
                                    AppDestination.Playback.createRoute(
                                        PlaybackContentType.EPISODE, selected.id
                                    )
                                ) { launchSingleTop = true }
                            }
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    seriesDetailsPage(true)

                    if (showSeriesOpening) {
                        SeriesLoadingOverlay(
                            seriesName = series?.name ?: episodeData?.title ?: "Série",
                            openingKey = seriesId,
                            isSeriesReady = seriesEssentialsReady,
                            isArtworkReady = seriesArtworkReady,
                            isSeriesFailed = loaded is SeriesEpisodesLoadState.Error || loaded is SeriesEpisodesLoadState.Unavailable,
                            previewBackdrop = openingBackdrop.first,
                            detailsContent = { seriesDetailsPage(false) },
                            onComplete = { ready ->
                                showSeriesOpening = false
                                if (ready) backStackEntry.savedStateHandle[SERIES_OPENING_COMPLETED_KEY] = true
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(openingFocusRequester)
                                .onPreviewKeyEvent { keyEvent ->
                                    // Enquanto a TV liga, as setas e o OK nao
                                    // acionam botoes da tela oculta por baixo.
                                    if (keyEvent.type != KeyEventType.KeyDown &&
                                        keyEvent.type != KeyEventType.KeyUp
                                    ) {
                                        false
                                    } else {
                                        when (keyEvent.key) {
                                            Key.DirectionUp,
                                            Key.DirectionDown,
                                            Key.DirectionLeft,
                                            Key.DirectionRight,
                                            Key.DirectionCenter,
                                            Key.Enter,
                                            Key.NumPadEnter -> true
                                            else -> false
                                        }
                                    }
                                }
                                .focusable()
                        )
                    }
                }
            }
        }

        br.com.flaviotorres.iptvpremium.ui.settings.ParentalCategoryDialog(parentalCategoryRequest) { parentalCategoryRequest = null }

        val visibleCatalogLoading = if (catalogLoadingDismissed) null else
            catalogLoadingState ?: if (portalSyncRequests > 0 || portalLoadRequest != null)
                AppCatalogLoadingState() else null
        BackHandler(enabled = visibleCatalogLoading != null) {
            leaveCatalogImport()
        }
        visibleCatalogLoading?.let { state ->
            AppStartupLoadingScreen(
                state = state,
                modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures {} },
                onOpenHome = if (state.phase == AppCatalogLoadingPhase.ERROR) {
                    { leaveCatalogImport() }
                } else null
            )
        }
        val historyRun = historySyncRun
        var historyNoticeId by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(historyRun?.id,enrichmentRoute,visibleCatalogLoading) {
            if(enrichmentRoute == AppDestination.Home.route && visibleCatalogLoading == null && historyRun != null &&
                historyRun.playlist == activePlaylistId && historyRun.announce && historyNoticeId != historyRun.id &&
                historyRun.phase in listOf(
                    br.com.flaviotorres.iptvpremium.data.history.HistorySyncSession.Phase.PENDING,
                    br.com.flaviotorres.iptvpremium.data.history.HistorySyncSession.Phase.RUNNING)) {
                historyNoticeId=historyRun.id
                Toast.makeText(context,"Seu histórico será importado em segundo plano. Você pode continuar usando o aplicativo.",Toast.LENGTH_LONG).show()
            }
        }
    } // Box: mantem o NavHost e mostra o overlay acima dele.

    /*
     * Mantemos esta leitura para deixar o estado de erro
     * observado pelo Compose e pronto para o próximo passo.
     */
    playlistConnectionError
}

/**
 * O provedor tem prioridade quando fornece episodios reais.
 * Se ele devolve somente metadados/temporadas, complementamos com os
 * episodios armazenados na geracao ACTIVE da MESMA playlist e serie.
 * As imagens e os nomes oficiais das temporadas do provedor sao preservados.
 */
private suspend fun loadSeriesForOpening(
    cacheDao: PlaylistCacheDao,
    repository: SeriesEpisodesRepository,
    source: PlaylistSource,
    seriesId: String
): SeriesEpisodesResult? = withContext(Dispatchers.IO) {
    val started = android.os.SystemClock.elapsedRealtime()

    try {
        val generation = cacheDao.getActiveGeneration(source.id)?.generationId ?: return@withContext null
        CatalogDetailsWarmupCache.seriesOrLoad(source.id, generation, seriesId) {
            fetchSeriesContent(cacheDao, repository, source, seriesId, generation)
        }.also {
            Log.d("SeriesDetails", "fluxo=series-r5 abertura episodios=${it?.episodes?.size ?: 0} totalMs=${android.os.SystemClock.elapsedRealtime()-started}")
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) {
        Log.w("SeriesDetails", "Carregamento indisponivel: ${error.javaClass.simpleName}")
        null
    }
}

/** Mesma regra para abrir a serie e para aquece-la antes do clique. */
private suspend fun fetchSeriesContent(
    cacheDao: PlaylistCacheDao,
    repository: SeriesEpisodesRepository,
    source: PlaylistSource,
    seriesId: String,
    generation: String
): SeriesEpisodesResult? =
    br.com.flaviotorres.iptvpremium.data.repository.loadSeriesEpisodeContent(
        local = {
            try { loadCachedSeriesEpisodes(cacheDao, source.id, seriesId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        },
        remote = {
            val started = android.os.SystemClock.elapsedRealtime()
            repository.load(source, seriesId, generation).also {
                Log.d("SeriesDetails", "Consulta episodios quantidade=${it?.episodes?.size ?: 0} duracaoMs=${android.os.SystemClock.elapsedRealtime() - started}")
            }
        }
    )

/**
 * Aquece os episodios da serie que esta com foco, em segundo plano.
 * Usa o limitador do cache (2 consultas simultaneas) para nao estourar o
 * limite de conexoes do provedor; o clique do usuario nao espera essa fila.
 */
private suspend fun prefetchSeriesForFocus(
    cacheDao: PlaylistCacheDao,
    repository: SeriesEpisodesRepository,
    source: PlaylistSource,
    seriesId: String
) = withContext(Dispatchers.IO) {
    try {
        val generation = cacheDao.getActiveGeneration(source.id)?.generationId
            ?: return@withContext
        CatalogDetailsWarmupCache.prefetchSeries(source.id, generation, seriesId) {
            fetchSeriesContent(cacheDao, repository, source, seriesId, generation)
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { /* Falha de aquecimento nao deve afetar a navegacao. */ }
}

private fun combineSeriesEpisodesWithCache(
    provider: SeriesEpisodesResult?,
    local: SeriesEpisodesResult?
): SeriesEpisodesResult? {
    if (provider == null) return local
    if (provider.episodes.isNotEmpty() || local == null || local.episodes.isEmpty()) {
        return provider
    }

    val localSeasons = local.seasons.associateBy { it.number }
    val providerSeasonNumbers = provider.seasons.map { it.number }.toSet()
    val mergedSeasons = (
            provider.seasons.map { season ->
                val cachedSeason = localSeasons[season.number]
                if (cachedSeason == null) season
                else season.copy(episodeCount = cachedSeason.episodeCount)
            } + local.seasons.filterNot { it.number in providerSeasonNumbers }
            ).sortedBy { it.number }

    return provider.copy(
        seasons = mergedSeasons,
        episodes = local.episodes
    )
}

/**
 * Fallback apenas para episodios da serie atual ja presentes na geracao ACTIVE.
 * Evita tratar IDs M3U sinteticos como identificadores Xtream numericos.
 * A busca e paginada, sem carregar o catalogo de outras series.
 */
private suspend fun loadCachedSeriesEpisodes(
    cacheDao: PlaylistCacheDao,
    playlistId: String,
    seriesId: String
): SeriesEpisodesResult? = withContext(Dispatchers.IO) {
    val generationId = cacheDao.getActiveGeneration(playlistId)?.generationId
        ?: return@withContext null
    val pageSize = 200
    val maxEpisodeCount = 5000
    val storedEpisodes = ArrayList<br.com.flaviotorres.iptvpremium.data.cache.entity.CachedEpisodeEntity>()

    var offset = 0
    while (offset < maxEpisodeCount) {
        val next = cacheDao.getEpisodesPage(
            playlistId = playlistId,
            generationId = generationId,
            seriesId = seriesId,
            limit = minOf(pageSize, maxEpisodeCount - offset),
            offset = offset
        )
        if (next.isEmpty()) break
        storedEpisodes.addAll(next)
        offset += next.size
        if (next.size < pageSize) break
    }
    if (storedEpisodes.isEmpty()) return@withContext null

    val realEpisodes = storedEpisodes.mapIndexed { index, episode ->
        SeriesEpisodeInfo(
            id = episode.episodeId,
            seasonNumber = (episode.seasonNumber ?: 1).coerceAtLeast(0),
            episodeNumber = (episode.episodeNumber ?: index + 1).coerceAtLeast(1),
            title = episode.name.ifBlank { "Episódio ${index + 1}" },
            thumbnailUrl = episode.posterUrl,
            synopsis = null,
            durationSeconds = episode.durationSeconds?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt(),
            containerExtension = null
        )
    }
    val seasons = realEpisodes
        .groupBy { it.seasonNumber }
        .toSortedMap()
        .map { (seasonNumber, episodes) ->
            SeriesSeasonInfo(
                number = seasonNumber,
                title = if (seasonNumber == 0) "Especiais" else "Temporada $seasonNumber",
                coverUrl = null,
                episodeCount = episodes.size
            )
        }

    SeriesEpisodesResult(
        seriesId = seriesId,
        title = null,
        plot = null,
        backdropUrl = null,
        posterUrl = null,
        trailerUrl = null,
        seasons = seasons,
        episodes = realEpisodes
    )
}

/**
 * Associa uma producao do TMDB SOMENTE a um filme ja existente na playlist.
 *
 * O Room deste projeto guarda movieId do provedor, nao tmdbId. Por isso:
 * - nunca usamos credit.tmdbId como movieId;
 * - consultamos somente a geracao ACTIVE da playlist selecionada;
 * - filtramos por titulo no SQLite (maximo de 120 registros por tentativa);
 * - exigimos titulo normalizado identico e ano compativel, se conhecido;
 * - sem correspondencia segura, retornamos null.
 *
 * Esta pesquisa roda apenas ao clicar em um card, fora da thread principal.
 */
private suspend fun findMovieIdForActorCredit(
    cacheDao: PlaylistCacheDao,
    playlistId: String,
    credit: ActorFilmographyCredit
): String? = withContext(Dispatchers.IO) {
    if (credit.mediaType != "movie" || credit.title.isBlank()) {
        return@withContext null
    }
    val generationId = cacheDao.getActiveGeneration(playlistId)?.generationId
        ?: return@withContext null
    val targetTitle = normalizeActorMovieTitle(credit.title)
    if (targetTitle.isBlank()) return@withContext null
    val creditYear = credit.year?.toIntOrNull()

    // Primeiro procura pelo titulo completo; se houver pontuacao ou tags
    // diferentes no provedor, tenta os dois fragmentos mais especificos.
    val fragments = Regex("[A-Za-z0-9]{4,}")
        .findAll(credit.title)
        .map { it.value }
        .filterNot { it.equals("filme", ignoreCase = true) }
        .distinct()
        .sortedByDescending { it.length }
        .take(2)
        .toList()
    val searches = (listOf(credit.title.trim()) + fragments).distinct()

    for (search in searches) {
        val source = cacheDao.getMoviesPagingSourceFiltered(
            playlistId = playlistId,
            generationId = generationId,
            categoryId = null,
            searchQuery = search,
            favoritesOnly = false,
            favoriteMovieIds = listOf("__ACTOR_MATCH_NO_FAVORITE__"),
            sortMode = "DEFAULT"
        )
        val rows = try {
            when (val result = source.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 120,
                    placeholdersEnabled = false
                )
            )) {
                is PagingSource.LoadResult.Page -> result.data
                else -> emptyList()
            }
        } finally {
            source.invalidate()
        }

        val exactTitle = rows.filter { normalizeActorMovieTitle(it.name) == targetTitle }
        if (exactTitle.isEmpty()) continue

        // O ano correto tem prioridade. Nunca escolher filme de outro ano
        // quando a lista informa um ano diferente para o mesmo titulo.
        val sameYear = if (creditYear != null) {
            exactTitle.filter { it.year == creditYear }
        } else emptyList()
        if (sameYear.isNotEmpty()) return@withContext sameYear.first().movieId

        val knownYears = exactTitle.mapNotNull { it.year }.distinct()
        if (creditYear != null) {
            // Se a playlist informa um ano conflitante, nao adivinhamos
            // usando uma copia sem ano preenchido.
            if (knownYears.isNotEmpty()) continue
            val withoutYear = exactTitle.firstOrNull { it.year == null }
            if (withoutYear != null) return@withContext withoutYear.movieId
        } else if (knownYears.size <= 1) {
            // Sem ano no TMDB, evita filmes homonimos de epocas diferentes.
            return@withContext exactTitle.first().movieId
        }
    }
    null
}

private fun normalizeActorMovieTitle(value: String): String {
    // Tags de qualidade nao fazem parte do titulo do filme.
    val noTags = value.replace(
        Regex("(?i)\\[(?:4k|uhd|hd|fhd|1080p|720p|hdr|dublado|legendado|dual audio)\\]"),
        " "
    )
    val noFinalYear = noTags.replace(Regex("\\s*\\((?:19|20)\\d{2}\\)\\s*$"), " ")
    return Normalizer.normalize(noFinalYear, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

/**
 * Carrossel cinematografico 3D em elipse alongada e inclinada.
 *
 * - Ate 20 capas reais e diferentes, somente das paginas ja presentes no Paging.
 * - Um unico relogio com withFrameNanos, sem reiniciar a animacao a cada volta.
 * - Uma orbita continua e sempre no mesmo sentido, com a frente sobre o fundo.
 * - Ao concluir: acelera para o filme escolhido, pressiona com borda dourada,
 *   oculta os textos e so entao expande para abrir a tela de detalhes.
 * - Nenhum clique real e disparado: onTransitionFinished e chamado uma vez.
 */
@Composable
internal fun CinematicCarouselLoadingScreen(
    posters: List<CinematicCarouselPoster>,
    selectedMovieId: String,
    finishRequested: Boolean,
    errorMessage: String?,
    onTransitionFinished: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val kidsLoading=br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active
    val sessionMetal=if(kidsLoading)LoadingMetallicGold else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBorder
    val sessionTextMetal=if(kidsLoading)LoadingMetallicGoldText else sessionMetal
    val cards = remember(posters, selectedMovieId) {
        val result = ArrayList<CinematicCarouselPoster>(MAX_CAROUSEL_POSTERS)
        val ids = HashSet<String>()
        val urls = HashSet<String>()
        val selected = posters.firstOrNull { it.movieId == selectedMovieId }
            ?: CinematicCarouselPoster(selectedMovieId, "FILME SELECIONADO", null)
        for (card in listOf(selected) + posters) {
            if (!ids.add(card.movieId)) continue
            val url = card.posterUrl?.trim()?.takeIf { it.isNotBlank() }
            if (url != null && !urls.add(url)) continue
            result += card
            if (result.size >= MAX_CAROUSEL_POSTERS) break
        }
        result
    }

    // Estado lido apenas durante o posicionamento dos posters.
    // A posicao do giro usa o tempo absoluto entre quadros, nao somas de deltas.
    val trackPosition = remember(selectedMovieId) { mutableFloatStateOf(0f) }
    val pressAmount = remember(selectedMovieId) { mutableFloatStateOf(0f) }
    val goldSelection = remember(selectedMovieId) { mutableFloatStateOf(0f) }
    val expansionAmount = remember(selectedMovieId) { mutableFloatStateOf(0f) }
    val selectionStarted = remember(selectedMovieId) { mutableStateOf(false) }
    val finishSignal = rememberUpdatedState(finishRequested)
    val finishCallback = rememberUpdatedState(onTransitionFinished)
    val orderKey = cards.joinToString("|") { it.movieId }

    val transition = rememberInfiniteTransition(label = "cinematic-carousel-light")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "carousel-reflection"
    )

    // Transicao de opacidade independente de ColumnScope.AnimatedVisibility.
    // O logo e o rodape somem juntos antes do zoom do poster escolhido.
    val chromeAlpha by animateFloatAsState(
        targetValue = if (selectionStarted.value) 0f else 1f,
        animationSpec = tween(durationMillis = 185),
        label = "carousel-loading-chrome-alpha"
    )

    // A barra nao apresenta 100% antes da preparacao real terminar.
    // O progresso intermediario e visual (o carregamento nao informa percentual).
    val sessionLoadingProgress by animateFloatAsState(
        targetValue = if (finishRequested) 1f else 0.76f,
        animationSpec = tween(
            durationMillis = if (finishRequested) 450 else 4_600,
            easing = LinearEasing
        ),
        label = "movie-session-loading-progress"
    )

    LaunchedEffect(selectedMovieId, orderKey, errorMessage) {
        if (errorMessage != null) return@LaunchedEffect
        val count = cards.size.coerceAtLeast(1).toFloat()
        val chosenIndex = cards.indexOfFirst { it.movieId == selectedMovieId }
            .coerceAtLeast(0).toFloat()
        val rotationSpeed = br.com.flaviotorres.iptvpremium.ui.movies.MovieOpeningPolicy.rotationCardsPerSecond
        // Relogio ancorado: nao acumula erros nem perde distancia em frames lentos.
        // Uma pausa longa (TV em segundo plano) nao faz o anel dar um salto.
        var rotationOriginNanos = withFrameNanos { it }
        var rotationOriginPosition = trackPosition.floatValue
        var priorNanos = rotationOriginNanos
        // 0: giro; 1: avanco acelerado; 2: clique; 3: expansao
        var mode = 0
        var phaseStarted = 0L
        var phaseDuration = 0L
        var initial = 0f
        var destination = 0f

        while (true) {
            val now = withFrameNanos { it }
            val frameGapNanos = (now - priorNanos).coerceAtLeast(0L)
            priorNanos = now

            when (mode) {
                0 -> {
                    if (finishSignal.value) {
                        initial = trackPosition.floatValue
                        val phase = ((initial % count) + count) % count
                        val forward = ((chosenIndex - phase) % count + count) % count
                        // Vai direto ao filme escolhido, sem acrescentar uma volta acelerada.
                        destination = initial + forward
                        phaseStarted = now
                        phaseDuration = br.com.flaviotorres.iptvpremium.ui.movies.MovieOpeningPolicy.alignNanos
                        mode = if (count > 1f) 1 else 2
                        if (mode == 2) {
                            selectionStarted.value = true
                            goldSelection.floatValue = 1f
                        }
                    } else if (count > 1f) {
                        // Amostras regulares usam tempo continuo, inclusive quando
                        // o Android deixa de apresentar um quadro intermediario.
                        // Apenas pausas muito longas congelam o tempo da animacao.
                        if (frameGapNanos > 120_000_000L) {
                            rotationOriginNanos += frameGapNanos - 16_666_667L
                        }
                        val elapsed = (now - rotationOriginNanos)
                            .coerceAtLeast(0L) / 1_000_000_000.0
                        val next = rotationOriginPosition +
                                rotationSpeed * elapsed.toFloat()
                        if (next > count * 128f) {
                            // Evita perda de precisao apos muitos minutos.
                            rotationOriginPosition = next - count * 128f
                            rotationOriginNanos = now
                            trackPosition.floatValue = rotationOriginPosition
                        } else {
                            trackPosition.floatValue = next
                        }
                    }
                }

                1 -> {
                    val t = ((now - phaseStarted).toDouble() /
                            phaseDuration.toDouble()).toFloat().coerceIn(0f, 1f)
                    val distance = (destination - initial).coerceAtLeast(0.001f)
                    val duration = phaseDuration / 1_000_000_000f
                    // Hermite continua a velocidade anterior; acelera e
                    // desacelera ate parar exatamente diante da camera.
                    val slope = (rotationSpeed * duration / distance)
                        .coerceIn(0.02f, 0.75f)
                    val t2 = t * t
                    val t3 = t2 * t
                    val smoothForward = (-2f * t3 + 3f * t2) +
                            (t3 - 2f * t2 + t) * slope
                    trackPosition.floatValue = initial + distance * smoothForward
                    if (t >= 1f) {
                        trackPosition.floatValue = destination
                        mode = 2
                        phaseStarted = now
                        selectionStarted.value = true
                        goldSelection.floatValue = 1f
                    }
                }

                2 -> {
                    // Clique e expansao compartilham a reserva do limite de entrada.
                    val t = ((now - phaseStarted) / br.com.flaviotorres.iptvpremium.ui.movies.MovieOpeningPolicy.pressNanos.toFloat())
                        .coerceIn(0f, 1f)
                    pressAmount.floatValue = if (t <= 0.44f) {
                        t / 0.44f
                    } else {
                        (1f - t) / 0.56f
                    }.coerceIn(0f, 1f)
                    if (t >= 1f) {
                        pressAmount.floatValue = 0f
                        mode = 3
                        phaseStarted = now
                    }
                }

                3 -> {
                    // Poster avancando na direcao da camera. O zoom ocorre
                    // dentro desta tela antes de navegar para MovieDetails.
                    val t = ((now - phaseStarted) / br.com.flaviotorres.iptvpremium.ui.movies.MovieOpeningPolicy.expandNanos.toFloat())
                        .coerceIn(0f, 1f)
                    expansionAmount.floatValue = t * t * (3f - 2f * t)
                    if (t >= 1f) {
                        currentCoroutineContext().ensureActive()
                        finishCallback.value()
                        break
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF05080F)),
        contentAlignment = Alignment.Center
    ) {
        // Somente o fundo foi substituido: logo e carrossel 3D abaixo
        // permanecem com a geometria, o foco e as animacoes originais.
        if(kidsLoading) br.com.flaviotorres.iptvpremium.ui.theme.KidsLoadingWallpaper(Modifier.fillMaxSize())
        else br.com.flaviotorres.iptvpremium.ui.theme.MarbleCatalogWallpaper(Modifier.fillMaxSize())

        val compact = maxHeight < 500.dp || maxWidth < 800.dp
        val density = LocalDensity.current
        val cardWidth = if (compact) 82.dp else 126.dp
        val cardHeight = if (compact) 123.dp else 189.dp
        val count = cards.size.coerceAtLeast(1)
        // Reservar a altura do logo + rodape, inclusive nas TVs em que o
        // Android usa menos dp que pixels fisicos (ex.: 4K em TV grande).
        val logoHeight=if(errorMessage!=null) {if(compact)64.dp else 96.dp} else if(compact)100.dp else 142.dp
        val footerHeight=if(errorMessage!=null) {if(compact)144.dp else 140.dp} else if(compact)84.dp else 80.dp
        val normalCarouselHeight = if (compact) {
            188.dp
        } else {
            minOf(300.dp, (maxHeight - 242.dp).coerceAtLeast(235.dp))
        }
        val carouselHeight=if(errorMessage!=null)minOf(normalCarouselHeight,
            (maxHeight-logoHeight-footerHeight-30.dp).coerceAtLeast(64.dp)) else normalCarouselHeight

        // Elipse bastante horizontal: ocupa cerca de 84% da tela.
        // A inclinacao e aplicada ao CAMINHO, nao so a cada capa.
        val orbitRadiusX = with(density) { maxWidth.toPx() } *
                if (compact) 0.40f else 0.415f
        val orbitRadiusY = with(density) { maxHeight.toPx() } *
                if (compact) 0.135f else 0.165f
        // Apenas desce a posicao das capas (sem alterar a geometria da orbita).
        // Em telas compactas, usa um deslocamento menor para preservar o rodape.
        val carouselDownPx = with(density) {
            (if(errorMessage!=null)0.dp else if (compact) 12.dp else 26.dp).toPx()
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Reserva a altura, mesmo quando a marca desaparece no clique.
            Box(
                modifier = Modifier
                    .width(if (compact) 155.dp else 214.dp)
                    .height(logoHeight),
                contentAlignment = Alignment.Center
            ) {
                if(kidsLoading) Image(painterResource(R.drawable.iptv_premium_logo),"IPTV PREMIUM",
                    Modifier.fillMaxSize().graphicsLayer {alpha=chromeAlpha},contentScale=ContentScale.Fit)
                else br.com.flaviotorres.iptvpremium.ui.theme.MarbleLogoImage(
                    modifier=Modifier.fillMaxSize().graphicsLayer { alpha = chromeAlpha },
                    description="IPTV PREMIUM")
            }
            Spacer(Modifier.height(if (compact) 3.dp else 9.dp))

            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(carouselHeight),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val lightX = size.width * (0.30f + shimmer * 0.40f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0x28D4AF37), Color(0x0B41618D), Color.Transparent
                            ),
                            center = Offset(lightX, size.height * 0.64f),
                            radius = size.height * 0.83f
                        ),
                        center = Offset(lightX, size.height * 0.64f),
                        radius = size.height * 0.83f
                    )
                }

                val widthPx = with(density) { cardWidth.roundToPx() }
                val heightPx = with(density) { cardHeight.roundToPx() }
                // Arrays reutilizados: sem Triple/map/sortedBy alocados a cada
                // quadro. A ordenacao continua por profundidade, igual a anterior.
                val ringAngles = remember(count) { FloatArray(count) }
                val ringDepths = remember(count) { FloatArray(count) }
                val ringOrder = remember(count) { IntArray(count) }
                Layout(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        cards.forEach { poster ->
                            key(poster.movieId) {
                                CinematicCarouselCard(
                                    poster = poster,
                                    width = cardWidth,
                                    height = cardHeight,
                                    selectedMovie = poster.movieId == selectedMovieId,
                                    selectionGlow = goldSelection,
                                    zoom = expansionAmount
                                )
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val placeables = measurables.map {
                        it.measure(Constraints.fixed(widthPx, heightPx))
                    }
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val position = trackPosition.floatValue
                        val click = pressAmount.floatValue
                        val expansion = expansionAmount.floatValue
                        val total = count.toFloat()

                        val angleStep = (2f * PI.toFloat()) / total
                        for (index in placeables.indices) {
                            val angle = (index.toFloat() - position) * angleStep
                            ringAngles[index] = angle
                            ringDepths[index] = cos(angle)
                            ringOrder[index] = index
                        }
                        // Insertion sort estavel em ate 20 indices inteiros.
                        // Preserva exatamente a mesma sobreposicao da versao atual.
                        for (i in 1 until placeables.size) {
                            val item = ringOrder[i]
                            var j = i - 1
                            while (j >= 0 && ringDepths[ringOrder[j]] > ringDepths[item]) {
                                ringOrder[j + 1] = ringOrder[j]
                                j--
                            }
                            ringOrder[j + 1] = item
                        }

                        for (slot in placeables.indices) {
                            val index = ringOrder[slot]
                            val angle = ringAngles[index]
                            val depth = ringDepths[index]
                            val placeable = placeables[index]
                            val xOrbit = sin(angle) * orbitRadiusX
                            // Anel inclinado: a parte direita sobe; a camada
                            // traseira fica acima e atras da dianteira.
                            val yOrbit = -xOrbit * 0.205f -
                                    (1f - depth) * orbitRadiusY * 0.63f
                            val near = (depth + 1f) * 0.5f
                            val perspectiveScale = 0.55f + 0.45f * near
                            val chosen = cards[index].movieId == selectedMovieId
                            val pressScale = if (chosen) 1f - 0.085f * click else 1f
                            val zoomScale = if (chosen) 1f + expansion * 7.5f else 1f
                            val fade = if (chosen) 1f else 1f - expansion
                            val centerX = (constraints.maxWidth - placeable.width) * 0.5f
                            val centerY = (constraints.maxHeight - placeable.height) * 0.5f
                            // Colocacao inteira fixa + deslocamento fracionario
                            // na camada grafica: elimina a 'escadinha' de 1 pixel
                            // e preserva o centro/escala/angulo do layout original.
                            val px = centerX.roundToInt()
                            val py = centerY.roundToInt()

                            placeable.placeRelativeWithLayer(px, py) {
                                translationX = xOrbit + (centerX - px)
                                translationY = yOrbit + carouselDownPx + (centerY - py)
                                rotationY = -sin(angle) * 47f * (1f - expansion)
                                rotationZ = (-5f + sin(angle) * 2.5f) *
                                        (1f - expansion)
                                cameraDistance = 28f * density.density
                                val finalScale = perspectiveScale * pressScale * zoomScale
                                scaleX = finalScale
                                scaleY = finalScale
                                alpha = ((0.41f + 0.59f * near) * fade)
                                    .coerceIn(0f, 1f)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(if (compact) 6.dp else 9.dp))
            // Mesma area reservada: nem o logo nem as capas saltam no fim.
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(footerHeight),
                contentAlignment = Alignment.TopCenter
            ) {
                if (!selectionStarted.value || chromeAlpha > 0.001f) {
                    Column(
                        modifier = Modifier.graphicsLayer { alpha = chromeAlpha },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (errorMessage == null) {
                                "Carregando…"
                            } else "FALHA NA CONEXÃO",
                            style = TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = if (compact) 12.sp else 14.sp,
                                brush = sessionTextMetal,
                                letterSpacing = if (compact) 0.4.sp else 1.3.sp,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 2
                        )
                        if (errorMessage == null) {
                            Spacer(Modifier.height(if (compact) 11.dp else 16.dp))
                            // A barra e desenhada no Compose: a imagem WebP
                            // contem APENAS o fundo e nunca um progresso estatico.
                            Canvas(
                                modifier = Modifier
                                    .width(if (compact) 218.dp else 365.dp)
                                    .height(if (compact) 3.dp else 4.dp)
                            ) {
                                val radius = size.height / 2f
                                val inset = 0.6.dp.toPx()
                                val innerHeight = (size.height - inset * 2f).coerceAtLeast(0f)
                                val fillWidth = (size.width - inset * 2f)
                                    .coerceAtLeast(0f) * sessionLoadingProgress

                                // Trilho escuro e contorno metalico.
                                drawRoundRect(
                                    color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xEC0B0B0D),
                                    cornerRadius = CornerRadius(radius)
                                )
                                drawRoundRect(
                                    brush = sessionMetal,
                                    cornerRadius = CornerRadius(radius),
                                    style = Stroke(width = 0.6.dp.toPx())
                                )
                                if (fillWidth > 1f && innerHeight > 1f) {
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF7A5215),
                                                br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFE6A0),
                                                br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD6A32E),
                                                br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFF5CD),
                                                br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF926315)
                                            ),
                                            startX = inset,
                                            endX = inset + fillWidth
                                        ),
                                        topLeft = Offset(inset, inset),
                                        size = Size(fillWidth, innerHeight),
                                        cornerRadius = CornerRadius(innerHeight / 2f)
                                    )
                                    // Reflexo em movimento, sem mudar a cor dourada.
                                    val lightX = inset + shimmer * fillWidth
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xA8FFFFFF),
                                                br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0x55FFEAAA),
                                                Color.Transparent
                                            ),
                                            center = Offset(lightX, size.height / 2f),
                                            radius = size.height * 1.6f
                                        ),
                                        radius = size.height * 1.6f,
                                        center = Offset(lightX, size.height / 2f)
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                            Text(
                                text = errorMessage,
                                color = if(kidsLoading)Color(0xFFC1C4CE)else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.secondary,
                                fontSize = if (compact) 11.sp else 14.sp,
                                style = TextStyle(textAlign = TextAlign.Center)
                            )
                            Spacer(Modifier.height(if (compact) 12.dp else 19.dp))
                            val retryFocus=remember {FocusRequester()}
                            LaunchedEffect(errorMessage) {withFrameNanos {};retryFocus.requestFocus()}
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetallicLoadingAction("TENTAR NOVAMENTE", onRetry,Modifier.focusRequester(retryFocus))
                                MetallicLoadingAction("VOLTAR", onBack)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CinematicCarouselCard(
    poster: CinematicCarouselPoster,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    selectedMovie: Boolean,
    selectionGlow: androidx.compose.runtime.State<Float>,
    zoom: androidx.compose.runtime.State<Float>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(MoviePosterLoader.peekBitmap(poster.posterUrl), poster.posterUrl, selectedMovie) {
        // Secondary cards read RAM/local files even while selected-media networking is busy.
        // They never trigger remote loads or the legacy network fallback.
        value = MoviePosterLoader.loadCached(context.applicationContext, poster.posterUrl)
        if (value == null && selectedMovie) {
            value = MoviePosterLoader.load(context.applicationContext, poster.posterUrl)
        }
    }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier.width(width).height(height)
            .shadow(elevation = if (selectedMovie) 10.dp else 5.dp,
                shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF202C40), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF090B10))
                )
            )
            .drawWithContent {
                drawContent()
                if (selectedMovie) {
                    // Borda so aparece no clique final, nao durante o giro.
                    // Leitura no DRAW: atualiza sem recompor todas as capas.
                    val glow = selectionGlow.value * (1f - zoom.value * 0.82f)
                    if (glow > 0.005f) {
                        drawRoundRect(
                            brush = LoadingMetallicGold,
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx()),
                            alpha = glow.coerceIn(0f, 1f)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = poster.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = poster.title,
                color = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFE6D3A3),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.padding(7.dp)
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x09000000), Color.Transparent, br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x80000000))
                )
            )
        )
    }
}

/**
 * Cache de posters: arquivos originais codificados, com hash SHA256 da URL.
 * Reutiliza o diretorio de imagens existente e limita a concorrencia de rede.
 * Nenhuma consulta TMDB em massa; apenas imagens dos cards ja carregados.
 */
private object FilmstripPosterCache {
    fun trimMemory(fraction: Float = .5f) {
        memory.trimToSize((memory.maxSize() * fraction.coerceIn(0f, 1f)).toInt())
    }
    private val slots = Semaphore(3)
    private val memory = object : LruCache<String, ImageBitmap>(br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.carouselMb * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return value.asAndroidBitmap().allocationByteCount
        }
    }

    suspend fun load(context: Context, rawUrl: String?): ImageBitmap? {
        val budget = br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.forContext(context).carouselMb * 1024 * 1024
        if (memory.maxSize() != budget) memory.resize(budget)
        val url = rawUrl?.trim()?.takeIf {
            it.startsWith("https://", true) || it.startsWith("http://", true)
        } ?: return null
        memory.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            memory.get(url)?.let { return@withContext it }
            slots.withPermit {
                memory.get(url)?.let { return@withPermit it }
                val dir = File(context.applicationContext.cacheDir, "movie_details_images")
                if (!dir.isDirectory && !dir.mkdirs() && !dir.isDirectory) {
                    return@withPermit null
                }
                val hash = MessageDigest.getInstance("SHA-256")
                    .digest(url.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                val file = File(dir, "$hash.img")
                br.com.flaviotorres.iptvpremium.core.storage.ImageDiskCache.get(context).retain(file).use {
                    if (!file.isFile || file.length() <= 0L) {
                        if (!download(context, url, file)) return@withPermit null
                    }
                    val bitmap = try {
                        decodeSmall(file)
                    } catch (_: OutOfMemoryError) {
                        memory.evictAll()
                        null
                    } catch (_: Exception) {
                        null
                    }
                    if (bitmap != null) {
                        file.setLastModified(System.currentTimeMillis())
                        memory.put(url, bitmap)
                    }
                    bitmap
                }
            }
        }
    }

    private fun decodeSmall(file: File): ImageBitmap? {
        return br.com.flaviotorres.iptvpremium.data.cache.PosterResolution
            .decode(file, Bitmap.Config.RGB_565)?.asImageBitmap()
    }

    private suspend fun download(context: Context, url: String, destination: File): Boolean {
        var reservation: br.com.flaviotorres.iptvpremium.core.storage.CacheStorageBudget.Reservation? = null
        var conn: HttpURLConnection? = null
        var partial: File? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 7_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            conn.connect()
            if (conn.responseCode !in 200..299 ||
                conn.contentLengthLong > 8L * 1024 * 1024
            ) return false
            reservation = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
                .reserve(destination, conn.contentLengthLong.takeIf { it > 0 }?.coerceAtLeast(1_000_000L) ?: 8L * 1024 * 1024) ?: return false
            val temp = reservation.temporary
            partial = temp
            var total = 0L
            conn.inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val size = input.read(buffer)
                        if (size < 0) break
                        total += size
                        if (total > reservation.limit) return false
                        output.write(buffer, 0, size)
                    }
                }
            }
            total > 0L &&
                br.com.flaviotorres.iptvpremium.data.cache.PosterResolution.prepareDownloaded(temp, reservation.limit) &&
                (temp.renameTo(destination) || destination.isFile)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        } finally {
            runCatching { conn?.disconnect() }
            partial?.delete()
            reservation?.close()
        }
    }
}

@Composable
private fun MetallicLoadingAction(
    text: String,
    onClick: () -> Unit,
    modifier:Modifier=Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .background(
                color = if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) {
                    if(focused)br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.panel
                } else if (focused) { br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color() } else {
                    br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF131315)
                },
                shape = shape
            )
            .border(
                border = BorderStroke(
                    width = if (focused) 2.dp else 1.dp,
                    brush = LoadingMetallicGold
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 15.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)
            )
        )
    }
}


/**
 * Historico minimo para ligar Assistir nas series sem alterar o player atual.
 * Salva apenas IDs e posicao; as URLs e credenciais ficam no provedor.
 * Apos a etapa de Continuar Assistindo, esse historico sera unificado.
 */
private class SeriesAssistirHistory(context: Context) {
    private val preferences = context.getSharedPreferences(
        "iptvpremium_series_assistir_v1", Context.MODE_PRIVATE
    )

    // Usa comprimento + codificacao para manter IDs do provedor sem colisao
    // entre playlists diferentes, series diferentes e episodios diferentes.
    private fun scope(playlistId: String, seriesId: String): String =
        "${playlistId.length}:$playlistId:${seriesId.length}:$seriesId"

    private fun positionKey(playlistId: String, seriesId: String, episodeId: String): String =
        "position:${scope(playlistId, seriesId)}:${episodeId.length}:$episodeId"

    fun getLastEpisodeId(playlistId: String, seriesId: String): String? =
        preferences.getString("last:${scope(playlistId, seriesId)}", null)
            ?.takeIf { it.isNotBlank() }

    fun rememberEpisode(playlistId: String, seriesId: String, episodeId: String) {
        preferences.edit()
            .putString("last:${scope(playlistId, seriesId)}", episodeId)
            .apply()
    }

    fun getPosition(playlistId: String, seriesId: String, episodeId: String): Long =
        preferences.getLong(positionKey(playlistId, seriesId, episodeId), 0L)
            .coerceAtLeast(0L)

    // Reinicia somente este episodio, preservando o ultimo episodio da serie.
    fun clearPosition(playlistId: String, seriesId: String, episodeId: String) {
        preferences.edit().remove(positionKey(playlistId, seriesId, episodeId)).apply()
    }

    fun savePosition(
        playlistId: String,
        seriesId: String,
        episodeId: String,
        positionMs: Long
    ) {
        // Ao sair enquanto o video ainda carrega, nao apagar uma posicao
        // salva anteriormente por causa de um resultado 0 ms.
        val key = positionKey(playlistId, seriesId, episodeId)
        val safePosition = if (positionMs <= 0L && preferences.contains(key)) {
            preferences.getLong(key, 0L)
        } else positionMs.coerceAtLeast(0L)
        preferences.edit()
            .putString("last:${scope(playlistId, seriesId)}", episodeId)
            .putLong(key, safePosition)
            .apply()
    }
}


/**
 * Retomada de filmes, isolada do historico de episodios e do cache do catalogo.
 * IDs sao compostos com comprimento para evitar colisao entre playlists.
 * Salva apenas ID e posicao; nunca URLs/credenciais.
 */
private class MovieAssistirHistory(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "iptvpremium_movies_assistir_v1", Context.MODE_PRIVATE
    )

    private fun key(playlistId: String, movieId: String): String =
        "movie:${playlistId.length}:$playlistId:${movieId.length}:$movieId"

    fun getPosition(playlistId: String, movieId: String): Long =
        prefs.getLong(key(playlistId, movieId), 0L).coerceAtLeast(0L)

    fun clearPosition(playlistId: String, movieId: String) {
        prefs.edit().remove(key(playlistId, movieId)).apply()
    }

    fun savePosition(playlistId: String, movieId: String, positionMs: Long) {
        if (playlistId.isBlank() || movieId.isBlank()) return
        val itemKey = key(playlistId, movieId)
        val previousMs = prefs.getLong(itemKey, 0L)
        // Um 'Voltar' antes de o stream iniciar nao apaga o historico.
        val safePositionMs = when {
            positionMs > 0L -> positionMs
            else -> previousMs
        }
        prefs.edit().putLong(itemKey, safePositionMs).apply()
    }
}

/**
 * Escolha de retomada dos filmes e episodios. A mesma identidade de vidro
 * escuro, linha fina e botoes iluminados da barra do PlaybackScreen.
 * O titulo do filme/episodio nao aparece nesta janela, de proposito.
 */
@Composable
private fun PlaybackResumeChoice(
    positionMs: Long,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val continueFocus = remember { FocusRequester() }
    val compact = LocalConfiguration.current.screenWidthDp < 700
    val gold = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFD762)
    val panelShape = RoundedCornerShape(if (compact) 18.dp else 22.dp)

    LaunchedEffect(positionMs) {
        withFrameNanos { }
        runCatching { continueFocus.requestFocus() }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF060809), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF11161A), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF060809))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (compact) 0.92f else 0.69f)
                .clip(panelShape)
                // Camadas suaves de vidro, sem a placa opaca de antes.
                .background(
                    Brush.verticalGradient(
                        listOf(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xB222292D), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xC90B0F13), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xD406080B))
                    )
                )
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.075f), Color.Transparent)
                    )
                )
                .border(
                    0.9.dp,
                    Color.White.copy(alpha = 0.27f),
                    panelShape
                )
                .padding(
                    horizontal = if (compact) 16.dp else 26.dp,
                    vertical = if (compact) 22.dp else 25.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 13.dp else 16.dp)
        ) {
            Text(
                text = "CONTINUAR ASSISTINDO",
                color = gold,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 12.sp else 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Você parou em ${formatMovieResumeTime(positionMs)}",
                color = Color.White.copy(alpha = 0.91f),
                fontSize = if (compact) 14.sp else 17.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Como deseja assistir?",
                color = Color.White.copy(alpha = 0.66f),
                fontSize = if (compact) 12.sp else 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(if (compact) 0.dp else 2.dp))
            if (compact) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlaybackResumeButton(
                        title = "Continuar",
                        icon = ResumeButtonIcon.PLAY,
                        focusRequester = continueFocus,
                        onClick = onContinue
                    )
                    PlaybackResumeButton(
                        title = "Iniciar novamente",
                        icon = ResumeButtonIcon.RESTART,
                        onClick = onRestart
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaybackResumeButton(
                        title = "Continuar",
                        icon = ResumeButtonIcon.PLAY,
                        focusRequester = continueFocus,
                        onClick = onContinue
                    )
                    PlaybackResumeButton(
                        title = "Iniciar novamente",
                        icon = ResumeButtonIcon.RESTART,
                        onClick = onRestart
                    )
                }
            }
        }
    }
}

private enum class ResumeButtonIcon { PLAY, RESTART }

/** Botoes em formato capsula, com vidro fumê e foco ambar do player. */
@Composable
private fun PlaybackResumeButton(
    title: String,
    icon: ResumeButtonIcon,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val gold = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFD762)
    val requestModifier = if (focusRequester == null) Modifier
    else Modifier.focusRequester(focusRequester)

    Row(
        modifier = Modifier
            .then(requestModifier)
            .onFocusChanged { focused = it.isFocused }
            .shadow(
                elevation = if (focused) 5.dp else 0.dp,
                shape = shape,
                ambientColor = gold.copy(alpha = if (focused) 0.21f else 0f),
                spotColor = gold.copy(alpha = if (focused) 0.20f else 0f)
            )
            .clip(shape)
            .background(
                if (focused) { br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.brush() } else {
                    Brush.verticalGradient(
                        listOf(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x86262D33), br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x9B0C1116))
                    )
                }
            )
            .border(
                width = if (focused) 1.5.dp else 0.9.dp,
                color = if (focused) gold else Color.White.copy(alpha = 0.30f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .focusable()
            .height(48.dp)
            .padding(horizontal = 21.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlaybackResumeIcon(
            icon = icon,
            color = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

/** Icones vetoriais no estilo dos controles de reproducao. */
@Composable
private fun PlaybackResumeIcon(
    icon: ResumeButtonIcon,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (icon) {
            ResumeButtonIcon.PLAY -> {
                val triangle = Path().apply {
                    moveTo(w * 0.26f, h * 0.15f)
                    lineTo(w * 0.85f, h * 0.50f)
                    lineTo(w * 0.26f, h * 0.85f)
                    close()
                }
                drawPath(triangle, color)
            }
            ResumeButtonIcon.RESTART -> {
                // Seta circular de reiniciar: traço continuo e ponta ligada
                // ao arco, sem o antigo triangulo desconectado.
                val restartStroke = Stroke(
                    width = w * 0.105f,
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
                val circularArrow = Path().apply {
                    moveTo(w * 0.18f, h * 0.44f)
                    cubicTo(w * 0.22f, h * 0.25f, w * 0.35f, h * 0.16f, w * 0.53f, h * 0.16f)
                    cubicTo(w * 0.73f, h * 0.16f, w * 0.85f, h * 0.31f, w * 0.85f, h * 0.51f)
                    cubicTo(w * 0.85f, h * 0.71f, w * 0.70f, h * 0.85f, w * 0.50f, h * 0.85f)
                    cubicTo(w * 0.35f, h * 0.85f, w * 0.24f, h * 0.76f, w * 0.19f, h * 0.63f)
                }
                drawPath(circularArrow, color, style = restartStroke)
                val arrowHead = Path().apply {
                    moveTo(w * 0.18f, h * 0.18f)
                    lineTo(w * 0.18f, h * 0.44f)
                    lineTo(w * 0.44f, h * 0.44f)
                }
                drawPath(arrowHead, color, style = restartStroke)
            }
        }
    }
}

private fun formatMovieResumeTime(positionMs: Long): String {
    val seconds = positionMs.coerceAtLeast(0L) / 1000L
    val hours = seconds / 3600L
    val minutes = (seconds % 3600L) / 60L
    val rest = seconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, rest)
    }
}

// IPTV_HDR_AUTONEXT_V1

// IPTV_RESUME_CINEMA_V1

// IPTV_RESUME_PANORAMA_V2

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_POLISH_LOADING_HOLD_V1

// IPTV_LISTS_SUBTITLES_CHANNELS_V1

// IPTV_SUBTITLE_LAYOUT_PORTAL_LOAD_V1

// IPTV_LIST_LOADING_AND_PLAYER_INPUT_V1

// IPTV_REMOVE_POSTER_PRELOAD_V1

// IPTV_AUTOMATIC_DOLBY_AND_EPISODE_CONTROLS_USER26_V2

// IPTV_SETTINGS_APPROVED_GRID_V1

// Text retains the approved palette; blue darkening applies to rims.
private val LoadingMetallicGoldText get() = Brush.linearGradient(
    colorStops = arrayOf(
        0.00f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF78530F),
        0.18f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD4AF37),
        0.37f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE991),
        0.52f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFF7CE),
        0.70f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFE3B83F),
        1.00f to br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF916914)
    )
)
