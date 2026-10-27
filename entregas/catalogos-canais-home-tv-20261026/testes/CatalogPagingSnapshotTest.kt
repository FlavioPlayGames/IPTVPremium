package br.com.flaviotorres.iptvpremium.ui.components

import androidx.paging.ItemSnapshotList
import kotlin.random.Random

fun main() {
    val old = ItemSnapshotList(0, 0, listOf("movie-a", "movie-b"))
    var live = old
    val oldKey: (Int) -> String? = { live[it] } // Former key provider consulted the live page.
    live = ItemSnapshotList(0, 0, emptyList())
    check(runCatching { oldKey(0) }.exceptionOrNull() is IndexOutOfBoundsException)
    val stableKey: (Int) -> String = { catalogSnapshotKey(old, it, "pending:") { id -> id } }
    check(stableKey(0) == "movie-a" && stableKey(1) == "movie-b")
    check(!catalogHintAllowed(old, live, 0, live.size))
    check(catalogSnapshotKey<String>(live, 0, "pending:") { it } == "pending:0")
    check(!catalogHintAllowed(live, live, 0, 0))
    val sameSizeNewGeneration = ItemSnapshotList(0, 0, listOf("new-a", "new-b"))
    check(!catalogHintAllowed(old, sameSizeNewGeneration, 1, sameSizeNewGeneration.size))
    check(catalogHintAllowed(sameSizeNewGeneration, sameSizeNewGeneration, 1, sameSizeNewGeneration.size))
    check(!catalogHintAllowed(old, old, -1, old.size))
    check(!catalogHintAllowed(old, old, 2, old.size))
    val placeholders = ItemSnapshotList(4, 7, listOf("loaded-a", "loaded-b"))
    check(catalogSnapshotKey(placeholders, 0, "pending:") { it } == "pending:0")
    check(catalogSnapshotKey(placeholders, 4, "pending:") { it } == "loaded-a")
    check(catalogSnapshotKey(placeholders, 12, "pending:") { it } == "pending:12")
    check(catalogHintAllowed(placeholders, placeholders, 12, placeholders.size)) // Placeholders must still trigger loading.
    val random = Random(47)
    repeat(5000) {
        val snapshot = ItemSnapshotList(random.nextInt(0, 12), random.nextInt(0, 12),
            List(random.nextInt(0, 60)) { index -> "id-$index" })
        val keys = (0 until snapshot.size).map { index -> catalogSnapshotKey(snapshot, index, "pending:") { id -> id } }
        val refreshed = ItemSnapshotList(0, 0, List(random.nextInt(0, 30)) { index -> "other-$index" })
        check((0 until snapshot.size).map { index -> catalogSnapshotKey(snapshot, index, "pending:") { id -> id } } == keys)
        check(keys.distinct().size == keys.size)
        for (index in -1..snapshot.size) check(!catalogHintAllowed(snapshot, refreshed, index, refreshed.size))
    }
    println("PASS original empty-list crash reproduced; retained key provider stable after clear/shrink/replacement; stale hints blocked; placeholders load; 5000 refresh transitions")
}
