package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.flaviotorres.iptvpremium.data.cache.CatalogVerticalIntent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest

/** Width belongs to the poster itself; surplus space is distributed between cells. */
internal val LocalCatalogPosterWidth = staticCompositionLocalOf { androidx.compose.ui.unit.Dp.Unspecified }
internal val LocalCatalogMetadataHeight = staticCompositionLocalOf { 34.dp }

/** One continuous scroll follows the latest DPAD destination, even while the key repeats. */
internal class CatalogGridNavigator(private val grid: LazyGridState, private val scope: CoroutineScope,
    private val columns: Int, private val count: () -> Int, private val load: (Int) -> Unit,
    private val manual: () -> Unit,
    private val onTop: (() -> Unit)? = null,
    private val onLeft: (() -> Unit)? = null) {
    private val requesters = mutableMapOf<Int, FocusRequester>()
    private val revision = MutableStateFlow(0L)
    private val intent = CatalogVerticalIntent(columns)
    private var heldDirection: Key? = null
    private var releaseSerial = 0
    private var job: Job? = null
    private var diagnosticPhase = "idle"
    private var diagnosticFrames = 0
    private var diagnosticDistance = 0f
    private var diagnosticVelocity = 0f
    private var diagnosticElapsedNanos = 0L
    internal fun motionDiagnostic(): String =
        "build=continuous-trace-1 phase=$diagnosticPhase frames=$diagnosticFrames " +
            "elapsedMs=${diagnosticElapsedNanos / 1_000_000} distance=$diagnosticDistance " +
            "velocity=$diagnosticVelocity desiredRow=$desiredFirstRow current=$currentIndex " +
            "active=${job?.isActive} attached=${requesters.containsKey(currentIndex)}"

    val host = FocusRequester()
    private var currentIndex = grid.firstVisibleItemIndex
    private var filterIdentity: Any? = null
    private var desiredFirstRow: Int? = null
    private var ownsFocus = false
    fun gridFocusChanged(focused: Boolean) {
        val leaving = ownsFocus && !focused
        ownsFocus = focused
        // A pending movement must never reclaim focus from the toolbar/sidebar.
        // Transfers between the parking host and a poster remain internal.
        if (leaving && !transferring) cancel()
    }
    private var transferring = false
    private var moving by mutableStateOf(false)
    private var visualIndex by mutableIntStateOf(-1)
    fun isMovingSelection(index: Int): Boolean = moving && visualIndex == index

    suspend fun observeExternalScroll() {
        snapshotFlow {
            val scrolling = grid.isScrollInProgress
            // Ignore per-pixel updates during movement, but observe atomic wheel jumps.
            scrolling to if (scrolling) 0 else grid.firstVisibleItemScrollOffset
        }.collectLatest { (scrolling, _) ->
            if (!scrolling) {
                // A wheel/drag may stop between rows. Wait for its next tick,
                // then use the same continuous motion to settle on a whole row.
                delay(100)
                if (ownsFocus && job?.isActive != true && grid.firstVisibleItemScrollOffset > 0 && count() > 0) {
                    val step = rowStep()
                    if (step > 0f) {
                        val nearest = grid.firstVisibleItemIndex / columns +
                            if (grid.firstVisibleItemScrollOffset >= step / 2f) 1 else 0
                        val last = ((count() + columns - 1) / columns - viewportRows()).coerceAtLeast(0)
                        desiredFirstRow = nearest.coerceIn(0, last)
                        val first = desiredFirstRow!! * columns
                        val end = minOf(count() - 1, first + viewportRows() * columns - 1)
                        if (currentIndex !in first..end) currentIndex = (first + currentIndex % columns).coerceAtMost(end)
                        visualIndex = currentIndex
                        startMovement(takeFocus = ownsFocus, keepRow = true)
                    }
                }
            }
        }
    }

    fun hostFocusChanged(focused: Boolean) {
        if (transferring) return
        if (!focused) {
            if (job?.isActive == true) cancel()
        } else if (job?.isActive != true && count() > 0) {
            desiredFirstRow = grid.firstVisibleItemIndex / columns
            reveal(currentIndex)
            startMovement()
        }
    }
    fun attach(index: Int, requester: FocusRequester) {
        requesters[index] = requester
        revision.value++
    }
    fun detach(index: Int, requester: FocusRequester) {
        if (requesters[index] === requester) requesters.remove(index)
    }
    fun focused(index: Int) {
        if (intent.desired == null || intent.desired == index) currentIndex = index
        intent.focused(index)
    }
    fun cancel() {
        job?.cancel(); job = null; heldDirection = null; moving = false; desiredFirstRow = null; intent.clear()
    }
    fun reset() { cancel(); currentIndex = 0 }
    fun filterChanged(key: Any) {
        // The first key belongs to restored screen state, not a user filter change.
        if (filterIdentity != null && filterIdentity != key) reset()
        filterIdentity = key
    }
    fun enter() { manual(); host.requestFocus() }
    fun hostKey(event: KeyEvent): Boolean = key(currentIndex, event)

    private fun rowStep(): Float {
        val layout = grid.layoutInfo
        return ((layout.visibleItemsInfo.firstOrNull()?.size?.height ?: 0) + layout.mainAxisItemSpacing).toFloat()
    }
    private fun viewportRows(): Int {
        val layout = grid.layoutInfo
        val step = rowStep()
        if (step <= 0f) return 2
        val usable = layout.viewportSize.height - layout.beforeContentPadding - layout.afterContentPadding
        return ((usable + layout.mainAxisItemSpacing) / step + .001f).toInt().coerceAtLeast(1)
    }
    private fun reveal(index: Int) {
        val rows = viewportRows()
        val first = desiredFirstRow ?: (grid.firstVisibleItemIndex / columns)
        val row = index / columns
        val next = when {
            row < first -> row
            row >= first + rows -> row - rows + 1
            else -> first
        }
        val last = ((count() + columns - 1) / columns - rows).coerceAtLeast(0)
        desiredFirstRow = next.coerceIn(0, last)
    }
    private fun transfer(requester: FocusRequester?): Boolean {
        transferring = true
        return try { runCatching { requester?.requestFocus() == true }.getOrDefault(false) }
        finally { transferring = false }
    }

    private fun startMovement(takeFocus: Boolean = true, keepRow: Boolean = false) {
        if (job?.isActive == true) return
        moving = takeFocus
        visualIndex = currentIndex
        if (takeFocus) transfer(host)
        job = scope.launch {
            try {
                while (isActive && count() > 0) {
                    currentIndex = currentIndex.coerceIn(0, count() - 1)
                    load(currentIndex)
                    diagnosticPhase = "await-layout"
                    val laidOut = withTimeoutOrNull(2_500L) {
                        snapshotFlow { grid.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
                    }
                    if (laidOut == null) return@launch
                    if (!keepRow) reveal(currentIndex)
                    // A critically damped spring retains its velocity when the target
                    // changes. Repeated arrows never restart a tween or jump a row.
                    diagnosticPhase = "await-scroll-lock"
                    grid.scroll(MutatePriority.UserInput) {
                        diagnosticPhase = "animate"
                        diagnosticFrames = 0
                        diagnosticElapsedNanos = 0L
                        var firstFrame = 0L
                        var motion = CatalogScrollMotion()
                        var observedRelease = releaseSerial
                        var lastFrame = 0L
                        var loaded = currentIndex
                        var position = (grid.firstVisibleItemIndex / columns) * rowStep() + grid.firstVisibleItemScrollOffset
                        while (true) {
                            val now = withFrameNanos { it }
                            ensureActive()
                            val dt = if (lastFrame == 0L) 1f / 60f else
                                ((now - lastFrame) / 1_000_000_000f).coerceIn(.001f, .05f)
                            lastFrame = now
                            if (firstFrame == 0L) firstFrame = now
                            diagnosticElapsedNanos = now - firstFrame
                            diagnosticFrames++
                            if (loaded != currentIndex) { load(currentIndex); loaded = currentIndex }
                            val step = rowStep()
                            if (step <= 0f) break
                            if (observedRelease != releaseSerial) {
                                observedRelease = releaseSerial
                                motion = CatalogScrollMotion()
                                position = (grid.firstVisibleItemIndex / columns) * step + grid.firstVisibleItemScrollOffset
                            }
                            val distance = (desiredFirstRow ?: 0) * step - position
                            diagnosticDistance = distance
                            diagnosticVelocity = motion.velocity
                            // LazyGrid rounds physical offsets to integers. Below one pixel,
                            // consumption may stop while the spring still has fractional error.
                            if (kotlin.math.abs(distance) <= 2f && kotlin.math.abs(motion.velocity) < 48f) {
                                scrollBy(distance)
                                break
                            }
                            // A bounded continuous spring follows repeated commands
                            // without allocating an animation object for each frame.
                            val delta = motion.advance(distance, dt, step, speedFactor = if (heldDirection != null) 3.59375f else 1f)
                            val consumed = scrollBy(delta)
                            // Integrate the continuous trajectory, not LazyGrid's rounded
                            // consumption. At a boundary, consumed pixels may stop before
                            // the spring settles; its virtual position must still converge.
                            position += delta
                            if (kotlin.math.abs(consumed) < .01f && kotlin.math.abs(delta) > .5f) break
                        }
                    }
                    // Floating-point consumption can leave one physical pixel after
                    // reversing direction. Commit the exact row before handing focus
                    // back, otherwise the idle observer may start another host move.
                    diagnosticPhase = "align-row"
                    val available = count()
                    if (available <= 0) return@launch
                    val settledRow = desiredFirstRow ?: (grid.firstVisibleItemIndex / columns)
                    val settledIndex = (settledRow * columns).coerceIn(0, available - 1)
                    if (grid.firstVisibleItemIndex != settledIndex || grid.firstVisibleItemScrollOffset != 0) {
                        grid.scrollToItem(settledIndex, 0)
                    }
                    ensureActive()
                    if (desiredFirstRow != settledRow) continue
                    if (!takeFocus) return@launch
                    val target = currentIndex
                    diagnosticPhase = "await-requester"
                    val ready = withTimeoutOrNull(2_500L) {
                        revision.first { currentIndex != target || requesters[target] != null }
                    }
                    if (currentIndex != target) continue
                    if (ready == null) { intent.clear(); return@launch }
                    diagnosticPhase = "transfer-focus"
                    repeat(3) { attempt ->
                        if (attempt > 0) withFrameNanos { }
                        ensureActive()
                        if (currentIndex != target) return@repeat
                        if (transfer(requesters[target])) return@launch
                    }
                    if (currentIndex == target) { intent.clear(); return@launch }
                }
            } finally { diagnosticPhase = "finished-$diagnosticPhase"; moving = false }
        }
    }

    fun key(index: Int, event: KeyEvent): Boolean {
        val direction = event.key
        val arrow = direction == Key.DirectionDown || direction == Key.DirectionUp ||
            direction == Key.DirectionLeft || direction == Key.DirectionRight
        if (!arrow) return job?.isActive == true && (direction == Key.DirectionCenter || direction == Key.Enter)
        val total = count()
        if (event.type == KeyEventType.KeyUp && heldDirection == direction) {
            heldDirection = null
            val step = rowStep()
            if (step > 0f && total > 0) {
                val last = ((total + columns - 1) / columns - viewportRows()).coerceAtLeast(0)
                val nearest = (grid.firstVisibleItemIndex / columns +
                    if (grid.firstVisibleItemScrollOffset >= step / 2f) 1 else 0).coerceIn(0, last)
                desiredFirstRow = nearest
                val row = nearest + if (direction == Key.DirectionDown) viewportRows() - 1 else 0
                currentIndex = (row * columns + currentIndex % columns).coerceAtMost(total - 1)
                visualIndex = currentIndex
                intent.clear()
                releaseSerial++
                revision.value++
                startMovement(keepRow = true)
            }
            return true
        }
        val isHeldRepeat = event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount > 0 &&
            (direction == Key.DirectionDown || direction == Key.DirectionUp)
        if (isHeldRepeat) heldDirection = direction
        val anchor = (intent.desired ?: currentIndex).coerceIn(0, (total - 1).coerceAtLeast(0))
        if (direction == Key.DirectionUp && anchor < columns && onTop != null) {
            if (event.type == KeyEventType.KeyDown) { cancel(); manual(); onTop.invoke() }
            return true
        }
        if (direction == Key.DirectionLeft && anchor % columns == 0) {
            if (event.type == KeyEventType.KeyDown) { cancel(); manual(); onLeft?.invoke() }
            return onLeft != null
        }
        if (event.type != KeyEventType.KeyDown) return true
        if (total == 0) return true
        manual()
        var target = when (direction) {
            Key.DirectionDown -> intent.request(anchor, total, true)
            Key.DirectionUp -> intent.request(anchor, total, false)
            Key.DirectionLeft -> (anchor - 1).also { intent.clear() }
            Key.DirectionRight -> if (anchor % columns < columns - 1 && anchor + 1 < total)
                (anchor + 1).also { intent.clear() } else null
            else -> null
        } ?: return true
        if (isHeldRepeat) {
            val first = grid.firstVisibleItemIndex / columns
            val targetRow = (target / columns).coerceIn((first - 1).coerceAtLeast(0), first + viewportRows())
            target = (targetRow * columns + anchor % columns).coerceAtMost(total - 1)
            intent.clear()
        }
        currentIndex = target
        visualIndex = target
        revision.value++
        if (job?.isActive != true) desiredFirstRow = grid.firstVisibleItemIndex / columns
        reveal(target)
        val layout = grid.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.index == target }
        if (job?.isActive != true && item != null && item.offset.y >= layout.viewportStartOffset &&
            item.offset.y + item.size.height <= layout.viewportEndOffset && transfer(requesters[target])) {
            return true
        }
        startMovement()
        return true
    }
}

