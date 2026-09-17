package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import androidx.compose.material.icons.outlined.EditNote
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionTopAppBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onNoteActions: (() -> Unit)? = null,
    onShareSelected: (() -> Unit)? = null,
    onCopySelectedAsText: (() -> Unit)? = null,
    onCopySelectedAsMarkdown: (() -> Unit)? = null,
    onDeleteSelected: (() -> Unit)? = null,
    deleteContentDescription: String? = null
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    val showCopyMenu = onCopySelectedAsText != null || onCopySelectedAsMarkdown != null

    TopAppBar(
        title = {
            Text(
                text = pluralStringResource(
                    R.plurals.common_selected_count,
                    selectedCount,
                    selectedCount
                ),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            NexIconButton(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.common_close_selection),
                onClick = onClose
            )
        },
        actions = {
            Row(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (onDeleteSelected != null) {
                    NexIconButton(
                        imageVector = Icons.Default.Delete,
                        contentDescription = deleteContentDescription
                            ?: stringResource(R.string.common_move_selected_to_trash),
                        onClick = onDeleteSelected,
                        enabled = selectedCount > 0,
                        destructive = true
                    )
                }
                val showsSingleNoteActions = selectedCount == 1 && onNoteActions != null
                Box {
                    NexIconButton(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.common_selection_options),
                        onClick = { overflowExpanded = true },
                        selected = overflowExpanded
                    )
                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false }
                    ) {
                        if (showsSingleNoteActions) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_note_actions)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.EditNote,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onNoteActions?.invoke()
                                }
                            )
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_select_all)) },
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
                            text = { Text(stringResource(R.string.common_deselect_all)) },
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
                                text = { Text(stringResource(R.string.common_share_selected)) },
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
                                text = { Text(stringResource(R.string.common_copy_as_text)) },
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
                                text = { Text(stringResource(R.string.common_copy_as_markdown)) },
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
            }
        },
        colors = nexTopAppBarColors(),
        scrollBehavior = scrollBehavior
    )
}
