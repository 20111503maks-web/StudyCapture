/*
 * FSRS-6 memory model formulas, ported from py-fsrs 6.3.2 `fsrs/scheduler.py`
 * (MIT License, open-spaced-repetition). Expression order mirrors the reference so
 * floating point results match it to within a few ulps.
 */
package app.studycapture.srs

import kotlin.math.E
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal const val FSRS_DEFAULT_DECAY = 0.1542
internal const val STABILITY_MIN = 0.001
internal const val INITIAL_STABILITY_MAX = 100.0
internal const val MIN_DIFFICULTY = 1.0
internal const val MAX_DIFFICULTY = 10.0

internal val DEFAULT_WEIGHTS = doubleArrayOf(
    0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
    1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, FSRS_DEFAULT_DECAY,
)

internal val LOWER_BOUNDS = doubleArrayOf(
    STABILITY_MIN, STABILITY_MIN, STABILITY_MIN, STABILITY_MIN, 1.0, 0.001, 0.001, 0.001,
    0.0, 0.0, 0.001, 0.001, 0.001, 0.001, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.1,
)

internal val UPPER_BOUNDS = doubleArrayOf(
    INITIAL_STABILITY_MAX, INITIAL_STABILITY_MAX, INITIAL_STABILITY_MAX, INITIAL_STABILITY_MAX,
    10.0, 4.0, 4.0, 0.75, 4.5, 0.8, 3.5, 5.0, 0.25, 0.9, 4.0, 1.0, 6.0, 2.0, 2.0, 0.8, 0.8,
)

/** Throws [IllegalArgumentException] exactly when py-fsrs `_validate_parameters` raises. */
internal fun validateParameters(parameters: DoubleArray) {
    require(parameters.size == LOWER_BOUNDS.size) {
        "Expected ${LOWER_BOUNDS.size} parameters, got ${parameters.size}."
    }
    val errors = parameters.indices.filterNot { i ->
        LOWER_BOUNDS[i] <= parameters[i] && parameters[i] <= UPPER_BOUNDS[i]
    }.map { i -> "parameters[$i] = ${parameters[i]} is out of bounds: (${LOWER_BOUNDS[i]}, ${UPPER_BOUNDS[i]})" }
    require(errors.isEmpty()) { "One or more parameters are out of bounds:\n" + errors.joinToString("\n") }
}

/** Python's `round()` (round half to even) for doubles. */
internal fun pyRound(x: Double): Long = Math.rint(x).toLong()

/** Python `math.e ** x` (kept as pow, not exp, to mirror the reference). */
private fun ePow(x: Double): Double = E.pow(x)

/** Pure FSRS-6 formulas; [w] must already be validated. */
internal class FsrsModel(
    private val w: DoubleArray,
    private val desiredRetention: Double,
    private val maximumIntervalDays: Int,
) {
    val decay: Double = -w[20]
    val factor: Double = 0.9.pow(1 / decay) - 1

    fun retrievability(elapsedDays: Long, stability: Double): Double =
        (1 + factor * elapsedDays / stability).pow(decay)

    fun initialStability(rating: Rating): Double = clampStability(w[rating.value - 1])

    fun initialDifficulty(rating: Rating, clamp: Boolean): Double {
        val d = w[4] - ePow(w[5] * (rating.value - 1)) + 1
        return if (clamp) clampDifficulty(d) else d
    }

    /** Whole-day interval for [stability], clamped to [1, maximumIntervalDays]. */
    fun nextIntervalDays(stability: Double): Long {
        val raw = (stability / factor) * (desiredRetention.pow(1 / decay) - 1)
        return min(max(pyRound(raw), 1L), maximumIntervalDays.toLong())
    }

    fun shortTermStability(stability: Double, rating: Rating): Double {
        var increase = ePow(w[17] * (rating.value - 3 + w[18])) * stability.pow(-w[19])
        if (rating != Rating.Again) increase = max(increase, 1.0)
        return clampStability(stability * increase)
    }

    fun nextDifficulty(difficulty: Double, rating: Rating): Double {
        val arg1 = initialDifficulty(Rating.Easy, clamp = false)
        val delta = -(w[6] * (rating.value - 3))
        val arg2 = difficulty + (10.0 - difficulty) * delta / 9.0
        return clampDifficulty(w[7] * arg1 + (1 - w[7]) * arg2)
    }

    fun nextStability(difficulty: Double, stability: Double, retrievability: Double, rating: Rating): Double {
        val next = if (rating == Rating.Again) {
            nextForgetStability(difficulty, stability, retrievability)
        } else {
            nextRecallStability(difficulty, stability, retrievability, rating)
        }
        return clampStability(next)
    }

    private fun nextForgetStability(difficulty: Double, stability: Double, retrievability: Double): Double {
        val longTerm = w[11] * difficulty.pow(-w[12]) * ((stability + 1).pow(w[13]) - 1) *
            ePow((1 - retrievability) * w[14])
        val shortTerm = stability / ePow(w[17] * w[18])
        return min(longTerm, shortTerm)
    }

    private fun nextRecallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating,
    ): Double {
        val hardPenalty = if (rating == Rating.Hard) w[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) w[16] else 1.0
        return stability * (
            1 + ePow(w[8]) * (11 - difficulty) * stability.pow(-w[9]) *
                (ePow((1 - retrievability) * w[10]) - 1) * hardPenalty * easyBonus
            )
    }

    private fun clampDifficulty(d: Double): Double = min(max(d, MIN_DIFFICULTY), MAX_DIFFICULTY)

    private fun clampStability(s: Double): Double = max(s, STABILITY_MIN)
}
