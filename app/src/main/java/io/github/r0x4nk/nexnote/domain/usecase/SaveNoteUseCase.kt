package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.Note
import io.github.r0x4nk.nexnote.domain.repository.NoteRepository

class SaveNoteUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Long {
        return repository.saveNote(note)
    }
}
