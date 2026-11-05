package br.com.flaviotorres.iptvpremium.data.history
import android.content.Context
import br.com.flaviotorres.iptvpremium.core.player.*
import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeInfo
import kotlinx.coroutines.runBlocking
import java.nio.file.Files

fun main()=runBlocking {
 var n=0
 fun verify(ok:Boolean,name:String){check(ok){name};n++;println("PASS $name")}
 fun fresh():Triple<Context,WatchHistoryStore,SharedLibraryStore>{
  for(field in listOf("instance","kidsInstance"))SharedLibraryStore::class.java.getDeclaredField(field).apply{isAccessible=true}.set(null,null)
  val context=Context(Files.createTempDirectory("global-history-").toFile());return Triple(context,WatchHistoryStore(context),SharedLibraryStore.get(context))
 }
 fun row(id:String,name:String,year:Int?=2019,tmdb:String?=null,order:Int?=null)=SharedCatalogIdentity(id,name,year,null,tmdb,null,order)
 fun movie(id:String,name:String="Homem-Aranha: Longe de Casa 4K",year:Int?=2019,tmdb:String?="429617")=PlaybackMedia(id,PlaybackContentType.MOVIE,name,durationMs=7_200_000,year=year,tmdbId=tmdb)
 run {
  val (ctx,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("a","Homem-Aranha: Longe de Casa 4K"),row("missingB","Outro Filme",2020))
  dao.movies["B"]=listOf(row("b","Homem-Aranha: Longe de Casa FHD",order=0),row("b2","Homem-Aranha: Longe de Casa HD",order=1),row("wrong","Homem-Aranha: De Volta ao Lar",2017))
  dao.movies["C"]=listOf(row("c","Homem-Aranha: Longe de Casa HD"),row("foundC","Outro Filme",2020))
  h.save("A",movie("a"),120_000,7_200_000)
  h.save("A",movie("missingB","Outro Filme",2020,null),330_000,7_200_000)
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  val b=h.all("B").filter{it.inProgress}
  verify(b.size==1 && b.single().contentId=="b","4K falls back to real FHD; one card despite two available editions")
  verify(b.single().positionMs==120_000L,"fallback preserves position")
  verify(b.none{it.contentId=="wrong"},"different Spider-Man film is not matched")
  lib.checkpointCompletedHistory("B",h)
  verify(h.allLocal().count{it.playlistId=="A"}==2 && h.allLocal().none{it.playlistId=="B"},"list switch does not materialize copies or discard absent titles")
  lib.reconcile(dao,"C",h.allLocal(),kind="movies")
  verify(h.all("C").map{it.contentId}.toSet()==setOf("c","foundC"),"A to B to C recovers work absent from B")
  dao.movies.remove("A")
  lib.reconcile(dao,"C",h.allLocal(),force=true,kind="movies")
  verify(h.all("C").size==2,"canonical progress survives unavailable original catalog")
  h.save("C",movie("c","Homem-Aranha: Longe de Casa HD"),240_000,7_200_000)
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(h.all("B").single().positionMs==240_000L,"newest watch wins on returning to an older list")
  h.remove("B",PlaybackContentType.MOVIE,h.all("B").single().contentId)
  verify(h.all("B").isEmpty(),"explicit removal hides all linked editions")
  verify(h.resumeEntry("B",movie("b","Homem-Aranha: Longe de Casa FHD"))?.positionMs==0L,"explicit removal suppresses stale standalone resume positions")
  lib.reconcile(dao,"C",h.allLocal(),kind="movies")
  verify(h.all("C").none{it.contentId=="c"},"removal does not resurrect through another list")
  h.save("C",movie("c","Homem-Aranha: Longe de Casa HD"),360_000,7_200_000)
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(h.all("B").single().positionMs==360_000L,"a later actual watch restores hidden work")
 }
 run {
  val (ctx,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("x","Favorito",2020),row("complete","Terminado",2021))
  dao.movies["B"]=listOf(row("bx","Favorito HD",2020),row("bc","Terminado",2021),row("noise","Filme Sem Relação",2022))
  ctx.getSharedPreferences("iptvpremium_favorites_v1",0).edit().putStringSet("movies_A",setOf("x")).apply()
  h.save("A",movie("complete","Terminado",2021,null),7_100_000,7_200_000)
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(lib.favorites("B","movies",emptySet())==setOf("bx"),"favorite-only movie is eligible and maps to available edition")
  verify(dao.requests.none{it.contains("Terminado")||it.contains("Sem Relação")},"completed nonfavorite and unrelated catalog do not seed searches")
  var metadataCalls=0
  lib.enrichMatchingMovies("B"){metadataCalls++;null}
  verify(metadataCalls==0,"no metadata requests for unrelated or already matched titles")
  lib.setFavorite(dao,"B","movies","bx",false)
  dao.requests.clear();lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(dao.requests.isEmpty(),"explicit unfavorite supersedes an older favorite without rescan")
 }
 run {
  val (_,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("a","A Obra",2020))
  dao.movies["B"]=listOf(row("b","A Obra",2020),row("remake","A Obra",1990))
  h.save("A",movie("a","A Obra",2020,null),200_000,1_000_000)
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(h.all("B").map{it.contentId}==listOf("b"),"name and year fallback keeps remakes separate")
  h.save("B",movie("b","A Obra",2020,null),950_000,1_000_000)
  lib.reconcile(dao,"A",h.allLocal(),kind="movies")
  verify(h.all("A").none{it.inProgress},"new completion supersedes old unfinished progress globally")
  val resume=h.resumeEntry("A",movie("a","A Obra",2020,null))
  verify(resume?.completed==true && SharedLibraryStore.resumePosition(resume,200_000,1_000_000)==0L,"completed work cannot offer an older resume position")
 }
 run {
  val (_,h,_)=fresh()
  val ep=PlaybackMedia("e1",PlaybackContentType.EPISODE,"E1",seriesId="s",seasonNumber=1,episodeNumber=1,year=2020,tmdbId="42",subtitleSearchTitle="Uma Série")
  h.save("A",ep,20_000,100_000,"Uma Série")
  verify(h.allLocal().single().inProgress,"10 second entry policy retained")
  h.save("A",ep,95_000,100_000,"Uma Série")
  verify(h.allLocal().single().completed,"95 percent completion retained")
  val next=SeriesEpisodeInfo("e2",1,2,"E2",100)
  h.queueNext("A",ep,next,"Uma Série",null)
  val raw=h.allLocal();val queued=raw.first{it.contentId=="e2"}
  verify(raw.first{it.contentId=="e1"}.completed,"queuing next episode retains completed predecessor")
  verify(queued.pendingNext && queued.positionMs==0L && queued.inProgress,"real next episode is queued at zero without inventing progress")
  verify(WatchHistoryStore(hContext(h)).allLocal().first{it.contentId=="e2"}.pendingNext,"queued state survives a new store instance")
  verify(latestHistorySeries(raw).single().contentId=="e2","series card follows actual next episode")
  h.save("A",ep.copy(id="e2",episodeNumber=2),100_000,100_000,"Uma Série",endReached=true)
  val before=h.allLocal().first{it.contentId=="e2"}
  h.queueNext("A",ep,next,"Uma Série",null)
  verify(h.allLocal().first{it.contentId=="e2"}==before,"queue cannot erase an already completed episode")
  verify(latestHistorySeries(h.allLocal()).none{it.inProgress},"completed newest episode does not expose an older unfinished episode")
  h.save("A",ep.copy(id="unknown"),20_000,null,"Uma Série",endReached=true)
  verify(h.allLocal().first{it.contentId=="unknown"}.completed,"end event marks completed even without known duration")
  h.save("A",ep.copy(id="unknown"),20_000,null,"Uma Série")
  verify(h.allLocal().first{it.contentId=="unknown"}.completed,"duplicate exit does not undo unknown-duration completion")
 }
 run {
  val (_,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("a","Uma Obra 4K",2020,"11"))
  dao.movies["B"]=listOf(row("weak","Uma Obra 4K",2020,null,0),row("strong","Uma Obra HD",2020,"11",1))
  h.save("A",movie("a","Uma Obra 4K",2020,"11"),200_000,1_000_000)
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(h.all("B").single().contentId=="strong","confirmed external ID outranks a weaker 4K title match")
  dao.movies["B"]=emptyList();lib.reconcile(dao,"B",h.allLocal(),force=true,kind="movies")
  verify(h.all("B").isEmpty() && h.allLocal().isNotEmpty(),"removed catalog version vanishes without deleting canonical history")
 }
 run {
  val (_,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("a","Título Original",null));dao.movies["B"]=listOf(row("b","Título Original HD",2020),row("noise","Outra Obra",null))
  h.save("A",movie("a","Título Original",null,null),200_000,1_000_000)
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(h.all("B").isEmpty(),"movie title alone cannot guess an edition without year or external ID")
  val calls=mutableListOf<String>()
  lib.enrichMatchingMovies("B"){request->calls+=request.id;ContentIdentity("movies",request.title,2020)}
  verify(calls==listOf("a"),"metadata enrichment is restricted to an eligible source missing identity")
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(h.all("B").single().contentId=="b","confirmed metadata unlocks the real available movie")
 }
 run {
  val (_,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("a","Nome em Português",2020,"12"));dao.movies["B"]=listOf(row("b","Official English Title",2020,"12"))
  h.save("A",movie("a","Nome em Português",2020,"12"),100_000,1_000_000)
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  var requests=0
  lib.enrichAlternateTitles("B","movies"){identity->requests++;identity.copy(alternateTitles=listOf("Official English Title"))}
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(requests==1 && h.all("B").single().contentId=="b","official aliases locate translated catalog names without fuzzy guesses")
 }
 run {
  val (_,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.series["A"]=listOf(row("sa","Série",2020,"44"));dao.series["B"]=listOf(row("sb","Série",2020,"44"))
  val e=PlaybackMedia("e2",PlaybackContentType.EPISODE,"E2",seriesId="sa",seasonNumber=1,episodeNumber=2,year=2020,tmdbId="44",subtitleSearchTitle="Série")
  h.save("A",e,30_000,1_000_000,"Série")
  lib.reconcile(dao,"B",h.allLocal(),kind="series")
  verify(h.all("B").isEmpty(),"series identity alone never invents a target episode")
  verify(lib.favorites("B","series",emptySet()).isEmpty(),"incomplete history cannot silently add a series favorite")
  lib.importEpisodeLinks("B"){listOf(SeriesEpisodeInfo("real-e2",1,2,"Real Episode 2"))}
  verify(h.all("B").single().contentId=="real-e2" && h.all("B").single().positionMs==30_000L,"series progress resolves actual target season and episode ID")
  lib.registerEpisodes("B","sb",listOf(SeriesEpisodeInfo("real-e2",1,2,"E2"),SeriesEpisodeInfo("duplicate-e2",1,2,"E2 alt")))
  verify(h.all("B").isEmpty(),"ambiguous episode coordinates are rejected")
  var rejected=false;try{lib.checkpointCompletedHistory("B",h)}catch(_:IllegalStateException){rejected=true}
  verify(rejected,"incomplete episode lookup never announces completed history")
 }
 run {
  val (_,h,_)=fresh()
  val workers=(0 until 12).map{i->Thread{val store=WatchHistoryStore(hContext(h));store.save("p",movie("m$i","M$i",2020,null),20_000,1_000_000)}}
  workers.forEach{it.start()};workers.forEach{it.join()}
  verify(h.allLocal().size==12,"concurrent store instances cannot overwrite each other's checkpoints")
 }
 run {
  val (ctx,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("legacy","Filme do Player",2020),row("main","Filme Principal",2021))
  dao.movies["B"]=listOf(row("b","Filme do Player HD",2020))
  val old=ctx.getSharedPreferences("iptvpremium_player_episode_favorites_v1",0)
  old.edit().putString("normal:A:movies","[\"legacy\"]").putString("kids:A:movies","[\"kid\"]").apply()
  val target=ctx.getSharedPreferences("iptvpremium_favorites_v1",0)
  target.edit().putStringSet("movies_A",setOf("main")).apply()
  verify(PlayerFavoriteMigration.run(ctx)==2,"old player favorites migrate once for each scope")
  verify(target.getStringSet("movies_A",emptySet())==setOf("legacy","main"),"migration unions player and normal favorites without dropping either")
  verify(target.getStringSet("kids_movies_A",emptySet())==setOf("kid"),"migration preserves Kids isolation")
  verify(PlayerFavoriteMigration.run(ctx)==0,"favorite migration is idempotent")
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(lib.favorites("B","movies",emptySet())==setOf("b"),"a former player-only favorite participates in cross-list matching")
  lib.setFavorite(dao,"B","movies","b",false)
  target.edit().putStringSet("movies_B",emptySet()).apply()
  lib.reconcile(dao,"B",h.allLocal(),kind="movies")
  verify(lib.favorites("B","movies",emptySet()).isEmpty(),"later removal wins over imported player favorites")
 }
 run {
  val (ctx,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("a","Uma Obra 4K",2020,"7"),row("a-hd","Uma Obra HD",2020,"7"))
  ctx.getSharedPreferences("iptvpremium_favorites_v1",0).edit().putStringSet("movies_A",setOf("a","a-hd")).apply()
  lib.reconcile(dao,"A",h.allLocal(),kind="movies")
  verify(lib.favorites("A","movies",setOf("a","a-hd")).size==1,"local favorite editions collapse into one actual catalog card")
  val older=lib.reserveFavoriteEdit();val newer=lib.reserveFavoriteEdit()
  lib.setFavorite(dao,"A","movies","a",false,newer);lib.setFavorite(dao,"A","movies","a",true,older)
  lib.reconcile(dao,"A",h.allLocal(),kind="movies")
  verify(lib.favorites("A","movies",setOf("a","a-hd")).isEmpty(),"out-of-order asynchronous favorite edits keep the latest user action")
 }
 run {
  val (_,h,lib)=fresh();val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("id","Filme Original",2019,"1"))
  h.save("A",movie("id","Filme Original",2019,"1"),200_000,1_000_000)
  lib.reconcile(dao,"A",h.allLocal(),kind="movies")
  dao.movies["A"]=listOf(row("id","Outro Filme",2022,"99"))
  lib.reconcile(dao,"A",h.allLocal(),force=true,kind="movies")
  verify(h.all("A").isEmpty(),"recycled provider ID cannot attach old progress to another movie")
  verify(h.resumeEntry("A",movie("id","Outro Filme",2022,"99"))==null,"explicit playback also rejects an unrelated recycled ID")
  verify(!h.allowLegacyResume("A",movie("id","Outro Filme",2022,"99")),"legacy resume cannot bypass a recognized identity conflict")
 }
 run {
  val (_,h,_)=fresh()
  h.save("A",movie("4k","A Obra 4K",2020,"7"),300_000,1_000_000)
  h.save("D",movie("hd","A Obra HD",2020,"7"),100_000,1_000_000)
  verify(h.allLocal().size==1 && h.allLocal().single().positionMs==100_000L,"confirmed duplicate history editions compact to the newest state, not the furthest position")
 }
 run {
  val gate=HistoryCheckpointGate();val old=gate.sequence.incrementAndGet();val newer=gate.sequence.incrementAndGet()
  verify(gate.accept(newer) && !gate.accept(old),"late checkpoints cannot overwrite newer playback positions")
  gate.close();verify(!gate.accept(gate.sequence.incrementAndGet()),"closed playback owner rejects pending background writes")
  gate.reopen();verify(gate.accept(gate.sequence.incrementAndGet()),"a failed navigation can reopen its existing playback owner")
 }
 verify(sameHistoryWork(ContentIdentity("movies","A",2020,tmdb="1"),ContentIdentity("movies","B",2020,tmdb="1")),"external ID allows translated titles")
 verify(!sameHistoryWork(ContentIdentity("movies","A",2020,tmdb="1"),ContentIdentity("movies","A",2020,tmdb="2")),"external-ID conflict blocks a false match")
 verify(ContentIdentity("movies","Homem-Aranha 2",2004).normalizedTitle.contains("2"),"numbers belonging to titles survive normalization")
 verify(preferredHistoryVariantIndex("A 4K",listOf("A HD" to 0,"A 4K" to 1))==1,"same quality preferred when actually available")
 verify(preferredHistoryVariantIndex("A 4K",listOf("A [L] FHD" to 0,"A HD" to 1))==1,"language preference before quality fallback")
 verify(SharedLibraryStore.resumePosition(null,950_000,1_000_000)==0L,"resume and Continue share completion threshold")
 run {
  val(ctx,normal,nlib)=fresh();val kids=WatchHistoryStore(ctx,true);val klib=SharedLibraryStore.get(ctx,true);val dao=PlaylistCacheDao()
  dao.movies["A"]=listOf(row("same","Filme Compartilhado",2020),row("n","Só Normal",2021),row("k","Só Kids",2022))
  dao.movies["B"]=listOf(row("sameB","Filme Compartilhado HD",2020),row("nB","Só Normal",2021),row("kB","Só Kids",2022))
  normal.save("A",movie("same","Filme Compartilhado",2020,null),120_000,7_200_000)
  normal.save("A",movie("n","Só Normal",2021,null),180_000,7_200_000)
  verify(kids.allLocal().isEmpty(),"Kids never imports unmarked legacy normal progress")
  kids.save("A",movie("same","Filme Compartilhado",2020,null),360_000,7_200_000)
  kids.save("A",movie("k","Só Kids",2022,null),240_000,7_200_000)
  verify(normal.allLocal().map{it.contentId}.toSet()==setOf("same","n"),"Kids writes do not enter normal Continue")
  verify(kids.allLocal().map{it.contentId}.toSet()==setOf("same","k"),"normal writes do not enter Kids Continue")
  nlib.reconcile(dao,"A",normal.allLocal(),kind="movies");klib.reconcile(dao,"A",kids.allLocal(),kind="movies")
  verify(normal.resumeEntry("A",movie("same","Filme Compartilhado",2020,null))?.positionMs==120_000L,"same movie resumes at normal position")
  verify(kids.resumeEntry("A",movie("same","Filme Compartilhado",2020,null))?.positionMs==360_000L,"same movie resumes independently in Kids")
  nlib.reconcile(dao,"B",normal.allLocal(),kind="movies");klib.reconcile(dao,"B",kids.allLocal(),kind="movies")
  verify(normal.all("B").map{it.contentId}.toSet()==setOf("sameB","nB"),"normal cross-list projection excludes Kids-only titles")
  verify(kids.all("B").map{it.contentId}.toSet()==setOf("sameB","kB"),"Kids cross-list projection excludes normal-only titles")
  kids.remove("B",PlaybackContentType.MOVIE,"sameB")
  verify(kids.all("B").none{it.contentId=="sameB"},"Kids removal hides only its own projection")
  verify(normal.all("B").any{it.contentId=="sameB"},"Kids removal cannot hide normal viewing of the same film")
  val old=ctx.getSharedPreferences("iptvpremium_movies_assistir_v1",0);old.edit().putLong("movie:1:A:6:legacy",123_000).commit()
  verify(normal.legacyMoviePositions("A").any{it.first=="legacy"},"normal retains its existing legacy resume file")
  verify(kids.legacyMoviePositions("A").isEmpty(),"Kids cannot read normal legacy resume positions")
  val kidLegacy=ctx.getSharedPreferences(HistoryProfile.preferences("iptvpremium_movies_assistir_v1",true),0)
  kidLegacy.edit().putLong("movie:1:A:6:legacy",456_000).commit()
  verify(kids.legacyMoviePositions("A").single().second==456_000L,"Kids has its own legacy resume namespace")
  val fav=ctx.getSharedPreferences("iptvpremium_favorites_v1",0)
  fav.edit().putStringSet("movies_A",setOf("n")).putStringSet("kids_movies_A",setOf("k")).commit()
  nlib.reconcile(dao,"B",normal.allLocal(),force=true,kind="movies");klib.reconcile(dao,"B",kids.allLocal(),force=true,kind="movies")
  verify(nlib.favorites("B","movies",emptySet())==setOf("nB"),"normal favorites import ignores Kids sets")
  verify(klib.favorites("B","movies",emptySet())==setOf("kB"),"Kids favorites import ignores normal sets")
  klib.checkpointCompletedHistory("B",kids)
  verify(fav.getStringSet("movies_A",emptySet())==setOf("n") && fav.getStringSet("movies_B",emptySet()).orEmpty().isEmpty(),"Kids checkpoint cannot write normal favorites")
  verify(fav.getStringSet("kids_movies_B",emptySet())==setOf("kB"),"Kids checkpoint persists only Kids target favorites")
  normal.clearType(PlaybackContentType.MOVIE)
  verify(normal.allLocal().isEmpty() && kids.allLocal().isNotEmpty(),"clearing normal history preserves Kids progress")
  verify(kidLegacy.getLong("movie:1:A:6:legacy",0)==456_000L,"clearing normal history preserves Kids legacy positions")
  kids.clearType(PlaybackContentType.MOVIE)
  verify(kids.allLocal().isEmpty() && old.all.isEmpty(),"each profile clears only its own history and resume")
 }
 run {
  val(ctx,normal,nlib)=fresh();val kids=WatchHistoryStore(ctx,true)
  val ep=PlaybackMedia("ep",PlaybackContentType.EPISODE,"Episódio",durationMs=1_000_000,seriesId="serie",seasonNumber=1,episodeNumber=1,subtitleSearchTitle="Série")
  normal.save("A",ep,990_000,1_000_000,seriesTitle="Série")
  kids.save("A",ep,120_000,1_000_000,seriesTitle="Série")
  verify(normal.allLocal().single().completed && kids.allLocal().single().inProgress,"normal episode completion cannot complete Kids episode")
  verify(kids.allLocal().single().positionMs==120_000L,"same episode retains independent Kids position")
  val normalRun=HistorySyncSession();val kidsRun=HistorySyncSession(true)
  val n=normalRun.request("A");val k=kidsRun.request("A");normalRun.start(n.id)
  verify(kidsRun.state.value?.phase==HistorySyncSession.Phase.PENDING,"normal history import cannot change Kids import phase")
  kidsRun.start(k.id);kidsRun.reset()
  verify(normalRun.state.value?.phase==HistorySyncSession.Phase.RUNNING,"resetting Kids ledger cannot reset normal import")
 }
 run {
  for(field in listOf("instance","kidsInstance"))SharedLibraryStore::class.java.getDeclaredField(field).apply{isAccessible=true}.set(null,null)
  val ctx=Context(Files.createTempDirectory("profile-migration-").toFile())
  val mixed=org.json.JSONArray().put(org.json.JSONObject().put("playlistId","A").put("type","MOVIE").put("contentId","old").put("title","Misturado").put("positionMs",120000).put("durationMs",7200000))
   .put(org.json.JSONObject().put("playlistId","A").put("type","EPISODE").put("seriesId","s").put("contentId","finished").put("title","Concluído").put("positionMs",990000).put("durationMs",1000000))
  ctx.getSharedPreferences("iptvpremium_watch_history_v1",0).edit().putString("items",mixed.toString()).commit()
  ctx.getSharedPreferences("iptvpremium_movies_assistir_v1",0).edit().putLong("movie:1:A:3:old",120000).commit()
  ctx.getSharedPreferences("iptvpremium_series_assistir_v1",0).edit().putString("last:1:A:1:s","ep").commit()
  val prefs=ctx.getSharedPreferences("iptvpremium_favorites_v1",0);prefs.edit().putStringSet("movies_A",setOf("fav")).putStringSet("kids_movies_A",setOf("kid-fav")).commit()
  val normal=WatchHistoryStore(ctx);val kids=WatchHistoryStore(ctx,true)
  verify(normal.allLocal().none{it.inProgress},"approved migration removes old mixed Continue")
  verify(normal.allLocal().single().contentId=="finished" && normal.allLocal().single().completed,"migration preserves completed episode marks")
  verify(normal.legacyMoviePositions("A").isEmpty() && normal.legacySeriesPositions("A").isEmpty(),"old standalone positions cannot resurrect cleared Continue")
  verify(prefs.getStringSet("movies_A",emptySet())==setOf("fav") && prefs.getStringSet("kids_movies_A",emptySet())==setOf("kid-fav"),"one-time cleanup preserves favorites from both profiles")
  normal.save("A",movie("new-normal","Novo normal",2023,null),120000,7200000)
  kids.save("A",movie("new-kids","Novo Kids",2024,null),240000,7200000)
  val reopenedNormal=WatchHistoryStore(ctx);val reopenedKids=WatchHistoryStore(ctx,true)
  verify(reopenedNormal.allLocal().any{it.contentId=="new-normal" && it.inProgress},"normal Continue survives subsequent app/store openings")
  verify(reopenedKids.allLocal().single().contentId=="new-kids","Kids Continue survives subsequent app/store openings")
  verify(ctx.getSharedPreferences("iptvpremium_history_profiles_v1",0).getBoolean("continue_reset_v1",false),"cleanup completion is durably marked once")
 }
 println("$n checks passed: global availability, A-B-C retention, variants, selective lookup, completion, queue and resume")
}
fun hContext(h:WatchHistoryStore):Context=WatchHistoryStore::class.java.getDeclaredField("prefsContext").apply{isAccessible=true}.get(h) as Context
