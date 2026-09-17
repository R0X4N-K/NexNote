package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.component.NoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.ScrollToTopButton
import io.github.r0x4nk.nexnote.ui.component.SelectionTopAppBar
import io.github.r0x4nk.nexnote.ui.component.TagFilterBar
import io.github.r0x4nk.nexnote.ui.component.buildNoteTagFolders
import io.github.r0x4nk.nexnote.ui.component.rememberNoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuSnackbarHost
import io.github.r0x4nk.nexnote.ui.theme.nexNoteBackground
import kotlinx.coroutines.launch

internal data class AgendaLayoutState(
    val uiState: AgendaUiState,
    val snackbarHostState: SnackbarHostState,
    val listState: LazyListState,
    val timelineListState: LazyListState,
    val pagerState: PagerState,
    val selectedTab: AgendaTab,
    val activeNotes: List<Note>,
    val sectionExpansionState: NoteTagFolderExpansionState,
    val noteCardStyle: NoteCardStyle,
    val isCalendarVisible: Boolean,
    val isToolbarSticky: Boolean,
    val floatingBottomPadding: Dp,
    val searchFocusRequester: FocusRequester,
    val selectionState: SelectionUiState,
    val selectableNoteIds: List<Long>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun AgendaScreenLayout(
    layoutState: AgendaLayoutState,
    actions: AgendaActions
) {
    val scope = rememberCoroutineScope()
    val pagerState = layoutState.pagerState
    val onTabSelected: (AgendaTab) -> Unit = remember(scope, pagerState) {
        { tab -> scope.launch { pagerState.animateScrollToPage(tab.pageIndex) } }
    }
    val onTimelineToday: () -> Unit = {
        // Sections are emitted most-recent first, so the top of the list is
        // always the closest bucket to today.
        scope.launch { layoutState.timelineListState.animateScrollToItem(0) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nexNoteBackground(),
        snackbarHost = {
            AgendaSnackbarHost(
                snackbarHostState = layoutState.snackbarHostState,
                floatingBottomPadding = layoutState.floatingBottomPadding
            )
        },
        topBar = {
            if (layoutState.selectionState.isActive) {
                SelectionTopAppBar(
                    selectedCount = layoutState.selectionState.selectedCount,
                    totalCount = layoutState.selectableNoteIds.size,
                    onClose = actions.onExitNoteSelection,
                    onSelectAll = actions.onSelectAllVisibleNotes,
                    onDeselectAll = actions.onDeselectAllNotes,
                    onNoteActions = layoutState.activeNotes
                        .singleOrNull { layoutState.selectionState.isSelected(it.id) }
                        ?.let { note -> { actions.onRequestNoteActions(note) } },
                    onShareSelected = actions.onShareSelectedNotes,
                    onCopySelectedAsText = actions.onCopySelectedNotesAsText,
                    onCopySelectedAsMarkdown = actions.onCopySelectedNotesAsMarkdown,
                    onDeleteSelected = actions.onDeleteSelectedNotes
                )
            } else {
                Column {
                    val isAgendaTab = layoutState.selectedTab == AgendaTab.AGENDA
                    AgendaTopBar(
                        title = stringResource(layoutState.selectedTab.labelRes),
                        onToday = if (!isAgendaTab) {
                            actions.onGoToToday
                        } else {
                            onTimelineToday
                        }
                    )
                    AgendaTabs(
                        selectedTab = layoutState.selectedTab,
                        onTabSelected = onTabSelected
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            userScrollEnabled = !layoutState.selectionState.isActive
        ) { page ->
            when (AgendaTab.fromPage(page)) {
                AgendaTab.CALENDAR -> AgendaCalendarPage(layoutState, actions)
                AgendaTab.AGENDA -> AgendaTimelinePage(
                    sections = layoutState.uiState.timelineGroups,
                    listState = layoutState.timelineListState,
                    expansionState = layoutState.sectionExpansionState,
                    isSearchActive = layoutState.uiState.isSearchActive,
                    searchQuery = layoutState.uiState.searchQuery,
                    searchSort = layoutState.uiState.searchSort,
                    hasActiveSearchFilters = layoutState.uiState.hasActiveSearchFilters,
                    noteCount = layoutState.uiState.timelineNoteCount,
                    searchFocusRequester = layoutState.searchFocusRequester,
                    noteCardStyle = layoutState.noteCardStyle,
                    selectionState = layoutState.selectionState,
                    floatingBottomPadding = layoutState.floatingBottomPadding,
                    actions = actions
                )
            }
        }
    }
}

/**
 * Snackbar host for the Agenda screen.
 *
 * See [RadialMenuSnackbarHost] for the Material 3 "lift the FAB" behaviour:
 * the snackbar appears at its natural bottom position (above the outer
 * Scaffold's bottom navigation, expressed by [floatingBottomPadding]), and
 * publishes its measured height to the [io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuController]
 * so the radial FAB animates upward while it is visible.
 */
@Composable
private fun AgendaSnackbarHost(
    snackbarHostState: SnackbarHostState,
    floatingBottomPadding: Dp
) {
    RadialMenuSnackbarHost(
        hostState = snackbarHostState,
        bottomInset = floatingBottomPadding
    )
}

private data class AgendaBodyParams(
    val uiState: AgendaUiState,
    val listState: LazyListState,
    val noteCardStyle: NoteCardStyle,
    val isCalendarVisible: Boolean,
    val isToolbarSticky: Boolean,
    val floatingBottomPadding: Dp,
    val searchFocusRequester: FocusRequester,
    val selectionState: SelectionUiState,
    val actions: AgendaActions
)

private fun AgendaLayoutState.toBodyParams(
    actions: AgendaActions
): AgendaBodyParams {
    return AgendaBodyParams(
        uiState = uiState,
        listState = listState,
        noteCardStyle = noteCardStyle,
        isCalendarVisible = isCalendarVisible,
        isToolbarSticky = isToolbarSticky,
        floatingBottomPadding = floatingBottomPadding,
        searchFocusRequester = searchFocusRequester,
        selectionState = selectionState,
        actions = actions
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgendaCalendarPage(
    layoutState: AgendaLayoutState,
    actions: AgendaActions
) {
    val params = layoutState.toBodyParams(actions)
    val scrollToTopBottomPadding = if (params.selectionState.isActive) {
        params.floatingBottomPadding + 16.dp
    } else {
        RadialMenuOverlayDefaults.fabBottomClearance(params.floatingBottomPadding)
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AgendaLazyColumn(params)
        ScrollToTopButton(
            listState = params.listState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = scrollToTopBottomPadding)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgendaLazyColumn(params: AgendaBodyParams) {
    val tagDisplayItems = remember(
        params.uiState.notesForSelectedDate,
        params.uiState.scoredResults
    ) {
        params.uiState.scoredResults.ifEmpty {
            params.uiState.notesForSelectedDate.map { note ->
                ScoredNote(note, score = 0, titleRanges = emptyList(), contentRanges = emptyList())
            }
        }
    }
    val tagFolders = remember(tagDisplayItems) { buildNoteTagFolders(tagDisplayItems) }
    val tagFolderExpansionState = rememberNoteTagFolderExpansionState(tagFolders)

    LazyColumn(
        state = params.listState,
        modifier = Modifier.fillMaxSize().clipToBounds(),
        contentPadding = PaddingValues(
            bottom = 16.dp
        )
    ) {
        agendaHeaderItems(params)
        agendaNotesItems(
            notes = params.uiState.notesForSelectedDate,
            displayItems = tagDisplayItems,
            viewMode = params.uiState.viewMode,
            noteCardStyle = params.noteCardStyle,
            selectionState = params.selectionState,
            isSearchEmpty = params.uiState.isSearchActive && params.uiState.searchQuery.isNotBlank(),
            tagFolders = tagFolders,
            tagFolderExpansionState = tagFolderExpansionState,
            actions = params.actions
        )
        // Reserve enough room at the end of the list so the FAB never covers
        // the last item — including the outer bottom-bar inset.
        item {
            Spacer(
                Modifier.height(
                    RadialMenuOverlayDefaults.fabBottomClearance(params.floatingBottomPadding)
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.agendaHeaderItems(params: AgendaBodyParams) {
    agendaCalendarItem(params)
    stickyHeader(
        key = AGENDA_CONTROLS_STICKY_KEY,
        contentType = AGENDA_CONTROLS_CONTENT_TYPE
    ) {
        AgendaStickyControlsRow(params)
    }
    agendaTagFilterItem(params)
}

private fun LazyListScope.agendaCalendarItem(params: AgendaBodyParams) {
    item {
        AgendaCalendarSection(
            uiState = params.uiState,
            isCalendarVisible = params.isCalendarVisible,
            actions = params.actions
        )
        Spacer(Modifier.height(8.dp))
    }
}

private fun LazyListScope.agendaTagFilterItem(params: AgendaBodyParams) {
    item {
        TagFilterBar(
            selectedTags = params.uiState.selectedTagFilters,
            onTagRemove = params.actions.onRemoveTagFilter,
            onClearAll = params.actions.onClearTagFilters
        )
    }
}

@Composable
private fun AgendaStickyControlsRow(params: AgendaBodyParams) {
    // While a note selection is active the contextual top bar owns every
    // action, so the in-body search/sort/view controls must not compete with
    // it. The empty sticky slot keeps the LazyColumn item indices stable.
    if (params.selectionState.isActive) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (params.isToolbarSticky) 2.dp else 0.dp,
        shadowElevation = if (params.isToolbarSticky) 4.dp else 0.dp
    ) {
        AgendaControlsRow(
            sortOrder = params.uiState.sortOrder,
            viewMode = params.uiState.viewMode,
            isSearchActive = params.uiState.isSearchActive,
            searchQuery = params.uiState.searchQuery,
            searchSort = params.uiState.searchSort,
            hasActiveSearchFilters = params.uiState.hasActiveSearchFilters,
            searchFocusRequester = params.searchFocusRequester,
            selectedYear = params.uiState.selectedYear,
            selectedMonth = params.uiState.selectedMonth,
            selectedDay = params.uiState.selectedDay,
            noteCount = params.uiState.notesForSelectedDate.size,
            actions = params.actions
        )
    }
}

private const val AGENDA_CONTROLS_STICKY_KEY = "agenda_controls"
private const val AGENDA_CONTROLS_CONTENT_TYPE = "agenda_controls"
