package android.content
import java.io.File
import org.json.JSONObject
class Context(val root:File) {
 companion object { const val MODE_PRIVATE=0 }
 val applicationContext get()=this
 var failCommit=false
 fun getSharedPreferences(name:String,mode:Int)=Prefs(File(root,"$name.json"),this)
}
class Prefs(val file:File,val context:Context) {
 private fun load()=if(file.exists())JSONObject(file.readText())else JSONObject()
 val all:Map<String,Any?> get(){val j=load();return j.keys().asSequence().associateWith{k->val v=j.get(k);if(v is org.json.JSONArray)(0 until v.length()).map{v.getString(it)}.toSet()else v}}
 fun getString(k:String,d:String?):String?=if(load().has(k))load().getString(k)else d
 fun getLong(k:String,d:Long)=load().optLong(k,d)
 fun getBoolean(k:String,d:Boolean)=load().optBoolean(k,d)
 fun getStringSet(k:String,d:Set<String>?):Set<String>?=(all[k] as? Set<String>)?:d
 fun edit()=Editor()
 inner class Editor {
  val updates=JSONObject(); var cleared=false
  fun putString(k:String,v:String)=apply{updates.put(k,v)}
  fun putBoolean(k:String,v:Boolean)=apply{updates.put(k,v)}
  fun putLong(k:String,v:Long)=apply{updates.put(k,v)}
  fun putStringSet(k:String,v:Set<String>)=apply{updates.put(k,org.json.JSONArray(v.toList()))}
  fun clear()=apply{cleared=true}
  fun commit():Boolean{if(context.failCommit)return false;val j=if(cleared)JSONObject()else load();updates.keys().forEach{j.put(it,updates.get(it))};file.parentFile.mkdirs();file.writeText(j.toString());return true}
  fun apply(){check(commit())}
 }
}
