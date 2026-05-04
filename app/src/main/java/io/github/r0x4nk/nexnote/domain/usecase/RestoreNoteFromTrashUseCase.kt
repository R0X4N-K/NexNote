package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteRepository

class RestoreNoteFromTrashUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long) {
        repository.restoreFromTrash(noteId)
    }
}
