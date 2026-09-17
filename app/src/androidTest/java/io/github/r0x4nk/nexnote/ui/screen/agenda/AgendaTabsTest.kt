package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.component.rememberNoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.theme.NexNoteTheme
import io.github.r0x4nk.nexnote.util.DateUtils
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class AgendaTabsTest {

    @get:Rule
    val compose = createComposeRule()

    private val timelineNoteA = Note(id = 2L, title = "Timeline note A")
    private val timelineNoteB = Note(id = 3L, title = "Timeline note B")

    private val sampleState = AgendaUiState(
        displayedYear = 2026,
        displayedMonth = Calendar.SEPTEMBER,
        selectedYear = 2026,
        selectedMonth = Calendar.SEPTEMBER,
        selectedDay = 15,
        notesForSelectedDate = listOf(Note(id = 1L, title = "Selected day note")),
        timelineGroups = listOf(
            AgendaTimelineSection(
                key = "month_2026_7",
                kind = AgendaSectionKind.MONTH,
                year = 2026,
                month = Calendar.AUGUST,
                items = listOf(timelineNoteA.toDisplayItem())
            ),
            AgendaTimelineSection(
                key = "month_2026_6",
                kind = AgendaSectionKind.MONTH,
                year = 2026,
                month = Calendar.JULY,
                items = listOf(timelineNoteB.toDisplayItem())
            )
        ),
        isLoading = false
    )

    @Test
    fun calendarTabIsShownFirstAndTimelineIsNotComposed() {
        setAgendaContent()

        compose.onNodeWithText(expectedMonthTitle()).assertExists()
        compose.onNodeWithText(timelineNoteA.title).assertDoesNotExist()
    }

    @Test
    fun tappingAgendaTabShowsTimelineGroups() {
        setAgendaContent()

        compose.onNode(hasClickAction() and hasText("Agenda"))
            .performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(timelineNoteA.title)
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(hasClickAction() and hasText("Agenda"))
            .assertIsSelected()
        compose.onNodeWithText(timelineNoteB.title).assertExists()
        compose.onNodeWithText(expectedMonthTitle()).assertDoesNotExist()
    }

    private fun expectedMonthTitle(): String =
        DateUtils.formatMonthYear(2026, Calendar.SEPTEMBER)

    @Test
    fun collapsingASectionHidesOnlyThatSectionsNotes() {
        setAgendaContent()

        compose.onNode(hasClickAction() and hasText("Agenda")).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(timelineNoteA.title)
                .fetchSemanticsNodes().isNotEmpty()
        }

        val collapseSection = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(R.string.agenda_timeline_collapse_section)
        compose.onAllNodesWithContentDescription(collapseSection).onFirst().performClick()
        compose.waitForIdle()

        compose.onNodeWithText(timelineNoteA.title).assertDoesNotExist()
        compose.onNodeWithText(timelineNoteB.title).assertExists()
    }

    @Test
    fun swipingLeftMovesFromCalendarToAgenda() {
        setAgendaContent()

        compose.onRoot().performTouchInput { swipeLeft() }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(timelineNoteA.title)
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(hasClickAction() and hasText("Agenda"))
            .assertIsSelected()
    }

    private fun setAgendaContent() {
        compose.setContent {
            NexNoteTheme {
                val pagerState = rememberPagerState(pageCount = { AgendaTab.entries.size })
                AgendaScreenLayout(
                    layoutState = AgendaLayoutState(
                        uiState = sampleState,
                        snackbarHostState = remember { SnackbarHostState() },
                        listState = rememberLazyListState(),
                        timelineListState = rememberLazyListState(),
                        pagerState = pagerState,
                        selectedTab = AgendaTab.fromPage(pagerState.currentPage),
                        activeNotes = sampleState.timelineGroups.flatMap { it.notes },
                        sectionExpansionState = rememberNoteTagFolderExpansionState(),
                        noteCardStyle = NoteCardStyle.TITLE_AND_PREVIEW,
                        isCalendarVisible = true,
                        isToolbarSticky = false,
                        floatingBottomPadding = 0.dp,
                        searchFocusRequester = remember { FocusRequester() },
                        selectionState = SelectionUiState(),
                        selectableNoteIds = emptyList()
                    ),
                    actions = noOpAgendaActions()
                )
            }
        }
    }
}

private fun Note.toDisplayItem(): ScoredNote =
    ScoredNote(
        note = this,
        score = 0,
        titleRanges = emptyList(),
        contentRanges = emptyList()
    )

private fun noOpAgendaActions(): AgendaActions = AgendaActions(
    onPreviousMonth = {},
    onNextMonth = {},
    onGoToToday = {},
    onSelectDate = { _, _, _ -> },
    onSearchQueryChange = {},
    onSearchToggle = {},
    onOpenSearchFilters = {},
    onSearchSortChange = {},
    onSearchScopeChange = {},
    onPinnedFilterChange = {},
    onToggleSort = {},
    onToggleView = {},
    onRemoveTagFilter = {},
    onToggleTagFilter = {},
    onClearTagFilters = {},
    onNewNote = {},
    onNoteClick = {},
    onTogglePin = {},
    onDuplicateNote = {},
    onRequestTrash = {},
    onRequestNoteActions = {},
    onStartNoteSelection = {},
    onExitNoteSelection = {},
    onSelectAllVisibleNotes = {},
    onDeselectAllNotes = {},
    onShareSelectedNotes = {},
    onCopySelectedNotesAsText = {},
    onCopySelectedNotesAsMarkdown = {},
    onDeleteSelectedNotes = {},
    onToggleNoteSelection = {},
    onUndoTrash = {},
    onConfirmTrash = {}
)
