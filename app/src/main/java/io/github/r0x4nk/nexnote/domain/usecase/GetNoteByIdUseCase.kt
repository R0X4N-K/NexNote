package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository

class GetNoteByIdUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(noteId: Long): Note? {
        return repository.getNoteById(noteId)
    }
}
