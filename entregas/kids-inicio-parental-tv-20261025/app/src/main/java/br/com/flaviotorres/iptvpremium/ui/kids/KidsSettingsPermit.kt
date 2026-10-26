package br.com.flaviotorres.iptvpremium.ui.kids

/** Ephemeral permission; no configured PIN or a stale/cancelled attempt can grant access. */
internal class KidsSettingsPermit(private val epoch:Long) {
    var approved=false;private set
    private var cancelled=false
    fun cancel(){cancelled=true;approved=false}
    fun approve(valid:Boolean,configured:Boolean,currentEpoch:Long):Boolean {
        approved=valid && configured && currentEpoch==epoch && !cancelled
        return approved
    }
}
