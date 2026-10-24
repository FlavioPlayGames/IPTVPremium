package br.com.flaviotorres.iptvpremium.data.cache

import br.com.flaviotorres.iptvpremium.core.storage.CacheStorageBudget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest

internal data class CategoryCountKey(val playlist: String, val generation: String, val type: String)

/** Raw counts of a completed generation, shared by regular/Kids projections. No catalogue rows. */
internal class CatalogCategoryCountCache(
    private val directory: File?,
    private val storage: () -> CacheStorageBudget? = { null },
    private val report: (type: String, source: String, groups: Int, elapsedMs: Long) -> Unit = { _, _, _, _ -> }
) {
    private val mutex = Mutex()
    private val memory = LinkedHashMap<CategoryCountKey, Map<String?, Int>>(16, .75f, true)

    suspend fun get(key: CategoryCountKey, load: suspend () -> Map<String?, Int>): Map<String?, Int> =
        withContext(Dispatchers.IO) {
            val started = System.nanoTime()
            mutex.withLock {
                memory[key]?.let {
                    report(key.type, "ram", it.size, (System.nanoTime() - started) / 1_000_000)
                    return@withLock it
                }
                val disk = read(key)
                val counts = disk ?: load().toMap()
                currentCoroutineContext().ensureActive()
                if (valid(counts)) {
                    memory[key] = counts
                    while (memory.size > MAX_ENTRIES) memory.remove(memory.keys.first())
                    if (disk == null) write(key, counts)
                }
                report(key.type, if (disk == null) "database" else "disk", counts.size,
                    (System.nanoTime() - started) / 1_000_000)
                counts
            }
        }

    private fun valid(counts: Map<String?, Int>) = counts.size <= MAX_GROUPS &&
        counts.values.all { it >= 0 } && counts.values.sumOf { it.toLong() } <= Int.MAX_VALUE &&
        counts.keys.sumOf { it?.length?.toLong() ?: 0L } <= 32_768L

    private fun file(key: CategoryCountKey): File? = directory?.let {
        val bytes = ByteArrayOutputStream().also { out -> DataOutputStream(out).use { writeKey(it, key) } }.toByteArray()
        File(it, digest(bytes).joinToString("") { byte -> "%02x".format(byte.toInt() and 255) } + ".bin")
    }

    private fun writeKey(out: DataOutputStream, key: CategoryCountKey) {
        out.writeUTF(key.playlist); out.writeUTF(key.generation); out.writeUTF(key.type)
    }
    private fun readKey(input: DataInputStream) = CategoryCountKey(input.readUTF(), input.readUTF(), input.readUTF())
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun read(key: CategoryCountKey): Map<String?, Int>? = try {
        val file = file(key)
        if (file == null || !file.isFile || file.length() !in 40L..MAX_FILE_BYTES) null else {
            storage()?.retain(file).use {
                val bytes = file.readBytes()
                if (bytes.size > MAX_FILE_BYTES || bytes.size < 40) return null
                val payload = bytes.copyOfRange(0, bytes.size - 32)
                if (!MessageDigest.isEqual(digest(payload), bytes.copyOfRange(payload.size, bytes.size))) return null
                DataInputStream(ByteArrayInputStream(payload)).use { input ->
                    if (input.readInt() != MAGIC || readKey(input) != key) return null
                    val count = input.readInt()
                    if (count !in 0..MAX_GROUPS) return null
                    val result = linkedMapOf<String?, Int>()
                    repeat(count) {
                        val category = if (input.readBoolean()) input.readUTF() else null
                        if (result.containsKey(category)) return null
                        result[category] = input.readInt()
                    }
                    if (input.available() != 0 || !valid(result)) return null
                    file.setLastModified(System.currentTimeMillis())
                    result.toMap()
                }
            }
        }
    } catch (_: IOException) { null }
      catch (_: SecurityException) { null }

    private suspend fun write(key: CategoryCountKey, counts: Map<String?, Int>) {
        var temporary: File? = null
        var reservation: CacheStorageBudget.Reservation? = null
        try {
            val target = file(key) ?: return
            val payload = ByteArrayOutputStream().also { bytes ->
                DataOutputStream(bytes).use { out ->
                    out.writeInt(MAGIC); writeKey(out, key); out.writeInt(counts.size)
                    for ((category, count) in counts) {
                        out.writeBoolean(category != null)
                        if (category != null) out.writeUTF(category)
                        out.writeInt(count)
                    }
                }
            }.toByteArray()
            val length = payload.size + 32
            if (length > MAX_FILE_BYTES || (!directory!!.isDirectory && !directory.mkdirs())) return
            val budget = storage()
            if (!prune(budget, length.toLong())) return
            if (budget != null) {
                reservation = budget.reserve(target, length.toLong()) ?: return
                temporary = reservation.temporary
            } else temporary = File.createTempFile("summary-", ".part", directory)
            FileOutputStream(temporary).use { out -> out.write(payload); out.write(digest(payload)) }
            currentCoroutineContext().ensureActive()
            // Same-directory rename: an interrupted write cannot expose a partial summary.
            temporary.renameTo(target)
        } catch (_: IOException) { /* Disposable optimisation: the Room result remains usable. */ }
          catch (_: SecurityException) { }
        finally { temporary?.delete(); reservation?.close() }
    }

    private fun prune(budget: CacheStorageBudget?, incoming: Long): Boolean {
        val files = directory?.listFiles { file -> file.isFile && file.name.matches(FILE_NAME) }
            ?.sortedBy { it.lastModified() }.orEmpty()
        var bytes = files.sumOf { it.length() }
        var count = files.size
        for (file in files) {
            if (count < MAX_ENTRIES && bytes + incoming <= MAX_DISK_BYTES) break
            val size = file.length()
            if (budget?.removeDisposable(file) ?: file.delete()) { bytes -= size; count-- }
        }
        return count < MAX_ENTRIES && bytes + incoming <= MAX_DISK_BYTES
    }

    private companion object {
        const val MAGIC = 0x43434331
        const val MAX_ENTRIES = 12
        const val MAX_GROUPS = 4096
        const val MAX_FILE_BYTES = 128 * 1024L
        const val MAX_DISK_BYTES = 512 * 1024L
        val FILE_NAME = Regex("[0-9a-f]{64}\\.bin")
    }
}
