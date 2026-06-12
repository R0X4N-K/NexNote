package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopAppBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onShareSelected: (() -> Unit)? = null,
    onCopySelectedAsText: (() -> Unit)? = null,
    onCopySelectedAsMarkdown: (() -> Unit)? = null,
    onDeleteSelected: (() -> Unit)? = null,
    deleteContentDescription: String = "Move selected to trash"
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    val showCopyMenu = onCopySelectedAsText != null || onCopySelectedAsMarkdown != null

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
            if (onDeleteSelected != null) {
                NexIconButton(
                    imageVector = Icons.Default.Delete,
                    contentDescription = deleteContentDescription,
                    onClick = onDeleteSelected,
                    enabled = selectedCount > 0,
                    destructive = true
                )
            }
            Box {
                NexIconButton(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Selection options",
                    onClick = { overflowExpanded = true },
                    selected = overflowExpanded
                )
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Select all") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = null
                            )
                        },
                        enabled = totalCount > 0 && selectedCount < totalCount,
                        onClick = {
                            overflowExpanded = false
                            onSelectAll()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Deselect all") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Deselect,
                                contentDescription = null
                            )
                        },
                        enabled = selectedCount > 0,
                        onClick = {
                            overflowExpanded = false
                            onDeselectAll()
                        }
                    )
                    if (onShareSelected != null || showCopyMenu) {
                        HorizontalDivider()
                    }
                    if (onShareSelected != null) {
                        DropdownMenuItem(
                            text = { Text("Share selected") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = null
                                )
                            },
                            enabled = selectedCount > 0,
                            onClick = {
                                overflowExpanded = false
                                onShareSelected()
                            }
                        )
                    }
                    if (onCopySelectedAsText != null) {
                        DropdownMenuItem(
                            text = { Text("Copy as text") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.TextSnippet,
                                    contentDescription = null
                                )
                            },
                            enabled = selectedCount > 0,
                            onClick = {
                                overflowExpanded = false
                                onCopySelectedAsText()
                            }
                        )
                    }
                    if (onCopySelectedAsMarkdown != null) {
                        DropdownMenuItem(
                            text = { Text("Copy as Markdown") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Code,
                                    contentDescription = null
                                )
                            },
                            enabled = selectedCount > 0,
                            onClick = {
                                overflowExpanded = false
                                onCopySelectedAsMarkdown()
                            }
                        )
                    }
                }
            }
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}
