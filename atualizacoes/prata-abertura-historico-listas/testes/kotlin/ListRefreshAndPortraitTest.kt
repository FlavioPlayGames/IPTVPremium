package br.com.flaviotorres.iptvpremium.ui.playlist

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.core.storage.CacheRestartActivity
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import br.com.flaviotorres.iptvpremium.ui.movies.MissingCastPortraitCache
import br.com.flaviotorres.iptvpremium.R
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w960dp-h540dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ListRefreshAndPortraitTest {
 @get:Rule val compose=createComposeRule()
 @Test fun useAndUpdateAreSeparateActions() {
  var used=0;var refreshed=0
  val list=PlaylistSource.M3u("A","Lista A",true,"https://example.invalid/a.m3u")
  compose.setContent {PlaylistListScreen(listOf(list),activePlaylistId="A",onPlaylistClick={used++},onRefreshClick={refreshed++},onAddClick={},onDeleteClick={})}
  compose.onNodeWithText("Atualizar lista").performScrollTo().performClick()
  compose.runOnIdle {assertEquals(0,used);assertEquals(1,refreshed)}
  compose.onNodeWithText("Usar lista").performScrollTo().performClick()
  compose.runOnIdle {assertEquals(1,used);assertEquals(1,refreshed)}
 }
 @Test fun homeCacheRestartNeverSendsAPlaylistRefreshRequest() {
  val activity=Robolectric.buildActivity(Activity::class.java).setup().get()
  CacheRestartActivity.restart(activity)
  val intent=shadowOf(activity).nextStartedActivity
  assertFalse(intent.hasExtra("refresh_playlist"))
  assertEquals(CacheRestartActivity::class.java.name,intent.component?.className)
 }
 @Test fun prataPortraitIsOneLocalBoundedImageSharedAcrossCards()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  BitmapFactory.decodeResource(context.resources,R.drawable.cast_missing_prata)!!.recycle()
  val image=MissingCastPortraitCache.load(context)!!
  assertTrue(image.width<=384);assertTrue(image.height<=512)
  repeat(10){assertSame(image,MissingCastPortraitCache.load(context))}
 }
}
