package br.com.flaviotorres.iptvpremium.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.playlistDataStore by preferencesDataStore(
    name = "playlist_storage"
)

class PlaylistStorage internal constructor(
    private val store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) {
    constructor(context: Context) : this(context.playlistDataStore)

    private val playlistsKey =
        stringPreferencesKey("playlists")

    private val activePlaylistIdKey =
        stringPreferencesKey("active_playlist_id")

    private val portalLoadKey = stringPreferencesKey("portal_load_request")
    private val changedDisabledCatalogsKey = stringSetPreferencesKey("changed_disabled_catalogs")
    private val completedPortalLoadKey = stringPreferencesKey("portal_catalog_completed_request")

    suspend fun isPortalCatalogReady(request: String): Boolean {
        val preferences = store.data.first()
        return preferences[portalLoadKey] == request && preferences[completedPortalLoadKey] == request
    }

    suspend fun markPortalCatalogReady(request: String) {
        store.edit { preferences ->
            if (preferences[portalLoadKey] == request) preferences[completedPortalLoadKey] = request
        }
    }
    data class Selection(val playlists: List<PlaylistSource>, val activeId: String?, val loadRequest: String?)
    // One emission for all three values: never combine an old active ID with a new list.
    val selection: Flow<Selection> = store.data.map { preferences ->
        Selection(decodePlaylists(preferences[playlistsKey].orEmpty()),
            preferences[activePlaylistIdKey]?.takeIf { it.isNotBlank() }, preferences[portalLoadKey])
    }
    /** Serialize the registry check with all local/portal edits. Never modifies preferences. */
    internal suspend fun withProtectedCatalogIds(action: suspend (Set<String>, Int) -> Unit) {
        store.edit { preferences ->
            val raw = preferences[playlistsKey]
            val protected = br.com.flaviotorres.iptvpremium.core.storage.RegisteredCatalogIds.parse(
                raw, preferences[activePlaylistIdKey], preferences[portalLoadKey]
            )
            action(protected, JSONArray(raw).length())
        }
    }

    /** Hold the registry edit guard through a category publish; portal edits cannot race it. */
    internal suspend fun withCurrentCatalogSource(source: PlaylistSource, action: suspend () -> Unit): Boolean {
        var accepted = false
        store.edit { preferences ->
            val current = decodePlaylists(preferences[playlistsKey].orEmpty()).firstOrNull { it.id == source.id }
            if (current != null && current.isEnabled && sameCatalogSource(current, source)) {
                action()
                accepted = true
            }
        }
        return accepted
    }

    suspend fun requestActiveCatalogLoad() {
        store.edit { preferences ->
            val id = preferences[activePlaylistIdKey] ?: return@edit
            if (decodePlaylists(preferences[playlistsKey].orEmpty()).none { it.id == id && it.isEnabled }) return@edit
            if (preferences[portalLoadKey] == null) preferences[portalLoadKey] = JSONObject().put("id", id)
                .put("request", java.util.UUID.randomUUID().toString()).toString()
        }
    }

    val portalLoadRequest: Flow<String?> = store.data.map { it[portalLoadKey] }

    suspend fun finishPortalLoad(request: String) {
        store.edit { preferences ->
            if (preferences[portalLoadKey] == request) {
                preferences.remove(portalLoadKey)
                preferences.remove(completedPortalLoadKey)
            }
        }
    }

    val playlists: Flow<List<PlaylistSource>> =
        store.data.map { preferences ->
            decodePlaylists(
                preferences[playlistsKey]
                    .orEmpty()
            )
        }

    val activePlaylistId: Flow<String?> =
        store.data.map { preferences ->
            preferences[activePlaylistIdKey]
                ?.takeIf { it.isNotBlank() }
        }

    suspend fun savePlaylists(
        playlists: List<PlaylistSource>
    ) {
        store.edit { preferences ->
            preferences[playlistsKey] =
                encodePlaylists(playlists)
        }
    }

    /** Apply a portal snapshot only if no local edit happened during its network request. */
    suspend fun applyPortalSnapshot(expected: List<PlaylistSource>, remote: List<PlaylistSource>): Boolean {
        var applied = false
        store.edit { preferences ->
            if (decodePlaylists(preferences[playlistsKey].orEmpty()) == expected) {
                preferences[playlistsKey] = encodePlaylists(remote)
                val active = preferences[activePlaylistIdKey]
                if (remote.none { it.id == active }) {
                    remote.firstOrNull { it.isEnabled }?.id?.let { preferences[activePlaylistIdKey] = it }
                        ?: preferences.remove(activePlaylistIdKey)
                }
                applied = true
            }
        }
        return applied
    }

    /** Apply one portal operation atomically against the latest local lists. */
    suspend fun applyPortalOperation(id: String, source: PlaylistSource?) {
        store.edit { preferences ->
            val current = decodePlaylists(preferences[playlistsKey].orEmpty())
            val previous = current.firstOrNull { it.id == id }
            val catalogChanged = source != null && !sameCatalogSource(previous, source)
            val changedDisabled = preferences[changedDisabledCatalogsKey].orEmpty().toMutableSet()
            if (source != null && !source.isEnabled && catalogChanged) changedDisabled.add(id)
            val next = if (source == null) current.filterNot { it.id == id }
                else if (current.any { it.id == id }) current.map { if (it.id == id) source else it }
                else current + source
            require(next.size <= 30)
            preferences[playlistsKey] = encodePlaylists(next)
            if (source != null && source.isEnabled) {
                preferences[activePlaylistIdKey] = id
                // Repeated delivery, rename or enabled-state change reuses the catalogue.
                // Preserve any genuine earlier request until its import has finished.
                if (catalogChanged || id in changedDisabled) {
                    changedDisabled.remove(id)
                    preferences.remove(completedPortalLoadKey)
                    preferences[portalLoadKey] = JSONObject().put("id", id)
                        .put("request", java.util.UUID.randomUUID().toString()).toString()
                }
            } else if (source == null) {
                changedDisabled.remove(id)
                val pending = preferences[portalLoadKey]
                if (pending != null && runCatching { JSONObject(pending).optString("id") == id }.getOrDefault(false)) {
                    preferences.remove(portalLoadKey)
                    preferences.remove(completedPortalLoadKey)
                }
            }
            if (changedDisabled.isEmpty()) preferences.remove(changedDisabledCatalogsKey)
            else preferences[changedDisabledCatalogsKey] = changedDisabled
            val active = preferences[activePlaylistIdKey]
            if (next.none { it.id == active }) {
                next.firstOrNull { it.isEnabled }?.id?.let { preferences[activePlaylistIdKey] = it }
                    ?: preferences.remove(activePlaylistIdKey)
            }
        }
    }

    suspend fun saveActivePlaylistId(
        playlistId: String?
    ) {
        store.edit { preferences ->
            if (playlistId.isNullOrBlank()) {
                preferences.remove(
                    activePlaylistIdKey
                )
            } else {
                preferences[activePlaylistIdKey] =
                    playlistId
            }
        }
    }

    suspend fun clear() {
        store.edit { preferences ->
            preferences.remove(playlistsKey)
            preferences.remove(activePlaylistIdKey)
            preferences.remove(changedDisabledCatalogsKey)
            preferences.remove(portalLoadKey)
            preferences.remove(completedPortalLoadKey)
        }
    }

    private fun encodePlaylists(
        playlists: List<PlaylistSource>
    ): String {
        val array = JSONArray()

        playlists.forEach { playlist ->
            val item = JSONObject()

            item.put(
                "id",
                playlist.id
            )

            item.put(
                "name",
                playlist.name
            )

            item.put(
                "isEnabled",
                playlist.isEnabled
            )

            when (playlist) {
                is PlaylistSource.XtreamCodes -> {
                    item.put(
                        "type",
                        "xtream"
                    )

                    item.put(
                        "serverUrl",
                        playlist.serverUrl
                    )

                    item.put(
                        "username",
                        playlist.username
                    )

                    item.put(
                        "password",
                        playlist.password
                    )
                    item.put("fallbackM3uUrl", playlist.fallbackM3uUrl)
                }

                is PlaylistSource.M3u -> {
                    item.put(
                        "type",
                        "m3u"
                    )

                    item.put(
                        "playlistUrl",
                        playlist.playlistUrl
                    )
                }
            }

            array.put(item)
        }

        return array.toString()
    }

    private fun decodePlaylists(
        raw: String
    ): List<PlaylistSource> {
        if (raw.isBlank()) {
            return emptyList()
        }

        return runCatching {
            val array = JSONArray(raw)

            buildList {
                for (
                index in 0 until array.length()
                ) {
                    val item =
                        array.optJSONObject(index)
                            ?: continue

                    val id =
                        item.optString("id")

                    val name =
                        item.optString("name")

                    val isEnabled =
                        item.optBoolean(
                            "isEnabled",
                            true
                        )

                    when (
                        item.optString("type")
                    ) {
                        "xtream" -> {
                            add(
                                PlaylistSource.XtreamCodes(
                                    id = id,
                                    name = name,
                                    isEnabled = isEnabled,
                                    serverUrl =
                                        item.optString(
                                            "serverUrl"
                                        ),
                                    username =
                                        item.optString(
                                            "username"
                                        ),
                                    password =
                                        item.optString(
                                            "password"
                                        ),
                                    fallbackM3uUrl = item.optString("fallbackM3uUrl").takeIf {
                                        it.startsWith("https://", true) || it.startsWith("http://", true)
                                    }
                                )
                            )
                        }

                        "m3u" -> {
                            add(
                                PlaylistSource.M3u(
                                    id = id,
                                    name = name,
                                    isEnabled = isEnabled,
                                    playlistUrl =
                                        item.optString(
                                            "playlistUrl"
                                        )
                                )
                            )
                        }
                    }
                }
            }
        }.getOrElse {
            emptyList()
        }
    }
}
// IPTV_PORTAL_PRODUCTION_V1

// IPTV_SUBTITLE_LAYOUT_PORTAL_LOAD_V1

// IPTV_LIST_LOADING_AND_PLAYER_INPUT_V1
