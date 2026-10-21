package br.com.flaviotorres.iptvpremium.ui.home

/** Coordinates of the approved 1672 x 941 artwork; labels are outside the animated crops. */
internal data class MarbleRegion(val left:Int,val top:Int,val right:Int,val bottom:Int) {
    val width get()=right-left
    val height get()=bottom-top
}
internal object MarbleHomeGeometry {
    const val width=1672
    const val height=941
    val logo=MarbleRegion(570,45,1108,365)
    val icons=listOf(
        MarbleRegion(78,439,386,688), MarbleRegion(416,380,718,691),
        MarbleRegion(737,430,1008,687), MarbleRegion(1012,430,1281,690),
        MarbleRegion(1296,443,1593,688), MarbleRegion(1465,43,1540,121),
        MarbleRegion(1557,39,1647,123))
    val hitAreas=listOf(
        MarbleRegion(58,376,404,762), MarbleRegion(409,376,723,762),
        MarbleRegion(728,376,1010,762), MarbleRegion(1015,376,1284,762),
        MarbleRegion(1289,376,1620,762), MarbleRegion(1456,33,1545,133),
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
