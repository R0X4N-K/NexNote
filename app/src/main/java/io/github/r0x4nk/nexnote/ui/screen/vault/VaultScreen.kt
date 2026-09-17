package io.github.r0x4nk.nexnote.ui.screen.vault

import io.github.r0x4nk.nexnote.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import io.github.r0x4nk.nexnote.ui.theme.nexNoteBackground
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import io.github.r0x4nk.nexnote.ui.common.NoteCollectionSortEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.common.NoteCollectionLayoutDefaults
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.common.animateNoteItem
import io.github.r0x4nk.nexnote.ui.common.selectedItems
import io.github.r0x4nk.nexnote.ui.component.AutoScrollingTagRow
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.NexSheetHeader
import io.github.r0x4nk.nexnote.ui.component.NoteActionsSheetRow
import io.github.r0x4nk.nexnote.ui.component.NoteCreationDatePickerDialog
import io.github.r0x4nk.nexnote.ui.component.NoteCard
import io.github.r0x4nk.nexnote.ui.component.NoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.component.NoteListOverflowMenu
import io.github.r0x4nk.nexnote.ui.component.NoteListSortButton
import io.github.r0x4nk.nexnote.ui.component.NoteSearchFiltersSheet
import io.github.r0x4nk.nexnote.ui.component.NoteSearchSortMenu
import io.github.r0x4nk.nexnote.ui.component.NoteShareCallbacks
import io.github.r0x4nk.nexnote.ui.component.NoteTagFolderCollection
import io.github.r0x4nk.nexnote.ui.component.NoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.OperationProgressDialog
import io.github.r0x4nk.nexnote.ui.component.rememberNoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.ScrollToTopButton
import io.github.r0x4nk.nexnote.ui.component.SelectionTopAppBar
import io.github.r0x4nk.nexnote.ui.component.TagFilterBar
import io.github.r0x4nk.nexnote.ui.component.TagFolderExpandAllButton
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuFabHideEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuItem
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuSnackbarHost
import io.github.r0x4nk.nexnote.ui.component.rememberNoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.component.rememberNoteShareCallbacks
import io.github.r0x4nk.nexnote.ui.screen.home.TemplatePickerDialog
import io.github.r0x4nk.nexnote.ui.screen.trash.TrashNoteCard

internal const val VAULT_NOTE_ACTION_COPY_TAG = "vault_note_action_copy"
internal const val VAULT_NOTE_ACTION_COPY_TEXT_TAG = "vault_note_action_copy_text"
internal const val VAULT_NOTE_ACTION_COPY_MARKDOWN_TAG = "vault_note_action_copy_markdown"
internal const val VAULT_NOTE_ACTION_MOVE_TO_TRASH_TAG = "vault_note_action_move_to_trash"
internal const val VAULT_NOTE_ACTION_DUPLICATE_TAG = "vault_note_action_duplicate"
internal const val VAULT_NOTE_ACTION_EDIT_DATE_TAG = "vault_note_action_edit_date"
internal const val VAULT_NOTE_ACTION_REMOVE_FROM_VAULT_TAG =
    "vault_note_action_remove_from_vault"
internal const val VAULT_NOTE_ROW_TAG = "vault_note_row"
internal const val VAULT_NOTES_LOADING_TAG = "vault_notes_loading"
internal const val VAULT_TOP_TAGS_TAG = "vault_top_tags"

