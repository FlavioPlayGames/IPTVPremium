package br.com.flaviotorres.iptvpremium.domain.model
import br.com.flaviotorres.iptvpremium.ui.components.catalogTwoRowGeometry

fun main() {
    val saved=mutableMapOf<String,Set<String>>()
    fun toggle(playlist:String,kind:String,kids:Boolean,id:String) {
        val key=FavoriteScope.key(playlist,kind,kids)
        val ids=saved[key].orEmpty()
        saved[key]=if(id in ids)ids-id else ids+id
    }
    toggle("playlist_A","movies",false,"same_id")
    check(saved[FavoriteScope.key("playlist_A","movies",true)].isNullOrEmpty())
    toggle("playlist_A","movies",true,"same_id")
    toggle("playlist_A","movies",true,"same_id")
    check(saved[FavoriteScope.key("playlist_A","movies",false)]==setOf("same_id"))
    for(kind in listOf("movies","series","channels")) {
        toggle("playlist_A",kind,true,"child")
        check("child" !in saved[FavoriteScope.key("playlist_A",kind,false)].orEmpty())
        check("child" !in saved[FavoriteScope.key("playlist_B",kind,true)].orEmpty())
    }
    check(FavoriteScope.key("playlist_A","movies",false)=="movies_playlist_A")
    check(FavoriteScope.key("playlist_A","series",false)=="series_playlist_A")
    check(FavoriteScope.key("playlist_A","channels",false)=="channels_playlist_A")
    check(runCatching{FavoriteScope.key("A","unsupported",true)}.isFailure)
    println("PASS favourite isolation, same-ID toggle, all kinds/playlists and legacy keys")
    check(MovieInfoDuration.resolve(120,78)==4680)
    check(MovieInfoDuration.resolve(7200,1)==60) // real short movie; no guessed minimum
    check(MovieInfoDuration.resolve(7200,null)==7200)
    check(MovieInfoDuration.resolve(7200,0)==7200)
    check(MovieInfoDuration.resolve(7200,-1)==7200)
    check(MovieInfoDuration.resolve(7200,Int.MAX_VALUE)==7200)
    check(MovieInfoDuration.resolve(0,null)==null)
    check(MovieInfoDuration.resolve(-5,90)==5400)
    println("PASS confirmed TMDB units/priority, shorts, missing/invalid/overflow values")
    for(columns in listOf(4,5)) {
        val before=catalogTwoRowGeometry(671f,460f,columns,10f,32f,8f)
        val enlarged=catalogTwoRowGeometry(710f,468f,columns,6f,32f,4f)
        check(enlarged.posterWidth>before.posterWidth)
        check(enlarged.cardHeight*2+enlarged.rowSpacing<=468f-8f+.01f)
        check(enlarged.posterWidth>=0 && enlarged.rowSpacing>=0)
    }
    println("PASS larger poster geometry with two complete rows and metadata preserved")
}
