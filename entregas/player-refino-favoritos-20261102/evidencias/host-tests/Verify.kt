package br.com.flaviotorres.iptvpremium.core.player
import android.content.Context
import org.json.JSONObject
import br.com.flaviotorres.iptvpremium.data.repository.catalogSeasonPosterPaths
fun main(){
 var checks=0
 fun verify(ok:Boolean){check(ok);checks++}
 verify(!playerFavoriteHoldComplete(2999,true,true))
 verify(playerFavoriteHoldComplete(3000,true,true))
 verify(playerFavoriteHoldComplete(9000,true,true))
 verify(!playerFavoriteHoldComplete(3000,false,true))
 verify(!playerFavoriteHoldComplete(3000,true,false))
 val context=Context();val store=PlayerEpisodeFavorites(context,"one",false)
 val a=PlayerFavoriteEpisode("seriesA","episode1","Series A","Episode 1",1,1,null,"https://img.test/a.jpg")
 val b=a.copy(seriesId="seriesB")
 verify(store.all().isEmpty())
 store.save(listOf(a,a,b));verify(store.all()==listOf(a,b))
 verify(PlayerEpisodeFavorites(context,"one",false).all()==listOf(a,b))
 verify(PlayerEpisodeFavorites(context,"two",false).all().isEmpty())
 verify(PlayerEpisodeFavorites(context,"one",true).all().isEmpty())
 val kids=PlayerEpisodeFavorites(context,"one",true);kids.save(listOf(b));store.save(listOf(a))
 verify(kids.all()==listOf(b));verify(store.all()==listOf(a))
 store.save(emptyList());verify(store.all().isEmpty())
 context.getSharedPreferences("iptvpremium_player_episode_favorites_v1",0).edit().putString("normal:one","bad json").apply()
 verify(store.all().isEmpty())
 context.getSharedPreferences("iptvpremium_player_episode_favorites_v1",0).edit().putString("normal:one","[{\"series\":\"\",\"id\":\"e\"}]").apply()
 verify(store.all().isEmpty())
 verify(catalogSeasonPosterPaths(JSONObject()).isEmpty())
 val seasons=JSONObject("""{"seasons":[{"season_number":0,"poster_path":"/special.jpg"},{"season_number":1,"poster_path":"/a-12.png"},{"season_number":2,"poster_path":null},{"season_number":-1,"poster_path":"/invalid.jpg"},{"season_number":3,"poster_path":"https://evil.test/x.jpg"},{"season_number":4,"poster_path":"/../../x.jpg"}]}""")
 verify(catalogSeasonPosterPaths(seasons)==mapOf(0 to "https://image.tmdb.org/t/p/w500/special.jpg",1 to "https://image.tmdb.org/t/p/w500/a-12.png"))
 println("$checks checks passed: hold boundary/focus cancellation, favorite persistence/profile/playlist/identity, corrupt data and season artwork validation")
}
