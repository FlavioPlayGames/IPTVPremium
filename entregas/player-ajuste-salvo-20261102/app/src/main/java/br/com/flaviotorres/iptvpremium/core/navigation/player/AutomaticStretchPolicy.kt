package br.com.flaviotorres.iptvpremium.core.player

/** Stretch each fitted axis to the viewport; never uses uniform zoom/crop or manual offsets. */
internal fun automaticStretchScale(viewportWidth:Float,viewportHeight:Float,fittedWidth:Float,fittedHeight:Float):Pair<Float,Float> {
    if(listOf(viewportWidth,viewportHeight,fittedWidth,fittedHeight).any{!it.isFinite()||it<=0f})return 1f to 1f
    return viewportWidth/fittedWidth to viewportHeight/fittedHeight
}
