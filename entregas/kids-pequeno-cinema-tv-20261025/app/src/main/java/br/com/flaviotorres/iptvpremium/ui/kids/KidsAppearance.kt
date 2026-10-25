package br.com.flaviotorres.iptvpremium.ui.kids

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import br.com.flaviotorres.iptvpremium.R
import br.com.flaviotorres.iptvpremium.ui.theme.PremiumTheme
import br.com.flaviotorres.iptvpremium.ui.theme.ThemeState
import br.com.flaviotorres.iptvpremium.ui.theme.themePainterResource

internal enum class KidsStyle(val key:String,val label:String,val accent:Color,val theme:PremiumTheme) {
    BLUE("blue","Azul",Color(0xFF51C8FF),PremiumTheme.BLUE),
    PINK("pink","Rosa",Color(0xFFFF86D5),PremiumTheme.PINK);
    val home:Int get()=if(this==BLUE)R.drawable.kids_home_blue else R.drawable.kids_home_pink
    companion object { fun fromKey(key:String?)=entries.firstOrNull {it.key==key}?:BLUE }
}

/** Aparência infantil é independente da preferência do menu principal. */
internal object KidsAppearance {
    var active by mutableStateOf(false); private set
    var style by mutableStateOf(KidsStyle.BLUE); private set
    fun apply(active:Boolean,key:String?) {
        this.active=active;style=KidsStyle.fromKey(key)
        ThemeState.useKidsPalette(if(active)style.theme else null)
    }
    val accent get()=style.accent
}

/** Desconhecidos recebem um símbolo sorteado pelo nome: estável ao voltar/reiniciar. */
internal fun kidsCategorySymbol(name:String):Int {
    val folded=KidsContent.folded(name)
    return when {
        Regex("halloween|abobora|bruxa").containsMatchIn(folded)->2
        Regex("fantasm|assombr|ghost|spooky").containsMatchIn(folded)->3
        Regex("aventur|adventur|espac|space|rocket|astron|\\bacao\\b|foguet").containsMatchIn(folded)->1
        Regex("princes|castel|castle|fadas|fairy|contos").containsMatchIn(folded)->4
        Regex("ocean|aquatic|tartarug|turtle|natureza|\\bmar\\b").containsMatchIn(folded)->5
        Regex("animais|pets|cachorr|gatinh|patrulha").containsMatchIn(folded)->7
        Regex("todos|estrela|musica|lancamento").containsMatchIn(folded)->6
        Regex("kids|infantil|desenho|cartoon|animacao|familia|family|children").containsMatchIn(folded)->0
        else->Math.floorMod(folded.hashCode(),8)
    }
}

private class AtlasPainter(private val source:Painter,private val columns:Int,private val rows:Int,private val index:Int,private val baselineShift:Float=0f,private val trimTop:Float=0f):Painter() {
    private val full=source.intrinsicSize
    override val intrinsicSize=Size(full.width/columns,full.height/rows)
    override fun DrawScope.onDraw() {
        if(!full.width.isFinite() || full.width<=0 || !full.height.isFinite() || full.height<=0)return
        val cell=intrinsicSize
        clipRect(top=(baselineShift+trimTop)*size.height/cell.height) {
            withTransform({
                scale(size.width/cell.width,size.height/cell.height,Offset.Zero)
                translate(-(index%columns)*cell.width,-(index/columns)*cell.height+baselineShift)
            }) { with(source) {draw(full)} }
        }
    }
}

