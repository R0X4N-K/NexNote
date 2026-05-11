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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.ui.component.TagFilterBar
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
    val searchFocusRequester: FocusRequester
)

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
            AgendaTopBar(
                displayedYear = layoutState.uiState.displayedYear,
                displayedMonth = layoutState.uiState.displayedMonth,
                actions = actions
            )
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
        actions = actions
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgendaBody(
    params: AgendaBodyParams,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AgendaLazyColumn(params)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AgendaLazyColumn(params: AgendaBodyParams) {
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
            viewMode = params.uiState.viewMode,
            noteCardStyle = params.noteCardStyle,
            isSearchEmpty = params.uiState.isSearchActive && params.uiState.searchQuery.isNotBlank(),
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
    agendaNotesHeaderItem(params)
}

private fun LazyListScope.agendaCalendarItem(params: AgendaBodyParams) {
    item {
        AgendaCalendarSection(
            uiState = params.uiState,
            isCalendarVisible = params.isCalendarVisible,
            actions = params.actions
        )
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

private fun LazyListScope.agendaNotesHeaderItem(params: AgendaBodyParams) {
    item {
        NotesSectionHeader(
            year = params.uiState.selectedYear,
            month = params.uiState.selectedMonth,
            day = params.uiState.selectedDay
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
            searchFocusRequester = params.searchFocusRequester,
            actions = params.actions
        )
    }
}

private const val AGENDA_CONTROLS_STICKY_KEY = "agenda_controls"
private const val AGENDA_CONTROLS_CONTENT_TYPE = "agenda_controls"
