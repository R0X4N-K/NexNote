package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildNoteStatisticsUseCaseTest {

    private val useCase = BuildNoteStatisticsUseCase()

    @Test
    fun `builds yearly totals calendar activity streaks and tag ranking`() {
        val notes = listOf(
            note(
                id = 1,
                date = LocalDate.of(2026, 8, 20),
                content = "First note #work"
            ),
            note(
                id = 2,
                date = LocalDate.of(2026, 8, 21),
                content = "Second #work #ideas note"
            ),
            note(
                id = 3,
                date = LocalDate.of(2025, 12, 31),
                content = "Previous year"
            )
        )
        val tags = listOf(
            tag("work", LocalDate.of(2026, 8, 20), noteCount = 2),
            tag("ideas", LocalDate.of(2026, 8, 21), noteCount = 1)
        )

        val result = useCase(
            notes = notes,
            tags = tags,
            selectedYear = 2026,
            timezoneId = "UTC",
            nowMillis = instant(LocalDate.of(2026, 8, 21))
        )

        assertEquals(365, result.days.size)
        assertEquals(2, result.totalNotes)
        assertEquals(7, result.totalWords)
        assertEquals(notes.take(2).sumOf { it.content.length }.toLong(), result.totalCharacters)
        assertEquals(2, result.totalTagsCreated)
        assertEquals(2, result.activeDays)
        assertEquals(4, result.averageWordsPerNote)
        assertEquals(4, result.longestNoteWords)
        assertEquals(2, result.currentStreakDays)
        assertEquals(2, result.longestStreakDays)
        assertEquals("work", result.topTags.first().name)
        assertEquals(2, result.topTags.first().noteCount)

        val firstDay = result.activityOn(LocalDate.of(2026, 8, 20))!!
        assertEquals(1, firstDay.noteCount)
        assertEquals(3, firstDay.wordCount)
        assertEquals(1, firstDay.tagsCreated)
        assertTrue(firstDay.activityLevel > 0)
    }

    @Test
    fun `uses configured timezone when assigning notes and tags to a year`() {
        val instantInUtc = java.time.Instant.parse("2026-01-01T00:30:00Z").toEpochMilli()
        val note = Note(id = 1, creationDate = instantInUtc, content = "Late note")
        val tag = Tag(
            name = "late",
            noteCount = 1,
            createdDate = instantInUtc,
            lastUpdatedDate = instantInUtc
        )

        val result = useCase(
            notes = listOf(note),
            tags = listOf(tag),
            selectedYear = 2025,
            timezoneId = "America/Los_Angeles",
            nowMillis = java.time.Instant.parse("2026-08-21T12:00:00Z").toEpochMilli()
        )

        assertEquals(2025, result.selectedYear)
        assertEquals(1, result.totalNotes)
        assertEquals(1, result.totalTagsCreated)
        assertEquals(1, result.activityOn(LocalDate.of(2025, 12, 31))?.noteCount)
    }

    @Test
    fun `counts each tag once per note regardless of case or duplicates`() {
        val notes = listOf(
            note(1, LocalDate.of(2026, 1, 1), "#Work #work"),
            note(2, LocalDate.of(2026, 1, 2), "Another #WORK item")
        )

        val result = useCase(
            notes = notes,
            tags = emptyList(),
            selectedYear = 2026,
            timezoneId = "UTC",
            nowMillis = instant(LocalDate.of(2026, 8, 21))
        )

        assertEquals(1, result.topTags.size)
        assertEquals("work", result.topTags.single().name)
        assertEquals(2, result.topTags.single().noteCount)
    }

    @Test
    fun `treats a newly created tag as activity even without a new note that day`() {
        val today = LocalDate.of(2026, 8, 21)
        val oldNote = note(1, LocalDate.of(2025, 12, 31), "Old note #fresh")
        val freshTag = tag("fresh", today, noteCount = 1)

        val result = useCase(
            notes = listOf(oldNote),
            tags = listOf(freshTag),
            selectedYear = 2026,
            timezoneId = "UTC",
            nowMillis = instant(today)
        )

        assertEquals(0, result.totalNotes)
        assertEquals(1, result.activeDays)
        assertEquals(1, result.currentStreakDays)
        assertEquals(1, result.longestStreakDays)
        assertEquals(1, result.activityOn(today)?.tagsCreated)
    }

    @Test
    fun `keeps future dated notes reachable through the year range`() {
        val futureNote = note(
            id = 1,
            date = LocalDate.of(2027, 2, 10),
            content = "Planned writing"
        )

        val result = useCase(
            notes = listOf(futureNote),
            tags = emptyList(),
            selectedYear = 2027,
            timezoneId = "UTC",
            nowMillis = instant(LocalDate.of(2026, 8, 21))
        )

        assertEquals(2027, result.latestYear)
        assertEquals(2027, result.selectedYear)
        assertEquals(1, result.totalNotes)
    }

    @Test
    fun `prepared history reuses aggregates across year changes`() {
        val notes = listOf(
            note(1, LocalDate.of(2024, 2, 10), "Older #archive note"),
            note(2, LocalDate.of(2026, 8, 21), "Current #work note")
        )
        val tags = listOf(tag("work", LocalDate.of(2026, 8, 21), noteCount = 1))
        val now = instant(LocalDate.of(2026, 8, 21))
        val history = useCase.prepare(
            notes = notes,
            tags = tags,
            timezoneId = "UTC",
            nowMillis = now
        )

        val older = history.statisticsFor(2024)
        val empty = history.statisticsFor(2025)
        val current = history.statisticsFor(2026)

        assertEquals(1, older.totalNotes)
        assertEquals("archive", older.topTags.single().name)
        assertEquals(0, empty.totalNotes)
        assertEquals(365, empty.days.size)
        assertEquals(1, current.totalNotes)
        assertEquals(1, current.totalTagsCreated)
        assertEquals(current, history.statisticsFor(2026))

        val cachedHistory = useCase.prepare(notes, tags, "UTC", now)
        val changedHistory = useCase.prepare(
            notes = notes.map { note ->
                if (note.id == 2L) {
                    note.copy(
                        content = "Changed #work content",
                        lastModifiedDate = note.lastModifiedDate + 1L
                    )
                } else {
                    note
                }
            },
            tags = tags,
            timezoneId = "UTC",
            nowMillis = now
        )

        assertSame(history, cachedHistory)
        assertNotSame(history, changedHistory)
    }

    @Test
    fun `incremental preparation emits partial snapshots then reuses completed history`() =
        runTest {
            val notes = (1L..80L).map { id ->
                note(
                    id = id,
                    date = LocalDate.of(2026, 1, 1).plusDays(id % 30),
                    content = "Representative markdown note $id #performance"
                )
            }
            val now = instant(LocalDate.of(2026, 8, 21))

            val progress = useCase.prepareIncrementally(
                notes = notes,
                tags = emptyList(),
                timezoneId = "UTC",
                nowMillis = now
            ).toList()

            assertEquals(0, progress.first().processedNotes)
            assertTrue(progress.size > 2)
            assertTrue(progress.zipWithNext().all { (first, second) ->
                first.processedNotes <= second.processedNotes
            })
            assertEquals(notes.size, progress.last().processedNotes)
            assertEquals(notes.size, progress.last().history.statisticsFor(2026).totalNotes)
            assertTrue(progress.last().isComplete)

            val cached = useCase.prepareIncrementally(
                notes = notes,
                tags = emptyList(),
                timezoneId = "UTC",
                nowMillis = now
            ).toList()

            assertEquals(1, cached.size)
            assertTrue(cached.single().isHistoryCacheHit)
        }

    private fun note(id: Long, date: LocalDate, content: String): Note = Note(
        id = id,
        creationDate = instant(date),
        content = content
    )

    private fun tag(name: String, date: LocalDate, noteCount: Int): Tag {
        val timestamp = instant(date)
        return Tag(
            name = name,
            noteCount = noteCount,
            createdDate = timestamp,
            lastUpdatedDate = timestamp
        )
    }

    private fun instant(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}
