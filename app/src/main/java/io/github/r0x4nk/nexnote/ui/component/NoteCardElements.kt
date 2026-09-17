package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.ui.common.NoteMotion
import io.github.r0x4nk.nexnote.ui.common.buildNoteCardMetadata
import io.github.r0x4nk.nexnote.ui.common.noteRelativeTimeLabel

/**
 * Header row for a note card: title text plus selection or overflow actions.
 *
 * The title is already markdown/search-highlight aware when it reaches this
 * function, so the row only handles layout, truncation, and trailing controls.
 * The pin badge expands and collapses horizontally so the pin/unpin state
 * change reads as motion instead of a layout jump.
 */
@Composable
internal fun NoteCardTitleRow(
    title: AnnotatedString,
    isPinned: Boolean,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = isPinned,
            enter = expandHorizontally(
                animationSpec = tween(
                    durationMillis = NoteMotion.CARD_STATE_MS,
                    easing = NoteMotion.cardStateEasing
                ),
                expandFrom = Alignment.Start
            ) + fadeIn(
                animationSpec = tween(durationMillis = NoteMotion.SELECTION_MS)
            ),
            exit = shrinkHorizontally(
                animationSpec = tween(
                    durationMillis = NoteMotion.SELECTION_MS,
                    easing = NoteMotion.cardStateEasing
                ),
                shrinkTowards = Alignment.Start
            ) + fadeOut(
                animationSpec = tween(durationMillis = NoteMotion.SELECTION_MS)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinnedNoteBadge()
                Spacer(Modifier.width(9.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selectionMode) {
            SelectionIndicator(selected = selected)
        }
    }
}

/** Displays pinned state as a compact status mark without tinting the note body. */
@Composable
private fun PinnedNoteBadge() {
    Icon(
        imageVector = Icons.Outlined.PushPin,
        contentDescription = stringResource(R.string.note_pinned),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
    )
}

/**
 * Three-line preview text shown by the default note card style.
 *
 * It receives an [AnnotatedString] so compact markdown styling and search
 * highlights survive the card-level truncation.
 */
@Composable
internal fun NoteCardPreview(content: AnnotatedString) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = noteCardSecondaryColor(NexContentAlpha.Body),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Footer metadata for a note card, shown by the information style.
 *
 * Shows the relative modification time followed by the note's tags and, when
 * present, attachment names and embedded-image count. The former "MD" marker
 * was removed: every note is Markdown, so it carried no information.
 */
@Composable
internal fun NoteCardFooter(
    note: Note,
    primaryColor: Color
) {
    val metadata = remember(note.id, note.content, note.imagePaths) {
        buildNoteCardMetadata(note)
    }
    val secondaryColor = noteCardSecondaryColor(NexContentAlpha.Metadata)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = noteRelativeTimeLabel(note.lastModifiedDate),
            style = MaterialTheme.typography.labelSmall,
            color = secondaryColor
        )
        if (metadata.tags.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = metadata.tags.joinToString(separator = " ") { tag -> "#$tag" },
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (metadata.hasAttachments) {
            Spacer(Modifier.width(8.dp))
            val firstName = metadata.attachmentNames.firstOrNull()
            NoteCardFooterFile(
                icon = Icons.Outlined.AttachFile,
                label = when {
                    firstName == null -> metadata.attachmentCount.toString()
                    metadata.attachmentCount > 1 ->
                        "$firstName +${metadata.attachmentCount - 1}"
                    else -> firstName
                },
                color = secondaryColor
            )
        }
        if (metadata.hasImages) {
            Spacer(Modifier.width(8.dp))
            NoteCardFooterFile(
                icon = Icons.Outlined.Image,
                label = metadata.imageCount.toString(),
                color = secondaryColor
            )
        }
    }
}

@Composable
private fun NoteCardFooterFile(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 132.dp)
        )
    }
}
