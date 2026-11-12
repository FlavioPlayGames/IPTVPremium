package br.com.flaviotorres.iptvpremium.core.player

/** Language selection is local UI work; no download is triggered by choosing a language. */
internal fun subtitleLanguageKey(language:String?,release:String=""):String {
    val code=language.orEmpty().trim().lowercase(java.util.Locale.ROOT).replace('_','-').replace(' ','-')
    val brazil=code in setOf("pt-br","ptbr","br-pt","brpt","pob")||code.startsWith("brazilian")||code.startsWith("brazillian")||
        code.contains("portuguese-brazil")||code.contains("portugues-brasil")||code.contains("portuguese-braz")||code.contains("portuguese-brasil")
    if(brazil)return "pt-BR"
    if(code in setOf("pt-pt","ptpt","portuguese-portugal","portugues-portugal"))return "pt-PT"
    val portuguese=code in setOf("pt","pt-pt","por")||code.startsWith("portug")
    val releaseBrazil=Regex("(?i)(?:^|[^a-z0-9])(?:pt[-_. ]?br|br[-_. ]?pt|brazill?ian[-_. ]?portuguese)(?:[^a-z0-9]|$)").containsMatchIn(release)
    if(portuguese)return if(releaseBrazil)"pt-BR"else"pt"
    if(code in setOf("","und","unknown","mul"))return if(releaseBrazil)"pt-BR"else"und"
    return when(code.substringBefore('-')){"eng","english","en"->"en";"spa","spanish","es"->"es";else->code.substringBefore('-')}
}
internal fun subtitleLanguageLabel(key:String):String=when(key) {
    "manual"->"Legendas manuais";"pt-BR"->"Português Brasil";"pt-PT"->"Português Portugal";"pt"->"Português";"en"->"Inglês";"es"->"Espanhol";"und"->"Idioma não informado"
    else->java.util.Locale.forLanguageTag(key).getDisplayLanguage(java.util.Locale.forLanguageTag("pt-BR"))
        .takeIf{it.isNotBlank()}?.replaceFirstChar{it.titlecase(java.util.Locale.forLanguageTag("pt-BR"))}?:key
}
internal fun subtitleLanguagePriority(key:String):Int=when(key){"manual"->-1;"pt-BR"->0;"pt-PT"->1;"pt"->2;"en"->3;"es"->4;"und"->99;else->5}
internal fun subtitleProviderPriority(provider:String):Int=when(provider){"SubDL"->0;"SubSource"->1;"OpenSubtitles"->2;else->3}
internal fun subtitleLanguageOrder(keys:List<String>):List<String> = keys.distinct().sortedWith(compareBy<String>{subtitleLanguagePriority(it)}.thenBy{subtitleLanguageLabel(it)})

/** Regionless native tracks remain Portuguese; provider PT categories are separate from BR_PT. */
internal fun subtitleProviderLanguageKey(provider:String,language:String,release:String=""):String {
    val key=subtitleLanguageKey(language,release)
    return if(key=="pt"&&provider in setOf("SubDL","SubSource"))"pt-PT"else key
}
