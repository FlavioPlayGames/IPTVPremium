package br.com.flaviotorres.iptvpremium.ui.live

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import br.com.flaviotorres.iptvpremium.core.player.PlaybackVideoLayout
import br.com.flaviotorres.iptvpremium.core.player.PlaybackAspectMode


import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import br.com.flaviotorres.iptvpremium.ui.movies.movieHoldReady
import kotlinx.coroutines.withTimeoutOrNull
import br.com.flaviotorres.iptvpremium.ui.components.reinforcedCatalogFocus
import br.com.flaviotorres.iptvpremium.ui.kids.*
import android.os.SystemClock
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import br.com.flaviotorres.iptvpremium.ui.components.catalogEscapeBack
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.core.player.PlaybackEpgProgram
import br.com.flaviotorres.iptvpremium.domain.model.Category
import br.com.flaviotorres.iptvpremium.domain.model.CategoryType
import br.com.flaviotorres.iptvpremium.domain.model.Channel
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent
import br.com.flaviotorres.iptvpremium.ui.components.AppKeyboard
import br.com.flaviotorres.iptvpremium.ui.components.TvKeyboardSearchText
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardType
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardDelete
import br.com.flaviotorres.iptvpremium.ui.components.tvKeyboardMoveCursor
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val FAVORITE_LONG_PRESS_MS = 1_000L

private val CinzelSemiBold = FontFamily(
    Font(
        resId = R.font.cinzel_semibold,
        weight = FontWeight.SemiBold
    )
)

private val MetallicGoldBrush get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF926315))
)

private val AppGold get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFD4AF37)
private val AppGoldLight get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFE991)

private val PremiumGoldBorder get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF926315))
)

private val NeutralLiveBorder get()=Brush.linearGradient(listOf(
    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.edge,
    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.edge))

private val PremiumPanelBackground get() = if(KidsAppearance.active) kidsCinemaPanelBrush() else br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.panelBrush

private val PremiumProgressGold get() = Brush.horizontalGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFC58B17), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFF8D863), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFEEAC))
)

// Brilho sutil aplicado apenas a itens destacados; não afeta o Media3.
private val channelCategoryPrefix = Regex("^Canais\\s*\\|\\s*", RegexOption.IGNORE_CASE)

private fun Modifier.premiumGlow(enabled: Boolean, roundedDp: Float = 12f): Modifier = this


private fun Modifier.premiumCategoryCardBorder(
    highlighted: Boolean,
    roundedDp: Float = 12f
): Modifier = if(KidsAppearance.active) this else if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) {
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

}

@Composable
private fun PremiumOrnamentDivider(
    modifier: Modifier = Modifier,
    compactLayout: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD4AF37))
                    )
                )
        )
        Text(
            text = "◇✦◇",
            color = AppGold,
            fontSize = if (compactLayout) 9.sp else 11.sp,
            modifier = Modifier.padding(horizontal = if (compactLayout) 5.dp else 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFD4AF37), Color.Transparent)
                    )
                )
        )
    }
}


internal enum class ChannelCategoryKind {
    ALL,
    RECENT,
    FAVORITES,
    SOURCE
}

internal data class ChannelCategory(
    val id: String,
    val title: String,
    val kind: ChannelCategoryKind,
    val sourceCategoryId: String? = null
)

private val AllChannelsCategory =
    ChannelCategory(
        id = "__all__",
        title = "Todos",
        kind = ChannelCategoryKind.ALL
    )

private val RecentChannelsCategory =
    ChannelCategory(
        id = "__recent__",
        title = "Continuar assistindo",
        kind = ChannelCategoryKind.RECENT
    )

private val FavoriteChannelsCategory =
    ChannelCategory(
        id = "__favorites__",
        title = "Favoritos",
        kind = ChannelCategoryKind.FAVORITES
    )

private enum class LeftPaneMode {
    CHANNELS,
    CATEGORIES
}

private enum class ChannelSortMode {
    DEFAULT,
    A_Z,
    Z_A
}

internal data class PreviewChannel(
    val id: String,
    val number: Int,
    val name: String,
    val categoryId: String?,
    val logoUrl: String? = null,
    val streamUrl: String = ""
)

