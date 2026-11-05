package br.com.flaviotorres.iptvpremium.data.history

/** Legacy unmarked history remains normal. Never infer a viewer from a film's genre. */
object HistoryProfile {
    fun preferences(base:String,kids:Boolean):String=if(kids)"${base}_kids"else base
    fun favorite(playlist:String,kind:String,kids:Boolean):String="${if(kids)"kids_"else""}${kind}_$playlist"
}
