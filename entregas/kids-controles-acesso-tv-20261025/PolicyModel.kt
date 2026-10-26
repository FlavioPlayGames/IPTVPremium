package br.com.flaviotorres.iptvpremium.ui.settings
import java.text.Normalizer
import java.util.Locale
/** Regras locais, pequenas e independentes do cache substituivel das listas. */
internal data class ParentalRules(
    val general: Boolean = true,
    val adultChannels: Boolean = false,
    val all: Set<String> = emptySet(),
    val categories: Map<String, Set<String>> = emptyMap(),
    val kidsOnly: Boolean = false,
    val startInKids: Boolean = false,
    val kidsBlockedSections: Set<String> = emptySet()
)
internal data class ParentalAccessState(
    val enabled: Boolean,
    val rules: ParentalRules,
    val grants: Set<String>,
    val kidsCatalog: Boolean = false
) {
    fun scope(playlist: String, type: String) = (if(kidsCatalog) "kids/" else "") + "$playlist/$type"
    fun hasRestrictions(playlist: String, type: String): Boolean = kidsCatalog || (enabled && rules.kidsOnly) || enabled &&
        ("__invalid__" in rules.all || if (type == "LIVE_TV") rules.adultChannels
            else scope(playlist, type) in rules.all || rules.categories[scope(playlist, type)].orEmpty().isNotEmpty())
    fun categoryKey(playlist: String, type: String, id: String?) = scope(playlist, type) + "/" + (id?.takeIf { it.isNotBlank() } ?: "__uncategorized__")
    fun blocked(playlist: String, type: String, id: String?, name: String = "", categoryName: String = ""): Boolean {
        if (enabled && kidsCatalog && type in rules.kidsBlockedSections) return true
        if (enabled && rules.kidsOnly && !kidsCatalog) return true
        if (kidsCatalog && (AdultChannelMarker.matches(name) ||
            (categoryName.isNotBlank() && !br.com.flaviotorres.iptvpremium.ui.kids.KidsContent.category(categoryName)))) return true
        if (!enabled) return false
        if ("__invalid__" in rules.all) return true
        val section = scope(playlist, type)
        val category = categoryKey(playlist, type, id)
        if (type == "LIVE_TV" && !kidsCatalog) return rules.adultChannels && "adult_live" !in grants &&
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
