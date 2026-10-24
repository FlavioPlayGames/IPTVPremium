package br.com.flaviotorres.iptvpremium.ui.components

private val catalogTitleYear = Regex("""\(((?:18|19|20|21)\d{2})\)|\[((?:18|19|20|21)\d{2})\]""")

/** Display only: no network, metadata mutation, or inference from numbers inside film titles. */
internal fun catalogYearLabel(year: Int?, title: String): String {
    val displayYear = year?.takeIf { it in 1888..2100 } ?: catalogTitleYear.findAll(title)
        .mapNotNull { match -> match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }?.toIntOrNull() }
        .filter { it in 1888..2100 }.distinct().take(2).toList().singleOrNull()
    return displayYear?.let { "Ano: $it" } ?: "Ano: —"
}
