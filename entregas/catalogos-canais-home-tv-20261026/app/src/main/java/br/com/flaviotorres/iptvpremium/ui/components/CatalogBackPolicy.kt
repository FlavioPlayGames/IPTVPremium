package br.com.flaviotorres.iptvpremium.ui.components

internal enum class CatalogBackAction { CLOSE_KEYBOARD, CATEGORIES, EXIT }
internal fun catalogBackAction(keyboard: Boolean, categories: Boolean): CatalogBackAction = when {
    keyboard -> CatalogBackAction.CLOSE_KEYBOARD
    categories -> CatalogBackAction.EXIT
    else -> CatalogBackAction.CATEGORIES
}
