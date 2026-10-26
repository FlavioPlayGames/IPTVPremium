package br.com.flaviotorres.iptvpremium.ui.series

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import br.com.flaviotorres.iptvpremium.data.cache.loadWhilePosterVisible

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
import android.os.SystemClock
import androidx.compose.foundation.basicMarquee
import br.com.flaviotorres.iptvpremium.ui.components.catalogGridHost
import br.com.flaviotorres.iptvpremium.ui.components.CatalogGridNavigator
import br.com.flaviotorres.iptvpremium.ui.components.catalogGridNavigation
import br.com.flaviotorres.iptvpremium.ui.components.BindCatalogFocus
import br.com.flaviotorres.iptvpremium.ui.components.CatalogPosterPlaceholder
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
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
import br.com.flaviotorres.iptvpremium.domain.model.Series
import br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import br.com.flaviotorres.iptvpremium.ui.components.AppKeyboard
import br.com.flaviotorres.iptvpremium.ui.components.TvKeyboardSearchText
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardType
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardDelete
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardMoveCursor
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val FAVORITE_LONG_PRESS_MS = 1_000L

// Series utiliza o MoviePosterLoader de Filmes: disco LRU de 250 MB.
// Somente 5 capas por lote e 2 posicoes de antecedencia ao rolar.
// Os 15 primeiros logos/backdrops ja sao preparados no AppNavigation.

private val CinzelSemiBold = FontFamily(
    Font(
        resId = R.font.cinzel_semibold,
        weight = FontWeight.SemiBold
    )
)

private val MetallicGoldBrush get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF926315))
)

private val AppGold get() = if(KidsAppearance.active) kidsCinemaAccent() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFD6A32E)
private val AppGoldLight get() = if(KidsAppearance.active) kidsCinemaAccent() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFE6A0)

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

private val PremiumPanelBackground get() = if(KidsAppearance.active) kidsCinemaPanelBrush() else br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.panelBrush

private fun Modifier.premiumGlow(enabled: Boolean, roundedDp: Float = 12f): Modifier = this


// Mesmo contorno metalizado das categorias de Canais e Filmes.
// O segundo filete aparece somente na categoria focada/selecionada.
private fun Modifier.premiumCategoryCardBorder(
    highlighted: Boolean,
    roundedDp: Float = 12f
): Modifier = if(KidsAppearance.active) this else if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) {
    if(highlighted) this.reinforcedCatalogFocus(true, roundedDp) else this.kidsCinemaBorder(.8.dp, br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.categoryEdge, RoundedCornerShape(roundedDp.dp))
} else this.drawBehind {
    val radius = CornerRadius(roundedDp.dp.toPx())
    val outerStroke = if (highlighted) 1.8.dp.toPx() else 1.2.dp.toPx()

    drawRoundRect(
        brush = PremiumGoldBorder,
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




/**
 * Remove somente os prefixos repetidos que os provedores adicionam ao
 * nome da categoria. O nome original e o ID permanecem intactos no Room.
 * Ex.: "Series | Amazon Prime Video" -> "Amazon Prime Video".
 */
private val seriesCategoryPrefix = Regex(
    pattern = """^\s*s[ée]ries?\s*[|｜:–—-]\s*""",
    option = RegexOption.IGNORE_CASE
)

private fun displaySeriesCategoryName(originalName: String): String {
    // Mesma classificacao/normalizacao visual do catalogo de Filmes.
    // Mantem ID, contagem e ordem original da categoria do provedor.
    val original = originalName.trim()
    val withoutPrefix = original.replaceFirst(seriesCategoryPrefix, "").trim()
    return when (withoutPrefix.lowercase(Locale("pt", "BR"))) {
        "acao" -> "Ação"
        "animacao" -> "Animação"
        "comedia" -> "Comédia"
        "familia" -> "Família"
        "ficcao" -> "Ficção"
        "ficcao cientifica" -> "Ficção Científica"
        "documentarios" -> "Documentários"
        else -> withoutPrefix.ifBlank { original }
    }
}

private enum class SeriesCategory(
    val title: String,
    val brandText: String,
    val brandColor: Color
) {
    CONTINUE_WATCHING(
        title = "Continuar assistindo",
        brandText = "▶",
        brandColor = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFC928)
    ),

    FAVORITES(
        title = "Favoritos",
        brandText = "♥",
        brandColor = Color(0xFFFF3045)
    ),

    ALL(
        title = "Todos",
        brandText = "▦",
        brandColor = Color(0xFF2FA8FF)
    ),

    RECENTLY_ADDED(
        title = "Adicionados recentemente",
        brandText = "◷",
        brandColor = Color(0xFF1ED98B)
    ),

    NETFLIX(
        title = "Netflix",
        brandText = "N",
        brandColor = Color(0xFFE50914)
    ),

    LEGENDADAS(
        title = "Legendadas",
        brandText = "▣",
        brandColor = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFC400)
    ),

    MAX(
        title = "Max",
        brandText = "max",
        brandColor = Color(0xFFA855F7)
    ),

    DISNEY_PLUS(
        title = "Disney Plus",
        brandText = "Disney+",
        brandColor = Color(0xFF28C7FA)
    ),

    PRIME_VIDEO(
        title = "Amazon Prime Video",
        brandText = "prime",
        brandColor = Color(0xFF21A8E8)
    ),

    GLOBOPLAY(
        title = "Globoplay",
        brandText = "globoplay",
        brandColor = Color(0xFFFF3264)
    ),

    DISCOVERY_PLUS(
        title = "Discovery Plus",
        brandText = "D+",
        brandColor = Color(0xFF49D7CA)
    ),

    APPLE_TV_PLUS(
        title = "Apple TV Plus",
        brandText = "TV+",
        brandColor = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFF5F5F5)
    )
}

private enum class SortMode {
    DEFAULT,
    A_Z,
    Z_A
}

private data class PreviewSeries(
    val id: String,
    val name: String,
    val providerCategory: SeriesCategory
)

