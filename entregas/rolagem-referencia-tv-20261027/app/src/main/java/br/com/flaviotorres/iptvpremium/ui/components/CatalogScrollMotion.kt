package br.com.flaviotorres.iptvpremium.ui.components

/** Trial: about 400 ms per row, with a smooth start and an aligned stop.
 * Retargets retain velocity. Relative distances avoid precision drift deep in a catalogue.
 * Key release, paging, boundaries and final alignment remain owned by the navigator. */
internal class CatalogScrollMotion {
    var velocity:Float=0f
        private set
    private var initialized=false
    private var previousDistance=0f
    private var previousDelta=0f
    private var span=0f
    private var startVelocity=0f
    private var elapsed=0f
    private var duration=.4f

    @Suppress("UNUSED_PARAMETER")
    fun advance(distance:Float,elapsedSeconds:Float,rowHeight:Float,speedFactor:Float=1f):Float {
        val dt=elapsedSeconds.coerceIn(.001f,.05f)
        val expectedDistance=previousDistance-previousDelta
        if(!initialized || kotlin.math.abs(distance-expectedDistance)>maxOf(.5f,rowHeight*.005f)) {
            initialized=true
            span=distance
            elapsed=0f
            duration=(.4f*kotlin.math.abs(distance)/rowHeight.coerceAtLeast(1f)).coerceIn(.10f,.80f)
            // Monotone Hermite interpolation: no overshoot after retargeting/reversal.
            val bound=3f*kotlin.math.abs(distance)/duration
            startVelocity=if(distance*velocity>0f)velocity.coerceIn(-bound,bound)else 0f
        }
        elapsed=(elapsed+dt).coerceAtMost(duration)
        val t=elapsed/duration
        val t2=t*t
        val t3=t2*t
        val remaining=if(elapsed>=duration)0f else span*(1f-3f*t2+2f*t3)-duration*startVelocity*(t3-2f*t2+t)
        velocity=if(elapsed>=duration)0f else span*(6f*t-6f*t2)/duration+startVelocity*(3f*t2-4f*t+1f)
        val delta=distance-remaining
        previousDistance=distance
        previousDelta=delta
        return delta
    }
}
