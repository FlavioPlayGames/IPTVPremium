package br.com.flaviotorres.iptvpremium.ui.theme

import kotlin.math.roundToInt

/** Change the hue of warm coloured metal only; finished pearl/gray pixels remain byte-identical. */
internal fun homeMetalTintPixel(argb:Int,matrix:FloatArray):Int {
    if(argb ushr 24==0)return argb
    val red=(argb ushr 16)and 255
    val green=(argb ushr 8)and 255
    val blue=argb and 255
    if(red==0||red<green||green<blue)return argb
    val chroma=red-blue
    if(chroma<=16)return argb
    val saturation=chroma.toFloat()/red
    if(saturation<=.20f)return argb
    // Soft colour transition on metal fringes, never an alpha or brightness replacement mask.
    val amount=((saturation-.20f)/.15f).coerceIn(0f,1f)*((chroma-16)/16f).coerceIn(0f,1f)
    fun channel(row:Int,original:Int):Int {
        val i=row*5
        val changed=(matrix[i]*red+matrix[i+1]*green+matrix[i+2]*blue+matrix[i+4]).coerceIn(0f,255f)
        return (original+(changed-original)*amount).roundToInt().coerceIn(0,255)
    }
    return (argb and -0x1000000)or(channel(0,red)shl 16)or(channel(1,green)shl 8)or channel(2,blue)
}
