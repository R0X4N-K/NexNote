package io.github.r0x4nk.nexnote.ui.screen.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.DailyWritingActivity
import io.github.r0x4nk.nexnote.domain.model.NoteStatistics
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val CELL_SIZE = 15.dp
private val CELL_GAP = 4.dp
private val WEEK_STEP = CELL_SIZE + CELL_GAP
private val MONTH_LABEL_HEIGHT = 22.dp
private val ACTIVITY_CELL_SHAPE = RoundedCornerShape(4.dp)
private const val CURRENT_WEEK_LEADING_CONTEXT = 3
private const val MONTH_LABEL_WEEK_SPAN = 3

@Composable
internal fun StatisticsHeatmap(
    statistics: NoteStatistics,
    selectedDay: DailyWritingActivity?,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val weeks = remember(statistics.selectedYear, statistics.days) {
        statistics.toHeatmapWeeks()
    }
    val locale = Locale.getDefault()
    val monthMarkers = remember(weeks, locale) {
        weeks.monthMarkers(locale).associateBy(MonthMarker::weekIndex)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(statistics.selectedYear, weeks) {
        val targetWeek = if (statistics.selectedYear == statistics.currentYear) {
            weeks.indexOfFirst { week ->
                week.days.any { day -> day?.date == statistics.today }
            }.coerceAtLeast(0)
        } else {
            0
        }
        listState.scrollToItem((targetWeek - CURRENT_WEEK_LEADING_CONTEXT).coerceAtLeast(0))
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            WeekdayLabels()
            LazyRow(
                modifier = Modifier.weight(1f),
                state = listState
            ) {
                itemsIndexed(
                    items = weeks,
                    key = { index, _ -> index },
                    contentType = { _, _ -> "heatmap-week" }
                ) { weekIndex, week ->
                    HeatmapWeekColumn(
                        week = week,
                        monthLabel = monthMarkers[weekIndex]?.label,
                        selectedDate = selectedDay?.date,
                        onSelectDay = onSelectDay
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        HeatmapLegend(modifier = Modifier.align(Alignment.End))
    }
}

@Composable
private fun HeatmapWeekColumn(
    week: HeatmapWeek,
    monthLabel: String?,
    selectedDate: LocalDate?,
    onSelectDay: (LocalDate) -> Unit
) {
    Column(modifier = Modifier.width(WEEK_STEP)) {
        Box(modifier = Modifier.height(MONTH_LABEL_HEIGHT)) {
            monthLabel?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier.requiredWidth(WEEK_STEP * MONTH_LABEL_WEEK_SPAN),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
            week.days.forEach { day ->
                if (day == null) {
                    Spacer(Modifier.size(CELL_SIZE))
                } else {
                    ActivityCell(
                        day = day,
                        selected = selectedDate == day.date,
                        onClick = { onSelectDay(day.date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayLabels() {
    val locale = Locale.getDefault()
    val labels = remember(locale) {
        listOf(
            DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, locale),
            "",
            DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.SHORT, locale),
            "",
            DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT, locale),
            "",
            ""
        )
    }
    Column(
        modifier = Modifier
            .padding(top = MONTH_LABEL_HEIGHT, end = 8.dp)
            .width(28.dp),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP)
    ) {
        labels.forEach { label ->
            Box(
                modifier = Modifier.height(CELL_SIZE),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActivityCell(
    day: DailyWritingActivity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = remember(day) { day.accessibilityLabel() }
    Box(
        modifier = Modifier
            .size(CELL_SIZE)
            .testTag("statistics-day-${day.date}")
            .clip(ACTIVITY_CELL_SHAPE)
            .background(activityColor(day.activityLevel))
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = ACTIVITY_CELL_SHAPE
                    )
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(onClick = onClick)
    )
}

@Composable
private fun HeatmapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        (0..4).forEach { level ->
            Box(
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(activityColor(level))
            )
        }
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun activityColor(level: Int): Color = when (level.coerceIn(0, 4)) {
    0 -> MaterialTheme.colorScheme.surfaceContainerHighest
    1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.44f)
    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
    else -> MaterialTheme.colorScheme.primary
}

private data class HeatmapWeek(val days: List<DailyWritingActivity?>)

private data class MonthMarker(val weekIndex: Int, val label: String)

private fun NoteStatistics.toHeatmapWeeks(): List<HeatmapWeek> {
    val firstDate = LocalDate.of(selectedYear, 1, 1)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastDate = LocalDate.of(selectedYear, 12, 31)
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val dayCount = ChronoUnit.DAYS.between(firstDate, lastDate).toInt() + 1
    return List(dayCount) { offset ->
        val date = firstDate.plusDays(offset.toLong())
        if (date.year == selectedYear) activityOn(date) else null
    }.chunked(7).map(::HeatmapWeek)
}

private fun List<HeatmapWeek>.monthMarkers(locale: Locale): List<MonthMarker> {
    val formatter = DateTimeFormatter.ofPattern("MMM", locale)
    return (1..12).map { month ->
        val weekIndex = indexOfFirst { week ->
            week.days.any { day -> day?.date?.monthValue == month }
        }
        val date = firstNotNullOf { week ->
            week.days.firstOrNull { day -> day?.date?.monthValue == month }
        }.date
        MonthMarker(weekIndex = weekIndex, label = date.format(formatter))
    }
}

private fun DailyWritingActivity.accessibilityLabel(): String {
    val dateLabel = date.format(
        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    )
    return "$dateLabel: ${noteCount.counted("note", "notes")}, " +
        "${wordCount.counted("word", "words")}, " +
        tagsCreated.counted("new tag", "new tags")
}

private fun Int.counted(singular: String, plural: String): String =
    "$this ${if (this == 1) singular else plural}"

private fun Long.counted(singular: String, plural: String): String =
    "$this ${if (this == 1L) singular else plural}"
