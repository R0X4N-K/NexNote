package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexIconButton

private val EditorTopBarMinHeight = 44.dp
private val EditorSearchFieldMinHeight = 36.dp

private enum class EditorOverflowMenuPage { Actions, Copy }

/**
 * Read-only snapshot of the editor controls that surface in the top bar.
 *
 * Bundling these flags into a single value class keeps [EditorTopBar]'s
 * signature compact while making the state vs. behaviour split explicit at
 * call sites. The class is intentionally [internal] so it cannot leak across
 * module boundaries.
 *
 * @property hasCustomColor whether the note has a custom background colour —
 *   used to render the palette icon in a selected state.
 */
internal data class EditorTopBarToolingState(
    val hasCustomColor: Boolean,
)

/**
 * Action callbacks for the editor-level controls hosted by the top bar.
 *
 * Pairing this with [EditorTopBarToolingState] keeps the read and write
 * surfaces aligned, mirroring the `*Content` / `*Actions` split already used
 * by [EditorScreenScaffoldContent] / [EditorScreenActions] in this package.
 */
internal data class EditorTopBarToolingActions(
    val onToggleColorPicker: () -> Unit,
    val onClearContent: (() -> Unit)? = null,
    val onTrash: (() -> Unit)? = null,
    val onMoveToVault: (() -> Unit)? = null,
    val onCheckAllTasks: (() -> Unit)? = null,
    val onUncheckAllTasks: (() -> Unit)? = null,
)

@Composable
internal fun EditorTopBar(
    isSaving: Boolean,
    title: String,
    isTemplateMode: Boolean,
    isReadOnly: Boolean,
    containerColor: Color,
    toolingState: EditorTopBarToolingState,
    toolingActions: EditorTopBarToolingActions,
    searchState: NoteSearchState,
    searchFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchPrevious: () -> Unit,
    onSearchNext: () -> Unit,
    metadata: EditorNoteMetadata? = null,
    onShare: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onCopyNoteAsText: (() -> Unit)? = null,
    onCopyNoteAsMarkdown: (() -> Unit)? = null,
    onCreationDateEdit: (() -> Unit)? = null
) {
    val newNoteFallback = stringResource(
        if (isTemplateMode) R.string.editor_new_template else R.string.editor_new_note
    )
    val displayTitle = when {
        title.isNotBlank() -> title
        else -> newNoteFallback
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = EditorTopBarMinHeight)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexIconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                onClick = onBack
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                EditorTopBarTitle(
                    displayTitle = displayTitle,
                    metadata = metadata,
                    searchState = searchState,
                    searchFocusRequester = searchFocusRequester,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchNext = onSearchNext
                )
            }
            EditorTopBarActions(
                isSaving = isSaving,
                isTemplateMode = isTemplateMode,
                isReadOnly = isReadOnly,
                toolingState = toolingState,
                toolingActions = toolingActions,
                searchState = searchState,
                onSearchOpen = onSearchOpen,
                onSearchClose = onSearchClose,
                onSearchPrevious = onSearchPrevious,
                onSearchNext = onSearchNext,
                onShare = onShare,
                onExport = onExport,
                onCopyNoteAsText = onCopyNoteAsText,
                onCopyNoteAsMarkdown = onCopyNoteAsMarkdown,
                onCreationDateEdit = onCreationDateEdit
            )
        }
    }
}

@Composable
private fun EditorTopBarTitle(
    displayTitle: String,
    metadata: EditorNoteMetadata?,
    searchState: NoteSearchState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchNext: () -> Unit
) {
    if (searchState.isActive) {
        EditorSearchField(
            value = searchState.query,
            focusRequester = searchFocusRequester,
            onValueChange = onSearchQueryChange,
            onSearchNext = onSearchNext
        )
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            metadata?.let { EditorMetadataBar(metadata = it) }
        }
    }
}

