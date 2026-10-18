package br.com.flaviotorres.iptvpremium.data.repository

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Recovers only an intact last root-level episodes value. Broken optional metadata
 * is discarded, never repaired heuristically. The node itself must parse normally;
 * IDs and season/episode mappings remain the provider's original JSON values.
 */
internal fun recoverCompleteSeriesEpisodes(json: String): JSONObject? {
    if (json.length > 8 * 1024 * 1024 || !json.startsWith('{')) return null
    val stack = ArrayList<Char>()
    var quoted = false
    var escaped = false
    var rootKeyExpected = false
    val candidates = ArrayList<Int>(1)
    var i = 0
    while (i < json.length) {
        val c = json[i]
        if (quoted) {
            if (escaped) escaped = false
            else if (c == '\\') escaped = true
            else if (c == '"') quoted = false
        } else when (c) {
            '"' -> {
                if (stack.size == 1 && rootKeyExpected && json.startsWith("\"episodes\"", i)) {
                    var at = i + 10
                    while (at < json.length && json[at].isWhitespace()) at++
                    if (at < json.length && json[at] == ':') {
                        at++
                        while (at < json.length && json[at].isWhitespace()) at++
                        if (at < json.length && (json[at] == '{' || json[at] == '[')) candidates += at
                    }
                }
                quoted = true
            }
            '{', '[' -> { stack += c; if (stack.size == 1) rootKeyExpected = true }
            '}', ']' -> {
                if (stack.isEmpty() || stack.last() != if (c == '}') '{' else '[') return null
                stack.removeAt(stack.lastIndex)
            }
            ',' -> if (stack.size == 1) rootKeyExpected = true
            ':' -> if (stack.size == 1) rootKeyExpected = false
        }
        i++
    }
    if (candidates.size != 1 || quoted || escaped) return null
    return try {
        val tokens = JSONTokener(json.substring(candidates.single()))
        val episodes = tokens.nextValue()
        if (episodes !is JSONObject && episodes !is JSONArray) return null
        // Complete episodes may survive only the missing outer root brace. Never
        // accept a cut-off episode/node, trailing fields, garbage or extra braces.
        val tail = tokens.nextClean()
        if (tail != '\u0000' && (tail != '}' || tokens.nextClean() != '\u0000')) return null
        JSONObject().put("episodes", episodes)
    } catch (_: org.json.JSONException) { null }
}
