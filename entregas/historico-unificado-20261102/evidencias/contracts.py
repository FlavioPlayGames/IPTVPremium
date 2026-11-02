from pathlib import Path
r=Path('/workspace/backup-reviewed-build/app/src/main/java/br/com/flaviotorres/iptvpremium')
nav=(r/'core/navigation/AppNavigation.kt').read_text();browser=(r/'core/navigation/player/PlaybackCatalogBrowser.kt').read_text();player=(r/'core/navigation/player/PlaybackScreen.kt').read_text();shared=(r/'data/history/SharedLibraryStore.kt').read_text()
assert 'val history = watchHistoryStore.allLocal()' in nav
assert 'it.playlistId==source.id || it.playlistId==origin' not in nav
assert 'historyStore.checkpointPlaylist(playlist,projected)' not in shared
assert 'eligibleKeys = (eligibleKeys - replacedKeys) + seeds.map' in shared
assert 'it.tracked && it.key in eligibleKeys' in shared
assert 'while(true){kotlinx.coroutines.delay(10_000);flushHistory(false,false)}' in player
assert 'Lifecycle.Event.ON_STOP' in player and 'onDispose{historyOwner.lifecycle.removeObserver(observer);flushHistory(false,false)}' in player
assert 'it.id==media.id && it.type==media.type && it.streamUrl==media.streamUrl' in player
assert 'flushHistory(true,true)\n            onEnded?.invoke()' in player
assert 'onCheckpoint = checkpoint@' in nav and 'coroutineScope.launch(Dispatchers.IO)' in nav[nav.index('onCheckpoint = checkpoint@'):nav.index('onPlaybackEnded =',nav.index('onCheckpoint = checkpoint@'))]
assert 'progressSession.accept(event)' in nav and 'progressSession.close()' in nav
assert 'remember(activePlaylistId,parentalAccessState)' in nav
assert 'LaunchedEffect(activePlaylistId, historyRefreshSerial,historyCheckpointRevision,' in nav
assert 'favoriteMovies=catalogFavoriteMovieIds,favoriteSeries=catalogFavoriteSeriesIds' in nav
assert 'onFavorite={kind,id->toggleSharedFavorite(kind,id)}' in nav
assert 'var favoriteMovies by remember' not in browser and 'episodeStore.saveIds(kind,next)' not in browser
assert 'onRemoveContinue=onRemoveFromContinue' in (r/'ui/series/SeriesScreen.kt').read_text()
assert 'movieResumeStore.clearPosition(playlistId,movieId)' in nav and 'seriesResumeStore.clearSeries(playlist,id)' in nav
start=nav.index('fun changeEpisode(');end=nav.index('val savedStartMs',start)
assert 'watchHistoryStore.remove' not in nav[start:end]
assert 'queueKnownNext' in nav and 'SharedLibraryStore.resumePosition(target,requested,duration)' in nav
repo=(r/'data/repository/PlaylistCatalogRepository.kt').read_text();a=repo.index('internal suspend fun filterHistoryForCatalog');b=repo.index('internal suspend fun isParentalBlocked',a)
assert 'entries.mapNotNull' in repo[a:b] and 'if(!exists)return@mapNotNull null' in repo[a:b]
print('PASS integration contracts: global source, selective lookup, no copies, real availability, checkpoint lifecycle/owner/IO, stable focus, unified favorites and removals')
