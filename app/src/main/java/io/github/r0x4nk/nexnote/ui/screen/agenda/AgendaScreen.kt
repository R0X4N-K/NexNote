package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.common.selectedItems
import io.github.r0x4nk.nexnote.ui.common.TrashSnackbarEffect
import io.github.r0x4nk.nexnote.ui.component.NoteActionsSheet
import io.github.r0x4nk.nexnote.ui.component.rememberNoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuFabHideEffect
import io.github.r0x4nk.nexnote.util.DateUtils

@Composable
fun AgendaScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: (Long) -> Unit,
    floatingBottomPadding: Dp = 0.dp,
    viewModel: AgendaViewModel = viewModel(factory = AgendaViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val noteCardStyle by viewModel.noteCardStyle.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val clipboardCallbacks = rememberNoteClipboardCallbacks(snackbarHostState)
    var activeActionsNote by remember { mutableStateOf<Note?>(null) }
    var selectionState by rememberSaveable(stateSaver = SelectionUiState.Saver) {
        mutableStateOf(SelectionUiState())
    }
    val selectableNotes = uiState.notesForSelectedDate
    val selectableNoteIds = remember(selectableNotes) { selectableNotes.map { it.id } }
    val selectedNotes = remember(selectionState, selectableNotes) {
        selectionState.selectedItems(selectableNotes) { it.id }
    }
    val actions = rememberAgendaActions(
        viewModel = viewModel,
        onNoteClick = onNoteClick,
        onNewNote = onNewNote,
        onRequestNoteActions = { note ->
            if (!selectionState.isActive) {
                activeActionsNote = note
            }
        },
        onStartNoteSelection = {
            selectionState = selectionState.enter()
            activeActionsNote = null
        },
        onExitNoteSelection = {
            selectionState = selectionState.exit()
            activeActionsNote = null
        },
        onSelectAllVisibleNotes = {
            selectionState = selectionState.selectAll(selectableNoteIds)
        },
        onDeselectAllNotes = {
            selectionState = selectionState.deselectAll()
        },
        onDeleteSelectedNotes = {
            viewModel.requestTrash(selectedNotes)
            selectionState = selectionState.exit()
            activeActionsNote = null
        },
        onToggleNoteSelection = { note ->
            selectionState = selectionState.toggle(note.id)
            activeActionsNote = null
        }
    )

    val isToolbarSticky by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= CONTROLS_ROW_INDEX }
    }

    var isCalendarVisible by remember { mutableStateOf(!uiState.isSearchActive) }

    TrashSnackbarEffect(
        trashEvents = viewModel.trashEvents,
        snackbarHostState = snackbarHostState,
        onUndoTrash = actions.onUndoTrash,
        onConfirmTrash = actions.onConfirmTrash
    )
    AgendaNoteActionMessagesEffect(viewModel, snackbarHostState)
    AgendaSelectionCleanupEffect(
        selectionState = selectionState,
        selectableIds = selectableNoteIds,
        onSelectionChange = { selectionState = it }
    )
    RadialMenuFabHideEffect(selectionState.isActive)
    AgendaCalendarVisibilityEffects(
        isSearchActive = uiState.isSearchActive,
        isToolbarSticky = isToolbarSticky,
        listState = listState,
        searchFocusRequester = searchFocusRequester,
        onCalendarVisibilityChange = { isCalendarVisible = it }
    )
    BackHandler(enabled = uiState.isSearchActive) {
        actions.onSearchToggle(false)
    }
    BackHandler(enabled = selectionState.isActive) {
        actions.onExitNoteSelection()
    }
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    AgendaNewNoteFab(
        selectedDateMillis = DateUtils.toMillis(
            uiState.selectedYear,
            uiState.selectedMonth,
            uiState.selectedDay
        ),
        actions = actions
    )

    AgendaScreenLayout(
        layoutState = AgendaLayoutState(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            listState = listState,
            noteCardStyle = noteCardStyle,
            isCalendarVisible = isCalendarVisible,
            isToolbarSticky = isToolbarSticky,
            floatingBottomPadding = floatingBottomPadding,
            searchFocusRequester = searchFocusRequester,
            selectionState = selectionState,
            selectableNoteIds = selectableNoteIds
        ),
        actions = actions
    )

    NoteActionsSheet(
        note = activeActionsNote,
        clipboardCallbacks = clipboardCallbacks,
        onDuplicate = actions.onDuplicateNote,
        onDelete = actions.onRequestTrash,
        onSelect = actions.onToggleNoteSelection,
        onDismiss = { activeActionsNote = null }
    )
}

@Composable
private fun AgendaNoteActionMessagesEffect(
    viewModel: AgendaViewModel,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.noteActionMessages.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }
}

@Composable
private fun AgendaSelectionCleanupEffect(
    selectionState: SelectionUiState,
    selectableIds: List<Long>,
    onSelectionChange: (SelectionUiState) -> Unit
) {
    LaunchedEffect(selectionState, selectableIds) {
        val retained = selectionState.retainSelectableIds(selectableIds)
        if (retained != selectionState) {
            onSelectionChange(retained)
        }
    }
}
