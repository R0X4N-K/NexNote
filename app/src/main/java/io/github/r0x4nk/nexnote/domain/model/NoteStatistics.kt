package io.github.r0x4nk.nexnote.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/** Writing activity attributed to one calendar day in the configured timezone. */
data class DailyWritingActivity(
    val date: LocalDate,
    val noteCount: Int = 0,
    val wordCount: Long = 0,
    val characterCount: Long = 0,
    val tagsCreated: Int = 0,
    val activityLevel: Int = 0
) {
    val hasActivity: Boolean
        get() = noteCount > 0 || tagsCreated > 0
}

/** Tag usage within notes created in the selected calendar year. */
data class TagUsageStatistic(
    val name: String,
    val noteCount: Int
)

/** Immutable statistics snapshot for one calendar year. */
data class NoteStatistics(
    val selectedYear: Int,
    val currentYear: Int,
    val earliestYear: Int,
    val latestYear: Int,
    val today: LocalDate,
    val days: List<DailyWritingActivity>,
    val totalNotes: Int,
    val totalWords: Long,
    val totalCharacters: Long,
    val totalTagsCreated: Int,
    val activeDays: Int,
    val averageWordsPerNote: Int,
    val longestNoteWords: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val busiestDay: DailyWritingActivity?,
    val notesByWeekday: Map<DayOfWeek, Int>,
    val topTags: List<TagUsageStatistic>
) {
    fun activityOn(date: LocalDate): DailyWritingActivity? =
        if (date.year == selectedYear) days.getOrNull(date.dayOfYear - 1) else null
}
