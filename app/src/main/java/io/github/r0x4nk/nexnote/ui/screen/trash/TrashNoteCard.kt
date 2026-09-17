package io.github.r0x4nk.nexnote.ui.screen.trash

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NoteCollectionCardDefaults
import io.github.r0x4nk.nexnote.ui.component.buildNoteCardDisplayText
import io.github.r0x4nk.nexnote.ui.theme.NoteContentTheme
import io.github.r0x4nk.nexnote.ui.theme.rememberContentMarkdownColors
import io.github.r0x4nk.nexnote.ui.theme.rememberNoteColors
import io.github.r0x4nk.nexnote.ui.common.noteRelativeTimeLabel
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
    modifier: Modifier = Modifier,
    onDeletePermanently: (() -> Unit)? = null
) {
    val colors = rememberNoteColors(note.backgroundColor)
    NoteContentTheme(colors) {
        Card(
            modifier  = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(
                defaultElevation = NoteCollectionCardDefaults.defaultElevation
            ),
            colors    = CardDefaults.cardColors(
                containerColor = colors.container,
                contentColor = colors.onContainer
            ),
            shape = NoteCollectionCardDefaults.shape,
            border = NoteCollectionCardDefaults.border()
        ) {
            TrashNoteCardContent(note, onRestore, onDeletePermanently)
        }
    }
}

@Composable
private fun TrashNoteCardContent(
    note: Note,
    onRestore: () -> Unit,
    onDeletePermanently: (() -> Unit)?
) {
    val textState = rememberTrashNoteCardTextState(note)

    Column(
        modifier = Modifier.padding(
            start = 20.dp, top = 16.dp, bottom = 12.dp, end = 20.dp
        )
    ) {
        TrashNoteTitle(textState.title)
        TrashNoteExcerpt(textState.excerpt)
        Spacer(Modifier.height(12.dp))
        TrashNoteDate(note)
        Spacer(Modifier.height(12.dp))
        TrashNoteActions(onRestore, onDeletePermanently)
    }
}

@Composable
private fun rememberTrashNoteCardTextState(note: Note): TrashNoteCardTextState {
    val primaryColor = MaterialTheme.colorScheme.primary
    val markdownColors = rememberContentMarkdownColors()
    val untitled = stringResource(R.string.untitled_note)
    val imagePlaceholder = stringResource(R.string.markdown_image_alt_fallback)
    // A missing title falls back to a placeholder; the body is never promoted
    // to the title, so the whole content stays available as the excerpt.
    val displayTitle = note.title.take(80).ifBlank { untitled }
    val excerptSource = note.content.take(TRASH_NOTE_EXCERPT_MAX_LENGTH)
    return remember(
        note.title,
        note.content,
        note.isMarkdown,
        markdownColors,
        displayTitle,
        excerptSource,
        imagePlaceholder
    ) {
        TrashNoteCardTextState(
            title = buildNoteCardDisplayText(
                sourceText = displayTitle,
                ranges = emptyList(),
                colors = markdownColors,
                highlightColor = primaryColor,
                renderMarkdown = note.isMarkdown,
                imagePlaceholder = imagePlaceholder
            ),
            excerpt = if (excerptSource.isNotBlank()) {
                buildNoteCardDisplayText(
                    sourceText = excerptSource,
                    ranges = emptyList(),
                    colors = markdownColors,
                    highlightColor = primaryColor,
                    renderMarkdown = note.isMarkdown,
                    imagePlaceholder = imagePlaceholder
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
        maxLines = 2,
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
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrashNoteDate(note: Note) {
    val deletedDate = note.deletedDate
    val label = if (deletedDate != null) {
        stringResource(R.string.trash_deleted_on, DateUtils.formatDate(deletedDate))
    } else {
        stringResource(R.string.trash_edited_on, noteRelativeTimeLabel(note.lastModifiedDate))
    }
    Text(
        text  = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TrashNoteActions(
    onRestore: () -> Unit,
    onDeletePermanently: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RestoreNoteButton(onRestore)
        if (onDeletePermanently != null) {
            DeleteNoteButton(onDeletePermanently)
        }
    }
}

@Composable
private fun RestoreNoteButton(onRestore: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.RestoreFromTrash,
        contentDescription = stringResource(R.string.trash_restore_action_description),
        onClick = onRestore
    )
}

@Composable
private fun DeleteNoteButton(onDeletePermanently: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.DeleteForever,
        contentDescription = stringResource(R.string.trash_delete_action_description),
        destructive = true,
        onClick = onDeletePermanently
    )
}