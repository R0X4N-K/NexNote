package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.common.EditorMotion
import io.github.r0x4nk.nexnote.util.MarkdownInlineToggle
import io.github.r0x4nk.nexnote.util.MarkdownLineToggle
import io.github.r0x4nk.nexnote.util.MarkdownTextEdit
import java.io.File

internal data class EditorScreenScaffoldContent(
    val uiState: EditorUiState,
    val undoRedoState: EditorUndoRedoState,
    val noteId: Long,
    val tagsForCurrentNote: List<Tag>,
    val selectedTagsInEditor: String?,
    val noteBackground: Color,
    val isDarkTheme: Boolean,
    val isKeyboardVisible: Boolean,
    val imageFileProvider: (String) -> File,
    val vaultImageByteProvider: (suspend (String) -> ByteArray?)?,
    val noteLinkTargets: List<NoteLinkTarget>,
    val state: EditorScreenState
)

internal data class EditorScreenActions(
    val onBack: () -> Unit,
    val onExport: (() -> Unit)?,
    val onTogglePreview: () -> Unit,
    val onInsertImage: () -> Unit,
    val onInsertNoteLink: () -> Unit,
    val insertAtCursor: (String) -> Unit,
    val applyMarkdownEdit: ((String, TextRange) -> MarkdownTextEdit) -> Unit,
    val onNoteLinkAutocompleteSelected: (NoteLinkAutocompleteMatch, NoteLinkTarget) -> Unit,
    val onPreviewNoteLinkClick: (Long) -> Unit,
    val onThemeToggle: () -> Unit,
    val onToggleColorPicker: () -> Unit,
    val onBackgroundColorChange: (Int?) -> Unit,
    val onTitleChange: (String) -> Unit,
    val onTagClick: (String) -> Unit,
    val onClearTagSelection: () -> Unit,
    val onContentEdited: () -> Unit,
    val onContentSelectionChange: (TextRange) -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onCreationDateTap: () -> Unit,
    val onSearchOpen: () -> Unit,
    val onSearchClose: () -> Unit,
    val onSearchQueryChange: (String) -> Unit,
    val onSearchPrevious: () -> Unit,
    val onSearchNext: () -> Unit,
    val onUnlockVault: () -> Unit
)

