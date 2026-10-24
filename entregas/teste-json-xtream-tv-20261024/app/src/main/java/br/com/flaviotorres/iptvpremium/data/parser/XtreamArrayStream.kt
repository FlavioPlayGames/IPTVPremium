package br.com.flaviotorres.iptvpremium.data.parser

import java.io.Reader
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Bounded framing; the existing org.json parser still validates and maps each batch. */
internal object XtreamArrayStream {
    data class Stats(val records: Long, val characters: Long, val recoveredClosingBracket: Boolean,
                     val framingMs: Long, val readerMs: Long, val consumerMs: Long)

    suspend fun read(reader: Reader, endpoint: String, batchSize: Int = br.com.flaviotorres.iptvpremium.data.cache.ImportTuningState.profile.batchSize,
                     batchCharacters: Int = br.com.flaviotorres.iptvpremium.data.cache.ImportTuningState.profile.batchCharacters,
                     emit: suspend (String, Int) -> Unit): Stats {
        require(batchSize > 0 && batchCharacters > 0)
        val started = System.nanoTime()
        var readerNanos = 0L
        var consumerNanos = 0L
        val context = currentCoroutineContext()
        val buffer = CharArray(8192)
        var bufferAt = 0
        var bufferSize = 0
        val recoveryLimit = if (endpoint.endsWith("categories")) 256_000L else 8_000_000L
        var characters = 0L
        var records = 0L
        // One reusable batch builder: no String allocation per record or joinToString copy.
        val pending = StringBuilder(minOf(batchCharacters, 32 * 1024)).append('[')
        var pendingCount = 0
        var pendingCharacters = 0
        var lastDelimiter = false
        var recovered = false
        var legacyArrays = 0
        var legacyObjects = 0
        var legacyQuote = false
        var legacyEscape = false
        var legacyInvalid = false
        var lastSignificant: Char? = null
        fun inspect(c: Char) {
            if (!c.isWhitespace()) lastSignificant = c
            if (legacyQuote) {
                if (legacyEscape) legacyEscape = false
                else when (c) {
                    '\\' -> legacyEscape = true
                    '"' -> legacyQuote = false
                    else -> if (c.code < 0x20) legacyInvalid = true
                }
            } else when (c) {
                '"' -> legacyQuote = true
                '[' -> legacyArrays++
                ']' -> { legacyArrays--; if (legacyArrays < 1) legacyInvalid = true }
                '{' -> legacyObjects++
                '}' -> { legacyObjects--; if (legacyObjects < 0) legacyInvalid = true }
            }
        }
        fun statistics(recovered: Boolean): Stats {
            val elapsed = System.nanoTime() - started
            return Stats(records, characters, recovered,
                (elapsed - readerNanos - consumerNanos).coerceAtLeast(0L) / 1_000_000,
                readerNanos / 1_000_000, consumerNanos / 1_000_000)
        }
        fun next(): Int {
            while (bufferAt >= bufferSize) {
                context.ensureActive()
                val readStarted = System.nanoTime()
                try { bufferSize = reader.read(buffer, 0, buffer.size) }
                finally { readerNanos += System.nanoTime() - readStarted }
                bufferAt = 0
                if (bufferSize < 0) return -1
            }
            val c = buffer[bufferAt++]
            characters++
            if (characters <= recoveryLimit) inspect(c)
            return c.code
        }
        fun invalid(): Nothing = throw IllegalStateException("Resposta Xtream inválida em $endpoint: estrutura JSON incompleta ou inválida.")
        suspend fun flush() {
            if (pendingCount == 0) return
            context.ensureActive()
            pending.append(']')
            val batch = pending.toString()
            val consumeStarted = System.nanoTime()
            try { emit(batch, pendingCount) }
            finally { consumerNanos += System.nanoTime() - consumeStarted }
            context.ensureActive()
            pending.setLength(1); pendingCount = 0; pendingCharacters = 0
        }
        suspend fun add(value: CharSequence) {
            if (pendingCount > 0) pending.append(',')
            pending.append(value)
            pendingCount++; pendingCharacters += value.length; records++
            if (pendingCount >= batchSize || pendingCharacters >= batchCharacters) flush()
        }
        var ch = next()
        while (true) {
            while (ch >= 0 && (ch.toChar().isWhitespace() || ch == 0xfeff)) ch = next()
            if (ch == '#'.code) {
                while (ch >= 0 && ch != '\n'.code && ch != '\r'.code) ch = next()
            } else if (ch == '/'.code) {
                ch = next()
                if (ch == '/'.code) {
                    while (ch >= 0 && ch != '\n'.code && ch != '\r'.code) ch = next()
                } else if (ch == '*'.code) {
                    var star = false
                    while (true) {
                        ch = next()
                        if (ch < 0) invalid()
                        if (star && ch == '/'.code) break
                        star = ch == '*'.code
                    }
                    ch = next()
                } else invalid()
            } else break
        }
        if (ch < 0) return statistics(false) // preserves the existing empty-response rule
        if (ch != '['.code) invalid()
        val value = StringBuilder()
        var stack = CharArray(16)
        var depth = 0
        var quote: Char? = null
        var escape = false
        var comment = 0
        var pendingSlash = false
        var star = false
        var hasToken = false
        // Quotes, nested values and escape sequences never create top-level separators.
        while (true) {
            ch = next()
            if (ch < 0) {
                if (quote != null || depth > 0 || comment == 2) invalid()
                if (hasToken) add(value)
                if (lastDelimiter || characters > recoveryLimit || legacyInvalid || legacyQuote || legacyEscape || legacyArrays != 1 || legacyObjects != 0 || lastSignificant != '}') invalid()
                recovered = true
                break
            }
            val c = ch.toChar()
            if (comment != 0) {
                value.append(c)
                if (comment == 1 && (c == '\n' || c == '\r')) comment = 0
                else if (comment == 2) {
                    if (star && c == '/') comment = 0
                    star = c == '*'
                }
                continue
            }
            if (pendingSlash) {
                pendingSlash = false
                if (c == '/' || c == '*') {
                    value.append(c); comment = if (c == '/') 1 else 2; star = false
                    continue
                }
                hasToken = true
            }
            if (quote != null) {
                value.append(c)
                if (escape) escape = false
                else if (c == '\\') escape = true
                else if (c == quote) quote = null
                continue
            }
            when {
                c == '/' -> { value.append(c); pendingSlash = true }
                c == '#' -> { value.append(c); comment = 1 }
                c == '"' || c == '\'' -> { quote = c; value.append(c); lastDelimiter = false; hasToken = true }
                c == '{' || c == '[' -> { if (depth == stack.size) stack = stack.copyOf(stack.size * 2)
                    stack[depth++] = c; value.append(c); lastDelimiter = false; hasToken = true }
                c == '}' -> {
                    if (depth == 0 || stack[depth - 1] != '{') invalid()
                    depth--; value.append(c)
                }
                c == ']' && depth > 0 -> {
                    if (stack[depth - 1] != '[') invalid()
                    depth--; value.append(c)
                }
                c == ']' -> {
                    if (hasToken) add(value)
                    else if (lastDelimiter) add("null") // org.json accepts omitted array entries
                    // Drain to EOF: read/network errors after the last item must still abort the import.
                    while (next() >= 0) { }
                    break
                }
                (c == ',' || c == ';') && depth == 0 -> {
                    add(if (hasToken) value else "null")
                    value.setLength(0); lastDelimiter = true; hasToken = false
                }
                else -> { value.append(c); if (!c.isWhitespace()) { lastDelimiter = false; hasToken = true } }
            }
        }
        flush()
        return statistics(recovered)
    }
}
