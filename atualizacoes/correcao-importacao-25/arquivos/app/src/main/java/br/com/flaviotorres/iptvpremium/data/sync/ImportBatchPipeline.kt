package br.com.flaviotorres.iptvpremium.data.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** One waiting batch, one writer. Ordering and the original staging/activation contract are preserved. */
internal object ImportBatchPipeline {
    const val pendingBatches = 1

    suspend fun <T, R> run(
        produce: suspend (suspend (T) -> Unit) -> R,
        consume: suspend (T) -> Unit,
        writerDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): R = coroutineScope {
        val queue = Channel<T>(pendingBatches)
        val writer = launch(writerDispatcher) {
            for (batch in queue) consume(batch)
        }
        try {
            val result = produce { queue.send(it) }
            queue.close()
            writer.join() // A complete JSON response is not a complete import until the last write succeeds.
            result
        } finally {
            queue.cancel()
            writer.cancel()
        }
    }
}
