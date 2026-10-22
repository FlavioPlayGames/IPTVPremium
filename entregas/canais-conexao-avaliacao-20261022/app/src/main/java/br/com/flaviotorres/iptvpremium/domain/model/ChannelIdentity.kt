package br.com.flaviotorres.iptvpremium.domain.model

import java.text.Normalizer
import java.util.Locale

/** Quality/codec may differ; brands, numbers, regions, languages and seasons must not. */
object ChannelIdentity {
    private val quality=Regex("\\b(?:sd|hd|fhd|uhd|4k|8k|720p|1080p|1080i|2160p|h264|h265|hevc|avc|hdr|60fps|30fps)\\b")
    fun name(value:String):String {
        val folded=Normalizer.normalize(value,Normalizer.Form.NFKD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT)
            .replace("+"," plus ").replace(Regex("[^\\p{L}\\p{N}]+")," ")
        return quality.replace(folded," ").trim().replace(Regex("\\s+")," ")
    }
    fun same(origin:Channel,candidate:Channel):Boolean {
        val a=name(origin.name);val b=name(candidate.name)
        if(a.isNotBlank()&&a==b)return true
        return false // An EPG ID alone is not enough to switch to a different channel name.
    }
    fun searchToken(channel:Channel):String {
        // SQLite LIKE does not fold accents: use only an ASCII token present in the raw name.
        // With accent-only names, an empty token safely scans bounded pages and same() filters them.
        val raw=Regex("[\\p{L}\\p{N}]+").findAll(channel.name).map{it.value.lowercase(Locale.ROOT)}.toSet()
        return name(channel.name).split(' ').firstOrNull {
            it.length>=3 && it in raw && it.all{c->c in 'a'..'z'||c in '0'..'9'}
        }.orEmpty()
    }
}
