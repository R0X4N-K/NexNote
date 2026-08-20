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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.r0x4nk.nexnote.ui.common.EditorMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

/** Actions supported by a collection card's horizontal swipe surface. */
internal sealed interface SwipeCollectionAction {
    val contentDescription: String

    data class Delete(
        override val contentDescription: String
    ) : SwipeCollectionAction

    data class TogglePin(
        val isCurrentlyPinned: Boolean
    ) : SwipeCollectionAction {
        override val contentDescription: String =
            if (isCurrentlyPinned) "Unpin" else "Pin to top"
    }
}

/**
 * Hosts the directional swipe actions shared by note and template cards.
 *
 * End-to-start is the destructive action and may collapse the item before its
 * callback runs. Start-to-end is optional and invokes its callback before the
 * gesture resets so a collection reorder cannot interrupt action delivery.
 */
@Composable
internal fun SwipeToCollectionActionsContainer(
    endToStartAction: SwipeCollectionAction,
    onEndToStart: () -> Unit,
    modifier: Modifier = Modifier,
    startToEndAction: SwipeCollectionAction? = null,
    onStartToEnd: (() -> Unit)? = null,
    collapseBeforeEndToStart: Boolean = true,
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
    val dismissState = rememberCollectionDismissState(startToEndAction)
    val startToEndEnabled = startToEndAction != null && onStartToEnd != null

    SwipeCollectionActionEffect(
        dismissState = dismissState,
        collapsedState = collapsedState,
        collapseBeforeEndToStart = collapseBeforeEndToStart,
        onEndToStart = onEndToStart,
        onStartToEnd = onStartToEnd.takeIf { startToEndEnabled }
    )

    AnimatedVisibility(
        visible = !collapsedState.value,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = EditorMotion.NOTE_CARD_EXIT_SHRINK_MS)
        ) + fadeOut(
            animationSpec = tween(durationMillis = EditorMotion.NOTE_CARD_EXIT_FADE_MS)
        ),
        modifier = modifier
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.clip(NoteCollectionCardDefaults.shape),
            enableDismissFromStartToEnd = startToEndEnabled,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                SwipeCollectionActionBackground(
                    state = dismissState,
                    endToStartAction = endToStartAction,
                    startToEndAction = startToEndAction
                )
            }
        ) {
            content()
        }
    }
}

/**
 * Recreates the ephemeral gesture state when its directional action changes.
 * A pin update can immediately reorder a collection, so carrying a dismissed
 * state across that update would leave only the action background visible.
 */
@Composable
private fun rememberCollectionDismissState(
    startToEndAction: SwipeCollectionAction?
): SwipeToDismissBoxState = key(startToEndAction) {
    rememberSwipeToDismissBoxState()
}

@Composable
private fun SwipeCollectionActionEffect(
    dismissState: SwipeToDismissBoxState,
    collapsedState: MutableState<Boolean>,
    collapseBeforeEndToStart: Boolean,
    onEndToStart: () -> Unit,
    onStartToEnd: (() -> Unit)?
) {
    val currentOnEndToStart by rememberUpdatedState(onEndToStart)
    val currentOnStartToEnd by rememberUpdatedState(onStartToEnd)

    LaunchedEffect(dismissState, collapseBeforeEndToStart) {
        snapshotFlow { dismissState.currentValue }
            .distinctUntilChanged()
            .collect { value ->
                when (value) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (collapsedState.value) return@collect
                        if (collapseBeforeEndToStart) {
                            collapsedState.value = true
                            delay(EditorMotion.NOTE_CARD_TRASH_DELAY_MS)
                        }
                        currentOnEndToStart()
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        collapsedState.value = false
                    }

                    SwipeToDismissBoxValue.StartToEnd -> {
                        currentOnStartToEnd?.invoke()
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    }

                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
    }
}
