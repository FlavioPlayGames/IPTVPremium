package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

internal enum class SettingsSymbol(val path:String) {
    DEVICE("M5 8H43V34H5Z M24 34V42 M15 42H33"),
    APP("M10 5H38V43H10Z M17 14H31 M17 23H31 M17 33H25"),
    SYSTEM("M10 15H38V37H10Z M14 15L9 6 M34 15L39 6 M17 37V44 M31 37V44 M4 18V33 M44 18V33"),
    CHECK("M24 4A20 20 0 1 1 23.9 4 M13 24L21 32L35 17"),
    UPDATE("M6 20A18 18 0 0 1 39 13 M40 4V15H29 M42 28A18 18 0 0 1 9 35 M8 44V33H19"),
    LIVE("M5 10H43V36H5Z M17 43H31 M24 36V43 M18 3L24 10L30 3"),
    MOVIE("M6 10H42V40H6Z M6 20H42 M12 10L19 20 M27 10L34 20"),
    SERIES("M8 6H35V34H8Z M14 12H41V41H14 M20 19L30 25L20 31Z")
}
@Composable
internal fun SettingsSymbolIcon(symbol:SettingsSymbol,modifier:Modifier=Modifier.size(30.dp)) {
    val path=remember(symbol){PathParser().parsePathString(symbol.path).toPath()}
    val color=SettingsLook.secondary
    Canvas(modifier){scale(size.width/48f,size.height/48f,pivot=androidx.compose.ui.geometry.Offset.Zero){
        drawPath(path,color,style=Stroke(2.2f,cap=StrokeCap.Round,join=StrokeJoin.Round))
    }}
}
@Composable
internal fun SettingsCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    val shape=RoundedCornerShape(12.dp)
    Column(modifier.background(SettingsLook.panel,shape).border(1.dp,SettingsLook.edge.copy(alpha=.65f),shape)
        .padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
@Composable
internal fun SettingsHeading(title:String,description:String?=null) {
    Text(title,color=SettingsLook.text,fontSize=18.sp,fontWeight=FontWeight.SemiBold)
    if(!description.isNullOrBlank())Text(description,color=SettingsLook.secondary,fontSize=13.sp)
}
@Composable
internal fun SettingsRadio(title:String,selected:Boolean,modifier:Modifier=Modifier,description:String?=null,onClick:()->Unit) {
    var focused by remember{mutableStateOf(false)}
    val shape=RoundedCornerShape(8.dp)
    Row(modifier.heightIn(min=46.dp).clip(shape).onFocusChanged{focused=it.isFocused}
        .semantics{this.selected=selected;role=Role.RadioButton}
        .background(if(focused)SettingsLook.selection else SettingsLook.control)
        .border(if(focused)2.dp else 1.dp,if(focused)CatalogStyle.accent else SettingsLook.edge.copy(alpha=.55f),shape)
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(title,color=SettingsLook.text,fontSize=15.sp,fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)
            if(!description.isNullOrBlank())Text(description,color=SettingsLook.secondary,fontSize=12.sp)
        }
        val rim=if(selected)CatalogStyle.accent else SettingsLook.secondary
        Box(Modifier.size(16.dp).border(1.5.dp,rim,RoundedCornerShape(50)),contentAlignment=Alignment.Center) {
            if(selected)Box(Modifier.size(8.dp).background(rim,RoundedCornerShape(50)))
        }
    }
}
@Composable
internal fun SettingsSwitchVisual(checked:Boolean) {
    Box(Modifier.size(34.dp,20.dp).background(if(checked)CatalogStyle.accent else SettingsLook.edge,RoundedCornerShape(50)).padding(3.dp),
        contentAlignment=if(checked)Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.size(14.dp).background(Color.White,RoundedCornerShape(50)))
    }
}
@Composable
internal fun SettingsToggle(title:String,description:String,checked:Boolean,modifier:Modifier=Modifier,onClick:()->Unit) {
    var focused by remember{mutableStateOf(false)}
    val shape=RoundedCornerShape(10.dp)
    Row(modifier.fillMaxWidth().heightIn(min=58.dp).onFocusChanged{focused=it.isFocused}
        .semantics{role=Role.Switch;stateDescription=if(checked)"Ativado" else "Desativado"}
        .background(if(focused)SettingsLook.selection else SettingsLook.panel,shape)
        .border(if(focused)2.dp else 1.dp,SettingsLook.border(focused),shape)
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick).padding(14.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,color=SettingsLook.text,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
            Text(description,color=SettingsLook.secondary,fontSize=12.sp)
        }
        SettingsSwitchVisual(checked)
    }
}
