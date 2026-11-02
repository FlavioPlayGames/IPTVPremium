from pathlib import Path
import shutil,subprocess,re
w=Path('/workspace/player-catalogo-ajuste');d=w/'tests';d.mkdir(exist_ok=True);r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium');tc=Path('/workspace/storage-toolchain')
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
 val pillar=automaticStretchScale(1920f,1080f,1440f,1080f)
 verify(kotlin.math.abs(pillar.first-4f/3f)<.0001f);verify(pillar.second==1f)
 verify(kotlin.math.abs(1440f*pillar.first-1920f)<.001f)
 val letter=automaticStretchScale(1920f,1080f,1920f,800f)
 verify(letter.first==1f);verify(kotlin.math.abs(800f*letter.second-1080f)<.001f)
 verify(automaticStretchScale(1920f,1080f,1920f,1080f)==(1f to 1f))
 verify(automaticStretchScale(0f,0f,0f,0f)==(1f to 1f))
 verify(automaticStretchScale(Float.NaN,1080f,1440f,1080f)==(1f to 1f))
 println("$checks checks passed: hold/repeats/cancellation, isolated player favorites, episode persistence, shared season parser and artwork validation")
}
'''
s=s.replace('2999','1999').replace('3000','2000').replace('2900','1900')
(d/'Verify.kt').write_text(s)
cp=':'.join(str(p)for p in(tc/'gradle-9.6.0/lib').glob('*.jar'))+':'+str(tc/'tests/json.jar');j=str(tc/'jdk-25.0.4.1/bin/java');jar=w/'tests.jar'
files=list(d.glob('*.kt'))+[r/'core/navigation/player/PlayerEpisodeFavorites.kt',r/'core/navigation/player/PlayerCatalogPress.kt',r/'data/repository/EpisodeThumbnailLoader.kt',r/'core/navigation/player/PlayerSeasonPosterPolicy.kt',r/'core/navigation/player/AutomaticStretchPolicy.kt']
x=subprocess.run([j,'-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*map(str,files)],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([j,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.core.player.VerifyKt'],capture_output=True,text=True);print(x.stdout+x.stderr);assert x.returncode==0
browser=(r/'core/navigation/player/PlaybackCatalogBrowser.kt').read_text();screen=(r/'core/navigation/player/PlaybackScreen.kt').read_text();nav=(r/'core/navigation/AppNavigation.kt').read_text();controls=(r/'core/navigation/player/NeutralPlayerControls.kt').read_text()
code=re.sub(r'/\*.*?\*/|//[^\n]*','',browser,flags=re.S)
assert 'controller.' not in code and 'navController' not in code
assert 'FocusDirection.Up else FocusDirection.Down' in browser
assert 'if(favorite)br.com.flaviotorres.iptvpremium.ui.components.ReferenceFavoriteGlyph' in browser
assert 'Text("♥"' not in browser and 'Text("▶"' not in browser
assert 'Reproduzindo agora' in browser
assert 'val height=with(density)' in browser and 'loadEpisodeThumbnail(images' in browser and 'bitmap?:fallback' in browser
assert 'onChangeEpisode(-1,playback.positionMs)' in screen and 'onChangeEpisode(1,playback.positionMs)' in screen
assert 'Alignment.BottomCenter).width(26.dp)' not in controls
assert 'seriesImages.stills[row.episodeNumber]' in browser
assert 'loadSeriesSeasonArtwork' in (r/'ui/series/SeriesDetailsScreen.kt').read_text()
assert 'loadSeriesSeasonArtwork' in (r/'core/navigation/player/PlayerSeriesArtwork.kt').read_text()
assert 'favoriteMovies=catalogFavoriteMovieIds' not in nav
assert '.border(' not in controls and '.background(' not in controls
format=(r/'core/navigation/player/ScreenFormatPanel.kt').read_text()
assert 'if(manual)Modifier.align(Alignment.Center).width(156.dp)' in format
assert '.then(if(manual)Modifier else Modifier.background' in format
assert 'NeutralPlayerGlyph(PlayerGlyph.PLAY' in format and '.rotate(turn)' in format
assert 'PlaybackAspectMode.STRETCH -> stretch.first' in screen and 'PlaybackAspectMode.STRETCH -> stretch.second' in screen
assert '.then(if(focused)Modifier.border(1.5.dp,accent,shape)' in browser
assert '.border(' not in format and 'drawLine(' not in format
assert 'Row(modifier.fillMaxWidth()' in browser
assert 'images=playerSeasonPosterCandidates' in browser
assert 'if(!neutralControls)' in screen
header=(r/'ui/components/ReferenceDetailHeader.kt').read_text()
assert 'ReferenceFavoriteGlyph(isFavorite,focused,' in header
assert 'ReferenceDetailsPalette.crimson' in header
assert 'filledColor=' not in browser
assert 'Segure OK por 2s' in browser
resume=(r/'core/navigation/player/PlaybackResumeCinema.kt').read_text().split('private fun CinemaResumeButton')[1]
assert '.shadow(' not in resume and '.background(' not in resume and '.border(' not in resume
assert 'indication=null' in resume
msg=x.stdout+'PASS integration contracts: clean artwork, shared heart outside images, no underline, CH/episode callbacks, independent playback, traditional season loader and landscape/backdrop fallback\n'
(w/'test-results.txt').write_text(msg);print(msg.splitlines()[-1])
