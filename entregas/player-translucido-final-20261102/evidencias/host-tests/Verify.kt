package br.com.flaviotorres.iptvpremium.core.player
import android.content.Context
import org.json.JSONObject
import br.com.flaviotorres.iptvpremium.data.repository.catalogSeasonPosterPaths
fun main(){
 var checks=0
 fun verify(ok:Boolean){check(ok);checks++}
 verify(!playerFavoriteHoldComplete(1999,true,true))
 verify(playerFavoriteHoldComplete(2000,true,true))
 verify(playerFavoriteHoldComplete(9000,true,true))
 verify(!playerFavoriteHoldComplete(2000,false,true))
 verify(!playerFavoriteHoldComplete(2000,true,false))
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

 val press=PlayerCatalogPress()
 verify(press.down(0));verify(!press.down(600));verify(!press.down(1500));verify(!press.down(1900))
 verify(press.hold(1999,true)==PlayerPressResult.NONE)
 verify(press.hold(2000,true)==PlayerPressResult.FAVORITE)
 verify(press.hold(6000,true)==PlayerPressResult.NONE)
 verify(press.up(7000,true)==PlayerPressResult.NONE)
 press.down(0);verify(press.up(1999,true)==PlayerPressResult.CLICK)
 press.down(0);verify(press.up(2000,true)==PlayerPressResult.FAVORITE)
 press.down(0);press.cancel();verify(press.up(9000,true)==PlayerPressResult.NONE)
 press.down(0);verify(press.up(9000,true,true)==PlayerPressResult.NONE)
 press.down(0);verify(press.hold(2000,false)==PlayerPressResult.NONE);verify(press.up(4000,false)==PlayerPressResult.CLICK)
 verify(press.up(5000,true)==PlayerPressResult.NONE)
 store.saveIds("movies",setOf("a","b"));store.saveIds("series",setOf("a"))
 verify(store.ids("movies")==setOf("a","b"));verify(store.ids("series")==setOf("a"))
 verify(PlayerEpisodeFavorites(context,"two",false).ids("movies").isEmpty())
 verify(PlayerEpisodeFavorites(context,"one",true).ids("movies").isEmpty())
 store.saveIds("movies",emptySet());verify(store.ids("movies").isEmpty());verify(store.ids("series")==setOf("a"))
 val parsed=br.com.flaviotorres.iptvpremium.data.repository.parseSeriesSeasonArtwork(JSONObject("""{"poster_path":"/season.jpg","episodes":[{"episode_number":1,"name":"One","still_path":"/still.jpg"},{"episode_number":0,"still_path":"/invalid.jpg"},{"episode_number":2,"still_path":null}]}"""))
 verify(parsed.episodeStills==mapOf(1 to "https://image.tmdb.org/t/p/w780/still.jpg"))
 verify(parsed.posterUrl=="https://image.tmdb.org/t/p/w500/season.jpg")
 verify(parsed.episodeNames==mapOf(1 to "One"))
 kotlinx.coroutines.runBlocking {
  val attempted=mutableListOf<String>();var result:String?=null
  br.com.flaviotorres.iptvpremium.data.repository.loadEpisodeThumbnail(listOf("official","provider","provider"),{url->attempted+=url;if(url=="official")throw IllegalStateException("photo unavailable") else "photo"},{result=it})
  verify(attempted==listOf("official","provider"));verify(result=="photo")
  result=null
  br.com.flaviotorres.iptvpremium.data.repository.loadEpisodeThumbnail(listOf("missing"),{_:String->null as String?},{result=it})
  verify(result==null)
  var canceled=false
  try {br.com.flaviotorres.iptvpremium.data.repository.loadEpisodeThumbnail<String>(listOf("cancel"),{throw kotlinx.coroutines.CancellationException()},{error("stale image")})}
  catch(_:kotlinx.coroutines.CancellationException){canceled=true};verify(canceled)
 }
 val cover="https://img.test/series.jpg";val season="https://img.test/season.jpg"
 verify(playerSeasonPosterCandidates(null,null,null,cover)==listOf(cover))
 verify(playerSeasonPosterCandidates(season,null,null,cover)==listOf(season,cover))
 verify(playerSeasonPosterCandidates(season,season,cover,cover)==listOf(season,cover))
 verify(playerSeasonPosterCandidates("bad",null,null,cover)==listOf(cover))
 kotlinx.coroutines.runBlocking {
  val attempts=mutableListOf<String>();var resolved:String?=null
  br.com.flaviotorres.iptvpremium.data.repository.loadEpisodeThumbnail(playerSeasonPosterCandidates(season,null,null,cover),{url->attempts+=url;if(url==season)null else url},{resolved=it})
  verify(attempts==listOf(season,cover));verify(resolved==cover)
 }
 println("$checks checks passed: hold/repeats/cancellation, isolated player favorites, episode persistence, shared season parser and artwork validation")
}
