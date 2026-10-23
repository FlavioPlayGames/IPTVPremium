package br.com.flaviotorres.iptvpremium.data.cache

import java.text.Normalizer
import java.util.Locale

internal enum class GenericPosterKind { CINEMA, FRAMES, ANIME, ANIMATION }
internal object GenericPosterPolicy {
    private fun folded(text:String)=Normalizer.normalize(text,Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT)
    private val anime=Regex("\\b(?:animes?|mangas?|donghuas?|crunchyroll|funimation|hidive)\\b")
    private val animation=Regex("\\b(?:animacao|animacoes|animad[oa]s?|desenhos?|cartoons?|animations?)\\b")
    fun select(mediaKind:String,id:String,category:String):GenericPosterKind {
        val text=folded(category)
        if(anime.containsMatchIn(text))return GenericPosterKind.ANIME
        if(animation.containsMatchIn(text))return GenericPosterKind.ANIMATION
        // Pseudorandom but stable across recomposition, navigation and process restarts.
        return if(Math.floorMod((mediaKind+":"+id).hashCode(),2)==0)GenericPosterKind.CINEMA else GenericPosterKind.FRAMES
    }
    fun hasSource(url:String?):Boolean=url?.trim()?.let {
        it.startsWith("http://",true)||it.startsWith("https://",true)
    }==true
}
