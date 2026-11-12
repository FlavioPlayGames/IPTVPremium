package br.com.flaviotorres.iptvpremium.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.ui.components.*
import br.com.flaviotorres.iptvpremium.ui.theme.MetallicPalette

/** The existing navigation route enters here; QR generation belongs only to a chosen action. */
@Composable
fun SubdlAccountScreen(onBack:()->Unit) {
    var page by rememberSaveable { mutableStateOf("home") }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val back:()->Unit={if(page=="home")onBack() else page="home"}
    BackHandler(onBack=back)
    when(page) {
        "accounts" -> SubtitleAccountsScreen(onBack=back)
        "appearance" -> SubtitleAppearanceSettingsScreen(onBack=back)
        "manual" -> ManualSubtitleSettingsScreen(onBack=back)
        else -> SubtitleSettingsLanding(selected,onSelected={selected=it},onOpen={
            selected=it;page=listOf("accounts","manual","appearance")[it]
        },onBack=back)
    }
}

@Composable
internal fun SubtitleSettingsFrame(title:String,description:String,onBack:()->Unit,
    backFocus:FocusRequester?=null,modifier:Modifier=Modifier,
    content:@Composable ColumnScope.()->Unit) {
    Box(modifier.fillMaxSize().background(SettingsLook.background)) {
        SettingsBackdrop(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().padding(horizontal=34.dp,vertical=22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(title,color=MetallicPalette.ink,fontSize=27.sp,fontWeight=FontWeight.Bold)
                    if(description.isNotBlank())Text(description,color=MetallicPalette.secondary,fontSize=15.sp)
                }
                Box(Modifier.width(110.dp).height(54.dp),contentAlignment=Alignment.Center) {
                    CatalogWordmark(compact=true,modifier=Modifier.fillMaxSize())
                }
                SubtitleActionButton("‹  Voltar",modifier=Modifier.width(104.dp)
                    .then(if(backFocus!=null)Modifier.focusRequester(backFocus)else Modifier),onClick=onBack)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MetallicPalette.accent.copy(alpha=.65f)))
            content()
        }
    }
}

internal enum class SubtitleSettingsIcon(val path:String) {
    ACCOUNTS("M14 35H12A9 9 0 0 1 10 17A14 14 0 0 1 37 16A10 10 0 0 1 38 36H35 M18 26L23 21A5 5 0 0 1 30 28L26 32 M28 30L23 35A5 5 0 0 1 16 28L20 24"),
    UPLOAD("M12 4H30L38 12V44H12Z M30 4V12H38 M25 35V20 M18 27L25 20L32 27"),
    APPEARANCE("M4 10H44V38H4Z M21 19C11 12 10 34 21 29 M38 19C28 12 27 34 38 29"),
    MOVIE("M5 17H43V42H5Z M5 17L2 8L39 2L42 11Z M12 6L19 14 M26 4L33 12 M22 24L32 30L22 36Z"),
    SERIES("M5 15H35V41H5Z M14 7H44V33 M17 22L27 28L17 34Z")
}

@Composable
internal fun SubtitleSettingsGlyph(icon:SubtitleSettingsIcon,modifier:Modifier=Modifier.size(48.dp)) {
    val path=remember(icon){PathParser().parsePathString(icon.path).toPath()}
    val color=MetallicPalette.accent
    Canvas(modifier) {
        scale(size.width/48f,size.height/48f,pivot=Offset.Zero) {
            drawPath(path,color,style=Stroke(2.1f,cap=StrokeCap.Round,join=StrokeJoin.Round))
        }
    }
}

