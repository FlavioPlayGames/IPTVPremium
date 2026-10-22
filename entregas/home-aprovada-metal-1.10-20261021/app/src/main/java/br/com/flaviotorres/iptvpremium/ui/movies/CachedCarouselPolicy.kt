package br.com.flaviotorres.iptvpremium.ui.movies

/** Small presentation list: selected title first, RAM hits next, then local-file hits. */
internal object CachedCarouselPolicy {
    fun <T : Any> select(selectedId: String, candidates: List<T>, id: (T) -> String,
        url: (T) -> String?, memoryUrls: Set<String>, diskUrls: Set<String>, limit: Int = 20): List<T> {
        if (limit <= 0) return emptyList()
        val selected = candidates.firstOrNull { id(it) == selectedId } ?: return emptyList()
        val result = arrayListOf(selected)
        val seenIds = hashSetOf(selectedId)
        val seenUrls = hashSetOf<String>()
        url(selected)?.trim()?.takeIf { it.isNotEmpty() }?.let { seenUrls.add(it) }
        for (item in candidates.sortedBy { if (url(it)?.trim() in memoryUrls) 0 else 1 }) {
            if (result.size >= limit) break
            val key = url(item)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            if (key !in memoryUrls && key !in diskUrls) continue
            val identity = id(item)
            if (identity.isBlank() || identity in seenIds || key in seenUrls) continue
            seenIds.add(identity); seenUrls.add(key); result.add(item)
        }
        return result
    }
}
