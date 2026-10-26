package br.com.flaviotorres.iptvpremium.data.cache

import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.PlaylistCacheGenerationEntity
import java.lang.reflect.Proxy
import kotlinx.coroutines.*

private data class Gen(var status: String, var rows: Int = 220, var age: Long = 0)
private class Store {
    val generations = linkedMapOf<Pair<String, String>, Gen>()
    val selected = mutableMapOf<String, Int>()
    val deleted = mutableListOf<String>()
    var failNext = false
    var firstDelete: CompletableDeferred<Unit>? = null
    fun add(id: String, status: String, playlist: String = "p", age: Long = 0) { generations[playlist to id] = Gen(status, age = age) }
    val dao = Proxy.newProxyInstance(PlaylistCacheDao::class.java.classLoader, arrayOf(PlaylistCacheDao::class.java)) { _, method, raw ->
        val a = raw.orEmpty()
        when (method.name) {
            "retireUnregisteredActiveGenerations" -> {
                val keep = a[0] as List<*>
                var retired = 0
                generations.forEach { (key, gen) ->
                    if (key.first !in keep && gen.status == "ACTIVE" && gen.age < a[1] as Long) {
                        gen.status = "STALE"; retired++
                    }
                }
                retired
            }
            "upsertGeneration" -> {
                val g = a[0] as PlaylistCacheGenerationEntity
                generations[g.playlistId to g.generationId] = Gen(g.status, age = System.currentTimeMillis() - 48L * 60 * 60 * 1000)
                Unit
            }
            "oldestAbandonedImport" -> generations.entries.firstOrNull { it.key.first == a[0] && it.value.status == "IMPORTING" && it.value.age < a[1] as Long }?.key?.second
            "generationStatus" -> generations[a[0] to a[1]]?.status
            "getOldestStaleGenerationId" -> generations.entries.firstOrNull { it.key.first == a[0] && it.value.status == "STALE" }?.key?.second?.also { selected[it] = (selected[it] ?: 0) + 1 }
            "deleteGenerationRow" -> {
                val key = a[0] to a[1]
                check(generations[key]?.status in listOf("STALE", "IMPORTING"))
                deleted.add(a[1] as String); generations.remove(key); Unit
            }
            "discardIncompleteImport" -> { generations.remove(a[0] to a[1]); true }
            else -> if (method.name.startsWith("delete") && method.name.endsWith("BatchForGeneration")) {
                val g = generations.getValue((a[0] as String) to (a[1] as String))
                check(g.status in listOf("STALE", "IMPORTING"))
                if (failNext) { failNext = false; error("simulated disk failure") }
                val n = minOf(g.rows, a[2] as Int); g.rows -= n
                if (n > 0) firstDelete?.complete(Unit)
                n
            } else error("Unexpected call ${method.name}")
        }
    } as PlaylistCacheDao
}
fun main() = runBlocking {
    var passed = 0
    suspend fun scenario(name: String, block: suspend () -> Unit) { block(); passed++; println("PASS $name") }
    scenario("drains old generations across playlists while retaining ACTIVE and READY") {
        val s = Store(); s.add("active", "ACTIVE"); s.add("ready", "READY"); s.add("old1", "STALE"); s.add("old2", "STALE"); s.add("other", "STALE", "q")
        val m = PlaylistCacheGenerationManager(s.dao)
        for (p in listOf("p", "q")) while (m.cleanupOldestStaleGeneration(p)) { yield() }
        check(s.deleted.toSet() == setOf("old1", "old2", "other")); check(s.generations.values.all { it.status in setOf("ACTIVE", "READY") })
    }
    scenario("process lease preserves an active import even after 48 hours") {
        val s = Store(); val m = PlaylistCacheGenerationManager(s.dao)
        val id = m.beginImport("p"); s.add("stale", "STALE")
        check(m.cleanupOldestStaleGeneration("p")); check(s.generations.containsKey("p" to id))
        check(!m.cleanupOldestStaleGeneration("p")); m.discardImport("p", id)
    }
    scenario("abandoned import removed; recent import retained") {
        val s = Store(); s.add("orphan", "IMPORTING"); s.add("recent", "IMPORTING", age = System.currentTimeMillis())
        val m = PlaylistCacheGenerationManager(s.dao)
        check(m.cleanupOldestStaleGeneration("p")); check(s.deleted == listOf("orphan")); check(!m.cleanupOldestStaleGeneration("p"))
    }
    scenario("cancelled batch cleanup resumes without losing active catalog") {
        val s = Store(); s.add("active", "ACTIVE"); s.add("old", "STALE"); s.firstDelete = CompletableDeferred()
        val m = PlaylistCacheGenerationManager(s.dao)
        val job = launch { m.cleanupOldestStaleGeneration("p") }; s.firstDelete!!.await(); job.cancelAndJoin()
        check(s.generations.containsKey("p" to "active")); check(s.generations.containsKey("p" to "old"))
        check(m.cleanupOldestStaleGeneration("p")); check(s.deleted == listOf("old"))
    }
    scenario("failed delete leaves generation recoverable") {
        val s = Store(); s.add("old", "STALE"); s.failNext = true
        val m = PlaylistCacheGenerationManager(s.dao)
        check(runCatching { m.cleanupOldestStaleGeneration("p") }.isFailure)
        check(s.generations.containsKey("p" to "old")); check(m.cleanupOldestStaleGeneration("p"))
    }
    scenario("16 concurrent cleanup requests select each generation once") {
        val s = Store(); repeat(4) { s.add("old$it", "STALE") }
        coroutineScope {
            (0 until 16).map { async { PlaylistCacheGenerationManager(s.dao).cleanupOldestStaleGeneration("p") } }.awaitAll()
        }
        check(s.deleted.size == 4 && s.selected.values.all { it == 1 })
    }
    scenario("only unregistered mature ACTIVE catalogs are retired") {
        val s = Store()
        s.add("valid", "ACTIVE", "registered"); s.add("disabled", "ACTIVE", "disabled")
        s.add("orphan", "ACTIVE", "removed"); s.add("fresh", "ACTIVE", "new", age = System.currentTimeMillis())
        s.add("ready", "READY", "removed")
        val m = PlaylistCacheGenerationManager(s.dao)
        check(m.retireUnregisteredCatalogs(setOf("registered", "disabled"), System.currentTimeMillis() - 900_000) == 1)
        check(s.generations["removed" to "orphan"]?.status == "STALE")
        check(s.generations["registered" to "valid"]?.status == "ACTIVE")
        check(s.generations["disabled" to "disabled"]?.status == "ACTIVE")
        check(s.generations["new" to "fresh"]?.status == "ACTIVE")
        check(s.generations["removed" to "ready"]?.status == "READY")
    }
    scenario("live import protects its older ACTIVE fallback even before registry save") {
        val s = Store(); val m = PlaylistCacheGenerationManager(s.dao)
        s.add("fallback", "ACTIVE", "not_saved_yet")
        val token = m.beginImport("not_saved_yet")
        check(m.retireUnregisteredCatalogs(emptySet(), System.currentTimeMillis()) == 0)
        check(s.generations["not_saved_yet" to "fallback"]?.status == "ACTIVE")
        m.discardImport("not_saved_yet", token)
        check(m.retireUnregisteredCatalogs(emptySet(), System.currentTimeMillis()) == 1)
    }
    println("$passed generation scenarios passed (real manager and DAO interface, fake persistence)")
}