@Composable
private fun EditorSearchField(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSearchNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = EditorSearchFieldMinHeight)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchNext() }),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_in_note),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun EditorTopBarActions(
    isSaving: Boolean,
    isTemplateMode: Boolean,
    isReadOnly: Boolean,
    toolingState: EditorTopBarToolingState,
    toolingActions: EditorTopBarToolingActions,
    searchState: NoteSearchState,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchPrevious: () -> Unit,
    onSearchNext: () -> Unit,
    onShare: (() -> Unit)?,
    onExport: (() -> Unit)?,
    onCopyNoteAsText: (() -> Unit)?,
    onCopyNoteAsMarkdown: (() -> Unit)?,
    onCreationDateEdit: (() -> Unit)?
) {
    if (isSaving) EditorSavingIndicator()

    if (searchState.isActive) {
        EditorSearchActions(
            searchState = searchState,
            onSearchClose = onSearchClose,
            onSearchPrevious = onSearchPrevious,
            onSearchNext = onSearchNext
        )
    } else {
        EditorBrowsingActions(
            isTemplateMode = isTemplateMode,
            isReadOnly = isReadOnly,
            toolingState = toolingState,
            toolingActions = toolingActions,
            onSearchOpen = onSearchOpen,
            onShare = onShare,
                onExport = onExport,
            onCopyNoteAsText = onCopyNoteAsText,
            onCopyNoteAsMarkdown = onCopyNoteAsMarkdown,
            onCreationDateEdit = onCreationDateEdit
        )
    }
}

@Composable
private fun EditorSavingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp).padding(end = 4.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    )
    Spacer(Modifier.width(8.dp))
}

@Composable
private fun EditorSearchActions(
    searchState: NoteSearchState,
    onSearchClose: () -> Unit,
    onSearchPrevious: () -> Unit,
    onSearchNext: () -> Unit
) {
    if (searchState.resultLabel.isNotEmpty()) {
        Text(
            text = searchState.resultLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 40.dp)
        )
    }
    NexIconButton(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = stringResource(R.string.common_previous_match),
        onClick = onSearchPrevious,
        enabled = searchState.hasMatches
    )
    NexIconButton(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = stringResource(R.string.common_next_match),
        onClick = onSearchNext,
        enabled = searchState.hasMatches
    )
    NexIconButton(
        imageVector = Icons.Default.Close,
        contentDescription = stringResource(R.string.common_close_search),
        onClick = onSearchClose
    )
}

/**
 * Trailing actions shown while the user is **not** searching.
 *
 * Search stays directly reachable because it is a frequent document task.
 * Export, copy and colour are secondary note actions, so they live in the
 * overflow menu instead of competing with the Markdown editing surface.
 *
 * Editing-history controls live in the IME toolbar because they directly
 * affect text entry and must remain reachable while formatting tools scroll.
 */
@Composable
private fun EditorBrowsingActions(
    isTemplateMode: Boolean,
    isReadOnly: Boolean,
    toolingState: EditorTopBarToolingState,
    toolingActions: EditorTopBarToolingActions,
    onSearchOpen: () -> Unit,
    onShare: (() -> Unit)?,
    onExport: (() -> Unit)?,
    onCopyNoteAsText: (() -> Unit)?,
    onCopyNoteAsMarkdown: (() -> Unit)?,
    onCreationDateEdit: (() -> Unit)?
) {
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = stringResource(R.string.search_in_note),
        onClick = onSearchOpen
    )
    if (
        onShare != null || onExport != null ||
        onCopyNoteAsText != null ||
        onCopyNoteAsMarkdown != null ||
        onCreationDateEdit != null ||
        (!isTemplateMode && !isReadOnly)
    ) {
        EditorOverflowMenu(
            showColorAction = !isTemplateMode && !isReadOnly,
            toolingState = toolingState,
            toolingActions = toolingActions,
            onShare = onShare,
                onExport = onExport,
            onCopyNoteAsText = onCopyNoteAsText,
            onCopyNoteAsMarkdown = onCopyNoteAsMarkdown,
            onCreationDateEdit = onCreationDateEdit
        )
    }
}

