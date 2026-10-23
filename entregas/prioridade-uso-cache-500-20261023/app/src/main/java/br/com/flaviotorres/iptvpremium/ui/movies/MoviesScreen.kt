package br.com.flaviotorres.iptvpremium.ui.movies

import br.com.flaviotorres.iptvpremium.data.cache.PosterKind

import br.com.flaviotorres.iptvpremium.ui.kids.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.wrapContentWidth
import br.com.flaviotorres.iptvpremium.ui.components.posterSelectionFrame
import br.com.flaviotorres.iptvpremium.ui.components.posterSelectionShadow
import br.com.flaviotorres.iptvpremium.ui.components.reinforcedCatalogFocus

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.compose.foundation.basicMarquee
import br.com.flaviotorres.iptvpremium.ui.components.catalogGridHost
import br.com.flaviotorres.iptvpremium.ui.components.CatalogGridNavigator
import br.com.flaviotorres.iptvpremium.ui.components.catalogGridNavigation
import br.com.flaviotorres.iptvpremium.ui.components.BindCatalogFocus
import br.com.flaviotorres.iptvpremium.ui.components.CatalogPosterPlaceholder
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.ui.graphics.asAndroidBitmap
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import br.com.flaviotorres.iptvpremium.ui.components.catalogEscapeBack
import br.com.flaviotorres.iptvpremium.ui.components.LocalCatalogRowSpacing
import br.com.flaviotorres.iptvpremium.ui.components.LocalCatalogCardHeight
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource as painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.domain.model.Movie
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import br.com.flaviotorres.iptvpremium.ui.components.AppKeyboard
import br.com.flaviotorres.iptvpremium.ui.components.TvKeyboardSearchText
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardType
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardDelete
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardMoveCursor
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

private const val FAVORITE_LONG_PRESS_MS = 1_000L
private const val FAVORITE_FOCUS_RESTORE_WINDOW_MS = 420L

// Apenas 5 capas por lote. Nao aumenta a quantidade preparada na Home;
// a navegacao ja prioriza as 5 primeiras capas quando entra na categoria.
// Ao rolar, a antecipacao avanca de 5 em 5 sem requisitar novas paginas.

private val CinzelSemiBold = FontFamily(
    Font(
        resId = R.font.cinzel_semibold,
        weight = FontWeight.SemiBold
    )
)

private val MetallicGoldBrush get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF926315))
)

private val AppGold get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFD6A32E)
private val AppGoldLight get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFE6A0)

// Mesmas cores, metalização e brilho usados pela tela de Canais.
private val PremiumGoldBorder get() = Brush.linearGradient(
    listOf(
        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF7A5215),
        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFE6A0),
        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD6A32E),
        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFF5CD),
        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF926315)
    )
)

private val PremiumPanelBackground get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.panelBrush

private fun Modifier.premiumGlow(enabled: Boolean, roundedDp: Float = 12f): Modifier = this

// Mesmo contorno dourado das categorias de Canais. Preserva os icones e o foco.
private fun Modifier.premiumCategoryCardBorder(
    highlighted: Boolean,
    roundedDp: Float = 12f
): Modifier = if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) {
    if(highlighted) this.reinforcedCatalogFocus(true, roundedDp) else this.border(.8.dp, br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categoryEdge, RoundedCornerShape(roundedDp.dp))
} else this.drawBehind {
    val radius = CornerRadius(roundedDp.dp.toPx())
    val outer = PremiumGoldBorder
    val outerStroke = if (highlighted) 1.8.dp.toPx() else 1.2.dp.toPx()

    drawRoundRect(
        brush = outer,
        cornerRadius = radius,
        style = Stroke(width = outerStroke)
    )

}.reinforcedCatalogFocus(highlighted, roundedDp)

private val PremiumQuietEdge get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF926315)).map { it.copy(alpha = 0.55f) }
)

@Composable
private fun PremiumDivider(compactLayout: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compactLayout) 11.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, AppGold)))
        )
        Text(
            text = "◇✦◇",
            color = AppGold,
            fontSize = if (compactLayout) 9.sp else 11.sp,
            modifier = Modifier.padding(horizontal = if (compactLayout) 5.dp else 8.dp)
        )
        Box(
            Modifier.weight(1f).height(1.dp)
                .background(Brush.horizontalGradient(listOf(AppGold, Color.Transparent)))
        )
    }
}

private enum class MovieCategory(
    val title: String,
    val brandText: String,
    val brandColor: Color
) {
    ALL("Todos", "▦", Color(0xFF2FA8FF)),
    CONTINUE_WATCHING("Continuar assistindo", "▶", br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFC928)),
    FAVORITES("Favoritos", "♥", Color(0xFFFF3045)),
    RECENTLY_ADDED("Adicionados recentemente", "◷", Color(0xFF1ED98B)),

    RELEASES("Lançamentos", "★", br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFC928)),
    SUBTITLED("Legendados", "▣", br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFC400)),
    UHD_4K("4K", "4K", Color(0xFF00D0FF)),
    CINEMA("Cinema", "◉", br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFF8A3D)),
    ACTION("Ação", "⚡", Color(0xFFFF5A5A)),
    ANIMATION("Animação", "✦", Color(0xFF9B7CFF)),
    ADVENTURE("Aventura", "▲", Color(0xFF4DD98B)),
    COMEDY("Comédia", "☺", br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFD54A)),
    CRIME("Crime", "◆", br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD)),

    NETFLIX("Netflix", "N", Color(0xFFE50914)),
    MAX("Max", "max", Color(0xFFA855F7)),
    DISNEY_PLUS("Disney Plus", "Disney+", Color(0xFF28C7FA)),
    PRIME_VIDEO("Amazon Prime Video", "prime", Color(0xFF21A8E8)),
    GLOBOPLAY("Globoplay", "globoplay", Color(0xFFFF3264)),
    DISCOVERY_PLUS("Discovery Plus", "D+", Color(0xFF49D7CA)),
    APPLE_TV_PLUS("Apple TV Plus", "TV+", br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5))
}

private enum class SortMode {
    DEFAULT,
    A_Z,
    Z_A
}

private data class PreviewMovie(
    val id: String,
    val name: String,
    val category: MovieCategory,
    val providerCategory: MovieCategory
)

