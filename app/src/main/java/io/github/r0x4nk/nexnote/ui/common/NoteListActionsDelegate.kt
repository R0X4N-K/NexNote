package io.github.r0x4nk.nexnote.ui.common

import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.di.StringProvider
import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.usecase.DuplicateNoteUseCase
import io.github.r0x4nk.nexnote.domain.usecase.MoveNoteToTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.RestoreNoteFromTrashUseCase
import io.github.r0x4nk.nexnote.domain.usecase.ToggleNotePinUseCase
import io.github.r0x4nk.nexnote.domain.usecase.UpdateNoteCreationDateUseCase
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
    private val duplicateNoteUseCase: DuplicateNoteUseCase,
    private val updateNoteCreationDate: UpdateNoteCreationDateUseCase,
    private val sortOrder: MutableStateFlow<SortOrder>,
    private val viewMode: MutableStateFlow<NoteListViewMode>,
    private val selectedTagFilters: MutableStateFlow<Set<String>>,
    private val trashEvents: Channel<TrashedNoteEvent>,
    private val noteActionMessages: Channel<String>,
    private val strings: StringProvider
) {
    private val noteMutations = NoteMutationActions(
        scope = scope,
        moveNoteToTrash = moveNoteToTrash,
        restoreNoteFromTrash = restoreNoteFromTrash,
        duplicateNoteUseCase = duplicateNoteUseCase,
        updateNoteCreationDate = updateNoteCreationDate,
        trashEvents = trashEvents,
        noteActionMessages = noteActionMessages,
        strings = strings
    )

    val operationProgress get() = noteMutations.operationProgress

    fun requestTrash(note: Note) {
        noteMutations.requestTrash(note)
    }

    fun requestTrash(notes: Collection<Note>) {
        noteMutations.requestTrash(notes)
    }

    fun requestTrashByIds(noteIds: Collection<Long>) {
        noteMutations.requestTrashByIds(noteIds)
    }

    fun confirmTrash() {
        noteMutations.confirmTrash()
    }

    fun undoPendingTrash(noteId: Long) {
        noteMutations.undoPendingTrash(noteId)
    }

    fun undoPendingTrash(noteIds: Collection<Long>) {
        noteMutations.undoPendingTrash(noteIds)
    }

    fun togglePin(note: Note) {
        scope.launch { toggleNotePin(note) }
    }

    fun duplicateNote(note: Note) {
        noteMutations.duplicateNote(note)
    }

    fun updateCreationDate(note: Note, creationDate: Long) {
        noteMutations.updateCreationDate(note, creationDate)
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

/**
 * Executes the normal-note mutations shared by every list surface.
 *
 * The delegate owns coroutine cancellation semantics and user feedback while
 * callers retain their screen-specific state and filtering controls.
 */
internal class NoteMutationActions(
    private val scope: CoroutineScope,
    private val moveNoteToTrash: MoveNoteToTrashUseCase,
    private val restoreNoteFromTrash: RestoreNoteFromTrashUseCase,
    private val duplicateNoteUseCase: DuplicateNoteUseCase,
    private val updateNoteCreationDate: UpdateNoteCreationDateUseCase,
    private val trashEvents: Channel<TrashedNoteEvent>,
    private val noteActionMessages: Channel<String>,
    private val strings: StringProvider
) {
    private val operations = NoteOperationRunner(scope)
    val operationProgress = operations.progress

    private fun mutate(label: String, action: suspend () -> Unit) {
        operations.launch(label, onError = {
            noteActionMessages.trySend(strings.get(R.string.note_op_generic_error))
        }, action = action)
    }

    fun requestTrash(note: Note) {
        requestTrash(listOf(note))
    }

    fun requestTrash(notes: Collection<Note>) {
        val event = notes.toTrashedNoteEvent(strings.get(R.string.untitled_note)) ?: return
        mutate(strings.get(R.string.vault_progress_move_trash)) {
            moveNoteToTrash(event.noteIds)
            trashEvents.trySend(event)
        }
    }

    fun requestTrashByIds(noteIds: Collection<Long>) {
        val event = trashedNoteEventForIds(
            noteIds,
            strings.get(R.string.untitled_note)
        ) ?: return
        mutate(strings.get(R.string.vault_progress_move_trash)) {
            moveNoteToTrash(event.noteIds)
            trashEvents.trySend(event)
        }
    }

    fun confirmTrash() {
        // No-op: Room flow already reflects the correct state.
    }

    fun undoPendingTrash(noteId: Long) {
        mutate(strings.get(R.string.trash_progress_restore_note)) { restoreNoteFromTrash(noteId) }
    }

    fun undoPendingTrash(noteIds: Collection<Long>) {
        mutate(strings.get(R.string.trash_progress_restore_notes)) { restoreNoteFromTrash(noteIds) }
    }

    fun duplicateNote(note: Note) {
        if (note.isInVault) {
            noteActionMessages.trySend(strings.get(R.string.note_op_duplicate_error))
            return
        }
        val noteLabel = note.displayLabel(untitledLabel = strings.get(R.string.untitled_note))
        mutate(strings.get(R.string.vault_progress_duplicate)) {
            try {
                duplicateNoteUseCase(note)
                noteActionMessages.trySend(strings.get(R.string.note_op_duplicated, noteLabel))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                noteActionMessages.trySend(
                    strings.get(R.string.note_op_duplicate_failed, noteLabel)
                )
            }
        }
    }

    fun updateCreationDate(note: Note, creationDate: Long) {
        if (note.isInVault || note.isDeleted) return
        mutate(strings.get(R.string.note_op_update_date_progress)) {
            val updated = updateNoteCreationDate(note.id, creationDate)
            noteActionMessages.trySend(
                strings.get(
                    if (updated) {
                        R.string.note_op_date_updated
                    } else {
                        R.string.note_op_date_update_failed
                    }
                )
            )
        }
    }
}