package br.com.flaviotorres.iptvpremium.data.epg

import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.core.player.PlaybackEpgProgram
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun xmltvHash(value:String):String=MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    .joinToString(""){"%02x".format(it.toInt() and 255)}
internal fun xmltvSourceSignature(source:PlaylistSource):String=xmltvHash(when(source){
    is PlaylistSource.M3u -> source.playlistUrl
    is PlaylistSource.XtreamCodes -> "${source.serverUrl}\n${source.username}\n${source.password}"
})
internal fun xmltvHeaderSources(header:String,base:String):List<String> {
    if(!header.trimStart('\uFEFF',' ','\t').startsWith("#EXTM3U",true))return emptyList()
    val attributes=Regex("(?:^|\\s)(?:url-tvg|x-tvg-url|tvg-url)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s]+))",RegexOption.IGNORE_CASE)
    return attributes.findAll(header.take(16_384)).flatMap {m->m.groupValues.drop(1).firstOrNull {it.isNotEmpty()}.orEmpty().split(Regex(",\\s*|\\s+(?=https?://)")).asSequence()}
        .mapNotNull {raw->runCatching { URI(base).resolve(raw.trim().replace("&amp;","&",true)).toString() }.getOrNull()}
        .filter {xmltvHttpUrl(it)}.distinct().take(4).toList()
}
internal fun xmltvHttpUrl(value:String):Boolean=runCatching {
    val u=URI(value);value.length<=4096 && u.scheme?.lowercase(Locale.ROOT) in listOf("http","https") && !u.host.isNullOrBlank()
}.getOrDefault(false)
internal fun xmltvXtreamSource(source:PlaylistSource.XtreamCodes):String? {
    var base=source.serverUrl.trim().trimEnd('/')
    if(!base.startsWith("http://",true) && !base.startsWith("https://",true))base="http://$base"
    if(!source.isEnabled || source.username.isBlank() || source.password.isBlank())return null
    fun encode(s:String)=URLEncoder.encode(s,"UTF-8")
    return "$base/xmltv.php?username=${encode(source.username.trim())}&password=${encode(source.password)}".takeIf(::xmltvHttpUrl)
}
internal fun xmltvLanguageRank(lang:String?):Int=when(val tag=lang.orEmpty().lowercase(Locale.ROOT).replace('_','-')) {
    "pt-br"->100;"pt"->95;"pt-pt"->90;"en","en-us","en-gb"->70;""->50
    else->if(tag.startsWith("pt-"))85 else if(tag.startsWith("en-"))65 else 10
}
internal fun xmltvChannelName(value:String):String=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
    .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+")," ").trim().replace(Regex("\\s+")," ")
internal fun xmltvTime(value:String):Long?=runCatching {
    val parts=value.trim().split(Regex("\\s+"),limit=2)
    val raw=parts[0]
    val time=when(raw.length){14->raw;12->raw+"00";else->return null}
    val offset=parts.getOrNull(1)?.takeIf {it.isNotBlank()}?.let {if(it=="Z")ZoneOffset.UTC else ZoneOffset.of(it)} ?: ZoneOffset.UTC
    LocalDateTime.parse(time,DateTimeFormatter.ofPattern("uuuuMMddHHmmss",Locale.ROOT).withResolverStyle(java.time.format.ResolverStyle.STRICT)).toInstant(offset).toEpochMilli()
}.getOrNull()
internal data class XmltvEntry(val program:PlaybackEpgProgram,val descriptionRank:Int)
internal fun mergeXmltvPrograms(api:List<PlaybackEpgProgram>,xml:List<XmltvEntry>,limit:Int,now:Long=System.currentTimeMillis()):List<PlaybackEpgProgram> {
    val primary=xml.groupBy {it.program.startEpochMs to it.program.endEpochMs}.values.map {items->items.maxBy {it.descriptionRank}}
    if(primary.isEmpty())return api.take(limit.coerceIn(1,200))
    val programs=primary.map {entry->
        val match=api.firstOrNull {it.startEpochMs==entry.program.startEpochMs && kotlin.math.abs(it.endEpochMs-entry.program.endEpochMs)<=60_000L}
        val description=entry.program.description?.takeIf {it.isNotBlank() && (entry.descriptionRank>=50 || match?.description.isNullOrBlank())}
            ?: match?.description
        entry.program.copy(description=description)
    }
    // XMLTV owns its covered intervals; never show conflicting duplicate events from another feed.
    val result=programs+api.filter {current->programs.none {it.startEpochMs<current.endEpochMs && it.endEpochMs>current.startEpochMs}}
    return result.distinctBy {it.startEpochMs to it.endEpochMs}
        .sortedBy {if(it.startEpochMs<=now && it.endEpochMs>now)0L else kotlin.math.abs(it.startEpochMs-now)}
        .take(limit.coerceIn(1,200)).sortedBy {it.startEpochMs}
}