@Composable
fun MoviesScreen(
    favoriteMovieIds: Set<String> = emptySet(),
    onMovieClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    pagedMovies: LazyPagingItems<Movie>? = null,
    pagingCategories: List<Category> = emptyList(),
    totalMovieCount: Int = 0,
    pagingCategoryCounts: Map<String?, Int> = emptyMap(),
    onCategoryAccessRequested: (String?, String, () -> Unit) -> Unit = { _, _, proceed -> proceed() },
    onSourceCategorySelected: (String?) -> Unit = {},
    onContinueWatchingSelected: () -> Unit = {},
    watchHistoryEntries: List<WatchHistoryEntry> = emptyList(),
    onFavoritesSelected: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSortModeChanged: (String) -> Unit = {},
    onRemoveFromContinue: (String) -> Unit = {},
    onCatalogInteraction: (Boolean) -> Unit = {},
    posterPage: (suspend (Int) -> br.com.flaviotorres.iptvpremium.data.cache.PosterPage)? = null,
    onResolvePosterCategory: (suspend (String)->String?)? = null
) {
    // SaveableStateHolder do NavHost restaura a categoria ao voltar dos detalhes.
    // Uma nova entrada em Filmes continua nascendo em "Todos".
    var selectedCategory by rememberSaveable {
        mutableStateOf(MovieCategory.ALL)
    }
    val movieCategoryListState = rememberLazyListState()
    val categoryFirstFocusRequester = remember { FocusRequester() }
    val menuFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    var catalogContentFocused by remember { mutableStateOf(false) }
    var categoryMenuActive by rememberSaveable { mutableStateOf(true) }
    var returningToCategory by remember { mutableStateOf(false) }
    val selectedCategoryFocus = remember { FocusRequester() }
    val catalogInputMode = androidx.compose.ui.platform.LocalInputModeManager.current

    var selectedSourceCategoryId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val useRoomPaging =
        pagedMovies != null

    /*
     * O catálogo original permanece intacto no Room.
     *
     * A apresentação utiliza todas as categorias e todos os filmes
     * disponíveis na fonte, sem bloqueios ou filtros de conteúdo
     * baseados no nome da categoria.
     */
    val visiblePagingCategories =
        pagingCategories
    val posterCategoryNames=remember(pagingCategories){pagingCategories.associate{it.id to it.name}}

    val visibleTotalMovieCount =
        totalMovieCount

    val movieHistory = remember(watchHistoryEntries) {
        watchHistoryEntries.filter { it.contentType == PlaybackContentType.MOVIE }
            .sortedByDescending { it.updatedAtMs }
    }
    val continueMovies = remember(movieHistory) { movieHistory.filter { it.inProgress } }
    val progressByMovie = remember(movieHistory) {
        movieHistory.associate { it.contentId to (it.fraction ?: 0f) }
    }

    // Salva o ID do ultimo card aberto enquanto a rota de Filmes esta no back stack.
    var selectedMovieId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // Solicitacao de foco feita APENAS ao entrar novamente na rota de Filmes.
    // Nao salva o pedido em si, evitando focos inesperados ao trocar categorias.
    var returningMovieFocusId by remember { mutableStateOf<String?>(null) }
    var returningMovieFocusRequest by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        if (selectedMovieId == null) {
            categoryMenuActive = true
            br.com.flaviotorres.iptvpremium.ui.components.restoreCatalogCategoryFocus(
                movieCategoryListState, 0, categoryFirstFocusRequester, menuFocusRequester, catalogInputMode)
        }
        selectedMovieId?.let { movieId ->
            categoryMenuActive = false
            returningMovieFocusId = movieId
            returningMovieFocusRequest++
        }
    }

    // Um pedido novo acompanha cada troca de categoria; a primeira capa
    // so solicita foco DEPOIS de existir na nova geracao do Paging.
    var pendingFirstMovieFocus by remember { mutableStateOf(false) }
    var firstMovieFocusRequest by remember { mutableLongStateOf(0L) }

    // Quando o Paging recria os cards depois de favoritar, restauramos
    // o foco somente no filme que estava sob o controle remoto.
    var favoriteFocusMovieId by remember {
        mutableStateOf<String?>(null)
    }
    var favoriteFocusRequest by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(favoriteFocusRequest) {
        if (favoriteFocusRequest > 0L) {
            delay(FAVORITE_FOCUS_RESTORE_WINDOW_MS)
            favoriteFocusMovieId = null
        }
    }

    var searchText by rememberSaveable {
        mutableStateOf("")
    }

    var searchKeyboardVisible by remember {
        mutableStateOf(false)
    }
    var appliedSearchText by rememberSaveable { mutableStateOf(searchText) }
    LaunchedEffect(searchText) {
        if (searchText.isNotBlank()) delay(br.com.flaviotorres.iptvpremium.data.cache.CATALOG_SEARCH_SETTLE_MS)
        appliedSearchText = searchText
    }


    // Posicao da letra destacada na busca. Quando esta no fim,
    // o componente compartilhado mostra uma barra dourada piscante.
    var searchCursorPosition by rememberSaveable {
        mutableIntStateOf(0)
    }

    var sortMode by rememberSaveable {
        mutableStateOf(SortMode.DEFAULT)
    }

    /*
     * ESTADO DE ROLAGEM DA GRADE
     *
     * rememberLazyGridState ja guarda/restaura posicao via SaveableStateHolder.
     * Nao vamos mais mandar a grade ao topo ao voltar dos detalhes.
     * Somente uma MUDANCA REAL de categoria, busca ou ordenacao reinicia a lista.
     */
    val moviesGridState =
        rememberLazyGridState()

    val posterCacheContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    // Intervalo ocioso conservador na Android TV para nao competir com o foco.
    // Mantem a pausa da navegacao; grupos de cinco formam a reserva de quinze.
    val posterIdleDelayMs = remember(posterCacheContext) {
        val mode = posterCacheContext.resources.configuration.uiMode and
                Configuration.UI_MODE_TYPE_MASK
        val isTv = mode == Configuration.UI_MODE_TYPE_TELEVISION ||
                posterCacheContext.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        if (isTv) 100L else 80L
    }

    // Sinal fora do estado de composicao: mover o foco pelo controle remoto
    // cancela imediatamente a antecipacao, sem recompor toda a tela.
    val posterFocusActivity = remember { MutableStateFlow(0L) }
    LaunchedEffect(moviesGridState) {
        combine(snapshotFlow { searchKeyboardVisible || moviesGridState.isScrollInProgress }, posterFocusActivity) { busy, _ -> busy }
            .collectLatest { busy ->
                onCatalogInteraction(true)
                if (!busy) { delay(180L); onCatalogInteraction(false) }
            }
    }
    DisposableEffect(Unit) { onDispose { onCatalogInteraction(false) } }


    // Ao selecionar outra categoria, evita mostrar a pagina da categoria
    // anterior enquanto o Room/Paging entrega a nova geracao.
    var categoryChangeSerial by remember { mutableIntStateOf(0) }
    var oldPagingSnapshot by remember { mutableStateOf<Any?>(null) }
    var awaitingCategoryPage by remember { mutableStateOf(false) }

    fun startCategoryChange() {
        val paging = pagedMovies ?: return
        oldPagingSnapshot = paging.itemSnapshotList
        awaitingCategoryPage = true
        categoryChangeSerial++
    }

    LaunchedEffect(categoryChangeSerial, pagedMovies) {
        val previous = oldPagingSnapshot
        val paging = pagedMovies
        if (!awaitingCategoryPage || previous == null || paging == null) return@LaunchedEffect

        // O snapshot da nova geracao substitui o anterior. Um limite protege
        // categorias vazias ou erros de consulta contra espera indefinida.
        withTimeoutOrNull(2_500L) {
            snapshotFlow { paging.itemSnapshotList }
                .first { snapshot -> snapshot !== previous }
        }
        awaitingCategoryPage = false
        oldPagingSnapshot = null
    }

    val posterQueueJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    fun openCatalogMovie(id: String) {
        posterQueueJob[0]?.cancel()
        onMovieClick(id)
    }

    br.com.flaviotorres.iptvpremium.ui.movies.CategoryPosterPrefetch(
        br.com.flaviotorres.iptvpremium.data.cache.PosterKind.MOVIES,
        "${selectedCategory.name}:${selectedSourceCategoryId.orEmpty()}:$searchText:$sortMode",
        moviesGridState, pagedMovies, { it.posterUrl },
        searchKeyboardVisible || awaitingCategoryPage || selectedCategory == MovieCategory.CONTINUE_WATCHING,
        posterPage
    )

    // A chave anterior e salva junto ao NavBackStackEntry, para distinguir
    // retorno dos detalhes de uma troca de filtro feita pelo usuario.
    val currentCategoryKey = "${selectedCategory.name}:${selectedSourceCategoryId.orEmpty()}"
    val currentFilterKey = "${useRoomPaging}|${currentCategoryKey}|${appliedSearchText}|${sortMode.name}"
    var lastCategoryKey by rememberSaveable { mutableStateOf<String?>(null) }
    var lastFilterKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentFilterKey) {
        val previousFilter = lastFilterKey
        val previousCategory = lastCategoryKey
        lastFilterKey = currentFilterKey
        lastCategoryKey = currentCategoryKey

        // Primeira composicao (inclusive ao voltar dos detalhes): mantem
        // o scroll e o card selecionado, sem forcá-los ao primeiro item.
        if (previousFilter == null || previousFilter == currentFilterKey) {
            return@LaunchedEffect
        }

        moviesGridState.scrollToItem(0)
        selectedMovieId = null
        returningMovieFocusId = null
        favoriteFocusMovieId = null

        if (previousCategory != currentCategoryKey) {
            // Ao entrar novamente na categoria A apos visitar B, foca
            // o primeiro filme da categoria A, nunca a posicao antiga.
            pendingFirstMovieFocus = true
            firstMovieFocusRequest++
        } else {
            pendingFirstMovieFocus = false
        }
    }


    val previewMovies = remember {
        listOf(
            PreviewMovie("1", "Homem-Aranha: Um Novo Dia", MovieCategory.RELEASES, MovieCategory.NETFLIX),
            PreviewMovie("2", "Obsessão 4K", MovieCategory.UHD_4K, MovieCategory.MAX),
            PreviewMovie("3", "Os Horrores do Lago", MovieCategory.CRIME, MovieCategory.PRIME_VIDEO),
            PreviewMovie("4", "O Diabo Veste Prada", MovieCategory.COMEDY, MovieCategory.DISNEY_PLUS),
            PreviewMovie("5", "A Princesa Prometida", MovieCategory.ADVENTURE, MovieCategory.APPLE_TV_PLUS),
            PreviewMovie("6", "Batman Begins", MovieCategory.ACTION, MovieCategory.MAX),
            PreviewMovie("7", "Batman: A Queda do Morcego", MovieCategory.ANIMATION, MovieCategory.NETFLIX),
            PreviewMovie("8", "Nasci para Brilhar", MovieCategory.COMEDY, MovieCategory.GLOBOPLAY),
            PreviewMovie("9", "Noite Sem Fim", MovieCategory.SUBTITLED, MovieCategory.DISCOVERY_PLUS),
            PreviewMovie("10", "Última Sessão", MovieCategory.CINEMA, MovieCategory.PRIME_VIDEO)
        )
    }

    val categoryFilteredMovies = when (selectedCategory) {
        MovieCategory.CONTINUE_WATCHING -> previewMovies.take(3)

        MovieCategory.FAVORITES -> {
            previewMovies.filter { movie ->
                movie.id in favoriteMovieIds
            }
        }

        MovieCategory.ALL -> previewMovies

        MovieCategory.RECENTLY_ADDED -> previewMovies.takeLast(6)

        MovieCategory.NETFLIX,
        MovieCategory.MAX,
        MovieCategory.DISNEY_PLUS,
        MovieCategory.PRIME_VIDEO,
        MovieCategory.GLOBOPLAY,
        MovieCategory.DISCOVERY_PLUS,
        MovieCategory.APPLE_TV_PLUS -> {
            previewMovies.filter { movie ->
                movie.providerCategory == selectedCategory
            }
        }

        else -> {
            previewMovies.filter { movie ->
                movie.category == selectedCategory
            }
        }
    }

    val searchedMovies =
        if (searchText.isBlank()) {
            categoryFilteredMovies
        } else {
            categoryFilteredMovies.filter { movie ->
                movie.name.contains(
                    searchText.trim(),
                    ignoreCase = true
                )
            }
        }

    val displayedMovies =
        when (sortMode) {
            SortMode.DEFAULT -> searchedMovies
            SortMode.A_Z -> searchedMovies.sortedBy { it.name.lowercase() }
            SortMode.Z_A -> searchedMovies.sortedByDescending { it.name.lowercase() }
        }

    val gridColumns = br.com.flaviotorres.iptvpremium.ui.settings.rememberSettingsPreferences().catalogColumns()
    val navScope = androidx.compose.runtime.rememberCoroutineScope()
    val navCount by rememberUpdatedState(if (selectedCategory == MovieCategory.CONTINUE_WATCHING) continueMovies.size else pagedMovies?.itemCount ?: displayedMovies.size)
    val navLoad by rememberUpdatedState<(Int) -> Unit>({ index -> if (selectedCategory != MovieCategory.CONTINUE_WATCHING) { pagedMovies?.get(index); Unit } })
    val navManual by rememberUpdatedState<() -> Unit>({
        pendingFirstMovieFocus = false; returningMovieFocusId = null; favoriteFocusMovieId = null
        posterFocusActivity.value++
    })
    val gridNavigator = remember(moviesGridState, gridColumns) {
        CatalogGridNavigator(moviesGridState, navScope, gridColumns, { navCount }, { navLoad(it) }, { navManual() },
            onTop = { searchFocusRequester.requestFocus() },
            onLeft = { categoryMenuActive = true; menuFocusRequester.requestFocus() })
    }
    DisposableEffect(gridNavigator) { onDispose { gridNavigator.cancel() } }
    LaunchedEffect(currentFilterKey) { gridNavigator.filterChanged(currentFilterKey) }
    LaunchedEffect(searchKeyboardVisible) { gridNavigator.cancel() }

    fun handleCatalogBack(): Boolean {
        if (searchKeyboardVisible) {
            searchKeyboardVisible = false
            searchFocusRequester.requestFocus()
            return true
        }
        if (returningToCategory) return true
        if (categoryMenuActive && !catalogContentFocused) return false
        gridNavigator.cancel()
        navManual()
        categoryMenuActive = true
        returningToCategory = true
        val index = if (useRoomPaging) when {
            selectedSourceCategoryId != null -> 3 + visiblePagingCategories.indexOfFirst { it.id == selectedSourceCategoryId }.coerceAtLeast(0)
            selectedCategory == MovieCategory.CONTINUE_WATCHING -> 1
            selectedCategory == MovieCategory.FAVORITES -> 2
            else -> 0
        } else MovieCategory.entries.filterNot { it == MovieCategory.SUBTITLED }.indexOf(selectedCategory).coerceAtLeast(0)
        navScope.launch {
            try {
                br.com.flaviotorres.iptvpremium.ui.components.restoreCatalogCategoryFocus(
                    movieCategoryListState, index, selectedCategoryFocus, menuFocusRequester, catalogInputMode)
            } finally { returningToCategory = false }
        }
        return true
    }
    BackHandler(enabled = searchKeyboardVisible || returningToCategory || !categoryMenuActive || catalogContentFocused) {
        handleCatalogBack()
    }

    BoxWithConstraints(
        modifier = Modifier
            .catalogEscapeBack(onBack = ::handleCatalogBack)
            .fillMaxSize()
            .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF070708))
    ) {
        val compactLayout =
            maxHeight < 650.dp ||
                    maxWidth < 1200.dp

        val outerHorizontalPadding =
            if (compactLayout) 12.dp else 20.dp

        val outerVerticalPadding =
            if (compactLayout) 12.dp else 20.dp

        val sidebarWidth =
            if (compactLayout) 245.dp else 280.dp

        val sidebarGap =
            if (compactLayout) 12.dp else 18.dp

        val categoryHeight =
            if (compactLayout) 38.dp else 42.dp

        val categoryGap =
            if (compactLayout) 5.dp else 7.dp

        val categoryFontSize =
            if (compactLayout) 11.sp else 14.sp

        val posterAspectRatio =
            if (compactLayout) 0.72f else 0.67f

        val gridHorizontalSpacing =
            if (compactLayout) 10.dp else 16.dp

        val gridVerticalSpacing =
            if (compactLayout) 10.dp else 18.dp

        br.com.flaviotorres.iptvpremium.ui.components.CatalogThemeBackdrop(Modifier.matchParentSize())

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = outerHorizontalPadding,
                    end = outerHorizontalPadding,
                    top = outerVerticalPadding,
                    bottom = if (compactLayout) 6.dp else 8.dp
                )
        ) {
            // Painel das categorias no mesmo estilo de vidro fumê do player.
            // Mantém a largura externa para não reduzir o espaço da grade.
            val categoryPanelShape = RoundedCornerShape(if (compactLayout) 14.dp else 18.dp)
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .clip(categoryPanelShape)
                    .background(
                        brush = br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.categoryPanelBrush,
                        shape = categoryPanelShape
                    )
                    .border(
                        border = BorderStroke(1.dp, if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) androidx.compose.ui.graphics.SolidColor(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.edge) else PremiumGoldBorder),
                        shape = categoryPanelShape
                    )
                    .kidsPanel()
                    .padding(horizontal = if (compactLayout) 8.dp else 12.dp,
                        vertical = if (compactLayout) 6.dp else 10.dp)
            ) {
                if (KidsAppearance.active) { KidsCatalogHeader(compactLayout) } else {
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLogo(compactLayout = compactLayout)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CATEGORIAS",
                            color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
                            fontSize = if (compactLayout) 10.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(0.dp))
                    }
                    Spacer(Modifier.width(0.dp))
                }
                }

                Spacer(modifier = Modifier.height(if (compactLayout) 5.dp else 8.dp))
                PremiumDivider(compactLayout = compactLayout)
                Spacer(modifier = Modifier.height(if (compactLayout) 5.dp else 8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                        .focusRequester(menuFocusRequester).focusGroup()
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.DirectionRight) {
                                if (event.type == KeyEventType.KeyDown) { categoryMenuActive = false; gridNavigator.enter() }
                                true
                            } else false
                        },
                    state = movieCategoryListState,
                    verticalArrangement = Arrangement.spacedBy(categoryGap)
                ) {
                    if (useRoomPaging) {
                        /*
                         * Atalhos locais aparecem antes das categorias do provedor.
                         *
                         * A filtragem real será conectada ao ViewModel/Room nas
                         * próximas etapas. Aqui restauramos a estrutura visual e
                         * o contrato de seleção sem inventar dados.
                         */
                        item(
                            key = "paging_all"
                        ) {
                            MovieCategoryItem(
                                returnFocus = selectedCategoryFocus,
                                category = MovieCategory.ALL,
                                categoryFocusRequester = categoryFirstFocusRequester,
                                count = visibleTotalMovieCount,
                                isSelected =
                                    selectedCategory ==
                                            MovieCategory.ALL &&
                                            selectedSourceCategoryId == null,
                                height = categoryHeight,
                                fontSize = categoryFontSize,
                                compactLayout = compactLayout,
                                onSelect = {
                                    categoryMenuActive = false; pendingFirstMovieFocus = true; firstMovieFocusRequest++
                                    if (selectedCategory != MovieCategory.ALL ||
                                        selectedSourceCategoryId != null
                                    ) startCategoryChange()
                                    selectedCategory =
                                        MovieCategory.ALL

                                    selectedSourceCategoryId =
                                        null

                                    onSourceCategorySelected(
                                        null
                                    )
                                }
                            )
                        }

                        item(
                            key = "paging_continue_watching"
                        ) {
                            MovieCategoryItem(
                                returnFocus = selectedCategoryFocus,
                                category =
                                    MovieCategory.CONTINUE_WATCHING,
                                count = continueMovies.size,
                                isSelected =
                                    selectedCategory ==
                                            MovieCategory.CONTINUE_WATCHING &&
                                            selectedSourceCategoryId == null,
                                height = categoryHeight,
                                fontSize = categoryFontSize,
                                compactLayout = compactLayout,
                                onSelect = {
                                    categoryMenuActive = false; pendingFirstMovieFocus = true; firstMovieFocusRequest++
                                    selectedCategory =
                                        MovieCategory.CONTINUE_WATCHING

                                    selectedSourceCategoryId =
                                        null

                                    onContinueWatchingSelected()
                                }
                            )
                        }

                        item(
                            key = "paging_favorites"
                        ) {
                            MovieCategoryItem(
                                returnFocus = selectedCategoryFocus,
                                category =
                                    MovieCategory.FAVORITES,
                                count =
                                    favoriteMovieIds.size,
                                isSelected =
                                    selectedCategory ==
                                            MovieCategory.FAVORITES &&
                                            selectedSourceCategoryId == null,
                                height = categoryHeight,
                                fontSize = categoryFontSize,
                                compactLayout = compactLayout,
                                onSelect = {
                                    categoryMenuActive = false; pendingFirstMovieFocus = true; firstMovieFocusRequest++
                                    if (selectedCategory != MovieCategory.FAVORITES ||
                                        selectedSourceCategoryId != null
                                    ) startCategoryChange()
                                    selectedCategory =
                                        MovieCategory.FAVORITES

                                    selectedSourceCategoryId =
                                        null

                                    onFavoritesSelected()
                                }
                            )
                        }

                        items(
                            items = visiblePagingCategories,
                            key = { category ->
                                "source_${category.id}"
                            }
                        ) { category ->
                            SourceMovieCategoryItem(
                                returnFocus = selectedCategoryFocus,
                                title = displayMovieCategoryName(
                                    category.name
                                ),
                                count =
                                    pagingCategoryCounts[
                                        category.id
                                    ] ?: 0,
                                isSelected =
                                    selectedSourceCategoryId ==
                                            category.id,
                                height = categoryHeight,
                                fontSize = categoryFontSize,
                                compactLayout = compactLayout,
                                onSelect = {
                                    categoryMenuActive = false; pendingFirstMovieFocus = true; firstMovieFocusRequest++
                                    onCategoryAccessRequested(category.id, category.name) {
                                    if (selectedSourceCategoryId != category.id) {
                                        startCategoryChange()
                                    }
                                    selectedCategory =
                                        MovieCategory.ALL

                                    selectedSourceCategoryId =
                                        category.id

                                    onSourceCategorySelected(
                                        category.id
                                    )
                                
                                    }
                                }
                            )
                        }
                    } else {
                        items(
                            items = MovieCategory.entries.filterNot { it == MovieCategory.SUBTITLED },
                            key = { it.name }
                        ) { category ->
                            MovieCategoryItem(
                                returnFocus = selectedCategoryFocus,
                                category = category,
                                categoryFocusRequester = if (category == MovieCategory.ALL) categoryFirstFocusRequester else null,
                                count = categoryCount(
                                    category = category,
                                    allMovies = previewMovies,
                                    favoriteMovieIds = favoriteMovieIds
                                ),
                                isSelected =
                                    selectedCategory == category,
                                height = categoryHeight,
                                fontSize = categoryFontSize,
                                compactLayout = compactLayout,
                                onSelect = {
                                    categoryMenuActive = false; pendingFirstMovieFocus = true; firstMovieFocusRequest++
                                    selectedCategory = category
                                }
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(sidebarGap)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { catalogContentFocused = it.hasFocus }.focusGroup()
                    .fillMaxSize()
            ) {
                TopBar(
                    searchFocusRequester = searchFocusRequester,
                    onGridEnter = { gridNavigator.enter() },
                    searchText = searchText,
                    searchActive = searchKeyboardVisible,
                    searchCursorPosition = searchCursorPosition,
                    onSearchClick = {
                        // Ao reabrir, começa no fim; as setas do teclado
                        // permitem escolher qualquer letra para substituir.
                        searchCursorPosition = searchText.length
                        searchKeyboardVisible = true
                    },
                    onSearchClear = {
                        searchText = ""
                        searchCursorPosition = 0
                        onSearchQueryChanged("")
                    },
                    sortMode = sortMode,
                    onSortClick = {
                        val nextSortMode =
                            when (sortMode) {
                                SortMode.DEFAULT -> SortMode.A_Z
                                SortMode.A_Z -> SortMode.Z_A
                                SortMode.Z_A -> SortMode.DEFAULT
                            }
                        sortMode = nextSortMode
                        onSortModeChanged(nextSortMode.name)
                    },
                    compactLayout = compactLayout
                )

                Spacer(
                    modifier = Modifier.height(
                        if (compactLayout) 10.dp else 16.dp
                    )
                )

                KidsCatalogTitle("FILMES KIDS", totalMovieCount)
                br.com.flaviotorres.iptvpremium.ui.components.CatalogTwoRowFrame(
                    columns = gridColumns,
                    compactLayout = compactLayout,
                ) { posterAspectRatio ->

                    if (
                        useRoomPaging &&
                        pagedMovies != null
                    ) {
                        if (selectedCategory == MovieCategory.CONTINUE_WATCHING &&
                            selectedSourceCategoryId == null) {
                            if (continueMovies.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Nenhum filme em andamento.", color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD))
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(gridColumns),
                                    state = moviesGridState,
                                    modifier = Modifier.fillMaxSize().catalogGridHost(gridNavigator),
                                    contentPadding = PaddingValues(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(gridHorizontalSpacing),
                                    verticalArrangement = Arrangement.spacedBy(LocalCatalogRowSpacing.current)
                                ) {
                                    items(continueMovies.size) { index ->
                                        val entry = continueMovies[index]
                                        MovieCard(
                                            navigator = gridNavigator, gridIndex = index, allowRestoration = !searchKeyboardVisible && !categoryMenuActive && !returningToCategory,
                                            name = entry.title,
                                            posterUrl = entry.posterUrl,
                                            resolvePosterCategory = onResolvePosterCategory,
                                            isSelected = selectedMovieId == entry.contentId,
                                            isFavorite = entry.contentId in favoriteMovieIds,
                                            posterAspectRatio = posterAspectRatio,
                                            compactLayout = compactLayout,
                                            movieId = entry.contentId,
                                            focusRestorationMovieId = if (pendingFirstMovieFocus && index == 0) entry.contentId else returningMovieFocusId,
                                            focusRestorationRequest = if (pendingFirstMovieFocus && index == 0) firstMovieFocusRequest else returningMovieFocusRequest,
                                            onFocused = { categoryMenuActive = false; if (index == 0) pendingFirstMovieFocus = false },
                                            watchProgress = entry.fraction,
                                            onSelect = {
                                                selectedMovieId = entry.contentId
                                                openCatalogMovie(entry.contentId)
                                            },
                                            onFavoriteClick = { _ -> onFavoriteClick(entry.contentId) },
                                            onRemoveFromContinue = { onRemoveFromContinue(entry.contentId) }
                                        )
                                    }
                                }
                            }
                        } else if (awaitingCategoryPage) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Carregando filmes...",
                                    color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                                    fontSize = 15.sp
                                )
                            }
                        } else when {
                            pagedMovies.loadState.refresh is
                                    LoadState.Loading &&
                                    pagedMovies.itemCount == 0 -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Carregando filmes...",
                                        color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            pagedMovies.loadState.refresh is
                                    LoadState.Error &&
                                    pagedMovies.itemCount == 0 -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Não foi possível carregar os filmes.",
                                        color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            pagedMovies.itemCount == 0 -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text =
                                            if (searchText.isNotBlank()) {
                                                "Nenhum filme encontrado."
                                            } else {
                                                "Nenhum filme nesta categoria."
                                            },
                                        color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            else -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(gridColumns),
                                    state = moviesGridState,
                                    modifier = Modifier.fillMaxSize().catalogGridHost(gridNavigator),
                                    // Reserva espaço para o zoom de 3,5% no primeiro card.
                                    // Sem isso a LazyGrid recorta o topo/lateral no foco.
                                    contentPadding = PaddingValues(8.dp),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            gridHorizontalSpacing
                                        ),
                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            LocalCatalogRowSpacing.current
                                        )
                                ) {
                                    items(
                                        count = pagedMovies.itemCount,
                                        key = { index ->
                                            pagedMovies.peek(index)
                                                ?.id
                                                ?: "paging_movie_$index"
                                        }
                                    ) { index ->
                                        val movie =
                                            pagedMovies[index]

                                        if (movie != null) {
                                            MovieCard(
                                                navigator = gridNavigator, gridIndex = index, allowRestoration = !searchKeyboardVisible && !categoryMenuActive && !returningToCategory,
                                                name =
                                                    movie.name,
                                                posterUrl =
                                                    movie.posterUrl,
                                                posterCategory = posterCategoryNames[movie.categoryId].orEmpty(),
                                                resolvePosterCategory = onResolvePosterCategory,
                                                year =
                                                    movie.year,
                                                rating =
                                                    movie.rating,
                                                durationSeconds =
                                                    movie.durationSeconds,
                                                isSelected =
                                                    selectedMovieId ==
                                                            movie.id,
                                                isFavorite =
                                                    movie.id in
                                                            favoriteMovieIds,
                                                posterAspectRatio =
                                                    posterAspectRatio,
                                                compactLayout =
                                                    compactLayout,
                                                watchProgress = progressByMovie[movie.id],
                                                onSelect = {
                                                    selectedMovieId =
                                                        movie.id

                                                    openCatalogMovie(
                                                        movie.id
                                                    )
                                                },
                                                movieId = movie.id,
                                                focusRestorationMovieId = when {
                                                    pendingFirstMovieFocus && index == 0 -> movie.id
                                                    returningMovieFocusId != null -> returningMovieFocusId
                                                    else -> favoriteFocusMovieId
                                                },
                                                focusRestorationRequest = when {
                                                    pendingFirstMovieFocus && index == 0 -> firstMovieFocusRequest
                                                    returningMovieFocusId != null -> returningMovieFocusRequest
                                                    else -> favoriteFocusRequest
                                                },
                                                onFocused = { categoryMenuActive = false;
                                                    posterFocusActivity.value = posterFocusActivity.value + 1L
                                                    if (index == 0 && pendingFirstMovieFocus) {
                                                        pendingFirstMovieFocus = false
                                                    }
                                                    if (returningMovieFocusId == movie.id) {
                                                        returningMovieFocusId = null
                                                    }
                                                },
                                                onFavoriteClick = { fromFocusedCard ->
                                                    if (fromFocusedCard) {
                                                        favoriteFocusMovieId =
                                                            if (selectedCategory == MovieCategory.FAVORITES &&
                                                                movie.id in favoriteMovieIds
                                                            ) {
                                                                // No filtro Favoritos o item removido desaparece:
                                                                // direciona o foco ao próximo, ou ao anterior.
                                                                nextFavoriteFocusId(
                                                                    pagedMovies.itemSnapshotList.items.map { it.id },
                                                                    movie.id
                                                                )
                                                            } else {
                                                                movie.id
                                                            }
                                                        favoriteFocusRequest++
                                                    }
                                                    onFavoriteClick(movie.id)
                                                }
                                            )
                                        } else { CatalogPosterPlaceholder(posterAspectRatio) }
                                    }
                                }
                            }
                        }
                    } else if (displayedMovies.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text =
                                    if (searchText.isNotBlank()) {
                                        "Nenhum filme encontrado."
                                    } else {
                                        emptyCategoryMessage(selectedCategory)
                                    },
                                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            state = moviesGridState,
                            modifier = Modifier.fillMaxSize().catalogGridHost(gridNavigator),
                            // Mesma proteção na grade de prévia (sem Room/Paging).
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement =
                                Arrangement.spacedBy(gridHorizontalSpacing),
                            verticalArrangement =
                                Arrangement.spacedBy(LocalCatalogRowSpacing.current)
                        ) {
                            items(
                                items = displayedMovies,
                                key = { movie -> movie.id }
                            ) { movie ->
                                MovieCard(
                                    name =
                                        movie.name,
                                    posterCategory = movie.category.title,
                                    isSelected =
                                        selectedMovieId == movie.id,
                                    isFavorite =
                                        movie.id in favoriteMovieIds,
                                    posterAspectRatio =
                                        posterAspectRatio,
                                    compactLayout =
                                        compactLayout,
                                    onSelect = {
                                        selectedMovieId = movie.id
                                        openCatalogMovie(movie.id)
                                    },
                                    movieId = movie.id,
                                    focusRestorationMovieId = when {
                                        pendingFirstMovieFocus && displayedMovies.firstOrNull()?.id == movie.id -> movie.id
                                        returningMovieFocusId != null -> returningMovieFocusId
                                        else -> favoriteFocusMovieId
                                    },
                                    focusRestorationRequest = when {
                                        pendingFirstMovieFocus && displayedMovies.firstOrNull()?.id == movie.id -> firstMovieFocusRequest
                                        returningMovieFocusId != null -> returningMovieFocusRequest
                                        else -> favoriteFocusRequest
                                    },
                                    onFocused = { categoryMenuActive = false;
                                        if (pendingFirstMovieFocus && displayedMovies.firstOrNull()?.id == movie.id) {
                                            pendingFirstMovieFocus = false
                                        }
                                        if (returningMovieFocusId == movie.id) {
                                            returningMovieFocusId = null
                                        }
                                    },
                                    onFavoriteClick = { fromFocusedCard ->
                                        if (fromFocusedCard) {
                                            favoriteFocusMovieId =
                                                if (selectedCategory == MovieCategory.FAVORITES &&
                                                    movie.id in favoriteMovieIds
                                                ) {
                                                    nextFavoriteFocusId(
                                                        displayedMovies.map { it.id },
                                                        movie.id
                                                    )
                                                } else {
                                                    movie.id
                                                }
                                            favoriteFocusRequest++
                                        }
                                        onFavoriteClick(movie.id)
                                    }
                                )
                            }
                        }
                    }
                } // MoviesCatalogFrame
            }
        }

        if (searchKeyboardVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = outerHorizontalPadding,
                        end = outerHorizontalPadding,
                        bottom = 8.dp
                    ),
                contentAlignment =
                    Alignment.BottomCenter
            ) {
                AppKeyboard(
                    modifier = Modifier.fillMaxWidth(),
                    actionText = "CONCLUIR",
                    onCharacter = { character ->
                        // Substitui a letra destacada; ao fim, insere.
                        // A busca continua sendo atualizada no Room.
                        val edited = tvKeyboardType(
                            searchText,
                            searchCursorPosition,
                            character
                        )
                        searchText = edited.value
                        searchCursorPosition = edited.cursor
                        onSearchQueryChanged(edited.value)
                    },
                    onBackspace = {
                        val edited = tvKeyboardDelete(
                            searchText,
                            searchCursorPosition
                        )
                        searchText = edited.value
                        searchCursorPosition = edited.cursor
                        onSearchQueryChanged(edited.value)
                    },
                    onMoveCursorLeft = {
                        searchCursorPosition = tvKeyboardMoveCursor(
                            searchText,
                            searchCursorPosition,
                            -1
                        )
                    },
                    onMoveCursorRight = {
                        searchCursorPosition = tvKeyboardMoveCursor(
                            searchText,
                            searchCursorPosition,
                            1
                        )
                    },
                    onAction = {
                        // Fecha apenas o teclado; mantém a busca ativa.
                        searchKeyboardVisible = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AppLogo(
    compactLayout: Boolean
) {
    if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) {
        br.com.flaviotorres.iptvpremium.ui.components.CatalogWordmark(compactLayout)
        return
    }
    Image(
        painter = painterResource(
            id = R.drawable.iptv_premium_logo
        ),
        contentDescription = "IPTV Premium",
        modifier = Modifier
            .width(
                if (compactLayout) {
                    82.dp
                } else {
                    98.dp
                }
            )
            .height(
                if (compactLayout) {
                    52.dp
                } else {
                    62.dp
                }
            ),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun TopBar(
    searchText: String,
    searchActive: Boolean,
    searchCursorPosition: Int,
    onSearchClick: () -> Unit,
    onSearchClear: () -> Unit,
    sortMode: SortMode,
    onSortClick: () -> Unit,
    compactLayout: Boolean,
    searchFocusRequester: FocusRequester? = null,
    onGridEnter: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(if (compactLayout) 14.dp else 18.dp)
    Row(
        modifier = Modifier
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionDown && onGridEnter != null) {
                    if (event.type == KeyEventType.KeyDown) onGridEnter()
                    true
                } else false
            }
            .fillMaxWidth()
            .height(if (compactLayout) 44.dp else 54.dp)
            .background(PremiumPanelBackground, shape)
            .border(BorderStroke(1.dp, if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) androidx.compose.ui.graphics.SolidColor(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.edge) else PremiumGoldBorder), shape)
            .padding(horizontal = if (compactLayout) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 9.dp)
    ) {
        MovieTopAction(
            label = if (sortMode == SortMode.DEFAULT) "Organizar" else if (sortMode == SortMode.A_Z) "A – Z" else "Z – A",
            iconKind = 0,
            compactLayout = compactLayout,
            onClick = onSortClick
        )
        SearchBox(
            value = searchText,
            active = searchActive,
            cursorPosition = searchCursorPosition,
            onClick = onSearchClick,
            onClear = onSearchClear,
            modifier = Modifier.weight(1f).then(
                if (searchFocusRequester != null) Modifier.focusRequester(searchFocusRequester) else Modifier
            ),
            compactLayout = compactLayout
        )
        ClockDisplay(compactLayout = compactLayout)
    }
}

@Composable
private fun MovieTopAction(
    label: String,
    iconKind: Int,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (compactLayout) 9.dp else 11.dp)
    Row(
        modifier = Modifier
            .height(if (compactLayout) 34.dp else 42.dp)
            .background(
                brush = if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBrush()
                else Brush.verticalGradient(listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10))),
                shape = shape
            )
            .premiumGlow(focused)
            .border(
                border = BorderStroke(if (focused) 1.5.dp else 1.dp, br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(focused, PremiumGoldBorder)),
                shape = shape
            )
            .kidsFocus(focused)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = if (compactLayout) 7.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 4.dp else 6.dp)
    ) {
        when (iconKind) {
            0 -> Canvas(Modifier.size(if (compactLayout) 12.dp else 15.dp)) {
                val unit = size.width / 20f
                val stroke = 1.8.dp.toPx()
                drawLine(PremiumGoldBorder, Offset(2*unit, 5*unit), Offset(18*unit, 5*unit), stroke)
                drawLine(PremiumGoldBorder, Offset(2*unit, 10*unit), Offset(14*unit, 10*unit), stroke)
                drawLine(PremiumGoldBorder, Offset(2*unit, 15*unit), Offset(10*unit, 15*unit), stroke)
            }
            1 -> Text("≡", fontSize = if (compactLayout) 13.sp else 15.sp, color = (if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else AppGold))
            else -> Text("♡", fontSize = if (compactLayout) 13.sp else 15.sp, color = (if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else AppGold))
        }
        Text(
            text = label,
            color = if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
            fontSize = if (compactLayout) 10.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SearchBox(
    value: String,
    active: Boolean,
    cursorPosition: Int,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    compactLayout: Boolean
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val highlighted =
        active || isFocused

    val shape =
        RoundedCornerShape(if (compactLayout) 10.dp else 12.dp)

    Row(
        modifier = modifier
            .height(
                if (compactLayout) {
                    34.dp
                } else {
                    42.dp
                }
            )
            .background(
                brush = Brush.verticalGradient(
                    if (highlighted) { br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColors() } else {
                        listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10))
                    }
                ),
                shape = shape
            )
            .border(
                border = BorderStroke(
                    width = if (highlighted) 1.7.dp else 1.dp,
                    brush = if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder) else PremiumQuietEdge
                ),
                shape = shape
            )
            .kidsFocus(isFocused,active)
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .onKeyEvent { event ->
                val selectKey =
                    event.key == Key.DirectionCenter ||
                            event.key == Key.Enter ||
                            event.key == Key.NumPadEnter

                if (
                    selectKey &&
                    event.type == KeyEventType.KeyUp
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(
                onClick = onClick
            )
            .focusable()
            .padding(
                horizontal =
                    if (compactLayout) {
                        12.dp
                    } else {
                        16.dp
                    }
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        // Lupa vetorial: mesmo traço fino dos ícones de controle do player.
        Canvas(Modifier.size(if (compactLayout) 16.dp else 19.dp)) {
            val tint = PremiumGoldBorder
            val stroke = 1.8.dp.toPx()
            val center = Offset(size.width * 0.43f, size.height * 0.41f)
            drawCircle(
                brush = tint,
                radius = size.minDimension * 0.29f,
                center = center,
                style = Stroke(width = stroke)
            )
            drawLine(
                brush = tint,
                start = Offset(size.width * 0.64f, size.height * 0.64f),
                end = Offset(size.width * 0.91f, size.height * 0.91f),
                strokeWidth = stroke
            )
        }

        Spacer(
            modifier = Modifier.width(9.dp)
        )

        TvKeyboardSearchText(
            textColor = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
            placeholderColor = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFF909096),
            value = value,
            active = active,
            cursorPosition = cursorPosition,
            placeholder = if (KidsAppearance.active) "Qual aventura vamos assistir?" else "Pesquisar filmes...",
            modifier = Modifier.weight(1f),
            fontSize = if (compactLayout) 12.sp else 14.sp
        )

        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clickable(
                        onClick = onClear
                    )
                    .padding(3.dp),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = (if (active || isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else AppGoldLight),
                    fontSize =
                        if (compactLayout) {
                            17.sp
                        } else {
                            20.sp
                        },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ClockDisplay(
    compactLayout: Boolean
) {
    var currentTime by remember {
        mutableStateOf(
            LocalDateTime.now()
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime =
                LocalDateTime.now()

            delay(30_000L)
        }
    }

    val locale = remember {
        Locale("pt", "BR")
    }

    val dayFormatter = remember {
        DateTimeFormatter.ofPattern(
            "EEE\nd MMM",
            locale
        )
    }

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern(
            "HH:mm",
            locale
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentTime
                .format(dayFormatter)
                .replace(".", "")
                .replaceFirstChar {
                    it.uppercase()
                },
            color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
            fontSize =
                if (compactLayout) {
                    9.sp
                } else {
                    12.sp
                },
            lineHeight =
                if (compactLayout) {
                    11.sp
                } else {
                    15.sp
                }
        )

        Spacer(
            modifier = Modifier.width(
                if (compactLayout) 8.dp else 12.dp
            )
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(
                    if (compactLayout) {
                        29.dp
                    } else {
                        38.dp
                    }
                )
                .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFAB842F))
        )

        Spacer(
            modifier = Modifier.width(
                if (compactLayout) 8.dp else 12.dp
            )
        )

        Text(
            text = currentTime.format(
                timeFormatter
            ),
            style = TextStyle(
                fontFamily = CinzelSemiBold,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize =
                    if (compactLayout) {
                        21.sp
                    } else {
                        30.sp
                    },
                brush = MetallicGoldBrushText
            )
        )
    }
}


@Composable
private fun MovieCategoryItem(
    category: MovieCategory,
    categoryFocusRequester: FocusRequester? = null,
    count: Int,
    isSelected: Boolean,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    compactLayout: Boolean,
    onSelect: () -> Unit,
    returnFocus: FocusRequester? = null
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val shape = RoundedCornerShape(
        if (compactLayout) {
            10.dp
        } else {
            12.dp
        }
    )

    Row(
        modifier = Modifier
            .then(if (isSelected && returnFocus != null) Modifier.focusRequester(returnFocus) else Modifier)
            .testTag("movie-category-" + category.name)
            .fillMaxWidth()
            .height(height)
            // Clip focus/click indication to the same rounded outline as the fill.
            .then(if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) Modifier.clip(shape) else Modifier)
            .background(
                brush = Brush.horizontalGradient(
                    if (isFocused || isSelected) { br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categorySelectionColors() } else {
                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categoryColors()
                    }
                ),
                shape = shape
            )
            .premiumGlow(isFocused || isSelected)
            .premiumCategoryCardBorder(
                highlighted = isFocused || isSelected,
                roundedDp = if (compactLayout) 10f else 12f
            )
            .kidsFocus(isFocused,isSelected)
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .then(
                if (categoryFocusRequester != null) Modifier.focusRequester(categoryFocusRequester)
                else Modifier
            )
            .onKeyEvent { event ->
                val selectKey =
                    event.key == Key.DirectionCenter ||
                            event.key == Key.Enter ||
                            event.key == Key.NumPadEnter

                if (
                    selectKey &&
                    event.type == KeyEventType.KeyUp
                ) {
                    onSelect()
                    true
                } else {
                    false
                }
            }
            .clickable(
                onClick = onSelect
            )
            .focusable()
            .padding(
                horizontal =
                    if (compactLayout) {
                        10.dp
                    } else {
                        14.dp
                    }
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        BrandMark(
            category = category,
            compactLayout = compactLayout
        )

        Spacer(
            modifier = Modifier.width(
                if (compactLayout) {
                    9.dp
                } else {
                    12.dp
                }
            )
        )

        Text(
            text = category.title,
            modifier = Modifier.weight(1f),
            color = when {
                isFocused ->
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)

                isSelected ->
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)

                else ->
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5)
            },
            fontSize = fontSize,
            fontWeight =
                if (
                    isFocused ||
                    isSelected
                ) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = count.toString(),
            color =
                if (
                    isFocused ||
                    isSelected
                ) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)
                } else {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD)
                },
            fontSize =
                if (compactLayout) {
                    10.sp
                } else {
                    12.sp
                }
        )
    }
}

