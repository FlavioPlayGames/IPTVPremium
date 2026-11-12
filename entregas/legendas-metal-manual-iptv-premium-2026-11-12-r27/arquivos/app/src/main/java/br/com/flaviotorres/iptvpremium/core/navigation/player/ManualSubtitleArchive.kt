package br.com.flaviotorres.iptvpremium.core.player

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipInputStream

internal data class ManualSubtitleFile(val name:String,val bytes:ByteArray,val season:Int?,val episode:Int?)

/** Original names are labels only: ZIP paths are never extracted onto the filesystem. */
internal object ManualSubtitleArchive {
    const val MAX_UPLOAD=8*1024*1024
    const val MAX_FILE=1024*1024
    private const val MAX_EXPANDED=16*1024*1024
    private val numbered=Regex("(?i)(?<![a-z0-9])s([0-9]{1,3})[ ._-]*e([0-9]{1,4})(?![0-9])|(?<![0-9])([0-9]{1,3})x([0-9]{1,4})(?![0-9])")
    private val time=Regex("(?m)^\\s*([0-9]{1,3}):([0-9]{2}):([0-9]{2})[,.]([0-9]{3})\\s*-->\\s*([0-9]{1,3}):([0-9]{2}):([0-9]{2})[,.]([0-9]{3})[^\\r\\n]*$")
    fun text(bytes:ByteArray):String {
        require(bytes.size in 1..MAX_FILE){"Cada legenda deve ter até 1 MB."}
        val charset=when {
            bytes.size>1&&bytes[0]==0xFF.toByte()&&bytes[1]==0xFE.toByte()->Charsets.UTF_16LE
            bytes.size>1&&bytes[0]==0xFE.toByte()&&bytes[1]==0xFF.toByte()->Charsets.UTF_16BE
            else->Charsets.UTF_8
        }
        return try{charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()}
        catch(_:java.nio.charset.CharacterCodingException){String(bytes,java.nio.charset.Charset.forName("windows-1252"))}
            .removePrefix("\uFEFF").replace("\r\n","\n").replace('\r','\n')
    }
    fun cues(bytes:ByteArray):List<ExternalSubtitleCue> {
        val raw=text(bytes);require('\u0000' !in raw){"Legenda inválida."}
        return raw.split(Regex("\\n[ \\t]*\\n")).mapNotNull {block->
            val m=time.find(block)?:return@mapNotNull null
            fun timestamp(start:Int):Long {
                val h=m.groupValues[start].toLong();val min=m.groupValues[start+1].toLong();val sec=m.groupValues[start+2].toLong()
                if(min>59||sec>59)return -1
                return ((h*60+min)*60+sec)*1000+m.groupValues[start+3].toLong()
            }
            val start=timestamp(1);val end=timestamp(5)
            val caption=block.substring(m.range.last+1).trim().replace(Regex("<[^>]*>"),"").take(8192)
            if(start<0||end<=start||caption.isBlank())null else ExternalSubtitleCue(start,end,caption)
        }.sortedBy{it.startMs}.take(20_000)
    }
    private fun file(name:String,bytes:ByteArray):ManualSubtitleFile? {
        if(cues(bytes).isEmpty())return null
        val matches=numbered.findAll(name).toList()
        if(matches.size>1||Regex("(?i)s[0-9]+[ ._-]*e[0-9]+[ ._-]*e[0-9]+").containsMatchIn(name))return null
        val m=matches.singleOrNull()
        val season=m?.let{(it.groupValues[1].ifEmpty{it.groupValues[3]}).toIntOrNull()}
        val episode=m?.let{(it.groupValues[2].ifEmpty{it.groupValues[4]}).toIntOrNull()}
        return ManualSubtitleFile(name.takeLast(200),bytes,season,episode)
    }
    fun read(bytes:ByteArray,name:String):List<ManualSubtitleFile> {
        require(bytes.size in 1..MAX_UPLOAD){"Envie um arquivo SRT ou ZIP de até 8 MB."}
        if(!name.endsWith(".zip",true)){
            require(name.endsWith(".srt",true)){"Use SRT ou ZIP."}
            return listOfNotNull(file(name,bytes)).also{require(it.isNotEmpty()){"O arquivo não contém uma legenda SRT válida."}}
        }
        require(bytes.size>=4&&bytes[0]=='P'.code.toByte()&&bytes[1]=='K'.code.toByte()){"Arquivo ZIP inválido."}
        val result=mutableListOf<ManualSubtitleFile>();var total=0L;var entries=0
        ZipInputStream(ByteArrayInputStream(bytes)).use {zip->
            val buffer=ByteArray(8192)
            while(true){
                val entry=zip.nextEntry?:break
                require(++entries<=512){"O ZIP contém arquivos demais."}
                val accepted=!entry.isDirectory&&entry.name.endsWith(".srt",true)&&
                    !entry.name.replace('\\','/').split('/').any{it==".."||it=="__MACOSX"}&&
                    !entry.name.substringAfterLast('/').startsWith("._")
                val out=if(accepted)ByteArrayOutputStream()else null;var size=0
                while(true){val count=zip.read(buffer);if(count<0)break;total+=count;size+=count
                    require(total<=MAX_EXPANDED){"O ZIP expandido excede 16 MB."}
                    if(size<=MAX_FILE)out?.write(buffer,0,count)
                }
                if(accepted&&size in 1..MAX_FILE){
                    val candidate=runCatching{file(entry.name,out!!.toByteArray())}.getOrNull()
                    if(candidate!=null){require(result.size<128){"Envie no máximo 128 legendas por ZIP."};result+=candidate}
                }
            }
        }
        require(result.isNotEmpty()){"Não encontramos legendas SRT válidas no ZIP."}
        return result
    }
}
