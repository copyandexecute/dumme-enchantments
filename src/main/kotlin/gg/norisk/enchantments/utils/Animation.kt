package gg.norisk.enchantments.utils

import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction
import kotlinx.serialization.Serializable
import kotlin.math.*
import kotlin.random.Random
import kotlin.time.toJavaDuration

@Serializable
data class Animation(
    var start: Float,
    var end: Float,
    var dur: kotlin.time.Duration,
    val id: Long = Random.nextLong()
) {
    var startTime: Long
    var easing: Easing = Easing.LINEAR
    var forward: Boolean = true

    init {
        this.startTime = System.nanoTime()
    }

    constructor(start: Float, end: Float, duration: kotlin.time.Duration, easing: Easing) : this(start, end, duration) {
        this.easing = easing
    }

    fun reset() {
        this.startTime = System.nanoTime()
    }

    fun get(): Float {
        val currentTime = System.nanoTime()
        val delta = currentTime - startTime
        val nanoDuration = dur.toJavaDuration().toNanos()
        var animDelta = delta.toFloat() / nanoDuration
        animDelta = max(0.0, min(1.0, animDelta.toDouble())).toFloat()
        if (!forward) {
            animDelta = (1 - animDelta).toFloat()
        }
        animDelta = easing.apply(animDelta.toDouble())
        return start + (end - start) * animDelta
    }

    val isDone: Boolean
        get() = System.nanoTime() - startTime >= dur.toJavaDuration().toNanos()

    enum class Easing(val floatFunction: Double2DoubleFunction) {
        LINEAR(Double2DoubleFunction { x: Double -> x }),
        SINE_IN(Double2DoubleFunction { x: Double -> 1 - cos(x * Math.PI / 2) }),
        SINE_OUT(Double2DoubleFunction { x: Double -> sin(x * Math.PI / 2) }),
        SINE_IN_OUT(Double2DoubleFunction { x: Double -> -(cos(Math.PI * x) - 1) / 2 }),

        CUBIC_IN(Double2DoubleFunction { x: Double -> x.pow(3.0) }),
        CUBIC_OUT(Double2DoubleFunction { x: Double -> 1 - (1 - x).pow(3.0) }),
        CUBIC_IN_OUT(Double2DoubleFunction { x: Double -> if (x < 0.5) 4 * x * x * x else 1 - (-2 * x + 2).pow(3.0) / 2 }),

        QUINT_IN(Double2DoubleFunction { x: Double -> x.pow(5.0) }),
        QUINT_OUT(Double2DoubleFunction { x: Double -> 1 - (1 - x).pow(5.0) }),
        QUINT_IN_OUT(Double2DoubleFunction { x: Double ->
            if (x < 0.5) 16 * x * x * x * x * x else 1 - (-2 * x + 2).pow(
                5.0
            ) / 2
        }),

        CIRC_IN(Double2DoubleFunction { x: Double -> 1 - sqrt(1 - x.pow(2.0)) }),
        CIRC_OUT(Double2DoubleFunction { x: Double -> sqrt(1 - (x - 1).pow(2.0)) }),
        CIRC_IN_OUT(Double2DoubleFunction { x: Double ->
            if (x < 0.5) (1 - sqrt(1 - (2 * x).pow(2.0))) / 2 else (sqrt(
                1 - (-2 * x + 2).pow(2.0)
            ) + 1) / 2
        }),

        ELASTIC_IN(Double2DoubleFunction { x: Double ->
            val c4 = 2 * Math.PI / 3
            if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else (-2.0).pow(10 * x - 10) * sin((x * 10 - 10.75) * c4)
        }),
        ELASTIC_OUT(Double2DoubleFunction { x: Double ->
            val c4 = 2 * Math.PI / 3
            if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else 2.0.pow(-10 * x) * sin((x * 10 - 0.75) * c4) + 1
        }),
        ELASTIC_IN_OUT(Double2DoubleFunction { x: Double ->
            val c5 = 2 * Math.PI / 4.5
            val sin = sin((20 * x - 11.125) * c5)
            if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else if (x < 0.5) -(2.0.pow(20 * x - 10) * sin) / 2 else 2.0.pow(-20 * x + 10) * sin / 2 + 1
        }),

        QUAD_IN(Double2DoubleFunction { x: Double -> x * x }),
        QUAD_OUT(Double2DoubleFunction { x: Double -> 1 - (1 - x) * (1 - x) }),
        QUAD_IN_OUT(Double2DoubleFunction { x: Double -> if (x < 0.5) 2 * x * x else 1 - (-2 * x + 2).pow(2.0) / 2 }),

        QUART_IN(Double2DoubleFunction { x: Double -> x * x * x * x }),
        QUART_OUT(Double2DoubleFunction { x: Double -> 1 - (1 - x).pow(4.0) }),
        QUART_IN_OUT(Double2DoubleFunction { x: Double -> if (x < 0.5) 8 * x * x * x * x else 1 - (-2 * x + 2).pow(4.0) / 2 }),

        EXPO_IN(Double2DoubleFunction { x: Double -> if (x == 0.0) 0.0 else 2.0.pow(10 * x - 10) }),
        EXPO_OUT(Double2DoubleFunction { x: Double -> if (x == 1.0) 1.0 else 1 - 2.0.pow(-10 * x) }),
        EXPO_IN_OUT(Double2DoubleFunction { x: Double ->
            if (x == 0.0) 0.0 else if (x == 1.0) 1.0 else if (x < 0.5) 2.0.pow(
                20 * x - 10
            ) / 2 else (2 - 2.0.pow(-20 * x + 10)) / 2
        }),

        BACK_IN(Double2DoubleFunction { x: Double ->
            val c1 = 1.70158
            val c3 = c1 + 1
            c3 * x * x * x - c1 * x * x
        }),
        BACK_OUT(Double2DoubleFunction { x: Double ->
            val c1 = 1.70158
            val c3 = c1 + 1
            1 + c3 * (x - 1).pow(3.0) + c1 * (x - 1).pow(2.0)
        }),
        BACK_IN_OUT(Double2DoubleFunction { x: Double ->
            val c1 = 1.70158
            val c2 = c1 * 1.525
            if (x < 0.5) (2 * x).pow(2.0) * ((c2 + 1) * 2 * x - c2) / 2 else ((2 * x - 2).pow(2.0) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2
        }),

        BOUNCE_OUT(Double2DoubleFunction { x: Double ->
            var x = x
            val n1 = 7.5625
            val d1 = 2.75
            if (x < 1 / d1) {
                return@Double2DoubleFunction n1 * x * x
            } else if (x < 2 / d1) {
                return@Double2DoubleFunction n1 * ((1.5 / d1).let { x -= it; x }) * x + 0.75
            } else if (x < 2.5 / d1) {
                return@Double2DoubleFunction n1 * ((2.25 / d1).let { x -= it; x }) * x + 0.9375
            } else {
                return@Double2DoubleFunction n1 * ((2.625 / d1).let { x -= it; x }) * x + 0.984375
            }
        }),
        BOUNCE_IN(Double2DoubleFunction { x: Double -> (1 - BOUNCE_OUT.apply(x)).toDouble() }),
        BOUNCE_IN_OUT(Double2DoubleFunction { x: Double ->
            if (x < 0.5) ((1 - BOUNCE_OUT.apply(1 - 2 * x)) / 2).toDouble() else ((1 + BOUNCE_OUT.apply(
                2 * x - 1
            )) / 2).toDouble()
        });

        fun apply(f: Double): Float {
            return floatFunction[f].toFloat()
        }
    }
}
