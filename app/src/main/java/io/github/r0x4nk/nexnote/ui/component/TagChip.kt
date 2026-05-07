package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

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
    val horizontalPadding = if (compact) 9.dp else 12.dp
    val verticalPadding = if (compact) 4.dp else 7.dp
    val textStyle = if (compact) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.labelMedium
    }

    Surface(
        onClick      = onClick,
        modifier     = modifier,
        shape        = MaterialTheme.shapes.extraLarge,
        color        = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier              = Modifier.padding(
                start = horizontalPadding,
                end = if (dismissible) 7.dp else horizontalPadding,
                top = verticalPadding,
                bottom = verticalPadding
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
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Remove #$tagName filter",
                    modifier           = Modifier
                        .size(14.dp)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}
