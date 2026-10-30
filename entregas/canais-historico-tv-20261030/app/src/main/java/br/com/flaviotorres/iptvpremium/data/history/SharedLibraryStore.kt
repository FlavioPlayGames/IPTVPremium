package br.com.flaviotorres.iptvpremium.data.history

import android.content.Context
import br.com.flaviotorres.iptvpremium.core.player.PlaybackContentType
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** Device-local identity links. No server requests, video URLs, passwords or catalogue copies. */
class SharedLibraryStore private constructor(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("iptvpremium_shared_library_v1", Context.MODE_PRIVATE)
    private val lock = Any()
    private val reconcileLock = Mutex()
    val changes = MutableStateFlow(0L)
    private data class Record(val key: String, val playlist: String, val id: String,
        val identity: ContentIdentity, val tracked: Boolean, val favorite: Boolean?, val changed: Long,
        val hiddenUntil: Long = 0L, val aliasesVersion: Int = 0)
    private data class Link(val id: String, val title: String, val poster: String?, val order: Int? = null)
    // Opening the app only creates the store; decoding/searching history waits for catalogue readiness.
    private var loadedRecords:MutableMap<String,Record>?=null
    private var records:MutableMap<String,Record>
        get()=synchronized(lock) {loadedRecords?:readRecords().also {loadedRecords=it}}
        set(value) {synchronized(lock) {loadedRecords=value}}
    private var linkedPlaylist: String? = null
    private var linkedGeneration: String? = null
    private val linkedSignatures = mutableMapOf<String, String>()
    private var eligibleKeys = emptySet<String>()
    private var links = emptyMap<String, List<Link>>()
    private var linkedHistory = emptyList<WatchHistoryEntry>()
    private val episodeAttempts = mutableMapOf<String, Long>()
    data class MovieMetadataRequest(val playlist: String, val id: String, val title: String, val year: Int?, val order: Int? = null)
    private var pendingMovies = emptyList<MovieMetadataRequest>()
    private var pendingSeries = emptyList<Pair<String, ContentIdentity>>()
    private var historyPriorities = emptyMap<String, Long>()
    private val metadataAttempts = linkedMapOf<String, Long>()
    private val episodeMaps = mutableMapOf<String, Map<Pair<Int, Int>, SeriesEpisodeInfo>>()

    private fun key(playlist: String, kind: String, id: String) = "${playlist.length}:$playlist:$kind:$id"
    private fun identity(row: SharedCatalogIdentity, kind: String):ContentIdentity {
        val asset=if(kind=="movies")providerMovieIdentity(row.streamUrl)else null
        return ContentIdentity(kind,row.title,row.year,row.imdb,row.tmdb,providerAssetKey=asset?.key,providerAssetId=asset?.id)
    }
    private fun changed() { changes.value = changes.value + 1L }
    private fun readRecords(): MutableMap<String, Record> = synchronized(lock) {
        val result = linkedMapOf<String, Record>()
        val array = try { JSONArray(prefs.getString("identities", "[]")) } catch (_: Exception) { JSONArray() }
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val kind = o.optString("kind"); if (kind !in listOf("movies", "series")) continue
            val playlist = o.optString("playlist"); val id = o.optString("id")
            if (playlist.isBlank() || id.isBlank()) continue
            val k = key(playlist, kind, id)
            result[k] = Record(k, playlist, id, ContentIdentity(kind, o.optString("title"),
                o.optInt("year").takeIf { it > 0 }, o.optString("imdb"), o.optString("tmdb"),
                    o.optJSONArray("aliases")?.let { a -> (0 until minOf(a.length(), 32)).map { a.optString(it) }.filter { it.isNotBlank() && it.length <= 160 } }.orEmpty(), providerAssetKey=o.optString("providerAssetKey").takeIf {it.matches(Regex("[0-9a-f]{64}"))},
                    providerAssetId=o.optString("providerAssetId").takeIf {it.matches(Regex("[0-9]{1,20}"))}),
                o.optBoolean("tracked"), if (o.has("favorite")) o.optBoolean("favorite") else null,
                o.optLong("changed"), o.optLong("hiddenUntil"), o.optInt("aliasesVersion"))
        }
        result
    }
    private fun persist() {
        val a = JSONArray()
        records.values.forEach { r -> a.put(JSONObject().apply {
            put("playlist", r.playlist); put("id", r.id); put("kind", r.identity.kind)
            put("title", r.identity.title); put("year", r.identity.year ?: 0)
            put("imdb", r.identity.imdb ?: ""); put("tmdb", r.identity.tmdb ?: "")
            put("aliases", JSONArray(r.identity.alternateTitles.take(32)))
            put("providerAssetKey",r.identity.providerAssetKey ?: "");put("providerAssetId",r.identity.providerAssetId ?: "")
            put("tracked", r.tracked); r.favorite?.let { put("favorite", it) }
            put("changed", r.changed); put("hiddenUntil", r.hiddenUntil); put("aliasesVersion", r.aliasesVersion)
        }) }
        // Keep only small identity records, never the catalogue or playback addresses.
        if (!prefs.edit().putString("identities", a.toString()).commit()) {
            records = readRecords()
            error("Falha ao salvar vínculos locais")
        }
    }
    private suspend fun ensureRecord(dao: PlaylistCacheDao, playlist: String, kind: String, id: String,
        fallback: ContentIdentity? = null, refresh: Boolean = false): Record? {
        val k = key(playlist, kind, id)
        if (!refresh) synchronized(lock) { records[k]?.takeIf { it.tracked && (kind!="movies" || it.identity.providerAssetKey!=null) }?.let { return it } }
        val basic = if (kind == "movies") dao.getActiveMovieById(playlist, id)?.let {
            providerMovieIdentity(it.streamUrl).let { asset ->
                ContentIdentity(kind,it.name,it.year,providerAssetKey=asset?.key,providerAssetId=asset?.id)
            }
        } else dao.getActiveGeneration(playlist)?.let { g ->
            dao.getParentalSeriesById(playlist, g.generationId, id)?.let {
                ContentIdentity(kind, it.name, it.year, it.imdbId, it.tmdbId)
            }
        }
        return synchronized(lock) {
            val previous = records[k]
            val known = previous?.identity
            val value = if (known != null) known.copy(
                title = basic?.title?.takeIf { it.isNotBlank() } ?: known.title,
                year = known.year ?: basic?.year ?: fallback?.year,
                imdb = known.imdbKey ?: basic?.imdbKey,
                tmdb = known.tmdbKey ?: basic?.tmdbKey,
                providerAssetKey=basic?.providerAssetKey ?: known.providerAssetKey,
                providerAssetId=basic?.providerAssetId ?: known.providerAssetId
            ) else basic ?: fallback ?: return@synchronized null
            val record = previous?.copy(tracked = true, identity = value) ?: Record(k, playlist, id, value, true, null, 0)
            records[k] = record
            record
        }
    }
    suspend fun setFavorite(dao: PlaylistCacheDao, playlist: String, kind: String, id: String, favorite: Boolean) {
        ensureRecord(dao, playlist, kind, id) ?: return
        synchronized(lock) {
            val k = key(playlist, kind, id); val r = records[k] ?: return
            val stamp = maxOf(System.currentTimeMillis(), records.values.maxOfOrNull { it.changed }?.plus(1) ?: 0)
            records[k] = r.copy(favorite = favorite, changed = stamp)
            persist(); changed()
        }
    }
    private fun mergeExternal(playlist: String, id: String, value: ContentIdentity): Boolean {
        if (playlist.isBlank() || id.isBlank()) return false
        val k = key(playlist, value.kind, id); val old = records[k]
        val merged = value.copy(providerAssetKey=value.providerAssetKey ?: old?.identity?.providerAssetKey,
            providerAssetId=value.providerAssetId ?: old?.identity?.providerAssetId, imdb = value.imdbKey ?: old?.identity?.imdbKey,
            tmdb = value.tmdbKey ?: old?.identity?.tmdbKey, year = value.year ?: old?.identity?.year,
            alternateTitles = (value.alternateTitles + old?.identity?.alternateTitles.orEmpty())
                .filter { it.isNotBlank() && it.length <= 160 }.distinct().take(32))
        if (old?.identity == merged) return false
        if (old == null && records.values.count { !it.tracked } >= 512) {
            records.entries.firstOrNull { !it.value.tracked }?.key?.let { records.remove(it) }
        }
        records[k] = old?.copy(identity = merged) ?: Record(k, playlist, id, merged, false, null, 0)
        return true
    }
    fun rememberExternal(playlist: String, id: String, value: ContentIdentity) = synchronized(lock) {
        if (mergeExternal(playlist, id, value)) { persist(); changed() }
    }
    /** Upgrade only tracked unmatched identities; two bounded requests at a time. */
    suspend fun enrichAlternateTitles(playlist: String? = null, kind: String? = null, resolve: suspend (ContentIdentity) -> ContentIdentity?): Boolean {
        val requests = synchronized(lock) {
            records.values.filter { it.tracked && (kind == null || it.identity.kind == kind && it.key in eligibleKeys) && it.aliasesVersion < 2 &&
                (playlist == null || (it.playlist != playlist && links[it.key].isNullOrEmpty())) &&
                System.currentTimeMillis() - (metadataAttempts["aliases:${it.key}"] ?: 0L) >= 5 * 60_000L }
                .sortedByDescending { maxOf(it.changed, historyPriorities[it.key] ?: 0L) }
        }
        var updated = false
        for (batch in requests.chunked(2)) {
            val found = coroutineScope {
                batch.map { r -> async {
                    synchronized(lock) {
                        if (metadataAttempts.size >= 512) metadataAttempts.remove(metadataAttempts.keys.first())
                        metadataAttempts["aliases:${r.key}"] = System.currentTimeMillis()
                    }
                    val value = try { kotlinx.coroutines.withTimeoutOrNull(2_500L) { resolve(r.identity) } }
                        catch (cancelled: kotlinx.coroutines.CancellationException) {
                            synchronized(lock) { metadataAttempts.remove("aliases:${r.key}") }; throw cancelled
                        }
                        catch (_: Exception) { null }
                    value?.takeIf { it.alternateTitles.isNotEmpty() }?.let { r to it }
                } }.awaitAll().filterNotNull()
            }
            synchronized(lock) {
                for ((r, value) in found) {
                    if (mergeExternal(r.playlist, r.id, value)) updated = true
                    records[r.key]?.let { records[r.key] = it.copy(aliasesVersion = 2) }
                }
                if (found.isNotEmpty()) persist()
            }
        }
        return updated
    }
    /** Resolve only exact-name candidates lacking a release year, never the whole catalogue. */
    suspend fun enrichMatchingMovies(playlist: String, resolve: suspend (MovieMetadataRequest) -> ContentIdentity?): Boolean {
        val requests = synchronized(lock) {
            if (linkedPlaylist != playlist) return false
            val sources = records.values.filter { it.tracked && it.identity.kind == "movies" && it.key in eligibleKeys }
            val sourcesByName = sources.groupBy { it.identity.normalizedTitle }
            val priorities = sourcesByName.mapValues { (_, items) -> items.maxOf { maxOf(it.changed, historyPriorities[it.key] ?: 0L) } }
            val names = pendingMovies.associateWith { ContentIdentity("movies", it.title, it.year).normalizedTitle }
            val targets = pendingMovies.sortedWith(compareByDescending<MovieMetadataRequest> { priorities[names[it]] ?: 0L }
                .thenBy { ContentIdentity.isSubtitled(it.title) }.thenBy { it.order ?: Int.MAX_VALUE })
            val paired = targets.flatMap { target ->
                val name = names[target]
                val origin = sourcesByName[name].orEmpty().filter { it.identity.normalizedYear == null }
                    .sortedByDescending { maxOf(it.changed, historyPriorities[it.key] ?: 0L) }
                    .map { MovieMetadataRequest(it.playlist, it.id, it.identity.title, it.identity.year) }
                origin + if (ContentIdentity("movies", target.title, target.year).normalizedYear == null) listOf(target) else emptyList()
            }
            paired.distinctBy { key(it.playlist, "movies", it.id) }.filter {
                val last = metadataAttempts[key(it.playlist, "movies", it.id)] ?: 0L
                System.currentTimeMillis() - last >= 5 * 60_000L
            }
        }
        var updated = false
        for (batch in requests.chunked(2)) {
            val resolved = coroutineScope {
                batch.map { request -> async {
                    synchronized(lock) {
                        if (metadataAttempts.size >= 512) metadataAttempts.remove(metadataAttempts.keys.first())
                        metadataAttempts[key(request.playlist, "movies", request.id)] = System.currentTimeMillis()
                    }
                    val value = try { kotlinx.coroutines.withTimeoutOrNull(2_500L) { resolve(request) } }
                        catch (cancelled: kotlinx.coroutines.CancellationException) {
                            synchronized(lock) { metadataAttempts.remove(key(request.playlist, "movies", request.id)) }; throw cancelled
                        }
                        catch (_: Exception) { null }
                    value?.let { request to it }
                } }.awaitAll().filterNotNull()
            }
            synchronized(lock) {
                for ((request, value) in resolved) if (mergeExternal(request.playlist, request.id, value)) updated = true
                if (resolved.isNotEmpty()) persist()
            }
        }
        return updated
    }
    suspend fun enrichMatchingSeries(playlist: String, resolve: suspend (ContentIdentity) -> ContentIdentity?): Boolean {
        val requests = synchronized(lock) {
            if (linkedPlaylist != playlist) return false
            pendingSeries.filter { (id, _) ->
                System.currentTimeMillis() - (metadataAttempts[key(playlist, "series", id)] ?: 0L) >= 5 * 60_000L
            }
        }
        var updated = false
        for (batch in requests.chunked(2)) {
            val found = coroutineScope { batch.map { (id, identity) -> async {
                val value = try { kotlinx.coroutines.withTimeoutOrNull(2_500L) { resolve(identity) } }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                synchronized(lock) { metadataAttempts[key(playlist, "series", id)] = System.currentTimeMillis() }
                value?.let { id to it }
            } }.awaitAll().filterNotNull() }
            synchronized(lock) {
                for ((id, value) in found) if (mergeExternal(playlist, id, value)) updated = true
                if (found.isNotEmpty()) persist()
            }
        }
        return updated
    }
    suspend fun reconcile(dao: PlaylistCacheDao, playlist: String, history: List<WatchHistoryEntry>, force: Boolean = false, reuseUnchanged: Boolean = false, kind: String? = null, origin:String?=null) = reconcileLock.withLock {
        require(kind == null || kind in listOf("movies", "series"))
        val generation = dao.getActiveGeneration(playlist)?.generationId ?: return@withLock
        val favoritePrefs = app.getSharedPreferences("iptvpremium_favorites_v1", Context.MODE_PRIVATE)
        // Existing 1.05 favorites are imported as timestamp-zero facts, never overwrite an explicit removal.
        favoritePrefs.all.forEach { (name, raw) ->
            val recordKind = when { name.startsWith("movies_") -> "movies"; name.startsWith("series_") -> "series"; else -> return@forEach }
            if (kind != null && kind != recordKind) return@forEach
            val source = name.removePrefix("${recordKind}_")
            if(origin!=null && source!=playlist && source!=origin)return@forEach
            (raw as? Set<*>)?.filterIsInstance<String>()?.forEach { id ->
                currentCoroutineContext().ensureActive()
                val r = ensureRecord(dao, source, recordKind, id, refresh = force) ?: return@forEach
                synchronized(lock) { val latest = records[r.key]!!
                    if (latest.favorite == null) records[r.key] = latest.copy(favorite = true) }
            }
        }
        val activeHistoryTitles = history.filter { it.inProgress }.map {
            ContentIdentity(if (it.contentType == PlaybackContentType.MOVIE) "movies" else "series",
                if (it.contentType == PlaybackContentType.EPISODE) it.seriesTitle.orEmpty() else it.title)
        } + synchronized(lock) { records.values.filter { it.favorite == true }.map { it.identity } }
        history.distinctBy { h ->
            Triple(h.playlistId, h.contentType,
                if (h.contentType == PlaybackContentType.EPISODE) h.seriesId ?: h.contentId else h.contentId)
        }.forEach { h ->
            currentCoroutineContext().ensureActive()
            val recordKind = if (h.contentType == PlaybackContentType.MOVIE) "movies" else if (h.contentType == PlaybackContentType.EPISODE) "series" else return@forEach
            if (kind != null && kind != recordKind) return@forEach
            val id = if (recordKind == "series") h.seriesId ?: return@forEach else h.contentId
            if (kind != null && !h.inProgress && synchronized(lock) { records[key(h.playlistId, recordKind, id)] == null } &&
                activeHistoryTitles.none { it.kind == recordKind && it.sharesTitle(ContentIdentity(recordKind,
                    if (recordKind == "series") h.seriesTitle.orEmpty() else h.title)) }) return@forEach
            ensureRecord(dao, h.playlistId, recordKind, id, ContentIdentity(recordKind, if (recordKind == "series") h.seriesTitle.orEmpty() else h.title), refresh = force)
        }
        val snapshot = synchronized(lock) { persist(); records.values.filter {origin==null || it.playlist==playlist || it.playlist==origin} }
        val progressKeys = history.filter { it.inProgress }.map { h ->
            key(h.playlistId, if (h.contentType == PlaybackContentType.MOVIE) "movies" else "series", h.seriesId ?: h.contentId)
        }.toSet()
        val seeds = snapshot.filter { it.tracked && (kind == null || it.identity.kind == kind) &&
            (it.favorite == true || it.key in progressKeys) }
        // Negative favorite/completion facts of the same titles must still supersede old progress.
        val tracked = snapshot.filter { r -> r.tracked && (kind == null || r.identity.kind == kind &&
            (r in seeds || seeds.any { seed -> seed.identity.sharesTitle(r.identity) ||
                seed.identity.imdbKey != null && seed.identity.imdbKey == r.identity.imdbKey ||
                seed.identity.tmdbKey != null && seed.identity.tmdbKey == r.identity.tmdbKey })) }
        val scope = kind ?: "all"
        val signature = snapshot.filter { kind == null || it.identity.kind == kind }.joinToString("\n") {
            "${it.key}:${it.identity}:${it.tracked}:${it.favorite}:${it.key in progressKeys}"
        }
        if ((!force || reuseUnchanged) && synchronized(lock) { linkedPlaylist == playlist && linkedGeneration == generation && linkedSignatures[scope] == signature }) {
            synchronized(lock) { linkedHistory = history.toList() }; return@withLock
        }
        val result = mutableMapOf<String, List<Link>>()
        val unresolved = mutableListOf<MovieMetadataRequest>()
        val unresolvedSeries = mutableListOf<Pair<String, ContentIdentity>>()
        for (kind in (kind?.let { listOf(it) } ?: listOf("movies", "series"))) {
            val wanted = tracked.filter { it.identity.kind == kind }
            if (wanted.isEmpty()) continue
            val ownIds = wanted.filter { it.playlist == playlist }.associateBy { it.id }
            val byName = wanted.flatMap { r -> r.identity.normalizedTitles.map { it to r } }.groupBy({ it.first }, { it.second })
            val byImdb = wanted.filter { it.identity.imdbKey != null }.groupBy { it.identity.imdbKey }
            val byAsset = wanted.filter {it.identity.providerAssetKey!=null}.groupBy {it.identity.providerAssetKey}
            val byTmdb = wanted.filter { it.identity.tmdbKey != null }.groupBy { it.identity.tmdbKey }
            val best = mutableMapOf<String, Pair<Int, MutableList<Pair<Link, ContentIdentity>>>>()
            val overrides = snapshot.filter { it.playlist == playlist && it.identity.kind == kind }.associateBy { it.id }
            // Accent/case-aware candidate patterns, then strict identity/year/ID validation.
            val patterns = wanted.flatMap { it.identity.normalizedTitles }.filter { it.isNotBlank() }
                .map(::sharedTitlePattern).distinct()
            val hintedIds = overrides.values.filter { r -> wanted.any { it.identity.confidence(r.identity) > 0 || it.identity.sharesTitle(r.identity) } }.map { it.id }
            val ids = (ownIds.keys + hintedIds).distinct()
            val imdbs = byImdb.keys.filterNotNull()
            val tmdbs = byTmdb.keys.filterNotNull()
            val seen = mutableSetOf<String>()
            // Bound SQL parameters for Android SQLite (999), including large local favorite sets.
            val assetLookups=if(kind=="movies") wanted.mapNotNull {it.identity.providerAssetId}.distinct().map {"@asset:$it" to emptyList<String>()} else emptyList()
            val lookups: List<Pair<String, List<String>>> = assetLookups + patterns.map { it to emptyList<String>() } + ids.chunked(200).map { "" to it } +
                (if (kind == "series" && (imdbs.isNotEmpty() || tmdbs.isNotEmpty())) listOf("" to emptyList<String>()) else emptyList())
            for ((pattern, targetIds) in lookups) {
              val externalBatches = if (pattern.isEmpty() && targetIds.isEmpty())
                  (imdbs.map { it to true } + tmdbs.map { it to false }).chunked(200) else listOf(emptyList())
              for (external in externalBatches) {
               var after = ""
               while (true) {
                currentCoroutineContext().ensureActive()
                val page = if (kind == "movies") dao.sharedMovieCandidates(playlist, generation, after, if(pattern.startsWith("@asset:")) "" else pattern, targetIds,
                    if(pattern.startsWith("@asset:")) "*/movie/*/*/${pattern.removePrefix("@asset:")}.*" else "")
                    else dao.sharedSeriesCandidates(playlist, generation, after, pattern, targetIds,
                        external.filter { it.second }.map { it.first }, external.filterNot { it.second }.map { it.first })
                if (page.isEmpty()) break
                for (row in page) {
                    kotlinx.coroutines.yield()
                    if (!seen.add(row.contentId)) continue
                    val base = identity(row, kind)
                    val hint = overrides[row.contentId]?.identity
                    val candidate = base.copy(year = hint?.year ?: base.year,
                        imdb = hint?.imdbKey ?: base.imdbKey, tmdb = hint?.tmdbKey ?: base.tmdbKey,
                        alternateTitles = hint?.alternateTitles.orEmpty())
                    val possible = (candidate.normalizedTitles.flatMap { byName[it].orEmpty() } + byImdb[candidate.imdbKey].orEmpty() + byTmdb[candidate.tmdbKey].orEmpty() + byAsset[candidate.providerAssetKey].orEmpty()).distinctBy { it.key }
                    if (kind == "movies" && possible.any { it.playlist != playlist } &&
                        possible.none {it.playlist!=playlist && it.identity.confidence(candidate)>0} &&
                        (candidate.normalizedYear == null || possible.any { it.identity.normalizedYear == null }) && unresolved.size < 128) {
                        unresolved.add(MovieMetadataRequest(playlist, row.contentId, row.title, candidate.year, row.providerOrder))
                    }
                    if (kind == "series" && possible.any { it.playlist != playlist } &&
                        possible.none { it.playlist != playlist && it.identity.confidence(candidate) > 0 } &&
                        candidate.normalizedYear == null && unresolvedSeries.size < 128) {
                        unresolvedSeries.add(row.contentId to candidate)
                    }
                    for (r in possible) {
                        // Same-playlist IDs always keep the original local data, regardless of duplicate names.
                        if (r.playlist == playlist) continue
                        val score = r.identity.confidence(candidate)
                        if (score == 0) continue
                        val previous = best[r.key]
                        if (previous == null) best[r.key] = score to mutableListOf(Link(row.contentId, row.title, row.poster, row.providerOrder) to candidate)
                        else {
                            best[r.key] = maxOf(score, previous.first) to previous.second
                            if (previous.second.size < 65) previous.second.add(Link(row.contentId, row.title, row.poster, row.providerOrder) to candidate)
                        }
                    }
                    ownIds[row.contentId]?.let {
                        result[it.key] = listOf(Link(row.contentId, row.title, row.poster))
                    }
                }
                after = page.last().contentId
               }
              }
            }
            val sourceByKey = wanted.associateBy { it.key }
            best.forEach { (k, match) ->
                val source = sourceByKey[k] ?: return@forEach
                val compatible = match.second.filter { (_, candidate) ->
                    source.identity.confidence(candidate) == match.first ||
                        (source.identity.normalizedYear != null && source.identity.normalizedYear == candidate.normalizedYear &&
                         source.identity.sharesTitle(candidate))
                }
                if (match.second.size <= 64 && ContentIdentity.compatibleVariants(source.identity, compatible.map { it.second }, match.first)) {
                    val preferred = ContentIdentity.preferredVariantIndex(compatible.map { it.first.title to it.first.order })
                    if (preferred != null) result[k] = listOf(compatible[preferred].first)
                }
            }
        }
        if (dao.getActiveGeneration(playlist)?.generationId != generation) return@withLock
        synchronized(lock) {
            if (linkedPlaylist != playlist || linkedGeneration != generation) {
                episodeMaps.clear(); episodeAttempts.clear(); links = emptyMap(); linkedSignatures.clear()
                pendingMovies = emptyList(); pendingSeries = emptyList(); eligibleKeys = emptySet()
            }
            linkedHistory = history.toList()
            linkedPlaylist = playlist; linkedGeneration = generation; linkedSignatures[scope] = signature
            val replacedKeys = records.values.filter { kind == null || it.identity.kind == kind }.map { it.key }.toSet()
            links = links.filterKeys { it !in replacedKeys } + result
            eligibleKeys = (eligibleKeys - replacedKeys) + tracked.map { it.key }
            if (kind == null || kind == "movies") pendingMovies = unresolved.distinctBy { it.id }
            if (kind == null || kind == "series") pendingSeries = unresolvedSeries.distinctBy { it.first }
            historyPriorities = history.filter { it.contentType in listOf(PlaybackContentType.MOVIE, PlaybackContentType.EPISODE) }
                .groupBy { key(it.playlistId, if (it.contentType == PlaybackContentType.MOVIE) "movies" else "series", it.seriesId ?: it.contentId) }.mapValues { it.value.maxOf { entry -> entry.updatedAtMs } }
        }
    }
    /** Exact episode identity resolves differing Xtream/M3U series IDs without title guesses or downloads. */
    suspend fun linkExactSeriesEpisodes(dao:PlaylistCacheDao,playlist:String,history:List<WatchHistoryEntry>,
        sourceUrl:suspend (WatchHistoryEntry)->String?):Boolean {
        val generation=dao.getActiveGeneration(playlist)?.generationId ?: return false
        if(synchronized(lock){linkedPlaylist!=playlist || linkedGeneration!=generation})return false
        var updated=false
        for(h in latestSeriesHistory(history).filter {it.playlistId!=playlist && it.inProgress && it.seasonNumber!=null && (it.episodeNumber ?: 0)>0}) {
            currentCoroutineContext().ensureActive()
            val asset=providerEpisodeIdentity(sourceUrl(h)) ?: continue
            val rows=dao.sharedSeriesEpisodeCandidates(playlist,generation,h.seasonNumber!!,h.episodeNumber!!,"*/series/*/*/${asset.id}.*")
            if(rows.size>64)continue
            val origin=synchronized(lock){records[key(h.playlistId,"series",h.seriesId!!)]} ?: continue
            val matches=rows.filter { row ->
                providerEpisodeIdentity(row.streamUrl)?.key==asset.key && row.seasonNumber==h.seasonNumber && row.episodeNumber==h.episodeNumber &&
                    !(origin.identity.imdbKey!=null && ContentIdentity("series",row.title,imdb=row.imdb).imdbKey?.let {it!=origin.identity.imdbKey}==true) &&
                    !(origin.identity.tmdbKey!=null && ContentIdentity("series",row.title,tmdb=row.tmdb).tmdbKey?.let {it!=origin.identity.tmdbKey}==true)
            }
            val index=ContentIdentity.preferredVariantIndex(matches.map {it.title to it.providerOrder}) ?: continue
            val row=matches[index]
            synchronized(lock) {
                if(linkedPlaylist==playlist && linkedGeneration==generation) {
                    val target=listOf(Link(row.seriesId,row.title,row.poster,row.providerOrder))
                    val episode=SeriesEpisodeInfo(row.episodeId,h.seasonNumber,h.episodeNumber,row.episodeTitle,null,null,null,null)
                    val coordinate=h.seasonNumber to h.episodeNumber
                    if(links[origin.key]!=target || episodeMaps[row.seriesId]?.get(coordinate)!=episode) {
                        links=links + (origin.key to target)
                        episodeMaps[row.seriesId]=episodeMaps[row.seriesId].orEmpty() + (coordinate to episode)
                        updated=true
                    }
                }
            }
        }
        if(updated)changed()
        return updated
    }

    private fun latestSeriesHistory(raw: List<WatchHistoryEntry>) = raw
        .filter { it.contentType == PlaybackContentType.EPISODE && it.seriesId != null }
        .sortedByDescending { it.updatedAtMs }.distinctBy { it.playlistId to it.seriesId }

    /** A newer watch on any linked list supersedes older progress for the same target series. */
    private fun targetSeriesHistory(playlist: String, raw: List<WatchHistoryEntry>): List<Pair<Link, WatchHistoryEntry>> =
        latestSeriesHistory(raw).flatMap { h ->
            val targets = if (h.playlistId == playlist) listOf(Link(h.seriesId!!, h.seriesTitle.orEmpty(), h.seriesPosterUrl))
                else links[key(h.playlistId, "series", h.seriesId!!)].orEmpty()
            targets.map { it to h }
        }.sortedByDescending { it.second.updatedAtMs }.distinctBy { it.first.id }

    /** Hydrate only linked in-progress series, using real target-provider episode coordinates. */
    suspend fun importEpisodeLinks(playlist: String, refresh: Boolean = false,
        load: suspend (String) -> List<SeriesEpisodeInfo>): Boolean {
        val requests = synchronized(lock) {
            if (linkedPlaylist != playlist) return false
            targetSeriesHistory(playlist, linkedHistory).filter { (_, h) ->
                h.playlistId != playlist && h.inProgress && h.seasonNumber != null && (h.episodeNumber ?: 0) > 0
            }.filter { (link, h) -> episodeMaps[link.id]?.get(h.seasonNumber to h.episodeNumber) == null &&
                (refresh || System.currentTimeMillis() - (episodeAttempts[link.id] ?: 0L) >= 60_000L)
            }.map { it.first.id }
        }
        var updated = false
        for (batch in requests.chunked(2)) {
            val found = coroutineScope {
                batch.map { id -> async {
                    synchronized(lock) { episodeAttempts[id] = System.currentTimeMillis() }
                    val episodes = try { kotlinx.coroutines.withTimeoutOrNull(8_000L) { load(id) } }
                        catch (cancelled: kotlinx.coroutines.CancellationException) {
                            synchronized(lock) { episodeAttempts.remove(id) }; throw cancelled
                        }
                        catch (_: Exception) { null }
                    id to episodes
                } }.awaitAll()
            }
            for ((id, episodes) in found) if (!episodes.isNullOrEmpty()) {
                registerEpisodes(playlist, id, episodes, notify = false); updated = true
            }
        }
        return updated
    }
    fun favorites(playlist: String, kind: String, existing: Set<String>): Set<String> = synchronized(lock) {
        if (linkedPlaylist != playlist) return@synchronized existing
        val events = records.values.filter { it.identity.kind == kind && it.favorite != null }
            .flatMap { r -> links[r.key].orEmpty().map { it.id to r } }.groupBy({ it.first }, { it.second })
        existing.toMutableSet().apply {
            events.forEach { (id, choices) ->
                if (choices.maxBy { it.changed }.favorite == true) add(id) else remove(id)
            }
            if (kind == "series") targetSeriesHistory(playlist, linkedHistory)
                .filter { (_, h) -> h.playlistId != playlist && h.inProgress }.forEach { (link, h) ->
                    val hidden = records.values.filter { it.identity.kind == "series" &&
                        links[it.key].orEmpty().any { target -> target.id == link.id } }.maxOfOrNull { it.hiddenUntil } ?: 0L
                    if (h.updatedAtMs > hidden && episodeMaps[link.id]?.get(h.seasonNumber to h.episodeNumber) == null &&
                        events[link.id]?.maxByOrNull { it.changed }?.favorite != false) add(link.id)
                }
        }.toSet()
    }
    fun registerEpisodes(playlist: String, series: String, episodes: List<SeriesEpisodeInfo>, notify: Boolean = true) = synchronized(lock) {
        if (playlist != linkedPlaylist) return@synchronized
        // Retain only coordinates needed by shared history, not whole anime seasons in RAM.
        val wanted = targetSeriesHistory(playlist, linkedHistory)
            .filter { (link, h) -> link.id == series && h.playlistId != playlist && h.inProgress }
            .map { (_, h) -> h.seasonNumber to h.episodeNumber }.toSet()
        val mapped = episodes.filter { it.seasonNumber >= 0 && it.episodeNumber > 0 &&
            (it.seasonNumber to it.episodeNumber) in wanted }
            .groupBy { it.seasonNumber to it.episodeNumber }.filterValues { it.size == 1 }.mapValues { it.value.single() }
        if (episodeMaps[series] != mapped) {
            episodeMaps[series] = mapped
            if (notify) changed()
        }
    }
    fun projectHistory(playlist: String, raw: List<WatchHistoryEntry>): List<WatchHistoryEntry> = synchronized(lock) {
        if (playlist != linkedPlaylist) return@synchronized raw.filter { it.playlistId == playlist }
        val latest = targetSeriesHistory(playlist, raw).associate { it.first.id to it.second }
        val mapped = raw.flatMap { h ->
            val kind = if (h.contentType == PlaybackContentType.MOVIE) "movies" else if (h.contentType == PlaybackContentType.EPISODE) "series" else return@flatMap emptyList()
            val id = if (kind == "series") h.seriesId ?: return@flatMap emptyList() else h.contentId
            val r = records[key(h.playlistId, kind, id)]
            val targets = if (h.playlistId == playlist) listOf(Link(id, h.title, h.posterUrl)) else r?.let { links[it.key] }.orEmpty()
            targets.mapNotNull { link ->
                if (kind == "series" && h.playlistId != playlist && latest[link.id] != h) return@mapNotNull null
                val hidden = records.values.filter { it.identity.kind == kind &&
                    (links[it.key].orEmpty().any { target -> target.id == link.id } || (it.playlist == playlist && it.id == link.id))
                }.maxOfOrNull { it.hiddenUntil } ?: 0
                if (h.updatedAtMs <= hidden) return@mapNotNull null
                if (h.playlistId == playlist) return@mapNotNull h
                if (kind == "movies") h.copy(playlistId = playlist, contentId = link.id, title = link.title,
                    posterUrl = link.poster, sharedFromPlaylist = h.playlistId)
                else {
                    val episode = episodeMaps[link.id]?.get(h.seasonNumber to h.episodeNumber)
                    if (episode == null) return@mapNotNull null
                    h.copy(playlistId = playlist, seriesId = link.id, seriesTitle = link.title, seriesPosterUrl = link.poster,
                        contentId = episode.id, title = episode.title, positionMs = h.positionMs,
                        durationMs = h.durationMs, seasonNumber = h.seasonNumber, episodeNumber = h.episodeNumber,
                        seriesOnly = false, sharedFromPlaylist = h.playlistId)
                }
            }
        }
        mapped.sortedByDescending { it.updatedAtMs }.distinctBy { it.contentType to it.contentId }
    }
    fun hide(playlist: String, type: PlaybackContentType, contentId: String, raw: List<WatchHistoryEntry>) = synchronized(lock) {
        val entry = projectHistory(playlist, raw).firstOrNull { it.contentType == type && it.contentId == contentId } ?: return@synchronized
        val kind = if (type == PlaybackContentType.MOVIE) "movies" else "series"
        val id = if (kind == "movies") entry.contentId else entry.seriesId ?: return@synchronized
        val until = System.currentTimeMillis()
        records.replaceAll { _, r -> if (r.identity.kind == kind &&
            ((r.playlist == playlist && r.id == id) || (linkedPlaylist == playlist && links[r.key].orEmpty().any { it.id == id }))) r.copy(hiddenUntil = until) else r }
        persist(); changed()
    }
    fun historySource():String? = prefs.getString("completed_history_source",null)?.takeIf {it.isNotBlank()}
    /** Existing active data is the baseline if this installation has not recorded a checkpoint yet. */
    fun seedHistorySource(playlist:String) = synchronized(lock) {
        if(historySource()==null)check(prefs.edit().putString("completed_history_source",playlist).commit())
    }
    /** Called only inside HistorySyncSession's current-run completion guard, on IO. */
    fun checkpointCompletedHistory(playlist:String, historyStore:WatchHistoryStore) {
        val raw=historyStore.allLocal()
        // A linked in-progress episode must have its real target-provider episode ID.
        // An interrupted/failed episode lookup must not promote a partial list to the origin.
        synchronized(lock) {
            val pending=targetSeriesHistory(playlist,linkedHistory).any {(link,h)->
                h.playlistId!=playlist && h.inProgress && h.seasonNumber!=null && (h.episodeNumber?:0)>0 &&
                    episodeMaps[link.id]?.get(h.seasonNumber to h.episodeNumber)==null
            }
            check(!pending) {"Histórico de episódios ainda incompleto"}
        }
        val projected=projectHistory(playlist,raw)
        val favoritePrefs=app.getSharedPreferences("iptvpremium_favorites_v1",Context.MODE_PRIVATE)
        val movieFavorites=favorites(playlist,"movies",favoritePrefs.getStringSet("movies_$playlist",emptySet()).orEmpty())
        val seriesFavorites=favorites(playlist,"series",favoritePrefs.getStringSet("series_$playlist",emptySet()).orEmpty())
        synchronized(lock) {
            check(linkedPlaylist==playlist) {"Vínculos de outra lista"}
            // Materialise completed inherited events under the target IDs, including removals/hides.
            val mapped=records.values.toList().flatMap {r->links[r.key].orEmpty().map {r to it}}
                .groupBy {(r,link)->r.identity.kind to link.id}
            mapped.forEach { (target,candidates)->
                val (source,link)=candidates.maxBy {it.first.changed}
                val k=key(playlist,target.first,target.second)
                records[k]=source.copy(key=k,playlist=playlist,id=target.second,
                    identity=source.identity.copy(title=link.title),tracked=true,
                    hiddenUntil=candidates.maxOf {it.first.hiddenUntil})
            }
            persist()
        }
        historyStore.checkpointPlaylist(playlist,projected)
        check(favoritePrefs.edit().putStringSet("movies_$playlist",movieFavorites)
            .putStringSet("series_$playlist",seriesFavorites).commit()) {"Falha ao salvar favoritos"}
        check(prefs.edit().putString("completed_history_source",playlist).commit()) {"Falha ao concluir histórico"}
    }

    companion object {
        fun resumePosition(entry: WatchHistoryEntry?, position: Long, duration: Long?): Long {
            if (entry?.seriesOnly == true) return 0L
            if (entry?.sharedFromPlaylist != null && duration != null && duration > 0 &&
                (position >= duration || (entry.durationMs != null &&
                kotlin.math.abs(entry.durationMs - duration) > maxOf(90_000L, duration / 10)))) return 0L
            return position.coerceAtLeast(0L)
        }
        @Volatile private var instance: SharedLibraryStore? = null
        fun get(context: Context): SharedLibraryStore = instance ?: synchronized(this) {
            instance ?: SharedLibraryStore(context).also { instance = it }
        }
    }
}

