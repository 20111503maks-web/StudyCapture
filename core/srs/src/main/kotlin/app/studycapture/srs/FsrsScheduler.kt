/*
 * FSRS-6 spaced-repetition scheduler.
 *
 * A faithful Kotlin port of `Scheduler` from py-fsrs 6.3.2
 * (https://github.com/open-spaced-repetition/py-fsrs, MIT License, Copyright (c)
 * open-spaced-repetition). FSRS (Free Spaced Repetition Scheduler) is by Jarrett Ye and
 * the open-spaced-repetition contributors. The only intentional difference is the fuzz
 * randomness source, which is an injectable [kotlin.random.Random].
 */
package app.studycapture.srs

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Schedules [SrsCard]s with FSRS-6. Instances are immutable and thread-safe as long as the
 * supplied [random] is.
 *
 * Note: with [enableFuzzing] each call to [review] / [preview] draws fresh fuzz, so the
 * card shown on a button by [preview] should be stored as-is when that button is pressed
 * (instead of calling [review] again) if the label must match the stored due date.
 *
 * @throws IllegalArgumentException if [parameters] has the wrong size or a value outside
 *   the py-fsrs bounds, if [desiredRetention] is not in (0, 1), or if
 *   [maximumIntervalDays] < 1.
 */
class FsrsScheduler(
    val parameters: DoubleArray = DEFAULT_PARAMETERS,
    val desiredRetention: Double = 0.9,
    val learningSteps: List<Duration> = listOf(Duration.ofMinutes(1), Duration.ofMinutes(10)),
    val relearningSteps: List<Duration> = listOf(Duration.ofMinutes(10)),
    val maximumIntervalDays: Int = 36500,
    val enableFuzzing: Boolean = true,
    private val random: Random = Random.Default,
) {
    private val model: FsrsModel

    init {
        validateParameters(parameters)
        require(desiredRetention > 0.0 && desiredRetention < 1.0) {
            "desiredRetention must be in (0, 1), got $desiredRetention"
        }
        require(maximumIntervalDays >= 1) { "maximumIntervalDays must be >= 1, got $maximumIntervalDays" }
        model = FsrsModel(parameters.copyOf(), desiredRetention, maximumIntervalDays)
    }

    /** A brand-new card, due immediately. */
    fun newCard(now: Instant): SrsCard = SrsCard(due = now)

    /** Probability of recall at [now] (0.0 for a card that was never reviewed). */
    fun retrievability(card: SrsCard, now: Instant): Double {
        val last = card.lastReview ?: return 0.0
        val stability = card.stability ?: return 0.0
        return model.retrievability(max(0L, elapsedDays(last, now)), stability)
    }

    /** Result for each rating without mutating anything — used to label the 4 buttons. */
    fun preview(card: SrsCard, now: Instant): Map<Rating, SrsCard> =
        Rating.entries.associateWith { review(card, it, now) }

    /** Reviews [card] with [rating] at [now] and returns the updated card. */
    fun review(card: SrsCard, rating: Rating, now: Instant): SrsCard {
        val daysSinceLastReview = card.lastReview?.let { elapsedDays(it, now) }
        val (stability, difficulty) = nextMemoryState(card, rating, now, daysSinceLastReview)

        var state = card.state
        var step = card.step
        val interval: Duration
        if (card.state == CardState.Review) {
            if (rating == Rating.Again && relearningSteps.isNotEmpty()) {
                state = CardState.Relearning
                step = 0
                interval = relearningSteps[0]
            } else {
                interval = Duration.ofDays(model.nextIntervalDays(stability))
            }
        } else {
            val steps = if (card.state == CardState.Learning) learningSteps else relearningSteps
            val current = card.step ?: 0
            val graduate = steps.isEmpty() ||
                (current >= steps.size && rating != Rating.Again) ||
                rating == Rating.Easy ||
                (rating == Rating.Good && current + 1 == steps.size)
            if (graduate) {
                state = CardState.Review
                step = null
                interval = Duration.ofDays(model.nextIntervalDays(stability))
            } else {
                step = when (rating) {
                    Rating.Again -> 0
                    Rating.Hard -> current
                    else -> current + 1
                }
                interval = if (rating == Rating.Hard && current == 0) {
                    if (steps.size == 1) scaleHalfEven(steps[0], 3, 2) else scaleHalfEven(steps[0] + steps[1], 1, 2)
                } else {
                    steps[step]
                }
            }
        }

        val finalInterval =
            if (enableFuzzing && state == CardState.Review) fuzzedInterval(interval) else interval
        return SrsCard(
            state = state,
            step = step,
            stability = stability,
            difficulty = difficulty,
            due = now + finalInterval,
            lastReview = now,
        )
    }

    private fun nextMemoryState(
        card: SrsCard,
        rating: Rating,
        now: Instant,
        daysSinceLastReview: Long?,
    ): Pair<Double, Double> {
        if (card.state == CardState.Learning && (card.stability == null || card.difficulty == null)) {
            return model.initialStability(rating) to model.initialDifficulty(rating, clamp = true)
        }
        val s = requireNotNull(card.stability) { "${card.state} card has no stability" }
        val d = requireNotNull(card.difficulty) { "${card.state} card has no difficulty" }
        val nextS = if (daysSinceLastReview != null && daysSinceLastReview < 1) {
            model.shortTermStability(s, rating)
        } else {
            model.nextStability(d, s, retrievability(card, now), rating)
        }
        return nextS to model.nextDifficulty(d, rating)
    }

    /** Inclusive bounds (in days) py-fsrs draws from for a [intervalDays]-day interval. */
    internal fun fuzzRange(intervalDays: Long): Pair<Long, Long> {
        var delta = 1.0
        for ((start, end, factor) in FUZZ_RANGES) {
            delta += factor * max(min(intervalDays.toDouble(), end) - start, 0.0)
        }
        var minIvl = pyRound(intervalDays - delta)
        var maxIvl = pyRound(intervalDays + delta)
        minIvl = max(2L, minIvl)
        maxIvl = min(maxIvl, maximumIntervalDays.toLong())
        minIvl = min(minIvl, maxIvl)
        return minIvl to maxIvl
    }

    internal fun fuzzedInterval(interval: Duration): Duration {
        val days = interval.toDays()
        if (days < 2.5) return interval
        val (minIvl, maxIvl) = fuzzRange(days)
        // Same expression as py-fsrs; note round() can yield maxIvl + 1 there as well.
        val fuzzed = random.nextDouble() * (maxIvl - minIvl + 1) + minIvl
        return Duration.ofDays(min(pyRound(fuzzed), maximumIntervalDays.toLong()))
    }

    companion object {
        /** FSRS-6 default weights w0..w20 (a fresh copy on every access). */
        val DEFAULT_PARAMETERS: DoubleArray get() = DEFAULT_WEIGHTS.copyOf()

        private val FUZZ_RANGES = listOf(
            Triple(2.5, 7.0, 0.15),
            Triple(7.0, 20.0, 0.1),
            Triple(20.0, Double.POSITIVE_INFINITY, 0.05),
        )

        /** Python `timedelta.days` of `to - from`: whole days, floored. */
        internal fun elapsedDays(from: Instant, to: Instant): Long =
            Math.floorDiv(Duration.between(from, to).seconds, 86_400L)

        /** `duration * num / den` at microsecond precision, rounded half-even like timedelta. */
        private fun scaleHalfEven(duration: Duration, num: Long, den: Long): Duration {
            val micros = Math.multiplyExact(duration.seconds, 1_000_000L) + duration.nano / 1_000
            val n = micros * num
            var q = Math.floorDiv(n, den)
            val r2 = 2 * Math.floorMod(n, den)
            if (r2 > den || (r2 == den && q % 2 != 0L)) q++
            return Duration.of(q, ChronoUnit.MICROS)
        }
    }
}
