package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.MarkdownPreview
import java.io.File

private val EditorContentHorizontalPadding = 8.dp
private val EditorContentTopPadding = 8.dp
private val EditorContentDefaultBottomPadding = 12.dp
private val EditorContentToolbarBottomPadding = 0.dp

private enum class EditorContentTarget {
    Loading,
    Edit,
    Preview
}

@Composable
internal fun ColumnScope.EditorContentModeBox(
    uiState: EditorUiState,
    imageFileProvider: (String) -> File,
    noteLinkTargets: List<NoteLinkTarget>,
    state: EditorScreenState,
    keyboardToolbarVisible: Boolean,
    onTogglePreview: () -> Unit,
    onContentEdited: () -> Unit,
    onContentSelectionChange: (TextRange) -> Unit,
    onNoteLinkAutocompleteSelected: (NoteLinkAutocompleteMatch, NoteLinkTarget) -> Unit,
    onPreviewNoteLinkClick: (Long) -> Unit
) {
    val previewWarmupKey = uiState.directPreviewWarmupKey(MaterialTheme.colorScheme.primary)
    val contentTarget = editorContentTarget(uiState, state, previewWarmupKey)
    val swipeDebouncer = remember { mutableLongStateOf(0L) }
    val onDebouncedSwipeToggle = remember(onTogglePreview) {
        {
            val now = System.currentTimeMillis()
            if (now - swipeDebouncer.longValue >= 150L) {
                swipeDebouncer.longValue = now
                onTogglePreview()
            }
        }
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .onSizeChanged { size -> state.contentViewportHeightPx = size.height }
            .editorContentSwipe(
                enabled = contentTarget != EditorContentTarget.Loading &&
                    !state.noteSearch.isActive &&
                    !state.showColorPicker &&
                    !state.isNoteLinkAutocompleteVisible,
                showPreview = uiState.showPreview,
                onSwipeToPreview = onDebouncedSwipeToggle,
                onSwipeToEdit = onDebouncedSwipeToggle
            )
    ) {
        AnimatedContent(
            targetState = contentTarget,
            transitionSpec = {
                editorContentTransition(
                    contentAnimationsEnabled = state.contentAnimationsEnabled,
                    initialState = initialState,
                    targetState = targetState,
                    directPreviewOpening = uiState.openedDirectlyInPreview && uiState.showPreview
                )
            },
            modifier = Modifier.fillMaxSize(),
            label = "content_mode"
        ) { target ->
            when (target) {
                EditorContentTarget.Loading -> {
                    EditorPreviewLoadingPlaceholder(modifier = Modifier.fillMaxWidth())
                }
                EditorContentTarget.Preview -> {
                    EditorMarkdownPreview(uiState, imageFileProvider, state, onPreviewNoteLinkClick)
                }
                EditorContentTarget.Edit -> {
                    EditorContentField(
                        state = state,
                        keyboardToolbarVisible = keyboardToolbarVisible,
                        onContentEdited = onContentEdited,
                        onContentSelectionChange = onContentSelectionChange
                    )
                }
            }
        }
        EditorNoteLinkAutocompletePopup(
            textFieldState = state.contentTextFieldState,
            contentRevision = state.contentEditRevision,
            modelContentVersion = uiState.contentVersion,
            targets = noteLinkTargets,
            enabled = !uiState.showPreview &&
                !state.noteSearch.isActive &&
                !state.showNoteLinkPicker,
            onTargetSelected = onNoteLinkAutocompleteSelected,
            onVisibilityChange = { state.isNoteLinkAutocompleteVisible = it }
        )
        when (contentTarget) {
            EditorContentTarget.Preview -> {
                EditorPreviewReadingProgressBar(
                    lazyListState = state.previewListState,
                    sourceRanges = state.currentSourceRanges,
                    contentLength = uiState.content.length,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            EditorContentTarget.Edit -> {
                EditorEditReadingProgressBar(
                    scrollState = state.contentScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            EditorContentTarget.Loading -> Unit
        }
    }
}

private fun editorContentTarget(
    uiState: EditorUiState,
    state: EditorScreenState,
    previewWarmupKey: DirectPreviewWarmupKey?
): EditorContentTarget = when {
    uiState.isLoading ||
        state.isDirectPreviewWarmupPending(previewWarmupKey) ||
        uiState.shouldDeferInitialEditContentSync(state.syncedContentVersion) -> {
        EditorContentTarget.Loading
    }
    uiState.showPreview -> EditorContentTarget.Preview
    else -> EditorContentTarget.Edit
}

@Composable
private fun EditorMarkdownPreview(
    uiState: EditorUiState,
    imageFileProvider: (String) -> File,
    state: EditorScreenState,
    onPreviewNoteLinkClick: (Long) -> Unit
) {
    MarkdownPreview(
        markdown = uiState.content,
        lazyListState = state.previewListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = EditorContentHorizontalPadding,
                vertical = EditorContentTopPadding
            ),
        imageFileProvider = imageFileProvider,
        highlightRanges = state.visibleContentHighlightRanges(),
        activeHighlightRange = state.activeContentHighlightRange(),
        onNoteLinkClick = onPreviewNoteLinkClick
    )
}

@Composable
private fun EditorContentField(
    state: EditorScreenState,
    keyboardToolbarVisible: Boolean,
    onContentEdited: () -> Unit,
    onContentSelectionChange: (TextRange) -> Unit
) {
    ContentField(
        textFieldState = state.contentTextFieldState,
        scrollState = state.contentScrollState,
        onContentEdited = onContentEdited,
        onSelectionChange = onContentSelectionChange,
        onLayoutResult = { state.textLayoutResult = it },
        highlightRange = state.fallbackContentHighlightRange(),
        searchRanges = state.searchContentHighlightRanges(),
        activeSearchRange = state.activeSearchHighlightRange(),
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = EditorContentHorizontalPadding,
                top = EditorContentTopPadding,
                end = EditorContentHorizontalPadding,
                bottom = editorContentBottomPadding(keyboardToolbarVisible)
            )
            .focusRequester(state.contentFocusRequester)
    )
}

private fun editorContentBottomPadding(keyboardToolbarVisible: Boolean): Dp =
    if (keyboardToolbarVisible) {
        EditorContentToolbarBottomPadding
    } else {
        EditorContentDefaultBottomPadding
    }

private fun EditorScreenState.visibleContentHighlightRanges(): List<IntRange> {
    val searchRanges = searchContentHighlightRanges()
    return searchRanges.ifEmpty {
        fallbackContentHighlightRange()?.let(::listOf).orEmpty()
    }
}

private fun EditorScreenState.activeContentHighlightRange(): IntRange? =
    activeSearchHighlightRange() ?: fallbackContentHighlightRange()

private fun EditorScreenState.searchContentHighlightRanges(): List<IntRange> =
    if (noteSearch.isActive && noteSearch.hasQuery) noteSearch.matches else emptyList()

private fun EditorScreenState.activeSearchHighlightRange(): IntRange? =
    if (noteSearch.isActive && noteSearch.hasQuery) noteSearch.currentMatch else null

private fun EditorScreenState.fallbackContentHighlightRange(): IntRange? =
    if (noteSearch.isActive) null else highlightRange

private fun editorContentTransition(
    contentAnimationsEnabled: Boolean,
    initialState: EditorContentTarget,
    targetState: EditorContentTarget,
    directPreviewOpening: Boolean
): ContentTransform = when {
    directPreviewOpening && loadingTransition(initialState, targetState) -> {
        contentTransform(EnterTransition.None, ExitTransition.None)
    }
    loadingTransition(initialState, targetState) -> {
        contentTransform(fadeIn(tween(120)), fadeOut(tween(90)))
    }
    !contentAnimationsEnabled -> {
        contentTransform(EnterTransition.None, ExitTransition.None)
    }
    targetState == EditorContentTarget.Preview -> {
        contentTransform(
            enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(160))
        )
    }
    else -> {
        contentTransform(
            enter = slideInHorizontally(tween(220)) { -it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(160))
        )
    }
}

private fun contentTransform(
    enter: EnterTransition,
    exit: ExitTransition
): ContentTransform =
    ContentTransform(
        targetContentEnter = enter,
        initialContentExit = exit,
        sizeTransform = null
    )

private fun loadingTransition(
    initialState: EditorContentTarget,
    targetState: EditorContentTarget
): Boolean =
    initialState == EditorContentTarget.Loading || targetState == EditorContentTarget.Loading
