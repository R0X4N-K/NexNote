package io.github.r0x4nk.nexnote.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    /** Formats a timestamp as "dd/MM/yyyy" using the given timezone (device default if null). */
    fun formatDate(timestamp: Long, timezone: String? = null): String =
        format(timestamp, pattern = "dd/MM/yyyy", timezone = timezone)

    /** Formats a timestamp as "dd/MM/yyyy HH:mm". */
    fun formatDateTime(timestamp: Long, timezone: String? = null): String =
        format(timestamp, pattern = "dd/MM/yyyy HH:mm", timezone = timezone)

    private fun format(timestamp: Long, pattern: String, timezone: String?): String =
        SimpleDateFormat(pattern, Locale.getDefault())
            .apply {
                timeZone = timezone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
            }
            .format(Date(timestamp))

    /**
     * Formats a timestamp as a human-readable relative string.
     * Examples: "Now", "5m ago", "3h ago", "2d ago", "dd/MM/yyyy" (beyond 7 days).
     */
    fun formatRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000L           -> "Now"
            diff < 3_600_000L        -> "${diff / 60_000L}m ago"
            diff < 86_400_000L       -> "${diff / 3_600_000L}h ago"
            diff < 7 * 86_400_000L   -> "${diff / 86_400_000L}d ago"
            else                     -> formatDate(timestamp)
        }
    }

    /**
     * Returns the UTC timestamp of midnight (00:00:00.000) of the day containing
     * [timestamp], in the local system timezone.
     */
    fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /**
     * Returns the UTC timestamp of the first millisecond of the day after [timestamp].
     * Useful as an exclusive upper bound in date range queries.
     */
    fun startOfNextDay(timestamp: Long): Long =
        startOfDay(timestamp) + 86_400_000L

    /**
     * Returns the timestamp of the first millisecond of the specified month.
     * [month] is 0-based (Calendar.JANUARY = 0).
     */
    fun startOfMonth(year: Int, month: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Returns the timestamp of the first millisecond of the month after the given one. */
    fun startOfNextMonth(year: Int, month: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis

    /** Number of days in the given month (0-based). */
    fun daysInMonth(year: Int, month: Int): Int =
        Calendar.getInstance().apply { set(year, month, 1) }
            .getActualMaximum(Calendar.DAY_OF_MONTH)

    /**
     * Day-of-week offset (0 = Monday, 6 = Sunday) for the first day of the given month.
     * Normalized to ISO 8601 week start (Monday).
     */
    fun firstWeekdayOfMonth(year: Int, month: Int): Int {
        val dow = Calendar.getInstance().apply { set(year, month, 1) }
            .get(Calendar.DAY_OF_WEEK)
        return (dow - Calendar.MONDAY + 7) % 7
    }

    /** Formats a 0-based year and month as a localized "Month YYYY" string. */
    fun formatMonthYear(year: Int, month: Int): String {
        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(cal.time)
            .replaceFirstChar { it.uppercaseChar() }
    }

    /** Converts year, 0-based month, and day to a timestamp at local noon. */
    fun toMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
