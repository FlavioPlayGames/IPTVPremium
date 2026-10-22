package br.com.flaviotorres.iptvpremium.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.withTransform
import android.widget.Toast
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.data.ratings.*
import kotlinx.coroutines.*
import kotlin.math.*

/** Fixed reference colours, intentionally independent of the selected app theme. */
internal object ReferenceDetailsPalette {
    val text=Color(0xFFD5D5D7)
    val border=Color(0xFF7A7A7E)
    val gold=Color(0xFFFFCC32)
    val crimson=Color(0xFFE51B46)
    val titleBrush get()=Brush.linearGradient(listOf(Color(0xFFFFE17C),Color(0xFFFFC329),Color(0xFFC9911B)))
}

private enum class DetailIcon { STAR, CALENDAR, CLOCK, GENRES, PLAY, HEART }
@Composable
private fun DetailGlyph(icon:DetailIcon,color:Color,filled:Boolean=false,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val w=size.width;val h=size.height;val stroke=Stroke(width=(w*.075f).coerceAtLeast(1f),cap=StrokeCap.Round,join=StrokeJoin.Round)
        when(icon) {
            DetailIcon.STAR -> {
                val p=Path();repeat(10) { n->val angle=(-90+n*36)*PI/180;val radius=if(n%2==0)w*.47f else w*.22f
                    val x=w/2+cos(angle).toFloat()*radius;val y=h/2+sin(angle).toFloat()*radius
                    if(n==0)p.moveTo(x,y)else p.lineTo(x,y) };p.close()
                if(filled)drawPath(p,color)else drawPath(p,color,style=stroke)
            }
            DetailIcon.CLOCK -> {drawCircle(color,w*.42f,style=stroke);drawLine(color,Offset(w*.5f,h*.22f),Offset(w*.5f,h*.5f),stroke.width);drawLine(color,Offset(w*.5f,h*.5f),Offset(w*.7f,h*.62f),stroke.width)}
            DetailIcon.CALENDAR -> {drawRoundRect(color,Offset(w*.12f,h*.2f),androidx.compose.ui.geometry.Size(w*.76f,h*.68f),androidx.compose.ui.geometry.CornerRadius(w*.05f),style=stroke)
                drawLine(color,Offset(w*.12f,h*.4f),Offset(w*.88f,h*.4f),stroke.width)
                for(x in listOf(.3f,.7f))drawLine(color,Offset(w*x,h*.08f),Offset(w*x,h*.3f),stroke.width)
                drawRect(color,Offset(w*.3f,h*.53f),androidx.compose.ui.geometry.Size(w*.17f,h*.16f))}
            DetailIcon.GENRES -> {drawRoundRect(color,Offset(w*.1f,h*.28f),androidx.compose.ui.geometry.Size(w*.8f,h*.6f),androidx.compose.ui.geometry.CornerRadius(w*.04f),style=stroke)
                drawLine(color,Offset(w*.1f,h*.43f),Offset(w*.9f,h*.43f),stroke.width)
                for(x in listOf(.22f,.48f,.74f))drawLine(color,Offset(w*x,h*.1f),Offset(w*(x+.1f),h*.28f),stroke.width)}
            DetailIcon.PLAY -> {val p=Path().apply {moveTo(w*.25f,h*.12f);lineTo(w*.85f,h*.5f);lineTo(w*.25f,h*.88f);close()};drawPath(p,color,style=stroke)}
            DetailIcon.HEART -> {val p=Path().apply {moveTo(w*.5f,h*.88f);cubicTo(-w*.15f,h*.43f,w*.05f,-h*.05f,w*.5f,h*.28f);cubicTo(w*.95f,-h*.05f,w*1.15f,h*.43f,w*.5f,h*.88f);close()};if(filled)drawPath(p,color)else drawPath(p,color,style=stroke)}
        }
    }
}

