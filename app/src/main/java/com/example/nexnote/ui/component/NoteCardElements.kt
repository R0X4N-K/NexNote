package com.example.nexnote.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nexnote.domain.model.Note
import com.example.nexnote.domain.model.NoteCardStyle
import com.example.nexnote.util.DateUtils

@Composable
internal fun NoteCardTitleRow(
    title: AnnotatedString,
    isPinned: Boolean,
    primaryColor: Color,
    onPin: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        NoteCardPinButton(isPinned, primaryColor, onPin)
    }
}

@Composable
internal fun NoteCardPreview(content: AnnotatedString) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun NoteCardFooter(
    note: Note,
    noteCardStyle: NoteCardStyle,
    primaryColor: Color
) {
    val dateStyle = if (noteCardStyle == NoteCardStyle.TITLE_DATE) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.labelSmall
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = DateUtils.formatRelative(note.lastModifiedDate),
            style = dateStyle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
        if (note.isMarkdown) {
            Text(
                text = "  ·  MD",
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor.copy(alpha = 0.7f)
            )
        }
    }
}

internal fun showsContentPreview(note: Note, noteCardStyle: NoteCardStyle): Boolean =
    noteCardStyle == NoteCardStyle.TITLE_AND_PREVIEW &&
        note.title.isNotBlank() &&
        note.content.isNotBlank()

@Composable
private fun NoteCardPinButton(
    isPinned: Boolean,
    primaryColor: Color,
    onPin: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .clip(CircleShape)
            .clickable(onClick = onPin)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (isPinned) "Unpin" else "Pin to top",
            tint = if (isPinned) {
                primaryColor
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            },
            modifier = Modifier.size(16.dp)
        )
    }
}
