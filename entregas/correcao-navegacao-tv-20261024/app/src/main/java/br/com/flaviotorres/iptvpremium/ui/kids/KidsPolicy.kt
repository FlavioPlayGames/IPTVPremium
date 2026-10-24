package br.com.flaviotorres.iptvpremium.ui.kids

import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow

internal object KidsSession { val active = MutableStateFlow(false) }
internal object KidsContent {
    private val marks = Regex("\\p{M}+")
    fun folded(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(marks, "").lowercase(Locale.ROOT)
    private val children = Regex("\\b(?:kids|infantil|infantis|criancas?|desenhos?|cartoons?|children|family|familia|infantojuvenil|infanto[ -]juvenil)\\b")
    // Same name markers as parental classification, without a settings UI dependency.
    private val adult = Regex("""\b(?:adult(?:s|o|os|a|as)?|xxx|porn\w*|playboy|penthouse|hustler|brazzers)\b|\b18\s*\+|\+\s*18\b|\bsexy\s*hot\b|\bsex\s*zone\b""", RegexOption.IGNORE_CASE)
    fun category(name: String): Boolean {
        val normalized = folded(name)
        return children.containsMatchIn(normalized) && !adult.containsMatchIn(normalized)
    }
}
internal object CatalogSections {
    const val SUBTITLED = "__iptv_subtitled__"
    private val subtitleMarker = Regex("(?:^|[^\\p{L}\\p{N}])(?:L|LEG|LEGENDADOS?|LEGENDADAS?)(?:$|[^\\p{L}\\p{N}])",RegexOption.IGNORE_CASE)
    fun subtitled(name: String): Boolean = subtitleMarker.containsMatchIn(name)
    private val year=Regex("(?:19|20)[0-9]{2}")
    private val prefix = Regex("^(?:filmes?|series?|canais|channels?)\\s*[|:–—-]\\s*")
    private val adultMarker = Regex("\\b(?:xxx|adultos?|adultas?)\\b|(?:18\\s*\\+|\\+\\s*18)")
    private val courseMarker = Regex("\\bcursos?\\b")
    private val releaseMarker = Regex("\\breleases?\\b")
    private val uhdMarker = Regex("\\b(?:4k|uhd)\\b")
    fun orderName(name: String): String = KidsContent.folded(name).trim().removePrefix("🔒 ")
        .replace(prefix, "").trim()
    /** Movie menu: special symbols and restricted/training catalogues always trail. */
    fun moviePriority(name: String): Int {
        val text = orderName(name)
        return when {
            text.firstOrNull()?.isLetterOrDigit() == false || adultMarker.containsMatchIn(text) ||
                courseMarker.containsMatchIn(text) -> 4
            "lancamento" in text || releaseMarker.containsMatchIn(text) -> 0
            subtitled(text) -> 1
            uhdMarker.containsMatchIn(text) -> 2
            else -> 3
        }
    }
    /** One stable category order for films, series and channels; never scans media items. */
    private data class OrderKey(val priority: Int, val year: Int, val name: String)
    fun <T> categoryComparator(name: (T) -> String): Comparator<T> {
        // One cache per sort, discarded with its comparator; no catalog retained globally.
        val keys = HashMap<String, OrderKey>()
        fun key(item: T): OrderKey {
            val raw = name(item)
            return keys.getOrPut(raw) {
                val priority = moviePriority(raw)
                OrderKey(priority, if (priority == 0) year(raw) else 0, orderName(raw))
            }
        }
        return Comparator { left, right ->
            val a = key(left)
            val b = key(right)
            val priority = a.priority.compareTo(b.priority)
            if (priority != 0) priority else {
                val year = b.year.compareTo(a.year)
                if (year != 0) year else a.name.compareTo(b.name)
            }
        }
    }
    fun year(name: String): Int = year.findAll(name).maxOfOrNull { it.value.toInt() } ?: 0
}
