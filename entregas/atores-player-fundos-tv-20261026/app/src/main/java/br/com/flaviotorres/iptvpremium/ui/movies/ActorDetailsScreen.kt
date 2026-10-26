package br.com.flaviotorres.iptvpremium.ui.movies

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.data.remote.TmdbClientProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tela informativa do ator. Os creditos do TMDB NAO representam disponibilidade
 * na playlist IPTV: a vinculacao com o catalogo local e tarefa posterior.
 *
 * Integracao futura, sem alterar esta tela:
 * - em AppNavigation, navegar ao selecionar MovieCastMember.id numerico;
 * - passar onMovieSelected/onSeriesSelected APENAS quando houver correspondencia
 *   com um item real do catalogo local.
 *
 * Recurso necessario: res/drawable-nodpi/actor_cinematic_background.webp
 */

data class ActorFilmographyCredit(
    val tmdbId: Int,
    val mediaType: String, // "movie" ou "tv"
    val title: String,
    val year: String?,
    val posterUrl: String?,
    val character: String?
)

private data class ActorCreditDetail(
    val title: String,
    val titleLogoUrl: String?,
    val year: String?,
    val rating: Double?,
    val duration: String?,
    val genres: List<String>,
    val synopsis: String,
    val backdropUrl: String?,
    val trailerKey: String?
)

// A janela so e criada quando dados, fundo e logo ja foram preparados.
// As imagens opcionais podem ser nulas se a producao nao disponibiliza-las.
private data class ActorPreparedCredit(
    val credit: ActorFilmographyCredit,
    val detail: ActorCreditDetail,
    val backdrop: ImageBitmap?,
    val titleLogo: ImageBitmap?
)

private data class ActorProfile(
    val name: String,
    val birthDate: String?,
    val birthPlace: String?,
    val biography: String,
    val portraitUrl: String?,
    val movies: List<ActorFilmographyCredit>,
    val series: List<ActorFilmographyCredit>
)

private sealed interface ActorFetchState {
    data object Loading : ActorFetchState
    data class Success(val actor: ActorProfile) : ActorFetchState
    data class Error(val message: String) : ActorFetchState
}

// Mesmo reflexo dourado metalico usado nos detalhes dos filmes.
private val ActorMetallicGold get() = Brush.linearGradient(
    colors = listOf(
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF8A6414),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFD4AF37),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFE991),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFFFF4BD),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFFE3B83F),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.borderColor(0xFF9E761B)
    )
)
private val ActorText = Color(0xFFF2F0E8)
private val ActorMuted = Color(0xFFCECBC3)
private val ActorGoldOutline get() = BorderStroke(2.dp, ActorMetallicGold)
private val ActorShape = RoundedCornerShape(9.dp)

