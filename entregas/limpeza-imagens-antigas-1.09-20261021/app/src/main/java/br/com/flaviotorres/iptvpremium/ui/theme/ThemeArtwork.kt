package br.com.flaviotorres.iptvpremium.ui.theme

import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import br.com.flaviotorres.iptvpremium.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal val ThemeArtworkEpoch = kotlinx.coroutines.flow.MutableStateFlow(0L)
private var resetMarker: ArtworkCacheResetMarker? = null
suspend fun refreshThemeArtworkCache(context: Context) = withContext(Dispatchers.IO) {
    val marker = synchronized(ThemeArtworkEpoch) {
        resetMarker ?: ArtworkCacheResetMarker(java.io.File(context.applicationContext.cacheDir, ".iptv_theme_cache"))
            .also { resetMarker = it }
    }
    if (marker.check()) {
        br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader.clearMemory()
        ThemeArtworkCache.invalidate()
    }
}

private data class ArtworkSet(val background: Int, val logo: Int)
private fun artwork(theme: PremiumTheme): ArtworkSet? = when (theme) {
    PremiumTheme.GOLD -> null
    PremiumTheme.BLUE -> ArtworkSet(R.drawable.theme_blue_background, R.drawable.theme_blue_logo)
    PremiumTheme.PURPLE -> ArtworkSet(R.drawable.theme_purple_background, R.drawable.theme_purple_logo)
    PremiumTheme.RED -> ArtworkSet(R.drawable.theme_red_background, R.drawable.theme_red_logo)
    PremiumTheme.PINK -> ArtworkSet(R.drawable.theme_pink_background, R.drawable.theme_pink_logo)
}

/** Home art stays untouched; loading scenes reuse their original photos and theme accents. */
private object ThemeArtworkCache {
    private val pins = ActiveThemePins<ImageBitmap>(32 * 1024) { ((it.width.toLong() * it.height * 4 + 1023) / 1024).toInt() }
    @Volatile private var activeTheme = ""
    @Synchronized fun invalidate() {
        activeTheme = ""; pins.reset(); cache.evictAll()
        MarbleArtworkCache.clear()
        ThemeArtworkEpoch.value = ThemeArtworkEpoch.value + 1L
    }
    @Synchronized fun activate(theme: String) {
        if (pins.activate(theme)) { activeTheme = theme; cache.evictAll() }
    }
    @Synchronized private fun put(key: String, bitmap: ImageBitmap, theme: String, pin: Boolean, group: String, epoch: Long) {
        if (activeTheme != theme || ThemeArtworkEpoch.value != epoch) return
        if (!pin || !pins.put(theme, key, group, bitmap)) cache.put(key, bitmap)
    }
    private val decode = Semaphore(1)
    // One shared budget replaces the separate Home caches (20 + 10 + 8 MiB).
    private val cache = object : LruCache<String, ImageBitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = ((value.width.toLong() * value.height * 4 + 1023) / 1024).toInt()
    }
    fun key(id: Int, cell: Int?, target: IntSize?) = "$id:$cell:${target?.width}:${target?.height}"
    fun cached(id: Int, cell: Int?, target: IntSize?): ImageBitmap? {
        val k = key(id, if (target == null) null else cell, target)
        return pins.get(activeTheme, k) ?: cache.get(k)
    }
    fun cachedBlur(id: Int, target: IntSize, radius: Int) = cache.get(key(id, null, target) + ":blur:$radius")
    suspend fun loadBlur(context: Context, id: Int, target: IntSize, radius: Int): ImageBitmap? {
        val epoch = ThemeArtworkEpoch.value
        val theme = activeTheme
        cachedBlur(id, target, radius)?.let { return it }
        val base = load(context, id, null, target) ?: return null
        return withContext(Dispatchers.Default) {
            decode.withPermit {
                cachedBlur(id, target, radius) ?: try {
                    val raw = base.asAndroidBitmap()
                    val pixels = IntArray(raw.width * raw.height)
                    raw.getPixels(pixels, 0, raw.width, 0, 0, raw.width, raw.height)
                    val blurred = blurBackdropPixels(pixels, raw.width, raw.height, radius)
                    Bitmap.createBitmap(blurred, raw.width, raw.height, Bitmap.Config.ARGB_8888).asImageBitmap()
                        .also { put(key(id, null, target) + ":blur:$radius", it, theme, false, "blur", epoch) }
                } catch (_: OutOfMemoryError) { cache.evictAll(); null }
            }
        }
    }
    suspend fun load(context: Context, id: Int, cell: Int?, target: IntSize?, pin: Boolean = false, theme: String = activeTheme): ImageBitmap? = withContext(Dispatchers.Default) {
        val epoch = ThemeArtworkEpoch.value
        decode.withPermit {
            val cacheKey = key(id, cell, target)
            pins.get(theme, cacheKey) ?: cache.get(cacheKey) ?: try {
                val rawKey = key(id, null, null)
                val raw = cache.get(rawKey) ?: BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                })?.asImageBitmap()?.also { put(rawKey, it, theme, false, "raw", epoch) } ?: return@withPermit null
                if (target == null) { if (pin) put(cacheKey, raw, theme, true, cacheKey, epoch); return@withPermit raw }
                val bitmap = raw.asAndroidBitmap()
                val width = if (cell == null) bitmap.width else bitmap.width / 3
                val height = if (cell == null) bitmap.height else if (cell <= 3) width * 2 / 3 else bitmap.height / 2
                val left = if (cell == null) 0 else cell % 3 * width
                val top = if (cell == null) 0 else cell / 3 * (bitmap.height / 2)
                val factor = minOf(target.width.toFloat() / width, target.height.toFloat() / height, 1f)
                val outWidth = (width * factor).roundToInt().coerceAtLeast(1)
                val outHeight = (height * factor).roundToInt().coerceAtLeast(1)
                if (id in listOf(R.drawable.series_living_room, R.drawable.kids_home_blue, R.drawable.kids_home_pink, R.drawable.kids_home_objects,
                    R.drawable.kids_category_symbols, R.drawable.kids_brand_marks) && cell == null) {
                    return@withPermit Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true).asImageBitmap()
                        .also { put(cacheKey, it, theme, pin, cacheKey, epoch) }
                }
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, left, top, width, height)
                val scaled = resampleArtwork(pixels, width, height, outWidth, outHeight)
                Bitmap.createBitmap(scaled, outWidth, outHeight, Bitmap.Config.ARGB_8888)
                    .asImageBitmap().also { put(cacheKey, it, theme, pin, cacheKey, epoch) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
              catch (_: OutOfMemoryError) { cache.evictAll(); null }
        }
    }
}