@Composable
private fun EditorOverflowMenu(
    showColorAction: Boolean,
    toolingState: EditorTopBarToolingState,
    toolingActions: EditorTopBarToolingActions,
    onShare: (() -> Unit)?,
    onExport: (() -> Unit)?,
    onCopyNoteAsText: (() -> Unit)?,
    onCopyNoteAsMarkdown: (() -> Unit)?,
    onCreationDateEdit: (() -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(EditorOverflowMenuPage.Actions) }
    val hasCopyActions = onCopyNoteAsText != null || onCopyNoteAsMarkdown != null
    val dismiss = {
        expanded = false
        page = EditorOverflowMenuPage.Actions
    }

    Box {
        NexIconButton(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.common_note_options),
            onClick = {
                page = EditorOverflowMenuPage.Actions
                expanded = true
            },
            selected = expanded
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = dismiss
        ) {
            when (page) {
                EditorOverflowMenuPage.Actions -> EditorOverflowActionsPage(
                    showColorAction = showColorAction,
                    hasCopyActions = hasCopyActions,
                    toolingState = toolingState,
                    toolingActions = toolingActions,
                    onShare = onShare,
                onExport = onExport,
                    onCreationDateEdit = onCreationDateEdit,
                    onCopyOpen = { page = EditorOverflowMenuPage.Copy },
                    onDismiss = dismiss
                )

                EditorOverflowMenuPage.Copy -> EditorOverflowCopyPage(
                    onBack = { page = EditorOverflowMenuPage.Actions },
                    onCopyNoteAsText = onCopyNoteAsText,
                    onCopyNoteAsMarkdown = onCopyNoteAsMarkdown,
                    onDismiss = dismiss
                )
            }
        }
    }
}

@Composable
private fun EditorOverflowActionsPage(
    showColorAction: Boolean,
    hasCopyActions: Boolean,
    toolingState: EditorTopBarToolingState,
    toolingActions: EditorTopBarToolingActions,
    onShare: (() -> Unit)?,
    onExport: (() -> Unit)?,
    onCreationDateEdit: (() -> Unit)?,
    onCopyOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    if (onShare != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.common_share_note)) },
            leadingIcon = { Icon(Icons.Default.IosShare, contentDescription = null) },
            onClick = { onDismiss(); onShare() }
        )
    }
    if (onExport != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.common_export_note)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null
                )
            },
            onClick = {
                onDismiss()
                onExport()
            }
        )
    }
    if (hasCopyActions) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.common_copy_note)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null
                )
            },
            onClick = onCopyOpen
        )
    }
    if (onCreationDateEdit != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.edit_creation_date)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null
                )
            },
            onClick = {
                onDismiss()
                onCreationDateEdit()
            }
        )
    }
    toolingActions.onMoveToVault?.let { move ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.common_move_to_vault)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
            },
            onClick = { onDismiss(); move() }
        )
    }
    toolingActions.onClearContent?.let { clear ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.editor_clear_content)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = null
                )
            },
            onClick = { onDismiss(); clear() }
        )
    }
    toolingActions.onCheckAllTasks?.let { checkAll ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.editor_check_all_tasks)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null
                )
            },
            onClick = { onDismiss(); checkAll() }
        )
    }
    toolingActions.onUncheckAllTasks?.let { uncheckAll ->
        DropdownMenuItem(
            text = { Text(stringResource(R.string.editor_uncheck_all_tasks)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.RemoveDone,
                    contentDescription = null
                )
            },
            onClick = { onDismiss(); uncheckAll() }
        )
    }
    toolingActions.onTrash?.let { trash ->
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.common_move_to_trash),
                    color = MaterialTheme.colorScheme.error
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = { onDismiss(); trash() }
        )
    }
    if (showColorAction) {
        val color = if (toolingState.hasCustomColor) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.note_background_color)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = color
                )
            },
            onClick = {
                onDismiss()
                toolingActions.onToggleColorPicker()
            }
        )
    }
}

@Composable
private fun EditorOverflowCopyPage(
    onBack: () -> Unit,
    onCopyNoteAsText: (() -> Unit)?,
    onCopyNoteAsMarkdown: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(R.string.common_back)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null
            )
        },
        onClick = onBack
    )
    if (onCopyNoteAsText != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.common_copy_as_text)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.TextSnippet,
                    contentDescription = null
                )
            },
            onClick = {
                onDismiss()
                onCopyNoteAsText()
            }
        )
    }
    if (onCopyNoteAsMarkdown != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.common_copy_as_markdown)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    contentDescription = null
                )
            },
            onClick = {
                onDismiss()
                onCopyNoteAsMarkdown()
            }
        )
    }
}
