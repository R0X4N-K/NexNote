package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.ui.component.TagFilterBar
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults

internal data class AgendaLayoutState(
    val uiState: AgendaUiState,
    val snackbarHostState: SnackbarHostState,
    val listState: LazyListState,
    val noteCardStyle: NoteCardStyle,
    val isCalendarVisible: Boolean,
    val isToolbarSticky: Boolean,
    val controlsRowHeightDp: Dp,
    val floatingBottomPadding: Dp,
    val searchFocusRequester: FocusRequester
)

@Composable
internal fun AgendaScreenLayout(
    layoutState: AgendaLayoutState,
    onControlsRowHeightChanged: (Int) -> Unit,
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
            params = layoutState.toBodyParams(onControlsRowHeightChanged, actions),
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun AgendaSnackbarHost(
    snackbarHostState: SnackbarHostState,
    floatingBottomPadding: Dp
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(
            bottom = RadialMenuOverlayDefaults.snackbarBottomPadding(floatingBottomPadding)
        )
    ) { data ->
        Snackbar(snackbarData = data)
    }
}

private data class AgendaBodyParams(
    val uiState: AgendaUiState,
    val listState: LazyListState,
    val noteCardStyle: NoteCardStyle,
    val isCalendarVisible: Boolean,
    val isToolbarSticky: Boolean,
    val controlsRowHeightDp: Dp,
    val floatingBottomPadding: Dp,
    val searchFocusRequester: FocusRequester,
    val onControlsRowHeightChanged: (Int) -> Unit,
    val actions: AgendaActions
)

private fun AgendaLayoutState.toBodyParams(
    onControlsRowHeightChanged: (Int) -> Unit,
    actions: AgendaActions
): AgendaBodyParams {
    return AgendaBodyParams(
        uiState = uiState,
        listState = listState,
        noteCardStyle = noteCardStyle,
        isCalendarVisible = isCalendarVisible,
        isToolbarSticky = isToolbarSticky,
        controlsRowHeightDp = controlsRowHeightDp,
        floatingBottomPadding = floatingBottomPadding,
        searchFocusRequester = searchFocusRequester,
        onControlsRowHeightChanged = onControlsRowHeightChanged,
        actions = actions
    )
}

@Composable
private fun AgendaBody(
    params: AgendaBodyParams,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AgendaLazyColumn(params)
        AgendaStickyControlsRow(params)
    }
}

@Composable
private fun AgendaLazyColumn(params: AgendaBodyParams) {
    LazyColumn(
        state = params.listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = if (params.isToolbarSticky) params.controlsRowHeightDp else 0.dp,
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
        item {
            Spacer(
                Modifier.height(
                    RadialMenuOverlayDefaults.snackbarBottomPadding(params.floatingBottomPadding)
                )
            )
        }
    }
}

private fun LazyListScope.agendaHeaderItems(params: AgendaBodyParams) {
    agendaCalendarItem(params)
    item { AgendaInlineControlsRow(params) }
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
private fun AgendaInlineControlsRow(params: AgendaBodyParams) {
    val inactiveFocusRequester = remember { FocusRequester() }
    AgendaControlsRow(
        sortOrder = params.uiState.sortOrder,
        viewMode = params.uiState.viewMode,
        isSearchActive = params.uiState.isSearchActive,
        searchQuery = params.uiState.searchQuery,
        searchFocusRequester = if (params.isToolbarSticky) {
            inactiveFocusRequester
        } else {
            params.searchFocusRequester
        },
        modifier = Modifier.onSizeChanged { size ->
            params.onControlsRowHeightChanged(size.height)
        },
        actions = params.actions
    )
}

@Composable
private fun BoxScope.AgendaStickyControlsRow(params: AgendaBodyParams) {
    AnimatedVisibility(
        visible = params.isToolbarSticky,
        enter = agendaStickyEnter(),
        exit = agendaStickyExit(),
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 4.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
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
}

private fun agendaStickyEnter(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { fullHeight -> -fullHeight },
        animationSpec = tween(durationMillis = 150)
    ) + fadeIn(animationSpec = tween(durationMillis = 150))
}

private fun agendaStickyExit(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { fullHeight -> -fullHeight },
        animationSpec = tween(durationMillis = 120)
    ) + fadeOut(animationSpec = tween(durationMillis = 120))
}
