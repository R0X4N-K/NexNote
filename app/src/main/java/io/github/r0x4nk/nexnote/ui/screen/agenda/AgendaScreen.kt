package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AgendaScreen(
    onNoteClick: (Long) -> Unit,
    floatingBottomPadding: Dp = 0.dp,
    viewModel: AgendaViewModel = viewModel(factory = AgendaViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val noteCardStyle by viewModel.noteCardStyle.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    val actions = rememberAgendaActions(viewModel, onNoteClick)

    val isToolbarSticky by remember {
        derivedStateOf { listState.firstVisibleItemIndex > CONTROLS_ROW_INDEX }
    }

    var controlsRowHeightPx by remember { mutableStateOf(0) }
    val controlsRowHeightDp = remember(controlsRowHeightPx) {
        with(density) { controlsRowHeightPx.toDp() }
    }
    var isCalendarVisible by remember { mutableStateOf(!uiState.isSearchActive) }

    AgendaTrashEventsEffect(
        trashEvents = viewModel.trashEvents,
        snackbarHostState = snackbarHostState,
        onUndoTrash = actions.onUndoTrash,
        onConfirmTrash = actions.onConfirmTrash
    )
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
    AgendaRadialMenu(
        viewMode = uiState.viewMode,
        actions = actions
    )

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    AgendaScreenLayout(
        layoutState = AgendaLayoutState(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            listState = listState,
            noteCardStyle = noteCardStyle,
            isCalendarVisible = isCalendarVisible,
            isToolbarSticky = isToolbarSticky,
            controlsRowHeightDp = controlsRowHeightDp,
            floatingBottomPadding = floatingBottomPadding,
            searchFocusRequester = searchFocusRequester
        ),
        onControlsRowHeightChanged = { height ->
            if (height > 0) controlsRowHeightPx = height
        },
        actions = actions
    )
}
