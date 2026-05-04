package com.example.nexnote.domain.usecase

import com.example.nexnote.domain.model.NoteCardStyle
import com.example.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNoteCardStyleUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<NoteCardStyle> {
        return repository.noteCardStyle
    }
}
