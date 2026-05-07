package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
        divider = {}
    ) {
        EditorModeTab(
            label = "Editing",
            selected = !showPreview,
            enabled = enabled,
            onClick = { onModeSelected(false) }
        )
        EditorModeTab(
            label = "Preview",
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
    Tab(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .heightIn(min = EditorModeTabsMinHeight)
            .semantics { contentDescription = label },
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}
