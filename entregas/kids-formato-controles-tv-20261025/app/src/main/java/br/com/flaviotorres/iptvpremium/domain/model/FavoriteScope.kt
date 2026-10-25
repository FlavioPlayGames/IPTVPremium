package br.com.flaviotorres.iptvpremium.domain.model

/** Kids favourites never alias the normal/shared collection or another playlist. */
internal object FavoriteScope {
    fun key(playlist:String,kind:String,kids:Boolean):String {
        require(kind in setOf("movies","series","channels"))
        return (if(kids)"kids_" else "")+kind+"_"+playlist
    }
}