@Composable
private fun SourceMovieCategoryItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    compactLayout: Boolean,
    onSelect: () -> Unit,
    returnFocus: FocusRequester? = null
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val shape =
        RoundedCornerShape(
            if (compactLayout) {
                10.dp
            } else {
                12.dp
            }
        )

    Row(
        modifier = Modifier
            .then(if (isSelected && returnFocus != null) Modifier.focusRequester(returnFocus) else Modifier)
            .testTag("movie-category-" + title)
            .fillMaxWidth()
            .height(height)
            // Clip focus/click indication to the same rounded outline as the fill.
            .then(if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) Modifier.clip(shape) else Modifier)
            .background(
                brush = Brush.horizontalGradient(
                    if (isFocused || isSelected) { br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categorySelectionColors() } else {
                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categoryColors()
                    }
                ),
                shape = shape
            )
            .premiumGlow(isFocused || isSelected)
            .premiumCategoryCardBorder(
                highlighted = isFocused || isSelected,
                roundedDp = if (compactLayout) 10f else 12f
            )
            .kidsFocus(isFocused,isSelected)
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .onKeyEvent { event ->
                val selectKey =
                    event.key == Key.DirectionCenter ||
                            event.key == Key.Enter ||
                            event.key == Key.NumPadEnter

                if (
                    selectKey &&
                    event.type == KeyEventType.KeyUp
                ) {
                    onSelect()
                    true
                } else {
                    false
                }
            }
            .clickable(
                onClick = onSelect
            )
            .focusable()
            .padding(
                horizontal =
                    if (compactLayout) {
                        10.dp
                    } else {
                        14.dp
                    }
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        if (KidsAppearance.active) KidsCategoryIcon(title) else {
        MovieCategoryIcon(
            type = MovieNavIcon.GENERIC,
            compactLayout = compactLayout
        )
        }

        Spacer(
            modifier = Modifier.width(
                if (compactLayout) {
                    9.dp
                } else {
                    12.dp
                }
            )
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color =
                when {
                    isFocused ->
                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)

                    isSelected ->
                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)

                    else ->
                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5)
                },
            fontSize = fontSize,
            fontWeight =
                if (
                    isFocused ||
                    isSelected
                ) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = count.toString(),
            color =
                if (
                    isFocused ||
                    isSelected
                ) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)
                } else {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD)
                },
            fontSize =
                if (compactLayout) {
                    10.sp
                } else {
                    12.sp
                }
        )
    }
}

