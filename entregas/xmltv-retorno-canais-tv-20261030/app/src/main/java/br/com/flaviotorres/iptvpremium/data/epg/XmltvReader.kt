package br.com.flaviotorres.iptvpremium.data.epg

import br.com.flaviotorres.iptvpremium.core.player.PlaybackEpgProgram
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.ext.DefaultHandler2

/** Streaming reader: bounded input/text, no DOM, external entities or external DTD. */
internal object XmltvReader {
    fun read(input:InputStream,epgId:String,name:String,now:Long,checkActive:()->Unit={}):List<XmltvEntry> {
        var body:InputStream=BufferedInputStream(input,16*1024)
        repeat(3) {
            val buffered=if(body is BufferedInputStream)body as BufferedInputStream else BufferedInputStream(body)
            buffered.mark(2);val a=buffered.read();val b=buffered.read();buffered.reset()
            body=if(a==0x1f && b==0x8b)BufferedInputStream(GZIPInputStream(buffered)) else buffered
        }
        val bounded=object:FilterInputStream(body){var count=0L
            fun add(n:Int):Int {if(n>0)count+=n;checkActive();if(count>96L*1024*1024)throw SAXException("XMLTV limit");return n}
            override fun read():Int=super.read().also {if(it>=0)add(1)}
            override fun read(b:ByteArray,o:Int,l:Int):Int=add(`in`.read(b,o,l))
        }
        val handler=Handler(epgId.trim(),xmltvChannelName(name),now,checkActive)
        val factory=SAXParserFactory.newInstance().apply {isNamespaceAware=true;isValidating=false}
        val parser=factory.newSAXParser().xmlReader
        listOf("http://xml.org/sax/features/external-general-entities","http://xml.org/sax/features/external-parameter-entities","http://apache.org/xml/features/nonvalidating/load-external-dtd").forEach {runCatching {parser.setFeature(it,false)}}
        parser.contentHandler=handler;parser.entityResolver=handler;parser.errorHandler=handler
        // Required: reject DOCTYPE before any entity expansion, including internal entities.
        parser.setProperty("http://xml.org/sax/properties/lexical-handler",handler)
        parser.parse(InputSource(bounded))
        return handler.result()
    }
    private class Handler(val targetId:String,val targetName:String,val now:Long,val active:()->Unit):DefaultHandler2() {
        val names=linkedSetOf<String>();val exact=mutableListOf<XmltvEntry>();val fallback=mutableListOf<XmltvEntry>()
        var depth=0;var nodes=0;var root=false;var channelId:String?=null;var programChannel:String?=null
        var start:Long?=null;var end:Long?=null;var title="";var description:String?=null;var descRank=-1;var titleRank=-1
        var field:String?=null;var fieldDepth=0;var language:String?=null;val text=StringBuilder()
        override fun error(error:org.xml.sax.SAXParseException){throw error}
        override fun fatalError(error:org.xml.sax.SAXParseException){throw error}
        override fun startDTD(name:String?,publicId:String?,systemId:String?){throw SAXException("DOCTYPE forbidden")}
        override fun resolveEntity(publicId:String?,systemId:String?):InputSource {throw SAXException("External entity forbidden")}
        override fun startElement(uri:String?,local:String?,q:String?,a:Attributes) {
            active();depth++;nodes++;if(nodes>2_000_000 || depth>64)throw SAXException("XMLTV structure limit")
            val tag=local?.takeIf {it.isNotEmpty()} ?: q.orEmpty()
            if(depth==1){if(tag!="tv")throw SAXException("Not XMLTV");root=true}
            when(tag) {
                "channel"->channelId=a.getValue("id")?.take(512)
                "programme"->{programChannel=a.getValue("channel")?.take(512);start=xmltvTime(a.getValue("start").orEmpty());end=xmltvTime(a.getValue("stop").orEmpty());title="";description=null;descRank=-1;titleRank=-1}
                "display-name","title","desc"->if(field==null){field=tag;fieldDepth=depth;language=a.getValue("lang") ?: a.getValue("http://www.w3.org/XML/1998/namespace","lang");text.setLength(0)}
            }
        }
        override fun characters(c:CharArray,start:Int,length:Int){active();if(field!=null && text.length<4096)text.append(c,start,minOf(length,4096-text.length))}
        override fun endElement(uri:String?,local:String?,q:String?) {
            val tag=local?.takeIf {it.isNotEmpty()} ?: q.orEmpty()
            if(field==tag && fieldDepth==depth) {
                val value=text.toString().trim();val rank=xmltvLanguageRank(language)
                when(tag){
                    "display-name"->if(targetName.isNotEmpty() && channelId!=null && xmltvChannelName(value)==targetName && names.size<3)names+=channelId!!
                    "title"->if(value.isNotEmpty() && rank>titleRank){title=value.take(512);titleRank=rank}
                    "desc"->if(value.isNotEmpty() && rank>descRank){description=value.take(2048);descRank=rank}
                };field=null
            }
            if(tag=="channel")channelId=null
            if(tag=="programme") {
                val s=start;val e=end;val id=programChannel
                if(s!=null && e!=null && e>s && e>=now-3*86_400_000L && s<=now+4*86_400_000L && title.isNotBlank()) {
                    val destination=if(targetId.isNotEmpty() && id==targetId)exact else if(id in names)fallback else null
                    if(destination!=null) {
                        val entry=XmltvEntry(PlaybackEpgProgram(title,s,e,description),descRank)
                        if(destination.size<200)destination+=entry else {
                            val far=destination.indices.maxBy {kotlin.math.abs(destination[it].program.startEpochMs-now)}
                            if(kotlin.math.abs(s-now)<kotlin.math.abs(destination[far].program.startEpochMs-now))destination[far]=entry
                        }
                    }
                };programChannel=null
            };depth--
        }
        fun result():List<XmltvEntry> {
            if(!root)throw SAXException("Empty XMLTV")
            return (if(exact.isNotEmpty())exact else if(names.size==1)fallback else emptyList())
                .groupBy {it.program.startEpochMs to it.program.endEpochMs}.values.map {it.maxBy {e->e.descriptionRank}}.sortedBy {it.program.startEpochMs}
        }
    }
}