private data class GuideProgram(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val progressFraction: Float = 0f,
    val isLive: Boolean = false
)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LiveTvScreen(
    catalogLoading: Boolean = false,
    catalogError: String? = null,
    onRetryCatalog: () -> Unit = {},
    onBrowseChanged: (String, Int, Set<String>?) -> Unit = { _, _, _ -> },
    playlistContent: PlaylistContent? = null,
    pagedChannels: LazyPagingItems<Channel>? = null,
    pagingCategories: List<Category> = emptyList(),
    totalChannelCount: Int = 0,
    pagingCategoryCounts: Map<String?, Int> = emptyMap(),
    onCategoryAccessRequested: (String?, String, () -> Unit) -> Unit = { _, _, proceed -> proceed() },
    onSourceCategorySelected: (String?) -> Unit = {},
    // Recebe a programação real do provedor, por ID do canal selecionado.
    // A navegação conectará a consulta após a compilação desta tela.
    onLoadChannelEpg: (suspend (String) -> List<PlaybackEpgProgram>)? = null,
    epgSourceKey: String? = null,
    onLoadFavoriteChannels: (suspend (Set<String>) -> List<Channel>)? = null,
    onLoadFirstChannel: (suspend () -> Channel?)? = null,
    onFindChannelAlternatives: ((String)->Flow<LiveStreamCandidate>)? = null,
    onLoadChannelNeighbours: (suspend (String,Int,String?,Boolean,Set<String>?)->List<Channel>)? = null,
    onExit: () -> Unit
) {
    /*
     * Compatibilidade temporária:
     *
     * playlistContent continua aceito enquanto o AppNavigation antigo
     * ainda estiver conectado. Assim conseguimos migrar a tela sem
     * quebrar o restante do projeto.
     *
     * Quando pagedChannels estiver presente, o catálogo exibido vem do
     * Room + Paging e não de uma lista gigante em memória.
     */
    val usingPaging =
        pagedChannels != null

    val legacyPreviewChannels =
        remember(playlistContent) {
            playlistContent
                ?.channels
                .orEmpty()
                .mapIndexed { index, channel ->
                    PreviewChannel(
                        id = channel.id,
                        number = index + 1,
                        name = channel.name,
                        categoryId = channel.categoryId,
                        logoUrl = channel.logoUrl,
                        streamUrl = channel.streamUrl
                    )
                }
        }

    /*
     * Snapshot somente dos itens que o Paging já trouxe para a memória.
     * Ele é usado para preview, favoritos e recursos temporariamente locais.
     *
     * A lista principal NÃO usa esse snapshot: ela acessa LazyPagingItems
     * diretamente para que o Paging carregue novas páginas conforme o usuário
     * navega.
     */
    // Ler o snapshot uma vez. Ele muda quando o Paging recebe uma nova
    // página, não quando o foco, o relógio ou outro estado visual recompõe.
    // Assim evitamos mapIndexed() em todas as recomposições da Android TV.
    val pagedSnapshot = pagedChannels?.itemSnapshotList

    val loadedPagedChannels = remember(pagedSnapshot) {
        pagedSnapshot
            ?.items
            ?.mapIndexed { index, channel ->
                PreviewChannel(
                    id = channel.id,
                    number = index + 1,
                    name = channel.name,
                    categoryId = channel.categoryId,
                    logoUrl = channel.logoUrl,
                    streamUrl = channel.streamUrl
                )
            }
            ?: emptyList()
    }

    val parentalAccess by br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.accessFlow.collectAsState(
        initial = br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access())
    val categoryNamesForProtection = remember(pagingCategories, playlistContent) {
        (pagingCategories + playlistContent?.categories.orEmpty()).associate { it.id to it.name }
    }
    val previewChannels = remember(loadedPagedChannels, legacyPreviewChannels, usingPaging, parentalAccess, categoryNamesForProtection, epgSourceKey) {
        (if (usingPaging) loadedPagedChannels else legacyPreviewChannels).filterNot {
            parentalAccess.blocked(epgSourceKey.orEmpty(), "LIVE_TV", it.categoryId, it.name, categoryNamesForProtection[it.categoryId].orEmpty())
        }
    }

    val sourceCategories =
        remember(
            playlistContent,
            pagingCategories,
            usingPaging
        ) {
            val source =
                if (usingPaging) {
                    pagingCategories
                } else {
                    playlistContent
                        ?.categories
                        .orEmpty()
                        .filter {
                            it.type == CategoryType.LIVE_TV
                        }
                }

            source
                .distinctBy {
                    it.id
                }
                .sortedWith(br.com.flaviotorres.iptvpremium.ui.kids.CatalogSections.categoryComparator { it.name })
                .map { category ->
                    ChannelCategory(
                        id = "source_${category.id}",
                        title = category.name,
                        kind = ChannelCategoryKind.SOURCE,
                        sourceCategoryId = category.id
                    )
                }
        }

    val channelCategories =
        remember(sourceCategories) {
            listOf(
                AllChannelsCategory,
                FavoriteChannelsCategory,
                RecentChannelsCategory
            ) + sourceCategories
        }

    var selectedCategoryId by remember(epgSourceKey) {
        mutableStateOf(AllChannelsCategory.id)
    }

    val selectedCategory =
        channelCategories.firstOrNull {
            it.id == selectedCategoryId
        } ?: AllChannelsCategory

    var leftPaneMode by remember(epgSourceKey) {
        mutableStateOf(LeftPaneMode.CATEGORIES)
    }

    var categoryHasFocus by remember(epgSourceKey) { mutableStateOf(false) }
    val categoryEntryFocus = remember { FocusRequester() }

    var selectedChannelId by remember(epgSourceKey) {
        mutableStateOf("")
    }

    val favoriteContext = LocalContext.current.applicationContext
    val favoritePreferences = remember(favoriteContext) {
        favoriteContext.getSharedPreferences("iptvpremium_favorites_v1", android.content.Context.MODE_PRIVATE)
    }
    val channelFavoritesKey = remember(epgSourceKey) { "channels_${epgSourceKey.orEmpty()}" }
    var favoriteChannelIds by remember(epgSourceKey) {
        mutableStateOf(if (epgSourceKey.isNullOrBlank()) emptySet<String>() else
            favoritePreferences.getStringSet(channelFavoritesKey, emptySet()).orEmpty().toSet())
    }
    fun toggleChannelFavorite(channelId: String) {
        val updated = if (channelId in favoriteChannelIds) favoriteChannelIds - channelId else favoriteChannelIds + channelId
        favoriteChannelIds = updated
        if (!epgSourceKey.isNullOrBlank()) {
            favoritePreferences.edit().putStringSet(channelFavoritesKey, updated.toHashSet()).apply()
        }
    }

    val historyKey=remember(epgSourceKey){"live_recent_${epgSourceKey.orEmpty()}"}
    var recentChannelIds by remember(epgSourceKey){mutableStateOf(runCatching {
        val json=org.json.JSONArray(favoritePreferences.getString(historyKey,"[]"))
        (0 until json.length()).map{json.getString(it)}
    }.getOrDefault(emptyList()))}
    var pinnedChannel by remember(epgSourceKey){mutableStateOf<PreviewChannel?>(null)}
    var restoringLast by remember(epgSourceKey){mutableStateOf(true)}
    var lastRestored by remember(epgSourceKey){mutableStateOf(false)}
    val lastChannelKey=remember(epgSourceKey){"live_last_confirmed_${epgSourceKey.orEmpty()}"}
    fun rememberConfirmed(id:String) {
        if(!epgSourceKey.isNullOrBlank()) favoritePreferences.edit().putString(lastChannelKey,id).apply()
    }
    val latestFirstLoader by rememberUpdatedState(onLoadFirstChannel)
    val latestSavedLoader by rememberUpdatedState(onLoadFavoriteChannels)
    LaunchedEffect(epgSourceKey, catalogLoading, catalogError, totalChannelCount, pagedChannels?.itemCount) {
        if (catalogLoading || catalogError != null || lastRestored) return@LaunchedEffect
        try {
            val ids=(listOfNotNull(favoritePreferences.getString(lastChannelKey,null))+recentChannelIds).distinct()
            val saved=latestSavedLoader?.invoke(ids.toSet()).orEmpty()
            val restored=ids.firstNotNullOfOrNull { id -> saved.firstOrNull { it.id==id } }
            val last=restored ?: latestFirstLoader?.invoke() ?: if(!usingPaging) previewChannels.firstOrNull()?.let { Channel(it.id,it.name,it.categoryId,it.logoUrl,it.streamUrl,null) } else null
            if(last!=null) {
                pinnedChannel=PreviewChannel(last.id,1,last.name,last.categoryId,last.logoUrl,last.streamUrl)
                selectedChannelId=last.id
                // Categories may arrive one frame after the channel lookup. Keep its real ID.
                selectedCategoryId=if(restored!=null) last.categoryId?.let { "source_$it" } ?: AllChannelsCategory.id
                    else AllChannelsCategory.id
                onSourceCategorySelected(if(restored!=null) last.categoryId else null)
                leftPaneMode=LeftPaneMode.CHANNELS
                lastRestored=true
            }
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){/* A stale/missing history entry must not prevent opening Channels. */}
        finally{restoringLast=false}
    }
    fun rememberWatched(channel:PreviewChannel) {
        if(epgSourceKey.isNullOrBlank())return
        rememberConfirmed(channel.id)
        val updated=(listOf(channel.id)+recentChannelIds.filterNot{it==channel.id}).take(32)
        recentChannelIds=updated
        favoritePreferences.edit().putString(historyKey,org.json.JSONArray(updated).toString()).apply()
    }

    // Resolve only saved IDs from Room when Favorites is opened; no full-channel scan.
    var resolvedFavorites by remember(epgSourceKey) { mutableStateOf<List<PreviewChannel>>(emptyList()) }
    val latestFavoriteLoader by rememberUpdatedState(onLoadFavoriteChannels)
    LaunchedEffect(epgSourceKey, favoriteChannelIds, recentChannelIds, selectedCategory.kind, parentalAccess, KidsAppearance.active) {
        val ids=when(selectedCategory.kind){ChannelCategoryKind.FAVORITES->favoriteChannelIds
            ChannelCategoryKind.RECENT->recentChannelIds.toSet();else->emptySet()}
        if (ids.isNotEmpty()) {
            resolvedFavorites = latestFavoriteLoader?.invoke(ids).orEmpty().mapIndexed { index, channel ->
                PreviewChannel(channel.id, index + 1, channel.name, channel.categoryId, channel.logoUrl, channel.streamUrl)
            }
        } else resolvedFavorites=emptyList()
    }
    val availableChannels = remember(previewChannels, resolvedFavorites) {
        (previewChannels + resolvedFavorites).distinctBy { it.id }
    }

    var sortMode by remember {
        mutableStateOf(ChannelSortMode.DEFAULT)
    }

    var selectedDayIndex by remember {
        mutableStateOf(3)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var searchKeyboardVisible by remember {
        mutableStateOf(false)
    }

    // Posicao da letra destacada, como em Filmes, Series e no cadastro.
    // No fim do texto, exibe a barra dourada piscante.
    var searchCursorPosition by rememberSaveable {
        mutableIntStateOf(0)
    }

    /*
     * Quando estamos dentro de uma categoria, o botão VOLTAR
     * retorna primeiro para a lista de categorias.
     *
     * Somente quando já estamos em CATEGORIAS o sistema deixa
     * o Back seguir para a tela anterior do app.
     */
    BackHandler(
        enabled =
            leftPaneMode == LeftPaneMode.CHANNELS &&
                    !searchKeyboardVisible
    ) {
        searchQuery = ""
        leftPaneMode = LeftPaneMode.CATEGORIES
    }

    BackHandler(enabled = leftPaneMode == LeftPaneMode.CATEGORIES && !searchKeyboardVisible) {
        if (categoryHasFocus) onExit() else categoryEntryFocus.requestFocus()
    }

    BackHandler(
        enabled = searchKeyboardVisible
    ) {
        searchKeyboardVisible = false
    }

    // As transformações locais não devem reexecutar quando apenas o
    // controle remoto, relógio ou foco provoca uma recomposição.
    val baseChannels =
        remember(availableChannels, selectedCategory, favoriteChannelIds, recentChannelIds) {
            when (selectedCategory.kind) {
                ChannelCategoryKind.ALL ->
                    availableChannels

                ChannelCategoryKind.RECENT ->
                    recentChannelIds.mapNotNull{id->availableChannels.firstOrNull{it.id==id}}

                ChannelCategoryKind.FAVORITES ->
                    availableChannels.filter { channel ->
                        channel.id in favoriteChannelIds
                    }

                ChannelCategoryKind.SOURCE ->
                    availableChannels.filter { channel ->
                        channel.categoryId ==
                                selectedCategory.sourceCategoryId
                    }
            }
        }

    val normalizedSearch =
        remember(searchQuery) { searchQuery.trim() }

    val searchedChannels =
        remember(baseChannels, normalizedSearch) {
            if (normalizedSearch.isBlank()) {
                baseChannels
            } else {
                /*
                 * Busca local temporária durante a migração.
                 *
                 * A consulta global do catálogo será movida para SQL/Paging
                 * em uma etapa própria, para não recriar todos os canais em RAM.
                 */
                baseChannels.filter { channel ->
                    channel.name.contains(
                        normalizedSearch,
                        ignoreCase = true
                    ) ||
                            channel.number
                                .toString()
                                .contains(normalizedSearch)
                }
            }
        }

    val displayedChannels =
        remember(searchedChannels, sortMode) {
            when (sortMode) {
                ChannelSortMode.DEFAULT -> searchedChannels
                ChannelSortMode.A_Z ->
                    searchedChannels.sortedBy {
                        it.name.lowercase()
                    }

                ChannelSortMode.Z_A ->
                    searchedChannels.sortedByDescending {
                        it.name.lowercase()
                    }
            }
        }

    val selectedChannel =
        remember(availableChannels, selectedChannelId, displayedChannels, pinnedChannel, restoringLast) {
            if(restoringLast)null else pinnedChannel?.takeIf{it.id==selectedChannelId} ?: availableChannels.firstOrNull {
                it.id == selectedChannelId
            }
        }

    /*
     * GUIA DE PROGRAMACAO REAL
     *
     * Sem dados EPG, nenhum programa ficticio e exibido.
     * A troca de canal cancela a consulta anterior automaticamente.
     * A origem (playlist) participa da chave para impedir reutilizacao
     * indevida da grade de outra playlist.
     */
    val latestEpgLoader by rememberUpdatedState(onLoadChannelEpg)
    var epgPrograms by remember { mutableStateOf<List<PlaybackEpgProgram>>(emptyList()) }
    var epgLoading by remember { mutableStateOf(false) }
    var epgClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(KidsAppearance.active) {
        if(KidsAppearance.active)return@LaunchedEffect
        while (true) {
            delay(60_000L)
            epgClockMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(selectedChannel?.id, epgSourceKey, onLoadChannelEpg != null, KidsAppearance.active) {
        epgPrograms = emptyList()
        val channelId = selectedChannel?.id?.trim().orEmpty()
        val loader = latestEpgLoader
        if (KidsAppearance.active || channelId.isEmpty() || loader == null) {
            epgLoading = false
            return@LaunchedEffect
        }

        epgLoading = true
        try {
            epgPrograms = loader(channelId)
                .filter { it.endEpochMs > it.startEpochMs }
                .sortedBy { it.startEpochMs }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // URL e credenciais do provedor nunca aparecem na interface.
            epgPrograms = emptyList()
        } finally {
            epgLoading = false
        }
    }

    val selectedGuideDate = remember(selectedDayIndex, epgClockMs) {
        LocalDate.now().plusDays((selectedDayIndex - 3).toLong())
    }
    val guidePrograms = remember(epgPrograms, selectedGuideDate, epgClockMs) {
        val zone = ZoneId.systemDefault()
        val dayStart = selectedGuideDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = selectedGuideDate.plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        epgPrograms.asSequence()
            .filter { program ->
                program.startEpochMs < dayEnd && program.endEpochMs > dayStart
            }
            .mapIndexed { index, program ->
                GuideProgram(
                    id = "${program.startEpochMs}:${program.endEpochMs}:$index",
                    title = program.title,
                    startTime = formatEpgClock(program.startEpochMs),
                    endTime = formatEpgClock(program.endEpochMs),
                    startEpochMs = program.startEpochMs,
                    endEpochMs = program.endEpochMs,
                    progressFraction = if (
                        epgClockMs >= program.startEpochMs &&
                        epgClockMs < program.endEpochMs &&
                        program.endEpochMs > program.startEpochMs
                    ) {
                        ((epgClockMs - program.startEpochMs).toFloat() /
                                (program.endEpochMs - program.startEpochMs).toFloat())
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    isLive = epgClockMs >= program.startEpochMs &&
                            epgClockMs < program.endEpochMs
                )
            }
            .toList()
    }
    val currentlyAiring = remember(epgPrograms, epgClockMs) {
        epgPrograms.firstOrNull { program ->
            epgClockMs >= program.startEpochMs && epgClockMs < program.endEpochMs
        }
    }
    val currentEpgTimeRange = currentlyAiring?.let { program ->
        "${formatEpgClock(program.startEpochMs)} – " +
                formatEpgClock(program.endEpochMs)
    }

    val effectiveTotalChannelCount =
        if (usingPaging) {
            totalChannelCount
        } else {
            previewChannels.size
        }

    val categoryCounts =
        remember(
            previewChannels,
            favoriteChannelIds,
            channelCategories,
            pagingCategoryCounts,
            effectiveTotalChannelCount,
            recentChannelIds,
            usingPaging
        ) {
            channelCategories.associateWith { category ->
                when (category.kind) {
                    ChannelCategoryKind.ALL ->
                        effectiveTotalChannelCount

                    ChannelCategoryKind.RECENT -> recentChannelIds.size

                    ChannelCategoryKind.FAVORITES ->
                        previewChannels.count {
                            it.id in favoriteChannelIds
                        }

                    ChannelCategoryKind.SOURCE ->
                        if (usingPaging) {
                            pagingCategoryCounts[
                                category.sourceCategoryId
                            ] ?: 0
                        } else {
                            previewChannels.count {
                                it.categoryId ==
                                        category.sourceCategoryId
                            }
                        }
                }
            }
        }

    /*
     * No fluxo normal (Todos ou uma categoria do provedor), usamos
     * LazyPagingItems diretamente. Recursos ainda locais, como favoritos,
     * recentes, pesquisa e ordenação, continuam usando somente o snapshot
     * carregado até migrarmos suas consultas para o Room.
     */
    val usePagedChannelList = usingPaging && selectedCategory.kind != ChannelCategoryKind.RECENT
    val browseIds = when (selectedCategory.kind) {
        ChannelCategoryKind.FAVORITES -> favoriteChannelIds
        ChannelCategoryKind.RECENT -> recentChannelIds.toSet()
        else -> null
    }
    LaunchedEffect(normalizedSearch, sortMode, browseIds) {
        if (normalizedSearch.isNotBlank()) delay(150)
        onBrowseChanged(normalizedSearch, sortMode.ordinal, browseIds)
    }

    var isLiveFullScreen by remember(epgSourceKey){mutableStateOf(false)}
    // A focusable loading anchor keeps Compose from falling back to Organizar
    // when restoring history or replacing a PagingSource removes the focused row.
    var leftReturnFocus by remember { mutableStateOf<FocusRequester?>(null) }
    var entryOwnsFocus by remember(epgSourceKey) { mutableStateOf(true) }
    var entryHandoff by remember(epgSourceKey) { mutableStateOf(true) }
    val entryParkingFocus=remember { FocusRequester() }
    val entryPending= entryOwnsFocus && !searchKeyboardVisible && !isLiveFullScreen && (catalogLoading || restoringLast ||
        (leftPaneMode==LeftPaneMode.CHANNELS && usePagedChannelList && pagedChannels!=null &&
            (pagedChannels.loadState.refresh is LoadState.Loading)))
    LaunchedEffect(entryPending) {
        android.util.Log.d("LiveEntryFocus","pending=$entryPending handoff=$entryHandoff pane=$leftPaneMode")
        if(entryPending) {
            entryHandoff=true
            withFrameNanos{};entryParkingFocus.requestFocus()
        } else {
            repeat(2){withFrameNanos{}}
            if(entryHandoff && !searchKeyboardVisible && !isLiveFullScreen) runCatching { leftReturnFocus?.requestFocus() }
            entryHandoff=false
        }
    }
    // The media, decoder and video surface are never recreated when expanding.
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val livePlayer=remember(context) {
        val http=DefaultHttpDataSource.Factory().setConnectTimeoutMs(7_000).setReadTimeoutMs(7_000)
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(http)
                .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(0)))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1_500,12_000,500,1_200)
                .setTargetBufferBytes(br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.playerBufferBytes).build())
            .setAudioAttributes(AudioAttributes.DEFAULT,true).setHandleAudioBecomingNoisy(true).build()
    }
    val connection=remember(livePlayer){LiveChannelConnection(livePlayer)}
    val connectionState by connection.state.collectAsState()
    val liveIsBuffering=connectionState.buffering
    val liveError=when {connectionState.searching->"Buscando servidor disponível…"
        connectionState.unavailable->"Canal indisponível no momento.";else->null}

    var liveVideoFocused by remember{mutableStateOf(false)}
    var pictureLayout by remember { mutableStateOf(readLiveVideoLayout(favoritePreferences)) }
    var pictureSettings by remember { mutableStateOf(false) }
    var channelNameVisible by remember{mutableStateOf(false)}
    var channelNameActivity by remember{mutableIntStateOf(0)}
    var retryChannelSerial by remember(epgSourceKey){mutableIntStateOf(0)}
    var videoBounds by remember{mutableStateOf(Rect.Zero)}
    var rootOrigin by remember{mutableStateOf(Offset.Zero)}
    val videoFocus=remember{FocusRequester()}
    val pictureFocus=remember{FocusRequester()}

    var pictureControlFocused by remember{mutableStateOf(false)}
    val latestPreviewChannel by rememberUpdatedState(selectedChannel)
    val latestConnection by rememberUpdatedState(connectionState)
    val latestAlternatives by rememberUpdatedState(onFindChannelAlternatives)
    val latestNeighbours by rememberUpdatedState(onLoadChannelNeighbours)
    val uiScope=rememberCoroutineScope()
    var zapJob by remember{mutableStateOf<Job?>(null)}
    var zapSerial by remember{mutableIntStateOf(0)}
    var zapVisible by remember{mutableStateOf(false)}
    val neighbourIds=when(selectedCategory.kind){ChannelCategoryKind.FAVORITES->favoriteChannelIds
        ChannelCategoryKind.RECENT->recentChannelIds.toSet();else->null}
    fun focusPictureControl() {
        zapVisible=false
        channelNameVisible=true
        channelNameActivity++
        uiScope.launch { withFrameNanos{};pictureFocus.requestFocus() }
    }
    fun chooseChannel(channel:Channel) {
        pinnedChannel=PreviewChannel(channel.id,1,channel.name,channel.categoryId,channel.logoUrl,channel.streamUrl)
        selectedChannelId=channel.id
    }
    fun confirmChannel(id: String) {
        if (id == selectedChannelId) {
            if (connectionState.unavailable) retryChannelSerial++
            else { isLiveFullScreen = true; channelNameActivity++ }
            return
        }
        val channel = availableChannels.firstOrNull { it.id == id } ?: return
        pinnedChannel = channel
        selectedChannelId = id
    }
    suspend fun neighbours(id:String,direction:Int):List<Channel> = latestNeighbours?.invoke(id,direction,
        selectedCategory.sourceCategoryId,selectedCategory.kind==ChannelCategoryKind.SOURCE,neighbourIds).orEmpty()
    fun zap(direction:Int) {
        zapVisible=true;zapSerial++
        val id=selectedChannel?.id?:return
        if(zapJob?.isActive==true)return
        zapJob=uiScope.launch {
            val channel=neighbours(id,direction).firstOrNull()
            if(channel!=null&&latestPreviewChannel?.id==id)chooseChannel(channel)
        }
    }
    LaunchedEffect(isLiveFullScreen,selectedChannel?.id,channelNameActivity,zapSerial,pictureSettings) {
        channelNameVisible=isLiveFullScreen
        if(isLiveFullScreen && !pictureSettings){
            delay(3_000L)
            channelNameVisible=false
            if(pictureControlFocused) videoFocus.requestFocus()
        }
    }
    LaunchedEffect(zapSerial,zapVisible){if(zapVisible && !pictureSettings){delay(8_000L);zapVisible=false;videoFocus.requestFocus()}}
    LaunchedEffect(isLiveFullScreen){if(isLiveFullScreen){withFrameNanos{};videoFocus.requestFocus()}}
    DisposableEffect(livePlayer,lifecycleOwner) {
        val observer=LifecycleEventObserver{_,event->when(event){
            Lifecycle.Event.ON_STOP->livePlayer.pause()
            Lifecycle.Event.ON_START->{if(latestConnection.playing)livePlayer.play()}
            else->Unit}}
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose{lifecycleOwner.lifecycle.removeObserver(observer);livePlayer.release()}
    }
    LaunchedEffect(epgSourceKey,selectedChannel?.id,selectedChannel?.streamUrl,parentalAccess,retryChannelSerial,lifecycleOwner) {
        val channel=selectedChannel?:return@LaunchedEffect
        if(parentalAccess.blocked(epgSourceKey.orEmpty(),"LIVE_TV",channel.categoryId,channel.name,categoryNamesForProtection[channel.categoryId].orEmpty()))return@LaunchedEffect
        val original=Channel(channel.id,channel.name,channel.categoryId,channel.logoUrl,channel.streamUrl,null)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            connection.connect(LiveStreamCandidate(epgSourceKey.orEmpty(),original),
                {latestAlternatives?.invoke(channel.id)?:emptyFlow()},{rememberWatched(channel)})
        }
    }

    BackHandler(enabled = isLiveFullScreen) {
        when {
            pictureSettings -> { pictureSettings=false; channelNameActivity++; videoFocus.requestFocus() }
            zapVisible -> { zapVisible=false; videoFocus.requestFocus() }
            else -> { isLiveFullScreen=false; searchQuery=""; leftPaneMode=LeftPaneMode.CATEGORIES }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .catalogEscapeBack {
                if (!isLiveFullScreen && !searchKeyboardVisible) {
                    if (leftPaneMode == LeftPaneMode.CHANNELS) {
                        searchQuery=""
                        leftPaneMode=LeftPaneMode.CATEGORIES
                    } else if (categoryHasFocus) onExit()
                    else categoryEntryFocus.requestFocus()
                    true
                } else false
            }
            .onGloballyPositioned{rootOrigin=it.boundsInWindow().topLeft}
            .onPreviewKeyEvent { event->
                if(isLiveFullScreen&&event.type==KeyEventType.KeyDown)channelNameActivity++
                val native = event.nativeKeyEvent.keyCode
                if(catalogError!=null && event.key in listOf(Key.DirectionCenter,Key.Enter,Key.NumPadEnter)) {
                    if(event.type==KeyEventType.KeyUp)onRetryCatalog()
                    true
                } else if (!searchKeyboardVisible && !pictureSettings &&
                    (native == android.view.KeyEvent.KEYCODE_CHANNEL_UP || native == android.view.KeyEvent.KEYCODE_CHANNEL_DOWN)) {
                    if (event.type == KeyEventType.KeyDown) zap(if (native == android.view.KeyEvent.KEYCODE_CHANNEL_UP) 1 else -1)
                    true
                } else false
            }
            .fillMaxSize()
            .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF070708))
    ) {
        val fullViewportWidth = maxWidth
        val compactLayout =
            maxHeight < 650.dp ||
                    maxWidth < 1200.dp

        val outerPadding =
            if (compactLayout) 10.dp else 18.dp

        val leftPaneWidth =
            if(KidsAppearance.active) maxWidth*.26f else if (compactLayout) 320.dp else 390.dp

        val rightDayWidth =
            if (compactLayout) 66.dp else 82.dp

        val horizontalGap =
            if (compactLayout) 12.dp else 18.dp

        val topBarHeight =
            if (compactLayout) 44.dp else 54.dp

        br.com.flaviotorres.iptvpremium.ui.components.CatalogThemeBackdrop(Modifier.matchParentSize(),cinemaKids=true,cinemaTitle="CANAIS")

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(if(KidsAppearance.active) androidx.compose.foundation.layout.PaddingValues(start=maxWidth*.025f,end=maxWidth*.025f,top=maxHeight*.065f,bottom=maxHeight*.10f) else androidx.compose.foundation.layout.PaddingValues(outerPadding)),
            horizontalArrangement = Arrangement.spacedBy(horizontalGap)
        ) {
            /*
             * COLUNA ESQUERDA
             */
            Column(
                modifier = Modifier
                    .width(leftPaneWidth)
                    .fillMaxHeight()
                    .focusProperties { canFocus = !searchKeyboardVisible }
                    .onPreviewKeyEvent { event ->
                        if (!searchKeyboardVisible && leftPaneMode == LeftPaneMode.CHANNELS && event.key == Key.DirectionLeft) {
                            if (event.type == KeyEventType.KeyDown) { searchQuery="";leftPaneMode = LeftPaneMode.CATEGORIES }
                            true
                        } else if (!searchKeyboardVisible && leftPaneMode == LeftPaneMode.CATEGORIES && event.key == Key.DirectionRight) {
                            if (event.type == KeyEventType.KeyDown) leftPaneMode = LeftPaneMode.CHANNELS
                            true
                        } else if (!searchKeyboardVisible && event.key == Key.DirectionRight && selectedChannel != null && videoBounds.width > 0f) {
                            if (event.type == KeyEventType.KeyDown) videoFocus.requestFocus()
                            true
                        } else false
                    }
                    .background(
                        brush = if(KidsAppearance.active) kidsCinemaPanelBrush() else br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.categoryPanelBrush,
                        shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
                    )
                    .kidsCinemaBorder(
                        border = BorderStroke(
                            width = 1.3.dp,
                            brush = NeutralLiveBorder
                        ),
                        shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
                    )
                    .focusRequester(entryParkingFocus)
                    .focusable(enabled=entryPending || entryHandoff)
                    .padding(if (compactLayout) 10.dp else 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (KidsAppearance.active) { if(compactLayout)88.dp else 110.dp } else topBarHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (KidsAppearance.active) {
                        KidsCinemaCatalogHeader(compactLayout, if(leftPaneMode == LeftPaneMode.CATEGORIES) "Categorias" else "Canais")
                    } else {
                    AppLogo(
                        compactLayout = compactLayout
                    )

                    Spacer(
                        modifier = Modifier.width(
                            if (compactLayout) 10.dp else 14.dp
                        )
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                if (leftPaneMode == LeftPaneMode.CATEGORIES) {
                                    "CATEGORIAS"
                                } else {
                                    "CANAIS"
                                },
                            color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
                            fontSize =
                                if (compactLayout) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(0.dp))
                    }

                    Spacer(Modifier.width(0.dp))
                
                    }
                }

                if(!KidsAppearance.active) PremiumOrnamentDivider(compactLayout = compactLayout)

                Spacer(
                    modifier = Modifier.height(
                        if (compactLayout) 8.dp else 12.dp
                    )
                )

                if (
                    leftPaneMode == LeftPaneMode.CATEGORIES &&
                    normalizedSearch.isBlank()
                ) {
                    CategoryList(
                        entryFocus = categoryEntryFocus,
                        onFocusWithin = { categoryHasFocus = it },
                        categories = channelCategories,
                        onRowFocused = { leftReturnFocus = it;entryOwnsFocus=true;entryHandoff=false;android.util.Log.d("LiveEntryFocus","row focused pane=$leftPaneMode") },
                        requestEntryFocus = !searchKeyboardVisible && !catalogLoading && !restoringLast && !isLiveFullScreen,
                        selectedCategory = selectedCategory,
                        categoryCounts = categoryCounts,
                        compactLayout = compactLayout,
                        onCategorySelected = { category ->
                            onCategoryAccessRequested(if (category.kind == ChannelCategoryKind.SOURCE) category.sourceCategoryId else null, category.title) {
                            selectedCategoryId =
                                category.id

                            /*
                             * ALL e categorias do provedor mudam o PagingSource
                             * no ViewModel. Favoritos/Recentes continuam locais
                             * por enquanto e usam a fonte completa.
                             */
                            onSourceCategorySelected(
                                if (
                                    category.kind ==
                                    ChannelCategoryKind.SOURCE
                                ) {
                                    category.sourceCategoryId
                                } else {
                                    null
                                }
                            )

                            leftPaneMode =
                                LeftPaneMode.CHANNELS
                        
                            }
                        }
                    )
                } else {
                    if (
                        usePagedChannelList &&
                        pagedChannels != null
                    ) {
                        androidx.compose.runtime.key(selectedCategory.id, normalizedSearch, sortMode, browseIds) {
                        PagedChannelList(
                            channels = pagedChannels,
                            restoredChannel = pinnedChannel?.takeIf {
                                normalizedSearch.isBlank() &&
                                (selectedCategory.kind != ChannelCategoryKind.SOURCE || it.categoryId == selectedCategory.sourceCategoryId) &&
                                (selectedCategory.kind != ChannelCategoryKind.FAVORITES || it.id in favoriteChannelIds)
                            },
                            requestEntryFocus = !searchKeyboardVisible && !catalogLoading && !restoringLast && !isLiveFullScreen,
                            onRowFocused = { leftReturnFocus = it;entryOwnsFocus=true;entryHandoff=false;android.util.Log.d("LiveEntryFocus","row focused pane=$leftPaneMode") },
                            selectedChannelId = selectedChannelId,
                            favoriteChannelIds = favoriteChannelIds,
                            compactLayout = compactLayout,
                            onChannelSelected = { channelId ->
                                confirmChannel(channelId)
                            },
                            onFavoriteClick = { channelId -> toggleChannelFavorite(channelId) }
                        )
                        }
                    } else {
                        ChannelList(
                            channels = displayedChannels,
                            requestEntryFocus = !searchKeyboardVisible && !catalogLoading && !restoringLast && !isLiveFullScreen,
                            onRowFocused = { leftReturnFocus = it;entryOwnsFocus=true;entryHandoff=false;android.util.Log.d("LiveEntryFocus","row focused pane=$leftPaneMode") },
                            selectedChannelId = selectedChannelId,
                            favoriteChannelIds = favoriteChannelIds,
                            compactLayout = compactLayout,
                            onChannelSelected = { channelId ->
                                confirmChannel(channelId)
                            },
                            onFavoriteClick = { channelId -> toggleChannelFavorite(channelId) }
                        )
                    }
                }
            }

            /*
             * ÁREA CENTRAL
             */
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                TopActionBar(
                    entryPending=entryPending || entryHandoff,
                    onToolbarFocused={entryOwnsFocus=false},
                    height = topBarHeight,
                    sortMode = sortMode,
                    compactLayout = compactLayout,
                    searchQuery = searchQuery,
                    searchActive = searchKeyboardVisible,
                    searchCursorPosition = searchCursorPosition,
                    onSearchClick = {
                        searchCursorPosition = searchQuery.length
                        searchKeyboardVisible = true
                    },
                    onSearchClear = {
                        searchQuery = ""
                        searchCursorPosition = 0
                    },
                    onSortClick = {
                        sortMode =
                            when (sortMode) {
                                ChannelSortMode.DEFAULT ->
                                    ChannelSortMode.A_Z

                                ChannelSortMode.A_Z ->
                                    ChannelSortMode.Z_A

                                ChannelSortMode.Z_A ->
                                    ChannelSortMode.DEFAULT
                            }
                    }
                )

                Spacer(
                    modifier = Modifier.height(
                        if (compactLayout) 8.dp else 12.dp
                    )
                )

                // Reserve a altura correta para o vídeo, antes de medir o EPG.
                // A largura disponível é a da coluna central, sem invadir as categorias.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().then(if(KidsAppearance.active)Modifier.weight(1f).testTag("kids-live-viewport")else Modifier)) {
                    // Prévia levemente mais baixa para ampliar a área útil do EPG.
                    // O vídeo mantém sua proporção dentro deste quadro, sem invadir
                    // a barra superior, as margens ou o guia de programação.
                    val idealPreviewHeight = maxWidth * (9f / 16f)
                    val reducedPreviewHeight = idealPreviewHeight * 0.85f
                    val minimumGuideHeight = if (compactLayout) 132.dp else 178.dp
                    val previewHeight = if(KidsAppearance.active) (maxHeight - if(liveError!=null)36.dp else 0.dp).coerceAtLeast(1.dp) else minOf(
                        reducedPreviewHeight,
                        (maxHeight - minimumGuideHeight).coerceAtLeast(120.dp)
                    )

                    run {
                        ChannelPreview(
                            compactLayout = compactLayout,
                            playbackError = if(connectionState.unavailable)liveError else null,
                            centerMessage = when {
                                catalogLoading -> "Estamos conectando seus canais. Aguarde…"
                                catalogError != null -> catalogError
                                selectedChannel == null -> "Selecione um canal e pressione OK."
                                else -> null
                            },
                            onRetry = if(catalogError != null) onRetryCatalog else null,
                            previewHeight = previewHeight,
                            onVideoBounds = {videoBounds=it},
                            surfaceFocused = liveVideoFocused
                        )
                    }
                }

                if(!KidsAppearance.active) {
                Spacer(
                    modifier = Modifier.height(
                        if (compactLayout) 8.dp else 12.dp
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            brush = PremiumPanelBackground,
                            shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
                        )
                        .kidsCinemaBorder(
                            border = BorderStroke(
                                width = 1.3.dp,
                                brush = NeutralLiveBorder
                            ),
                            shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
                        )
                        .padding(if (compactLayout) 10.dp else 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "▣  GUIA DE PROGRAMAÇÃO",
                            color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
                            fontSize = if (compactLayout) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        selectedChannel?.let { channel ->
                            Text(
                                text = channel.name,
                                color = AppGoldLight,
                                fontSize = if (compactLayout) 11.sp else 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if(!KidsAppearance.active) PremiumOrnamentDivider(compactLayout = compactLayout)

                    Spacer(
                        modifier = Modifier.height(
                            if (compactLayout) 8.dp else 12.dp
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                if (compactLayout) 6.dp else 9.dp
                            )
                    ) {
                        if (guidePrograms.isEmpty()) {
                            item(key = "epg_status") {
                                Text(
                                    text = when {
                                        selectedChannel == null -> "Selecione um canal para ver a programação."
                                        epgLoading -> "Consultando a programação do canal..."
                                        onLoadChannelEpg == null -> "Guia de programação ainda não conectado."
                                        else -> "Programação indisponível para este canal nesta data."
                                    },
                                    color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                                    fontSize = if (compactLayout) 12.sp else 14.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            items(
                                items = guidePrograms,
                                key = { program -> program.id }
                            ) { program ->
                                GuideProgramRow(
                                    program = program,
                                    compactLayout = compactLayout
                                )
                            }
                        }
                    }
                }
                }
            }

            /*
             * DIAS DO GUIA
             */
            if(!KidsAppearance.active) DayGuideColumn(
                modifier = Modifier
                    .width(rightDayWidth)
                    .fillMaxHeight(),
                selectedDayIndex = selectedDayIndex,
                compactLayout = compactLayout,
                onDayClick = { index ->
                    selectedDayIndex = index
                }
            )
        }

        if (searchKeyboardVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = outerPadding,
                        end = outerPadding,
                        bottom = 8.dp
                    ),
                contentAlignment =
                    Alignment.BottomCenter
            ) {
                AppKeyboard(
                    modifier = Modifier.fillMaxWidth().onKeyEvent { event ->
                        event.key in listOf(Key.DirectionLeft,Key.DirectionRight,Key.DirectionUp,Key.DirectionDown)
                    },
                    actionText = "CONCLUIR",
                    onCharacter = { character ->
                        // Substitui a letra destacada; no final, acrescenta.
                        val edited = tvKeyboardType(
                            searchQuery,
                            searchCursorPosition,
                            character
                        )
                        searchQuery = edited.value
                        searchCursorPosition = edited.cursor
                    },
                    onBackspace = {
                        val edited = tvKeyboardDelete(
                            searchQuery,
                            searchCursorPosition
                        )
                        searchQuery = edited.value
                        searchCursorPosition = edited.cursor
                    },
                    onMoveCursorLeft = {
                        searchCursorPosition = tvKeyboardMoveCursor(
                            searchQuery,
                            searchCursorPosition,
                            -1
                        )
                    },
                    onMoveCursorRight = {
                        searchCursorPosition = tvKeyboardMoveCursor(
                            searchQuery,
                            searchCursorPosition,
                            1
                        )
                    },
                    onAction = {
                        // Apenas fecha o teclado e preserva a pesquisa.
                        searchKeyboardVisible = false
                    }
                )
            }
        }

        // Exactly one AndroidView remains mounted in this root position in both sizes.
        if(selectedChannel!=null&&videoBounds.width>0f) {
            val density=LocalDensity.current
            val target=if(isLiveFullScreen)Rect(0f,0f,constraints.maxWidth.toFloat(),constraints.maxHeight.toFloat())
                else Rect(videoBounds.left-rootOrigin.x,videoBounds.top-rootOrigin.y,videoBounds.right-rootOrigin.x,videoBounds.bottom-rootOrigin.y)
            Box(Modifier.offset{IntOffset(target.left.toInt(),target.top.toInt())}
                .size(with(density){target.width.toDp()},with(density){target.height.toDp()})
                .background(Color.Black).focusRequester(videoFocus)
                .onFocusChanged{liveVideoFocused=it.isFocused}
                .graphicsLayer{alpha=if(searchKeyboardVisible)0f else 1f}
                .onPreviewKeyEvent {event->
                    val ok=event.key==Key.DirectionCenter||event.key==Key.Enter||event.key==Key.NumPadEnter
                    when {
                        !isLiveFullScreen && event.key == Key.DirectionLeft && leftReturnFocus != null -> {
                            if (event.type == KeyEventType.KeyDown) leftReturnFocus?.requestFocus()
                            true
                        }
                        isLiveFullScreen && !pictureSettings && !zapVisible && event.key in listOf(Key.DirectionUp,Key.DirectionDown,Key.DirectionLeft) -> {
                            if(event.type==KeyEventType.KeyDown){zapVisible=true;zapSerial++};true
                        }
                        isLiveFullScreen&&!pictureControlFocused&&!pictureSettings&&!zapVisible&&event.key==Key.DirectionRight->{
                            if(event.type==KeyEventType.KeyDown)focusPictureControl();true
                        }
                        isLiveFullScreen&&pictureControlFocused&&!pictureSettings&&event.key in listOf(Key.DirectionLeft,Key.DirectionUp,Key.DirectionDown)->{
                            if(event.type==KeyEventType.KeyDown){
                                zapVisible=true;zapSerial++
                            };true
                        }
                        isLiveFullScreen&&pictureControlFocused&&!pictureSettings&&event.key==Key.DirectionRight->true
                        isLiveFullScreen&&ok&&!pictureControlFocused&&!pictureSettings&&!zapVisible->{
                            if(event.type==KeyEventType.KeyDown){zapVisible=!zapVisible;zapSerial++};true
                        }
                        else->false
                    }
                }
                .clickable {zapVisible=false;pictureSettings=false;isLiveFullScreen=!isLiveFullScreen}) {
                StableLiveVideo(livePlayer,isLiveFullScreen,pictureLayout,Modifier.fillMaxSize())
                if(liveIsBuffering||(isLiveFullScreen&&liveError!=null))Text(liveError?:"Conectando canal…",color=Color.White,
                    modifier=Modifier.align(Alignment.Center).background(Color(0xB8000000),RoundedCornerShape(8.dp)).padding(12.dp),fontSize=14.sp)
                if(isLiveFullScreen) {
                    if(channelNameVisible && !zapVisible && !pictureSettings) LiveFormatButton(
                        focusRequester=pictureFocus,onFocus={pictureControlFocused=it},
                        onClick={pictureSettings=true;zapVisible=false},
                        modifier=Modifier.align(Alignment.BottomEnd).padding(16.dp))
                    if(pictureSettings) LiveImageSettings(pictureLayout, {
                        pictureLayout=it;saveLiveVideoLayout(favoritePreferences,it)
                    }, {pictureSettings=false;channelNameActivity++;videoFocus.requestFocus()}, Modifier.align(Alignment.CenterEnd)
                        .padding(16.dp).width((fullViewportWidth*.36f).coerceIn(250.dp,400.dp)).fillMaxHeight(.90f))
                    if(channelNameVisible && !zapVisible && !pictureSettings)Text(selectedChannel.name,color=Color.White,fontSize=18.sp,
                        modifier=Modifier.align(Alignment.TopStart).padding(18.dp).background(Color(0x99000000)).padding(8.dp))
                    if(zapVisible && !pictureSettings) LiveSlimMenu(
                        categories=channelCategories, selectedCategory=selectedCategory,
                        channels=if(usePagedChannelList) pagedChannels else null,
                        fallbackChannels=displayedChannels, playingChannel=selectedChannel,
                        favorites=favoriteChannelIds,
                        restorePlaying=normalizedSearch.isBlank(),
                        onCategory={category ->
                            onCategoryAccessRequested(category.sourceCategoryId, category.title) {
                                selectedCategoryId=category.id
                                onSourceCategorySelected(category.sourceCategoryId)
                                leftPaneMode=LeftPaneMode.CHANNELS
                            }
                        },
                        onConfirm={channel ->
                            if(channel.id==selectedChannelId&&connectionState.unavailable)retryChannelSerial++
                            pinnedChannel=channel;selectedChannelId=channel.id
                            zapVisible=false;videoFocus.requestFocus()
                        },
                        onInteraction={zapSerial++;channelNameActivity++},
                        onClose={zapVisible=false;videoFocus.requestFocus()},
                        onImageSettings={focusPictureControl()},
                        modifier=Modifier.align(Alignment.CenterEnd).padding(end=16.dp)
                            .fillMaxHeight(.86f), channelWidth=(fullViewportWidth*.30f).coerceIn(220.dp,360.dp))
                }
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
                if (compactLayout) 74.dp else 98.dp
            )
            .height(
                if (compactLayout) 42.dp else 52.dp
            ),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun TopActionBar(
    entryPending:Boolean=false,
    onToolbarFocused:()->Unit={},
    height: androidx.compose.ui.unit.Dp,
    sortMode: ChannelSortMode,
    compactLayout: Boolean,
    searchQuery: String,
    searchActive: Boolean,
    searchCursorPosition: Int,
    onSearchClick: () -> Unit,
    onSearchClear: () -> Unit,
    onSortClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().focusProperties { canFocus = !entryPending }
            .height(height)
            .background(
                brush = PremiumPanelBackground,
                shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
            )
            .kidsCinemaBorder(
                border = BorderStroke(
                    width = 1.25.dp,
                    brush = NeutralLiveBorder
                ),
                shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
            )
            .padding(
                horizontal = if (compactLayout) 12.dp else 16.dp,
                vertical = if (compactLayout) 6.dp else 8.dp
            ),
        horizontalArrangement =
            Arrangement.spacedBy(
                if (compactLayout) 8.dp else 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            text =
                when (sortMode) {
                    ChannelSortMode.DEFAULT -> "Organizar"
                    ChannelSortMode.A_Z -> "A – Z"
                    ChannelSortMode.Z_A -> "Z – A"
                },
            compactLayout = compactLayout,
            onClick = onSortClick,
            onFocused=onToolbarFocused
        )

        ChannelSearchBar(
            modifier = Modifier.weight(1f),
            query = searchQuery,
            active = searchActive,
            cursorPosition = searchCursorPosition,
            compactLayout = compactLayout,
            onClick = onSearchClick,
            onClear = onSearchClear,
            onFocused=onToolbarFocused
        )

        ClockDisplay(
            compactLayout = compactLayout
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    compactLayout: Boolean,
    onClick: () -> Unit,
    onFocused:()->Unit={}
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (compactLayout) 9.dp else 12.dp)
    val iconGlyph = when (text) {
        "Organizar", "A – Z", "Z – A" -> "↕"
        "Categoria" -> "≣"
        "Favoritos" -> "♡"
        else -> "•"
    }

    Row(
        modifier = Modifier
            .height(if (compactLayout) 38.dp else 46.dp)
            .background(
                brush = if (isFocused) { (if(KidsAppearance.active) Brush.verticalGradient(kidsCinemaSelectionColors()) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBrush()) } else {
                    Brush.verticalGradient((if(KidsAppearance.active) kidsCinemaCategoryColors(false) else listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10))))
                },
                shape = shape
            )
            .premiumGlow(isFocused)
            .kidsCinemaBorder(BorderStroke(if (isFocused) 1.5.dp else 1.dp, br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(isFocused, PremiumGoldBorder)), shape)
            .kidsCinemaToolbarFocus(isFocused)
            .onFocusChanged { isFocused = it.isFocused;if(it.isFocused)onFocused() }
            .onKeyEvent { event ->
                val selectKey = event.key == Key.DirectionCenter ||
                        event.key == Key.Enter || event.key == Key.NumPadEnter
                if (selectKey && event.type == KeyEventType.KeyUp) {
                    onClick()
                    true
                } else false
            }
            .kidsCinemaClickable(onClick = onClick)
            .focusable()
            .padding(horizontal = if (compactLayout) 9.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 5.dp else 8.dp)
    ) {
        Text(
            text = iconGlyph,
            color = if (isFocused) kidsCinemaRowInk(true,0xFFFFFFFF) else kidsCinemaInk(0xFFFFFFFF),
            fontSize = if (compactLayout) 12.sp else 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            color = if (isFocused) kidsCinemaRowInk(true,0xFFFFFFFF) else kidsCinemaInk(0xFFFFFFFF),
            fontSize = if (compactLayout) 11.sp else 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ChannelSearchBar(
    modifier: Modifier = Modifier,
    query: String,
    active: Boolean,
    cursorPosition: Int,
    compactLayout: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
    onFocused:()->Unit={}
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val highlighted = active || isFocused
    val shape = RoundedCornerShape(if (compactLayout) 10.dp else 12.dp)

    Row(
        modifier = modifier
            .height(if (compactLayout) 40.dp else 48.dp)
            .background(
                brush = Brush.verticalGradient(
                    (if(KidsAppearance.active) kidsCinemaCategoryColors(false) else listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10)))
                ),
                shape = shape
            )
            .kidsCinemaBorder(
                border = BorderStroke(
                    width = if (highlighted) 2.dp else 1.2.dp,
                    brush = if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder) else NeutralLiveBorder
                ),
                shape = shape
            )
            .premiumGlow(highlighted)
            .kidsCinemaToolbarFocus(isFocused,active)
            .onFocusChanged {
                isFocused = it.isFocused;if(it.isFocused)onFocused()
            }
            .onKeyEvent { event ->
                val selectKey =
                    event.key == Key.DirectionCenter ||
                            event.key == Key.Enter ||
                            event.key == Key.NumPadEnter

                if (selectKey && event.type == KeyEventType.KeyUp) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .kidsCinemaClickable(onClick = onClick)
            .focusable()
            .padding(horizontal = if (compactLayout) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compactLayout) 22.dp else 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⌕",
                color = if (highlighted) kidsCinemaRowInk(true,0xFFFFFFFF) else kidsCinemaInk(0xFFFFFFFF),
                fontSize = if (compactLayout) 17.sp else 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        TvKeyboardSearchText(
            textColor = kidsCinemaRowInk(highlighted,0xFFF5F5F5),
            placeholderColor = kidsCinemaRowInk(highlighted,0xFF909096),
            value = query,
            active = active,
            cursorPosition = cursorPosition,
            placeholder = if (KidsAppearance.active) "Qual canal vamos assistir?" else "Pesquisar canais",
            modifier = Modifier.weight(1f),
            fontSize = if (compactLayout) 11.sp else 13.sp
        )

        if (query.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(if (compactLayout) 22.dp else 24.dp)
                    .background(
                        color = if (highlighted) (if(KidsAppearance.active) kidsCinemaSelectionColors().first() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor()) else br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x2218181D),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = (if (active || isFocused) kidsCinemaInk(0xFFFFFFFF) else AppGoldLight),
                    fontSize = if (compactLayout) 15.sp else 17.sp,
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
        mutableStateOf(LocalDateTime.now())
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(30_000L)
        }
    }

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm")
    }
    val dayFormatter = remember {
        DateTimeFormatter.ofPattern("EEE", Locale("pt", "BR"))
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM", Locale("pt", "BR"))
    }

    if(KidsAppearance.active) { KidsCinemaClock(currentTime.format(timeFormatter));return }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = currentTime.format(dayFormatter).replaceFirstChar { it.uppercase() },
                color = kidsCinemaInk(0xFFF5F5F5),
                fontSize = if (compactLayout) 11.sp else 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = currentTime.format(dateFormatter),
                color = kidsCinemaInk(0xFFB7B7BD),
                fontSize = if (compactLayout) 10.sp else 12.sp
            )
        }

        Text(
            text = currentTime.format(timeFormatter),
            style = TextStyle(
                fontFamily = CinzelSemiBold,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compactLayout) 21.sp else 27.sp,
                brush = MetallicGoldBrushText
            )
        )
    }
}

