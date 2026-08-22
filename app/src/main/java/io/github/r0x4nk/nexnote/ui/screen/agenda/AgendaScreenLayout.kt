package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.component.SelectionTopAppBar
import io.github.r0x4nk.nexnote.ui.component.ScrollToTopButton
import io.github.r0x4nk.nexnote.ui.component.TagFilterBar
import io.github.r0x4nk.nexnote.ui.component.buildNoteTagFolders
import io.github.r0x4nk.nexnote.ui.component.rememberNoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuSnackbarHost

internal data class AgendaLayoutState(
    val uiState: AgendaUiState,
    val snackbarHostState: SnackbarHostState,
    val listState: LazyListState,
    val noteCardStyle: NoteCardStyle,
    val isCalendarVisible: Boolean,
    val isToolbarSticky: Boolean,
    val floatingBottomPadding: Dp,
    val searchFocusRequester: FocusRequester,
    val selectionState: SelectionUiState,
    val selectableNoteIds: List<Long>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgendaScreenLayout(
    layoutState: AgendaLayoutState,
    actions: AgendaActions
) {
    Scaffold(
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
                    onShareSelected = actions.onShareSelectedNotes,
                    onCopySelectedAsText = actions.onCopySelectedNotesAsText,
                    onCopySelectedAsMarkdown = actions.onCopySelectedNotesAsMarkdown,
                    onDeleteSelected = actions.onDeleteSelectedNotes
                )
            } else {
                AgendaTopBar(actions = actions)
            }
        }
    ) { padding ->
        AgendaBody(
            params = layoutState.toBodyParams(actions),
            modifier = Modifier.padding(padding)
        )
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
private fun AgendaBody(
    params: AgendaBodyParams,
    modifier: Modifier = Modifier
) {
    val scrollToTopBottomPadding = if (params.selectionState.isActive) {
        params.floatingBottomPadding + 16.dp
    } else {
        RadialMenuOverlayDefaults.fabBottomClearance(params.floatingBottomPadding)
    }
    Box(
        modifier = modifier.fillMaxSize()
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
        modifier = Modifier.fillMaxSize(),
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
