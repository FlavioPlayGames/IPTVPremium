from pathlib import Path
import json
w=Path('/workspace/legendas-duas-etapas');r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium');wizard=(r/'core/navigation/SubtitleSetupWizard.kt').read_text();sync=(r/'core/device/DevicePortalSync.kt').read_text();qr=(r/'core/device/DevicePortalQr.kt').read_text();client=(r/'core/navigation/player/OpenSubtitlesClient.kt').read_text()
assert 'PASSO $step DE 2' in wizard and 'step=2' in wizard
assert 'https://subdl.com' in wizard and 'https://www.opensubtitles.com' in wizard
assert 'DevicePortalQr(destination="subtitles"' in wizard and 'wizard=true' in wizard
assert 'Se o seu provedor não oferecer legendas, use estas opções gratuitas. Recomendamos cadastrar-se nas duas, pois cada serviço tem limites de uso.' in wizard
assert 'MarbleSettingsButton' in wizard and 'CatalogStyle.accent' in wizard and 'SettingsBackdrop' in wizard and '0xFF00' not in wizard
assert 'repeatOnLifecycle(Lifecycle.State.STARTED)' in wizard and '900_000L' in wizard and 'delay(8_000)' in wizard
assert 'automatic && !includeSubtitleCommands && command.getString' in sync and '"subtitle_session" ->' in sync
assert 'open.acceptPortalSession(data)' in sync and '.put("subtitleProtocol",2)' in sync
assert 'decodeOpenPortalSession' in client and '.save(session.token' in client
base=Path('/workspace/outputs/opensubtitles-integracao/app/src/main/java/br/com/flaviotorres/iptvpremium')
for name in ['core/navigation/player/ExternalSubtitleRepositor.kt','core/navigation/player/PlaybackScreen.kt','core/navigation/player/OpenSubtitlesAccountStore.kt']:
 assert (r/name).read_text()==(base/name).read_text(),name
for name in ['Media3PlaybackAdapter.kt','LibVlcPlaybackAdapter.kt']:
 assert (r/'core/navigation/player'/name).read_text()==(Path('/workspace/vod-legendas-vlc-review/upload/java/br/com/flaviotorres/iptvpremium/core/navigation/player')/name).read_text()
site=w/'site';worker=(site/'worker-atualizacao-abi.js').read_text();browser=(site/'public/app.js').read_text();module=(site/'public/subtitles.js').read_text()
assert 'connectOpenAccount(env,b)' in worker and 'Math.min(900000,payload.expiresAt-Date.now())' in worker
assert 'if(b.data.provider==="opensubtitles" && b.data.key!==null)' in worker
assert 'openPortalReady(env,account.snapshot)' in worker and 'openSubtitlesAvailable' in worker
assert 'password' not in worker[worker.index('const queued=await queue(env,id,"subtitle_session"'):worker.index('if (p === "/api/rename"')]
for f in ['index.html','admin/index.html']:
 s=(site/'public'/f).read_text();assert 'SubSource' not in s and 'subtitle-steps-2' in s and 'method="post"' in s and 'id="appearance-settings"' in s
assert 'subsource' not in browser and 'subtitlePortal?.render(account)' in browser
assert 'localStorage' not in module and 'sessionStorage' not in module
print('PASS integration: two themed TV stages, official vs private QR destinations, lifecycle-bound sync, session-only import, phone fields, preserved appearance tab, video engines and download path unchanged.')
(w/'integration-results.txt').write_text('PASS app/site integration and unchanged video/download pipeline.\n')
