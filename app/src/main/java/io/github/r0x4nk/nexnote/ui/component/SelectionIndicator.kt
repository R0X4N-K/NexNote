package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.common.NoteMotion

/**
 * Checkbox-style selection affordance for collection cards.
 *
 * The container colour and the check/uncheck glyph crossfade when the selection
 * changes so entering and leaving selection mode feels continuous.
 */
@Composable
internal fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val containerColor by animateColorAsState(
        targetValue = if (selected) primary else Color.Transparent,
        animationSpec = tween(
            durationMillis = NoteMotion.SELECTION_MS,
            easing = NoteMotion.cardStateEasing
        ),
        label = "selectionContainerColor"
    )

    Box(
        modifier = modifier
            .padding(start = 6.dp)
            .size(28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = selected,
            animationSpec = tween(durationMillis = NoteMotion.SELECTION_MS),
            label = "selectionIcon"
        ) { isSelected ->
            Icon(
                imageVector = if (isSelected) {
                    Icons.Default.Check
                } else {
                    Icons.Outlined.RadioButtonUnchecked
                },
                contentDescription = stringResource(
                    if (isSelected) R.string.common_selected else R.string.common_not_selected
                ),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
