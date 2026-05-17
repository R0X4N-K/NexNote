package io.github.r0x4nk.nexnote.ui.screen.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    pendingMoveNoteId: Long = 0L,
    onBack: () -> Unit,
    onCreateVaultNote: () -> Unit,
    onNoteClick: (Long) -> Unit,
    accessViewModel: VaultAccessViewModel = viewModel(factory = VaultAccessViewModel.Factory),
    notesViewModel: VaultNotesViewModel = viewModel(factory = VaultNotesViewModel.Factory)
) {
    val accessState by accessViewModel.uiState.collectAsStateWithLifecycle()
    val notesState by notesViewModel.uiState.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    var activeActionsNote by remember { mutableStateOf<Note?>(null) }
    var moveNoteIdToConsume by rememberSaveable(pendingMoveNoteId) {
        mutableStateOf(pendingMoveNoteId)
    }

    VaultSearchFocusEffect(notesState.isSearchActive, searchFocusRequester)
    VaultActionMessagesEffect(notesViewModel, snackbarHostState)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VaultTopBar(
                isUnlocked = accessState.isUnlocked,
                searchQuery = notesState.searchQuery,
                isSearchActive = notesState.isSearchActive,
                sortOrder = notesState.sortOrder,
                viewMode = notesState.viewMode,
                searchFocusRequester = searchFocusRequester,
                onBack = onBack,
                onLock = accessViewModel::lock,
                onCreateVaultNote = onCreateVaultNote,
                onSearchQueryChange = notesViewModel::onSearchQueryChange,
                onSearchToggle = notesViewModel::onSearchToggle,
                onToggleSortOrder = notesViewModel::toggleSortOrder,
                onToggleViewMode = notesViewModel::toggleViewMode
            )
        }
    ) { innerPadding ->
        VaultContent(
            uiState = accessState,
            notesState = notesState,
            onConfigurePin = accessViewModel::configurePin,
            onUnlockWithPin = accessViewModel::unlockWithPin,
            onRequestAndroidCredentialPrompt =
                accessViewModel::requestAndroidCredentialPrompt,
            onClearError = accessViewModel::clearError,
            onNoteClick = onNoteClick,
            onRequestNoteActions = { note -> activeActionsNote = note },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    VaultNoteActionsSheet(
        note = if (accessState.isUnlocked) activeActionsNote else null,
        onRemoveFromVault = notesViewModel::removeFromVault,
        onDismiss = { activeActionsNote = null }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultTopBar(
    isUnlocked: Boolean,
    searchQuery: String,
    isSearchActive: Boolean,
    sortOrder: SortOrder,
    viewMode: NoteListViewMode,
    searchFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onCreateVaultNote: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    TopAppBar(
        title = {
            VaultTopBarTitle(
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
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
                    NexIconButton(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Vault note",
                        onClick = onCreateVaultNote
                    )
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
                        contentDescription = "Search Vault",
                        onClick = { onSearchToggle(true) }
                    )
                }
                NexIconButton(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Vault",
                    onClick = onLock
                )
            }
        },
        colors = nexTopAppBarColors()
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
                placeholder = "Search Vault",
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
                text = "Vault",
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
    onConfigurePin: (CharArray, CharArray) -> Unit,
    onUnlockWithPin: (CharArray) -> Unit,
    onRequestAndroidCredentialPrompt: () -> Unit,
    onClearError: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
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
            VaultNotesCollection(
                notes = if (notesState.isUnlocked) notesState.notes else emptyList(),
                viewMode = notesState.viewMode,
                isSearchResultEmpty = notesState.isSearchActive &&
                    notesState.searchQuery.isNotBlank() &&
                    notesState.totalNoteCount > 0,
                onNoteClick = onNoteClick,
                onRequestNoteActions = onRequestNoteActions,
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
private fun VaultNotesCollection(
    notes: List<Note>,
    viewMode: NoteListViewMode,
    isSearchResultEmpty: Boolean,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) {
        VaultNotesEmptyState(
            isSearchResultEmpty = isSearchResultEmpty,
            modifier = modifier
        )
        return
    }
    if (viewMode == NoteListViewMode.GRID) {
        VaultNotesGrid(
            notes = notes,
            onNoteClick = onNoteClick,
            onRequestNoteActions = onRequestNoteActions,
            modifier = modifier
        )
    } else {
        VaultNotesList(
            notes = notes,
            onNoteClick = onNoteClick,
            onRequestNoteActions = onRequestNoteActions,
            modifier = modifier
        )
    }
}

@Composable
private fun VaultNotesList(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = notes, key = { it.id }) { note ->
            VaultNoteRow(
                note = note,
                onNoteClick = onNoteClick,
                onRequestNoteActions = onRequestNoteActions
            )
        }
    }
}

@Composable
private fun VaultNotesGrid(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(items = notes, key = { it.id }) { note ->
            VaultNoteRow(
                note = note,
                onNoteClick = onNoteClick,
                onRequestNoteActions = onRequestNoteActions
            )
        }
    }
}

@Composable
private fun VaultNotesEmptyState(
    isSearchResultEmpty: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSearchResultEmpty) {
                    Icons.Default.Search
                } else {
                    Icons.Default.Lock
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (isSearchResultEmpty) "No results" else "Vault unlocked",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isSearchResultEmpty) {
                    "Try different words."
                } else {
                    "No notes in your Vault yet."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VaultNoteRow(
    note: Note,
    onNoteClick: (Long) -> Unit,
    onRequestNoteActions: (Note) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNoteClick(note.id) },
                onLongClick = { onRequestNoteActions(note) }
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val titleText = note.title.ifBlank { "Untitled" }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            val preview = note.content
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
                .take(120)
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultNoteActionsSheet(
    note: Note?,
    onRemoveFromVault: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    if (note == null) return

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
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(
                    text = "Vault note actions",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Selected Vault note",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Remove from Vault") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.combinedClickable(
                    onClick = {
                        onRemoveFromVault(note)
                        onDismiss()
                    }
                )
            )
        }
    }
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
