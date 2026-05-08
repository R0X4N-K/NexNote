package io.github.r0x4nk.nexnote.ui.screen.trash

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.buildNoteCardDisplayText
import io.github.r0x4nk.nexnote.util.DateUtils

private const val TRASH_NOTE_EXCERPT_MAX_LENGTH = 120

private data class TrashNoteCardTextState(
    val title: AnnotatedString,
    val excerpt: AnnotatedString?
)

@Composable
internal fun TrashNoteCard(
    note: Note,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        )
    ) {
        TrashNoteCardContent(note, onRestore, onDeletePermanently)
    }
}

@Composable
private fun TrashNoteCardContent(
    note: Note,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val textState = rememberTrashNoteCardTextState(note)

    Column(
        modifier = Modifier.padding(
            start = 16.dp, top = 12.dp, bottom = 4.dp, end = 8.dp
        )
    ) {
        TrashNoteTitle(textState.title)
        TrashNoteExcerpt(textState.excerpt)
        Spacer(Modifier.height(4.dp))
        TrashNoteDate(note)
        TrashNoteActions(onRestore, onDeletePermanently)
    }
}

@Composable
private fun rememberTrashNoteCardTextState(note: Note): TrashNoteCardTextState {
    val primaryColor = MaterialTheme.colorScheme.primary
    return remember(note.title, note.content, note.isMarkdown, primaryColor) {
        TrashNoteCardTextState(
            title = buildNoteCardDisplayText(
                sourceText = noteDisplayTitle(note),
                ranges = emptyList(),
                linkColor = primaryColor,
                highlightColor = primaryColor,
                renderMarkdown = note.isMarkdown
            ),
            excerpt = if (note.title.isNotBlank() && note.content.isNotBlank()) {
                buildNoteCardDisplayText(
                    sourceText = note.content.take(TRASH_NOTE_EXCERPT_MAX_LENGTH),
                    ranges = emptyList(),
                    linkColor = primaryColor,
                    highlightColor = primaryColor,
                    renderMarkdown = note.isMarkdown
                )
            } else {
                null
            }
        )
    }
}

@Composable
private fun TrashNoteTitle(title: AnnotatedString) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TrashNoteExcerpt(excerpt: AnnotatedString?) {
    if (excerpt != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text     = excerpt,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrashNoteDate(note: Note) {
    Text(
        text  = noteDateLabel(note),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    )
}

@Composable
private fun TrashNoteActions(
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        RestoreNoteButton(onRestore)
        DeleteNoteButton(onDeletePermanently)
    }
}

@Composable
private fun RestoreNoteButton(onRestore: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.RestoreFromTrash,
        contentDescription = "Restore note",
        onClick = onRestore,
        selected = true
    )
}

@Composable
private fun DeleteNoteButton(onDeletePermanently: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.DeleteForever,
        contentDescription = "Delete permanently",
        onClick = onDeletePermanently,
        destructive = true
    )
}

private fun noteDisplayTitle(note: Note): String {
    return note.title.ifBlank {
        note.content.lines().firstOrNull { it.isNotBlank() }?.take(80)
            ?: "Untitled note"
    }
}

private fun noteDateLabel(note: Note): String {
    return note.deletedDate
        ?.let { "Deleted ${DateUtils.formatDate(it)}" }
        ?: "Edited ${DateUtils.formatRelative(note.lastModifiedDate)}"
}
