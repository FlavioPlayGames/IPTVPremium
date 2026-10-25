package br.com.flaviotorres.iptvpremium.data.remote

import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.*
import java.io.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class Import25HttpTest {
 @Test fun streamingM3uPreservesLinesGzipAndCancelsConnection()=runBlocking {
  val began=CountDownLatch(1);val closed=CountDownLatch(1)
  URL.setURLStreamHandlerFactory { scheme ->
   if(scheme!="importtest")null else object:URLStreamHandler(){
    override fun openConnection(url:URL):URLConnection=object:HttpURLConnection(url){
     override fun usingProxy()=false
     override fun connect(){}
     override fun disconnect(){if(url.path=="/blocked")closed.countDown()}
     override fun getResponseCode():Int {
      assertEquals(if(url.path=="/reader")10_000 else 15_000,connectTimeout)
      assertEquals(if(url.path=="/reader")15_000 else 30_000,readTimeout)
      return 200
     }
     override fun getContentEncoding()=if(url.path=="/gzip")"gzip"else null
     override fun getContentType()="audio/x-mpegurl"
     override fun getInputStream():InputStream {
      if(url.path=="/blocked")return object:InputStream(){override fun read():Int{began.countDown();check(closed.await(5,TimeUnit.SECONDS));return -1}}
      val raw="#EXTM3U\r\n#EXTINF:-1,Filme ação\nhttps://invalid.example/movie\n".toByteArray()
      if(url.path!="/gzip")return ByteArrayInputStream(raw)
      val b=ByteArrayOutputStream();GZIPOutputStream(b).use{it.write(raw)};return ByteArrayInputStream(b.toByteArray())
     }
    }
   }
  }
  val client=DefaultPlaylistHttpClient()
  assertTrue(client.withReader("importtest://provider/reader"){it.readText()}.startsWith("#EXTM3U"))
  val plain=mutableListOf<String>();val gzip=mutableListOf<String>()
  client.readLines("importtest://provider/plain"){plain+=it}
  client.readLines("importtest://provider/gzip"){gzip+=it}
  assertEquals(listOf("#EXTM3U","#EXTINF:-1,Filme ação","https://invalid.example/movie"),plain);assertEquals(plain,gzip)
  val task=launch(Dispatchers.IO){client.readLines("importtest://provider/blocked"){}}
  assertTrue(began.await(3,TimeUnit.SECONDS));assertFalse(task.isCompleted);task.cancel();withTimeout(2_000){task.join()};assertEquals(0L,closed.count)
 }
}