/** Preserve neutral surfaces; only the warm accent is mapped to the selected hue. */
private fun loadingAccent(theme: PremiumTheme, kids: Boolean = false): ColorFilter {
    // O urso usa a saturacao dos icones metalicos; fundos continuam com o filtro suave.
    val accent = Color.hsv(theme.hue, if (kids) .98f else .58f, if (kids) .74f else .72f)
    val gain=if(kids).78f else 1f
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        accent.red*gain, 0f, (1f-accent.red)*gain, 0f, 0f,
        accent.green*gain, 0f, (1f-accent.green)*gain, 0f, 0f,
        accent.blue*gain, 0f, (1f-accent.blue)*gain, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )))
}

/** Icons are cropped without tint; only loading-scene accents may be recolored. */
private class ArtworkPainter(private val bitmap: ImageBitmap, private val cell: Int?,
    private val accent: ColorFilter? = null, private val roomLeds: Boolean = false) : Painter() {
    private val cellSize = IntSize(bitmap.width / 3, bitmap.height / 2)
    // Os quatro icones principais conservam a tela original 3:2.
    // Atualizar e configuracoes conservam a tela quadrada original.
    private val sourceSize = if (cell == null) IntSize(bitmap.width, bitmap.height)
        else if (cell <= 3) IntSize(cellSize.width, cellSize.width * 2 / 3) else cellSize
    private val offset = if (cell == null) IntOffset.Zero else IntOffset((cell % 3) * cellSize.width, (cell / 3) * cellSize.height)
    override val intrinsicSize = Size(sourceSize.width.toFloat(), sourceSize.height.toFloat())
    override fun DrawScope.onDraw() {
        fun draw(filter: ColorFilter?) {
            drawImage(bitmap, srcOffset = offset, srcSize = sourceSize,
                dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
                colorFilter = filter, filterQuality = FilterQuality.High)
        }
        draw(if (roomLeds) null else accent)
        if (roomLeds && accent != null) {
            // Only the two vertical LED strips beside the TV, in original-photo coordinates.
            // Feathering keeps the surrounding room and reflections intact.
            for (range in listOf(1028f to 1105f, 3053f to 3130f)) {
                val left=range.first/3840f*size.width; val right=range.second/3840f*size.width
                val bottom=1340f/2160f*size.height
                val bounds=Rect(left,0f,right,bottom)
                drawContext.canvas.saveLayer(bounds, Paint())
                try {
                    clipRect(left, 0f, right, bottom) {
                        draw(accent)
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White, Color.White, Color.Transparent),left,right),
                            topLeft = androidx.compose.ui.geometry.Offset(left,0f), size = Size(right-left,bottom), blendMode = BlendMode.DstIn)
                    }
                } finally { drawContext.canvas.restore() }
            }
        }
    }
}

