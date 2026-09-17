package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.ScoredNote
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.component.NoteCard

/**
 * Shared note card used by every Agenda surface (calendar day list, masonry
 * grid, tag folders and the timeline) so behaviour and highlighting stay
 * identical across them.
 */
@Composable
internal fun AgendaNoteCardItem(
    scored: ScoredNote,
    noteCardStyle: NoteCardStyle,
    selectionState: SelectionUiState,
    actions: AgendaActions,
    modifier: Modifier = Modifier
) {
    val note = scored.note
    NoteCard(
        note = note,
        onClick = {
            if (selectionState.isActive) {
                actions.onToggleNoteSelection(note)
            } else {
                actions.onNoteClick(note.id)
            }
        },
        noteCardStyle = noteCardStyle,
        titleHighlightRanges = scored.titleRanges,
        contentHighlightRanges = scored.contentRanges,
        onPin = { actions.onTogglePin(note) },
        onLongPress = { actions.onToggleNoteSelection(note) },
        selectionMode = selectionState.isActive,
        selected = selectionState.isSelected(note.id),
        modifier = modifier,
        onTrash = { actions.onRequestTrash(note) },
        // Card swipes would steal the horizontal pager gesture, so pin/trash
        // stay available through the card's overflow actions on Agenda.
        swipeActionsEnabled = false
    )
}
