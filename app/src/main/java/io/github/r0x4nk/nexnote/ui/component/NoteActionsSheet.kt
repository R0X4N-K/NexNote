package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import android.content.ClipData
import android.os.PersistableBundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.copyAsMarkdown
import io.github.r0x4nk.nexnote.ui.common.copyAsPlainText
import io.github.r0x4nk.nexnote.ui.common.displayLabel
import kotlinx.coroutines.launch

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
    val resources = LocalResources.current

    return remember(clipboard, snackbarHostState, scope, resources) {
        val clipLabel = resources.getString(R.string.note_clip_label)
        NoteClipboardCallbacks(
            onCopyPlainText = { note ->
                scope.launch {
                    copyTextToClipboard(
                        clipboard = clipboard,
                        snackbarHostState = snackbarHostState,
                        text = note.copyAsPlainText(),
                        snackbarMessage = resources.getString(R.string.editor_copied_as_text),
                        clipLabel = clipLabel
                    )
                }
            },
            onCopyMarkdown = { note ->
                scope.launch {
                    copyTextToClipboard(
                        clipboard = clipboard,
                        snackbarHostState = snackbarHostState,
                        text = note.copyAsMarkdown(),
                        snackbarMessage = resources.getString(R.string.editor_copied_as_markdown),
                        clipLabel = clipLabel
                    )
                }
            },
            onCopyPlainTextNotes = { notes ->
                scope.launch {
                    copyNotesToClipboard(
                        clipboard = clipboard,
                        snackbarHostState = snackbarHostState,
                        resources = resources,
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
                        resources = resources,
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
    resources: android.content.res.Resources,
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
    val message = when {
        count == 1 && asMarkdown -> resources.getString(R.string.editor_copied_as_markdown)
        count == 1 -> resources.getString(R.string.editor_copied_as_text)
        asMarkdown -> resources.getQuantityString(R.plurals.copied_notes_as_markdown, count, count)
        else -> resources.getQuantityString(R.plurals.copied_notes_as_text, count, count)
    }
    copyTextToClipboard(
        clipboard = clipboard,
        snackbarHostState = snackbarHostState,
        text = text,
        snackbarMessage = message,
        clipLabel = resources.getString(R.string.note_clip_label)
    )
}

internal suspend fun copyTextToClipboard(
    clipboard: Clipboard,
    snackbarHostState: SnackbarHostState,
    text: String,
    snackbarMessage: String,
    clipLabel: String
) {
    clipboard.setClipEntry(sensitiveNoteClipData(text, clipLabel).toClipEntry())
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
internal fun sensitiveNoteClipData(text: String, clipLabel: String): ClipData =
    ClipData.newPlainText(clipLabel, text).apply {
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
    onExport: ((Note) -> Unit)? = null,
    onEditCreationDate: ((Note) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (note == null) return

    var page by remember(note.id) { mutableStateOf(NoteActionsPage.Actions) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        dragHandle = { NexSheetDragHandle() },
        tonalElevation = 1.dp,
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            val onHeaderBack: () -> Unit = if (page == NoteActionsPage.Actions) {
                onDismiss
            } else {
                { page = NoteActionsPage.Actions }
            }
            NexSheetHeader(
                title = if (page == NoteActionsPage.Actions) {
                    stringResource(R.string.common_note_actions)
                } else {
                    stringResource(R.string.common_copy_note)
                },
                subtitle = note.displayLabel(untitledLabel = stringResource(R.string.untitled_note)),
                onBack = onHeaderBack
            )
            Spacer(Modifier.height(8.dp))
            when (page) {
                NoteActionsPage.Actions -> NoteActionsMainPage(
                    showMoveToVault = onMoveToVault != null && !note.isInVault,
                    showShare = shareCallbacks != null,
                    onExport = onExport?.let { export -> { export(note); onDismiss() } },
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
                    onEditCreationDate = onEditCreationDate?.let { edit ->
                        {
                            edit(note)
                            onDismiss()
                        }
                    },
                    onDelete = {
                        onDelete(note)
                        onDismiss()
                    }
                )

                NoteActionsPage.Copy -> NoteActionsCopyPage(
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
private fun NoteActionsMainPage(
    showMoveToVault: Boolean,
    showShare: Boolean,
    onExport: (() -> Unit)?,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMoveToVault: () -> Unit,
    onDuplicate: () -> Unit,
    onEditCreationDate: (() -> Unit)?,
    onDelete: () -> Unit
) {
    if (onExport != null) {
        NoteActionsSheetRow(
            text = stringResource(R.string.common_export_note),
            icon = Icons.Default.FileDownload,
            onClick = onExport
        )
    }
    if (showShare) {
        NoteActionsSheetRow(
            text = stringResource(R.string.common_share),
            icon = Icons.Default.IosShare,
            onClick = onShare
        )
    }
    NoteActionsSheetRow(
        text = stringResource(R.string.common_copy),
        icon = Icons.Outlined.ContentCopy,
        onClick = onCopy
    )
    NoteActionsSheetRow(
        text = stringResource(R.string.common_duplicate),
        icon = Icons.Outlined.FileCopy,
        onClick = onDuplicate
    )
    if (onEditCreationDate != null) {
        NoteActionsSheetRow(
            text = stringResource(R.string.edit_creation_date),
            icon = Icons.Outlined.EditCalendar,
            onClick = onEditCreationDate
        )
    }
    if (showMoveToVault) {
        NoteActionsSheetRow(
            text = stringResource(R.string.common_move_to_vault),
            icon = Icons.Outlined.Lock,
            onClick = onMoveToVault
        )
    }
    NoteActionsSheetRow(
        text = stringResource(R.string.delete),
        icon = Icons.Outlined.Delete,
        destructive = true,
        onClick = onDelete
    )
}

@Composable
private fun NoteActionsCopyPage(
    onCopyPlainText: () -> Unit,
    onCopyMarkdown: () -> Unit
) {
    NoteActionsSheetRow(
        text = stringResource(R.string.common_copy_as_text),
        icon = Icons.AutoMirrored.Outlined.TextSnippet,
        onClick = onCopyPlainText
    )
    NoteActionsSheetRow(
        text = stringResource(R.string.common_copy_as_markdown),
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            headlineColor = contentColor
        ),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 2.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
    )
}
