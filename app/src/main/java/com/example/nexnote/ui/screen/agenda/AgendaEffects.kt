package com.example.nexnote.ui.screen.agenda

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.lazy.LazyListState
import com.example.nexnote.ui.common.TrashedNoteEvent
import com.example.nexnote.ui.common.NoteListViewMode
import com.example.nexnote.ui.common.snackbarMessage
import com.example.nexnote.ui.component.radial.RadialMenuEffect
import com.example.nexnote.ui.component.radial.RadialMenuItem
import kotlinx.coroutines.flow.Flow

@Composable
internal fun AgendaTrashEventsEffect(
    trashEvents: Flow<TrashedNoteEvent>,
    snackbarHostState: SnackbarHostState,
    onUndoTrash: (Long) -> Unit,
    onConfirmTrash: (Long) -> Unit
) {
    LaunchedEffect(trashEvents, snackbarHostState, onUndoTrash, onConfirmTrash) {
        trashEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.snackbarMessage(),
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndoTrash(event.noteId)
            } else {
                onConfirmTrash(event.noteId)
            }
        }
    }
}

@Composable
internal fun AgendaCalendarVisibilityEffects(
    isSearchActive: Boolean,
    isToolbarSticky: Boolean,
    listState: LazyListState,
    searchFocusRequester: FocusRequester,
    onCalendarVisibilityChange: (Boolean) -> Unit
) {
    LaunchedEffect(isSearchActive, listState, searchFocusRequester, onCalendarVisibilityChange) {
        if (isSearchActive) {
            onCalendarVisibilityChange(false)
            searchFocusRequester.requestFocus()
        } else if (listState.firstVisibleItemIndex <= CONTROLS_ROW_INDEX) {
            onCalendarVisibilityChange(true)
        }
    }

    LaunchedEffect(isToolbarSticky, isSearchActive, onCalendarVisibilityChange) {
        if (!isToolbarSticky && !isSearchActive) {
            onCalendarVisibilityChange(true)
        }
    }
}

@Composable
internal fun AgendaRadialMenu(
    viewMode: NoteListViewMode,
    actions: AgendaActions
) {
    RadialMenuEffect(items = remember(viewMode, actions) {
        listOf(
            RadialMenuItem(Icons.Default.CalendarToday, "") { actions.onGoToToday() },
            RadialMenuItem(Icons.Default.SwapVert, "") { actions.onToggleSort() },
            RadialMenuItem(
                icon = if (viewMode == NoteListViewMode.LIST) {
                    Icons.Default.GridView
                } else {
                    Icons.AutoMirrored.Filled.ViewList
                },
                label = ""
            ) { actions.onToggleView() },
            RadialMenuItem(Icons.Default.Search, "") { actions.onSearchToggle(true) }
        )
    })
}