private enum class VaultNoteActionsPage { Actions, Copy }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    pendingMoveNoteId: Long = 0L,
    onBack: () -> Unit,
    onCreateVaultNote: () -> Unit,
    onCreateVaultNoteFromTemplate: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    floatingBottomPadding: Dp = 0.dp,
    accessViewModel: VaultAccessViewModel = viewModel(factory = VaultAccessViewModel.Factory),
    notesViewModel: VaultNotesViewModel = viewModel(factory = VaultNotesViewModel.Factory)
) {
    val accessState by accessViewModel.uiState.collectAsStateWithLifecycle()
    val notesState by notesViewModel.uiState.collectAsStateWithLifecycle()
    val operationProgress by notesViewModel.operationProgress.collectAsStateWithLifecycle()
    OperationProgressDialog(operationProgress)
    val noteCardStyle by notesViewModel.noteCardStyle.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val tagFolderExpansion = rememberNoteTagFolderExpansionState()
    NoteCollectionSortEffect(notesState.sortOrder to notesState.searchSort, listState, gridState)
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val clipboardCallbacks = rememberNoteClipboardCallbacks(snackbarHostState)
    val shareCallbacks = rememberNoteShareCallbacks(snackbarHostState)
    var activeActionsNote by remember { mutableStateOf<Note?>(null) }
    var dateEditNote by remember { mutableStateOf<Note?>(null) }
    var showSearchFilters by rememberSaveable { mutableStateOf(false) }
    var selectionState by rememberSaveable(stateSaver = SelectionUiState.Saver) {
        mutableStateOf(SelectionUiState())
    }
    var moveNoteIdToConsume by rememberSaveable(pendingMoveNoteId) {
        mutableLongStateOf(pendingMoveNoteId)
    }

    val isVaultNotesSurfaceUnlocked = accessState.isUnlocked && notesState.isUnlocked
    val selectableVaultNotes = remember(
        isVaultNotesSurfaceUnlocked,
        notesState.isTrashVisible,
        notesState.notes
    ) {
        if (isVaultNotesSurfaceUnlocked && !notesState.isTrashVisible) {
            notesState.notes
        } else {
            emptyList()
        }
    }
    val selectableVaultNoteIds = remember(selectableVaultNotes) {
        selectableVaultNotes.map { it.id }
    }
    val selectedVaultNotes = remember(selectionState, selectableVaultNotes) {
        selectionState.selectedItems(selectableVaultNotes) { it.id }
    }
    val isVaultRadialMenuAvailable =
        isVaultNotesSurfaceUnlocked && !notesState.isTrashVisible && !selectionState.isActive

    VaultSearchFocusEffect(notesState.isSearchActive, searchFocusRequester)
    VaultRadialMenu(
        isAvailable = isVaultRadialMenuAvailable,
        onCreateVaultNote = onCreateVaultNote,
        onCreateVaultNoteFromTemplate = notesViewModel::showTemplatePicker,
        onSearchClick = { notesViewModel.onSearchToggle(true) }
    )
    VaultActionMessagesEffect(notesViewModel, snackbarHostState)
    VaultTrashSnackbarEffect(
        trashEvents = notesViewModel.vaultTrashEvents,
        snackbarHostState = snackbarHostState,
        onUndoTrashEvent = notesViewModel::undoTrashSnackbarEvent
    )
    VaultAndroidCredentialPromptCoordinator(
        requestId = accessState.androidCredentialPromptRequestId,
        isPromptPending = accessState.isAndroidCredentialPromptPending,
        onPromptResult = accessViewModel::onAndroidCredentialPromptResult
    )
    LaunchedEffect(accessState.isUnlocked) {
        if (!accessState.isUnlocked) {
            activeActionsNote = null
            dateEditNote = null
            selectionState = selectionState.exit()
            showSearchFilters = false
        }
    }
    LaunchedEffect(notesState.isSearchActive) {
        if (!notesState.isSearchActive) showSearchFilters = false
    }
    VaultSelectionCleanupEffect(
        selectionState = selectionState,
        selectableIds = selectableVaultNoteIds,
        onSelectionChange = { selectionState = it }
    )
    RadialMenuFabHideEffect(selectionState.isActive)
    LaunchedEffect(
        accessState.isUnlocked,
        notesState.isUnlocked,
        moveNoteIdToConsume,
        notesViewModel
    ) {
        if (accessState.isUnlocked && notesState.isUnlocked && moveNoteIdToConsume > 0L) {
            val noteId = moveNoteIdToConsume
            moveNoteIdToConsume = 0L
            notesViewModel.moveNormalNoteToVault(noteId)
        }
    }
    val handleBack: () -> Unit = {
        when {
            selectionState.isActive -> {
                selectionState = selectionState.exit()
                activeActionsNote = null
            }
            accessState.isUnlocked && notesState.isSearchActive -> {
                notesViewModel.onSearchToggle(false)
            }
            accessState.isUnlocked && notesState.isTrashVisible -> {
                notesViewModel.toggleTrashVisibility()
            }
            else -> onBack()
        }
    }
    BackHandler(
        enabled = accessState.isUnlocked &&
            (selectionState.isActive || notesState.isSearchActive || notesState.isTrashVisible),
        onBack = handleBack
    )

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nexNoteBackground().nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { VaultSnackbarHost(snackbarHostState, floatingBottomPadding) },
        topBar = {
            if (selectionState.isActive) {
                SelectionTopAppBar(
                    selectedCount = selectionState.selectedCount,
                    totalCount = selectableVaultNoteIds.size,
                    scrollBehavior = scrollBehavior,
                    onClose = {
                        selectionState = selectionState.exit()
                        activeActionsNote = null
                    },
                    onSelectAll = {
                        selectionState = selectionState.selectAll(selectableVaultNoteIds)
                    },
                    onDeselectAll = {
                        selectionState = selectionState.deselectAll()
                    },
                    onNoteActions = selectedVaultNotes.singleOrNull()?.let { note ->
                        { activeActionsNote = note }
                    },
                    onShareSelected = {
                        shareCallbacks.onShareNotes(selectedVaultNotes)
                        selectionState = selectionState.exit()
                        activeActionsNote = null
                    },
                    onCopySelectedAsText = {
                        clipboardCallbacks.onCopyPlainTextNotes(selectedVaultNotes)
                        selectionState = selectionState.exit()
                        activeActionsNote = null
                    },
                    onCopySelectedAsMarkdown = {
                        clipboardCallbacks.onCopyMarkdownNotes(selectedVaultNotes)
                        selectionState = selectionState.exit()
                        activeActionsNote = null
                    },
                    onDeleteSelected = {
                        notesViewModel.moveToTrash(selectedVaultNotes)
                        selectionState = selectionState.exit()
                        activeActionsNote = null
                    }
                )
            } else {
                VaultTopBar(
                    isUnlocked = accessState.isUnlocked,
                    scrollBehavior = scrollBehavior,
                    searchQuery = notesState.searchQuery,
                    isSearchActive = notesState.isSearchActive,
                    sortOrder = notesState.sortOrder,
                    searchSort = notesState.searchSort,
                    hasActiveSearchFilters = notesState.hasActiveSearchFilters,
                    viewMode = notesState.viewMode,
                    isTrashVisible = notesState.isTrashVisible,
                    hasNotes = notesState.notes.isNotEmpty(),
                    tagFolderExpansion = tagFolderExpansion,
                    searchFocusRequester = searchFocusRequester,
                    onBack = handleBack,
                    onLock = accessViewModel::lock,
                    onSearchQueryChange = notesViewModel::onSearchQueryChange,
                    onSearchToggle = notesViewModel::onSearchToggle,
                    onOpenSearchFilters = { showSearchFilters = true },
                    onSearchSortChange = notesViewModel::setSearchSort,
                    onToggleSortOrder = notesViewModel::toggleSortOrder,
                    onToggleViewMode = notesViewModel::toggleViewMode,
                    onToggleTrashVisibility = notesViewModel::toggleTrashVisibility,
                    onStartSelection = {
                        selectionState = selectionState.enter()
                        activeActionsNote = null
                    }
                )
            }
        }
    ) { innerPadding ->
        VaultContent(
            uiState = accessState,
            notesState = notesState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            selectionState = selectionState,
            tagFolderExpansion = tagFolderExpansion,
            onConfigurePin = accessViewModel::configurePin,
            onUnlockWithPin = accessViewModel::unlockWithPin,
            onRequestAndroidCredentialPrompt =
                accessViewModel::requestAndroidCredentialPrompt,
            onClearError = accessViewModel::clearError,
            onNoteClick = onNoteClick,
            onToggleNoteSelection = { note ->
                selectionState = selectionState.toggle(note.id)
                activeActionsNote = null
            },
            onMoveToTrash = notesViewModel::moveToTrash,
            onTogglePin = notesViewModel::togglePin,
            onToggleTagFilter = notesViewModel::toggleTagFilter,
            onRemoveTagFilter = notesViewModel::removeTagFilter,
            onClearTagFilters = notesViewModel::clearTagFilters,
            onRestoreFromTrash = notesViewModel::restoreFromTrash,
            onRequestDeletePermanentlyFromTrash =
                notesViewModel::requestDeletePermanentlyFromTrash,
            bottomContentPadding = vaultBottomContentPadding(
                isFabAvailable = isVaultRadialMenuAvailable,
                floatingBottomPadding = floatingBottomPadding
            ),
            scrollToTopBottomPadding = if (isVaultRadialMenuAvailable) {
                RadialMenuOverlayDefaults.fabBottomClearance(floatingBottomPadding)
            } else {
                floatingBottomPadding + 16.dp
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    if (
        showSearchFilters && accessState.isUnlocked &&
        notesState.isUnlocked && notesState.isSearchActive
    ) {
        NoteSearchFiltersSheet(
            searchScope = notesState.searchScope,
            pinnedFilter = notesState.pinnedFilter,
            selectedTagFilters = notesState.selectedTagFilters,
            availableTagNames = notesState.availableTagNames,
            onSearchScopeChange = notesViewModel::setSearchScope,
            onPinnedFilterChange = notesViewModel::setPinnedFilter,
            onToggleTagFilter = notesViewModel::toggleTagFilter,
            onClearTagFilters = notesViewModel::clearTagFilters,
            onDismiss = { showSearchFilters = false }
        )
    }

    VaultNoteActionsSheet(
        note = if (accessState.isUnlocked) activeActionsNote else null,
        clipboardCallbacks = clipboardCallbacks,
        shareCallbacks = shareCallbacks,
        onMoveToTrash = notesViewModel::moveToTrash,
        onDuplicate = notesViewModel::duplicate,
        onRemoveFromVault = notesViewModel::removeFromVault,
        onEditCreationDate = { note -> dateEditNote = note },
        onDismiss = {
            activeActionsNote = null
            selectionState = selectionState.exit()
        }
    )
    NoteCreationDatePickerDialog(
        note = if (accessState.isUnlocked) dateEditNote else null,
        onConfirm = { creationDate ->
            dateEditNote?.let { note -> notesViewModel.updateCreationDate(note, creationDate) }
        },
        onDismiss = { dateEditNote = null }
    )
    if (accessState.isUnlocked && notesState.showTemplatePicker) {
        TemplatePickerDialog(
            templates = notesState.templates,
            onSelect = { templateId ->
                notesViewModel.dismissTemplatePicker()
                onCreateVaultNoteFromTemplate(templateId)
            },
            onDismiss = notesViewModel::dismissTemplatePicker
        )
    }
    VaultDeletePermanentlyDialog(
        visible = accessState.isUnlocked &&
            notesState.notePendingPermanentDeleteId != null,
        onConfirm = notesViewModel::confirmDeletePermanentlyFromTrash,
        onDismiss = notesViewModel::cancelDeletePermanentlyFromTrash
    )
}

@Composable
private fun VaultRadialMenu(
    isAvailable: Boolean,
    onCreateVaultNote: () -> Unit,
    onCreateVaultNoteFromTemplate: () -> Unit,
    onSearchClick: () -> Unit
) {
    val newNoteDescription = stringResource(R.string.vault_new_note)
    val newNoteFromTemplateDescription = stringResource(R.string.vault_new_note_from_template)
    val searchDescription = stringResource(R.string.vault_search)
    RadialMenuEffect(
        items = remember(
            isAvailable,
            onCreateVaultNote,
            onCreateVaultNoteFromTemplate,
            onSearchClick,
            newNoteDescription,
            newNoteFromTemplateDescription,
            searchDescription
        ) {
            if (!isAvailable) {
                emptyList()
            } else {
                listOf(
                    RadialMenuItem(
                        icon = Icons.Default.Add,
                        label = "",
                        contentDescription = newNoteDescription,
                        action = onCreateVaultNote
                    ),
                    RadialMenuItem(
                        icon = Icons.Default.Description,
                        label = "",
                        contentDescription = newNoteFromTemplateDescription,
                        action = onCreateVaultNoteFromTemplate
                    ),
                    RadialMenuItem(
                        icon = Icons.Default.Search,
                        label = "",
                        contentDescription = searchDescription,
                        action = onSearchClick
                    )
                )
            }
        },
        fabContentDescription = stringResource(R.string.vault_open_menu)
    )
}

private fun vaultBottomContentPadding(
    isFabAvailable: Boolean,
    floatingBottomPadding: Dp
): Dp =
    if (isFabAvailable) {
        RadialMenuOverlayDefaults.fabBottomClearance(floatingBottomPadding)
    } else {
        NoteCollectionLayoutDefaults.defaultBottomPadding
    }

@Composable
private fun VaultSnackbarHost(
    snackbarHostState: SnackbarHostState,
    floatingBottomPadding: Dp
) {
    RadialMenuSnackbarHost(
        hostState = snackbarHostState,
        bottomInset = floatingBottomPadding
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultTopBar(
    isUnlocked: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    searchQuery: String,
    isSearchActive: Boolean,
    sortOrder: SortOrder,
    searchSort: NoteSearchSort,
    hasActiveSearchFilters: Boolean,
    viewMode: NoteListViewMode,
    isTrashVisible: Boolean,
    hasNotes: Boolean,
    tagFolderExpansion: NoteTagFolderExpansionState,
    searchFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onOpenSearchFilters: () -> Unit,
    onSearchSortChange: (NoteSearchSort) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleTrashVisibility: () -> Unit,
    onStartSelection: () -> Unit
) {
    TopAppBar(
        title = {
            VaultTopBarTitle(
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                isTrashVisible = isTrashVisible,
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = onSearchQueryChange
            )
        },
        navigationIcon = {
            NexIconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack
            )
        },
        actions = {
            if (isUnlocked) {
                if (isSearchActive) {
                    NoteSearchSortMenu(
                        selected = searchSort,
                        onSelect = onSearchSortChange
                    )
                    NexIconButton(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = stringResource(R.string.common_filter_search_results),
                        selected = hasActiveSearchFilters,
                        onClick = onOpenSearchFilters
                    )
                    NexIconButton(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close_search),
                        onClick = { onSearchToggle(false) }
                    )
                } else {
                    NexIconButton(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(
                            if (isTrashVisible) R.string.vault_search_trash
                            else R.string.vault_search
                        ),
                        onClick = { onSearchToggle(true) }
                    )
                    NoteListSortButton(
                        sortOrder = sortOrder,
                        onToggleSortOrder = onToggleSortOrder
                    )
                    if (!isTrashVisible && viewMode == NoteListViewMode.TAGS && hasNotes) {
                        TagFolderExpandAllButton(
                            isAllCollapsed = tagFolderExpansion.isAllCollapsed,
                            onClick = tagFolderExpansion::toggleAll
                        )
                    }
                    if (!isTrashVisible) {
                        VaultTrashButton(onToggleTrashVisibility = onToggleTrashVisibility)
                    }
                }
                VaultOverflowMenu(
                    viewMode = viewMode,
                    isTrashVisible = isTrashVisible,
                    onToggleViewMode = onToggleViewMode,
                    onStartSelection = onStartSelection,
                    onLock = onLock
                )
            }
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun VaultOverflowMenu(
    viewMode: NoteListViewMode,
    isTrashVisible: Boolean,
    onToggleViewMode: () -> Unit,
    onStartSelection: () -> Unit,
    onLock: () -> Unit
) {
    NoteListOverflowMenu(
        viewMode = viewMode,
        onToggleViewMode = onToggleViewMode,
        contentDescription = stringResource(R.string.vault_options),
        availableViewModes = if (isTrashVisible) listOf(NoteListViewMode.LIST)
            else NoteListViewMode.noteModes
    ) { dismiss ->
        if (!isTrashVisible) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.vault_select_notes)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = null
                    )
                },
                onClick = {
                    dismiss()
                    onStartSelection()
                }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.vault_lock)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null
                )
            },
            onClick = {
                dismiss()
                onLock()
            }
        )
    }
}

