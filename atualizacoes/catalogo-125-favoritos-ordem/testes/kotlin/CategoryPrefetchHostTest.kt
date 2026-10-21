package br.com.flaviotorres.iptvpremium.ui.movies
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import br.com.flaviotorres.iptvpremium.data.cache.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.*
import org.junit.Assert.*

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35],qualifiers="w960dp-h540dp")
class CategoryPrefetchHostTest {
    @get:Rule val compose=createComposeRule()
    @Before fun setup(){CatalogMemoryPolicy.pressured.value=false;MediaOpeningPriority.setBusy(false)}
    @After fun cleanup(){MediaOpeningPriority.setBusy(false);CatalogMemoryPolicy.pressured.value=false}
    @Test fun scrollDoesNotPauseDownloadsAndReserveStopsAt125(){
        val reads=ConcurrentLinkedQueue<Int>();var grid:LazyGridState?=null
        val flow=flowOf(PagingData.from((0 until 400).toList(), sourceLoadStates=androidx.paging.LoadStates(androidx.paging.LoadState.NotLoading(false),androidx.paging.LoadState.NotLoading(true),androidx.paging.LoadState.NotLoading(true))))
        compose.setContent {
            val state=rememberLazyGridState();grid=state
            val paging=flow.collectAsLazyPagingItems()
            LaunchedEffect(state){state.scroll{awaitCancellation()}}
            LazyVerticalGrid(columns=GridCells.Fixed(5),state=state,modifier=Modifier.size(600.dp,240.dp)) {
                items(400){Box(Modifier.height(80.dp))}
            }
            CategoryPosterPrefetch(PosterKind.MOVIES,"test",state,paging,{null},false,{offset->
                reads+=offset;PosterPage(List(50){null},offset+50)
            })
        }
        compose.waitUntil(10000){reads.size>=3}
        compose.waitForIdle()
        assertTrue(grid!!.isScrollInProgress)
        assertTrue("Does not scan all 400 titles",reads.max()<125)
        assertEquals(3,reads.size)
    }
    @Test fun selectingMediaCancelsReaderBeforeAnotherPage(){
        val entered=AtomicBoolean();val cancelled=AtomicBoolean();val flow=flowOf(PagingData.from((0 until 400).toList(), sourceLoadStates=androidx.paging.LoadStates(androidx.paging.LoadState.NotLoading(false),androidx.paging.LoadState.NotLoading(true),androidx.paging.LoadState.NotLoading(true))))
        compose.setContent {
            val state=rememberLazyGridState();val paging=flow.collectAsLazyPagingItems()
            LazyVerticalGrid(columns=GridCells.Fixed(5),state=state,modifier=Modifier.size(600.dp,240.dp)) {
                items(400){Box(Modifier.height(80.dp))}
            }
            CategoryPosterPrefetch(PosterKind.SERIES,"test",state,paging,{null},false,{
                entered.set(true)
                try {awaitCancellation()}finally{cancelled.set(true)}
            })
        }
        compose.waitUntil(10000){entered.get()}
        compose.runOnIdle {MediaOpeningPriority.setBusy(true)}
        compose.waitUntil(5000){cancelled.get()}
    }
    @Test fun activityStopDoesNotCancelCategoryDownload(){
        val reads=ConcurrentLinkedQueue<Int>();val release=CompletableDeferred<Unit>()
        val owner=object:androidx.lifecycle.LifecycleOwner {
            val registry=androidx.lifecycle.LifecycleRegistry(this)
            override val lifecycle:androidx.lifecycle.Lifecycle get()=registry
        }
        val flow=flowOf(PagingData.from((0 until 400).toList(), sourceLoadStates=androidx.paging.LoadStates(androidx.paging.LoadState.NotLoading(false),androidx.paging.LoadState.NotLoading(true),androidx.paging.LoadState.NotLoading(true))))
        compose.runOnIdle {owner.registry.currentState=androidx.lifecycle.Lifecycle.State.RESUMED}
        compose.setContent {
            CompositionLocalProvider(androidx.lifecycle.compose.LocalLifecycleOwner provides owner) {
                val state=rememberLazyGridState();val paging=flow.collectAsLazyPagingItems()
                LazyVerticalGrid(columns=GridCells.Fixed(5),state=state,modifier=Modifier.size(600.dp,240.dp)) {
                    items(400){Box(Modifier.height(80.dp))}
                }
                CategoryPosterPrefetch(PosterKind.MOVIES,"background",state,paging,{null},false,{offset->
                    reads+=offset
                    if(reads.size==1)release.await()
                    PosterPage(List(50){null},offset+50)
                })
            }
        }
        compose.waitUntil(10000){reads.isNotEmpty()}
        compose.runOnIdle {owner.registry.currentState=androidx.lifecycle.Lifecycle.State.CREATED}
        release.complete(Unit)
        compose.waitUntil(10000){reads.size>=3}
        assertEquals(androidx.lifecycle.Lifecycle.State.CREATED,owner.lifecycle.currentState)
        assertEquals(3,reads.size)
    }

}
