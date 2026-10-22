package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.domain.model.CategoryType

data class CategoryImportTask(val type: CategoryType, val categoryId: String?) {
    val key: String get() = "${type.name}:${categoryId ?: "*"}"
    val endpoint: String get() = when(type) {
        CategoryType.MOVIES -> "get_vod_streams"
        CategoryType.SERIES -> "get_series"
        CategoryType.LIVE_TV -> "get_live_streams"
    }
}
internal data class CatalogAccess(val playlist: String, val type: CategoryType, val category: String? = null)

/** One complete section per request. Access wins; idle order is movies, series, channels. */
internal object CategoryImportPolicy {
    val order = listOf(CategoryType.MOVIES, CategoryType.SERIES, CategoryType.LIVE_TV)
    fun next(tasks: List<CategoryImportTask>, done: Set<String>, access: CatalogAccess?): List<CategoryImportTask> {
        val remaining = tasks.filter { it.key !in done }
        val selected = remaining.firstOrNull { it.type == access?.type && it.categoryId == access.category }
        val section = remaining.filter { it.type == access?.type }
        return (listOfNotNull(selected) + section + remaining).distinctBy { it.key }
    }
}
