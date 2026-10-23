package br.com.flaviotorres.iptvpremium.data.catalog

import android.util.JsonReader
import android.util.JsonToken
import br.com.flaviotorres.iptvpremium.data.cache.entity.*
import java.io.Reader
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Original implementation: no textual framing, JSONArray, domain lists or whole-catalog heap. */
internal object NativeXtreamReader {
    data class Fields(val id:String?,val name:String?,val category:String?,val poster:String?,val backdrop:String?,
        val direct:String?,val epg:String?,val extension:String?,val year:Int?,val rating:Double?,
        val duration:Long?,val added:Long?,val plot:String?,val imdb:String?,val tmdb:String?,
        val logo:String?,val genres:String?,val trailer:String?)
    data class ReadStats(val raw:Long,val accepted:Long)

    suspend fun read(input:Reader,action:String,category:Boolean=false,emit:suspend(Fields)->Unit):ReadStats {
        var raw=0L;var accepted=0L
        val ctx=currentCoroutineContext()
        JsonReader(input).use { reader ->
            reader.isLenient=false
            suspend fun array() {
                check(reader.peek()==JsonToken.BEGIN_ARRAY){"Formato de catálogo inválido."}
                reader.beginArray()
                while(reader.hasNext()) {
                    ctx.ensureActive();raw++
                    if(reader.peek()!=JsonToken.BEGIN_OBJECT){reader.skipValue();continue}
                    val fields=fields(reader,category)
                    if(!fields.id.isNullOrBlank()) {emit(fields);accepted++}
                }
                reader.endArray()
            }
            when(reader.peek()) {
                JsonToken.BEGIN_ARRAY->array()
                JsonToken.BEGIN_OBJECT->{
                    reader.beginObject();var found=false
                    while(reader.hasNext()) {
                        val key=reader.nextName()
                        if(key in setOf(action,action.removePrefix("get_"),"items","data","categories") && reader.peek()==JsonToken.BEGIN_ARRAY && !found) {
                            array();found=true
                        }else reader.skipValue()
                    }
                    reader.endObject();check(found){"O provedor não retornou um catálogo."}
                }
                else->error("O provedor não retornou um catálogo.")
            }
            check(reader.peek()==JsonToken.END_DOCUMENT){"Dados extras depois do catálogo."}
        }
        check(raw==0L || accepted>0L){"Catálogo sem identificadores válidos."}
        return ReadStats(raw,accepted)
    }

    private fun fields(r:JsonReader,categoryRow:Boolean):Fields {
        var id:String?=null;var name:String?=null;var cat:String?=null;var poster:String?=null;var backdrop:String?=null
        var direct:String?=null;var epg:String?=null;var ext:String?=null;var year:Int?=null;var rating:Double?=null
        var duration:Long?=null;var added:Long?=null;var plot:String?=null;var imdb:String?=null;var tmdb:String?=null
        var logo:String?=null;var genres:String?=null;var trailer:String?=null
        r.beginObject()
        while(r.hasNext())when(r.nextName()) {
            "stream_id","series_id"->id=r.scalar()?:id
            "category_id"->{val value=r.scalar();if(categoryRow)id=value else cat=value}
            "category_ids"->{val value=r.firstScalar();if(cat==null)cat=value}
            "name","category_name"->name=r.scalar()?:name
            "stream_icon","cover"->poster=r.scalar()?:poster
            "backdrop_path","backdrop"->backdrop=r.firstScalar()?:backdrop
            "direct_source"->direct=r.scalar()
            "epg_channel_id"->epg=r.scalar()
            "container_extension"->ext=r.scalar()
            "year"->year=r.scalar()?.take(4)?.toIntOrNull()
            "releaseDate","release_date","releasedate"->{val date=r.scalar();if(year==null)year=date?.take(4)?.toIntOrNull()}
            "rating"->rating=r.scalar()?.toDoubleOrNull()?.takeIf{it.isFinite()}
            "duration_secs","duration_seconds"->duration=r.scalar()?.toLongOrNull()
            "added","last_modified"->{val value=r.scalar()?.toLongOrNull();if(value!=null)added=value}
            "plot","description"->{val value=r.scalar();if(plot==null)plot=value}
            "imdb_id"->imdb=r.scalar()
            "tmdb_id","tmdb"->tmdb=r.scalar()?.takeIf{it.all(Char::isDigit)}?:tmdb
            "logo","clearlogo","title_logo"->logo=r.firstScalar()?:logo
            "genre"->genres=r.firstScalar()
            "youtube_trailer"->trailer=r.scalar()
            else->r.skipValue()
        }
        r.endObject()
        return Fields(id,name,cat,poster,backdrop,direct,epg,ext,year,rating,duration,added,plot,imdb,tmdb,logo,genres,trailer)
    }
    private fun JsonReader.scalar():String?=when(peek()) {
        JsonToken.STRING,JsonToken.NUMBER->nextString().trim().also{check(it.length<=1_048_576){"Campo excessivamente grande."}}.takeUnless{it.isBlank()||it.equals("null",true)}
        JsonToken.NULL->{nextNull();null}
        else->{skipValue();null}
    }
    private fun JsonReader.firstScalar():String? {
        if(peek()!=JsonToken.BEGIN_ARRAY)return scalar()
        beginArray();val first=if(hasNext())scalar()else null
        while(hasNext())skipValue()
        endArray();return first
    }
}
