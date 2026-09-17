package io.github.r0x4nk.nexnote.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
import io.github.r0x4nk.nexnote.ui.theme.nexNoteBackground
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import io.github.r0x4nk.nexnote.ui.common.NoteCollectionSortEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.common.TrashSnackbarEffect
import io.github.r0x4nk.nexnote.ui.common.selectedItems
import io.github.r0x4nk.nexnote.ui.component.NoteActionsSheet
import io.github.r0x4nk.nexnote.ui.component.NoteCreationDatePickerDialog
import io.github.r0x4nk.nexnote.ui.component.OperationProgressDialog
import io.github.r0x4nk.nexnote.ui.component.SelectionTopAppBar
import io.github.r0x4nk.nexnote.ui.component.rememberNoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuFabHideEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuItem
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuSnackbarHost
import io.github.r0x4nk.nexnote.ui.component.rememberNoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.component.rememberNoteShareCallbacks

private const val EXIT_BACK_PRESS_WINDOW_MS = 2000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNoteClick: (noteId: Long) -> Unit,
    onNewNote: () -> Unit,
    onNewNoteFromTemplate: (templateId: Long) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenVault: () -> Unit,
    onMoveNoteToVault: (noteId: Long) -> Unit,
    onExportNote: ((Long) -> Unit)? = null,
    floatingBottomPadding: Dp = 0.dp,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val operationProgress by viewModel.operationProgress.collectAsStateWithLifecycle()
    OperationProgressDialog(operationProgress)
    val allSelectionCandidateIds by viewModel.selectionCandidateIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val noteCardStyle by viewModel.noteCardStyle.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val tagFolderExpansion = rememberNoteTagFolderExpansionState()
    NoteCollectionSortEffect(uiState.appliedSortOrder to uiState.appliedSearchSort, listState, gridState)
    val clipboardCallbacks = rememberNoteClipboardCallbacks(snackbarHostState)
    val shareCallbacks = rememberNoteShareCallbacks(snackbarHostState)
    var activeActionsNote by remember { mutableStateOf<Note?>(null) }
    var dateEditNote by remember { mutableStateOf<Note?>(null) }
    var showSearchFilters by rememberSaveable { mutableStateOf(false) }
    var selectionState by rememberSaveable(stateSaver = SelectionUiState.Saver) {
        mutableStateOf(SelectionUiState())
    }
    val selectableNotes = rememberHomeSelectableNotes(uiState)
    val selectableNoteIds = allSelectionCandidateIds.orEmpty()
    val selectedNotes = remember(selectionState, selectableNotes) {
        selectionState.selectedItems(selectableNotes) { it.id }
    }
    fun performSelectedAction(action: (Collection<Note>) -> Unit) {
        val selectedIds = selectionState.selectedIds.toSet()
        viewModel.withSelectedNotes(selectedIds) { notes ->
            action(notes)
            if (selectionState.selectedIds == selectedIds) {
                selectionState = selectionState.exit()
                activeActionsNote = null
            }
        }
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
        selectableIds = allSelectionCandidateIds,
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
        containerColor = Color.Transparent,
        modifier = Modifier.nexNoteBackground().nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    onNoteActions = selectedNotes.singleOrNull()?.let { note ->
                        { activeActionsNote = note }
                    },
                    onShareSelected = {
                        performSelectedAction(shareCallbacks.onShareNotes)
                    },
                    onCopySelectedAsText = {
                        performSelectedAction(clipboardCallbacks.onCopyPlainTextNotes)
                    },
                    onCopySelectedAsMarkdown = {
                        performSelectedAction(clipboardCallbacks.onCopyMarkdownNotes)
                    },
                    onDeleteSelected = {
                        viewModel.requestTrashByIds(selectionState.selectedIds)
                        selectionState = selectionState.exit()
                        activeActionsNote = null
                    }
                )
            } else {
                HomeTopAppBar(
                    uiState = uiState,
                    scrollBehavior = scrollBehavior,
                    tagFolderExpansion = tagFolderExpansion,
                    searchFocusRequester = searchFocusRequester,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSearchToggle = { active ->
                        if (!active) showSearchFilters = false
                        viewModel.onSearchToggle(active)
                    },
                    onOpenSearchFilters = { showSearchFilters = true },
                    onSearchSortChange = viewModel::setSearchSort,
                    onSortToggle = viewModel::toggleSortOrder,
                    onViewModeToggle = viewModel::toggleViewMode,
                    onOpenTrash = onOpenTrash,
                    onOpenStatistics = onOpenStatistics,
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
            tagFolderExpansion = tagFolderExpansion,
            vaultPullEnabled = !selectionState.isActive && !uiState.isSearchActive,
            onNoteClick = onNoteClick,
            onOpenVault = onOpenVault,
            onToggleTagFilter = viewModel::toggleTagFilter,
            onRemoveTagFilter = viewModel::removeTagFilter,
            onClearTagFilters = viewModel::clearTagFilters,
            onLoadMoreNotes = viewModel::loadMoreNotes,
            onTogglePin = viewModel::togglePin,
            onRequestTrash = viewModel::requestTrash,
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
        onExport = onExportNote?.let { export -> { note -> export(note.id) } },
        clipboardCallbacks = clipboardCallbacks,
        shareCallbacks = shareCallbacks,
        onDuplicate = viewModel::duplicateNote,
        onDelete = viewModel::requestTrash,
        onMoveToVault = { note -> onMoveNoteToVault(note.id) },
        onEditCreationDate = { note -> dateEditNote = note },
        onDismiss = {
            activeActionsNote = null
            selectionState = selectionState.exit()
        }
    )

    NoteCreationDatePickerDialog(
        note = dateEditNote,
        onConfirm = { creationDate ->
            dateEditNote?.let { note -> viewModel.updateCreationDate(note, creationDate) }
        },
        onDismiss = { dateEditNote = null }
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

    if (showSearchFilters && uiState.isSearchActive) {
        HomeSearchFiltersSheet(
            uiState = uiState,
            onSearchScopeChange = viewModel::setSearchScope,
            onPinnedFilterChange = viewModel::setPinnedFilter,
            onToggleTagFilter = viewModel::toggleTagFilter,
            onClearTagFilters = viewModel::clearTagFilters,
            onDismiss = { showSearchFilters = false }
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
    val pressAgainMessage = stringResource(R.string.home_press_again_to_exit)
    var lastBackPressMs by remember { mutableLongStateOf(0L) }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressMs < EXIT_BACK_PRESS_WINDOW_MS) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressMs = now
            Toast.makeText(context, pressAgainMessage, Toast.LENGTH_SHORT).show()
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
    selectableIds: Set<Long>?,
    onSelectionChange: (SelectionUiState) -> Unit
) {
    LaunchedEffect(selectionState, selectableIds) {
        if (selectableIds == null) return@LaunchedEffect
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