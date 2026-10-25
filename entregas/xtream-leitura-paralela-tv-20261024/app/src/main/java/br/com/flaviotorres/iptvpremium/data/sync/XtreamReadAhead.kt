package br.com.flaviotorres.iptvpremium.data.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope

/** One reader, one ordered consumer, at most three queued batches. All work belongs to the caller. */
internal suspend fun <T, R> xtreamReadAhead(
    produce: suspend (emit: suspend (T) -> Unit) -> R,
    consume: suspend (T) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): R = coroutineScope {
    val batches = Channel<T>(capacity = 3)
    val reader = async(dispatcher) {
        try { produce { batch -> batches.send(batch) } }
        finally { batches.close() }
    }
    try {
        for (batch in batches) consume(batch)
        // Parsing success is not publication: every queued batch must have been consumed first.
        reader.await()
    } finally {
        batches.cancel()
        reader.cancel()
        // coroutineScope also joins the reader on failure/cancellation, before a caller can reset/retry.
    }
}