@Composable
internal fun KidsSeriesCatalog(
    favoriteSeriesIds: Set<String> = emptySet(),
    onSeriesClick: (String) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    // Argumentos opcionais: o AppNavigation os conectara na proxima etapa.
    pagedSeries: LazyPagingItems<Series>? = null,
    pagingCategories: List<Category> = emptyList(),
    totalSeriesCount: Int = 0,
    pagingCategoryCounts: Map<String?, Int> = emptyMap(),
    onCategoryAccessRequested: (String?, String, () -> Unit) -> Unit = { _, _, proceed -> proceed() },
    onSourceCategorySelected: (String?) -> Unit = {},
    onFavoritesSelected: () -> Unit = {},
    onContinueWatchingSelected: () -> Unit = {},
    watchHistoryEntries: List<WatchHistoryEntry> = emptyList(),
    onSearchQueryChanged: (String) -> Unit = {},
    onSortModeChanged: (String) -> Unit = {},
    onSeriesFocused: (String) -> Unit = {},
    onSeriesBlurred: (String) -> Unit = {},
    onCatalogInteraction: (Boolean) -> Unit = {},
    posterPage: (suspend (Int) -> br.com.flaviotorres.iptvpremium.data.cache.PosterPage)? = null,
    onResolvePosterCategory: (suspend (String)->String?)? = null
) {
    // Preserva temporariamente a interface anterior ate a rota real ser ligada.
    // Com Paging, a tela usa o catalogo real e nunca cria series ficticias.
    if (pagedSeries != null) {
        RealSeriesCatalogScreen(
            pagedSeries = pagedSeries,
            pagingCategories = pagingCategories,
            totalSeriesCount = totalSeriesCount,
            pagingCategoryCounts = pagingCategoryCounts,
            favoriteSeriesIds = favoriteSeriesIds,
            onCategoryAccessRequested = onCategoryAccessRequested,
            onSourceCategorySelected = onSourceCategorySelected,
            onFavoritesSelected = onFavoritesSelected,
            onContinueWatchingSelected = onContinueWatchingSelected,
            watchHistoryEntries = watchHistoryEntries,
            onSearchQueryChanged = onSearchQueryChanged,
            onSortModeChanged = onSortModeChanged,
            onSeriesClick = onSeriesClick,
            onFavoriteClick = onFavoriteClick,
            onSeriesFocused = onSeriesFocused,
            onSeriesBlurred = onSeriesBlurred,
            onCatalogInteraction = onCatalogInteraction,
            posterPage = posterPage,
            onResolvePosterCategory = onResolvePosterCategory
        )
        return
    }
    var selectedCategory by remember {
        mutableStateOf(SeriesCategory.ALL)
    }

    var selectedSeriesId by remember {
        mutableStateOf<String?>(null)
    }

    // Identifica o card a receber novamente o foco após mudar o favorito.
    var favoriteFocusSeriesId by remember {
        mutableStateOf<String?>(null)
    }
    var favoriteFocusRequest by remember {
        mutableLongStateOf(0L)
    }

    var searchText by remember {
        mutableStateOf("")
    }
    // Destaque da letra que sera substituida; no fim mostra a barra dourada.
    var searchCursorPosition by rememberSaveable { mutableIntStateOf(0) }

    var searchKeyboardVisible by remember {
        mutableStateOf(false)
    }

    var sortMode by remember {
        mutableStateOf(SortMode.DEFAULT)
    }

    BackHandler(
        enabled = searchKeyboardVisible
    ) {
        searchKeyboardVisible = false
    }

    /*
     * 10 séries:
     * exatamente 2 fileiras de 5 no protótipo.
     */
    val previewSeries = remember {
        listOf(
            PreviewSeries(
                "1",
                "Sombras de uma Cidade",
                SeriesCategory.NETFLIX
            ),
            PreviewSeries(
                "2",
                "A Coroa Perdida",
                SeriesCategory.MAX
            ),
            PreviewSeries(
                "3",
                "Horizontes Distantes",
                SeriesCategory.DISNEY_PLUS
            ),
            PreviewSeries(
                "4",
                "O Preço da Verdade",
                SeriesCategory.PRIME_VIDEO
            ),
            PreviewSeries(
                "5",
                "Terra em Ruínas",
                SeriesCategory.LEGENDADAS
            ),
            PreviewSeries(
                "6",
                "Reinos em Guerra",
                SeriesCategory.MAX
            ),
            PreviewSeries(
                "7",
                "Linha Tênue",
                SeriesCategory.NETFLIX
            ),
            PreviewSeries(
                "8",
                "Vidas Cruzadas",
                SeriesCategory.GLOBOPLAY
            ),
            PreviewSeries(
                "9",
                "Segredos do Lago",
                SeriesCategory.DISCOVERY_PLUS
            ),
            PreviewSeries(
                "10",
                "Órbita Zero",
                SeriesCategory.APPLE_TV_PLUS
            )
        )
    }

    val categoryFilteredSeries =
        when (selectedCategory) {
            SeriesCategory.CONTINUE_WATCHING -> {
                previewSeries.take(3)
            }

            SeriesCategory.FAVORITES -> {
                previewSeries.filter { series ->
                    series.id in favoriteSeriesIds
                }
            }

            SeriesCategory.ALL -> {
                previewSeries
            }

            SeriesCategory.RECENTLY_ADDED -> {
                previewSeries.takeLast(6)
            }

            else -> {
                previewSeries.filter { series ->
                    series.providerCategory == selectedCategory
                }
            }
        }

    val searchedSeries =
        if (searchText.isBlank()) {
            categoryFilteredSeries
        } else {
            categoryFilteredSeries.filter { series ->
                series.name.contains(
                    searchText.trim(),
                    ignoreCase = true
                )
            }
        }

    val displayedSeries =
        when (sortMode) {
            SortMode.DEFAULT -> {
                searchedSeries
            }

            SortMode.A_Z -> {
                searchedSeries.sortedBy {
                    it.name.lowercase()
                }
            }

            SortMode.Z_A -> {
                searchedSeries.sortedByDescending {
                    it.name.lowercase()
                }
            }
        }

    val previewGridColumns = br.com.flaviotorres.iptvpremium.ui.settings.rememberSettingsPreferences().catalogColumns()
    BoxWithConstraints(
        modifier = Modifier
            .catalogEscapeBack()
            .fillMaxSize()
            .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF070708))
    ) {
        /*
         * Em telas Android TV a resolução em dp pode ser
         * bem menor que a resolução física em pixels.
         *
         * Por isso usamos dois níveis de escala.
         */
        val compactLayout =
            maxHeight < 650.dp ||
                    maxWidth < 1200.dp

        val outerHorizontalPadding =
            if(KidsAppearance.active) maxWidth*.025f else if (compactLayout) 10.dp else 16.dp

        val outerVerticalPadding =
            if(KidsAppearance.active) maxHeight*.055f else if (compactLayout) 10.dp else 16.dp

        val sidebarWidth =
            if(KidsAppearance.active) maxWidth*.205f else maxWidth*.225f

        val sidebarGap =
            if(KidsAppearance.active) 6.dp else if (compactLayout) 6.dp else 8.dp

        val categoryHeight =
            if (compactLayout) 38.dp else 42.dp

        val categoryGap =
            if (compactLayout) 5.dp else 7.dp

        val categoryFontSize =
            if (compactLayout) 11.sp else 14.sp

        val posterAspectRatio =
            if (compactLayout) {
                0.72f
            } else {
                0.67f
            }

        val gridHorizontalSpacing =
            if(KidsAppearance.active) 6.dp else if (compactLayout) 4.dp else 6.dp

        val gridVerticalSpacing =
            if (compactLayout) 10.dp else 18.dp

        /*
         * Fundo preto/dourado.
         */
        br.com.flaviotorres.iptvpremium.ui.components.CatalogThemeBackdrop(Modifier.matchParentSize(), cinemaKids=true, cinemaTitle="SÉRIES")

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = outerHorizontalPadding,
                    end = outerHorizontalPadding,
                    top = outerVerticalPadding,
                    bottom = if(KidsAppearance.active) maxHeight*.025f else if (compactLayout) 4.dp else 6.dp
                )
        ) {
            /*
             * =========================================================
             * MENU ESQUERDO
             * =========================================================
             */
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(if (compactLayout) 14.dp else 18.dp))
                    .background(
                        brush = (if(KidsAppearance.active) kidsCinemaPanelBrush() else br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.categoryPanelBrush),
                        shape = RoundedCornerShape(if (compactLayout) 14.dp else 18.dp)
                    )
                    .kidsCinemaBorder(
                        border = BorderStroke(1.dp, if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) androidx.compose.ui.graphics.SolidColor(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.edge) else PremiumGoldBorder),
                        shape = RoundedCornerShape(if (compactLayout) 14.dp else 18.dp)
                    )
                    
                    .padding(
                        horizontal = if (compactLayout) 8.dp else 12.dp,
                        vertical = if (compactLayout) 6.dp else 10.dp
                    )
            ) {
                if (KidsAppearance.active) { KidsCinemaCatalogHeader(compactLayout) } else {
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLogo(compactLayout = compactLayout)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CATEGORIAS",
                            color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFF5F5F5),
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
                if(!KidsAppearance.active) PremiumDivider(compactLayout = compactLayout)
                Spacer(Modifier.height(if (compactLayout) 8.dp else 12.dp))

                /*
                 * 1
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.CONTINUE_WATCHING,
                    count = categoryCount(
                        SeriesCategory.CONTINUE_WATCHING,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.CONTINUE_WATCHING,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.CONTINUE_WATCHING
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 2
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.FAVORITES,
                    count = categoryCount(
                        SeriesCategory.FAVORITES,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.FAVORITES,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.FAVORITES
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 3
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.ALL,
                    count = categoryCount(
                        SeriesCategory.ALL,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.ALL,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.ALL
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 4
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.RECENTLY_ADDED,
                    count = categoryCount(
                        SeriesCategory.RECENTLY_ADDED,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.RECENTLY_ADDED,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.RECENTLY_ADDED
                    }
                )

                Spacer(
                    modifier = Modifier.height(
                        if (compactLayout) 6.dp else 10.dp
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF333337)
                        )
                )

                Spacer(
                    modifier = Modifier.height(
                        if (compactLayout) 6.dp else 10.dp
                    )
                )

                /*
                 * 5
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.NETFLIX,
                    count = categoryCount(
                        SeriesCategory.NETFLIX,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.NETFLIX,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.NETFLIX
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 6
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.LEGENDADAS,
                    count = categoryCount(
                        SeriesCategory.LEGENDADAS,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.LEGENDADAS,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.LEGENDADAS
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 7
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.MAX,
                    count = categoryCount(
                        SeriesCategory.MAX,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.MAX,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.MAX
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 8
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.DISNEY_PLUS,
                    count = categoryCount(
                        SeriesCategory.DISNEY_PLUS,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.DISNEY_PLUS,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.DISNEY_PLUS
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 9
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.PRIME_VIDEO,
                    count = categoryCount(
                        SeriesCategory.PRIME_VIDEO,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.PRIME_VIDEO,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.PRIME_VIDEO
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 10
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.GLOBOPLAY,
                    count = categoryCount(
                        SeriesCategory.GLOBOPLAY,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.GLOBOPLAY,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.GLOBOPLAY
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 11
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.DISCOVERY_PLUS,
                    count = categoryCount(
                        SeriesCategory.DISCOVERY_PLUS,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.DISCOVERY_PLUS,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.DISCOVERY_PLUS
                    }
                )

                Spacer(Modifier.height(categoryGap))

                /*
                 * 12
                 */
                SeriesCategoryItem(
                    category = SeriesCategory.APPLE_TV_PLUS,
                    count = categoryCount(
                        SeriesCategory.APPLE_TV_PLUS,
                        previewSeries,
                        favoriteSeriesIds
                    ),
                    isSelected =
                        selectedCategory ==
                                SeriesCategory.APPLE_TV_PLUS,
                    height = categoryHeight,
                    fontSize = categoryFontSize,
                    compactLayout = compactLayout,
                    onSelect = {
                        selectedCategory =
                            SeriesCategory.APPLE_TV_PLUS
                    }
                )
            }

            Spacer(
                modifier = Modifier.width(sidebarGap)
            )

            /*
             * =========================================================
             * ÁREA PRINCIPAL
             * =========================================================
             */
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                TopBar(
                    searchText = searchText,
                    searchActive = searchKeyboardVisible,
                    searchCursorPosition = searchCursorPosition,
                    onSearchClick = {
                        searchCursorPosition = searchText.length
                        searchKeyboardVisible = true
                    },
                    onSearchClear = {
                        searchText = ""
                        searchCursorPosition = 0
                    },
                    sortMode = sortMode,
                    onSortClick = {
                        sortMode =
                            when (sortMode) {
                                SortMode.DEFAULT ->
                                    SortMode.A_Z

                                SortMode.A_Z ->
                                    SortMode.Z_A

                                SortMode.Z_A ->
                                    SortMode.DEFAULT
                            }
                    },
                    compactLayout = compactLayout
                )

                Spacer(
                    modifier = Modifier.height(
                        if(KidsAppearance.active) 6.dp else if (compactLayout) 4.dp else 6.dp
                    )
                )

                br.com.flaviotorres.iptvpremium.ui.components.CatalogTwoRowFrame(
                    columns = previewGridColumns,
                    compactLayout = compactLayout,
                    framed = KidsAppearance.active,
                ) { posterAspectRatio ->
                    if (displayedSeries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text =
                                    if (searchText.isNotBlank()) {
                                        "Nenhuma série encontrada."
                                    } else {
                                        emptyCategoryMessage(
                                            selectedCategory
                                        )
                                    },
                                color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFB7B7BD),
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        /*
                         * EXATAMENTE 5 COLUNAS.
                         *
                         * Com 10 séries:
                         * 5 + 5.
                         */
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(previewGridColumns),
                            modifier = Modifier.fillMaxSize(),
                            // Resguarda o zoom de foco (3,5%) também na primeira capa.
                            // Segue o mesmo criterio da grade de Filmes.
                            contentPadding = PaddingValues(if(KidsAppearance.active)2.dp else 4.dp),
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
                                items = displayedSeries,
                                key = { series ->
                                    series.id
                                }
                            ) { series ->
                                SeriesCard(
                                    seriesId = series.id,
                                    focusRestorationSeriesId = favoriteFocusSeriesId,
                                    focusRestorationRequest = favoriteFocusRequest,
                                    name = series.name,
                                    isSelected =
                                        selectedSeriesId ==
                                                series.id,
                                    isFavorite =
                                        series.id in
                                                favoriteSeriesIds,
                                    posterAspectRatio =
                                        posterAspectRatio,
                                    compactLayout =
                                        compactLayout,
                                    onSelect = {
                                        selectedSeriesId =
                                            series.id

                                        onSeriesClick(
                                            series.id
                                        )
                                    },
                                    onFavoriteClick = { fromFocusedCard ->
                                        if (fromFocusedCard) {
                                            // Ao remover no filtro Favoritos, o item some.
                                            // Escolhemos um vizinho, sem voltar ao primeiro.
                                            favoriteFocusSeriesId =
                                                if (
                                                    selectedCategory == SeriesCategory.FAVORITES &&
                                                    series.id in favoriteSeriesIds
                                                ) {
                                                    nextSeriesFavoriteFocusId(
                                                        displayedSeries.map { it.id },
                                                        series.id
                                                    )
                                                } else {
                                                    series.id
                                                }
                                            favoriteFocusRequest++
                                        }
                                        onFavoriteClick(series.id)
                                    }
                                )
                            }
                        }
                    }
                } // SeriesCatalogFrame
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
                    onCharacter = { value ->
                        val edited = tvKeyboardType(searchText, searchCursorPosition, value)
                        searchText = edited.value
                        searchCursorPosition = edited.cursor
                    },
                    onBackspace = {
                        val edited = tvKeyboardDelete(searchText, searchCursorPosition)
                        searchText = edited.value
                        searchCursorPosition = edited.cursor
                    },
                    onMoveCursorLeft = {
                        searchCursorPosition = tvKeyboardMoveCursor(searchText, searchCursorPosition, -1)
                    },
                    onMoveCursorRight = {
                        searchCursorPosition = tvKeyboardMoveCursor(searchText, searchCursorPosition, 1)
                    },
                    onAction = { searchKeyboardVisible = false }
                )
            }
        }
    }
}