@Composable
fun themePainterResource(id: Int, targetSize: IntSize? = null): Painter {
    val theme = ThemeState.selected
    val epoch by ThemeArtworkEpoch.collectAsState()
    ThemeArtworkCache.activate(theme.name)
    // Only the legacy Kids logo crop remains; the current Home uses separate PNG assets.
    if (id == R.drawable.kids_bear) return standardHomePainter(theme, 3, epoch)
    val pinned = id in listOf(R.drawable.kids_home_blue, R.drawable.kids_home_pink, R.drawable.kids_home_objects, R.drawable.kids_category_symbols, R.drawable.kids_brand_marks, R.drawable.kids_bear,
        R.drawable.iptv_premium_logo, R.drawable.series_living_room)
    val resolvedTarget = targetSize ?: when (id) {
        R.drawable.kids_home_blue, R.drawable.kids_home_pink -> IntSize(1920,1080)
        R.drawable.kids_home_objects -> IntSize(1536,1024)
        R.drawable.kids_category_symbols -> IntSize(1024,512)
        R.drawable.kids_brand_marks -> IntSize(768,384)
        R.drawable.kids_bear -> IntSize(320,320)
        R.drawable.series_living_room -> IntSize(2560, 1440)
        R.drawable.iptv_premium_logo -> IntSize(768, 512)
        else -> null
    }
    val set = artwork(theme)
    if (set == null && resolvedTarget == null && !pinned) return androidx.compose.ui.res.painterResource(id)
    val asset = when {
        id in listOf(R.drawable.kids_bear, R.drawable.kids_home_blue, R.drawable.kids_home_pink, R.drawable.kids_home_objects,
            R.drawable.kids_category_symbols, R.drawable.kids_brand_marks) -> id
        set == null -> id
        id == R.drawable.iptv_premium_logo -> set.logo
        id == R.drawable.movie_session_loading_bg || id == R.drawable.series_living_room -> id
        id in listOf(R.drawable.home_background, R.drawable.actor_cinematic_background) -> set.background
        else -> return androidx.compose.ui.res.painterResource(id)
    }
    val context = LocalContext.current.applicationContext
    val sourceCell: Int? = null
    var image by remember(asset, sourceCell, resolvedTarget, theme, epoch) { mutableStateOf(ThemeArtworkCache.cached(asset, sourceCell, resolvedTarget)) }
    LaunchedEffect(asset, sourceCell, resolvedTarget, theme, epoch) { image = ThemeArtworkCache.load(context, asset, sourceCell, resolvedTarget, pinned, theme.name) }
    val loading = set != null && id in listOf(R.drawable.kids_bear, R.drawable.movie_session_loading_bg, R.drawable.series_living_room)
    return remember(image, sourceCell, resolvedTarget, loading, theme) {
        image?.let { ArtworkPainter(it, if (resolvedTarget == null) sourceCell else null,
            if (id == R.drawable.kids_bear && set == null) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(.85f,0f,0f,0f,0f, 0f,.85f,0f,0f,0f, 0f,0f,.85f,0f,0f, 0f,0f,0f,1f,0f)))
            else if (loading) loadingAccent(theme, id == R.drawable.kids_bear) else null, id == R.drawable.series_living_room && loading) } ?: ColorPainter(Color.Transparent)
    }
}

/** Android 9–11 have no Compose render blur. Cache a small preblurred wallpaper off the UI thread. */
@Composable
fun themeBackdropPainter(id: Int, radius: Dp): Painter {
    if (Build.VERSION.SDK_INT >= 31) return themePainterResource(id)
    val theme = ThemeState.selected
    val epoch by ThemeArtworkEpoch.collectAsState()
    ThemeArtworkCache.activate(theme.name)
    val set = artwork(theme)
    val asset = if (id == R.drawable.home_background || id == R.drawable.actor_cinematic_background) set?.background ?: id else id
    val target = IntSize(384, 216)
    val screenWidth = LocalConfiguration.current.screenWidthDp.coerceAtLeast(1)
    val smallRadius = (radius.value * target.width / screenWidth).roundToInt().coerceIn(1, 24)
    val context = LocalContext.current.applicationContext
    var image by remember(asset, smallRadius, epoch) { mutableStateOf(ThemeArtworkCache.cachedBlur(asset, target, smallRadius)) }
    LaunchedEffect(asset, smallRadius, epoch) { image = ThemeArtworkCache.loadBlur(context, asset, target, smallRadius) }
    return remember(image) { image?.let { ArtworkPainter(it, null) } ?: ColorPainter(Color.Transparent) }
}

