package br.com.flaviotorres.iptvpremium.ui.components

import androidx.paging.compose.LazyPagingItems

/** Keys must use the same immutable snapshot as the item provider's count. */
internal fun <T : Any> catalogSnapshotKey(snapshot: List<T?>?, index: Int,
    placeholder: String, id: (T) -> String): String =
    snapshot?.getOrNull(index)?.let(id) ?: "$placeholder$index"

/** A provider retained by LazyGrid cannot send a hint into a different generation. */
internal fun catalogHintAllowed(rendered: Any?, current: Any?, index: Int, size: Int): Boolean =
    rendered != null && rendered === current && index >= 0 && index < size

internal fun <T : Any> requestCatalogPageItem(paging: LazyPagingItems<T>, index: Int,
    rendered: List<T?>? = paging.itemSnapshotList) {
    val current = paging.itemSnapshotList
    if (catalogHintAllowed(rendered, current, index, current.size)) {
        paging[index] // Preserve Paging access hints only for the current, valid position.
    }
}
