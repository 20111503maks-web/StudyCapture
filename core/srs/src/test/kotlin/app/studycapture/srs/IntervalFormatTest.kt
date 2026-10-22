package app.studycapture.srs

import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class IntervalFormatTest {
    private val t0 = Instant.parse("2025-03-01T09:00:00Z")

    private fun fmt(d: Duration) = formatInterval(t0, t0.plus(d))

    @Test
    fun formatsEachUnit() {
        assertEquals("<1m", fmt(Duration.ZERO))
        assertEquals("<1m", fmt(Duration.ofSeconds(59)))
        assertEquals("<1m", fmt(Duration.ofMinutes(-5)))
        assertEquals("1m", fmt(Duration.ofSeconds(60)))
        assertEquals("6m", fmt(Duration.ofSeconds(330)))
        assertEquals("10m", fmt(Duration.ofMinutes(10)))
        assertEquals("59m", fmt(Duration.ofMinutes(59)))
        assertEquals("1h", fmt(Duration.ofSeconds(59 * 60 + 40)))
        assertEquals("3h", fmt(Duration.ofHours(3)))
        assertEquals("23h", fmt(Duration.ofHours(23)))
        assertEquals("1d", fmt(Duration.ofHours(24)))
        assertEquals("4d", fmt(Duration.ofDays(4)))
        assertEquals("29d", fmt(Duration.ofDays(29)))
        assertEquals("1mo", fmt(Duration.ofDays(30)))
        assertEquals("2mo", fmt(Duration.ofDays(61)))
        assertEquals("11mo", fmt(Duration.ofDays(334)))
        assertEquals("1y", fmt(Duration.ofDays(365)))
        assertEquals("1.2y", fmt(Duration.ofDays(438)))
        assertEquals("3y", fmt(Duration.ofDays(1096)))
        assertEquals("100y", fmt(Duration.ofDays(36525)))
    }

    @Test
    fun isLocaleIndependent() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1.2y", fmt(Duration.ofDays(438)))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