@Composable
fun ActorDetailsScreen(
    personTmdbId: String,
    initialName: String = "",
    initialProfileUrl: String? = null,
    onBackClick: () -> Unit,
    onMovieSelected: ((ActorFilmographyCredit) -> Unit)? = null,
    onSeriesSelected: ((ActorFilmographyCredit) -> Unit)? = null,
    // Vincular aos IDs REAIS do catalogo no AppNavigation; o TMDB nao identifica favoritos IPTV.
    isMovieFavorite: (ActorFilmographyCredit) -> Boolean = { false },
    onToggleMovieFavorite: ((ActorFilmographyCredit) -> Unit)? = null,
    // Permite resolver o ID verdadeiro do IPTV enquanto a previa e preparada.
    // Callback opcional para manter compatibilidade com a navegacao atual.
    onPrepareMoviePreview: (suspend (ActorFilmographyCredit) -> Unit)? = null,
    isSeriesFavorite: (ActorFilmographyCredit) -> Boolean = { false },
    onToggleSeriesFavorite: ((ActorFilmographyCredit) -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val personId = remember(personTmdbId) {
        personTmdbId.toIntOrNull()?.takeIf { it > 0 }
    }

    val fetchState by produceState<ActorFetchState>(
        initialValue = ActorFetchState.Loading,
        key1 = personId
    ) {
        value = if (personId == null) {
            ActorFetchState.Error("Identificador do ator indisponível no TMDB.")
        } else {
            try {
                ActorFetchState.Success(fetchActorProfile(personId))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ActorFetchState.Error(
                    "Não foi possível carregar os dados do ator. Confira sua conexão."
                )
            }
        }
    }

    val actor = (fetchState as? ActorFetchState.Success)?.actor
    var showFullBiography by remember(personTmdbId) { mutableStateOf(false) }
    var preparingCredit by remember(personTmdbId) {
        mutableStateOf<ActorFilmographyCredit?>(null)
    }
    var selectedPreview by remember(personTmdbId) {
        mutableStateOf<ActorPreparedCredit?>(null)
    }
    var showPreparingIndicator by remember(personTmdbId) { mutableStateOf(false) }

    // O primeiro clique apenas prepara os recursos. A janela so aparece
    // quando a consulta, o backdrop e a logo terminam (ou falham/expiram).
    // Nada disso bloqueia a thread principal nem a navegacao do controle.
    LaunchedEffect(personTmdbId, preparingCredit) {
        val credit = preparingCredit ?: return@LaunchedEffect
        try {
            val prepared = coroutineScope {
                // Resolve a correspondencia do filme com o catalogo em paralelo
                // a consulta de detalhes, sem procurar toda a filmografia.
                // Uma falha dessa busca NAO deve impedir a abertura da previa.
                val favoriteLookup = async {
                    if (credit.mediaType == "movie" && onPrepareMoviePreview != null) {
                        try {
                            withTimeoutOrNull(3_000L) {
                                onPrepareMoviePreview(credit)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // A previa segue informativa se o filme nao for localizado.
                        }
                    }
                }
                val detail = withTimeoutOrNull(15_000L) {
                    withContext(Dispatchers.IO) { fetchActorCreditDetail(credit) }
                } ?: error("Tempo esgotado ao consultar esta produção.")
                val backdropJob = async {
                    loadActorPreviewImage(context, detail.backdropUrl, 8_000L)
                        ?: if (detail.backdropUrl != credit.posterUrl) {
                            loadActorPreviewImage(context, credit.posterUrl, 3_000L)
                        } else null
                }
                val logoJob = async {
                    loadActorPreviewImage(context, detail.titleLogoUrl, 5_000L)
                }
                val result = ActorPreparedCredit(
                    credit = credit,
                    detail = detail,
                    backdrop = backdropJob.await(),
                    titleLogo = logoJob.await()
                )
                // A janela abre com o estado conhecido quando a busca termina.
                favoriteLookup.await()
                result
            }
            if (preparingCredit == credit) {
                selectedPreview = prepared
                preparingCredit = null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (preparingCredit == credit) {
                preparingCredit = null
                Toast.makeText(
                    context,
                    "Não foi possível preparar a prévia. Tente novamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Aviso discreto somente se a preparação levar mais de 350 ms.
    LaunchedEffect(preparingCredit) {
        showPreparingIndicator = false
        if (preparingCredit != null) {
            delay(350L)
            showPreparingIndicator = true
        }
    }

    // Ao descer pelo controle remoto, sempre entrar no PRIMEIRO card da fileira.
    // Apos percorrer uma fileira na horizontal, voltamos ao inicio antes do foco.
    val moviesFirstFocus = remember(personTmdbId) { FocusRequester() }
    val seriesFirstFocus = remember(personTmdbId) { FocusRequester() }
    val moviesListState = rememberLazyListState()
    val seriesListState = rememberLazyListState()
    val focusScope = rememberCoroutineScope()

    val focusFirstMovie: () -> Unit = {
        if (!actor?.movies.isNullOrEmpty()) {
            focusScope.launch {
                moviesListState.scrollToItem(0)
                withFrameNanos { }
                moviesFirstFocus.requestFocus()
            }
        }
    }
    val focusFirstSeries: () -> Unit = {
        if (!actor?.series.isNullOrEmpty()) {
            focusScope.launch {
                seriesListState.scrollToItem(0)
                withFrameNanos { }
                seriesFirstFocus.requestFocus()
            }
        }
    }

    BackHandler {
        when {
            selectedPreview != null -> selectedPreview = null
            preparingCredit != null -> preparingCredit = null
            showFullBiography -> showFullBiography = false
            else -> onBackClick()
        }
    }

    val background by produceState<ImageBitmap?>(initialValue = ActorBackgroundCache.image) {
        value = ActorBackgroundCache.get(context)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF05090F))
    ) {
        // Medidas calculadas no escopo de BoxWithConstraints (maxHeight/maxWidth).
        val tight = maxHeight < 550.dp || maxWidth < 860.dp
        val horizontalPadding = if (tight) 18.dp else 34.dp
        val verticalPadding = if (tight) 10.dp else 16.dp
        val headerHeight = (maxHeight * 0.29f)
            .coerceAtLeast(if (tight) 124.dp else 154.dp)
            .coerceAtMost(if (tight) 165.dp else 205.dp)
        val rowSpacing = if (tight) 5.dp else 9.dp
        val availableRowsHeight = maxHeight - verticalPadding * 2 -
                headerHeight - rowSpacing * 2
        val rowHeight = (availableRowsHeight / 2f).coerceAtLeast(95.dp)
        // Restringe apenas o resumo, sem deslocar o nome nem o botao Voltar.
        // A biografia deixa livre o lado direito do fundo cinematografico.
        val biographyPreviewWidth = (maxWidth * 0.32f)
            .coerceAtLeast(if (tight) 235.dp else 330.dp)
            .coerceAtMost(if (tight) 375.dp else 440.dp)
        // Somente a camada da tela principal desfoca: o dialogo fica nitido.
        Box(
            modifier = Modifier.fillMaxSize().then(
                if (showFullBiography || selectedPreview != null) Modifier.blur(16.dp) else Modifier
            )
        ) {
            background?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Protege a leitura sem cobrir completamente o fundo cinematografico.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xBB05080D),
                                br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x8A090E17),
                                br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x4D090E17),
                                br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x4006090F)
                            )
                        )
                    )
            )


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                when {
                                    !actor?.movies.isNullOrEmpty() -> { focusFirstMovie(); true }
                                    !actor?.series.isNullOrEmpty() -> { focusFirstSeries(); true }
                                    else -> false
                                }
                            } else false
                        },
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(if (tight) 12.dp else 20.dp)
                ) {
                    val profileWidth = (headerHeight * 0.67f)
                        .coerceAtLeast(80.dp)
                        .coerceAtMost(142.dp)
                    val profileHeight = headerHeight - 12.dp
                    ActorPortrait(
                        url = actor?.portraitUrl ?: initialProfileUrl,
                        width = profileWidth,
                        height = profileHeight
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = actor?.name ?: initialName.ifBlank { "Elenco" },
                                fontSize = if (tight) 22.sp else 29.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(brush = ActorMetallicGoldText),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            ActorTextAction(label = "Voltar", onClick = onBackClick)
                        }
                        Spacer(Modifier.height(if (tight) 2.dp else 5.dp))

                        when (val state = fetchState) {
                            ActorFetchState.Loading -> {
                                Text("Carregando dados do TMDB...", color = ActorMuted, fontSize = 13.sp)
                            }
                            is ActorFetchState.Error -> {
                                Text(state.message, color = ActorMuted, fontSize = 13.sp)
                            }
                            is ActorFetchState.Success -> {
                                val profile = state.actor
                                profile.birthDate?.let {
                                    ActorInfoLine("Nascimento", it, tight)
                                }
                                profile.birthPlace?.let {
                                    ActorInfoLine("Naturalidade", it, tight)
                                }
                                Spacer(Modifier.height(if (tight) 2.dp else 5.dp))
                                var previewIsTruncated by remember(
                                    personTmdbId, profile.biography, tight
                                ) { mutableStateOf(false) }
                                Column(modifier = Modifier.widthIn(max = biographyPreviewWidth)) {
                                    Text(
                                        text = profile.biography.ifBlank {
                                            "Biografia não disponível em português."
                                        },
                                        color = ActorText,
                                        fontSize = if (tight) 11.sp else 13.sp,
                                        lineHeight = if (tight) 13.sp else 17.sp,
                                        maxLines = if (tight) 3 else 4,
                                        overflow = TextOverflow.Ellipsis,
                                        onTextLayout = { layout ->
                                            // So oferece a leitura completa se o texto foi cortado.
                                            val truncated = layout.hasVisualOverflow
                                            if (previewIsTruncated != truncated) {
                                                previewIsTruncated = truncated
                                            }
                                        }
                                    )
                                    if (profile.biography.isNotBlank() && previewIsTruncated) {
                                        ActorTextAction(
                                            label = "Ler biografia completa",
                                            onClick = { showFullBiography = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(rowSpacing))
                ActorCreditsRow(
                    label = "FILMES",
                    credits = actor?.movies.orEmpty(),
                    firstFocusRequester = moviesFirstFocus,
                    listState = moviesListState,
                    onDown = if (actor?.series.isNullOrEmpty()) null else focusFirstSeries,
                    height = rowHeight,
                    tight = tight,
                    loading = fetchState is ActorFetchState.Loading,
                    onClick = { credit ->
                        if (preparingCredit == null && selectedPreview == null) {
                            preparingCredit = credit
                        }
                    }
                )

                Spacer(Modifier.height(rowSpacing))
                ActorCreditsRow(
                    label = "SÉRIES",
                    credits = actor?.series.orEmpty(),
                    firstFocusRequester = seriesFirstFocus,
                    listState = seriesListState,
                    onDown = null,
                    height = rowHeight,
                    tight = tight,
                    loading = fetchState is ActorFetchState.Loading,
                    onClick = { credit ->
                        if (preparingCredit == null && selectedPreview == null) {
                            preparingCredit = credit
                        }
                    }
                )
            }
        } // Fim da camada desfocavel da tela principal.

        if (showFullBiography) {
            Dialog(
                onDismissRequest = { showFullBiography = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                ActorBiographyOverlay(
                    title = actor?.name ?: initialName,
                    biography = actor?.biography.orEmpty(),
                    onClose = { showFullBiography = false }
                )
            }
        }
        if (showPreparingIndicator && preparingCredit != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xDD080D15))
                    .border(1.dp, ActorMetallicGold, RoundedCornerShape(7.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Preparando prévia...", color = Color.White, fontSize = 12.sp)
            }
        }
        selectedPreview?.let { preview ->
            val credit = preview.credit
            // O Dialog mantem o foco no proprio modal; a fileira preserva a posicao
            // e volta ao card escolhido assim que a visualizacao e fechada.
            Dialog(
                onDismissRequest = { selectedPreview = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                ActorCreditPreviewDialog(
                    preview = preview,
                    isFavorite = if (credit.mediaType == "movie") {
                        isMovieFavorite(credit)
                    } else {
                        isSeriesFavorite(credit)
                    },
                    onToggleFavorite = if (credit.mediaType == "movie") {
                        onToggleMovieFavorite?.let { handler -> { handler(credit) } }
                    } else {
                        onToggleSeriesFavorite?.let { handler -> { handler(credit) } }
                    },
                    onWatch = if (credit.mediaType == "movie") {
                        onMovieSelected?.let { handler ->
                            { selectedPreview = null; handler(credit) }
                        }
                    } else null,
                    onClose = { selectedPreview = null }
                )
            }
        }
    }
}

@Composable
private fun ActorInfoLine(label: String, value: String, tight: Boolean) {
    Text(
        text = "$label: $value",
        color = ActorMuted,
        fontSize = if (tight) 10.sp else 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ActorTextAction(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = label,
        color = if (focused) Color.White else Color.Unspecified,
        style = if (focused) TextStyle.Default else TextStyle(brush = (if (focused) Brush.linearGradient(listOf(Color.White, Color.White)) else ActorMetallicGoldText)),
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) 1.dp else 0.dp,
                ActorMetallicGold,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
private fun ActorPortrait(url: String?, width: Dp, height: Dp) {
    val context = LocalContext.current.applicationContext
    val initialImage = remember(url) { MoviePosterLoader.peekBitmap(url) }
    val bitmap by produceState<ImageBitmap?>(initialValue = initialImage, key1 = url) {
        value = MoviePosterLoader.load(context, url)
    }
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(ActorShape)
            .background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF171C24))
            .border(1.dp, ActorMetallicGold, ActorShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Foto do ator",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("ELENCO", fontSize = 12.sp, color = ActorMuted)
        }
    }
}

@Composable
private fun ActorCreditsRow(
    label: String,
    credits: List<ActorFilmographyCredit>,
    firstFocusRequester: FocusRequester,
    listState: LazyListState,
    onDown: (() -> Unit)?,
    height: Dp,
    tight: Boolean,
    loading: Boolean,
    onClick: (ActorFilmographyCredit) -> Unit
) {
    val cardHeight = (height - if (tight) 36.dp else 43.dp).coerceAtLeast(56.dp)
    val cardWidth = (cardHeight * 0.69f).coerceAtLeast(44.dp)

    Column(modifier = Modifier.height(height)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = if (tight) 15.sp else 19.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(brush = ActorMetallicGoldText)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = when {
                    loading -> "Carregando..."
                    credits.isEmpty() -> "Nenhum crédito encontrado"
                    else -> "${credits.size} produções"
                },
                color = ActorMuted,
                fontSize = if (tight) 9.sp else 11.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(if (tight) 8.dp else 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (onDown != null &&
                        event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionDown
                    ) {
                        onDown()
                        true
                    } else false
                }
        ) {
            itemsIndexed(
                items = credits,
                key = { _, credit -> "${credit.mediaType}-${credit.tmdbId}" }
            ) { index, credit ->
                ActorCreditCard(
                    credit = credit,
                    firstFocusRequester = if (index == 0) firstFocusRequester else null,
                    width = cardWidth,
                    height = cardHeight,
                    tight = tight,
                    onClick = { onClick(credit) }
                )
            }
        }
    }
}

@Composable
private fun ActorCreditCard(
    credit: ActorFilmographyCredit,
    firstFocusRequester: FocusRequester?,
    width: Dp,
    height: Dp,
    tight: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    // Mede o nome na largura real do poster. Nomes que cabem ficam centralizados;
    // os que nao cabem permanecem alinhados a esquerda e rolam somente no foco.
    val titleFontSize = if (tight) 9.sp else 11.sp
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleWidthPx = with(density) { width.roundToPx() }
    val titleIsLong = remember(credit.title, titleFontSize, titleWidthPx, textMeasurer) {
        textMeasurer.measure(
            text = credit.title,
            style = TextStyle(fontSize = titleFontSize, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = titleWidthPx)
        ).hasVisualOverflow
    }

    val context = LocalContext.current.applicationContext
    val initialImage = remember(credit.posterUrl) {
        MoviePosterLoader.peekBitmap(credit.posterUrl)
    }
    val bitmap by produceState<ImageBitmap?>(
        initialValue = initialImage,
        key1 = credit.posterUrl
    ) {
        value = MoviePosterLoader.load(context, credit.posterUrl)
    }

    Column(modifier = Modifier.width(width)) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(ActorShape)
                .background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF202530))
                .border(
                    if (focused) ActorGoldOutline
                    else BorderStroke(1.dp, Color(0xFF55504A)),
                    ActorShape
                )
                .then(
                    if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester)
                    else Modifier
                )
                .onFocusChanged { focused = it.isFocused }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = credit.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("SEM CAPA", color = ActorMuted, fontSize = 10.sp)
            }
        }
        Text(
            text = credit.title,
            color = if (focused) Color.Unspecified else ActorText,
            style = if (focused) TextStyle(brush = (if (focused) Brush.linearGradient(listOf(Color.White, Color.White)) else ActorMetallicGoldText)) else TextStyle.Default,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = titleFontSize,
            maxLines = 1,
            softWrap = false,
            textAlign = if (titleIsLong) TextAlign.Start else TextAlign.Center,
            overflow = if (focused && titleIsLong) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (focused && titleIsLong) {
                        Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 500,
                            repeatDelayMillis = 700,
                            velocity = 26.dp
                        )
                    } else Modifier
                )
        )
    }
}

