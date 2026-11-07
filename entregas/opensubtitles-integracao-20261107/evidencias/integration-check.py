from pathlib import Path
import re
w=Path('/workspace/opensubtitles-integracao');r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium');base=Path('/workspace/vod-legendas-vlc-review/upload/java/br/com/flaviotorres/iptvpremium')
changed=[str(p.relative_to(base))for p in base.rglob('*.kt')if p.read_text()!= (r/p.relative_to(base)).read_text()]
expected={'core/device/DevicePortalSync.kt','core/navigation/SubtitleSettingsScreen.kt','core/navigation/player/ExternalSubtitleRepositor.kt','core/navigation/player/PlaybackScreen.kt'}
assert set(changed)==expected,changed
repo=(r/'core/navigation/player/ExternalSubtitleRepositor.kt').read_text();screen=(r/'core/navigation/player/PlaybackScreen.kt').read_text();settings=(r/'core/navigation/SubtitleSettingsScreen.kt').read_text();store=(r/'core/navigation/player/OpenSubtitlesAccountStore.kt').read_text();client=(r/'core/navigation/player/OpenSubtitlesClient.kt').read_text();portal=(r/'core/device/DevicePortalSync.kt').read_text()
assert 'api.subsource.net' not in repo and 'SubsourceCredentialStore' not in repo+settings+portal
assert 'SubSource' not in settings
assert 'if (isLive || panel != PlayerPanel.SUBTITLES || externalSearched)' in screen
assert screen.index('val cues = externalSubtitleRepo.download(option)')>screen.index('externalOptions.forEach')
search=repo[repo.index('suspend fun search(media:'):repo.index('private suspend fun searchSubdlOnly')]
assert '.download(' not in search
search=repo[repo.index('private suspend fun searchOpenSubtitles'):repo.index('suspend fun download(option:')]
assert '.download(' not in search and search.index('val cached=')<search.index('resolveTmdb(')
download=repo[repo.index('suspend fun download(option:'):repo.index('private fun downloadedMediaKey')]
assert download.index('readSavedBytes(key)')<download.index('openSubtitles.download(')
assert 'option.accountScope' in download
assert 'ownOpenMediaKey' in repo and 'it.provider == "SubDL"' in repo
assert 'file_id' in client and '"password",password' in client and '.put("password"' not in store and '.put("username"' not in store
# The store's explanatory comment contains Passwords; inspect writes, not comments.
assert 'AES/GCM/NoPadding' in store and 'noBackupFilesDir' in store and 'AtomicFile' in store
assert 'require(data.isNull("key"))' in portal and '.put("subsource", false)' in portal
assert '.put("opensubtitles"' not in portal # Preserve the current portal schema.
assert 'withContext(Dispatchers.IO)' in client and 'Log.' not in client+store
assert 'OpenSubtitleException' in screen and 'error.problem.text' in screen
print('PASS integration: only four existing sources changed; video engines/quality/history unchanged; no SubSource requests; menu-demand search; click-only downloads after cache; encrypted token only; account scopes; safe portal compatibility.')
(w/'integration-results.txt').write_text('PASS source integration and baseline preservation; see integration-check.py for assertions.\n')
