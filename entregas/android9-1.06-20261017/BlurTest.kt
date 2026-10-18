package br.com.flaviotorres.iptvpremium.ui.theme
fun main() {
 fun test(name:String,body:()->Unit){body();println("PASS $name")}
 test("cores dos sete temas permanecem uniformes") {
  for(c in listOf(0xffbe9630.toInt(),0xff1955bb.toInt(),0xff8142bd.toInt(),0xffb5212a.toInt(),0xffbb315b.toInt(),0xff18a466.toInt(),0xff13a6ac.toInt()))
   check(blurBackdropPixels(IntArray(37*23){c},37,23,7).all{it==c})
 }
 test("sem raio preserva os pixels e a entrada nunca e modificada") {
  val p=intArrayOf(0xff000000.toInt(),0xffffffff.toInt(),0xff123456.toInt(),0xff010203.toInt());val copy=p.copyOf()
  check(blurBackdropPixels(p,2,2,0).contentEquals(p));blurBackdropPixels(p,2,2,5);check(p.contentEquals(copy))
 }
 test("desfoque espalha detalhe sem perder opacidade") {
  val p=IntArray(21*21){0xff000000.toInt()};p[10*21+10]=0xffffffff.toInt()
  val b=blurBackdropPixels(p,21,21,2);check((b[10*21+10] and 255)in 1..254);check((b[10*21+9] and 255)>0);check(b.all{it ushr 24==255})
 }
 test("imagens pequenas e raios limitados nao excedem os limites") {
  check(blurBackdropPixels(intArrayOf(0xff123456.toInt()),1,1,500).single()==0xff123456.toInt())
  check(runCatching{blurBackdropPixels(IntArray(2),2,2,1)}.isFailure)
 }
 test("wallpaper limitado a 384x216 e computado em tempo finito") {
  val p=IntArray(384*216){0xff000000.toInt() or(it*713)};val start=System.nanoTime();val b=blurBackdropPixels(p,384,216,24)
  check(b.size==p.size && (System.nanoTime()-start)<5_000_000_000L)
 }
}
