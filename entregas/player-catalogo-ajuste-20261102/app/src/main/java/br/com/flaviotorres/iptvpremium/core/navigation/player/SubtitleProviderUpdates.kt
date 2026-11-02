package br.com.flaviotorres.iptvpremium.core.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A new portal key invalidates search state, not the playing video or loaded subtitle cues. */
internal object SubtitleProviderUpdates {
    private val state=MutableStateFlow(0L)
    val revision=state.asStateFlow()
    fun changed(){synchronized(this){state.value=state.value+1}}
}
