package br.com.flaviotorres.iptvpremium.ui.theme

/** Alpha-only mask. Never recolour chromatic metal, including its warm dark shadows. */
internal fun homeIconSurfaceMaskAlpha(argb: Int): Int {
    val alpha = argb ushr 24
    if (alpha == 0) return 0
    val red = (argb ushr 16) and 255
    val green = (argb ushr 8) and 255
    val blue = argb and 255
    val chroma = maxOf(red, green, blue) - minOf(red, green, blue)
    if (chroma >= 16 || (red - green >= 2 && green - blue >= 2)) return 0
    val weight = if (chroma <= 4) 255 else (16 - chroma) * 255 / 12
    return alpha * weight / 255
}
