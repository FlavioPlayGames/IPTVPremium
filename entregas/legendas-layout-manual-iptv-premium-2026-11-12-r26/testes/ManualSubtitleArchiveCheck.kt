package br.com.flaviotorres.iptvpremium.core.player

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private fun zip(vararg files:Pair<String,ByteArray>):ByteArray {
    val output=ByteArrayOutputStream()
    ZipOutputStream(output).use {z->for((name,bytes)in files){z.putNextEntry(ZipEntry(name));z.write(bytes);z.closeEntry()}}
    return output.toByteArray()
}
fun main() {
    val srt="1\n00:00:01,000 --> 00:00:03,000\nOlá, mundo!\n".toByteArray()
    val name="Fear.The_.Walking.Dead_.S05E01.720p.AMZN_.WEBRip.x264-GalaxyTV.srt"
    val pack=zip(name to srt,"Fear.The.Walking.Dead.S05E02.srt" to srt,"foto.jpg" to byteArrayOf(1,2),"../outside.srt" to srt,"__MACOSX/._test.srt" to srt)
    val parsed=ManualSubtitleArchive.read(pack,"temporada.zip")
    check(parsed.size==2)
    check(parsed.first().name==name&&parsed.first().season==5&&parsed.first().episode==1)
    check(ManualSubtitleArchive.cues(parsed.first().bytes).single().text=="Olá, mundo!")
    val target=ManualSubtitleTarget("season","list","series","Fear the Walking Dead",season=5,episodes=listOf(1,2))
    check(target.validEpisode(parsed.first())==1)
    check(target.copy(season=4).validEpisode(parsed.first())==null)
    val single=ManualSubtitleArchive.read(srt,"qualquer-nome.srt").single()
    check(target.copy(kind="episode",episode=7).validEpisode(single)==7)
    check(target.validEpisode(single)==null)
    check(ManualSubtitleArchive.read(zip("S05E01.srt" to srt,"S05E01.pt-br.srt" to srt),"duplicadas.zip").size==2)
    check(ManualSubtitleArchive.cues("1\r\n00:00:02,000 --> 00:00:04,000\r\nAção\r\n".toByteArray(charset("windows-1252"))).single().text=="Ação")
    check(ManualSubtitleArchive.cues(("\uFEFF1\n00:00:01,000 --> 00:00:03,000\nOlá\n").toByteArray(Charsets.UTF_16LE).let {byteArrayOf(0xFF.toByte(),0xFE.toByte())+it}).single().text=="Olá")
    check(runCatching{ManualSubtitleArchive.read("invalid".toByteArray(),"x.srt")}.isFailure)
    check(runCatching{ManualSubtitleArchive.read(byteArrayOf(80),"x.zip")}.isFailure)
    check(runCatching{ManualSubtitleArchive.read(zip("bomb.txt" to ByteArray(17*1024*1024)),"bomb.zip")}.isFailure)
    check(runCatching{ManualSubtitleArchive.read(zip("S05E01E02.srt" to srt),"duplo.zip")}.isFailure)
    val unordered="1\n00:00:05,000 --> 00:00:06,000\nSegundo\n\n2\n00:00:01,000 --> 00:00:02,000\nPrimeiro\n".toByteArray()
    check(ManualSubtitleArchive.cues(unordered).map{it.startMs}==listOf(1000L,5000L))
    val many=Array(513){index->"$index.txt" to byteArrayOf(0)}
    check(runCatching{ManualSubtitleArchive.read(zip(*many),"many.zip")}.isFailure)
    println("PASS: nome original, S05E01, temporada/episodio, escolha individual, duplicatas, descarte de extras/caminhos, UTF-8/1252/UTF-16, formato invalido e limites de ZIP.")
}
