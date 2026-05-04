package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository

class SetNoteCardStyleUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(style: NoteCardStyle) {
        repository.setNoteCardStyle(style)
    }
}
