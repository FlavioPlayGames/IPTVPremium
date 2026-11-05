package android.content
class Context {
    val applicationContext get()=this
    private val prefs=mutableMapOf<String,SharedPreferences>()
    fun getSharedPreferences(name:String,mode:Int)=prefs.getOrPut(name){SharedPreferences()}
    companion object {const val MODE_PRIVATE=0}
}
class SharedPreferences {
    private val values=mutableMapOf<String,Any>()
    fun getInt(key:String,fallback:Int)=values[key] as? Int ?:fallback
    fun getBoolean(key:String,fallback:Boolean)=values[key] as? Boolean ?:fallback
    fun getString(key:String,fallback:String?)=values[key] as? String ?:fallback
    private val listeners=mutableSetOf<OnSharedPreferenceChangeListener>()
    fun interface OnSharedPreferenceChangeListener {fun onSharedPreferenceChanged(p:SharedPreferences,key:String)}
    fun registerOnSharedPreferenceChangeListener(l:OnSharedPreferenceChangeListener){listeners+=l}
    fun unregisterOnSharedPreferenceChangeListener(l:OnSharedPreferenceChangeListener){listeners-=l}
    fun edit()=Editor()
    inner class Editor {
        private val pending=mutableMapOf<String,Any>()
        fun putInt(k:String,v:Int)=apply{pending[k]=v}
        fun putString(k:String,v:String)=apply{pending[k]=v}
        fun putBoolean(k:String,v:Boolean)=apply{pending[k]=v}
        fun apply(){values.putAll(pending);pending.keys.forEach{key->listeners.toList().forEach{it.onSharedPreferenceChanged(this@SharedPreferences,key)}}}
    }
}
