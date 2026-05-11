package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun EditorNoteSearchEffects(
    showPreview: Boolean,
    state: EditorScreenState,
    density: Density
) {
    EditorNoteSearchFocusEffect(state)
    EditorNoteSearchRefreshEffect(state)
    EditorNoteSearchScrollEffect(showPreview, state, density)
}

@Composable
private fun EditorNoteSearchFocusEffect(state: EditorScreenState) {
    LaunchedEffect(state.noteSearch.isActive) {
        if (state.noteSearch.isActive) {
            runCatching { state.searchFocusRequester.requestFocus() }
        }
    }
}

@Composable
private fun EditorNoteSearchRefreshEffect(state: EditorScreenState) {
    val contentText = state.contentFieldValue.text
    val query = state.noteSearch.query
    val isActive = state.noteSearch.isActive

    LaunchedEffect(contentText, query, isActive) {
        if (isActive) {
            state.noteSearch = state.noteSearch.refresh(contentText)
        }
    }
}

/**
 * Scrolls to the currently active search match.
 *
 * In **preview mode** the match is mapped to a lazy preview item and an
 * intra-item offset, so repeated matches in the same block do not all jump to
 * the block top.
 * In **edit mode** the pixel Y is resolved from the [TextLayoutResult].
 */
@Composable
private fun EditorNoteSearchScrollEffect(
    showPreview: Boolean,
    state: EditorScreenState,
    density: Density
) {
    val currentMatch = state.noteSearch.currentMatch

    LaunchedEffect(showPreview, state.noteSearch.isActive, currentMatch) {
        if (!state.noteSearch.isActive || currentMatch == null) return@LaunchedEffect

        state.isNoteSearchScrolling[0] = true
        try {
            if (showPreview) {
                scrollPreviewToSearchMatch(currentMatch, state)
            } else {
                scrollEditToSearchMatch(currentMatch, state, density)
            }
        } finally {
            state.isNoteSearchScrolling[0] = false
        }
    }
}

private suspend fun scrollPreviewToSearchMatch(
    match: IntRange,
    state: EditorScreenState
) {
    state.previewListState.scrollToSourceOffset(
        sourceRanges = state.currentSourceRanges,
        sourceOffset = match.first,
        viewportFraction = CONTENT_SCROLL_ANCHOR_FRACTION,
        animated = true
    )
}

private suspend fun scrollEditToSearchMatch(
    match: IntRange,
    state: EditorScreenState,
    density: Density
) {
    state.selectNoteSearchRange(match)

    val targetY = editModeNoteSearchTargetY(match, state, density) ?: return
    val viewportHeight = state.unobscuredContentViewportHeightPx
    val targetScroll = (targetY - viewportHeight * CONTENT_SCROLL_ANCHOR_FRACTION)
        .toInt()
        .coerceIn(0, state.contentScrollState.maxValue)
    state.contentScrollState.animateScrollTo(targetScroll)
}

private fun EditorScreenState.selectNoteSearchRange(range: IntRange) {
    val contentLength = contentFieldValue.text.length
    val safeStart = range.first.coerceIn(0, contentLength)
    val safeEnd = (range.last + 1).coerceIn(safeStart, contentLength)
    setContentFieldValue(contentFieldValue.copy(selection = TextRange(safeStart, safeEnd)))
}

private suspend fun editModeNoteSearchTargetY(
    range: IntRange,
    state: EditorScreenState,
    density: Density
): Float? {
    val layout = withTimeoutOrNull(500) {
        snapshotFlow { state.textLayoutResult }
            .first { it != null && it.layoutInput.text.text == state.contentFieldValue.text }
    } ?: return null

    val safeOffset = range.first.coerceIn(0, layout.layoutInput.text.length)
    val contentTopPadPx = with(density) { 8.dp.roundToPx() }
    return contentTopPadPx + layout.getCursorRect(safeOffset).top
}
