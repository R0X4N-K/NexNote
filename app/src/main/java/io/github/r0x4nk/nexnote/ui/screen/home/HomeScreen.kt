package io.github.r0x4nk.nexnote.ui.screen.home

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.common.selectedItems
import io.github.r0x4nk.nexnote.ui.common.TrashSnackbarEffect
import io.github.r0x4nk.nexnote.ui.component.NoteActionsSheet
import io.github.r0x4nk.nexnote.ui.component.SelectionTopAppBar
import io.github.r0x4nk.nexnote.ui.component.rememberNoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuFabHideEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuItem
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuSnackbarHost

private const val EXIT_BACK_PRESS_WINDOW_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNoteClick: (noteId: Long) -> Unit,
    onNewNote: () -> Unit,
    onNewNoteFromTemplate: (templateId: Long) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenVault: () -> Unit,
    onMoveNoteToVault: (noteId: Long) -> Unit,
    floatingBottomPadding: Dp = 0.dp,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val noteCardStyle by viewModel.noteCardStyle.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val clipboardCallbacks = rememberNoteClipboardCallbacks(snackbarHostState)
    var activeActionsNote by remember { mutableStateOf<Note?>(null) }
    var selectionState by rememberSaveable(stateSaver = SelectionUiState.Saver) {
        mutableStateOf(SelectionUiState())
    }
    val selectableNotes = rememberHomeSelectableNotes(uiState)
    val selectableNoteIds = remember(selectableNotes) { selectableNotes.map { it.id } }
    val selectedNotes = remember(selectionState, selectableNotes) {
        selectionState.selectedItems(selectableNotes) { it.id }
    }

    TrashSnackbarEffect(
        trashEvents = viewModel.trashEvents,
        snackbarHostState = snackbarHostState,
        onUndoTrash = viewModel::undoPendingTrash,
        onConfirmTrash = viewModel::confirmTrash
    )
    NoteActionMessagesEffect(viewModel, snackbarHostState)
    DoubleBackToExitHandler()
    HomeSelectionCleanupEffect(
        selectionState = selectionState,
        selectableIds = selectableNoteIds,
        onSelectionChange = { selectionState = it }
    )
    RadialMenuFabHideEffect(selectionState.isActive)
    HomeRadialMenu(
        isAvailable = !selectionState.isActive,
        onNewNote = onNewNote,
        onTemplateClick = viewModel::showTemplatePicker
    ) {
        viewModel.onSearchToggle(true)
    }
    SearchFocusEffect(uiState.isSearchActive, searchFocusRequester)
    BackHandler(enabled = selectionState.isActive) {
        selectionState = selectionState.exit()
        activeActionsNote = null
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { HomeSnackbarHost(snackbarHostState, floatingBottomPadding) },
        topBar = {
            if (selectionState.isActive) {
                SelectionTopAppBar(
                    selectedCount = selectionState.selectedCount,
                    totalCount = selectableNoteIds.size,
                    scrollBehavior = scrollBehavior,
                    onClose = {
                        selectionState = selectionState.exit()
                        activeActionsNote = null
                    },
                    onSelectAll = {
                        selectionState = selectionState.selectAll(selectableNoteIds)
                    },
                    onDeselectAll = {
                        selectionState = selectionState.deselectAll()
                    },
                    onDeleteSelected = {
                        viewModel.requestTrash(selectedNotes)
                        selectionState = selectionState.exit()
                        activeActionsNote = null
                    }
                )
            } else {
                HomeTopAppBar(
                    uiState = uiState,
                    scrollBehavior = scrollBehavior,
                    searchFocusRequester = searchFocusRequester,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSearchToggle = viewModel::onSearchToggle,
                    onSortToggle = viewModel::toggleSortOrder,
                    onViewModeToggle = viewModel::toggleViewMode,
                    onOpenTrash = onOpenTrash,
                    onOpenVault = onOpenVault,
                    onStartSelection = {
                        selectionState = selectionState.enter()
                        activeActionsNote = null
                    }
                )
            }
        }
    ) { innerPadding ->
        HomeContent(
            uiState = uiState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            selectionState = selectionState,
            onNoteClick = onNoteClick,
            onToggleTagFilter = viewModel::toggleTagFilter,
            onRemoveTagFilter = viewModel::removeTagFilter,
            onClearTagFilters = viewModel::clearTagFilters,
            onTogglePin = viewModel::togglePin,
            onRequestTrash = viewModel::requestTrash,
            onRequestNoteActions = { note ->
                if (!selectionState.isActive) {
                    activeActionsNote = note
                }
            },
            onToggleNoteSelection = { note ->
                selectionState = selectionState.toggle(note.id)
                activeActionsNote = null
            },
            floatingBottomPadding = floatingBottomPadding,
            modifier = Modifier.padding(innerPadding)
        )
    }

    NoteActionsSheet(
        note = activeActionsNote,
        clipboardCallbacks = clipboardCallbacks,
        onDuplicate = viewModel::duplicateNote,
        onDelete = viewModel::requestTrash,
        onMoveToVault = { note -> onMoveNoteToVault(note.id) },
        onSelect = { note ->
            selectionState = selectionState.select(note.id)
        },
        onDismiss = { activeActionsNote = null }
    )

    if (uiState.showTemplatePicker) {
        TemplatePickerDialog(
            templates = uiState.templates,
            onSelect = { templateId ->
                viewModel.dismissTemplatePicker()
                onNewNoteFromTemplate(templateId)
            },
            onDismiss = viewModel::dismissTemplatePicker
        )
    }
}