/** Exibe nomes limpos sem alterar os IDs ou os filtros originais. */
private fun cleanCategoryDisplayName(value: String): String {
    return value.trim().replace(
        channelCategoryPrefix,
        ""
    ).trim()
}

@Composable
private fun CategoryList(
    entryFocus: FocusRequester,
    onFocusWithin: (Boolean) -> Unit,
    categories: List<ChannelCategory>,
    onRowFocused: (FocusRequester) -> Unit,
    requestEntryFocus: Boolean,
    selectedCategory: ChannelCategory,
    categoryCounts: Map<ChannelCategory, Int>,
    compactLayout: Boolean,
    onCategorySelected: (ChannelCategory) -> Unit
) {
    val selectedIndex = categories.indexOfFirst { it.id == selectedCategory.id }.coerceAtLeast(0)
    val state = androidx.compose.foundation.lazy.rememberLazyListState(selectedIndex)
    val selectedFocus = remember { FocusRequester() }
    LaunchedEffect(selectedCategory.id, categories) {
        if (categories.isNotEmpty()) {
            state.scrollToItem(selectedIndex)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().focusRequester(entryFocus).onFocusChanged { onFocusWithin(it.hasFocus) }.focusGroup(),
        state = state,
        verticalArrangement =
            Arrangement.spacedBy(
                if (compactLayout) 4.dp else 6.dp
            )
    ) {
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            CategoryItem(
                focus = if (requestEntryFocus && category.id == categories.getOrNull(selectedIndex)?.id) selectedFocus else null,
                onRowFocused = onRowFocused,
                title = cleanCategoryDisplayName(category.title),
                count = categoryCounts[category] ?: 0,
                isSelected = category == selectedCategory,
                compactLayout = compactLayout,
                onSelect = {
                    onCategorySelected(category)
                }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    onRowFocused: (FocusRequester) -> Unit,
    title: String,
    count: Int,
    isSelected: Boolean,
    compactLayout: Boolean,
    onSelect: () -> Unit,
    focus: FocusRequester? = null
) {
    val rowFocus = remember { FocusRequester() }
    LaunchedEffect(focus) {
        if (focus != null) {
            androidx.compose.runtime.withFrameNanos { }
            rowFocus.requestFocus()
        }
    }
    var isFocused by remember {
        mutableStateOf(false)
    }

    val shape = RoundedCornerShape(if (compactLayout) 10.dp else 12.dp)
    val highlighted = isFocused || isSelected

    Row(
        modifier = Modifier
            .focusRequester(rowFocus)
            .fillMaxWidth()
            .height(if (compactLayout) 40.dp else 46.dp)
            .then(if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) Modifier.clip(shape) else Modifier)
            .background(
                brush = Brush.horizontalGradient(
                    if (highlighted) kidsCinemaCategoryColors(true)
                    else kidsCinemaCategoryColors(false)
                ),
                shape = shape
            )
            .premiumGlow(highlighted)
            .premiumCategoryCardBorder(
                highlighted = highlighted,
                roundedDp = if (compactLayout) 10f else 12f
            )
            .kidsCinemaFocus(isFocused,isSelected)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onRowFocused(rowFocus)
            }
            .onKeyEvent { event ->
                val selectKey =
                    event.key == Key.DirectionCenter ||
                            event.key == Key.Enter ||
                            event.key == Key.NumPadEnter

                if (selectKey && event.type == KeyEventType.KeyUp) {
                    onSelect()
                    true
                } else {
                    false
                }
            }
            .kidsCinemaClickable(onClick = onSelect)
            .focusable()
            .padding(horizontal = if (compactLayout) 12.dp else 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (KidsAppearance.active) {
            // Clean text-only category navigation in Kids.
        }
        Text(
            text = kidsCinemaCategoryLabel(title),
            modifier = Modifier.weight(1f),
            color = if (highlighted) kidsCinemaRowInk(true,0xFFFFFFFF) else kidsCinemaInk(0xFFF5F5F5),
            fontSize = if (compactLayout) 12.sp else 14.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight=if(KidsAppearance.active)14.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
            maxLines = if(KidsAppearance.active)2 else 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = count.toString(),
            modifier=Modifier.padding(start=if(KidsAppearance.active)8.dp else 0.dp).widthIn(min=if(KidsAppearance.active)25.dp else 0.dp),
            textAlign=androidx.compose.ui.text.style.TextAlign.End,
            color = if (highlighted) kidsCinemaRowInk(true,0xFFFFFFFF) else kidsCinemaInk(0xFFB7B7BD),
            fontSize = if (compactLayout) 11.sp else 13.sp
        )
    }
}

@Composable
private fun PagedChannelList(
    channels: LazyPagingItems<Channel>,
    restoredChannel: PreviewChannel?,
    requestEntryFocus: Boolean,
    onRowFocused: (FocusRequester) -> Unit,
    selectedChannelId: String,
    favoriteChannelIds: Set<String>,
    compactLayout: Boolean,
    onChannelSelected: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    val refreshState =
        channels.loadState.refresh

    if (
        refreshState is LoadState.Loading && channels.itemCount == 0
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Estamos conectando seus canais. Aguarde…",
                color = AppGoldLight,
                fontSize =
                    if (compactLayout) 12.sp else 14.sp
            )
        }
        return
    }

    if (
        refreshState is LoadState.Error &&
        channels.itemCount == 0
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Não foi possível carregar os canais.",
                color = kidsCinemaInk(0xFFB7B7BD),
                fontSize =
                    if (compactLayout) 12.sp else 14.sp
            )
        }
        return
    }

    if (
        refreshState is LoadState.NotLoading &&
        channels.itemCount == 0
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhum canal nesta categoria.",
                color = kidsCinemaInk(0xFFB7B7BD),
                fontSize =
                    if (compactLayout) 12.sp else 14.sp
            )
        }
        return
    }

    var entryFocused by remember { mutableStateOf(false) }
    val snapshot = channels.itemSnapshotList
    val entryId = remember {
        restoredChannel?.id?.takeIf { it == selectedChannelId }
            ?: snapshot.items.firstOrNull { it.id == selectedChannelId }?.id
            ?: snapshot.items.firstOrNull()?.id.orEmpty()
    }
    val entryIndex = snapshot.items.indexOfFirst { it.id == entryId }
        .let { if (it < 0) 0 else it + snapshot.placeholdersBefore }
    // The saved channel can be outside the first loaded page. Keep a small local
    // entry rather than fetching every preceding page just to recover focus.
    val savedEntry = remember { restoredChannel?.takeIf { it.id == entryId && snapshot.items.none { row -> row.id == entryId } } }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(if (savedEntry != null) 0 else entryIndex)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.spacedBy(
                if (compactLayout) 5.dp else 8.dp
            )
    ) {
        if (savedEntry != null) item(key = "restored_${savedEntry.id}") {
            ChannelRow(savedEntry, savedEntry.id == selectedChannelId, savedEntry.id in favoriteChannelIds,
                compactLayout, { onChannelSelected(savedEntry.id) }, { onFavoriteClick(savedEntry.id) },
                focusOnEntry = requestEntryFocus && !entryFocused, onRowFocused = { entryFocused = true; onRowFocused(it) })
        }
        items(
            count = snapshot.size,
            key = { index -> liveChannelSnapshotKey(snapshot, index) }
        ) { index ->
            // The old layout may finish while a smaller/new category is already presented.
            val currentSnapshot = channels.itemSnapshotList
            if (index !in currentSnapshot.indices) return@items
            val expected = snapshot.getOrNull(index)?.id
            if (expected != currentSnapshot.getOrNull(index)?.id) return@items
            val channel = channels[index] ?: return@items

            if (channel.id == savedEntry?.id) return@items
            val previewChannel =
                PreviewChannel(
                    id = channel.id,
                    number = index + 1,
                    name = channel.name,
                    categoryId = channel.categoryId,
                    logoUrl = channel.logoUrl,
                    streamUrl = channel.streamUrl
                )

            ChannelRow(
                channel = previewChannel,
                focusOnEntry = requestEntryFocus && !entryFocused && savedEntry == null && (channel.id == entryId || (entryId.isBlank() && index == 0)),
                onRowFocused = { entryFocused = true; onRowFocused(it) },
                isSelected =
                    channel.id == selectedChannelId,
                isFavorite =
                    channel.id in favoriteChannelIds,
                compactLayout = compactLayout,
                onSelect = {
                    onChannelSelected(
                        channel.id
                    )
                },
                onFavoriteClick = {
                    onFavoriteClick(
                        channel.id
                    )
                }
            )
        }

        if (
            channels.loadState.append is
                    LoadState.Loading
        ) {
            item(
                key = "__loading_more_channels__"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (compactLayout) {
                                42.dp
                            } else {
                                50.dp
                            }
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = "Carregando mais canais...",
                        color = AppGold,
                        fontSize =
                            if (compactLayout) {
                                11.sp
                            } else {
                                13.sp
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<PreviewChannel>,
    requestEntryFocus: Boolean,
    onRowFocused: (FocusRequester) -> Unit,
    selectedChannelId: String,
    favoriteChannelIds: Set<String>,
    compactLayout: Boolean,
    onChannelSelected: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    if (channels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhum canal nesta categoria.",
                color = kidsCinemaInk(0xFFB7B7BD),
                fontSize =
                    if (compactLayout) 12.sp else 14.sp
            )
        }
        return
    }

    var entryFocused by remember { mutableStateOf(false) }
    val entryId = remember { channels.firstOrNull { it.id == selectedChannelId }?.id ?: channels.first().id }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(channels.indexOfFirst { it.id == entryId }.coerceAtLeast(0))
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.spacedBy(
                if (compactLayout) 5.dp else 8.dp
            )
    ) {
        items(
            items = channels,
            key = { channel -> channel.id }
        ) { channel ->
            ChannelRow(
                channel = channel,
                focusOnEntry = requestEntryFocus && !entryFocused && channel.id == entryId,
                onRowFocused = { entryFocused = true; onRowFocused(it) },
                isSelected = channel.id == selectedChannelId,
                isFavorite = channel.id in favoriteChannelIds,
                compactLayout = compactLayout,
                onSelect = {
                    onChannelSelected(channel.id)
                },
                onFavoriteClick = {
                    onFavoriteClick(channel.id)
                }
            )
        }
    }
}

