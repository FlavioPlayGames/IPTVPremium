package br.com.flaviotorres.iptvpremium.ui.live

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.domain.model.Channel
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle
import kotlinx.coroutines.delay

/** Slim reference: one channel column; the category drawer expands to its left. */
@Composable
internal fun LiveSlimMenu(categories:List<ChannelCategory>,selectedCategory:ChannelCategory,
    channels:LazyPagingItems<Channel>?,fallbackChannels:List<PreviewChannel>,playingChannel:PreviewChannel,
    favorites:Set<String>,restorePlaying:Boolean,onCategory:(ChannelCategory)->Unit,onConfirm:(PreviewChannel)->Unit,
    onInteraction:()->Unit,onClose:()->Unit,modifier:Modifier=Modifier,channelWidth:Dp) {
    var expanded by remember { mutableStateOf(false) }
    val latestConfirm by rememberUpdatedState(onConfirm)
    var focusedChannel by remember { mutableStateOf<PreviewChannel?>(null) }
    var activity by remember { mutableIntStateOf(0) }
    var channelFocusTarget by remember { mutableStateOf<Int?>(null) }
    var categoryFocusTarget by remember { mutableStateOf<Int?>(null) }
    var lastChannelIndex by remember { mutableIntStateOf(0) }
    val indicatorFocus=remember { FocusRequester() }
    val channelState=rememberLazyListState()
    val categoryState=rememberLazyListState()
    val loaded=channels?.itemSnapshotList
    // A saved channel outside the loaded page is a local entry, not a reason to load preceding pages.
    val savedEntry=remember(selectedCategory.id,restorePlaying,playingChannel.id) { playingChannel.takeIf {
        restorePlaying && (selectedCategory.kind!=ChannelCategoryKind.SOURCE || it.categoryId==selectedCategory.sourceCategoryId) &&
        (selectedCategory.kind!=ChannelCategoryKind.FAVORITES || it.id in favorites) &&
        loaded?.items?.none { row->row.id==it.id } == true
    } }
    val offset=if(savedEntry!=null)1 else 0
    val count=(channels?.itemCount?:fallbackChannels.size)+offset
    val loading=channels?.loadState?.refresh is LoadState.Loading
    fun at(index:Int):PreviewChannel? {
        if(index==0&&savedEntry!=null)return savedEntry
        val i=index-offset
        if(i<0)return null
        return if(channels!=null) channels[i]?.let { PreviewChannel(it.id,i+1,it.name,it.categoryId,it.logoUrl,it.streamUrl) }
            else fallbackChannels.getOrNull(i)
    }
    fun touch(){activity++;onInteraction()}
    fun openCategories(){focusedChannel=null;expanded=true;categoryFocusTarget=categories.indexOfFirst{it.id==selectedCategory.id}.coerceAtLeast(0);touch()}
    fun closeCategories(){expanded=false;channelFocusTarget=lastChannelIndex.coerceIn(0,(count-1).coerceAtLeast(0));touch()}
    BackHandler(enabled=expanded) { closeCategories() }
    LaunchedEffect(selectedCategory.id,count>0,loading,playingChannel.id) {
        focusedChannel=null
        if(count>0&&!loading&&!expanded) {
            val found=loaded?.items?.indexOfFirst{it.id==playingChannel.id}?:fallbackChannels.indexOfFirst{it.id==playingChannel.id}
            val index=if(savedEntry!=null)0 else if(found>=0) found+(loaded?.placeholdersBefore?:0) else 0
            lastChannelIndex=index.coerceIn(0,count-1);channelFocusTarget=lastChannelIndex
        } else if(count==0&&!loading&&!expanded) {
            withFrameNanos{};indicatorFocus.requestFocus()
        }
    }
    LaunchedEffect(channelFocusTarget,count) {
        channelFocusTarget?.takeIf{it in 0 until count}?.let{channelState.scrollToItem(it)}
    }
    LaunchedEffect(categoryFocusTarget) {
        categoryFocusTarget?.takeIf{it in categories.indices}?.let{categoryState.scrollToItem(it)}
    }
    LaunchedEffect(focusedChannel?.id,expanded,activity,loading,playingChannel.id) {
        val candidate=focusedChannel
        if(candidate!=null&&!expanded&&!loading) {
            delay(3_000)
            if(candidate.id!=playingChannel.id)latestConfirm(candidate)
        }
    }
    Row(modifier.onPreviewKeyEvent { event ->
        val direction=event.key in listOf(Key.DirectionLeft,Key.DirectionRight,Key.DirectionUp,Key.DirectionDown)
        if(event.type==KeyEventType.KeyDown)touch()
        when {
            event.key==Key.DirectionLeft -> {if(event.type==KeyEventType.KeyDown)openCategories();true}
            expanded&&event.key==Key.DirectionRight -> {if(event.type==KeyEventType.KeyDown)closeCategories();true}
            !expanded&&event.key==Key.DirectionRight -> {if(event.type==KeyEventType.KeyDown)onClose();true}
            direction && event.type==KeyEventType.KeyUp -> true
            else -> false
        }
    },verticalAlignment=Alignment.CenterVertically) {
        AnimatedVisibility(expanded,enter=expandHorizontally(tween(180),expandFrom=Alignment.End),
            exit=shrinkHorizontally(tween(180),shrinkTowards=Alignment.End)) {
            Column(Modifier.width(channelWidth*.63f).fillMaxHeight().slimPanel().padding(10.dp)) {
                SlimLabel("Categoria")
                LazyColumn(Modifier.weight(1f),state=categoryState,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    items(categories.size,key={categories[it].id}) { index->
                        val category=categories[index]
                        SlimRow(category.title,category.id==selectedCategory.id,false,categoryFocusTarget==index,
                            onFocus={focusedChannel=null;categoryFocusTarget=null},
                            onArrow={direction->categoryFocusTarget=Math.floorMod(index+direction,categories.size)},
                            onSelect={onCategory(category);closeCategories()})
                    }
                }
            }
        }
        Box(Modifier.focusRequester(indicatorFocus).width(22.dp).height(44.dp).background(Color(0xED111316),RoundedCornerShape(topStart=7.dp,bottomStart=7.dp))
            .border(1.dp,if(expanded)CatalogStyle.accent else Color(0xFF72767C),RoundedCornerShape(topStart=7.dp,bottomStart=7.dp))
            .semantics{contentDescription="Abrir Categorias. Pressione Esquerda."}.clickable{openCategories()},contentAlignment=Alignment.Center) {
            Text("≡",color=Color(0xFFEEEEEF),fontSize=20.sp,fontFamily=FontFamily.SansSerif)
        }
        Column(Modifier.width(channelWidth).fillMaxHeight().slimPanel().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                SlimLabel("Canais",Modifier.weight(1f))
                Text("${(count-offset).coerceAtLeast(0)} canais",color=Color(0xFF85898F),fontSize=10.sp,fontFamily=FontFamily.SansSerif)
            }
            if(loading&&count==0)SlimLabel("Carregando canais…")
            else if(count==0)SlimLabel("Nenhum canal nesta categoria.")
            LazyColumn(Modifier.weight(1f),state=channelState,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                items(count,key={index-> if(index==0&&savedEntry!=null)"saved_${savedEntry.id}" else "channel_$index"}) { index ->
                    val channel=at(index)
                    // Avoid a duplicate visual entry when Paging subsequently reaches the saved channel.
                    if(index>=offset&&channel?.id==savedEntry?.id)return@items
                    SlimRow(channel?.name?:"Carregando…",channel?.id==playingChannel.id,true,channelFocusTarget==index,
                        onFocus={focusedChannel=channel;lastChannelIndex=index;channelFocusTarget=null},
                        onBlur={if(focusedChannel?.id==channel?.id)focusedChannel=null},
                        onArrow={direction->if(count>0) {
                            var next=Math.floorMod(index+direction,count)
                            if(savedEntry!=null&&next>=offset&&channels?.peek(next-offset)?.id==savedEntry.id)
                                next=Math.floorMod(next+direction,count)
                            channelFocusTarget=next
                        }},
                        onSelect={channel?.let{latestConfirm(it)}},enabled=channel!=null)
                }
            }
        }
    }
}
private fun Modifier.slimPanel()=background(Color(0xED0D1012),RoundedCornerShape(10.dp))
    .border(1.dp,Color(0xFF292E32),RoundedCornerShape(10.dp))
