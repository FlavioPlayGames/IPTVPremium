package br.com.flaviotorres.iptvpremium.ui.components
import kotlin.math.abs
fun main() {
 var passed = 0
 fun test(name:String,body:()->Unit) {body();passed++;println("PASS $name")}
 for (fps in listOf(24,30,60,120)) for (rows in listOf(1,8)) test("$fps fps, $rows rows: bounded continuous movement and exact settling") {
  val motion=CatalogScrollMotion();var position=0f;val target=rows*200f;val dt=1f/fps;var settled=false
  repeat(fps*4) {
   val distance=target-position
   if(abs(distance)<.5f&&abs(motion.velocity)<12f){position+=distance;settled=true}
   else {
    val delta=motion.advance(distance,dt,200f);check(delta>=-.001f);check(abs(delta)<=200f*6f*dt+.01f);position+=delta;check(position<=target+.05f)
   }
  }
  check(settled&&abs(position-target)<.01f)
 }
 test("held direction retargets without resetting velocity or teleporting") {
  val m=CatalogScrollMotion();var p=0f
  repeat(30){frame ->val target=if(frame<10)200f else if(frame<20)600f else 1000f;val delta=m.advance(target-p,1f/60,200f);check(abs(delta)<=20.01f);p+=delta}
  check(p>200f&&p<1000f)
 }
 test("reversing direction preserves a continuous path then settles") {
  val m=CatalogScrollMotion();var p=0f;repeat(12){p+=m.advance(1200-p,1f/60,200f)}
  check(p>0);repeat(180){val delta=m.advance(-p,1f/60,200f);check(abs(delta)<=20.01f);p+=delta};check(abs(p)<.5f)
 }
 test("slow frames and tiny steps remain finite and bounded") {
  val m=CatalogScrollMotion();var p=0f
  repeat(400){val delta=m.advance(3f-p,if(it%3==0).2f else .007f,3f);check(delta.isFinite()&&m.velocity.isFinite());check(abs(delta)<=.901f);p+=delta};check(abs(p-3f)<.01f)
 }
 println("$passed checks passed")
}