/** Quatro símbolos fixos para manter o menu coerente em todos os provedores. */
private enum class MovieNavIcon { ALL, CONTINUE, FAVORITES, GENERIC }

@Composable
private fun BrandMark(
    category: MovieCategory,
    compactLayout: Boolean
) {
    if (KidsAppearance.active && category !in listOf(MovieCategory.FAVORITES, MovieCategory.CONTINUE_WATCHING)) {
        KidsCategoryIcon(category.title); return
    }
    val type = when (category) {
        MovieCategory.ALL -> MovieNavIcon.ALL
        MovieCategory.CONTINUE_WATCHING -> MovieNavIcon.CONTINUE
        MovieCategory.FAVORITES -> MovieNavIcon.FAVORITES
        else -> MovieNavIcon.GENERIC
    }
    MovieCategoryIcon(type = type, compactLayout = compactLayout)
}

@Composable
private fun MovieCategoryIcon(
    type: MovieNavIcon,
    compactLayout: Boolean
) {
    val side = if (compactLayout) 18.dp else 21.dp
    Box(
        modifier = Modifier.width(if (compactLayout) 27.dp else 33.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(Modifier.size(side)) {
            val u = size.minDimension / 24f
            val line = 1.8.dp.toPx()
            val gold = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categoryIconBrush(PremiumGoldBorder)
            fun point(x: Float, y: Float) = Offset(x * u, y * u)
            when (type) {
                MovieNavIcon.ALL -> {
                    for (x in listOf(3f, 13f)) {
                        for (y in listOf(3f, 13f)) {
                            drawRoundRect(
                                brush = gold,
                                topLeft = point(x, y),
                                size = Size(8f * u, 8f * u),
                                cornerRadius = CornerRadius(1.5f * u),
                                style = Stroke(width = line)
                            )
                        }
                    }
                }
                MovieNavIcon.CONTINUE -> {
                    drawCircle(gold, radius = 9.2f * u,
                        center = point(12f, 12f), style = Stroke(width = line))
                    val play = Path().apply {
                        moveTo(10f * u, 7.5f * u)
                        lineTo(17f * u, 12f * u)
                        lineTo(10f * u, 16.5f * u)
                        close()
                    }
                    drawPath(play, gold)
                }
                MovieNavIcon.FAVORITES -> {
                    val heart = Path().apply {
                        moveTo(12f * u, 20f * u)
                        cubicTo(9f * u, 17.3f * u, 3f * u, 12.6f * u,
                            3f * u, 8.5f * u)
                        cubicTo(3f * u, 3.5f * u, 9f * u, 2f * u,
                            12f * u, 6.3f * u)
                        cubicTo(15f * u, 2f * u, 21f * u, 3.5f * u,
                            21f * u, 8.5f * u)
                        cubicTo(21f * u, 12.6f * u, 15f * u, 17.3f * u,
                            12f * u, 20f * u)
                        close()
                    }
                    drawPath(heart, gold)
                }
                MovieNavIcon.GENERIC -> {
                    // Claquete âmbar vetorial para todas as categorias comuns.
                    // Preserva os três ícones exclusivos já aprovados.
                    val amberDark = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFF1A160C)
                    drawRoundRect(
                        brush = gold,
                        topLeft = point(2.8f, 10f),
                        size = Size(18.4f * u, 10.5f * u),
                        cornerRadius = CornerRadius(1.8f * u)
                    )
                    drawRoundRect(
                        color = amberDark,
                        topLeft = point(4.8f, 12.7f),
                        size = Size(14.4f * u, 5.8f * u),
                        cornerRadius = CornerRadius(0.6f * u)
                    )
                    val slate = Path().apply {
                        moveTo(2.7f * u, 7.6f * u)
                        lineTo(20.0f * u, 3.2f * u)
                        lineTo(21.2f * u, 7.7f * u)
                        lineTo(3.9f * u, 12f * u)
                        close()
                    }
                    drawPath(slate, gold)
                    // Divisórias escuras da haste superior da claquete.
                    drawLine(amberDark, point(7.7f, 6.5f), point(10.0f, 10.4f), 2.2f * u)
                    drawLine(amberDark, point(13.1f, 5.1f), point(15.4f, 9.0f), 2.2f * u)
                    drawLine(amberDark, point(18.3f, 3.9f), point(20.2f, 7.8f), 1.8f * u)
                }
            }
        }
    }
}

