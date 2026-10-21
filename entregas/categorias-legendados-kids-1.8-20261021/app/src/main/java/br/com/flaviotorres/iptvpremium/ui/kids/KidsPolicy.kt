package br.com.flaviotorres.iptvpremium.ui.kids

import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow

internal object KidsSession { val active = MutableStateFlow(false) }
internal object KidsContent {
    fun folded(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    private val children = Regex("\\b(?:kids|infantil|infantis|criancas?|desenhos?|cartoons?|children|family|familia|infantojuvenil|infanto[ -]juvenil)\\b")
    fun category(name: String): Boolean = children.containsMatchIn(folded(name)) &&
        !br.com.flaviotorres.iptvpremium.ui.settings.AdultChannelMarker.matches(name)
}
internal object CatalogSections {
    const val SUBTITLED = "__iptv_subtitled__"
    fun subtitled(name: String): Boolean = Regex("(?:^|[^\\p{L}\\p{N}])(?:L|LEG|LEGENDADO[S]?|LEGENDADA[S]?)(?:$|[^\\p{L}\\p{N}])",RegexOption.IGNORE_CASE).containsMatchIn(name)
    private val year=Regex("(?:19|20)[0-9]{2}")
    fun orderName(name: String): String = KidsContent.folded(name).trim().removePrefix("🔒 ")
        .replace(Regex("^(?:filmes?|series?)\\s*[|:–—-]\\s*"), "").trim()
    fun priority(name: String): Int {
        val folded=orderName(name)
        return when {
            Regex("\\b(?:4k|uhd)\\b").containsMatchIn(folded) -> 1
            "lancamento" in folded || "release" in folded || year.containsMatchIn(folded) -> 0
            else -> 2
        }
    }
    /** Movie menu: special symbols and restricted/training catalogues always trail. */
    fun moviePriority(name: String): Int {
        val text = orderName(name)
        val adult = Regex("\\b(?:xxx|adultos?|adultas?)\\b|(?:18\\s*\\+|\\+\\s*18)")
        return when {
            text.firstOrNull()?.isLetterOrDigit() == false || adult.containsMatchIn(text) ||
                Regex("\\bcursos?\\b").containsMatchIn(text) -> 4
            "lancamento" in text || Regex("\\breleases?\\b").containsMatchIn(text) -> 0
            subtitled(text) -> 1
            Regex("\\b(?:4k|uhd)\\b").containsMatchIn(text) -> 2
            else -> 3
        }
    }
    fun year(name: String): Int = year.findAll(name).map { it.value.toInt() }.maxOrNull() ?: 0
}
