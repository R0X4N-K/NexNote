package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle

/**
 * Card for a single note in list and grid views.
 *
 * Swipe left (EndToStart) → vertical collapse animation → [onTrash].
 * Swipe right (StartToEnd) → [onPin] and restore the card's resting position.
 * Tap → [onClick].
 * Long-press → [onLongPress].
 *
 * [noteCardStyle] controls how much information is shown:
 *   - TITLE_ONLY: title only (most compact).
 *   - TITLE_AND_PREVIEW: title and content preview (default).
 *   - TITLE_INFORMATION: title, content preview, and note metadata
 *     (date, tags, attachments and image count).
 *
 * [titleHighlightRanges] and [contentHighlightRanges] highlight search matches.
 * Pinned notes expose a compact status badge without recoloring the card.
 *
 * [swipeActionsEnabled] is turned off inside a horizontal pager (e.g. the Agenda
 * tabs) so the page swipe is never captured by a card. Cards never expose their
 * own overflow button: note actions live in the selection top bar, which opens
 * when the note is long-pressed.
 */
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
    noteCardStyle: NoteCardStyle = NoteCardStyle.TITLE_AND_PREVIEW,
    titleHighlightRanges: List<IntRange> = emptyList(),
    contentHighlightRanges: List<IntRange> = emptyList(),
    onPin: () -> Unit = {},
    onLongPress: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    swipeActionsEnabled: Boolean = true
) {
    SwipeToCollectionActionsContainer(
        endToStartAction = SwipeCollectionAction.Delete(stringResource(R.string.common_move_to_trash)),
        onEndToStart = onTrash,
        startToEndAction = SwipeCollectionAction.TogglePin(note.isPinned),
        onStartToEnd = onPin,
        modifier = modifier,
        collapseBeforeEndToStart = true,
        enabled = !selectionMode && swipeActionsEnabled
    ) {
        NoteCardContent(
            note = note,
            onClick = onClick,
            onLongPress = onLongPress,
            selectionMode = selectionMode,
            selected = selected,
            noteCardStyle = noteCardStyle,
            titleHighlightRanges = titleHighlightRanges,
            contentHighlightRanges = contentHighlightRanges
        )
    }
}
