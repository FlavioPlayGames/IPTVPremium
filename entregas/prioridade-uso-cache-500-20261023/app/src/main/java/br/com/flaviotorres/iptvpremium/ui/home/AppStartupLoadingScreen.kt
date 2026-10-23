package br.com.flaviotorres.iptvpremium.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource as painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import kotlin.math.roundToInt

/**
 * Estado verdadeiro da preparação do catálogo.
 *
 * IMPORTANTE: os contadores devem ser atualizados pelo AppNavigation,
 * após a conclusão de cada operação real. A tela nunca simula progresso.
 */
enum class AppCatalogLoadingPhase {
    READING_CATALOG,
    IMPORTING_HISTORY,
    SELECTING_TITLES,
    PREPARING_TITLES,
    FINISHING,
    COMPLETED,
    ERROR
}

@Immutable
data class AppCatalogLoadingState(
    val phase: AppCatalogLoadingPhase = AppCatalogLoadingPhase.READING_CATALOG,
    val preparedTitles: Int = 0,
    val totalTitles: Int = 0,
    val failedTitles: Int = 0,
    val currentTitle: String? = null,
    val currentCategory: String? = null,
    val errorMessage: String? = null,
    val isPlaylistRefresh: Boolean = false
) {
    val progress: Float
        get() = if (phase == AppCatalogLoadingPhase.COMPLETED) {
            1f
        } else if (totalTitles > 0) {
            (preparedTitles.toFloat() / totalTitles.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

private val StartupGold get() = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFE0B747)
private val StartupCream get() = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE8A3)
private val StartupGrey = Color(0xFFA9A9AD)
private val StartupGoldGradient get() = Brush.horizontalGradient(
    listOf(
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFF8B6419),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFD3A531),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFFFE9A5),
        br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.color(0xFFE0B747)
    )
)
private val StartupCinzel = FontFamily(
    Font(R.font.cinzel_semibold, weight = FontWeight.SemiBold)
)

/** Sized startup/update placeholder. Catalogue readiness controls its lifetime;
 * history and artwork preparation never run here or delay Home. */
@Composable
fun AppStartupLoadingScreen(
    state: AppCatalogLoadingState,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onOpenPlaylists: (() -> Unit)? = null,
    onOpenHome: (() -> Unit)? = null
) {
    val error=state.phase==AppCatalogLoadingPhase.ERROR
    // Fixed, fully bounded composition: no asynchronous artwork or completed-screen flash.
    BoxWithConstraints(modifier.fillMaxSize().background(Color(0xFF0C0E11)).padding(20.dp),contentAlignment=Alignment.Center) {
        val small=maxHeight<360.dp
        Column(Modifier.widthIn(max=600.dp).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(if(small)12.dp else 20.dp)) {
            Text("IPTV PREMIUM",color=StartupGold,fontSize=if(small)24.sp else 32.sp,
                fontFamily=StartupCinzel,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center,
                maxLines=1,modifier=Modifier.fillMaxWidth())
            Text(if(error)"Não foi possível carregar a lista agora." else "Carregando seu conteúdo…",
                color=Color(0xFFE5E6E8),fontSize=if(small)13.sp else 16.sp,textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth())
            if(error) {
                Text("Confira sua conexão ou tente novamente em Listas.",color=Color(0xFFB9BCC2),
                    fontSize=13.sp,textAlign=TextAlign.Center,maxLines=3)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    onRetry?.let {LoadingAction("TENTAR NOVAMENTE",it)}
                    onOpenHome?.let {LoadingAction("FECHAR",it)}
                    onOpenPlaylists?.let {LoadingAction("MINHAS LISTAS",it)}
                }
            }
        }
    }
}

@Composable
private fun LoadingAction(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val actionFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { actionFocus.requestFocus() }
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .focusRequester(actionFocus)
            .clip(shape)
            .background(if (focused) br.com.flaviotorres.iptvpremium.ui.theme.SelectionTheme.color() else br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.surface(0xFF232025))
            .border(
                width = if (focused) 2.dp else 1.dp, brush = br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors.metallicBorder,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 17.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            color = if (focused) Color.White else StartupCream
        )
    }
}

// IPTV_GLOBAL_GRAPHITE_V1

// IPTV_POLISH_LOADING_HOLD_V1
