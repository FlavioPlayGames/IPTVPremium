package br.com.flaviotorres.iptvpremium.data.cache

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** At most 15 compressed posters; only the nearest five are prepared in the existing RAM LRU. */
internal object PosterAheadWindow {
    const val ROWS = 3
    const val LIMIT = ROWS * PosterCategoryPolicy.GROUP
    suspend fun fill(urls: List<String?>, download: suspend (List<String?>) -> Unit,
        prepareNearest: suspend (List<String?>) -> Unit) {
        for ((index, batch) in urls.take(LIMIT).chunked(PosterCategoryPolicy.GROUP).withIndex()) {
            currentCoroutineContext().ensureActive()
            download(batch)
            currentCoroutineContext().ensureActive()
            if (index == 0) prepareNearest(batch)
        }
    }
}
