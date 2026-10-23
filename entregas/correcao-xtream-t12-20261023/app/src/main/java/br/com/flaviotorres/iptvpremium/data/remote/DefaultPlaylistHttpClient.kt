package br.com.flaviotorres.iptvpremium.data.remote

import br.com.flaviotorres.iptvpremium.core.network.*
import okhttp3.*
import kotlinx.coroutines.*
import java.io.Reader

class PlaylistHttpStatusException(val statusCode:Int):java.io.IOException("Erro HTTP $statusCode ao acessar o servidor.")

class PlaylistAccessDeniedException(message:String):java.io.IOException(message)

/** OkHttp 5 streaming, pooled connections, transparent gzip and real Call cancellation. */
class DefaultPlaylistHttpClient(private val clients:()->OkHttpClient={AppNetwork.client()},private val maxTextChars:Int=32*1024*1024):PlaylistHttpClient {
    override suspend fun <T> withReader(url:String,consume:suspend(Reader)->T):T=withContext(Dispatchers.IO) {
        coroutineScope {
            val call=clients().newCall(Request.Builder().url(url).header("User-Agent","IPTVPremium/1.12")
                .header("Accept","*/*").build())
            val cancel=launch(start=CoroutineStart.UNDISPATCHED){try{awaitCancellation()}finally{call.cancel()}}
            try {
                call.awaitResponse().use { response->
                    if(response.code==401||response.code==403)throw PlaylistAccessDeniedException("Acesso negado pelo provedor. Confira suas credenciais.")
                    if(!response.isSuccessful)throw PlaylistHttpStatusException(response.code)
                    response.body.charStream().buffered().use{consume(it)}
                }
            }catch(error:Exception){currentCoroutineContext().ensureActive();throw error}
            finally{cancel.cancel();call.cancel()}
        }
    }
    override suspend fun get(url:String):String=withReader(url){reader->
        val result=StringBuilder();val buffer=CharArray(8192)
        while(true){currentCoroutineContext().ensureActive();val count=reader.read(buffer);if(count<0)break
            check(result.length+count<=maxTextChars){"Resposta muito grande; use o leitor em fluxo."};result.append(buffer,0,count)}
        result.toString()
    }
    override suspend fun readLines(url:String,onLine:suspend(String)->Unit) {withReader(url){input->
        val reader=input.buffered()
        while(true){currentCoroutineContext().ensureActive();val line=reader.readLine()?:break;onLine(line)}
    }}
}
