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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.r0x4nk.nexnote.ui.common.EditorMotion
import io.github.r0x4nk.nexnote.ui.component.MarkdownPreview
import java.io.File

private val EditorContentHorizontalPadding = 8.dp
private val EditorContentTopPadding = 8.dp
private val EditorContentDefaultBottomPadding = 8.dp
private val EditorBottomFadeHeight = 52.dp
private val EditorContentFadeBottomPadding = 44.dp
private const val EditorContentFadeSpacerLines = 2
private const val EditorBottomFadeZIndex = 10f

internal const val EDITOR_BOTTOM_FADE_TAG = "editor_bottom_fade"
internal const val EDITOR_CONTENT_FIELD_TAG = "editor_content_field"

private enum class EditorContentTarget {
    Loading,
    Edit,
    Preview
}

@Composable
internal fun ColumnScope.EditorContentModeBox(
    uiState: EditorUiState,
    noteBackground: Color,
    imageFileProvider: (String) -> File,
    vaultImageByteProvider: (suspend (String) -> ByteArray?)?,
    noteLinkTargets: List<NoteLinkTarget>,
    state: EditorScreenState,
    keyboardToolbarVisible: Boolean,
    onTogglePreview: () -> Unit,
    onContentEdited: () -> Unit,
    onContentSelectionChange: (TextRange) -> Unit,
    onNoteLinkAutocompleteSelected: (NoteLinkAutocompleteMatch, NoteLinkTarget) -> Unit,
    onPreviewNoteLinkClick: (Long) -> Unit
) {
    val density = LocalDensity.current
    val previewWarmupKey = uiState.directPreviewWarmupKey(MaterialTheme.colorScheme.primary)
    val contentTarget = editorContentTarget(uiState, state, previewWarmupKey)
    val bottomFadeVisible = editorBottomFadeVisible(uiState) &&
        contentTarget != EditorContentTarget.Loading
    val fadeBottomPadding = editorBottomFadePadding(bottomFadeVisible)
    val keyboardToolbarHeight = with(density) {
        state.keyboardToolbarHeightPx.toDp()
    }
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

    LaunchedEffect(bottomFadeVisible) {
        if (!bottomFadeVisible) {
            state.updateBottomFadeHeight(0)
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
                    EditorMarkdownPreview(
                        uiState = uiState,
                        fadeBottomPadding = fadeBottomPadding,
                        imageFileProvider = imageFileProvider,
                        vaultImageByteProvider = vaultImageByteProvider,
                        state = state,
                        onPreviewNoteLinkClick = onPreviewNoteLinkClick
                    )
                }
                EditorContentTarget.Edit -> {
                    EditorContentField(
                        state = state,
                        readOnly = uiState.isReadOnly,
                        keyboardToolbarVisible = keyboardToolbarVisible,
                        bottomFadeVisible = bottomFadeVisible,
                        onContentEdited = onContentEdited,
                        onContentSelectionChange = onContentSelectionChange
                    )
                }
            }
        }
        if (bottomFadeVisible) {
            EditorBottomFade(
                noteBackground = noteBackground,
                keyboardToolbarVisible = keyboardToolbarVisible,
                keyboardToolbarHeight = keyboardToolbarHeight,
                onHeightChanged = state::updateBottomFadeHeight
            )
        }
        EditorNoteLinkAutocompletePopup(
            textFieldState = state.contentTextFieldState,
            contentRevision = state.contentEditRevision,
            modelContentVersion = uiState.contentVersion,
            targets = noteLinkTargets,
            enabled = !uiState.showPreview &&
                !uiState.isReadOnly &&
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
    fadeBottomPadding: Dp,
    imageFileProvider: (String) -> File,
    vaultImageByteProvider: (suspend (String) -> ByteArray?)?,
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
        vaultImageByteProvider = vaultImageByteProvider,
        highlightRanges = state.visibleContentHighlightRanges(),
        activeHighlightRange = state.activeContentHighlightRange(),
        contentBottomPadding = fadeBottomPadding,
        onNoteLinkClick = onPreviewNoteLinkClick
    )
}

