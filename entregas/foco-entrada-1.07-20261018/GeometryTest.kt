package br.com.flaviotorres.iptvpremium.ui.components

import kotlin.math.abs
fun main() {
    var cases = 0
    for (width in listOf(360f, 540f, 720f, 1000f))
        for (height in listOf(260f, 340f, 440f, 620f))
            for (scale in listOf(1f, 1.1f, 1.2f, 1.5f)) {
                val gap = if (height < 500) 10f else 16f
                val metadata = if (height < 500) 26f * scale + 6f else 34f * scale + 10f
                val poster = catalogTwoRowPosterWidth(width, height, 4, gap, metadata)
                val cell = (width - 16f - 3 * gap) / 4
                val usedHeight = 2 * (poster * 1.5f + metadata) + gap + 18
                check(usedHeight <= height + .01f) { "rows overflow" }
                check(4 * poster + 3 * gap + 16 <= width + .01f) { "columns overflow" }
                check(poster > 0 && poster.isFinite())
                // A larger poster would exceed at least one available dimension.
                check(abs(poster - cell) < .01f || abs(usedHeight - height) < .01f) { "not maximal" }
                cases++
            }
    check(catalogTwoRowPosterWidth(0f, 0f, 0, 10f, 38f) == 0f)
    println("PASS $cases geometrias com oito posters 2:3 inteiros, texto ampliado e maior tamanho possivel")
    println("PASS dimensoes invalidas nao produzem tamanhos negativos")
}
