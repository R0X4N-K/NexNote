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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Tag
import java.io.File

internal data class EditorScreenScaffoldContent(
    val uiState: EditorUiState,
    val undoRedoState: EditorUndoRedoState,
    val noteId: Long,
    val tagsForCurrentNote: List<Tag>,
    val selectedTagsInEditor: String?,
    val noteBackground: Color,
    val isDarkTheme: Boolean,
    val imageFileProvider: (String) -> File,
    val noteLinkTargets: List<NoteLinkTarget>,
    val state: EditorScreenState
)

internal data class EditorScreenActions(
    val onBack: () -> Unit,
    val onExport: (() -> Unit)?,
    val onMarkdownToggle: () -> Unit,
    val onTogglePreview: () -> Unit,
    val onInsertImage: () -> Unit,
    val onInsertNoteLink: () -> Unit,
    val insertAtCursor: (String) -> Unit,
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
    val onSearchNext: () -> Unit
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
    EditorTopBar(
        isSaving = content.uiState.isSaving,
        isMarkdown = content.uiState.isMarkdown,
        title = content.uiState.title,
        isTemplateMode = content.uiState.isTemplateMode,
        containerColor = content.noteBackground,
        searchState = content.state.noteSearch,
        searchFocusRequester = content.state.searchFocusRequester,
        onBack = actions.onBack,
        onMarkdownToggle = actions.onMarkdownToggle,
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(content.noteBackground)
            .navigationBarsPadding()
            .imePadding()
    ) {
        EditorToolbar(
            showPreview = content.uiState.showPreview,
            isTemplateMode = content.uiState.isTemplateMode,
            isDarkTheme = content.isDarkTheme,
            hasCustomColor = content.uiState.backgroundColor != null,
            canUndo = content.undoRedoState.canUndo,
            canRedo = content.undoRedoState.canRedo,
            noteBackground = content.noteBackground,
            onTogglePreview = actions.onTogglePreview,
            onUndo = actions.onUndo,
            onRedo = actions.onRedo,
            onInsertImage = actions.onInsertImage,
            onInsertChecklist = { actions.insertAtCursor(MARKDOWN_CHECKLIST_SNIPPET) },
            onInsertWebLink = { actions.insertAtCursor(MARKDOWN_WEB_LINK_SNIPPET) },
            onInsertNoteLink = actions.onInsertNoteLink,
            onThemeToggle = actions.onThemeToggle,
            onToggleColorPicker = actions.onToggleColorPicker,
            modifier = Modifier.fillMaxWidth()
        )
        // Metadata is placed between toolbar and divider to reclaim bottom space
        EditorMetadataArea(content.uiState, actions.onCreationDateTap)
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
            content.noteLinkTargets,
            content.state,
            actions.onContentEdited,
            actions.onContentSelectionChange,
            actions.onNoteLinkAutocompleteSelected,
            actions.onPreviewNoteLinkClick
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
    if (!uiState.isTemplateMode) {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
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
                .padding(horizontal = 12.dp, vertical = 2.dp)
        )
    }
}

private fun editorExpandEnter(): EnterTransition {
    return expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
    ) + slideInVertically(
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        initialOffsetY = { -it / 3 }
    ) + fadeIn(animationSpec = tween(durationMillis = 140))
}

private fun editorExpandExit(): ExitTransition {
    return shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
    ) + slideOutVertically(
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        targetOffsetY = { -it / 3 }
    ) + fadeOut(animationSpec = tween(durationMillis = 110))
}
