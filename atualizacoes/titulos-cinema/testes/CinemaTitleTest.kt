package br.com.flaviotorres.iptvpremium.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.R
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w960dp-h540dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CinemaTitleTest {
 @get:Rule val compose=createComposeRule()
 private fun red(s:String)=cinemaTitleText(s).let { a -> a.spanStyles.filter {it.item.color==CinemaTitleRed}.map {a.text.substring(it.start,it.end)} }
 @Test fun partsAndQualityAreCompleteTokens() {
  assertEquals(listOf("PARTE 2"),red("A conquista da lua Parte 2"))
  assertEquals(listOf("[DV]","[HDR]","HDR+","DV","HDR"),red("Uma grande história [DV] [HDR] HDR+ DV HDR"))
  assertEquals(emptyList<String>(),red("A aventura de Dvorak"))
 }
 @Test fun twoWordsRespectAccentsAndDoNotCountQualityTags() {
  assertEquals(listOf("ESCURAS"),red("  Noites   Escuras  "))
  assertEquals(listOf("[HDR]","CORAÇÃO"),red("Meu Coração [HDR]"))
  assertEquals(emptyList<String>(),red("Uma longa história"))
  assertEquals(emptyList<String>(),red("Sozinho"))
 }
 @Test fun sharedLocalBackdropIsBoundedAndRealTitleFitsCompactScreen() {
  val context=ApplicationProvider.getApplicationContext<Context>()
  // Initialize native bitmap classes on the test UI thread before the IO decode.
  Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
  BitmapFactory.decodeResource(context.resources,R.drawable.cinema_details_fallback,
   BitmapFactory.Options().apply {inSampleSize=16})
  val first=runBlocking {CinemaBackdropCache.load(context)}!!
  assertTrue(first.width<=1280 && first.height<=720)
  assertSame(first,runBlocking {CinemaBackdropCache.load(context)})
  lateinit var view: android.view.View
  compose.setContent {
   view = LocalView.current
   Box(Modifier.requiredSize(960.dp,540.dp).background(Color.Black).testTag("preview")) {
    CinemaDetailsBackdrop(Modifier.fillMaxSize())
    Column(Modifier.padding(36.dp)) {
     CinemaDetailsTitle("A Conquista da Lua Parte 2",32.sp,Modifier.width(850.dp))
     Spacer(Modifier.height(14.dp))
     androidx.tv.material3.Text("2024  •  Documentário",color=Color(0xFFDDDDDD),fontSize=14.sp)
     Spacer(Modifier.height(12.dp))
     androidx.tv.material3.Text("Uma nova aventura para descobrir.",color=Color.White,fontSize=16.sp)
     Spacer(Modifier.height(58.dp))
     CinemaDetailsTitle("Noites Escuras",32.sp,Modifier.width(850.dp))
     Spacer(Modifier.height(30.dp))
     CinemaDetailsTitle("Uma Grande Aventura [DV] [HDR] HDR+",26.sp,Modifier.width(850.dp))
    }
   }
  }
  compose.onNodeWithText("A CONQUISTA DA LUA PARTE 2").assertIsDisplayed()
  compose.onNodeWithText("NOITES ESCURAS").assertIsDisplayed()
  compose.waitForIdle()
  compose.runOnIdle {
   val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
   view.draw(android.graphics.Canvas(image))
   val output = File(System.getProperty("cinema.preview") ?: "build/cinema-title-preview.png")
   output.parentFile?.mkdirs()
   output.outputStream().use {assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,it))}
  }
 }
}
