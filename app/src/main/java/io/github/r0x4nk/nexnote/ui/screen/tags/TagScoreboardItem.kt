package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.Tag
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.util.DateUtils

@Composable
internal fun TagScoreboardItem(
    tag: Tag,
    maxCount: Int,
    isExpanded: Boolean,
    notes: List<Note>,
    onTagClick: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isExpanded) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (isExpanded) 2.dp else 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TagScoreboardHeader(
                tag = tag,
                maxCount = maxCount,
                isExpanded = isExpanded,
                onTagClick = onTagClick,
                onDeleteClick = onDeleteClick
            )
            ExpandedNotesSection(
                isExpanded = isExpanded,
                notes = notes,
                onNoteClick = onNoteClick
            )
        }
    }
}

@Composable
private fun TagScoreboardHeader(
    tag: Tag,
    maxCount: Int,
    isExpanded: Boolean,
    onTagClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTagClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TagUsageColumn(
            tag = tag,
            maxCount = maxCount,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        ExpandCollapseIcon(isExpanded)
        DeleteTagButton(tag.name, onDeleteClick)
    }
}

@Composable
private fun TagUsageColumn(
    tag: Tag,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${tag.name}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = tag.noteCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { tag.noteCount.toFloat() / maxCount.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
        )
    }
}

@Composable
private fun ExpandCollapseIcon(isExpanded: Boolean) {
    Icon(
        imageVector = if (isExpanded) {
            Icons.Default.KeyboardArrowUp
        } else {
            Icons.Default.KeyboardArrowDown
        },
        contentDescription = if (isExpanded) "Collapse" else "Expand",
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun DeleteTagButton(tagName: String, onDeleteClick: () -> Unit) {
    NexIconButton(
        imageVector = Icons.Default.Delete,
        contentDescription = "Delete #$tagName",
        onClick = onDeleteClick,
        destructive = true,
        modifier = Modifier.size(36.dp)
    )
}

@Composable
private fun ExpandedNotesSection(
    isExpanded: Boolean,
    notes: List<Note>,
    onNoteClick: (Long) -> Unit
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(tween(150)) + fadeIn(tween(150)),
        exit = shrinkVertically(tween(130)) + fadeOut(tween(110))
    ) {
        ExpandedNotesContent(notes = notes, onNoteClick = onNoteClick)
    }
}

@Composable
private fun ExpandedNotesContent(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (notes.isEmpty()) {
            EmptyNotesText()
        } else {
            NotesPreviewList(notes, onNoteClick)
        }
    }
}

@Composable
private fun EmptyNotesText() {
    Text(
        text = "No notes",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    )
}

@Composable
private fun NotesPreviewList(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit
) {
    val displayed = notes.take(MAX_NOTES_SHOWN)
    displayed.forEach { note ->
        NoteRowItem(note = note, onClick = { onNoteClick(note.id) })
    }
    if (notes.size > MAX_NOTES_SHOWN) {
        Text(
            text = "+${notes.size - MAX_NOTES_SHOWN} more",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun NoteRowItem(note: Note, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = note.title.ifBlank { "Untitled note" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = DateUtils.formatRelative(note.lastModifiedDate),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}
