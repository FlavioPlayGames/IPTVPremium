package br.com.flaviotorres.iptvpremium.core.storage
import java.io.File
import java.nio.file.Files
fun main() {
    val overloaded = AppStorageSnapshot(100_000_000, 1_000_000_000, 7_000_000, 7_000_000, true)
    check(AppStoragePolicy.cacheAllowance(overloaded) == 24_000_000L)
    check(AppStoragePolicy.cacheAllowance(overloaded.copy(exact=false)) == 24_000_000L)
    check(AppStoragePolicy.cacheAllowance(overloaded.copy(dataBytes=100_000_000)) == 100_000_000L)
    check(AppStoragePolicy.cacheAllowance(overloaded.copy(dataBytes=250_000_000)) == 37_000_000L)
    println("PASS quota: over-target / estimate / normal / remaining allowance")
    val root=Files.createTempDirectory("pressure-budget").toFile()
    try {
        val cache=File(root,"cache").apply{mkdirs()}
        val budget=CacheStorageBudget(cache,File(root,"subtitles"))
        check(budget.allowsArtworkPrefetch())
        budget.setTotalLimit(AppStoragePolicy.cacheAllowance(overloaded))
        check(!budget.allowsArtworkPrefetch())
        val dir=File(cache,"iptv_movie_posters_125").apply{mkdirs()}
        fun write(name:String): File {
            val f=File(dir,"$name.img")
            val r=checkNotNull(budget.reserve(f,6_000_000))
            r.use { it.temporary.writeBytes(ByteArray(6_000_000));check(it.temporary.renameTo(f)) }
            return f
        }
        val first=write("first")
        budget.retain(first).use {
            repeat(7){write("next$it")}
            check(first.exists())
            check(budget.accountedBytes()<=24_000_000)
        }
        println("PASS pressure: visible downloads succeed, LRU stays bounded, active reader preserved")
        check(budget.reserve(File(dir,"oversized.img"),25_000_000)==null)
        val active=checkNotNull(budget.reserve(File(dir,"active.img"),1_000_000))
        active.use{ it.temporary.writeBytes(ByteArray(10));budget.maintain();check(it.temporary.exists()) }
        check(!active.temporary.exists())
        println("PASS oversized reservation rejected; maintenance protects active transfer; release cleans temporary")
        budget.setTotalLimit(100_000_000)
        check(budget.allowsArtworkPrefetch())
        println("PASS prefetch restored after pressure clears")
    } finally {root.deleteRecursively()}
}
