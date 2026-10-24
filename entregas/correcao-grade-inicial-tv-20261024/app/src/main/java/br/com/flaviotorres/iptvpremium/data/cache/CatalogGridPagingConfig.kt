package br.com.flaviotorres.iptvpremium.data.cache

import androidx.paging.PagingConfig

/** Room supports indexed jumps: restoring a distant grid position must not scan intervening pages. */
internal fun catalogGridPagingConfig(): PagingConfig = PagingConfig(
    pageSize = 60,
    initialLoadSize = 30,
    prefetchDistance = 15,
    enablePlaceholders = true,
    maxSize = CatalogMemoryPolicy.pagingWindow,
    jumpThreshold = 60
)
