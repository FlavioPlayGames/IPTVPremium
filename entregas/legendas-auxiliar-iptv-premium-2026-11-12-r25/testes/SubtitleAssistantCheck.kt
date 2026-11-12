package br.com.flaviotorres.iptvpremium.core.player

import java.math.BigDecimal
import java.math.RoundingMode

fun main() {
    val cases=listOf(
        Triple(4_400L,0L,4_000L),Triple(4_499L,0L,4_000L),Triple(4_500L,0L,5_000L),
        Triple(4_999L,0L,5_000L),Triple(0L,4_400L,-4_000L),Triple(0L,4_500L,-5_000L),
        Triple(10_000L,10_000L,0L),Triple(10_500L,10_000L,1_000L),
        Triple(0L,499L,0L),Triple(0L,500L,-1_000L),
        Triple(Long.MAX_VALUE,0L,120_000L),Triple(0L,Long.MAX_VALUE,-120_000L),
        Triple(Long.MAX_VALUE,Long.MAX_VALUE,0L)
    )
    for((click,cue,expected) in cases)check(subtitleAssistantOffset(click,cue)==expected) {"click=$click cue=$cue expected=$expected"}
    for(delta in -120_000L..120_000L) {
        val actual=subtitleAssistantOffset(maxOf(delta,0L),maxOf(-delta,0L))
        val reference=BigDecimal.valueOf(delta,3).setScale(0,RoundingMode.HALF_UP).longValueExact()*1_000L
        check(actual==reference) {"delta=$delta expected=$reference actual=$actual"}
    }
    check(runCatching{subtitleAssistantOffset(-1L,0L)}.isFailure)
    check(runCatching{subtitleAssistantOffset(0L,-1L)}.isFailure)
    check(externalSubtitleClock(10_000L,2_000L)==8_000L)
    check(externalSubtitleClock(10_000L,-2_000L)==12_000L)
    check(subtitleSyncSteps==listOf(100L,500L,1_000L,5_000L))
    println("PASS: 240001 valores confrontados com HALF_UP; limites, empates, sinais, entradas invalidas e ajustes manuais preservados.")
}
