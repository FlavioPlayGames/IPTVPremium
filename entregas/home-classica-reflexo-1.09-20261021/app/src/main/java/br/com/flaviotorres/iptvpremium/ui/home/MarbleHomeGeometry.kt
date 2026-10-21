package br.com.flaviotorres.iptvpremium.ui.home

/** 1672 x 941 layout: classic transparent icons at approved slot widths, with uniform gaps and native captions. */
internal data class MarbleRegion(val left:Int,val top:Int,val right:Int,val bottom:Int) {
    val width get()=right-left
    val height get()=bottom-top
}
internal object MarbleHomeGeometry {
    const val width=1672
    const val height=941
    val logo=MarbleRegion(570,45,1108,365)
    val icons=listOf(
        MarbleRegion(64,517,357,690), MarbleRegion(399,474,686,690),
        MarbleRegion(729,495,986,690), MarbleRegion(1028,458,1284,690),
        MarbleRegion(1326,485,1608,690), MarbleRegion(1472,50,1537,111),
        MarbleRegion(1570,49,1642,115))
    val captions=listOf(
        MarbleRegion(122,700,300,742),MarbleRegion(448,700,638,742),
        MarbleRegion(758,700,957,742),MarbleRegion(1070,700,1242,742),
        MarbleRegion(1362,700,1572,742))
    val hitAreas=listOf(
        MarbleRegion(44,352,378,762), MarbleRegion(378,352,707,762),
        MarbleRegion(707,352,1007,762), MarbleRegion(1007,352,1304,762),
        MarbleRegion(1304,352,1628,762), MarbleRegion(1456,33,1545,133),
        MarbleRegion(1549,29,1658,133))
    val labels=listOf("Canais","Filmes","Séries","Kids","Listas","Atualizar","Configurações")
}
internal enum class MarbleDirection { LEFT,RIGHT,UP,DOWN }
internal object MarbleHomeNavigation {
    fun enabled(index:Int,normalEnabled:Boolean)=index in 0..6 && (normalEnabled || index>=3)
    fun initial(normalEnabled:Boolean)=if(normalEnabled)0 else 3
    fun next(current:Int,direction:MarbleDirection,normalEnabled:Boolean,lastMain:Int=0):Int {
        val at=current.takeIf{enabled(it,normalEnabled)}?:initial(normalEnabled)
        val available=(if(at<5)0..4 else 5..6).filter{enabled(it,normalEnabled)}
        return when(direction) {
            MarbleDirection.LEFT -> available.lastOrNull{it<at}?:at
            MarbleDirection.RIGHT -> available.firstOrNull{it>at}?:at
            MarbleDirection.UP -> if(at<5){if(at<2)5 else 6}else at
            MarbleDirection.DOWN -> if(at>=5)lastMain.takeIf{it<5&&enabled(it,normalEnabled)}?:initial(normalEnabled) else at
        }
    }
}
