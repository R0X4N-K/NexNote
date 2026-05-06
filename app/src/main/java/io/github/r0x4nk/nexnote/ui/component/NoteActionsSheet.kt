package io.github.r0x4nk.nexnote.ui.component

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.copyAsMarkdown
import io.github.r0x4nk.nexnote.ui.common.copyAsPlainText
import io.github.r0x4nk.nexnote.ui.common.displayLabel
import kotlinx.coroutines.launch

private const val NOTE_CLIP_LABEL = "NexNote note"

private enum class NoteActionsPage { Actions, Copy }

@Immutable
internal data class NoteClipboardCallbacks(
    val onCopyPlainText: (Note) -> Unit,
    val onCopyMarkdown: (Note) -> Unit
)

@Composable
internal fun rememberNoteClipboardCallbacks(
    snackbarHostState: SnackbarHostState
): NoteClipboardCallbacks {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    return remember(clipboard, snackbarHostState, scope) {
        NoteClipboardCallbacks(
            onCopyPlainText = { note ->
                scope.launch {
                    copyTextToClipboard(
                        clipboard = clipboard,
                        snackbarHostState = snackbarHostState,
                        text = note.copyAsPlainText(),
                        snackbarMessage = "Copied as text"
                    )
                }
            },
            onCopyMarkdown = { note ->
                scope.launch {
                    copyTextToClipboard(
                        clipboard = clipboard,
                        snackbarHostState = snackbarHostState,
                        text = note.copyAsMarkdown(),
                        snackbarMessage = "Copied as Markdown"
                    )
                }
            }
        )
    }
}

private suspend fun copyTextToClipboard(
    clipboard: Clipboard,
    snackbarHostState: SnackbarHostState,
    text: String,
    snackbarMessage: String
) {
    clipboard.setClipEntry(ClipData.newPlainText(NOTE_CLIP_LABEL, text).toClipEntry())
    snackbarHostState.showSnackbar(
        message = snackbarMessage,
        duration = SnackbarDuration.Short
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteActionsSheet(
    note: Note?,
    clipboardCallbacks: NoteClipboardCallbacks,
    onDuplicate: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    if (note == null) return

    var page by remember(note.id) { mutableStateOf(NoteActionsPage.Actions) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            NoteActionsHeader(
                title = if (page == NoteActionsPage.Actions) "Note actions" else "Copy note",
                noteLabel = note.displayLabel()
            )
            HorizontalDivider()
            when (page) {
                NoteActionsPage.Actions -> NoteActionsMainPage(
                    onCopy = { page = NoteActionsPage.Copy },
                    onDuplicate = {
                        onDuplicate(note)
                        onDismiss()
                    },
                    onDelete = {
                        onDelete(note)
                        onDismiss()
                    }
                )

                NoteActionsPage.Copy -> NoteActionsCopyPage(
                    onBack = { page = NoteActionsPage.Actions },
                    onCopyPlainText = {
                        clipboardCallbacks.onCopyPlainText(note)
                        onDismiss()
                    },
                    onCopyMarkdown = {
                        clipboardCallbacks.onCopyMarkdown(note)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun NoteActionsHeader(
    title: String,
    noteLabel: String
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = noteLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NoteActionsMainPage(
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    NoteActionRow(
        text = "Copy",
        icon = Icons.Outlined.ContentCopy,
        onClick = onCopy
    )
    NoteActionRow(
        text = "Duplicate",
        icon = Icons.Outlined.FileCopy,
        onClick = onDuplicate
    )
    NoteActionRow(
        text = "Delete",
        icon = Icons.Outlined.Delete,
        destructive = true,
        onClick = onDelete
    )
}

@Composable
private fun NoteActionsCopyPage(
    onBack: () -> Unit,
    onCopyPlainText: () -> Unit,
    onCopyMarkdown: () -> Unit
) {
    NoteActionRow(
        text = "Back",
        icon = Icons.AutoMirrored.Outlined.ArrowBack,
        onClick = onBack
    )
    NoteActionRow(
        text = "Copy as text",
        icon = Icons.AutoMirrored.Outlined.TextSnippet,
        onClick = onCopyPlainText
    )
    NoteActionRow(
        text = "Copy as Markdown",
        icon = Icons.Outlined.Code,
        onClick = onCopyMarkdown
    )
}

@Composable
private fun NoteActionRow(
    text: String,
    icon: ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val leadingColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        headlineContent = { Text(text = text) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = leadingColor
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = contentColor
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}
