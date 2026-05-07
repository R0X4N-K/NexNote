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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import kotlinx.coroutines.delay

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
    val dismissState = rememberNoteCardDismissState(collapsedState)

    TrashAfterCollapseEffect(
        collapsed = collapsedState.value,
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

@Composable
private fun rememberNoteCardDismissState(
    collapsedState: MutableState<Boolean>
): SwipeToDismissBoxState =
    rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !collapsedState.value) {
                collapsedState.value = true
            }
            false
        }
    )

@Composable
private fun TrashAfterCollapseEffect(
    collapsed: Boolean,
    onTrash: () -> Unit
) {
    val currentOnTrash by rememberUpdatedState(onTrash)

    LaunchedEffect(collapsed) {
        if (collapsed) {
            delay(280)
            currentOnTrash()
        }
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
        exit = shrinkVertically(animationSpec = tween(durationMillis = 250)) +
            fadeOut(animationSpec = tween(durationMillis = 200)),
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
