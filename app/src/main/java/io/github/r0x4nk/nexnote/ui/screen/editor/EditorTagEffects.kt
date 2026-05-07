package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Tag
import kotlinx.coroutines.flow.first

@Composable
internal fun EditorTagEffects(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    selectedTagsInEditor: String?,
    tagsForCurrentNote: List<Tag>,
    state: EditorScreenState,
    density: Density
) {
    EditorTagSearchCollectorEffect(viewModel, state)
    EditorPendingTagScrollEffect(uiState, state, density)
    EditorSelectedTagClearEffect(selectedTagsInEditor, state)
    EditorTagsAppearVisibilityEffect(tagsForCurrentNote, state)
    EditorTagAutoHideEffect(uiState.showPreview, state)
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

/**
 * Scrolls to the tag occurrence highlighted in [EditorScreenState.pendingTagScroll].
 *
 * In **preview mode** the target source offset is resolved against the lazy
 * preview item and its measured height, keeping the highlighted occurrence near
 * the same viewport anchor used by edit mode.
 * In **edit mode** the pixel Y is computed from the [TextLayoutResult] and the
 * [ScrollState] is animated to the corresponding position.
 */
@Composable
private fun EditorPendingTagScrollEffect(
    uiState: EditorUiState,
    state: EditorScreenState,
    density: Density
) {
    LaunchedEffect(state.pendingTagScroll) {
        val event = state.pendingTagScroll ?: return@LaunchedEffect
        state.highlightRange = event.charOffset until (event.charOffset + 1 + event.tagName.length)

        state.isTagSearchScrolling[0] = true
        try {
            if (uiState.showPreview) {
                state.previewListState.scrollToSourceOffset(
                    sourceRanges = state.currentSourceRanges,
                    sourceOffset = event.charOffset,
                    viewportFraction = CONTENT_SCROLL_ANCHOR_FRACTION,
                    animated = true
                )
            } else {
                scrollEditToTagSearch(event, state, density)
            }
        } finally {
            state.isTagSearchScrolling[0] = false
        }
        state.pendingTagScroll = null
    }
}

private suspend fun scrollEditToTagSearch(
    event: TagSearchState,
    state: EditorScreenState,
    density: Density
) {
    val targetY = editModeTagSearchTargetY(event, state, density) ?: return
    val viewportHeight = state.contentViewportHeightPx.coerceAtLeast(1)
    val targetScroll = (targetY - viewportHeight * CONTENT_SCROLL_ANCHOR_FRACTION)
        .toInt()
        .coerceIn(0, state.contentScrollState.maxValue)
    state.contentScrollState.animateScrollTo(targetScroll)
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

/**
 * Hides/shows the tag bar based on scroll direction.
 *
 * In **edit mode** the pixel-based [TagBarScrollVisibilityController] is fed
 * from [contentScrollState].
 * In **preview mode** we observe [LazyListState] item/offset changes and derive
 * scroll direction directly — no pixel approximation needed, just monotonic
 * tracking of the (index, offset) pair.
 */
@Composable
private fun EditorTagAutoHideEffect(
    showPreview: Boolean,
    state: EditorScreenState
) {
    if (showPreview) {
        LaunchedEffect(state.previewListState) {
            previewTagAutoHide(state)
        }
    } else {
        LaunchedEffect(state.contentScrollState) {
            editTagAutoHide(state)
        }
    }
}

// ── Edit-mode tag auto-hide (pixel-based, unchanged logic) ──────────────────

private suspend fun editTagAutoHide(
    state: EditorScreenState
) {
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
        if (!state.canAutoAdjustTags()) {
            controller.syncTo(scroll.value, scroll.maxValue)
            return@collect
        }
        controller
            .onScrollChanged(scroll.value, scroll.maxValue, scroll.isInProgress)
            ?.let(state::applyTagBarVisibilityRequest)
    }
}

private data class TagBarScrollSnapshot(
    val value: Int,
    val maxValue: Int,
    val isInProgress: Boolean
)

// ── Preview-mode tag auto-hide (index/offset pair tracking) ─────────────────

/**
 * Watches [LazyListState] and toggles tag-bar visibility based on scroll
 * direction. Uses the (firstVisibleItemIndex, firstVisibleItemScrollOffset)
 * pair to determine direction without needing pixel-level total-scroll values.
 */
private suspend fun previewTagAutoHide(
    state: EditorScreenState
) {
    var prevIndex = state.previewListState.firstVisibleItemIndex
    var prevOffset = state.previewListState.firstVisibleItemScrollOffset
    var accumulatedDistance = 0
    var trackedDirection: TagBarVisibilityRequest? = null

    snapshotFlow {
        PreviewScrollSnapshot(
            index = state.previewListState.firstVisibleItemIndex,
            offset = state.previewListState.firstVisibleItemScrollOffset,
            isInProgress = state.previewListState.isScrollInProgress
        )
    }.collect { snapshot ->
        if (!state.canAutoAdjustTagsPreview() || !snapshot.isInProgress) {
            prevIndex = snapshot.index
            prevOffset = snapshot.offset
            accumulatedDistance = 0
            trackedDirection = null
            return@collect
        }

        val direction = resolvePreviewScrollDirection(
            prevIndex, prevOffset, snapshot.index, snapshot.offset
        )
        if (direction == null) {
            prevIndex = snapshot.index
            prevOffset = snapshot.offset
            return@collect
        }

        if (direction != trackedDirection) {
            trackedDirection = direction
            accumulatedDistance = 0
        }
        accumulatedDistance += estimatePixelDelta(
            prevIndex, prevOffset, snapshot.index, snapshot.offset
        )
        prevIndex = snapshot.index
        prevOffset = snapshot.offset

        val threshold = when (direction) {
            TagBarVisibilityRequest.Show -> TAG_SCROLL_REVEAL_THRESHOLD_PX
            TagBarVisibilityRequest.Hide -> TAG_SCROLL_VISIBILITY_THRESHOLD_PX
        }
        if (accumulatedDistance >= threshold) {
            accumulatedDistance = 0
            state.applyTagBarVisibilityRequest(direction)
        }
    }
}

private data class PreviewScrollSnapshot(
    val index: Int,
    val offset: Int,
    val isInProgress: Boolean
)

private fun resolvePreviewScrollDirection(
    prevIndex: Int,
    prevOffset: Int,
    curIndex: Int,
    curOffset: Int
): TagBarVisibilityRequest? = when {
    curIndex > prevIndex -> TagBarVisibilityRequest.Hide
    curIndex < prevIndex -> TagBarVisibilityRequest.Show
    curOffset > prevOffset -> TagBarVisibilityRequest.Hide
    curOffset < prevOffset -> TagBarVisibilityRequest.Show
    else -> null
}

/**
 * Rough pixel-distance estimate used only for the threshold check.
 * When the item index changes the jump is assumed to be at least one full
 * viewport-height, so the threshold is always exceeded on the next frame.
 */
private fun estimatePixelDelta(
    prevIndex: Int,
    prevOffset: Int,
    curIndex: Int,
    curOffset: Int
): Int {
    if (curIndex != prevIndex) return TAG_SCROLL_VISIBILITY_THRESHOLD_PX
    return kotlin.math.abs(curOffset - prevOffset)
}

// ── Shared helpers ──────────────────────────────────────────────────────────

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

private fun EditorScreenState.canAutoAdjustTags(): Boolean {
    return !tagsPinned &&
        !isTagSearchScrolling[0] &&
        !isNoteSearchScrolling[0] &&
        !isRestoringContentScroll[0] &&
        contentScrollState.maxValue > 0
}

/**
 * Preview-mode variant — uses [previewListState] item count instead of
 * [contentScrollState.maxValue] to decide whether the content is scrollable.
 */
private fun EditorScreenState.canAutoAdjustTagsPreview(): Boolean {
    return !tagsPinned &&
        !isTagSearchScrolling[0] &&
        !isNoteSearchScrolling[0] &&
        !isRestoringContentScroll[0] &&
        previewListState.layoutInfo.totalItemsCount > 0
}
