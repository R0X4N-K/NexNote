package io.github.r0x4nk.nexnote.ui.screen.statistics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.domain.usecase.BuildNoteStatisticsUseCase
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StatisticsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentShowsCalendarSummaryAndSelectableDays() {
        val date = LocalDate.of(2026, 8, 21)
        val timestamp = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val statistics = BuildNoteStatisticsUseCase()(
            notes = listOf(
                Note(id = 1, content = "Write useful tests #quality", creationDate = timestamp)
            ),
            tags = listOf(
                Tag(
                    name = "quality",
                    noteCount = 1,
                    createdDate = timestamp,
                    lastUpdatedDate = timestamp
                )
            ),
            selectedYear = 2026,
            timezoneId = "UTC",
            nowMillis = timestamp
        )
        var selectedDate: LocalDate? = null

        composeRule.setContent {
            NexNoteTheme {
                StatisticsContent(
                    uiState = StatisticsUiState(
                        statistics = statistics,
                        selectedDay = statistics.activityOn(date),
                        isCalculating = true,
                        processedNotes = 1,
                        totalNotes = 2
                    ),
                    onPreviousYear = {},
                    onNextYear = {},
                    onSelectDay = { selectedDate = it }
                )
            }
        }

        composeRule.onNodeWithText("note dated 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Updating statistics").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 2 notes processed").assertIsDisplayed()
        composeRule.onNodeWithText("Writing activity").assertIsDisplayed()
        composeRule.onNodeWithTag("statistics-day-2026-08-21")
            .performClick()

        assertEquals(date, selectedDate)
    }
}
