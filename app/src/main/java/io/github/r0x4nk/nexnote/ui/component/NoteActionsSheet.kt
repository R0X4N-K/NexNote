package io.github.r0x4nk.nexnote.ui.component

import android.content.ClipData
import android.os.PersistableBundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.filled.IosShare
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

/**
 * Clipboard operations exposed to note-list surfaces that can copy a note.
 *
 * Keeping these lambdas in a small immutable holder lets callers remember the
 * clipboard/snackbar wiring once and pass a stable object into [NoteActionsSheet].
 */
@Immutable
internal data class NoteClipboardCallbacks(
    val onCopyPlainText: (Note) -> Unit,
    val onCopyMarkdown: (Note) -> Unit,
    val onCopyPlainTextNotes: (Collection<Note>) -> Unit = { notes ->
        notes.singleOrNull()?.let(onCopyPlainText)
    },
    val onCopyMarkdownNotes: (Collection<Note>) -> Unit = { notes ->
        notes.singleOrNull()?.let(onCopyMarkdown)
    }
)

/**
 * Creates clipboard callbacks that copy a note and report the result via a snackbar.
 *
 * The implementation stays in the component layer because it depends on Compose
 * clipboard locals and on [SnackbarHostState], while callers only need the
 * stable [NoteClipboardCallbacks] contract.
 */
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
            },
            onCopyPlainTextNotes = { notes ->
                scope.launch {
                    copyNotesToClipboard(
                        clipboard = clipboard,
                        snackbarHostState = snackbarHostState,
                        notes = notes,
                        asMarkdown = false
                    )
                }
            },
            onCopyMarkdownNotes = { notes ->
                scope.launch {
                    copyNotesToClipboard(
                        clipboard = clipboard,
                        snackbarHostState = snackbarHostState,
                        notes = notes,
                        asMarkdown = true
                    )
                }
            }
        )
    }
}

private suspend fun copyNotesToClipboard(
    clipboard: Clipboard,
    snackbarHostState: SnackbarHostState,
    notes: Collection<Note>,
    asMarkdown: Boolean
) {
    if (notes.isEmpty()) return

    val text = if (asMarkdown) {
        notes.copyAsMarkdown()
    } else {
        notes.copyAsPlainText()
    }
    val count = notes.size
    val format = if (asMarkdown) "Markdown" else "text"
    copyTextToClipboard(
        clipboard = clipboard,
        snackbarHostState = snackbarHostState,
        text = text,
        snackbarMessage = if (count == 1) {
            "Copied as $format"
        } else {
            "Copied $count notes as $format"
        }
    )
}

internal suspend fun copyTextToClipboard(
    clipboard: Clipboard,
    snackbarHostState: SnackbarHostState,
    text: String,
    snackbarMessage: String
) {
    clipboard.setClipEntry(sensitiveNoteClipData(text).toClipEntry())
    snackbarHostState.showSnackbar(
        message = snackbarMessage,
        duration = SnackbarDuration.Short
    )
}

/**
 * Marks copied note text as sensitive so compatible Android keyboards and
 * system surfaces obscure the clipboard preview. This flag is a presentation
 * safeguard; it does not encrypt or isolate the clipboard contents.
 */
internal fun sensitiveNoteClipData(text: String): ClipData =
    ClipData.newPlainText(NOTE_CLIP_LABEL, text).apply {
        description.extras = PersistableBundle().apply {
            putBoolean(SENSITIVE_CLIPBOARD_EXTRA, true)
        }
    }

private const val SENSITIVE_CLIPBOARD_EXTRA = "android.content.extra.IS_SENSITIVE"

/**
 * Bottom sheet for secondary note actions in list and agenda surfaces.
 *
 * The sheet owns the two-step copy flow (actions page, then copy format page)
 * and dismisses itself after mutating actions so list ViewModels only receive
 * domain-level callbacks such as duplicate, delete, or copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteActionsSheet(
    note: Note?,
    clipboardCallbacks: NoteClipboardCallbacks,
    shareCallbacks: NoteShareCallbacks? = null,
    onDuplicate: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onMoveToVault: ((Note) -> Unit)? = null,
    onSelect: ((Note) -> Unit)? = null,
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
            NoteActionsSheetHeader(
                title = if (page == NoteActionsPage.Actions) "Note actions" else "Copy note",
                noteLabel = note.displayLabel()
            )
            HorizontalDivider()
            when (page) {
                NoteActionsPage.Actions -> NoteActionsMainPage(
                    showMoveToVault = onMoveToVault != null && !note.isInVault,
                    showSelect = onSelect != null,
                    showShare = shareCallbacks != null,
                    onSelect = {
                        onSelect?.invoke(note)
                        onDismiss()
                    },
                    onShare = {
                        shareCallbacks?.onShareNote(note)
                        onDismiss()
                    },
                    onCopy = { page = NoteActionsPage.Copy },
                    onMoveToVault = {
                        onMoveToVault?.invoke(note)
                        onDismiss()
                    },
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
internal fun NoteActionsSheetHeader(
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
    showMoveToVault: Boolean,
    showSelect: Boolean,
    showShare: Boolean,
    onSelect: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMoveToVault: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    if (showSelect) {
        NoteActionsSheetRow(
            text = "Select",
            icon = Icons.Outlined.CheckCircle,
            onClick = onSelect
        )
    }
    if (showShare) {
        NoteActionsSheetRow(
            text = "Share",
            icon = Icons.Default.IosShare,
            onClick = onShare
        )
    }
    NoteActionsSheetRow(
        text = "Copy",
        icon = Icons.Outlined.ContentCopy,
        onClick = onCopy
    )
    NoteActionsSheetRow(
        text = "Duplicate",
        icon = Icons.Outlined.FileCopy,
        onClick = onDuplicate
    )
    if (showMoveToVault) {
        NoteActionsSheetRow(
            text = "Move to Vault",
            icon = Icons.Outlined.Lock,
            onClick = onMoveToVault
        )
    }
    NoteActionsSheetRow(
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
    NoteActionsSheetRow(
        text = "Back",
        icon = Icons.AutoMirrored.Outlined.ArrowBack,
        onClick = onBack
    )
    NoteActionsSheetRow(
        text = "Copy as text",
        icon = Icons.AutoMirrored.Outlined.TextSnippet,
        onClick = onCopyPlainText
    )
    NoteActionsSheetRow(
        text = "Copy as Markdown",
        icon = Icons.Outlined.Code,
        onClick = onCopyMarkdown
    )
}

@Composable
internal fun NoteActionsSheetRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
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
        modifier = modifier.clickable(onClick = onClick)
    )
}
