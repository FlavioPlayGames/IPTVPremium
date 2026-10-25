package br.com.flaviotorres.iptvpremium.ui.live

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
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

/** One focus owner for the menu; rows never fight platform focus scrolling. */
@Composable
internal fun LiveSlimMenu(categories:List<ChannelCategory>,selectedCategory:ChannelCategory,
    channels:LazyPagingItems<Channel>?,fallbackChannels:List<PreviewChannel>,playingChannel:PreviewChannel,
    favorites:Set<String>,restorePlaying:Boolean,onCategory:(ChannelCategory)->Unit,onConfirm:(PreviewChannel)->Unit,
    onInteraction:()->Unit,onClose:()->Unit,onImageSettings:()->Unit,
    modifier:Modifier=Modifier,channelWidth:Dp) {
    var expanded by remember { mutableStateOf(false) }
    var cursor by remember(selectedCategory.id) { mutableIntStateOf(0) }
    var categoryCursor by remember { mutableIntStateOf(categories.indexOfFirst { it.id==selectedCategory.id }.coerceAtLeast(0)) }
    var activity by remember { mutableIntStateOf(0) }
    var hasFocus by remember { mutableStateOf(false) }
    val menuFocus=remember { FocusRequester() }
    val channelState=rememberLazyListState()
    val categoryState=rememberLazyListState()
    val latestConfirm by rememberUpdatedState(onConfirm)
    val loaded=channels?.itemSnapshotList
    val savedEntry=remember(selectedCategory.id,restorePlaying) { playingChannel.takeIf {
        restorePlaying && (selectedCategory.kind!=ChannelCategoryKind.SOURCE || it.categoryId==selectedCategory.sourceCategoryId) &&
        (selectedCategory.kind!=ChannelCategoryKind.FAVORITES || it.id in favorites) &&
        loaded?.items?.none { row->row.id==it.id } == true
    } }
    val sourceCount=loaded?.size?:fallbackChannels.size
    val duplicate=loaded?.items?.indexOfFirst { it.id==savedEntry?.id }?.takeIf { it>=0 }
        ?.plus(loaded.placeholdersBefore) ?: -1
    val rows=LiveMenuRows(sourceCount,savedEntry!=null,duplicate)
    val count=rows.count
    val loading=channels?.loadState?.refresh is LoadState.Loading
    fun at(index:Int):PreviewChannel? {
        if(index==0&&savedEntry!=null)return savedEntry
        val i=rows.sourceIndex(index)?:return null
        if(channels!=null) {
            val current=channels.itemSnapshotList
            if(i !in current.indices || loaded?.getOrNull(i)?.id != current.getOrNull(i)?.id)return null
            return channels[i]?.let { PreviewChannel(it.id,i+1,it.name,it.categoryId,it.logoUrl,it.streamUrl) }
        }
        return fallbackChannels.getOrNull(i)
    }
    fun peekId(index:Int):String? {
        if(index==0&&savedEntry!=null)return savedEntry.id
        val i=rows.sourceIndex(index)?:return null
        return if(channels!=null)loaded?.getOrNull(i)?.id else fallbackChannels.getOrNull(i)?.id
    }
    fun touch(){activity++;onInteraction()}
    fun openCategories(){expanded=true;categoryCursor=categories.indexOfFirst{it.id==selectedCategory.id}.coerceAtLeast(0);touch()}
    fun closeCategories(){expanded=false;touch()}
    BackHandler(enabled=expanded) { closeCategories() }
    LaunchedEffect(Unit) { withFrameNanos{};menuFocus.requestFocus() }
    LaunchedEffect(selectedCategory.id,count>0,loading) {
        if(count>0&&!loading) {
            val found=loaded?.items?.indexOfFirst{it.id==playingChannel.id}?.takeIf { it>=0 }
                ?.plus(loaded.placeholdersBefore) ?: fallbackChannels.indexOfFirst{it.id==playingChannel.id}
            cursor=if(savedEntry!=null)0 else rows.visualIndex(found)?:0
        }
    }
    LaunchedEffect(cursor,count,expanded) {
        if(!expanded && count>0)channelState.revealRow(cursor.coerceIn(0,count-1))
    }
    LaunchedEffect(categoryCursor,expanded) {
        if(expanded && categories.isNotEmpty())categoryState.revealRow(categoryCursor.coerceIn(categories.indices))
    }
    val candidate=if(!expanded && hasFocus && cursor in 0 until count)at(cursor) else null
    LaunchedEffect(candidate?.id,expanded,activity,loading,hasFocus) {
        val channel=candidate
        if(channel!=null&&!expanded&&!loading&&hasFocus) {
            delay(3_000L)
            if(channel.id!=playingChannel.id)latestConfirm(channel)
        }
    }
    Row(modifier.focusRequester(menuFocus).onFocusChanged { hasFocus=it.isFocused }
        .onPreviewKeyEvent { event ->
            val navigation=event.key in listOf(Key.DirectionLeft,Key.DirectionRight,Key.DirectionUp,Key.DirectionDown,
                Key.DirectionCenter,Key.Enter,Key.NumPadEnter)
            if(!navigation)return@onPreviewKeyEvent false
            if(event.type!=KeyEventType.KeyDown)return@onPreviewKeyEvent true
            touch()
            when(event.key) {
                Key.DirectionLeft -> openCategories()
                Key.DirectionRight -> if(expanded)closeCategories() else onImageSettings()
                Key.DirectionUp,Key.DirectionDown -> {
                    val direction=if(event.key==Key.DirectionUp)-1 else 1
                    if(expanded)categoryCursor=liveMenuNextIndex(categoryCursor,direction,categories.size)
                    else cursor=liveMenuNextIndex(cursor,direction,count)
                }
                else -> if(expanded)categories.getOrNull(categoryCursor)?.let { onCategory(it);closeCategories() }
                    else at(cursor)?.let { latestConfirm(it) }
            }
            true
        }.focusable(),verticalAlignment=Alignment.CenterVertically) {
        AnimatedVisibility(expanded,enter=expandHorizontally(tween(180),expandFrom=Alignment.End),
            exit=shrinkHorizontally(tween(180),shrinkTowards=Alignment.End)) {
            Column(Modifier.width(channelWidth*.63f).fillMaxHeight().slimPanel().padding(10.dp).clipToBounds()) {
                SlimLabel("Categoria")
                LazyColumn(Modifier.weight(1f).clipToBounds(),state=categoryState,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    items(categories.size,key={categories[it].id}) { index->
                        val category=categories[index]
                        SlimRow(category.title,category.id==selectedCategory.id,false,hasFocus&&expanded&&categoryCursor==index,
                            onSelect={categoryCursor=index;onCategory(category);closeCategories()})
                    }
                }
            }
        }
        Box(Modifier.width(22.dp).height(44.dp).background(Color(0xED111316),RoundedCornerShape(topStart=7.dp,bottomStart=7.dp))
            .border(1.dp,if(expanded)CatalogStyle.accent else Color(0xFF72767C),RoundedCornerShape(topStart=7.dp,bottomStart=7.dp))
            .semantics{contentDescription="Abrir Categorias. Pressione Esquerda."}
            .focusProperties { canFocus=false }.clickable{openCategories()},contentAlignment=Alignment.Center) {
            Text("≡",color=Color(0xFFEEEEEF),fontSize=20.sp,fontFamily=FontFamily.SansSerif)
        }
        Column(Modifier.width(channelWidth).fillMaxHeight().slimPanel().padding(12.dp).clipToBounds()) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                SlimLabel("Canais",Modifier.weight(1f))
                Text("$sourceCount canais",color=Color(0xFFB4B8BE),fontSize=10.sp,fontFamily=FontFamily.SansSerif)
            }
            if(loading&&count==0)SlimLabel("Carregando canais…")
            else if(count==0)SlimLabel("Nenhum canal nesta categoria.")
            LazyColumn(Modifier.weight(1f).clipToBounds(),state=channelState,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                items(count,key={index->peekId(index)?.let { "channel_$it" }?:"placeholder_$index"}) { index ->
                    val channel=at(index)
                    SlimRow(channel?.name?:"Carregando…",channel?.id==playingChannel.id,true,hasFocus&&!expanded&&cursor==index,
                        onSelect={cursor=index;touch();channel?.let{latestConfirm(it)}},enabled=channel!=null)
                }
            }
            Text("→ Ajustar imagem",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Medium,
                modifier=Modifier.fillMaxWidth().padding(top=10.dp).focusProperties { canFocus=false }
                    .clickable(onClick=onImageSettings).padding(vertical=4.dp))
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
private fun SlimRow(title:String,selected:Boolean,showIcon:Boolean,cursor:Boolean,
    onSelect:()->Unit,enabled:Boolean=true) {
    val shape=RoundedCornerShape(5.dp)
    Row(Modifier.fillMaxWidth().height(31.dp).clipToBounds()
        .background(if(cursor)Color(0xFF343940)else Color(0xFF161A1D),shape)
        .border(if(cursor)2.dp else .7.dp,if(cursor)CatalogStyle.accent else Color(0xFF51565C),shape)
        .focusProperties { canFocus=false }.clickable(enabled=enabled,onClick=onSelect)
        .padding(horizontal=9.dp),verticalAlignment=Alignment.CenterVertically) {
        if(showIcon) {
            Canvas(Modifier.size(14.dp)) {
                val stroke=Stroke(1.1.dp.toPx());val color=Color(0xFFD6D8DA)
                drawRoundRect(color,Offset(1f,size.height*.23f),Size(size.width-2f,size.height*.66f),CornerRadius(1.5f),style=stroke)
                drawLine(color,Offset(size.width*.5f,size.height*.23f),Offset(size.width*.25f,0f),stroke.width)
                drawLine(color,Offset(size.width*.5f,size.height*.23f),Offset(size.width*.75f,0f),stroke.width)
            }
            Spacer(Modifier.width(9.dp))
        }
        Text(title,color=if(enabled)Color.White else Color(0xFF85898F),fontSize=11.sp,
            modifier=Modifier.weight(1f),fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Normal,
            maxLines=1,overflow=TextOverflow.Ellipsis)
        if(selected)Text(if(showIcon)"▶" else "•",color=Color(0xFFD0D4DA),fontSize=10.sp,
            modifier=Modifier.padding(start=5.dp))
    }
}

/** Keep visible rows still. Scroll only the amount needed at a viewport edge. */
private suspend fun LazyListState.revealRow(index: Int) {
    val layout = layoutInfo
    val visible = layout.visibleItemsInfo
    val row = visible.firstOrNull { it.index == index }
    if (row != null) {
        val delta=liveMenuEdgeScroll(row.offset,row.size,layout.viewportStartOffset,layout.viewportEndOffset)
        if(delta!=0f)scrollBy(delta)
    } else if (visible.isEmpty() || index < visible.first().index) {
        scrollToItem(index)
    } else {
        val height = visible.last().size
        val room = (layout.viewportEndOffset-layout.viewportStartOffset-height).coerceAtLeast(0)
        scrollToItem(index, -room)
    }
}
