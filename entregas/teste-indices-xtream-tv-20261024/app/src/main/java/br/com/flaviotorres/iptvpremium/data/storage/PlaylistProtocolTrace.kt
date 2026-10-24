package br.com.flaviotorres.iptvpremium.data.storage

import android.util.Log
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import java.security.MessageDigest

/** Correlates a list's type without logging its ID, name, URL, user or password. */
internal object PlaylistProtocolTrace {
    private val fingerprints = object : LinkedHashMap<String, String>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 64
    }
    private val lastStorageType = object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 128
    }
    fun record(phase: String, source: PlaylistSource) {
        val fingerprint = synchronized(fingerprints) {
            fingerprints.getOrPut(source.id) {
                val digest = MessageDigest.getInstance("SHA-256").digest(source.id.toByteArray(Charsets.UTF_8))
                val hex = "0123456789abcdef"
                CharArray(12) { i ->
                    val value = digest[i / 2].toInt() and 255
                    hex[if (i % 2 == 0) value ushr 4 else value and 15]
                }.concatToString()
            }
        }
        val type = when (source) {
            is PlaylistSource.XtreamCodes -> "XTREAM"
            is PlaylistSource.M3u -> "M3U"
        }
        if (phase == "storage_read" || phase == "storage_save") {
            val unchanged = synchronized(lastStorageType) {
                lastStorageType.put("$phase:$fingerprint", type) == type
            }
            if (unchanged) return
        }
        Log.i("PlaylistProtocolPerf", "phase=$phase list=$fingerprint type=$type")
    }
}
