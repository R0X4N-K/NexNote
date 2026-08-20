package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.util.DateUtils

/**
 * Header row for a note card: title text plus selection or overflow actions.
 *
 * The title is already markdown/search-highlight aware when it reaches this
 * function, so the row only handles layout, truncation, and trailing controls.
 */
@Composable
internal fun NoteCardTitleRow(
    title: AnnotatedString,
    isPinned: Boolean,
    onActions: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isPinned) {
            PinnedNoteBadge()
            Spacer(Modifier.width(9.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selectionMode) {
            SelectionIndicator(selected = selected)
        }
        if (!selectionMode && onActions != null) {
            NoteCardActionsButton(onActions)
        }
    }
}

/** Displays pinned state as a compact status mark without tinting the note body. */
@Composable
private fun PinnedNoteBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "Pinned note",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Two-line preview text shown by the default note card style.
 *
 * It receives an [AnnotatedString] so compact markdown styling and search
 * highlights survive the card-level truncation.
 */
@Composable
internal fun NoteCardPreview(content: AnnotatedString) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Footer metadata for a note card.
 *
 * Displays the relative modification time and a compact Markdown marker when
 * the source note should render with markdown semantics.
 */
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f)
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

/**
 * Decides whether the card can show body preview text for the current style.
 *
 * Empty-title notes promote their first content line into the title slot, so
 * showing the same content again as a preview would be redundant.
 */
internal fun showsContentPreview(note: Note, noteCardStyle: NoteCardStyle): Boolean =
    noteCardStyle == NoteCardStyle.TITLE_AND_PREVIEW &&
        note.title.isNotBlank() &&
        note.content.isNotBlank()

@Composable
private fun NoteCardActionsButton(onActions: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 2.dp)
            .clip(CircleShape)
            .clickable(onClick = onActions)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Note actions",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
            modifier = Modifier.size(16.dp)
        )
    }
}