@Composable
private fun NoteActionMessagesEffect(
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.noteActionMessages.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }
}

@Composable
private fun DoubleBackToExitHandler() {
    val context = LocalContext.current
    var lastBackPressMs by remember { mutableLongStateOf(0L) }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressMs < EXIT_BACK_PRESS_WINDOW_MS) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressMs = now
            Toast.makeText(context, "Press again to exit", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun HomeRadialMenu(
    isAvailable: Boolean,
    onNewNote: () -> Unit,
    onTemplateClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    RadialMenuEffect(items = remember(isAvailable, onNewNote, onTemplateClick, onSearchClick) {
        if (!isAvailable) {
            emptyList()
        } else {
            listOf(
                RadialMenuItem(Icons.Default.Add, "", action = onNewNote),
                RadialMenuItem(Icons.Default.Description, "", action = onTemplateClick),
                RadialMenuItem(Icons.Default.Search, "", action = onSearchClick)
            )
        }
    })
}

@Composable
private fun rememberHomeSelectableNotes(uiState: HomeUiState): List<Note> =
    remember(
        uiState.isSearchActive,
        uiState.searchQuery,
        uiState.scoredResults,
        uiState.notes
    ) {
        if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
            uiState.scoredResults.map { it.note }
        } else {
            uiState.notes
        }
    }

@Composable
private fun HomeSelectionCleanupEffect(
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

@Composable
private fun SearchFocusEffect(
    isSearchActive: Boolean,
    searchFocusRequester: FocusRequester
) {
    LaunchedEffect(isSearchActive, searchFocusRequester) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }
}

/**
 * Snackbar host for the Home screen.
 *
 * Delegates to [RadialMenuSnackbarHost] so that the snackbar sits at its
 * natural bottom position and the radial FAB animates up out of its way —
 * the Material 3 "lift the FAB" interaction. [floatingBottomPadding]
 * carries the outer Scaffold's bottom-bar height so the snackbar still
 * clears the global bottom navigation bar.
 */
@Composable
private fun HomeSnackbarHost(
    snackbarHostState: SnackbarHostState,
    floatingBottomPadding: Dp
) {
    RadialMenuSnackbarHost(
        hostState = snackbarHostState,
        bottomInset = floatingBottomPadding
    )
}
