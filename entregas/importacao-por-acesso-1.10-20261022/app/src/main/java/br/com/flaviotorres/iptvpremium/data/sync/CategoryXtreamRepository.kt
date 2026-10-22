package br.com.flaviotorres.iptvpremium.data.sync

import br.com.flaviotorres.iptvpremium.domain.model.*

/** Keeps transport type Xtream, even for credentials converted by the portal. */
interface CategoryXtreamRepository {
    suspend fun importCategoryIndex(source: PlaylistSource.XtreamCodes, sink: XtreamCatalogSink): List<Category>
    suspend fun importCategory(source: PlaylistSource.XtreamCodes, task: CategoryImportTask, sink: XtreamCatalogSink)
}

class CategoryFilterUnsupportedException : IllegalStateException("O provedor não respeitou o filtro da categoria Xtream.")
