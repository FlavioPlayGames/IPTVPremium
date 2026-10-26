package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.data.resolver.M3uXtreamResolver
import br.com.flaviotorres.iptvpremium.domain.model.PlaylistSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Locale

internal interface M3uTransportStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
}

/** Negotiates once for this exact configuration. A failed API import NEVER chooses M3U. */
class M3uTransportPolicy internal constructor(
    private val store: M3uTransportStore,
    private val verifyAccount: suspend (PlaylistSource.XtreamCodes) -> Boolean
) {
    suspend fun select(source: PlaylistSource.M3u): PlaylistSource.XtreamCodes? {
        val selected = store.read(key(source))
        val wasApi = selected?.endsWith(":API") == true
        val candidate = candidate(source) ?: run {
            check(!wasApi) { "A nova configuração não permite manter a API. Cadastre outra entrada para mudar o formato." }
            return null
        }
        if (selected == fingerprint(source) + ":API") return candidate
        if (selected == fingerprint(source) + ":M3U") return null
        val api = verifyAccount(candidate)
        currentCoroutineContext().ensureActive()
        check(api || !wasApi) { "A API escolhida anteriormente não foi confirmada. A lista atual foi preservada." }
        store.write(key(source), fingerprint(source) + if (api) ":API" else ":M3U")
        return candidate.takeIf { api }
    }

    suspend fun hasApiSelection(source: PlaylistSource.M3u): Boolean = store.read(key(source))?.endsWith(":API") == true

    /** Used by deferred channels only; never starts a probe from navigation. */
    suspend fun selected(source: PlaylistSource.M3u): PlaylistSource.XtreamCodes? {
        val candidate = candidate(source) ?: return null
        return candidate.takeIf { store.read(key(source)) == fingerprint(source) + ":API" }
    }

    private fun candidate(source: PlaylistSource.M3u): PlaylistSource.XtreamCodes? {
        val clean = source.playlistUrl.trim().replace("&amp;", "&", true).replace("\\&", "&")
        val uri = runCatching { URI(clean) }.getOrNull() ?: return null
        // A filtered/signed/custom get.php must retain its exact M3U semantics.
        val allowed = setOf("username", "password", "type", "output")
        val parameters = linkedMapOf<String, String>()
        for (pair in uri.rawQuery.orEmpty().split('&')) {
            val name = runCatching { URLDecoder.decode(pair.substringBefore('='), "UTF-8").lowercase(Locale.ROOT) }.getOrNull() ?: return null
            if (name !in allowed) return null
            val value = runCatching { URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8") }.getOrNull() ?: return null
            if (parameters[name]?.let { it != value } == true) return null
            parameters[name] = value
        }
        if (parameters["type"]?.lowercase(Locale.ROOT)?.let { it !in setOf("m3u", "m3u_plus") } == true) return null
        if (parameters["output"]?.lowercase(Locale.ROOT)?.let { it !in setOf("ts", "mpegts") } == true) return null
        if (uri.rawFragment != null) return null
        return M3uXtreamResolver.resolve(source)
    }

    private fun key(source: PlaylistSource.M3u) = "source_" + digest(source.id)
    private fun fingerprint(source: PlaylistSource.M3u) = digest(source.id + "\u0000" + source.playlistUrl.trim())
    private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
