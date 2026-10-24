package br.com.flaviotorres.iptvpremium.data.cache

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** A changed query/access never keeps the previous rows while constructing the new pager. */
internal fun <T : Any> clearPagingBeforeLoad(load: suspend () -> Flow<PagingData<T>>): Flow<PagingData<T>> = flow {
    emit(PagingData.empty(sourceLoadStates = LoadStates(
        refresh = LoadState.Loading,
        prepend = LoadState.NotLoading(false),
        append = LoadState.NotLoading(false)
    )))
    emitAll(load())
}
