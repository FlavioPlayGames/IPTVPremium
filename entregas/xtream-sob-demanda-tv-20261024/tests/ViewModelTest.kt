package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.data.cache.dao.PlaylistCacheDao
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import br.com.flaviotorres.iptvpremium.data.repository.PlaylistCatalogRepository
import br.com.flaviotorres.iptvpremium.ui.movies.MoviesViewModel
import br.com.flaviotorres.iptvpremium.ui.series.SeriesViewModel
import androidx.paging.*
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
fun main()=runTest {
 Dispatchers.setMain(StandardTestDispatcher(testScheduler))
 try {
  for(series in listOf(false,true)) {
   val generation=MutableStateFlow<String?>("g1")
   val dao=Proxy.newProxyInstance(PlaylistCacheDao::class.java.classLoader,arrayOf(PlaylistCacheDao::class.java)){_,m,args ->
    when(m.name){
     "observeActiveGenerationId" -> generation
     "getActiveGeneration" -> PlaylistCacheGenerationEntity(args!![0] as String,generation.value!!,"ACTIVE",0)
     "hasCatalogMarker" -> true
     "hasCatalogScope" -> false
     "getCategoriesByType","getMovieCountsByCategory","getSeriesCountsByCategory" -> emptyList<Any>()
     else -> if(m.name.contains("PagingSource")) object:PagingSource<Int,Any>() {
      override suspend fun load(params:LoadParams<Int>)=LoadResult.Page<Int,Any>(emptyList(),null,null)
      override fun getRefreshKey(state:PagingState<Int,Any>):Int?=null
     } else error("Unexpected DAO ${m.name}")
    }
   } as PlaylistCacheDao
   val repo=PlaylistCatalogRepository(dao)
   val calls=mutableListOf<Triple<String?,Boolean,String?>>()
   var cancelled=0
   repo.ensureOnDemand={_,_,category,full ->
    calls+=Triple(category,full,generation.value)
    if(category==null && !full)false else try {awaitCancellation()}finally{cancelled++}
   }
   val movie=MoviesViewModel(repo);val serial=SeriesViewModel(repo)
   val store=ViewModelStore();store.put("vm",if(series)serial else movie)
   val job=backgroundScope.launch {if(series)serial.series.collect{} else movie.movies.collect{}}
   fun playlist(){if(series)serial.setPlaylist("p")else movie.setPlaylist("p")}
   fun select(category:String?){if(series)serial.selectCategory(category)else movie.selectCategory(category)}
   fun visible(v:Boolean){if(series)serial.setCatalogVisible(v)else movie.setCatalogVisible(v)}
   fun prompt()=if(series)serial.selectionRequired.value else movie.selectionRequired.value
   playlist();advanceTimeBy(300);runCurrent()
   check(calls.last().first==null && !calls.last().second && prompt())
   calls.clear();select("1");runCurrent();check(calls.size==1 && calls.last().first=="1")
   select("2");runCurrent();check(calls.size==2 && calls.last().first=="2" && cancelled==1)
   select(null);runCurrent();check(calls.size==3 && calls.last().first==null && calls.last().second && cancelled==2)
   val size=calls.size;visible(false);runCurrent();check(cancelled==3 && calls.size==size)
   visible(true);runCurrent();check(calls.size==size+1)
   generation.value="g2";runCurrent();check(calls.last().third=="g2" && cancelled==4)
   job.cancelAndJoin();store.clear();runCurrent()
   println("PASS ${if(series) "series" else "movies"}: initial prompt; atomic category/All intent; cancel on replacement and route exit; generation change reloads")
  }
 } finally {Dispatchers.resetMain()}
}