@Composable
private fun EditorContentField(
    state: EditorScreenState,
    readOnly: Boolean,
    keyboardToolbarVisible: Boolean,
    bottomFadeVisible: Boolean,
    onContentEdited: () -> Unit,
    onContentSelectionChange: (TextRange) -> Unit
) {
    val density = LocalDensity.current
    val toolbarBottomPadding = with(density) {
        state.keyboardToolbarHeightPx.toDp()
    }

    ContentField(
        textFieldState = state.contentTextFieldState,
        scrollState = state.contentScrollState,
        readOnly = readOnly,
        onContentEdited = onContentEdited,
        onSelectionChange = onContentSelectionChange,
        onLayoutResult = { state.textLayoutResult = it },
        highlightRange = state.fallbackContentHighlightRange(),
        searchRanges = state.searchContentHighlightRanges(),
        activeSearchRange = state.activeSearchHighlightRange(),
        trailingSpacerLines = if (bottomFadeVisible) EditorContentFadeSpacerLines else 0,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = EditorContentHorizontalPadding,
                top = EditorContentTopPadding,
                end = EditorContentHorizontalPadding,
                bottom = editorContentBottomPadding(
                    keyboardToolbarVisible = keyboardToolbarVisible,
                    keyboardToolbarHeight = toolbarBottomPadding
                )
            )
            .testTag(EDITOR_CONTENT_FIELD_TAG)
            .focusRequester(state.contentFocusRequester)
    )
}

private fun editorContentBottomPadding(
    keyboardToolbarVisible: Boolean,
    keyboardToolbarHeight: Dp
): Dp {
    val toolbarPadding = editorKeyboardToolbarBottomPadding(
        keyboardToolbarVisible = keyboardToolbarVisible,
        keyboardToolbarHeight = keyboardToolbarHeight
    )

    return if (toolbarPadding > 0.dp) {
        toolbarPadding
    } else {
        EditorContentDefaultBottomPadding
    }
}

private fun editorKeyboardToolbarBottomPadding(
    keyboardToolbarVisible: Boolean,
    keyboardToolbarHeight: Dp
): Dp {
    if (!keyboardToolbarVisible) return 0.dp

    return if (keyboardToolbarHeight > 0.dp) {
        keyboardToolbarHeight
    } else {
        EditorKeyboardToolbarMinHeight
    }
}

internal fun editorBottomFadeVisible(uiState: EditorUiState): Boolean =
    !uiState.isLoading && !uiState.isVaultLocked

private fun editorBottomFadePadding(visible: Boolean): Dp =
    if (visible) {
        EditorContentFadeBottomPadding
    } else {
        0.dp
    }

@Composable
private fun BoxScope.EditorBottomFade(
    noteBackground: Color,
    keyboardToolbarVisible: Boolean,
    keyboardToolbarHeight: Dp,
    onHeightChanged: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .zIndex(EditorBottomFadeZIndex)
            .fillMaxWidth()
            .padding(
                bottom = editorKeyboardToolbarBottomPadding(
                    keyboardToolbarVisible = keyboardToolbarVisible,
                    keyboardToolbarHeight = keyboardToolbarHeight
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EditorBottomFadeHeight)
                .testTag(EDITOR_BOTTOM_FADE_TAG)
                .background(
                    editorBottomFadeBrush(
                        noteBackground = noteBackground,
                        surfaceTint = MaterialTheme.colorScheme.surfaceTint
                    )
                )
                .onSizeChanged { size -> onHeightChanged(size.height) }
        )
    }
}

private fun editorBottomFadeBrush(
    noteBackground: Color,
    surfaceTint: Color
): Brush {
    val settledBackground = lerp(noteBackground, surfaceTint, 0.04f)
    return Brush.verticalGradient(
        colors = listOf(
            noteBackground.copy(alpha = 0f),
            noteBackground.copy(alpha = 0.86f),
            settledBackground
        )
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
        contentTransform(
            fadeIn(tween(durationMillis = EditorMotion.CONTENT_LOADING_FADE_IN_MS)),
            fadeOut(tween(durationMillis = EditorMotion.CONTENT_LOADING_FADE_OUT_MS))
        )
    }
    !contentAnimationsEnabled -> {
        contentTransform(EnterTransition.None, ExitTransition.None)
    }
    targetState == EditorContentTarget.Preview -> {
        contentTransform(
            enter = slideInHorizontally(tween(durationMillis = EditorMotion.CONTENT_MODE_SLIDE_MS)) { it } +
                fadeIn(tween(durationMillis = EditorMotion.CONTENT_MODE_FADE_IN_MS)),
            exit = slideOutHorizontally(tween(durationMillis = EditorMotion.CONTENT_MODE_SLIDE_MS)) { -it } +
                fadeOut(tween(durationMillis = EditorMotion.CONTENT_MODE_FADE_OUT_MS))
        )
    }
    else -> {
        contentTransform(
            enter = slideInHorizontally(tween(durationMillis = EditorMotion.CONTENT_MODE_SLIDE_MS)) { -it } +
                fadeIn(tween(durationMillis = EditorMotion.CONTENT_MODE_FADE_IN_MS)),
            exit = slideOutHorizontally(tween(durationMillis = EditorMotion.CONTENT_MODE_SLIDE_MS)) { it } +
                fadeOut(tween(durationMillis = EditorMotion.CONTENT_MODE_FADE_OUT_MS))
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
