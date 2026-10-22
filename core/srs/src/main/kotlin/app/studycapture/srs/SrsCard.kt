/*
 * FSRS-6 scheduler for StudyCapture.
 *
 * A Kotlin port of py-fsrs 6.3.2 (https://github.com/open-spaced-repetition/py-fsrs),
 * Copyright (c) open-spaced-repetition, MIT License. The FSRS algorithm itself is by
 * Jarrett Ye and the open-spaced-repetition contributors.
 */
package app.studycapture.srs

import java.time.Instant

/** The four answer buttons. [value] matches py-fsrs `Rating`. */
enum class Rating(val value: Int) { Again(1), Hard(2), Good(3), Easy(4) }

/** Learning state of a card. [value] matches py-fsrs `State`. */
enum class CardState(val value: Int) { Learning(1), Review(2), Relearning(3) }

/**
 * Immutable FSRS card memory state (py-fsrs `Card` without the id).
 *
 * @property step current (re)learning step; `null` while in [CardState.Review].
 * @property stability memory stability in days; `null` until the first review.
 * @property difficulty difficulty in [1, 10]; `null` until the first review.
 */
data class SrsCard(
    val state: CardState = CardState.Learning,
    val step: Int? = 0,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Instant,
    val lastReview: Instant? = null,
)