/**
 * Biografia em destaque sobre o layout desfocado, sem caixa opaca.
 * O texto pode rolar pelo controle remoto; Voltar ou FECHAR encerra o dialogo.
 */
@Composable
private fun ActorBiographyOverlay(
    title: String,
    biography: String,
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
        modifier = Modifier
            .fillMaxSize()
            .background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x5203070D)),
        contentAlignment = Alignment.Center
    ) {
        val tight = maxWidth < 860.dp || maxHeight < 550.dp
        // Evita acessar BoxWithConstraintsScope de dentro do ColumnScope.
        val biographyScrollMaxHeight = maxHeight * 0.53f
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
                text = title,
                color = Color.White,
                fontSize = if (tight) 22.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(if (tight) 14.dp else 22.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = biographyScrollMaxHeight)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = biography,
                    color = Color.White,
                    fontSize = if (tight) 14.sp else 17.sp,
                    lineHeight = if (tight) 19.sp else 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(if (tight) 12.dp else 20.dp))
            ActorTextAction(
                label = "FECHAR",
                onClick = onClose,
                focusRequester = closeFocus
            )
        }
    }
}

/**
 * Previa compacta da filmografia. Uma unica consulta de detalhes por abertura;
 * o carrossel do ator NAO dispara requisicoes extras ao rolar.
 */
