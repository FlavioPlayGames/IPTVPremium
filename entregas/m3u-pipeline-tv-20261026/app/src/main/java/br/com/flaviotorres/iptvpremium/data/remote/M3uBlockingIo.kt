package br.com.flaviotorres.iptvpremium.data.remote

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Some HttpURLConnection implementations cannot interrupt a blocked read/close.
 * Keep those operations off the structured import job, with strictly bounded workers.
 * Cancellation releases the coroutine; late results are never delivered to the parser.
 */
internal object M3uBlockingIo {
    private fun pool(name:String,workers:Int,queue:Int)=ThreadPoolExecutor(
        workers,workers,30L,TimeUnit.SECONDS,ArrayBlockingQueue<Runnable>(queue),
        {task->Thread(task,name).apply{isDaemon=true}},ThreadPoolExecutor.AbortPolicy()
    ).apply{allowCoreThreadTimeOut(true)}
    private val reads=pool("m3u-network",2,2)
    private val closes=pool("m3u-close",2,16)
    suspend fun <T> run(block:()->T):T=suspendCancellableCoroutine {continuation->
        val task=FutureTask<Unit> {
            try {
                if(continuation.isActive) {
                    val result=block()
                    if(continuation.isActive)continuation.resume(result)
                }
            } catch(error:Throwable) {
                if(continuation.isActive)continuation.resumeWithException(error)
            }
        }
        continuation.invokeOnCancellation {task.cancel(true);reads.remove(task)}
        try {reads.execute(task)} catch(error:java.util.concurrent.RejectedExecutionException) {
            if(continuation.isActive)continuation.resumeWithException(java.io.IOException("Uma leitura M3U anterior ainda esta sendo encerrada. Aguarde alguns segundos e tente novamente."))
        }
    }
    fun close(block:()->Unit) {
        // Only up to two reads can be blocked. Closing them may block too; never on the UI/import job.
        try {closes.execute {runCatching(block)}} catch(_:java.util.concurrent.RejectedExecutionException) { }
    }
}
