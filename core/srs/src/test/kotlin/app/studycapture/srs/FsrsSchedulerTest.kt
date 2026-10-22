package app.studycapture.srs

import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class FsrsSchedulerTest {
    private val now = Instant.parse("2025-03-01T09:00:00Z")

    /** Random whose nextDouble() is fixed, to hit the fuzz range extremes deterministically. */
    private class FixedRandom(private val value: Double) : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = value
    }

    @Test
    fun defaultParametersMatchPyFsrsAndAreDefensivelyCopied() {
        val expected = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
            1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
        )
        assertContentEquals(expected, FsrsScheduler.DEFAULT_PARAMETERS)
        FsrsScheduler.DEFAULT_PARAMETERS[0] = 99.0
        assertEquals(0.212, FsrsScheduler.DEFAULT_PARAMETERS[0])
        assertNotSame(FsrsScheduler.DEFAULT_PARAMETERS, FsrsScheduler.DEFAULT_PARAMETERS)
    }

    @Test
    fun rejectsWrongParameterCount() {
        val e = assertFailsWith<IllegalArgumentException> { FsrsScheduler(parameters = DoubleArray(19) { 1.0 }) }
        assertEquals("Expected 21 parameters, got 19.", e.message)
        assertFailsWith<IllegalArgumentException> { FsrsScheduler(parameters = FsrsScheduler.DEFAULT_PARAMETERS + 0.1) }
    }

    @Test
    fun rejectsOutOfBoundsParameters() {
        for (i in 0 until 21) {
            val low = FsrsScheduler.DEFAULT_PARAMETERS.also { it[i] = LOWER_BOUNDS[i] - 1e-3 }
            val high = FsrsScheduler.DEFAULT_PARAMETERS.also { it[i] = UPPER_BOUNDS[i] + 1e-3 }
            val nan = FsrsScheduler.DEFAULT_PARAMETERS.also { it[i] = Double.NaN }
            for (p in listOf(low, high, nan)) {
                val e = assertFailsWith<IllegalArgumentException>("index $i value ${p[i]}") { FsrsScheduler(parameters = p) }
                assertTrue(e.message!!.contains("parameters[$i] = "), e.message)
            }
        }
        // Bounds themselves are inclusive, as in py-fsrs.
        FsrsScheduler(parameters = LOWER_BOUNDS.copyOf())
        FsrsScheduler(parameters = UPPER_BOUNDS.copyOf())
    }

    @Test
    fun rejectsInvalidRetentionAndMaximumInterval() {
        assertFailsWith<IllegalArgumentException> { FsrsScheduler(desiredRetention = 0.0) }
        assertFailsWith<IllegalArgumentException> { FsrsScheduler(desiredRetention = 1.0) }
        assertFailsWith<IllegalArgumentException> { FsrsScheduler(maximumIntervalDays = 0) }
    }

    @Test
    fun parametersArrayIsCopiedAtConstruction() {
        val p = FsrsScheduler.DEFAULT_PARAMETERS
        val s = FsrsScheduler(parameters = p, enableFuzzing = false)
        val before = s.review(s.newCard(now), Rating.Good, now)
        p[2] = 50.0
        assertEquals(before, s.review(s.newCard(now), Rating.Good, now))
    }

    @Test
    fun newCardIsLearningAndDueNow() {
        val card = FsrsScheduler().newCard(now)
        assertEquals(SrsCard(CardState.Learning, 0, null, null, now, null), card)
        assertEquals(0.0, FsrsScheduler().retrievability(card, now))
    }

    @Test
    fun previewDoesNotMutateAndMatchesReview() {
        val s = FsrsScheduler(enableFuzzing = false)
        var card = s.newCard(now)
        var t = now
        for (rating in listOf(Rating.Good, Rating.Good, Rating.Good, Rating.Again, Rating.Hard)) {
            val snapshot = card.copy()
            val preview = s.preview(card, t)
            assertEquals(Rating.entries, preview.keys.toList())
            assertEquals(snapshot, card)
            for ((r, result) in preview) assertEquals(s.review(card, r, t), result)
            card = preview.getValue(rating)
            t = card.due
        }
        // Intervals are ordered Again <= Hard <= Good <= Easy for a review card.
        val due = s.preview(card, t).values.map { it.due }
        assertEquals(due.sorted(), due)
    }

    @Test
    fun fuzzExtremesMatchReferenceRanges() {
        // (interval, maxInterval) -> (result for random()=0, result for random()->1), from py-fsrs 6.3.2.
        val table = listOf(
            Triple(36500, 1L, 1L to 1L), Triple(36500, 2L, 2L to 2L), Triple(36500, 3L, 2L to 5L),
            Triple(36500, 4L, 3L to 6L), Triple(36500, 5L, 4L to 7L), Triple(36500, 7L, 5L to 10L),
            Triple(36500, 10L, 8L to 13L), Triple(36500, 20L, 17L to 24L), Triple(36500, 21L, 18L to 25L),
            Triple(36500, 50L, 46L to 55L), Triple(36500, 99L, 92L to 107L), Triple(36500, 100L, 93L to 108L),
            Triple(36500, 365L, 345L to 386L), Triple(36500, 36500L, 34673L to 36500L),
            Triple(100, 50L, 46L to 55L), Triple(100, 99L, 92L to 100L), Triple(100, 100L, 93L to 100L),
        )
        for ((maxIvl, days, extremes) in table) {
            val lo = FsrsScheduler(maximumIntervalDays = maxIvl, random = FixedRandom(0.0))
            val hi = FsrsScheduler(maximumIntervalDays = maxIvl, random = FixedRandom(0.9999999999))
            val ivl = Duration.ofDays(days)
            assertEquals(extremes.first, lo.fuzzedInterval(ivl).toDays(), "low $days/$maxIvl")
            assertEquals(extremes.second, hi.fuzzedInterval(ivl).toDays(), "high $days/$maxIvl")
        }
    }

    @Test
    fun fuzzedReviewsStayWithinReferenceRange() {
        val plain = FsrsScheduler(enableFuzzing = false)
        val fuzzy = FsrsScheduler(random = Random(42))
        val seen = HashMap<Long, MutableSet<Long>>()
        for (stability in listOf(0.5, 3.0, 4.2, 9.0, 25.0, 80.0, 400.0)) {
            val card = SrsCard(CardState.Review, null, stability, 5.0, now, now.minus(Duration.ofDays(3)))
            val base = plain.review(card, Rating.Good, now)
            val baseDays = Duration.between(now, base.due).toDays()
            val (min, max) = fuzzy.fuzzRange(baseDays)
            repeat(400) {
                val r = fuzzy.review(card, Rating.Good, now)
                assertEquals(base.copy(due = r.due), r, "only due may change")
                val days = Duration.between(now, r.due)
                assertEquals(0L, days.toMillis() % 86_400_000L)
                val d = days.toDays()
                if (baseDays < 3) {
                    assertEquals(baseDays, d)
                } else {
                    // py-fsrs rounds random()*(max-min+1)+min, so max+1 is reachable there too.
                    assertTrue(d in min..max + 1, "$d not in [$min, ${max + 1}] for base $baseDays")
                }
                seen.getOrPut(baseDays) { mutableSetOf() } += d
            }
        }
        assertTrue(seen.any { it.value.size > 3 }, "fuzz never varied: $seen")
    }

    @Test
    fun fuzzIsNotAppliedToLearningSteps() {
        val s = FsrsScheduler(random = FixedRandom(0.9))
        val card = s.newCard(now)
        assertEquals(now.plus(Duration.ofMinutes(1)), s.review(card, Rating.Again, now).due)
        assertEquals(now.plus(Duration.ofSeconds(330)), s.review(card, Rating.Hard, now).due)
        assertEquals(now.plus(Duration.ofMinutes(10)), s.review(card, Rating.Good, now).due)
    }

    @Test
    fun elapsedDaysFloorsLikePythonTimedelta() {
        val t = Instant.parse("2025-01-01T00:00:00Z")
        assertEquals(0L, FsrsScheduler.elapsedDays(t, t.plusSeconds(86_399)))
        assertEquals(1L, FsrsScheduler.elapsedDays(t, t.plusSeconds(86_400)))
        assertEquals(-1L, FsrsScheduler.elapsedDays(t, t.minusSeconds(1)))
        assertEquals(-1L, FsrsScheduler.elapsedDays(t, t.minusNanos(1)))
    }
}
