import java.io.File
import kotlinx.coroutines.*
class Context(val cacheDir: File) { val applicationContext get() = this }
class ImageBitmap
class Loader {
 private var diskDirectory: File? = null
 var calls = 0
 var succeedAt = 99
 private fun validUrl(url: String?) = url?.takeIf { it.startsWith("https://") }
 suspend fun load(context: Context, url: String?): ImageBitmap? { calls++; return if(calls >= succeedAt) ImageBitmap() else null }
    @Synchronized
    fun persistentDirectory(context: Context): File? {
        diskDirectory?.let { directory ->
            if (directory.isDirectory || directory.mkdirs() || directory.isDirectory) return directory
            diskDirectory = null
        }
        val directory = File(context.applicationContext.cacheDir, "iptv_movie_posters_session")
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) return null
        // Nunca apaga o diretorio inteiro ao abrir o aplicativo.
        // Initialization is synchronized and happens before any downloader gets this directory.
        val abandoned = 0
        if (abandoned > 0) println(abandoned)
        diskDirectory = directory
        return directory
    }
    suspend fun loadWithRetry(context: Context, rawUrl: String?): ImageBitmap? {
        if (validUrl(rawUrl) == null) return null
        for (attempt in 0..2) {
            currentCoroutineContext().ensureActive()
            load(context, rawUrl)?.let { return it }
            if (attempt < 2) delay(if (attempt == 0) 1_000L else 3_000L)
        }
        return null
    }
}
fun main() = runBlocking {
 val root = java.nio.file.Files.createTempDirectory("poster-test").toFile()
 try {
 val context = Context(root); val loader = Loader()
 val first = loader.persistentDirectory(context)!!
 check(first.isDirectory)
 first.deleteRecursively()
 check(!first.exists())
 check(loader.persistentDirectory(context)!!.isDirectory)
 println("PASS recria pasta apagada mantendo o mesmo carregador vivo")
 File(first,"old.img").writeText("x")
 check(loader.persistentDirectory(context) == first && File(first,"old.img").readText()=="x")
 println("PASS preserva arquivos existentes")
 val retry=Loader(); retry.succeedAt=2
 check(retry.loadWithRetry(context,"https://example.invalid/a") != null && retry.calls==2)
 println("PASS falha transitoria tenta novamente")
 val failure=Loader()
 check(failure.loadWithRetry(context,"https://example.invalid/a")==null && failure.calls==3)
 println("PASS falha permanente limitada a tres tentativas")
 val invalid=Loader();check(invalid.loadWithRetry(context,null)==null && invalid.calls==0)
 println("PASS URL ausente nao inicia rede")
 val cancelled=Loader(); val job=launch { cancelled.loadWithRetry(context,"https://example.invalid/a") }
 delay(100);job.cancelAndJoin();check(cancelled.calls==1)
 println("PASS sair da tela cancela repeticao")
 } finally { root.deleteRecursively() }
}