/**
 * Ao remover um item de uma lista filtrada por favoritos, esse card
 * deixa de existir. Escolhemos um vizinho estável, sem saltar ao topo.
 */
private fun nextFavoriteFocusId(
    visibleIds: List<String>,
    removedId: String
): String? {
    val index = visibleIds.indexOf(removedId)
    if (index < 0) return null
    return visibleIds.getOrNull(index + 1)
        ?: visibleIds.getOrNull(index - 1)
}

@Composable
private fun MovieCard(
    name: String,
    posterUrl: String? = null,
    posterCategory:String = "",
    resolvePosterCategory:(suspend (String)->String?)? = null,
    year: Int? = null,
    rating: Double? = null,
    durationSeconds: Long? = null,
    isSelected: Boolean,
    isFavorite: Boolean,
    posterAspectRatio: Float,
    compactLayout: Boolean,
    movieId: String,
    watchProgress: Float? = null,
    focusRestorationMovieId: String?,
    focusRestorationRequest: Long,
    onSelect: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onFocused: () -> Unit = {},
    onRemoveFromContinue: (() -> Unit)? = null,
    navigator: CatalogGridNavigator? = null,
    gridIndex: Int = -1,
    allowRestoration: Boolean = true
) {
    val fittedPosterWidth = br.com.flaviotorres.iptvpremium.ui.components.LocalCatalogPosterWidth.current
    var isFocused by remember {
        mutableStateOf(false)
    }
    val visuallyFocused = isFocused || navigator?.isMovingSelection(gridIndex) == true
    val focusScale by animateFloatAsState(if (visuallyFocused) { if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) 1.03f else 1.035f } else 1f,
        animationSpec = tween(140), label = "PosterFocusScale")

    val focusRequester = remember { FocusRequester() }
    BindCatalogFocus(navigator, gridIndex, focusRequester)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnFavoriteClick by rememberUpdatedState(onFavoriteClick)
    val currentOnFocused by rememberUpdatedState(onFocused)
    val currentOnRemoveFromContinue by rememberUpdatedState(onRemoveFromContinue)
    val holdThreshold = movieHoldDurationMs(onRemoveFromContinue != null, FAVORITE_LONG_PRESS_MS)
    fun performHeldAction(hadFocus: Boolean) {
        val remove = currentOnRemoveFromContinue
        if (remove != null) remove() else currentOnFavoriteClick(hadFocus)
    }

    var selectKeyDownTime by remember(movieId) {
        mutableLongStateOf(0L)
    }
    var selectKeyHeld by remember(movieId) {
        mutableStateOf(false)
    }
    var keyboardFavoriteTriggered by remember(movieId) {
        mutableStateOf(false)
    }
    var pointerFavoriteTriggered by remember(movieId) {
        mutableStateOf(false)
    }

    val ownerView = LocalView.current
    DisposableEffect(ownerView, movieId) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) {
                selectKeyHeld = false
                selectKeyDownTime = 0L
            }
        }
        val observer = ownerView.viewTreeObserver
        observer.addOnWindowFocusChangeListener(listener)
        onDispose {
            if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener)
        }
    }

    // Dispara sem depender do repeatCount do dispositivo: alguns controles
    // não repetem o OK enquanto está pressionado. O timer só é ativo
    // durante o pressionamento, e executa exatamente uma vez.
    LaunchedEffect(selectKeyHeld, movieId, holdThreshold) {
        if (selectKeyHeld) {
            delay(holdThreshold)
            if (selectKeyHeld && !keyboardFavoriteTriggered && movieHoldReady(SystemClock.elapsedRealtime() - selectKeyDownTime, holdThreshold, isFocused, false)) {
                keyboardFavoriteTriggered = true
                performHeldAction(true)
            }
        }
    }

    // Alguns Pagers reconstroem o item quando muda o conjunto de favoritos.
    // Recupera o foco por uma janela pequena, sem reposicionar a grade.
    LaunchedEffect(focusRestorationRequest, focusRestorationMovieId, allowRestoration) {
        if (allowRestoration && focusRestorationRequest > 0L && focusRestorationMovieId == movieId) {
            repeat(3) {
                androidx.compose.runtime.withFrameNanos { }
                if (!isFocused) runCatching { focusRequester.requestFocus() }
            }
        }
    }

    val shape = RoundedCornerShape(br.com.flaviotorres.iptvpremium.ui.components.CATALOG_POSTER_CORNER_DP.dp)

    val posterContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val artworkEpoch by br.com.flaviotorres.iptvpremium.ui.theme.ThemeArtworkEpoch.collectAsState()
    // Chave por filme/URL impede reaproveitamento visual da capa anterior
    // quando o LazyGrid recicla o card. Hit de RAM aparece no primeiro frame.
    val posterState by key(movieId,posterUrl,artworkEpoch) {
        val cached=MoviePosterLoader.peekBitmap(posterUrl)
        produceState(br.com.flaviotorres.iptvpremium.ui.components.PosterArtworkState(cached,
            cached!=null||MoviePosterLoader.knownMissing(posterUrl))) {
            value=br.com.flaviotorres.iptvpremium.ui.components.PosterArtworkState(
                MoviePosterLoader.loadWithRetry(posterContext,posterUrl),true)
        }
    }
    val posterBitmap=posterState.bitmap

    val metadataText =
        remember(
            fittedPosterWidth,
            year,
            rating,
            durationSeconds
        ) {
            movieMetadataText(
                year = year,
                rating = rating,
                durationSeconds =
                    durationSeconds
            )
        }

    Column(
        modifier = Modifier
            .then(if (fittedPosterWidth.value.isFinite()) Modifier.wrapContentWidth(Alignment.CenterHorizontally).width(fittedPosterWidth).height(LocalCatalogCardHeight.current) else Modifier)
            .graphicsLayer {
                val scale = if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) 1f else focusScale
                scaleX = scale; scaleY = scale
            }
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !isFocused) {
                    navigator?.focused(gridIndex)
                    currentOnFocused()
                }
                isFocused = focusState.isFocused
                if (!focusState.isFocused) {
                    selectKeyHeld = false
                    selectKeyDownTime = 0L
                }
            }
            .focusRequester(focusRequester)
            .catalogGridNavigation(navigator, gridIndex, focusRequester)
            .onPreviewKeyEvent { event ->
                val selectKey = event.key == Key.DirectionCenter ||
                        event.key == Key.Enter ||
                        event.key == Key.NumPadEnter

                if (!selectKey) {
                    false
                } else {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            // Repetições não reiniciam o relógio nem
                            // executam o favorito uma segunda vez.
                            if (!selectKeyHeld && event.nativeKeyEvent.repeatCount == 0) {
                                selectKeyDownTime = SystemClock.elapsedRealtime()
                                keyboardFavoriteTriggered = false
                                selectKeyHeld = true
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            if (selectKeyHeld && !event.nativeKeyEvent.isCanceled) {
                                val duration = SystemClock.elapsedRealtime() - selectKeyDownTime
                                if (keyboardFavoriteTriggered || movieHoldReady(duration, holdThreshold, isFocused, event.nativeKeyEvent.isCanceled)) {
                                    if (!keyboardFavoriteTriggered) {
                                        keyboardFavoriteTriggered = true
                                        performHeldAction(true)
                                    }
                                } else {
                                    currentOnSelect()
                                }
                            }
                            selectKeyHeld = false
                            selectKeyDownTime = 0L
                            true
                        }
                        else -> false
                    }
                }
            }
            // Toque e mouse também obedecem ao limite real de 1 segundo.
            // Diferentemente do combinedClickable, não acionamos favoritos
            // com o long press padrão do Android (cerca de 500 ms).
            .pointerInput(movieId, holdThreshold) {
                detectTapGestures(
                    onPress = {
                        pointerFavoriteTriggered = false
                        val releasedBeforeTimeout = withTimeoutOrNull(holdThreshold) {
                            tryAwaitRelease()
                        }
                        if (releasedBeforeTimeout == null) {
                            pointerFavoriteTriggered = true
                            performHeldAction(isFocused)
                            tryAwaitRelease()
                        }
                    },
                    onTap = {
                        if (!pointerFavoriteTriggered) {
                            currentOnSelect()
                        }
                        pointerFavoriteTriggered = false
                    }
                )
            }
            .semantics {
                role = Role.Button
                onClick(label = "Abrir detalhes do filme") {
                    currentOnSelect()
                    true
                }
            }
            .focusable(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    posterAspectRatio
                )
                .graphicsLayer {
                    val scale = if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) focusScale else 1f
                    scaleX = scale; scaleY = scale
                }
                .posterSelectionShadow(visuallyFocused, br.com.flaviotorres.iptvpremium.ui.components.CATALOG_POSTER_CORNER_DP)
                .background(
                    brush = Brush.verticalGradient(
                        colors =
                            if (
                                visuallyFocused
                            ) { br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColors() } else {
                                listOf(
                                    br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF242428),
                                    br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF151518)
                                )
                            }
                    ),
                    shape = shape
                )
                .premiumGlow(visuallyFocused, roundedDp = br.com.flaviotorres.iptvpremium.ui.components.CATALOG_POSTER_CORNER_DP)
                .border(
                    border = BorderStroke(
                        width =
                            if (
                                visuallyFocused
                            ) {
                                2.dp
                            } else {
                                1.dp
                            },
                        brush =
                            if (
                                visuallyFocused
                            ) {
                                br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder)
                            } else {
                                if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) androidx.compose.ui.graphics.SolidColor(Color.Transparent) else Brush.linearGradient(
                                    listOf(
                                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF776034),
                                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF3B342A)
                                    )
                                )
                            }
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (posterBitmap != null) {
                Image(
                    bitmap = posterBitmap!!,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                    contentScale =
                        if (fittedPosterWidth.value.isFinite()) ContentScale.Crop else ContentScale.Fit
                )
            } else if(posterState.finished) {
                br.com.flaviotorres.iptvpremium.ui.components.GenericPosterArtwork("movies",movieId,name,
                    posterCategory,resolvePosterCategory,Modifier.fillMaxSize().clip(shape))
            }

            watchProgress?.takeIf { it > 0f }?.let { fraction ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .align(Alignment.BottomCenter)
                        .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x88000000))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .height(4.dp).background(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFD54A))
                    )
                }
            }
            // A capa é desenhada depois da borda de fundo do Box. Ao receber
            // foco, sobrepomos o contorno por último para que os quatro lados
            // permaneçam visíveis, inclusive a linha superior do primeiro card.
            if (visuallyFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .posterSelectionFrame(true, br.com.flaviotorres.iptvpremium.ui.components.CATALOG_POSTER_CORNER_DP)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(
                if (compactLayout) {
                    4.dp
                } else {
                    7.dp
                }
            )
        )

        Text(
            text = name,
            lineHeight = if (compactLayout) 14.sp else 18.sp,
            modifier = if (isFocused) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 800, repeatDelayMillis = 1000) else Modifier,
            color =
                if (
                    visuallyFocused
                ) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)
                } else {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5)
                },
            fontSize =
                if (compactLayout) {
                    11.sp
                } else {
                    14.sp
                },
            fontWeight =
                if (
                    visuallyFocused
                ) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            maxLines = 1,
            overflow = if (isFocused) TextOverflow.Clip else TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(if (compactLayout) 2.dp else 3.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (metadataText.isNotBlank()) Text(
                text = metadataText,
                modifier = Modifier.weight(1f, fill = false),
                lineHeight = if (compactLayout) 12.sp else 16.sp,
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFF9D9DA3),
                fontSize = if (compactLayout) 9.sp else 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            br.com.flaviotorres.iptvpremium.ui.components.CatalogFavoriteHeart(isFavorite, compactLayout) {
                currentOnFavoriteClick(false)
            }
        }
    }
}