@Composable
private fun VaultTrashButton(
    onToggleTrashVisibility: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.Delete,
        contentDescription = stringResource(R.string.vault_show_trash),
        onClick = onToggleTrashVisibility
    )
}

@Composable
private fun VaultTopBarTitle(
    searchQuery: String,
    isSearchActive: Boolean,
    isTrashVisible: Boolean,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            NexSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = stringResource(
                    if (isTrashVisible) R.string.vault_search_trash
                    else R.string.vault_search
                ),
                modifier = Modifier.fillMaxWidth(),
                focusRequester = searchFocusRequester,
                textStyle = MaterialTheme.typography.titleMedium
            )
        }
        AnimatedVisibility(
            visible = !isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            Text(
                text = stringResource(
                    if (isTrashVisible) R.string.vault_trash_title else R.string.vault_title
                ),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VaultSearchFocusEffect(
    isSearchActive: Boolean,
    searchFocusRequester: FocusRequester
) {
    LaunchedEffect(isSearchActive, searchFocusRequester) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun VaultSelectionCleanupEffect(
    selectionState: SelectionUiState,
    selectableIds: List<Long>,
    onSelectionChange: (SelectionUiState) -> Unit
) {
    LaunchedEffect(selectionState, selectableIds) {
        val retained = if (selectableIds.isEmpty() && selectionState.isActive) {
            selectionState.exit()
        } else {
            selectionState.retainSelectableIds(selectableIds)
        }
        if (retained != selectionState) {
            onSelectionChange(retained)
        }
    }
}

@Composable
private fun VaultActionMessagesEffect(
    viewModel: VaultNotesViewModel,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.vaultActionMessages.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }
}

@Composable
private fun VaultContent(
    uiState: VaultAccessUiState,
    notesState: VaultNotesUiState,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    selectionState: SelectionUiState,
    tagFolderExpansion: NoteTagFolderExpansionState,
    onConfigurePin: (CharArray, CharArray) -> Unit,
    onUnlockWithPin: (CharArray) -> Unit,
    onRequestAndroidCredentialPrompt: () -> Unit,
    onClearError: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    bottomContentPadding: Dp,
    scrollToTopBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    when {
        uiState.requiresSetup -> {
            CenteredFormBox(modifier = modifier) {
                VaultSetupForm(
                    uiState = uiState,
                    onConfigurePin = onConfigurePin,
                    onClearError = onClearError
                )
            }
        }
        uiState.isUnlocked -> {
            // [notesState.isUnlocked] is an additional UI-level guard on top of
            // the data-layer contract: the notes flow only emits decrypted
            // content when the Vault key is unlocked in memory.
            //
            // Treat "access is unlocked but notes flow hasn't emitted yet" as
            // the same loading state Home renders via `HomeUiState.isLoading`:
            // the access ViewModel may report `UNLOCKED` a few frames before
            // the encrypted notes/trash/tags flows join, and during that
            // window we must not flash the "No Vault notes" empty state. No
            // Vault content is visible while the spinner is shown.
            val isVaultNotesLoading = notesState.isLoading || !notesState.isUnlocked
            VaultNotesCollection(
                notes = if (notesState.isUnlocked) notesState.notes else emptyList(),
                scoredResults = if (notesState.isUnlocked) {
                    notesState.scoredResults
                } else {
                    emptyList()
                },
                viewMode = notesState.viewMode,
                noteCardStyle = noteCardStyle,
                listState = listState,
                gridState = gridState,
                selectionState = selectionState,
                tagFolderExpansion = tagFolderExpansion,
                isLoading = isVaultNotesLoading,
                isTrashVisible = notesState.isTrashVisible,
                isSearchActive = notesState.isSearchActive,
                topTags = if (notesState.isUnlocked) notesState.topTags else emptyList(),
                selectedTagFilters = if (notesState.isUnlocked) {
                    notesState.selectedTagFilters
                } else {
                    emptySet()
                },
                onNoteClick = onNoteClick,
                onToggleNoteSelection = onToggleNoteSelection,
                onMoveToTrash = onMoveToTrash,
                onTogglePin = onTogglePin,
                onToggleTagFilter = onToggleTagFilter,
                onRemoveTagFilter = onRemoveTagFilter,
                onClearTagFilters = onClearTagFilters,
                onRestoreFromTrash = onRestoreFromTrash,
                onRequestDeletePermanentlyFromTrash =
                    onRequestDeletePermanentlyFromTrash,
                bottomContentPadding = bottomContentPadding,
                scrollToTopBottomPadding = scrollToTopBottomPadding,
                modifier = modifier
            )
        }
        else -> {
            CenteredFormBox(modifier = modifier) {
                VaultUnlockForm(
                    uiState = uiState,
                    onUnlockWithPin = onUnlockWithPin,
                    onRequestAndroidCredentialPrompt = onRequestAndroidCredentialPrompt,
                    onClearError = onClearError
                )
            }
        }
    }
}

@Composable
private fun CenteredFormBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.imePadding()) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.widthIn(max = 480.dp)) { content() }
        }
    }
}