@Composable
private fun ChannelRow(
    channel: PreviewChannel,
    isSelected: Boolean,
    isFavorite: Boolean,
    compactLayout: Boolean,
    onSelect: () -> Unit,
    onFavoriteClick: () -> Unit,
    focusOnEntry: Boolean = false,
    onRowFocused: (FocusRequester) -> Unit = {}
) {
    val rowFocus = remember(channel.id) { FocusRequester() }
    var entryFocusRequested by remember(channel.id) { mutableStateOf(false) }
    LaunchedEffect(focusOnEntry) {
        if (focusOnEntry && !entryFocusRequested) {
            withFrameNanos { }
            rowFocus.requestFocus()
            entryFocusRequested = true
        }
    }
    var isFocused by remember {
        mutableStateOf(false)
    }

    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnFavoriteClick by rememberUpdatedState(onFavoriteClick)
    var selectKeyDownTime by remember(channel.id) { mutableLongStateOf(0L) }
    var selectKeyHeld by remember(channel.id) { mutableStateOf(false) }
    var keyboardFavoriteTriggered by remember(channel.id) { mutableStateOf(false) }
    var pointerFavoriteTriggered by remember(channel.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (compactLayout) 10.dp else 12.dp)

    // Like films: one action after a full second, independent of key-repeat timing.
    LaunchedEffect(selectKeyHeld, channel.id) {
        if (selectKeyHeld) {
            delay(FAVORITE_LONG_PRESS_MS)
            if (selectKeyHeld && !keyboardFavoriteTriggered &&
                movieHoldReady(SystemClock.elapsedRealtime() - selectKeyDownTime, FAVORITE_LONG_PRESS_MS, isFocused, false)) {
                keyboardFavoriteTriggered = true
                currentOnFavoriteClick()
            }
        }
    }
    val ownerView = LocalView.current
    DisposableEffect(ownerView, channel.id) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) { selectKeyHeld = false; selectKeyDownTime = 0L }
        }
        val observer = ownerView.viewTreeObserver
        observer.addOnWindowFocusChangeListener(listener)
        onDispose { if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener) }
    }

    Row(
        modifier = Modifier
            .focusRequester(rowFocus)
            .fillMaxWidth()
            .height(
                if (compactLayout) 42.dp else 50.dp
            )
            .scale(
                if (isFocused) 1.015f else 1f
            )
            .then(if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) Modifier.clip(shape) else Modifier)
            .background(
                color = when {
                    isFocused ->
                        (if(KidsAppearance.active) kidsCinemaSelectionColors().first() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor())

                    isSelected ->
                        (if(KidsAppearance.active) kidsCinemaSelectionColors().first() else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor())

                    else ->
                        (if(KidsAppearance.active) Color.Transparent else br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x60111419))
                },
                shape = shape
            )
            .kidsCinemaBorder(
                border = BorderStroke(
                    width =
                        if (isFocused || isSelected) {
                            2.dp
                        } else {
                            1.dp
                        },
                    brush =
                        if (isFocused || isSelected) {
                            MetallicGoldBrush
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF3B3B40),
                                    br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF3B3B40)
                                )
                            )
                        }
                ),
                shape = shape
            )
            .kidsCinemaFocus(isFocused,isSelected)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) onRowFocused(rowFocus)
                if (!isFocused) { selectKeyHeld = false; selectKeyDownTime = 0L }
            }
            .onPreviewKeyEvent { event ->
                val selectKey = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                if (!selectKey) false else when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (!selectKeyHeld && event.nativeKeyEvent.repeatCount == 0) {
                            selectKeyDownTime = SystemClock.elapsedRealtime()
                            keyboardFavoriteTriggered = false
                            selectKeyHeld = true
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (selectKeyHeld && !event.nativeKeyEvent.isCanceled) {
                            val elapsed = SystemClock.elapsedRealtime() - selectKeyDownTime
                            if (keyboardFavoriteTriggered || movieHoldReady(elapsed, FAVORITE_LONG_PRESS_MS, isFocused, false)) {
                                if (!keyboardFavoriteTriggered) {
                                    keyboardFavoriteTriggered = true
                                    currentOnFavoriteClick()
                                }
                            } else currentOnSelect()
                        }
                        selectKeyHeld = false
                        selectKeyDownTime = 0L
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(channel.id) {
                detectTapGestures(
                    onPress = {
                        pointerFavoriteTriggered = false
                        val released = withTimeoutOrNull(FAVORITE_LONG_PRESS_MS) { tryAwaitRelease() }
                        if (released == null) {
                            pointerFavoriteTriggered = true
                            currentOnFavoriteClick()
                            tryAwaitRelease()
                        }
                    },
                    onTap = {
                        if (!pointerFavoriteTriggered) currentOnSelect()
                        pointerFavoriteTriggered = false
                    }
                )
            }
            .semantics {
                role = Role.Button
                onClick(label = "Selecionar canal") { currentOnSelect(); true }
                onLongClick(label = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos") {
                    currentOnFavoriteClick(); true
                }
            }
            .focusable()
            .padding(
                horizontal =
                    if (compactLayout) 10.dp else 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if(!KidsAppearance.active) {

        Text(
            text = channel.number.toString(),
            modifier = Modifier.width(
                if (compactLayout) 30.dp else 38.dp
            ),
            color = kidsCinemaInk(0xFFB7B7BD),
            fontSize =
                if (compactLayout) 11.sp else 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        /*
         * O logo do canal virá de channel.logoUrl quando
         * adicionarmos o carregamento remoto de imagens.
         *
         * Se a lista não fornecer logo, nada é inventado:
         * mostramos somente o nome do canal.
         */
        }
        Text(
            text = channel.name,
            modifier = Modifier.weight(1f),
            color =
                if (isFocused || isSelected) {
                    kidsCinemaRowInk(true,0xFFFFFFFF)
                } else {
                    kidsCinemaInk(0xFFF5F5F5)
                },
            fontSize =
                if (compactLayout) 12.sp else 14.sp,
            fontWeight =
                if (isFocused || isSelected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            FavoriteHeart(
                isFavorite = isFavorite,
                selected = isFocused || isSelected,
                size = if (compactLayout) 16 else 18
            )
        }
    }
}

@Composable
private fun EmptyChannelPreview(
    compactLayout: Boolean,
    height: androidx.compose.ui.unit.Dp?=null
) {
    val shape =
        RoundedCornerShape(
            if (compactLayout) 8.dp else 12.dp
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                height ?: if (compactLayout) 168.dp else 250.dp
            )
            .background(
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF111113),
                shape = shape
            )
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF4A4A50)
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nenhum canal disponível.",
            color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
            fontSize =
                if (compactLayout) 12.sp else 14.sp
        )
    }
}