/**
 * Catalogo real de Series, usando Room + Paging, sem carregar tudo na RAM.
 *
 * A grade e os menus reutilizam os mesmos componentes visuais do prototipo.
 * Filtros de favoritos, busca e ordenação usam as consultas SQL do Room.
 * Nenhum filtro da grade é aplicado a uma amostra parcial do Paging.
 */
@Composable
private fun RealSeriesCatalogScreen(
    pagedSeries: LazyPagingItems<Series>,
    pagingCategories: List<Category>,
    totalSeriesCount: Int,
    pagingCategoryCounts: Map<String?, Int>,
    favoriteSeriesIds: Set<String>,
    onCategoryAccessRequested: (String?, String, () -> Unit) -> Unit,
    onSourceCategorySelected: (String?) -> Unit,
    onFavoritesSelected: () -> Unit,
    onContinueWatchingSelected: () -> Unit,
    watchHistoryEntries: List<WatchHistoryEntry>,
    onSearchQueryChanged: (String) -> Unit,
    onSortModeChanged: (String) -> Unit,
    onSeriesClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onSeriesFocused: (String) -> Unit = {},
    onSeriesBlurred: (String) -> Unit = {},
    onCatalogInteraction: (Boolean) -> Unit = {},
    posterPage: (suspend (Int) -> br.com.flaviotorres.iptvpremium.data.cache.PosterPage)? = null,
    onResolvePosterCategory: (suspend (String)->String?)? = null
) {
    // Decode the local TV-room scene while browsing, so details can reuse its retained bitmap.
    br.com.flaviotorres.iptvpremium.ui.theme.rememberSeriesRoomReady()
    // O NavHost mantém esta rota e o estado de grade ao abrir detalhes.
    // rememberSaveable restaura categoria, busca e posição ao voltar.
    val menuFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    var catalogContentFocused by remember { mutableStateOf(false) }
    var categoryMenuActive by rememberSaveable { mutableStateOf(true) }
    var returningToCategory by remember { mutableStateOf(false) }
    val selectedCategoryFocus = remember { FocusRequester() }
    val catalogInputMode = androidx.compose.ui.platform.LocalInputModeManager.current
    val categoryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    var selectedMenu by rememberSaveable { mutableStateOf("all") }
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSeriesId by rememberSaveable { mutableStateOf<String?>(null) }
    var favoriteFocusSeriesId by remember { mutableStateOf<String?>(null) }
    var favoriteFocusRequest by remember { mutableLongStateOf(0L) }
    var returningSeriesFocusId by remember { mutableStateOf(selectedSeriesId) }
    var returningSeriesFocusRequest by remember { mutableLongStateOf(if(selectedSeriesId!=null)1L else 0L) }
    var searchText by rememberSaveable { mutableStateOf("") }
    // Mesmo cursor editavel e piscante aprovado no cadastro e em Filmes.
    var searchCursorPosition by rememberSaveable { mutableIntStateOf(0) }
    var searchKeyboardVisible by remember { mutableStateOf(false) }
    var appliedSearchText by rememberSaveable { mutableStateOf(searchText) }
    LaunchedEffect(searchText) {
        if (searchText.isNotBlank()) delay(br.com.flaviotorres.iptvpremium.data.cache.CATALOG_SEARCH_SETTLE_MS)
        appliedSearchText = searchText
    }

    var sortMode by rememberSaveable { mutableStateOf(SortMode.DEFAULT) }
    val gridState = rememberLazyGridState()

    // Mesmo carregador compartilhado por Filmes e Series. Esta tela antecipa
    // apenas as capas de series JA presentes na janela do Paging/Room.
    val posterCacheContext = LocalContext.current.applicationContext
    val posterIdleDelayMs = remember(posterCacheContext) {
        val mode = posterCacheContext.resources.configuration.uiMode and
                Configuration.UI_MODE_TYPE_MASK
        val isTv = mode == Configuration.UI_MODE_TYPE_TELEVISION ||
                posterCacheContext.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        if (isTv) 100L else 80L
    }

    // O foco do controle remoto reinicia a pausa da fila sem recompor a grade.
    val posterFocusActivity = remember { MutableStateFlow(0L) }
    LaunchedEffect(gridState) {
        combine(snapshotFlow { searchKeyboardVisible || gridState.isScrollInProgress }, posterFocusActivity) { busy, _ -> busy }
            .collectLatest { busy ->
                onCatalogInteraction(true)
                if (!busy) { delay(180L); onCatalogInteraction(false) }
            }
    }
    DisposableEffect(Unit) { onDispose { onCatalogInteraction(false) } }


    val posterQueueJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    fun openCatalogSeries(id: String) {
        // Save grid ownership before the destination is pushed, including mouse/touch opens.
        categoryMenuActive=false
        posterQueueJob[0]?.cancel()
        onSeriesClick(id)
    }

    br.com.flaviotorres.iptvpremium.ui.movies.CategoryPosterPrefetch(
        br.com.flaviotorres.iptvpremium.data.cache.PosterKind.SERIES,
        "$selectedMenu:${selectedProviderId.orEmpty()}:$searchText:$sortMode",
        gridState, pagedSeries, { it.posterUrl },
        searchKeyboardVisible || selectedMenu == "continue", posterPage, navigationActivity = posterFocusActivity
    )

    // Com a volta dos detalhes, mantém a posição e restaura o foco da capa.
    LaunchedEffect(pagedSeries) {
        if (selectedSeriesId == null) {
            categoryMenuActive = true
            br.com.flaviotorres.iptvpremium.ui.components.restoreCatalogCategoryFocus(
                categoryListState, 0, selectedCategoryFocus, menuFocusRequester, catalogInputMode)
        }
        selectedSeriesId?.let {
            categoryMenuActive = false
            if(returningSeriesFocusId!=it) {
                returningSeriesFocusId = it
                returningSeriesFocusRequest++
            }
            android.util.Log.d("SeriesReturnPerf","phase=restoring_grid menu=$selectedMenu")
        }
        // Sincroniza o ViewModel se esta tela voltar do back stack.
        onSearchQueryChanged(searchText)
        onSortModeChanged(sortMode.name)
        if (selectedMenu == "favorites") {
            onFavoritesSelected()
        } else if (selectedMenu != "continue") {
            onSourceCategorySelected(selectedProviderId)
        }
    }

    var pendingFirstSeriesFocus by remember { mutableStateOf(false) }
    var firstSeriesFocusRequest by remember { mutableLongStateOf(0L) }
    var beforeCategorySnapshot by remember { mutableStateOf<Any?>(null) }

    fun selectMenu(menu: String, categoryId: String? = null) {
        onCategoryAccessRequested(categoryId, pagingCategories.firstOrNull { it.id == categoryId }?.name.orEmpty()) {
        val sameCategory = selectedMenu == menu && selectedProviderId == categoryId
        beforeCategorySnapshot = if (sameCategory || menu == "continue") null else pagedSeries.itemSnapshotList
        categoryMenuActive = false
        pendingFirstSeriesFocus = true
        firstSeriesFocusRequest++
        if (sameCategory) return@onCategoryAccessRequested
        selectedMenu = menu
        selectedProviderId = categoryId
        selectedSeriesId = null
        favoriteFocusSeriesId = null
        returningSeriesFocusId = null
        searchKeyboardVisible = false
        when (menu) {
            "favorites" -> onFavoritesSelected()
            "continue" -> onContinueWatchingSelected()
            else -> onSourceCategorySelected(categoryId)
        }
    
        }
    }

    // Nova categoria e novos resultados começam no topo.
    // Na primeira composição NÃO derrubamos a posição restaurada do back stack.
    var gridFilterInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(selectedMenu, selectedProviderId, appliedSearchText, sortMode) {
        if (gridFilterInitialized) {
            gridState.scrollToItem(0)
        } else {
            gridFilterInitialized = true
        }
    }


    // Mesmo criterio do catalogo de Filmes: categorias reais, sem
    // descartar IDs nem modificar a ordem entregue pelo provedor.
    val realCategories = pagingCategories
    val posterCategoryNames=remember(pagingCategories){pagingCategories.associate{it.id to it.name}}
    val continueWatchingPlaceholder = selectedMenu == "continue"
    val continueWatchingEntries = remember(watchHistoryEntries) {
        watchHistoryEntries
            .filter { it.contentType == PlaybackContentType.EPISODE &&
                    !it.seriesId.isNullOrBlank() && it.inProgress }
            .sortedByDescending { it.updatedAtMs }
            .distinctBy { it.seriesId }
    }

    val gridColumns = br.com.flaviotorres.iptvpremium.ui.settings.rememberSettingsPreferences().catalogColumns()
    val firstSeriesFocusReady = pendingFirstSeriesFocus &&
        (beforeCategorySnapshot == null || beforeCategorySnapshot !== pagedSeries.itemSnapshotList) &&
        (selectedMenu == "continue" || pagedSeries.loadState.refresh !is LoadState.Loading)
    val navScope = androidx.compose.runtime.rememberCoroutineScope()
    val navCount by rememberUpdatedState(if (selectedMenu == "continue") continueWatchingEntries.size else pagedSeries.itemCount)
    val navLoad by rememberUpdatedState<(Int) -> Unit>({ index -> if (selectedMenu != "continue") { pagedSeries[index]; Unit } })
    val navManual by rememberUpdatedState<() -> Unit>({
        pendingFirstSeriesFocus = false; beforeCategorySnapshot = null
        returningSeriesFocusId = null; favoriteFocusSeriesId = null; posterFocusActivity.value++
    })
    val gridNavigator = remember(gridState, gridColumns) { CatalogGridNavigator(gridState, navScope, gridColumns, { navCount }, { navLoad(it) }, { navManual() },
        onTop = { searchFocusRequester.requestFocus() },
        onLeft = { categoryMenuActive = true; menuFocusRequester.requestFocus() }) }
    DisposableEffect(gridNavigator) { onDispose { gridNavigator.cancel() } }
    LaunchedEffect(selectedMenu, selectedProviderId, appliedSearchText, sortMode) {
        gridNavigator.filterChanged(listOf(selectedMenu, selectedProviderId, appliedSearchText, sortMode))
    }
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
        val index = when (selectedMenu) {
            "continue" -> 1
            "favorites" -> 2
            "provider" -> 4 + realCategories.indexOfFirst { it.id == selectedProviderId }.coerceAtLeast(0)
            else -> 0
        }
        navScope.launch {
            try {
                android.util.Log.d("SeriesReturnPerf","phase=category_focus_request index=$index")
                br.com.flaviotorres.iptvpremium.ui.components.restoreCatalogCategoryFocus(
                    categoryListState, index, selectedCategoryFocus, menuFocusRequester, catalogInputMode)
            } finally { returningToCategory = false }
        }
        return true
    }
    BackHandler(enabled = searchKeyboardVisible || returningToCategory || !categoryMenuActive || catalogContentFocused) {
        handleCatalogBack()
    }

    BoxWithConstraints(
        modifier = Modifier.catalogEscapeBack(onBack = ::handleCatalogBack).fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF070708))
    ) {
        val compactLayout = maxHeight < 650.dp || maxWidth < 1200.dp
        val outerHorizontalPadding = if(KidsAppearance.active) maxWidth*.025f else if (compactLayout) 10.dp else 16.dp
        val outerVerticalPadding = if(KidsAppearance.active) maxHeight*.055f else if (compactLayout) 10.dp else 16.dp
        val sidebarWidth = if(KidsAppearance.active) maxWidth*.205f else maxWidth*.225f
        val sidebarGap = if(KidsAppearance.active) 6.dp else if (compactLayout) 6.dp else 8.dp
        val categoryHeight = if (compactLayout) 38.dp else 42.dp
        val categoryGap = if (compactLayout) 6.dp else 8.dp
        val categoryFontSize = if (compactLayout) 11.sp else 14.sp
        val posterAspectRatio = if (compactLayout) 0.72f else 0.67f
        val gridHorizontalSpacing = if(KidsAppearance.active) 6.dp else if (compactLayout) 4.dp else 6.dp
        val gridVerticalSpacing = if (compactLayout) 10.dp else 18.dp

        br.com.flaviotorres.iptvpremium.ui.components.CatalogThemeBackdrop(Modifier.matchParentSize(), cinemaKids=true, cinemaTitle="SÉRIES")

        Row(
            modifier = Modifier.fillMaxSize().padding(
                start = outerHorizontalPadding,
                end = outerHorizontalPadding,
                top = outerVerticalPadding,
                bottom = if(KidsAppearance.active) maxHeight*.025f else if (compactLayout) 4.dp else 6.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(if (compactLayout) 14.dp else 18.dp))
                    .background(
                        brush = PremiumPanelBackground,
                        shape = RoundedCornerShape(if (compactLayout) 14.dp else 18.dp)
                    )
                    .kidsCinemaBorder(
                        border = BorderStroke(1.dp, if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) androidx.compose.ui.graphics.SolidColor(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.edge) else PremiumGoldBorder),
                        shape = RoundedCornerShape(if (compactLayout) 14.dp else 18.dp)
                    )
                    
                    .padding(
                        horizontal = if (compactLayout) 8.dp else 12.dp,
                        vertical = if (compactLayout) 6.dp else 10.dp
                    )
            ) {
                if (KidsAppearance.active) { KidsCinemaCatalogHeader(compactLayout) } else {
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLogo(compactLayout = compactLayout)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CATEGORIAS",
                            color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFF5F5F5),
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
                if(!KidsAppearance.active) PremiumDivider(compactLayout = compactLayout)
                Spacer(Modifier.height(if (compactLayout) 8.dp else 12.dp))

                // As categorias reais podem ser numerosas: barra lateral rolavel.
                LazyColumn(
                    state = categoryListState,
                    modifier = Modifier.testTag("series-category-list").fillMaxSize()
                        // Isolate this narrow scrolling panel from reused display lists during return.
                        .graphicsLayer { clip=true;compositingStrategy=androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        .focusRequester(menuFocusRequester).focusGroup()
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.DirectionRight) {
                                if (event.type == KeyEventType.KeyDown) { categoryMenuActive = false; gridNavigator.enter() }
                                true
                            } else false
                        },
                    verticalArrangement = Arrangement.spacedBy(categoryGap)
                ) {
                    item(key = "all") {
                        SeriesCategoryItem(
                            returnFocus = selectedCategoryFocus,
                            showKeyboardFocus = categoryMenuActive && !catalogContentFocused && !returningToCategory,
                            category = SeriesCategory.ALL,
                            count = totalSeriesCount,
                            isSelected = selectedMenu == "all",
                            height = categoryHeight,
                            fontSize = categoryFontSize,
                            compactLayout = compactLayout,
                            onSelect = { selectMenu("all") }
                        )
                    }
                    item(key = "continue") {
                        SeriesCategoryItem(
                            returnFocus = selectedCategoryFocus,
                            showKeyboardFocus = categoryMenuActive && !catalogContentFocused && !returningToCategory,
                            category = SeriesCategory.CONTINUE_WATCHING,
                            count = continueWatchingEntries.size,
                            isSelected = selectedMenu == "continue",
                            height = categoryHeight,
                            fontSize = categoryFontSize,
                            compactLayout = compactLayout,
                            onSelect = { selectMenu("continue") }
                        )
                    }
                    item(key = "favorites") {
                        SeriesCategoryItem(
                            returnFocus = selectedCategoryFocus,
                            showKeyboardFocus = categoryMenuActive && !catalogContentFocused && !returningToCategory,
                            category = SeriesCategory.FAVORITES,
                            count = favoriteSeriesIds.size,
                            isSelected = selectedMenu == "favorites",
                            height = categoryHeight,
                            fontSize = categoryFontSize,
                            compactLayout = compactLayout,
                            onSelect = { selectMenu("favorites") }
                        )
                    }
                    item(key = "separator") {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(1.dp)
                                .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF333337))
                        )
                    }
                    lazyListItems(realCategories, key = { "provider:${it.id}" }) { category ->
                        SeriesCategoryItem(
                            returnFocus = selectedCategoryFocus,
                            showKeyboardFocus = categoryMenuActive && !catalogContentFocused && !returningToCategory,
                            category = SeriesCategory.ALL,
                            customTitle = displaySeriesCategoryName(category.name),
                            count = pagingCategoryCounts[category.id] ?: 0,
                            isSelected = selectedMenu == "provider" &&
                                    selectedProviderId == category.id,
                            height = categoryHeight,
                            fontSize = categoryFontSize,
                            compactLayout = compactLayout,
                            onSelect = { selectMenu("provider", category.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.width(sidebarGap))
            Column(modifier = Modifier.weight(1f)
                .onFocusChanged { catalogContentFocused = it.hasFocus }.focusGroup().fillMaxSize()) {
                TopBar(
                    searchFocusRequester = searchFocusRequester,
                    onGridEnter = { gridNavigator.enter() },
                    searchText = searchText,
                    searchActive = searchKeyboardVisible,
                    searchCursorPosition = searchCursorPosition,
                    onSearchClick = {
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
                        sortMode = when (sortMode) {
                            SortMode.DEFAULT -> SortMode.A_Z
                            SortMode.A_Z -> SortMode.Z_A
                            SortMode.Z_A -> SortMode.DEFAULT
                        }
                        onSortModeChanged(sortMode.name)
                    },
                    compactLayout = compactLayout
                )
                Spacer(Modifier.height(if(KidsAppearance.active) 6.dp else if (compactLayout) 4.dp else 6.dp))
                br.com.flaviotorres.iptvpremium.ui.components.CatalogTwoRowFrame(
                    columns = gridColumns,
                    compactLayout = compactLayout,
                    framed = KidsAppearance.active,
                ) { posterAspectRatio ->
                    val refreshState = pagedSeries.loadState.refresh

                    if (continueWatchingPlaceholder) {
                        if (continueWatchingEntries.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Nenhuma série em andamento.",
                                    color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFF2F0E8),
                                    fontSize = if (compactLayout) 12.sp else 15.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridColumns),
                                state = gridState,
                                modifier = Modifier.fillMaxSize().catalogGridHost(gridNavigator),
                                contentPadding = PaddingValues(if(KidsAppearance.active)2.dp else 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(gridHorizontalSpacing),
                                verticalArrangement = Arrangement.spacedBy(LocalCatalogRowSpacing.current)
                            ) {
                                items(continueWatchingEntries.size) { index ->
                                    val entry = continueWatchingEntries[index]
                                    val id = entry.seriesId ?: return@items
                                    SeriesCard(
                                        navigator = gridNavigator, gridIndex = index, allowRestoration = !searchKeyboardVisible && !categoryMenuActive && !returningToCategory,
                                        seriesId = id,
                                        focusRestorationSeriesId = if (firstSeriesFocusReady && index == 0) id else returningSeriesFocusId,
                                        focusRestorationRequest = if (firstSeriesFocusReady && index == 0) firstSeriesFocusRequest else returningSeriesFocusRequest,
                                        name = entry.seriesTitle ?: entry.title,
                                        posterUrl = entry.seriesPosterUrl ?: entry.posterUrl,
                                        resolvePosterCategory = onResolvePosterCategory,
                                        isSelected = selectedSeriesId == id,
                                        isFavorite = id in favoriteSeriesIds,
                                        posterAspectRatio = posterAspectRatio,
                                        compactLayout = compactLayout,
                                        onFocused = { categoryMenuActive = false;
                                            if (index == 0 && firstSeriesFocusReady) { pendingFirstSeriesFocus = false; beforeCategorySnapshot = null }
                                            onSeriesFocused(id)
                                        },
                                        onBlurred = { onSeriesBlurred(id) },
                                        onSelect = { selectedSeriesId = id; openCatalogSeries(id) },
                                        onFavoriteClick = { _ -> onFavoriteClick(id) }
                                    )
                                }
                            }
                        }
                    } else if (pagedSeries.itemCount == 0 && refreshState is LoadState.Loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Carregando series...", color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFFFFFFF), fontSize = 15.sp)
                        }
                    } else if (pagedSeries.itemCount == 0 && refreshState is LoadState.Error) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Falha ao carregar series. Tente novamente.", color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFFFFFFF))
                        }
                    } else if (pagedSeries.itemCount == 0) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (searchText.isNotBlank()) "Nenhuma série encontrada."
                                else "Nenhuma série nesta categoria.",
                                color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFB7B7BD)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            state = gridState,
                            modifier = Modifier.fillMaxSize().catalogGridHost(gridNavigator),
                            // O padding DEVE ser interno ao LazyVerticalGrid, nao externo.
                            // Assim o card ampliado nao e cortado no topo/lateral.
                            contentPadding = PaddingValues(if(KidsAppearance.active)2.dp else 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(gridHorizontalSpacing),
                            verticalArrangement = Arrangement.spacedBy(LocalCatalogRowSpacing.current)
                        ) {
                            items(
                                count = pagedSeries.itemCount,
                                key = pagedSeries.itemKey { it.id }
                            ) { index ->
                                val series = pagedSeries[index]
                                if (series != null) {
                                    SeriesCard(
                                        navigator = gridNavigator, gridIndex = index, allowRestoration = !searchKeyboardVisible && !categoryMenuActive && !returningToCategory,
                                        seriesId = series.id,
                                        focusRestorationSeriesId =
                                            if (firstSeriesFocusReady && index == 0) series.id else returningSeriesFocusId ?: favoriteFocusSeriesId,
                                        focusRestorationRequest =
                                            if (firstSeriesFocusReady && index == 0) firstSeriesFocusRequest else if (returningSeriesFocusId != null) {
                                                returningSeriesFocusRequest
                                            } else {
                                                favoriteFocusRequest
                                            },
                                        name = series.name,
                                        posterUrl = series.posterUrl,
                                        posterCategory = posterCategoryNames[series.categoryId].orEmpty()+" "+series.genres.joinToString(" "),
                                        resolvePosterCategory = onResolvePosterCategory,
                                        year = series.year,
                                        rating = series.rating,
                                        isSelected = selectedSeriesId == series.id,
                                        isFavorite = series.id in favoriteSeriesIds,
                                        onBlurred = { onSeriesBlurred(series.id) },
                                        onFocused = { categoryMenuActive = false;
                                            // Cancela a antecipacao enquanto o usuario navega.
                                            posterFocusActivity.value = posterFocusActivity.value + 1L
                                            onSeriesFocused(series.id)
                                            if (index == 0 && firstSeriesFocusReady) { pendingFirstSeriesFocus = false; beforeCategorySnapshot = null }
                                            if (returningSeriesFocusId == series.id) {
                                                returningSeriesFocusId = null
                                                android.util.Log.d("SeriesReturnPerf","phase=grid_focus_restored")
                                            }
                                        },
                                        posterAspectRatio = posterAspectRatio,
                                        compactLayout = compactLayout,
                                        onSelect = {
                                            selectedSeriesId = series.id
                                            openCatalogSeries(series.id)
                                        },
                                        onFavoriteClick = { fromFocusedCard ->
                                            if (fromFocusedCard) {
                                                favoriteFocusSeriesId =
                                                    if (selectedMenu == "favorites" &&
                                                        series.id in favoriteSeriesIds) {
                                                        nextSeriesFavoriteFocusId(
                                                            pagedSeries.itemSnapshotList.items.map { it.id },
                                                            series.id
                                                        )
                                                    } else {
                                                        series.id
                                                    }
                                                favoriteFocusRequest++
                                            }
                                            onFavoriteClick(series.id)
                                        }
                                    )
                                } else { CatalogPosterPlaceholder(posterAspectRatio) }
                            }
                        }
                    }
                } // SeriesCatalogFrame
            }
        }

        if (searchKeyboardVisible) {
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    start = outerHorizontalPadding,
                    end = outerHorizontalPadding,
                    bottom = 8.dp
                ),
                contentAlignment = Alignment.BottomCenter
            ) {
                AppKeyboard(
                    modifier = Modifier.fillMaxWidth(),
                    actionText = "CONCLUIR",
                    onCharacter = { value ->
                        val edited = tvKeyboardType(searchText, searchCursorPosition, value)
                        searchText = edited.value
                        searchCursorPosition = edited.cursor
                        onSearchQueryChanged(edited.value)
                    },
                    onBackspace = {
                        val edited = tvKeyboardDelete(searchText, searchCursorPosition)
                        searchText = edited.value
                        searchCursorPosition = edited.cursor
                        onSearchQueryChanged(edited.value)
                    },
                    onMoveCursorLeft = {
                        searchCursorPosition = tvKeyboardMoveCursor(searchText, searchCursorPosition, -1)
                    },
                    onMoveCursorRight = {
                        searchCursorPosition = tvKeyboardMoveCursor(searchText, searchCursorPosition, 1)
                    },
                    onAction = { searchKeyboardVisible = false; searchFocusRequester.requestFocus() }
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
            .kidsCinemaBorder(BorderStroke(1.dp, if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) androidx.compose.ui.graphics.SolidColor(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.edge) else PremiumGoldBorder), shape)
            .padding(horizontal = if (compactLayout) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 9.dp)
    ) {
        SeriesTopAction(
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
private fun SeriesTopAction(
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
                brush = if(KidsAppearance.active) androidx.compose.ui.graphics.SolidColor(Color.Transparent) else if (focused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBrush()
                else Brush.verticalGradient((if(KidsAppearance.active) kidsCinemaCategoryColors(false) else listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10)))),
                shape = shape
            )
            .premiumGlow(focused)
            .kidsCinemaBorder(
                border = BorderStroke(if (focused) 1.5.dp else 1.dp, br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(focused, PremiumGoldBorder)),
                shape = shape
            )
            .kidsCinemaToolbarFocus(focused)
            .onFocusChanged { focused = it.isFocused }
            .kidsCinemaClickable(onClick = onClick)
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
            1 -> Text("≡", fontSize = if (compactLayout) 13.sp else 15.sp, color = (if (focused) kidsCinemaRowInk(true,0xFFFFFFFF) else AppGold))
            else -> Text("♡", fontSize = if (compactLayout) 13.sp else 15.sp, color = (if (focused) kidsCinemaRowInk(true,0xFFFFFFFF) else AppGold))
        }
        Text(
            text = label,
            color = if (focused) kidsCinemaRowInk(true,0xFFFFFFFF) else br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFF5F5F5),
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
                    if (highlighted) { kidsCinemaSelectionColors() } else {
                        (if(KidsAppearance.active) kidsCinemaCategoryColors(false) else listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10)))
                    }
                ),
                shape = shape
            )
            .kidsCinemaBorder(
                border = BorderStroke(
                    width = if (highlighted) 1.7.dp else 1.dp,
                    brush = if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder) else PremiumQuietEdge
                ),
                shape = shape
            )
            .kidsCinemaToolbarFocus(isFocused,active)
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
            .kidsCinemaClickable(onClick = onClick)
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
            textColor = kidsCinemaRowInk(active || isFocused,0xFFF5F5F5),
            placeholderColor = kidsCinemaRowInk(active || isFocused,0xFF909096),
            value = value,
            active = active,
            cursorPosition = cursorPosition,
            placeholder = if (KidsAppearance.active) "Qual aventura vamos assistir?" else "Pesquisar séries...",
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
                    color = (if (active || isFocused) kidsCinemaRowInk(true,0xFFFFFFFF) else AppGoldLight),
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

    if(KidsAppearance.active) { KidsCinemaClock(currentTime.format(timeFormatter));return }
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
            color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFF5F5F5),
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
                .background(
                    br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFF77777C)
                )
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
private fun SeriesCategoryItem(
    category: SeriesCategory,
    count: Int,
    isSelected: Boolean,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    compactLayout: Boolean,
    onSelect: () -> Unit,
    customTitle: String? = null,
    returnFocus: FocusRequester? = null,
    showKeyboardFocus: Boolean = true
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    // Persistent selection is stable; transient platform focus is painted only in the menu.
    val visibleFocus=isFocused && showKeyboardFocus
    val shape = RoundedCornerShape(
        if (compactLayout) 10.dp else 12.dp
    )

    Row(
        modifier = Modifier
            .then(if (isSelected && returnFocus != null) Modifier.focusRequester(returnFocus) else Modifier)
            .testTag("series-category-" + (customTitle ?: category.name))
            .fillMaxWidth()
            .height(height)
            // Clip focus/click indication to the same rounded outline as the fill.
            .then(if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) Modifier.clip(shape) else Modifier)
            .background(
                brush = Brush.horizontalGradient(
                    if (visibleFocus || isSelected) { kidsCinemaCategoryColors(true) } else {
                        kidsCinemaCategoryColors(false)
                    }
                ),
                shape = shape
            )
            .premiumGlow(visibleFocus || isSelected)
            .premiumCategoryCardBorder(
                highlighted = visibleFocus || isSelected,
                roundedDp = if (compactLayout) 10f else 12f
            )
            .kidsCinemaFocus(visibleFocus,isSelected)
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
            .kidsCinemaClickable(onClick = onSelect)
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
        if (!KidsAppearance.active) {
            if (customTitle != null) {
                SeriesCategoryIcon(type = SeriesNavIcon.GENERIC, compactLayout = compactLayout)
            } else {
                BrandMark(category = category, compactLayout = compactLayout)
            }
        }

        Spacer(
            modifier = Modifier.width(
                if(KidsAppearance.active) {
                    0.dp
                } else if (compactLayout) {
                    9.dp
                } else {
                    12.dp
                }
            )
        )

        Text(
            text = kidsCinemaCategoryLabel(customTitle ?: category.title),
            modifier = Modifier.weight(1f),
            color = when {
                visibleFocus ->
                    kidsCinemaRowInk(true,0xFFFFFFFF)

                isSelected ->
                    kidsCinemaRowInk(true,0xFFFFFFFF)

                else ->
                    br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFF5F5F5)
            },
            fontSize = fontSize,
            fontWeight =
                if (
                    visibleFocus ||
                    isSelected
                ) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            lineHeight=if(KidsAppearance.active)14.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
            maxLines = if(KidsAppearance.active)2 else 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = count.toString(),
            modifier=Modifier.padding(start=if(KidsAppearance.active)8.dp else 0.dp).widthIn(min=if(KidsAppearance.active)25.dp else 0.dp),
            textAlign=androidx.compose.ui.text.style.TextAlign.End,
            color =
                if (
                    visibleFocus ||
                    isSelected
                ) {
                    kidsCinemaRowInk(true,0xFFFFFFFF)
                } else {
                    br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFB7B7BD)
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
private enum class SeriesNavIcon { ALL, CONTINUE, FAVORITES, GENERIC }

@Composable
private fun BrandMark(
    category: SeriesCategory,
    compactLayout: Boolean
) {
    if (KidsAppearance.active) return
    val type = when (category) {
        SeriesCategory.ALL -> SeriesNavIcon.ALL
        SeriesCategory.CONTINUE_WATCHING -> SeriesNavIcon.CONTINUE
        SeriesCategory.FAVORITES -> SeriesNavIcon.FAVORITES
        else -> SeriesNavIcon.GENERIC
    }
    SeriesCategoryIcon(type = type, compactLayout = compactLayout)
}

@Composable
private fun SeriesCategoryIcon(
    type: SeriesNavIcon,
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
                SeriesNavIcon.ALL -> {
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
                SeriesNavIcon.CONTINUE -> {
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
                SeriesNavIcon.FAVORITES -> {
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
                SeriesNavIcon.GENERIC -> {
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
 * Mesmo comportamento aprovado para os cards de Filmes:
 * OK/Enter curto abre os detalhes; 1 segundo segurando alterna o favorito
 * exatamente uma vez, sem mover o foco do card.
 */
private fun nextSeriesFavoriteFocusId(
    visibleIds: List<String>,
    removedId: String
): String? {
    val index = visibleIds.indexOf(removedId)
    if (index < 0) return null
    return visibleIds.getOrNull(index + 1)
        ?: visibleIds.getOrNull(index - 1)
}

@Composable
private fun SeriesCard(
    seriesId: String,
    focusRestorationSeriesId: String?,
    focusRestorationRequest: Long,
    name: String,
    posterUrl: String? = null,
    posterCategory:String = "",
    resolvePosterCategory:(suspend (String)->String?)? = null,
    year: Int? = null,
    rating: Double? = null,
    isSelected: Boolean,
    isFavorite: Boolean,
    posterAspectRatio: Float,
    compactLayout: Boolean,
    onSelect: () -> Unit,
    onFavoriteClick: (Boolean) -> Unit,
    onFocused: () -> Unit = {},
    onBlurred: () -> Unit = {},
    navigator: CatalogGridNavigator? = null,
    gridIndex: Int = -1,
    allowRestoration: Boolean = true
) {
    val fittedPosterWidth = br.com.flaviotorres.iptvpremium.ui.components.LocalCatalogPosterWidth.current
    var isFocused by remember { mutableStateOf(false) }
    val visuallyFocused = isFocused || navigator?.isMovingSelection(gridIndex) == true
    val focusScale by animateFloatAsState(if (visuallyFocused) { if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) 1.03f else 1.035f } else 1f,
        animationSpec = tween(140), label = "PosterFocusScale")
    val context = LocalContext.current.applicationContext
    val artworkEpoch by br.com.flaviotorres.iptvpremium.ui.theme.ThemeArtworkEpoch.collectAsState()
    var posterVisible by remember { mutableStateOf(false) }
    val posterState by key(seriesId,posterUrl,artworkEpoch) {
        val cached=MoviePosterLoader.peekBitmap(posterUrl)
        produceState(br.com.flaviotorres.iptvpremium.ui.components.PosterArtworkState(cached,
            cached!=null||MoviePosterLoader.knownMissing(posterUrl))) {
            value=br.com.flaviotorres.iptvpremium.ui.components.PosterArtworkState(
                loadWhilePosterVisible(snapshotFlow { posterVisible }) {
                    MoviePosterLoader.loadWithRetry(context,posterUrl,br.com.flaviotorres.iptvpremium.data.cache.PosterKind.SERIES)
                },true)
        }
    }
    val poster=posterState.bitmap
    val focusRequester = remember { FocusRequester() }
    BindCatalogFocus(navigator, gridIndex, focusRequester)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnFavoriteClick by rememberUpdatedState(onFavoriteClick)

    var selectKeyDownTime by remember { mutableLongStateOf(0L) }
    var selectKeyHeld by remember { mutableStateOf(false) }
    var keyboardFavoriteTriggered by remember { mutableStateOf(false) }
    var pointerFavoriteTriggered by remember { mutableStateOf(false) }

    // Relógio independente da repetição de teclas do controle remoto.
    // Quando completa 1 segundo, dispara uma única vez por pressionamento.
    LaunchedEffect(selectKeyHeld) {
        if (selectKeyHeld) {
            delay(FAVORITE_LONG_PRESS_MS)
            if (selectKeyHeld && isFocused && !keyboardFavoriteTriggered) {
                keyboardFavoriteTriggered = true
                currentOnFavoriteClick(true)
            }
        }
    }

    // Mantém o mesmo card selecionado após a alteração, inclusive se
    // a grade for recomposta. No filtro Favoritos pode apontar ao vizinho.
    LaunchedEffect(focusRestorationRequest, focusRestorationSeriesId, allowRestoration) {
        if (allowRestoration && focusRestorationRequest > 0L && focusRestorationSeriesId == seriesId) {
            repeat(3) {
                androidx.compose.runtime.withFrameNanos { }
                if (!isFocused) runCatching { focusRequester.requestFocus() }
            }
            // An already-focused card does not emit another focus-change event.
            if(isFocused)onFocused()
        }
    }

    val shape = RoundedCornerShape(br.com.flaviotorres.iptvpremium.ui.components.CATALOG_POSTER_CORNER_DP.dp)

    Column(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                posterVisible = bounds.width > 0f && bounds.height > 0f
            }
            .then(if (fittedPosterWidth.value.isFinite()) Modifier.wrapContentWidth(Alignment.CenterHorizontally).width(fittedPosterWidth).height(LocalCatalogCardHeight.current) else Modifier)
            .graphicsLayer {
                val scale = if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) 1f else focusScale
                scaleX = scale; scaleY = scale
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    navigator?.focused(gridIndex)
                    onFocused()
                } else {
                    onBlurred()
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
                            // Repetições não reiniciam o cronômetro nem
                            // provocam múltiplas mudanças no favorito.
                            if (!selectKeyHeld && event.nativeKeyEvent.repeatCount == 0) {
                                selectKeyDownTime = SystemClock.elapsedRealtime()
                                keyboardFavoriteTriggered = false
                                selectKeyHeld = true
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            if (selectKeyHeld) {
                                val duration = SystemClock.elapsedRealtime() - selectKeyDownTime
                                if (keyboardFavoriteTriggered || duration >= FAVORITE_LONG_PRESS_MS) {
                                    if (!keyboardFavoriteTriggered) {
                                        keyboardFavoriteTriggered = true
                                        currentOnFavoriteClick(true)
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
            // Mouse e toque também respeitam 1 segundo. Não utilizamos
            // o long press nativo (~500 ms) do combinedClickable.
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pointerFavoriteTriggered = false
                        val releasedBeforeTimeout = withTimeoutOrNull(FAVORITE_LONG_PRESS_MS) {
                            tryAwaitRelease()
                        }
                        if (releasedBeforeTimeout == null) {
                            pointerFavoriteTriggered = true
                            currentOnFavoriteClick(isFocused)
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
                onClick(label = "Abrir detalhes da série") {
                    currentOnSelect()
                    true
                }
            }
            .focusable(),
        horizontalAlignment = Alignment.Start
    ) {
        /*
         * POSTER
         *
         * 5 por fileira.
         * Em TV grande usa proporção próxima de 2:3.
         */
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
                .background(
                    brush = Brush.verticalGradient(
                        colors =
                            if (
                                visuallyFocused
                            ) { kidsCinemaSelectionColors() } else {
                                listOf(
                                    br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF242428),
                                    br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF151518)
                                )
                            }
                    ),
                    shape = shape
                )
                .premiumGlow(visuallyFocused, roundedDp = br.com.flaviotorres.iptvpremium.ui.components.CATALOG_POSTER_CORNER_DP)
                .kidsCinemaBorder(
                    border = BorderStroke(
                        width = if (visuallyFocused) 2.dp else 1.dp,
                        brush = if (visuallyFocused) {
                            PremiumGoldBorder
                        } else {
                            if(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) androidx.compose.ui.graphics.SolidColor(Color.Transparent) else Brush.linearGradient(
                                listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF776034), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF3B342A))
                            )
                        }
                    ),
                    shape = shape
                )
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            if (poster != null) {
                Image(
                    bitmap = poster!!,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (fittedPosterWidth.value.isFinite()) ContentScale.Crop else ContentScale.Fit
                )
            } else if(posterState.finished) {
                br.com.flaviotorres.iptvpremium.ui.components.GenericPosterArtwork("series",seriesId,name,
                    posterCategory,resolvePosterCategory,Modifier.fillMaxSize())
            }
            if(KidsAppearance.active && visuallyFocused) KidsCinemaPosterStar(Modifier.align(Alignment.TopEnd).padding(5.dp))
            // Foco por cima da arte, com metalizado da paleta ativa.
            Box(Modifier.matchParentSize().kidsCinemaPosterFrame(
                visuallyFocused, br.com.flaviotorres.iptvpremium.ui.components.CATALOG_POSTER_CORNER_DP
            ))


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

        // Mesmos metadados de classificacao mostrados nos cards de Filmes.
        val metadata = remember(rating) {
            String.format(Locale.US, "Nota: %.1f", rating?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0)
        }
        if(!KidsAppearance.active) {
        Text(
            text = name,
            lineHeight = if (compactLayout) 14.sp else 18.sp,
            modifier = if (isFocused) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 800, repeatDelayMillis = 1000) else Modifier,
            color =
                if (
                    visuallyFocused
                ) {
                    br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFFFFFFF)
                } else {
                    br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFFF5F5F5)
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
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (metadata.isNotBlank()) Text(
                text = metadata,
                modifier = Modifier.weight(1f, fill = false),
                lineHeight = if (compactLayout) 12.sp else 16.sp,
                color = br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFF9D9DA3),
                fontSize = if (compactLayout) 9.sp else 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            br.com.flaviotorres.iptvpremium.ui.components.CatalogFavoriteHeart(isFavorite, compactLayout) {
                currentOnFavoriteClick(false)
            }
        }
    }
}

private fun categoryCount(
    category: SeriesCategory,
    allSeries: List<PreviewSeries>,
    favoriteSeriesIds: Set<String>
): Int {
    return when (category) {
        SeriesCategory.CONTINUE_WATCHING -> {
            minOf(
                3,
                allSeries.size
            )
        }

        SeriesCategory.FAVORITES -> {
            allSeries.count { series ->
                series.id in favoriteSeriesIds
            }
        }

        SeriesCategory.ALL -> {
            allSeries.size
        }

        SeriesCategory.RECENTLY_ADDED -> {
            minOf(
                6,
                allSeries.size
            )
        }

        else -> {
            allSeries.count { series ->
                series.providerCategory ==
                        category
            }
        }
    }
}

private fun emptyCategoryMessage(
    category: SeriesCategory
): String {
    return when (category) {
        SeriesCategory.CONTINUE_WATCHING ->
            "Nenhuma série em andamento."

        SeriesCategory.FAVORITES ->
            "Nenhuma série favorita."

        SeriesCategory.RECENTLY_ADDED ->
            "Nenhuma série adicionada recentemente."

        else ->
            "Nenhuma série nesta categoria."
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
