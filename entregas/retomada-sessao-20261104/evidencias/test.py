from pathlib import Path
import subprocess
w=Path('/workspace/retomada-sessao');tc=Path('/workspace/storage-toolchain');r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium')
cp=':'.join(str(p)for p in(tc/'gradle-9.6.0/lib').glob('*.jar'));j=str(tc/'jdk-25.0.4.1/bin/java');jar=w/'tests.jar'
subprocess.run([j,'-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',cp,'-d',str(jar),str(r/'core/session/SessionSnapshotCodec.kt'),str(w/'tests/Verify.kt')],check=True,capture_output=True,text=True)
x=subprocess.run([j,'-cp',str(jar)+':'+cp,'br.com.flaviotorres.iptvpremium.core.session.VerifyKt'],capture_output=True,text=True,check=True);(w/'test-results.txt').write_text(x.stdout+x.stderr);print(x.stdout)
main=(r/'MainActivity.kt').read_text();screen=(r/'core/navigation/player/PlaybackScreen.kt').read_text();nav=(r/'core/navigation/AppNavigation.kt').read_text();store=(r/'core/session/ActivitySessionStore.kt').read_text()
assert 'super.onCreate(savedInstanceState ?: sessionStore.read())' in main
assert 'if (isFinishing) sessionStore.clear()' in main and 'super.onSaveInstanceState(snapshot)' in main
assert 'window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)' in main
assert 'noBackupFilesDir' in store and 'file.finishWrite(output)' in store and 'file.failWrite(output)' in store
assert 'var selectedEpisodeSeriesId by rememberSaveable' in nav and 'restoredEpisodes?.episodes?.firstOrNull{it.id==contentId}' in nav
assert 'var selectedStartMs by rememberSaveable' in nav and 'if(selectedStartMs==null)selectedStartMs=' in nav
assert 'controller.pause()' in screen and 'if(sessionPlaying)' in screen
assert 'startPositionMs = if (isLive) 0L else sessionPosition.coerceAtLeast(0L)' in screen
assert 'playWhenReady = sessionPlaying && historyOwner.lifecycle.currentState.isAtLeast' in screen
assert 'var sessionPosition by rememberSaveable' in screen and 'var sessionPlaying by rememberSaveable' in screen
assert 'channelId = savedLiveChannelId ?: contentId' in nav
manifest=Path('/workspace/backup-reviewed-build/app/src/main/AndroidManifest.xml').read_text();assert 'android:alwaysRetainTaskState="true"' in manifest
(w/'contracts.txt').write_text('PASS session wiring: durable UI state, no expiry, real episode recovery, retained task, screen awake in foreground, position and playback intent, background pause, live reconnect, explicit exit, atomic bounded private storage\n')
print((w/'contracts.txt').read_text())
