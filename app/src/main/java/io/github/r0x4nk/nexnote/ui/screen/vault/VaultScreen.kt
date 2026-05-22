package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.common.NoteCollectionLayoutDefaults
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.component.AutoScrollingTagRow
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.NoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.component.NoteCard
import io.github.r0x4nk.nexnote.ui.component.NoteActionsSheetHeader
import io.github.r0x4nk.nexnote.ui.component.NoteActionsSheetRow
import io.github.r0x4nk.nexnote.ui.component.TagFilterBar
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors
import io.github.r0x4nk.nexnote.ui.component.rememberNoteClipboardCallbacks
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuItem
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuSnackbarHost
import io.github.r0x4nk.nexnote.ui.screen.home.TemplatePickerDialog
import io.github.r0x4nk.nexnote.ui.screen.trash.TrashNoteCard

internal const val VAULT_NOTE_ACTION_COPY_TAG = "vault_note_action_copy"
internal const val VAULT_NOTE_ACTION_COPY_TEXT_TAG = "vault_note_action_copy_text"
internal const val VAULT_NOTE_ACTION_COPY_MARKDOWN_TAG = "vault_note_action_copy_markdown"
internal const val VAULT_NOTE_ACTION_MOVE_TO_TRASH_TAG = "vault_note_action_move_to_trash"
internal const val VAULT_NOTE_ACTION_DUPLICATE_TAG = "vault_note_action_duplicate"
internal const val VAULT_NOTE_ACTION_REMOVE_FROM_VAULT_TAG =
    "vault_note_action_remove_from_vault"
