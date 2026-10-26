package br.com.flaviotorres.iptvpremium.ui.kids

/** Main-thread gate for an opening route: cancellation wins and completion is once-only. */
internal class KidsLoadingCompletion {
    private var finished=false
    private var cancelled=false
    fun cancel() { cancelled=true }
    fun completeIfReady(ready:Boolean,failed:Boolean):Boolean {
        if(!ready || failed || finished || cancelled)return false
        finished=true
        return true
    }
}
