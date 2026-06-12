package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.common.SortOrder
import io.github.r0x4nk.nexnote.ui.common.nextIn

@Composable
internal fun NoteListOverflowMenu(
    sortOrder: SortOrder,
    viewMode: NoteListViewMode,
    onToggleSortOrder: () -> Unit,
    onToggleViewMode: () -> Unit,
    availableViewModes: List<NoteListViewMode> = NoteListViewMode.noteModes,
    contentDescription: String = "More options",
    extraItems: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val dismiss = { expanded = false }
    val nextViewMode = viewMode.nextIn(availableViewModes)

    Box {
        NexIconButton(
            imageVector = Icons.Default.MoreVert,
            contentDescription = contentDescription,
            onClick = { expanded = true },
            selected = expanded
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = dismiss
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (sortOrder == SortOrder.MODIFIED_DESC) {
                            "Oldest first"
                        } else {
                            "Newest first"
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null
                    )
                },
                onClick = {
                    onToggleSortOrder()
                    dismiss()
                }
            )
            if (availableViewModes.size > 1) {
                DropdownMenuItem(
                    text = { Text(nextViewMode.actionLabel()) },
                    leadingIcon = {
                        Icon(
                            imageVector = nextViewMode.icon(),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onToggleViewMode()
                        dismiss()
                    }
                )
            }
            HorizontalDivider()
            extraItems(dismiss)
        }
    }
}

private fun NoteListViewMode.icon(): ImageVector =
    when (this) {
        NoteListViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
        NoteListViewMode.GRID -> Icons.Default.GridView
        NoteListViewMode.TAGS -> Icons.Default.Folder
    }

private fun NoteListViewMode.actionLabel(): String =
    when (this) {
        NoteListViewMode.LIST -> "List view"
        NoteListViewMode.GRID -> "Grid view"
        NoteListViewMode.TAGS -> "Tag folders"
    }
