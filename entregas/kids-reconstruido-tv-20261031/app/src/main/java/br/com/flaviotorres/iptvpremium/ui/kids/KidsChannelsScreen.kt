package br.com.flaviotorres.iptvpremium.ui.kids

import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.domain.model.*
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.live.*
import br.com.flaviotorres.iptvpremium.ui.settings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray

private data class KidsChannelMenu(val key:String,val name:String,val count:Int,val source:String?=null)

/** Independent Kids browser. Data, history and decoder services remain shared, not the former UI. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun KidsChannelsScreen(
    catalogLoading:Boolean,catalogError:String?,onRetryCatalog:()->Unit,
    onBrowseChanged:(String,Int,Set<String>?)->Unit,playlistContent:PlaylistContent?,
    pagedChannels:LazyPagingItems<Channel>?,pagingCategories:List<Category>,totalChannelCount:Int,
    pagingCategoryCounts:Map<String?,Int>,onCategoryAccessRequested:(String?,String,()->Unit)->Unit,
    onSourceCategorySelected:(String?)->Unit,sourceKey:String?,
    onLoadChannels:(suspend (Set<String>)->List<Channel>)?,onLoadFirst:(suspend ()->Channel?)?,
    onAlternatives:((String)->Flow<LiveStreamCandidate>)?,
    onNeighbours:(suspend (String,Int,String?,Boolean,Set<String>?)->List<Channel>)?,onExit:()->Unit
) {
    val context=LocalContext.current.applicationContext
    val prefs=remember(context){context.getSharedPreferences("iptvpremium_favorites_v1",Context.MODE_PRIVATE)}
    val uiPrefs=remember(context){context.getSharedPreferences("iptvpremium_settings_v2",Context.MODE_PRIVATE)}
    val favoritesKey=remember(sourceKey){FavoriteScope.key(sourceKey.orEmpty(),"channels",true)}
    val recentKey=remember(sourceKey){liveChannelHistoryKey(sourceKey.orEmpty(),true,false)}
    val lastKey=remember(sourceKey){liveChannelHistoryKey(sourceKey.orEmpty(),true,true)}
    var favorites by remember(sourceKey){mutableStateOf(prefs.getStringSet(favoritesKey,emptySet()).orEmpty().toSet())}
    var recent by remember(sourceKey){mutableStateOf(runCatching{
        val a=JSONArray(prefs.getString(recentKey,"[]"));(0 until a.length()).map{a.getString(it)}
    }.getOrDefault(emptyList()))}
    var dual by remember{mutableStateOf(uiPrefs.getBoolean(liveNavigationPreferenceKey(true),true))}
    var menu by remember(sourceKey){mutableStateOf("all")}
    var categoryId by remember(sourceKey){mutableStateOf<String?>(null)}
    var query by remember(sourceKey){mutableStateOf("")}
    var sort by remember(sourceKey){mutableIntStateOf(0)}
    var keyboard by remember{mutableStateOf(false)}
    var cursor by remember{mutableIntStateOf(0)}
    var modeDialog by remember{mutableStateOf(false)}
    var playing by remember(sourceKey){mutableStateOf<Channel?>(null)}
    var restored by remember(sourceKey){mutableStateOf(false)}
    var retry by remember{mutableIntStateOf(0)}
    var full by remember{mutableStateOf(false)}
    var picture by remember{mutableStateOf(false)}
    var fullMenu by remember{mutableStateOf(false)}
    var fullMenuActivity by remember{mutableIntStateOf(0)}
    var area by remember{mutableStateOf(KidsChannelArea.CATEGORIES)}
    var compactPane by remember{mutableStateOf(KidsChannelArea.CATEGORIES)}
    var pendingEntry by remember{mutableStateOf(true)}
    var entrySerial by remember{mutableIntStateOf(0)}
    var previousPage by remember{mutableStateOf<Any?>(null)}
    var switching by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    val categoriesState=rememberLazyListState();val channelsState=rememberLazyListState()
    val categoryFocus=remember{FocusRequester()};val channelFocus=remember{FocusRequester()}
    val waitingFocus=remember{FocusRequester()};val retryFocus=remember{FocusRequester()};val videoFocus=remember{FocusRequester()}
    val organizeFocus=remember{FocusRequester()};val searchFocus=remember{FocusRequester()};val navigationFocus=remember{FocusRequester()}
    val fullMenuFocus=remember{FocusRequester()}
    var viewport by remember{mutableStateOf(Rect.Zero)};var origin by remember{mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)}
    val access by ParentalPolicy.accessFlow.collectAsState(initial=ParentalPolicy.access())
    val categories=if(pagedChannels!=null)pagingCategories else playlistContent?.categories.orEmpty().filter{it.type==CategoryType.LIVE_TV}
    val names=remember(categories){categories.associate{it.id to it.name}}
    fun allowed(channel:Channel)=!access.blocked(sourceKey.orEmpty(),"LIVE_TV",channel.categoryId,channel.name,names[channel.categoryId].orEmpty())
    val menus=remember(categories,pagingCategoryCounts,totalChannelCount,favorites,recent){
        listOf(KidsChannelMenu("all","Todos",totalChannelCount.takeIf{pagedChannels!=null}?:playlistContent?.channels.orEmpty().size),
            KidsChannelMenu("favorites","Favoritos",favorites.size),KidsChannelMenu("recent","Continuar assistindo",recent.size))+
            categories.distinctBy{it.id}.filter{(pagingCategoryCounts[it.id]?:if(pagedChannels==null)1 else 0)>0}
                .sortedWith(CatalogSections.categoryComparator{it.name})
                .map{KidsChannelMenu("source:${it.id}",it.name,pagingCategoryCounts[it.id]?:playlistContent?.channels.orEmpty().count{c->c.categoryId==it.id},it.id)}
    }
    val selectedMenu=menus.firstOrNull{it.key==menu}?:menus.first()
    val selectedMenuIndex=menus.indexOf(selectedMenu).coerceAtLeast(0)
    val browseIds=when(menu){"favorites"->favorites;"recent"->recent.toSet();else->null}
    var localChannels by remember(sourceKey){mutableStateOf<List<Channel>>(emptyList())}
    var loadedIds by remember(sourceKey){mutableStateOf<Set<String>?>(null)}
    val latestLoader by rememberUpdatedState(onLoadChannels)
    LaunchedEffect(sourceKey,browseIds,access){
        localChannels=if(browseIds!=null)latestLoader?.invoke(browseIds).orEmpty().filter{allowed(it)}else emptyList()
        loadedIds=browseIds
    }
    val snapshot=pagedChannels?.itemSnapshotList
    val loaded=if(browseIds!=null)localChannels else snapshot?.items?:playlistContent?.channels.orEmpty()
    val rows=remember(loaded,query,sort,categoryId,browseIds,access,menu,recent){
        val filtered=loaded.filter{allowed(it)&&(categoryId==null||it.categoryId==categoryId)&&it.name.contains(query.trim(),true)}
        when(sort){1->filtered.sortedBy{it.name.lowercase()};2->filtered.sortedByDescending{it.name.lowercase()};else->if(menu=="recent")recent.mapNotNull{id->filtered.firstOrNull{it.id==id}}else filtered}
    }
    val pagingMode=pagedChannels!=null && browseIds==null
    val busy=catalogLoading || switching || (browseIds!=null && loadedIds!=browseIds) || (pagingMode && pagedChannels?.loadState?.refresh is LoadState.Loading)
    val currentPlaying=playing
    val playingBelongs=currentPlaying!=null && allowed(currentPlaying) && currentPlaying.name.contains(query.trim(),true) &&
        (categoryId==null||currentPlaying.categoryId==categoryId) && (browseIds==null||currentPlaying.id in browseIds)
    val pinned=currentPlaying?.takeIf{playingBelongs && rows.none{row->row.id==it.id}}
    val visualRows=if(pinned!=null)listOf(pinned)+rows else rows
    val entryId=kidsChannelEntry(visualRows.map{it.id},currentPlaying?.id,playingBelongs)
    fun enter(target:KidsChannelArea){area=target
        if(target==KidsChannelArea.CATEGORIES||target==KidsChannelArea.CHANNELS)compactPane=target
        pendingEntry=true;entrySerial++}
    fun waitForPage(){if(pagingMode){previousPage=pagedChannels?.itemSnapshotList;switching=true}}
    fun choose(row:KidsChannelMenu){
        onCategoryAccessRequested(row.source,row.name){
            if(menu!=row.key){waitForPage();menu=row.key;categoryId=row.source;onSourceCategorySelected(row.source)}
            enter(KidsChannelArea.CHANNELS)
        }
    }
    fun returnPlaying(){
        keyboard=false;query=""
        val channel=playing
        if(channel!=null){
            val target=menus.firstOrNull{it.source!=null&&it.source==channel.categoryId}?:menus.first()
            if(menu!=target.key){waitForPage();menu=target.key;categoryId=target.source;onSourceCategorySelected(target.source)}
            enter(KidsChannelArea.CHANNELS)
        }else enter(KidsChannelArea.CATEGORIES)
    }
    fun toggleFavorite(id:String){favorites=if(id in favorites)favorites-id else favorites+id
        prefs.edit().putStringSet(favoritesKey,favorites.toHashSet()).apply()}
    fun confirm(channel:Channel){
        if(!allowed(channel))return
        if(playing?.id==channel.id){full=true;fullMenu=false}else{playing=channel;area=KidsChannelArea.CHANNELS}
    }
    val player=remember(context){
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(
            DefaultHttpDataSource.Factory().setConnectTimeoutMs(7000).setReadTimeoutMs(7000).setAllowCrossProtocolRedirects(true)))
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(1500,12000,500,1200)
                .setTargetBufferBytes(br.com.flaviotorres.iptvpremium.data.cache.CatalogMemoryPolicy.playerBufferBytes).build())
            .setAudioAttributes(AudioAttributes.DEFAULT,true).setHandleAudioBecomingNoisy(true).build()
    }
    val connection=remember(player){LiveChannelConnection(player)}
    val connectionState by connection.state.collectAsState()
    val latestConnection by rememberUpdatedState(connectionState)
    val latestAlternatives by rememberUpdatedState(onAlternatives)
    val lifecycle=LocalLifecycleOwner.current
    var layout by remember{mutableStateOf(readLiveVideoLayout(prefs))}
    DisposableEffect(player,lifecycle){
        val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_STOP->player.pause();Lifecycle.Event.ON_START->if(latestConnection.playing)player.play();else->Unit}}
        lifecycle.lifecycle.addObserver(observer)
        onDispose{lifecycle.lifecycle.removeObserver(observer);player.release()}
    }
    LaunchedEffect(sourceKey,playing?.id,playing?.streamUrl,retry,lifecycle,access){
        val channel=playing?:return@LaunchedEffect
        if(!allowed(channel))return@LaunchedEffect
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
            connection.connect(LiveStreamCandidate(sourceKey.orEmpty(),channel),{latestAlternatives?.invoke(channel.id)?:emptyFlow()}){
                recent=(listOf(channel.id)+recent.filterNot{it==channel.id}).take(32)
                if(!sourceKey.isNullOrBlank())prefs.edit().putString(lastKey,channel.id).putString(recentKey,JSONArray(recent).toString()).apply()
            }
        }
    }
    LaunchedEffect(sourceKey,catalogLoading,pagedChannels?.itemCount,access){
        if(restored||catalogLoading||catalogError!=null)return@LaunchedEffect
        val ids=(listOfNotNull(prefs.getString(lastKey,null))+recent).distinct()
        val saved=latestLoader?.invoke(ids.toSet()).orEmpty().filter{allowed(it)}
        val previous=ids.firstNotNullOfOrNull{id->saved.firstOrNull{it.id==id}}
        val first=previous?:onLoadFirst?.invoke()?.takeIf{allowed(it)}?:rows.firstOrNull()
        if(first!=null){
            playing=first;restored=true
            if(previous!=null && first.categoryId!=null){menu="source:${first.categoryId}";categoryId=first.categoryId;onSourceCategorySelected(first.categoryId)}
            enter(KidsChannelArea.CHANNELS)
        }
    }
    LaunchedEffect(query,sort,browseIds){if(query.isNotBlank())delay(150);onBrowseChanged(query.trim(),sort,browseIds)}
    LaunchedEffect(snapshot,pagedChannels?.loadState?.refresh,switching){
        if(switching && kidsPageReady(previousPage,snapshot,pagedChannels?.loadState?.refresh is LoadState.Loading,
            pagedChannels?.loadState?.refresh is LoadState.Error,pagingMode)){switching=false;previousPage=null}
    }
    LaunchedEffect(pendingEntry,entrySerial,busy,entryId,keyboard,full){
        if(keyboard||full || (!pendingEntry && !(busy&&area==KidsChannelArea.CHANNELS)))return@LaunchedEffect
        if(busy&&area==KidsChannelArea.CHANNELS)pendingEntry=true
        when(area){
            KidsChannelArea.CATEGORIES->{categoriesState.scrollToItem(selectedMenuIndex);withFrameNanos{};categoryFocus.requestFocus();pendingEntry=false}
            KidsChannelArea.CHANNELS->{
                if(busy){withFrameNanos{};runCatching{waitingFocus.requestFocus()}}
                else if(catalogError!=null || (pagingMode&&pagedChannels?.loadState?.refresh is LoadState.Error)) {
                    withFrameNanos{};retryFocus.requestFocus();pendingEntry=false
                }
                else if(entryId!=null){channelsState.scrollToItem(visualRows.indexOfFirst{it.id==entryId}.coerceAtLeast(0));withFrameNanos{};withFrameNanos{};runCatching{channelFocus.requestFocus()};pendingEntry=false}
                else{pendingEntry=false;organizeFocus.requestFocus();area=KidsChannelArea.HEADER}
            }
            else->pendingEntry=false
        }
    }
    LaunchedEffect(full){if(full){withFrameNanos{};videoFocus.requestFocus()}}
    LaunchedEffect(fullMenu){if(fullMenu){withFrameNanos{};fullMenuFocus.requestFocus()}}
    LaunchedEffect(fullMenu,fullMenuActivity){if(fullMenu){delay(5000);fullMenu=false;withFrameNanos{};videoFocus.requestFocus()}}
    fun back():Boolean{
        when(kidsChannelBack(area,full,picture,keyboard,fullMenu,modeDialog)){
            KidsChannelBack.CLOSE_DIALOG->modeDialog=false
            KidsChannelBack.CLOSE_PICTURE->{picture=false;scope.launch{withFrameNanos{};videoFocus.requestFocus()}}
            KidsChannelBack.CLOSE_MENU->{fullMenu=false;scope.launch{withFrameNanos{};videoFocus.requestFocus()}}
            KidsChannelBack.PLAYING->{full=false;returnPlaying()}
            KidsChannelBack.CATEGORIES->enter(KidsChannelArea.CATEGORIES)
            KidsChannelBack.EXIT->onExit()
        };return true
    }
    BackHandler{back()}
    fun header(){pendingEntry=false;area=KidsChannelArea.HEADER;organizeFocus.requestFocus()}
    fun video(){if(playing!=null&&viewport.width>0){pendingEntry=false;area=KidsChannelArea.VIDEO;videoFocus.requestFocus()}}
    var zapJob by remember{mutableStateOf<Job?>(null)}
    fun zap(direction:Int){
        val channel=playing?:return
        if(zapJob?.isActive==true)return
        zapJob=scope.launch{onNeighbours?.invoke(channel.id,direction,categoryId,categoryId!=null,browseIds)?.firstOrNull{allowed(it)}?.let{playing=it}}
    }
    BoxWithConstraints(Modifier.fillMaxSize().catalogEscapeBack(::back).onGloballyPositioned{origin=it.boundsInWindow().topLeft}){
        KidsCinemaBackdrop(modifier=Modifier.matchParentSize(),catalog=true)
        val compact=maxHeight<650.dp||maxWidth<1200.dp
        val gap=if(compact)12.dp else 18.dp
        val paneWidth=(maxWidth-gap*4)*.225f
        Column(Modifier.fillMaxSize().focusProperties{canFocus=!full&&!keyboard&&!modeDialog}.padding(gap),verticalArrangement=Arrangement.spacedBy(gap)){
            Row(Modifier.fillMaxWidth().height(if(compact)76.dp else 92.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(gap)){
                KidsCinemaBrand(Modifier.width(paneWidth).fillMaxHeight())
                Row(Modifier.weight(1f).onFocusChanged{if(it.hasFocus){area=KidsChannelArea.HEADER;pendingEntry=false}}
                    .onPreviewKeyEvent{e->if(e.key==Key.DirectionDown){if(e.type==KeyEventType.KeyDown)video();true}else false},
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    KidsChannelButton(if(sort==0)"Organizar"else if(sort==1)"A–Z"else"Z–A",Modifier.focusRequester(organizeFocus)
                        .onPreviewKeyEvent{e->if(e.key==Key.DirectionLeft){if(e.type==KeyEventType.KeyDown)returnPlaying();true}else false}){
                        waitForPage();sort=(sort+1)%3
                    }
                    KidsChannelButton(if(query.isBlank())"Pesquisar canais"else query,Modifier.weight(1f).focusRequester(searchFocus)
                        .onPreviewKeyEvent{e->if(e.key==Key.DirectionLeft){if(e.type==KeyEventType.KeyDown)organizeFocus.requestFocus();true}else false}){cursor=query.length;keyboard=true}
                    KidsChannelButton("Navegação",Modifier.focusRequester(navigationFocus)
                        .onPreviewKeyEvent{e->if(e.key==Key.DirectionLeft){if(e.type==KeyEventType.KeyDown)searchFocus.requestFocus();true}else false}){modeDialog=true}
                    KidsMinuteClock(compact)
                }
            }
            Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(gap)){
                if(dual||compactPane==KidsChannelArea.CATEGORIES)Column(Modifier.width(paneWidth).fillMaxHeight()
                    .background(Color.White.copy(alpha=.91f),RoundedCornerShape(14.dp)).padding(8.dp)){
                    Text("CATEGORIAS",color=KidsCatalogLook.text,fontSize=12.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(8.dp))
                    LazyColumn(state=categoriesState,verticalArrangement=Arrangement.spacedBy(5.dp)){
                        itemsIndexed(menus,key={_,r->r.key}){i,row->
                            KidsChannelButton(row.name,Modifier.fillMaxWidth().then(if(i==selectedMenuIndex)Modifier.focusRequester(categoryFocus)else Modifier)
                                .onPreviewKeyEvent{e->when(e.key){
                                    Key.DirectionLeft->true
                                    Key.DirectionRight->{if(e.type==KeyEventType.KeyDown)enter(KidsChannelArea.CHANNELS);true}
                                    Key.DirectionUp->if(i==0){if(e.type==KeyEventType.KeyDown)header();true}else false
                                    else->false
                                }},active=row.key==menu,trailing=row.count.toString(),
                                icon=when(row.key){"all"->KidsCatalogIcon.ALL;"favorites"->KidsCatalogIcon.FAVORITE;"recent"->KidsCatalogIcon.CONTINUE;else->KidsCatalogIcon.CATEGORY},onFocus={area=KidsChannelArea.CATEGORIES;pendingEntry=false}){choose(row)}
                        }
                    }
                }
                if(dual||compactPane==KidsChannelArea.CHANNELS)Column(Modifier.width(paneWidth).fillMaxHeight()
                    .background(Color.White.copy(alpha=.91f),RoundedCornerShape(14.dp)).padding(8.dp)){
                    Text(selectedMenu.name,color=KidsCatalogLook.text,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(8.dp))
                    Box(Modifier.fillMaxWidth().weight(1f).focusRequester(waitingFocus)
                        .onFocusChanged{if(it.isFocused){area=KidsChannelArea.CHANNELS;pendingEntry=true}}
                        .focusable(enabled=busy)){
                        when{
                            catalogError!=null || (pagingMode && pagedChannels?.loadState?.refresh is LoadState.Error)->
                                KidsChannelButton("Tentar novamente",Modifier.align(Alignment.Center).focusRequester(retryFocus)){
                                    if(catalogError!=null)onRetryCatalog()else pagedChannels?.retry()
                                }
                            busy->Text("Carregando canais…",Modifier.align(Alignment.Center),color=KidsCatalogLook.secondary,fontSize=12.sp)
                            visualRows.isEmpty()->Text("Nenhum canal nesta categoria.",Modifier.align(Alignment.Center),color=KidsCatalogLook.secondary,fontSize=12.sp)
                            else->LazyColumn(state=channelsState,verticalArrangement=Arrangement.spacedBy(5.dp)){
                                itemsIndexed(visualRows,key={_,c->c.id}){i,channel->
                                    if(pagingMode){val pageIndex=snapshot?.items?.indexOfFirst{it.id==channel.id}?:-1
                                        if(pageIndex>=0 && pageIndex+(snapshot?.placeholdersBefore?:0)<(pagedChannels?.itemCount?:0))pagedChannels?.get(pageIndex+(snapshot?.placeholdersBefore?:0))}
                                    KidsChannelRow(channel.name,channel.id==playing?.id,channel.id in favorites,i+1,
                                        Modifier.fillMaxWidth().then(if(channel.id==entryId)Modifier.focusRequester(channelFocus)else Modifier)
                                            .onPreviewKeyEvent{e->when(e.key){
                                                Key.DirectionLeft->{if(e.type==KeyEventType.KeyDown)enter(KidsChannelArea.CATEGORIES);true}
                                                Key.DirectionRight->{if(e.type==KeyEventType.KeyDown)video();true}
                                                Key.DirectionUp->if(i==0){if(e.type==KeyEventType.KeyDown)header();true}else false
                                                else->false
                                            }},onFocus={area=KidsChannelArea.CHANNELS;pendingEntry=false},
                                        onFavorite={toggleFavorite(channel.id)},onClick={if(channel.id==playing?.id&&connectionState.unavailable)retry++ else confirm(channel)})
                                }
                            }
                        }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                val videoWidth=minOf(maxWidth,((maxHeight-80.dp).coerceAtLeast(1.dp))*16f/9f)
                Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Box(Modifier.width(videoWidth).aspectRatio(16f/9f).border(2.dp,KidsCatalogLook.accent,RoundedCornerShape(8.dp)).padding(2.dp)
                        .onGloballyPositioned{viewport=it.boundsInWindow()}.background(Color.Black)){
                        if(playing==null)Text("Escolha um canal",Modifier.align(Alignment.Center),color=Color.White)
                    }
                    Text("ASSISTINDO AGORA",color=KidsCatalogLook.secondary,fontSize=10.sp,fontWeight=FontWeight.SemiBold)
                    Text(playing?.name?:"Escolha um canal",color=KidsCatalogLook.text,fontSize=18.sp,fontWeight=FontWeight.Bold,maxLines=2)
                }
                }
            }
        }
        if(playing!=null&&viewport.width>0){
            val density=LocalDensity.current
            val bounds=if(full)Rect(0f,0f,constraints.maxWidth.toFloat(),constraints.maxHeight.toFloat())else viewport.translate(-origin.x,-origin.y)
            Box(Modifier.offset{IntOffset(bounds.left.toInt(),bounds.top.toInt())}.size(with(density){bounds.width.toDp()},with(density){bounds.height.toDp()})
                .clip(RoundedCornerShape(if(full)0.dp else 6.dp)).background(Color.Black).focusProperties{canFocus=!keyboard&&!modeDialog}.focusRequester(videoFocus)
                .onFocusChanged{if(it.isFocused){area=KidsChannelArea.VIDEO;pendingEntry=false}}
                .graphicsLayer{alpha=if(keyboard)0f else 1f}
                .onPreviewKeyEvent{e->
                    if(fullMenu&&e.type==KeyEventType.KeyDown)fullMenuActivity++
                    when{
                    picture||fullMenu->false
                    e.key==Key.DirectionLeft->{if(e.type==KeyEventType.KeyDown){full=false;returnPlaying()};true}
                    !full&&e.key==Key.DirectionUp->{if(e.type==KeyEventType.KeyDown)header();true}
                    full&&e.key in listOf(Key.DirectionUp,Key.DirectionDown)->{if(e.type==KeyEventType.KeyDown)zap(if(e.key==Key.DirectionUp)1 else -1);true}
                    full&&e.key==Key.DirectionRight->{if(e.type==KeyEventType.KeyDown)picture=true;true}
                    else->false
                }}.clickable{if(connectionState.unavailable){retry++;fullMenu=false}else if(full)fullMenu=!fullMenu else full=true}){
                StableLiveVideo(player,full,layout,Modifier.fillMaxSize())
                if(connectionState.buffering)Text("Conectando canal…",Modifier.align(Alignment.Center).background(Color(0xCC11151A)).padding(10.dp),color=Color.White)
                if(connectionState.unavailable)Text("Canal indisponível. Pressione OK para tentar novamente.",Modifier.align(Alignment.Center).background(Color(0xCC11151A)).padding(10.dp),color=Color.White)
                if(fullMenu&&full)Row(Modifier.align(Alignment.BottomCenter).padding(20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    KidsChannelButton("Canais",Modifier.focusRequester(fullMenuFocus)){full=false;fullMenu=false;returnPlaying()}
                    KidsChannelButton("Formato de tela"){fullMenu=false;picture=true}
                }
                if(picture)LiveImageSettings(layout,{layout=it;saveLiveVideoLayout(prefs,it)},{picture=false;scope.launch{withFrameNanos{};videoFocus.requestFocus()}},Modifier.fillMaxSize())
            }
        }
        if(keyboard)Box(Modifier.fillMaxSize().background(Color(0x66000000)).padding(12.dp),contentAlignment=Alignment.BottomCenter){
            AppKeyboard(Modifier.fillMaxWidth(),actionText="CONCLUIR",
                onCharacter={val e=tvKeyboardType(query,cursor,it);if(query!=e.value)waitForPage();query=e.value;cursor=e.cursor},
                onBackspace={val e=tvKeyboardDelete(query,cursor);if(query!=e.value)waitForPage();query=e.value;cursor=e.cursor},
                onMoveCursorLeft={cursor=tvKeyboardMoveCursor(query,cursor,-1)},onMoveCursorRight={cursor=tvKeyboardMoveCursor(query,cursor,1)},
                onAction={keyboard=false;enter(KidsChannelArea.CHANNELS)})
        }
        if(modeDialog)androidx.compose.ui.window.Dialog(onDismissRequest={modeDialog=false;scope.launch{withFrameNanos{};navigationFocus.requestFocus()}}){
            val selected=remember{FocusRequester()};LaunchedEffect(Unit){withFrameNanos{};selected.requestFocus()}
            Column(Modifier.width(320.dp).background(Color(0xFFFFFCF7),RoundedCornerShape(14.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("Navegação",color=KidsCatalogLook.text,fontSize=20.sp,fontWeight=FontWeight.Bold)
                listOf(false to "Compacto",true to "Duas colunas").forEach{(value,label)->
                    KidsChannelButton(label,Modifier.fillMaxWidth().then(if(dual==value)Modifier.focusRequester(selected)else Modifier),active=dual==value){
                        dual=value;uiPrefs.edit().putBoolean(liveNavigationPreferenceKey(true),value).apply();modeDialog=false;enter(KidsChannelArea.CHANNELS)
                    }
                }
            }
        }
    }
}

@Composable
private fun KidsChannelButton(label:String,modifier:Modifier=Modifier,active:Boolean=false,trailing:String?=null,icon:KidsCatalogIcon?=null,
    onFocus:()->Unit={},onClick:()->Unit){
    var focused by remember{mutableStateOf(false)}
    Row(modifier.height(42.dp).kidsNewSurface(focused,active,RoundedCornerShape(10.dp))
        .onFocusChanged{focused=it.isFocused;if(it.isFocused)onFocus()}.kidsNewClick(onClick).padding(horizontal=10.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        val leading=icon?:when {label=="Organizar"||label in listOf("A–Z","Z–A")->KidsCatalogIcon.SORT
            label.startsWith("Pesquisar")->KidsCatalogIcon.SEARCH;label=="Navegação"->KidsCatalogIcon.ALL;else->null}
        if(leading!=null)KidsSmallIcon(leading,true,focused||active)
        Text(label,Modifier.weight(1f,false),color=KidsControlLook.foreground(focused||active),fontSize=12.sp,
            fontWeight=if(focused||active)FontWeight.SemiBold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis)
        if(trailing!=null)Text(trailing,color=KidsControlLook.foreground(focused||active),fontSize=11.sp)
    }
}
@Composable
private fun KidsChannelRow(name:String,playing:Boolean,favorite:Boolean,number:Int,modifier:Modifier,
    onFocus:()->Unit,onFavorite:()->Unit,onClick:()->Unit){
    var focused by remember{mutableStateOf(false)};var held by remember{mutableStateOf(false)}
    var triggered by remember{mutableStateOf(false)};var started by remember{mutableLongStateOf(0)}
    val favoriteAction by rememberUpdatedState(onFavorite);val click by rememberUpdatedState(onClick)
    val view=LocalView.current
    DisposableEffect(view,held){
        if(!held)return@DisposableEffect onDispose{}
        val observer=view.viewTreeObserver
        val listener=android.view.ViewTreeObserver.OnWindowFocusChangeListener{hasFocus->if(!hasFocus)held=false}
        observer.addOnWindowFocusChangeListener(listener)
        if(!view.hasWindowFocus())held=false
        onDispose{if(observer.isAlive)observer.removeOnWindowFocusChangeListener(listener)}
    }
    LaunchedEffect(held){if(held){delay(1000);if(held&&focused&&view.hasWindowFocus()){triggered=true;favoriteAction()}}}
    Row(modifier.height(44.dp).kidsNewSurface(focused,false,RoundedCornerShape(10.dp))
        .onFocusChanged{focused=it.isFocused;if(it.isFocused)onFocus()else held=false}
        .onPreviewKeyEvent{e->if(e.key !in listOf(Key.DirectionCenter,Key.Enter,Key.NumPadEnter))false else{
            if(e.type==KeyEventType.KeyDown&&!held&&e.nativeKeyEvent.repeatCount==0){held=true;triggered=false;started=SystemClock.elapsedRealtime()}
            if(e.type==KeyEventType.KeyUp){if(held&&view.hasWindowFocus()&&!e.nativeKeyEvent.isCanceled&&!triggered){if(SystemClock.elapsedRealtime()-started>=1000)favoriteAction()else click()};held=false};true
        }}.pointerInput(Unit){detectTapGestures(onLongPress={favoriteAction()},onTap={click()})}.focusable().padding(horizontal=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        Text(if(playing)"▶"else number.toString(),color=if(focused)Color.White else KidsCatalogLook.accent,fontSize=11.sp,modifier=Modifier.width(18.dp))
        Text(name,Modifier.weight(1f),color=KidsControlLook.foreground(focused),fontSize=12.sp,fontWeight=if(playing)FontWeight.Bold else FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text(if(favorite)"♥"else"♡",color=KidsControlLook.foreground(focused),fontSize=18.sp,modifier=Modifier.focusProperties{canFocus=false}.kidsNewClick(onFavorite))
    }
}
