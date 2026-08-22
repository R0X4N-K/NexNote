package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.repository.NoteRepository

class MoveNoteToTrashUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long) {
        repository.moveToTrash(noteId)
    }

    suspend operator fun invoke(noteIds: Collection<Long>) {
        repository.moveToTrash(noteIds)
    }
}
