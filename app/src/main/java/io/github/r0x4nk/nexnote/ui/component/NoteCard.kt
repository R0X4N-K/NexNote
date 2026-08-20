package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 *   - TITLE_ONLY: title and date only (most compact).
 *   - TITLE_AND_PREVIEW: title, content preview, and date (default).
 *   - TITLE_DATE: title and date with the date shown more prominently.
 *
 * [titleHighlightRanges] and [contentHighlightRanges] highlight search matches.
 * Pinned notes expose a compact status badge without recoloring the card.
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
    onActions: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    SwipeToCollectionActionsContainer(
        endToStartAction = SwipeCollectionAction.Delete("Move to trash"),
        onEndToStart = onTrash,
        startToEndAction = SwipeCollectionAction.TogglePin(note.isPinned),
        onStartToEnd = onPin,
        modifier = modifier,
        collapseBeforeEndToStart = true,
        enabled = !selectionMode
    ) {
        NoteCardContent(
            note = note,
            onClick = onClick,
            onLongPress = onLongPress,
            onActions = onActions,
            selectionMode = selectionMode,
            selected = selected,
            noteCardStyle = noteCardStyle,
            titleHighlightRanges = titleHighlightRanges,
            contentHighlightRanges = contentHighlightRanges
        )
    }
}
