package br.com.flaviotorres.iptvpremium.ui.kids

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toPixelMap
import br.com.flaviotorres.iptvpremium.ui.settings.NumericPinField
import br.com.flaviotorres.iptvpremium.ui.home.HomeScreen
import br.com.flaviotorres.iptvpremium.ui.theme.*
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[35],qualifiers="w960dp-h540dp")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class KidsUiHostTest {
    @get:Rule val compose=createComposeRule()
    @Test fun homeHasKidsAndNormalContentButtonsCanBeDisabled() {
        var clicked=0
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){HomeScreen({}, {}, {}, onKidsClick={clicked++}, normalEnabled=false)}}
        try { compose.waitUntil(30000){compose.onAllNodesWithText("KIDS").fetchSemanticsNodes().isNotEmpty()} }
        catch(e:Throwable) { println(compose.onRoot().printToString());throw e }
        compose.onNodeWithText("FILMES").assertIsNotEnabled()
        compose.onNodeWithText("CANAIS").assertIsNotEnabled()
        compose.onNodeWithText("SÉRIES").assertIsNotEnabled()
        compose.onNodeWithText("KIDS").assertIsEnabled().performClick()
        assertEquals(1,clicked)
    }
    @Test fun newPinKeyboardStopsAtSixDigits() {
        var value=""
        compose.setContent {var pin by remember {mutableStateOf("")};Column{NumericPinField("Novo PIN",pin,{pin=it;value=it},true)}}
        compose.onNodeWithContentDescription("Novo PIN").performClick()
        for(n in 1..7)compose.onNodeWithText(n.toString()).performClick()
        compose.onNodeWithText("Concluir").performClick()
        assertEquals("123456",value)
    }
    @Test fun legacyPinCanBeConfirmedWithFourDigits() {
        var value=""
        compose.setContent {var pin by remember {mutableStateOf("")};Column{NumericPinField("PIN atual",pin,{pin=it;value=it},true,minimum=4)}}
        compose.onNodeWithContentDescription("PIN atual").performClick()
        for(n in 1..4)compose.onNodeWithText(n.toString()).performClick()
        compose.onNodeWithText("Concluir").performClick()
        assertEquals("1234",value)
    }
    @Test fun movieDetailsNoLongerExposeTrailerButton() {
        compose.setContent {br.com.flaviotorres.iptvpremium.ui.movies.MovieDetailsScreen(movieName="Teste")}
        compose.onNodeWithText("ASSISTIR").assertExists()
        compose.onNodeWithText("TRAILER").assertDoesNotExist()
    }
    @Test fun seriesDetailsNoLongerExposeTrailerButton() {
        compose.setContent {br.com.flaviotorres.iptvpremium.ui.series.SeriesDetailsScreen(seriesName="Teste",trailerAvailable=true)}
        compose.onNodeWithText("TRAILER").assertDoesNotExist()
        compose.onNodeWithText("TENTAR NOVAMENTE").assertExists()
    }
    @Test fun kidsLandingUsesRemoteBackAndThreeContentButtons() {
        var back=0;var movies=0
        compose.setContent {Box(Modifier.requiredSize(960.dp,540.dp)){KidsScreen({}, {movies++}, {}, {back++})}}
        compose.onNodeWithText("CANAIS").assertExists()
        compose.onNodeWithText("FILMES").performClick()
        assertEquals(1,movies)
        compose.onNodeWithText("SÉRIES").assertExists()
        compose.onRoot().performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Escape) }
        assertEquals(1,back)
    }
    @Test fun legacyPinMustBeUpgradedBeforeManagingKids() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val salt=ByteArray(16){it.toByte()}
        fun hex(b:ByteArray)=b.joinToString(""){"%02x".format(it.toInt()and 255)}
        val hash=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(javax.crypto.spec.PBEKeySpec("12345".toCharArray(),salt,120000,256)).encoded
        java.io.File(context.noBackupFilesDir,"parental_pin_v1.json").apply {parentFile!!.mkdirs();writeText(org.json.JSONObject().put("salt",hex(salt)).put("hash",hex(hash)).toString())}
        br.com.flaviotorres.iptvpremium.ui.settings.ParentalSession.initialize(context)
        compose.setContent {br.com.flaviotorres.iptvpremium.ui.settings.ParentalPreferencesSettings({})}
        compose.onNodeWithContentDescription("PIN do responsável").performClick()
        for(n in 1..5) compose.onNodeWithText(n.toString()).performClick()
        compose.onNodeWithText("Concluir").performClick()
        compose.onNodeWithText("Acessar controles").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("Seu PIN atual foi confirmado. Cadastre 6 números para continuar.").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("‹ Voltar aos controles").assertDoesNotExist()
        compose.onNodeWithText("Kids").assertDoesNotExist()
        compose.onNodeWithContentDescription("Novo PIN (6 números)").performClick()
        for(n in 1..6) compose.onNodeWithText(n.toString()).performClick()
        compose.onNodeWithText("Concluir").performClick()
        compose.onNodeWithContentDescription("Repita o PIN").performClick()
        for(n in 1..6) compose.onNodeWithText(n.toString()).performClick()
        compose.onNodeWithText("Concluir").performClick()
        compose.onNodeWithText("Salvar PIN").performClick()
        compose.waitUntil(10000) {compose.onAllNodesWithText("Kids").fetchSemanticsNodes().isNotEmpty()}
        assertTrue(br.com.flaviotorres.iptvpremium.ui.settings.ParentalPinStore(context).verify("123456"))
    }
    @Test fun bearGold() = assertBearTheme(PremiumTheme.GOLD)
    @Test fun bearBlue() = assertBearTheme(PremiumTheme.BLUE)
    @Test fun bearPurple() = assertBearTheme(PremiumTheme.PURPLE)
    @Test fun bearRed() = assertBearTheme(PremiumTheme.RED)
    @Test fun bearPink() = assertBearTheme(PremiumTheme.PINK)
    @Test fun bearEmerald() = assertBearTheme(PremiumTheme.EMERALD)
    @Test fun bearCyan() = assertBearTheme(PremiumTheme.CYAN)
    private fun assertBearTheme(theme:PremiumTheme) {
        val context=ApplicationProvider.getApplicationContext<Context>()
        ThemeState.choose(context,theme)
        var painter: androidx.compose.ui.graphics.painter.Painter? = null
        compose.setContent { painter=themePainterResource(br.com.flaviotorres.iptvpremium.R.drawable.kids_bear, androidx.compose.ui.unit.IntSize(320,320)); androidx.compose.foundation.Image(painter!!,null,Modifier.size(180.dp)) }
        compose.waitUntil(10000) { painter?.javaClass?.simpleName == "ArtworkPainter" }
            val bitmap=androidx.compose.ui.graphics.ImageBitmap(180,180)
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(androidx.compose.ui.unit.Density(1f),androidx.compose.ui.unit.LayoutDirection.Ltr,androidx.compose.ui.graphics.Canvas(bitmap),androidx.compose.ui.geometry.Size(180f,180f)) {
                with(painter!!) { draw(size) }
            }
                val pixels=bitmap.toPixelMap()
                var hits=0
                var vividBlue=0
                for(y in 0 until pixels.height step 4) for(x in 0 until pixels.width step 4) {
                    val c=pixels[x,y]
                    if(c.blue>.5f && c.blue>c.red*3f) vividBlue++
                    val ok=when(theme) {
                        PremiumTheme.GOLD->c.red>.5f&&c.green>.3f&&c.blue<c.red*.7f
                        PremiumTheme.BLUE->c.blue>.3f&&c.blue>c.red*1.2f
                        PremiumTheme.RED->c.red>.3f&&c.red>c.green*1.2f
                        PremiumTheme.PURPLE,PremiumTheme.PINK->c.red>c.green*1.2f&&c.blue>c.green*1.2f
                        PremiumTheme.EMERALD->c.green>.3f&&c.green>c.red*1.2f
                        PremiumTheme.CYAN->c.blue>c.red*1.2f&&c.green>c.red*1.2f
                    }
                    if(ok)hits++
                }
                assertTrue("Visible themed bear for $theme, pixels=$hits", hits>30)
                if(theme==PremiumTheme.BLUE) assertTrue("Bear must match vivid blue icons, not silver",vividBlue>30)
        compose.runOnIdle {ThemeState.choose(context,PremiumTheme.GOLD)}
    }
}