internal const val VAULT_NOTE_ROW_TAG = "vault_note_row"
internal const val VAULT_NOTES_LOADING_TAG = "vault_notes_loading"

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
    val noteCardStyle by notesViewModel.noteCardStyle.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val clipboardCallbacks = rememberNoteClipboardCallbacks(snackbarHostState)
    var activeActionsNote by remember { mutableStateOf<Note?>(null) }
    var moveNoteIdToConsume by rememberSaveable(pendingMoveNoteId) {
        mutableStateOf(pendingMoveNoteId)
    }

    val isVaultNotesSurfaceUnlocked = accessState.isUnlocked && notesState.isUnlocked
    val isVaultRadialMenuAvailable =
        isVaultNotesSurfaceUnlocked && !notesState.isTrashVisible

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
        }
    }
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
            (notesState.isSearchActive || notesState.isTrashVisible),
        onBack = handleBack
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { VaultSnackbarHost(snackbarHostState, floatingBottomPadding) },
        topBar = {
            VaultTopBar(
                isUnlocked = accessState.isUnlocked,
                scrollBehavior = scrollBehavior,
                searchQuery = notesState.searchQuery,
                isSearchActive = notesState.isSearchActive,
                sortOrder = notesState.sortOrder,
                viewMode = notesState.viewMode,
                isTrashVisible = notesState.isTrashVisible,
                searchFocusRequester = searchFocusRequester,
                onBack = handleBack,
                onLock = accessViewModel::lock,
                onSearchQueryChange = notesViewModel::onSearchQueryChange,
                onSearchToggle = notesViewModel::onSearchToggle,
                onToggleSortOrder = notesViewModel::toggleSortOrder,
                onToggleViewMode = notesViewModel::toggleViewMode,
                onToggleTrashVisibility = notesViewModel::toggleTrashVisibility
            )
        }
    ) { innerPadding ->
        VaultContent(
            uiState = accessState,
            notesState = notesState,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            onConfigurePin = accessViewModel::configurePin,
            onUnlockWithPin = accessViewModel::unlockWithPin,
            onRequestAndroidCredentialPrompt =
                accessViewModel::requestAndroidCredentialPrompt,
            onClearError = accessViewModel::clearError,
            onNoteClick = onNoteClick,
            onRequestNoteActions = { note -> activeActionsNote = note },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    VaultNoteActionsSheet(
        note = if (accessState.isUnlocked) activeActionsNote else null,
        clipboardCallbacks = clipboardCallbacks,
        onMoveToTrash = notesViewModel::moveToTrash,
        onDuplicate = notesViewModel::duplicate,
        onRemoveFromVault = notesViewModel::removeFromVault,
        onDismiss = { activeActionsNote = null }
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
    RadialMenuEffect(
        items = remember(
            isAvailable,
            onCreateVaultNote,
            onCreateVaultNoteFromTemplate,
            onSearchClick
        ) {
            if (!isAvailable) {
                emptyList()
            } else {
                listOf(
                    RadialMenuItem(
                        icon = Icons.Default.Add,
                        label = "",
                        contentDescription = "New Vault note",
                        action = onCreateVaultNote
                    ),
                    RadialMenuItem(
                        icon = Icons.Default.Description,
                        label = "",
                        contentDescription = "New Vault note from template",
                        action = onCreateVaultNoteFromTemplate
                    ),
                    RadialMenuItem(
                        icon = Icons.Default.Search,
                        label = "",
                        contentDescription = "Search Vault",
                        action = onSearchClick
                    )
                )
            }
        },
        fabContentDescription = "Open Vault creation menu"
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
    viewMode: NoteListViewMode,
    isTrashVisible: Boolean,
    searchFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit,
    onToggleTrashVisibility: () -> Unit
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
                contentDescription = "Go back",
                onClick = onBack
            )
        },
        actions = {
            if (isUnlocked) {
                if (isSearchActive) {
                    NexIconButton(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close search",
                        onClick = { onSearchToggle(false) }
                    )
                } else {
                    VaultSortButton(
                        sortOrder = sortOrder,
                        onToggleSortOrder = onToggleSortOrder
                    )
                    VaultViewModeButton(
                        viewMode = viewMode,
                        onToggleViewMode = onToggleViewMode
                    )
                    NexIconButton(
                        imageVector = Icons.Default.Search,
                        contentDescription = if (isTrashVisible) {
                            "Search Vault trash"
                        } else {
                            "Search Vault"
                        },
                        onClick = { onSearchToggle(true) }
                    )
                    VaultTrashButton(
                        isTrashVisible = isTrashVisible,
                        onToggleTrashVisibility = onToggleTrashVisibility
                    )
                }
                NexIconButton(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Vault",
                    onClick = onLock
                )
            }
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun VaultTrashButton(
    isTrashVisible: Boolean,
    onToggleTrashVisibility: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.Delete,
        contentDescription = if (isTrashVisible) {
            "Vault notes"
        } else {
            "Vault trash"
        },
        onClick = onToggleTrashVisibility,
        selected = isTrashVisible
    )
}

@Composable
private fun VaultViewModeButton(
    viewMode: NoteListViewMode,
    onToggleViewMode: () -> Unit
) {
    NexIconButton(
        imageVector = if (viewMode == NoteListViewMode.LIST) {
            Icons.Default.GridView
        } else {
            Icons.AutoMirrored.Filled.ViewList
        },
        contentDescription = if (viewMode == NoteListViewMode.LIST) {
            "Grid view"
        } else {
            "List view"
        },
        onClick = onToggleViewMode
    )
}

@Composable
private fun VaultSortButton(
    sortOrder: SortOrder,
    onToggleSortOrder: () -> Unit
) {
    NexIconButton(
        imageVector = Icons.Default.SwapVert,
        contentDescription = if (sortOrder == SortOrder.MODIFIED_DESC) {
            "Sort: newest first"
        } else {
            "Sort: oldest first"
        },
        onClick = onToggleSortOrder,
        selected = sortOrder == SortOrder.MODIFIED_ASC
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
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(100))
        ) {
            NexSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = if (isTrashVisible) {
                    "Search Vault trash"
                } else {
                    "Search Vault"
                },
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
                text = if (isTrashVisible) "Vault Trash" else "Vault",
                style = MaterialTheme.typography.headlineSmall
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
    onConfigurePin: (CharArray, CharArray) -> Unit,
    onUnlockWithPin: (CharArray) -> Unit,
    onRequestAndroidCredentialPrompt: () -> Unit,
    onClearError: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onRemoveTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    bottomContentPadding: Dp,
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
                onRequestNoteActions = onRequestNoteActions,
                onMoveToTrash = onMoveToTrash,
                onTogglePin = onTogglePin,
                onToggleTagFilter = onToggleTagFilter,
                onRemoveTagFilter = onRemoveTagFilter,
                onClearTagFilters = onClearTagFilters,
                onRestoreFromTrash = onRestoreFromTrash,
                onRequestDeletePermanentlyFromTrash =
                    onRequestDeletePermanentlyFromTrash,
                bottomContentPadding = bottomContentPadding,
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
    Box(
        modifier = modifier.padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
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
        title = "Set up Vault",
        pin = pin,
        confirmation = confirmation,
        showConfirmation = true,
        buttonText = "Create PIN",
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
private fun VaultUnlockForm(
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
        title = "Unlock Vault",
        pin = pin,
        confirmation = "",
        showConfirmation = false,
        buttonText = "Unlock",
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
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        VaultPinField(
            value = pin,
            label = "PIN",
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
                label = "Confirm PIN",
                enabled = !isBusy,
                imeAction = ImeAction.Done,
                onValueChange = onConfirmationChange,
                onDone = onSubmit
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (failedPinAttemptsText != null) {
            Text(
                text = failedPinAttemptsText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Icon(
            imageVector = Icons.Default.LockOpen,
            contentDescription = null
        )
        Spacer(Modifier.size(8.dp))
        Text("Use Android screen lock")
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
    scoredResults: List<ScoredNote> = emptyList(),
    viewMode: NoteListViewMode,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    isLoading: Boolean = false,
    isTrashVisible: Boolean,
    isSearchActive: Boolean = false,
    topTags: List<Tag> = emptyList(),
    selectedTagFilters: Set<String> = emptySet(),
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onToggleTagFilter: (String) -> Unit = {},
    onRemoveTagFilter: (String) -> Unit = {},
    onClearTagFilters: () -> Unit = {},
    onRestoreFromTrash: (Note) -> Unit,
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit,
    bottomContentPadding: Dp = NoteCollectionLayoutDefaults.defaultBottomPadding,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
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
            viewMode = viewMode,
            noteCardStyle = noteCardStyle,
            listState = listState,
            gridState = gridState,
            isLoading = isLoading,
            isTrashVisible = isTrashVisible,
            isSearchActive = isSearchActive,
            hasTagFilter = selectedTagFilters.isNotEmpty(),
            onNoteClick = onNoteClick,
            onRequestNoteActions = onRequestNoteActions,
            onMoveToTrash = onMoveToTrash,
            onTogglePin = onTogglePin,
            onRestoreFromTrash = onRestoreFromTrash,
            onRequestDeletePermanentlyFromTrash = onRequestDeletePermanentlyFromTrash,
            bottomContentPadding = bottomContentPadding,
            modifier = Modifier.weight(1f)
        )
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
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
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
    if (viewMode == NoteListViewMode.GRID) {
        VaultNotesGrid(
            notes = notes,
            scoredResults = scoredResults,
            isReadOnly = isTrashVisible,
            noteCardStyle = noteCardStyle,
            gridState = gridState,
            onNoteClick = onNoteClick,
            onRequestNoteActions = onRequestNoteActions,
            onMoveToTrash = onMoveToTrash,
            onTogglePin = onTogglePin,
            onRestoreFromTrash = onRestoreFromTrash,
            onRequestDeletePermanentlyFromTrash = onRequestDeletePermanentlyFromTrash,
            bottomContentPadding = bottomContentPadding,
            modifier = modifier
        )
    } else {
        VaultNotesList(
            notes = notes,
            scoredResults = scoredResults,
            isReadOnly = isTrashVisible,
            noteCardStyle = noteCardStyle,
            listState = listState,
            onNoteClick = onNoteClick,
            onRequestNoteActions = onRequestNoteActions,
            onMoveToTrash = onMoveToTrash,
            onTogglePin = onTogglePin,
            onRestoreFromTrash = onRestoreFromTrash,
            onRequestDeletePermanentlyFromTrash = onRequestDeletePermanentlyFromTrash,
            bottomContentPadding = bottomContentPadding,
            modifier = modifier
        )
    }
}

@Composable
private fun VaultNotesList(
    notes: List<Note>,
    scoredResults: List<ScoredNote>,
    isReadOnly: Boolean,
    noteCardStyle: NoteCardStyle,
    listState: LazyListState,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
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
        modifier = modifier.fillMaxSize(),
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
                        onRequestDeletePermanentlyFromTrash
                )
            } else {
                VaultActiveNoteCard(
                    scored = scored,
                    onNoteClick = onNoteClick,
                    onRequestNoteActions = onRequestNoteActions,
                    onMoveToTrash = onMoveToTrash,
                    onTogglePin = onTogglePin,
                    noteCardStyle = noteCardStyle,
                    modifier = Modifier
                        .testTag(VAULT_NOTE_ROW_TAG)
                        .animateItem()
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
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
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
        modifier = modifier.fillMaxSize(),
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
                        onRequestDeletePermanentlyFromTrash
                )
            } else {
                VaultActiveNoteCard(
                    scored = scored,
                    onNoteClick = onNoteClick,
                    onRequestNoteActions = onRequestNoteActions,
                    onMoveToTrash = onMoveToTrash,
                    onTogglePin = onTogglePin,
                    noteCardStyle = noteCardStyle,
                    modifier = Modifier
                        .testTag(VAULT_NOTE_ROW_TAG)
                        .animateItem()
                )
            }
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
    onRequestDeletePermanentlyFromTrash: (Note) -> Unit
) {
    TrashNoteCard(
        note = note,
        onRestore = { onRestoreFromTrash(note) },
        onDeletePermanently = { onRequestDeletePermanentlyFromTrash(note) }
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
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Vault note?") },
        text = {
            Text("Permanently delete this Vault note? This cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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

private fun vaultEmptyStateTitle(
    isTrashVisible: Boolean,
    isSearchActive: Boolean,
    hasTagFilter: Boolean
): String =
    when {
        isSearchActive -> "No results"
        hasTagFilter -> "No notes with these tags"
        isTrashVisible -> "Vault trash empty"
        else -> "No Vault notes"
    }

private fun vaultEmptyStateMessage(
    isTrashVisible: Boolean,
    isSearchActive: Boolean,
    hasTagFilter: Boolean
): String =
    when {
        isSearchActive -> "Try different words"
        hasTagFilter -> "Try removing some tag filters"
        isTrashVisible -> "No deleted Vault notes."
        else -> "Use the + button below to create your first Vault note"
    }

@Composable
private fun VaultActiveNoteCard(
    scored: ScoredNote,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    onMoveToTrash: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    noteCardStyle: NoteCardStyle,
    modifier: Modifier = Modifier
) {
    val note = scored.note
    NoteCard(
        note = note,
        onClick = { onNoteClick(note.id) },
        onTrash = { onMoveToTrash(note) },
        modifier = modifier,
        noteCardStyle = noteCardStyle,
        titleHighlightRanges = scored.titleRanges,
        contentHighlightRanges = scored.contentRanges,
        onPin = { onTogglePin(note) },
        onLongPress = { onRequestNoteActions(note) },
        showPinAction = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultNoteActionsSheet(
    note: Note?,
    clipboardCallbacks: NoteClipboardCallbacks,
    onMoveToTrash: (Note) -> Unit,
    onDuplicate: (Note) -> Unit,
    onRemoveFromVault: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    if (note == null) return

    var page by remember(note.id) { mutableStateOf(VaultNoteActionsPage.Actions) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            NoteActionsSheetHeader(
                title = if (page == VaultNoteActionsPage.Actions) {
                    "Vault note actions"
                } else {
                    "Copy Vault note"
                },
                noteLabel = "Selected Vault note"
            )
            HorizontalDivider()
            when (page) {
                VaultNoteActionsPage.Actions -> VaultNoteActionsMainPage(
                    onCopy = { page = VaultNoteActionsPage.Copy },
                    onMoveToTrash = {
                        onMoveToTrash(note)
                        onDismiss()
                    },
                    onDuplicate = {
                        onDuplicate(note)
                        onDismiss()
                    },
                    onRemoveFromVault = {
                        onRemoveFromVault(note)
                        onDismiss()
                    }
                )

                VaultNoteActionsPage.Copy -> VaultNoteActionsCopyPage(
                    onBack = { page = VaultNoteActionsPage.Actions },
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
    onCopy: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDuplicate: () -> Unit,
    onRemoveFromVault: () -> Unit
) {
    NoteActionsSheetRow(
        text = "Copy",
        icon = Icons.Outlined.ContentCopy,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_COPY_TAG),
        onClick = onCopy
    )
    NoteActionsSheetRow(
        text = "Duplicate",
        icon = Icons.Outlined.FileCopy,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_DUPLICATE_TAG),
        onClick = onDuplicate
    )
    NoteActionsSheetRow(
        text = "Remove from Vault",
        icon = Icons.Default.LockOpen,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_REMOVE_FROM_VAULT_TAG),
        onClick = onRemoveFromVault
    )
    NoteActionsSheetRow(
        text = "Move to trash",
        icon = Icons.Outlined.Delete,
        destructive = true,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_MOVE_TO_TRASH_TAG),
        onClick = onMoveToTrash
    )
}

@Composable
private fun VaultNoteActionsCopyPage(
    onBack: () -> Unit,
    onCopyPlainText: () -> Unit,
    onCopyMarkdown: () -> Unit
) {
    NoteActionsSheetRow(
        text = "Back",
        icon = Icons.AutoMirrored.Outlined.ArrowBack,
        onClick = onBack
    )
    NoteActionsSheetRow(
        text = "Copy as text",
        icon = Icons.AutoMirrored.Outlined.TextSnippet,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_COPY_TEXT_TAG),
        onClick = onCopyPlainText
    )
    NoteActionsSheetRow(
        text = "Copy as Markdown",
        icon = Icons.Outlined.Code,
        modifier = Modifier.testTag(VAULT_NOTE_ACTION_COPY_MARKDOWN_TAG),
        onClick = onCopyMarkdown
    )
}

private fun VaultAccessError.message(): String = when (this) {
    VaultAccessError.EMPTY_PIN -> "Enter a PIN."
    VaultAccessError.PIN_MISMATCH -> "PINs do not match."
    VaultAccessError.WRONG_PIN -> "Wrong PIN."
    VaultAccessError.VAULT_NOT_CONFIGURED -> "Vault is not configured."
    VaultAccessError.ANDROID_CREDENTIAL_UNAVAILABLE ->
        "Android screen lock unlock is not available."
    VaultAccessError.ANDROID_CREDENTIAL_CANCELED ->
        "Android screen lock was canceled."
    VaultAccessError.ANDROID_CREDENTIAL_RESET_REQUIRED ->
        "Android screen lock unlock is no longer available. " +
            "Unlock with your PIN to re-enable it."
    VaultAccessError.OPERATION_FAILED -> "Vault access failed."
}

private fun Int.toFailedPinAttemptsText(): String? =
    takeIf { it > 0 }?.let { attempts ->
        "Failed PIN attempts: $attempts"
    }
