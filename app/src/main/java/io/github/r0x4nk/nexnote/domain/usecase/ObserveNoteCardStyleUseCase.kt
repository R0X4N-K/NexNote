package io.github.r0x4nk.nexnote.domain.usecase

import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.repository.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNoteCardStyleUseCase(
    private val repository: IUserPreferencesRepository
) {
    operator fun invoke(): Flow<NoteCardStyle> {
        return repository.noteCardStyle
    }
}
