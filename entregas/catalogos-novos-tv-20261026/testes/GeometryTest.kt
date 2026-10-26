package br.com.flaviotorres.iptvpremium.ui.components
import kotlin.math.abs
fun main() {
 var checked=0;var minGrowth=Float.MAX_VALUE;var maxGrowth=0f
 for ((width,height) in listOf(960f to 540f,1280f to 720f,1920f to 1080f))
 for(columns in listOf(4,5)) for(font in listOf(1f,1.3f,1.5f)) {
  val compact=height<650||width<1200
  val metadata=if(compact)26f*font+6f else 34f*font+10f
  val oldOuter=if(compact)12f else 20f;val newOuter=if(compact)10f else 16f
  val sidebar=width*.225f;val sideGap=if(compact)6f else 8f;val header=if(compact)44f else 54f
  val oldHeight=height-oldOuter-(if(compact)6f else 8f)-header-(if(compact)6f else 8f)-4f
  val newHeight=height-newOuter-(if(compact)4f else 6f)-header-(if(compact)4f else 6f)
  val oldWidth=width-2*oldOuter-sidebar-sideGap-4f;val newWidth=width-2*newOuter-sidebar-sideGap
  val old=catalogTwoRowGeometry(oldWidth,oldHeight,columns,if(compact)6f else 8f,metadata,4f)
  val current=catalogTwoRowGeometry(newWidth,newHeight,columns,if(compact)4f else 6f,metadata,4f)
  check(current.posterWidth>=old.posterWidth)
  check(current.posterWidth*columns+(columns-1)*(if(compact)4f else 6f)+8f<=newWidth+.01f)
  check(abs(2*current.cardHeight+current.rowSpacing+8f-newHeight)<.01f)
  check(abs(current.cardHeight-current.posterWidth/CATALOG_POSTER_ASPECT-metadata)<.01f)
  val growth=(current.posterWidth/old.posterWidth-1)*100;check(growth in 0f..10f)
  minGrowth=minOf(minGrowth,growth);maxGrowth=maxOf(maxGrowth,growth);checked++
 }
 println("PASS $checked layouts: four/five columns, three screen sizes and font scales; two rows fit, metadata reserved, aspect 2:3 preserved")
 println("Calculated width growth in these DP scenarios: $minGrowth% .. $maxGrowth%; actual TV screenshot still required")
}
