package br.com.flaviotorres.iptvpremium.ui.movies

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import br.com.flaviotorres.iptvpremium.data.cache.loadWhilePosterVisible
import br.com.flaviotorres.iptvpremium.data.cache.PosterCategoryPolicy
import br.com.flaviotorres.iptvpremium.data.cache.interruptPosterDownload
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
    private val urlLocks = br.com.flaviotorres.iptvpremium.data.cache.PosterRequestLocks()
    private val bitmapLocks = br.com.flaviotorres.iptvpremium.data.cache.PosterRequestLocks()

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
            val workers = if (visible) PosterCategoryPolicy.GROUP else 2
            repeat(minOf(workers, batch.size)) { worker -> launch(Dispatchers.IO) {
                for (index in worker until batch.size step workers) {
                    currentCoroutineContext().ensureActive()
                    try { prefetch(context, batch[index], priority = visible, kind = kind, duringNavigation = !visible) }
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

    suspend fun prefetch(context: Context, rawUrl: String?, priority: Boolean = false, kind: PosterKind = PosterKind.MOVIES, duringNavigation: Boolean = false): Boolean {
        configureRam(context)
        val url = validUrl(rawUrl) ?: return false
        return withContext(Dispatchers.IO) {
            val directory = persistentDirectory(context, kind) ?: return@withContext false
            val budget = br.com.flaviotorres.iptvpremium.core.storage.ImageDiskCache.get(context)
            budget.prefer(protectedHomeFiles)
            budget.retain(File(directory, "${sha256(url)}.img")).use {
                ensureFile(context, url, background = !priority, kind = kind, duringNavigation = duringNavigation) != null
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
    private suspend fun prepareCached(context: Context, rawUrl: String?, kind: PosterKind = PosterKind.MOVIES) {
        val url = validUrl(rawUrl) ?: return
        if (memoryCache.get(sha256(url)) != null) return
        withContext(Dispatchers.IO) {
            val directory = persistentDirectory(context, kind) ?: return@withContext
            val file = File(directory, "${sha256(url)}.img")
            br.com.flaviotorres.iptvpremium.core.storage.ImageDiskCache.get(context).retain(file).use {
                if (file.isFile && file.length() > 0L) decodeCachedFile(url, file)
            }
        }
    }

    suspend fun prepareNearestCategoryRow(context: Context, kind: PosterKind, urls: List<String?>) {
        configureRam(context)
        for (url in urls.take(PosterCategoryPolicy.GROUP)) {
            currentCoroutineContext().ensureActive()
            if (br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.pressured.value) return
            prepareCached(context, url, kind)
            kotlinx.coroutines.yield()
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
        val url = validUrl(rawUrl) ?: return null
        peekBitmap(url)?.let { return it }
        return loadVisiblePoster(context, url, kind)
    }

    private suspend fun loadVisiblePoster(context: Context, url: String, kind: PosterKind): ImageBitmap? {
        peekBitmap(url)?.let { return it }
        prepareFailures(context)
        if(knownMissing(url))return loadCached(context,url)
        currentCoroutineContext().ensureActive()
        val started = System.nanoTime()
        val result=load(context,url,kind)
        currentCoroutineContext().ensureActive()
        android.util.Log.d("PosterLoadPerf", "kind=${kind.name} stage=load success=${result != null} elapsedMs=${(System.nanoTime()-started)/1_000_000}")
        if(result==null && !br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.value && !br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.busy.value &&
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
        val started = System.nanoTime()
        val file = ensureFile(context, url, background = false, kind = kind) ?: return null
        val fileReady = System.nanoTime()
        val bitmap = decodeCachedFile(url, file)
        android.util.Log.d("PosterLoadPerf", "kind=${kind.name} stage=ready success=${bitmap != null} " +
            "fileMs=${(fileReady-started)/1_000_000} decodeMs=${(System.nanoTime()-fileReady)/1_000_000}")
        return bitmap
    }

    private suspend fun decodeCachedFile(url: String, file: File): ImageBitmap? =
        decodeCachedKey(sha256(url), file)

    private suspend fun decodeCachedKey(key: String, file: File): ImageBitmap? =
        withContext(Dispatchers.Default) {
            bitmapLocks.withLock(key) {
            memoryCache.get(key)?.let { return@withLock it }
            decodeSlots.withPermit {
            memoryCache.get(key)?.let { return@withPermit it }

            val bitmap = try {
                br.com.flaviotorres.iptvpremium.data.cache.PosterResolution.decode(file, Bitmap.Config.RGB_565)
                    ?.also { it.prepareToDraw() }?.asImageBitmap()
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
        }

    private suspend fun ensureFile(context: Context, url: String, background: Boolean,
        kind: PosterKind = PosterKind.MOVIES, duringNavigation: Boolean = false): File? = withContext(Dispatchers.IO) {
        urlLocks.withLock(url) {
            val directory = persistentDirectory(context, kind) ?: return@withLock null
            val key = sha256(url)
            val file = File(directory, "$key.img")
            val slot = ledger(context, kind)?.acquire(key) ?: return@withLock null
            slot.use {
                if (file.isFile && file.length() > 0L) {
                    file.setLastModified(System.currentTimeMillis())
                    return@withLock file
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
                            if (lease.temporary.renameTo(file)) return@withLock file
                        }
                    }
                }
                val queuedAt = System.nanoTime()
                val downloaded = networkBudget.run(background) {
                    val downloadAt = System.nanoTime()
                    val result = download(context, url, file, background, duringNavigation, kind)
                    android.util.Log.d("PosterLoadPerf", "kind=${kind.name} stage=download background=$background success=$result " +
                        "queueMs=${(downloadAt-queuedAt)/1_000_000} fetchAndPrepareMs=${(System.nanoTime()-downloadAt)/1_000_000}")
                    result
                }
                if (!downloaded) return@withLock null
                file.takeIf { it.isFile && it.length() > 0L }
            }
        }
    }

    private suspend fun download(context: Context, url: String, target: File, background: Boolean, duringNavigation: Boolean = false, kind: PosterKind = PosterKind.MOVIES): Boolean {
        var reservation: br.com.flaviotorres.iptvpremium.core.storage.CacheStorageBudget.Reservation? = null
        var connection: HttpURLConnection? = null
        var backgroundInterrupt: kotlinx.coroutines.Job? = null
        var cancellationInterrupt: kotlinx.coroutines.Job? = null
        var outcome = "unknown"
        var httpStatus = 0
        var exceptionType = "none"
        fun reject(reason: String): Boolean { outcome = reason; return false }
        try {
            currentCoroutineContext().ensureActive()
            if (br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.value ||
                br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.busy.value) return reject("priority_busy")
            val parent = target.parentFile ?: return reject("storage_path")
            if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) return reject("storage_directory")
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            val activeConnection = connection
            cancellationInterrupt = kotlinx.coroutines.CoroutineScope(currentCoroutineContext())
                .interruptPosterDownload { activeConnection.disconnect() }
            backgroundInterrupt = if(background && !duringNavigation)
                br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.interruptBackground { activeConnection.disconnect() }
            else br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority
                .interruptBackgroundOnSelection { activeConnection.disconnect() }
            currentCoroutineContext().ensureActive()
            connection.connect()
            httpStatus = connection.responseCode
            if (httpStatus !in 200..299) return reject("http_status")
            if (connection.contentLengthLong > MAX_POSTER_BYTES) return reject("image_too_large")
            reservation = br.com.flaviotorres.iptvpremium.core.storage.AppCacheStorage.get(context)
                .reserve(target, connection.contentLengthLong.takeIf { it > 0 }?.coerceAtLeast(1_000_000L) ?: MAX_POSTER_BYTES) ?: return reject("cache_budget")
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
                        if (byteCount > reservation.limit) return reject("image_too_large")
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (byteCount == 0L) return reject("empty_body")
            if (!br.com.flaviotorres.iptvpremium.data.cache.PosterResolution.prepareDownloaded(temporary, reservation.limit)) return reject("image_prepare")
            val saved = temporary.renameTo(target)
            outcome = if (saved) "success" else "storage_commit"
            return saved
        } catch (cancelled: CancellationException) {
            outcome = "cancelled"
            throw cancelled
        } catch (error: Exception) {
            exceptionType = error.javaClass.simpleName
            try { currentCoroutineContext().ensureActive() }
            catch (cancelled: CancellationException) { outcome = "cancelled"; throw cancelled }
            outcome = when {
                br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.value ||
                    br.com.flaviotorres.iptvpremium.data.cache.CatalogImportPriority.busy.value -> "priority_interrupted"
                background && !duringNavigation && !br.com.flaviotorres.iptvpremium.data.cache.UserActivityPriority.idle.value -> "activity_interrupted"
                error is java.net.SocketTimeoutException -> "timeout"
                error is java.io.IOException -> "io_error"
                else -> "error"
            }
            return false
        } finally {
            android.util.Log.d("PosterDownloadResult", "kind=${kind.name} outcome=$outcome background=$background httpStatus=$httpStatus exception=$exceptionType")
            cancellationInterrupt?.cancel()
            backgroundInterrupt?.cancel()
            reservation?.close()
            connection?.disconnect()
        }
    }


}

