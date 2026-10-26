package br.com.flaviotorres.iptvpremium.ui.kids

internal data class ClayTextureSlice(val sx:Int,val sy:Int,val sw:Int,val sh:Int,
    val dx:Int,val dy:Int,val dw:Int,val dh:Int)

/** Fixed-radius corners; only the centre stretches on long controls. */
internal fun clayTextureSlices(sw:Int,sh:Int,dw:Int,dh:Int):List<ClayTextureSlice> {
    if(sw<=0 || sh<=0 || dw<=0 || dh<=0) return emptyList()
    if(minOf(sw,sh,dw,dh)<4) return listOf(ClayTextureSlice(0,0,sw,sh,0,0,dw,dh))
    val sourceCorner=(minOf(sw,sh)/4).coerceAtLeast(1)
    val corner=(minOf(dw,dh)/4).coerceAtLeast(1)
    val sx=intArrayOf(0,sourceCorner,sw-sourceCorner,sw)
    val sy=intArrayOf(0,sourceCorner,sh-sourceCorner,sh)
    val dx=intArrayOf(0,corner,dw-corner,dw)
    val dy=intArrayOf(0,corner,dh-corner,dh)
    return buildList(9) {
        for(y in 0..2) for(x in 0..2) add(ClayTextureSlice(
            sx[x],sy[y],sx[x+1]-sx[x],sy[y+1]-sy[y],
            dx[x],dy[y],dx[x+1]-dx[x],dy[y+1]-dy[y]))
    }
}

internal data class ClayFocusRegion(val x:Int,val y:Int,val width:Int,val height:Int,val radius:Float)
/** Only the opaque centre is sampled, so no independently scaled seams remain. */
internal fun clayFocusRegion(sw:Int,sh:Int,dw:Int,dh:Int):ClayFocusRegion? {
    if(sw<4 || sh<4 || dw<=0 || dh<=0)return null
    val x=sw/4;val y=sh/4
    return ClayFocusRegion(x,y,sw-x*2,sh-y*2,minOf(dw,dh)*.25f)
}
