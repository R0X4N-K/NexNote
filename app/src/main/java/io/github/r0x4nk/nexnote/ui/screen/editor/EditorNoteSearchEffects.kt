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
            if (!showPreview) state.selectNoteSearchRange(currentMatch)

            val viewportHeight = state.contentViewportHeightPx.coerceAtLeast(1)
            val targetY = noteSearchTargetY(currentMatch, showPreview, state, density)
                ?: return@LaunchedEffect
            val targetScroll = (targetY - viewportHeight * CONTENT_SCROLL_ANCHOR_FRACTION)
                .toInt()
                .coerceIn(0, state.contentScrollState.maxValue)

            state.contentScrollState.animateScrollTo(targetScroll)
        } finally {
            state.isNoteSearchScrolling[0] = false
        }
    }
}

private fun EditorScreenState.selectNoteSearchRange(range: IntRange) {
    val contentLength = contentFieldValue.text.length
    val safeStart = range.first.coerceIn(0, contentLength)
    val safeEnd = (range.last + 1).coerceIn(safeStart, contentLength)
    setContentFieldValue(contentFieldValue.copy(selection = TextRange(safeStart, safeEnd)))
}

private suspend fun noteSearchTargetY(
    range: IntRange,
    showPreview: Boolean,
    state: EditorScreenState,
    density: Density
): Float? {
    return if (showPreview) {
        previewNoteSearchTargetY(range, state)
    } else {
        editModeNoteSearchTargetY(range, state, density)
    }
}

private suspend fun previewNoteSearchTargetY(
    range: IntRange,
    state: EditorScreenState
): Float? {
    val sourceOffset = range.first
    val layouts = withTimeoutOrNull(500) {
        snapshotFlow { state.previewSourceLayouts }.first { layouts ->
            layouts.any { it.containsSourceOffset(sourceOffset) }
        }
    }.orEmpty()

    return layouts.previewYForSourceOffset(sourceOffset)
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
