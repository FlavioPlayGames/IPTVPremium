package br.com.flaviotorres.iptvpremium.data.cache

import java.io.File

/** Per-kind compressed files, not 125 decoded bitmaps. Disk calls belong on IO. */
internal class PosterFileLru(
    private val directory: File,
    private val remove: (File) -> Boolean,
    private val capacity: Int = PosterCategoryPolicy.CAPACITY,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val entries = LinkedHashMap<String, File>(capacity, .75f, true)
    private val active = mutableMapOf<String, Int>()
    private var visible = emptySet<String>()
    init {
        require(capacity > 0)
        directory.listFiles().orEmpty().filter { it.isFile && it.name.matches(Regex("[a-f0-9]{64}\\.img")) }
            .sortedBy { it.lastModified() }.forEach { entries[it.nameWithoutExtension] = it }
        trimTo(capacity)
    }
    private fun trimTo(maximum: Int): Boolean {
        for (key in entries.keys.toList()) {
            if (entries.size <= maximum) return true
            if (key in visible || (active[key] ?: 0) > 0) continue
            val file = entries[key] ?: continue
            if (!file.exists() || remove(file)) entries.remove(key)
        }
        return entries.size <= maximum
    }
    @Synchronized fun setVisible(keys: Set<String>) {
        visible = keys.toSet()
        keys.forEach { entries[it]?.takeIf(File::isFile)?.setLastModified(clock()) }
    }
    @Synchronized fun acquire(key: String): AutoCloseable? {
        require(key.matches(Regex("[a-f0-9]{64}")))
        if (key !in entries && !trimTo(capacity - 1)) return null
        val file = entries.getOrPut(key) { File(directory, "$key.img") }
        if (file.isFile) file.setLastModified(clock())
        active[key] = (active[key] ?: 0) + 1
        var closed = false
        return AutoCloseable { synchronized(this) {
            if (closed) return@synchronized
            closed = true
            val count = active[key] ?: 0
            if (count <= 1) active.remove(key) else active[key] = count - 1
            if (!file.exists() && key !in active) entries.remove(key)
        } }
    }
    @Synchronized internal fun keys(): Set<String> = entries.keys.toSet()
}
