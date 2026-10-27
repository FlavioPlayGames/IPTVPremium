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
    PremiumTheme.BLUE -> ArtworkSet(marbleWallpaperAsset(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.light), R.drawable.theme_blue_logo)
    PremiumTheme.PURPLE -> ArtworkSet(marbleWallpaperAsset(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.light), R.drawable.theme_purple_logo)
    PremiumTheme.RED -> ArtworkSet(marbleWallpaperAsset(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.light), R.drawable.theme_red_logo)
    PremiumTheme.PINK -> ArtworkSet(marbleWallpaperAsset(br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.light), R.drawable.theme_pink_logo)
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
                    Bitmap.createBitmap(blurred, raw.width, raw.height, Bitmap.Config.ARGB_8888).also { it.prepareToDraw() }.asImageBitmap()
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
                val options=BitmapFactory.Options().apply {
                    inScaled=false
                    inPreferredConfig=Bitmap.Config.ARGB_8888
                }
                // Only this 4K loading photograph needs a smaller decode. Home and icons are unchanged.
                if(id==R.drawable.series_living_room && target!=null) {
                    val bounds=BitmapFactory.Options().apply { inScaled=false;inJustDecodeBounds=true }
                    BitmapFactory.decodeResource(context.resources,id,bounds)
                    options.inSampleSize=seriesRoomDecodeSample(bounds.outWidth,bounds.outHeight,target.width,target.height)
                }
                val raw = cache.get(rawKey) ?: BitmapFactory.decodeResource(context.resources,id,options)
                    ?.asImageBitmap()?.also {
                        if(options.inSampleSize<=1)put(rawKey,it,theme,false,"raw",epoch)
                    } ?: return@withPermit null
                if (target == null) { raw.asAndroidBitmap().prepareToDraw(); if (pin) put(cacheKey, raw, theme, true, cacheKey, epoch); return@withPermit raw }
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
                    return@withPermit Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true).also { it.prepareToDraw() }.asImageBitmap()
                        .also { put(cacheKey, it, theme, pin, cacheKey, epoch) }
                }
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, left, top, width, height)
                val scaled = resampleArtwork(pixels, width, height, outWidth, outHeight)
                Bitmap.createBitmap(scaled, outWidth, outHeight, Bitmap.Config.ARGB_8888)
                    .also { it.prepareToDraw() }.asImageBitmap().also { put(cacheKey, it, theme, pin, cacheKey, epoch) }
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
    val pinned = id in listOf(R.drawable.kids_home_blue, R.drawable.kids_home_pink, R.drawable.kids_home_objects, R.drawable.kids_category_symbols, R.drawable.kids_brand_marks,
        R.drawable.iptv_premium_logo, R.drawable.series_living_room)
    val resolvedTarget = targetSize ?: when (id) {
        R.drawable.kids_home_blue, R.drawable.kids_home_pink -> IntSize(1920,1080)
        R.drawable.kids_home_objects -> IntSize(1536,1024)
        R.drawable.kids_category_symbols -> IntSize(1024,512)
        R.drawable.kids_brand_marks -> IntSize(768,384)
        R.drawable.series_living_room -> IntSize(1920, 1080)
        R.drawable.iptv_premium_logo -> IntSize(768, 512)
        else -> null
    }
    val set = artwork(theme)
    if (set == null && resolvedTarget == null && !pinned) return androidx.compose.ui.res.painterResource(id)
    val asset = when {
        id in listOf(R.drawable.kids_home_blue, R.drawable.kids_home_pink, R.drawable.kids_home_objects,
            R.drawable.kids_category_symbols, R.drawable.kids_brand_marks) -> id
        set == null -> id
        id == R.drawable.iptv_premium_logo -> set.logo
        id == R.drawable.series_living_room -> id
        id in listOf(R.drawable.home_background, R.drawable.actor_cinematic_background) -> set.background
        else -> return androidx.compose.ui.res.painterResource(id)
    }
    val context = LocalContext.current.applicationContext
    val sourceCell: Int? = null
    var image by remember(asset, sourceCell, resolvedTarget, theme, epoch) { mutableStateOf(ThemeArtworkCache.cached(asset, sourceCell, resolvedTarget)) }
    LaunchedEffect(asset, sourceCell, resolvedTarget, theme, epoch) { image = ThemeArtworkCache.load(context, asset, sourceCell, resolvedTarget, pinned, theme.name) }
    val loading = set != null && id in listOf(R.drawable.series_living_room)
    return remember(image, sourceCell, resolvedTarget, loading, theme) {
        image?.let { ArtworkPainter(it, if (resolvedTarget == null) sourceCell else null,
            if (id in listOf(R.drawable.home_background,R.drawable.actor_cinematic_background)) marbleBackdropFilter(theme,br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.light)
            else if (loading) loadingAccent(theme, false) else null, id == R.drawable.series_living_room && loading) } ?: ColorPainter(Color.Transparent)
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
    val target = IntSize(1920, 1080)
    var ready by remember(theme, epoch) { mutableStateOf(ThemeArtworkCache.cached(R.drawable.series_living_room, null, target) != null) }
    LaunchedEffect(theme, epoch) {
        val started=android.os.SystemClock.elapsedRealtime()
        val cached=ThemeArtworkCache.cached(R.drawable.series_living_room,null,target)!=null
        ready=ThemeArtworkCache.load(context,R.drawable.series_living_room,null,target,true,theme.name)!=null
        android.util.Log.i("SeriesRoomPerf","cached=$cached ready=$ready elapsedMs=${android.os.SystemClock.elapsedRealtime()-started} target=1920x1080")
    }
    return ready
}

