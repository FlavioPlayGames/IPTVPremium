package br.com.flaviotorres.iptvpremium.ui.live

/** One logical cursor, including circular navigation and a pinned row outside the current page. */
internal fun liveMenuNextIndex(index: Int, direction: Int, count: Int): Int =
    if (count <= 0) 0 else Math.floorMod(index.toLong() + direction.toLong(), count.toLong()).toInt()

internal data class LiveMenuRows(val sourceCount: Int, val pinned: Boolean, val duplicateSourceIndex: Int = -1) {
    private val duplicate = duplicateSourceIndex.takeIf { pinned && it in 0 until sourceCount }
    private val offset = if (pinned) 1 else 0
    val count: Int get() = sourceCount + offset - if (duplicate != null) 1 else 0
    fun sourceIndex(index: Int): Int? {
        if (index !in 0 until count || (pinned && index == 0)) return null
        val source = index - offset
        return source + if (duplicate != null && source >= duplicate) 1 else 0
    }
    fun visualIndex(source: Int): Int? {
        if (source !in 0 until sourceCount || source == duplicate) return null
        return source + offset - if (duplicate != null && source > duplicate) 1 else 0
    }
}

internal fun liveMenuEdgeScroll(offset: Int, size: Int, start: Int, end: Int): Float = when {
    offset < start -> (offset-start).toFloat()
    offset+size > end -> (offset+size-end).toFloat()
    else -> 0f
}

/** Lazy layout may retain key callbacks after Paging has replaced its current page. */
internal fun liveChannelSnapshotKey(snapshot: List<br.com.flaviotorres.iptvpremium.domain.model.Channel?>,
    index: Int): String = snapshot.getOrNull(index)?.let { "channel_${it.id}" } ?: "channel_placeholder_$index"
