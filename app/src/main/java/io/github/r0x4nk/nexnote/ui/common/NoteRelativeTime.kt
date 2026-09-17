package io.github.r0x4nk.nexnote.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.util.DateUtils

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 3_600_000L
private const val DAY_MS = 86_400_000L
private const val WEEK_MS = 7 * DAY_MS

/**
 * Localized relative timestamp for note metadata: "Now", "5 minutes ago",
 * "3 hours ago", "2 days ago", or the localized short date beyond a week.
 */
@Composable
internal fun noteRelativeTimeLabel(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < MINUTE_MS -> stringResource(R.string.relative_time_now)
        diff < HOUR_MS -> {
            val minutes = (diff / MINUTE_MS).toInt()
            pluralStringResource(R.plurals.relative_time_minutes, minutes, minutes)
        }
        diff < DAY_MS -> {
            val hours = (diff / HOUR_MS).toInt()
            pluralStringResource(R.plurals.relative_time_hours, hours, hours)
        }
        diff < WEEK_MS -> {
            val days = (diff / DAY_MS).toInt()
            pluralStringResource(R.plurals.relative_time_days, days, days)
        }
        else -> DateUtils.formatDate(timestamp)
    }
}
