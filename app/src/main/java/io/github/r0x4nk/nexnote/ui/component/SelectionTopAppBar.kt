package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopAppBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onDeleteSelected: (() -> Unit)? = null,
    deleteContentDescription: String = "Move selected to trash"
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            NexIconButton(
                imageVector = Icons.Default.Close,
                contentDescription = "Close selection",
                onClick = onClose
            )
        },
        actions = {
            NexIconButton(
                imageVector = Icons.Default.SelectAll,
                contentDescription = "Select all",
                onClick = onSelectAll,
                enabled = totalCount > 0 && selectedCount < totalCount
            )
            NexIconButton(
                imageVector = Icons.Default.Deselect,
                contentDescription = "Deselect all",
                onClick = onDeselectAll,
                enabled = selectedCount > 0
            )
            if (onDeleteSelected != null) {
                NexIconButton(
                    imageVector = Icons.Default.Delete,
                    contentDescription = deleteContentDescription,
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0,
                    destructive = true
                )
            }
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}
