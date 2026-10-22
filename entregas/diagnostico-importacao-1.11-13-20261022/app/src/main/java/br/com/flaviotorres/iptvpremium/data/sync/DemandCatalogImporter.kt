package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.data.diagnostics.ImportDiagnostics
import br.com.flaviotorres.iptvpremium.data.diagnostics.ImportDiagnostics.Phase

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import androidx.room.withTransaction
import br.com.flaviotorres.iptvpremium.data.cache.*
import br.com.flaviotorres.iptvpremium.data.storage.PlaylistStorage
import br.com.flaviotorres.iptvpremium.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Owns one serialized importer. Checkpoints contain hashes and category IDs, never credentials/URLs. */
class DemandCatalogImporter(
    context: Context,
    private val repository: CategoryXtreamRepository,
    private val database: PlaylistCacheDatabase,
    private val manager: PlaylistCacheGenerationManager,
    private val storage: PlaylistStorage
) {
    private val directory = File(context.applicationContext.filesDir, "catalog_category_progress_v1")
    private val dao = database.playlistCacheDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var worker: Job? = null
    @Volatile private var plan: Plan? = null
    @Volatile private var activePlaylist: String? = null
    @Volatile private var registered: Map<String, PlaylistSource> = emptyMap()
    @Volatile private var foreground = false
    private val failures = mutableMapOf<String, Int>()
    private val retryAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val epoch = MutableStateFlow(0L)

    private data class Plan(val source: PlaylistSource.XtreamCodes, val generation: String,
        val refresh: Boolean, val tasks: List<CategoryImportTask>, val done: MutableSet<String>)

    init {
        scope.launch {
            storage.selection.collect { selection ->
                lock.withLock {
                    registered = selection.playlists.associateBy { it.id }
                    activePlaylist = selection.activeId
                    val current = plan
                    if (current != null && !valid(current)) {
                        worker?.cancelAndJoin()
                        worker = null
                        val changedSource = registered[current.source.id]
                        if ((changedSource == null || fingerprint(changedSource) != fingerprint(current.source)) &&
                            dao.generationStatus(current.source.id, current.generation) == "IMPORTING") {
                            manager.discardImport(current.source.id, current.generation)
                        }
                        plan = null
                    }
                    epoch.value += 1
                }
            }
        }
        scope.launch {
            combine(CatalogImportAccess.access, CatalogImportAccess.environment, MediaOpeningPriority.busy, CatalogImportAccess.lastInteraction) { access, enabled, busy, _ ->
                Triple(access, enabled, busy)
            }.collect { (_, enabled, busy) ->
                lock.withLock {
                    foreground = enabled && !busy
                    // In-flight stages are disposable. Cancellation never publishes a partial category.
                    worker?.cancelAndJoin()
                    worker = null
                    epoch.value += 1
                }
            }
        }
        scope.launch {
            epoch.collectLatest {
                delay(250)
                lock.withLock {
                    if (worker?.isActive == true || !foreground) return@withLock
                    val current = plan ?: return@withLock
                    if (!valid(current) || activePlaylist != current.source.id) return@withLock
                    worker = scope.launch { run(current) }
                }
            }
        }
    }

    private fun valid(p: Plan): Boolean = registered[p.source.id]?.let {
        it.isEnabled && fingerprint(it) == fingerprint(p.source)
    } == true

    /** Called only for an explicit new import/refresh, preserving the stored source type. */
    suspend fun prepare(source: PlaylistSource.XtreamCodes): String = ImportDiagnostics.session(ImportDiagnostics.Kind.XTREAM_INDEX) { withContext(Dispatchers.IO) {
        lock.withLock {
            worker?.cancelAndJoin()
            worker = null
            plan?.takeIf { it.source.id != source.id }?.let { save(it) }
            val resumed = read(source)
            if (resumed != null && resumed.done.size < resumed.tasks.size) {
                plan = resumed
                epoch.value += 1
                return@withLock dao.getActiveGeneration(source.id)?.generationId ?: resumed.generation
            }
            val previous = dao.getActiveGeneration(source.id)
            val generation = manager.beginImport(source.id)
            val writer = XtreamCatalogWriter(dao, manager, source.id, generation)
            try {
                val categories = repository.importCategoryIndex(source, writer)
                currentCoroutineContext().ensureActive()
                check(categories.isNotEmpty()) { "A API Xtream não retornou categorias. O catálogo anterior foi preservado." }
                val tasks = sectionTasks()
                val newPlan = Plan(source, generation, previous != null, tasks, linkedSetOf())
                save(newPlan)
                // A first import exposes the category index immediately. A refresh keeps its previous ACTIVE.
                if (previous == null) {
                    ImportDiagnostics.measure(Phase.READY) {
                        manager.markImportReady(source.id, generation)
                        manager.activateReadyGenerationOnStartup(source.id)
                    }
                }
                plan = newPlan
                failures.clear(); retryAt.clear()
                epoch.value += 1
                Log.i(TAG, "INDICE_PRONTO categorias=${categories.size} tarefas=${tasks.size} atualizacao=${previous != null}")
                previous?.generationId ?: generation
            } catch (error: Throwable) {
                manager.discardImport(source.id, generation)
                throw error
            } finally { writer.closeStages() }
        }
    }

    }

    /** Reopening an imported list restores unfinished work, never creates a fresh import. */
    suspend fun resume(source: PlaylistSource.XtreamCodes) = withContext(Dispatchers.IO) {
        lock.withLock {
            if (plan?.source?.let { fingerprint(it) == fingerprint(source) && it.id == source.id } == true) return@withLock
            worker?.cancelAndJoin(); worker = null
            plan = read(source)?.takeIf { it.done.size < it.tasks.size || it.refresh && dao.generationStatus(source.id, it.generation) == "IMPORTING" }
            epoch.value += 1
        }
    }

    private suspend fun run(p: Plan) {
        try {
            while (currentCoroutineContext().isActive && valid(p) && activePlaylist == p.source.id) {
                if (!foreground) return
                val access = CatalogImportAccess.access.value?.takeIf { it.playlist == p.source.id }
                // A screen switch wins immediately; other sections run only while the user is on Home.
                val now = android.os.SystemClock.elapsedRealtime()
                val idle = now - CatalogImportAccess.lastInteraction.value >= 2500L
                val candidates = if (access == null || idle) p.tasks else p.tasks.filter { it.type == access.type }
                if (access == null && now - CatalogImportAccess.lastInteraction.value < 2500L) { delay(250); continue }
                if (p.done.size == p.tasks.size) { finish(p); return }
                val available = candidates.filter { (retryAt[it.key] ?: 0L) <= now }
                val next = CategoryImportPolicy.next(available, p.done, access).firstOrNull()
                if (next == null) {
                    if (p.done.size == p.tasks.size) { finish(p); return }
                    delay(1000)
                    continue
                }
                CatalogImportAccess.report(CategoryImportStatus(p.source.id, next.type, next.categoryId, false))
                try {
                    importOne(p, next)
                    p.done.add(next.key)
                    save(p)
                    failures.remove(next.key); retryAt.remove(next.key)
                    CatalogImportAccess.completed()
                    Log.i(TAG, "CATEGORIA_PRONTA tipo=${next.type} prontas=${p.done.size} total=${p.tasks.size} modo=SECAO_COMPLETA")
                    CatalogImportAccess.report(null)
                    // Give UI/Room readers breathing room; access is reevaluated before the next category.
                    delay(if (access == null) 300L else 25L)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    val attempt = (failures[next.key] ?: 0) + 1
                    failures[next.key] = attempt
                    retryAt[next.key] = android.os.SystemClock.elapsedRealtime() + minOf(60_000L, 2000L shl minOf(attempt, 5))
                    CatalogImportAccess.report(CategoryImportStatus(p.source.id, next.type, next.categoryId, true))
                    Log.w(TAG, "CATEGORIA_ERRO tipo=${next.type} tentativa=$attempt erro=${error.javaClass.simpleName}; formato=XTREAM; cache preservado")
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            Log.w(TAG, "FINALIZACAO_ERRO tipo=${error.javaClass.simpleName}; catálogo anterior preservado")
        } finally {
            // Don't clear the shared status if a new worker already replaced this one.
            if (plan === p && !currentCoroutineContext().isActive) CatalogImportAccess.report(null)
        }
    }

    private suspend fun importOne(p: Plan, task: CategoryImportTask) = ImportDiagnostics.session(when (task.type) {
        br.com.flaviotorres.iptvpremium.domain.model.CategoryType.MOVIES -> ImportDiagnostics.Kind.XTREAM_MOVIES
        br.com.flaviotorres.iptvpremium.domain.model.CategoryType.SERIES -> ImportDiagnostics.Kind.XTREAM_SERIES
        br.com.flaviotorres.iptvpremium.domain.model.CategoryType.LIVE_TV -> ImportDiagnostics.Kind.XTREAM_CHANNELS
    }) {
        val writer = XtreamCatalogWriter(dao, manager, p.source.id, p.generation, true)
        writer.resumeOffsets()
        val stage = writer.stage(task.endpoint)
        try {
            // Download/parsing stays outside the publish transaction and outside ACTIVE.
            repository.importCategory(p.source, task, stage.sink)
            currentCoroutineContext().ensureActive()
            check(valid(p) && activePlaylist == p.source.id) { "A fonte foi alterada durante a importação." }
            check(storage.withCurrentCatalogSource(p.source) {
                check(dao.generationStatus(p.source.id, p.generation) in listOf("ACTIVE", "IMPORTING")) { "Cache invalidado." }
                ImportDiagnostics.measure(Phase.MERGE) { database.withTransaction { writer.merge(stage) } }
            }) { "A fonte foi alterada durante a importação." }
        } finally { writer.closeStages() }
    }

    private suspend fun finish(p: Plan) {
        if (!valid(p)) return
        if (p.refresh && dao.generationStatus(p.source.id, p.generation) == "IMPORTING") {
            check(dao.getMovieCount(p.source.id, p.generation) + dao.getSeriesCount(p.source.id, p.generation) +
                dao.getChannelCount(p.source.id, p.generation) > 0) { "O provedor retornou um catálogo vazio; a versão anterior foi preservada." }
            check(storage.withCurrentCatalogSource(p.source) {
                manager.markImportReady(p.source.id, p.generation)
                manager.activateReadyGenerationOnStartup(p.source.id)
            }) { "A fonte foi alterada durante a atualização." }
        }
        save(p)
        CatalogImportAccess.report(null)
        CatalogImportAccess.completed()
        Log.i(TAG, "CATALOGO_COMPLETO formato=XTREAM tarefas=${p.tasks.size}")
    }

    private fun sectionTasks(): List<CategoryImportTask> =
        CategoryImportPolicy.order.map { CategoryImportTask(it, null) }

    private fun file(id: String) = AtomicFile(File(directory, digest(id) + ".json"))
    private fun save(p: Plan) {
        check(directory.isDirectory || directory.mkdirs()) { "Não foi possível salvar o progresso." }
        val json = JSONObject().put("source", fingerprint(p.source)).put("generation", p.generation).put("refresh", p.refresh)
            .put("tasks", JSONArray().apply { p.tasks.forEach { put(JSONObject().put("type", it.type.name).put("id", it.categoryId ?: JSONObject.NULL)) } })
            .put("done", JSONArray(p.done.toList()))
        val target = file(p.source.id)
        val out = target.startWrite()
        try { out.write(json.toString().toByteArray(Charsets.UTF_8)); target.finishWrite(out) }
        catch (error: Throwable) { target.failWrite(out); throw error }
    }

    private suspend fun read(source: PlaylistSource.XtreamCodes): Plan? {
        val target = file(source.id)
        if (!target.baseFile.exists()) return null
        try {
            val json = JSONObject(target.openRead().use { it.readBytes().toString(Charsets.UTF_8) })
            if (json.getString("source") != fingerprint(source)) return null
            val generation = json.getString("generation")
            if (dao.generationStatus(source.id, generation) !in listOf("ACTIVE", "IMPORTING", "READY")) return null
            val rows = json.getJSONArray("tasks")
            // Validate the old task types before migrating their checkpoint.
            (0 until rows.length()).forEach { CategoryType.valueOf(rows.getJSONObject(it).getString("type")) }
            val done = json.getJSONArray("done")
            manager.retainImport(source.id, generation)
            val completed = (0 until done.length()).map { done.getString(it) }.toSet()
            val sections = sectionTasks()
            return Plan(source, generation, json.getBoolean("refresh"), sections,
                sections.filter { it.key in completed }.map { it.key }.toMutableSet())
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            Log.w(TAG, "PROGRESSO_INDISPONIVEL erro=${error.javaClass.simpleName}; cache preservado")
            return null
        }
    }

    fun close() { scope.cancel() }
    internal suspend fun closeAndJoin() { scope.coroutineContext[Job]?.cancelAndJoin() }
    private fun fingerprint(source: PlaylistSource): String = digest(when(source) {
        is PlaylistSource.XtreamCodes -> JSONObject().put("type", "XTREAM").put("server", source.serverUrl)
            .put("user", source.username).put("password", source.password).put("fallback", source.fallbackM3uUrl ?: JSONObject.NULL).toString()
        is PlaylistSource.M3u -> JSONObject().put("type", "M3U").put("url", source.playlistUrl).toString()
    })
    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private companion object { const val TAG = "CategoryCatalogImport" }
}
