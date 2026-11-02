package br.com.flaviotorres.iptvpremium.core.player

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
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

/** Carries catalog identities only; URLs are resolved again by the existing playback route. */
data class PlayerCatalogSelection(val movie: Movie? = null, val series: Series? = null,
    val episode: SeriesEpisodeInfo? = null, val episodes: SeriesEpisodesResult? = null)


/** Independent Paging collectors: never changes the outside catalog or the playing controller. */
@Composable
internal fun PlaybackCatalogBrowser(playlistId:String, media:PlaybackMedia,
    repository:PlaylistCatalogRepository, loadEpisodes:suspend (String)->SeriesEpisodesResult?,
    onSelect:(PlayerCatalogSelection)->Unit, onClose:()->Unit) {
    val seriesMode=media.type==PlaybackContentType.EPISODE
    val type=if(seriesMode)"Séries"else"Filmes"
    val accent=br.com.flaviotorres.iptvpremium.ui.theme.interfaceThemeAccent()
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
    val scope=rememberCoroutineScope()
    var episodeJob by remember { mutableStateOf<Job?>(null) }
    var episodeRequest by remember { mutableIntStateOf(0) }
    val access by br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.accessFlow.collectAsState(initial=br.com.flaviotorres.iptvpremium.ui.settings.ParentalPolicy.access())
    val headerFocus=remember { FocusRequester() }
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
    var entryPending by remember(level,category,selectedSeries?.id,season) { mutableStateOf(true) }
    fun back() {
        error=null
        when(level) {
            PlayerCatalogLevel.ITEMS -> level=PlayerCatalogLevel.CATEGORIES
            PlayerCatalogLevel.CATEGORIES -> level=PlayerCatalogLevel.ITEMS
            PlayerCatalogLevel.SEASONS -> {episodeRequest++;episodeJob?.cancel();loadingEpisodes=false;level=PlayerCatalogLevel.ITEMS;selectedSeries=null;result=null}
            PlayerCatalogLevel.EPISODES -> level=PlayerCatalogLevel.SEASONS
        }
    }
    fun openSeries(value:Series) {
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().focusProperties{canFocus=false}.clickable(
            interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null,onClick=onClose))
        val panelWidth=(maxWidth*.34f).coerceIn(260.dp,420.dp).coerceAtMost(maxWidth)
        Column(Modifier.width(panelWidth).fillMaxHeight().background(Color(0xED111417))
            .onPreviewKeyEvent { e ->
                when(e.key) {
                    Key.DirectionLeft->{if(e.type==KeyEventType.KeyDown){if(level==PlayerCatalogLevel.CATEGORIES)onClose()else back()};true}
                    Key.DirectionRight->{if(e.type==KeyEventType.KeyDown)onClose();true}
                    else->false
                }
            }.focusGroup().padding(horizontal=14.dp,vertical=18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            val categoryName=categories.firstOrNull {it.id==category}?.name?:"Todos"
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
                PlayerCatalogLevel.CATEGORIES -> LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    item {CatalogBrowserRow("Todos","",null,false,entryPending,accent,onFocused={entryPending=false}){category=null;level=PlayerCatalogLevel.ITEMS}}
                    items(categories,key={it.id}) { row ->CatalogBrowserRow(row.name,"",null,false,false,accent){category=row.id;level=PlayerCatalogLevel.ITEMS} }
                }
                PlayerCatalogLevel.ITEMS -> {
                    if(seriesMode) {
                        val flow=remember(playlistId,category){repository.series(playlistId,categoryId=category)}
                        val paging=flow.collectAsLazyPagingItems()
                        val pinned=currentSeries?.takeIf {category==null||it.categoryId==category}
                        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(pinned!=null)item(key="playing:${pinned.id}") {
                                CatalogBrowserRow(pinned.name,"Em reprodução",pinned.posterUrl,false,entryPending,accent,kind=PosterKind.SERIES,artworkYear=pinned.year,onFocused={entryPending=false}){openSeries(pinned)}
                            }
                            items(paging.itemCount,key={"series:"+(paging.peek(it)?.id?:"loading:$it")}) { i ->
                                val row=paging[i]
                                if(row!=null&&row.id!=pinned?.id)CatalogBrowserRow(row.name,row.year?.toString().orEmpty(),row.posterUrl,false,pinned==null&&i==0&&entryPending,accent,kind=PosterKind.SERIES,artworkYear=row.year,onFocused={entryPending=false}){openSeries(row)}
                            }
                            item { CatalogPagingStatus(if(paging.loadState.refresh is LoadState.NotLoading)paging.loadState.append else paging.loadState.refresh,paging.itemCount,{paging.retry()},headerFocus) }
                        }
                    } else {
                        val flow=remember(playlistId,category){repository.movies(playlistId,categoryId=category)}
                        val paging=flow.collectAsLazyPagingItems()
                        val pinned=currentMovie?.takeIf {category==null||it.categoryId==category}
                        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(pinned!=null)item(key="playing:${pinned.id}") {
                                CatalogBrowserRow(pinned.name,"Em reprodução",pinned.posterUrl,false,entryPending,accent,kind=PosterKind.MOVIES,artworkYear=pinned.year,onFocused={entryPending=false}){onSelect(PlayerCatalogSelection(movie=pinned))}
                            }
                            items(paging.itemCount,key={"movie:"+(paging.peek(it)?.id?:"loading:$it")}) { i ->
                                val row=paging[i]
                                if(row!=null&&row.id!=pinned?.id)CatalogBrowserRow(row.name,row.year?.toString().orEmpty(),row.posterUrl,false,pinned==null&&i==0&&entryPending,accent,kind=PosterKind.MOVIES,artworkYear=row.year,onFocused={entryPending=false}){onSelect(PlayerCatalogSelection(movie=row))}
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
                    LazyColumn(state=state,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        items(seasons.keys.toList(),key={it}) { number ->
                            CatalogBrowserRow(if(number==0)"Especiais"else"Temporada $number","${seasons[number]?.size?:0} episódios",selectedSeries?.posterUrl,false,entryPending&&number==entrySeason,accent,onFocused={entryPending=false}){season=number;level=PlayerCatalogLevel.EPISODES}
                        }
                    }
                }
                PlayerCatalogLevel.EPISODES -> {
                    Text("${selectedSeries?.name.orEmpty()} · Temporada $season",color=Color.LightGray,fontSize=13.sp,maxLines=2)
                    val episodes=remember(result,season){result?.episodes.orEmpty().filter{it.seasonNumber==season}.sortedBy{it.episodeNumber}}
                    val entryEpisode=media.id.takeIf {selectedSeries?.id==media.seriesId && episodes.any{it.id==media.id}}
                        ?:episodes.firstOrNull()?.id
                    val state=rememberLazyListState(episodes.indexOfFirst{it.id==entryEpisode}.coerceAtLeast(0))
                    LazyColumn(state=state,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        items(episodes,key={it.id}) { row ->
                            CatalogBrowserRow("E${row.episodeNumber.toString().padStart(2,'0')} · ${row.title}","",row.thumbnailUrl,true,entryPending&&row.id==entryEpisode,accent,onFocused={entryPending=false}){
                                onSelect(PlayerCatalogSelection(series=selectedSeries,episode=row,episodes=result))
                            }
                        }
                    }
                }
            }
        }
        br.com.flaviotorres.iptvpremium.ui.components.PlaybackMenuClock(Modifier.align(Alignment.TopEnd).padding(22.dp))
    }
}

