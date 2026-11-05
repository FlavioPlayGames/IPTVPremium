package br.com.flaviotorres.iptvpremium.core.player

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.*
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.data.cache.PosterKind
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodesResult
import br.com.flaviotorres.iptvpremium.data.repository.SeriesEpisodeInfo
import br.com.flaviotorres.iptvpremium.domain.model.*
import br.com.flaviotorres.iptvpremium.ui.movies.MoviePosterLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import br.com.flaviotorres.iptvpremium.data.history.WatchHistoryEntry

/** Carries catalog identities only; URLs are resolved again by the existing playback route. */
data class PlayerCatalogSelection(val movie: Movie? = null, val series: Series? = null,
    val episode: SeriesEpisodeInfo? = null, val episodes: SeriesEpisodesResult? = null, val startPositionMs:Long?=null)


private data class CatalogCover(val id:String,val title:String,val year:Int?,val focus:FocusRequester,
    val center:Float,val loader:suspend ()->String?)

/** Independent Paging collectors: never changes the outside catalog or the playing controller. */
@Composable
internal fun PlaybackCatalogBrowser(playlistId:String, media:PlaybackMedia,
    repository:PlaylistCatalogRepository, loadEpisodes:suspend (String)->SeriesEpisodesResult?,
    onSelect:(PlayerCatalogSelection)->Unit, onClose:()->Unit,
    history:List<WatchHistoryEntry> = emptyList(),favoriteMovies:Set<String> = emptySet(),favoriteSeries:Set<String> = emptySet(),
    onFavorite:(String,String)->Unit={_,_->},loadMovieSynopsis:suspend (String)->String?={null},
    loadSeriesSynopsis:suspend (String)->String?={null},registerBack:((()->Unit)?)->Unit={}) {
    val seriesMode=media.type==PlaybackContentType.EPISODE
    val type=if(seriesMode)"Séries"else"Filmes"
    val accent=playerThemeAccent()
    val focusManager=LocalFocusManager.current
    val context=LocalContext.current.applicationContext
    val access by br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.accessFlow.collectAsState(initial=br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access())
    // Cache only this open browser and playlist/profile; never scan the catalog.
    val synopsisCache=remember(playlistId,access){linkedMapOf<String,String?>()}
    fun synopsisLoader(id:String,series:Boolean=false,known:String?=null):suspend ()->String? = {
        val key="${if(series)"series"else"movies"}:$id"
        val cached=synchronized(synopsisCache){synopsisCache.containsKey(key) to synopsisCache[key]}
        if(cached.first)cached.second else (known?.trim()?.takeIf{it.isNotEmpty()}
            ?: if(series)loadSeriesSynopsis(id)else loadMovieSynopsis(id))?.trim()?.takeIf{it.isNotEmpty()}.also{plot->
            synchronized(synopsisCache){synopsisCache[key]=plot;if(synopsisCache.size>48)synopsisCache.remove(synopsisCache.keys.first())}
        }
    }
    val kids=br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active
    val episodeStore=remember(playlistId,kids){PlayerEpisodeFavorites(context,playlistId,kids)}
    var favoriteEpisodes by remember(episodeStore){mutableStateOf<List<PlayerFavoriteEpisode>>(emptyList())}
    LaunchedEffect(episodeStore){
        favoriteEpisodes=withContext(Dispatchers.IO){episodeStore.all()}
    }
    fun notifyFavorite(added:Boolean){android.widget.Toast.makeText(context,if(added)"Adicionado aos favoritos"else"Removido dos favoritos",android.widget.Toast.LENGTH_SHORT).show()}

    var filter by remember{mutableStateOf("category")}
    var localMovies by remember(playlistId,access){mutableStateOf<List<Pair<Movie,WatchHistoryEntry>>>(emptyList())}
    var localSeries by remember(playlistId,access){mutableStateOf<List<Pair<Series,WatchHistoryEntry>>>(emptyList())}
    var allowedFavoriteSeries by remember{mutableStateOf<Map<String,Series>>(emptyMap())}
    var localLoading by remember{mutableStateOf(false)}
    var category by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var level by remember { mutableStateOf(PlayerCatalogLevel.ITEMS) }
    var selectedSeries by remember { mutableStateOf<Series?>(null) }
    var result by remember { mutableStateOf<SeriesEpisodesResult?>(null) }
    var season by remember { mutableIntStateOf(1) }
    var loadingEpisodes by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentMovie by remember { mutableStateOf<Movie?>(null) }
    var currentSeries by remember { mutableStateOf<Series?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    val seriesImages=rememberPlayerSeriesImages(selectedSeries,season,result?.episodes.orEmpty())
    val seasonPosters=seriesImages.posters
    val scope=rememberCoroutineScope()
    var episodeJob by remember { mutableStateOf<Job?>(null) }
    var episodeRequest by remember { mutableIntStateOf(0) }
    val headerFocus=remember { FocusRequester() }
    var focusedCover by remember{mutableStateOf<CatalogCover?>(null)}
    var synopsisCover by remember{mutableStateOf<CatalogCover?>(null)}
    var returnCoverId by remember{mutableStateOf<String?>(null)}
    val coverListState=rememberLazyListState()
    LaunchedEffect(category,filter){coverListState.scrollToItem(0);returnCoverId=null}
    fun coverAnchor(id:String,title:String,year:Int?,loader:suspend ()->String?):(FocusRequester?,Float)->Unit = {focus,center->
        if(focus!=null)focusedCover=CatalogCover(id,title,year,focus,center,loader)
        else if(focusedCover?.id==id)focusedCover=null
    }
    fun openSynopsis(){if(level==PlayerCatalogLevel.ITEMS)synopsisCover=focusedCover}

    LaunchedEffect(playlistId,media.id,media.seriesId,access,reload) {
        episodeRequest++;episodeJob?.cancel();loadingEpisodes=false
        error=null;currentMovie=null;currentSeries=null;selectedSeries=null;result=null;level=PlayerCatalogLevel.ITEMS;ready=false
        try {
            categories=if(seriesMode)repository.getSeriesCategories(playlistId)else repository.getMovieCategories(playlistId)
            if(seriesMode) {
                currentSeries=media.seriesId?.let { repository.getSeriesById(playlistId,it) }
                category=currentSeries?.categoryId
            } else {
                currentMovie=repository.getMovieById(playlistId,media.id)
                category=currentMovie?.categoryId
            }
        } catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){error="Não foi possível carregar o catálogo."}
        finally {ready=true}
    }
    var entryPending by remember(level,category,filter,selectedSeries?.id,season) { mutableStateOf(true) }
    fun coverEntry(id:String,fallback:Boolean)=entryPending && (returnCoverId?.let{it==id}?:fallback)
    fun back() {
        error=null
        when(catalogBackStep(level,synopsisCover!=null)) {
            CatalogBackStep.SYNOPSIS->{val focus=synopsisCover?.focus;synopsisCover=null;scope.launch{withFrameNanos{};runCatching{focus?.requestFocus()}}}
            CatalogBackStep.CATEGORIES->{focusedCover=null;level=PlayerCatalogLevel.CATEGORIES;scope.launch{coverListState.scrollToItem(0)}}
            CatalogBackStep.CLOSE->onClose()
            CatalogBackStep.COVERS->{returnCoverId=selectedSeries?.id;episodeRequest++;episodeJob?.cancel();loadingEpisodes=false;level=PlayerCatalogLevel.ITEMS;selectedSeries=null;result=null}
            CatalogBackStep.SEASONS->level=PlayerCatalogLevel.SEASONS
        }
    }
    val latestBack by rememberUpdatedState<()->Unit>({back()})
    val latestRegister by rememberUpdatedState(registerBack)
    DisposableEffect(Unit){latestRegister({latestBack()});onDispose{latestRegister(null)}}
    fun openSeries(value:Series) {
        synopsisCover=null;focusedCover=null
        episodeRequest++
        val request=episodeRequest
        episodeJob?.cancel()
        selectedSeries=value;season=if(value.id==media.seriesId)media.seasonNumber?:1 else 1;result=null;error=null;level=PlayerCatalogLevel.SEASONS;loadingEpisodes=true
        episodeJob=scope.launch {
            try {
                val loaded=loadEpisodes(value.id)
                if(request==episodeRequest){result=loaded;if(loaded?.episodes.isNullOrEmpty())error="Nenhum episódio disponível."}

            }
            catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){if(request==episodeRequest)error="Não foi possível carregar os episódios."}
            finally{if(request==episodeRequest)loadingEpisodes=false}
        }
    }
    fun toggle(kind:String,id:String){
        val values=if(kind=="movies")favoriteMovies else favoriteSeries
        val next=if(id in values)values-id else values+id
        onFavorite(kind,id)
        notifyFavorite(id in next)
        if(filter=="favorites")scope.launch{withFrameNanos{};headerFocus.requestFocus()}
    }
    fun toggleEpisode(series:Series,episode:SeriesEpisodeInfo){
        val key="${series.id}\u0000${episode.id}"
        favoriteEpisodes=if(favoriteEpisodes.any{it.key==key})favoriteEpisodes.filterNot{it.key==key}
            else favoriteEpisodes+PlayerFavoriteEpisode(series.id,episode.id,series.name,episode.title,episode.seasonNumber,episode.episodeNumber,episode.thumbnailUrl,series.posterUrl)
        val snapshot=favoriteEpisodes
        episodeStore.save(snapshot)
        notifyFavorite(snapshot.any{it.key==key})
    }
    fun playSavedEpisode(series:Series,id:String,number:Int?,seasonNumber:Int?,position:Long?){
        episodeRequest++;val request=episodeRequest;episodeJob?.cancel();loadingEpisodes=true;error=null
        episodeJob=scope.launch {
            try {
                val loaded=loadEpisodes(series.id)
                if(request!=episodeRequest)return@launch
                val target=loaded?.episodes?.firstOrNull{it.id==id}
                    ?:loaded?.episodes?.filter{it.seasonNumber==seasonNumber && it.episodeNumber==number}?.singleOrNull()
                if(target==null){openSeries(series)}
                else onSelect(PlayerCatalogSelection(series=series,episode=target,episodes=loaded,startPositionMs=position))
            }catch(cancelled:CancellationException){throw cancelled}
            catch(_:Exception){if(request==episodeRequest)error="Não foi possível abrir o episódio."}
            finally{if(request==episodeRequest)loadingEpisodes=false}
        }
    }
    LaunchedEffect(filter,history,favoriteEpisodes,access,ready){
        if(!ready)return@LaunchedEffect
        localLoading=if(filter=="continue")if(seriesMode)localSeries.isEmpty()else localMovies.isEmpty()else false
        try {
            if(filter=="continue") {
                val entries=history.filter{it.inProgress}.sortedByDescending{it.updatedAtMs}
                if(seriesMode) {
                    localSeries=br.com.flaviotorres.iptvpremium.data.history.latestHistorySeries(history).filter{it.inProgress}.mapNotNull { h->
                        repository.getSeriesById(playlistId,h.seriesId!!)?.let{it to h}
                    }
                } else localMovies=entries.filter{it.contentType==PlaybackContentType.MOVIE}.distinctBy{it.contentId}.mapNotNull{h->repository.getMovieById(playlistId,h.contentId)?.let{it to h}}
            }
            if(filter=="favorites" && seriesMode)allowedFavoriteSeries=favoriteEpisodes.map{it.seriesId}.distinct().mapNotNull{id->repository.getSeriesById(playlistId,id)?.let{id to it}}.toMap()
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){error="Não foi possível carregar esta coleção."}
        finally{localLoading=false}
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().focusProperties{canFocus=false}.clickable(
            interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null,onClick=onClose))
        val panelWidth=(maxWidth*.34f).coerceIn(260.dp,420.dp).coerceAtMost(maxWidth)
        Column(Modifier.width(panelWidth).fillMaxHeight().background(NeutralPlayerPalette.menu)
            .onPreviewKeyEvent { e ->
                if(e.nativeKeyEvent.keyCode in listOf(android.view.KeyEvent.KEYCODE_CHANNEL_UP,android.view.KeyEvent.KEYCODE_CHANNEL_DOWN)) {
                    if(e.type==KeyEventType.KeyDown)focusManager.moveFocus(if(e.nativeKeyEvent.keyCode==android.view.KeyEvent.KEYCODE_CHANNEL_UP)FocusDirection.Up else FocusDirection.Down)
                    return@onPreviewKeyEvent true
                }
                when(e.key) {
                    Key.DirectionLeft->{if(e.type==KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount==0)back();true}
                    Key.DirectionRight->{if(e.type==KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount==0)openSynopsis();true}
                    else->false
                }
            }.focusGroup().padding(horizontal=14.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            val categoryName=when(filter){"continue"->"Continuar";"favorites"->"Favoritos";else->categories.firstOrNull {it.id==category}?.name?:"Todos"}
            Text("$type › $categoryName",color=Color.White,fontSize=17.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
            CatalogBrowserRow(if(level==PlayerCatalogLevel.ITEMS)"‹ Categorias"else"‹ Voltar", "", null,
                false,false,accent,Modifier.focusRequester(headerFocus)){back()}
            if(!ready||loadingEpisodes) {
                Text("Carregando…",color=Color.LightGray,fontSize=14.sp)
                LaunchedEffect(Unit){withFrameNanos{};runCatching{headerFocus.requestFocus()}}
            } else if(error!=null) {
                Text(error.orEmpty(),color=Color.LightGray,fontSize=14.sp)
                CatalogBrowserRow("Tentar novamente","",null,false,true,accent){
                    val target=selectedSeries
                    if(target!=null)openSeries(target)else {reload++}
                }
            } else when(level) {
                PlayerCatalogLevel.CATEGORIES -> LazyColumn(modifier=Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    item {CatalogBrowserRow("Continuar","",null,false,entryPending,accent,onFocused={entryPending=false}){filter="continue";level=PlayerCatalogLevel.ITEMS}}
                    item {CatalogBrowserRow("Favoritos","",null,false,false,accent){filter="favorites";level=PlayerCatalogLevel.ITEMS}}
                    item {CatalogBrowserRow("Todos","",null,false,false,accent){filter="category";category=null;level=PlayerCatalogLevel.ITEMS}}
                    items(categories,key={it.id}) { row ->CatalogBrowserRow(row.name,"",null,false,false,accent){filter="category";category=row.id;level=PlayerCatalogLevel.ITEMS} }
                }
                PlayerCatalogLevel.ITEMS -> {
                    if(filter=="continue") {
                        if(localLoading)Text("Carregando histórico…",color=Color.LightGray,fontSize=13.sp)
                        else if(if(seriesMode)localSeries.isEmpty()else localMovies.isEmpty()) {
                            Text("Nenhum título para continuar.",color=Color.LightGray,fontSize=13.sp)
                            LaunchedEffect(Unit){withFrameNanos{};headerFocus.requestFocus()}
                        }
                        LazyColumn(modifier=Modifier.weight(1f),state=coverListState,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(seriesMode)items(localSeries,key={it.first.id}){(row,h)->
                                CatalogBrowserRow(row.name,"T${h.seasonNumber?:1} · E${h.episodeNumber?:1}",row.posterUrl,false,coverEntry(row.id,row.id==localSeries.firstOrNull()?.first?.id),accent,
                                    kind=PosterKind.SERIES,activeCover=synopsisCover?.id==row.id,onCoverAnchor=coverAnchor(row.id,row.name,row.year,synopsisLoader(row.id,true,row.plot)),artworkYear=row.year,isPlaying=row.id==media.seriesId,favorite=row.id in favoriteSeries,
                                    onFavorite={toggle("series",row.id)},onFocused={entryPending=false;returnCoverId=null}){
                                    if(h.seriesOnly)openSeries(row)else playSavedEpisode(row,h.contentId,h.episodeNumber,h.seasonNumber,h.positionMs)
                                }
                            }else items(localMovies,key={it.first.id}){(row,h)->
                                CatalogBrowserRow(row.name,"Continuar assistindo",row.posterUrl,false,entryPending&&row.id==localMovies.firstOrNull()?.first?.id,accent,
                                    kind=PosterKind.MOVIES,activeCover=synopsisCover?.id==row.id,onCoverAnchor=coverAnchor(row.id,row.name,row.year,synopsisLoader(row.id)),artworkYear=row.year,isPlaying=row.id==media.id,favorite=row.id in favoriteMovies,
                                    onFavorite={toggle("movies",row.id)},onFocused={entryPending=false;returnCoverId=null}){onSelect(PlayerCatalogSelection(movie=row,startPositionMs=h.positionMs))}
                            }
                        }
                    } else if(seriesMode) {
                        val activeFavorites=if(filter=="favorites")favoriteSeries else emptySet()
                        val flow=remember(playlistId,category,filter,activeFavorites){repository.series(playlistId,categoryId=if(filter=="favorites")null else category,favoritesOnly=filter=="favorites",favoriteSeriesIds=activeFavorites)}
                        val paging=flow.collectAsLazyPagingItems()
                        val pinned=currentSeries?.takeIf {filter!="favorites" && (category==null||it.categoryId==category)}
                        LazyColumn(modifier=Modifier.weight(1f),state=coverListState,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(pinned!=null)item(key="playing:${pinned.id}") {
                                CatalogBrowserRow(pinned.name,"Em reprodução",pinned.posterUrl,false,coverEntry(pinned.id,true),accent,kind=PosterKind.SERIES,activeCover=synopsisCover?.id==pinned.id,onCoverAnchor=coverAnchor(pinned.id,pinned.name,pinned.year,synopsisLoader(pinned.id,true,pinned.plot)),artworkYear=pinned.year,isPlaying=pinned.id==media.seriesId,favorite=pinned.id in favoriteSeries,onFavorite={toggle("series",pinned.id)},onFocused={entryPending=false;returnCoverId=null}){openSeries(pinned)}
                            }
                            items(paging.itemCount,key={"series:"+(paging.peek(it)?.id?:"loading:$it")}) { i ->
                                val row=paging[i]
                                if(row!=null&&row.id!=pinned?.id)CatalogBrowserRow(row.name,row.year?.toString().orEmpty(),row.posterUrl,false,coverEntry(row.id,pinned==null&&i==0),accent,kind=PosterKind.SERIES,activeCover=synopsisCover?.id==row.id,onCoverAnchor=coverAnchor(row.id,row.name,row.year,synopsisLoader(row.id,true,row.plot)),artworkYear=row.year,isPlaying=row.id==media.seriesId,favorite=row.id in favoriteSeries,onFavorite={toggle("series",row.id)},onFocused={entryPending=false;returnCoverId=null}){openSeries(row)}
                            }
                            if(filter=="favorites")items(favoriteEpisodes.filter{it.seriesId in allowedFavoriteSeries},key={"episode:"+it.key}){f->
                                val owner=allowedFavoriteSeries[f.seriesId]!!
                                CatalogBrowserRow("${owner.name} · E${f.number}","Temporada ${f.season}",f.image,true,entryPending&&paging.itemCount==0&&f.key==favoriteEpisodes.firstOrNull{it.seriesId in allowedFavoriteSeries}?.key,accent,onFocused={entryPending=false;returnCoverId=null},
                                    favorite=true,isPlaying=f.episodeId==media.id&&f.seriesId==media.seriesId,
                                    onFavorite={favoriteEpisodes=favoriteEpisodes.filterNot{it.key==f.key};val snapshot=favoriteEpisodes;episodeStore.save(snapshot);scope.launch{withFrameNanos{};headerFocus.requestFocus()}}){playSavedEpisode(owner,f.episodeId,f.number,f.season,null)}
                            }
                            item { CatalogPagingStatus(if(paging.loadState.refresh is LoadState.NotLoading)paging.loadState.append else paging.loadState.refresh,paging.itemCount+(if(filter=="favorites")favoriteEpisodes.count{it.seriesId in allowedFavoriteSeries}else 0),{paging.retry()},headerFocus) }
                        }
                    } else {
                        val activeFavorites=if(filter=="favorites")favoriteMovies else emptySet()
                        val flow=remember(playlistId,category,filter,activeFavorites){repository.movies(playlistId,categoryId=if(filter=="favorites")null else category,favoritesOnly=filter=="favorites",favoriteMovieIds=activeFavorites)}
                        val paging=flow.collectAsLazyPagingItems()
                        val pinned=currentMovie?.takeIf {filter!="favorites" && (category==null||it.categoryId==category)}
                        LazyColumn(modifier=Modifier.weight(1f),state=coverListState,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(pinned!=null)item(key="playing:${pinned.id}") {
                                CatalogBrowserRow(pinned.name,"Em reprodução",pinned.posterUrl,false,entryPending,accent,kind=PosterKind.MOVIES,activeCover=synopsisCover?.id==pinned.id,onCoverAnchor=coverAnchor(pinned.id,pinned.name,pinned.year,synopsisLoader(pinned.id)),artworkYear=pinned.year,isPlaying=pinned.id==media.id,favorite=pinned.id in favoriteMovies,onFavorite={toggle("movies",pinned.id)},onFocused={entryPending=false;returnCoverId=null}){onSelect(PlayerCatalogSelection(movie=pinned))}
                            }
                            items(paging.itemCount,key={"movie:"+(paging.peek(it)?.id?:"loading:$it")}) { i ->
                                val row=paging[i]
                                if(row!=null&&row.id!=pinned?.id)CatalogBrowserRow(row.name,row.year?.toString().orEmpty(),row.posterUrl,false,pinned==null&&i==0&&entryPending,accent,kind=PosterKind.MOVIES,activeCover=synopsisCover?.id==row.id,onCoverAnchor=coverAnchor(row.id,row.name,row.year,synopsisLoader(row.id)),artworkYear=row.year,isPlaying=row.id==media.id,favorite=row.id in favoriteMovies,onFavorite={toggle("movies",row.id)},onFocused={entryPending=false;returnCoverId=null}){onSelect(PlayerCatalogSelection(movie=row))}
                            }
                            item { CatalogPagingStatus(if(paging.loadState.refresh is LoadState.NotLoading)paging.loadState.append else paging.loadState.refresh,paging.itemCount,{paging.retry()},headerFocus) }
                        }
                    }
                }
                PlayerCatalogLevel.SEASONS -> {
                    Text(selectedSeries?.name.orEmpty(),color=Color.White,fontSize=15.sp,maxLines=2)
                    val seasons=remember(result){result?.episodes.orEmpty().groupBy{it.seasonNumber}.toSortedMap()}
                    val entrySeason=season.takeIf {it in seasons}?:seasons.keys.firstOrNull{it>0}?:seasons.keys.firstOrNull()
                    val state=rememberLazyListState(seasons.keys.indexOf(entrySeason).coerceAtLeast(0))
                    LazyColumn(modifier=Modifier.weight(1f),state=state,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        items(seasons.keys.toList(),key={it}) { number ->
                            CatalogBrowserRow(if(number==0)"Especiais"else"Temporada $number","${seasons[number]?.size?:0} episódios",(result?.seasons?.firstOrNull{it.number==number}?.coverUrl?.takeIf{it.startsWith("http")}?:seasonPosters[number]?:selectedSeries?.posterUrl),false,entryPending&&number==entrySeason,accent,isPlaying=selectedSeries?.id==media.seriesId&&number==media.seasonNumber,onFocused={entryPending=false},
                                images=playerSeasonPosterCandidates(seasonPosters[number],result?.seasons?.firstOrNull{it.number==number}?.coverUrl,
                                    br.com.flaviotorres.iptvpremium.data.repository.seriesArtworkImage("w500",seriesImages.root?.optString("poster_path")),selectedSeries?.posterUrl)){
                                season=number;level=PlayerCatalogLevel.EPISODES
                            }
                        }
                    }
                }
                PlayerCatalogLevel.EPISODES -> {
                    Text("${selectedSeries?.name.orEmpty()} · Temporada $season",color=Color.LightGray,fontSize=13.sp,maxLines=2)
                    val episodes=remember(result,season){result?.episodes.orEmpty().filter{it.seasonNumber==season}.sortedBy{it.episodeNumber}}
                    val entryEpisode=media.id.takeIf {selectedSeries?.id==media.seriesId && episodes.any{it.id==media.id}}
                        ?:episodes.firstOrNull()?.id
                    val state=rememberLazyListState(episodes.indexOfFirst{it.id==entryEpisode}.coerceAtLeast(0))
                    LazyColumn(modifier=Modifier.weight(1f),state=state,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        items(episodes,key={it.id}) { row ->
                            CatalogBrowserRow("E${row.episodeNumber.toString().padStart(2,'0')} · ${row.title}","",row.thumbnailUrl,true,entryPending&&row.id==entryEpisode,accent,isPlaying=selectedSeries?.id==media.seriesId&&row.id==media.id,favorite=favoriteEpisodes.any{it.seriesId==selectedSeries?.id&&it.episodeId==row.id},onFavorite={selectedSeries?.let{toggleEpisode(it,row)}},onFocused={entryPending=false},
                                images=br.com.flaviotorres.iptvpremium.data.repository.episodeReferenceImages(seriesImages.stills[row.episodeNumber],row.thumbnailUrl,seriesImages.supplemental.episodeStills[row.episodeNumber]),
                                fallback=seriesImages.backdrop,onPhotoResolved={refs,ok->if(!ok)seriesImages.failures[row.id]=refs}){
                                onSelect(PlayerCatalogSelection(series=selectedSeries,episode=row,episodes=result))
                            }
                        }
                    }
                }
            }
            Text("Segure OK por 2s para favoritar",color=Color.LightGray,fontSize=11.sp)
        }
        val density=LocalDensity.current
        val cover=focusedCover
        if(level==PlayerCatalogLevel.ITEMS && synopsisCover==null && cover!=null) {
            val y=with(density){cover.center.toDp()}-20.dp
            Box(Modifier.offset(x=panelWidth,y=y.coerceIn(8.dp,(maxHeight-48.dp).coerceAtLeast(8.dp)))
                .height(40.dp).background(NeutralPlayerPalette.menu,RoundedCornerShape(topEnd=6.dp,bottomEnd=6.dp))
                .border(1.dp,accent,RoundedCornerShape(topEnd=6.dp,bottomEnd=6.dp))
                .focusProperties{canFocus=false}.clickable{openSynopsis()}.padding(horizontal=16.dp),contentAlignment=Alignment.Center) {
                Text("Sinopse ›",color=accent,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
            }
        }
        synopsisCover?.let{reading->
            val readerFocus=remember(reading.id){FocusRequester()}
            val scroll=rememberScrollState()
            var plot by remember(reading.id){mutableStateOf<String?>(null)}
            var loading by remember(reading.id){mutableStateOf(true)}
            LaunchedEffect(reading.id){
                scroll.scrollTo(0);readerFocus.requestFocus()
                try{plot=withTimeoutOrNull(8_000){withContext(Dispatchers.IO){reading.loader()}}}
                catch(cancelled:CancellationException){throw cancelled}
                catch(_:Exception){plot=null}
                finally{loading=false}
            }
            val readerWidth=(maxWidth-panelWidth).coerceAtMost(420.dp)
            Column(Modifier.offset(x=panelWidth).width(readerWidth).fillMaxHeight().background(NeutralPlayerPalette.menu)
                .focusRequester(readerFocus).onPreviewKeyEvent{event->
                    when(event.key){
                        Key.DirectionLeft,Key.Back->{if(event.type==KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount==0)back();true}
                        Key.DirectionUp,Key.DirectionDown->{if(event.type==KeyEventType.KeyDown)scope.launch{scroll.scrollBy(if(event.key==Key.DirectionDown)100f else -100f)};true}
                        else->false
                    }
                }.focusable().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Text("SINOPSE",color=accent,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                Text(reading.title,color=Color.White,fontSize=21.sp,fontWeight=FontWeight.SemiBold)
                reading.year?.let{Text(it.toString(),color=Color.LightGray,fontSize=13.sp)}
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
                    Text(if(loading)"Carregando sinopse…"else plot?:"Sinopse indisponível.",color=Color.White,fontSize=15.sp,lineHeight=22.sp)
                }
                Text("↑ ↓ Rolar sinopse",color=Color.LightGray,fontSize=12.sp)
                Text("‹ Voltar ao título",color=accent,fontSize=14.sp,modifier=Modifier.clickable{back()})
            }
        }
        br.com.flaviotorres.iptvpremium.ui.components.PlaybackMenuClock(Modifier.align(Alignment.TopEnd).padding(22.dp))
    }
}

@Composable
private fun CatalogPagingStatus(state:LoadState,count:Int,retry:()->Unit,header:FocusRequester) {
    if(state is LoadState.Loading)Text("Carregando…",color=Color.LightGray,fontSize=13.sp)
    if(state is LoadState.Error)CatalogBrowserRow("Tentar novamente","",null,false,count==0,
        playerThemeAccent(),onClick=retry)
    else if(state !is LoadState.Loading && count==0) {
        Text("Nenhum título nesta categoria.",color=Color.LightGray,fontSize=13.sp)
        LaunchedEffect(Unit){withFrameNanos{};runCatching{header.requestFocus()}}
    }
}

@Composable
private fun CatalogBrowserRow(title:String,subtitle:String,url:String?,landscape:Boolean,entry:Boolean,
    accent:Color,modifier:Modifier=Modifier,kind:PosterKind?=null,artworkYear:Int?=null,isPlaying:Boolean=false,favorite:Boolean=false,onFavorite:(()->Unit)?=null,onFocused:()->Unit={},
    activeCover:Boolean=false,onCoverAnchor:((FocusRequester?,Float)->Unit)?=null,
    images:List<String>?=null,fallback:ImageBitmap?=null,onPhotoResolved:(List<String>,Boolean)->Unit={_,_->},onClick:()->Unit) {
    val focus=remember {FocusRequester()}
    var focused by remember {mutableStateOf(false)}
    val view=LocalView.current
    val latestAnchor by rememberUpdatedState(onCoverAnchor)
    var center by remember{mutableFloatStateOf(0f)}
    LaunchedEffect(focused,center){latestAnchor?.invoke(if(focused)focus else null,center)}
    DisposableEffect(Unit){onDispose{latestAnchor?.invoke(null,center)}}
    val click by rememberUpdatedState(onClick)
    val favoriteAction by rememberUpdatedState(onFavorite)
    var held by remember{mutableStateOf(false)}
    val press=remember{PlayerCatalogPress()}
    var pointerTriggered by remember{mutableStateOf(false)}
    DisposableEffect(view){
        val tree=view.viewTreeObserver
        val listener=android.view.ViewTreeObserver.OnWindowFocusChangeListener{if(!it){held=false;press.cancel()}}
        tree.addOnWindowFocusChangeListener(listener)
        onDispose{if(tree.isAlive)tree.removeOnWindowFocusChangeListener(listener)}
    }
    LaunchedEffect(held){if(held && favoriteAction!=null){delay(PLAYER_FAVORITE_HOLD_MS)
        if(focused && view.hasWindowFocus() && press.hold(android.os.SystemClock.elapsedRealtime(),true)==PlayerPressResult.FAVORITE)favoriteAction?.invoke()
    }}
    LaunchedEffect(entry){if(entry)repeat(3){withFrameNanos{};if(!focused)runCatching{focus.requestFocus()}}}
    val shape=RoundedCornerShape(7.dp)
    Column(modifier.fillMaxWidth().onGloballyPositioned{center=it.boundsInRoot().center.y}.background(if(focused || activeCover)accent.copy(alpha=.22f)else Color.White.copy(alpha=.025f),shape)
        .then(if(focused || activeCover)Modifier.border(1.5.dp,accent,shape)else Modifier)
        .focusRequester(focus).onFocusChanged{focused=it.isFocused;if(it.isFocused)onFocused()else {held=false;press.cancel()}}
        .onPreviewKeyEvent{e->
            if(e.nativeKeyEvent.keyCode !in listOf(android.view.KeyEvent.KEYCODE_DPAD_CENTER,android.view.KeyEvent.KEYCODE_ENTER,android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,android.view.KeyEvent.KEYCODE_BUTTON_A))false else {
                val now=android.os.SystemClock.elapsedRealtime()
                if(e.type==KeyEventType.KeyDown && press.down(now))held=true
                if(e.type==KeyEventType.KeyUp){
                    when(press.up(now,favoriteAction!=null,e.nativeKeyEvent.isCanceled || !focused || !view.hasWindowFocus())) {
                        PlayerPressResult.FAVORITE->favoriteAction?.invoke()
                        PlayerPressResult.CLICK->click()
                        PlayerPressResult.NONE->Unit
                    };held=false
                };true
            }
        }.pointerInput(Unit){detectTapGestures(
            onTap={if(!pointerTriggered)click()},
            onPress={
                pointerTriggered=false;runCatching{focus.requestFocus()}
                coroutineScope {
                    val timer=launch{delay(PLAYER_FAVORITE_HOLD_MS);if(view.hasWindowFocus()&&favoriteAction!=null){pointerTriggered=true;favoriteAction?.invoke()}}
                    try{tryAwaitRelease()}finally{timer.cancel()}
                }
            })}
        .semantics{role=Role.Button;onClick{click();true};if(favoriteAction!=null)onLongClick(label="Favoritar"){favoriteAction?.invoke();true}}
        .focusable().padding(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        if(url!=null || kind!=null || landscape || isPlaying || favorite) {
            val context=LocalContext.current
            val openingBusy by br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.collectAsState()
            val density=LocalDensity.current
            val width=with(density){(if(landscape)92.dp else 58.dp).roundToPx()}
            val height=with(density){(if(landscape)52.dp else 87.dp).roundToPx()}
            val latestResolved by rememberUpdatedState(onPhotoResolved)
            val bitmap by key(url,title,artworkYear,kind) {
                produceState<ImageBitmap?>(if(images==null)MoviePosterLoader.peekBitmap(url)else null,openingBusy,images) {
                    if(!openingBusy) {
                        if(images!=null) {
                            value=null
                            br.com.flaviotorres.iptvpremium.data.repository.loadEpisodeThumbnail(images,{candidate->
                                br.com.flaviotorres.iptvpremium.ui.movies.MovieDetailsImageLoader.load(context,candidate,width,height,if(landscape)"series_episode"else"series_poster")
                                    ?.takeIf{!landscape || br.com.flaviotorres.iptvpremium.data.repository.seriesLandscapeImage(it.width,it.height)}
                            },{value=it})
                            latestResolved(images,value!=null)
                        } else if(value==null)value=if(kind==null)MoviePosterLoader.loadWithRetry(context,url,PosterKind.SERIES)
                            else br.com.flaviotorres.iptvpremium.ui.movies.loadCatalogPoster(context,url,kind,title,artworkYear,null,null)
                    }
                }
            }
            Box(Modifier.width(if(landscape)92.dp else 58.dp).aspectRatio(if(landscape)16f/9f else 2f/3f)
                .clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2E34)),contentAlignment=Alignment.Center) {
                (bitmap?:fallback)?.let {Image(it,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}

            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(title,color=Color.White,fontSize=14.sp,fontWeight=if(focused)FontWeight.SemiBold else FontWeight.Normal,maxLines=2,overflow=TextOverflow.Ellipsis)
            if(isPlaying)Text("Reproduzindo agora",color=Color.LightGray,fontSize=11.sp)
            if(subtitle.isNotBlank() && !(isPlaying && subtitle=="Em reprodução"))Text(subtitle,color=Color.LightGray,fontSize=12.sp,maxLines=1)
        }
        if(favorite)br.com.flaviotorres.iptvpremium.ui.components.ReferenceFavoriteGlyph(true,modifier=Modifier.size(18.dp))
        }

    }
}