/** Reuse the retained Home sheet instead of a standalone legacy bear. */
@Composable
internal fun kidsHomePainter(): Painter {
    val theme=ThemeState.selected
    val asset=br.com.flaviotorres.iptvpremium.ui.home.MarbleHomePngAssets.forMode(
        br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle.light,theme)[3]
    val context=LocalContext.current.applicationContext
    val epoch by ThemeArtworkEpoch.collectAsState()
    var image by remember(asset,epoch){mutableStateOf(MarbleArtworkCache.peek(asset.resource))}
    LaunchedEffect(asset,epoch){image=MarbleArtworkCache.load(context,asset.resource)}
    return remember(image,asset,theme){image?.let{HomeBearPainter(it,asset,marbleAccentFilter(theme))}?:ColorPainter(Color.Transparent)}
}
private class HomeBearPainter(private val image:ImageBitmap,
    private val asset:br.com.flaviotorres.iptvpremium.ui.home.MarblePngAsset,
    private val tint:androidx.compose.ui.graphics.ColorFilter?):Painter() {
    override val intrinsicSize=Size(asset.crop.width.toFloat(),asset.crop.height.toFloat())
    override fun DrawScope.onDraw() {
        val r=asset.crop
        val sx=image.width.toFloat()/asset.width
        val sy=image.height.toFloat()/asset.height
        drawImage(image,srcOffset=IntOffset((r.left*sx).roundToInt(),(r.top*sy).roundToInt()),
            srcSize=IntSize((r.width*sx).roundToInt(),(r.height*sy).roundToInt()),
            dstSize=IntSize(size.width.roundToInt(),size.height.roundToInt()),colorFilter=tint,filterQuality=FilterQuality.High)
    }
}

/** Static cached blur of the selected Kids Home; no blur animation or per-frame processing. */
@Composable
internal fun KidsLoadingWallpaper(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val asset=br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.style.home
    val context=LocalContext.current.applicationContext
    val epoch by ThemeArtworkEpoch.collectAsState()
    val target=IntSize(384,216)
    val radius=10
    var image by remember(asset,epoch){mutableStateOf(ThemeArtworkCache.cachedBlur(asset,target,radius))}
    LaunchedEffect(asset,epoch){image=ThemeArtworkCache.loadBlur(context,asset,target,radius)}
    val bitmap=image
    if(bitmap!=null) androidx.compose.foundation.Image(bitmap,null,modifier,
        contentScale=androidx.compose.ui.layout.ContentScale.Crop)
    else androidx.compose.foundation.Canvas(modifier){drawRect(Color(0xFF101322))}
}

/** Power-of-two decode, preserving at least the requested image dimensions. */
internal fun seriesRoomDecodeSample(width:Int,height:Int,targetWidth:Int,targetHeight:Int):Int {
    if(width<=0 || height<=0 || targetWidth<=0 || targetHeight<=0)return 1
    var sample=1
    while(sample<=Int.MAX_VALUE/2 && width/(sample*2)>=targetWidth && height/(sample*2)>=targetHeight)
        sample*=2
    return sample
}
