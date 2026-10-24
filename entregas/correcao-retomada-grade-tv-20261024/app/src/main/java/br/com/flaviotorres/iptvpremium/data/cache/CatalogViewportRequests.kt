package br.com.flaviotorres.iptvpremium.data.cache

/** Send missing-item hints after collection starts; cached placeholders alone do not replay hints. */
internal fun <T : Any> requestMissingCatalogItems(
    indices: List<Int>, itemCount: () -> Int, peek: (Int) -> T?, request: (Int) -> T?
): Int {
    var requested = 0
    for (index in indices.distinct().take(30)) {
        if (index >= 0 && index < itemCount() && peek(index) == null) {
            request(index)
            requested++
        }
    }
    return requested
}