@Composable
internal fun KidsCategoryIcon(name:String,modifier:Modifier=Modifier) {
    val label=KidsContent.folded(name)
    if("favorit" in label || "continua" in label) {
        val accent=KidsAppearance.accent
        Canvas(modifier.size(30.dp)) {
            val u=size.minDimension/24f
            if("favorit" in label) {
                val heart=Path().apply {
                    moveTo(12*u,21*u);cubicTo(1*u,14*u,0f,7*u,5*u,4*u)
                    cubicTo(8*u,2*u,11*u,4*u,12*u,6*u)
                    cubicTo(14*u,1*u,21*u,2*u,22*u,7*u)
                    cubicTo(24*u,12*u,19*u,16*u,12*u,21*u);close()
                }
                drawPath(heart,Brush.verticalGradient(listOf(Color.White,accent,accent.copy(alpha=.65f))))
            } else {
                drawCircle(Brush.verticalGradient(listOf(accent,accent.copy(alpha=.4f))))
                val play=Path().apply {moveTo(9*u,6*u);lineTo(18*u,12*u);lineTo(9*u,18*u);close()}
                drawPath(play,Color.White)
            }
        }
        return
    }
    val source=themePainterResource(R.drawable.kids_category_symbols,IntSize(1024,512))
    val index=kidsCategorySymbol(name)
    val painter=remember(source,index){AtlasPainter(source,4,2,index)}
    Image(painter,null,modifier.size(30.dp),contentScale=ContentScale.Fit)
}

@Composable
internal fun KidsBrand(modifier:Modifier=Modifier,style:KidsStyle=KidsAppearance.style) {
    val source=themePainterResource(R.drawable.kids_brand_marks,IntSize(768,384))
    val painter=remember(source,style){AtlasPainter(source,2,1,style.ordinal)}
    Image(painter,"IPTV Premium Kids",modifier,contentScale=ContentScale.Fit)
}

@Composable
internal fun KidsCatalogHeader(compact:Boolean,title:String="MINHAS AVENTURAS") {
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
        KidsBrand(Modifier.fillMaxWidth().height(if(compact)96.dp else 118.dp))
        Text(title,color=KidsAppearance.accent,fontSize=if(compact)12.sp else 15.sp,
            fontWeight=FontWeight.ExtraBold,letterSpacing=1.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
internal fun KidsHomeArtwork(style:KidsStyle,modifier:Modifier=Modifier,labels:Boolean=false) {
    BoxWithConstraints(modifier) {
        KidsCinemaHomeArtwork(style,Modifier.fillMaxSize())
        if(labels) KidsHomeMenu(style,maxWidth,maxHeight) { index,cell ->
            KidsHomeCell(style,index,false,maxHeight,cell)
        }
    }
}

/** Mesma geometria nas duas cores e na prévia: a arte não contém nomes ou seleção. */
@Composable
internal fun KidsHomeMenu(style:KidsStyle,width:Dp,height:Dp,content:@Composable (Int,Modifier)->Unit) {
    Row(Modifier.fillMaxWidth().offset(y=height*.26f).height(height*.51f)
        .padding(horizontal=width*.14f),horizontalArrangement=Arrangement.spacedBy(width*.025f)) {
        repeat(3) {index ->content(index,Modifier.weight(1f).fillMaxHeight())}
    }
}

@Composable
internal fun KidsHomeCell(style:KidsStyle,index:Int,focused:Boolean,height:Dp,modifier:Modifier=Modifier) {
    KidsCinemaHomeCell(style,index,focused,height,modifier)
}

/** O foco branco indica o controle atual; a cor do tema indica apenas o item selecionado. */
internal fun kidsFocusFill(style:KidsStyle,focused:Boolean):Color=when(style) {
    KidsStyle.BLUE->if(focused)Color(0xFF174C78)else Color(0xFF102D48)
    KidsStyle.PINK->if(focused)Color(0xFF6D2455)else Color(0xFF39182F)
}
internal fun Modifier.kidsFocus(focused:Boolean,selected:Boolean=false,fill:Boolean=true):Modifier {
    if(!KidsAppearance.active)return this
    val accent=KidsAppearance.accent
    val surface=kidsFocusFill(KidsAppearance.style,focused)
    return focusProperties {canFocus=true}.drawWithCache {
        val radius=androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
        onDrawWithContent {
            if(fill && (focused || selected)) drawRoundRect(surface,cornerRadius=radius)
            drawContent()
            if(focused || selected) {
                val stroke=if(focused)3.dp.toPx()else 1.5.dp.toPx()
                val color=if(focused)Color.White else accent
                drawRoundRect(color,topLeft=Offset(stroke/2,stroke/2),size=Size(size.width-stroke,size.height-stroke),
                    cornerRadius=radius,style=Stroke(stroke))
                drawRoundRect(color,topLeft=Offset(5.dp.toPx(),size.height*.22f),
                    size=Size(if(focused)4.dp.toPx()else 2.dp.toPx(),size.height*.56f),cornerRadius=radius)
            }
        }
    }
}

/** Decoração estática: sem shimmer, partículas animadas ou trabalho por foco. */
@Composable
internal fun KidsScenery(modifier:Modifier=Modifier,panel:Boolean=false) {
    val accent=KidsAppearance.accent
    val pink=KidsAppearance.style==KidsStyle.PINK
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(if(pink)listOf(Color(0xFF1B0625),Color(0xFF080411))else listOf(Color(0xFF031B38),Color(0xFF020714))))
        fun star(center:Offset,r:Float,color:Color) {
            val path=Path()
            for(i in 0..9) {
                val a=-Math.PI/2+i*Math.PI/5;val radius=if(i%2==0)r else r*.42f
                val p=Offset(center.x+kotlin.math.cos(a).toFloat()*radius,center.y+kotlin.math.sin(a).toFloat()*radius)
                if(i==0)path.moveTo(p.x,p.y)else path.lineTo(p.x,p.y)
            };path.close();drawPath(path,color)
        }
        repeat(if(panel)12 else 35) {i->
            val x=((i*73+13)%101)/101f*size.width;val y=((i*47+9)%103)/103f*size.height
            if(!panel || y>size.height*.72f)star(Offset(x,y),if(i%5==0)4.dp.toPx()else 1.4.dp.toPx(),if(i%5==0)Color(0xFFEACB7E)else accent.copy(alpha=.45f))
        }
        val cloud=if(pink)Color(0xFF491444)else Color(0xFF063168)
        repeat(8){i->drawCircle(cloud,28.dp.toPx(),Offset(i*size.width/6,size.height+(if(i%2==0)8.dp else 22.dp).toPx()))}
        for(top in listOf(-7.dp.toPx(),size.height-12.dp.toPx())) {
            drawRect(accent.copy(alpha=.15f),Offset(0f,top),Size(size.width,18.dp.toPx()))
            repeat((size.width/14.dp.toPx()).toInt()+1){i->drawRect(accent.copy(alpha=.28f),Offset(i*14.dp.toPx(),top+3.dp.toPx()),Size(6.dp.toPx(),4.dp.toPx()))}
        }
    }
}

