package br.com.flaviotorres.iptvpremium.data.remote

import java.io.BufferedReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** At most one queued block, one being read and one being parsed; never a whole playlist. */
internal object M3uLineStream {
    data class Metrics(val blocks:Long,val readNanos:Long,val queueWaitNanos:Long,val peakLines:Int,val peakCharacters:Int)
    suspend fun consume(reader:BufferedReader,onFirstLine:()->Unit={},onLine:suspend(String)->Unit):Metrics=coroutineScope {
        val blocks=Channel<List<String>>(1)
        var count=0L;var readNanos=0L;var waitNanos=0L;var peakLines=0;var peakChars=0
        val readerJob=launch(Dispatchers.IO) {
            try {
                var ended=false
                while(!ended) {
                    val lines=M3uBlockingIo.run {
                        val current=ArrayList<String>(128);var chars=0
                        while(current.size<128 && chars<64*1024) {
                            if(Thread.currentThread().isInterrupted)throw java.io.InterruptedIOException("M3U cancelada")
                            val started=System.nanoTime()
                            val line=reader.readLine()
                            readNanos+=System.nanoTime()-started
                            if(line==null){ended=true;break}
                            if(count==0L && current.isEmpty())onFirstLine()
                            current.add(line);chars+=line.length
                        }
                        peakChars=maxOf(peakChars,chars)
                        current
                    }
                    if(lines.isNotEmpty()) {
                        count++;peakLines=maxOf(peakLines,lines.size)
                        val started=System.nanoTime();blocks.send(lines);waitNanos+=System.nanoTime()-started
                    }
                }
            } finally {blocks.close()}
        }
        for(lines in blocks) for(line in lines) {currentCoroutineContext().ensureActive();onLine(line)}
        readerJob.join()
        Metrics(count,readNanos,waitNanos,peakLines,peakChars)
    }
}
