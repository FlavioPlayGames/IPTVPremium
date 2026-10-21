package br.com.flaviotorres.iptvpremium.ui.live

import br.com.flaviotorres.iptvpremium.ui.kids.*
import android.os.SystemClock
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

private const val FAVORITE_LONG_PRESS_MS = 600L
private const val FAVORITE_ACTION_DEBOUNCE_MS = 800L
private const val SUPPRESS_CLICK_AFTER_LONG_PRESS_MS = 250L

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

private val PremiumPanelBackground get() = br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.panelBrush

private val PremiumProgressGold get() = Brush.horizontalGradient(
    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFC58B17), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFF8D863), br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFFFFEEAC))
)

// Brilho sutil aplicado apenas a itens destacados; não afeta o Media3.
private fun Modifier.premiumGlow(enabled: Boolean, roundedDp: Float = 12f): Modifier = this


private fun Modifier.premiumCategoryCardBorder(
    highlighted: Boolean,
    roundedDp: Float = 12f
): Modifier = if (br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.regular) {
    if (!highlighted) this else this.border(1.dp, br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBorder, RoundedCornerShape(roundedDp.dp))
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


private enum class ChannelCategoryKind {
    ALL,
    RECENT,
    FAVORITES,
    SOURCE
}

private data class ChannelCategory(
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
        title = "Assistidos recentemente",
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

private data class PreviewChannel(
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
    playlistContent: PlaylistContent? = null,
    pagedChannels: LazyPagingItems<Channel>? = null,
    pagingCategories: List<Category> = emptyList(),
    totalChannelCount: Int = 0,
    pagingCategoryCounts: Map<String?, Int> = emptyMap(),
    onCategoryAccessRequested: (String?, String, () -> Unit) -> Unit = { _, _, proceed -> proceed() },
    onSourceCategorySelected: (String?) -> Unit = {},
    // Quando conectado pelo AppNavigation, abre o canal no player principal.
    // Sem callback, preserva a tela cheia Media3 local já existente.
    onPlayChannel: ((String) -> Unit)? = null,
    // Recebe a programação real do provedor, por ID do canal selecionado.
    // A navegação conectará a consulta após a compilação desta tela.
    onLoadChannelEpg: (suspend (String) -> List<PlaybackEpgProgram>)? = null,
    epgSourceKey: String? = null
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
                FavoriteChannelsCategory
            ) + sourceCategories
        }

    var selectedCategoryId by remember {
        mutableStateOf(AllChannelsCategory.id)
    }

    val selectedCategory =
        channelCategories.firstOrNull {
            it.id == selectedCategoryId
        } ?: AllChannelsCategory

    var leftPaneMode by remember {
        mutableStateOf(LeftPaneMode.CATEGORIES)
    }

    var selectedChannelId by remember {
        mutableStateOf("")
    }

    var favoriteChannelIds by remember {
        mutableStateOf(setOf<String>())
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
        leftPaneMode = LeftPaneMode.CATEGORIES
    }

    BackHandler(
        enabled = searchKeyboardVisible
    ) {
        searchKeyboardVisible = false
    }

    LaunchedEffect(previewChannels) {
        if (
            selectedChannelId.isBlank() ||
            previewChannels.none {
                it.id == selectedChannelId
            }
        ) {
            selectedChannelId =
                previewChannels.firstOrNull()
                    ?.id
                    .orEmpty()
        }
    }

    // As transformações locais não devem reexecutar quando apenas o
    // controle remoto, relógio ou foco provoca uma recomposição.
    val baseChannels =
        remember(previewChannels, selectedCategory, favoriteChannelIds) {
            when (selectedCategory.kind) {
                ChannelCategoryKind.ALL ->
                    previewChannels

                ChannelCategoryKind.RECENT ->
                    previewChannels.take(4)

                ChannelCategoryKind.FAVORITES ->
                    previewChannels.filter { channel ->
                        channel.id in favoriteChannelIds
                    }

                ChannelCategoryKind.SOURCE ->
                    previewChannels.filter { channel ->
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
        remember(previewChannels, selectedChannelId, displayedChannels) {
            previewChannels.firstOrNull {
                it.id == selectedChannelId
            } ?: displayedChannels.firstOrNull()
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
            usingPaging
        ) {
            channelCategories.associateWith { category ->
                when (category.kind) {
                    ChannelCategoryKind.ALL ->
                        effectiveTotalChannelCount

                    ChannelCategoryKind.RECENT ->
                        minOf(
                            4,
                            effectiveTotalChannelCount
                        )

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
    val usePagedChannelList =
        usingPaging &&
                normalizedSearch.isBlank() &&
                sortMode == ChannelSortMode.DEFAULT &&
                (
                        selectedCategory.kind == ChannelCategoryKind.ALL ||
                                selectedCategory.kind == ChannelCategoryKind.SOURCE
                        )

    // Player único para prévia e tela cheia: não duplica decodificação nem áudio.
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val livePlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
    var liveIsBuffering by remember { mutableStateOf(false) }
    var liveError by remember { mutableStateOf<String?>(null) }
    var isLiveFullScreen by remember { mutableStateOf(false) }

    // Libera o decodificador ao sair da tela e interrompe a reprodução
    // quando a Activity deixa de estar visível.
    val latestPreviewChannel by rememberUpdatedState(selectedChannel)
    val latestProtectionNames by rememberUpdatedState(categoryNamesForProtection)
    val latestProtectionPlaylist by rememberUpdatedState(epgSourceKey)
    DisposableEffect(livePlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                liveIsBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                // Nunca exibir a mensagem bruta: a URL pode conter credenciais Xtream.
                liveIsBuffering = false
                liveError = "Este canal não pôde ser reproduzido. Tente outro canal."
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> livePlayer.pause()
                Lifecycle.Event.ON_START -> {
                    val protectedChannel = latestPreviewChannel
                    val allowed = protectedChannel != null && !br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access()
                        .blocked(latestProtectionPlaylist.orEmpty(), "LIVE_TV", protectedChannel.categoryId,
                            protectedChannel.name, latestProtectionNames[protectedChannel.categoryId].orEmpty())
                    if (allowed && livePlayer.mediaItemCount > 0 && liveError == null) {
                        livePlayer.play()
                    }
                }
                else -> Unit
            }
        }
        livePlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            livePlayer.removeListener(listener)
            livePlayer.release()
        }
    }

    // Selecionar um canal troca a mídia no mesmo player.
    // O endereço do stream não passa pela rota de navegação nem pelos logs.
    LaunchedEffect(selectedChannel?.id, selectedChannel?.streamUrl) {
        val url = selectedChannel?.streamUrl?.trim().orEmpty()
        liveError = null
        liveIsBuffering = false
        livePlayer.stop()
        livePlayer.clearMediaItems()
        if (url.isBlank()) {
            if (selectedChannel != null) {
                liveError = "Este canal não possui endereço de reprodução."
            }
        } else {
            // Não deixar exceções de uma URL inválida derrubarem a tela.
            try {
                liveIsBuffering = true
                val path = Uri.parse(url).path.orEmpty().lowercase(Locale.ROOT)
                val item = MediaItem.Builder()
                    .setUri(url)
                    .apply {
                        when {
                            path.endsWith(".m3u8") -> setMimeType(MimeTypes.APPLICATION_M3U8)
                            path.endsWith(".mpd") -> setMimeType(MimeTypes.APPLICATION_MPD)
                        }
                    }
                    .build()
                livePlayer.setMediaItem(item)
                livePlayer.prepare()
                livePlayer.play()
            } catch (_: Exception) {
                liveIsBuffering = false
                liveError = "Não foi possível abrir este canal."
            }
        }
    }

    BackHandler(enabled = isLiveFullScreen) {
        isLiveFullScreen = false
    }

    BoxWithConstraints(
        modifier = Modifier
            .catalogEscapeBack()
            .fillMaxSize()
            .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF070708))
    ) {
        val compactLayout =
            maxHeight < 650.dp ||
                    maxWidth < 1200.dp

        val outerPadding =
            if (compactLayout) 10.dp else 18.dp

        val leftPaneWidth =
            if (compactLayout) 320.dp else 390.dp

        val rightDayWidth =
            if (compactLayout) 66.dp else 82.dp

        val horizontalGap =
            if (compactLayout) 12.dp else 18.dp

        val topBarHeight =
            if (compactLayout) 44.dp else 54.dp

        br.com.flaviotorres.iptvpremium.ui.components.CatalogThemeBackdrop(Modifier.matchParentSize())

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding),
            horizontalArrangement = Arrangement.spacedBy(horizontalGap)
        ) {
            /*
             * COLUNA ESQUERDA
             */
            Column(
                modifier = Modifier
                    .width(leftPaneWidth)
                    .fillMaxHeight()
                    .background(
                        brush = PremiumPanelBackground,
                        shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
                    )
                    .border(
                        border = BorderStroke(
                            width = 1.3.dp,
                            brush = PremiumGoldBorder
                        ),
                        shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
                    )
                    .kidsPanel()
                    .padding(if (compactLayout) 10.dp else 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (KidsAppearance.active) { if(compactLayout)128.dp else 154.dp } else topBarHeight)
                        .clickable(
                            enabled = leftPaneMode == LeftPaneMode.CHANNELS,
                            onClick = {
                                leftPaneMode = LeftPaneMode.CATEGORIES
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (KidsAppearance.active) {
                        KidsCatalogHeader(compactLayout, if(leftPaneMode == LeftPaneMode.CATEGORIES) "MINHAS AVENTURAS"
                            else "‹ " + cleanCategoryDisplayName(selectedCategory.title))
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
                                    "‹  ${cleanCategoryDisplayName(selectedCategory.title)}"
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

                PremiumOrnamentDivider(compactLayout = compactLayout)

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
                        categories = channelCategories,
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

                            val channelsInCategory =
                                when (category.kind) {
                                    ChannelCategoryKind.ALL ->
                                        previewChannels

                                    ChannelCategoryKind.RECENT ->
                                        previewChannels.take(4)

                                    ChannelCategoryKind.FAVORITES ->
                                        previewChannels.filter {
                                            it.id in favoriteChannelIds
                                        }

                                    ChannelCategoryKind.SOURCE ->
                                        previewChannels.filter {
                                            it.categoryId ==
                                                    category.sourceCategoryId
                                        }
                                }

                            selectedChannelId =
                                channelsInCategory
                                    .firstOrNull()
                                    ?.id
                                    .orEmpty()

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
                        PagedChannelList(
                            channels = pagedChannels,
                            selectedChannelId = selectedChannelId,
                            favoriteChannelIds = favoriteChannelIds,
                            compactLayout = compactLayout,
                            onChannelSelected = { channelId ->
                                selectedChannelId = channelId
                            },
                            onFavoriteClick = { channelId ->
                                favoriteChannelIds =
                                    if (
                                        channelId in
                                        favoriteChannelIds
                                    ) {
                                        favoriteChannelIds -
                                                channelId
                                    } else {
                                        favoriteChannelIds +
                                                channelId
                                    }
                            }
                        )
                    } else {
                        ChannelList(
                            channels = displayedChannels,
                            selectedChannelId = selectedChannelId,
                            favoriteChannelIds = favoriteChannelIds,
                            compactLayout = compactLayout,
                            onChannelSelected = { channelId ->
                                selectedChannelId = channelId
                            },
                            onFavoriteClick = { channelId ->
                                favoriteChannelIds =
                                    if (
                                        channelId in
                                        favoriteChannelIds
                                    ) {
                                        favoriteChannelIds -
                                                channelId
                                    } else {
                                        favoriteChannelIds +
                                                channelId
                                    }
                            }
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
                    // A largura continua inteira; o Media3 usa ZOOM apenas na prévia,
                    // preenchendo o quadro sem distorcer o vídeo ou criar barras pretas.
                    val idealPreviewHeight = maxWidth * (9f / 16f)
                    val reducedPreviewHeight = idealPreviewHeight * 0.85f
                    val minimumGuideHeight = if (compactLayout) 132.dp else 178.dp
                    val previewHeight = if(KidsAppearance.active) (maxHeight - if(liveError!=null)36.dp else 0.dp).coerceAtLeast(1.dp) else minOf(
                        reducedPreviewHeight,
                        (maxHeight - minimumGuideHeight).coerceAtLeast(120.dp)
                    )

                    if (selectedChannel != null && !isLiveFullScreen) {
                        ChannelPreview(
                            channel = selectedChannel,
                            compactLayout = compactLayout,
                            player = livePlayer,
                            isBuffering = liveIsBuffering,
                            playbackError = liveError,
                            currentProgramTime = currentEpgTimeRange,
                            previewHeight = previewHeight,
                            onFullScreen = {
                                val channelId = selectedChannel.id
                                if (onPlayChannel != null && channelId.isNotBlank()) {
                                    onPlayChannel(channelId)
                                } else {
                                    isLiveFullScreen = true
                                }
                            }
                        )
                    } else {
                        EmptyChannelPreview(compactLayout = compactLayout, height=if(KidsAppearance.active)previewHeight else null)
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
                        .border(
                            border = BorderStroke(
                                width = 1.3.dp,
                                brush = PremiumGoldBorder
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

                    PremiumOrnamentDivider(compactLayout = compactLayout)

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
                    modifier = Modifier.fillMaxWidth(),
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

        // O mesmo ExoPlayer é reaproveitado na tela cheia: a transmissão
        // continua em andamento, apenas o destino de vídeo muda.
        if (isLiveFullScreen && selectedChannel != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                LiveVideoSurface(
                    player = livePlayer,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(20.dp)
                        .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xC9141416), RoundedCornerShape(10.dp))
                        .clickable { isLiveFullScreen = false }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹  Voltar",
                        color = AppGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(18.dp))
                    Text(
                        text = selectedChannel.name,
                        color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (liveIsBuffering || liveError != null) {
                    Text(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        text = liveError ?: "Carregando canal ao vivo...",
                        color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF),
                        fontSize = 15.sp
                    )
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
            .fillMaxWidth()
            .height(height)
            .background(
                brush = PremiumPanelBackground,
                shape = RoundedCornerShape(if (compactLayout) 16.dp else 20.dp)
            )
            .border(
                border = BorderStroke(
                    width = 1.25.dp,
                    brush = PremiumGoldBorder
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
            onClick = onSortClick
        )

        ChannelSearchBar(
            modifier = Modifier.weight(1f),
            query = searchQuery,
            active = searchActive,
            cursorPosition = searchCursorPosition,
            compactLayout = compactLayout,
            onClick = onSearchClick,
            onClear = onSearchClear
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
    onClick: () -> Unit
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
                brush = if (isFocused) { br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionBrush() } else {
                    Brush.verticalGradient(listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10)))
                },
                shape = shape
            )
            .premiumGlow(isFocused)
            .border(BorderStroke(if (isFocused) 1.5.dp else 1.dp, br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(isFocused, PremiumGoldBorder)), shape)
            .kidsFocus(isFocused)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                val selectKey = event.key == Key.DirectionCenter ||
                        event.key == Key.Enter || event.key == Key.NumPadEnter
                if (selectKey && event.type == KeyEventType.KeyUp) {
                    onClick()
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = if (compactLayout) 9.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 5.dp else 8.dp)
    ) {
        Text(
            text = iconGlyph,
            color = if (isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF),
            fontSize = if (compactLayout) 12.sp else 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            color = if (isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF),
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
    onClear: () -> Unit
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
                    listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10))
                ),
                shape = shape
            )
            .border(
                border = BorderStroke(
                    width = if (highlighted) 2.dp else 1.2.dp,
                    brush = if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder) else Brush.linearGradient(
                        listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF9A7A39), Color(0xFF444046))
                    )
                ),
                shape = shape
            )
            .premiumGlow(highlighted)
            .kidsFocus(isFocused,active)
            .onFocusChanged {
                isFocused = it.isFocused
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
            .clickable(onClick = onClick)
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
                color = if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF),
                fontSize = if (compactLayout) 17.sp else 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        TvKeyboardSearchText(
            textColor = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
            placeholderColor = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFF909096),
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
                        color = if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor() else br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x2218181D),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = (if (active || isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else AppGoldLight),
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = currentTime.format(dayFormatter).replaceFirstChar { it.uppercase() },
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
                fontSize = if (compactLayout) 11.sp else 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = currentTime.format(dateFormatter),
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
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
        Regex("^Canais\\s*\\|\\s*", RegexOption.IGNORE_CASE),
        ""
    ).trim()
}

@Composable
private fun CategoryList(
    categories: List<ChannelCategory>,
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
        modifier = Modifier.fillMaxSize(),
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
                focus = if (category.id == categories.getOrNull(selectedIndex)?.id) selectedFocus else null,
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
    title: String,
    count: Int,
    isSelected: Boolean,
    compactLayout: Boolean,
    onSelect: () -> Unit,
    focus: FocusRequester? = null
) {
    LaunchedEffect(focus) {
        if (focus != null) {
            androidx.compose.runtime.withFrameNanos { }
            focus.requestFocus()
        }
    }
    var isFocused by remember {
        mutableStateOf(false)
    }

    val shape = RoundedCornerShape(if (compactLayout) 10.dp else 12.dp)
    val highlighted = isFocused || isSelected

    Row(
        modifier = Modifier
            .then(if (focus != null) Modifier.focusRequester(focus) else Modifier)
            .fillMaxWidth()
            .height(if (compactLayout) 40.dp else 46.dp)
            .background(
                brush = Brush.horizontalGradient(
                    if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColors()
                    else listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x50111419), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x600B0C10))
                ),
                shape = shape
            )
            .premiumGlow(highlighted)
            .premiumCategoryCardBorder(
                highlighted = highlighted,
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

                if (selectKey && event.type == KeyEventType.KeyUp) {
                    onSelect()
                    true
                } else {
                    false
                }
            }
            .clickable(onClick = onSelect)
            .focusable()
            .padding(horizontal = if (compactLayout) 12.dp else 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (KidsAppearance.active) {
            KidsCategoryIcon(title)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
            fontSize = if (compactLayout) 12.sp else 14.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = count.toString(),
            color = if (highlighted) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF) else br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
            fontSize = if (compactLayout) 11.sp else 13.sp
        )
    }
}

