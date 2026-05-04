package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Tag
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * A horizontally scrollable row of [TagChip]s that auto-scrolls continuously
 * in a ticker/marquee style.
 *
 * Role: UI component layer — stateless except for internal scroll state.
 *
 * Auto-scroll behaviour:
 * - On first composition, measures whether the tag list overflows the available
 *   width. If it does NOT overflow, the list is displayed statically (no animation).
 * - When overflow is detected the item list is doubled (tags + tags) to create a
 *   seamless looping illusion: when the scroll position enters the second copy,
 *   an instant reset to the equivalent position in the first copy is performed.
 *   Because both halves are identical, the jump is invisible to the user.
 * - Auto-scroll runs at [SCROLL_SPEED_PX_PER_FRAME] pixels per frame (~60 fps)
 *   via [LazyListState.scroll], which yields to user gestures automatically.
 * - Touching the row sets [isUserTouching] = true and resets [lastTouchMs].
 *   Auto-scroll resumes [RESUME_DELAY_MS] milliseconds after the last touch.
 *
 * Accessibility: if the system reduces animation/motion, the row remains
 * scrollable manually but does not auto-scroll — pass [autoScrollEnabled] = false
 * from the caller after checking [LocalReduceMotion] if desired.
 *
 * @param tags              List of tags to display. No-op when empty.
 * @param onTagClick        Called when a tag chip is tapped, with the tag name.
 * @param selectedTags      Names of currently selected (highlighted) tags.
 * @param autoScrollEnabled Set to false to suppress auto-scroll (e.g., reduced motion).
 * @param modifier          Applied to the [LazyRow].
 */
@Composable
fun AutoScrollingTagRow(
    tags: List<Tag>,
    onTagClick: (tagName: String) -> Unit,
    modifier: Modifier = Modifier,
    selectedTags: Set<String> = emptySet(),
    autoScrollEnabled: Boolean = true
) {
    if (tags.isEmpty()) return

    val listState = rememberLazyListState()

    // Overflow detection: after first layout, check if the tag list exceeds the viewport.
    // Only enable auto-scroll (and item doubling) when content actually overflows.
    var shouldLoop by remember(tags) { mutableStateOf(false) }

    LaunchedEffect(tags) {
        // Reset scroll position first so that a previously out-of-bounds offset
        // (from a longer list that was displayed before tags changed) does not
        // cause canScrollForward to report false from the "end" of the new list,
        // which would permanently stop the auto-scroll loop.
        listState.scrollToItem(0)
        // Wait until at least one item has been laid out before checking overflow.
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.size }
            .first { it > 0 }
        shouldLoop = autoScrollEnabled && listState.canScrollForward
    }

    // Doubled list for seamless looping. When [shouldLoop] is false, only the
    // original tags are shown (identical to a plain non-scrolling row).
    val displayTags = remember(tags, shouldLoop) {
        if (shouldLoop) tags + tags else tags
    }

    // Touch state: when the user touches the row, auto-scroll pauses.
    var isUserTouching by remember { mutableStateOf(false) }
    var lastTouchMs    by remember { mutableLongStateOf(0L) }

    // Auto-scroll loop: runs at ~60 fps, pauses during and shortly after touch.
    LaunchedEffect(shouldLoop) {
        if (!shouldLoop) return@LaunchedEffect
        while (true) {
            delay(FRAME_DELAY_MS)
            val now = System.currentTimeMillis()
            if (!isUserTouching && (now - lastTouchMs) > RESUME_DELAY_MS) {
                listState.scroll { scrollBy(SCROLL_SPEED_PX_PER_FRAME) }

                // Seamless loop: when the first visible item is in the second copy
                // (index >= tags.size), instantly jump back to the equivalent item
                // in the first copy at the same pixel offset. Both copies are
                // identical so the jump is invisible.
                val firstIndex = listState.firstVisibleItemIndex
                if (firstIndex >= tags.size) {
                    listState.scrollToItem(
                        index       = firstIndex - tags.size,
                        scrollOffset = listState.firstVisibleItemScrollOffset
                    )
                }
            }
        }
    }

    LazyRow(
        state                  = listState,
        modifier               = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isUserTouching = true
                lastTouchMs    = System.currentTimeMillis()
                waitForUpOrCancellation()
                isUserTouching = false
                lastTouchMs    = System.currentTimeMillis()
            }
        },
        contentPadding         = PaddingValues(horizontal = 16.dp),
        horizontalArrangement  = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count        = displayTags.size,
            // Index-based keys ensure uniqueness when the list is doubled.
            key          = { index -> index },
            contentType  = { "tag_chip" }
        ) { index ->
            val tag = displayTags[index]
            TagChip(
                tagName    = tag.name,
                onClick    = { onTagClick(tag.name) },
                isSelected = tag.name in selectedTags
            )
        }
    }
}

private const val FRAME_DELAY_MS             = 16L    // ~60 fps
private const val SCROLL_SPEED_PX_PER_FRAME  = 1.2f  // ~72 px/s at 60 fps
private const val RESUME_DELAY_MS            = 3_000L // Resume 3 s after last touch
