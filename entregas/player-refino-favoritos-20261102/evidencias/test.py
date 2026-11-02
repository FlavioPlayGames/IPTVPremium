from pathlib import Path
import shutil,subprocess,re
w=Path('/workspace/player-polish-favorites');d=w/'tests';d.mkdir(exist_ok=True);r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium');tc=Path('/workspace/storage-toolchain')
shutil.copy2('/workspace/player-catalog-minimal/tests/Android.kt',d/'Android.kt')
s=(r/'core/navigation/player/PlayerTheme.kt').read_text();(d/'Hold.kt').write_text('package br.com.flaviotorres.iptvpremium.core.player\n'+s[s.index('internal const val'):])
s=(r/'data/repository/CatalogSeasonArtwork.kt').read_text();(d/'Season.kt').write_text('package br.com.flaviotorres.iptvpremium.data.repository\nimport org.json.JSONObject\n'+s[s.index('internal fun catalogSeasonPosterPaths'):])
(d/'Verify.kt').write_text('''package br.com.flaviotorres.iptvpremium.core.player
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
 context.getSharedPreferences("iptvpremium_player_episode_favorites_v1",0).edit().putString("normal:one","[{\\"series\\":\\"\\",\\"id\\":\\"e\\"}]").apply()
 verify(store.all().isEmpty())
 verify(catalogSeasonPosterPaths(JSONObject()).isEmpty())
 val seasons=JSONObject("""{"seasons":[{"season_number":0,"poster_path":"/special.jpg"},{"season_number":1,"poster_path":"/a-12.png"},{"season_number":2,"poster_path":null},{"season_number":-1,"poster_path":"/invalid.jpg"},{"season_number":3,"poster_path":"https://evil.test/x.jpg"},{"season_number":4,"poster_path":"/../../x.jpg"}]}""")
 verify(catalogSeasonPosterPaths(seasons)==mapOf(0 to "https://image.tmdb.org/t/p/w500/special.jpg",1 to "https://image.tmdb.org/t/p/w500/a-12.png"))
 println("$checks checks passed: hold boundary/focus cancellation, favorite persistence/profile/playlist/identity, corrupt data and season artwork validation")
}
''')
cp=':'.join(str(p)for p in(tc/'gradle-9.6.0/lib').glob('*.jar'))+':'+str(tc/'tests/json.jar');j=str(tc/'jdk-25.0.4.1/bin/java');jar=w/'tests.jar'
files=list(d.glob('*.kt'))+[r/'core/navigation/player/PlayerEpisodeFavorites.kt']
x=subprocess.run([j,'-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*map(str,files)],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([j,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.core.player.VerifyKt'],capture_output=True,text=True);print(x.stdout+x.stderr);assert x.returncode==0
browser=(r/'core/navigation/player/PlaybackCatalogBrowser.kt').read_text();screen=(r/'core/navigation/player/PlaybackScreen.kt').read_text();nav=(r/'core/navigation/AppNavigation.kt').read_text()
code=re.sub(r'/\*.*?\*/|//[^\n]*','',browser,flags=re.S)
assert 'controller.' not in code and 'navController' not in code
assert 'FocusDirection.Up else FocusDirection.Down' in browser
assert 'if(favorite)Text("♥"' in browser and 'if(isPlaying)Text("▶"' in browser
assert 'onChangeEpisode(-1,playback.positionMs)' in screen and 'onChangeEpisode(1,playback.positionMs)' in screen
assert 'directCatalogStartMs=selection.startPositionMs' in nav.replace(' ','')
assert 'resolveSeriesArtworkIdentity' in (r/'data/repository/CatalogSeasonArtwork.kt').read_text()
assert 'LiveSlimMenu(' in (r/'ui/kids/KidsChannelsScreen.kt').read_text()
msg=x.stdout+'PASS integration contracts: CH directions, direct resume position, previous/next callbacks, scoped favorites, shared series identity, Kids menu reuse, browser independent of playback controller\n'
(w/'test-results.txt').write_text(msg);print(msg.splitlines()[-1])