@Composable
private fun PagedChannelList(
    channels: LazyPagingItems<Channel>,
    selectedChannelId: String,
    favoriteChannelIds: Set<String>,
    compactLayout: Boolean,
    onChannelSelected: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    val refreshState =
        channels.loadState.refresh

    if (
        refreshState is LoadState.Loading &&
        channels.itemCount == 0
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Carregando canais...",
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
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
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
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                fontSize =
                    if (compactLayout) 12.sp else 14.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.spacedBy(
                if (compactLayout) 5.dp else 8.dp
            )
    ) {
        items(
            count = channels.itemCount,
            key = { index ->
                channels.peek(index)
                    ?.id
                    ?: "channel_placeholder_$index"
            }
        ) { index ->
            val channel =
                channels[index]
                    ?: return@items

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
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
                fontSize =
                    if (compactLayout) 12.sp else 14.sp
            )
        }
        return
    }

    LazyColumn(
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
    onFavoriteClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    var selectKeyDownTime by remember {
        mutableLongStateOf(0L)
    }

    var keyboardLongPressDetected by remember {
        mutableStateOf(false)
    }

    var suppressSelectUntil by remember {
        mutableLongStateOf(0L)
    }

    var lastFavoriteActionTime by remember {
        mutableLongStateOf(0L)
    }

    val shape =
        RoundedCornerShape(
            if (compactLayout) 10.dp else 12.dp
        )

    fun performFavoriteAction() {
        val now =
            SystemClock.elapsedRealtime()

        if (
            lastFavoriteActionTime == 0L ||
            now - lastFavoriteActionTime >=
            FAVORITE_ACTION_DEBOUNCE_MS
        ) {
            lastFavoriteActionTime = now
            onFavoriteClick()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                if (compactLayout) 42.dp else 50.dp
            )
            .scale(
                if (isFocused) 1.015f else 1f
            )
            .background(
                color = when {
                    isFocused ->
                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor()

                    isSelected ->
                        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.selectionColor()

                    else ->
                        br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x60111419)
                },
                shape = shape
            )
            .border(
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
            .kidsFocus(isFocused,isSelected)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused

                if (!focusState.isFocused) {
                    selectKeyDownTime = 0L
                    keyboardLongPressDetected = false
                    suppressSelectUntil = 0L
                }
            }
            .onPreviewKeyEvent { event ->
                val selectKey =
                    event.key == Key.DirectionCenter ||
                            event.key == Key.Enter ||
                            event.key == Key.NumPadEnter

                if (!selectKey) {
                    false
                } else {
                    val now =
                        SystemClock.elapsedRealtime()

                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (
                                event.nativeKeyEvent.repeatCount == 0
                            ) {
                                selectKeyDownTime = now
                                keyboardLongPressDetected = false
                            } else if (
                                !keyboardLongPressDetected
                            ) {
                                keyboardLongPressDetected = true
                                performFavoriteAction()
                            }

                            false
                        }

                        KeyEventType.KeyUp -> {
                            val pressedDuration =
                                if (selectKeyDownTime > 0L) {
                                    now - selectKeyDownTime
                                } else {
                                    0L
                                }

                            val wasLongPress =
                                keyboardLongPressDetected ||
                                        pressedDuration >=
                                        FAVORITE_LONG_PRESS_MS

                            if (wasLongPress) {
                                if (!keyboardLongPressDetected) {
                                    performFavoriteAction()
                                }

                                suppressSelectUntil =
                                    now +
                                            SUPPRESS_CLICK_AFTER_LONG_PRESS_MS
                            }

                            selectKeyDownTime = 0L
                            keyboardLongPressDetected = false

                            false
                        }

                        else -> false
                    }
                }
            }
            .combinedClickable(
                onClick = {
                    if (
                        SystemClock.elapsedRealtime() >=
                        suppressSelectUntil
                    ) {
                        onSelect()
                    }
                },
                onLongClick = {
                    performFavoriteAction()
                },
                onLongClickLabel =
                    if (isFavorite) {
                        "Remover dos favoritos"
                    } else {
                        "Adicionar aos favoritos"
                    }
            )
            .padding(
                horizontal =
                    if (compactLayout) 10.dp else 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = channel.number.toString(),
            modifier = Modifier.width(
                if (compactLayout) 30.dp else 38.dp
            ),
            color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFB7B7BD),
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
        Text(
            text = channel.name,
            modifier = Modifier.weight(1f),
            color =
                if (isFocused || isSelected) {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF)
                } else {
                    br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5)
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
            modifier = Modifier
                .clickable(
                    onClick = onFavoriteClick
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            FavoriteHeart(
                isFavorite = isFavorite,
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LiveVideoSurface(
    player: Player,
    modifier: Modifier = Modifier,
    cropToFill: Boolean = false
) {
    // Somente a prévia usa ZOOM para ocupar a área inteira.
    // A tela cheia permanece em FIT, preservando o enquadramento original.
    val resizeMode = if (cropToFill) {
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    } else {
        AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                this.resizeMode = resizeMode
                this.player = player
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
            if (view.resizeMode != resizeMode) view.resizeMode = resizeMode
        },
        onRelease = { view ->
            view.player = null
        }
    )
}

/**
 * Prévia ampla 16:9: o vídeo ocupa toda a área reservada pelo layout,
 * sem rótulos, horários ou informações desenhadas sobre a imagem.
 * A prévia usa ZOOM apenas aqui para eliminar barras pretas internas.
 * OK / clique em qualquer ponto abre o canal no player principal.
 */
@Composable
private fun ChannelPreview(
    channel: PreviewChannel,
    compactLayout: Boolean,
    player: Player,
    isBuffering: Boolean,
    playbackError: String?,
    currentProgramTime: String?,
    previewHeight: androidx.compose.ui.unit.Dp,
    onFullScreen: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val outerShape = RoundedCornerShape(if (compactLayout) 14.dp else 18.dp)
    val innerShape = RoundedCornerShape(if (compactLayout) 10.dp else 14.dp)

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
                        brush = PremiumGoldBorder
                    ),
                    shape = outerShape
                )
                .kidsFocus(isFocused,fill=false)
                .padding(if (compactLayout) 5.dp else 7.dp)
                .premiumGlow(isFocused)
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(onClick = onFullScreen)
                .focusable()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(innerShape)
                    .background(Color.Black)
            ) {
                LiveVideoSurface(
                    player = player,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if(KidsAppearance.active)1f else 1.11f
                            scaleY = if(KidsAppearance.active)1f else 1.11f
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        },
                    cropToFill = !KidsAppearance.active
                )

                if (isBuffering) {
                    Text(
                        text = "Carregando canal ao vivo...",
                        color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFFFFFFF),
                        fontSize = if (compactLayout) 12.sp else 14.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0x88000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            PreviewFrameAccent(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(if (compactLayout) 4.dp else 6.dp)
            )
            PreviewFrameAccent(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(if (compactLayout) 4.dp else 6.dp),
                mirrorHorizontally = true
            )
            PreviewFrameAccent(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (compactLayout) 4.dp else 6.dp),
                mirrorVertically = true
            )
            PreviewFrameAccent(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(if (compactLayout) 4.dp else 6.dp),
                mirrorHorizontally = true,
                mirrorVertically = true
            )
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

