package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.TrashSnackbarEffect
import io.github.r0x4nk.nexnote.ui.component.NoteActionsSheet
import io.github.r0x4nk.nexnote.ui.component.NoteCreationDatePickerDialog
import io.github.r0x4nk.nexnote.ui.component.OperationProgressDialog
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect
import io.github.r0x4nk.nexnote.ui.component.rememberNoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.component.rememberNoteShareCallbacks

/**
 * Tags screen route: connects [TagsViewModel] state to the Tags UI.
 *
 * Navigation: tapping a note in the expanded list calls [onNoteClick] to open
 * the Editor. The Tags screen remains self-contained and does not set global
 * cross-screen filter state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onNoteClick: (noteId: Long) -> Unit,
    onExportNote: ((Long) -> Unit)? = null,
    onMoveNoteToVault: ((Long) -> Unit)? = null,
    floatingBottomPadding: Dp = 0.dp,
    viewModel: TagsViewModel = viewModel(factory = TagsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val operationProgress by viewModel.operationProgress.collectAsStateWithLifecycle()
    OperationProgressDialog(operationProgress)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardCallbacks = rememberNoteClipboardCallbacks(snackbarHostState)
    val shareCallbacks = rememberNoteShareCallbacks(snackbarHostState)
    val searchFocusRequester = remember { FocusRequester() }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var activeActionsNote by remember { mutableStateOf<Note?>(null) }
    var dateEditNote by remember { mutableStateOf<Note?>(null) }
    val actions = rememberTagsActions(
        viewModel = viewModel,
        onNoteClick = onNoteClick,
        onRequestNoteActions = { note -> activeActionsNote = note },
        onSearchActiveChange = { isSearchActive = it },
        onSortMenuChange = { showSortMenu = it }
    )

    RadialMenuEffect(items = emptyList())
    TrashSnackbarEffect(
        trashEvents = viewModel.trashEvents,
        snackbarHostState = snackbarHostState,
        onUndoTrash = viewModel::undoPendingTrash,
        onConfirmTrash = viewModel::confirmTrash
    )
    TagsNoteActionMessagesEffect(viewModel, snackbarHostState)

    TagsScreenLayout(
        uiState = uiState,
        scrollBehavior = scrollBehavior,
        snackbarHostState = snackbarHostState,
        searchFocusRequester = searchFocusRequester,
        isSearchActive = isSearchActive,
        showSortMenu = showSortMenu,
        floatingBottomPadding = floatingBottomPadding,
        actions = actions
    )
    TagsDialogHost(
        activeDialog = uiState.activeDialog,
        actions = actions
    )
    NoteActionsSheet(
        note = activeActionsNote,
        onExport = onExportNote?.let { export -> { note -> export(note.id) } },
        clipboardCallbacks = clipboardCallbacks,
        shareCallbacks = shareCallbacks,
        onDuplicate = viewModel::duplicateNote,
        onDelete = viewModel::requestTrash,
        onMoveToVault = onMoveNoteToVault?.let { move -> { note -> move(note.id) } },
        onEditCreationDate = { note -> dateEditNote = note },
        onDismiss = { activeActionsNote = null }
    )

    NoteCreationDatePickerDialog(
        note = dateEditNote,
        onConfirm = { creationDate ->
            dateEditNote?.let { note -> viewModel.updateCreationDate(note, creationDate) }
        },
        onDismiss = { dateEditNote = null }
    )
}

@Composable
private fun TagsNoteActionMessagesEffect(
    viewModel: TagsViewModel,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.noteActionMessages.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }
}