private fun displayMovieCategoryName(
    rawName: String
): String {
    val withoutPrefix =
        rawName
            .trim()
            .replace(
                Regex(
                    pattern = """^filmes\s*\|\s*""",
                    option = RegexOption.IGNORE_CASE
                ),
                ""
            )
            .trim()

    return when (
        withoutPrefix
            .lowercase(
                Locale("pt", "BR")
            )
    ) {
        "acao" -> "Ação"
        "animacao" -> "Animação"
        "comedia" -> "Comédia"
        "familia" -> "Família"
        "ficcao" -> "Ficção"
        "ficcao cientifica" ->
            "Ficção Científica"

        "documentarios" ->
            "Documentários"

        else ->
            withoutPrefix.ifBlank {
                rawName.trim()
            }
    }
}

private fun movieMetadataText(
    year: Int?,
    rating: Double?,
    durationSeconds: Long?
): String {
    val parts =
        mutableListOf<String>()

    year
        ?.takeIf {
            it in 1888..2100
        }
        ?.let {
            parts += it.toString()
        }

    rating
        ?.takeIf {
            it > 0.0
        }
        ?.let {
            parts +=
                String.format(
                    Locale.US,
                    "★ %.1f",
                    it
                )
        }

    formatMovieDuration(
        durationSeconds
    )
        ?.let {
            parts += it
        }

    return parts.joinToString(
        separator = "  •  "
    )
}

private fun formatMovieDuration(
    durationSeconds: Long?
): String? {
    val totalSeconds =
        durationSeconds
            ?.takeIf {
                it > 0L
            }
            ?: return null

    val totalMinutes =
        totalSeconds / 60L

    if (totalMinutes <= 0L) {
        return null
    }

    val hours =
        totalMinutes / 60L

    val minutes =
        totalMinutes % 60L

    return if (hours > 0L) {
        if (minutes > 0L) {
            "${hours}h ${minutes}min"
        } else {
            "${hours}h"
        }
    } else {
        "${minutes}min"
    }
}


