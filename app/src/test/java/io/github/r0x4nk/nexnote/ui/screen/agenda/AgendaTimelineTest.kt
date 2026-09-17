package io.github.r0x4nk.nexnote.ui.screen.agenda

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AgendaTimelineTest {

    private val now = DateUtils.toMillis(2026, Calendar.SEPTEMBER, 17)

    @Test
    fun `bucket resolves the relative sections around now`() {
        assertEquals(
            AgendaSectionKind.TODAY,
            bucket(DateUtils.toMillis(2026, Calendar.SEPTEMBER, 17)).kind
        )
        assertEquals(
            AgendaSectionKind.YESTERDAY,
            bucket(DateUtils.toMillis(2026, Calendar.SEPTEMBER, 16)).kind
        )
        assertEquals(
            AgendaSectionKind.LAST_7_DAYS,
            bucket(DateUtils.toMillis(2026, Calendar.SEPTEMBER, 10)).kind
        )
        assertEquals(
            AgendaSectionKind.LAST_30_DAYS,
            bucket(DateUtils.toMillis(2026, Calendar.SEPTEMBER, 9)).kind
        )
        assertEquals(
            AgendaSectionKind.LAST_30_DAYS,
            bucket(DateUtils.toMillis(2026, Calendar.AUGUST, 18)).kind
        )
    }

    @Test
    fun `notes older than thirty days are grouped by month for the last year`() {
        val bucket = bucket(DateUtils.toMillis(2026, Calendar.AUGUST, 17))

        assertEquals(AgendaSectionKind.MONTH, bucket.kind)
        assertEquals(2026, bucket.year)
        assertEquals(Calendar.AUGUST, bucket.month)
    }

    @Test
    fun `notes older than a year are grouped by calendar year`() {
        val bucket = bucket(DateUtils.toMillis(2025, Calendar.AUGUST, 1))

        assertEquals(AgendaSectionKind.YEAR, bucket.kind)
        assertEquals(2025, bucket.year)
    }

    @Test
    fun `section title uses relative labels for the recent buckets`() {
        assertEquals(
            "Today",
            title(AgendaSectionKind.TODAY)
        )
        assertEquals("Yesterday", title(AgendaSectionKind.YESTERDAY))
        assertEquals("Previous 7 days", title(AgendaSectionKind.LAST_7_DAYS))
        assertEquals("Previous 30 days", title(AgendaSectionKind.LAST_30_DAYS))
    }

    @Test
    fun `section title formats months and years`() {
        val monthSection = section(
            kind = AgendaSectionKind.MONTH,
            year = 2026,
            month = Calendar.AUGUST,
            noteCount = 1
        )
        val yearSection = section(
            kind = AgendaSectionKind.YEAR,
            year = 2025,
            noteCount = 1
        )

        assertEquals(DateUtils.formatMonthYear(2026, Calendar.AUGUST), title(monthSection))
        assertEquals("2025", title(yearSection))
    }

    @Test
    fun `timeline flow groups scored items into ordered sections`() = runTest {
        val today = DateUtils.toMillis(2026, Calendar.SEPTEMBER, 17)
        val lastWeek = DateUtils.toMillis(2026, Calendar.SEPTEMBER, 12)
        val olderMonth = DateUtils.toMillis(2026, Calendar.JULY, 10)
        val processed = MutableStateFlow(
            AgendaProcessedNotes(
                notes = listOf(
                    Note(id = 1L, creationDate = olderMonth),
                    Note(id = 2L, creationDate = lastWeek),
                    Note(id = 3L, creationDate = today)
                ),
                scoredResults = listOf(
                    scoredItem(id = 3L, creationDate = today),
                    scoredItem(id = 2L, creationDate = lastWeek),
                    scoredItem(id = 1L, creationDate = olderMonth)
                )
            )
        )

        val sections = buildAgendaTimelineFlow(processed) { now }.first()

        assertEquals(
            listOf(AgendaSectionKind.TODAY, AgendaSectionKind.LAST_7_DAYS, AgendaSectionKind.MONTH),
            sections.map { section -> section.kind }
        )
        assertEquals(listOf(3L), sections[0].items.map { item -> item.note.id })
        assertEquals(listOf(2L), sections[1].items.map { item -> item.note.id })
        assertEquals(listOf(1L), sections[2].items.map { item -> item.note.id })
        assertEquals(3, sections.sumOf { section -> section.noteCount })
    }

    @Test
    fun `timeline sections are ordered newest first`() {
        val items = listOf(
            scoredItem(id = 1L, creationDate = DateUtils.toMillis(2025, Calendar.JANUARY, 5)),
            scoredItem(id = 2L, creationDate = DateUtils.toMillis(2024, Calendar.DECEMBER, 20)),
            scoredItem(id = 3L, creationDate = DateUtils.toMillis(2026, Calendar.JULY, 10))
        )

        val sections = agendaTimelineSections(items, now)

        assertEquals(
            listOf(2025, 2024),
            sections.filter { it.kind == AgendaSectionKind.YEAR }.map { it.year }
        )
        assertEquals(AgendaSectionKind.MONTH, sections.first().kind)
    }

    @Test
    fun `empty input produces no sections`() {
        assertEquals(emptyList<AgendaTimelineSection>(), agendaTimelineSections(emptyList(), now))
    }

    private fun bucket(creationDate: Long): AgendaTimelineBucket =
        agendaTimelineBucketFor(creationDate, now)

    private fun title(kind: AgendaSectionKind): String =
        title(section(kind = kind, noteCount = 1))

    private fun title(section: AgendaTimelineSection): String =
        agendaTimelineSectionTitle(
            section = section,
            todayLabel = "Today",
            yesterdayLabel = "Yesterday",
            last7DaysLabel = "Previous 7 days",
            last30DaysLabel = "Previous 30 days"
        )

    private fun scoredItem(id: Long, creationDate: Long): ScoredNote =
        ScoredNote(
            note = Note(id = id, creationDate = creationDate),
            score = 0,
            titleRanges = emptyList(),
            contentRanges = emptyList()
        )

    private fun section(
        kind: AgendaSectionKind,
        noteCount: Int,
        year: Int = 0,
        month: Int = 0
    ): AgendaTimelineSection =
        AgendaTimelineSection(
            key = "section_$kind",
            kind = kind,
            year = year,
            month = month,
            items = List(noteCount) { index ->
                ScoredNote(
                    note = Note(id = index.toLong(), creationDate = 0L),
                    score = 0,
                    titleRanges = emptyList(),
                    contentRanges = emptyList()
                )
            }
        )
}
