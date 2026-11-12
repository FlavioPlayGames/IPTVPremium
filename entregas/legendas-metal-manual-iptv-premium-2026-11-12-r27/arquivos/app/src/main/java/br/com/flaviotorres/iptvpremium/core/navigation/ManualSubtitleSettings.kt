package br.com.flaviotorres.iptvpremium.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.core.player.*
import br.com.flaviotorres.iptvpremium.data.cache.PlaylistCacheDatabase
import br.com.flaviotorres.iptvpremium.data.m3uepisodes.DeferredM3uEpisodeAccess
import br.com.flaviotorres.iptvpremium.data.remote.DefaultPlaylistHttpClient
import br.com.flaviotorres.iptvpremium.data.repository.*
import br.com.flaviotorres.iptvpremium.data.storage.PlaylistStorage
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.kids.KidsSession
import kotlinx.coroutines.*

/** Pick from the active, parentally filtered catalogue; never send the catalogue to the site. */
@Composable
internal fun ManualSubtitleSettingsScreen(onBack:()->Unit) {
    val context=LocalContext.current.applicationContext
    val storage=remember(context){PlaylistStorage(context)}
    val selection by storage.selection.collectAsState(initial=null)
    val source=selection?.playlists?.firstOrNull{it.id==selection?.activeId}
    val access by ParentalPolicy.accessFlow.collectAsState(initial=ParentalPolicy.access())
    val kids by KidsSession.active.collectAsState()
    val dao=remember(context){PlaylistCacheDatabase.getInstance(context).playlistCacheDao()}
    val repository=remember(dao){PlaylistCatalogRepository(dao,context)}
    val scope=rememberCoroutineScope()
    var kind by rememberSaveable{mutableStateOf<String?>(null)}
    var contentId by rememberSaveable{mutableStateOf<String?>(null)}
    var title by rememberSaveable{mutableStateOf("")}
    var year by rememberSaveable{mutableStateOf<Int?>(null)}
    var tmdb by rememberSaveable{mutableStateOf<String?>(null)}
    var season by rememberSaveable{mutableStateOf<Int?>(null)}
    var episode by rememberSaveable{mutableStateOf<Int?>(null)}
    var sendSeason by rememberSaveable{mutableStateOf<Boolean?>(null)}
    var connecting by rememberSaveable{mutableStateOf(false)}
    var episodes by remember{mutableStateOf<List<SeriesEpisodeInfo>>(emptyList())}
    var loading by remember{mutableStateOf(false)}
    var failure by remember{mutableStateOf<String?>(null)}
    var query by rememberSaveable{mutableStateOf("")}
    var keyboard by remember{mutableStateOf(false)}
    var loadRevision by remember{mutableIntStateOf(0)}
    val first=remember{FocusRequester()}
    var appliedSelection by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(selection?.activeId,access) {
        val revision="${selection?.activeId}|$access"
        if(appliedSelection!=null&&appliedSelection!=revision){kind=null;contentId=null;season=null;episode=null;connecting=false;query=""}
        appliedSelection=revision
    }
    val back:()->Unit={
        when {
            keyboard->keyboard=false
            connecting->connecting=false
            episode!=null->episode=null
            season!=null->season=null
            sendSeason!=null->sendSeason=null
            contentId!=null->{contentId=null;query="";episodes=emptyList();failure=null}
            kind!=null->{kind=null;query=""}
            else->onBack()
        }
    }
    BackHandler(onBack=back)
    LaunchedEffect(kind,contentId,sendSeason,season,episode,connecting){withFrameNanos{};runCatching{first.requestFocus()}}
    LaunchedEffect(source?.id,contentId,kind,loadRevision) {
        val active=source?:return@LaunchedEffect
        val id=contentId?:return@LaunchedEffect
        if(kind!="series")return@LaunchedEffect
        loading=true;failure=null;episodes=emptyList()
        try {episodes=loadManualSeries(context,dao,active,id)}
        catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){failure="Não foi possível carregar os episódios. Tente novamente."}
        finally{loading=false}
    }
    val target=source?.let{active->contentId?.let{id->when {
        kind=="movie"->ManualSubtitleTarget("movie",active.id,id,title,year,tmdb,kids=kids)
        season!=null&&sendSeason==true->ManualSubtitleTarget("season",active.id,id,title,year,tmdb,season=season,
            episodes=episodes.filter{it.seasonNumber==season}.map{it.episodeNumber}.distinct().sorted(),kids=kids)
        season!=null&&episode!=null->ManualSubtitleTarget("episode",active.id,id,title,year,tmdb,season,episode,kids=kids)
        else->null
    }}}
    if(connecting&&target!=null) {
        ManualSubtitleUploadScreen(target,allowed={
            selection?.activeId==target.playlistId &&
                (if(target.kind=="movie")repository.getMovieById(target.playlistId,target.contentId)?.name
                else repository.getSeriesById(target.playlistId,target.contentId)?.name)==target.title
        },onBack={connecting=false})
        return
    }
    SubtitleSettingsFrame("ENVIO MANUAL",if(title.isNotBlank()&&contentId!=null)title else "Escolha o destino antes de conectar o celular ou PC.",back) {
        if(source==null){Text("Selecione uma lista com catálogo disponível para escolher o conteúdo.",color=SettingsLook.text);return@SubtitleSettingsFrame}
        when {
            kind==null->Row(Modifier.fillMaxWidth().weight(1f),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                SubtitleLandingCard("Filme","Um arquivo para o filme","SRT ou ZIP",SubtitleSettingsIcon.MOVIE,
                    Modifier.weight(1f).height(210.dp).focusRequester(first),onClick={kind="movie"})
                SubtitleLandingCard("Série","Temporada ou episódio","SRT ou ZIP",SubtitleSettingsIcon.SERIES,
                    Modifier.weight(1f).height(210.dp),onClick={kind="series"})
            }
            contentId==null->{
                SubtitleActionButton(if(query.isBlank())"Pesquisar ${if(kind=="movie")"filme"else"série"}"else "Pesquisar: $query",modifier=Modifier.focusRequester(first)){keyboard=!keyboard}
                if(keyboard) {
                    Text(query.ifBlank{"Digite o nome"},color=SettingsLook.text,fontSize=18.sp)
                    AppKeyboard(actionText="PESQUISAR",onCharacter={query=(query+it).take(100)},onBackspace={query=query.dropLast(1)},
                        currentText=query,onReplaceAll={query=it.take(100)},onClearAll={query=""},onAction={keyboard=false})
                }else ManualCatalogPicker(repository,source.id,kind!!,query,scope,access,Modifier.weight(1f)) {id,name,release,external ->
                    contentId=id;title=name;year=release;tmdb=external
                }
            }
            kind=="series"&&sendSeason==null->Row(Modifier.fillMaxWidth().weight(1f),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                SubtitleLandingCard("Temporada inteira","Escolha a temporada","Os episódios serão identificados",SubtitleSettingsIcon.SERIES,
                    Modifier.weight(1f).height(210.dp).focusRequester(first),onClick={sendSeason=true})
                SubtitleLandingCard("Episódio individual","Escolha um episódio","Sem renomear o arquivo",SubtitleSettingsIcon.MOVIE,
                    Modifier.weight(1f).height(210.dp),onClick={sendSeason=false})
            }
            kind=="series"&&loading->Text("Carregando episódios…",color=SettingsLook.secondary)
            kind=="series"&&episodes.isEmpty()->Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(failure?:"Não encontramos episódios disponíveis para esta série.",color=SettingsLook.secondary)
                SubtitleActionButton("Tentar novamente",modifier=Modifier.focusRequester(first)){loadRevision++}
            }
            kind=="series"&&season==null->LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                val seasons=episodes.map{it.seasonNumber}.distinct().sorted()
                items(seasons,key={it}) {number->SubtitleActionButton(if(number==0)"Especiais"else"Temporada $number",
                    modifier=if(number==seasons.first())Modifier.focusRequester(first)else Modifier){season=number}}
            }
            kind=="series"&&sendSeason==false&&episode==null->LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                val list=episodes.filter{it.seasonNumber==season}.distinctBy{it.episodeNumber}.sortedBy{it.episodeNumber}
                items(list,key={it.episodeNumber}) {item->SubtitleActionButton("Episódio ${item.episodeNumber} · ${item.title}",
                    modifier=if(item==list.first())Modifier.focusRequester(first)else Modifier){episode=item.episodeNumber}}
            }
            target!=null->SettingsCard(Modifier.fillMaxWidth()) {
                SettingsHeading("Destino do envio",target.description)
                Text(target.title,color=SettingsLook.text,fontSize=22.sp)
                Text("Anexe SRT ou ZIP pelo celular ou PC. Não é necessário renomear os arquivos.",color=SettingsLook.secondary,fontSize=15.sp)
                SubtitleActionButton("Conectar para enviar",modifier=Modifier.focusRequester(first)){connecting=true}
            }
        }
    }
}