/*
 * Cache progressivo e persistente de capas, compartilhado por Filmes e Series.
 *
 * - RAM: LRU adaptativo coordenado com detalhes/carrossel, com cinco futuras.
 * - Disco: orcamento compartilhado de 100 MB, preservado ao reiniciar.
 * - Prefetch: ciclos de 30 em grupos de 5; somente os proximos 5 preparados na RAM.
 * - Rede: ate seis downloads simultaneos, no maximo dois em antecipacao.
 * - Politica LRU: ao exceder o limite, remove somente os arquivos antigos
 *   necessarios, evitando apagar todas as capas a cada reinicio.
 * - As dez capas da Home recebem prioridade de retencao dentro do limite.
 * - Series reutiliza este carregador, cache RAM/disco e mesmas URLs.
 * - Nao altera Room, favoritos, dados da playlist nem a qualidade dos cards.
 */
internal object MoviePosterLoader {
    private const val RAM_CACHE_KB = br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.postersKb
    private const val MAX_POSTER_BYTES = 6L * 1024L * 1024L

    private val memoryCache = object : LruCache<String, ImageBitmap>(RAM_CACHE_KB) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            ((value.asAndroidBitmap().allocationByteCount.toLong() + 1023L) / 1024L)
                .coerceAtLeast(1L)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun clearMemory() { memoryCache.evictAll();br.com.flaviotorres.iptvpremium.ui.components.GenericPosterArtworkCache.trimMemory() }
    fun trimMemory(fraction: Float = .5f) {
        br.com.flaviotorres.iptvpremium.ui.components.GenericPosterArtworkCache.trimMemory()
        memoryCache.trimToSize((memoryCache.maxSize() * fraction.coerceIn(0f, 1f)).toInt())
    }
    private fun configureRam(context: Context) {
        val size = br.com.flaviotorres.iptvpremium.data.cache.ImageRamPolicy.forContext(context).postersMb * 1024
        if (memoryCache.maxSize() != size) memoryCache.resize(size)
    }

    private val decodeSlots = Semaphore(2)
    private val networkBudget = br.com.flaviotorres.iptvpremium.data.cache.PosterNetworkBudget()
    private val urlLocks = Array(64) { Mutex() }

    // Mesma pasta dos builds anteriores: nao perde as imagens existentes.
    private val diskDirectories = java.util.concurrent.ConcurrentHashMap<PosterKind, File>()
    private val ledgers = java.util.concurrent.ConcurrentHashMap<PosterKind, br.com.flaviotorres.iptvpremium.data.cache.PosterFileLru>()
    @Volatile private var protectedHomeFiles: Set<String> = emptySet()

    /**
     * A Home pode reservar ate dez posters para evitar novos downloads.
     * Chamado pela navegacao quando o carrossel recebe os posters reais.
     * URLs vazias sao ignoradas; nunca guarda URLs nos logs.
     */
    fun protectHomePosters(urls: List<String?>) {
        protectedHomeFiles = urls.asSequence()
            .mapNotNull(::validUrl)
            .distinct()
            .take(10)
            .map { "${sha256(it)}.img" }
            .toSet() + ".recent-posters"
    }

    @Synchronized
    private fun persistentDirectory(context: Context, kind: PosterKind = PosterKind.MOVIES): File? {
        diskDirectories[kind]?.takeIf { it.isDirectory }?.let { return it }
        val directory = File(context.applicationContext.cacheDir, kind.directory)
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) return null
        br.com.flaviotorres.iptvpremium.data.cache.PosterTemporaryFiles.cleanupBeforeFirstUse(directory)
        diskDirectories[kind] = directory
        ledgers.remove(kind)
        return directory
    }
    private fun ledger(context: Context, kind: PosterKind): br.com.flaviotorres.iptvpremium.data.cache.PosterFileLru? {
        ledgers[kind]?.let { return it }
        val directory = persistentDirectory(context, kind) ?: return null
        return synchronized(ledgers) {
            ledgers[kind] ?: br.com.flaviotorres.iptvpremium.data.cache.PosterFileLru(directory,
                { br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context).removeDisposable(it) })
                .also { ledgers[kind] = it }
        }
    }
    suspend fun updateVisible(context: Context, kind: PosterKind, urls: List<String?>) = withContext(Dispatchers.IO) {
        ledger(context, kind)?.setVisible(urls.mapNotNull(::validUrl).map(::sha256).toSet())
    }
    fun releaseVisible(context: Context, kind: PosterKind) {
        ledgers[kind]?.setVisible(emptySet())
    }
    suspend fun prefetchCategoryBatch(context: Context, kind: PosterKind, urls: List<String?>, visible: Boolean) =
        kotlinx.coroutines.coroutineScope {
            val batch = urls.mapNotNull(::validUrl).distinct().take(5)
            repeat(minOf(2, batch.size)) { worker -> launch(Dispatchers.IO) {
                for (index in worker until batch.size step 2) {
                    currentCoroutineContext().ensureActive()
                    try { prefetch(context, batch[index], priority = visible, kind = kind) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { }
                }
            } }
        }

    private fun validUrl(rawUrl: String?): String? =
        rawUrl?.trim()?.takeIf {
            it.startsWith("https://", ignoreCase = true) ||
                    it.startsWith("http://", ignoreCase = true)
        }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val alphabet = "0123456789abcdef"
        return buildString(64) {
            for (byte in digest) {
                val value = byte.toInt() and 255
                append(alphabet[value ushr 4]); append(alphabet[value and 15])
            }
        }
    }

    /** Consulta instantanea ao cache RAM, sem rede ou acesso ao disco. */
    fun peekBitmap(rawUrl: String?): ImageBitmap? {
        val url = validUrl(rawUrl) ?: return null
        return memoryCache.get(sha256(url))
    }

    suspend fun prefetch(context: Context, rawUrl: String?, priority: Boolean = false, kind: PosterKind = PosterKind.MOVIES) {
        configureRam(context)
        val url = validUrl(rawUrl) ?: return
        withContext(Dispatchers.IO) {
            val directory = persistentDirectory(context, kind) ?: return@withContext
            val budget = br.com.flaviotorres.iptvpremium.core.storage.ImageDiskCache.get(context)
            budget.prefer(protectedHomeFiles)
            budget.retain(File(directory, "${sha256(url)}.img")).use {
                ensureFile(context, url, background = !priority, kind = kind)
            }
        }
    }

    /** Compressed-file anticipation leaves decoding/RAM to the visible cards. */
    suspend fun prefetchPriorityBatch(context: Context, urls: List<String?>) = kotlinx.coroutines.coroutineScope {
        val batch = urls.mapNotNull(::validUrl).distinct().take(br.com.flaviotorres.iptvpremium.data.cache.PosterAheadQueue.GROUP)
        val workers = br.com.flaviotorres.iptvpremium.data.cache.PosterNetworkBudget.BACKGROUND_REQUESTS
        repeat(minOf(workers, batch.size)) { worker ->
            launch(Dispatchers.IO) {
                for (index in worker until batch.size step workers) {
                    currentCoroutineContext().ensureActive()
                    try { prefetch(context, batch[index], priority = false) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* Uma capa ausente nao interrompe o lote. */ }
                }
            }
        }
    }

    /** Disco: ate 15 a frente em grupos de 5; RAM: apenas os proximos 5, no LRU existente. */
    suspend fun prefetchAhead(
        context: Context,
        queue: br.com.flaviotorres.iptvpremium.data.cache.PosterAheadQueue,
        afterVisible: Int,
        firstLoaded: Int,
        posters: List<String?>
    ) {
        configureRam(context)
        if (br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value) return
        val recentFrom = (afterVisible - firstLoaded - 10).coerceAtLeast(0)
        val recentUntil = (afterVisible - firstLoaded).coerceAtMost(posters.size)
        if (recentFrom < recentUntil) {
            try { saveRecent(context, posters.subList(recentFrom, recentUntil)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Metadado opcional: a fila continua. */ }
        }
        val loadedEnd = firstLoaded + posters.size
        suspend fun prepareNextFive() {
            if (br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value) return
            val from = (afterVisible - firstLoaded).coerceAtLeast(0)
            val until = minOf(posters.size, from + br.com.flaviotorres.iptvpremium.data.cache.PosterAheadQueue.GROUP)
            for (index in from until until) {
                currentCoroutineContext().ensureActive()
                prepareCached(context, posters[index])
                kotlinx.coroutines.yield()
            }
        }
        prepareNextFive()
        while (true) {
            currentCoroutineContext().ensureActive()
            if (br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value) break
            val batch = queue.nextBatch(afterVisible, firstLoaded, loadedEnd) ?: break
            prefetchPriorityBatch(context, posters.subList(batch.first - firstLoaded, batch.last + 1 - firstLoaded))
            queue.completed(batch)
            prepareNextFive()
            kotlinx.coroutines.yield()
        }
    }

    private suspend fun saveRecent(context: Context, urls: List<String?>) = withContext(Dispatchers.IO) {
        val keys = urls.mapNotNull(::validUrl).map(::sha256)
        val text = br.com.flaviotorres.iptvpremium.data.cache.RecentPosterKeys.encode(keys)
        if (text.isEmpty()) return@withContext
        val directory = persistentDirectory(context) ?: return@withContext
        val file = File(directory, ".recent-posters")
        val storage = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
        storage.retain(file).use {
            if (file.isFile && file.length() <= 1024 && runCatching { file.readText() }.getOrNull() == text) return@withContext
            val bytes = text.toByteArray(Charsets.UTF_8)
            val lease = storage.reserve(file, bytes.size.toLong()) ?: return@withContext
            lease.use {
                lease.temporary.writeBytes(bytes)
                currentCoroutineContext().ensureActive()
                lease.temporary.renameTo(file)
            }
        }
    }

    /** Abertura: cinco arquivos conhecidos, sem varrer capas nem consultar a rede. */
    suspend fun prepareRecentCachedPosters(context: Context) = withContext(Dispatchers.IO) {
        configureRam(context)
        val directory = persistentDirectory(context) ?: return@withContext
        val file = File(directory, ".recent-posters")
        if (!file.isFile || file.length() > 1024) return@withContext
        val storage = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
        val keys = br.com.flaviotorres.iptvpremium.data.cache.RecentPosterKeys.decode(file.readText())
        protectHomeKeys(keys)
        storage.prefer(protectedHomeFiles)
        storage.retain(file).close()
        for (key in keys) {
            currentCoroutineContext().ensureActive()
            if (br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value) break
            val poster = File(directory, "$key.img")
            storage.retain(poster).use {
                if (poster.isFile && poster.length() > 0) {
                    decodeCachedKey(key, poster)
                    poster.setLastModified(System.currentTimeMillis())
                }
            }
            kotlinx.coroutines.yield()
        }
    }

    private fun protectHomeKeys(keys: List<String>) {
        protectedHomeFiles = keys.map { "$it.img" }.toSet() + ".recent-posters"
        // A preferencia atua dentro da cota, sem impedir a rotacao quando necessario.
    }

    /** Apenas RAM/disco: nunca abre conexao para preparar uma capa antecipada. */
    private suspend fun prepareCached(context: Context, rawUrl: String?) {
        val url = validUrl(rawUrl) ?: return
        if (memoryCache.get(sha256(url)) != null) return
        withContext(Dispatchers.IO) {
            val directory = persistentDirectory(context) ?: return@withContext
            val file = File(directory, "${sha256(url)}.img")
            br.com.flaviotorres.iptvpremium.core.storage.ImageDiskCache.get(context).retain(file).use {
                if (file.isFile && file.length() > 0L) decodeCachedFile(url, file)
            }
        }
    }

    /** Cache-only carousel lookup: bounded known URLs, never scans directories or downloads. */
    suspend fun cachedPosterUrls(context: Context, rawUrls: List<String?>): Set<String> = withContext(Dispatchers.IO) {
        val directory = persistentDirectory(context) ?: return@withContext emptySet()
        rawUrls.mapNotNull(::validUrl).distinct().take(125).filterTo(LinkedHashSet()) { url ->
            currentCoroutineContext().ensureActive()
            val file = File(directory, "${sha256(url)}.img")
            memoryCache.get(sha256(url)) != null || (file.isFile && file.length() in 1..MAX_POSTER_BYTES)
        }
    }

    /** RAM -> retained local file only. Safe while the selected media owns network priority. */
    suspend fun loadCached(context: Context, rawUrl: String?): ImageBitmap? {
        configureRam(context)
        val url = validUrl(rawUrl) ?: return null
        memoryCache.get(sha256(url))?.let { return it }
        return withContext(Dispatchers.IO) {
            val directory = persistentDirectory(context) ?: return@withContext null
            val file = File(directory, "${sha256(url)}.img")
            br.com.flaviotorres.iptvpremium.core.storage.ImageDiskCache.get(context).retain(file).use {
                if (file.isFile && file.length() in 1..MAX_POSTER_BYTES) decodeCachedFile(url, file) else null
            }
        }
    }

    private val posterMisses=java.util.concurrent.ConcurrentHashMap<String,Long>()
    private val missesLock=kotlinx.coroutines.sync.Mutex()
    @Volatile private var missesReady=false
    suspend fun prepareFailures(context:Context)=withContext(Dispatchers.IO) {
        missesLock.lock()
        try {
            if(!missesReady) {
                val now=System.currentTimeMillis()
                val prefs=context.applicationContext.getSharedPreferences("poster_failures_v1",Context.MODE_PRIVATE)
                val retained=prefs.all.entries.filter {(_,value)->value is Long && now-value in 0L..300_000L}
                    .sortedByDescending {it.value as Long}.take(512)
                val edit=prefs.edit().clear()
                retained.forEach{(key,value)->posterMisses[key]=value as Long;edit.putLong(key,value)}
                edit.apply()
                missesReady=true
            }
        } finally {missesLock.unlock()}
    }
    fun knownMissing(rawUrl:String?):Boolean {
        val url=validUrl(rawUrl)?:return true
        val failed=posterMisses[sha256(url)]?:return false
        return System.currentTimeMillis()-failed in 0L..300_000L
    }
    /** One visible attempt; recent failures reuse the shared fallback instead of retrying on every return. */
    suspend fun loadWithRetry(context: Context, rawUrl: String?, kind: PosterKind = PosterKind.MOVIES): ImageBitmap? {
        val url=validUrl(rawUrl)?:return null
        peekBitmap(url)?.let{return it}
        prepareFailures(context)
        if(knownMissing(url))return loadCached(context,url)
        currentCoroutineContext().ensureActive()
        val result=load(context,url,kind)
        currentCoroutineContext().ensureActive()
        if(result==null && !br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.value &&
            !br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value) {
            withContext(Dispatchers.IO) {
                val now=System.currentTimeMillis();val key=sha256(url)
                posterMisses[key]=now
                val prefs=context.applicationContext.getSharedPreferences("poster_failures_v1",Context.MODE_PRIVATE)
                val edit=prefs.edit().putLong(key,now)
                if(posterMisses.size>512)posterMisses.entries.sortedBy{it.value}.take(posterMisses.size-512).forEach {
                    posterMisses.remove(it.key);edit.remove(it.key)
                }
                edit.apply()
            }
        }
        return result
    }

    suspend fun load(context: Context, rawUrl: String?, kind: PosterKind = PosterKind.MOVIES): ImageBitmap? {
        configureRam(context)
        val url = validUrl(rawUrl) ?: return null
        memoryCache.get(sha256(url))?.let { return it }
        return withContext(Dispatchers.IO) {
            val directory = persistentDirectory(context, kind) ?: return@withContext null
            val budget = br.com.flaviotorres.iptvpremium.core.storage.ImageDiskCache.get(context)
            budget.prefer(protectedHomeFiles)
            budget.retain(File(directory, "${sha256(url)}.img")).use {
                loadUnmanaged(context, url, kind)
            }
        }
    }

    /** Antecipa o arquivo comprimido sem alocar bitmap extra na RAM. */
    private suspend fun prefetchUnmanaged(context: Context, rawUrl: String?) {
        val url = validUrl(rawUrl) ?: return
        if (memoryCache.get(sha256(url)) != null) return
        ensureFile(context, url, background = true)
    }

    /** Usado por Filmes, Series e carrossel: RAM -> disco persistente -> rede. */
    private suspend fun loadUnmanaged(context: Context, rawUrl: String?, kind: PosterKind): ImageBitmap? {
        val url = validUrl(rawUrl) ?: return null
        memoryCache.get(sha256(url))?.let { return it }

        val file = ensureFile(context, url, background = false, kind = kind) ?: return null
        return decodeCachedFile(url, file)
    }

    private suspend fun decodeCachedFile(url: String, file: File): ImageBitmap? =
        decodeCachedKey(sha256(url), file)

    private suspend fun decodeCachedKey(key: String, file: File): ImageBitmap? =
        withContext(Dispatchers.Default) {
            decodeSlots.withPermit {
            memoryCache.get(key)?.let { return@withPermit it }

            val bitmap = try {
                br.com.flaviotorres.iptvpremium.data.cache.PosterResolution.decode(file, Bitmap.Config.RGB_565)?.asImageBitmap()
            } catch (_: OutOfMemoryError) {
                memoryCache.evictAll()
                null
            } catch (_: Exception) {
                null
            }
            if (bitmap != null) {
                memoryCache.put(key, bitmap)
            } else {
                file.delete()
            }
            bitmap
            }
        }

    private suspend fun ensureFile(context: Context, url: String, background: Boolean,
        kind: PosterKind = PosterKind.MOVIES): File? = withContext(Dispatchers.IO) {
        val urlLock = urlLocks[(url.hashCode() and Int.MAX_VALUE) % urlLocks.size]
        urlLock.lock()
        try {
            val directory = persistentDirectory(context, kind) ?: return@withContext null
            val key = sha256(url)
            val file = File(directory, "$key.img")
            val slot = ledger(context, kind)?.acquire(key) ?: return@withContext null
            slot.use {
                if (file.isFile && file.length() > 0L) {
                    file.setLastModified(System.currentTimeMillis())
                    return@withContext file
                }
                // Reuse compressed old/shared files without another network request.
                val candidates = listOf("iptv_movie_posters_session") + PosterKind.entries.filter { it != kind }.map { it.directory }
                val storage = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
                for (name in candidates) {
                    val source = File(File(context.applicationContext.cacheDir, name), "$key.img")
                    if (!source.isFile || source.length() == 0L) continue
                    storage.retain(source).use {
                        val reservation = storage.reserve(file, source.length()) ?: return@use
                        reservation.use { lease ->
                            source.inputStream().use { input -> lease.temporary.outputStream().use { output -> input.copyTo(output) } }
                            currentCoroutineContext().ensureActive()
                            if (lease.temporary.renameTo(file)) return@withContext file
                        }
                    }
                }
                if (!networkBudget.run(background) { download(context, url, file, background) }) return@withContext null
                file.takeIf { it.isFile && it.length() > 0L }
            }
        } finally { urlLock.unlock() }
    }

    private suspend fun download(context: Context, url: String, target: File, background: Boolean): Boolean {
        var reservation: br.com.flaviotorres.iptvpremium.core.storage.CacheStorageBudget.Reservation? = null
        var connection: HttpURLConnection? = null
        var backgroundInterrupt: kotlinx.coroutines.Job? = null
        try {
            currentCoroutineContext().ensureActive()
            if (br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.value) return false
            val parent = target.parentFile ?: return false
            if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) return false
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            val activeConnection = connection
            backgroundInterrupt = if(background)
                br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.interruptBackground { activeConnection.disconnect() }
            else br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority
                .interruptBackgroundOnSelection { activeConnection.disconnect() }
            currentCoroutineContext().ensureActive()
            connection.connect()
            if (connection.responseCode !in 200..299) return false
            if (connection.contentLengthLong > MAX_POSTER_BYTES) return false
            reservation = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
                .reserve(target, connection.contentLengthLong.takeIf { it > 0 }?.coerceAtLeast(1_000_000L) ?: MAX_POSTER_BYTES) ?: return false
            val temporary = reservation.temporary

            var byteCount = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temporary).buffered().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        byteCount += read
                        if (byteCount > reservation.limit) return false
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (byteCount == 0L) return false
            if (!br.com.flaviotorres.iptvpremium.data.cache.PosterResolution.prepareDownloaded(temporary, reservation.limit)) return false
            return temporary.renameTo(target)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return false
        } finally {
            backgroundInterrupt?.cancel()
            reservation?.close()
            connection?.disconnect()
        }
    }


}

