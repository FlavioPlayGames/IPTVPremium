package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** Small heart in the metadata row; long OK on the card still toggles favorites. */
@Composable
internal fun CatalogFavoriteHeart(isFavorite: Boolean, compact: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(if (compact) 12.dp else 15.dp)
        .pointerInput(onClick) { detectTapGestures { onClick() } }
        .semantics {
            role = Role.Button
            stateDescription = if (isFavorite) "Favorito" else "Não favorito"
            onClick(label = if (isFavorite) "Remover favorito" else "Adicionar favorito") { onClick(); true }
        }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(if (compact) 10.dp else 12.dp)) {
            val w = size.width; val h = size.height
            val heart = Path().apply {
                moveTo(w * .5f, h * .88f)
                cubicTo(w * .42f, h * .8f, w * .08f, h * .58f, w * .08f, h * .32f)
                cubicTo(w * .08f, h * .05f, w * .36f, h * .0f, w * .5f, h * .17f)
                cubicTo(w * .64f, h * .0f, w * .92f, h * .05f, w * .92f, h * .32f)
                cubicTo(w * .92f, h * .58f, w * .58f, h * .8f, w * .5f, h * .88f)
                close()
            }
            drawPath(heart, color = if (isFavorite)
                (if(br.com.flaviotorres.iptvpremium.ui.kids.KidsAppearance.active) br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaAccent() else CatalogStyle.color(0xFFFFD54A))
                else br.com.flaviotorres.iptvpremium.ui.kids.kidsCinemaInk(0xFF9D9DA3), style = if (isFavorite) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 1.dp.toPx()))
        }
    }
}