/** Aplicado só à superfície do painel, depois do seu fundo e antes do conteúdo. */
internal fun Modifier.kidsPanel():Modifier=if(!KidsAppearance.active)this else this.drawKidsPanel()
private fun Modifier.drawKidsPanel()=drawWithCache {
    val accent=KidsAppearance.accent
    onDrawBehind {
        drawRect(Brush.verticalGradient(listOf(accent.copy(alpha=.12f),Color.Transparent,accent.copy(alpha=.10f))))
        repeat(7){i->drawCircle(accent.copy(alpha=.14f),12.dp.toPx(),Offset(i*size.width/5,size.height+if(i%2==0)0f else 10.dp.toPx()))}
        val ribbon=Path().apply {
            moveTo(-size.width*.1f,size.height-35.dp.toPx())
            cubicTo(size.width*.35f,size.height-75.dp.toPx(),size.width*.45f,size.height+35.dp.toPx(),size.width*1.1f,size.height-10.dp.toPx())
        }
        drawPath(ribbon,accent.copy(alpha=.25f),style=Stroke(16.dp.toPx()))
        drawPath(ribbon,Color.Black.copy(alpha=.35f),style=Stroke(7.dp.toPx()))
    }
}

@Composable
internal fun KidsCatalogTitle(title:String,count:Int) {
    if(!KidsAppearance.active)return
    Row(Modifier.fillMaxWidth().padding(start=6.dp,bottom=5.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,color=kidsCinemaText,fontSize=16.sp,fontWeight=FontWeight.Black,letterSpacing=.7.sp)
        Text("$count ITENS",color=kidsCinemaText.copy(alpha=.8f),fontSize=10.sp)
    }
}
