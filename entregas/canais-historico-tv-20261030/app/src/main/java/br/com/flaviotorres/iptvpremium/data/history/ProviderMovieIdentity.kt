package br.com.flaviotorres.iptvpremium.data.history

import java.net.URI
import java.security.MessageDigest
import java.util.Locale

internal data class ProviderMovieIdentity(val key:String,val id:String)

/** Only the exact provider asset is linked. Credentials/query/title are not persisted. */
internal fun providerMovieIdentity(url:String?):ProviderMovieIdentity? = providerAssetIdentity(url,"movie")
internal fun providerEpisodeIdentity(url:String?):ProviderMovieIdentity? = providerAssetIdentity(url,"series")
private fun providerAssetIdentity(url:String?,type:String):ProviderMovieIdentity? = runCatching {
    val uri=URI(url ?: return null)
    val scheme=uri.scheme?.lowercase(Locale.ROOT)?.takeIf {it=="http" || it=="https"} ?: return null
    val host=uri.host?.lowercase(Locale.ROOT) ?: return null
    val parts=uri.rawPath.orEmpty().split('/').filter {it.isNotEmpty()}
    if(parts.size<4 || parts[parts.size-4]!=type) return null
    val file=parts.last()
    val id=file.substringBeforeLast('.',"").takeIf {it.matches(Regex("[0-9]{1,20}"))} ?: return null
    val namespace=parts.dropLast(3).joinToString("/")
    val port=if(uri.port==-1) {if(scheme=="https")443 else 80} else uri.port
    val value="$scheme|$host|$port|$namespace|$id"
    val key=MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") {"%02x".format(it.toInt() and 255)}
    ProviderMovieIdentity(key,id)
}.getOrNull()
