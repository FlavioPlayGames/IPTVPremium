package br.com.flaviotorres.iptvpremium.data.ratings

import android.content.Context

/** Personal scores only. Separate from public metadata and the disposable catalogue/cache. */
internal class PersonalRatingStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("iptv_personal_ratings_v1", Context.MODE_PRIVATE)
    fun read(identity: PersonalRatingIdentity): Int? = synchronized(lock) {
        val keys=identity.keys()
        val records=keys.mapNotNull {key->
            if(!preferences.contains(key)) return@mapNotNull null
            val value=runCatching {preferences.getInt(key,0)}.getOrNull()?.takeIf {it in 0..5}?:return@mapNotNull null
            val revision=runCatching {preferences.getLong("time_$key",0L)}.getOrDefault(0L)
            Triple(key,value,revision)
        }
        // Metadata can arrive later or omit IMDb on a later visit. The newest personal
        // choice wins over an older alias; zero is an explicit removal, not a missing record.
        val latest=records.maxByOrNull {it.third}?:return@synchronized null
        if(keys.any {key->records.none {it.first==key&&it.second==latest.second&&it.third==latest.third}}) {
            runCatching {
                val editor=preferences.edit()
                keys.forEach {editor.putInt(it,latest.second).putLong("time_$it",latest.third)}
                editor.commit()
            }
        }
        latest.second.takeIf {it>0}
    }
    fun save(identity: PersonalRatingIdentity, stars: Int?) = synchronized(lock) {
        require(stars == null || stars in 1..5)
        val keys=identity.keys();require(keys.isNotEmpty())
        val previous=runCatching {preferences.getLong("last_revision",0L)}.getOrDefault(0L)
        val revision=maxOf(System.currentTimeMillis(),previous+1)
        val editor=preferences.edit().putLong("last_revision",revision)
        keys.forEach {editor.putInt(it,stars?:0).putLong("time_$it",revision)}
        check(editor.commit()) { "rating_save_failed" }
    }
    companion object { private val lock=Any() }
}
