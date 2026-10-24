package br.com.flaviotorres.iptvpremium.core.device

import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

internal fun encodePortalLists(lists: List<PlaylistSource>): JSONArray = JSONArray().apply {
    lists.forEach { source ->
        put(JSONObject().put("id", source.id).put("name", source.name).put("isEnabled", source.isEnabled).apply {
            when (source) {
                is PlaylistSource.M3u -> put("type", "m3u").put("playlistUrl", source.playlistUrl)
                is PlaylistSource.XtreamCodes -> put("type", "xtream").put("serverUrl", source.serverUrl)
                    .put("username", source.username).put("password", source.password)
                    .put("fallbackM3uUrl", source.fallbackM3uUrl)
            }
        })
    }
}
internal fun decodePortalLists(array: JSONArray): List<PlaylistSource> = buildList {
    require(array.length() <= 30)
    for (i in 0 until array.length()) {
        val x = array.getJSONObject(i)
        val id = x.getString("id"); val name = x.getString("name"); val enabled = x.optBoolean("isEnabled", true)
        require(id.isNotBlank() && name.isNotBlank())
        fun checkedUrl(key: String): String = x.getString(key).also { require(URL(it).protocol in listOf("http", "https")) }
        add(when (x.getString("type")) {
            "m3u" -> PlaylistSource.M3u(id, name, enabled, checkedUrl("playlistUrl"))
            "xtream" -> PlaylistSource.XtreamCodes(id, name, enabled, checkedUrl("serverUrl"), x.getString("username"), x.getString("password"),
                if (x.has("fallbackM3uUrl") && !x.isNull("fallbackM3uUrl")) checkedUrl("fallbackM3uUrl") else null)
            else -> error("Tipo de lista inválido")
        }.also { br.com.flaviotorres.iptvpremium.data.storage.PlaylistProtocolTrace.record("portal_received", it) })
    }
    require(map { it.id }.distinct().size == size)
}
