package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Scoped to settings: no changes to license dialogs, player controls or Kids actions. */
@Composable
internal fun MarbleSettingsButton(label:String,enabled:Boolean=true,modifier:Modifier=Modifier.fillMaxWidth(),
    textColor:Color?=null,onClick:()->Unit) {
    if(!CatalogStyle.regular) {
        br.com.flaviotorres.iptvpremium.core.device.PortalOutlineButton(label,enabled,modifier,textColor,onClick)
        return
    }
    var focused by remember{mutableStateOf(false)}
    val shape=RoundedCornerShape(10.dp)
    Box(modifier.heightIn(min=48.dp).alpha(if(enabled)1f else .45f).onFocusChanged{focused=it.isFocused}
        .background(androidx.compose.ui.graphics.SolidColor(if(focused)SettingsLook.selection else SettingsLook.control),shape)
        .border(if(focused)1.5.dp else .7.dp,SettingsLook.border(focused),shape)
        .clickable(enabled=enabled,role=Role.Button,interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(horizontal=14.dp,vertical=10.dp)) {
        Text(label,color=CatalogStyle.text,fontSize=14.sp)
    }
}
