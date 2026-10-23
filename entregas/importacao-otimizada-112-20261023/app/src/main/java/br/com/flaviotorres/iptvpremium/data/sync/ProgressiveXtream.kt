package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.domain.model.PlaylistContent
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource

/** Ordered sink; the producer may queue one batch while the current write completes. */
interface XtreamCatalogSink {
    val includeLive: Boolean get() = true
    val includeOnDemand: Boolean get() = true
    val includeMovies: Boolean get() = includeOnDemand
    val includeSeries: Boolean get() = includeOnDemand
    suspend fun append(endpoint: String, content: PlaylistContent)
    suspend fun reset(endpoint: String)
    suspend fun stage(endpoint: String): XtreamCatalogStage
    suspend fun merge(stage: XtreamCatalogStage)
    suspend fun discard(stage: XtreamCatalogStage)
}
class XtreamCatalogStage(val generationId: String, val endpoint: String, val sink: XtreamCatalogSink)
interface ProgressiveXtreamRepository {
    val supportsProgressiveXtream: Boolean get() = true
    suspend fun importXtream(source: PlaylistSource.XtreamCodes, sink: XtreamCatalogSink)
}
/** Do not mistake a local write failure for a retryable provider response. */
class XtreamCatalogWriteException(cause: Exception) : IllegalStateException("Falha ao gravar catalogo Xtream.", cause)
