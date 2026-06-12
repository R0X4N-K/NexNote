package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleNotePinUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates actions shared by note-list ViewModels.
 *
 * Home and Agenda expose the same note actions and list controls. Keeping their
 * behavior here preserves identical trash/undo timing, duplicate feedback, and
 * filter toggles without making either ViewModel inherit from a common base.
 */
internal class NoteListActionsDelegate(
    private val scope: CoroutineScope,
    private val moveNoteToTrash: MoveNoteToTrashUseCase,
    private val restoreNoteFromTrash: RestoreNoteFromTrashUseCase,
    private val toggleNotePin: ToggleNotePinUseCase,
    private val duplicateNoteUseCase: DuplicateNoteUseCase?,
    private val sortOrder: MutableStateFlow<SortOrder>,
    private val viewMode: MutableStateFlow<NoteListViewMode>,
    private val selectedTagFilters: MutableStateFlow<Set<String>>,
    private val trashEvents: Channel<TrashedNoteEvent>,
    private val noteActionMessages: Channel<String>
) {

    fun requestTrash(note: Note) {
        requestTrash(listOf(note))
    }

    fun requestTrash(notes: Collection<Note>) {
        val event = notes.toTrashedNoteEvent() ?: return
        scope.launch {
            event.noteIds.forEach { noteId ->
                moveNoteToTrash(noteId)
            }
            trashEvents.trySend(event)
        }
    }

    fun confirmTrash() {
        // No-op: Room flow already reflects the correct state.
    }

    fun undoPendingTrash(noteId: Long) {
        scope.launch { restoreNoteFromTrash(noteId) }
    }

    fun togglePin(note: Note) {
        scope.launch { toggleNotePin(note) }
    }

    fun duplicateNote(note: Note) {
        if (note.isInVault) {
            noteActionMessages.trySend("Could not duplicate note")
            return
        }
        val duplicate = duplicateNoteUseCase ?: return
        val noteLabel = note.displayLabel()
        scope.launch {
            try {
                duplicate(note)
                noteActionMessages.trySend("Duplicated \"$noteLabel\"")
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                noteActionMessages.trySend("Could not duplicate \"$noteLabel\"")
            }
        }
    }

    fun toggleSortOrder() {
        sortOrder.update { current ->
            if (current == SortOrder.MODIFIED_DESC) SortOrder.MODIFIED_ASC else SortOrder.MODIFIED_DESC
        }
    }

    fun toggleViewMode() {
        viewMode.update { current -> current.nextIn() }
    }

    fun toggleTagFilter(tagName: String) {
        selectedTagFilters.update { current ->
            if (tagName in current) current - tagName else current + tagName
        }
    }

    fun removeTagFilter(tagName: String) {
        selectedTagFilters.update { it - tagName }
    }

    fun clearTagFilters() {
        selectedTagFilters.update { emptySet() }
    }
}
