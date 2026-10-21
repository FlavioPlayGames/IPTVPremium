package br.com.flaviotorres.iptvpremium.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.ui.components.CatalogBackground
import br.com.flaviotorres.iptvpremium.ui.components.CatalogStyle
import br.com.flaviotorres.iptvpremium.ui.theme.*

@Composable
internal fun MarbleHomePreview(modifier:Modifier=Modifier) {
    val layers=rememberMarbleHomeLayers()
    if(layers!=null)MarbleLayeredHomePicture(layers,null,1f,true,modifier)
    else Box(modifier.background(CatalogStyle.background))
}

/** Separate PNG objects and native captions with the original seven accessible action targets. */
@Composable
internal fun MarbledHomeScreen(onChannels:()->Unit,onMovies:()->Unit,onSeries:()->Unit,
    onKids:()->Unit,onLists:()->Unit,onRefresh:()->Unit,onSettings:()->Unit,normalEnabled:Boolean) {
    val layers=rememberMarbleHomeLayers()
    val bitmap=layers?.backdrop
    val actions=listOf(onChannels,onMovies,onSeries,onKids,onLists,onRefresh,onSettings)
    val focus=remember{List(7){FocusRequester()}}
    var lastMain by rememberSaveable{mutableIntStateOf(MarbleHomeNavigation.initial(normalEnabled))}
    var lastFocus by rememberSaveable{mutableIntStateOf(MarbleHomeNavigation.initial(normalEnabled))}
    var focused by remember{mutableStateOf<Int?>(null)}
    val enlargement=remember{Animatable(1f)}
    LaunchedEffect(focused){enlargement.snapTo(1f);if(focused!=null)enlargement.animateTo(1.10f,tween(180))}
    val light=CatalogStyle.selected==CatalogBackground.WHITE
    LaunchedEffect(bitmap,normalEnabled) {
        if(bitmap!=null) {
            withFrameNanos{}
            val index=lastFocus.takeIf{MarbleHomeNavigation.enabled(it,normalEnabled)}?:MarbleHomeNavigation.initial(normalEnabled)
            focus[index].requestFocus()
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(if(light)Color(0xFFE7E5E2)else Color(0xFF303235)),contentAlignment=Alignment.Center) {
        if(maxWidth<600.dp && maxHeight>maxWidth) {
            MarblePortraitHome(actions,normalEnabled,layers)
            return@BoxWithConstraints
        }
        if(bitmap==null) {
            // Even if a bundled image cannot be decoded, every permitted destination remains usable.
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("IPTV PREMIUM",color=CatalogStyle.accent)
                MarbleHomeGeometry.labels.forEachIndexed{index,label->
                    if(MarbleHomeNavigation.enabled(index,normalEnabled))Text(label,color=CatalogStyle.text,
                        modifier=Modifier.clickable(onClick=actions[index]).padding(8.dp))
                }
            }
        } else {
            val aspect=MarbleHomeGeometry.width.toFloat()/MarbleHomeGeometry.height
            val width=minOf(maxWidth,maxHeight*aspect)
            val height=width/aspect
            Box(Modifier.size(width,height)) {
                MarbleLayeredHomePicture(layers!!,focused,enlargement.value,normalEnabled,Modifier.fillMaxSize())
                MarbleHomeGeometry.hitAreas.forEachIndexed{index,area->
                    val enabled=MarbleHomeNavigation.enabled(index,normalEnabled)
                    Box(Modifier.offset(width*(area.left.toFloat()/MarbleHomeGeometry.width),height*(area.top.toFloat()/MarbleHomeGeometry.height))
                        .size(width*(area.width.toFloat()/MarbleHomeGeometry.width),height*(area.height.toFloat()/MarbleHomeGeometry.height))
                        .testTag("marble-home-$index")
                        .focusRequester(focus[index])
                        .onFocusChanged{state->
                            if(state.isFocused){focused=index;lastFocus=index;if(index<5)lastMain=index}
                            else if(focused==index)focused=null
                        }
                        .onPreviewKeyEvent{event->
                            val direction=when(event.key){Key.DirectionLeft->MarbleDirection.LEFT;Key.DirectionRight->MarbleDirection.RIGHT
                                Key.DirectionUp->MarbleDirection.UP;Key.DirectionDown->MarbleDirection.DOWN;else->null}
                            if(direction==null)false else {
                                if(event.type==KeyEventType.KeyDown)focus[MarbleHomeNavigation.next(index,direction,normalEnabled,lastMain)].requestFocus()
                                true
                            }
                        }
                        .semantics{contentDescription=MarbleHomeGeometry.labels[index];role=Role.Button}
                        .clickable(enabled=enabled,interactionSource=remember{MutableInteractionSource()},indication=null,onClick=actions[index]))
                }
            }
        }
    }
}

@Composable
private fun MarblePortraitHome(actions:List<()->Unit>,normalEnabled:Boolean,layers:MarbleHomeLayers?) {
    Box(Modifier.fillMaxSize()) {
        MarbleCatalogWallpaper(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
            MarbleLogoImage(Modifier.size(220.dp,130.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                for(index in 5..6)MarblePortraitButton(index,actions[index],true,small=true,layers=layers)
            }
            listOf(listOf(0,1),listOf(2,3),listOf(4)).forEach {row->
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEach{index->MarblePortraitButton(index,actions[index],MarbleHomeNavigation.enabled(index,normalEnabled),layers=layers)}
                }
            }
        }
    }
}
@Composable
private fun MarblePortraitButton(index:Int,action:()->Unit,enabled:Boolean,small:Boolean=false,layers:MarbleHomeLayers?=null) {
    var focused by remember{mutableStateOf(false)}
    val zoom by animateFloatAsState(if(focused)1.10f else 1f,tween(180),label="portraitMarbleFocusIntensity")
    Column(Modifier.width(if(small)48.dp else 136.dp).onFocusChanged{focused=it.isFocused}
        .semantics{contentDescription=MarbleHomeGeometry.labels[index];role=Role.Button}
        .clickable(enabled=enabled,interactionSource=remember{MutableInteractionSource()},indication=null,onClick=action),
        horizontalAlignment=Alignment.CenterHorizontally) {
        if(layers!=null)MarblePngIcon(layers,index,Modifier.size(if(small)44.dp else 130.dp,if(small)44.dp else 116.dp).then(if(small)Modifier else Modifier.scale(zoom)))
        else Box(Modifier.size(if(small)44.dp else 130.dp,if(small)44.dp else 116.dp))
        if(!small)Text(MarbleHomeGeometry.labels[index].uppercase(),color=if(focused)CatalogStyle.accent else CatalogStyle.text,
            modifier=Modifier.padding(top=8.dp),fontSize=(14f*zoom).sp,textAlign=TextAlign.Center)
    }
}
