package app.studycapture.srs

import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

private const val SECONDS_PER_DAY = 86_400.0
private const val DAYS_PER_MONTH = 30.4375 // 365.25 / 12
private const val DAYS_PER_YEAR = 365.25

/**
 * Human-readable gap between [from] and [to], e.g. "<1m", "10m", "3h", "4d", "2mo", "1.2y".
 *
 * Each unit is rounded to the nearest whole value and promoted to the next unit once it
 * reaches it (59.6 minutes is "1h", not "60m"). Years carry one decimal ("1.2y", "3y").
 * Locale-independent. A [to] at or before [from] formats as "<1m".
 */
fun formatInterval(from: Instant, to: Instant): String {
    val d = Duration.between(from, to)
    val seconds = d.seconds + d.nano / 1e9
    if (seconds < 60) return "<1m"
    val minutes = (seconds / 60).roundToLong()
    if (minutes < 60) return "${minutes}m"
    val hours = (seconds / 3600).roundToLong()
    if (hours < 24) return "${hours}h"
    val days = seconds / SECONDS_PER_DAY
    val wholeDays = days.roundToLong()
    if (wholeDays < 30) return "${wholeDays}d"
    val months = (days / DAYS_PER_MONTH).roundToLong()
    if (months < 12) return "${months}mo"
    val tenthsOfYears = (days / DAYS_PER_YEAR * 10).roundToLong()
    val whole = tenthsOfYears / 10
    val tenth = tenthsOfYears % 10
    return if (tenth == 0L) "${whole}y" else "$whole.${tenth}y"
}
