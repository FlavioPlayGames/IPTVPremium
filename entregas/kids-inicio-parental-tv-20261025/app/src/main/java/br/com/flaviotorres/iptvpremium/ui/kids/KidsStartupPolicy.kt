package br.com.flaviotorres.iptvpremium.ui.kids

internal object KidsStartupPolicy {
    fun enabled(parentalEnabled:Boolean,startInKids:Boolean)=parentalEnabled && startInKids
    fun protectSettings(fromKids:Boolean,parentalEnabled:Boolean,startInKids:Boolean,kidsOnly:Boolean,unlocked:Boolean):Boolean =
        fromKids || parentalEnabled && (startInKids || kidsOnly) && !unlocked
    fun redirect(parentalEnabled:Boolean,startInKids:Boolean,unlocked:Boolean,route:String?,
        markedKids:Boolean,inKidsSession:Boolean):Boolean = enabled(parentalEnabled,startInKids) && !unlocked &&
        route!=null && route!="kids" && !(route=="settings" || route.startsWith("settings/")) && !markedKids && !inKidsSession
}
