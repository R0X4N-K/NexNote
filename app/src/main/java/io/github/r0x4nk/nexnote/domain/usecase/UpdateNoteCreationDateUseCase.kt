package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteRepository

/**
 * Updates the user-editable creation date of an active normal note.
 *
 * The note is reloaded from storage so the caller only needs its id, and only
 * the `creationDate` field is changed. Vault notes and trashed notes are
 * rejected: this use case never touches encrypted or deleted content.
 *
 * @return `true` when the note was updated, `false` when it is missing, trashed
 * or stored in the Vault.
 */
class UpdateNoteCreationDateUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long, creationDate: Long): Boolean {
        val note = repository.getNoteById(noteId) ?: return false
        if (note.isDeleted || note.isInVault) return false
        repository.saveNote(note.copy(creationDate = creationDate))
        return true
    }
}