@Composable
internal fun EditorScreenScaffold(
    content: EditorScreenScaffoldContent,
    actions: EditorScreenActions
) {
    Scaffold(
        containerColor = content.noteBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            EditorSnackbarHost(content.state)
        },
        topBar = {
            EditorScreenTopBar(content, actions)
        }
    ) { innerPadding ->
        EditorScreenBody(
            content = content,
            actions = actions,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun EditorSnackbarHost(state: EditorScreenState) {
    SnackbarHost(hostState = state.snackbarHostState) { data ->
        Snackbar(snackbarData = data)
    }
}

@Composable
private fun EditorScreenTopBar(
    content: EditorScreenScaffoldContent,
    actions: EditorScreenActions
) {
    // Visual-styling controls live in the top bar so they stay reachable in
    // preview mode, while editing-history and Markdown tools stay near the IME.
    val toolingState = EditorTopBarToolingState(
        isDarkTheme = content.isDarkTheme,
        hasCustomColor = content.uiState.backgroundColor != null,
    )
    val toolingActions = EditorTopBarToolingActions(
        onThemeToggle = actions.onThemeToggle,
        onToggleColorPicker = actions.onToggleColorPicker,
    )
    EditorTopBar(
        isSaving = content.uiState.isSaving,
        title = if (content.uiState.isVaultLocked) "Vault locked" else content.uiState.title,
        isTemplateMode = content.uiState.isTemplateMode,
        isReadOnly = content.uiState.isReadOnly,
        containerColor = content.noteBackground,
        toolingState = toolingState,
        toolingActions = toolingActions,
        searchState = content.state.noteSearch,
        searchFocusRequester = content.state.searchFocusRequester,
        onBack = actions.onBack,
        onSearchOpen = actions.onSearchOpen,
        onSearchClose = actions.onSearchClose,
        onSearchQueryChange = actions.onSearchQueryChange,
        onSearchPrevious = actions.onSearchPrevious,
        onSearchNext = actions.onSearchNext,
        onExport = editorExportAction(content, actions)
    )
}

private fun editorExportAction(
    content: EditorScreenScaffoldContent,
    actions: EditorScreenActions
): (() -> Unit)? {
    return if (!content.uiState.isTemplateMode && content.noteId != EditorViewModel.NO_ID) {
        actions.onExport
    } else {
        null
    }
}

@Composable
private fun EditorScreenBody(
    content: EditorScreenScaffoldContent,
    actions: EditorScreenActions,
    modifier: Modifier = Modifier
) {
    // While the link-type or heading-level chooser is open, Material3's focusable
    // popup briefly hides the IME. We treat those flags as a soft "keyboard
    // intent" so the toolbar stays mounted and the dropdown can complete its
    // interaction without flicker. A non-collapsed content selection also keeps
    // the formatting toolbar available when the keyboard is closed.
    val toolbarVisible = shouldShowEditorKeyboardToolbar(
        isKeyboardVisible = content.isKeyboardVisible,
        keepOpenForToolbarMenu = content.state.showLinkTypeMenu || content.state.showHeadingMenu,
        contentSelection = content.state.contentFieldValue.selection,
        showPreview = content.uiState.showPreview,
        isReadOnly = content.uiState.isReadOnly,
        isNoteSearchActive = content.state.noteSearch.isActive,
        showNoteLinkPicker = content.state.showNoteLinkPicker,
        isLoading = content.uiState.isLoading
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(content.noteBackground)
    ) {
        if (content.uiState.isVaultLocked) {
            LockedVaultEditorBody(
                onUnlockVault = actions.onUnlockVault,
                modifier = Modifier.fillMaxSize()
            )
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Metadata is placed above mode tabs to keep the editor chrome compact.
            EditorMetadataArea(content.uiState, actions.onCreationDateTap)
            EditorModeTabsArea(content, actions)
            EditorColorPickerPanel(
                content.uiState,
                content.state,
                content.noteBackground,
                actions.onBackgroundColorChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            // Tags appear above the title for quicker contextual orientation
            if (!content.uiState.isTemplateMode) {
                EditorTagsPanel(
                    content.tagsForCurrentNote,
                    content.selectedTagsInEditor,
                    content.state,
                    actions.onTagClick,
                    actions.onClearTagSelection
                )
            }
            EditorTitleArea(content.uiState, content.state, actions.onTitleChange)
            EditorContentModeBox(
                content.uiState,
                content.imageFileProvider,
                content.vaultImageByteProvider,
                content.noteLinkTargets,
                content.state,
                toolbarVisible,
                actions.onTogglePreview,
                actions.onContentEdited,
                actions.onContentSelectionChange,
                actions.onNoteLinkAutocompleteSelected,
                actions.onPreviewNoteLinkClick
            )
        }
        EditorKeyboardToolbar(
            visible = toolbarVisible,
            isTemplateMode = content.uiState.isTemplateMode,
            canInsertImages = true,
            canUndo = content.undoRedoState.canUndo,
            canRedo = content.undoRedoState.canRedo,
            linkMenuExpanded = content.state.showLinkTypeMenu,
            onLinkMenuExpandedChange = { expanded -> content.state.showLinkTypeMenu = expanded },
            headingMenuExpanded = content.state.showHeadingMenu,
            onHeadingMenuExpandedChange = { expanded -> content.state.showHeadingMenu = expanded },
            onUndo = actions.onUndo,
            onRedo = actions.onRedo,
            onInsertImage = actions.onInsertImage,
            onInsertChecklist = { actions.applyMarkdownEdit(MarkdownLineToggle::taskList) },
            onSetHeadingLevel = { level ->
                content.state.showHeadingMenu = false
                actions.applyMarkdownEdit { text, range ->
                    MarkdownLineToggle.setHeading(text, range, level)
                }
            },
            onToggleBold = { actions.applyMarkdownEdit(MarkdownInlineToggle::bold) },
            onToggleItalic = { actions.applyMarkdownEdit(MarkdownInlineToggle::italic) },
            onToggleStrikethrough = { actions.applyMarkdownEdit(MarkdownInlineToggle::strikethrough) },
            onToggleInlineCode = { actions.applyMarkdownEdit(MarkdownInlineToggle::inlineCode) },
            onInsertCodeBlock = { actions.applyMarkdownEdit(MarkdownLineToggle::codeBlock) },
            onToggleQuote = { actions.applyMarkdownEdit(MarkdownLineToggle::quote) },
            onToggleUnorderedList = { actions.applyMarkdownEdit(MarkdownLineToggle::unorderedList) },
            onToggleOrderedList = { actions.applyMarkdownEdit(MarkdownLineToggle::orderedList) },
            onInsertHorizontalRule = { actions.applyMarkdownEdit(MarkdownLineToggle::horizontalRule) },
            onInsertWebLink = {
                // Close the chooser first so the toolbar can resume tracking the IME
                // intent purely from the user's editing focus.
                content.state.showLinkTypeMenu = false
                actions.applyMarkdownEdit(MarkdownInlineToggle::link)
            },
            onInsertNoteLink = {
                content.state.showLinkTypeMenu = false
                actions.onInsertNoteLink()
            },
            onHeightChanged = content.state::updateKeyboardToolbarHeight,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .fillMaxWidth()
        )
    }
}

internal fun shouldShowEditorKeyboardToolbar(
    isKeyboardVisible: Boolean,
    keepOpenForToolbarMenu: Boolean,
    contentSelection: TextRange,
    showPreview: Boolean,
    isReadOnly: Boolean,
    isNoteSearchActive: Boolean,
    showNoteLinkPicker: Boolean,
    isLoading: Boolean
): Boolean {
    val hasSelectedContent = !contentSelection.collapsed
    val toolbarRequested = isKeyboardVisible || keepOpenForToolbarMenu || hasSelectedContent

    return toolbarRequested &&
        !showPreview &&
        !isReadOnly &&
        !isNoteSearchActive &&
        !showNoteLinkPicker &&
        !isLoading
}

@Composable
private fun LockedVaultEditorBody(
    onUnlockVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Vault locked",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Unlock the Vault to view this note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onUnlockVault,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Unlock Vault")
            }
        }
    }
}