private fun categoryCount(
    category: MovieCategory,
    allMovies: List<PreviewMovie>,
    favoriteMovieIds: Set<String>
): Int {
    return when (category) {
        MovieCategory.CONTINUE_WATCHING -> {
            minOf(
                3,
                allMovies.size
            )
        }

        MovieCategory.FAVORITES -> {
            allMovies.count { movie ->
                movie.id in favoriteMovieIds
            }
        }

        MovieCategory.ALL -> {
            allMovies.size
        }

        MovieCategory.RECENTLY_ADDED -> {
            minOf(
                6,
                allMovies.size
            )
        }

        MovieCategory.NETFLIX,
        MovieCategory.MAX,
        MovieCategory.DISNEY_PLUS,
        MovieCategory.PRIME_VIDEO,
        MovieCategory.GLOBOPLAY,
        MovieCategory.DISCOVERY_PLUS,
        MovieCategory.APPLE_TV_PLUS -> {
            allMovies.count { movie ->
                movie.providerCategory == category
            }
        }

        else -> {
            allMovies.count { movie ->
                movie.category == category
            }
        }
    }
}

private fun emptyCategoryMessage(
    category: MovieCategory
): String {
    return when (category) {
        MovieCategory.CONTINUE_WATCHING ->
            "Nenhum filme em andamento."

        MovieCategory.FAVORITES ->
            "Nenhum filme favorito."

        MovieCategory.RECENTLY_ADDED ->
            "Nenhum filme adicionado recentemente."

        else ->
            "Nenhum filme nesta categoria."
    }
}

@Composable
private fun FavoriteHeart(
    isFavorite: Boolean,
    size: Int
) {
    Canvas(
        modifier = Modifier.size(
            size.dp
        )
    ) {
        val path = Path()

        val width = this.size.width
        val height = this.size.height

        path.moveTo(
            width * 0.50f,
            height * 0.88f
        )

        path.cubicTo(
            width * 0.42f,
            height * 0.80f,
            width * 0.08f,
            height * 0.58f,
            width * 0.08f,
            height * 0.32f
        )

        path.cubicTo(
            width * 0.08f,
            height * 0.12f,
            width * 0.23f,
            height * 0.05f,
            width * 0.36f,
            height * 0.05f
        )

        path.cubicTo(
            width * 0.44f,
            height * 0.05f,
            width * 0.49f,
            height * 0.10f,
            width * 0.50f,
            height * 0.17f
        )

        path.cubicTo(
            width * 0.51f,
            height * 0.10f,
            width * 0.56f,
            height * 0.05f,
            width * 0.64f,
            height * 0.05f
        )

        path.cubicTo(
            width * 0.77f,
            height * 0.05f,
            width * 0.92f,
            height * 0.12f,
            width * 0.92f,
            height * 0.32f
        )

        path.cubicTo(
            width * 0.92f,
            height * 0.58f,
            width * 0.58f,
            height * 0.80f,
            width * 0.50f,
            height * 0.88f
        )

        path.close()

        if (isFavorite) {
            drawPath(
                path = path,
                brush = PremiumGoldBorder
            )
        } else {
            drawPath(
                path = path,
                brush = PremiumGoldBorder,
                style = Stroke(
                    width = 1.6.dp.toPx()
                )
            )
        }
    }
}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_CATALOG_GLASS_V1

// IPTV_POLISH_LOADING_HOLD_V1

// Text retains the approved palette; blue darkening applies to rims.
private val MetallicGoldBrushText get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFF926315))
)