/**
 * Prévia ampla 16:9: o vídeo ocupa toda a área reservada pelo layout,
 * sem rótulos, horários ou informações desenhadas sobre a imagem.
 * FIT contém a superfície nativa na área reservada, sem cortar ou transbordar.
 * A camada única de vídeo usa estes limites; expandir não troca o player.
 */
@Composable
private fun ChannelPreview(
    compactLayout: Boolean,
    playbackError: String?,
    centerMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    previewHeight: androidx.compose.ui.unit.Dp,
    surfaceFocused: Boolean,
    onVideoBounds: (Rect)->Unit
) {
    val isFocused=surfaceFocused
    val outerShape = RoundedCornerShape(6.dp)
    val innerShape = RoundedCornerShape(4.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .background(
                    color = br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF0B0B10),
                    shape = outerShape
                )
                .border(
                    border = BorderStroke(
                        width = if (isFocused) 2.5.dp else 2.dp,
                        brush = if(KidsAppearance.active) androidx.compose.ui.graphics.SolidColor(if(isFocused) Color(0xFFE4B45C) else Color(0xFF202631)) else if(isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBorder else NeutralLiveBorder
                    ),
                    shape = outerShape
                )
                .then(if(KidsAppearance.active) Modifier else Modifier.kidsFocus(isFocused,fill=false))
                .padding(if (compactLayout) 5.dp else 7.dp)
                .premiumGlow(isFocused)

        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(innerShape)
                    .background(Color.Black)
            ) {
                Box(Modifier.fillMaxSize().onGloballyPositioned {onVideoBounds(it.boundsInWindow())},contentAlignment=Alignment.Center) {
                    if(centerMessage!=null)Text(centerMessage,color=Color.White,fontSize=14.sp,
                        textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                        modifier=Modifier.padding(16.dp).then(if(onRetry!=null)Modifier.clickable(onClick=onRetry)else Modifier))
                }
            }


        }

        if (playbackError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = playbackError,
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                fontSize = if (compactLayout) 11.sp else 13.sp
            )
        }
    }
}