@Composable
private fun VaultSetupForm(
    uiState: VaultAccessUiState,
    onConfigurePin: (CharArray, CharArray) -> Unit,
    onClearError: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    LaunchedEffect(uiState.vaultState) {
        pin = ""
        confirmation = ""
    }

    VaultAccessForm(
        title = stringResource(R.string.vault_setup_title),
        pin = pin,
        confirmation = confirmation,
        showConfirmation = true,
        buttonText = stringResource(R.string.vault_create_pin),
        isBusy = uiState.isBusy,
        errorText = uiState.error?.message(),
        failedPinAttemptsText = null,
        onPinChange = {
            pin = it
            onClearError()
        },
        onConfirmationChange = {
            confirmation = it
            onClearError()
        },
        onSubmit = {
            val pinChars = pin.toCharArray()
            val confirmationChars = confirmation.toCharArray()
            pin = ""
            confirmation = ""
            onConfigurePin(pinChars, confirmationChars)
        }
    )
}

@Composable
internal fun VaultUnlockForm(
    uiState: VaultAccessUiState,
    onUnlockWithPin: (CharArray) -> Unit,
    onRequestAndroidCredentialPrompt: () -> Unit,
    onClearError: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(uiState.vaultState) {
        pin = ""
    }

    VaultAccessForm(
        title = stringResource(R.string.vault_unlock_title),
        pin = pin,
        confirmation = "",
        showConfirmation = false,
        buttonText = stringResource(R.string.vault_unlock_action),
        isBusy = uiState.isBusy,
        errorText = uiState.error?.message(),
        failedPinAttemptsText = uiState.failedPinAttempts.toFailedPinAttemptsText(),
        onPinChange = {
            pin = it
            onClearError()
        },
        onConfirmationChange = {},
        onSubmit = {
            val pinChars = pin.toCharArray()
            pin = ""
            onUnlockWithPin(pinChars)
        },
        extraActions = {
            if (uiState.canUseAndroidCredential) {
                VaultAndroidCredentialUnlockButton(
                    enabled = !uiState.isBusy,
                    onClick = {
                        pin = ""
                        onRequestAndroidCredentialPrompt()
                    }
                )
            }
        }
    )
}

