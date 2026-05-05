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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.MarkdownPreview
import java.io.File

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
    onContentValueChange: (TextFieldValue) -> Unit,
    onNoteLinkAutocompleteSelected: (NoteLinkAutocompleteMatch, NoteLinkTarget) -> Unit,
    onPreviewNoteLinkClick: (Long) -> Unit
) {
    val previewWarmupKey = uiState.directPreviewWarmupKey(MaterialTheme.colorScheme.primary)
    val contentTarget = editorContentTarget(uiState, state, previewWarmupKey)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .onSizeChanged { size -> state.contentViewportHeightPx = size.height }
            .then(contentModeScrollModifier(contentTarget, state))
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
            modifier = contentModeAnimatedContentModifier(contentTarget),
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
                    EditorContentField(state, onContentValueChange)
                }
            }
        }
        EditorNoteLinkAutocompletePopup(
            contentValue = state.contentFieldValue,
            targets = noteLinkTargets,
            enabled = !uiState.showPreview && !state.noteSearch.isActive,
            onTargetSelected = onNoteLinkAutocompleteSelected
        )
    }
}

private fun contentModeScrollModifier(
    contentTarget: EditorContentTarget,
    state: EditorScreenState
): Modifier {
    return if (contentTarget == EditorContentTarget.Edit) {
        Modifier
    } else {
        Modifier.verticalScroll(state.contentScrollState)
    }
}

private fun contentModeAnimatedContentModifier(
    contentTarget: EditorContentTarget
): Modifier {
    return if (contentTarget == EditorContentTarget.Edit) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxWidth()
    }
}

private fun editorContentTarget(
    uiState: EditorUiState,
    state: EditorScreenState,
    previewWarmupKey: DirectPreviewWarmupKey?
): EditorContentTarget = when {
    uiState.isLoading || state.isDirectPreviewWarmupPending(previewWarmupKey) -> {
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        imageFileProvider = imageFileProvider,
        highlightRanges = state.visibleContentHighlightRanges(),
        activeHighlightRange = state.activeContentHighlightRange(),
        onNoteLinkClick = onPreviewNoteLinkClick,
        onSourceLayoutsChange = { layouts -> state.previewSourceLayouts = layouts }
    )
}

@Composable
private fun EditorContentField(
    state: EditorScreenState,
    onContentValueChange: (TextFieldValue) -> Unit
) {
    ContentField(
        value = state.contentFieldValue,
        textFieldState = state.contentTextFieldState,
        scrollState = state.contentScrollState,
        onValueChange = onContentValueChange,
        onLayoutResult = { state.textLayoutResult = it },
        highlightRange = state.fallbackContentHighlightRange(),
        searchRanges = state.searchContentHighlightRanges(),
        activeSearchRange = state.activeSearchHighlightRange(),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp)
            .focusRequester(state.contentFocusRequester)
    )
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