/** Horarios locais apresentados ao usuario a partir dos timestamps EPG. */
private fun formatEpgClock(epochMs: Long): String {
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR")))
}

@Composable
private fun GuideProgramRow(
    program: GuideProgram,
    compactLayout: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(if (compactLayout) 12.dp else 14.dp)
    val live = program.isLive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = if (live || isFocused) { br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBrush() } else {
                    Brush.verticalGradient(
                        listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF1A1B1E), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF0C0E13))
                    )
                },
                shape = shape
            )
            .premiumGlow(live || isFocused)
            .border(
                border = BorderStroke(
                    width = if (live || isFocused) 2.dp else 1.dp,
                    brush = if (live || isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder) else NeutralLiveBorder
                ),
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(
                horizontal = if (compactLayout) 12.dp else 16.dp,
                vertical = if (compactLayout) 10.dp else 12.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (compactLayout) 12.dp else 15.dp)
                    .background(
                        color = if (live) AppGold else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
                        shape = RoundedCornerShape(50)
                    )
            )

            Spacer(modifier = Modifier.width(if (compactLayout) 10.dp else 14.dp))

            Text(
                text = program.title,
                modifier = Modifier.weight(1f),
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
                fontSize = if (compactLayout) 12.sp else 14.sp,
                fontWeight = if (live) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "${program.startTime} • ${program.endTime}",
                color = if (live) (if (isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else AppGoldLight) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                fontSize = if (compactLayout) 11.sp else 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (live) {
            Spacer(modifier = Modifier.height(if (compactLayout) 8.dp else 10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compactLayout) 10.dp else 12.dp)
                ) {
                    val currentProgress = program.progressFraction.coerceIn(0f, 1f)
                    val thumbSize = if (compactLayout) 10.dp else 12.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compactLayout) 6.dp else 8.dp)
                            .align(Alignment.CenterStart)
                            .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFF383127), RoundedCornerShape(50))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(currentProgress)
                            .height(if (compactLayout) 6.dp else 8.dp)
                            .align(Alignment.CenterStart)
                            .background(PremiumProgressGold, RoundedCornerShape(50))
                    )
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((maxWidth - thumbSize) * currentProgress)
                                    .coerceIn(0.dp, maxWidth)
                            )
                            .size(thumbSize)
                            .align(Alignment.CenterStart)
                            .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFF2C1), RoundedCornerShape(50))
                    )
                }

                Spacer(modifier = Modifier.width(if (compactLayout) 8.dp else 10.dp))

                Text(
                    text = "${(program.progressFraction * 100f).toInt()}% exibido",
                    color = (if (isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else AppGoldLight),
                    fontSize = if (compactLayout) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DayGuideColumn(
    modifier: Modifier = Modifier,
    selectedDayIndex: Int,
    compactLayout: Boolean,
    onDayClick: (Int) -> Unit
) {
    val locale = remember {
        Locale("pt", "BR")
    }

    val dayFormatter = remember {
        DateTimeFormatter.ofPattern("EEE", locale)
    }

    val dates = remember {
        val today = LocalDate.now()
        (-3..3).map { offset ->
            today.plusDays(offset.toLong())
        }
    }

    Column(
        modifier = modifier
            .background(
                brush = PremiumPanelBackground,
                shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
            )
            .border(
                border = BorderStroke(
                    width = 1.3.dp,
                    brush = NeutralLiveBorder
                ),
                shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
            )
            .padding(if (compactLayout) 8.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DIAS",
            color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
            fontSize = if (compactLayout) 11.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        dates.forEachIndexed { index, date ->
            DayButton(
                dayNumber = date.dayOfMonth,
                dayLabel = date
                    .format(dayFormatter)
                    .replace(".", "")
                    .replaceFirstChar {
                        it.uppercase()
                    },
                isSelected = index == selectedDayIndex,
                compactLayout = compactLayout,
                onClick = {
                    onDayClick(index)
                }
            )
        }
    }
}

@Composable
private fun DayButton(
    dayNumber: Int,
    dayLabel: String,
    isSelected: Boolean,
    compactLayout: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val shape =
        RoundedCornerShape(
            if (compactLayout) 7.dp else 9.dp
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                if (compactLayout) 54.dp else 66.dp
            )
            .background(
                brush = if (isFocused || isSelected) { br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBrush() } else {
                    Brush.verticalGradient(
                        listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10))
                    )
                },
                shape = shape
            )
            .premiumGlow(isFocused || isSelected)
            .border(
                border = BorderStroke(
                    width =
                        if (isFocused || isSelected) {
                            2.dp
                        } else {
                            1.dp
                        },
                    brush =
                        if (isFocused || isSelected) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder) else NeutralLiveBorder
                ),
                shape = shape
            )
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .clickable(
                onClick = onClick
            )
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber.toString(),
            color =
                if (isFocused || isSelected) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)
                } else {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5)
                },
            fontSize =
                if (compactLayout) 18.sp else 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = dayLabel,
            color =
                if (isFocused || isSelected) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)
                } else {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD)
                },
            fontSize =
                if (compactLayout) 9.sp else 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FavoriteHeart(
    isFavorite: Boolean,
    selected: Boolean=false,
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
                color = if(KidsAppearance.active) Color(0xFFE9B94D) else AppGold
            )
        } else {
            drawPath(
                path = path,
                color = kidsCinemaRowInk(selected,0xFFF5F5F5),
                style = Stroke(
                    width = 1.6.dp.toPx()
                )
            )
        }
    }
}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_POLISH_LOADING_HOLD_V1

// IPTV_LISTS_SUBTITLES_CHANNELS_V1

// Text retains the approved palette; blue darkening applies to rims.
private val MetallicGoldBrushText get() = Brush.linearGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFF7A5215), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFE6A0), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFD6A32E), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFFFFF5CD), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFF926315))
)
