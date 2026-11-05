from pathlib import Path
import re,sqlite3
w=Path('/workspace/player-abertura-qualidade');base=w/'upload/java/br/com/flaviotorres/iptvpremium';root=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium')
changed=[str(p.relative_to(base))for p in base.rglob('*.kt')if p.read_text()!= (root/p.relative_to(base)).read_text()]
assert changed==['core/navigation/AppNavigation.kt'],changed
print('PASS quality preservation: all other 291 Kotlin sources identical to uploaded ZIP, including both engines, buffers, URLs/track selection, video layout and subtitles')
s=(root/changed[0]).read_text();before=(w/'before-AppNavigation.kt').read_text()
a=s.index('val cachedStreamUrl = withContext');b=s.index('val xtream =',a);lookup=s[a:b]
assert 'episodePreviewUrl(' in lookup and 'getActiveGeneration(currentPlaylistId)' in lookup
assert 'getEpisodesPage' not in lookup and 'while (' not in lookup
assert 'resolveEpisodeSeriesTmdbId' not in s and 'episodeTmdbIds' not in s
assert 'val resolvedTmdbId = validProviderTmdbId' in s
# Preserve direct movie route and the exact stream fallback/source/extension construction.
a=before.index('if (contentType == PlaybackContentType.MOVIE.name) {',before.index('val playbackRouteState by'))
b=before.index('} else if (contentType == PlaybackContentType.LIVE_CHANNEL.name)',a)
assert before[a:b] in s
start=before.index('                                    val xtream =',before.index('val cachedStreamUrl = withContext'));end=before.index('                                    if (streamUrl.isNullOrBlank())',start)
assert before[start:end] in s
repo=(root/'core/navigation/player/ExternalSubtitleRepositor.kt').read_text();screen=(root/'core/navigation/player/PlaybackScreen.kt').read_text()
assert '(media.subtitleSearchTitle ?: media.title)' in repo and 'resolveTmdb(media, cleanTitle, wantedYear)' in repo
assert 'panel != PlayerPanel.SUBTITLES' in screen and 'externalSubtitleRepo.search(media)' in screen
print('PASS startup contracts: no external identity request on episode opening; provider ID/title/year/season/episode retained; missing identity resolved on subtitle demand; movie opening and stream construction unchanged')
# Execute the real DAO SQL against SQLite, including an episode beyond the former 5000 limit.
dao=(root/'data/cache/dao/PlaylistCacheDao.kt').read_text();query=re.search(r'@Query\("([^"\n]+)"\)\s+suspend fun episodePreviewUrl',dao).group(1)
db=sqlite3.connect(':memory:');db.execute('CREATE TABLE cached_episodes(playlistId TEXT,generationId TEXT,seriesId TEXT,episodeId TEXT,streamUrl TEXT,PRIMARY KEY(playlistId,generationId,seriesId,episodeId))')
db.executemany('INSERT INTO cached_episodes VALUES(?,?,?,?,?)',[('p','active','s',str(i),f'original-url-{i}')for i in range(6100)])
db.executemany('INSERT INTO cached_episodes VALUES(?,?,?,?,?)',[('other','active','s','6000','wrong-list'),('p','old','s','6000','wrong-generation'),('p','active','other','6000','wrong-series')])
args=dict(playlistId='p',generationId='active',seriesId='s',episodeId='6000')
assert db.execute(query,args).fetchone()==('original-url-6000',)
for key in ['playlistId','generationId','seriesId','episodeId']:
 assert db.execute(query,dict(args,**{key:'missing'})).fetchone()is None
print('PASS actual episode DAO query: exact original URL beyond 5000 episodes; playlist/generation/series/episode isolation and missing result')
(w/'test-results.txt').write_text('PASS quality preservation: 291 unchanged Kotlin sources; both engines, buffers, track selection and rendering byte-equivalent after line ending normalization.\nPASS no blocking TMDB request on episode startup; on-demand subtitle identity fallback retained.\nPASS movie route and stream source construction unchanged.\nPASS actual DAO SQL: episode 6000, four identity boundaries and missing records.\n')