/** Parent remains composed even when a held key moves beyond the loaded rows. */
internal fun Modifier.catalogGridHost(navigator: CatalogGridNavigator): Modifier =
    this.composed {
        LaunchedEffect(navigator) { navigator.observeExternalScroll() }
        Modifier.focusRequester(navigator.host)
            .onFocusChanged { navigator.gridFocusChanged(it.hasFocus); navigator.hostFocusChanged(it.isFocused) }
            .onPreviewKeyEvent { navigator.hostKey(it) }.focusable()
    }

internal fun Modifier.catalogGridNavigation(navigator: CatalogGridNavigator?, index: Int, requester: FocusRequester): Modifier =
    this.then(if (navigator == null) Modifier else Modifier.onPreviewKeyEvent { navigator.key(index, it) })

@Composable
internal fun BindCatalogFocus(navigator: CatalogGridNavigator?, index: Int, requester: FocusRequester) {
    DisposableEffect(navigator, index, requester) {
        navigator?.attach(index, requester)
        onDispose { navigator?.detach(index, requester) }
    }
}

@Composable
internal fun CatalogPosterPlaceholder(aspect: Float) {
    val width = LocalCatalogPosterWidth.current
    val metadata = LocalCatalogMetadataHeight.current
    Column(Modifier.fillMaxWidth().then(if (width.value.isFinite())
        Modifier.wrapContentWidth(Alignment.CenterHorizontally).width(width).height(LocalCatalogCardHeight.current) else Modifier)) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).background(Color(0xFF0B1020)))
        Spacer(Modifier.height(if (width.value.isFinite()) metadata else 34.dp))
    }
}