@Composable
private fun SlimLabel(text:String,modifier:Modifier=Modifier) {
    Text(text,color=Color(0xFFF2F2F3),fontSize=11.sp,fontWeight=FontWeight.Medium,fontFamily=FontFamily.SansSerif,
        modifier=modifier.padding(bottom=10.dp,top=2.dp),maxLines=1,overflow=TextOverflow.Ellipsis)
}
@Composable
private fun SlimRow(title:String,selected:Boolean,showIcon:Boolean,requestFocus:Boolean,
    onFocus:()->Unit,onBlur:()->Unit={},onArrow:(Int)->Unit,onSelect:()->Unit,enabled:Boolean=true) {
    val focus=remember{FocusRequester()}
    var focused by remember{mutableStateOf(false)}
    LaunchedEffect(requestFocus,enabled){if(requestFocus&&enabled){withFrameNanos{};focus.requestFocus()}}
    val shape=RoundedCornerShape(5.dp)
    Row(Modifier.fillMaxWidth().height(31.dp).focusRequester(focus)
        .onFocusChanged {if(focused!=it.isFocused){focused=it.isFocused;if(focused)onFocus()else onBlur()}}
        .onPreviewKeyEvent {event->
            if(event.key==Key.DirectionUp||event.key==Key.DirectionDown) {
                if(event.type==KeyEventType.KeyDown)onArrow(if(event.key==Key.DirectionUp)-1 else 1)
                true
            }else false
        }
        .background(if(focused)Color(0xFF281719)else Color(0xFF161A1D),shape)
        .border(if(focused||selected)1.3.dp else .7.dp,if(focused||selected)CatalogStyle.accent else Color(0xFF353A3E),shape)
        .clickable(enabled=enabled,onClick=onSelect).padding(horizontal=9.dp),verticalAlignment=Alignment.CenterVertically) {
        if(showIcon) {
            Canvas(Modifier.size(14.dp)) {
                val stroke=Stroke(1.1.dp.toPx());val color=Color(0xFFD6D8DA)
                drawRoundRect(color,Offset(1f,size.height*.23f),Size(size.width-2f,size.height*.66f),CornerRadius(1.5f),style=stroke)
                drawLine(color,Offset(size.width*.5f,size.height*.23f),Offset(size.width*.25f,0f),stroke.width)
                drawLine(color,Offset(size.width*.5f,size.height*.23f),Offset(size.width*.75f,0f),stroke.width)
            }
            Spacer(Modifier.width(9.dp))
        }
        Text(title,color=if(enabled)Color(0xFFF0F0F2)else Color(0xFF85898F),fontSize=11.sp,
            fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Normal,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
