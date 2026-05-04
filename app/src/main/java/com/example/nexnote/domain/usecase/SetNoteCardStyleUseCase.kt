package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.NoteCardStyle
import com.example.nexnote.domain.repository.IUserPreferencesRepository

class SetNoteCardStyleUseCase(
    private val repository: IUserPreferencesRepository
) {
    suspend operator fun invoke(style: NoteCardStyle) {
        repository.setNoteCardStyle(style)
    }
}
