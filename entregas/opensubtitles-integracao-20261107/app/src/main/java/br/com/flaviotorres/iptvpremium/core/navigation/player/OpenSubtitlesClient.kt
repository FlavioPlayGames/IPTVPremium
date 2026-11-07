package br.com.flaviotorres.iptvpremium.core.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

internal data class OpenSubtitleAppConfig(val apiKey:String,val userAgent:String) {
    val available get()=apiKey.length in 8..512 && apiKey.none{it.isWhitespace()||it.isISOControl()} &&
        userAgent.length in 3..200 && userAgent.none{it.isISOControl()}
    val fingerprint get()=openSubtitleDigest(apiKey)
}
internal interface OpenSubtitleAccounts {
    fun generation():Long
    fun read():OpenSubtitleSession?
    fun save(token:String,base:String,expiresAt:Long,appFingerprint:String,expectedGeneration:Long,accountScope:String):Boolean
    fun disconnect(expectedToken:String?=null):Boolean
}
internal data class OpenSubtitleResponse(val code:Int,val data:ByteArray,val retryAfterSeconds:Long?=null)
internal fun interface OpenSubtitleTransport {
    suspend fun request(url:String,method:String,headers:Map<String,String>,body:String?,limit:Int):OpenSubtitleResponse
}

/** Bounded HTTPS transport. API requests never follow redirects with credentials. */
internal class OpenSubtitleHttpTransport:OpenSubtitleTransport {
    override suspend fun request(url:String,method:String,headers:Map<String,String>,body:String?,limit:Int):OpenSubtitleResponse {
        coroutineContext.ensureActive()
        val uri=java.net.URI(url)
        require(uri.scheme=="https" && uri.userInfo==null && uri.port in setOf(-1,443))
        val con=URL(url).openConnection() as HttpURLConnection
        try {
            con.requestMethod=method;con.instanceFollowRedirects=false
            con.connectTimeout=7000;con.readTimeout=10000
            headers.forEach{(k,v)->con.setRequestProperty(k,v)}
            if(body!=null) {
                val data=body.toByteArray(Charsets.UTF_8);con.doOutput=true
                con.setRequestProperty("Content-Type","application/json")
                con.setFixedLengthStreamingMode(data.size);con.outputStream.use{it.write(data)}
            }
            val code=con.responseCode
            if(code in 300..399) {
                val location=con.getHeaderField("Location").orEmpty()
                // Only public file downloads can redirect; still no token or API key.
                return OpenSubtitleResponse(code,location.toByteArray(Charsets.UTF_8))
            }
            val retryHeader=con.getHeaderField("Retry-After")
            val retry=retryHeader?.toLongOrNull() ?: runCatching {
                val instant=java.time.ZonedDateTime.parse(retryHeader,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                (instant.toEpochMilli()-System.currentTimeMillis())/1000
            }.getOrNull()
            if(code !in 200..299)return OpenSubtitleResponse(code,byteArrayOf(),retry)
            if(con.contentLengthLong>limit)return OpenSubtitleResponse(413,byteArrayOf())
            val data=con.inputStream.use { input->
                val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(out.size()<=limit) {
                    coroutineContext.ensureActive()
                    val n=input.read(buffer,0,minOf(buffer.size,limit+1-out.size()))
                    if(n<0)break
                    if(n>0)out.write(buffer,0,n)
                }
                out.toByteArray()
            }
            return if(data.size>limit)OpenSubtitleResponse(413,byteArrayOf()) else OpenSubtitleResponse(code,data)
        }finally{con.disconnect()}
    }
}

/** No password persistence, no URL/token logging, no subtitle download during search. */
internal class OpenSubtitlesClient(
    private val config:()->OpenSubtitleAppConfig,
    private val accounts:OpenSubtitleAccounts,
    private val transport:OpenSubtitleTransport=OpenSubtitleHttpTransport(),
    private val now:()->Long=System::currentTimeMillis
) {
    companion object { private val limits=java.util.concurrent.ConcurrentHashMap<String,Long>() }
    val appAvailable get()=config().available
    fun scope():String? {
        val app=config();if(!app.available)return null
        return accounts.read()?.takeIf{it.appFingerprint==app.fingerprint && it.expiresAt>now()}?.scope
    }
    fun connected()=scope()!=null
    fun disconnect(){accounts.disconnect()}
    private fun session():OpenSubtitleSession {
        if(!appAvailable)throw OpenSubtitleException(OpenSubtitleProblem.APP_UNAVAILABLE)
        val s=accounts.read() ?: throw OpenSubtitleException(OpenSubtitleProblem.LOGIN_REQUIRED)
        if(s.appFingerprint!=config().fingerprint || s.expiresAt<=now()) {
            accounts.disconnect(s.token);throw OpenSubtitleException(OpenSubtitleProblem.SESSION_EXPIRED)
        }
        return s
    }
    fun checkScope(scope:String) {
        if(session().scope!=scope)throw OpenSubtitleException(OpenSubtitleProblem.SESSION_EXPIRED)
    }
    suspend fun login(username:String,password:String):Unit=withContext(Dispatchers.IO) {
        val app=config();if(!app.available)throw OpenSubtitleException(OpenSubtitleProblem.APP_UNAVAILABLE)
        if(username.trim().length !in 1..320 || password.length !in 1..1024 ||
            username.any{it.isISOControl()} || password.any{it.isISOControl()})
            throw OpenSubtitleException(OpenSubtitleProblem.INVALID_LOGIN)
        val generation=accounts.generation()
        val result=call("https://api.opensubtitles.com/api/v1/login","POST",null,
            JSONObject().put("username",username.trim()).put("password",password).toString(),true)
        val token=result.optString("token")
        if(token.length !in 8..16000 || token.any{it.isWhitespace()||it.isISOControl()})
            throw OpenSubtitleException(OpenSubtitleProblem.INVALID_RESPONSE)
        val rawBase=result.optString("base_url").ifBlank{"api.opensubtitles.com"}
        val base=openSubtitleApiBase(rawBase) ?: throw OpenSubtitleException(OpenSubtitleProblem.INVALID_RESPONSE)
        val expires=runCatching {
            val jwt=token.split('.');if(jwt.size!=3)return@runCatching null
            val decoded=String(java.util.Base64.getUrlDecoder().decode(jwt[1]),Charsets.UTF_8)
            JSONObject(decoded).optLong("exp",0).takeIf{it>now()/1000}?.times(1000)
        }.getOrNull() ?: (now()+23*60*60*1000L)
        val userId=result.optJSONObject("user")?.optString("user_id")
            ?.takeIf{it.matches(Regex("[1-9][0-9]{0,17}"))}
        val identity=userId?.let{"id:$it"}?:"name:${username.trim().lowercase(java.util.Locale.ROOT)}"
        val accountScope=openSubtitleDigest("${app.fingerprint}|$identity")
        coroutineContext.ensureActive()
        if(config().fingerprint!=app.fingerprint || !accounts.save(token,base,expires,app.fingerprint,generation,accountScope))
            throw OpenSubtitleException(OpenSubtitleProblem.STORAGE)
    }
    suspend fun search(query:OpenSubtitleQuery):Pair<String,List<OpenSubtitleHit>> = withContext(Dispatchers.IO) {
        val s=session()
        suspend fun fetch(language:String):Pair<List<OpenSubtitleHit>,Boolean> {
            val json=call("${s.apiBase}/subtitles?${query.parameters(language)}","GET",s,null)
            checkScope(s.scope)
            if(json.optJSONArray("data")==null)throw OpenSubtitleException(OpenSubtitleProblem.INVALID_RESPONSE)
            return decodeOpenSubtitleHits(json,query) to (json.optInt("total_pages",1)>1)
        }
        val (initial,morePages)=fetch("pt-br,en,es")
        val found=initial.toMutableList()
        // A popular English release must not crowd PT-BR or Spanish out of page one.
        for((code,label)in listOf("pt-br" to "pt-BR","en" to "en","es" to "es")) {
            if(morePages && found.none{it.language==label})found.addAll(fetch(code).first)
        }
        checkScope(s.scope)
        val bounded=found.distinctBy{it.fileId}.groupBy{it.language}
        s.scope to listOf("pt-BR","en","es").flatMap{bounded[it].orEmpty().take(30)}

    }
    /** Exactly one quota-consuming POST, only after explicit selection/cache miss. */
    suspend fun download(fileId:String,expectedScope:String):ByteArray=withContext(Dispatchers.IO) {
        require(fileId.matches(Regex("[1-9][0-9]{0,17}")))
        checkScope(expectedScope);val s=session()
        val response=call("${s.apiBase}/download","POST",s,
            JSONObject().put("file_id",fileId.toLong()).toString())
        checkScope(expectedScope)
        var link=response.optString("link")
        repeat(3) {
            if(!openSubtitleDownloadUrl(link))throw OpenSubtitleException(OpenSubtitleProblem.INVALID_RESPONSE)
            // Never send Api-Key, Authorization, cookies or a password to the download host.
            val r=safeRequest(link,"GET",mapOf("User-Agent" to config().userAgent),null,5_000_000)
            checkScope(expectedScope)
            if(r.code in 300..399) {
                link=runCatching{URL(URL(link),String(r.data,Charsets.UTF_8)).toString()}.getOrElse{
                    throw OpenSubtitleException(OpenSubtitleProblem.INVALID_RESPONSE)}
            }else {
                if(r.code !in 200..299)throw OpenSubtitleException(
                    if(r.code in setOf(406,429))OpenSubtitleProblem.LIMITED else OpenSubtitleProblem.UNAVAILABLE)
                return@withContext r.data
            }
        }
        throw OpenSubtitleException(OpenSubtitleProblem.INVALID_RESPONSE)
    }
    private suspend fun call(url:String,method:String,s:OpenSubtitleSession?,body:String?,login:Boolean=false):JSONObject {
        val app=config();if(!app.available)throw OpenSubtitleException(OpenSubtitleProblem.APP_UNAVAILABLE)
        if(now()<(limits[app.fingerprint]?:0))throw OpenSubtitleException(OpenSubtitleProblem.LIMITED)
        val headers=mutableMapOf("Api-Key" to app.apiKey,"User-Agent" to app.userAgent,"Accept" to "application/json")
        if(s!=null)headers["Authorization"]="Bearer ${s.token}"
        val r=safeRequest(url,method,headers,body,2_000_000)
        coroutineContext.ensureActive()
        if(r.code==401) {
            if(s!=null)accounts.disconnect(s.token)
            throw OpenSubtitleException(if(login)OpenSubtitleProblem.INVALID_LOGIN else OpenSubtitleProblem.SESSION_EXPIRED)
        }
        if(r.code==429) {
            limits[app.fingerprint]=now()+(r.retryAfterSeconds?:60).coerceIn(1,86400)*1000
            throw OpenSubtitleException(OpenSubtitleProblem.LIMITED)
        }
        if(r.code==406)throw OpenSubtitleException(OpenSubtitleProblem.LIMITED)
        if(r.code !in 200..299)throw OpenSubtitleException(
            if(login && r.code==403)OpenSubtitleProblem.APP_UNAVAILABLE else OpenSubtitleProblem.UNAVAILABLE)
        return try{JSONObject(String(r.data,Charsets.UTF_8))}catch(_:Exception){
            throw OpenSubtitleException(OpenSubtitleProblem.INVALID_RESPONSE)}
    }
    private suspend fun safeRequest(url:String,method:String,headers:Map<String,String>,body:String?,limit:Int):OpenSubtitleResponse=try {
        transport.request(url,method,headers,body,limit)
    }catch(cancelled:CancellationException){throw cancelled}
    catch(error:OpenSubtitleException){throw error}
    catch(_:Exception){throw OpenSubtitleException(OpenSubtitleProblem.UNAVAILABLE)}
}
