from pathlib import Path
import shutil,subprocess,re
w=Path('/workspace/player-visual-aprovado');d=w/'tests';d.mkdir(exist_ok=True);r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium');tc=Path('/workspace/storage-toolchain')
for name in ['Android.kt','Hold.kt','Season.kt']:shutil.copy2(Path('/workspace/player-polish-favorites/tests')/name,d/name)
s=(r/'core/navigation/player/PlayerTheme.kt').read_text();(d/'Hold.kt').write_text('package br.com.flaviotorres.iptvpremium.core.player\n'+s[s.index('internal const val'):])
s=(r/'data/repository/SeriesSeasonArtwork.kt').read_text();(d/'SharedSeason.kt').write_text('package br.com.flaviotorres.iptvpremium.data.repository\nimport org.json.JSONObject\n'+s[s.index('internal data class'):s.index('internal suspend fun loadSeriesSeasonArtwork')])
s=Path('/workspace/player-polish-favorites/tests/Verify.kt').read_text();idx=s.index(' println("$checks checks passed:')
s=s[:idx]+'''
 val press=PlayerCatalogPress()
 verify(press.down(0));verify(!press.down(600));verify(!press.down(1500));verify(!press.down(2900))
 verify(press.hold(2999,true)==PlayerPressResult.NONE)
 verify(press.hold(3000,true)==PlayerPressResult.FAVORITE)
 verify(press.hold(6000,true)==PlayerPressResult.NONE)
 verify(press.up(7000,true)==PlayerPressResult.NONE)
 press.down(0);verify(press.up(2999,true)==PlayerPressResult.CLICK)
 press.down(0);verify(press.up(3000,true)==PlayerPressResult.FAVORITE)
 press.down(0);press.cancel();verify(press.up(9000,true)==PlayerPressResult.NONE)
 press.down(0);verify(press.up(9000,true,true)==PlayerPressResult.NONE)
 press.down(0);verify(press.hold(3000,false)==PlayerPressResult.NONE);verify(press.up(4000,false)==PlayerPressResult.CLICK)
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
 println("$checks checks passed: hold/repeats/cancellation, isolated player favorites, episode persistence, shared season parser and artwork validation")
}
'''
(d/'Verify.kt').write_text(s)
cp=':'.join(str(p)for p in(tc/'gradle-9.6.0/lib').glob('*.jar'))+':'+str(tc/'tests/json.jar');j=str(tc/'jdk-25.0.4.1/bin/java');jar=w/'tests.jar'
files=list(d.glob('*.kt'))+[r/'core/navigation/player/PlayerEpisodeFavorites.kt',r/'core/navigation/player/PlayerCatalogPress.kt',r/'data/repository/EpisodeThumbnailLoader.kt']
x=subprocess.run([j,'-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*map(str,files)],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([j,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.core.player.VerifyKt'],capture_output=True,text=True);print(x.stdout+x.stderr);assert x.returncode==0
browser=(r/'core/navigation/player/PlaybackCatalogBrowser.kt').read_text();screen=(r/'core/navigation/player/PlaybackScreen.kt').read_text();nav=(r/'core/navigation/AppNavigation.kt').read_text();controls=(r/'core/navigation/player/NeutralPlayerControls.kt').read_text()
code=re.sub(r'/\*.*?\*/|//[^\n]*','',browser,flags=re.S)
assert 'controller.' not in code and 'navController' not in code
assert 'FocusDirection.Up else FocusDirection.Down' in browser
assert 'if(favorite)br.com.flaviotorres.iptvpremium.ui.series.FavoriteHeart' in browser
assert 'Text("♥"' not in browser and 'Text("▶"' not in browser
assert 'Reproduzindo agora' in browser
assert 'val height=with(density)' in browser and 'loadEpisodeThumbnail(images' in browser and 'bitmap?:fallback' in browser
assert 'onChangeEpisode(-1,playback.positionMs)' in screen and 'onChangeEpisode(1,playback.positionMs)' in screen
assert 'Alignment.BottomCenter).width(26.dp)' not in controls
assert 'seriesImages.stills[row.episodeNumber]' in browser
assert 'loadSeriesSeasonArtwork' in (r/'ui/series/SeriesDetailsScreen.kt').read_text()
assert 'loadSeriesSeasonArtwork' in (r/'core/navigation/player/PlayerSeriesArtwork.kt').read_text()
assert 'favoriteMovies=catalogFavoriteMovieIds' not in nav
msg=x.stdout+'PASS integration contracts: clean artwork, shared heart outside images, no underline, CH/episode callbacks, independent playback, traditional season loader and landscape/backdrop fallback\n'
(w/'test-results.txt').write_text(msg);print(msg.splitlines()[-1])
