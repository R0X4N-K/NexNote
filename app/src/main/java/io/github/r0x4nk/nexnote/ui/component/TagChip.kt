package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R

/**
 * Reusable pill chip displaying a tag name with the '#' prefix.
 *
 * Role: UI component layer - stateless, theme-aware, reused across Home,
 * Agenda, Editor, and Tags screens.
 *
 * Visual states:
 * - Default: elevated surface-container background, [onSurfaceVariant] text.
 * - Selected: [primaryContainer] background, [onPrimaryContainer] text.
 *
 * Variants:
 * - Standard: clickable pill with ripple.
 * - Dismissible: shows a close icon that calls [onDismiss] independently
 *   from the chip body click. Used in [TagFilterBar] for active filters.
 *
 * @param tagName    Lowercase tag name without the leading '#'.
 * @param onClick    Called when the chip body is tapped.
 * @param isSelected Whether the chip should render in its selected state.
 * @param dismissible Whether to show the dismiss icon.
 * @param onDismiss  Called when the dismiss icon is tapped (only relevant when
 *                   [dismissible] is true).
 * @param compact Reduces the chip padding for dense editor tool rows.
 */
@Composable
fun TagChip(
    tagName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    dismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    compact: Boolean = false
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHighest

    val contentColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant
    val horizontalPadding = if (compact) 8.dp else 12.dp
    val verticalPadding = if (compact) 3.dp else 6.dp
    val hasDismissAction = dismissible && onDismiss != null
    val textStyle = if (compact) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.labelMedium
    }

    Surface(
        onClick      = onClick,
        modifier     = modifier,
        shape        = if (compact) MaterialTheme.shapes.small else MaterialTheme.shapes.medium,
        color        = containerColor,
        contentColor = contentColor,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier              = Modifier.padding(
                start = horizontalPadding,
                end = if (hasDismissAction) 0.dp else horizontalPadding,
                top = if (hasDismissAction) 0.dp else verticalPadding,
                bottom = if (hasDismissAction) 0.dp else verticalPadding
            ),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text  = "#$tagName",
                style = textStyle,
                color = contentColor
            )
            if (dismissible && onDismiss != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(
                            R.string.tag_filter_remove,
                            tagName
                        ),
                        modifier = Modifier.size(18.dp),
                        tint = contentColor
                    )
                }
            }
        }
    }
}
