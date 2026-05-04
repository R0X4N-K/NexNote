package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun EditorTagEffects(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    selectedTagsInEditor: String?,
    tagsForCurrentNote: List<Tag>,
    isKeyboardVisible: Boolean,
    state: EditorScreenState,
    density: Density
) {
    EditorTagSearchCollectorEffect(viewModel, state)
    EditorPendingTagScrollEffect(uiState, state, density)
    EditorSelectedTagClearEffect(selectedTagsInEditor, state)
    EditorTagsAppearVisibilityEffect(tagsForCurrentNote, state)
    EditorTagAutoHideEffect(isKeyboardVisible, state)
}

@Composable
private fun EditorTagSearchCollectorEffect(
    viewModel: EditorViewModel,
    state: EditorScreenState
) {
    LaunchedEffect(viewModel, state) {
        viewModel.tagSearchEvent.collect { event ->
            state.pendingTagScroll = event
        }
    }
}

@Composable
private fun EditorPendingTagScrollEffect(
    uiState: EditorUiState,
    state: EditorScreenState,
    density: Density
) {
    LaunchedEffect(state.pendingTagScroll) {
        val event = state.pendingTagScroll ?: return@LaunchedEffect
        state.highlightRange = event.charOffset until (event.charOffset + 1 + event.tagName.length)

        val viewportHeight = state.contentViewportHeightPx.coerceAtLeast(1)
        val targetY = tagSearchTargetY(event, uiState.showPreview, state, density)
        val targetScroll = ((targetY ?: return@LaunchedEffect) -
            viewportHeight * CONTENT_SCROLL_ANCHOR_FRACTION)
            .toInt()
            .coerceIn(0, state.contentScrollState.maxValue)

        state.isTagSearchScrolling[0] = true
        try {
            state.contentScrollState.animateScrollTo(targetScroll)
        } finally {
            state.isTagSearchScrolling[0] = false
        }
        state.pendingTagScroll = null
    }
}

private suspend fun tagSearchTargetY(
    event: TagSearchState,
    showPreview: Boolean,
    state: EditorScreenState,
    density: Density
): Float? {
    return if (showPreview) {
        val layouts = withTimeoutOrNull(500) {
            snapshotFlow { state.previewSourceLayouts }.first { layouts ->
                layouts.any { it.containsSourceOffset(event.charOffset) }
            }
        }.orEmpty()
        layouts.previewYForSourceOffset(event.charOffset)
    } else {
        editModeTagSearchTargetY(event, state, density)
    }
}

private suspend fun editModeTagSearchTargetY(
    event: TagSearchState,
    state: EditorScreenState,
    density: Density
): Float? {
    val layout = snapshotFlow { state.textLayoutResult }
        .first { it != null && it.layoutInput.text.text == state.contentFieldValue.text }
        ?: return null
    val safeOffset = event.charOffset.coerceIn(0, layout.layoutInput.text.length)
    val contentTopPadPx = with(density) { 8.dp.roundToPx() }
    return contentTopPadPx + layout.getCursorRect(safeOffset).top
}

@Composable
private fun EditorSelectedTagClearEffect(
    selectedTagsInEditor: String?,
    state: EditorScreenState
) {
    LaunchedEffect(selectedTagsInEditor) {
        if (selectedTagsInEditor == null) state.highlightRange = null
    }
}

@Composable
private fun EditorTagsAppearVisibilityEffect(
    tagsForCurrentNote: List<Tag>,
    state: EditorScreenState
) {
    LaunchedEffect(tagsForCurrentNote.isEmpty()) {
        if (tagsForCurrentNote.isNotEmpty()) state.tagsVisible = true
    }
}

@Composable
private fun EditorTagAutoHideEffect(
    isKeyboardVisible: Boolean,
    state: EditorScreenState
) {
    val isKeyboardVisibleRef = rememberUpdatedState(isKeyboardVisible)
    LaunchedEffect(state.contentScrollState) {
        val controller = TagBarScrollVisibilityController(
            initialScroll = state.contentScrollState.value,
            initialMaxScroll = state.contentScrollState.maxValue
        )
        snapshotFlow {
            TagBarScrollSnapshot(
                value = state.contentScrollState.value,
                maxValue = state.contentScrollState.maxValue,
                isInProgress = state.contentScrollState.isScrollInProgress
            )
        }.collect { scroll ->
            if (!state.canAutoAdjustTags(isKeyboardVisibleRef.value)) {
                controller.syncTo(scroll.value, scroll.maxValue)
                return@collect
            }
            controller
                .onScrollChanged(scroll.value, scroll.maxValue, scroll.isInProgress)
                ?.let(state::applyTagBarVisibilityRequest)
        }
    }
}

private data class TagBarScrollSnapshot(
    val value: Int,
    val maxValue: Int,
    val isInProgress: Boolean
)

private fun EditorScreenState.applyTagBarVisibilityRequest(
    request: TagBarVisibilityRequest
) {
    val targetVisibility = when (request) {
        TagBarVisibilityRequest.Show -> true
        TagBarVisibilityRequest.Hide -> false
    }
    if (tagsVisible != targetVisibility) {
        tagsVisible = targetVisibility
    }
}

private fun EditorScreenState.canAutoAdjustTags(isKeyboardVisible: Boolean): Boolean {
    return !isKeyboardVisible &&
        !tagsPinned &&
        !isTagSearchScrolling[0] &&
        !isNoteSearchScrolling[0] &&
        !isRestoringContentScroll[0] &&
        contentScrollState.maxValue > 0
}