/** Carrega os recursos antes de abrir a janela, usando o cache existente. */
private suspend fun loadActorPreviewImage(
    context: android.content.Context,
    url: String?,
    maxWaitMs: Long
): ImageBitmap? {
    if (url.isNullOrBlank()) return null
    return MoviePosterLoader.peekBitmap(url) ?: withTimeoutOrNull(maxWaitMs) {
        MoviePosterLoader.load(context, url)
    }
}

@Composable
private fun ActorCreditPreviewDialog(
    preview: ActorPreparedCredit,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onWatch: (() -> Unit)?,
    onClose: () -> Unit
) {
    val credit = preview.credit
    val detail = preview.detail
    val backdrop = preview.backdrop
    val titleLogo = preview.titleLogo
    val context = LocalContext.current
    val firstActionFocus = remember(credit.tmdbId, credit.mediaType) { FocusRequester() }
    val scrollState = rememberScrollState()
    val scrollScope = rememberCoroutineScope()
    val isMovie = credit.mediaType == "movie"

    LaunchedEffect(credit.tmdbId, credit.mediaType) {
        withFrameNanos { }
        firstActionFocus.requestFocus()
    }
    val pageTitle = detail.title
    val synopsis = detail.synopsis.ifBlank {
        "Sinopse indisponível nos idiomas cadastrados."
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x80000000)),
        contentAlignment = Alignment.Center
    ) {
        val compact = maxWidth < 900.dp || maxHeight < 560.dp
        val modalWidth = (maxWidth * 0.58f)
            .coerceAtLeast(if (compact) 320.dp else 530.dp)
            .coerceAtMost(850.dp)
            .coerceAtMost(maxWidth - 30.dp)
        val modalHeight = (maxHeight * 0.69f)
            .coerceAtLeast(if (compact) 290.dp else 360.dp)
            .coerceAtMost(555.dp)
            .coerceAtMost(maxHeight - 22.dp)
        val shape = RoundedCornerShape(if (compact) 12.dp else 16.dp)
        Box(
            modifier = Modifier
                .width(modalWidth)
                .height(modalHeight)
                .clip(shape)
                .background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF080D15))
                .border(1.5.dp, ActorMetallicGold, shape)
        ) {
            if (backdrop != null) {
                Image(
                    bitmap = backdrop!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Uma unica pelicula escura a 50% sobre a imagem:
            // o fundo continua visivel e a sinopse branca permanece legivel.
            Box(
                modifier = Modifier.fillMaxSize().background(br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0x8005090F))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (compact) 17.dp else 24.dp,
                        vertical = if (compact) 12.dp else 18.dp
                    )
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) false
                        else when (event.key) {
                            Key.DirectionDown -> {
                                if (scrollState.value < scrollState.maxValue) {
                                    scrollScope.launch { scrollState.scrollBy(70f) }
                                    true
                                } else false
                            }
                            Key.DirectionUp -> {
                                if (scrollState.value > 0) {
                                    scrollScope.launch { scrollState.scrollBy(-70f) }
                                    true
                                } else false
                            }
                            else -> false
                        }
                    }
            ) {
                if (titleLogo != null) {
                    Image(
                        bitmap = titleLogo!!,
                        contentDescription = pageTitle,
                        modifier = Modifier
                            .width(if (compact) 200.dp else 260.dp)
                            .height(if (compact) 46.dp else 64.dp),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart
                    )
                } else {
                    Text(
                        text = pageTitle,
                        fontSize = if (compact) 20.sp else 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = if (compact) 23.sp else 32.sp
                    )
                }
                Spacer(Modifier.height(if (compact) 5.dp else 9.dp))
                val metaPieces = buildList {
                    detail.rating?.takeIf { it > 0.0 }?.let {
                        add("★ ${String.format(Locale.forLanguageTag("pt-BR"), "%.1f", it)}")
                    }
                    (detail.year ?: credit.year)?.let { add(it) }
                    detail.duration?.let { add(it) }
                    detail.genres.take(3).takeIf { it.isNotEmpty() }
                        ?.let { add(it.joinToString(" • ")) }
                }
                if (metaPieces.isNotEmpty()) {
                    Text(
                        text = metaPieces.joinToString("   ·   "),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 12.sp else 15.sp,
                        lineHeight = if (compact) 17.sp else 21.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(if (compact) 8.dp else 13.dp))
                }
                // A sinopse ocupa apenas a altura de que precisa. Quando for longa,
                // torna-se rolavel para preservar os botoes na mesma janela.
                // ASSISTIR/TRAILER/CORACAO ficam logo apos o fim do texto,
                // enquanto FECHAR permanece centralizado no rodape do modal.
                Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = synopsis,
                            color = Color.White,
                            fontSize = if (compact) 13.sp else 17.sp,
                            lineHeight = if (compact) 18.sp else 24.sp
                        )
                    }
                    Spacer(Modifier.height(if (compact) 9.dp else 14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMovie) {
                            br.com.flaviotorres.iptvpremium.ui.components.ReferenceWatchButton(
                                text = "ASSISTIR",
                                focusRequester = firstActionFocus,
                                compact = compact,
                                onClick = {
                                    if (onWatch != null) onWatch()
                                    else Toast.makeText(
                                        context,
                                        "Filme ainda não vinculado ao seu catálogo.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                        br.com.flaviotorres.iptvpremium.ui.components.ReferenceFavoriteButton(
                            isFavorite = isFavorite, compact = compact,
                            focusRequester = if (isMovie) null else firstActionFocus,
                            onClick = {
                                if (onToggleFavorite != null) onToggleFavorite()
                                else Toast.makeText(
                                    context,
                                    "Favoritos desta produção serão vinculados ao catálogo IPTV.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(if (compact) 7.dp else 11.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ActorPreviewActionButton(
                        label = "FECHAR",
                        compact = compact,
                        onClick = onClose,
                        useGoldBackground = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ActorPreviewActionButton(
    label: String,
    compact: Boolean,
    onClick: () -> Unit,
    firstFocusRequester: FocusRequester? = null,
    useGoldBackground: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (useGoldBackground) 7.dp else 4.dp)
    val focusModifier = Modifier
        .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
        .onFocusChanged { focused = it.isFocused }

    if (!useGoldBackground) {
        // Mesmo acabamento de 'Ler biografia completa': somente contorno
        // dourado, sem bloco preto; texto dourado ou branco quando focado.
        Text(
            text = label,
            color = if (focused) Color.White else Color.Unspecified,
            style = if (focused) TextStyle.Default else TextStyle(brush = (if (focused) Brush.linearGradient(listOf(Color.White, Color.White)) else ActorMetallicGoldText)),
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = focusModifier
                .border(if (focused) 1.5.dp else 1.dp, ActorMetallicGold, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = if (compact) 9.dp else 12.dp, vertical = 5.dp)
        )
    } else {
        br.com.flaviotorres.iptvpremium.ui.components.MediaActionButton(
            text = label, compactLayout = compact, onClick = onClick,
            focusRequester = firstFocusRequester,
            buttonWidth = if (compact) 93.dp else 135.dp,
            buttonHeight = if (compact) 33.dp else 44.dp
        )
    }
}

/** Traduções retornadas na mesma chamada dos detalhes (sem uma segunda requisição). */
private fun actorCreditTranslations(root: JSONObject): List<JSONObject> {
    val array = root.optJSONObject("translations")?.optJSONArray("translations") ?: return emptyList()
    return buildList {
        for (i in 0 until array.length()) {
            array.optJSONObject(i)?.let { add(it) }
        }
    }
}

private fun actorJsonText(value: String?): String? = value
    ?.trim()
    ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

private fun preferredActorCreditTitle(
    root: JSONObject,
    type: String,
    fallbackTitle: String
): String {
    val titleField = if (type == "movie") "title" else "name"
    val originalField = if (type == "movie") "original_title" else "original_name"
    val translations = actorCreditTranslations(root)
    fun translated(language: String, country: String): String? = translations
        .firstOrNull { it.optString("iso_639_1") == language &&
                it.optString("iso_3166_1") == country }
        ?.optJSONObject("data")?.optString(titleField)?.let(::actorJsonText)

    return translated("pt", "BR")
        ?: translated("pt", "PT")
        ?: actorJsonText(root.optString(titleField))
        ?: actorJsonText(root.optString(originalField))
        ?: fallbackTitle
}

private fun preferredActorCreditSynopsis(root: JSONObject): String {
    val translations = actorCreditTranslations(root)
    fun synopsisIn(language: String, country: String? = null): String? = translations
        .firstOrNull { it.optString("iso_639_1") == language &&
                (country == null || it.optString("iso_3166_1") == country) }
        ?.optJSONObject("data")?.optString("overview")?.let(::actorJsonText)

    // A consulta principal já usa pt-BR. Se estiver vazia, buscamos uma
    // tradução cadastrada: pt-PT, idioma original, inglês e outros idiomas.
    val primary = actorJsonText(root.optString("overview"))
    if (primary != null) return primary

    val original = actorJsonText(root.optString("original_language"))
    return synopsisIn("pt", "BR")
        ?: synopsisIn("pt", "PT")
        ?: original?.let { synopsisIn(it) }
        ?: synopsisIn("en", "US")
        ?: synopsisIn("en")
        ?: translations.asSequence()
            .mapNotNull { it.optJSONObject("data")?.optString("overview")?.let(::actorJsonText) }
            .firstOrNull()
        ?: ""
}

/** Escolhe a mesma arte de titulo usada nos detalhes dos filmes.
 * Prefere logo em portugues, depois ingles e por ultimo idioma neutro.
 */
private fun preferredActorCreditLogo(root: JSONObject): String? {
    val logos = root.optJSONObject("images")?.optJSONArray("logos") ?: return null
    val matches = (0 until logos.length()).mapNotNull { logos.optJSONObject(it) }
        .filter { it.optString("file_path").startsWith("/") }
    val best = matches.minWithOrNull(compareBy<JSONObject> {
        when (it.optString("iso_639_1").lowercase(Locale.ROOT)) {
            "pt" -> 0
            "en" -> 1
            "", "null" -> 2
            else -> 3
        }
    }.thenByDescending { it.optDouble("vote_average", 0.0) })
    return tmdbImage("w500", best?.optString("file_path"))
}

private suspend fun fetchActorCreditDetail(credit: ActorFilmographyCredit): ActorCreditDetail {
    val type = if (credit.mediaType == "tv") "tv" else "movie"
    val root = JSONObject(
        TmdbClientProvider.client.get(
            path = "/$type/${credit.tmdbId}",
            queryParameters = mapOf(
                "language" to "pt-BR",
                "append_to_response" to "videos,translations,images",
                "include_image_language" to "pt,en,null"
            )
        )
    )
    val genreArray = root.optJSONArray("genres") ?: JSONArray()
    val genres = buildList {
        for (i in 0 until genreArray.length()) {
            genreArray.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
                ?.let { add(it) }
        }
    }
    val runtime = if (type == "movie") {
        root.optInt("runtime", 0)
    } else {
        root.optJSONArray("episode_run_time")?.optInt(0, 0) ?: 0
    }
    val duration = runtime.takeIf { it > 0 }?.let { minutes ->
        val hours = minutes / 60
        val rest = minutes % 60
        if (hours > 0) "${hours}h ${rest}min" else "${rest}min"
    } ?: if (type == "tv") {
        root.optInt("number_of_seasons", 0).takeIf { it > 0 }
            ?.let { "$it ${if (it == 1) "temporada" else "temporadas"}" }
    } else null
    val release = root.optString(if (type == "movie") "release_date" else "first_air_date")
    val videos = root.optJSONObject("videos")?.optJSONArray("results") ?: JSONArray()
    val trailerCandidates = (0 until videos.length()).mapNotNull { videos.optJSONObject(it) }
        .filter { it.optString("site") == "YouTube" &&
                it.optString("key").matches(Regex("[a-zA-Z0-9_-]{11}")) }
    val trailer = trailerCandidates.firstOrNull { it.optString("type") == "Trailer" && it.optBoolean("official", false) }
        ?: trailerCandidates.firstOrNull { it.optString("type") == "Trailer" }
        ?: trailerCandidates.firstOrNull { it.optString("type") == "Teaser" }
    return ActorCreditDetail(
        // Quando houver, mostra o título localizado em português (versão de exibição);
        // caso contrário preserva o título disponível da produção.
        title = preferredActorCreditTitle(root, type, credit.title),
        titleLogoUrl = preferredActorCreditLogo(root),
        year = release.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: credit.year,
        rating = root.optDouble("vote_average", 0.0).takeIf { it > 0 && it.isFinite() },
        duration = duration,
        genres = genres,
        // Busca primeiro português; na ausência dele, a sinopse disponível
        // no idioma original, inglês ou qualquer outra tradução cadastrada.
        synopsis = preferredActorCreditSynopsis(root),
        // w1280 evita esticar a imagem de 780 px na tela da TV.
        // Se nao houver backdrop, usa poster com resolucao maior.
        backdropUrl = tmdbImage("w1280", root.optString("backdrop_path"))
            ?: tmdbImage("w780", root.optString("poster_path"))
            ?: credit.posterUrl,
        trailerKey = trailer?.optString("key")?.takeIf { it.isNotBlank() }
    )
}

private suspend fun fetchActorProfile(personId: Int): ActorProfile {
    val root = JSONObject(
        TmdbClientProvider.client.get(
            path = "/person/$personId",
            queryParameters = mapOf(
                "language" to "pt-BR",
                "append_to_response" to "combined_credits"
            )
        )
    )

    val credits = root.optJSONObject("combined_credits")
        ?.optJSONArray("cast") ?: JSONArray()
    val items = ArrayList<ActorFilmographyCredit>(credits.length())
    for (index in 0 until credits.length()) {
        val item = credits.optJSONObject(index) ?: continue
        if (item.optBoolean("adult", false)) continue
        val mediaType = item.optString("media_type")
        if (mediaType != "movie" && mediaType != "tv") continue
        val id = item.optInt("id", -1)
        if (id <= 0) continue
        val title = item.optString(if (mediaType == "movie") "title" else "name")
            .trim().takeIf { it.isNotBlank() } ?: continue
        val released = item.optString(
            if (mediaType == "movie") "release_date" else "first_air_date"
        ).trim()
        val year = released.take(4).takeIf {
            it.length == 4 && it.all(Char::isDigit)
        }
        items += ActorFilmographyCredit(
            tmdbId = id,
            mediaType = mediaType,
            title = title,
            year = year,
            posterUrl = tmdbImage("w500", item.optString("poster_path")),
            character = item.optString("character").trim().takeIf { it.isNotBlank() }
        )
    }
    // Elimina aparicoes repetidas na mesma producao sem apagar outros titulos.
    val unique = items.distinctBy { it.mediaType to it.tmdbId }
        .sortedWith(compareByDescending<ActorFilmographyCredit> {
            it.year?.toIntOrNull() ?: 0
        }.thenBy { it.title })

    val rawBirthday = root.optString("birthday").trim()
    val localizedBirthday = try {
        if (rawBirthday.isBlank() || rawBirthday == "null") null
        else LocalDate.parse(rawBirthday).format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        )
    } catch (_: Exception) {
        rawBirthday.takeIf { it.isNotBlank() && it != "null" }
    }

    return ActorProfile(
        name = root.optString("name").trim().ifBlank { "Elenco" },
        birthDate = localizedBirthday,
        birthPlace = root.optString("place_of_birth").trim()
            .takeIf { it.isNotBlank() && it != "null" },
        biography = root.optString("biography").trim().let {
            if (it == "null") "" else it
        },
        portraitUrl = tmdbImage("w342", root.optString("profile_path")),
        movies = unique.filter { it.mediaType == "movie" },
        series = unique.filter { it.mediaType == "tv" }
    )
}

private fun tmdbImage(size: String, path: String?): String? {
    val safePath = path?.trim()?.takeIf { it.startsWith("/") } ?: return null
    return "https://image.tmdb.org/t/p/$size$safePath"
}

/** Um unico bitmap do fundo, decodificado fora da thread de interface. */
private object ActorBackgroundCache {
    @Volatile var image: ImageBitmap? = null
        private set
    private val lock = Any()

    suspend fun get(context: android.content.Context): ImageBitmap? =
        withContext(Dispatchers.IO) {
            image?.let { return@withContext it }
            synchronized(lock) {
                image ?: try {
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.actor_cinematic_background,
                        BitmapFactory.Options().apply {
                            inScaled = false
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                    )?.asImageBitmap()?.also { image = it }
                } catch (_: Exception) {
                    null
                }
            }
        }
}
// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_MEDIA_ACTION_BUTTONS_V1

// Text retains the approved palette; blue darkening applies to rims.
private val ActorMetallicGoldText get() = Brush.linearGradient(
    colors = listOf(
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF8A6414),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD4AF37),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE991),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFF4BD),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFE3B83F),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF9E761B)
    )
)
