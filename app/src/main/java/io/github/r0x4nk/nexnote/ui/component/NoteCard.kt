package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle

/**
 * Card for a single note in list and grid views.
 *
 * Swipe left (EndToStart) → vertical collapse animation → [onTrash].
 * Tap → [onClick].
 * Pin button (top-right of card header) → [onPin] toggles the pinned state.
 * Long-press → [onLongPress].
 * [showPinAction] can hide the pin affordance on surfaces that render note
 * cards but do not yet support pin toggling.
 *
 * [noteCardStyle] controls how much information is shown:
 *   - TITLE_ONLY: title and date only (most compact).
 *   - TITLE_AND_PREVIEW: title, content preview, and date (default).
 *   - TITLE_DATE: title and date with the date shown more prominently.
 *
 * [titleHighlightRanges] and [contentHighlightRanges] highlight search matches.
 * Pinned notes receive a subtle primaryContainer tint.
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
    showPinAction: Boolean = true,
    selectionMode: Boolean = false,
    selected: Boolean = false
) {
    SwipeToDeleteContainer(
        onDelete = onTrash,
        contentDescription = "Move to trash",
        modifier = modifier,
        collapseBeforeDelete = true,
        enabled = !selectionMode
    ) {
        NoteCardContent(
            note = note,
            onClick = onClick,
            onPin = onPin,
            onLongPress = onLongPress,
            onActions = onActions,
            showPinAction = showPinAction,
            selectionMode = selectionMode,
            selected = selected,
            noteCardStyle = noteCardStyle,
            titleHighlightRanges = titleHighlightRanges,
            contentHighlightRanges = contentHighlightRanges
        )
    }
}
