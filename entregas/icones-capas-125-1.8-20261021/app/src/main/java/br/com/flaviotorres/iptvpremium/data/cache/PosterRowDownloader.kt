package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.*

/** Bounded database pages, ordered network groups; doesn't own a grid or decoded bitmap. */
internal suspend fun downloadPosterRows(start: Int, read: suspend (Int) -> PosterPage,
    deliver: suspend (List<String?>) -> Unit, advanced: (Int) -> Unit) {
    var cursor = start.coerceAtLeast(0)
    var failures = 0
    while (true) {
        currentCoroutineContext().ensureActive()
        val page = try { read(cursor) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (++failures >= 3) return
            delay(500); continue
        }
        require(page.urls.size <= 50) { "Poster query exceeded bounded page" }
        failures = 0
        for (batch in page.urls.chunked(PosterCategoryPolicy.GROUP)) {
            currentCoroutineContext().ensureActive()
            deliver(batch)
            delay(12)
        }
        val next = page.next ?: return
        if (next <= cursor) return
        cursor = next
        advanced(cursor)
    }
}
