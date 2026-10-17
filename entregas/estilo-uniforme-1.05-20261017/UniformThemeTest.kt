package br.com.flaviotorres.iptvpremium.ui.theme
import androidx.compose.ui.graphics.Color
import kotlin.math.*
fun main() {
 val setter=ThemeState::class.java.getDeclaredMethod("setSelected",PremiumTheme::class.java).apply{isAccessible=true}
 var count=0
 fun test(name:String,block:()->Unit){block();count++;println("PASS $name")}
 fun luminance(c:Color):Double{fun f(x:Float)=if(x<=.04045f)x/12.92 else ((x+.055)/1.055).pow(2.4)
 return .2126*f(c.red)+.7152*f(c.green)+.0722*f(c.blue)}
 for(theme in PremiumTheme.entries){setter.invoke(ThemeState,theme)
  test("${theme.key}: tinted dark surfaces, visible colored selection and white contrast"){
   val bg=ThemeColors.background;val fill=SelectionTheme.color()
   check(bg!=Color.Black);check(fill!=bg);check(fill.alpha==1f)
   check(SelectionTheme.contentColor==Color.White)
   check(1.05/(luminance(fill)+.05)>7.0)
   check(SelectionTheme.colors().all{it==fill})
   check(ThemeColors.surface(0x80111419).alpha in .49f.. .51f)
   check(SelectionTheme.color(.4f).alpha in .39f.. .41f)
   check(SelectionTheme.color(-1f).alpha==0f && SelectionTheme.color(2f).alpha==1f)
  }
  test("${theme.key}: metallic highlights preserve alpha and ordered contrast"){
   val stops=listOf(0xFF7A5215L,0xFFFFE6A0L,0xFFD6A32EL,0xFFFFF5CDL,0x70926315L)
   for(stop in stops){val c=ThemeColors.color(stop);val rim=ThemeColors.borderColor(stop);check(rim.alpha==c.alpha)
    if(theme==PremiumTheme.GOLD)check(rim==c)
    else {val previous=if(theme==PremiumTheme.BLUE)c.green*.82f else c.green;check(rim.green>previous)}
   }
   check(ThemeColors.borderColor(stops[1])!=ThemeColors.borderColor(stops[0]))
  }
 }
 test("switching theme updates cached surfaces and selection without restarting"){
  setter.invoke(ThemeState,PremiumTheme.EMERALD);val green=ThemeColors.surface(0xFF151518);val selectedGreen=SelectionTheme.color()
  setter.invoke(ThemeState,PremiumTheme.BLUE);check(ThemeColors.surface(0xFF151518)!=green);check(SelectionTheme.color()!=selectedGreen)
  setter.invoke(ThemeState,PremiumTheme.EMERALD);check(ThemeColors.surface(0xFF151518)==green)
 }
 setter.invoke(ThemeState,PremiumTheme.GOLD)
 println("$count palette scenarios passed")
}
