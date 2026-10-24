package br.com.flaviotorres.iptvpremium.core.storage

import java.io.File

/** Decimal MB: storage limits, deliberately separate from the RAM policy. */
internal object CacheStorageLimits {
    const val TOTAL = 100_000_000L
    const val IMAGES = 75_000_000L
    const val EPISODES = 10_000_000L
    const val RESPONSES = 5_000_000L
    const val SUBTITLES = 5_000_000L
    const val OTHER = 100_000_000L
}
internal enum class CacheStorageKind { IMAGES, EPISODES, RESPONSES, SUBTITLES, OTHER }

/** Indexed LRU with pre-write reservations. Only disposable cache roots are ever enumerated. */
internal class CacheStorageBudget(
    cacheRoot: File,
    subtitlesRoot: File,
    private var totalLimit: Long = CacheStorageLimits.TOTAL,
    private val limits: Map<CacheStorageKind, Long> = mapOf(
        CacheStorageKind.IMAGES to CacheStorageLimits.IMAGES,
        CacheStorageKind.EPISODES to CacheStorageLimits.EPISODES,
        CacheStorageKind.RESPONSES to CacheStorageLimits.RESPONSES,
        CacheStorageKind.SUBTITLES to CacheStorageLimits.SUBTITLES,
        CacheStorageKind.OTHER to CacheStorageLimits.OTHER
    ),
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val root = cacheRoot.canonicalFile
    private val subtitles = subtitlesRoot.canonicalFile
    private data class Entry(val bytes: Long, val accessed: Long, val kind: CacheStorageKind)
    private val entries = linkedMapOf<File, Entry>()
    private val readers = mutableMapOf<File, Int>()
    private val reservations = mutableSetOf<Reservation>()
    private var preferredNames = emptySet<String>()
    private val cacheMarkers = setOf("history-session.marker", ".history-session.marker", ".iptv_theme_cache")
    private var initialized = false
    private fun child(file: File, directory: File) = file.path.startsWith(directory.path + File.separator)
    private fun kind(file: File): CacheStorageKind {
        require(child(file, root) || child(file, subtitles)) { "Not a disposable cache file" }
        if (child(file, subtitles)) return CacheStorageKind.SUBTITLES
        return when (file.relativeTo(root).invariantSeparatorsPath.substringBefore('/')) {
            "iptv_movie_posters_session", "iptv_movie_posters_125", "iptv_series_posters_125", "movie_details_images" -> CacheStorageKind.IMAGES
            "series_episode_details" -> CacheStorageKind.EPISODES
            "series_public_artwork_v1" -> CacheStorageKind.RESPONSES
            else -> CacheStorageKind.OTHER
        }
    }
    private fun index(file: File) {
        if (file.isFile) entries[file] = Entry(file.length(), maxOf(file.lastModified(), entries[file]?.accessed ?: 0L), kind(file))
        else entries.remove(file)
    }
    private fun scan() {
        entries.clear()
        val writing = reservations.map { it.temporary }.toSet()
        fun visit(file: File) {
            if (java.nio.file.Files.isSymbolicLink(file.toPath())) return
            if (file.isDirectory) file.listFiles().orEmpty().forEach(::visit)
            else if (file.isFile && file.canonicalFile !in writing) index(file.canonicalFile)
        }
        visit(root); visit(subtitles); initialized = true
    }
    private fun ensureIndex() {
        if (!initialized) {
            scan()
            limits.forEach { (bucket, cap) -> trim(bucket, cap) }
            trim(null, totalLimit)
        }
    }
    @Synchronized fun prefer(names: Set<String>) { preferredNames = names.toSet() }
    @Synchronized fun retain(file: File): AutoCloseable {
        ensureIndex()
        val key = file.canonicalFile; kind(key)
        index(key)
        readers[key] = (readers[key] ?: 0) + 1
        entries[key]?.let { entries[key] = it.copy(accessed = clock()) }
        return AutoCloseable { synchronized(this) {
            val count = readers[key] ?: return@synchronized
            if (count <= 1) readers.remove(key) else readers[key] = count - 1
            index(key)
        } }
    }
    private fun protected(file: File) = file in readers ||
        (file.parentFile == root && file.name in cacheMarkers) ||
        reservations.any { it.target == file || it.temporary == file }
    private fun used(bucket: CacheStorageKind? = null): Long = entries.values.sumOf { if (bucket == null || it.kind == bucket) it.bytes else 0L } +
        reservations.sumOf { if (bucket == null || it.kind == bucket) it.limit else 0L }
    private fun trim(bucket: CacheStorageKind?, maximum: Long): Long {
        var freed = 0L
        val candidates = entries.entries.filter { (file, entry) -> (bucket == null || entry.kind == bucket) && !protected(file) }
            .sortedWith(compareBy<Map.Entry<File, Entry>> { it.key.name in preferredNames }.thenBy { it.value.accessed })
        for ((file, entry) in candidates) {
            if (used(bucket) <= maximum) break
            if (!file.exists() || file.delete()) { entries.remove(file); freed += entry.bytes }
        }
        return freed
    }
    @Synchronized fun reserve(target: File, maximumBytes: Long): Reservation? {
        ensureIndex()
        val file = target.canonicalFile
        val bucket = kind(file)
        val cap = limits.getValue(bucket)
        if (maximumBytes <= 0 || maximumBytes > cap || maximumBytes > totalLimit || reservations.any { it.target == file }) return null
        index(file)
        // Existing targets/readers are protected even while preparing their replacement.
        val guard = retain(file)
        try {
            trim(bucket, cap - maximumBytes)
            trim(null, totalLimit - maximumBytes)
            if (used(bucket) + maximumBytes > cap || used() + maximumBytes > totalLimit) return null
            return Reservation(file, File(file.parentFile, file.name + "." + java.util.UUID.randomUUID() + ".part"), maximumBytes, bucket)
                .also { reservations += it }
        } finally { guard.close() }
    }
    inner class Reservation internal constructor(val target: File, val temporary: File, val limit: Long, internal val kind: CacheStorageKind) : AutoCloseable {
        private var closed = false
        override fun close() = synchronized(this@CacheStorageBudget) {
            if (!closed) {
                closed = true
                temporary.delete()
                reservations.remove(this)
                index(target); index(temporary)
            }
        }
    }
    /** Count-based poster LRU must also respect readers and reservations. */
    @Synchronized fun removeDisposable(file: File): Boolean {
        ensureIndex()
        val key = file.canonicalFile
        kind(key)
        if (protected(key)) return false
        if (!key.exists() || key.delete()) { entries.remove(key); return true }
        return false
    }

    /** Periodic reconciliation runs off the UI thread after navigation/import is idle. */
    @Synchronized fun maintain(): Long {
        scan()
        var freed = 0L
        val cutoff = clock() - 24L * 60 * 60 * 1000
        val abandoned = entries.keys.filter { file ->
            file.name.endsWith(".part") && file.lastModified() < cutoff && !protected(file) &&
                (file.name.startsWith("download-") || file.name.contains(Regex("[0-9a-f]{8}-[0-9a-f-]{27}")))
        }
        abandoned.forEach { file -> if (file.delete()) { freed += entries.remove(file)?.bytes ?: 0L } }
        limits.forEach { (bucket, cap) -> freed += trim(bucket, cap) }
        freed += trim(null, totalLimit)
        return freed
    }
    /** Pressure can reduce disposable cache further; protected readers/writers are never deleted. */
    @Synchronized fun setTotalLimit(bytes: Long) {
        totalLimit = bytes.coerceIn(0L, CacheStorageLimits.TOTAL)
        ensureIndex()
        trim(null, totalLimit)
    }
    @Synchronized fun accountedBytes(): Long { ensureIndex(); return used() }
}
