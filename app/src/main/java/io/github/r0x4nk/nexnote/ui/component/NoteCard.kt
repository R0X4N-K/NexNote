package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.ui.common.EditorMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Card for a single note in list and grid views.
 *
 * Swipe left (EndToStart) → vertical collapse animation → [onTrash].
 * Tap → [onClick].
 * Pin button (top-right of card header) → [onPin] toggles the pinned state.
 * Long-press → [onLongPress].
 *
 * [noteCardStyle] controls how much information is shown:
 *   - TITLE_ONLY: title and date only (most compact).
 *   - TITLE_AND_PREVIEW: title, content preview, and date (default).
 *   - TITLE_DATE: title and date with the date shown more prominently.
 *
 * [titleHighlightRanges] and [contentHighlightRanges] highlight search matches.
 * Pinned notes receive a primaryContainer background tint as a subtle accent.
 */
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
    noteCardStyle: NoteCardStyle = NoteCardStyle.TITLE_AND_PREVIEW,
    titleHighlightRanges: List<IntRange> = emptyList(),
    contentHighlightRanges: List<IntRange> = emptyList(),
    onPin: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val collapsedState = remember { mutableStateOf(false) }
    val dismissState = rememberNoteCardDismissState()

    CollapseOnEndToStartSwipeEffect(
        dismissState = dismissState,
        collapsedState = collapsedState
    )

    TrashAfterCollapseEffect(
        collapsedState = collapsedState,
        dismissState = dismissState,
        onTrash = onTrash
    )

    DismissibleNoteCard(
        collapsed = collapsedState.value,
        dismissState = dismissState,
        modifier = modifier
    ) {
        NoteCardContent(
            note = note,
            onClick = { if (!collapsedState.value) onClick() },
            onPin = onPin,
            onLongPress = onLongPress,
            noteCardStyle = noteCardStyle,
            titleHighlightRanges = titleHighlightRanges,
            contentHighlightRanges = contentHighlightRanges
        )
    }
}

/**
 * Hosts a plain [SwipeToDismissBoxState]. The previous implementation gated the
 * dismissal through a `confirmValueChange` callback that always returned `false`
 * while opportunistically firing a side effect. That callback is deprecated in
 * Material3: the recommended pattern is to let the swipe settle on a valid
 * anchor and observe the resulting value externally — which we now do in
 * [CollapseOnEndToStartSwipeEffect].
 */
@Composable
private fun rememberNoteCardDismissState(): SwipeToDismissBoxState =
    rememberSwipeToDismissBoxState()

/**
 * Bridges the swipe gesture to the vertical-collapse animation. When the
 * [SwipeToDismissBoxState] settles on [SwipeToDismissBoxValue.EndToStart] we
 * flip [collapsedState] so the surrounding [AnimatedVisibility] can shrink the
 * card out, then [TrashAfterCollapseEffect] fires the trash callback once the
 * exit animation has had time to play.
 *
 * `distinctUntilChanged` guards against re-firing if the state recomposes for
 * other reasons while already at the EndToStart anchor.
 */
@Composable
private fun CollapseOnEndToStartSwipeEffect(
    dismissState: SwipeToDismissBoxState,
    collapsedState: MutableState<Boolean>
) {
    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.currentValue }
            .distinctUntilChanged()
            .collect { value ->
                if (value == SwipeToDismissBoxValue.EndToStart && !collapsedState.value) {
                    collapsedState.value = true
                }
            }
    }
}

@Composable
private fun TrashAfterCollapseEffect(
    collapsedState: MutableState<Boolean>,
    dismissState: SwipeToDismissBoxState,
    onTrash: () -> Unit
) {
    val currentOnTrash by rememberUpdatedState(onTrash)

    LaunchedEffect(collapsedState.value) {
        if (!collapsedState.value) return@LaunchedEffect

        delay(EditorMotion.NOTE_CARD_TRASH_DELAY_MS)
        currentOnTrash()

        // Reset before Room can remove this keyed item from composition; otherwise
        // a fast Undo can reuse the dismissed state and keep the restored card hidden.
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        collapsedState.value = false
    }
}

@Composable
private fun DismissibleNoteCard(
    collapsed: Boolean,
    dismissState: SwipeToDismissBoxState,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = !collapsed,
        exit = shrinkVertically(animationSpec = tween(durationMillis = EditorMotion.NOTE_CARD_EXIT_SHRINK_MS)) +
            fadeOut(animationSpec = tween(durationMillis = EditorMotion.NOTE_CARD_EXIT_FADE_MS)),
        modifier = modifier
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.clip(MaterialTheme.shapes.large),
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = { NoteCardSwipeBackground(dismissState) }
        ) {
            content()
        }
    }
}
