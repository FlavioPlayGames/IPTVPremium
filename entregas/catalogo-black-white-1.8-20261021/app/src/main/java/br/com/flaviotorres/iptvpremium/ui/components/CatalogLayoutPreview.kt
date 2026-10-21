package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeColors
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource

/** A full TV layout scaled as a unit: actual theme wallpaper, local portrait art and captions. */
@Composable
internal fun CatalogLayoutPreview(columns: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val density = LocalDensity.current
    val virtualWidth = with(density) { 960.dp.roundToPx() }
    val virtualHeight = with(density) { 540.dp.roundToPx() }
    Layout(modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape)
        .border(1.dp, ThemeColors.metallicBorder, shape)
        .testTag("catalog-layout-preview")
        .semantics { contentDescription = "Prévia: duas fileiras de $columns capas, ${columns * 2} capas visíveis" },
        content = { CatalogPreviewScene(columns) }
    ) { measurables, constraints ->
        val scene = measurables.single().measure(Constraints.fixed(virtualWidth, virtualHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            scene.placeWithLayer(0, 0) {
                scaleX = constraints.maxWidth.toFloat() / virtualWidth
                scaleY = constraints.maxHeight.toFloat() / virtualHeight
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

@Composable
private fun CatalogPreviewScene(columns: Int) {
    val edge = CatalogStyle.outline(false, ThemeColors.metallicBorder)
    val panel = RoundedCornerShape(14.dp)
    val art = listOf(R.drawable.preview_poster_01, R.drawable.preview_poster_02,
        R.drawable.preview_poster_03, R.drawable.preview_poster_04, R.drawable.preview_poster_05,
        R.drawable.preview_poster_06, R.drawable.preview_poster_07, R.drawable.preview_poster_08,
        R.drawable.preview_poster_09, R.drawable.preview_poster_01)
    val titles = listOf("Deep", "Vök", "The Fairy", "Cursed", "My Crazy One", "Gone",
        "A Cold Night to Stay", "The Silence", "Hear the Roar", "Deep")
    Box(Modifier.fillMaxSize()) {
        CatalogThemeBackdrop(Modifier.matchParentSize())
        Row(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(245.dp).fillMaxHeight().clip(panel)
                .background(CatalogStyle.surface(0xFF080B10)).border(1.dp, edge, panel).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                    CatalogWordmark(true)
                    Text("CATEGORIAS", color = CatalogStyle.ink(0xFFFFFFFF), fontSize = 10.sp)
                }
                Spacer(Modifier.height(3.dp))
                listOf("Todos" to "43517", "Continuar assistindo" to "3", "Favoritos" to "12",
                    "Documentários" to "1015", "Esportes" to "204", "4K" to "1751",
                    "Ação" to "2492", "Animação" to "1227", "Aventura" to "71").forEachIndexed { index, (label, count) ->
                    val itemShape = RoundedCornerShape(10.dp)
                    Row(Modifier.fillMaxWidth().height(38.dp).clip(itemShape)
                        .background(if(index==0)CatalogStyle.selectionColor()else CatalogStyle.surface(0x70111419))
                        .border(1.dp, CatalogStyle.outline(index==0,edge), itemShape).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (index < 3) listOf("▦", "▷", "♥")[index] else "▣",
                            color = CatalogStyle.accent, fontSize = 14.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(label, color = CatalogStyle.ink(0xFFFFFFFF), fontSize = 11.sp, maxLines = 1,
                            modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                        Text(count, color = CatalogStyle.secondary, fontSize = 9.sp)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().height(44.dp).clip(panel)
                    .background(CatalogStyle.surface(0xFF080B10)).border(1.dp, edge, panel).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("☰  Organizar", color = CatalogStyle.ink(0xFFFFFFFF), fontSize = 10.sp,
                        modifier = Modifier.border(1.dp, edge, RoundedCornerShape(8.dp)).padding(8.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("⌕  Pesquisar filmes...", color = CatalogStyle.secondary, fontSize = 11.sp,
                        modifier = Modifier.weight(1f).border(1.dp, edge, RoundedCornerShape(8.dp)).padding(8.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("12:00", color = CatalogStyle.accent, fontSize = 20.sp)
                }
                CatalogTwoRowFrame(compactLayout = true, columns = columns) { aspect ->
                    Column(Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(LocalCatalogRowSpacing.current)) {
                        repeat(2) { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                repeat(columns) { column ->
                                    val index = row * columns + column
                                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                        Column(Modifier.width(LocalCatalogPosterWidth.current)
                                            .height(LocalCatalogCardHeight.current).testTag("preview-poster-$index")) {
                                            val posterShape = RoundedCornerShape(CATALOG_POSTER_CORNER_DP.dp)
                                            Box(Modifier.fillMaxWidth().aspectRatio(aspect)) {
                                                Image(painterResource(art[index]), "Capa de exemplo ${index + 1}",
                                                    Modifier.fillMaxSize().clip(posterShape)
                                                        .border(1.dp, edge, posterShape), contentScale = ContentScale.Crop)
                                                Text("♡", color = CatalogStyle.accent, fontSize = 20.sp,
                                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(titles[index], color = CatalogStyle.ink(0xFFFFFFFF), fontSize = 11.sp,
                                                lineHeight = 14.sp, maxLines = 1)
                                            Spacer(Modifier.height(2.dp))
                                            Text("2026  ·  ★ 8.0", color = CatalogStyle.secondary, fontSize = 9.sp,
                                                lineHeight = 12.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
