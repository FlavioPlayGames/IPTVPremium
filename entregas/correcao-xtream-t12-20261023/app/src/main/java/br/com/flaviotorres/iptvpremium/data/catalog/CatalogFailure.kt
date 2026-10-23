package br.com.flaviotorres.iptvpremium.data.catalog

import br.com.flaviotorres.iptvpremium.data.remote.PlaylistAccessDeniedException
import br.com.flaviotorres.iptvpremium.data.remote.PlaylistHttpStatusException
import br.com.flaviotorres.iptvpremium.data.repository.XtreamAuthenticationResponseException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException
import javax.net.ssl.SSLException

internal class NativeCatalogOpenException(message:String,cause:Exception):IOException(message,cause)

/** Only controlled labels reach the TV. Provider addresses, bodies and credentials stay private. */
internal object CatalogFailure {
    fun describe(error:Throwable,stage:String):String {
        val causes=generateSequence(error){it.cause}.take(10).toList()
        val reason=when {
            causes.any{it is UnknownHostException}->"DNS: não foi possível localizar o servidor."
            causes.any{it is SocketTimeoutException}->"TEMPO: o servidor não respondeu dentro do prazo."
            causes.any{it is SSLException}->"SSL: não foi possível validar a conexão segura."
            causes.any{it is PlaylistAccessDeniedException}->"ACESSO: conta recusada pelo provedor."
            causes.any{it is PlaylistHttpStatusException}->"HTTP ${(causes.first{it is PlaylistHttpStatusException}as PlaylistHttpStatusException).statusCode}: resposta de erro do servidor."
            causes.any{it.javaClass.simpleName.contains("SQLite",true)||it.javaClass.simpleName.contains("Database",true)}->"BANCO: não foi possível preparar o catálogo local."
            causes.any{it is XtreamAuthenticationResponseException}->"RESPOSTA: o servidor não retornou uma conta Xtream válida."
            causes.any{it is IOException}->"CONEXÃO: a transferência foi interrompida."
            causes.any{it is IllegalArgumentException||it is IllegalStateException}->"FORMATO: dados inválidos ou incompletos."
            else->"INTERNO: não foi possível concluir esta etapa."
        }
        val label=when(stage){"MOVIES"->"Filmes";"SERIES"->"Séries";"LIVE_TV"->"Canais";else->"Abertura"}
        return "$label · $reason"
    }
}
