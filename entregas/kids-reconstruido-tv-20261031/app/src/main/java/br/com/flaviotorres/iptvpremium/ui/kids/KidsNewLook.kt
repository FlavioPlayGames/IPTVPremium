package br.com.flaviotorres.iptvpremium.ui.kids

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal object KidsCatalogLook {
    val background get()=Color(0xFFFFFCF7)
    val text get()=kidsCinemaText
    val secondary get()=kidsCinemaText.copy(alpha=.75f)
    val accent get()=kidsCinemaAccent()
    val categorySurface get()=Color.White.copy(alpha=.90f)
}
internal object KidsControlLook {
    val background get()=KidsCatalogLook.background
    val divider get()=KidsCatalogLook.accent.copy(alpha=.18f)
    val metalBorder:Brush get()=SolidColor(KidsCatalogLook.accent)
    fun foreground(selected:Boolean)=if(selected)Color.White else KidsCatalogLook.text
}
internal fun Modifier.kidsNewSurface(focused:Boolean,active:Boolean,shape:Shape,
    translucent:Boolean=false,category:Boolean=false):Modifier = clip(shape)
    .background(if(focused||active)KidsCatalogLook.accent else Color.White.copy(alpha=if(translucent).92f else .86f),shape)
    .border(if(focused)2.5.dp else 1.dp,
        if(focused||active)KidsCatalogLook.accent else KidsCatalogLook.accent.copy(alpha=if(category).12f else .22f),shape)

@Composable
internal fun Modifier.kidsNewClick(onClick:()->Unit):Modifier=clickable(
    interactionSource=remember{MutableInteractionSource()},indication=null,role=Role.Button,onClick=onClick)