@Composable
private fun ManualCatalogPicker(repository:PlaylistCatalogRepository,playlist:String,kind:String,query:String,
    scope:CoroutineScope,access:Any,modifier:Modifier,onChoose:(String,String,Int?,String?)->Unit) {
    val settled by produceState(query,query){delay(350L);value=query}
    if(kind=="movie") {
        val flow=remember(repository,playlist,settled,access){repository.movies(playlistId=playlist,searchQuery=settled)}
        val paging=flow.collectAsLazyPagingItems()
        LazyColumn(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(paging.itemCount,key={index->paging.peek(index)?.id?:"empty-$index"}){index->
                val item=paging[index]
                if(item!=null)SubtitleActionButton(item.name+item.year?.let{" · $it"}.orEmpty()){onChoose(item.id,item.name,item.year,item.tmdbId)}
            }
            item{ManualPickerStatus(paging.loadState.refresh,paging.itemCount,{paging.retry()})}
        }
    }else {
        val flow=remember(repository,playlist,settled,access){repository.series(playlistId=playlist,searchQuery=settled)}
        val paging=flow.collectAsLazyPagingItems()
        LazyColumn(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(paging.itemCount,key={index->paging.peek(index)?.id?:"empty-$index"}){index->
                val item=paging[index]
                if(item!=null)SubtitleActionButton(item.name+item.year?.let{" · $it"}.orEmpty()){onChoose(item.id,item.name,item.year,item.tmdbId)}
            }
            item{ManualPickerStatus(paging.loadState.refresh,paging.itemCount,{paging.retry()})}
        }
    }
}
@Composable
private fun ManualPickerStatus(state:LoadState,count:Int,retry:()->Unit) {
    when {state is LoadState.Loading->Text("Carregando…",color=SettingsLook.secondary)
        state is LoadState.Error->SubtitleActionButton("Tentar novamente",onClick=retry)
        count==0->Text("Nenhum resultado encontrado.",color=SettingsLook.secondary)}
}

private suspend fun loadManualSeries(context:android.content.Context,dao:br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao,
    source:PlaylistSource,id:String):List<SeriesEpisodeInfo> = withContext(Dispatchers.IO) {
    val generation=dao.getActiveGeneration(source.id)?.generationId?:return@withContext emptyList()
    if(source is PlaylistSource.M3u)DeferredM3uEpisodeAccess.initialize(context).ensure(dao,source.id,generation,id)
    val local=mutableListOf<SeriesEpisodeInfo>();var offset=0
    while(offset<5000){
        val rows=dao.getEpisodesPage(source.id,generation,id,200,offset);if(rows.isEmpty())break
        local+=rows.mapIndexed{index,item->SeriesEpisodeInfo(item.episodeId,item.seasonNumber?:1,item.episodeNumber?:offset+index+1,
            item.name,item.posterUrl,null,item.durationSeconds?.toInt(),null)}
        offset+=rows.size;if(rows.size<200)break
    }
    if(local.isNotEmpty())return@withContext local
    SeriesEpisodesRepository(DefaultPlaylistHttpClient()).load(source,id,generation)?.episodes.orEmpty()
}
