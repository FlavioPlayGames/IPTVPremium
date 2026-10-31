package br.com.flaviotorres.iptvpremium.ui.kids

import br.com.flaviotorres.iptvpremium.ui.components.*

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import br.com.flaviotorres.iptvpremium.data.cache.PosterKind
import br.com.flaviotorres.iptvpremium.data.cache.PosterPage
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader
import br.com.flaviotorres.iptvpremium.ui.movies.CategoryPosterPrefetch
import br.com.flaviotorres.iptvpremium.ui.movies.movieHoldDurationMs
import br.com.flaviotorres.iptvpremium.data.cache.loadWhilePosterVisible
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeArtworkEpoch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

internal data class KidsPoster(val id: String, val name: String, val url: String?,
    val categoryId: String? = null, val rating: Double? = null, val progress: Float? = null)
private data class KidsMenuRow(val key: String, val label: String, val count: Int, val categoryId: String? = null)

/** Rebuilt Kids catalogue: one focus/paging path for both movies and series. */
@Composable
internal fun <T : Any> KidsMediaCatalog(
    kind: PosterKind, title: String, paging: LazyPagingItems<T>?, item: (T) -> KidsPoster,
    categories: List<Category>, totalCount: Int, categoryCounts: Map<String?, Int>,
    favorites: Set<String>, history: List<WatchHistoryEntry>,
    onCategoryAccess: (String?, String, () -> Unit) -> Unit,
    onCategory: (String?) -> Unit, onFavorites: () -> Unit, onContinue: () -> Unit,
    onSearch: (String) -> Unit, onSort: (String) -> Unit, onOpen: (String) -> Unit,
    onFavorite: (String) -> Unit, onRemoveContinue: ((String) -> Unit)? = null,
    onFocused: (String) -> Unit = {}, onBlurred: (String) -> Unit = {},
    onInteraction: (Boolean) -> Unit,
    posterPage: (suspend (Int) -> PosterPage)?, resolveCategory: (suspend (String) -> String?)?, onExit: () -> Unit
) {
    var menu by rememberSaveable(kind) { mutableStateOf("all") }
    var sourceId by rememberSaveable(kind) { mutableStateOf<String?>(null) }
    var query by rememberSaveable(kind) { mutableStateOf("") }
    var sort by rememberSaveable(kind) { mutableStateOf("DEFAULT") }
    var lastOpened by rememberSaveable(kind) { mutableStateOf<String?>(null) }
    var restorePoster by remember { mutableStateOf(lastOpened) }
    var firstPosterPending by remember { mutableStateOf(false) }
    var menuActive by rememberSaveable(kind) { mutableStateOf(lastOpened == null) }
    var keyboard by remember { mutableStateOf(false) }
    var cursor by remember { mutableIntStateOf(query.length) }
    var previousPage by remember { mutableStateOf<Any?>(null) }
    var switching by remember { mutableStateOf(false) }
    var lastPagingKey by rememberSaveable(kind) { mutableStateOf(kidsPagingKey(menu, sourceId, query, sort)) }
    val grid = rememberLazyGridState()
    val menuList = rememberLazyListState()
    val menuHost = remember { FocusRequester() }
    val selectedMenuFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val sortFocus = remember { FocusRequester() }
    val inputMode = LocalInputModeManager.current
    val scope = rememberCoroutineScope()
    val activity = remember { MutableStateFlow(0L) }
    val readItem by rememberUpdatedState(item)
    val readPaging by rememberUpdatedState(paging)
    val columns = br.com.flaviotorres.iptvpremium.ui.settings.rememberSettingsPreferences().catalogColumns()
    val series = kind == PosterKind.SERIES
    val continueItems = remember(history, series) { kidsContinuePosters(history, series) }
    val progress = remember(history, series) {
        if (series) emptyMap() else history.filter { it.contentType == PlaybackContentType.MOVIE }
            .sortedBy { it.updatedAtMs }.associate { it.contentId to it.fraction }
    }
    val visibleHistory = remember(continueItems, query, sort) {
        val filtered = continueItems.filter { query.isBlank() || it.name.contains(query.trim(), true) }
        when (sort) { "A_Z" -> filtered.sortedBy { it.name.lowercase() }; "Z_A" -> filtered.sortedByDescending { it.name.lowercase() }; else -> filtered }
    }
    val menuRows = remember(categories, categoryCounts, totalCount, favorites, continueItems.size) {
        listOf(KidsMenuRow("all", "Todos", totalCount),
            KidsMenuRow("continue", "Continuar assistindo", continueItems.size),
            KidsMenuRow("favorites", "Favoritos", favorites.size)) +
            categories.filter { (categoryCounts[it.id] ?: 0) > 0 }.map { KidsMenuRow("source:${it.id}", cleanKidsCatalogName(it.name), categoryCounts[it.id] ?: 0, it.id) }
    }
    val selectedMenuIndex = menuRows.indexOfFirst { it.key == menu }.coerceAtLeast(0)
    val currentMenuIndex by rememberUpdatedState(selectedMenuIndex)
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val count by rememberUpdatedState(if (menu == "continue") visibleHistory.size else paging?.itemCount ?: 0)
    val currentMenu by rememberUpdatedState(menu)
    val manual by rememberUpdatedState<() -> Unit>({
        firstPosterPending = false; restorePoster = null; activity.value++
    })
    val navigator = remember(grid, columns) {
        CatalogGridNavigator(grid, scope, columns, { count },
            { index -> if (currentMenu != "continue") { readPaging?.let { requestCatalogPageItem(it, index) }; Unit } },
            { manual() }, onTop = { sortFocus.requestFocus() },
            onLeft = { menuActive = true; scope.launch { restoreCatalogCategoryFocus(menuList, currentMenuIndex, selectedMenuFocus, menuHost, inputMode) } })
    }
    val filter = "$menu|${sourceId.orEmpty()}|$query|$sort"
    DisposableEffect(navigator) { onDispose { navigator.cancel() } }
    LaunchedEffect(filter) { navigator.filterChanged(filter) }
    LaunchedEffect(keyboard) { if (keyboard) navigator.cancel() }
    // Existing prefetch implementation and limits remain unchanged.
    CategoryPosterPrefetch(kind, filter, grid, paging, { readItem(it).url },
        keyboard || switching || menu == "continue", posterPage, navigationActivity = activity)
    LaunchedEffect(paging) {
        when (menu) { "favorites" -> onFavorites(); "continue" -> { onCategory(null); onContinue() }; else -> onCategory(sourceId) }
        onSearch(query); onSort(sort)
    }
    LaunchedEffect(switching, paging) {
        if (!switching || menu == "continue" || paging == null) { switching = false; return@LaunchedEffect }
        snapshotFlow { paging.itemSnapshotList to paging.loadState.refresh }.first { (page, state) ->
            state is LoadState.Error || (page !== previousPage && state !is LoadState.Loading)
        }
        switching = false; previousPage = null
    }
    LaunchedEffect(grid) {
        combine(snapshotFlow { keyboard || grid.isScrollInProgress }, activity) { busy, _ -> busy }
            .collectLatest { busy -> onInteraction(true); if (!busy) { delay(180); onInteraction(false) } }
    }
    DisposableEffect(Unit) { onDispose { onInteraction(false) } }
    LaunchedEffect(Unit) {
        if (lastOpened == null) restoreCatalogCategoryFocus(menuList, selectedMenuIndex, selectedMenuFocus, menuHost, inputMode)
    }
    fun beginFilterChange() {
        navigator.cancel(); restorePoster = null; lastOpened = null
        val nextKey = kidsPagingKey(menu, sourceId, query, sort)
        val queryChanged = nextKey != lastPagingKey
        previousPage = if (queryChanged) paging?.itemSnapshotList else null
        switching = menu != "continue" && paging != null && queryChanged
        lastPagingKey = nextKey
        firstPosterPending = true; menuActive = false
        scope.launch { grid.scrollToItem(0) }
    }
    fun choose(row: KidsMenuRow) {
        val apply: () -> Unit = {
            if (menu != row.key) {
                menu = row.key; sourceId = row.categoryId; beginFilterChange()
                when (menu) { "favorites" -> onFavorites(); "continue" -> { onCategory(null); onContinue() }; else -> onCategory(sourceId) }
            } else { navigator.reset(); restorePoster = null; menuActive = false; firstPosterPending = true; scope.launch { grid.scrollToItem(0) } }
        }
        if (row.key.startsWith("source:")) onCategoryAccess(row.categoryId, row.label, apply) else apply()
    }
    fun back(): Boolean {
        when (catalogBackAction(keyboard, menuActive)) {
            CatalogBackAction.CLOSE_KEYBOARD -> {
                firstPosterPending = false; keyboard = false; searchFocus.requestFocus()
            }
            CatalogBackAction.EXIT -> onExit()
            CatalogBackAction.CATEGORIES -> {
                navigator.cancel(); manual(); menuActive = true
                scope.launch { restoreCatalogCategoryFocus(menuList, selectedMenuIndex, selectedMenuFocus, menuHost, inputMode) }
            }
        }
        return true
    }
    BackHandler { back() }
    BoxWithConstraints(Modifier.fillMaxSize().catalogEscapeBack(::back).background(KidsControlLook.background)) {
        KidsCinemaBackdrop(modifier=Modifier.matchParentSize(),catalog=true)
        val compact = maxHeight < 650.dp || maxWidth < 1200.dp
        val horizontal = if (compact) 10.dp else 16.dp
        val top = if (compact) 10.dp else 16.dp
        val sidebarWidth = maxWidth * .225f
        Box(Modifier.offset(x = horizontal + sidebarWidth).width(1.dp).fillMaxHeight().background(KidsControlLook.divider))
        Row(Modifier.fillMaxSize().padding(start = horizontal, end = horizontal, top = top, bottom = if (compact) 4.dp else 6.dp)) {
            Column(Modifier.width(sidebarWidth).fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 10.dp)) {
                Box(Modifier.fillMaxWidth().height(if (compact) 64.dp else 80.dp), contentAlignment = Alignment.Center) {
                    KidsCinemaBrand(Modifier.fillMaxSize())
                }
                Text("CATEGORIAS", Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), textAlign = TextAlign.Center,
                    color = KidsCatalogLook.secondary, fontSize = if (compact) 10.sp else 12.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(state = menuList, modifier = Modifier.fillMaxWidth().weight(1f)
                    .focusRequester(menuHost).focusable(), verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(vertical = 3.dp)) {
                    itemsIndexed(menuRows, key = { _, row -> row.key }) { index, row ->
                        KidsCatalogAction(row.label, if (compact) 38 else 44,
                            Modifier.fillMaxWidth().then(if (index == selectedMenuIndex) Modifier.focusRequester(selectedMenuFocus) else Modifier)
                                .onPreviewKeyEvent {event ->
                                    if(index==0 && event.key==Key.DirectionUp) {
                                        if(event.type==KeyEventType.KeyDown)sortFocus.requestFocus()
                                        true
                                    } else false
                                },
                            active = row.key == menu, trailing = row.count.toString(), compact = compact, category = true,
                            icon = when(row.key) {
                                "all" -> KidsCatalogIcon.ALL
                                "continue" -> KidsCatalogIcon.CONTINUE
                                "favorites" -> KidsCatalogIcon.FAVORITE
                                else -> KidsCatalogIcon.CATEGORY
                            },
                            onFocus = { navigator.cancel(); manual(); menuActive = true }, onClick = { choose(row) })
                    }
                }
            }
            Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().height(if (compact) 44.dp else 54.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.DirectionDown) {
                            if (event.type == KeyEventType.KeyDown && count > 0 && !switching) { menuActive = false; navigator.enter() }
                            true
                        } else false
                    }, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp)) {
                    KidsCatalogAction(if (sort == "DEFAULT") "Organizar" else if (sort == "A_Z") "A – Z" else "Z – A",
                        if (compact) 38 else 46, Modifier.width(if (compact) 132.dp else 160.dp).focusRequester(sortFocus)
                            .onPreviewKeyEvent { event ->
                                if (!keyboard && event.key == Key.DirectionLeft) {
                                    if (event.type == KeyEventType.KeyDown) {
                                        navigator.cancel(); manual(); menuActive = true
                                        scope.launch { restoreCatalogCategoryFocus(menuList, selectedMenuIndex, selectedMenuFocus, menuHost, inputMode) }
                                    }
                                    true
                                } else false
                            }, compact = compact, icon = KidsCatalogIcon.SORT,
                        onFocus = { navigator.cancel(); manual(); menuActive = false }) {
                        sort = when(sort) { "DEFAULT" -> "A_Z"; "A_Z" -> "Z_A"; else -> "DEFAULT" }
                        beginFilterChange(); onSort(sort)
                    }
                    KidsCatalogAction(if (query.isBlank()) "Pesquisar ${title.lowercase()}…" else query,
                        if (compact) 38 else 46, Modifier.weight(1f).focusRequester(searchFocus)
                            .onPreviewKeyEvent { event ->
                                if (!keyboard && event.key == Key.DirectionLeft) {
                                    if (event.type == KeyEventType.KeyDown) {
                                        sortFocus.requestFocus()
                                    }
                                    true
                                } else false
                            },
                        compact = compact, icon = KidsCatalogIcon.SEARCH,
                        onFocus = { navigator.cancel(); manual(); menuActive = false }) { keyboard = true }
                    KidsMinuteClock(compact)
                }
                Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
                CatalogTwoRowFrame(compactLayout = compact, columns = columns, framed = false) { aspect ->
                    Box(Modifier.fillMaxSize()) {
                    val load = paging?.loadState?.refresh
                    val historyMode = menu == "continue"
                    // LazyGrid can retain this provider after Paging clears/replaces its data.
                    val renderedPage = paging?.itemSnapshotList
                    val renderedHistory = visibleHistory
                    val renderedCount = if (historyMode) renderedHistory.size else renderedPage?.size ?: 0
                    val loading = !historyMode && (switching || (count == 0 && load is LoadState.Loading))
                    when {
                        loading -> KidsMessage("Carregando ${title.lowercase()}…")
                        !historyMode && load is LoadState.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Não foi possível carregar o catálogo.", color = KidsCatalogLook.text)
                            Spacer(Modifier.height(12.dp))
                            KidsCatalogAction("Tentar novamente", 42, compact = compact) { paging?.retry() }
                        }
                        count == 0 -> KidsMessage(if (query.isNotBlank()) "Nenhum resultado para a pesquisa." else "Nenhum conteúdo nesta categoria.")
                        else -> LazyVerticalGrid(columns = GridCells.Fixed(columns), state = grid,
                            modifier = Modifier.fillMaxSize().clipToBounds().catalogGridHost(navigator),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
                            verticalArrangement = Arrangement.spacedBy(LocalCatalogRowSpacing.current)) {
                            items(count = renderedCount, key = { index ->
                                if (historyMode) catalogSnapshotKey(renderedHistory, index, "history:") { it.id }
                                else catalogSnapshotKey(renderedPage, index, "pending:") { readItem(it).id }
                            }, contentType = { "poster" }) { index ->
                                if (!historyMode) paging?.let { requestCatalogPageItem(it, index, renderedPage) }
                                val entry = if (historyMode) renderedHistory.getOrNull(index) else renderedPage?.getOrNull(index)?.let(readItem)
                                if (entry == null) KidsPosterPlaceholder(aspect) else {
                                    val readyFocus = !keyboard && !menuActive && !switching &&
                                        ((firstPosterPending && index == 0) || restorePoster == entry.id)
                                    KidsCatalogCard(entry.copy(progress = entry.progress ?: progress[entry.id]),
                                        kind, navigator, index, compact, aspect, entry.id in favorites,
                                        readyFocus, categoryName = categoryNames[entry.categoryId].orEmpty(),
                                        resolveCategory = resolveCategory,
                                        onFocus = {
                                            menuActive = false; firstPosterPending = false; restorePoster = null
                                            activity.value++; onFocused(entry.id)
                                        }, onBlur = { onBlurred(entry.id) },
                                        onOpen = { lastOpened = entry.id; menuActive = false; onOpen(entry.id) },
                                        onFavorite = { hadFocus ->
                                            if (hadFocus) {
                                                val ids = if (historyMode) visibleHistory.map { it.id }
                                                    else paging?.itemSnapshotList?.items?.map { readItem(it).id }.orEmpty()
                                                restorePoster = kidsFocusAfterAction(ids, entry.id, menu == "favorites" && entry.id in favorites)
                                                if (restorePoster == null) {
                                                    menuActive = true
                                                    scope.launch { restoreCatalogCategoryFocus(menuList, selectedMenuIndex, selectedMenuFocus, menuHost, inputMode) }
                                                }
                                            }
                                            onFavorite(entry.id)
                                        },
                                        onRemove = if (historyMode && onRemoveContinue != null) ({
                                            restorePoster = kidsFocusAfterAction(visibleHistory.map { it.id }, entry.id, true)
                                            if (restorePoster == null) {
                                                menuActive = true
                                                scope.launch { restoreCatalogCategoryFocus(menuList, selectedMenuIndex, selectedMenuFocus, menuHost, inputMode) }
                                            }
                                            onRemoveContinue(entry.id)
                                        }) else null)
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
        if (keyboard) Box(Modifier.fillMaxSize().padding(horizontal = horizontal, vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
            AppKeyboard(Modifier.fillMaxWidth(), actionText = "CONCLUIR",
                onCharacter = { value -> val edited = tvKeyboardType(query, cursor, value)
                    cursor = edited.cursor
                    if (query != edited.value) { query = edited.value; beginFilterChange(); onSearch(query) } },
                onBackspace = { val edited = tvKeyboardDelete(query, cursor)
                    cursor = edited.cursor
                    if (query != edited.value) { query = edited.value; beginFilterChange(); onSearch(query) } },
                onMoveCursorLeft = { cursor = tvKeyboardMoveCursor(query, cursor, -1) },
                onMoveCursorRight = { cursor = tvKeyboardMoveCursor(query, cursor, 1) },
                onAction = { firstPosterPending = false; keyboard = false; searchFocus.requestFocus() })
        }
    }
}

private fun cleanKidsCatalogName(name: String): String = name.trim().replace(Regex("^(filmes|s[eé]ries)\\s*\\|\\s*", RegexOption.IGNORE_CASE), "").trim()

@Composable
private fun BoxScope.KidsMessage(message: String) {
    Text(message, Modifier.align(Alignment.Center), color = KidsCatalogLook.secondary, fontSize = 14.sp)
}

/** Minute updates are isolated from the catalogue composition. */
@Composable
internal fun KidsMinuteClock(compact: Boolean) {
    val format = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var now by remember { mutableStateOf(format.format(Date())) }
    LaunchedEffect(Unit) { while (isActive) { delay(60_000L - System.currentTimeMillis() % 60_000L); now = format.format(Date()) } }
    Text(now, Modifier.padding(horizontal = 6.dp), color = KidsCatalogLook.text,
        fontSize = if (compact) 11.sp else 14.sp, fontWeight = FontWeight.Normal)
}

@Composable
private fun KidsCatalogAction(label: String, height: Int, modifier: Modifier = Modifier,
    active: Boolean = false, trailing: String? = null, compact: Boolean = true, category: Boolean = false,
    icon: KidsCatalogIcon? = null,
    onFocus: () -> Unit = {}, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val toolbar = icon == KidsCatalogIcon.SORT || icon == KidsCatalogIcon.SEARCH
    val shape = if (toolbar) RoundedCornerShape(50) else RoundedCornerShape(10.dp)
    Row(modifier.height(height.dp)
        .kidsNewSurface(focused, active, shape,
            translucent = icon == KidsCatalogIcon.SORT || icon == KidsCatalogIcon.SEARCH, category = category)
        .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
        .kidsNewClick(onClick).padding(horizontal = if (compact) 10.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) KidsSmallIcon(icon, compact, focused || active)
        else Spacer(Modifier.size(if (compact) 15.dp else 18.dp))
        Text(label, Modifier.weight(1f), color = KidsControlLook.foreground(focused || active),
            fontSize = if (compact) 11.sp else 14.sp, fontWeight = if (focused || active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) Text(trailing, Modifier.width(if (compact) 36.dp else 44.dp),
            color = if (focused || active) Color.White else KidsCatalogLook.secondary, textAlign = TextAlign.End,
            fontSize = if (compact) 10.sp else 12.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KidsCatalogCard(entry: KidsPoster, kind: PosterKind, navigator: CatalogGridNavigator,
    index: Int, compact: Boolean, aspect: Float, favorite: Boolean, restore: Boolean,
    categoryName: String, resolveCategory: (suspend (String) -> String?)?,
    onFocus: () -> Unit, onBlur: () -> Unit, onOpen: () -> Unit, onFavorite: (Boolean) -> Unit, onRemove: (() -> Unit)?) {
    val context = LocalContext.current.applicationContext
    val view = LocalView.current
    val requester = remember { FocusRequester() }
    BindCatalogFocus(navigator, index, requester)
    var focused by remember { mutableStateOf(false) }
    // Other cards are not recomposed when the moving index changes elsewhere.
    val movingFocus by remember(navigator, index) { derivedStateOf { navigator.isMovingSelection(index) } }
    val selected = focused || movingFocus
    val scale = animateFloatAsState(if (selected) 1.03f else 1f, tween(140), label = "CatalogueFocus")
    val open by rememberUpdatedState(onOpen)
    val favoriteAction by rememberUpdatedState(onFavorite)
    val remove by rememberUpdatedState(onRemove)
    val focusAction by rememberUpdatedState(onFocus)
    val blurAction by rememberUpdatedState(onBlur)
    var held by remember(entry.id) { mutableStateOf(false) }
    var started by remember(entry.id) { mutableLongStateOf(0L) }
    var triggered by remember(entry.id) { mutableStateOf(false) }
    val threshold = movieHoldDurationMs(onRemove != null, 1_000L)
    fun longAction() { remove?.invoke() ?: favoriteAction(focused) }
    DisposableEffect(view, held) {
        if (!held) return@DisposableEffect onDispose { }
        val observer = view.viewTreeObserver
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus -> if (!hasFocus) held = false }
        observer.addOnWindowFocusChangeListener(listener)
        if (!view.hasWindowFocus()) held = false
        onDispose { if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener) }
    }
    LaunchedEffect(held, entry.id, threshold) {
        if (held) { delay(threshold); if (held && focused && view.hasWindowFocus() && !triggered) { triggered = true; longAction() } }
    }
    LaunchedEffect(restore) {
        if (restore) repeat(3) { withFrameNanos { }; if (!focused) runCatching { requester.requestFocus() } }
    }
    var visible by remember { mutableStateOf(false) }
    val epoch by ThemeArtworkEpoch.collectAsState()
    val initial = remember(entry.id, entry.url, epoch) {
        val bitmap = MoviePosterLoader.peekBitmap(entry.url)
        PosterArtworkState(bitmap, bitmap != null || MoviePosterLoader.knownMissing(entry.url))
    }
    // Recreate the state on identity/URL/cache epoch changes: never flash an old bitmap.
    val artwork by key(entry.id, entry.url, epoch) {
        produceState(initial) {
            value = PosterArtworkState(loadWhilePosterVisible(snapshotFlow { visible }) {
                MoviePosterLoader.loadWithRetry(context, entry.url, kind)
            }, true)
        }
    }
    val width = LocalCatalogPosterWidth.current
    val shape = RoundedCornerShape(CATALOG_POSTER_CORNER_DP.dp)
    val rating = remember(entry.rating) { String.format(Locale.US, "Nota: %.1f", entry.rating?.takeIf { it.isFinite() && it > 0 } ?: 0.0) }
    Column(Modifier.wrapContentWidth(Alignment.CenterHorizontally).width(width).height(LocalCatalogCardHeight.current)
        .onGloballyPositioned { val b = it.boundsInWindow(); visible = b.width > 0 && b.height > 0 }
        .onFocusChanged {
            if (it.isFocused && !focused) { navigator.focused(index); focusAction() }
            if (!it.isFocused && focused) { held = false; blurAction() }
            focused = it.isFocused
        }.focusRequester(requester).catalogGridNavigation(navigator, index, requester)
        .onPreviewKeyEvent { event ->
            val ok = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
            if (!ok) false else {
                when(event.type) {
                    KeyEventType.KeyDown -> if (!held && event.nativeKeyEvent.repeatCount == 0) {
                        started = SystemClock.elapsedRealtime(); triggered = false; held = true
                    }
                    KeyEventType.KeyUp -> {
                        if (held && !event.nativeKeyEvent.isCanceled && view.hasWindowFocus()) {
                            if (!triggered) { if (SystemClock.elapsedRealtime() - started >= threshold) longAction() else open() }
                        }
                        held = false
                    }
                }; true
            }
        }.pointerInput(entry.id, threshold) {
            var longTriggered = false
            detectTapGestures(onPress = {
                longTriggered = false
                if (withTimeoutOrNull(threshold) { tryAwaitRelease() } == null) {
                    longTriggered = true; longAction(); tryAwaitRelease()
                }
            }, onTap = { if (!longTriggered) open() })
        }.semantics { role = Role.Button; onClick("Abrir detalhes") { open(); true } }.focusable()) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clip(shape).background(KidsCatalogLook.categorySurface)) {
            if (artwork.bitmap != null) Image(artwork.bitmap!!, entry.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else if (artwork.finished) GenericPosterArtwork(if (kind == PosterKind.SERIES) "series" else "movies", entry.id,
                entry.name, categoryName, resolveCategory, Modifier.fillMaxSize())
            val fraction = entry.progress?.coerceIn(0f, 1f) ?: 0f
            if (fraction > 0f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x99000000))) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(KidsCatalogLook.accent))
            }
            Box(Modifier.matchParentSize().border(3.dp,
                if (selected) KidsControlLook.metalBorder else SolidColor(Color.Transparent), shape))
        }
        Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
        Text(entry.name, modifier = Modifier.fillMaxWidth().clipToBounds().then(
            if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 800, repeatDelayMillis = 1000) else Modifier),
            color = KidsCatalogLook.text, fontSize = if (compact) 11.sp else 14.sp, lineHeight = if (compact) 14.sp else 18.sp,
            maxLines = 1, overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis)
        Spacer(Modifier.height(if (compact) 2.dp else 3.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(rating, Modifier.wrapContentWidth(), color = KidsCatalogLook.secondary,
                fontSize = if (compact) 9.sp else 11.sp, lineHeight = if (compact) 12.sp else 16.sp, maxLines = 1)
            KidsFavoriteHeart(favorite, compact) { favoriteAction(false) }
        }
    }
}

/** Keep focus local when a favourite/history action removes its current item. */
internal fun kidsFocusAfterAction(ids: List<String>, current: String, removed: Boolean): String? {
    if (!removed) return current
    val index = ids.indexOf(current)
    if (index < 0) return ids.firstOrNull()
    return ids.getOrNull(index + 1) ?: ids.getOrNull(index - 1)
}

internal fun kidsContinuePosters(history: List<WatchHistoryEntry>, series: Boolean): List<KidsPoster> {
        val sorted = history.sortedByDescending { it.updatedAtMs }
        return if (series) sorted.filter { it.contentType == PlaybackContentType.EPISODE && it.inProgress && !it.seriesId.isNullOrBlank() }
            .distinctBy { it.seriesId }.map { KidsPoster(it.seriesId!!, it.seriesTitle?.takeIf { title -> title.isNotBlank() } ?: it.title, it.seriesPosterUrl?.takeIf { url -> url.isNotBlank() }) }
        else sorted.filter { it.contentType == PlaybackContentType.MOVIE && it.inProgress }
            .map { KidsPoster(it.contentId, it.title, it.posterUrl, progress = it.fraction) }
}

/** History is rendered locally while the retained Pager is kept on All. */
internal fun kidsPagingKey(menu: String, category: String?, query: String, sort: String): String =
    "${if (menu == "favorites") "favorites" else if (menu.startsWith("source:")) "source:${category.orEmpty()}" else "all"}|${query.trim()}|$sort"

internal enum class KidsCatalogIcon { ALL, CONTINUE, FAVORITE, CATEGORY, SORT, SEARCH, STAR }

/** Small vector marks: no bitmap decoding, animated shadows or blur layers. */
@Composable
internal fun KidsSmallIcon(icon: KidsCatalogIcon, compact: Boolean, selected: Boolean = false) {
    val color = if (icon == KidsCatalogIcon.STAR) KidsCatalogLook.accent else KidsControlLook.foreground(selected)
    val heart = remember {
        Path().apply {
            moveTo(.5f, .88f)
            cubicTo(.38f, .77f, .08f, .55f, .08f, .31f)
            cubicTo(.08f, .08f, .37f, .02f, .5f, .23f)
            cubicTo(.63f, .02f, .92f, .08f, .92f, .31f)
            cubicTo(.92f, .55f, .62f, .77f, .5f, .88f)
            close()
        }
    }
    val play = remember { Path().apply { moveTo(.22f,.12f); lineTo(.87f,.5f); lineTo(.22f,.88f); close() } }
    Canvas(Modifier.size(if (compact) 15.dp else 18.dp)) {
        scale(size.width, size.height, pivot = Offset.Zero) {
            val stroke = Stroke(.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when(icon) {
                KidsCatalogIcon.ALL -> for (x in listOf(.10f,.57f)) for (y in listOf(.10f,.57f)) {
                    drawRoundRect(color, Offset(x,y), Size(.32f,.32f), CornerRadius(.04f), style = stroke)
                }
                KidsCatalogIcon.CONTINUE -> {
                    drawPath(play, color, style = stroke)
                }
                KidsCatalogIcon.FAVORITE -> drawPath(heart, color, style = stroke)
                KidsCatalogIcon.SEARCH -> {
                    drawCircle(color, .29f, Offset(.39f,.39f), style = stroke)
                    drawLine(color, Offset(.61f,.61f), Offset(.90f,.90f), .08f, StrokeCap.Round)
                }
                KidsCatalogIcon.SORT -> {
                    drawLine(color, Offset(.10f,.25f), Offset(.90f,.25f), .08f, StrokeCap.Round)
                    drawLine(color, Offset(.10f,.50f), Offset(.68f,.50f), .08f, StrokeCap.Round)
                    drawLine(color, Offset(.10f,.75f), Offset(.45f,.75f), .08f, StrokeCap.Round)
                }
                KidsCatalogIcon.CATEGORY -> {
                    drawRoundRect(color, Offset(.12f,.41f), Size(.76f,.47f), CornerRadius(.04f), style = stroke)
                    val lid = Path().apply { moveTo(.12f,.36f); lineTo(.85f,.13f); lineTo(.90f,.29f); lineTo(.17f,.52f); close() }
                    drawPath(lid, color, style = stroke)
                    for (x in listOf(.32f,.57f)) drawLine(color, Offset(x,.30f), Offset(x+.11f,.39f), .055f)
                }
                KidsCatalogIcon.STAR -> {
                    val star = Path()
                    for (i in 0 until 10) {
                        val angle = -Math.PI / 2 + i * Math.PI / 5
                        val radius = if (i % 2 == 0) .45f else .20f
                        val x = .5f + kotlin.math.cos(angle).toFloat() * radius
                        val y = .5f + kotlin.math.sin(angle).toFloat() * radius
                        if (i == 0) star.moveTo(x,y) else star.lineTo(x,y)
                    }
                    star.close(); drawPath(star, color)
                }
            }
        }
    }
}

@Composable
private fun KidsPosterPlaceholder(aspect:Float) {
    Box(Modifier.width(LocalCatalogPosterWidth.current).height(LocalCatalogCardHeight.current)) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect)
            .background(KidsCatalogLook.categorySurface,RoundedCornerShape(CATALOG_POSTER_CORNER_DP.dp)))
    }
}
@Composable
private fun KidsFavoriteHeart(favorite:Boolean,compact:Boolean,onClick:()->Unit) {
    Box(Modifier.size(if(compact)24.dp else 28.dp).focusProperties {canFocus=false}.kidsNewClick(onClick),contentAlignment=Alignment.Center) {
        Text(if(favorite)"♥"else"♡",color=if(favorite)KidsCatalogLook.accent else KidsCatalogLook.secondary,fontSize=if(compact)16.sp else 20.sp)
    }
}
