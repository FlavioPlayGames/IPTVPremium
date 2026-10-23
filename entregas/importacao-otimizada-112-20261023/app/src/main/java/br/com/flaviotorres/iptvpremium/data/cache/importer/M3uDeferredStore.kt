package br.com.flaviotorres.iptvpremium.data.cache.importer

import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Private, generation-scoped source fragments. Never bitmap storage or a RAM catalogue. */
class M3uDeferredStore(private val root:File) {
    private fun directory(generation:String):File {
        require(generation.matches(Regex("[A-Za-z0-9_-]{1,96}")))
        return File(root,generation)
    }
    private fun file(generation:String,type:String):File {
        require(type=="SERIES" || type=="LIVE_TV")
        return File(directory(generation),"$type.m3u.gz")
    }
    fun exists(generation:String,type:String)=file(generation,type).isFile
    fun writer(generation:String)=Writer(generation)
    inner class Writer(private val generation:String):Closeable {
        private val writers=linkedMapOf<String,BufferedWriter>()
        private var entries=0
        private var committed=false
        init {
            check(directory(generation).mkdirs() || directory(generation).isDirectory)
            try {
                for(type in listOf("SERIES","LIVE_TV")) {
                    val part=File(directory(generation),"$type.part")
                    writers[type]=object:GZIPOutputStream(part.outputStream(),32*1024) {
                        init { def.setLevel(java.util.zip.Deflater.BEST_SPEED) }
                    }.bufferedWriter(Charsets.UTF_8)
                }
            }catch(error:Throwable){close();throw error}
        }
        fun append(type:String,extInf:String,url:String) {
            val out=checkNotNull(writers[type]);out.appendLine(extInf);out.appendLine(url)
            if(++entries%256==0)checkBudget()
        }
        private fun checkBudget() {
            check(directory(generation).listFiles().orEmpty().sumOf{it.length()}<=128L*1024*1024) {
                "A lista excedeu o limite de armazenamento temporário."
            }
        }
        fun finish() {
            writers.values.forEach{it.close()};writers.clear();checkBudget()
            for(type in listOf("SERIES","LIVE_TV")) {
                check(File(directory(generation),"$type.part").renameTo(file(generation,type)))
            }
            committed=true
        }
        override fun close() {
            try { writers.values.forEach { runCatching{it.close()} };writers.clear() }
            finally {if(!committed)discard(generation)}
        }
    }
    suspend fun read(generation:String,type:String,consume:suspend(String)->Unit) {
        GZIPInputStream(file(generation,type).inputStream(),32*1024).bufferedReader(Charsets.UTF_8).use { input->
            while(true) {
                currentCoroutineContext().ensureActive()
                val line=input.readLine()?:break
                consume(line)
            }
        }
    }
    fun consumed(generation:String,type:String) {
        file(generation,type).delete()
        val dir=directory(generation)
        if(dir.listFiles().orEmpty().isEmpty())dir.delete()
    }
    fun discard(generation:String){directory(generation).deleteRecursively()}
    /** Called only by the maintenance gate, while no foreground import owns storage. */
    fun prune(keep:Set<String>) {
        root.listFiles().orEmpty().filter {it.isDirectory && it.name !in keep}.forEach{it.deleteRecursively()}
    }
}
