package io.github.r0x4nk.nexnote.ui.screen.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import io.github.r0x4nk.nexnote.ui.theme.nexNoteBackground
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.DailyWritingActivity
import io.github.r0x4nk.nexnote.domain.model.NoteStatistics
import io.github.r0x4nk.nexnote.domain.model.TagUsageStatistic
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSectionLabel
import io.github.r0x4nk.nexnote.ui.component.ScrollToTopButton
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nexNoteBackground(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.statistics_title),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    NexIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        onClick = onBack
                    )
                },
                colors = nexTopAppBarColors()
            )
        }
    ) { innerPadding ->
        StatisticsContent(
            uiState = uiState,
            onPreviousYear = viewModel::showPreviousYear,
            onNextYear = viewModel::showNextYear,
            onSelectDay = viewModel::selectDay,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
internal fun StatisticsContent(
    uiState: StatisticsUiState,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onSelectDay: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val statistics = uiState.statistics
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (uiState.isCalculating || statistics == null) {
                item(key = "calculation", contentType = "calculation") {
                    StatisticsCalculationStatus(
                        processedNotes = uiState.processedNotes,
                        totalNotes = uiState.totalNotes,
                        hasVisibleStatistics = statistics != null,
                        isRetryingAfterError = uiState.isRetryingAfterError
                    )
                }
            }
            if (statistics == null) return@LazyColumn

            item(contentType = "hero") {
                StatisticsHero(
                    statistics = statistics,
                    onPreviousYear = onPreviousYear,
                    onNextYear = onNextYear
                )
            }
            item(contentType = "calendar") {
                ActivityCalendarCard(
                    statistics = statistics,
                    selectedDay = uiState.selectedDay,
                    onSelectDay = onSelectDay
                )
            }
            item(contentType = "overview") {
                StatisticsOverview(statistics)
            }
            item(contentType = "rhythm") {
                WritingRhythmCard(statistics)
            }
            item(contentType = "weekdays") {
                WeekdayActivityCard(statistics)
            }
            item(contentType = "tags") {
                TopTagsCard(
                    tags = statistics.topTags,
                    year = statistics.selectedYear
                )
            }
            item(contentType = "methodology") {
                StatisticsMethodologyNote()
            }
        }
        ScrollToTopButton(
            listState = listState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun StatisticsCalculationStatus(
    processedNotes: Int,
    totalNotes: Int,
    hasVisibleStatistics: Boolean,
    isRetryingAfterError: Boolean
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                Text(
                    text = if (isRetryingAfterError) {
                        stringResource(R.string.statistics_status_retrying)
                    } else if (hasVisibleStatistics) {
                        stringResource(R.string.statistics_status_updating)
                    } else {
                        stringResource(R.string.statistics_status_preparing)
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                    Text(
                        text = calculationStatusText(processedNotes, totalNotes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (totalNotes > 0) {
                LinearProgressIndicator(
                    progress = {
                        processedNotes.toFloat().div(totalNotes).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun calculationStatusText(processedNotes: Int, totalNotes: Int): String = when {
    totalNotes <= 0 -> stringResource(R.string.statistics_status_reading)
    processedNotes <= 0 -> pluralStringResource(
        R.plurals.statistics_status_found,
        totalNotes,
        totalNotes
    )
    else -> stringResource(R.string.statistics_status_processed, processedNotes, totalNotes)
}

@Composable
private fun StatisticsHero(
    statistics: NoteStatistics,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(24.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                YearSelector(
                    year = statistics.selectedYear,
                    canGoBack = statistics.selectedYear > statistics.earliestYear,
                    canGoForward = statistics.selectedYear < statistics.latestYear,
                    onPrevious = onPreviousYear,
                    onNext = onNextYear
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = formatCount(statistics.totalNotes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = pluralStringResource(
                    R.plurals.statistics_notes_dated,
                    statistics.totalNotes,
                    statistics.selectedYear
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroBadge(
                    icon = Icons.Default.LocalFireDepartment,
                    value = if (statistics.selectedYear == statistics.currentYear) {
                        "${statistics.currentStreakDays}"
                    } else {
                        "${statistics.longestStreakDays}"
                    },
                    label = if (statistics.selectedYear == statistics.currentYear) {
                        stringResource(R.string.statistics_current_streak)
                    } else {
                        stringResource(R.string.statistics_best_streak)
                    }
                )
                HeroBadge(
                    icon = Icons.Default.CalendarMonth,
                    value = "${statistics.activeDays}",
                    label = stringResource(R.string.statistics_active_days)
                )
            }
        }
    }
}

@Composable
private fun YearSelector(
    year: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NexIconButton(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.statistics_previous_year),
                enabled = canGoBack,
                onClick = onPrevious,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            NexIconButton(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.statistics_next_year),
                enabled = canGoForward,
                onClick = onNext,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun HeroBadge(icon: ImageVector, value: String, label: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(text = value, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityCalendarCard(
    statistics: NoteStatistics,
    selectedDay: DailyWritingActivity?,
    onSelectDay: (java.time.LocalDate) -> Unit
) {
    StatisticsCard {
        Text(
            text = stringResource(R.string.statistics_writing_activity),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.statistics_writing_activity_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        StatisticsHeatmap(
            statistics = statistics,
            selectedDay = selectedDay,
            onSelectDay = onSelectDay,
            modifier = Modifier.fillMaxWidth()
        )
        selectedDay?.let { day ->
            Spacer(Modifier.height(18.dp))
            SelectedDaySummary(day)
        }
    }
}

@Composable
private fun SelectedDaySummary(day: DailyWritingActivity) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = day.date.format(
                    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
                ),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayMetric(stringResource(R.string.statistics_metric_notes), day.noteCount, Modifier.weight(1f))
                DayMetric(stringResource(R.string.statistics_metric_words), day.wordCount, Modifier.weight(1f))
                DayMetric(stringResource(R.string.statistics_metric_characters), day.characterCount, Modifier.weight(1f))
                DayMetric(stringResource(R.string.statistics_metric_new_tags), day.tagsCreated, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayMetric(label: String, value: Number, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCount(value),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatisticsOverview(statistics: NoteStatistics) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NexSectionLabel(stringResource(R.string.statistics_overview))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OverviewMetric(
                icon = Icons.AutoMirrored.Filled.Notes,
                value = statistics.totalNotes,
                label = stringResource(R.string.statistics_metric_notes),
                modifier = Modifier.weight(1f)
            )
            OverviewMetric(
                icon = Icons.Default.EditNote,
                value = statistics.totalWords,
                label = stringResource(R.string.statistics_metric_words),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OverviewMetric(
                icon = Icons.Default.TextFields,
                value = statistics.totalCharacters,
                label = stringResource(R.string.statistics_metric_characters),
                modifier = Modifier.weight(1f)
            )
            OverviewMetric(
                icon = Icons.Default.Tag,
                value = statistics.totalTagsCreated,
                label = stringResource(R.string.statistics_metric_new_tags),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewMetric(
    icon: ImageVector,
    value: Number,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(text = formatCount(value), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WritingRhythmCard(statistics: NoteStatistics) {
    StatisticsCard {
        Text(
            text = stringResource(R.string.statistics_writing_rhythm),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RhythmMetric(
                value = statistics.longestStreakDays,
                label = stringResource(R.string.statistics_rhythm_best_streak),
                suffix = stringResource(R.string.statistics_unit_days),
                modifier = Modifier.weight(1f)
            )
            RhythmMetric(
                value = statistics.averageWordsPerNote,
                label = stringResource(R.string.statistics_rhythm_average_note),
                suffix = stringResource(R.string.statistics_unit_words),
                modifier = Modifier.weight(1f)
            )
            RhythmMetric(
                value = statistics.longestNoteWords,
                label = stringResource(R.string.statistics_rhythm_longest_note),
                suffix = stringResource(R.string.statistics_unit_words),
                modifier = Modifier.weight(1f)
            )
        }
        statistics.busiestDay?.let { busiest ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(
                    R.string.statistics_busiest_day,
                    busiest.date.format(
                        DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
                    )
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(
                    R.string.statistics_busiest_day_detail,
                    busiest.noteCount,
                    formatCount(busiest.wordCount)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RhythmMetric(
    value: Int,
    label: String,
    suffix: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = formatCount(value), style = MaterialTheme.typography.headlineSmall)
        Text(text = suffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeekdayActivityCard(statistics: NoteStatistics) {
    val maxNotes = statistics.notesByWeekday.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    StatisticsCard {
        Text(
            text = stringResource(R.string.statistics_weekly_pattern),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(14.dp))
        DayOfWeek.entries.forEach { weekday ->
            WeekdayBar(
                weekday = weekday,
                noteCount = statistics.notesByWeekday[weekday] ?: 0,
                maxNotes = maxNotes
            )
            if (weekday != DayOfWeek.SUNDAY) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun WeekdayBar(weekday: DayOfWeek, noteCount: Int, maxNotes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = weekday.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            modifier = Modifier.width(38.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { noteCount.toFloat() / maxNotes.toFloat() },
            modifier = Modifier.weight(1f).height(7.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = formatCount(noteCount),
            modifier = Modifier.width(30.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun TopTagsCard(tags: List<TagUsageStatistic>, year: Int) {
    val maxUsage = tags.maxOfOrNull { tag -> tag.noteCount }?.coerceAtLeast(1) ?: 1
    StatisticsCard {
        Text(
            text = stringResource(R.string.statistics_top_tags),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(R.string.statistics_tags_in_year, year),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        if (tags.isEmpty()) {
            Text(
                text = stringResource(R.string.statistics_no_tags_year),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            tags.forEachIndexed { index, tag ->
                TagUsageRow(tag = tag, maxUsage = maxUsage)
                if (index != tags.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TagUsageRow(tag: TagUsageStatistic, maxUsage: Int) {
    Column {
        Row {
            Text(text = "#${tag.name}", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            Text(
                text = formatCount(tag.noteCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { tag.noteCount.toFloat() / maxUsage.toFloat() },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun StatisticsMethodologyNote() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.statistics_methodology),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatisticsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun formatCount(value: Number): String = remember(value, Locale.getDefault()) {
    NumberFormat.getIntegerInstance().format(value)
}
