package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
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
import io.github.r0x4nk.nexnote.ui.common.EditorMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Shared end-to-start swipe host for collection cards.
 *
 * Notes commit immediately after the collapse animation, while templates can
 * opt out of collapse and use the gesture to request a confirmation dialog.
 */
@Composable
internal fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    collapseBeforeDelete: Boolean = true,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val collapsedState = remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState()

    SwipeDeleteEffect(
        dismissState = dismissState,
        collapsedState = collapsedState,
        collapseBeforeDelete = collapseBeforeDelete,
        onDelete = onDelete
    )

    AnimatedVisibility(
        visible = !collapsedState.value,
        exit = shrinkVertically(animationSpec = tween(durationMillis = EditorMotion.NOTE_CARD_EXIT_SHRINK_MS)) +
            fadeOut(animationSpec = tween(durationMillis = EditorMotion.NOTE_CARD_EXIT_FADE_MS)),
        modifier = modifier
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.clip(NoteCollectionCardDefaults.shape),
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                SwipeDeleteBackground(
                    state = dismissState,
                    contentDescription = contentDescription
                )
            }
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeDeleteEffect(
    dismissState: SwipeToDismissBoxState,
    collapsedState: MutableState<Boolean>,
    collapseBeforeDelete: Boolean,
    onDelete: () -> Unit
) {
    val currentOnDelete by rememberUpdatedState(onDelete)

    LaunchedEffect(dismissState, collapseBeforeDelete) {
        snapshotFlow { dismissState.currentValue }
            .distinctUntilChanged()
            .collect { value ->
                if (value != SwipeToDismissBoxValue.EndToStart || collapsedState.value) {
                    return@collect
                }

                if (collapseBeforeDelete) {
                    collapsedState.value = true
                    delay(EditorMotion.NOTE_CARD_TRASH_DELAY_MS)
                }

                currentOnDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                collapsedState.value = false
            }
    }
}
