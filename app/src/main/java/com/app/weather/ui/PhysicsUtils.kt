package com.app.weather.ui

class SpringFloat(initial: Float) {
    var value:    Float = initial
    var velocity: Float = 0f
    var target:   Float = initial

    fun step(dt: Float, stiffness: Float = 280f, damping: Float = 22f, animationEnabled: Boolean = true) {
        if (!animationEnabled) {
            value = target
            velocity = 0f
            return
        }
        val force = (target - value) * stiffness
        velocity  = (velocity + force * dt) * (1f - damping * dt).coerceAtLeast(0f)
        value    += velocity * dt
    }

    fun impulse(v: Float) { velocity += v }
}
