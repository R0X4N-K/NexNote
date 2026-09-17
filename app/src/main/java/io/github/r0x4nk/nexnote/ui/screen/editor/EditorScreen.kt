package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.copyAsMarkdown
import io.github.r0x4nk.nexnote.ui.common.copyAsPlainText
import io.github.r0x4nk.nexnote.ui.component.OperationProgressDialog
import io.github.r0x4nk.nexnote.ui.component.buildMarkdownBlockSourceRanges
import io.github.r0x4nk.nexnote.ui.component.copyTextToClipboard
import io.github.r0x4nk.nexnote.ui.component.rememberNoteShareCallbacks
import io.github.r0x4nk.nexnote.ui.navigation.Screen
import io.github.r0x4nk.nexnote.ui.theme.NoteContentTheme
import io.github.r0x4nk.nexnote.ui.theme.rememberNoteColors
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    mode: EditorMode,
    navController: NavController,
    onExport: (() -> Unit)? = null,
    viewModel: EditorViewModel = viewModel(
        factory = EditorViewModel.factory(mode)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val operationProgress by viewModel.operationProgress.collectAsStateWithLifecycle()
    OperationProgressDialog(operationProgress)
    val undoRedoState by viewModel.undoRedoState.collectAsStateWithLifecycle()
    val tagsForCurrentNote by viewModel.tagsForCurrentNote.collectAsStateWithLifecycle()
    val selectedTagsInEditor by viewModel.selectedTagsInEditor.collectAsStateWithLifecycle()
    val noteLinkTargets by viewModel.noteLinkTargets.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val state = rememberEditorScreenState(mode)
    val showTrashConfirmation = remember { mutableStateOf(false) }
    val showClearContentConfirmation = remember { mutableStateOf(false) }
    val showRemoveAllTagsConfirmation = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val imageFileProvider = remember(viewModel) { viewModel::getImageFile }
    // Provider used by the Markdown preview to decode Vault images from their
    // decrypted bytes; only wired when the current note is a Vault note so the
    // normal-notes path keeps its on-disk fast path unchanged.
    val vaultImageByteProvider: (suspend (String) -> ByteArray?)? = remember(
        viewModel,
        uiState.isVaultNote
    ) {
        if (uiState.isVaultNote) {
            val provider: suspend (String) -> ByteArray? = { path ->
                viewModel.decryptVaultImageBytes(path)
            }
            provider
        } else {
            null
        }
    }
    val noteBackground = rememberNoteColors(uiState.backgroundColor).container

    LaunchedEffect(mode) {
        NexNoteDebugLog.editor(
            event = "routeEntered",
            details = "${mode.debugRouteSummary()} ${uiState.debugEditorSummary()}"
        )
    }

    val redactEditorContent = uiState.redactContentForLogs
    val togglePreviewPreservingScroll = rememberTogglePreviewPreservingScroll(
        state = state,
        showPreview = uiState.showPreview,
        content = uiState.content,
        contentVersion = uiState.contentVersion,
        redactContent = redactEditorContent,
        viewModel = viewModel
    )
    val insertAtCursor = rememberInsertAtCursor(state, redactEditorContent, viewModel)
    val applyMarkdownEdit = rememberApplyMarkdownEdit(state, redactEditorContent, viewModel)
    val replaceNoteLinkAutocomplete = rememberReplaceNoteLinkAutocomplete(
        state,
        redactEditorContent,
        viewModel
    )
    val launchAttachmentPickerAtCursor = rememberLaunchAttachmentPicker(context, state, viewModel)
    EditorAttachmentResumeEffect(viewModel)
    val openNoteLinkPicker: () -> Unit = {
        state.openNoteLinkPickerDetachedFromEditor(focusManager)
    }
    val toggleColorPicker: () -> Unit = {
        state.highlightRange = null
        state.pendingTagScroll = null
        state.showNoteLinkPicker = false
        state.showColorPicker = !state.showColorPicker
    }
    val openCreationDatePicker: () -> Unit = {
        if (!uiState.isTemplateMode && !uiState.isReadOnly) {
            state.highlightRange = null
            state.pendingTagScroll = null
            state.showColorPicker = false
            state.showNoteLinkPicker = false
            state.showDatePicker = true
        }
    }
    val currentNoteTextSnapshot: () -> Note = {
        val body = if (uiState.showPreview) {
            uiState.content
        } else {
            state.currentContentTextFieldValue().text
        }

        Note(
            title = uiState.title,
            content = body,
            isMarkdown = true
        )
    }
    val shareCallbacks = rememberNoteShareCallbacks(state.snackbarHostState)
    val copiedAsTextMessage = stringResource(R.string.editor_copied_as_text)
    val copiedAsMarkdownMessage = stringResource(R.string.editor_copied_as_markdown)
    val clipLabel = stringResource(R.string.note_clip_label)
    val shareCurrentNote: () -> Unit = { shareCallbacks.onShareNote(currentNoteTextSnapshot()) }
    val copyCurrentNoteAsText: () -> Unit = {
        val text = currentNoteTextSnapshot().copyAsPlainText()
        scope.launch {
            copyTextToClipboard(
                clipboard = clipboard,
                snackbarHostState = state.snackbarHostState,
                text = text,
                snackbarMessage = copiedAsTextMessage,
                clipLabel = clipLabel
            )
        }
    }
    val copyCurrentNoteAsMarkdown: () -> Unit = {
        val text = currentNoteTextSnapshot().copyAsMarkdown()
        scope.launch {
            copyTextToClipboard(
                clipboard = clipboard,
                snackbarHostState = state.snackbarHostState,
                text = text,
                snackbarMessage = copiedAsMarkdownMessage,
                clipLabel = clipLabel
            )
        }
    }
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val commitActiveEditContent: () -> Unit = {
        if (!uiState.showPreview) {
            NexNoteDebugLog.editor(
                event = "commitActiveEditContent",
                details = "field=${state.currentContentTextFieldValue().debugTextFieldValueSummary(redactEditorContent)} " +
                    "remembered=${state.contentFieldValue.debugTextFieldValueSummary(redactEditorContent)} " +
                    "model=${uiState.debugEditorSummary()}"
            )
            state.commitContentTextFieldValue(
                modelContent = uiState.content,
                modelContentVersion = uiState.contentVersion,
                onContentChange = viewModel::onContentChange,
                redactContent = redactEditorContent
            )
        } else {
            NexNoteDebugLog.editor(
                event = "commitActiveEditContentSkippedPreview",
                details = uiState.debugEditorSummary()
            )
        }
    }
    if (showTrashConfirmation.value) {
        AlertDialog(
            tonalElevation = 1.dp,
            onDismissRequest = { showTrashConfirmation.value = false },
            title = { Text(stringResource(R.string.editor_trash_dialog_title)) },
            text = { Text(stringResource(R.string.editor_trash_dialog_message)) },
            confirmButton = {
                io.github.r0x4nk.nexnote.ui.component.NexDestructiveButton(onClick = {
                    showTrashConfirmation.value = false
                    commitActiveEditContent()
                    viewModel.trashCurrentNote { navController.popBackStack() }
                }) { Text(stringResource(R.string.common_move_to_trash)) }
            },
            dismissButton = {
                TextButton(onClick = { showTrashConfirmation.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showClearContentConfirmation.value) {
        AlertDialog(
            tonalElevation = 1.dp,
            onDismissRequest = { showClearContentConfirmation.value = false },
            title = { Text(stringResource(R.string.editor_clear_content_dialog_title)) },
            text = { Text(stringResource(R.string.editor_clear_content_dialog_message)) },
            confirmButton = {
                io.github.r0x4nk.nexnote.ui.component.NexDestructiveButton(onClick = {
                    showClearContentConfirmation.value = false
                    commitActiveEditContent()
                    viewModel.clearContent()
                }) { Text(stringResource(R.string.common_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearContentConfirmation.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showRemoveAllTagsConfirmation.value) {
        AlertDialog(
            tonalElevation = 1.dp,
            onDismissRequest = { showRemoveAllTagsConfirmation.value = false },
            title = { Text(stringResource(R.string.editor_remove_all_tags_dialog_title)) },
            text = { Text(stringResource(R.string.editor_remove_all_tags_dialog_message)) },
            confirmButton = {
                io.github.r0x4nk.nexnote.ui.component.NexDestructiveButton(onClick = {
                    showRemoveAllTagsConfirmation.value = false
                    commitActiveEditContent()
                    viewModel.clearAllTags()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAllTagsConfirmation.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    val launchAttachmentPickerAfterCommit: () -> Unit = {
        commitActiveEditContent()
        launchAttachmentPickerAtCursor()
    }
    val navigateToTagOccurrence: (String) -> Unit = { tagName ->
        commitActiveEditContent()
        if (!uiState.showPreview) {
            focusManager.clearFocus(force = true)
        }
        state.showColorPicker = false
        viewModel.onTagChipClick(tagName)
    }
    val openSearch: () -> Unit = {
        commitActiveEditContent()
        state.highlightRange = null
        state.pendingTagScroll = null
        state.showColorPicker = false
        state.noteSearch = state.noteSearch.open(state.contentFieldValue.text)
    }
    val openVaultAccess: () -> Unit = {
        state.noteSearch = state.noteSearch.close()
        if (!navController.popBackStack(Screen.Vault.route, inclusive = false)) {
            navController.navigate(Screen.Vault.route()) {
                launchSingleTop = true
            }
        }
    }
    val closeSearch: () -> Unit = {
        state.noteSearch = state.noteSearch.close()
        if (!uiState.showPreview) {
            runCatching { state.contentFocusRequester.requestFocus() }
        }
    }
    val updateSearchQuery: (String) -> Unit = { query ->
        state.noteSearch = state.noteSearch.updateQuery(query, state.contentFieldValue.text)
    }
    val searchPrevious: () -> Unit = {
        state.noteSearch = state.noteSearch.previous()
    }
    val searchNext: () -> Unit = {
        state.noteSearch = state.noteSearch.next()
    }

    val shouldMaintainPreviewSourceRanges =
        uiState.showPreview || state.pendingContentScrollAnchor != null
    val currentSourceRanges = remember(uiState.content, shouldMaintainPreviewSourceRanges) {
        if (shouldMaintainPreviewSourceRanges) {
            buildMarkdownBlockSourceRanges(uiState.content)
        } else {
            emptyList()
        }
    }
    SideEffect {
        if (shouldMaintainPreviewSourceRanges || state.currentSourceRanges.isNotEmpty()) {
            state.currentSourceRanges = currentSourceRanges
        }
    }

    EditorContentAnimationsReadyEffect(uiState, state)
    EditorContentSyncEffect(uiState, state)
    EditorPendingContentCommitEffect(uiState, state, viewModel)
    NoteContentTheme(rememberNoteColors(uiState.backgroundColor)) {
        EditorDirectPreviewWarmupEffect(uiState, state)
        EditorBackgroundPreParseEffect(uiState)
    }
    EditorPreviewScrollRestorationEffect(uiState, state, density)
    EditorKeyboardTagBarEffect(isKeyboardVisible, state)
    EditorRadialMenuBindings(
        showPreview = uiState.showPreview,
        isTemplateMode = uiState.isTemplateMode,
        isReadOnly = uiState.isReadOnly,
        state = state,
        onToggleColorPicker = toggleColorPicker,
        onCreationDateEdit = openCreationDatePicker,
        onSearchOpen = openSearch,
        scope = scope
    )
    EditorErrorSnackbarEffect(uiState, state, viewModel)
    EditorInitialFocusEffect(mode, state)
    EditorTagEffects(
        viewModel = viewModel,
        uiState = uiState,
        selectedTagsInEditor = selectedTagsInEditor,
        tagsForCurrentNote = tagsForCurrentNote,
        state = state,
        density = density
    )
    EditorNoteSearchEffects(
        showPreview = uiState.showPreview,
        state = state,
        density = density
    )

    val alreadyOnNoteMessage = stringResource(R.string.editor_link_self)
    val linkUnavailableMessage = stringResource(R.string.editor_link_unavailable)
    val insertNoteLinkAtCursor: (NoteLinkTarget) -> Unit = { target ->
        insertAtCursor(noteLinkMarkdownFor(target))
    }
    val openNoteFromPreviewLink: (Long) -> Unit = { targetNoteId ->
        when {
            targetNoteId == uiState.noteId -> {
                scope.launch { state.snackbarHostState.showSnackbar(alreadyOnNoteMessage) }
            }
            noteLinkTargets.none { it.id == targetNoteId } -> {
                scope.launch { state.snackbarHostState.showSnackbar(linkUnavailableMessage) }
            }
            else -> {
                scope.launch {
                    viewModel.flushPendingChanges()
                    navController.navigate(
                        previewNoteLinkEditorRoute(
                            isVaultNote = uiState.isVaultNote,
                            targetNoteId = targetNoteId
                        )
                    )
                }
            }
        }
    }

    val handleBack: () -> Unit = {
        NexNoteDebugLog.editor(
            event = "handleBack",
            details = "searchActive=${state.noteSearch.isActive} ${uiState.debugEditorSummary()}"
        )
        if (state.noteSearch.isActive) {
            closeSearch()
        } else {
            commitActiveEditContent()
            scope.launch {
                viewModel.flushPendingChanges()
                viewModel.pruneUnreferencedStoredFiles()
                viewModel.clearContentHistory()
                navController.popBackStack()
            }
        }
    }
    BackHandler(onBack = handleBack)

    EditorScreenScaffold(
        content = EditorScreenScaffoldContent(
            uiState = uiState,
            undoRedoState = undoRedoState,
            noteId = mode.routeNoteId,
            tagsForCurrentNote = tagsForCurrentNote,
            selectedTagsInEditor = selectedTagsInEditor,
            noteBackground = noteBackground,
            isKeyboardVisible = isKeyboardVisible,
            imageFileProvider = imageFileProvider,
            vaultImageByteProvider = vaultImageByteProvider,
            noteLinkTargets = noteLinkTargets,
            state = state
        ),
        actions = EditorScreenActions(
            onBack = handleBack,
            onExport = {
                commitActiveEditContent()
                scope.launch {
                    if (viewModel.flushPendingChanges() && viewModel.uiState.value.noteId != EditorViewModel.NO_ID) {
                        onExport?.invoke() ?: navController.navigate(Screen.Export.route(viewModel.uiState.value.noteId))
                    }
                }
            },
            onShare = shareCurrentNote,
            onClearContent = { showClearContentConfirmation.value = true },
            onTrash = { showTrashConfirmation.value = true },
            onMoveToVault = {
                commitActiveEditContent()
                scope.launch {
                    if (viewModel.flushPendingChanges() && viewModel.uiState.value.noteId != EditorViewModel.NO_ID) {
                        navController.navigate(Screen.Vault.moveNoteRoute(viewModel.uiState.value.noteId)) {
                            popUpTo(Screen.Editor.route) { inclusive = true }
                        }
                    }
                }
            },
            onCopyNoteAsText = copyCurrentNoteAsText,
            onCopyNoteAsMarkdown = copyCurrentNoteAsMarkdown,
            onTogglePreview = togglePreviewPreservingScroll,
            onInsertAttachment = launchAttachmentPickerAfterCommit,
            onInsertNoteLink = openNoteLinkPicker,
            insertAtCursor = insertAtCursor,
            applyMarkdownEdit = applyMarkdownEdit,
            onNoteLinkAutocompleteSelected = replaceNoteLinkAutocomplete,
            onPreviewNoteLinkClick = openNoteFromPreviewLink,
            onPreviewTaskListItemClick = viewModel::togglePreviewTaskListItem,
            onCheckAllTasks = {
                commitActiveEditContent()
                viewModel.setAllTaskListItems(checked = true)
            },
            onUncheckAllTasks = {
                commitActiveEditContent()
                viewModel.setAllTaskListItems(checked = false)
            },
            onClearAllTags = { showRemoveAllTagsConfirmation.value = true },
            onToggleColorPicker = toggleColorPicker,
            onBackgroundColorChange = viewModel::onBackgroundColorChange,
            onTitleChange = viewModel::onTitleChange,
            onTagClick = navigateToTagOccurrence,
            onClearTagSelection = viewModel::clearTagSelectionInEditor,
            onContentEdited = {
                state.highlightRange = null
                state.pendingTagScroll = null
                state.markContentEdited()
            },
            onContentSelectionChange = { selection ->
                val textLength = state.contentTextFieldState.text.length
                val safeSelection = selection.coerceInText(textLength)
                if (
                    state.contentFieldValue.text.length == textLength &&
                    state.contentFieldValue.selection != safeSelection
                ) {
                    state.contentFieldValue = state.contentFieldValue.copy(selection = safeSelection)
                }
                viewModel.onContentSelectionChange(safeSelection.end)
            },
            onUndo = {
                state.highlightRange = null
                state.pendingTagScroll = null
                commitActiveEditContent()
                viewModel.undoContentChange()
            },
            onRedo = {
                state.highlightRange = null
                state.pendingTagScroll = null
                commitActiveEditContent()
                viewModel.redoContentChange()
            },
            onCreationDateTap = {
                openCreationDatePicker()
            },
            onSearchOpen = openSearch,
            onSearchClose = closeSearch,
            onSearchQueryChange = updateSearchQuery,
            onSearchPrevious = searchPrevious,
            onSearchNext = searchNext,
            onUnlockVault = openVaultAccess
        )
    )

    if (state.showNoteLinkPicker) {
        NoteLinkPickerDialog(
            targets = noteLinkTargets,
            onDismiss = { state.showNoteLinkPicker = false },
            onTargetSelected = { target ->
                insertNoteLinkAtCursor(target)
                state.showNoteLinkPicker = false
            }
        )
    }

    EditorCreationDateDialog(uiState, state, viewModel)
}

private fun EditorScreenState.openNoteLinkPickerDetachedFromEditor(focusManager: FocusManager) {
    highlightRange = null
    pendingTagScroll = null
    showColorPicker = false
    isNoteLinkAutocompleteVisible = false

    // The picker is a modal flow: release editor focus first so the IME cannot
    // reopen the content field and dismiss the dialog during the toolbar exit.
    focusManager.clearFocus(force = true)
    showNoteLinkPicker = true
}

internal fun previewNoteLinkEditorRoute(
    isVaultNote: Boolean,
    targetNoteId: Long
): String =
    if (isVaultNote) {
        Screen.Editor.vaultNoteRoute(targetNoteId)
    } else {
        Screen.Editor.existingNoteRoute(targetNoteId)
    }

private fun EditorUiState.debugEditorSummary(): String {
    return "noteId=$noteId templateId=$templateId templateMode=$isTemplateMode " +
        "loading=$isLoading dirty=$isDirty saving=$isSaving " +
        "preview=$showPreview vault=$isVaultNote readOnly=$isReadOnly " +
        "contentVersion=$contentVersion selection=$contentSelectionOffset " +
        "${NexNoteDebugLog.textSummary("title", title, redact = redactContentForLogs)} " +
        NexNoteDebugLog.textSummary("content", content, redact = redactContentForLogs)
}

private fun androidx.compose.ui.text.input.TextFieldValue.debugTextFieldValueSummary(
    redact: Boolean
): String {
    return "selection=${selection.start}-${selection.end} " +
        NexNoteDebugLog.textSummary("text", text, redact = redact)
}

private fun TextRange.coerceInText(textLength: Int): TextRange {
    return TextRange(
        start = start.coerceIn(0, textLength),
        end = end.coerceIn(0, textLength)
    )
}