package br.com.flaviotorres.iptvpremium.ui.kids
fun main() {
    for((width,height) in listOf(96 to 38,220 to 38,900 to 38,34 to 34,1 to 1)) {
        val slices=clayTextureSlices(456,192,width,height)
        check(slices.sumOf{it.dw*it.dh}==width*height)
        check(slices.all{it.dw>0 && it.dh>0 && it.sw>0 && it.sh>0})
        check(slices.all{it.dx>=0 && it.dy>=0 && it.dx+it.dw<=width && it.dy+it.dh<=height})
        check(slices.all{it.sx>=0 && it.sy>=0 && it.sx+it.sw<=456 && it.sy+it.sh<=192})
        for(a in slices)for(b in slices) if(a!==b) {
            val overlapX=minOf(a.dx+a.dw,b.dx+b.dw)-maxOf(a.dx,b.dx)
            val overlapY=minOf(a.dy+a.dh,b.dy+b.dh)-maxOf(a.dy,b.dy)
            check(overlapX<=0 || overlapY<=0)
        }
    }
    check(clayTextureSlices(456,192,96,38).first().dw==clayTextureSlices(456,192,900,38).first().dw)
    check(clayTextureSlices(456,192,0,38).isEmpty())
    println("PASS fixed corner radius for short/long controls; complete coverage without overlaps or out-of-bounds slices")
}
