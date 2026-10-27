package br.com.flaviotorres.iptvpremium.ui.components

/** Reused for an entire gesture: targets may change without resetting momentum. */
internal class CatalogScrollMotion {
    var velocity: Float = 0f
        private set

    fun advance(distance: Float, elapsedSeconds: Float, rowHeight: Float, speedFactor: Float = 1f): Float {
        val dt = elapsedSeconds.coerceIn(.001f, .05f)
        // Trial profile: half the physical scroll speed, retaining frame cadence and key intents.
        val speed = speedFactor.coerceIn(1f, 3.59375f) * .50f
        val omega = 18f * speed
        val error = -distance
        val decay = kotlin.math.exp(-omega * dt)
        val nextError = (error + (velocity + omega * error) * dt) * decay
        val nextVelocity = (velocity - omega * (velocity + omega * error) * dt) * decay
        val limit = rowHeight.coerceAtLeast(1f) * 6f * speed
        val requested = nextError - error
        val delta = requested.coerceIn(-limit * dt, limit * dt)
        velocity = if (delta != requested) delta / dt else nextVelocity.coerceIn(-limit, limit)
        return delta
    }
}
