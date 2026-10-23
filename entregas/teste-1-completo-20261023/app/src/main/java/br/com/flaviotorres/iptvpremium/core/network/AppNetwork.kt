package br.com.flaviotorres.iptvpremium.core.network

import android.content.Context
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import org.xbill.DNS.*
import org.xbill.DNS.Record
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class DnsConfig(val mode:String="system",val address:String="",val bootstrap:String="")

/** One HTTP pool for catalogue, images and Media3; never logs URLs or bodies. */
internal object AppNetwork {
    @Volatile private var app:Context?=null
    private val system=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS)
        .retryOnConnectionFailure(true).build()
    @Volatile private var selected:Pair<DnsConfig,OkHttpClient>?=null
    fun initialize(context:Context){app=context.applicationContext}
    fun config():DnsConfig {
        val prefs=app?.getSharedPreferences("network_dns_v1",Context.MODE_PRIVATE)?:return DnsConfig()
        return DnsConfig(prefs.getString("mode","system").orEmpty(),prefs.getString("address","").orEmpty(),prefs.getString("bootstrap","").orEmpty())
    }
    fun save(value:DnsConfig) {
        validate(value)
        checkNotNull(app).getSharedPreferences("network_dns_v1",Context.MODE_PRIVATE).edit()
            .putString("mode",value.mode).putString("address",value.address.trim()).putString("bootstrap",value.bootstrap.trim()).apply()
        selected?.second?.connectionPool?.evictAll()
        selected=null
    }
    fun validate(value:DnsConfig) {
        require(value.mode in setOf("system","google","cloudflare","quad9","custom","doh")){"Opção DNS inválida."}
        if(value.mode=="custom")literal(value.address.trim())
        if(value.mode=="doh") {
            val url=value.address.trim().toHttpUrl()
            require(url.isHttps && url.username.isEmpty()&&url.password.isEmpty()){ "Use uma URL HTTPS sem usuário ou senha." }
            require(value.bootstrap.isNotBlank()){ "Informe o IP de inicialização do servidor DoH." }
            value.bootstrap.split(',').forEach{literal(it.trim())}
        }
    }
    fun client():OkHttpClient {
        val cfg=config();selected?.takeIf{it.first==cfg}?.let{return it.second}
        return synchronized(this) {
            selected?.takeIf{it.first==cfg}?.second?:system.newBuilder().dns(resolver(cfg)).build().also{selected=cfg to it}
        }
    }
    private fun resolver(cfg:DnsConfig):Dns=when(cfg.mode) {
        "google"->ServerDns("8.8.8.8")
        "cloudflare"->ServerDns("1.1.1.1")
        "quad9"->ServerDns("9.9.9.9")
        "custom"->ServerDns(literal(cfg.address.trim()).hostAddress!!)
        "doh"->DnsOverHttps.Builder().client(system).url(cfg.address.trim().toHttpUrl())
            .bootstrapDnsHosts(*cfg.bootstrap.split(',').map{literal(it.trim())}.toTypedArray()).build()
        else->Dns.SYSTEM
    }
    internal fun literal(value:String):InetAddress {
        require(value.isNotEmpty()&&value.all{it.isDigit()||it in ".:abcdefABCDEF"}&&(value.contains('.')||value.contains(':'))){"Informe um endereço IP, sem nome de domínio."}
        if(!value.contains(':'))require(value.split('.').size==4&&value.split('.').all{it.toIntOrNull() in 0..255}){"IP inválido."}
        return InetAddress.getByName(value)
    }
    private class ServerDns(private val server:String):Dns {
        override fun lookup(hostname:String):List<InetAddress> {
            if(hostname.contains(':')||hostname.matches(Regex("[0-9.]+")))return listOf(literal(hostname))
            try {
                val resolver=SimpleResolver(server).apply{setTimeout(Duration.ofSeconds(3))}
                val name=Name.fromString(hostname.trimEnd('.')+".")
                val addresses=ArrayList<InetAddress>()
                for(type in listOf(Type.A,Type.AAAA)) {
                    val response=resolver.send(Message.newQuery(Record.newRecord(name,type,DClass.IN)))
                    for(record in response.getSection(Section.ANSWER))when(record) {
                        is ARecord->addresses+=record.address
                        is AAAARecord->addresses+=record.address
                    }
                }
                if(addresses.isEmpty())throw UnknownHostException("DNS selecionado não encontrou o servidor.")
                return addresses.distinct()
            }catch(error:Exception){throw UnknownHostException("Falha no DNS selecionado.").apply{initCause(error)}}
        }
    }
}

internal suspend fun Call.awaitResponse():Response=suspendCancellableCoroutine { continuation->
    continuation.invokeOnCancellation{cancel()}
    enqueue(object:Callback {
        override fun onFailure(call:Call,e:java.io.IOException){if(continuation.isActive)continuation.resumeWithException(e)}
        override fun onResponse(call:Call,response:Response){
            if(continuation.isActive)continuation.resume(response, onCancellation={_,value,_->value.close()})else response.close()
        }
    })
}
