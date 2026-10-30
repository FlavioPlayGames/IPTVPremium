package br.com.flaviotorres.iptvpremium.ui.components

/** The current category keeps its exact screen position when focus returns. */
internal fun catalogCategoryFullyVisible(offset: Int, size: Int, viewportStart: Int, viewportEnd: Int): Boolean =
    size > 0 && viewportEnd > viewportStart && offset >= viewportStart &&
        offset.toLong() + size <= viewportEnd.toLong()