/** Narrow GLOB classes preserve original-language letters instead of replacing them with wildcards. */
private val sharedTitleLetterClasses: Map<Char, String> by lazy {
    val variants = mutableMapOf<Char, MutableSet<Char>>()
    for (letter in '\u0041'..'\u1FFF') {
        if (!letter.isLetter()) continue
        val normalized = java.text.Normalizer.normalize(letter.toString(), java.text.Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
            .lowercase(java.util.Locale.ROOT)
        if (normalized.length == 1) variants.getOrPut(normalized.single()) { linkedSetOf() }.add(letter)
    }
    variants.mapValues { (key, letters) -> "[" + (letters + key.uppercaseChar() + key.lowercaseChar()).joinToString("") + "]" }
}
internal fun sharedTitlePattern(normalizedTitle: String): String = "*" +
    java.text.Normalizer.normalize(normalizedTitle, java.text.Normalizer.Form.NFC).split(' ').filter { it.isNotBlank() }.joinToString("*") { token ->
        token.map { letter ->
            sharedTitleLetterClasses[letter] ?: if (letter.isLetter()) {
                val variants = linkedSetOf(letter, letter.uppercaseChar(), letter.lowercaseChar())
                if (variants.size == 1) letter.toString() else "[" + variants.joinToString("") + "]"
            } else letter.toString()
        }.joinToString("")
    } + "*"
