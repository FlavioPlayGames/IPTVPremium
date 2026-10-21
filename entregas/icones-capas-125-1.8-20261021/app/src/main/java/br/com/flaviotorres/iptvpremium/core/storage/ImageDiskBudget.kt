package br.com.flaviotorres.iptvpremium.core.storage

import java.io.File

/** Shared on-disk budget only. Call on an IO dispatcher, never while drawing the UI. */
internal class ImageDiskBudget(
    private val cacheRoot: File,
    private val maxBytes: Long = CacheStorageLimits.IMAGES,
    private val clock: () -> Long = System::nanoTime,
    private val storage: CacheStorageBudget? = null
) {
    private val directories = listOf("iptv_movie_posters_session", "iptv_movie_posters_125", "iptv_series_posters_125", "movie_details_images")
    private val readers = mutableMapOf<File, Int>()
    private var lastScan: Long? = null
    private var overBudget = false
    private var preferredNames: Set<String> = emptySet()

    @Synchronized fun prefer(names: Set<String>) { preferredNames = names.toSet(); storage?.prefer(names) }

    @Synchronized fun retain(file: File): Lease {
        require(file.parentFile in directories.map { File(cacheRoot, it) })
        readers[file] = (readers[file] ?: 0) + 1
        val lease = Lease(file, file.length(), storage?.retain(file))
        if (storage == null) maintain(false)
        return lease
    }

    inner class Lease internal constructor(private val file: File, private val initialBytes: Long, private val guard: AutoCloseable?) : AutoCloseable {
        private var closed = false
        override fun close() = synchronized(this@ImageDiskBudget) {
            if (!closed) {
                closed = true
                val count = readers.getValue(file)
                if (count == 1) readers.remove(file) else readers[file] = count - 1
                guard?.close()
                if (storage == null) maintain(file.length() != initialBytes || overBudget)
            }
        }
    }

    /** Strict names and age, and never a directory with an active download/decoder. */
    @Synchronized fun cleanupAbandonedTemporaries(beforeMillis: Long): Int {
        var removed = 0
        for (name in directories) {
            val directory = File(cacheRoot, name)
            if (readers.keys.any { it.parentFile == directory }) continue
            for (file in directory.listFiles().orEmpty()) {
                val known = file.name.matches(Regex("[0-9a-f]{64}\\.img\\.part")) ||
                    file.name.matches(Regex("(loading_|carousel_)[A-Za-z0-9_-]+\\.tmp"))
                if (known && file.isFile && file.lastModified() < beforeMillis && file.delete()) removed++
            }
        }
        return removed
    }

    /** Includes partial downloads in accounting, but never deletes an active reader or writer. */
    @Synchronized fun maintain(force: Boolean = true): Long {
        if (storage != null) return if (force) storage.maintain() else 0L
        val now = clock()
        if (!force && lastScan?.let { now - it < 60_000_000_000L } == true) return 0L
        lastScan = now
        val files = directories.flatMap { File(cacheRoot, it).listFiles()?.toList().orEmpty() }
            .filter { it.isFile && it.extension in setOf("img", "part", "tmp") }
        var used = files.sumOf { it.length() }
        var freed = 0L
        if (used <= maxBytes) { overBudget = false; return 0L }
        val candidates = files.filter { it.extension == "img" && it !in readers }
            .sortedWith(compareBy<File> { it.name in preferredNames }.thenBy { it.lastModified() })
        for (file in candidates) {
            if (used <= maxBytes) break
            val size = file.length()
            if (file.delete()) { used -= size; freed += size }
        }
        // Concurrent transfers may temporarily exceed the budget. Their final release trims again.
        overBudget = used > maxBytes
        return freed
    }
}
