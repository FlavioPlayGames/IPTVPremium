package br.com.flaviotorres.iptvpremium.core.player

import android.content.Context
import br.com.flaviotorres.iptvpremium.BuildConfig
import br.com.flaviotorres.iptvpremium.core.device.PortalIdentityStore
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class ManualSubtitleRequest(val id:String,val url:String,val expires:Long)
internal data class ManualSubtitleDownload(val name:String,val bytes:ByteArray)

/** Same device credential and origin as the portal. Never follows a redirect with that credential. */
internal class ManualSubtitleClient(context:Context) {
    private val app=context.applicationContext
    private val base=BuildConfig.DEVICE_PORTAL_URL.trimEnd('/')
    suspend fun create(target:ManualSubtitleTarget):ManualSubtitleRequest {
        val id=java.util.UUID.randomUUID().toString()
        val result=json("/request","POST",JSONObject().put("id",id).put("target",target.json()))
        val link=result.getString("url")
        check(link.startsWith(base+"/#access=")){"A central retornou um acesso incompatível."}
        return ManualSubtitleRequest(result.getString("id"),link,result.getLong("expires"))
    }
    suspend fun status(id:String):JSONObject=json("/request?id=${checked(id)}").getJSONObject("request")
    suspend fun cancel(id:String){json("/request?id=${checked(id)}","DELETE")}
    suspend fun receipt(id:String,count:Int,success:Boolean=true){json("/receipt?id=${checked(id)}","POST",JSONObject().put("success",success).put("count",count))}
    suspend fun download(id:String):ManualSubtitleDownload {
        val response=request("/file?id=${checked(id)}","GET",null,ManualSubtitleArchive.MAX_UPLOAD)
        val expected=response.headers["x-subtitle-sha256"]?.singleOrNull().orEmpty()
        check(expected.matches(Regex("[a-f0-9]{64}"))&&MessageDigest.getInstance("SHA-256").digest(response.bytes).joinToString(""){"%02x".format(it)}==expected){"O arquivo recebido ficou incompleto. Tente novamente."}
        val name=java.net.URLDecoder.decode(response.headers["x-subtitle-name"]?.singleOrNull().orEmpty(),"UTF-8")
        return ManualSubtitleDownload(name,response.bytes)
    }
    private suspend fun json(path:String,method:String="GET",body:JSONObject?=null):JSONObject {
        val response=request(path,method,body?.toString()?.toByteArray(Charsets.UTF_8),256*1024)
        return JSONObject(String(response.bytes,Charsets.UTF_8))
    }
    private data class Response(val bytes:ByteArray,val headers:Map<String,List<String>>)
    private suspend fun request(path:String,method:String,body:ByteArray?,limit:Int):Response {
        check(base.isNotBlank()){ "A central ainda não foi configurada." }
        val endpoint=URL(base);check(endpoint.protocol=="https"&&endpoint.userInfo==null)
        val secret=withContext(Dispatchers.IO){PortalIdentityStore(app).load().optString("secret")}
        check(secret.matches(Regex("[a-f0-9]{64}"))){"Sincronize o aparelho com a central primeiro."}
        return suspendCancellableCoroutine {continuation->
            val connection=AtomicReference<HttpURLConnection?>()
            val task=java.util.concurrent.FutureTask<Unit> {
                var c:HttpURLConnection?=null
                try{
                    if(!continuation.isActive)return@FutureTask Unit
                    c=URL(base+"/api/device/manual-subtitles"+path).openConnection() as HttpURLConnection;connection.set(c)
                    c.connectTimeout=8_000;c.readTimeout=12_000;c.instanceFollowRedirects=false;c.useCaches=false;c.requestMethod=method
                    c.setRequestProperty("Authorization","Bearer $secret");c.setRequestProperty("Accept","application/json")
                    if(body!=null){c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body)}}
                    val status=c.responseCode
                    if(status !in 200..299) {
                        val responseError=runCatching{c?.errorStream?.use{input->val buffer=ByteArray(1024);val n=input.read(buffer);if(n>0)String(buffer,0,n,Charsets.UTF_8)else ""}}.getOrNull()
                        val message=runCatching{JSONObject(responseError.orEmpty()).optString("error")}.getOrNull()?.takeIf{it.isNotBlank()}
                        error(message?:"Não foi possível conectar à central (HTTP $status).")
                    }
                    val bytes=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    c.inputStream.use {input->while(true){if(!continuation.isActive)return@FutureTask Unit
                        val n=input.read(buffer);if(n<0)break;check(bytes.size()+n<=limit){"Resposta da central excede o limite."};bytes.write(buffer,0,n)}}
                    val headers=c.headerFields.filterKeys{it!=null}.mapKeys{it.key.lowercase(java.util.Locale.ROOT)}
                    if(continuation.isActive)continuation.resume(Response(bytes.toByteArray(),headers))
                }catch(error:Throwable){if(continuation.isActive)continuation.resumeWithException(error)}
                finally{runCatching{c?.disconnect()}}
            }
            continuation.invokeOnCancellation {task.cancel(true);requests.remove(task);connection.get()?.let{c->runCatching{cleanup.execute{runCatching{c.disconnect()}}}}}
            try{requests.execute(task)}catch(error:java.util.concurrent.RejectedExecutionException){continuation.resumeWithException(java.io.IOException("Aguarde o encerramento do envio anterior."))}
        }
    }
    private fun checked(id:String)=id.also{require(it.matches(Regex("[0-9a-f-]{36}")))}
    private companion object {
        val requests=java.util.concurrent.ThreadPoolExecutor(2,2,30,java.util.concurrent.TimeUnit.SECONDS,java.util.concurrent.ArrayBlockingQueue<Runnable>(4),
            {task->Thread(task,"manual-subtitles").apply{isDaemon=true}})
        val cleanup=java.util.concurrent.ThreadPoolExecutor(2,2,30,java.util.concurrent.TimeUnit.SECONDS,java.util.concurrent.ArrayBlockingQueue<Runnable>(8),
            {task->Thread(task,"manual-subtitle-close").apply{isDaemon=true}})
    }
}