@Composable
private fun CatalogPagingStatus(state:LoadState,count:Int,retry:()->Unit,header:FocusRequester) {
    if(state is LoadState.Loading)Text("Carregando…",color=Color.LightGray,fontSize=13.sp)
    if(state is LoadState.Error)CatalogBrowserRow("Tentar novamente","",null,false,count==0,
        br.com.flaviotorres.iptvpremium.ui.theme.interfaceThemeAccent(),onClick=retry)
    else if(state !is LoadState.Loading && count==0) {
        Text("Nenhum título nesta categoria.",color=Color.LightGray,fontSize=13.sp)
        LaunchedEffect(Unit){withFrameNanos{};runCatching{header.requestFocus()}}
    }
}

@Composable
private fun CatalogBrowserRow(title:String,subtitle:String,url:String?,landscape:Boolean,entry:Boolean,
    accent:Color,modifier:Modifier=Modifier,kind:PosterKind?=null,artworkYear:Int?=null,onFocused:()->Unit={},onClick:()->Unit) {
    val focus=remember {FocusRequester()}
    var focused by remember {mutableStateOf(false)}
    val shape=RoundedCornerShape(7.dp)
    LaunchedEffect(entry){if(entry)repeat(3){withFrameNanos{};if(!focused)runCatching{focus.requestFocus()}}}
    Row(modifier.fillMaxWidth().background(if(focused)accent.copy(alpha=.22f)else Color.White.copy(alpha=.025f),shape)
        .then(if(focused)Modifier.border(1.5.dp,accent,shape)else Modifier)
        .focusRequester(focus).onFocusChanged{focused=it.isFocused;if(it.isFocused)onFocused()}
        .clickable(onClick=onClick).padding(8.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        if(url!=null || kind!=null || landscape) {
            val context=LocalContext.current
            val openingBusy by br.com.flaviotorres.iptvpremium.data.cache.MediaOpeningPriority.busy.collectAsState()
            val bitmap by key(url,title,artworkYear,kind) {
                produceState(MoviePosterLoader.peekBitmap(url),openingBusy) {
                    if(!openingBusy && value==null) {
                        value=if(kind==null)MoviePosterLoader.loadWithRetry(context,url,PosterKind.SERIES)
                            else br.com.flaviotorres.iptvpremium.ui.movies.loadCatalogPoster(context,url,kind,title,artworkYear,null,null)
                    }
                }
            }
            Box(Modifier.width(if(landscape)92.dp else 58.dp).aspectRatio(if(landscape)16f/9f else 2f/3f)
                .clip(RoundedCornerShape(4.dp)).background(Color(0xFF2A2E34)),contentAlignment=Alignment.Center) {
                bitmap?.let {Image(it,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(title,color=Color.White,fontSize=14.sp,fontWeight=if(focused)FontWeight.SemiBold else FontWeight.Normal,maxLines=2,overflow=TextOverflow.Ellipsis)
            if(subtitle.isNotBlank())Text(subtitle,color=Color.LightGray,fontSize=12.sp,maxLines=1)
        }
    }
}
