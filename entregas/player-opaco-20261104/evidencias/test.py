from pathlib import Path
import subprocess,re
w=Path('/workspace/player-opaco');r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium');tc=Path('/workspace/storage-toolchain');d=w/'tests'
s=(r/'core/navigation/player/SubtitleAppearanceRendering.kt').read_text();a=s.index('internal class SubtitleAppearanceStore');b=s.index('\ninternal fun applySubtitleAppearance',a)
(d/'AppearanceStore.kt').write_text('package br.com.flaviotorres.iptvpremium.core.player\nimport android.content.Context\n'+s[a:b].split('\n@androidx.annotation.OptIn')[0])
s=(r/'core/navigation/player/PlaybackModels.kt').read_text();a=s.index('enum class PlaybackContentType');b=s.index('\n}',a)+2;(d/'MediaType.kt').write_text('package br.com.flaviotorres.iptvpremium.core.player\n'+s[a:b]+'\n'+s[s.index('enum class PlaybackStatus'):s.index('enum class PlaybackHdrFormat')])
cp=':'.join(str(p)for p in (tc/'gradle-9.6.0/lib').glob('*.jar'));j=str(tc/'jdk-25.0.4.1/bin/java');jar=w/'tests.jar'
files=list(d.glob('*.kt'))+[r/f for f in ['core/navigation/player/PlaybackCatalogPolicy.kt','core/navigation/player/CatalogSynopsisPresentation.kt','core/navigation/player/SubtitleAppearance.kt','core/navigation/player/SubtitleProviderUpdates.kt','core/device/PortalContentSyncPolicy.kt']]
x=subprocess.run([j,'-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),*map(str,files)],capture_output=True,text=True)
if x.returncode:print(x.stderr);raise SystemExit(x.returncode)
x=subprocess.run([j,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.core.player.VerifyKt'],capture_output=True,text=True)
print(x.stdout+x.stderr);(w/'test-results.txt').write_text(x.stdout+x.stderr)
if x.returncode:raise SystemExit(x.returncode)
# Integration contracts guard the actual callback wiring, not a duplicate implementation.
s=(r/'core/navigation/player/PlaybackCatalogBrowser.kt').read_text()
code=re.sub(r'/\*.*?\*/|//[^\n]*','',s,flags=re.S)
assert 'controller.' not in code and 'navController' not in code
assert 'collectAsLazyPagingItems' in s and 'aspectRatio(if(landscape)16f/9f else 2f/3f)' in s
assert 'onSelect(PlayerCatalogSelection' in s
s=(r/'core/navigation/AppNavigation.kt').read_text();a=s.index('onCatalogSelected = {');b=s.index('// Filmes usam a arte',a);callback=s[a:b]
assert 'AppDestination.Playback.createRoute' in callback and 'MovieDetails' not in callback and 'SeriesDetails.createRoute' not in callback
assert 'savePlaybackProgress(position,measuredDurationMs?:media.durationMs)' in callback
saver=s[s.index('fun savePlaybackProgress'):s.index('fun changeEpisode')]
assert 'watchHistoryStore.save' in saver and 'seriesResumeStore.savePosition' in saver
s=(r/'core/navigation/player/PlaybackScreen.kt').read_text()
assert 'LaunchedEffect(controlsVisible, panel, interactionTick, seekTargetMs,catalogVisible)' in s
assert 'if (controlsVisible && !catalogVisible && panel == PlayerPanel.NONE' in s
s=(r/'MainActivity.kt').read_text();a=s.index('lifecycle.repeatOnLifecycle');b=s.index('init_push',a)
assert 'portalContentRetryDelay' in s[a:b] and 'includeSubtitleCommands=true' in s[a:b]
with (w/'test-results.txt').open('a')as f:f.write('PASS integration contracts: independent Paging, no player control from browser, direct playback routes, history, catalog-safe hide timer and lifecycle-scoped sync\n')
print('PASS integration contracts')