@Composable
internal fun ReferenceMetadataRow(rating:Double?,year:Int?,durationSeconds:Int?,genres:List<String>,compact:Boolean,dense:Boolean=false) {
    Row(Modifier.fillMaxWidth(if(compact).86f else .74f),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
        if(rating!=null&&rating.isFinite()&&rating in 0.0..10.0) MetadataPill(DetailIcon.STAR,String.format(java.util.Locale.US,"%.1f",rating),compact,star=true,dense=dense)
        year?.takeIf {it>0}?.let {MetadataPill(DetailIcon.CALENDAR,it.toString(),compact,dense=dense)}
        durationSeconds?.takeIf {it>0}?.let {seconds->
            val minutes=(seconds+59)/60;val text=if(minutes>=60)"${minutes/60}h ${minutes%60}min"else"${minutes}min"
            MetadataPill(DetailIcon.CLOCK,text,compact,dense=dense)
        }
        if(genres.isNotEmpty())MetadataPill(DetailIcon.GENRES,genres.joinToString(" • "),compact,modifier=Modifier.weight(1f,fill=false),dense=dense)
    }
}
@Composable
private fun MetadataPill(icon:DetailIcon,text:String,compact:Boolean,modifier:Modifier=Modifier,star:Boolean=false,dense:Boolean=false) {
    Row(modifier.height(if(dense)22.dp else if(compact)25.dp else 29.dp).background(Color(0xAA151617),RoundedCornerShape(50))
        .border(.8.dp,ReferenceDetailsPalette.border.copy(alpha=.8f),RoundedCornerShape(50)).padding(horizontal=8.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        DetailGlyph(icon,if(star)ReferenceDetailsPalette.gold else ReferenceDetailsPalette.text,star,Modifier.size(if(compact)13.dp else 15.dp))
        Text(text,color=ReferenceDetailsPalette.text,fontSize=if(dense)9.sp else if(compact)10.sp else 12.sp,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ReferenceDetailActions(watchText:String="ASSISTIR",compact:Boolean,isFavorite:Boolean,onWatch:()->Unit,onFavorite:()->Unit,
    kind:String,playlist:String?,content:String?,tmdb:String?,imdb:String?,dense:Boolean=false,identityTitle:String?=null,identityYear:Int?=null) {
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
        ReferenceWatchButton(watchText,compact,onWatch,dense)
        var heartFocused by remember {mutableStateOf(false)}
        Box(Modifier.size(if(dense)28.dp else if(compact)32.dp else 36.dp).onFocusChanged {heartFocused=it.isFocused}
            .semantics {contentDescription=if(isFavorite)"Remover dos favoritos"else"Adicionar aos favoritos";role=Role.Button}
            .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onFavorite),contentAlignment=Alignment.Center) {
            DetailGlyph(DetailIcon.HEART,if(isFavorite)ReferenceDetailsPalette.crimson else if(heartFocused)Color.White else ReferenceDetailsPalette.text,
                isFavorite,Modifier.size(if(compact)22.dp else 25.dp).scale(if(heartFocused)1.12f else 1f))
        }
        Box(Modifier.width(1.dp).height(20.dp).background(Color(0xFF68686B)))
        PersonalRatingControl(kind,playlist,content,tmdb,imdb,compact,dense,identityTitle,identityYear)
    }
}
@Composable
private fun ReferenceWatchButton(text:String,compact:Boolean,onClick:()->Unit,dense:Boolean=false) {
    var focused by remember {mutableStateOf(false)}
    val shape=RoundedCornerShape(4.dp)
    val regular=CatalogStyle.regular
    val watchAccent=if(regular)CatalogStyle.accent else ReferenceDetailsPalette.crimson
    val watchFill=if(regular)Brush.horizontalGradient(listOf(watchAccent.copy(alpha=.20f),watchAccent.copy(alpha=.07f)))
        else Brush.horizontalGradient(listOf(Color(0xA65B071B),Color(0xB015080C)))
    Row(Modifier.width(if(text=="ASSISTIR"){if(dense)112.dp else if(compact)128.dp else 146.dp}else 170.dp).height(if(dense)26.dp else if(compact)30.dp else 35.dp)
        .background(watchFill,shape)
        .border(if(focused)1.6.dp else 1.dp,if(regular)watchAccent else if(focused)Color(0xFFFF5777)else ReferenceDetailsPalette.crimson,shape)
        .onFocusChanged {focused=it.isFocused}.semantics {role=Role.Button}
        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
        DetailGlyph(DetailIcon.PLAY,ReferenceDetailsPalette.text,modifier=Modifier.size(12.dp));Spacer(Modifier.width(8.dp))
        Text(text,color=Color(0xFFE3DDE0),fontWeight=FontWeight.Bold,fontSize=if(compact)11.sp else 12.sp,maxLines=1)
    }
}

@Composable
private fun PersonalRatingControl(kind:String,playlist:String?,content:String?,tmdb:String?,imdb:String?,compact:Boolean,dense:Boolean=false,identityTitle:String?=null,identityYear:Int?=null) {
    val app=LocalContext.current.applicationContext
    val identity=remember(kind,playlist,content,tmdb,imdb,identityTitle,identityYear){PersonalRatingIdentity(kind,playlist,content,tmdb,imdb,identityTitle,identityYear)}
    var rating by remember(identity){mutableStateOf<Int?>(null)}
    var loaded by remember(identity){mutableStateOf(false)}
    var saving by remember(identity){mutableStateOf(false)}
    var showDialog by remember(identity){mutableStateOf(false)}
    val scope=rememberCoroutineScope()
    LaunchedEffect(identity) {rating=withContext(Dispatchers.IO){PersonalRatingStore(app).read(identity)};loaded=true}
    var focused by remember {mutableStateOf(false)}
    val enabled=loaded&&!saving&&identity.keys().isNotEmpty()
    Row(Modifier.height(if(dense)26.dp else 32.dp).onFocusChanged {focused=it.isFocused}
        .border(if(focused)1.dp else 0.dp,if(focused)ReferenceDetailsPalette.border else Color.Transparent,RoundedCornerShape(4.dp))
        .semantics {contentDescription="Avaliar ${if(kind=="movies")"filme"else"série"}";stateDescription=rating?.let {"Sua nota: $it de 5 estrelas"}?:"Sem avaliação pessoal";role=Role.Button}
        .clickable(enabled=enabled,interactionSource=remember{MutableInteractionSource()},indication=null){showDialog=true}
        .padding(horizontal=3.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(3.dp)) {
        for(star in 1..5)DetailGlyph(DetailIcon.STAR,if(star<=(rating?:0))ReferenceDetailsPalette.gold else ReferenceDetailsPalette.text,
            star<=(rating?:0),Modifier.size(if(compact)13.dp else 15.dp))
        Spacer(Modifier.width(3.dp))
        Text(if(rating==null)"Avalie"else"${rating}/5",color=ReferenceDetailsPalette.text,fontSize=if(compact)11.sp else 12.sp,maxLines=1)
    }
    if(showDialog) RatingDialog(rating,{showDialog=false}) {value->
        val previous=rating;rating=value;saving=true
        // A selected score must finish saving even if the user immediately leaves the details.
        scope.launch(start=CoroutineStart.UNDISPATCHED) {val ok=withContext(NonCancellable+Dispatchers.IO){runCatching{PersonalRatingStore(app).save(identity,value)}.isSuccess}
            saving=false;if(!ok){rating=previous;Toast.makeText(app,"Não foi possível salvar sua avaliação.",Toast.LENGTH_SHORT).show()}}
    }
}

@Composable
internal fun RatingDialog(current:Int?,onDismiss:()->Unit,onSelect:(Int?)->Unit) {
    val initial=remember{FocusRequester()}
    val hadRating=remember { current!=null }
    var confirmed by remember { mutableStateOf<Int?>(null) }
    val reaction=remember { Animatable(0f) }
    val dismissLatest by rememberUpdatedState(onDismiss)
    LaunchedEffect(confirmed) {
        val score=confirmed ?: return@LaunchedEffect
        reaction.animateTo(1f,tween(if(score==5)1500 else 800,easing=LinearEasing))
        dismissLatest()
    }
    Dialog(onDismissRequest=onDismiss) {
        Column(Modifier.width(340.dp).background(Color(0xFF151618),RoundedCornerShape(12.dp))
            .border(1.dp,ReferenceDetailsPalette.border,RoundedCornerShape(12.dp)).padding(22.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Sua avaliação",color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=20.sp)
            Text(confirmed?.let { "Você escolheu $it de 5 estrelas" } ?: "Escolha de 1 a 5 estrelas",color=ReferenceDetailsPalette.text,fontSize=13.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                for(star in 1..5) {
                    var focused by remember{mutableStateOf(false)}
                    Box(Modifier.size(46.dp).then(if(star==(current?:3))Modifier.focusRequester(initial)else Modifier)
                        .border(if(focused)1.5.dp else 0.dp,if(focused)Color.White else Color.Transparent,RoundedCornerShape(7.dp))
                        .onFocusChanged{focused=it.isFocused}
                        .semantics{contentDescription="$star de 5 estrelas";role=Role.Button;selected=star==(confirmed?:current)}
                        .clickable(interactionSource=remember{MutableInteractionSource()},indication=null){
                            if(confirmed==null) { confirmed=star; onSelect(star) }
                        },contentAlignment=Alignment.Center) {
                        RatingReactionStar(star,confirmed?:current,focused,confirmed!=null,reaction.value,Modifier.size(46.dp))
                    }
                }
            }
            if(hadRating)ReferenceDialogAction("Remover avaliação",onClick={onSelect(null);onDismiss()},enabled=confirmed==null)
            ReferenceDialogAction("Fechar",onDismiss)
        }
    }
    LaunchedEffect(Unit){withFrameNanos { };initial.requestFocus()}
}
@Composable
private fun ReferenceDialogAction(text:String,onClick:()->Unit,enabled:Boolean=true) {
    var focused by remember{mutableStateOf(false)}
    Text(text,color=if(focused)Color.White else ReferenceDetailsPalette.text,fontSize=13.sp,
        modifier=Modifier.onFocusChanged{focused=it.isFocused}.border(if(focused)1.dp else 0.dp,
            if(focused)ReferenceDetailsPalette.border else Color.Transparent,RoundedCornerShape(4.dp))
            .clickable(enabled=enabled,interactionSource=remember{MutableInteractionSource()},indication=null,onClick=onClick).padding(horizontal=12.dp,vertical=7.dp))
}

/** Only the personal-rating dialog uses these faces and particles. Page stars stay unchanged. */
@Composable
private fun RatingReactionStar(index:Int,score:Int?,focused:Boolean,reacting:Boolean,progress:Float,modifier:Modifier) {
    Canvas(modifier) {
        val filled=index<=(score?:0)
        val p=progress.coerceIn(0f,1f)
        val radius=size.minDimension*.285f
        fun starAt(center:Offset,r:Float):Path = Path().apply {
            repeat(10){n->val angle=(-90+n*36)*PI/180;val length=if(n%2==0)r else r*.468f
                val x=center.x+cos(angle).toFloat()*length;val y=center.y+sin(angle).toFloat()*length
                if(n==0)moveTo(x,y)else lineTo(x,y)};close()
        }
        val low=reacting&&filled&&(score?:0)<=2
        val smiling=reacting&&filled&&(score?:0) in 3..4
        val burst=reacting&&score==5&&index==5
        val wave=sin(PI*p).toFloat()
        val rotation=if(low)sin(PI*6*p).toFloat()*9f*wave else 0f
        val expansion=when {
            smiling->1f+.17f*wave
            burst&&p<.22f->1f+.3f*sin(p/.22f*PI/2).toFloat()
            burst&&p<.66f->1.3f
            burst->.65f+.35f*((p-.66f)/.24f).coerceIn(0f,1f)
            else->1f
        }
        val opacity=when {
            !burst->1f
            p<.18f->1f
            p<.26f->1f-(p-.18f)/.08f
            p<.66f->0f
            else->((p-.66f)/.18f).coerceIn(0f,1f)
        }
        val star=starAt(center,radius)
        withTransform({rotate(rotation,center);scale(expansion,expansion,center)}) {
            if(filled) {
                drawPath(star,Brush.verticalGradient(listOf(Color(0xFFFFE681),ReferenceDetailsPalette.gold,Color(0xFFE8A514)),center.y-radius,center.y+radius),alpha=opacity)
                drawPath(star,Color(0xFFFFE997),alpha=opacity,style=Stroke(.65.dp.toPx(),join=StrokeJoin.Round))
            } else drawPath(star,if(focused)ReferenceDetailsPalette.gold else ReferenceDetailsPalette.text,
                style=Stroke(1.2.dp.toPx(),join=StrokeJoin.Round))
            if(smiling) {
                val faceAlpha=minOf((p/.18f).coerceIn(0f,1f),((1f-p)/.18f).coerceIn(0f,1f))
                val ink=Color(0xFF593713).copy(alpha=faceAlpha)
                for(sign in listOf(-1f,1f))drawCircle(ink,radius*.065f,center+Offset(sign*radius*.19f,-radius*.06f))
                drawArc(ink,0f,180f,false,center+Offset(-radius*.20f,radius*.05f),
                    androidx.compose.ui.geometry.Size(radius*.4f,radius*.25f),style=Stroke(radius*.065f,cap=StrokeCap.Round))
            }
        }
        if(burst&&p>.18f&&p<.88f) {
            val q=((p-.18f)/.7f).coerceIn(0f,1f)
            val spread=sin(PI*q).toFloat().coerceAtLeast(0f)
            repeat(12){n->
                val angle=(n*30-90)*PI/180
                val distance=radius*(.6f+1.55f*spread)
                val location=center+Offset(cos(angle).toFloat()*distance,sin(angle).toFloat()*distance)
                val alpha=minOf(q/.15f,(1f-q)/.2f).coerceIn(0f,1f)
                val particle=radius*(if(n%3==0).18f else .12f)*(.65f+.35f*spread)
                drawPath(starAt(location,particle),ReferenceDetailsPalette.gold,alpha=alpha)
            }
        }
    }
}
