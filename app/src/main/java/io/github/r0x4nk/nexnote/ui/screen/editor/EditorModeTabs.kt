package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R

private val EditorModeTabsMinHeight = 40.dp

@Composable
internal fun EditorModeTabs(
    showPreview: Boolean,
    enabled: Boolean,
    onModeSelected: (showPreview: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = if (showPreview) 1 else 0,
        modifier = modifier.heightIn(min = EditorModeTabsMinHeight),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = {},
        divider = {}
    ) {
        EditorModeTab(
            label = stringResource(R.string.editor_mode_editing),
            selected = !showPreview,
            enabled = enabled,
            onClick = { onModeSelected(false) }
        )
        EditorModeTab(
            label = stringResource(R.string.editor_mode_preview),
            selected = showPreview,
            enabled = enabled,
            onClick = { onModeSelected(true) }
        )
    }
}

@Composable
private fun EditorModeTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    Tab(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .heightIn(min = EditorModeTabsMinHeight)
            .drawBehind {
                if (selected) {
                    val inset = 6.dp.toPx()
                    drawRoundRect(
                        color = selectedContainer,
                        topLeft = Offset(inset, inset),
                        size = Size((size.width - 2 * inset).coerceAtLeast(0f), (size.height - 2 * inset).coerceAtLeast(0f)),
                        cornerRadius = CornerRadius(14.dp.toPx())
                    )
                }
            }
            .semantics { contentDescription = label },
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}
