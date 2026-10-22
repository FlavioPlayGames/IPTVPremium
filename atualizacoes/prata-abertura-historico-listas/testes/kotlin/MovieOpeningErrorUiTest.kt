package br.com.flaviotorres.iptvpremium.core.navigation

import android.content.Context
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w640dp-h360dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MovieOpeningErrorUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun compactLightScreenShowsTheFailureAndFocusedRetryButton() {
  val context=ApplicationProvider.getApplicationContext<Context>()
  KidsAppearance.apply(false,"blue");CatalogStyle.choose(context,CatalogBackground.WHITE)
  var retried=0
  compose.mainClock.autoAdvance=false
  compose.setContent {
   androidx.compose.foundation.layout.Box(Modifier.requiredSize(640.dp,360.dp)) {
    CinematicCarouselLoadingScreen(emptyList(),"selected",false,"Houve uma falha ao conectar o filme.",{}, {retried++},{})
   }
  }
  compose.mainClock.advanceTimeBy(100)
  compose.onNodeWithText("Houve uma falha ao conectar o filme.").assertIsDisplayed()
  compose.onNodeWithText("TENTAR NOVAMENTE").assertIsDisplayed().performClick()
  compose.runOnIdle {assertEquals(1,retried)}
 }
}
