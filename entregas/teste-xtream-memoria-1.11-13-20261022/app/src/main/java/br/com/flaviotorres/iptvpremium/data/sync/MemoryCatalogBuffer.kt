package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Experiment only: holds a section before its single atomic publication. */
internal class MemoryCatalogBuffer : XtreamCatalogSink {
    val batches = mutableListOf<PlaylistContent>()
    override suspend fun append(endpoint: String, content: PlaylistContent) {
        currentCoroutineContext().ensureActive()
        batches.add(content)
    }
    override suspend fun reset(endpoint: String) { batches.clear() }
    override suspend fun stage(endpoint: String): XtreamCatalogStage = error("Unsupported experimental operation")
    override suspend fun merge(stage: XtreamCatalogStage): Unit = error("Unsupported experimental operation")
    override suspend fun discard(stage: XtreamCatalogStage): Unit = error("Unsupported experimental operation")
}
