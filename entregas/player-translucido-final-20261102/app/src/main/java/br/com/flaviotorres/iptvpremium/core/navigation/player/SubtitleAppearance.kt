package br.com.flaviotorres.iptvpremium.core.player

enum class SubtitleBackground(val label: String, val argb: Int) {
    NONE("Sem fundo", 0x00000000),
    TRANSLUCENT("Transparente (60%)", 0x99000000.toInt()),
    OPAQUE("Fechado", 0xFF000000.toInt())
}
enum class SubtitleTypeface(val label: String, val family: String) {
    DEFAULT("Padrão", "sans-serif"), SERIF("Serifada", "serif"), MONOSPACE("Monoespaçada", "monospace")
}
enum class SubtitleEdge(val label: String) { NONE("Sem contorno"), OUTLINE("Contorno preto"), SHADOW("Sombra") }
data class SubtitleTextColor(val label: String, val argb: Int)
internal val subtitleTextColors = listOf(
    SubtitleTextColor("Branco forte", 0xFFFFFFFF.toInt()),
    SubtitleTextColor("Branco médio", 0xFFF2F2F2.toInt()),
    SubtitleTextColor("Branco suave", 0xFFE5E5E5.toInt()),
    SubtitleTextColor("Amarelo forte", 0xFFFFFF00.toInt()),
    SubtitleTextColor("Amarelo médio", 0xFFFFD54F.toInt()),
    SubtitleTextColor("Amarelo suave", 0xFFFFE991.toInt())
)
internal val subtitleSizeChoices = listOf(18, 22, 26, 30)
data class SubtitleAppearance(
    val textArgb: Int = 0xFFFFFF00.toInt(),
    val background: SubtitleBackground = SubtitleBackground.TRANSLUCENT,
    val sizeSp: Int = 22,
    val typeface: SubtitleTypeface = SubtitleTypeface.DEFAULT,
    val edge: SubtitleEdge = SubtitleEdge.SHADOW
) {
    init { require(sizeSp in 16..40) }
}

// IPTV_SUBTITLE_READABILITY_V1
