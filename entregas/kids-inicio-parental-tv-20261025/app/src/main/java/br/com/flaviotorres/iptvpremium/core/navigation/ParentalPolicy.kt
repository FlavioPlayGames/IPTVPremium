package br.com.flaviotorres.iptvpremium.ui.settings

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.text.Normalizer
import java.util.Locale
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** Regras locais, pequenas e independentes do cache substituivel das listas. */
internal data class ParentalRules(
    val general: Boolean = true,
    val adultChannels: Boolean = false,
    val all: Set<String> = emptySet(),
    val categories: Map<String, Set<String>> = emptyMap(),
    val kidsOnly: Boolean = false,
    val startInKids: Boolean = false
)
internal data class ParentalAccessState(
    val enabled: Boolean,
    val rules: ParentalRules,
    val grants: Set<String>,
    val kidsCatalog: Boolean = false
) {
    fun scope(playlist: String, type: String) = "$playlist/$type"
    fun hasRestrictions(playlist: String, type: String): Boolean = kidsCatalog || (enabled && rules.kidsOnly) || enabled &&
        ("__invalid__" in rules.all || if (type == "LIVE_TV") rules.adultChannels
            else scope(playlist, type) in rules.all || rules.categories[scope(playlist, type)].orEmpty().isNotEmpty())
    fun categoryKey(playlist: String, type: String, id: String?) = scope(playlist, type) + "/" + (id?.takeIf { it.isNotBlank() } ?: "__uncategorized__")
    fun blocked(playlist: String, type: String, id: String?, name: String = "", categoryName: String = ""): Boolean {
        if (enabled && rules.kidsOnly && !kidsCatalog) return true
        if (kidsCatalog && (AdultChannelMarker.matches(name) ||
            (categoryName.isNotBlank() && !br.com.flaviotorres.iptvpremium.ui.kids.KidsContent.category(categoryName)))) return true
        if (!enabled) return false
        if ("__invalid__" in rules.all) return true
        val section = scope(playlist, type)
        val category = categoryKey(playlist, type, id)
        if (type == "LIVE_TV") return rules.adultChannels && "adult_live" !in grants &&
            (AdultChannelMarker.matches(name) || AdultChannelMarker.matches(categoryName))
        val chosen = id?.takeIf { it.isNotBlank() } ?: "__uncategorized__"
        return (section in rules.all || chosen in rules.categories[section].orEmpty()) &&
            section !in grants && category !in grants
    }
}
internal object AdultChannelMarker {
    private val marker = Regex("""\b(?:adult(?:s|o|os|a|as)?|xxx|porn\w*|playboy|penthouse|hustler|brazzers)\b|\b18\s*\+|\+\s*18\b|\bsexy\s*hot\b|\bsex\s*zone\b""", RegexOption.IGNORE_CASE)
    fun matches(value: String): Boolean {
        val folded = Normalizer.normalize(value, Normalizer.Form.NFKD).replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        return marker.containsMatchIn(folded)
    }
}
internal object ParentalPolicy {
    val rules = MutableStateFlow(ParentalRules())
    val grants = MutableStateFlow<Set<String>>(emptySet())
    val accessFlow = combine(ParentalSession.enabled, rules, grants, br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active) { enabled, rules, grants, kids -> ParentalAccessState(enabled, rules, grants, kids) }
    private lateinit var file: AtomicFile
    private val lock = Any()
    fun initialize(context: Context) = synchronized(lock) {
        file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "parental_rules_v2.json"))
        rules.value = try {
            if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) ParentalRules()
            else decode(JSONObject(file.readFully().toString(Charsets.UTF_8)))
        } catch (_: Exception) {
            // Falha fechada: preserva a barreira geral por PIN; nao apaga regras silenciosamente.
            ParentalRules(general = true, adultChannels = true, all = setOf("__invalid__"))
        }
        grants.value = emptySet()
    }
    fun access() = ParentalAccessState(ParentalSession.enabled.value, rules.value, grants.value, br.com.flaviotorres.iptvpremium.ui.kids.KidsSession.active.value)
    fun update(change: (ParentalRules) -> ParentalRules) = synchronized(lock) {
        val next = change(rules.value)
        val categories = JSONObject()
        next.categories.forEach { (key, ids) -> categories.put(key, JSONArray(ids.sorted())) }
        val data = JSONObject().put("general", next.general).put("adultChannels", next.adultChannels)
            .put("kidsOnly", next.kidsOnly).put("startInKids",next.startInKids).put("all", JSONArray(next.all.sorted())).put("categories", categories)
        val stream = file.startWrite()
        try { stream.write(data.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
        rules.value = next
        grants.value = emptySet()
    }
    fun grant(playlist: String, type: String, id: String?, wholeSection: Boolean = false) {
        val access = access()
        val key = if (type == "LIVE_TV") "adult_live" else if (wholeSection) access.scope(playlist, type) else access.categoryKey(playlist, type, id)
        grants.value = grants.value + key
    }
    fun reset() = update { ParentalRules(general = false) }
    private fun decode(data: JSONObject): ParentalRules {
        fun strings(array: JSONArray?): Set<String> = if (array == null) emptySet() else
            (0 until array.length()).map { array.getString(it) }.toSet()
        val categories = data.optJSONObject("categories") ?: JSONObject()
        return ParentalRules(data.optBoolean("general", true), data.optBoolean("adultChannels", false),
            strings(data.optJSONArray("all")), categories.keys().asSequence().associateWith { strings(categories.getJSONArray(it)) }, data.optBoolean("kidsOnly", false), data.optBoolean("startInKids",false))
    }
}
