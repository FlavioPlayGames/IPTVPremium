package br.com.flaviotorres.iptvpremium.ui.kids

internal enum class KidsChannelArea { CATEGORIES, CHANNELS, VIDEO, HEADER }
internal enum class KidsChannelBack { CLOSE_DIALOG, CLOSE_PICTURE, CLOSE_MENU, PLAYING, CATEGORIES, EXIT }
internal fun kidsChannelBack(area:KidsChannelArea,full:Boolean,picture:Boolean,keyboard:Boolean,menu:Boolean,dialog:Boolean):KidsChannelBack=when{
    dialog->KidsChannelBack.CLOSE_DIALOG
    picture->KidsChannelBack.CLOSE_PICTURE
    menu->KidsChannelBack.CLOSE_MENU
    full||keyboard||area==KidsChannelArea.HEADER||area==KidsChannelArea.VIDEO->KidsChannelBack.PLAYING
    area==KidsChannelArea.CHANNELS->KidsChannelBack.CATEGORIES
    else->KidsChannelBack.EXIT
}
internal fun kidsHomeStep(allowed:List<Int>,current:Int,step:Int):Int?=allowed.getOrNull(allowed.indexOf(current)+step)
internal fun kidsChannelEntry(ids:List<String>,playing:String?,belongs:Boolean):String?=
    if(belongs&&playing in ids)playing else ids.firstOrNull()
internal fun kidsPageReady(previous:Any?,current:Any?,loading:Boolean,error:Boolean,paging:Boolean):Boolean=
    error || !paging || (!loading && previous!==current)
