package br.com.flaviotorres.iptvpremium.data.sync

import android.os.SystemClock
import android.util.Log
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheGenerationManager
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One foreground writer. Each response is staged; a cancelled/failed request is never published. */
internal class OnDemandXtreamCatalog(
    private val dao: PlaylistCacheDao,
    private val manager: PlaylistCacheGenerationManager,
    private val repository: ProgressiveXtreamRepository
) {
    private val writerMutex = Mutex()
    val completed = kotlinx.coroutines.flow.MutableSharedFlow<Pair<String,String>>(extraBufferCapacity=8)

    /** False means an untouched, partial catalogue needs a category selection. */
    suspend fun ensure(source: PlaylistSource, type: String, category: String?, allowFull: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            if (source !is PlaylistSource.XtreamCodes) return@withContext true
            val generation = dao.getActiveGeneration(source.id)?.generationId ?: return@withContext true
            val pending = "__LAZY_${type}__"
            if (!dao.hasCatalogMarker(source.id, generation, pending)) return@withContext true
            val scope = category?.let { "category:"+it } ?: "all"
            if (category == null && !allowFull) return@withContext false
            writerMutex.withLock {
                check(dao.getActiveGeneration(source.id)?.generationId == generation) { "A lista mudou. Abra a categoria novamente." }
                if (!dao.hasCatalogMarker(source.id,generation,pending) ||
                    dao.hasCatalogScope(source.id,generation,"__DONE_${type}__",scope)) return@withLock
                val start = SystemClock.elapsedRealtime()
                var outcome = "ERRO"
                br.com.flaviotorres.iptvpremium.core.storage.StorageMaintenance.foregroundImport {
                    val stage = manager.beginImport(source.id)
                    val writer = XtreamCatalogWriter(dao,manager,source.id,stage,includeLive=false,bufferedTransactions=true)
                    try {
                        repository.importXtream(source,object : XtreamCatalogSink by writer {
                            override val selectionType = type
                            override val selectionCategoryId = category
                        })
                        writer.flushPending()
                        currentCoroutineContext().ensureActive()
                        check(dao.publishLazyCatalog(source.id,stage,generation,type,category)) { "A lista mudou. Abra a categoria novamente." }
                        outcome = "SUCESSO"
                        if(category == null) completed.tryEmit(source.id to type.lowercase())
                    } catch (cancelled: CancellationException) { outcome="CANCELADA"; throw cancelled }
                    finally {
                        try { withContext(NonCancellable) { writer.closeStages() } }
                        finally {
                            manager.discardImport(source.id,stage)
                            Log.i("OnDemandCatalogPerf","kind=$type scope=${if(category==null) "ALL" else "CATEGORY"} status=$outcome totalMs=${SystemClock.elapsedRealtime()-start}")
                        }
                    }
                }
            }
            true
        }
}