@Composable
private fun SubtitleSettingsLanding(selected:Int,onSelected:(Int)->Unit,onOpen:(Int)->Unit,onBack:()->Unit) {
    val cards=remember {List(3){FocusRequester()}}
    val back=remember{FocusRequester()}
    LaunchedEffect(Unit) {withFrameNanos{};cards[selected.coerceIn(0,2)].requestFocus()}
    SubtitleSettingsFrame("LEGENDAS","Escolha como deseja configurar.",onBack,back) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide=maxWidth>=660.dp
            val height=if(wide)((maxWidth-28.dp)/3f/1.55f).coerceAtMost(maxHeight-12.dp).coerceAtLeast(168.dp)else 190.dp
            val title=listOf("Contas de legendas","Envio manual","Aparência")
            val detail=listOf("SubDL · OpenSubtitles · SubSource","Arquivo SRT ou ZIP","Fonte, tamanho e cores")
            val caption=listOf("Gerenciar conexões","Filme, temporada ou episódio","Personalize a leitura")
            val card:@Composable (Int,Modifier)->Unit={index,cell->
                SubtitleLandingCard(title[index],detail[index],caption[index],SubtitleSettingsIcon.entries[index],
                    cell.height(height).focusRequester(cards[index]).focusProperties {
                        if(wide){left=if(index==0)FocusRequester.Cancel else cards[index-1]
                            right=if(index==2)FocusRequester.Cancel else cards[index+1]
                            up=back;down=FocusRequester.Cancel}
                    },onFocused={onSelected(index)},onClick={onOpen(index)})
            }
            if(wide) Row(Modifier.fillMaxSize(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                for(index in 0..2)card(index,Modifier.weight(1f))
            } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                for(index in 0..2)card(index,Modifier.fillMaxWidth())
            }
        }
        Text("Use as setas para navegar · OK para selecionar",color=MetallicPalette.ink,fontSize=12.sp,
            modifier=Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
internal fun SubtitleLandingCard(title:String,detail:String,caption:String,icon:SubtitleSettingsIcon,
    modifier:Modifier=Modifier,onFocused:()->Unit={},onClick:()->Unit) {
    var focused by remember{mutableStateOf(false)}
    val shape=RoundedCornerShape(16.dp)
    Column(modifier.onFocusChanged{focused=it.isFocused;if(it.isFocused)onFocused()}
        .semantics{contentDescription="$title. $detail. $caption"}.clip(shape)
        .background(if(focused)MetallicPalette.selected else MetallicPalette.card)
        .border(if(focused)3.dp else 1.dp,if(focused)MetallicPalette.accent else androidx.compose.ui.graphics.Color(0xFFB4C6D6),shape)
        .clickable(role=Role.Button,interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(horizontal=14.dp,vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(6.dp,Alignment.CenterVertically)) {
        SubtitleSettingsGlyph(icon,Modifier.size(58.dp))
        Text(title,color=MetallicPalette.ink,fontSize=21.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
        Text(detail,color=MetallicPalette.secondary,fontSize=14.sp,textAlign=TextAlign.Center)
        Box(Modifier.fillMaxWidth().height(1.dp).background(MetallicPalette.secondary.copy(alpha=.65f)))
        Text(caption,color=MetallicPalette.ink,fontSize=14.sp,textAlign=TextAlign.Center)
    }
}

/** Focus treatment shared by subtitle actions; keeps the chosen palette vivid in light and dark. */
@Composable
internal fun SubtitleActionButton(label:String,enabled:Boolean=true,modifier:Modifier=Modifier.fillMaxWidth(),
    onClick:()->Unit) {
    var focused by remember{mutableStateOf(false)}
    val shape=RoundedCornerShape(9.dp)
    Box(modifier.heightIn(min=46.dp).alpha(if(enabled)1f else .45f)
        .onFocusChanged{focused=it.isFocused}
        .background(if(focused)MetallicPalette.selected else MetallicPalette.card,shape)
        .border(if(focused)3.dp else 1.dp,if(focused)MetallicPalette.accent else MetallicPalette.secondary.copy(alpha=.45f),shape)
        .clickable(enabled=enabled,role=Role.Button,interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick)
        .padding(horizontal=14.dp,vertical=11.dp),contentAlignment=Alignment.CenterStart) {
        Text(label,color=MetallicPalette.ink,fontSize=14.sp,fontWeight=if(focused)FontWeight.SemiBold else FontWeight.Normal)
    }
}
