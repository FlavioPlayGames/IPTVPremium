package br.com.flaviotorres.iptvpremium.data.remote

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.Locale
import java.util.zip.GZIPInputStream

/** Bounded buffers: neither the compressed nor expanded body is materialized here. */
internal class PlaylistResponseBody(connection: HttpURLConnection) {
    private val encoding = connection.contentEncoding.orEmpty().trim().lowercase(Locale.ROOT)
    private val coding = when (encoding) {
        "", "identity" -> "identity"
        "gzip", "x-gzip" -> "gzip"
        else -> throw IOException("Compactacao HTTP nao suportada pelo cliente de listas.")
    }
    private val wire = CountingInputStream(connection.inputStream)
    private val decoded = CountingInputStream(
        if (coding == "gzip") GZIPInputStream(wire, 32 * 1024) else wire
    )
    val stream: InputStream get() = decoded
    fun statistics(): String = "codificacao=$coding bytesRede=${wire.count} bytesTexto=${decoded.count} leituraRedeMs=${wire.readNanos / 1_000_000} leituraCorpoMs=${decoded.readNanos / 1_000_000} leituraRedePicoMs=${wire.maxReadNanos / 1_000_000}"

    private class CountingInputStream(source: InputStream) : FilterInputStream(source) {
        var count = 0L
            private set
        var readNanos = 0L
            private set
        var maxReadNanos = 0L
            private set
        private inline fun <T> measured(block: () -> T): T {
            val started = System.nanoTime()
            try { return block() } finally {
                val elapsed = System.nanoTime() - started
                readNanos += elapsed
                maxReadNanos = maxOf(maxReadNanos, elapsed)
            }
        }
        override fun read(): Int = measured { `in`.read().also { if (it >= 0) count++ } }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = measured {
            `in`.read(buffer, offset, length).also { if (it > 0) count += it }
        }
        override fun skip(amount: Long): Long = measured { `in`.skip(amount).also { count += it } }
    }
}
