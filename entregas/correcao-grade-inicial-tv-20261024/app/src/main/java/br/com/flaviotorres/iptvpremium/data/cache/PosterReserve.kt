package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*

/** Maintain a bounded reserve while retaining useful in-flight and completed files. */
internal class PosterReservePlan {
    private var anchor: Int? = null
    private var end = 0
    fun reset() { anchor = null; end = 0 }
    fun size(afterVisible: Int): Int {
        val start = afterVisible.coerceAtLeast(0)
        val previous = anchor
        if (previous == null || start < previous || start > end) {
            anchor = start; end = start + BATCH
        } else {
            val rows = (start - previous) / PosterCategoryPolicy.GROUP
            end = (end.toLong() + rows.toLong() * BATCH).coerceAtMost(start.toLong() + MAX_AHEAD).toInt()
            anchor = previous + rows * PosterCategoryPolicy.GROUP
        }
        return (end - start).coerceIn(0, MAX_AHEAD)
    }
    companion object {
        const val BATCH = 15
        const val MAX_AHEAD = 25 // Five rows of five; existing per-kind file cache stays unchanged.
    }
}

/** Confined to the category coroutine context. Only two active jobs, never a full window of queued HTTP locks. */
internal class PosterReserve(
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> Boolean,
    private val prepare: suspend (List<String>) -> Unit,
    private val report: (Int, Int, Int) -> Unit = { _, _, _ -> }
) {
    private var targets = emptyList<String>()
    private var nearest = emptyList<String>()
    private val done = mutableSetOf<String>()
    private val failed = mutableSetOf<String>()
    private val active = linkedMapOf<String, Job>()
    private var prepareJob: Job? = null
    private var enabled = false

    fun pause() {
        enabled = false
        targets = emptyList(); nearest = emptyList()
        done.clear(); failed.clear()
        active.values.toList().forEach { it.cancel() }
        prepareJob?.cancel()
    }
    fun update(urls: List<String?>, visible: List<String?>) {
        enabled = true
        targets = urls.take(PosterReservePlan.MAX_AHEAD).mapNotNull { it?.trim()?.takeIf { u -> u.startsWith("http://", true) || u.startsWith("https://", true) } }.distinct()
        val keep = (targets + visible.filterNotNull().map(String::trim)).toSet()
        done.retainAll(keep); failed.retainAll(keep)
        active.filterKeys { it !in keep }.values.toList().forEach { it.cancel() }
        val next = targets.take(PosterCategoryPolicy.GROUP)
        if (next != nearest) {
            nearest = next
            prepareJob?.cancel()
            prepareJob = scope.launch { prepare(next) } // Cache only; never starts HTTP.
        }
        pump()
        status()
    }
    private fun status() { report(targets.size, targets.count { it in done }, active.size) }
    private fun pump() {
        if (!enabled || !scope.isActive) return
        while (active.size < PosterNetworkBudget.BACKGROUND_REQUESTS) {
            val row = targets.chunked(PosterCategoryPolicy.GROUP).firstOrNull { batch -> batch.any { it !in done && it !in failed } } ?: return
            val url = row.firstOrNull { it !in done && it !in failed && it !in active } ?: return
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    val success = fetch(url)
                    currentCoroutineContext().ensureActive()
                    if (success) {
                        done += url
                        if (url in nearest) prepare(listOf(url))
                    } else failed += url
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { failed += url }
                finally {
                    active.remove(url)
                    pump()
                    status()
                }
            }
            active[url] = job
            job.start()
        }
    }
}