@Composable
private fun VaultAccessForm(
    title: String,
    pin: String,
    confirmation: String,
    showConfirmation: Boolean,
    buttonText: String,
    isBusy: Boolean,
    errorText: String?,
    failedPinAttemptsText: String?,
    onPinChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onSubmit: () -> Unit,
    extraActions: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface
        )
        VaultPinField(
            value = pin,
            label = stringResource(R.string.vault_pin_label),
            enabled = !isBusy,
            imeAction = if (showConfirmation) ImeAction.Next else ImeAction.Done,
            onValueChange = onPinChange,
            onDone = {
                if (!showConfirmation) onSubmit()
            }
        )
        if (showConfirmation) {
            VaultPinField(
                value = confirmation,
                label = stringResource(R.string.vault_confirm_pin_label),
                enabled = !isBusy,
                imeAction = ImeAction.Done,
                onValueChange = onConfirmationChange,
                onDone = onSubmit
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (failedPinAttemptsText != null) {
            Text(
                text = failedPinAttemptsText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = !isBusy,
            onClick = onSubmit,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                )
            } else {
                Text(buttonText)
            }
        }
        extraActions()
    }
}

@Composable
private fun VaultAndroidCredentialUnlockButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Icon(
            imageVector = Icons.Default.LockOpen,
            contentDescription = null
        )
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.vault_use_android_lock))
    }
}

@Composable
private fun VaultPinField(
    value: String,
    label: String,
    enabled: Boolean,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() })
    )
}

@Composable
internal fun VaultNotesCollection(
    notes: List<Note>,
    viewMode: NoteListViewMode,
    noteCardStyle: NoteCardStyle,
    isTrashVisible: Boolean,
    onNoteClick: (Long) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    modifier: Modifier = Modifier,
    scoredResults: List<ScoredNote> = emptyList(),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    selectionState: SelectionUiState = SelectionUiState(),
    tagFolderExpansion: NoteTagFolderExpansionState = rememberNoteTagFolderExpansionState(),
    isLoading: Boolean = false,
    isSearchActive: Boolean = false,
    topTags: List<Tag> = emptyList(),
    selectedTagFilters: Set<String> = emptySet(),
    onToggleNoteSelection: (Note) -> Unit = {},
    onToggleTagFilter: (String) -> Unit = {},
    onRemoveTagFilter: (String) -> Unit = {},
    onClearTagFilters: () -> Unit = {},
    bottomContentPadding: Dp = NoteCollectionLayoutDefaults.defaultBottomPadding,
    scrollToTopBottomPadding: Dp = 16.dp
) {
    val collectionViewMode = if (isTrashVisible) NoteListViewMode.LIST else viewMode
    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        Column(modifier = Modifier.fillMaxSize()) {
            VaultProtectionBanner(isTrashVisible = isTrashVisible, viewMode = collectionViewMode)
            // Tag filter bars are hidden while loading: their state derives from
            // the encrypted Vault tags flow, which has not yet emitted.
            if (!isLoading) {
                VaultTagFilterBars(
                    topTags = topTags,
                    selectedTagFilters = selectedTagFilters,
                    isTrashVisible = isTrashVisible,
                    isSearchActive = isSearchActive,
                    onToggleTagFilter = onToggleTagFilter,
                    onRemoveTagFilter = onRemoveTagFilter,
                    onClearTagFilters = onClearTagFilters
                )
            }
            VaultNotesBody(
                notes = notes,
                scoredResults = scoredResults,
                viewMode = collectionViewMode,
                noteCardStyle = noteCardStyle,
                listState = listState,
                gridState = gridState,
                isLoading = isLoading,
                isTrashVisible = isTrashVisible,
                isSearchActive = isSearchActive,
                hasTagFilter = selectedTagFilters.isNotEmpty(),
                selectionState = selectionState,
                tagFolderExpansion = tagFolderExpansion,
                onNoteClick = onNoteClick,
                onToggleNoteSelection = onToggleNoteSelection,
                onMoveToTrash = onMoveToTrash,
                onTogglePin = onTogglePin,
                onRestoreFromTrash = onRestoreFromTrash,
                onRequestDeletePermanentlyFromTrash = onRequestDeletePermanentlyFromTrash,
                bottomContentPadding = bottomContentPadding,
                modifier = Modifier.weight(1f)
            )
        }
        if (!isLoading && notes.isNotEmpty()) {
            when (collectionViewMode) {
                NoteListViewMode.GRID -> ScrollToTopButton(
                    gridState = gridState,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = scrollToTopBottomPadding)
                )
                NoteListViewMode.LIST,
                NoteListViewMode.TAGS -> ScrollToTopButton(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = scrollToTopBottomPadding)
                )
            }
        }
    }
}

