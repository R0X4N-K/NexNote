package com.example.nexnote.ui.screen.trash

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nexnote.domain.model.Note
import com.example.nexnote.util.DateUtils

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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
    Column(
        modifier = Modifier.padding(
            start = 16.dp, top = 12.dp, bottom = 4.dp, end = 8.dp
        )
    ) {
        TrashNoteTitle(note)
        TrashNoteExcerpt(note)
        Spacer(Modifier.height(4.dp))
        TrashNoteDate(note)
        TrashNoteActions(onRestore, onDeletePermanently)
    }
}

@Composable
private fun TrashNoteTitle(note: Note) {
    Text(
        text     = noteDisplayTitle(note),
        style    = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TrashNoteExcerpt(note: Note) {
    if (note.title.isNotBlank() && note.content.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text     = note.content.take(120),
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
    IconButton(onClick = onRestore) {
        Icon(
            imageVector        = Icons.Default.RestoreFromTrash,
            contentDescription = "Restore note",
            tint               = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DeleteNoteButton(onDeletePermanently: () -> Unit) {
    IconButton(onClick = onDeletePermanently) {
        Icon(
            imageVector        = Icons.Default.DeleteForever,
            contentDescription = "Delete permanently",
            tint               = MaterialTheme.colorScheme.error
        )
    }
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
