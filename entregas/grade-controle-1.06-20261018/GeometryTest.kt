package br.com.flaviotorres.iptvpremium.ui.components

fun main() {
    var cases = 0
    for (width in listOf(360f, 540f, 720f, 1000f))
        for (height in listOf(260f, 340f, 440f, 620f))
            for (scale in listOf(1f, 1.1f, 1.2f, 1.5f)) {
                val gap = if (height < 500) 10f else 16f
                val metadata = (if (height < 500) 38f else 48f) * scale
                val aspect = catalogTwoRowAspect(width, height, 4, gap, metadata)
                val cell = (width - 16 - 3 * gap) / 4
                val used = 2 * (cell / aspect + metadata) + gap + 16
                check(used <= height + .01f) { "two rows overflow: $width $height $scale $used" }
                check(aspect >= .67f && aspect.isFinite())
                cases++
            }
    check(catalogTwoRowAspect(0f, 0f, 0, 10f, 38f) == .72f)
    println("PASS $cases geometrias de duas fileiras: quatro colunas, texto ampliado e margens")
    println("PASS limites de dimensoes invalidas")
}