@Composable
private fun VaultProtectionBanner(
    isTrashVisible: Boolean,
    viewMode: NoteListViewMode,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NoteCollectionLayoutDefaults.horizontalPadding(viewMode), vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isTrashVisible) R.string.vault_protected_trash_title
                        else R.string.vault_protected_mode_title
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = stringResource(
                        if (isTrashVisible) R.string.vault_protected_trash_message
                        else R.string.vault_protected_mode_message
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VaultTagFilterBars(
    topTags: List<Tag>,
    selectedTagFilters: Set<String>,
    isTrashVisible: Boolean,
    isSearchActive: Boolean,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit
) {
    if (isTrashVisible) return

    if (topTags.isNotEmpty() && !isSearchActive) {
        AutoScrollingTagRow(
            tags = topTags,
            onTagClick = onToggleTagFilter,
            selectedTags = selectedTagFilters,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag(VAULT_TOP_TAGS_TAG)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
    TagFilterBar(
        selectedTags = selectedTagFilters,
        onTagRemove = onRemoveTagFilter,
        onClearAll = onClearTagFilters
    )
}

@Composable
private fun VaultNotesBody(
    notes: List<Note>,
    scoredResults: List<ScoredNote>,
    viewMode: NoteListViewMode,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    isLoading: Boolean,
    isTrashVisible: Boolean,
    isSearchActive: Boolean,
    hasTagFilter: Boolean,
    selectionState: SelectionUiState,
    tagFolderExpansion: NoteTagFolderExpansionState,
    onNoteClick: (Long) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        VaultNotesLoadingState(modifier = modifier)
        return
    }
    if (notes.isEmpty()) {
        VaultNotesEmptyState(
            isTrashVisible = isTrashVisible,
            isSearchActive = isSearchActive,
            hasTagFilter = hasTagFilter,
            modifier = modifier
        )
        return
    }
    when (viewMode) {
        NoteListViewMode.GRID -> {
            VaultNotesGrid(
                notes = notes,
                scoredResults = scoredResults,
                isReadOnly = isTrashVisible,
                noteCardStyle = noteCardStyle,
                gridState = gridState,
                selectionState = selectionState,
                onNoteClick = onNoteClick,
                onToggleNoteSelection = onToggleNoteSelection,
                onMoveToTrash = onMoveToTrash,
                onTogglePin = onTogglePin,
                onRestoreFromTrash = onRestoreFromTrash,
                onRequestDeletePermanentlyFromTrash = onRequestDeletePermanentlyFromTrash,
                bottomContentPadding = bottomContentPadding,
                modifier = modifier
            )
        }
        NoteListViewMode.TAGS -> {
            VaultNotesTagFolders(
                notes = notes,
                scoredResults = scoredResults,
                isReadOnly = isTrashVisible,
                noteCardStyle = noteCardStyle,
                listState = listState,
                selectionState = selectionState,
                tagFolderExpansion = tagFolderExpansion,
                onNoteClick = onNoteClick,
                onToggleNoteSelection = onToggleNoteSelection,
                onMoveToTrash = onMoveToTrash,
                onTogglePin = onTogglePin,
                onRestoreFromTrash = onRestoreFromTrash,
                onRequestDeletePermanentlyFromTrash = onRequestDeletePermanentlyFromTrash,
                bottomContentPadding = bottomContentPadding,
                modifier = modifier
            )
        }
        NoteListViewMode.LIST -> {
            VaultNotesList(
                notes = notes,
                scoredResults = scoredResults,
                isReadOnly = isTrashVisible,
                noteCardStyle = noteCardStyle,
                listState = listState,
                selectionState = selectionState,
                onNoteClick = onNoteClick,
                onToggleNoteSelection = onToggleNoteSelection,
                onMoveToTrash = onMoveToTrash,
                onTogglePin = onTogglePin,
                onRestoreFromTrash = onRestoreFromTrash,
                onRequestDeletePermanentlyFromTrash = onRequestDeletePermanentlyFromTrash,
                bottomContentPadding = bottomContentPadding,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun VaultNotesList(
    notes: List<Note>,
    scoredResults: List<ScoredNote>,
    isReadOnly: Boolean,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    selectionState: SelectionUiState,
    onNoteClick: (Long) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val displayItems = rememberVaultDisplayItems(notes, scoredResults)
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().clipToBounds(),
        contentPadding = NoteCollectionLayoutDefaults.listContentPadding(
            bottomPadding = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(NoteCollectionLayoutDefaults.itemSpacing)
    ) {
        items(
            items = displayItems,
            key = { it.note.id },
            contentType = { "note_card" }
        ) { scored ->
            val note = scored.note
            if (isReadOnly) {
                VaultTrashNoteCard(
                    note = note,
                    onRestoreFromTrash = onRestoreFromTrash,
                    onRequestDeletePermanentlyFromTrash =
                        onRequestDeletePermanentlyFromTrash,
                    modifier = animateNoteItem()
                )
            } else {
                VaultActiveNoteCard(
                    scored = scored,
                    onNoteClick = onNoteClick,
                    onToggleNoteSelection = onToggleNoteSelection,
                    onMoveToTrash = onMoveToTrash,
                    onTogglePin = onTogglePin,
                    noteCardStyle = noteCardStyle,
                    selectionState = selectionState,
                    modifier = Modifier
                        .testTag(VAULT_NOTE_ROW_TAG)
                        .then(animateNoteItem())
                )
            }
        }
    }
}

@Composable
private fun VaultNotesGrid(
    notes: List<Note>,
    scoredResults: List<ScoredNote>,
    isReadOnly: Boolean,
    noteCardStyle: NoteCardStyle,
    gridState: LazyStaggeredGridState,
    selectionState: SelectionUiState,
    onNoteClick: (Long) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val displayItems = rememberVaultDisplayItems(notes, scoredResults)
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        modifier = modifier.fillMaxSize().clipToBounds(),
        contentPadding = NoteCollectionLayoutDefaults.gridContentPadding(
            bottomPadding = bottomContentPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(NoteCollectionLayoutDefaults.itemSpacing),
        verticalItemSpacing = NoteCollectionLayoutDefaults.itemSpacing
    ) {
        items(
            items = displayItems,
            key = { it.note.id },
            contentType = { "note_card" }
        ) { scored ->
            val note = scored.note
            if (isReadOnly) {
                VaultTrashNoteCard(
                    note = note,
                    onRestoreFromTrash = onRestoreFromTrash,
                    onRequestDeletePermanentlyFromTrash =
                        onRequestDeletePermanentlyFromTrash,
                    modifier = animateNoteItem()
                )
            } else {
                VaultActiveNoteCard(
                    scored = scored,
                    onNoteClick = onNoteClick,
                    onToggleNoteSelection = onToggleNoteSelection,
                    onMoveToTrash = onMoveToTrash,
                    onTogglePin = onTogglePin,
                    noteCardStyle = noteCardStyle,
                    selectionState = selectionState,
                    modifier = Modifier
                        .testTag(VAULT_NOTE_ROW_TAG)
                        .then(animateNoteItem())
                )
            }
        }
    }
}

@Composable
private fun VaultNotesTagFolders(
    notes: List<Note>,
    scoredResults: List<ScoredNote>,
    isReadOnly: Boolean,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    selectionState: SelectionUiState,
    tagFolderExpansion: NoteTagFolderExpansionState,
    onNoteClick: (Long) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val displayItems = rememberVaultDisplayItems(notes, scoredResults)
    NoteTagFolderCollection(
        displayItems = displayItems,
        listState = listState,
        bottomContentPadding = bottomContentPadding,
        modifier = modifier,
        expansionState = tagFolderExpansion
    ) { scored, itemModifier ->
        val note = scored.note
        if (isReadOnly) {
            VaultTrashNoteCard(
                note = note,
                onRestoreFromTrash = onRestoreFromTrash,
                onRequestDeletePermanentlyFromTrash = onRequestDeletePermanentlyFromTrash,
                modifier = itemModifier
            )
        } else {
            VaultActiveNoteCard(
                scored = scored,
                onNoteClick = onNoteClick,
                onToggleNoteSelection = onToggleNoteSelection,
                onMoveToTrash = onMoveToTrash,
                onTogglePin = onTogglePin,
                noteCardStyle = noteCardStyle,
                selectionState = selectionState,
                modifier = itemModifier.testTag(VAULT_NOTE_ROW_TAG)
            )
        }
    }
}

@Composable
private fun rememberVaultDisplayItems(
    notes: List<Note>,
    scoredResults: List<ScoredNote>
): List<ScoredNote> =
    remember(notes, scoredResults) {
        if (scoredResults.isNotEmpty()) {
            scoredResults
        } else {
            notes.map { ScoredNote(it, 0, emptyList(), emptyList()) }
        }
    }

@Composable
private fun VaultTrashNoteCard(
    note: Note,
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    TrashNoteCard(
        note = note,
        onRestore = { onRestoreFromTrash(note) },
        onDeletePermanently = { onRequestDeletePermanentlyFromTrash(note) },
        modifier = modifier
    )
}

@Composable
private fun VaultDeletePermanentlyDialog(
    visible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        tonalElevation = 1.dp,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.vault_delete_dialog_title)) },
        text = {
            Text(stringResource(R.string.vault_delete_dialog_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun VaultNotesLoadingState(
    modifier: Modifier = Modifier
) {
    // Mirrors `HomeContent.HomeLoadingState`: a single centered spinner while
    // the Vault notes flow has not yet emitted. No Vault content is rendered.
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(VAULT_NOTES_LOADING_TAG),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun VaultNotesEmptyState(
    isTrashVisible: Boolean,
    isSearchActive: Boolean,
    hasTagFilter: Boolean,
    modifier: Modifier = Modifier
) {
    NexEmptyState(
        icon = vaultEmptyStateIcon(
            isTrashVisible = isTrashVisible,
            isSearchActive = isSearchActive,
            hasTagFilter = hasTagFilter
        ),
        title = vaultEmptyStateTitle(
            isTrashVisible = isTrashVisible,
            isSearchActive = isSearchActive,
            hasTagFilter = hasTagFilter
        ),
        message = vaultEmptyStateMessage(
            isTrashVisible = isTrashVisible,
            isSearchActive = isSearchActive,
            hasTagFilter = hasTagFilter
        ),
        modifier = modifier
    )
}

private fun vaultEmptyStateIcon(
    isTrashVisible: Boolean,
    isSearchActive: Boolean,
    hasTagFilter: Boolean
): androidx.compose.ui.graphics.vector.ImageVector =
    when {
        isSearchActive -> Icons.AutoMirrored.Filled.ManageSearch
        hasTagFilter -> Icons.Default.Sell
        isTrashVisible -> Icons.Default.Delete
        else -> Icons.AutoMirrored.Filled.Note
    }

@Composable
private fun vaultEmptyStateTitle(
    isTrashVisible: Boolean,
    isSearchActive: Boolean,
    hasTagFilter: Boolean
): String =
    stringResource(
        when {
            isSearchActive -> R.string.vault_empty_no_results_title
            hasTagFilter -> R.string.vault_empty_tag_filter_title
            isTrashVisible -> R.string.vault_empty_trash_title
            else -> R.string.vault_empty_notes_title
        }
    )

@Composable
private fun vaultEmptyStateMessage(
    isTrashVisible: Boolean,
    isSearchActive: Boolean,
    hasTagFilter: Boolean
): String =
    stringResource(
        when {
            isSearchActive -> R.string.vault_empty_no_results_message
            hasTagFilter -> R.string.vault_empty_tag_filter_message
            isTrashVisible -> R.string.vault_empty_trash_message
            else -> R.string.vault_empty_notes_message
        }
    )

@Composable
private fun VaultActiveNoteCard(
    scored: ScoredNote,
    onNoteClick: (Long) -> Unit,
    onToggleNoteSelection: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    noteCardStyle: NoteCardStyle,
    selectionState: SelectionUiState,
    modifier: Modifier = Modifier
) {
    val note = scored.note
    NoteCard(
        note = note,
        onClick = {
            if (selectionState.isActive) {
                onToggleNoteSelection(note)
            } else {
                onNoteClick(note.id)
            }
        },
        onTrash = { onMoveToTrash(note) },
        modifier = modifier,
        noteCardStyle = noteCardStyle,
        titleHighlightRanges = scored.titleRanges,
        contentHighlightRanges = scored.contentRanges,
        onPin = { onTogglePin(note) },
        onLongPress = { onToggleNoteSelection(note) },
        selectionMode = selectionState.isActive,
        selected = selectionState.isSelected(note.id)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultNoteActionsSheet(
    note: Note?,
    clipboardCallbacks: NoteClipboardCallbacks,
    shareCallbacks: NoteShareCallbacks? = null,
    onMoveToTrash: (Note) -> Unit,
    onDuplicate: (Note) -> Unit,
    onRemoveFromVault: (Note) -> Unit,
    onEditCreationDate: ((Note) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (note == null) return

    var page by remember(note.id) { mutableStateOf(VaultNoteActionsPage.Actions) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        dragHandle = { io.github.r0x4nk.nexnote.ui.component.NexSheetDragHandle() },
        tonalElevation = 1.dp,
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            val onHeaderBack: () -> Unit = if (page == VaultNoteActionsPage.Actions) {
                onDismiss
            } else {
                { page = VaultNoteActionsPage.Actions }
            }
            NexSheetHeader(
                title = stringResource(
                    if (page == VaultNoteActionsPage.Actions) {
                        R.string.vault_actions_title
                    } else {
                        R.string.vault_copy_actions_title
                    }
                ),
                subtitle = stringResource(R.string.vault_selected_note),
                onBack = onHeaderBack
            )
            Spacer(Modifier.size(8.dp))
            when (page) {
                VaultNoteActionsPage.Actions -> VaultNoteActionsMainPage(
                    showShare = shareCallbacks != null,
                    onShare = {
                        shareCallbacks?.onShareNote(note)
                        onDismiss()
                    },
                    onCopy = { page = VaultNoteActionsPage.Copy },
                    onMoveToTrash = {
                        onMoveToTrash(note)
                        onDismiss()
                    },
                    onDuplicate = {
                        onDuplicate(note)
                        onDismiss()
                    },
                    onEditCreationDate = onEditCreationDate?.let { edit ->
                        {
                            edit(note)
                            onDismiss()
                        }
                    },
                    onRemoveFromVault = {
                        onRemoveFromVault(note)
                        onDismiss()
                    }
                )

                VaultNoteActionsPage.Copy -> VaultNoteActionsCopyPage(
                    onCopyPlainText = {
                        clipboardCallbacks.onCopyPlainText(note)
                        onDismiss()
                    },
                    onCopyMarkdown = {
                        clipboardCallbacks.onCopyMarkdown(note)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun VaultNoteActionsMainPage(
    showShare: Boolean,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDuplicate: () -> Unit,
    onEditCreationDate: (() -> Unit)?,
    onRemoveFromVault: () -> Unit
) {
    if (showShare) {
        NoteActionsSheetRow(
            text = stringResource(R.string.common_share),
            icon = Icons.Default.IosShare,
            onClick = onShare
        )
    }
    NoteActionsSheetRow(
        text = stringResource(R.string.common_copy),
        icon = Icons.Outlined.ContentCopy,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_COPY_TAG),
        onClick = onCopy
    )
    NoteActionsSheetRow(
        text = stringResource(R.string.common_duplicate),
        icon = Icons.Outlined.FileCopy,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_DUPLICATE_TAG),
        onClick = onDuplicate
    )
    if (onEditCreationDate != null) {
        NoteActionsSheetRow(
            text = stringResource(R.string.edit_creation_date),
            icon = Icons.Outlined.EditCalendar,
            modifier = Modifier.testTag(VAULT_NOTE_ACTION_EDIT_DATE_TAG),
            onClick = onEditCreationDate
        )
    }
    NoteActionsSheetRow(
        text = stringResource(R.string.common_remove_from_vault),
        icon = Icons.Default.LockOpen,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_REMOVE_FROM_VAULT_TAG),
        onClick = onRemoveFromVault
    )
    NoteActionsSheetRow(
        text = stringResource(R.string.common_move_to_trash),
        icon = Icons.Outlined.Delete,
        destructive = true,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_MOVE_TO_TRASH_TAG),
        onClick = onMoveToTrash
    )
}

@Composable
private fun VaultNoteActionsCopyPage(
    onCopyPlainText: () -> Unit,
    onCopyMarkdown: () -> Unit
) {
    NoteActionsSheetRow(
        text = stringResource(R.string.common_copy_as_text),
        icon = Icons.AutoMirrored.Outlined.TextSnippet,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_COPY_TEXT_TAG),
        onClick = onCopyPlainText
    )
    NoteActionsSheetRow(
        text = stringResource(R.string.common_copy_as_markdown),
        icon = Icons.Outlined.Code,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_COPY_MARKDOWN_TAG),
        onClick = onCopyMarkdown
    )
}

@Composable
private fun VaultAccessError.message(): String = stringResource(
    when (this) {
        VaultAccessError.EMPTY_PIN -> R.string.vault_error_empty_pin
        VaultAccessError.PIN_MISMATCH -> R.string.vault_error_pin_mismatch
        VaultAccessError.WRONG_PIN -> R.string.vault_error_wrong_pin
        VaultAccessError.PIN_RATE_LIMITED -> R.string.vault_error_rate_limited
        VaultAccessError.VAULT_NOT_CONFIGURED -> R.string.vault_error_not_configured
        VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE ->
            R.string.vault_error_credential_unavailable
        VaultAccessError.ANDROID_CREDENTIAL_CANCELED ->
            R.string.vault_error_credential_canceled
        VaultAccessError.ANDROID_CREDENTIAL_RESET_REQUIRED ->
            R.string.vault_error_credential_reset_required
        VaultAccessError.OPERATION_FAILED -> R.string.vault_error_operation_failed
    }
)

@Composable
private fun Int.toFailedPinAttemptsText(): String? =
    if (this > 0) stringResource(R.string.vault_failed_pin_attempts, this) else null