@Composable
fun rememberSeriesRoomReady(): Boolean {
    val theme = ThemeState.selected
    val epoch by ThemeArtworkEpoch.collectAsState()
    ThemeArtworkCache.activate(theme.name)
    val context = LocalContext.current.applicationContext
    val target = IntSize(2560, 1440)
    var ready by remember(theme, epoch) { mutableStateOf(ThemeArtworkCache.cached(R.drawable.series_living_room, null, target) != null) }
    LaunchedEffect(theme, epoch) { ready = ThemeArtworkCache.load(context, R.drawable.series_living_room, null, target, true, theme.name) != null }
    return ready
}

/** Shared approved material, real five-icon artwork per theme, no runtime tint. */
private fun standardHomeAsset(theme: PremiumTheme): Int = when(theme) {
    PremiumTheme.GOLD -> R.drawable.standard_home_gold
    PremiumTheme.BLUE -> R.drawable.standard_home_blue
    PremiumTheme.PURPLE -> R.drawable.standard_home_purple
    PremiumTheme.RED -> R.drawable.standard_home_red
    PremiumTheme.PINK -> R.drawable.standard_home_pink
 }
private fun standardHomeBounds(theme: PremiumTheme): Array<IntArray> = when(theme) {
    PremiumTheme.GOLD -> arrayOf(intArrayOf(9, 191, 456, 516), intArrayOf(456, 168, 874, 543), intArrayOf(883, 168, 1299, 542), intArrayOf(1308, 156, 1715, 562), intArrayOf(1722, 183, 2167, 544))
    PremiumTheme.BLUE -> arrayOf(intArrayOf(12, 211, 453, 517), intArrayOf(460, 175, 873, 541), intArrayOf(881, 176, 1299, 533), intArrayOf(1308, 169, 1714, 558), intArrayOf(1727, 200, 2167, 536))
    PremiumTheme.PURPLE -> arrayOf(intArrayOf(13, 216, 468, 520), intArrayOf(468, 188, 873, 545), intArrayOf(881, 185, 1292, 543), intArrayOf(1303, 162, 1708, 560), intArrayOf(1723, 201, 2160, 543))
    PremiumTheme.RED -> arrayOf(intArrayOf(9, 207, 452, 511), intArrayOf(457, 192, 872, 542), intArrayOf(887, 179, 1304, 539), intArrayOf(1316, 178, 1719, 559), intArrayOf(1730, 197, 2163, 542))
    PremiumTheme.PINK -> arrayOf(intArrayOf(10, 192, 461, 513), intArrayOf(466, 165, 875, 535), intArrayOf(890, 172, 1298, 533), intArrayOf(1309, 159, 1706, 549), intArrayOf(1723, 185, 2165, 536))
}
private class StandardHomePainter(private val image: ImageBitmap, private val bounds: IntArray): Painter() {
    override val intrinsicSize = Size((bounds[2]-bounds[0]).toFloat(),(bounds[3]-bounds[1]).toFloat())
    override fun DrawScope.onDraw() {
        val sx=image.width/2172f;val sy=image.height/724f
        drawImage(image,srcOffset=IntOffset((bounds[0]*sx).roundToInt(),(bounds[1]*sy).roundToInt()),
            srcSize=IntSize(((bounds[2]-bounds[0])*sx).roundToInt().coerceAtLeast(1),((bounds[3]-bounds[1])*sy).roundToInt().coerceAtLeast(1)),
            dstSize=IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1)),filterQuality=FilterQuality.High)
    }
}
@Composable
private fun standardHomePainter(theme: PremiumTheme,index:Int,epoch:Long):Painter {
    val context=LocalContext.current.applicationContext
    val asset=standardHomeAsset(theme)
    val target=IntSize(1600,534)
    var bitmap by remember(asset,epoch) { mutableStateOf(ThemeArtworkCache.cached(asset,null,target)) }
    LaunchedEffect(asset,epoch) { bitmap=ThemeArtworkCache.load(context,asset,null,target,true,theme.name) }
    return remember(bitmap,index,theme) {
        bitmap?.let { StandardHomePainter(it,standardHomeBounds(theme)[index]) } ?: ColorPainter(Color.Transparent)
    }
}
