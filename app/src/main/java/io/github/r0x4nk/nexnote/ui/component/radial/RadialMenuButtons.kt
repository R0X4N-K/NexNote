package io.github.r0x4nk.nexnote.ui.component.radial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SCROLL_BUTTON_SIZE_DP  = 36.dp
private val SCROLL_BUTTON_GAP_DP   = 10.dp   // gap between scroll buttons and FAB top
private val SCROLL_BUTTON_SPACE_DP =  6.dp   // vertical gap between the two scroll buttons
private val FAB_SHAPE = RoundedCornerShape(18.dp)

private data class ScrollShortcutButtonLayout(
    val buttonX: Float,
    val topY: Float,
    val bottomY: Float,
    val sizePx: Float
)

// Scroll shortcut buttons.

/**
 * Renders a "scroll to top" and "scroll to bottom" button stacked above the
 * FAB, horizontally centred on it.
 *
 * The buttons are smaller than the FAB (36 dp vs 56 dp) and use a lighter
 * surface color so they feel secondary: present but unobtrusive.
 *
 * Layout (bottom-to-top):
 *   FAB <- [SCROLL_BUTTON_GAP_DP] <- bottom-button <- [SCROLL_BUTTON_SPACE_DP] <- top-button
 */
@Composable
internal fun ScrollShortcutButtons(
    fabX: Float,
    fabY: Float,
    buttonSizePx: Float,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit
) {
    val layout = scrollShortcutButtonLayout(fabX, fabY, buttonSizePx)

    ScrollShortcutButton(
        x           = layout.buttonX,
        y           = layout.topY,
        sizePx      = layout.sizePx,
        icon        = Icons.Default.ArrowUpward,
        description = "Scroll to top",
        onClick     = onScrollToTop
    )
    ScrollShortcutButton(
        x           = layout.buttonX,
        y           = layout.bottomY,
        sizePx      = layout.sizePx,
        icon        = Icons.Default.ArrowDownward,
        description = "Scroll to bottom",
        onClick     = onScrollToBottom
    )
}

@Composable
private fun scrollShortcutButtonLayout(
    fabX: Float,
    fabY: Float,
    buttonSizePx: Float
): ScrollShortcutButtonLayout {
    val density = LocalDensity.current
    val smallPx = with(density) { SCROLL_BUTTON_SIZE_DP.toPx() }
    val gapPx = with(density) { SCROLL_BUTTON_GAP_DP.toPx() }
    val spacingPx = with(density) { SCROLL_BUTTON_SPACE_DP.toPx() }
    val bottomY = fabY - smallPx - gapPx

    return ScrollShortcutButtonLayout(
        buttonX = fabX + (buttonSizePx - smallPx) / 2f,
        topY = bottomY - smallPx - spacingPx,
        bottomY = bottomY,
        sizePx = smallPx
    )
}

@Composable
private fun ScrollShortcutButton(
    x: Float,
    y: Float,
    sizePx: Float,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeDp = with(LocalDensity.current) { sizePx.toDp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(sizeDp)
            .shadow(elevation = 3.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier           = Modifier.size(18.dp)
        )
    }
}

// Static floating action button.

/**
 * Non-draggable rounded button at the fixed pixel position ([fabX], [fabY]).
 *
 * When the menu is open the icon is always Close. When closed the icon is
 * [closedIcon], which each screen can set via [RadialMenuController.fabIcon]
 * to reflect the nature of the items in its arc.
 */
@Composable
internal fun StaticMenuButton(
    isMenuOpen: Boolean,
    fabX: Float,
    fabY: Float,
    buttonSizePx: Float,
    closedIcon: ImageVector = Icons.Default.Add,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density      = LocalDensity.current
    val buttonSizeDp = with(density) { buttonSizePx.toDp() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset { IntOffset(fabX.roundToInt(), fabY.roundToInt()) }
            .size(buttonSizeDp)
            .shadow(elevation = 8.dp, shape = FAB_SHAPE, clip = false)
            .clip(FAB_SHAPE)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onToggle)
    ) {
        Icon(
            imageVector        = if (isMenuOpen) Icons.Default.Close else closedIcon,
            contentDescription = if (isMenuOpen) "Close menu" else "Open menu",
            tint               = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier           = androidx.compose.ui.Modifier.size(24.dp)
        )
    }
}