@Composable
private fun PreviewFrameAccent(
    modifier: Modifier = Modifier,
    mirrorHorizontally: Boolean = false,
    mirrorVertically: Boolean = false
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = if (mirrorHorizontally) -1f else 1f
                scaleY = if (mirrorVertically) -1f else 1f
            }
    ) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.dp)
                .background(AppGoldLight, RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(20.dp)
                .background(AppGoldLight, RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .padding(start = 6.dp, top = 6.dp)
                .width(8.dp)
                .height(2.dp)
                .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFF7C6328), RoundedCornerShape(50))
        )
        Box(
            modifier = Modifier
                .padding(start = 6.dp, top = 6.dp)
                .width(2.dp)
                .height(8.dp)
                .background(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.color(0xFF7C6328), RoundedCornerShape(50))
        )
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
                    brush = if (live || isFocused) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder) else Brush.linearGradient(
                        listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF7D6532), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF33343B))
                    )
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
                    brush = PremiumGoldBorder
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
                        if (isFocused || isSelected) br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.outline(true, PremiumGoldBorder) else Brush.linearGradient(
                            listOf(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.borderColor(0xFF91723B), br.com.flaviotorres.iptvpremium.ui.components.CatalogGlass.surface(0xFF35353A))
                        )
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
                color = AppGold
            )
        } else {
            drawPath(
                path = path,
                color = br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.ink(0xFFF5F5F5),
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
