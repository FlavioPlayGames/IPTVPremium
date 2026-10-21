package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ReferenceDetailsUiDeviceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun metadataChipsAppearInReferenceOrderWithoutOverlap() {
        compose.setContent {Box(Modifier.width(640.dp)){ReferenceMetadataRow(6.4,2026,5940,listOf("Aventura","Comédia","Família"),true)}}
        val labels=listOf("6.4","2026","1h 39min","Aventura • Comédia • Família")
        val bounds=labels.map {label->compose.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot}
        bounds.zipWithNext().forEach {(before,after)->assertTrue(before.right<=after.left)}
    }
    @Test fun ratingDialogSelectsPersonalScore() {
        var score:Int?=null
        compose.setContent {RatingDialog(null,{}, {score=it})}
        compose.onNodeWithContentDescription("4 de 5 estrelas").performClick()
        compose.runOnIdle{assertEquals(4,score)}
    }
    @Test fun closingDialogDoesNotChangeScore() {
        var selected=false;var dismissed=false
        compose.setContent {RatingDialog(3,{dismissed=true},{selected=true})}
        compose.onNodeWithText("Fechar").performClick()
        compose.runOnIdle{assertTrue(dismissed);assertFalse(selected)}
    }
    @Test fun ratingCanBeRemoved() {
        var score:Int?=4;var changed=false
        compose.setContent {RatingDialog(score,{}, {score=it;changed=true})}
        compose.onNodeWithText("Remover avaliação").performClick()
        compose.runOnIdle{assertTrue(changed);assertNull(score)}
    }
    @Test fun watchAndFavoriteActionsStayIndependentOfRating() {
        var watched=0;var favorited=0
        compose.setContent {ReferenceDetailActions(compact=true,isFavorite=false,onWatch={watched++},onFavorite={favorited++},kind="movies",playlist=null,content=null,tmdb=null,imdb=null)}
        compose.onNodeWithText("ASSISTIR").performClick()
        compose.onNodeWithContentDescription("Adicionar aos favoritos").performClick()
        compose.runOnIdle{assertEquals(1,watched);assertEquals(1,favorited)}
    }
    @Test fun remoteControlMovesBetweenStarsAndConfirmsWithEnter() {
        var score:Int?=null
        compose.setContent {RatingDialog(null,{}, {score=it})}
        compose.onNodeWithContentDescription("3 de 5 estrelas").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.Enter)
        }
        compose.runOnIdle{assertEquals(4,score)}
    }

}
