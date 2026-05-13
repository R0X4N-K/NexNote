package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role

/**
 * Keeps touch feedback inside the same rounded outline as the visual surface.
 *
 * Compose click indications are rectangular unless the clickable node is
 * hosted inside a clipped target. Centralising the modifier chain avoids the
 * same subtle pressed-state bug on cards, chips, and calendar cells.
 */
internal fun Modifier.roundedClickableTarget(
    shape: Shape,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = clip(shape).clickable(
    enabled = enabled,
    role = role,
    onClick = onClick
)

/**
 * Combined-click variant of [roundedClickableTarget] for surfaces that support
 * both tap and long-press gestures while keeping ripple clipping consistent.
 */
internal fun Modifier.roundedCombinedClickableTarget(
    shape: Shape,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = clip(shape).combinedClickable(
    enabled = enabled,
    onClick = onClick,
    onLongClick = onLongClick
)