@Composable
private fun EditorModeTabsArea(
    content: EditorScreenScaffoldContent,
    actions: EditorScreenActions
) {
    AnimatedVisibility(
        visible = !content.state.noteSearch.isActive,
        enter = editorExpandEnter(),
        exit = editorExpandExit()
    ) {
        EditorModeTabs(
            showPreview = content.uiState.showPreview,
            enabled = !content.uiState.isLoading,
            onModeSelected = { targetPreview ->
                if (targetPreview != content.uiState.showPreview) {
                    actions.onTogglePreview()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditorColorPickerPanel(
    uiState: EditorUiState,
    state: EditorScreenState,
    noteBackground: Color,
    onBackgroundColorChange: (Int?) -> Unit
) {
    if (!uiState.isTemplateMode && !uiState.isReadOnly) {
        AnimatedVisibility(
            visible = state.showColorPicker,
            enter = editorExpandEnter(),
            exit = editorExpandExit()
        ) {
            NoteColorPicker(
                selected = uiState.backgroundColor,
                onSelect = { color ->
                    onBackgroundColorChange(color)
                    state.showColorPicker = false
                },
                noteBackground = noteBackground
            )
        }
    }
}

@Composable
private fun EditorTitleArea(
    uiState: EditorUiState,
    state: EditorScreenState,
    onTitleChange: (String) -> Unit
) {
    TitleField(
        value = uiState.title,
        onValueChange = onTitleChange,
        placeholder = if (uiState.isTemplateMode) "Template name" else "Title",
        onNext = { state.contentFocusRequester.requestFocus() },
        readOnly = uiState.showPreview || uiState.isReadOnly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .focusRequester(state.titleFocusRequester)
    )
}

@Composable
private fun EditorTagsPanel(
    tagsForCurrentNote: List<Tag>,
    selectedTagsInEditor: String?,
    state: EditorScreenState,
    onTagClick: (String) -> Unit,
    onClearTagSelection: () -> Unit
) {
    if (tagsForCurrentNote.isNotEmpty()) {
        AnimatedVisibility(
            visible = state.tagsVisible,
            enter = editorExpandEnter(),
            exit = editorExpandExit()
        ) {
            TagChipsEditorRow(
                tags = tagsForCurrentNote,
                selectedTag = selectedTagsInEditor,
                onTagClick = onTagClick,
                onClearSelection = onClearTagSelection,
                isPinned = state.tagsPinned,
                onTogglePin = {
                    state.tagsPinned = !state.tagsPinned
                    if (state.tagsPinned) state.tagsVisible = true
                }
            )
        }
    }
}

@Composable
private fun EditorMetadataArea(
    uiState: EditorUiState,
    onCreationDateTap: () -> Unit
) {
    if (!uiState.isTemplateMode) {
        MetadataBar(
            charCount = uiState.content.length,
            lastModifiedDate = uiState.lastModifiedDate,
            creationDate = uiState.creationDate,
            onCreationDateTap = onCreationDateTap,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
    }
}

private fun editorExpandEnter(): EnterTransition {
    return expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = tween(durationMillis = EditorMotion.PANEL_EXPAND_MS, easing = FastOutSlowInEasing)
    ) + slideInVertically(
        animationSpec = tween(durationMillis = EditorMotion.PANEL_EXPAND_MS, easing = FastOutSlowInEasing),
        initialOffsetY = { -it / 3 }
    ) + fadeIn(animationSpec = tween(durationMillis = EditorMotion.PANEL_EXPAND_FADE_MS))
}

private fun editorExpandExit(): ExitTransition {
    return shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = tween(durationMillis = EditorMotion.PANEL_COLLAPSE_MS, easing = FastOutSlowInEasing)
    ) + slideOutVertically(
        animationSpec = tween(durationMillis = EditorMotion.PANEL_COLLAPSE_MS, easing = FastOutSlowInEasing),
        targetOffsetY = { -it / 3 }
    ) + fadeOut(animationSpec = tween(durationMillis = EditorMotion.PANEL_COLLAPSE_FADE_MS))
}
