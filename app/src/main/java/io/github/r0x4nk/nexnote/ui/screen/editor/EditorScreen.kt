package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.r0x4nk.nexnote.domain.model.ThemeMode
import io.github.r0x4nk.nexnote.ui.component.buildMarkdownBlockSourceRanges
import io.github.r0x4nk.nexnote.ui.navigation.Screen
import io.github.r0x4nk.nexnote.ui.theme.adaptNoteColor
import io.github.r0x4nk.nexnote.util.NexNoteDebugLog
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    noteId: Long,
    templateId: Long,
    editTemplateId: Long,
    navController: NavController,
    onExport: (() -> Unit)? = null,
    viewModel: EditorViewModel = viewModel(
        factory = EditorViewModel.factory(noteId, templateId, editTemplateId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val undoRedoState by viewModel.undoRedoState.collectAsStateWithLifecycle()
    val tagsForCurrentNote by viewModel.tagsForCurrentNote.collectAsStateWithLifecycle()
    val selectedTagsInEditor by viewModel.selectedTagsInEditor.collectAsStateWithLifecycle()
    val noteLinkTargets by viewModel.noteLinkTargets.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val state = rememberEditorScreenState(noteId, templateId, editTemplateId)
    val context = LocalContext.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val imageFileProvider = remember(viewModel) { viewModel::getImageFile }
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK, ThemeMode.TRUE_DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val noteBackground = uiState.backgroundColor?.let { adaptNoteColor(it, isDarkTheme) }
        ?: androidx.compose.material3.MaterialTheme.colorScheme.surface

    LaunchedEffect(noteId, templateId, editTemplateId) {
        NexNoteDebugLog.editor(
            event = "routeEntered",
            details = "routeNoteId=$noteId routeTemplateId=$templateId " +
                "routeEditTemplateId=$editTemplateId ${uiState.debugEditorSummary()}"
        )
    }

    val togglePreviewPreservingScroll = rememberTogglePreviewPreservingScroll(
        state = state,
        showPreview = uiState.showPreview,
        content = uiState.content,
        contentVersion = uiState.contentVersion,
        viewModel = viewModel
    )
    val insertAtCursor = rememberInsertAtCursor(state, viewModel)
    val replaceNoteLinkAutocomplete = rememberReplaceNoteLinkAutocomplete(state, viewModel)
    val launchImagePickerAtCursor = rememberLaunchImagePickerAtCursor(context, state, viewModel)
    val openNoteLinkPicker: () -> Unit = {
        state.openNoteLinkPickerDetachedFromEditor(focusManager)
    }
    val toggleColorPicker: () -> Unit = {
        state.highlightRange = null
        state.pendingTagScroll = null
        state.showNoteLinkPicker = false
        state.showColorPicker = !state.showColorPicker
    }
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val commitActiveEditContent: () -> Unit = {
        if (!uiState.showPreview) {
            NexNoteDebugLog.editor(
                event = "commitActiveEditContent",
                details = "field=${state.currentContentTextFieldValue().debugTextFieldValueSummary()} " +
                    "remembered=${state.contentFieldValue.debugTextFieldValueSummary()} " +
                    "model=${uiState.debugEditorSummary()}"
            )
            state.commitContentTextFieldValue(
                modelContent = uiState.content,
                modelContentVersion = uiState.contentVersion,
                onContentChange = viewModel::onContentChange
            )
        } else {
            NexNoteDebugLog.editor(
                event = "commitActiveEditContentSkippedPreview",
                details = uiState.debugEditorSummary()
            )
        }
    }
    val launchImagePickerAfterCommit: () -> Unit = {
        commitActiveEditContent()
        launchImagePickerAtCursor()
    }
    val navigateToTagOccurrence: (String) -> Unit = { tagName ->
        commitActiveEditContent()
        if (!uiState.showPreview) {
            focusManager.clearFocus(force = true)
        }
        state.showColorPicker = false
        viewModel.onTagChipClick(tagName)
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
    EditorDirectPreviewWarmupEffect(uiState, state)
    EditorBackgroundPreParseEffect(uiState)
    EditorPreviewScrollRestorationEffect(uiState, state, density)
    EditorKeyboardTagBarEffect(isKeyboardVisible, state)
    EditorRadialMenuBindings(
        isKeyboardVisible = isKeyboardVisible,
        showPreview = uiState.showPreview,
        isTemplateMode = uiState.isTemplateMode,
        state = state,
        launchImagePickerAtCursor = launchImagePickerAfterCommit,
        onInsertNoteLink = openNoteLinkPicker,
        onToggleColorPicker = toggleColorPicker,
        insertAtCursor = insertAtCursor,
        scope = scope
    )
    EditorErrorSnackbarEffect(uiState, state, viewModel)
    EditorInitialFocusEffect(noteId, editTemplateId, state)
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

    val openSearch: () -> Unit = {
        commitActiveEditContent()
        state.highlightRange = null
        state.pendingTagScroll = null
        state.showColorPicker = false
        state.noteSearch = state.noteSearch.open(state.contentFieldValue.text)
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
    val insertNoteLinkAtCursor: (NoteLinkTarget) -> Unit = { target ->
        insertAtCursor(noteLinkMarkdownFor(target))
    }
    val openNoteFromPreviewLink: (Long) -> Unit = { targetNoteId ->
        when {
            targetNoteId == uiState.noteId -> {
                scope.launch { state.snackbarHostState.showSnackbar("Already on this note") }
            }
            noteLinkTargets.none { it.id == targetNoteId } -> {
                scope.launch { state.snackbarHostState.showSnackbar("Linked note is not available") }
            }
            else -> {
                scope.launch {
                    viewModel.flushPendingChanges()
                    navController.navigate(Screen.Editor.route(noteId = targetNoteId))
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
            noteId = noteId,
            tagsForCurrentNote = tagsForCurrentNote,
            selectedTagsInEditor = selectedTagsInEditor,
            noteBackground = noteBackground,
            isDarkTheme = isDarkTheme,
            isKeyboardVisible = isKeyboardVisible,
            imageFileProvider = imageFileProvider,
            noteLinkTargets = noteLinkTargets,
            state = state
        ),
        actions = EditorScreenActions(
            onBack = handleBack,
            onExport = onExport?.let { export ->
                {
                    commitActiveEditContent()
                    export()
                }
            },
            onTogglePreview = togglePreviewPreservingScroll,
            onInsertImage = launchImagePickerAfterCommit,
            onInsertNoteLink = openNoteLinkPicker,
            insertAtCursor = insertAtCursor,
            onNoteLinkAutocompleteSelected = replaceNoteLinkAutocomplete,
            onPreviewNoteLinkClick = openNoteFromPreviewLink,
            onThemeToggle = { viewModel.toggleTheme(isDarkTheme) },
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
            onCreationDateTap = { state.showDatePicker = true },
            onSearchOpen = openSearch,
            onSearchClose = closeSearch,
            onSearchQueryChange = updateSearchQuery,
            onSearchPrevious = searchPrevious,
            onSearchNext = searchNext
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

private fun EditorUiState.debugEditorSummary(): String {
    return "noteId=$noteId templateId=$templateId templateMode=$isTemplateMode " +
        "loading=$isLoading dirty=$isDirty saving=$isSaving " +
        "preview=$showPreview contentVersion=$contentVersion selection=$contentSelectionOffset " +
        "${NexNoteDebugLog.textSummary("title", title)} " +
        NexNoteDebugLog.textSummary("content", content)
}

private fun androidx.compose.ui.text.input.TextFieldValue.debugTextFieldValueSummary(): String {
    return "selection=${selection.start}-${selection.end} " +
        NexNoteDebugLog.textSummary("text", text)
}

private fun TextRange.coerceInText(textLength: Int): TextRange {
    return TextRange(
        start = start.coerceIn(0, textLength),
        end = end.coerceIn(0, textLength)
    )
}
